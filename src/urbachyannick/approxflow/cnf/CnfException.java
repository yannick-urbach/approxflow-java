package urbachyannick.approxflow.cnf;

public class CnfException extends Exception {
    public CnfException (String message, Throwable cause) {
        super(message, cause);
    }

    public CnfException (String message) {
        super(message);
    }
}
