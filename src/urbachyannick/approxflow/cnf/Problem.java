package urbachyannick.approxflow.cnf;

import java.util.*;
import java.util.stream.*;

public class Problem {
    private final int variableCount;
    private final Clause[] clauses;
    private final Set<Integer> usedVariables;

    public Problem(int variableCount, Clause[] clauses) {
        this.variableCount = variableCount;
        this.clauses = Arrays.copyOf(clauses, clauses.length);
        this.usedVariables = new HashSet<>();

        for (Clause c : this.clauses)
            c.getLiterals().forEach(l -> usedVariables.add(l.getVariable()));
    }

    public Problem(int variableCount, Stream<Clause> clauses) {
        this.variableCount = variableCount;
        this.clauses = clauses.toArray(Clause[]::new);
        this.usedVariables = new HashSet<>();

        for (Clause c : this.clauses)
            c.getLiterals().forEach(l -> usedVariables.add(l.getVariable()));
    }

    public Stream<Clause> getClauses() {
        return Arrays.stream(clauses);
    }

    public IntStream getVariables() {
        return IntStream.range(1, variableCount + 1);
    }

    public boolean isVariableUsed(int variable) {
        return usedVariables.contains(variable);
    }

    public int getVariableCount() {
        return variableCount;
    }

    public int getClauseCount() {
        return clauses.length;
    }
}
