package urbachyannick.approxflow.javasignatures;

import java.util.*;
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

    public static FunctionCall tryParse(String input, MutableInteger inoutOffset) {
        input = input + "  "; // sentinel

        MutableInteger offset = new MutableInteger(inoutOffset);

        if (input.charAt(offset.get()) != '.')
            return null;

        offset.increment();

        String name = Identifiers.tryParseUnqualified(input, offset);

        if (name == null) {
            if (ParseUtil.checkConstant(input, "<clinit>", offset))
                name = "<clinit>";
            else if (ParseUtil.checkConstant(input, "<init>", offset))
                name = "<init>";
            else
                return null;
        }

        if (!ParseUtil.checkConstant(input, ":(", offset))
            return null;

        List<TypeSpecifier> parameterTypes = new ArrayList<>();

        while (!ParseUtil.checkConstant(input, ")", offset))
            parameterTypes.add(TypeSpecifier.parse(input, offset));

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

    @Override
    public int hashCode() {
        return Objects.hash(name, Arrays.hashCode(parameterTypes), returnType, variable);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof FunctionCall))
            return false;

        FunctionCall o = (FunctionCall) other;

        return (
                name.equals(o.name) &&
                Arrays.equals(parameterTypes, o.parameterTypes) &&
                returnType.equals(o.returnType) &&
                variable.equals(o.variable)
        );
    }
}
