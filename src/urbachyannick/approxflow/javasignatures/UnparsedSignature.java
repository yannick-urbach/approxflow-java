package urbachyannick.approxflow.javasignatures;

public class UnparsedSignature extends Signature {
    private String text;

    public UnparsedSignature(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return text;
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UnparsedSignature))
            return false;

        return ((UnparsedSignature) o).text.equals(text);
    }

    @Override
    public boolean matches(Signature signature) {
        return equals(signature);
    }
}
