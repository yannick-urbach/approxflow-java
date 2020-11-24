package urbachyannick.approxflow.javasignatures;

import java.util.Objects;

public class DynamicArraySignature extends Signature {
    private final long address;
    private final long elementIndex;
    private final VariableIndices indices;

    public DynamicArraySignature(long address, long elementIndex, VariableIndices indices) {
        this.address = address;
        this.elementIndex = elementIndex;
        this.indices = indices;
    }

    public DynamicArraySignature(long address, long elementIndex) {
        this.address = address;
        this.elementIndex = elementIndex;
        this.indices = new VariableIndices(-1, -1, -1);
    }

    public static DynamicArraySignature tryParse(String input) {
        MutableInteger offset = new MutableInteger(0);

        if (!ParseUtil.checkConstant(input, "symex_dynamic::dynamic_", offset))
            return null;

        Long address = ParseUtil.tryParseNumber(input, offset);

        if (address == null)
            return null;

        if (!ParseUtil.checkConstant(input, "_array", offset))
            return null;

        VariableIndices indices = VariableIndices.parse(input, offset);

        if (!ParseUtil.checkConstant(input, "[[", offset))
            return null;

        long elementIndex = ParseUtil.parseNumber(input, offset);

        if (!ParseUtil.checkConstant(input, "]]", offset))
            return null;

        if (offset.get() != input.length())
            return null;

        return new DynamicArraySignature(address, elementIndex, indices);
    }

    @Override
    public String toString() {
        return "symex_dynamic::dynamic_" + address + "_array" + indices + "[[" + elementIndex + "]]";
    }

    public long getAddress() {
        return address;
    }

    public long getElementIndex() {
        return elementIndex;
    }

    public VariableIndices getIndices() {
        return indices;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, elementIndex, indices);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DynamicArraySignature))
            return false;

        DynamicArraySignature other = (DynamicArraySignature) o;

        return other.address == address && other.elementIndex == elementIndex && other.indices.equals(indices);
    }

    @Override
    public boolean matches(Signature signature) {
        if (!(signature instanceof DynamicArraySignature))
            return false;

        DynamicArraySignature other = (DynamicArraySignature) signature;

        return other.address == address && other.elementIndex == elementIndex;
    }
}
