import urbachyannick.approxflow.*;

public class Program {

    @PublicOutput
    public static boolean out;

    @PrivateInput
    public static boolean privateIn() {
        return false;
    }

    @PublicInput
    public static boolean publicIn() {
        return true;
    }

    public static void main(String[] args) {
        out = privateIn() || publicIn();
    }
}