package urbachyannick.approxflow.modelcounting;

import urbachyannick.approxflow.IOCallbacks;
import urbachyannick.approxflow.cnf.*;

public class CounterPicker implements MaxModelCounter {
    private final MaxModelCounter maxModelCounter;
    private final ModelCounter modelCounter;

    public CounterPicker(MaxModelCounter maxModelCounter, ModelCounter modelCounter) {
        this.maxModelCounter = maxModelCounter;
        this.modelCounter = modelCounter;
    }

    @Override
    public double count(MaxModelCountingProblem problem, IOCallbacks ioCallbacks) throws ModelCountingException {
        if (problem instanceof ModelCountingProblem)
            return maxModelCounter.count(problem, ioCallbacks);
        if (problem.getMaxVars().getVariables().findAny().isPresent())
            return maxModelCounter.count(problem, ioCallbacks);

        return modelCounter.count(new ModelCountingProblem(problem.getMappedProblem(), problem.getCountVars()), ioCallbacks);
    }
}
