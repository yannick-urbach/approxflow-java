package urbachyannick.approxflow.cnf;

import org.objectweb.asm.tree.ClassNode;
import urbachyannick.approxflow.IOCallbacks;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import static urbachyannick.approxflow.codetransformation.BytecodeUtil.findClassWithMainMethod;

public class Jbmc implements CnfGenerator {

    public Jbmc(boolean partialLoops, int unwind) {
        this.partialLoops = partialLoops;
        this.unwind = unwind;
    }

    private final boolean partialLoops;
    private final int unwind;

    @Override
    public MappedProblem generate(Stream<ClassNode> classes, IOCallbacks ioCallbacks) throws CnfException {
        List<ClassNode> classesList = classes.collect(Collectors.toList());

        Path classesPath;
        Path cnfFilePath;

        try {
            classesPath = ioCallbacks.createTemporaryDirectory("jbmc-input");
            cnfFilePath = ioCallbacks.createTemporaryFile("jbmc-output.cnf");
            urbachyannick.approxflow.codetransformation.IO.writeAll(classesList.stream(), classesPath);
        } catch (IOException e) {
            throw new CnfException("Could not write classes to disk", e);
        }

        Optional<ClassNode> classWithMainMethod = findClassWithMainMethod(classesList.stream());

        if (!classWithMainMethod.isPresent())
            throw new CnfException("Missing main method");

        List<String> command = new ArrayList<String>() {{
            add("jbmc");
            add(classWithMainMethod.get().name);
            add("--classpath");
            add(classesPath.toAbsolutePath().toString() + ":" + Paths.get("res/jbmc-core-models.jar").toAbsolutePath().toString());
            add("--dimacs");
            add("--outfile");
            add(cnfFilePath.toAbsolutePath().toString());

            if (partialLoops)
                add("--partial-loops");

            if (unwind > 0) {
                add("--unwind");
                add(Integer.toString(unwind));
            }
        }};

        try {
            ProcessBuilder.Redirect out = ProcessBuilder.Redirect.to(ioCallbacks.createTemporaryFile("jbmc-log.txt").toFile());

            Process process = new ProcessBuilder()
                    .command(command)
                    .redirectOutput(out)
                    .redirectError(out)
                    .start();
            process.waitFor();

            if (process.exitValue() != 0)
                throw new CnfException("JBMC returned error code " + process.exitValue());
        } catch (IOException | InterruptedException e) {
            throw new CnfException("Failed to run JBMC");
        }

        try {
            MappedProblem problem = IO.readMappedProblem(cnfFilePath);
            return problem;
        } catch (IOException e) {
            throw new CnfException("Failed to read CNF file");
        }
    }
}
