import urbachyannick.approxflow.*;

public class OutMethodSample {

    @PrivateInput
    public static boolean in() {
        return true;
    }

    public static void out(@PublicOutput(maxInstances = 5) boolean value) {
        System.out.println(value);
    }

    public static void main(String[] args) {
        out(in());

        if (in()) {
            out(true);
        } else {
            out(false);
        }
    }
}