package urbachyannick.approxflow.cnf;

import org.objectweb.asm.tree.*;
import urbachyannick.approxflow.javasignatures.*;

import java.util.*;
import java.util.stream.*;

import static urbachyannick.approxflow.codetransformation.BytecodeUtil.*;

public class JavaVarToCnfVar {
    private static IntStream variablesForMapping(VariableMapping mapping) {
        return mapping.getMappingValues()
                .filter(v -> !v.isTrivial())
                .mapToInt(v -> ((Literal) v).getVariable());
    }

    private static Stream<VariableMapping> allMappings(VariableTable varTable, Signature signature) {
        return varTable.getMatching(signature);
    }

    public static Optional<VariableMapping> lastMapping(VariableTable varTable, Signature signature) {
            return varTable
                    .getMatching(signature)
                    .max(VariableMapping::compareByGeneration);
    }

    public static IntStream variablesForMethodReturnValues(List<ClassNode> classes, VariableTable variableTable, ClassNode owner, MethodNode method, int addressOffset) {
        TypeSpecifier returnType = getReturnType(method);
        TypeSpecifier[] argumentTypes = getArgumentTypes(method).toArray(TypeSpecifier[]::new);

        JavaSignature signature = new JavaSignature(
                ClassName.tryParseFromTypeSpecifier("L" + owner.name + ";", new MutableInteger(0)),
                new FunctionCall(method.name, argumentTypes, returnType, new ReturnValue())
        );

        Stream<VariableMapping> roots = allMappings(variableTable, signature);
        return roots
                .flatMap(root -> cascade(classes, variableTable, root, returnType, addressOffset))
                .flatMapToInt(JavaVarToCnfVar::variablesForMapping);
    }

    public static IntStream variablesForStaticField(List<ClassNode> classes, VariableTable variableTable, ClassNode owner, FieldNode field, int addressOffset) {
        TypeSpecifier fieldType = getFieldType(field);

        Signature signature = new JavaSignature(
                ClassName.tryParseFromTypeSpecifier("L" + owner.name + ";", new MutableInteger(0)),
                new FieldAccess(field.name)
        );

        Optional<VariableMapping> root = lastMapping(variableTable, signature);

        if (!root.isPresent()) {
            System.err.println("Can not find variable line for " + signature.toString());
            return IntStream.empty();
        }

        return cascade(classes, variableTable, root.get(), fieldType, addressOffset)
                .flatMapToInt(JavaVarToCnfVar::variablesForMapping);
    }

    public static IntStream variablesForMethodParameter(List<ClassNode> classes, VariableTable variableTable, ClassNode owner, MethodNode method, int paramIndex, int addressOffset) {
        TypeSpecifier returnType = getReturnType(method);
        TypeSpecifier[] argumentTypes = getArgumentTypes(method).toArray(TypeSpecifier[]::new);

        if (paramIndex < 0 || paramIndex >= argumentTypes.length)
            throw new IndexOutOfBoundsException("paramIndex must be between 0 (inclusive) and the number of parameters (exclusive).");

        TypeSpecifier argumentType = argumentTypes[paramIndex];

        JavaSignature signature = new JavaSignature(
                ClassName.tryParseFromTypeSpecifier("L" + owner.name + ";", new MutableInteger(0)),
                new FunctionCall(
                        method.name,
                        argumentTypes,
                        returnType,
                        new AnonymousParameter(paramIndex, argumentType.asPrimitive())
                )
        );

        Stream<VariableMapping> roots = allMappings(variableTable, signature);
        return roots
                .flatMap(root -> cascade(classes, variableTable, root, argumentType, addressOffset))
                .flatMapToInt(JavaVarToCnfVar::variablesForMapping);
    }

