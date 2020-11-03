package urbachyannick.approxflow.javasignatures;

import java.text.ParseException;

public class AnonymousLocal extends FunctionCallVariable {
    int stackSlot;
    PrimtiveType type;

    public AnonymousLocal(int stackSlot, PrimtiveType type) {
        this.stackSlot = stackSlot;
        this.type = type;
    }

    public static AnonymousLocal tryParse(String input, MutableInteger inoutOffset) {
        if (!input.regionMatches(inoutOffset.get(), "::anonlocal::", 0, 13))
            return null;

        inoutOffset.add(13); // "::anonlocal::".length()

        Integer stackSlot = ParseUtil.tryParseNumber(input, inoutOffset);

        if (stackSlot == null)
            return null;

        PrimtiveType type = PrimtiveType.tryParseFromVariableNamePostfix(input, inoutOffset);

        if (type == null)
            return null;

        return new AnonymousLocal(stackSlot, type);
    }

    @Override
    public String toString() {
        return "::anonlocal::" + stackSlot + type.asVariableNamePostfix();
    }
}
