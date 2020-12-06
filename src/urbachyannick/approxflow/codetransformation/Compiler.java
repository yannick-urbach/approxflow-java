package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.tree.ClassNode;
import urbachyannick.approxflow.IOCallbacks;

import java.nio.file.Path;
import java.util.stream.Stream;

public interface Compiler {
    Stream<ClassNode> compile(Path classpath, IOCallbacks debug) throws CompilationError;
}
