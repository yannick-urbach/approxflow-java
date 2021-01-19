package urbachyannick.approxflow.codetransformation;

public class InvalidTransformationException extends Exception {
    public InvalidTransformationException(String message) {
        super(message);
    }

    public InvalidTransformationException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidTransformationException(Throwable cause) {
        super(cause);
    }
}
