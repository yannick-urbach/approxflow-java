package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.util.*;

public class UnrollLoops extends Transformation {

    @Override
    public void apply(ClassNode sourceClass, ClassNode targetClass) throws IOException, InvalidTransformationException {
        sourceClass.accept(targetClass);

        for (MethodNode m : targetClass.methods)
            applyToInstructions(m.instructions);
    }

    private static boolean isCall(AbstractInsnNode instruction) {
        if (instruction == null || instruction.getType() != AbstractInsnNode.METHOD_INSN)
            return false;

        MethodInsnNode i = (MethodInsnNode) instruction;
        return i.owner.equals("urbachyannick/approxflow/Loops") && i.name.equals("unroll");
    }

    private void applyToInstructions(InsnList instructions) {
        Map<LabelNode, Integer> labels = new HashMap<>();

        int count = -1;

        for (AbstractInsnNode instruction : instructions) {
            Optional<Object> constant = BytecodeUtil.readConstantInstruction(instruction);

            if (constant.isPresent() && constant.get() instanceof Integer && isCall(instruction.getNext()))
                count = (Integer) constant.get();

            if (instruction.getType() == AbstractInsnNode.LABEL && count > 0)
                labels.put((LabelNode) instruction, count);

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
