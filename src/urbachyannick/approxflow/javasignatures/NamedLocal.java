package urbachyannick.approxflow.javasignatures;

public class NamedLocal extends FunctionCallVariable {
    private String name;
    private Long mysteryNumber; // somehow related to "start" in the local variable table, but 1 lower for some reason?

    public NamedLocal(String name) {
        this.name = name;
        mysteryNumber = 0L;
    }

    private NamedLocal(String name, Long mysteryNumber) {
        this.name = name;
        this.mysteryNumber = mysteryNumber;
    }

    public static NamedLocal tryParse(String input, MutableInteger inoutOffset) {
        MutableInteger offset = new MutableInteger(inoutOffset);

        if (!ParseUtil.checkConstant(input, "::", offset))
            return null;

        MutableInteger beforeNumber = new MutableInteger(offset);

        Long mysteryNumber = ParseUtil.tryParseNumber(input, offset);

        if (mysteryNumber != null && !ParseUtil.checkConstant(input, "::", offset)) {
            // actually part of the name (is that possible? probably not...)
            offset = beforeNumber;
            mysteryNumber = null;
        }

        String name = Identifiers.parseUnqualified(input, offset);

        inoutOffset.set(offset.get());
        return new NamedLocal(name, mysteryNumber);
    }

    @Override
    public String toString() {
        return (mysteryNumber == null ? "" : "::" + mysteryNumber) + "::" + name;
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
