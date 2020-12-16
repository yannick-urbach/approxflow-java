package urbachyannick.approxflow.trees;

import java.util.*;
import java.util.function.Function;
import java.util.stream.*;

public class Tree<Value> {
    private List<Tree<Value>> subNodes;
    private final Value value;

    @FunctionalInterface
    public interface SubordinateRelation<Value> {
        boolean isSubordinateTo(Value ancestor, Value descendant);
    }

    public Tree(Value value) {
        this.value = value;
        this.subNodes = new ArrayList<>();
    }

    public Tree(Value value, Stream<Tree<Value>> subNodes) {
        this.value = value;
        this.subNodes = subNodes.collect(Collectors.toList());
    }

    public Tree(Value value, Tree<Value>... subNodes) {
        this.value = value;
        this.subNodes = Arrays.asList(subNodes);
    }

    public Value getValue() {
        return value;
    }

    public static <Value> List<Tree<Value>> fromSubordinateRelation(Stream<Value> elements, SubordinateRelation<Value> subordinateRelation) {
        return fromSubordinateRelationInternal(elements.map(Tree::new).collect(Collectors.toList()), new ArrayList<>(), subordinateRelation);
    }

    private static <Value> List<Tree<Value>> fromSubordinateRelationInternal(List<Tree<Value>> freeElements, List<Tree<Value>> trees, SubordinateRelation<Value> subordinateRelation) {
        if (freeElements.size() == 0)
            return trees;

        Tree<Value> pivot = freeElements.remove(freeElements.size() - 1);

        Map<Boolean, List<Tree<Value>>> partitionedFree = freeElements.stream()
                .collect(Collectors.partitioningBy(e -> subordinateRelation.isSubordinateTo(pivot.value, e.value)));

        Map<Boolean, List<Tree<Value>>> partitionedTrees = trees.stream()
                .collect(Collectors.partitioningBy(e -> subordinateRelation.isSubordinateTo(pivot.value, e.value)));

        pivot.subNodes = fromSubordinateRelationInternal(partitionedFree.get(true), partitionedTrees.get(true), subordinateRelation);

        partitionedTrees.get(false).add(pivot);

        return fromSubordinateRelationInternal(partitionedFree.get(false), partitionedTrees.get(false), subordinateRelation);
    }

    public boolean contains(Value v) {
        if (this.value.equals(v))
            return true;

        return subNodes.stream().anyMatch(tree -> tree.contains(v));
    }

    public Stream<Tree<Value>> preOrder() {
        return Stream.of(Stream.of(this), subNodes.stream().flatMap(Tree::preOrder)).flatMap(Function.identity());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(value.toString() + "\n");
        String[][] subnodeLines = subNodes.stream().map(t -> t.toString().split("\n")).toArray(String[][]::new);

        if (subnodeLines.length == 0)
            return builder.toString();

        for (int i = 0; i < subnodeLines.length - 1; ++i) {
            builder.append("├─").append(subnodeLines[i][0]).append("\n");

            for (int j = 1; j < subnodeLines[i].length; ++j)
                builder.append("│ ").append(subnodeLines[i][j]).append("\n");
        }

        builder.append("└─").append(subnodeLines[subnodeLines.length - 1][0]).append("\n");

        for (int j = 1; j < subnodeLines[subnodeLines.length - 1].length; ++j)
            builder.append("  ").append(subnodeLines[subnodeLines.length - 1][j]).append("\n");

        return builder.toString();
    }
}
