package urbachyannick.approxflow.modelcounting;

import urbachyannick.approxflow.IOCallbacks;
import urbachyannick.approxflow.cnf.ModelCountingProblem;

@FunctionalInterface
public interface ModelCounter {
    double count(ModelCountingProblem problem, IOCallbacks ioCallbacks) throws ModelCountingException;
}
