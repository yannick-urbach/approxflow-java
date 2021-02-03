package urbachyannick.approxflow.blackboxes;

import org.objectweb.asm.tree.*;
import urbachyannick.approxflow.codetransformation.RuntimeInvalidTransformationException;

import java.util.List;

import static urbachyannick.approxflow.codetransformation.BytecodeUtil.*;

public class BlackboxCallInfo {
    private final int indexInMainMethod;
    private final ClassNode calledMethodOwner;
    private final MethodNode calledMethod;
    private final ClassNode callingMethodOwner;
    private final MethodNode callingMethod;
    private final MethodInsnNode call;

    public BlackboxCallInfo(int indexInMainMethod, List<ClassNode> classes) {
        this.indexInMainMethod = indexInMainMethod;

        callingMethodOwner = findClassWithMainMethod(classes.stream())
                .orElseThrow(() -> new RuntimeInvalidTransformationException("No main method found"));

        callingMethod = findMainMethod(callingMethodOwner)
                .orElseThrow(() -> new RuntimeInvalidTransformationException("No main method found"));

        call = (MethodInsnNode) callingMethod.instructions.get(indexInMainMethod);

        calledMethodOwner = findClass(classes.stream(), call.owner)
                .orElseThrow(() -> new RuntimeInvalidTransformationException("Class " + call.owner + " not found"));

        calledMethod = findMethod(calledMethodOwner, call.owner, call.name, call.desc)
                .orElseThrow(() -> new RuntimeInvalidTransformationException("Method " + call.name + " not found"));
    }

    public int getIndexInMainMethod() {
        return indexInMainMethod;
    }

    public ClassNode getCalledMethodOwner() {
        return calledMethodOwner;
    }

    public MethodNode getCalledMethod() {
        return calledMethod;
    }

    public ClassNode getCallingMethodOwner() {
        return callingMethodOwner;
    }

    public MethodNode getCallingMethod() {
        return callingMethod;
    }

    public MethodInsnNode getCall() {
        return call;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlackboxCallInfo)) return false;
        BlackboxCallInfo other = (BlackboxCallInfo) o;
        return indexInMainMethod == other.indexInMainMethod;
    }

    @Override
    public int hashCode() {
        return indexInMainMethod;
    }
}
