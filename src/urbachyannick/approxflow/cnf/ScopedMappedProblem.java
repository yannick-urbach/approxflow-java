package urbachyannick.approxflow.cnf;

import urbachyannick.approxflow.javasignatures.Signature;

import java.util.stream.*;

public class ScopedMappedProblem {
    private final MappedProblem problem;
    private final Scope scope;

    public ScopedMappedProblem(MappedProblem problem, Scope scope) {
        this.problem = problem;
        this.scope = scope;
    }

    public ScopedMappedProblem(Problem problem, VariableTable table, Scope scope) {
        this.problem = new MappedProblem(problem, table);
        this.scope = scope;
    }

    public MappedProblem getMappedProblem() {
        return problem;
    }

    public Problem getProblem() {
        return problem.getProblem();
    }

    public Scope getScope() {
        return scope;
    }

    public VariableTable getVariableTable() {
        return problem.getVariableTable();
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
        return problem.getVariableTable().get(signature);
    }
}
