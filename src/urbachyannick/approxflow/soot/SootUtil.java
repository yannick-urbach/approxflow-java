package urbachyannick.approxflow.soot;

import soot.*;
import soot.toolkits.graph.Block;

import java.util.stream.*;

public class SootUtil {
    public static Stream<Unit> getUnits(Block block) {
        Stream.Builder<Unit> builder = Stream.builder();
        block.iterator().forEachRemaining(builder::add);
        return builder.build();
    }
}
