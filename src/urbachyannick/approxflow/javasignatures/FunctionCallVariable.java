package urbachyannick.approxflow.javasignatures;

public abstract class FunctionCallVariable {
    public static FunctionCallVariable parse(String input, MutableInteger inoutOffset) {
        FunctionCallVariable variable = ReturnValue.tryParse(input, inoutOffset);

        if (variable == null)
            variable = AnonymousLocal.tryParse(input, inoutOffset);

        if (variable == null)
            variable = AnonymousParameter.tryParse(input, inoutOffset);

        if (variable == null)
            variable = NamedLocal.tryParse(input, inoutOffset);

        return variable;
    }
}
