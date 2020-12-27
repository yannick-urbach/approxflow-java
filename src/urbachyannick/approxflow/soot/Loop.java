package urbachyannick.approxflow.soot;

import soot.toolkits.graph.*;

import java.util.*;
import java.util.stream.*;

public class Loop {
    private final Jump backJump;

    public Loop(Block header, Block backJump) {
        this.backJump = new Jump(backJump, header);
    }

    public Block getHeader() {
        return backJump.getTo();
    }

    public Jump getBackJump() {
        return backJump;
    }

    public Stream<Block> getLoopBodyBlocks() {
        Set<Block> blocks = collectBodyBlocks(getHeader(), new HashSet<>()).stream()
                .map(Jump::getTo)
                .collect(Collectors.toSet());

        blocks.add(getHeader());

        return blocks.stream();
    }

    // walk all paths from current and return visited blocks if the path reaches backJump
    private Set<Jump> collectBodyBlocks(Block current, Set<Jump> visited) {
        if (current.equals(backJump.getFrom()))
            return visited;

        Stream<Set<Jump>> childResults = current.getSuccs().stream()
                .filter(successor -> !successor.equals(getHeader()))
                .map(successor -> new Jump(current, successor))
                .filter(jump -> !visited.contains(jump))
                .map(jump -> {
                    Set<Jump> visitedParam = new HashSet<>(visited);
                    visitedParam.add(jump);
                    return collectBodyBlocks(jump.getTo(), visitedParam);
                });

        Set<Jump> result = new HashSet<>();
        childResults.forEach(result::addAll);

        return result;
    }

    @Override
    public String toString() {
        return String.format("Loop from %d to %d", getHeader().getIndexInMethod(), backJump.getFrom().getIndexInMethod());
    }
}
