package urbachyannick.approxflow.javasignatures;

import java.util.Objects;

public class DynamicObjectSignature extends Signature {
    private final long address;
    private final String fieldName;
    private final VariableIndices indices;

    public DynamicObjectSignature(long address, String fieldName, VariableIndices indices) {
        this.address = address;
        this.fieldName = fieldName;
        this.indices = indices;
    }

    public DynamicObjectSignature(long address, String fieldName) {
        this.address = address;
        this.fieldName = fieldName;
        this.indices = new VariableIndices(-1, -1, -1);
    }

    public static DynamicObjectSignature tryParse(String input) {
        MutableInteger offset = new MutableInteger(0);

        if (!ParseUtil.checkConstant(input, "symex_dynamic::dynamic_object", offset))
            return null;

        Long address = ParseUtil.tryParseNumber(input, offset);

        if (address == null)
            return null;

        VariableIndices indices = VariableIndices.parse(input, offset);

        if (!ParseUtil.checkConstant(input, "..", offset))
            return null;

        String fieldName = Identifiers.tryParseUnqualified(input, offset);

        if (fieldName == null)
            return null;

        if (offset.get() != input.length())
            return null;

        return new DynamicObjectSignature(address, fieldName, indices);
    }

    @Override
    public String toString() {
        return "symex_dynamic::dynamic_object" + address + indices + ".." + fieldName;
    }

    public long getAddress() {
        return address;
    }

    public String getFieldName() {
        return fieldName;
    }

    public VariableIndices getIndices() {
        return indices;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, fieldName, indices);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DynamicObjectSignature))
            return false;

        DynamicObjectSignature other = (DynamicObjectSignature) o;

        return other.address == address && other.fieldName.equals(fieldName) && other.indices.equals(indices);
    }

    @Override
    public boolean matches(Signature signature) {
        if (!(signature instanceof DynamicObjectSignature))
            return false;

        DynamicObjectSignature other = (DynamicObjectSignature) signature;

        return other.address == address && other.fieldName.equals(fieldName);
    }
}
