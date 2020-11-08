package urbachyannick.approxflow;

import picocli.CommandLine;

import java.io.*;
import java.nio.file.Files;
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

        CnfFile cnfFile = generateCnf();
        CnfVarLine valLine = getValLine(cnfFile);
        CnfFile renamed = null;

        List<Integer> variables = valLine
                .getLiterals()
                .filter(CnfLiteral::isNonTrivial)
                .boxed()
                .collect(Collectors.toList());

        if (variables.isEmpty())
            fail("No variables for which to solve. Information flow might be 0.");

        try {
            renamed = cnfFile.renameVariablesToBottom(variables.stream().mapToInt(Integer::intValue));
        } catch (IOException e) {
            fail("Failed to rename variables", e);
            throw new Unreachable();
        }

        int variableCount = variables.size();

        addIndLines(renamed, IntStream.range(1, variableCount + 1));
        addCrLines(renamed, IntStream.range(1, variableCount + 1));
        createScopeFile(classpath.resolve(className + ".cnf.scope"), IntStream.range(1, variableCount + 1));

        if (!cnfOnly) {
            double informationFlow = calculateInformationFlow(renamed);
            System.out.println("Approximated flow is: " + informationFlow);
        }
    }

    /**
     * Builds the main class using javac.
     */
    private void buildClass() {
        runCommand(
                classpath,
                "javac",
                "-classpath", Paths.get("res/jbmc-core-models.jar").toAbsolutePath().toString(),
                "-g",
                className + ".java"
        );
    }

    /**
     * Generates the CNF file from the compiled class
     *
     * @return the CNF file
     */
    private CnfFile generateCnf() {
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
            return new CnfFile(cnfFilePath);
        } catch (CnfException | IOException e) {
            fail("Failed to generate CNF");
            throw new Unreachable();
        }
    }

    /**
     * Finds the variable line for the static ___val variable in a CNF file.
     *
     * @param cnfFile the CNF file in which to search
     * @return the variable line
     */
    private CnfVarLine getValLine(CnfFile cnfFile) {
        try {
            String signature = "java::" + className + ".___val";

            return cnfFile
                    .getVarLines()
                    .filter(line -> signature.equals(line.getSignature()))
                    .max(CnfVarLine::compareByIndex)
                    .orElseThrow(() -> new CnfException("missing variable line for ___val"));

        } catch (IOException | CnfException e) {
            fail("Failed to get ___val line", e);
            throw new Unreachable();
        }
    }

    /**
     * Adds the scope lines for ApproxMC (c ind ...). If scope lines are already present, duplicates are avoided.
     *
     * @param file the CNF file to which the lines will be added
     * @param variables the variables to add to the scope lines
     */
    private void addIndLines(CnfFile file, IntStream variables) {
        try {
            // variables already in scope lines (why can there already be scope lines?)
            Set<Integer> approxMc = file.getIndLines()
                    .flatMap(line -> line.getLiterals().boxed())
                    .collect(Collectors.toSet());

            // variables requested, but not yet in scope lines
            List<Integer> approxMcAllsatVars = variables
                    .filter(variable -> !approxMc.contains(variable))
                    .boxed().collect(Collectors.toList());

            // split approxMcAllsatVars into scope lines of 10 literals each (why is this necessary?)
            Stream<CnfIndLine> newLines = IntStream
                    .range(0, (approxMcAllsatVars.size() + 9) / 10)
                    .map(i -> 10 * i)
                    .mapToObj(i ->
                            new CnfIndLine(
                                    approxMcAllsatVars.subList(i, Math.min(i + 10, approxMcAllsatVars.size()))
                                            .stream().mapToInt(Integer::intValue)
                            )
                    );

            // add missing
            file.addIndLines(newLines);
        } catch (IOException e) {
            fail("FAIL: Failed to add ind lines", e);
        }
    }

    /**
     * Adds the scope lines for ApproxMC-py (cr ...). If scope lines are already present, duplicates are avoided.
     *
     * @param file the CNF file to which the lines will be added
     * @param variables the variables to add to the scope lines
     */
    private void addCrLines(CnfFile file, IntStream variables) {
        try {
            // variables in cr lines (why can there already be cr lines?)
            Set<Integer> approxMcPy = file.getCrLines()
                    .flatMap(line -> line.getLiterals().boxed())
                    .collect(Collectors.toSet());

            // variables in val line, but not yet in cr lines
            List<Integer> approxMcPyAllsatVars = variables
                    .filter(variable -> !approxMcPy.contains(variable))
                    .boxed().collect(Collectors.toList());

            // add missing
            file.addCrLines(Stream.of(new CnfCrLine(approxMcPyAllsatVars.stream().mapToInt(Integer::intValue))));
        } catch (IOException e) {
            fail("Failed to add cr line", e);
        }
    }

    /**
     * Creates the scope file for sharpCDCL.
     *
     * @param path the path of the scope file
     * @param variables the variables to add to the scope file
     */
    private void createScopeFile(Path path, IntStream variables) {
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                FilesUtil.writeLines(writer, variables.mapToObj(CnfLiteral::toString));
            }
        } catch (IOException e) {
            fail("Failed to create scope file", e);
        }
    }

    /**
     * Uses scalmc to calculate the information flow for a given CNF file.
     *
     * @param file the CNF file
     * @return the information flow
     */
    private double calculateInformationFlow(CnfFile file) {
        try {
            Process process = new ProcessBuilder()
                    .command(Paths.get("util/scalmc").toAbsolutePath().toString(), file.getPath().toString())
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
                fail(command.stream().findFirst().orElse("command") + "returned error code " + process.exitValue());
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