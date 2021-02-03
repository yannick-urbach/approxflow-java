package urbachyannick.approxflow.blackboxes;

import static urbachyannick.approxflow.MiscUtil.append;

import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.stream.*;

public class StagedFlow implements FlowConstraintAlgorithm {
    @Override
    public double findFlowUpperBound(Stream<BlackboxCall> blackboxCalls, ToDoubleFunction<SourcesSinksPair> getPartialFlow) {
        List<BlackboxCall> calls = blackboxCalls.collect(Collectors.toList());

        double flow = Double.POSITIVE_INFINITY;

        for (int i = 0; i <= calls.size(); ++i) {
            double stageFlow = getPartialFlow.applyAsDouble(
                    new SourcesSinksPair(
                            append(calls.subList(0, i).stream().map(c -> c), Input.SINGLETON), // input and blackboxes up to (excluding) i
                            append(calls.subList(i, calls.size()).stream().map(c -> c), Output.SINGLETON) // output and blackboxes starting from i
                    )
            );

            flow = Math.min(flow, stageFlow);
        }

        return flow;
    }
}
