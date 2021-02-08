package urbachyannick.approxflow.javasignatures;

import org.objectweb.asm.Opcodes;

import java.util.*;
import java.util.stream.Collectors;

public enum PrimitiveType implements TypeSpecifier {
    VOID("Void", 'V', null, 0, false, Opcodes.RETURN, Opcodes.NOP, Opcodes.NOP, Opcodes.NOP, -1, Opcodes.NOP),
    BOOLEAN("Boolean", 'Z', 'z', 1, true, Opcodes.IRETURN, Opcodes.ILOAD, Opcodes.ISTORE, Opcodes.BASTORE, Opcodes.T_BOOLEAN, Opcodes.ICONST_0),
    BYTE("Byte", 'B', 'b', 1, true, Opcodes.IRETURN, Opcodes.ILOAD, Opcodes.ISTORE, Opcodes.BASTORE, Opcodes.T_BYTE, Opcodes.ICONST_0),
    SHORT("Short", 'S', 's', 1, true, Opcodes.IRETURN, Opcodes.ILOAD, Opcodes.ISTORE, Opcodes.SASTORE, Opcodes.T_SHORT, Opcodes.ICONST_0),
    INT("Int", 'I', 'i', 1, true, Opcodes.IRETURN, Opcodes.ILOAD, Opcodes.ISTORE, Opcodes.IASTORE, Opcodes.T_INT, Opcodes.ICONST_0),
    LONG("Long", 'J', 'l', 2, true, Opcodes.LRETURN, Opcodes.LLOAD, Opcodes.LSTORE, Opcodes.LASTORE, Opcodes.T_LONG, Opcodes.LCONST_0),
    FLOAT("Float", 'F', 'f', 1, true, Opcodes.FRETURN, Opcodes.FLOAD, Opcodes.FSTORE, Opcodes.FASTORE, Opcodes.T_FLOAT, Opcodes.FCONST_0),
    DOUBLE("Double", 'D', 'd', 2, true, Opcodes.DRETURN, Opcodes.DLOAD, Opcodes.DSTORE, Opcodes.DASTORE, Opcodes.T_DOUBLE, Opcodes.DCONST_0),
    CHAR("Char", 'C', 'c', 1, true, Opcodes.IRETURN, Opcodes.ILOAD, Opcodes.ISTORE, Opcodes.CASTORE, Opcodes.T_CHAR, Opcodes.ICONST_0),
    ADDRESS(null, null, 'a', 1, false, Opcodes.ARETURN, Opcodes.ALOAD, Opcodes.ASTORE, Opcodes.AASTORE, -1, Opcodes.ACONST_NULL); // weird

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
    private final int loadDefaultOpcode;
    private static final Map<Character, PrimitiveType> baseTypeMap;
    private static final Map<Character, PrimitiveType> variableNamePostfixMap;

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

    PrimitiveType(
            String name,
            Character baseType,
            Character variableNamePostfix,
            int stackSlots,
            boolean primitive,
            int returnOpcode,
            int loadLocalOpcode,
            int storeLocalOpcode,
            int arrayStoreOpcode,
            int typeOpcode,
            int loadDefaultOpcode
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
        this.loadDefaultOpcode = loadDefaultOpcode;
    }

    public static PrimitiveType tryParseFromTypeSpecifier(String input, MutableInteger offset) {
        PrimitiveType type = baseTypeMap.get(input.charAt(offset.get()));

        if (type == null)
            return null;

        offset.increment();
        return type;
    }

    public static PrimitiveType tryParseFromVariableNamePostfix(String input, MutableInteger offset) {
        PrimitiveType type = variableNamePostfixMap.get(input.charAt(offset.get()));

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
    public PrimitiveType asPrimitive() {
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

    public int getLoadDefaultOpcode() {
        return loadDefaultOpcode;
    }
}
