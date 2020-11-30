package urbachyannick.approxflow;

import picocli.CommandLine;
import urbachyannick.approxflow.cnf.IO;
import urbachyannick.approxflow.cnf.MappedProblem;
import urbachyannick.approxflow.cnf.Scope;
import urbachyannick.approxflow.cnf.ScopedMappedProblem;
import urbachyannick.approxflow.codetransformation.*;
import urbachyannick.approxflow.codetransformation.Scanner;
import urbachyannick.approxflow.modelcounting.ModelCounter;
import urbachyannick.approxflow.modelcounting.ModelCountingException;
import urbachyannick.approxflow.modelcounting.ScalMC;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static picocli.CommandLine.*;

/**
 * Main class, containing the main method. On execution, an instance of this class is created, and its fields are filled
 * with the command line arguments as parsed by PicoCLI. Then {@link #run()} is called.
 *
 * @author Yannick Urbach
 */
@Command(name = "approxflow", mixinStandardHelpOptions = true)
public class Main implements Runnable {



    // region --- Command line arguments and options ---
    // (parsed and filled in by PicoCLI)

    @Parameters(index = "0", paramLabel = "class", description = "main class with a static ___val variable for output (must be in the default package)")
    private String className;

    @Parameters(index = "1", paramLabel = "classpath", description = "classpath containing the main class")
    private Path classpath;

    @Option(names = {"--write-cnf"}, description = "write the cnf file to disk")
    private boolean writeCnf;

    @Option(names = {"-c", "--cnf-only"}, description = "print only the cnf")
    private boolean cnfOnly;

    @Option(names = {"--partial-loops"}, description = "passed to jbmc")
    private boolean partialLoops;

    @Option(names = {"--unwind"}, description = "passed to jbmc", paramLabel = "nr", defaultValue = "0")
    private int unwind;

    // endregion



    // region --- Configuration ---

    private static final List<Transformation> transformations = new ArrayList<Transformation>(){{
            add(new MethodOfInterestTransform());
            add(new ReturnValueInput());
            add(new ParameterOutput());
            add(new AssertToAssume());
            add(new AddDummyThrow());
            add(new UnrollLoops());
    }};

    private static final List<Scanner<IntStream>> relevantVariableScanners = new ArrayList<Scanner<IntStream>>(){{
            add(new OutputVariable());
            add(new OutputArray());
            add(new ParameterOutputOverApproximated());
    }};

    private static final ModelCounter modelCounter = new ScalMC();

    // endregion



    /**
     * Actual program. Called by PicoCLI.
     */
    @Override
    public void run() {
        classpath = classpath.toAbsolutePath();

        buildClass();
        transformBytecode();

        MappedProblem problem = generateCnf();

        List<Integer> variables = getRelevantVariables(problem)
                .distinct()
                .boxed()
                .collect(Collectors.toList());

        if (variables.isEmpty())
            fail("No variables for which to solve. Information flow might be 0.");

        Scope scope = new Scope(variables.stream().mapToInt(Integer::intValue));
        ScopedMappedProblem scopedMappedProblem = new ScopedMappedProblem(problem, scope);

        if (writeCnf) {
            try {
                IO.write(scopedMappedProblem, classpath.resolve(className + ".cnf"));
            } catch (IOException e) {
                fail("Can not write cnf file to disk");
            }
        }

        if (!cnfOnly) {
            try {
                double solutions = modelCounter.count(scopedMappedProblem);
                double informationFlow = Math.log(solutions) / Math.log(2);
                System.out.println("Approximated flow is: " + informationFlow);
            } catch (ModelCountingException e) {
                fail("Error during model counting", e);
            }
        }
    }

    /**
     * Builds the main class using javac.
     */
    private void buildClass() {
        Path resPath = Paths.get("res").toAbsolutePath();

        runCommand(
                classpath,
                "javac",
                "-classpath", resPath.resolve("jbmc-core-models.jar").toString() + ":" + resPath.toString(),
                "-g",
                "-parameters",
                className + ".java"
        );
    }

    private void transformBytecode() {
        Path classFilePath = classpath.resolve(className + ".class");

        try {
            Transformation.applyMultiple(classFilePath, classFilePath, transformations.stream());
        } catch (IOException | InvalidTransformationException e) {
            fail("Failed to transform bytecode", e);
        }
    }

    /**
     * Generates the CNF file from the compiled class
     *
     * @return the CNF file
     */
    private MappedProblem generateCnf() {
        try {
            Path cnfFilePath = classpath.resolve(className + ".cnf");

            cnfFilePath.toAbsolutePath();

            List<String> command = new ArrayList<>();
            command.add("jbmc");
            command.add(className);
            command.add("--classpath");
            command.add("./:" + Paths.get("res/jbmc-core-models.jar").toAbsolutePath().toString());
            command.add("--dimacs");
            command.add("--outfile");
            command.add(cnfFilePath.toAbsolutePath().toString());

            if (partialLoops)
                command.add("--partial-loops");

            if (unwind > 0) {
                command.add("--unwind");
                command.add(Integer.toString(unwind));
            }

            runCommand(classpath, command);
            MappedProblem problem = IO.readMappedProblem(cnfFilePath);
            Files.delete(cnfFilePath);
            return problem;
        } catch (IOException e) {
            fail("Failed to generate CNF");
            throw new Unreachable();
        }
    }

    private IntStream getRelevantVariables(MappedProblem problem) {
        Path classFilePath = classpath.resolve(className + ".class");

        return relevantVariableScanners.stream().flatMapToInt(s -> {
            try {
                return s.scan(classFilePath, problem);
            } catch (IOException e) {
                fail("Failed to scan bytecode for outputs");
                throw new Unreachable();
            }
        });
    }

    // helper to run a *necessary* command (exits with fail message if not successful)
    private static void runCommand(Path workingDirectory, List<String> command) {
        try {
            Process process = new ProcessBuilder()
                    .command(command)
                    .directory(workingDirectory.toFile())
                    .inheritIO()
                    .start();
            process.waitFor();

            if (process.exitValue() != 0)
                fail(command.stream().findFirst().orElse("command") + " returned error code " + process.exitValue());
        } catch (IOException | InterruptedException e) {
            fail("Failed to run " + command.stream().findFirst().orElse("command"));
        }
    }

    private static void runCommand(Path workingDirectory, String... command) {
        runCommand(workingDirectory, Arrays.asList(command));
    }

    // helper to exit with fail message (never returns)
    private static void fail(String message, Throwable cause) {
        cause.printStackTrace();
        System.err.println("FAIL: " + message);
        System.exit(1);
    }

    private static void fail(String message) {
        System.err.println("FAIL: " + message);
        System.exit(1);
    }

    /**
     * Entry point for the program. Uses PicoCLI to parse arguments and options, then creates an instance of
     * {@link Main} and calls {@link #run()}
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        new CommandLine(new Main()).execute(args);
    }
}