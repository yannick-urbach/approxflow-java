import urbachyannick.approxflow.*;

public class Program {

    public static void out(@PublicOutput(maxInstances = 10) boolean value) {
        System.out.println(value);
    }

    @PrivateInput
    public static boolean in() {
        return true;
    }

    @InlineMethod(recursions = 5)
    public static void inline(int i) {
        if (i <= 0)
            return;

        inline(i - 1);
        inline(i - 2);
        out(in());
    }

    public static void main(String[] args) {
        inline(3);
    }
}