package urbachyannick.approxflow.javasignatures;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

public class ClassName implements TypeSpecifier {
    private final String[] parts;

    public ClassName(String... parts) {
        this.parts = Arrays.copyOf(parts, parts.length);
    }

    public static ClassName parseWithMember(String input, MutableInteger inoutOffset) throws ParseException {
        return parse(input, inoutOffset, '.', true);
    }

    public static ClassName tryParseFromTypeSpecifier(String input, MutableInteger inoutOffset) throws ParseException {
        MutableInteger offset = new MutableInteger(inoutOffset);

        if (input.charAt(offset.get()) != 'L')
            return null;

        offset.increment();

        ClassName name = parse(input, offset, '/', false);

        if (offset.get() >= input.length())
            throw new ParseException("Unexpected end of input", offset.get());

        if (input.charAt(offset.get()) != ';')
            throw new ParseException("Expected \";\", got \"" + input.charAt(offset.get()) + "\"", offset.get());

        offset.increment();
        inoutOffset.set(offset.get());
        return name;
    }

    private static ClassName parse(String input, MutableInteger inoutOffset, char separator, boolean hasMember) throws ParseException {
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

    @Override
    public PrimtiveType asPrimitive() {
        return PrimtiveType.ADDRESS;
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
