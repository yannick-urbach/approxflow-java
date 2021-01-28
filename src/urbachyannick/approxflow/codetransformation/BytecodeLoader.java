package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.tree.ClassNode;
import urbachyannick.approxflow.IOCallbacks;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class BytecodeLoader implements Compiler {
    private final List<Path> ignorePaths;

    public BytecodeLoader(Stream<Path> ignorePaths) {
        this.ignorePaths = ignorePaths.collect(Collectors.toList());
    }

    @Override
    public Stream<ClassNode> compile(Path classpath, IOCallbacks debug) throws CompilationError {
        try {
            List<Path> absoluteIgnorePaths = ignorePaths.stream()
                    .map(classpath::resolve)
                    .collect(Collectors.toList());

            Iterator<Path> i = Files
                    .walk(classpath)
                    .filter(f -> f.toString().endsWith(".class") && absoluteIgnorePaths.stream().noneMatch(f::startsWith))
                    .iterator();

            Stream.Builder<ClassNode> builder = Stream.builder();

            while (i.hasNext())
                builder.accept(IO.read(i.next()));

            return builder.build();
        } catch (IOException e) {
            throw new CompilationError("Failed to load class files");
        }
    }
}
