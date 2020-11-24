package urbachyannick.approxflow.cnf;

import urbachyannick.approxflow.javasignatures.Signature;

import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MappedProblem {
    private final Problem problem;
    private final VariableTable variableTable;

    public MappedProblem(Problem problem, VariableTable variableTable) {
        this.problem = problem;
        this.variableTable = variableTable;
    }

    public Problem getProblem() {
        return problem;
    }

    public VariableTable getVariableTable() {
        return variableTable;
    }

    public Stream<Clause> getClauses() {
        return problem.getClauses();
    }

    public IntStream getVariables() {
        return problem.getVariables();
    }

    public int getVariableCount() {
        return problem.getVariableCount();
    }

    public int getClauseCount() {
        return problem.getClauseCount();
    }

    public VariableMapping getMapping(Signature signature) {
        return variableTable.get(signature);
    }
}
