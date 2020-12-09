package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.stream.Stream;

import static urbachyannick.approxflow.codetransformation.BytecodeUtil.findMainMethod;

/**
 * Adds an assert(false) before every return statement in the main method to force jbmc to output a cnf file.
 * For some reason this doesn't seem to work if externalized into a method.
 */
public class AddDummyThrow implements Transformation {
    public static InsnList dummyThrow() {
        return new InsnList() {{
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
    }

    @Override
    public Stream<ClassNode> apply(Stream<ClassNode> sourceClasses) {
        return sourceClasses.map(sourceClass -> {
            ClassNode targetClass = new ClassNode(Opcodes.ASM5);
            sourceClass.accept(targetClass);
            findMainMethod(targetClass).ifPresent(this::addDummyThrow);
            return targetClass;
        });
    }

    private void addDummyThrow(MethodNode method) {
        InsnList body = method.instructions;

        for (AbstractInsnNode n = body.getFirst(); n != null; n = n.getNext()) {
            if (n.getOpcode() == Opcodes.RETURN) {
                method.instructions.insertBefore(n, dummyThrow());
            }
        }
    }
}
