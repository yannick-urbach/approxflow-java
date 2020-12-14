package urbachyannick.approxflow.javasignatures;

import java.util.Objects;

public class ArrayType implements TypeSpecifier {
    private final TypeSpecifier elementType;

    public ArrayType(TypeSpecifier elementType) {
        this.elementType = elementType;
    }

    public ArrayType(TypeSpecifier elementType, int dimensions) {
        this(
                dimensions == 1
                        ? elementType
                        : new ArrayType(elementType, dimensions - 1)
        );
    }

    public static ArrayType tryParse(String input, MutableInteger inoutOffset) {
        MutableInteger offset = new MutableInteger(inoutOffset);

        if (input.charAt(offset.get()) != '[')
            return null;

        offset.increment();

        if (offset.get() >= input.length())
            throw new SignatureParseException("Unexpected end of input.", offset.get());

        TypeSpecifier type = TypeSpecifier.parse(input, offset);

        inoutOffset.set(offset.get());
        return new ArrayType(type);
    }

    @Override
    public String asTypeSpecifierString() {
        return "[" + elementType.asTypeSpecifierString();
    }

    @Override
    public PrimitiveType asPrimitive() {
        return PrimitiveType.ADDRESS;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ArrayType.class.hashCode(), elementType); // to prevent A and A[] from having the same hash code
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ArrayType))
            return false;

        return ((ArrayType) o).elementType.equals(elementType);
    }

    public TypeSpecifier getElementType() {
        return elementType;
    }
}
