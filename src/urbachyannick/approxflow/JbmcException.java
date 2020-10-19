package urbachyannick.approxflow;

public class JbmcException extends Exception {
    public JbmcException(String message) {
        super("Error during JBMC call: " + message);
    }

    public JbmcException(String message, Throwable inner) {
        super("Error during JBMC call: " + message, inner);
    }
}
