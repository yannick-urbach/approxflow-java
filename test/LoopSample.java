import urbachyannick.approxflow.*;
import static urbachyannick.approxflow.Loops.*;

public class LoopSample {

    public static void out(@PublicOutput(maxInstances = 10) boolean value) {
        System.out.println(value);
    }

    @PrivateInput
    public static boolean in() {
        return true;
    }

    public static void main(String[] args) {
        // should of course unroll at least 5 times, but this demonstrates that unrolling works
        unroll(2);
        for (int i = 0; i < 5; ++i) {
            out(in());
        }
    }
}