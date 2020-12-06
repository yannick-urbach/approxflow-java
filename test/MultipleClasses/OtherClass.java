public class OtherClass {
    public static long doSomething(int param1, boolean param2) {
        return (param1 & 0x00FF00FF) | (param2 ? 0xFF00FF00 : 0);
    }
}