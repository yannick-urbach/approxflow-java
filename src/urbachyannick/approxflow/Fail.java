package urbachyannick.approxflow;

public class Fail extends RuntimeException {
    public Fail(String message) {
        super("FAIL: " + message);
    }

    public Fail(String message, Exception reason) {
        super("FAIL: " + message);
    }
}
