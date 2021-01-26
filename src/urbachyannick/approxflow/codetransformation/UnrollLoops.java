package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.*;

import static urbachyannick.approxflow.MiscUtil.or;
import static urbachyannick.approxflow.codetransformation.BytecodeUtil.*;

public class UnrollLoops extends Transformation.PerClass {

    private final Integer defaultIterations;

    public UnrollLoops(Integer defaultIterations) {
        this.defaultIterations = defaultIterations;
    }

    @Override
    protected ClassNode applyToClass(ClassNode sourceClass) throws InvalidTransformationException {
        ClassNode targetClass = new ClassNode(Opcodes.ASM5);

        sourceClass.accept(targetClass);

        Optional<AnnotationNode> classBlackbox = getAnnotation(sourceClass.visibleAnnotations, "Lurbachyannick/approxflow/BlackboxLoops;");
        Optional<AnnotationNode> classUnroll = getAnnotation(sourceClass.visibleAnnotations, "Lurbachyannick/approxflow/Unroll;");
        Optional<Object> classIterations = classUnroll.flatMap(a -> getAnnotationValue(a, "iterations"));

        if (classBlackbox.isPresent() && classUnroll.isPresent())
            throw new InvalidTransformationException("Can not have both Unroll and BlackboxLoops");

        for (MethodNode m : targetClass.methods) {
            Optional<AnnotationNode> methodBlackbox = getAnnotation(m.visibleAnnotations, "Lurbachyannick/approxflow/BlackboxLoops;");
            Optional<AnnotationNode> methodUnroll = getAnnotation(m.visibleAnnotations, "Lurbachyannick/approxflow/Unroll;");
            Optional<Object> methodIterations = methodUnroll.flatMap(a -> getAnnotationValue(a, "iterations"));

            if (methodBlackbox.isPresent() && methodUnroll.isPresent())
                throw new InvalidTransformationException("Can not have both Unroll and BlackboxLoops");

            if (methodIterations.isPresent())
                applyToInstructions(m.instructions, methodIterations.map(i -> (int) i));
            else if (classIterations.isPresent() && !methodBlackbox.isPresent())
                applyToInstructions(m.instructions, classIterations.map(i -> (int) i));
            else if (!methodBlackbox.isPresent() && !classBlackbox.isPresent())
                applyToInstructions(m.instructions, Optional.ofNullable(defaultIterations));
            else
                applyToInstructions(m.instructions, Optional.empty());
        }

        return targetClass;
    }

    private static boolean isCall(AbstractInsnNode instruction) {
        if (instruction == null || instruction.getType() != AbstractInsnNode.METHOD_INSN)
            return false;

        MethodInsnNode i = (MethodInsnNode) instruction;
        return i.owner.equals("urbachyannick/approxflow/Loops") && i.name.equals("unroll");
    }

    private void applyToInstructions(InsnList instructions, Optional<Integer> methodIterations) {
        Map<LabelNode, Integer> labels = new HashMap<>();

        Optional<Integer> count = methodIterations;

        for (AbstractInsnNode instruction : instructions) {
            Optional<Object> constant = BytecodeUtil.readConstantInstruction(instruction);

            if (constant.isPresent() && constant.get() instanceof Integer && isCall(instruction.getNext()))
                count = Optional.of((Integer) constant.get());

            if (instruction.getType() == AbstractInsnNode.LABEL && count.isPresent())
                labels.put((LabelNode) instruction, count.get());

            if (instruction.getType() == AbstractInsnNode.JUMP_INSN && labels.containsKey(((JumpInsnNode) instruction).label)) {
                JumpInsnNode jump = (JumpInsnNode) instruction;
                int jumpOpcode = jump.getOpcode();
                boolean emitJump = jumpOpcode != Opcodes.GOTO;
                int invertedJumpOpcode = emitJump ? BytecodeUtil.invertJumpCondition(jumpOpcode) : -1;
                int unrollCount = labels.get(jump.label);

                AbstractInsnNode start = jump.label;
                AbstractInsnNode end = jump;
                AbstractInsnNode target = jump;

                for (int i = 1; i < unrollCount; ++i) {
                    LabelNode endLabel = BytecodeUtil.copyRange(instructions, start, end, target);

                    if (emitJump)
                        instructions.insert(target, new JumpInsnNode(invertedJumpOpcode, endLabel));

                    target = endLabel;
                }

                instructions.remove(jump);
            }
        }
    }
}
