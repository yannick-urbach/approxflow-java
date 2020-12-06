import urbachyannick.approxflow.*;

public class Program {

    @MethodOfInterest
    public static long doSomething(int param1, boolean param2) {
        return OtherClass.doSomething(param1, param2);
    }
}