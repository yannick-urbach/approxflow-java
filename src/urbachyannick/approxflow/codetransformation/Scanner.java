package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.tree.ClassNode;
import urbachyannick.approxflow.cnf.MappedProblem;

import java.util.stream.Stream;

public interface Scanner<T> {
    T scan(Stream<ClassNode> sourceClass, MappedProblem problem);
}
