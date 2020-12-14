package urbachyannick.approxflow.javasignatures;

public interface TypeSpecifier {
    String asTypeSpecifierString();
    PrimitiveType asPrimitive();
    boolean isPrimitive();

    static TypeSpecifier parse(String input, MutableInteger inoutOffset) {
        TypeSpecifier type = PrimitiveType.tryParseFromTypeSpecifier(input, inoutOffset);

        if (type == null)
            type = ClassName.tryParseFromTypeSpecifier(input, inoutOffset);

        if (type == null)
            type = ArrayType.tryParse(input, inoutOffset);

        return type;
    }
}
