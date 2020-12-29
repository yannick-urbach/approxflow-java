package urbachyannick.approxflow.cnf;

import urbachyannick.approxflow.FilesUtil;
import urbachyannick.approxflow.javasignatures.Signature;

import java.io.*;
import java.nio.file.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.*;

public class IO {
    public static Problem readProblem(Path path) throws IOException {
        String problemLine = Files
                .lines(path)
                .filter(l -> l.startsWith("p "))
                .findFirst()
                .orElseThrow(() -> new IOException("Invalid CNF file format (missing problem line)"));

        if (!problemLine.matches("p cnf \\d+ \\d+"))
            throw new IOException("Invalid CNF file format (invalid problem line)");

        String[] parts = problemLine.split(" ");

        Pattern clauseLine = Pattern.compile("(-?\\d+ )+0");
        Pattern space = Pattern.compile(" ");

        int variableCount = Integer.parseInt(parts[2]);
        int clauseCount = Integer.parseInt(parts[3]);

        Stream<Clause> clauses = Files
                .lines(path)
                .filter(l -> clauseLine.matcher(l).matches())
                .map(l -> new Clause(space.splitAsStream(l.subSequence(0, l.length() - 2)).map(i -> new Literal(Integer.parseInt(i)))));

        Problem p = new Problem(variableCount, clauses);

        if (p.getClauseCount() != clauseCount)
            throw new IOException("Invalid CNF file format (incorrect clause count)");

        return p;
    }

    public static VariableTable readVariableTable(Path path) throws IOException {
        Pattern space = Pattern.compile(" ");

        Stream<VariableMapping> mappings = Files
                .lines(path)
                .filter(l -> l.startsWith("c ") && !l.startsWith("c ind "))
                .map(l -> {
                    String[] parts = l.split(" ", 3);
                    Signature signature = Signature.parse(parts[1]);
                    Stream<MappingValue> values = space
                            .splitAsStream(parts[2])
                            .map(v ->
                                    v.equals("TRUE") ? TrivialMappingValue.TRUE :
                                    v.equals("FALSE") ? TrivialMappingValue.FALSE :
                                    new Literal(Integer.parseInt(v))
                            );
                    return new VariableMapping(signature, values);
                });

        return new VariableTable(mappings);
    }

    public static MappedProblem readMappedProblem(Path path) throws IOException {
        Problem p = readProblem(path);
        VariableTable v = readVariableTable(path);
        return new MappedProblem(p, v);
    }

    public static Scope readScopeLines(Path path, String prefix) throws IOException {
        Pattern space = Pattern.compile(" ");

        IntStream variables = Files
                .lines(path)
                .filter(l -> l.startsWith(prefix))
                .flatMapToInt(l -> space.splitAsStream(l.subSequence(6, l.length() - 2)).mapToInt(Integer::parseInt));

        return new Scope(variables);
    }

    public static ModelCountingProblem readModelCountingProblem(Path path) throws IOException {
        MappedProblem p = readMappedProblem(path);
        Scope s = readScopeLines(path, "c ind ");
        return new ModelCountingProblem(p, s);
    }

    public static MaxModelCountingProblem readMaxModelCountingProblem(Path path) throws IOException {
        MappedProblem p = readMappedProblem(path);
        Scope countVars = readScopeLines(path, "c ind ");
        Scope maxVars = readScopeLines(path, "c max ");

        if (maxVars.getVariables().findAny().isPresent())
            return new MaxModelCountingProblem(p, countVars, maxVars);
        else
            return new ModelCountingProblem(p, countVars);
    }

    public static Stream<String> problemLines(Problem problem) {
        return Stream.concat(
                Stream.of(String.format("p cnf %d %d", problem.getVariableCount(), problem.getClauseCount())),
                problem
                        .getClauses()
                        .map(c -> c
                                .getLiterals()
                                .map(l -> Integer.toString((l.getValue() ? 1 : -1) * l.getVariable()))
                                .collect(Collectors.joining(" "))
                                + " 0"
                        )
        );
    }

    public static Stream<String> variableTableLines(VariableTable table) {
        return table
                .getMappings()
                .map(m ->
                        String.format("c %s %s",
                                m.getSignature(),
                                m.getMappingValues().map(IO::mappingValueToString).collect(Collectors.joining(" "))
                        )
                );
    }

    private static String mappingValueToString(MappingValue value) {
        if (value == TrivialMappingValue.TRUE)
            return "TRUE";

        if (value == TrivialMappingValue.FALSE)
            return "FALSE";

        Literal literal = (Literal) value;

        return "" + (literal.getValue() ? 1 : -1) * literal.getVariable();
    }

    public static String scopeLine(Scope scope, String prefix) {
        return prefix + scope.getVariables().mapToObj(Integer::toString).collect(Collectors.joining(" ")) + " 0";
    }

    public static String crLine(Scope scope) {
        return "cr " + scope.getVariables().mapToObj(Integer::toString).collect(Collectors.joining(" "));
    }

    public static void write(Problem problem, Path path) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            FilesUtil.writeLines(writer, problemLines(problem));
        }
    }

    public static void write(VariableTable table, Path path) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            FilesUtil.writeLines(writer, variableTableLines(table));
        }
    }

    public static void write(MappedProblem problem, Path path) throws IOException {
        Stream<String> lines = Stream.concat(
                problemLines(problem.getProblem()),
                variableTableLines(problem.getVariableTable())
        );

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            FilesUtil.writeLines(writer, lines);
        }
    }

    public static void write(ModelCountingProblem problem, Path path) throws IOException {
        Stream<String> lines = Stream.of(
                problemLines(problem.getProblem()),
                variableTableLines(problem.getVariableTable()),
                Stream.of(scopeLine(problem.getCountVars(), "c ind ")),
                Stream.of(crLine(problem.getCountVars()))
        ).flatMap(Function.identity());

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            FilesUtil.writeLines(writer, lines);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(path.resolveSibling(path.getFileName() + ".scope"))) {
            FilesUtil.writeLines(writer, problem.getCountVars().getVariables().mapToObj(Integer::toString));
        }
    }

    public static void write(MaxModelCountingProblem problem, Path path) throws IOException {
        Stream<String> lines = Stream.of(
                problemLines(problem.getProblem()),
                variableTableLines(problem.getVariableTable()),
                Stream.of(scopeLine(problem.getCountVars(), "c ind ")),
                Stream.of(scopeLine(problem.getMaxVars(), "c max "))
        ).flatMap(Function.identity());

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            FilesUtil.writeLines(writer, lines);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(path.resolveSibling(path.getFileName() + ".scope"))) {
            FilesUtil.writeLines(writer, problem.getCountVars().getVariables().mapToObj(Integer::toString));
        }
    }
}
