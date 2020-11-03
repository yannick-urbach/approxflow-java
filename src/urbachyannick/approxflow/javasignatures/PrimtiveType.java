package urbachyannick.approxflow.javasignatures;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum PrimtiveType implements TypeSpecifier {
    VOID('V', null, 0),
    BOOLEAN('Z', 'z', 1),
    BYTE('B', 'b', 1),
    SHORT('S', 's', 1),
    INT('I', 'i', 1),
    LONG('J', 'l', 2),
    FLOAT('F', 'f', 1),
    DOUBLE('D', 'd', 2),
    CHAR('C', 'c', 1),
    ADDRESS(null, 'a', 1); // weird

    private final Character baseType;
    private final Character variableNamePostfix;
    private final int stackSlots;
    private static final Map<Character, PrimtiveType> baseTypeMap;
    private static final Map<Character, PrimtiveType> variableNamePostfixMap;

    static {
        baseTypeMap = Arrays
                .stream(values())
                .filter(v -> v.baseType != null)
                .collect(Collectors.toMap(t -> t.baseType, t -> t));

        variableNamePostfixMap = Arrays
                .stream(values())
                .filter(v -> v.variableNamePostfix != null)
                .collect(Collectors.toMap(t -> t.variableNamePostfix, t -> t));
    }

    PrimtiveType(Character baseType, Character variableNamePostfix, int stackSlots) {
        this.baseType = baseType;
        this.variableNamePostfix = variableNamePostfix;
        this.stackSlots = stackSlots;
    }

    public static PrimtiveType tryParseFromTypeSpecifier(String input, MutableInteger offset) {
        PrimtiveType type = baseTypeMap.get(input.charAt(offset.get()));

        if (type == null)
            return null;

        offset.increment();
        return type;
    }

    public static PrimtiveType tryParseFromVariableNamePostfix(String input, MutableInteger offset) {
        PrimtiveType type = variableNamePostfixMap.get(input.charAt(offset.get()));

        if (type == null)
            return null;

        offset.increment();
        return type;
    }

    @Override
    public String asTypeSpecifierString() {
        return Character.toString(baseType);
    }

    @Override
    public PrimtiveType asPrimitive() {
        return this;
    }

    public String asVariableNamePostfix() {
        return Character.toString(variableNamePostfix);
    }
}
