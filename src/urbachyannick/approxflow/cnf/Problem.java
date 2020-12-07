package urbachyannick.approxflow.cnf;

import java.util.Arrays;
import java.util.stream.*;

public class Problem {
    private final int variableCount;
    private final Clause[] clauses;

    public Problem(int variableCount, Clause[] clauses) {
        this.variableCount = variableCount;
        this.clauses = Arrays.copyOf(clauses, clauses.length);
    }

    public Problem(int variableCount, Stream<Clause> clauses) {
        this.variableCount = variableCount;
        this.clauses = clauses.toArray(Clause[]::new);
    }

    public Stream<Clause> getClauses() {
        return Arrays.stream(clauses);
    }

    public IntStream getVariables() {
        return IntStream.range(1, variableCount + 1);
    }

    public int getVariableCount() {
        return variableCount;
    }

    public int getClauseCount() {
        return clauses.length;
    }
}
