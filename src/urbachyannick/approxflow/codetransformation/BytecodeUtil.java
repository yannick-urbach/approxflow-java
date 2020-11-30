package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class BytecodeUtil {
    private static final Map<Integer, Integer> jumpInversions = new HashMap<Integer, Integer>() {
        void putSymmetric(Integer left, Integer right) {
            put(left, right);
            put(right, left);
        }

        {
            putSymmetric(Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE);
            putSymmetric(Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE);
            putSymmetric(Opcodes.IF_ICMPGE, Opcodes.IF_ICMPLT);
            putSymmetric(Opcodes.IF_ICMPLE, Opcodes.IF_ICMPGT);
            putSymmetric(Opcodes.IFEQ, Opcodes.IFNE);
            putSymmetric(Opcodes.IFGE, Opcodes.IFLT);
            putSymmetric(Opcodes.IFLE, Opcodes.IFGT);
            putSymmetric(Opcodes.IFNULL, Opcodes.IFNONNULL);
        }
    };

    private static final Map<Integer, Object> specialConstInstructions = new HashMap<Integer, Object>() {{
        put(Opcodes.ICONST_0, 0);
        put(Opcodes.ICONST_1, 1);
        put(Opcodes.ICONST_2, 2);
        put(Opcodes.ICONST_3, 3);
        put(Opcodes.ICONST_4, 4);
        put(Opcodes.ICONST_5, 5);
        put(Opcodes.ICONST_M1, -1);
        put(Opcodes.FCONST_0, 0f);
        put(Opcodes.FCONST_1, 1f);
        put(Opcodes.FCONST_2, 2f);
        put(Opcodes.DCONST_0, 0d);
        put(Opcodes.DCONST_1, 1d);
        put(Opcodes.LCONST_0, 0L);
        put(Opcodes.LCONST_1, 1L);
    }};

    public static Optional<MethodNode> findMainMethod(ClassNode classNode) {
        return classNode.methods.stream()
                .filter(m ->
                        m.name.equals("main") &&
                        m.desc.equals("([Ljava/lang/String;)V") &&
                        hasFlag(m.access, Opcodes.ACC_STATIC)
                ).findFirst();
    }

    public static boolean hasFlag(int value, int flag) {
        return (value & flag) != 0;
    }

    public static boolean hasAnnotation(List<AnnotationNode> annotations, String name) {
        if (annotations == null)
            return false;

        return annotations.stream()
                .anyMatch(a -> a.desc.equals(name));
    }

    public static Optional<AnnotationNode> getAnnotation(List<AnnotationNode> annotations, String name) {
        if (annotations == null)
            return Optional.empty();

        return annotations.stream()
                .filter(a -> a.desc.equals(name))
                .findFirst();
    }

    public static int invertJumpCondition(int opcode) {
        return jumpInversions.get(opcode);
    }

    public static boolean isProperInstruction(AbstractInsnNode instruction) {
        int type = instruction.getType();

        return type != AbstractInsnNode.LABEL && type != AbstractInsnNode.FRAME && type != AbstractInsnNode.LINE;
    }

    public static Optional<Object> readConstantInstruction(AbstractInsnNode instruction) {
        if (instruction == null)
            return Optional.empty();

        if (instruction.getType() == AbstractInsnNode.INSN) {
            Object mapped = specialConstInstructions.get(instruction.getOpcode());
            return Optional.ofNullable(mapped);
        }

        if (instruction.getType() != AbstractInsnNode.LDC_INSN)
            return Optional.empty();

        LdcInsnNode i = (LdcInsnNode) instruction;

        return Optional.ofNullable(i.cst);
    }

    public static Optional<Object> getAnnotationValue(AnnotationNode annotation, String key) {
        for (int i = 0; i < annotation.values.size(); i += 2) {
            if (annotation.values.get(i).equals(key))
                return Optional.ofNullable(annotation.values.get(i + 1));
        }

        return Optional.empty();
    }

    // copy [start, end) and insert behind target
    // returns end label
    public static LabelNode copyRange(InsnList instructions, AbstractInsnNode start, AbstractInsnNode end, AbstractInsnNode target) {
        Map<LabelNode, LabelNode> labelMap = new HashMap<>();
        InsnList copy = new InsnList();

        AbstractInsnNode sourcePointer = start;

        // first copy labels
        while (sourcePointer != end) {
            if (sourcePointer.getType() == AbstractInsnNode.LABEL) {
                LabelNode newLabel = new LabelNode(new Label());
                labelMap.put((LabelNode) sourcePointer, newLabel);
                copy.add(newLabel);
            }

            sourcePointer = sourcePointer.getNext();
        }

        LabelNode endLabel = new LabelNode(new Label());
        copy.add(endLabel);

        sourcePointer = start;
        AbstractInsnNode targetPointer = copy.getFirst();

        // then the rest
        while (sourcePointer != end) {
            if (sourcePointer.getType() == AbstractInsnNode.LABEL) {
                targetPointer = targetPointer.getNext();
            } else if (sourcePointer.getType() == AbstractInsnNode.JUMP_INSN) {
                JumpInsnNode newJump = (JumpInsnNode) sourcePointer.clone(labelMap);

                if (newJump.label == null)
                    newJump.label = ((JumpInsnNode) sourcePointer).label;

                copy.insertBefore(targetPointer, newJump);
            } else {
                copy.insertBefore(targetPointer, sourcePointer.clone(labelMap));
            }

            sourcePointer = sourcePointer.getNext();
        }

        instructions.insert(target, copy);
        return endLabel;
    }
}
