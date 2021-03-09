import urbachyannick.approxflow.*;

@Unroll(iterations = 0)
@BlackboxLoops
public class Program {

    @PublicOutput
    public static int out;

    @PrivateInput
    public static int in() {
        return 5;
    }

    public static void main(String[] args) {
        int i = in();
        int j = 0;

        for (int k = 0; k < i; ++k)
            ++j;

        j = j & 0b101;

        int l = 0;

        for (int m = 0; m < j; ++m)
            ++l;

        out = l;
    }
}