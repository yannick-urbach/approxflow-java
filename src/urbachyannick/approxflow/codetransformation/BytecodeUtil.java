package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.Optional;

public class BytecodeUtil {
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
}
