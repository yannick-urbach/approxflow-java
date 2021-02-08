import urbachyannick.approxflow.*;


public class Program {

    @PrivateInput
    public static boolean in() {
        return true;
    }

    public static void out(@PublicOutput(maxInstances = 0) boolean value) {
        System.out.println(value);
    }

    public static void main(String[] args) {
        boolean i = in();

        if (i)
            out(false);

        out(i);

        if (!i)
            out(true);
    }
}
