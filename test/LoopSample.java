import urbachyannick.approxflow.*;
import static urbachyannick.approxflow.Loops.*;

// should of course unroll at least 5 times, but this demonstrates that unrolling works

// method annotation overrides class annotation, and "loop annotation" overrides method annotation.

@Unroll(iterations = 1)
public class LoopSample {

    public static void out(@PublicOutput(maxInstances = 10) boolean value) {
        System.out.println(value);
    }

    @PrivateInput
    public static boolean in() {
        return true;
    }

    @Unroll(iterations = 2)
    public static void main(String[] args) {

        unroll(3);
        for (int i = 0; i < 5; ++i) {
            out(in());
        }
    }
}