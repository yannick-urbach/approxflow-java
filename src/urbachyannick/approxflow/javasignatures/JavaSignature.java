package urbachyannick.approxflow.javasignatures;

import java.util.Objects;

public class JavaSignature extends Signature {
    private final ClassName className;
    private final MemberAccess memberAccess;
    private final VariableIndices indices;

    public JavaSignature(ClassName className, MemberAccess memberAccess, VariableIndices indices) {
        this.className = className;
        this.memberAccess = memberAccess;
        this.indices = indices;
    }

    public JavaSignature(ClassName className, MemberAccess memberAccess) {
        this.className = className;
        this.memberAccess = memberAccess;
        this.indices = new VariableIndices(-1, -1, -1);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof JavaSignature))
            return false;

        JavaSignature other = (JavaSignature) o;
        return other.className.equals(className) && other.memberAccess.equals(memberAccess) && other.indices.equals(indices);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, memberAccess, indices);
    }

    public VariableIndices getIndices() {
        return indices;
    }

    @Override
    public boolean matches(Signature other) {
        if (!(other instanceof JavaSignature))
            return false;

        JavaSignature o = (JavaSignature) other;

        return className.equals(o.className) && memberAccess.equals(o.memberAccess);
    }

    @Override
    public String toString() {
        return "java::" + className.asQualifiedName() + memberAccess.toString() + indices.toString();
    }

    public static Signature tryParse(String input) {
        MutableInteger offset = new MutableInteger(0);

        if (!ParseUtil.checkConstant(input, "java::", offset))
            return null;

        try {
            ClassName className = ClassName.parseWithMember(input, offset);
            MemberAccess memberAccess = MemberAccess.parse(input, offset);
            VariableIndices indices = VariableIndices.parse(input, offset);

            if (offset.get() != input.length())
                return null;

            return new JavaSignature(className, memberAccess, indices);
        } catch (SignatureParseException e) {
            return null;
        }
    }
}
