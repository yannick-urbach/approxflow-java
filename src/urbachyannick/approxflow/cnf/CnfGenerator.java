package urbachyannick.approxflow.cnf;

import org.objectweb.asm.tree.ClassNode;
import urbachyannick.approxflow.IOCallbacks;

import java.util.stream.Stream;

@FunctionalInterface
public interface CnfGenerator {
    MappedProblem generate(Stream<ClassNode> classes, IOCallbacks ioCallbacks) throws CnfException;
}
