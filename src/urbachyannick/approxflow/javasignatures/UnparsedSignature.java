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
}
