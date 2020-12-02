import urbachyannick.approxflow.*;

public class Program {
    @PublicOutput
    private static boolean outB;
    @PublicOutput
    private static boolean outA;

    @PrivateInput
    public static boolean in() {
        return true;
    }

    public static void main(String[] args) {
        outA = in();
        outB = in();

        assert outA != outB;
    }
}