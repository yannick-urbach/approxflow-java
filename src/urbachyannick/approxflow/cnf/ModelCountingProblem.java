package urbachyannick.approxflow.cnf;

public class ModelCountingProblem extends MaxModelCountingProblem {
    public ModelCountingProblem(MappedProblem problem, Scope countVars) {
        super(problem, countVars, new Scope());
    }
}
