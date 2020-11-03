package urbachyannick.approxflow.javasignatures;

import java.text.ParseException;

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

    public static ArrayType tryParse(String input, MutableInteger inoutOffset) throws ParseException {
        MutableInteger offset = new MutableInteger(inoutOffset);

        if (input.charAt(offset.get()) != '[')
            return null;

        offset.increment();

        if (offset.get() >= input.length())
            throw new ParseException("Unexpected end of input.", offset.get());

        TypeSpecifier type = TypeSpecifier.parse(input, offset);

        inoutOffset.set(offset.get());
        return new ArrayType(type);
    }

    @Override
    public String asTypeSpecifierString() {
        return "[" + elementType.asTypeSpecifierString();
    }

    @Override
    public PrimtiveType asPrimitive() {
        return PrimtiveType.ADDRESS;
    }
}
