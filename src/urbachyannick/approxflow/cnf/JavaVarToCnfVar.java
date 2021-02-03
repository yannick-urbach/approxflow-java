package urbachyannick.approxflow.cnf;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import urbachyannick.approxflow.javasignatures.*;

import java.util.*;
import java.util.stream.*;

import static urbachyannick.approxflow.codetransformation.BytecodeUtil.*;

public class JavaVarToCnfVar {
    public static IntStream variablesForMapping(VariableMapping mapping) {
        return mapping.getMappingValues()
                .filter(v -> !v.isTrivial())
                .mapToInt(v -> ((Literal) v).getVariable());
    }

    public static Stream<VariableMapping> allMappings(VariableTable varTable, Signature signature) {
        return varTable.getMatching(signature);
    }

    public static VariableMapping lastMapping(VariableTable varTable, Signature signature) throws CnfException {
            return varTable
                    .getMatching(signature)
                    .max(VariableMapping::compareByGeneration)
                    .orElseThrow(() -> new CnfException("missing variable line for " + signature.toString()));
    }

    public static IntStream variablesForMethodReturnValues(List<ClassNode> classes, VariableTable variableTable, ClassNode owner, MethodNode method, int addressOffset) {
        TypeSpecifier returnType = getReturnType(method);
        TypeSpecifier[] argumentTypes = getArgumentTypes(method).toArray(TypeSpecifier[]::new);

        IntStream.Builder builder = IntStream.builder();

        JavaSignature signature = new JavaSignature(
                ClassName.tryParseFromTypeSpecifier("L" + owner.name + ";", new MutableInteger(0)),
                new FunctionCall(method.name, argumentTypes, returnType, new ReturnValue())
        );

        Iterator<VariableMapping> roots = JavaVarToCnfVar.allMappings(variableTable, signature).iterator();
        while (roots.hasNext()) {
            JavaVarToCnfVar
                    .cascade(classes, variableTable, roots.next(), returnType, addressOffset)
                    .forEach(m -> JavaVarToCnfVar.variablesForMapping(m).forEach(builder::add));
        }

        return builder.build();
    }

    public static IntStream variablesForStaticField(VariableTable varTable, Signature signature) {
        try {
            return variablesForMapping(lastMapping(varTable, signature));
        } catch (CnfException e) {
            System.err.println("Can not find variable line for " + signature.toString());
            return IntStream.empty();
        }
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
        Stream<VariableMapping> lastGeneration = elementLines.values().stream()
                .map(l -> l.stream().max(VariableMapping::compareByGeneration).get());

        return lastGeneration;
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

            try {
                mappings.add(new FieldMapping(TypeSpecifier.parse(field.desc, new MutableInteger(0)), lastMapping(varTable, signature)));
            } catch (CnfException e) {
                System.err.println("Can not find variable line for " + signature.toString());
            }
        }

        return mappings.stream();
    }

    public static Stream<VariableMapping> cascade(List<ClassNode> classes, VariableTable varTable, VariableMapping root, TypeSpecifier type, int addressOffset) {
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
    public static long parseAddressFromTrivialLiterals(Stream<TrivialMappingValue> literals) {
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

    public static int getAddressOffset(Stream<ClassNode> classes) {
        return hasAssertionClassInitCode(classes) ? 3 : 0;
    }

    private static boolean hasAssertionClassInitCode(Stream<ClassNode> classes) {
        return classes.anyMatch(class_ -> {

            Optional<MethodNode> classInit = findMethod(class_, class_.name, "<clinit>", "()V");

            return classInit.map(i -> {

                for (AbstractInsnNode insn : i.instructions) {

                    if (insn.getType() != AbstractInsnNode.FIELD_INSN)

                        if (insn.getOpcode() != Opcodes.PUTSTATIC)
                            continue;

                    FieldInsnNode fieldInsn = (FieldInsnNode) insn;

                    if (!fieldInsn.owner.equals(class_.name))
                        continue;

                    if (!fieldInsn.name.equals("$assertionsDisabled"))
                        continue;

                    if (fieldInsn.desc.equals("Z"))
                        return true;
                }

                return false;
            }).orElse(false);
        });
    }
}
