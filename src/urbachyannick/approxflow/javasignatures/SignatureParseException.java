package urbachyannick.approxflow.javasignatures;

public class SignatureParseException extends RuntimeException {
    public SignatureParseException(String message, int offset) {
        super(message + " (at " + offset + ")");
    }
}
