import urbachyannick.approxflow.*;

public class Program {

    @PrivateInput
    public static boolean in() {
        return true;
    }

    public static void out1(@PublicOutput(maxInstances = 5) boolean value) {
        System.out.println(value);
    }

    public static void out2(@PublicOutput(maxInstances = 5) boolean value) {
        System.out.println(value);
    }

    public static void main(String[] args) {
        if (in()) {
            out1(true);
            out2(true);
        } else {
            out2(true);
            out1(true);
        }
    }
}