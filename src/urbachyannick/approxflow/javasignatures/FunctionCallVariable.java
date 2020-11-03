package urbachyannick.approxflow.javasignatures;

import java.text.ParseException;

public abstract class FunctionCallVariable {
    public static FunctionCallVariable parse(String input, MutableInteger inoutOffset) throws ParseException {
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
