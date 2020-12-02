import urbachyannick.approxflow.*;

public class Program {

    @PublicOutput
    public static boolean[] out = new boolean[5];

    @PrivateInput
    public static boolean in() {
        return false;
    }

    public static void main(String[] args) {
        out[0] = in();
        out[1] = in();
        out[2] = in();
        out[0] = in();
        out[1] = in();
    }
}