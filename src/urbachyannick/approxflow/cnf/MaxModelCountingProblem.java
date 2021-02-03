package urbachyannick.approxflow.cnf;

import urbachyannick.approxflow.javasignatures.Signature;

import java.util.stream.*;

public class MaxModelCountingProblem {
    private final MappedProblem problem;
    private final Scope countVars;
    private final Scope maxVars;

    public MaxModelCountingProblem(MappedProblem problem, Scope countVars, Scope maxVars) {
        this.problem = problem;
        this.countVars = countVars.except(maxVars); // exclude count vars that are also in the maximization set
        this.maxVars = new Scope( // exclude max vars that do not occur in clauses
                maxVars.getVariables().filter(v -> problem.getProblem().isVariableUsed(v))
        );
    }

    public MappedProblem getMappedProblem() {
        return problem;
    }

    public Problem getProblem() {
        return problem.getProblem();
    }

    public Scope getCountVars() {
        return countVars;
    }

    public Scope getMaxVars() {
        return maxVars;
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
