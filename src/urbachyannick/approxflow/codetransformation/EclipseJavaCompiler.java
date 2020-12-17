package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.tree.ClassNode;
import urbachyannick.approxflow.IOCallbacks;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class EclipseJavaCompiler implements Compiler {

    @Override
    public Stream<ClassNode> compile(Path classpath, IOCallbacks ioCallbacks) throws CompilationError {
        Path compilerPath = Paths.get("util", "ecj-4.18.jar").toAbsolutePath();
        Path resPath = Paths.get("res").toAbsolutePath();
        List<String> command = new ArrayList<>();

        Path targetDir = null;
        try {
            targetDir = ioCallbacks.createTemporaryDirectory("ecj-output");
        } catch (IOException e) {
            throw new CompilationError("Failed to create output directory", e);
        }

        try {
            command.add("java");
            command.add("-jar");
            command.add(compilerPath.toString());
            command.add("-1.8");
            command.add("-classpath");
            command.add(resPath.resolve("jbmc-core-models.jar").toString() + ":" + resPath.toString());
            command.add("-g");
            command.add("-parameters");
            command.add("-d");
            command.add(targetDir.toString());

            Files
                    .walk(classpath)
                    .filter(p -> p.toString().endsWith(".java"))
                    .map(Path::toString)
                    .forEach(command::add);

        } catch (IOException e) {
            throw new CompilationError("Failed to list source files", e);
        }

        try {
            ProcessBuilder.Redirect out = ProcessBuilder.Redirect.to(ioCallbacks.createTemporaryFile("ecj-log.txt").toFile());

            Process process = new ProcessBuilder()
                    .command(command)
                    .redirectOutput(out)
                    .redirectError(out)
                    .start();
            process.waitFor();

            if (process.exitValue() != 0)
                throw new CompilationError("ecj returned error code " + process.exitValue());
        } catch (IOException | InterruptedException e) {
            throw new CompilationError("Failed to run ecj", e);
        }

        try {
            return IO.readAll(targetDir);
        } catch (IOException e) {
            throw new CompilationError("Failed to run ecj");
        }
    }
}
