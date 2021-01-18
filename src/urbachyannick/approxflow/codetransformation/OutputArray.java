package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import urbachyannick.approxflow.MiscUtil;
import urbachyannick.approxflow.cnf.*;
import urbachyannick.approxflow.javasignatures.*;

import java.util.*;
import java.util.stream.*;

import static urbachyannick.approxflow.codetransformation.BytecodeUtil.*;

public class OutputArray implements Scanner<IntStream> {
    @Override
    public IntStream scan(Stream<ClassNode> sourceClasses, MappedProblem problem) {

        List<ClassNode> classesList = sourceClasses.collect(Collectors.toList());
        int addressOffset = hasAssertionClassInitCode(classesList.stream()) ? 3 : 0;
        List<VariableMapping> mappings = problem.getVariableTable().getMappings().collect(Collectors.toList());

        return classesList.stream().flatMapToInt(sourceClass ->
                sourceClass.fields.stream().flatMapToInt(field -> {

                    if (!hasFlag(field.access, Opcodes.ACC_STATIC))
                        return IntStream.empty();

                    if (!hasAnnotation(field.visibleAnnotations, "Lurbachyannick/approxflow/PublicOutput;"))
                        return IntStream.empty();

                    TypeSpecifier fieldType = TypeSpecifier.parse(field.desc, new MutableInteger(0));

                    if (!(fieldType instanceof ArrayType))
                        return IntStream.empty();

                    TypeSpecifier elementType = ((ArrayType) fieldType).getElementType();

                    if (!elementType.isPrimitive())
                        return IntStream.empty();

                    JavaSignature fieldSignature = new JavaSignature(
                            ClassName.tryParseFromTypeSpecifier("L" + sourceClass.name + ";", new MutableInteger(0)),
                            new FieldAccess(field.name)
                    );

                    Optional<VariableMapping> varLine = mappings.stream()
                            .filter(l -> fieldSignature.matches(l.getSignature()))
                            .max(VariableMapping::compareByGeneration);

                    if (!varLine.isPresent()) {
                        System.err.println("Can not find variable line for " + fieldSignature.toString());
                        return IntStream.empty();
                    }

                    long address = MiscUtil.parseAddressFromTrivialLiterals(
                            varLine.get().getMappingValues().map(v -> (TrivialMappingValue) v)
                    ) - addressOffset;

                    // all lines of elements of the array with this address, grouped by the element index
                    Map<Long, List<VariableMapping>> elementLines = mappings.stream()
                            .filter(l -> (
                                    l.getSignature() instanceof DynamicArraySignature &&
                                    ((DynamicArraySignature) l.getSignature()).getAddress() == address
                            ))
                            .collect(Collectors.groupingBy(l -> ((DynamicArraySignature) l.getSignature()).getElementIndex()));

                    // the literals of the last generation of each individual element index
                    IntStream literals = elementLines.values().stream()
                            .flatMapToInt(
                                    l -> l.stream()
                                            .max(VariableMapping::compareByGeneration)
                                            .get()
                                            .getMappingValues()
                                            .filter(v -> !v.isTrivial())
                                            .mapToInt(v -> ((Literal) v).getVariable())
                            );

                    return literals;
                })
        );
    }

    public Boolean hasAssertionClassInitCode(Stream<ClassNode> classes) {
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
