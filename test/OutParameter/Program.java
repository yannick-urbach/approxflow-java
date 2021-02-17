import urbachyannick.approxflow.*;

public class Program {

    @PrivateInput
    public static boolean in() {
        return true;
    }

    public static void out(@PublicOutput boolean value) {
        System.out.println(value);
    }

    public static void main(String[] args) {
        out(in());

        if (in()) {
            out(true);
        } else {
            out(false);
        }
    }
}