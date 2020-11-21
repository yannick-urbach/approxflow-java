package urbachyannick.approxflow.javasignatures;

import java.text.ParseException;

public class NamedLocal extends FunctionCallVariable {
    private String name;
    private int mysteryNumber; // somehow related to "start" in the local variable table, but 1 lower for some reason?

    public NamedLocal(String name) {
        this.name = name;
        mysteryNumber = 0;
    }

    private NamedLocal(String name, int mysteryNumber) {
        this.name = name;
        this.mysteryNumber = mysteryNumber;
    }

    public static NamedLocal tryParse(String input, MutableInteger inoutOffset) {
        MutableInteger offset = new MutableInteger(inoutOffset);

        if (!ParseUtil.checkConstant(input, "::", offset))
            return null;

        MutableInteger beforeNumber = new MutableInteger(offset);

        Long mysteryNumber = ParseUtil.tryParseNumber(input, offset);

        if (mysteryNumber == null) {
            mysteryNumber = 0L;
        } else if (!ParseUtil.checkConstant(input, "::", offset)) {
            // actually part of the name (is that possible? probably not...)
            offset = beforeNumber;
            mysteryNumber = 0L;
        } else {
            // is mystery number
            offset.add(2);
        }

        String name = Identifiers.parseUnqualified(input, offset);

        inoutOffset.set(offset.get());
        return new NamedLocal(name, mysteryNumber.intValue());
    }

    @Override
    public String toString() {
        return "::" + name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NamedLocal))
            return false;

        return ((NamedLocal) o).name.equals(name);
    }
}
