package urbachyannick.approxflow;

import org.objectweb.asm.tree.ClassNode;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import picocli.CommandLine;
import urbachyannick.approxflow.cnf.Jbmc;
import urbachyannick.approxflow.codetransformation.Compiler;
import urbachyannick.approxflow.codetransformation.*;
import urbachyannick.approxflow.informationflow.*;
import urbachyannick.approxflow.modelcounting.ScalMC;

import javax.xml.parsers.*;
import javax.xml.xpath.*;
import java.io.*;
import java.nio.file.*;
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
        @Parameters(index = "0", paramLabel = "classpath", description = "classpath containing the source files")
        private Path classpath;

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

    @Option(names = {"--keep-intermediate"}, description = "keep intermediate results")
    private boolean keepIntermediate;

    @Option(names = {"-c", "--cnf-only"}, description = "print only the cnf")
    private boolean cnfOnly;

    @Option(names = {"--partial-loops"}, description = "passed to jbmc")
    private boolean partialLoops;

    @Option(names = {"--unwind"}, description = "passed to jbmc", paramLabel = "nr", defaultValue = "0")
    private int unwind;

    @Option(names = {"--eclipse"}, description = "use eclipse java compiler instead of javac")
    private boolean eclipse;

    // endregion




    /**
     * Actual program. Called by PicoCLI.
     */
    @Override
    public void run() {
        Compiler compiler = eclipse ? new EclipseJavaCompiler() : new Javac();

        FlowAnalyzer analyzer = new DefaultAnalyzer(
                new Jbmc(partialLoops, unwind),
                Stream.of(
                        new MethodOfInterestTransform(),
                        new ReturnValueInput(),
                        new ParameterOutput(),
                        new AssertToAssume(),
                        new AddDummyThrow(),
                        new UnrollLoops(),
                        new InlineMethods()
                ),
                Stream.of(
                        new OutputVariable(),
                        new OutputArray(),
                        new ParameterOutputOverApproximated()
                ),
                new ScalMC()
        );

        if (operationMode.test)
            runTests(compiler, analyzer);
        else
            runRegular(compiler, analyzer);
    }


    public void runRegular(Compiler compiler, FlowAnalyzer analyzer) {
        IOCallbacks ioCallbacks = new IOCallbacks() {
            @Override
            public Path createTemporaryFileImpl(String name) {
                return operationMode.classpath.resolve(Paths.get("intermediate", name));
            }

            @Override
            public Path createTemporaryDirectoryImpl(String name) {
                return operationMode.classpath.resolve(Paths.get("intermediate", name));
            }

            @Override
            protected boolean shouldDeleteTemporary(String name) {
                return !keepIntermediate;
            }
        };

        try (IOCallbacks c = ioCallbacks) {
            Stream<ClassNode> classes = compiler.compile(operationMode.classpath, c);
            double informationFlow = analyzer.analyzeInformationFlow(classes, c);

            if (!cnfOnly)
                System.out.println("Approximated flow is: " + informationFlow);
        } catch(Fail f) {
            fail(f);
        } catch (CompilationError e) {
            fail("Failed to compile source files", e);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Could not delete temporary files and/or directories");
        }
    }



    private static class TestResult {
        public boolean skipped;
        public boolean success;
        public String message;
        public Path testPath;
    }

    private void runTests(Compiler compiler, FlowAnalyzer analyzer) {
        try {
            Path testRoot = Paths.get("test").toAbsolutePath();

            Files
                    .list(testRoot)
                    .filter(t ->
                            Files.isDirectory(t) &&
                            Files.exists(t.resolve("config.xml"))
                    )
                    .map(t -> runTest(t, compiler, analyzer))
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

    private TestResult runTest(Path testDirectory, Compiler compiler, FlowAnalyzer analyzer) {
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

        IOCallbacks ioCallbacks = new IOCallbacks() {
            @Override
            protected Path createTemporaryFileImpl(String name) {
                return testDirectory.resolve(Paths.get("intermediate", name));
            }

            @Override
            protected Path createTemporaryDirectoryImpl(String name) {
                return testDirectory.resolve(Paths.get("intermediate", name));
            }

            @Override
            protected boolean shouldDeleteTemporary(String name) {
                return !keepIntermediate;
            }
        };

        try (IOCallbacks c = ioCallbacks) {
            Stream<ClassNode> classes = compiler.compile(testDirectory, c);
            double informationFlow = analyzer.analyzeInformationFlow(classes, c);

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