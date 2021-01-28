package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.tree.ClassNode;
import urbachyannick.approxflow.IOCallbacks;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class Kotlinc implements Compiler {
    @Override
    public Stream<ClassNode> compile(Path classpath, IOCallbacks ioCallbacks) throws CompilationError {
        Path coreModelsPath = ioCallbacks.findInProgramDirectory(Paths.get("res/jbmc-core-models.jar"));
        Path jarPath = ioCallbacks.getJarPath();

        List<String> command = new ArrayList<>();

        Path targetDir = null;
        try {
            targetDir = ioCallbacks.createTemporaryDirectory("kotlinc-output");
        } catch (IOException e) {
            throw new CompilationError("Failed create output directory", e);
        }

        try {
            command.add("kotlinc");
            command.add("-jvm-target");
            command.add("1.8");
            command.add("-classpath");
            command.add(coreModelsPath + ":" + jarPath);
            command.add("-d");
            command.add(targetDir.toString());

            List<Path> sourceFiles = Files
                    .walk(classpath)
                    .filter(p -> p.toString().endsWith(".kt"))
                    .collect(Collectors.toList());

            if (sourceFiles.size() == 0)
                return Stream.empty();

            sourceFiles.stream()
                    .map(Path::toString)
                    .forEach(command::add);

        } catch (IOException e) {
            throw new CompilationError("Failed to list source files", e);
        }

        try {
            ProcessBuilder.Redirect out = ProcessBuilder.Redirect.to(ioCallbacks.createTemporaryFile("kotlinc-log.txt").toFile());

            Process process = new ProcessBuilder()
                    .command(command)
                    .redirectOutput(out)
                    .redirectError(out)
                    .start();
            process.waitFor();

            if (process.exitValue() != 0)
                throw new CompilationError("kotlinc returned error code " + process.exitValue());
        } catch (IOException | InterruptedException e) {
            throw new CompilationError("Failed to run kotlinc", e);
        }

        try {
            return IO.readAll(targetDir);
        } catch (IOException e) {
            throw new CompilationError("Failed to run kotlinc");
        }
    }
}
