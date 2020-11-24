package urbachyannick.approxflow;

import picocli.CommandLine;
import urbachyannick.approxflow.cnf.IO;
import urbachyannick.approxflow.cnf.MappedProblem;
import urbachyannick.approxflow.cnf.Scope;
import urbachyannick.approxflow.cnf.ScopedMappedProblem;
import urbachyannick.approxflow.codetransformation.*;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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

    @Option(names = {"-c", "--cnf-only"}, description = "print only the cnf")
    private boolean cnfOnly;

    @Option(names = {"--partial-loops"}, description = "passed to jbmc")
    private boolean partialLoops;

    @Option(names = {"--unwind"}, description = "passed to jbmc", paramLabel = "nr", defaultValue = "0")
    private int unwind;

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
        Path cnfFilePath = classpath.resolve(className + ".cnf");

        try {
            IO.write(scopedMappedProblem, cnfFilePath);
        } catch (IOException e) {
            fail("Can not write CNF file", e);
        }

        if (!cnfOnly) {
            double informationFlow = calculateInformationFlow(cnfFilePath);
            System.out.println("Approximated flow is: " + informationFlow);
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
            Transformation.applyMultiple(classFilePath, classFilePath,
                    new MethodOfInterestTransform(),
                    new ReturnValueInput(),
                    new ParameterOutput(),
                    new AssertToAssume(),
                    new AddDummyThrow(),
                    new UnrollLoops()
            );
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
            return IO.readMappedProblem(cnfFilePath);
        } catch (IOException e) {
            fail("Failed to generate CNF");
            throw new Unreachable();
        }
    }

    private IntStream getRelevantVariables(MappedProblem problem) {
        Path classFilePath = classpath.resolve(className + ".class");

        return Stream.of(
                new OutputVariable(),
                new OutputArray()
        ).flatMapToInt(s -> {
            try {
                return s.scan(classFilePath, problem);
            } catch (IOException e) {
                fail("Failed to scan bytecode for outputs");
                throw new Unreachable();
            }
        });
    }

    /**
     * Uses scalmc to calculate the information flow for a given CNF file.
     *
     * @param cnfFilePath the CNF file path
     * @return the information flow
     */
    private double calculateInformationFlow(Path cnfFilePath) {
        try {
            Process process = new ProcessBuilder()
                    .command(Paths.get("util/scalmc").toAbsolutePath().toString(), cnfFilePath.toString())
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .directory(classpath.toFile())
                    .start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;

                while ((line = reader.readLine()) != null) {
                    System.out.println(line);

                    if (line.startsWith("Number of solutions is:")) {
                        Matcher matcher = Pattern.compile("Number of solutions is: (\\d+) x (\\d+)\\^(\\d+)").matcher(line);

                        if (!matcher.find())
                            fail("Failed to parse result of SAT solver");

                        int multiplier = Integer.parseInt(matcher.group(1));
                        int base = Integer.parseInt(matcher.group(2));
                        int exponent = Integer.parseInt(matcher.group(3));
                        double solutions = multiplier * Math.pow(base, exponent);
                        return Math.log(solutions) / Math.log(2);
                    }
                }
            } finally {
                process.waitFor();
            }
        } catch (IOException | InterruptedException e) {
            fail("Failed to run SAT solver", e);
        }

        fail("Failed to parse result of SAT solver");
        throw new Unreachable();
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