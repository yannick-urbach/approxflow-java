package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.tree.*;

import java.util.*;

import static urbachyannick.approxflow.codetransformation.BytecodeUtil.*;

public class InlinePreferences {
    private final boolean override;
    private final Boolean _shouldInline;
    private final Integer recursions;

    private InlinePreferences(Boolean shouldInline, Integer recursions, boolean override) {
        this._shouldInline = shouldInline;
        this.recursions = recursions;
        this.override = override;
    }

    public InlinePreferences(boolean shouldInline, int recursions, boolean override) {
        this._shouldInline = shouldInline;
        this.recursions = recursions;
        this.override = override;
    }

    public boolean shouldInline() {
        return _shouldInline != null && _shouldInline;
    }

    public int getRecursions() {
        return recursions == null ? 1 : recursions;
    }

    public InlinePreferences combineWith(InlinePreferences other) {
        if (other.override)
            return this.overrideWith(other);
        else if (this.override)
            return other.overrideWith(this);

        Boolean shouldInline = this._shouldInline;
        Integer recursions = this.recursions;

        if (shouldInline == null || (other._shouldInline != null && other._shouldInline))
            shouldInline = other._shouldInline;

        if (other.recursions != null && (recursions == null || recursions < other.recursions))
            recursions = other.recursions;

        return new InlinePreferences(shouldInline, recursions, false);
    }

    public InlinePreferences overrideWith(InlinePreferences other) {
        Boolean shouldInline = this._shouldInline;
        Integer recursions = this.recursions;

        if (other._shouldInline != null)
            shouldInline = other._shouldInline;

        if (other.recursions != null)
            recursions = other.recursions;

        return new InlinePreferences(shouldInline, recursions, other.override);
    }

    private static InlinePreferences fromInlineMethodAnnotation(List<AnnotationNode> annotations) {
        Optional<AnnotationNode> node = getAnnotation(annotations, "Lurbachyannick/approxflow/InlineMethod;");
        Optional<Integer> recursions = node.flatMap(n -> getAnnotationValue(n, "recursions").map(v -> (int) v));
        Optional<Boolean> override = node.flatMap(n -> getAnnotationValue(n, "override").map(v -> (boolean) v));
        Optional<Boolean> shouldInline = node.flatMap(n -> getAnnotationValue(n, "shouldInline").map(v -> (boolean) v));

        return node
                .map(n -> new InlinePreferences(shouldInline.orElse(true), recursions.orElse(null), (boolean) override.orElse(false)))
                .orElse(new InlinePreferences(null, null, false));
    }

    private static InlinePreferences fromInlineCallsAnnotation(List<AnnotationNode> annotations) {
        Optional<AnnotationNode> node = getAnnotation(annotations, "Lurbachyannick/approxflow/InlineCalls;");
        Optional<Integer> recursions = node.flatMap(n -> getAnnotationValue(n, "recursions").map(v -> (int) v));
        Optional<Boolean> override = node.flatMap(n -> getAnnotationValue(n, "override").map(v -> (boolean) v));
        Optional<Boolean> shouldInline = node.flatMap(n -> getAnnotationValue(n, "shouldInline").map(v -> (boolean) v));

        return node
                .map(n -> new InlinePreferences(shouldInline.orElse(true), recursions.orElse(null), (boolean) override.orElse(false)))
                .orElse(new InlinePreferences(null, null, false));
    }

    public static InlinePreferences get(
            InlinePreferences defaultPrefs,
            ClassNode calledMethodOwner, MethodNode calledMethod,
            ClassNode callingMethodOwner, MethodNode callingMethod
    ) {
        return defaultPrefs
                .combineWith(fromInlineMethodAnnotation(calledMethodOwner.visibleAnnotations))
                .combineWith(fromInlineMethodAnnotation(calledMethod.visibleAnnotations))
                .combineWith(fromInlineCallsAnnotation(callingMethodOwner.visibleAnnotations))
                .combineWith(fromInlineCallsAnnotation(callingMethod.visibleAnnotations));
    }
}
