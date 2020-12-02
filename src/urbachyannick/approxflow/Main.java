package urbachyannick.approxflow;

import com.sun.org.apache.xpath.internal.operations.Bool;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
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

import javax.xml.parsers.*;
import javax.xml.xpath.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static picocli.CommandLine.*;
import static urbachyannick.approxflow.MiscUtil.throwableToString;

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

    @ArgGroup(exclusive = true, multiplicity = "1")
    OperationMode operationMode;
    static class OperationMode {
        @ArgGroup(exclusive = false, multiplicity = "1")
        Regular regular;
        static class Regular {
            @Parameters(index = "0", paramLabel = "class", description = "main class with a static ___val variable for output (must be in the default package)")
            private String className;

            @Parameters(index = "1", paramLabel = "classpath", description = "classpath containing the main class")
            private Path classpath;
        }

        @Option(names = {"--tests"}, description = "run tests")
        private boolean test;
    }

    /*
    @Parameters(index = "0", paramLabel = "class", description = "main class with a static ___val variable for output (must be in the default package)")
    private String className;

    @Parameters(index = "1", paramLabel = "classpath", description = "classpath containing the main class")
    private Path classpath;
    */
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
            add(new InlineMethods());
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
        if (operationMode.test) {
            runTests();
            return;
        }

        try {
            double informationFlow = analyzeInformationFlow(operationMode.regular.className, operationMode.regular.classpath);

            if (!cnfOnly)
                System.out.println("Approximated flow is: " + informationFlow);
        } catch(Fail f) {
            fail(f);
        }


    }

    public double analyzeInformationFlow(String className, Path classpath) {
        classpath = classpath.toAbsolutePath();

        buildClass(className, classpath);
        transformBytecode(className, classpath);

        MappedProblem problem = generateCnf(className, classpath);

        if (writeCnf) {
            try {
                IO.write(problem, classpath.resolve(className + ".cnf"));
            } catch (IOException e) {
                throw new Fail("Can not write cnf file to disk");
            }
        }

        List<Integer> variables = getRelevantVariables(className, classpath, problem)
                .distinct()
                .boxed()
                .collect(Collectors.toList());

        if (variables.isEmpty())
            throw new Fail("No variables for which to solve. Information flow might be 0.");

        Scope scope = new Scope(variables.stream().mapToInt(Integer::intValue));
        ScopedMappedProblem scopedMappedProblem = new ScopedMappedProblem(problem, scope);

        if (writeCnf) {
            try {
                IO.write(scopedMappedProblem, classpath.resolve(className + ".cnf"));
            } catch (IOException e) {
                throw new Fail("Can not write cnf file to disk");
            }
        }

        if (!cnfOnly) {
            try {
                double solutions = modelCounter.count(scopedMappedProblem);
                return Math.log(solutions) / Math.log(2);
            } catch (ModelCountingException e) {
                throw new Fail("Error during model counting", e);
            }
        } else {
            return -1;
        }
    }

    private static class TestResult {
        public boolean skipped;
        public boolean success;
        public String message;
        public Path testPath;
    }

    private void runTests() {
        try {
            Path testRoot = Paths.get("test").toAbsolutePath();

            Files
                    .list(testRoot)
                    .filter(t ->
                            Files.isDirectory(t) &&
                            Files.exists(t.resolve("Program.java")) &&
                            Files.exists(t.resolve("config.xml"))
                    )
                    .map(this::runTest)
                    .collect(Collectors.toList())
                    .forEach(r -> {
                        if (r.skipped)
                            System.out.println("SKIPPED: " + r.testPath.getFileName());
                        else {
                            writeTestResult(r);
                            System.out.println((r.success ? "SUCCESS: " : "FAIL:    ") + r.testPath.getFileName());
                        }
                    });

        } catch (IOException e) {
            fail("Can not list test directories");
        }

    }

    private TestResult runTest(Path testDirectory) {
        Path configFile = testDirectory.resolve("config.xml");

        double minFlow;
        double maxFlow;

        try {
            Document document = DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(configFile.toFile());

            XPath xPath = XPathFactory.newInstance().newXPath();

            String skipText = xPath.evaluate("/test/skip", document);
            if (skipText != null && skipText.trim().equals("true")) {
                return new TestResult() {{
                    skipped = true;
                    testPath = testDirectory;
                }};
            }

            minFlow = Double.parseDouble(xPath.evaluate("/test/minflow", document));
            maxFlow = Double.parseDouble(xPath.evaluate("/test/maxflow", document));

        } catch (ParserConfigurationException | java.io.IOException | SAXException | XPathExpressionException e) {
            return new TestResult() {{
                testPath = testDirectory;
                success = false;
                message = "Can not read config\n" + e.toString();
            }};
        }

        try {
            double informationFlow = analyzeInformationFlow("Program", testDirectory);

            if (informationFlow < minFlow - 0.2) {
                return new TestResult() {{
                    testPath = testDirectory;
                    success = false;
                    message = "flow too low: " + informationFlow + " (should be >= " + minFlow + ")";
                }};
            } else if (informationFlow > maxFlow + 0.2) {
                return new TestResult() {{
                    testPath = testDirectory;
                    success = false;
                    message = "flow too high: " + informationFlow + " (should be <= " + minFlow + ")";
                }};
            } else {
                return new TestResult() {{
                    testPath = testDirectory;
                    success = true;
                    message = "flow within bounds: " + informationFlow;
                }};
            }

        } catch (Exception e) {
            return new TestResult() {{
                testPath = testDirectory;
                success = false;
                message = "Exception during analysis\n" + throwableToString(e);
            }};
        }
    }

    private void writeTestResult(TestResult result) {
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(result.testPath.resolve("result.txt"))) {
                FilesUtil.writeLines(writer, Stream.of(result.success ? "SUCCESS" : "FAIL", result.message));
            }
        } catch (IOException e) {
            fail("Can not write test result");
        }
    }

    /**
     * Builds the main class using javac.
     */
    private void buildClass(String className, Path classpath) {
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

    private void transformBytecode(String className, Path classpath) {
        Path classFilePath = classpath.resolve(className + ".class");

        try {
            Transformation.applyMultiple(classFilePath, classFilePath, transformations.stream());
        } catch (IOException | InvalidTransformationException e) {
            throw new Fail("Failed to transform bytecode", e);
        }
    }

    /**
     * Generates the CNF file from the compiled class
     *
     * @return the CNF file
     */
    private MappedProblem generateCnf(String className, Path classpath) {
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
            throw new Fail("Failed to generate CNF", e);
        }
    }

    private IntStream getRelevantVariables(String className, Path classpath, MappedProblem problem) {
        Path classFilePath = classpath.resolve(className + ".class");

        return relevantVariableScanners.stream().flatMapToInt(s -> {
            try {
                return s.scan(classFilePath, problem);
            } catch (IOException e) {
                throw new Fail("Failed to scan bytecode for outputs", e);
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
                throw new Fail(command.stream().findFirst().orElse("command") + " returned error code " + process.exitValue());
        } catch (IOException | InterruptedException e) {
            throw new Fail("Failed to run " + command.stream().findFirst().orElse("command"));
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

    private static void fail(Fail f) {
        if (f.getCause() != null)
            f.getCause().printStackTrace();
        System.err.println(f.getMessage());
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