package urbachyannick.approxflow.javasignatures;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FunctionCall extends MemberAccess {
    private final String name;
    private final TypeSpecifier[] parameterTypes;
    private final TypeSpecifier returnType;
    private final FunctionCallVariable variable;

    public FunctionCall(String name, TypeSpecifier[] parameterTypes, TypeSpecifier returnType, FunctionCallVariable variable) {
        this.name = name;
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
        this.variable = variable;
    }

    public static FunctionCall tryParse(String input, MutableInteger inoutOffset) throws ParseException {
        input = input + "  "; // sentinel

        MutableInteger offset = new MutableInteger(inoutOffset);

        if (input.charAt(offset.get()) != '.')
            return null;

        offset.increment();

        String name = Identifiers.tryParseUnqualified(input, offset);

        if (name == null) {
            if (input.regionMatches(offset.get(), "<clinit>", 0, 8)) {
                name = "<clinit>";
                offset.add(8); // name.length()
            } else if (input.regionMatches(offset.get(), "<init>", 0, 6)) {
                name = "<init>";
                offset.add(6); // name.length()
            } else {
                return null;
            }
        }

        if (!input.regionMatches(offset.get(), ":(", 0, 2))
            return null;

        offset.add(2);

        List<TypeSpecifier> parameterTypes = new ArrayList<>();

        while (input.charAt(offset.get()) != ')')
            parameterTypes.add(TypeSpecifier.parse(input, offset));

        offset.increment();

        TypeSpecifier returnType = TypeSpecifier.parse(input, offset);
        FunctionCallVariable variable = FunctionCallVariable.parse(input, offset);

        inoutOffset.set(offset.get());
        return new FunctionCall(name, parameterTypes.toArray(new TypeSpecifier[0]), returnType, variable);
    }

    @Override
    public String toString() {
        return String.format(".%s:(%s)%s%s",
                name,
                Arrays  .stream(parameterTypes)
                        .map(TypeSpecifier::asTypeSpecifierString)
                        .collect(Collectors.joining()),
                returnType.asTypeSpecifierString(),
                variable.toString()
        );
    }
}
