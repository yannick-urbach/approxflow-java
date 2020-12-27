package urbachyannick.approxflow.soot;

import soot.toolkits.graph.Block;

import java.util.Objects;

public class Jump {
    private final Block from;
    private final Block to;

    public Jump(Block from, Block to) {
        this.from = from;
        this.to = to;
    }

    public Block getFrom() {
        return from;
    }

    public Block getTo() {
        return to;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof Jump))
            return false;

        Jump jump = (Jump) o;

        return (
                Objects.equals(from, jump.from) &&
                Objects.equals(to, jump.to)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }
}
