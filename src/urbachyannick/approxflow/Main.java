package urbachyannick.approxflow;

import org.objectweb.asm.tree.ClassNode;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import picocli.CommandLine;
import urbachyannick.approxflow.cnf.Jbmc;
import urbachyannick.approxflow.codetransformation.Compiler;
import urbachyannick.approxflow.codetransformation.*;
import urbachyannick.approxflow.informationflow.*;
import urbachyannick.approxflow.modelcounting.*;
import urbachyannick.approxflow.soot.AsmSootConverter;

import javax.xml.parsers.*;
import javax.xml.xpath.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

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

        @Option(names = {"--tests"}, description = "run tests", paramLabel = "testroot", arity = "0..1", defaultValue = "___not_test_mode___", fallbackValue = "test")
        private Path testroot;
    }

    @Option(names = {"--keep-intermediate"}, description = "keep intermediate results")
    private boolean keepIntermediate;

    @Option(names = {"--jbmc-partial-loops"}, description = "passed to jbmc")
    private boolean partialLoops;

    @Option(names = {"--jbmc-unwind"}, description = "passed to jbmc", paramLabel = "nr", defaultValue = "0")
    private int unwind;

    @Option(names = {"--eclipse"}, description = "use eclipse java compiler instead of javac")
    private boolean eclipse;

    @Option(names = {"--maxcount-k"}, description = "passed to maxcount", paramLabel = "k", defaultValue = "1")
    private int maxcountK;

    @Option(names = {"--program-dir"}, description = "root directory of approxflow-java; filled in by launcher script", paramLabel = "path", defaultValue = "./")
    private Path programRoot;

    @Option(names = {"--inline"}, description = "inline loops with maximum recursion depth nr by default", paramLabel = "nr")
    private int defaultInlineRecursionDepth;

    @ArgGroup(exclusive = true, multiplicity = "0..1")
    private LoopHandling loopHandling;
    private static class LoopHandling {
        @Option(names = {"--loops-unroll"}, description = "unroll loops with nr iterations by default", paramLabel = "nr", defaultValue = "10")
        public int defaultUnrollIterations;

        @Option(names = {"--loops-blackbox"}, description = "treat loops as blackboxes by default (not recommended)")
        public boolean defaultBlackboxLoops;
    }

    // endregion




    /**
     * Actual program. Called by PicoCLI.
     */
    @Override
    public void run() {
        Stream<Compiler> compilers = Stream.of(
                new Kotlinc(),
                eclipse ? new EclipseJavaCompiler() : new Javac()
        );

        if (loopHandling == null) {
            loopHandling = new LoopHandling();
            loopHandling.defaultBlackboxLoops = false;
            loopHandling.defaultUnrollIterations = 10;
        }

        FlowAnalyzer analyzer = new BlackboxSplitter(
                new Jbmc(partialLoops, unwind),
                new CounterPicker(
                        new MaxCount(maxcountK),
                        new ApproxMC()
                ),
                defaultInlineRecursionDepth,
                loopHandling.defaultUnrollIterations,
                loopHandling.defaultBlackboxLoops
        );

        if (!operationMode.testroot.equals(Paths.get("___not_test_mode___")))
            runTests(compilers, analyzer, operationMode.testroot);
        else
            runRegular(compilers, analyzer);
    }


    public void runRegular(Stream<Compiler> compilers, FlowAnalyzer analyzer) {
        IOCallbacks ioCallbacks = new IOCallbacks(programRoot) {
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

        AsmSootConverter.initSoot(ioCallbacks);

        try (IOCallbacks c = ioCallbacks) {
            Iterator<Compiler> i = compilers.iterator();
            Stream.Builder<ClassNode> classesBuilder = Stream.builder();

            while (i.hasNext())
                i.next().compile(operationMode.classpath, c).forEach(classesBuilder);

            double informationFlow = analyzer.analyzeInformationFlow(classesBuilder.build(), c);
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
        public long compileTime = -1; // nanoseconds
        public long analyzeTime = -1; // nanoseconds
    }

    private void runTests(Stream<Compiler> compilers, FlowAnalyzer analyzer, Path testRootArg) {
        List<Compiler> compilerList = compilers.collect(Collectors.toList());

        try {
            Path testRoot = testRootArg.toAbsolutePath();

            if (!Files.exists(testRoot))
                testRoot = programRoot.resolve(testRootArg).toAbsolutePath();

            if (!Files.exists(testRoot))
                fail("test root " + testRootArg.toString() + " not found");

            Files
                    .walk(testRoot)
                    .filter(t ->
                            Files.isDirectory(t) &&
                            Files.exists(t.resolve("config.xml"))
                    )
                    .map(t -> runTest(t, compilerList.stream(), analyzer))
                    .forEach(r -> {
                        if (r.skipped)
                            System.out.println("SKIPPED: " + r.testPath.getFileName());
                        else {
                            writeTestResult(r);
                            System.out.println(
                                    (r.success ? "SUCCESS: " : "FAIL:    ") +
                                    r.testPath.getFileName() +
                                    (r.compileTime != -1 && r.analyzeTime != -1 ? " (" + ((r.compileTime + r.analyzeTime) / 1000000 + "ms)") : "")
                            );
                        }
                    });

        } catch (IOException e) {
            fail("Can not list test directories");
        }

    }

    private TestResult runTest(Path testDirectory, Stream<Compiler> compilers, FlowAnalyzer analyzer) {
        Path configFile = testDirectory.resolve("config.xml");

        double minFlow;
        double maxFlow;

        TestResult result = new TestResult();
        result.testPath = testDirectory;

        try {
            Document document = DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(configFile.toFile());

            XPath xPath = XPathFactory.newInstance().newXPath();

            String skipText = xPath.evaluate("/test/skip", document);
            if (skipText != null && skipText.trim().equals("true")) {
                result.skipped = true;
                return result;
            }

            minFlow = Double.parseDouble(xPath.evaluate("/test/minflow", document));
            maxFlow = Double.parseDouble(xPath.evaluate("/test/maxflow", document));

        } catch (ParserConfigurationException | java.io.IOException | SAXException | XPathExpressionException e) {
            result.success = false;
            result.message = "Can not read config\n" + e.toString();
            return result;
        }

        IOCallbacks ioCallbacks = new IOCallbacks(programRoot) {
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

        AsmSootConverter.initSoot(ioCallbacks);

        try (IOCallbacks c = ioCallbacks) {

            long start = System.nanoTime();

            Iterator<Compiler> i = compilers.iterator();
            Stream.Builder<ClassNode> classesBuilder = Stream.builder();

            while (i.hasNext())
                i.next().compile(testDirectory, c).forEach(classesBuilder);


            long compileDone = System.nanoTime();
            double informationFlow = analyzer.analyzeInformationFlow(classesBuilder.build(), c);
            long analysisDone = System.nanoTime();

            result.compileTime = compileDone - start;
            result.analyzeTime = analysisDone - compileDone;

            if (informationFlow < minFlow - 0.2) {
                result.success = false;
                result.message = "flow too low: " + informationFlow + " (should be >= " + minFlow + ")";
                return result;
            } else if (informationFlow > maxFlow + 0.2) {
                result.success = false;
                result.message = "flow too high: " + informationFlow + " (should be <= " + minFlow + ")";
                return result;
            } else {
                result.success = true;
                result.message = "flow within bounds: " + informationFlow;
                return result;
            }

        } catch (Exception e) {
            result.success = false;
            result.message = "Exception during analysis\n" + throwableToString(e);
            return result;
        }
    }

    private void writeTestResult(TestResult result) {
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(result.testPath.resolve("result.txt"))) {
                FilesUtil.writeLines(writer, Stream.of(result.success ? "SUCCESS" : "FAIL", result.message));

                if (result.compileTime < 0)
                    return;

                writer.write("compile duration: " + (result.compileTime / 1000000) + "ms\n");

                if (result.analyzeTime < 0)
                    return;

                writer.write("analysis duration: " + (result.analyzeTime / 1000000) + "ms\n");
                writer.write("total duration: " + ((result.compileTime + result.analyzeTime) / 1000000) + "ms\n");
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