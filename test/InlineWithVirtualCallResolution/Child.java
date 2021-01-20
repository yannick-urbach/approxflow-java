import urbachyannick.approxflow.*;

public class Child extends Parent {
    @PublicOutput
    public static boolean out;

    public void inlineThis(boolean b1, boolean b2) {
        out = b2;
    }
}