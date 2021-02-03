package urbachyannick.approxflow.blackboxes;

import org.objectweb.asm.tree.*;
import urbachyannick.approxflow.codetransformation.RuntimeInvalidTransformationException;

import java.util.List;
import java.util.stream.Stream;

import static urbachyannick.approxflow.codetransformation.BytecodeUtil.*;

public class BlackboxCall implements FlowSource, FlowSink {
    private final int indexInMainMethod;

    public BlackboxCall(int indexInMainMethod) {
        this.indexInMainMethod = indexInMainMethod;
    }

    public int getIndexInMainMethod() {
        return indexInMainMethod;
    }

    public BlackboxCallInfo getInfo(List<ClassNode> classes) {
        return new BlackboxCallInfo(indexInMainMethod, classes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlackboxCall)) return false;
        BlackboxCall other = (BlackboxCall) o;
        return indexInMainMethod == other.indexInMainMethod;
    }

    @Override
    public int hashCode() {
        return indexInMainMethod;
    }

    @Override
    public String toString() {
        return "b" + indexInMainMethod;
    }
}
