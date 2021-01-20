import urbachyannick.approxflow.*;

public class Program {

    @PrivateInput
    public static byte in() {
        return 5;
    }

    public static void out(@PublicOutput(maxInstances = 3) boolean value) {
        System.out.println(value);
    }

    @Unroll(iterations = 3)
    public static void main(String[] args) {
        int count = in() & 0x3; // limit to 0..3

        for (int i = 0; i < count; ++i)
            out(false);
    }
}