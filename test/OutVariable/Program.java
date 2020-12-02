import urbachyannick.approxflow.*;

public class Program {

    @PublicOutput
    public static int out;

    @PrivateInput
    public static int in() {
        return 5;
    }

    public static void main(String[] args) {
        out = in();
    }
}