    private static Stream<VariableMapping> mappingsForArray(VariableTable varTable, long address) {
        // all lines of elements of the array with this address, grouped by the element index
        Map<Long, List<VariableMapping>> elementLines = varTable.getMappings()
                .filter(l -> (
                        l.getSignature() instanceof DynamicArraySignature &&
                        ((DynamicArraySignature) l.getSignature()).getAddress() == address
                ))
                .collect(Collectors.groupingBy(l -> ((DynamicArraySignature) l.getSignature()).getElementIndex()));

        // the last generation of each individual element index
        return elementLines.values().stream()
                .map(l -> l.stream().max(VariableMapping::compareByGeneration).get());
    }

    private static class FieldMapping {
        public final TypeSpecifier fieldType;
        public final VariableMapping mapping;

        public FieldMapping(TypeSpecifier fieldType, VariableMapping mapping) {
            this.fieldType = fieldType;
            this.mapping = mapping;
        }
    }

    private static Stream<FieldMapping> mappingsForObjectFields(VariableTable varTable, long address, ClassNode class_) {
        List<FieldMapping> mappings = new ArrayList<>();

        for (FieldNode field : class_.fields) {
            DynamicObjectSignature signature = new DynamicObjectSignature(address, field.name);

            Optional<VariableMapping> mapping = lastMapping(varTable, signature);

            if (!mapping.isPresent()) {
                System.err.println("Can not find variable line for " + signature.toString());
                break;
            }

            mappings.add(new FieldMapping(TypeSpecifier.parse(field.desc, new MutableInteger(0)), mapping.get()));
        }

        return mappings.stream();
    }

    private static Stream<VariableMapping> cascade(List<ClassNode> classes, VariableTable varTable, VariableMapping root, TypeSpecifier type, int addressOffset) {
        if (type.isPrimitive()) {
            return Stream.of(root);
        }

        if (type instanceof ArrayType) {
            Stream<TrivialMappingValue> addressLiterals = root
                    .getMappingValues()
                    .filter(MappingValue::isTrivial)
                    .map(v -> (TrivialMappingValue) v);

            long address = parseAddressFromTrivialLiterals(addressLiterals);
            address -= addressOffset;

            Stream<VariableMapping> elementMappings = mappingsForArray(varTable, address);

            return elementMappings.flatMap(m -> cascade(classes, varTable, m, ((ArrayType) type).getElementType(), addressOffset));
        }

        if (type instanceof ClassName) {
            ClassName className = (ClassName) type;
            Optional<ClassNode> classNode = findClass(classes.stream(), className.asQualifiedName());

            if (!classNode.isPresent()) {
                System.err.println("Can not find class " + className.asQualifiedName());
                return Stream.empty();
            }

            Stream<TrivialMappingValue> addressLiterals = root
                    .getMappingValues()
                    .filter(MappingValue::isTrivial)
                    .map(v -> (TrivialMappingValue) v);

            long address = parseAddressFromTrivialLiterals(addressLiterals);
            address -= addressOffset;
            address -= 1;

            Stream<FieldMapping> fieldMappings = mappingsForObjectFields(varTable, address, classNode.get());

            return fieldMappings.flatMap(m -> cascade(classes, varTable, m.mapping, m.fieldType, addressOffset));
        }

        System.err.println("Can not get SAT variables for type " + type);
        return Stream.empty();
    }

    // Weirdly seems to be in 16 bit words, most significant 16-bit word first, but least significant bit first within words
    private static long parseAddressFromTrivialLiterals(Stream<TrivialMappingValue> literals) {
        Iterator<TrivialMappingValue> iterator = literals.iterator();

        long result = 0;

        for (int word = 0; word < 4; ++word) {
            for (int bit = 0; bit < 16; ++bit) {
                if (!iterator.hasNext())
                    throw new IllegalArgumentException("Must have exactly 64 literals");

                TrivialMappingValue literal = iterator.next();

                result |= (long)(literal.get() ? 1 : 0) << (16 * (3 - word)) << bit;
            }
        }

        if (iterator.hasNext())
            throw new IllegalArgumentException("Must have exactly 64 literals");

        return result;
    }
}
