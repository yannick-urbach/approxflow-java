package urbachyannick.approxflow.javasignatures;

import java.text.ParseException;

public abstract class Signature {
    public static Signature parse(String input) {
        if (!input.startsWith("java::"))
            return new UnparsedSignature(input);

        MutableInteger offset = new MutableInteger(6); // "java::".length()

        try {
            ClassName className = ClassName.parseWithMember(input, offset);
            MemberAccess memberAccess = MemberAccess.parse(input, offset);
            VariableIndices indices = VariableIndices.parse(input, offset);

            if (offset.get() != input.length())
                return new UnparsedSignature(input);

            return new ParsedSignature(className, memberAccess, indices);
        } catch (ParseException e) {
            return new UnparsedSignature(input);
        }
    }
}
