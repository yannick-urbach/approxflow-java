package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.tree.ClassNode;
import urbachyannick.approxflow.IOCallbacks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class Javac implements Compiler {

    @Override
    public Stream<ClassNode> compile(Path classpath, IOCallbacks ioCallbacks) throws CompilationError {
        Path resPath = Paths.get("res").toAbsolutePath();
        List<String> command = new ArrayList<>();

        Path targetDir = null;
        try {
            targetDir = ioCallbacks.createTemporaryDirectory("javac-output");
        } catch (IOException e) {
            throw new CompilationError("Failed create output directory", e);
        }

        try {
            command.add("javac");
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
            ProcessBuilder.Redirect out = ProcessBuilder.Redirect.to(ioCallbacks.createTemporaryFile("javac-log.txt").toFile());

            Process process = new ProcessBuilder()
                    .command(command)
                    .redirectOutput(out)
                    .redirectError(out)
                    .start();
            process.waitFor();

            if (process.exitValue() != 0)
                throw new CompilationError("javac returned error code " + process.exitValue());
        } catch (IOException | InterruptedException e) {
            throw new CompilationError("Failed to run javac", e);
        }

        try {
            return IO.readAll(targetDir);
        } catch (IOException e) {
            throw new CompilationError("Failed to run javac");
        }
    }
}
