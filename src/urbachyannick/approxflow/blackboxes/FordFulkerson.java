package urbachyannick.approxflow.blackboxes;

import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.stream.*;

public class FordFulkerson implements FlowConstraintAlgorithm {
    private static final double ZERO_FLOW_THRESHOLD = 0.5;

    @Override
    public double findFlowUpperBound(Stream<BlackboxCall> blackboxCalls, ToDoubleFunction<SourcesSinksPair> getPartialFlow) {
        List<FlowNode> nodes = new ArrayList<>();
        nodes.add(Input.SINGLETON);
        blackboxCalls.forEach(nodes::add);
        nodes.add(Output.SINGLETON);

        int nodeCount = nodes.size();

        double flow = 0;
        Stack<Integer> path = new Stack<>();
        path.push(0);

        for (int i = 1; i < nodeCount; ++i) {
            flow += visitPaths(path, i, nodeCount, Double.POSITIVE_INFINITY, (source, sink) -> segmentFlow(source, sink, nodes, getPartialFlow));
        }

        return flow;
    }

    private double segmentFlow(int source, int sink, List<FlowNode> nodes, ToDoubleFunction<SourcesSinksPair> getPartialFlow) {
        return getPartialFlow.applyAsDouble(
                new SourcesSinksPair(
                        Stream.of((FlowSource) nodes.get(source)),
                        Stream.of((FlowSink) nodes.get(sink))
                )
        );
    }

    @FunctionalInterface
    public interface FlowProvider {
        double getFlow(int i, int j);
    }

    public double visitPaths(Stack<Integer> path, int current, int nodeCount, double inflow, FlowProvider flowProvider) {
        double availableFlow = Math.min(inflow, flowProvider.getFlow(path.peek(), current));

        if (availableFlow < ZERO_FLOW_THRESHOLD)
            return 0;

        if (current == nodeCount - 1) // path complete
            return availableFlow;

        double childFlowSum = 0;

        path.push(current);
        for (int j = current + 1; j < nodeCount; ++j) { // only go downwards
            childFlowSum += visitPaths(path, j, nodeCount, availableFlow - childFlowSum, flowProvider);

            if (availableFlow - childFlowSum < ZERO_FLOW_THRESHOLD)
                break;
        }
        path.pop();

        return childFlowSum;
    }
}
