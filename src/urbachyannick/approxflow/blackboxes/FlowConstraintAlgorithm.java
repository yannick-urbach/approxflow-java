package urbachyannick.approxflow.blackboxes;

import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

@FunctionalInterface
public interface FlowConstraintAlgorithm {
    double findFlowUpperBound(Stream<BlackboxCall> blackboxCalls, ToDoubleFunction<SourcesSinksPair> getPartialFlow);
}
