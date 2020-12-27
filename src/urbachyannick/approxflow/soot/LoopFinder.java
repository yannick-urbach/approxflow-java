package urbachyannick.approxflow.soot;

import soot.toolkits.graph.*;
import urbachyannick.approxflow.trees.Tree;

import java.util.List;
import java.util.stream.*;

// Uses dominator-based method as shown in
// https://www.cs.colostate.edu/~mstrout/CS553Fall09/Slides/lecture12-control.ppt.pdf slide 6

public class LoopFinder {
    public static List<Tree<Loop>> findLoops(BlockGraph graph) {
        DominatorsFinder<Block> dominatorsFinder = new MHGDominatorsFinder<>(graph);

        List<Loop> loops = graph.getBlocks().stream()
                .flatMap(block -> findLoops(dominatorsFinder, block))
                .collect(Collectors.toList());

        return toHierarchy(loops.stream());
    }

    private static Stream<Loop> findLoops(DominatorsFinder<Block> dominatorsFinder, Block block) {
        return block.getSuccs().stream()
                .filter(successor -> dominatorsFinder.isDominatedBy(block, successor))
                .map(successor -> new Loop(successor, block));
    }

    private static List<Tree<Loop>> toHierarchy(Stream<Loop> loops) {
        return Tree.fromSubordinateRelation(loops, LoopFinder::isNestedIn);
    }

    // not ideal; somehow decide with dominator relation instead?
    private static boolean isNestedIn(Loop ancestor, Loop descendant) {
        return ancestor.getHeader().getIndexInMethod() <= descendant.getHeader().getIndexInMethod() &&
                ancestor.getBackJump().getFrom().getIndexInMethod() >= descendant.getBackJump().getFrom().getIndexInMethod();
    }
}
