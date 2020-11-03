package urbachyannick.approxflow.javasignatures;

import java.text.ParseException;

public interface TypeSpecifier {
    String asTypeSpecifierString();
    PrimtiveType asPrimitive();

    static TypeSpecifier parse(String input, MutableInteger inoutOffset) throws ParseException {
        TypeSpecifier type = PrimtiveType.tryParseFromTypeSpecifier(input, inoutOffset);

        if (type == null)
            type = ClassName.tryParseFromTypeSpecifier(input, inoutOffset);

        if (type == null)
            type = ArrayType.tryParse(input, inoutOffset);

        return type;
    }
}
