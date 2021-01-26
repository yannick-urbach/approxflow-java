import urbachyannick.approxflow.*;

public class Program {
    @PrivateInput
    public static boolean in() {
        return true;
    }

    public static void main(String[] args) {
        Parent p = new Child();
        p.inlineThis(false, in()); // resolves to Child#inlineThis
    }
}