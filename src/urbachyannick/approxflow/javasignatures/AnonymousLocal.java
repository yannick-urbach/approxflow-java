package urbachyannick.approxflow.javasignatures;

import java.util.Objects;

public class AnonymousLocal extends FunctionCallVariable {
    int stackSlot;
    PrimtiveType type;

    public AnonymousLocal(int stackSlot, PrimtiveType type) {
        this.stackSlot = stackSlot;
        this.type = type;
    }

    public static AnonymousLocal tryParse(String input, MutableInteger inoutOffset) {
        if (!ParseUtil.checkConstant(input, "::anonlocal::", inoutOffset))
            return null;

        Long stackSlot = ParseUtil.tryParseNumber(input, inoutOffset);

        if (stackSlot == null)
            return null;

        PrimtiveType type = PrimtiveType.tryParseFromVariableNamePostfix(input, inoutOffset);

        if (type == null)
            return null;

        return new AnonymousLocal(stackSlot.intValue(), type);
    }

    @Override
    public String toString() {
        return "::anonlocal::" + stackSlot + type.asVariableNamePostfix();
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
