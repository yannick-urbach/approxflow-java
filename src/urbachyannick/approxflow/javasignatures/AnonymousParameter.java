package urbachyannick.approxflow.javasignatures;

import java.util.Objects;

public class AnonymousParameter extends FunctionCallVariable {
    int stackSlot;
    PrimitiveType type;

    public AnonymousParameter(int stackSlot, PrimitiveType type) {
        this.stackSlot = stackSlot;
        this.type = type;
    }

    public static AnonymousParameter tryParse(String input, MutableInteger inoutOffset) {
        if (!ParseUtil.checkConstant(input, "::arg", inoutOffset))
            return null;

        Long stackSlot = ParseUtil.tryParseNumber(input, inoutOffset);

        if (stackSlot == null)
            return null;

        PrimitiveType type = PrimitiveType.tryParseFromVariableNamePostfix(input, inoutOffset);

        if (type == null)
            return null;

        if (inoutOffset.get() < input.length() && Character.isJavaIdentifierPart(input.charAt(inoutOffset.get())))
            return null;

        return new AnonymousParameter(stackSlot.intValue(), type);
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
        if (!(other instanceof AnonymousParameter))
            return false;

        AnonymousParameter o = (AnonymousParameter) other;

        return (
                type.equals(o.type) &&
                stackSlot == o.stackSlot
        );
    }
}
