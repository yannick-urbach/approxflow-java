package urbachyannick.approxflow.javasignatures;

import java.text.ParseException;

public abstract class MemberAccess {
    public static MemberAccess parse(String input, MutableInteger inoutOffset) {
        MemberAccess memberAccess = FunctionCall.tryParse(input, inoutOffset);

        if (memberAccess == null)
            memberAccess = FieldAccess.tryParse(input, inoutOffset);

        if (memberAccess == null)
            throw new SignatureParseException("Failed to parse as member access.", inoutOffset.get());

        return memberAccess;
    }
}
