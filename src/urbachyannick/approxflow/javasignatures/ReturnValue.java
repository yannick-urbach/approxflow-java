package urbachyannick.approxflow.javasignatures;

public class ReturnValue extends FunctionCallVariable {
    public static ReturnValue tryParse(String input, MutableInteger inoutOffset) {
        if (!input.regionMatches(inoutOffset.get(), "#return_value", 0, 13))
            return null;

        inoutOffset.add(13); // "#return_value".length()
        return new ReturnValue();
    }

    @Override
    public String toString() {
        return "#return_value";
    }
}
