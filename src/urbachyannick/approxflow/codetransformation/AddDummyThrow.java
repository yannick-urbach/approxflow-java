package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.IOException;

/**
 * Adds an assert(false) before every return statement in the main method to force jbmc to output a cnf file.
 * For some reason this doesn't seem to work if externalized into a method.
 */
public class AddDummyThrow extends Transformation {
    private static final InsnList dummyThrow = new InsnList() {{
        add(new TypeInsnNode(Opcodes.NEW, "Ljava/lang/AssertionError;")); // allocate error
        add(new InsnNode(Opcodes.DUP)); // duplicate reference to feed one into constructor
        add(new MethodInsnNode( // call constructor
                Opcodes.INVOKESPECIAL,
                "Ljava/lang/AssertionError;",
                "<init>",
                "()V"
        ));
        add(new InsnNode(Opcodes.ATHROW)); // throw error
    }};

    @Override
    public void apply(ClassNode sourceClass, ClassNode targetClass) throws IOException, InvalidTransformationException {
        sourceClass.accept(targetClass); // copy over

        targetClass.methods.stream()
                .filter(m -> m.name.equals("main") && m.desc.equals("([Ljava/lang/String;)V"))
                .forEach(this::addDummyThrow);
    }

    private void addDummyThrow(MethodNode method) {
        InsnList body = method.instructions;

        for (AbstractInsnNode n = body.getFirst(); n != body.getLast(); n = n.getNext()) {
            if (n.getOpcode() == Opcodes.RETURN) {
                method.instructions.insertBefore(n, new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "urbachyannick.approxflow.Util",
                        "dummyThrow",
                        "()V"
                ));

                method.instructions.insertBefore(n, dummyThrow);
            }
        }
    }
}
