package urbachyannick.approxflow.javasignatures;

import java.util.Objects;

public class ParsedSignature extends Signature {
    private final ClassName className;
    private final MemberAccess memberAccess;
    private final VariableIndices indices;

    public ParsedSignature(ClassName className, MemberAccess memberAccess, VariableIndices indices) {
        this.className = className;
        this.memberAccess = memberAccess;
        this.indices = indices;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ParsedSignature))
            return false;

        ParsedSignature other = (ParsedSignature) o;
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
    public String toString() {
        return className.asQualifiedName() + memberAccess.toString() + indices.toString();
    }
}
