package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import urbachyannick.approxflow.Fail;
import urbachyannick.approxflow.cnf.*;
import urbachyannick.approxflow.javasignatures.*;

import java.util.*;
import java.util.stream.Collectors;

import static urbachyannick.approxflow.MiscUtil.*;
import static urbachyannick.approxflow.codetransformation.BytecodeUtil.findMainMethod;

public class OffsetMarker extends Transformation.PerClassNoExcept {

    private static final int markerSize = 1;

    private static final String markerName = "$$offsetMarker";
    private static final String markerType = "[J";

    private final long[] marker = new long[markerSize];
    private ClassName className;

    @Override
    protected ClassNode applyToClass(ClassNode sourceClass) {

        if (sourceClass.methods.stream().anyMatch(BytecodeUtil::isMainMethod)) {

            ClassNode targetClass = new ClassNode(Opcodes.ASM5);
            sourceClass.accept(targetClass);

            className = ClassName.tryParseFromTypeSpecifier("L" + targetClass.name + ";", new MutableInteger(0));

            MethodNode mainMethod = findMainMethod(targetClass).get();

            FieldNode outputArray = new FieldNode(
                    Opcodes.ASM5,
                    Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC,
                    markerName,
                    markerType,
                    null,
                    null
            );
            targetClass.fields.add(outputArray);

            Random random = new Random();

            InsnList initInstructions = new InsnList();

            initInstructions.add(new LdcInsnNode(markerSize));
            initInstructions.add(new IntInsnNode(Opcodes.NEWARRAY, PrimitiveType.LONG.getTypeOpcode()));

            for (int i = 0; i < markerSize; ++i) {
                long l = random.nextLong();
                marker[i] = l;
                initInstructions.add(new InsnNode(Opcodes.DUP));
                initInstructions.add(new LdcInsnNode(i));
                initInstructions.add(new LdcInsnNode(l));
                initInstructions.add(new InsnNode(PrimitiveType.LONG.getArrayStoreOpcode()));
            }

            initInstructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, targetClass.name, markerName, markerType));

            mainMethod.instructions.insertBefore(mainMethod.instructions.getFirst(), initInstructions);

            return targetClass;
        }

        return sourceClass;
    }

    private boolean matchesMarker(long index, List<VariableMapping> mappings) {
        if (mappings == null)
            return false;

        Optional<VariableMapping> maxOpt = mappings.stream().max(VariableMapping::compareByGeneration);

        if (!maxOpt.isPresent())
            return false;

        VariableMapping max = maxOpt.get();

        if (!max.getMappingValues().allMatch(MappingValue::isTrivial))
            return false;

        return parseLongFromTrivialLiterals(max.getMappingValues().map(v -> (TrivialMappingValue) v)) == marker[(int) index];
    }

    private boolean matchesMarker(Map<Long, List<VariableMapping>> mappingsByElementIndex) {
        for (int i = 0; i < markerSize; ++i) {
            List<VariableMapping> mappings = mappingsByElementIndex.get((long) i);

            if (!matchesMarker(i, mappings))
                return false;
        }

        return true;
    }

    public int getAddressOffset(VariableTable varTable) {
        Signature referenceSignature = new JavaSignature(className, new FieldAccess(markerName));
        VariableMapping referenceMapping = JavaVarToCnfVar.lastMapping(varTable, referenceSignature).get();
        long referenceAddress = parseAddressFromTrivialLiterals(referenceMapping.getMappingValues().map(v -> (TrivialMappingValue) v));

        int offset = 0;
        int matchingCount = 0;

        Map<Long, List<VariableMapping>> mappingsByArrayAddress = varTable.getMappings()
                .filter(m -> m.getSignature() instanceof DynamicArraySignature)
                .collect(Collectors.groupingBy(m -> ((DynamicArraySignature) m.getSignature()).getAddress()));

        for (long arrayAddress : mappingsByArrayAddress.keySet()) {
            Map<Long, List<VariableMapping>> mappingsByElementIndex = mappingsByArrayAddress.get(arrayAddress).stream()
                    .collect(Collectors.groupingBy(m -> ((DynamicArraySignature) m.getSignature()).getElementIndex()));

            if (matchesMarker(mappingsByElementIndex)) {
                offset = (int) (referenceAddress - arrayAddress);
                matchingCount++;
            }
        }

        if (matchingCount == 0)
            throw new Fail("Could not determine address offset");

        if (matchingCount > 1)
            throw new AmbiguousOffsetMarkerException();

        return offset;
    }
}
