package urbachyannick.approxflow.javasignatures;

import java.nio.file.*;
import java.util.*;

public class ClassName implements TypeSpecifier {
    private final String[] parts;

    public ClassName(String... parts) {
        this.parts = Arrays.copyOf(parts, parts.length);
    }

    public static ClassName parseWithMember(String input, MutableInteger inoutOffset) {
        return parse(input, inoutOffset, '.', true);
    }

    public static ClassName tryParseFromTypeSpecifier(String input, MutableInteger inoutOffset) {
        MutableInteger offset = new MutableInteger(inoutOffset);

        if (!ParseUtil.checkConstant(input, "L", offset))
            return null;

        ClassName name = parse(input, offset, '/', false);

        if (offset.get() >= input.length())
            throw new SignatureParseException("Unexpected end of input", offset.get());

        if (!ParseUtil.checkConstant(input, ";", offset))
            throw new SignatureParseException("Expected \";\", got \"" + input.charAt(offset.get()) + "\"", offset.get());

        inoutOffset.set(offset.get());
        return name;
    }

    private static ClassName parse(String input, MutableInteger inoutOffset, char separator, boolean hasMember) {
        List<String> parts = Identifiers.parseQualified(input, inoutOffset, separator, hasMember);

        return new ClassName(parts.toArray(new String[0]));
    }

    public String asQualifiedName()  {
        return String.join(".", parts);
    }

    @Override
    public String asTypeSpecifierString()  {
        return "L" + String.join("/", parts) + ";";
    }

    public Path asPath(String extension) {
        String[] packageParts = Arrays.copyOfRange(parts, 0, parts.length - 1);
        Path path = Paths.get("", packageParts).resolve(parts[parts.length - 1] + extension);

        return Paths.get("", path.toString());
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
    public boolean equals(Object o) {
        if (!(o instanceof ClassName))
            return false;

        ClassName other = (ClassName) o;
        return Arrays.equals(other.parts, parts);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(parts);
    }
}
