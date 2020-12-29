package urbachyannick.approxflow.modelcounting;

import urbachyannick.approxflow.IOCallbacks;
import urbachyannick.approxflow.cnf.*;

@FunctionalInterface
public interface MaxModelCounter {
    double count(MaxModelCountingProblem problem, IOCallbacks ioCallbacks) throws ModelCountingException;

    default double count(ModelCountingProblem problem, IOCallbacks ioCallbacks) throws ModelCountingException {
        return count((MaxModelCountingProblem) problem, ioCallbacks);
    }
}
