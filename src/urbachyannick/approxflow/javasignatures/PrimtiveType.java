package urbachyannick.approxflow.javasignatures;

import org.objectweb.asm.Opcodes;

import java.util.*;
import java.util.stream.Collectors;

public enum PrimtiveType implements TypeSpecifier {
    VOID("Void", 'V', null, 0, false, Opcodes.RETURN, 0, 0, 0, 0),
    BOOLEAN("Boolean", 'Z', 'z', 1, true, Opcodes.IRETURN, Opcodes.ILOAD, Opcodes.ISTORE, Opcodes.BASTORE, Opcodes.T_BOOLEAN),
    BYTE("Byte", 'B', 'b', 1, true, Opcodes.IRETURN, Opcodes.ILOAD, Opcodes.ISTORE, Opcodes.BASTORE, Opcodes.T_BYTE),
    SHORT("Short", 'S', 's', 1, true, Opcodes.IRETURN, Opcodes.ILOAD, Opcodes.ISTORE, Opcodes.SASTORE, Opcodes.T_SHORT),
    INT("Int", 'I', 'i', 1, true, Opcodes.IRETURN, Opcodes.ILOAD, Opcodes.ISTORE, Opcodes.IASTORE, Opcodes.T_INT),
    LONG("Long", 'J', 'l', 2, true, Opcodes.LRETURN, Opcodes.LLOAD, Opcodes.LSTORE, Opcodes.LASTORE, Opcodes.T_LONG),
    FLOAT("Float", 'F', 'f', 1, true, Opcodes.FRETURN, Opcodes.FLOAD, Opcodes.FSTORE, Opcodes.FASTORE, Opcodes.T_FLOAT),
    DOUBLE("Double", 'D', 'd', 2, true, Opcodes.DRETURN, Opcodes.DLOAD, Opcodes.DSTORE, Opcodes.DASTORE, Opcodes.T_DOUBLE),
    CHAR("Char", 'C', 'c', 1, true, Opcodes.IRETURN, Opcodes.ILOAD, Opcodes.ISTORE, Opcodes.CASTORE, Opcodes.T_CHAR),
    ADDRESS(null, null, 'a', 1, false, Opcodes.ARETURN, Opcodes.ALOAD, Opcodes.ASTORE, Opcodes.AASTORE, 0); // weird

    private final Character baseType;
    private final Character variableNamePostfix;
    private final int stackSlots;
    private final boolean primitive;
    private final String name;
    private final int returnOpcode;
    private final int loadLocalOpcode;
    private final int arrayStoreOpcode;
    private final int typeOpcode;
    private final int storeLocalOpcode;
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

    PrimtiveType(
            String name,
            Character baseType,
            Character variableNamePostfix,
            int stackSlots,
            boolean primitive,
            int returnOpcode,
            int loadLocalOpcode,
            int storeLocalOpcode,
            int arrayStoreOpcode,
            int typeOpcode
    ) {
        this.baseType = baseType;
        this.variableNamePostfix = variableNamePostfix;
        this.stackSlots = stackSlots;
        this.primitive = primitive;
        this.name = name;
        this.returnOpcode = returnOpcode;
        this.loadLocalOpcode = loadLocalOpcode;
        this.storeLocalOpcode = storeLocalOpcode;
        this.arrayStoreOpcode = arrayStoreOpcode;
        this.typeOpcode = typeOpcode;
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

    public int getLoadLocalOpcode() {
        return loadLocalOpcode;
    }

    public int getStoreLocalOpcode() {
        return storeLocalOpcode;
    }

    public int getArrayStoreOpcode() {
        return arrayStoreOpcode;
    }

    public int getTypeOpcode() {
        return typeOpcode;
    }
}
