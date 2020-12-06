package urbachyannick.approxflow.modelcounting;

import urbachyannick.approxflow.IOCallbacks;
import urbachyannick.approxflow.cnf.ScopedMappedProblem;

@FunctionalInterface
public interface ModelCounter {
    double count(ScopedMappedProblem problem, IOCallbacks ioCallbacks) throws ModelCountingException;
}
