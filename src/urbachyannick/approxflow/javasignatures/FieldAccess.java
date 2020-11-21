package urbachyannick.approxflow.javasignatures;

import java.text.ParseException;

public class FieldAccess extends MemberAccess {
    private String name;

    public FieldAccess(String name) {
        this.name = name;
    }

    public static FieldAccess tryParse(String input, MutableInteger inoutOffset) {
        MutableInteger offset = new MutableInteger(inoutOffset);

        if (!ParseUtil.checkConstant(input, ".", offset))
            return null;

        String name = Identifiers.parseUnqualified(input, offset);

        inoutOffset.set(offset.get());
        return new FieldAccess(name);
    }

    @Override
    public String toString() {
        return "." + name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FieldAccess))
            return false;

        return ((FieldAccess) o).name.equals(name);
    }
}
