package urbachyannick.approxflow.javasignatures;

public abstract class Signature {
    public static Signature parse(String input) {
        Signature signature = JavaSignature.tryParse(input);

        if (signature == null)
            signature = DynamicArraySignature.tryParse(input);

        if (signature == null)
            signature = new UnparsedSignature(input);

        return signature;
    }

    public abstract boolean matches(Signature signature);
}
