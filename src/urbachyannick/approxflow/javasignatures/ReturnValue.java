package urbachyannick.approxflow.javasignatures;

public class ReturnValue extends FunctionCallVariable {
    public static ReturnValue tryParse(String input, MutableInteger inoutOffset) {
        if (!ParseUtil.checkConstant(input, "#return_value", inoutOffset))
            return null;

        return new ReturnValue();
    }

    @Override
    public String toString() {
        return "#return_value";
    }

    @Override
    public int hashCode() {
        return ReturnValue.class.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ReturnValue;
    }
}
