package urbachyannick.approxflow.soot;

import soot.*;
import soot.tagkit.*;
import soot.toolkits.graph.Block;

import java.util.*;
import java.util.stream.*;

public class SootUtil {
    public static Stream<Unit> getUnits(Block block) {
        Stream.Builder<Unit> builder = Stream.builder();
        block.iterator().forEachRemaining(builder::add);
        return builder.build();
    }

    public static boolean hasAnnotation(List<Tag> tags, String name) {
        if (tags == null)
            return false;

        return tags.stream()
                .filter(t -> t instanceof VisibilityAnnotationTag)
                .flatMap(t -> ((VisibilityAnnotationTag) t).getAnnotations().stream())
                .anyMatch(a -> name.equals(a.getType()));
    }

    public static Optional<AnnotationTag> getAnnotation(List<Tag> tags, String name) {
        if (tags == null)
            return Optional.empty();

        return tags.stream()
                .filter(t -> t instanceof VisibilityAnnotationTag)
                .flatMap(t -> ((VisibilityAnnotationTag) t).getAnnotations().stream())
                .filter(a -> name.equals(a.getType()))
                .findFirst();
    }
}
