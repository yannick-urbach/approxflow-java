import urbachyannick.approxflow.*;

public class Program {

    @PublicOutput
    public static int out;

    @PrivateInput
    public static int in() {
        return 5;
    }

    @Blackbox
    public static int blackbox(int i) {
        return 5;
    }

    public static void main(String[] args) {
        int i = in();
        int j = blackbox(i & 0b0011);
        int k = blackbox((j & 0b0011) | (i & 0b1100));

        out = k;
    }
}