package urbachyannick.approxflow.modelcounting;

import urbachyannick.approxflow.cnf.ScopedMappedProblem;

@FunctionalInterface
public interface ModelCounter {
    double count(ScopedMappedProblem problem) throws ModelCountingException;
}
