import urbachyannick.approxflow.*;

public class OutArraySample {

    @PublicOutput
    public static boolean[] outArray = new boolean[5];

    @PrivateInput
    public static boolean in() {
        return true;
    }

    public static void main(String[] args) {
        outArray[0] = in();
        outArray[1] = in();
        outArray[2] = in();
        outArray[0] = in();
        outArray[1] = in();
    }
}