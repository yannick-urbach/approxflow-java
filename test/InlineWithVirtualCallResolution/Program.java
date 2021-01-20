import urbachyannick.approxflow.*;

@Inline(recursions = 2)
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