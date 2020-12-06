package urbachyannick.approxflow.codetransformation;

public class CompilationError extends Exception {
    public CompilationError(String message, Throwable cause) {
        super(message, cause);
    }

    public CompilationError(String message) {
        super(message);
    }
}
