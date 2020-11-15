package urbachyannick.approxflow.javasignatures;

import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum PrimtiveType implements TypeSpecifier {
    VOID("Void", 'V', null, 0, false, Opcodes.RETURN),
    BOOLEAN("Boolean", 'Z', 'z', 1, true, Opcodes.IRETURN),
    BYTE("Byte", 'B', 'b', 1, true, Opcodes.IRETURN),
    SHORT("Short", 'S', 's', 1, true, Opcodes.IRETURN),
    INT("Int", 'I', 'i', 1, true, Opcodes.IRETURN),
    LONG("Long", 'J', 'l', 2, true, Opcodes.LRETURN),
    FLOAT("Float", 'F', 'f', 1, true, Opcodes.FRETURN),
    DOUBLE("Double", 'D', 'd', 2, true, Opcodes.DRETURN),
    CHAR("Char", 'C', 'c', 1, true, Opcodes.IRETURN),
    ADDRESS(null, null, 'a', 1, false, Opcodes.ARETURN); // weird

    private final Character baseType;
    private final Character variableNamePostfix;
    private final int stackSlots;
    private final boolean primitive;
    private final String name;
    private final int returnOpcode;
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

    PrimtiveType(String name, Character baseType, Character variableNamePostfix, int stackSlots, boolean primitive, int returnOpcode) {
        this.baseType = baseType;
        this.variableNamePostfix = variableNamePostfix;
        this.stackSlots = stackSlots;
        this.primitive = primitive;
        this.name = name;
        this.returnOpcode = returnOpcode;
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

    @Override
    public boolean isPrimitive() {
        return primitive;
    }

    public String asVariableNamePostfix() {
        return Character.toString(variableNamePostfix);
    }

    public String getName() {
        return name;
    }

    public int getReturnOpcode() {
        return returnOpcode;
    }
}
