package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.IOException;

public class AssertToAssume extends Transformation {

    @Override
    public void apply(ClassNode sourceClass, ClassNode targetClass) throws IOException, InvalidTransformationException {
        sourceClass.accept(targetClass);

        for (MethodNode m : targetClass.methods)
            applyToMethod(m);
    }

    private void applyToMethod(MethodNode method) {
        AbstractInsnNode aet = method.instructions.getFirst();

        while ((aet = findAssertionErrorThrow(aet)) != null) {
            AbstractInsnNode before = aet.getPrevious();
            method.instructions.remove(aet);

            InsnList assumeFalse = new InsnList();
            assumeFalse.add(new InsnNode(Opcodes.POP));
            assumeFalse.add(new LdcInsnNode(false));
            assumeFalse.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "org/cprover/CProver", "assume", "(Z)V"));
            method.instructions.insert(before, assumeFalse);
        }
    }

    private AbstractInsnNode findAssertionErrorThrow(AbstractInsnNode searchStart) {

        for (AbstractInsnNode current = searchStart; current != null; current = current.getNext()) {
            // java.lang.AssertionError constructor call
            if (!(
                    current.getType() == AbstractInsnNode.METHOD_INSN &&
                    current.getOpcode() == Opcodes.INVOKESPECIAL &&
                    ((MethodInsnNode) current).owner.equals("java/lang/AssertionError") &&
                    ((MethodInsnNode) current).name.equals("<init>")
            )) continue;

            AbstractInsnNode next = findNextTrueInstruction(current.getNext());

            // throw
            if (!(
                    next != null &&
                    next.getOpcode() == Opcodes.ATHROW
            )) continue;

            return next;
        }

        return null;
    }

    private AbstractInsnNode findNextTrueInstruction(AbstractInsnNode searchStart) {
        AbstractInsnNode current = searchStart;

        while (
                current != null && (
                        current.getType() == AbstractInsnNode.FRAME ||
                        current.getType() == AbstractInsnNode.LABEL ||
                        current.getType() == AbstractInsnNode.LINE
                )
        ) {
            current = current.getNext();
        }

        return current;
    }
}
