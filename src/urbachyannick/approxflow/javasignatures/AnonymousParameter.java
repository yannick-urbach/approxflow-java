package urbachyannick.approxflow.javasignatures;

import java.text.ParseException;
import java.util.Objects;

public class AnonymousParameter extends FunctionCallVariable {
    int stackSlot;
    PrimtiveType type;

    public AnonymousParameter(int stackSlot, PrimtiveType type) {
        this.stackSlot = stackSlot;
        this.type = type;
    }

    public static AnonymousParameter tryParse(String input, MutableInteger inoutOffset) {
        if (!input.regionMatches(inoutOffset.get(), "::arg", 0, 5))
            return null;

        inoutOffset.add(5); // "::arg".length()
        Integer stackSlot = ParseUtil.tryParseNumber(input, inoutOffset);

        if (stackSlot == null)
            return null;

        PrimtiveType type = PrimtiveType.tryParseFromVariableNamePostfix(input, inoutOffset);

        if (type == null)
            return null;

        if (inoutOffset.get() < input.length() && Character.isJavaIdentifierPart(input.charAt(inoutOffset.get())))
            return null;

        return new AnonymousParameter(stackSlot, type);
    }

    @Override
    public String toString() {
        return "::arg" + stackSlot + type.asVariableNamePostfix();
    }

    @Override
    public int hashCode() {
        return Objects.hash(stackSlot, type);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AnonymousLocal))
            return false;

        AnonymousLocal o = (AnonymousLocal) other;

        return (
                type.equals(o.type) &&
                stackSlot == o.stackSlot
        );
    }
}
