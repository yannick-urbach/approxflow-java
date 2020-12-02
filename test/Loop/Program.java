import urbachyannick.approxflow.*;

@Unroll(iterations = 10)
public class Program {

    public static void out(@PublicOutput(maxInstances = 10) boolean value) {
        System.out.println(value);
    }

    @PrivateInput
    public static boolean in() {
        return true;
    }

    public static void main(String[] args) {
        for (int i = 0; i < 5; ++i)
            out(in());
    }
}