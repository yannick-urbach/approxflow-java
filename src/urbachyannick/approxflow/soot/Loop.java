package urbachyannick.approxflow.soot;

import soot.toolkits.graph.Block;

public class Loop {
    private final Block header;
    private final Block backJump;

    public Loop(Block header, Block backJump) {
        this.header = header;
        this.backJump = backJump;
    }

    public Block getHeader() {
        return header;
    }

    public Block getBackJump() {
        return backJump;
    }

    @Override
    public String toString() {
        return String.format("Loop from %d to %d", header.getIndexInMethod(), backJump.getIndexInMethod());
    }
}
