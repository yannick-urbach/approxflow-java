import urbachyannick.approxflow.*;

public class Program {

    public static class SomeClass {
        private boolean field1;
        private boolean field2;

        @Invariant
        public boolean fieldsEqual() {
            return field1 == field2;
        }

        public boolean get1() { return field1; }
        public boolean get2() { return field2; }
    }

    @PrivateInput
    public static SomeClass in() {
        SomeClass i = new SomeClass();
        return i;
    }

    public static void out(@PublicOutput(maxInstances = 5) boolean value) {
        System.out.println(value);
    }

    public static void main(String[] args) {
        SomeClass i = in();
        out(i.get1());
        out(i.get2());
    }
}