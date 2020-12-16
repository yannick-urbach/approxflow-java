import urbachyannick.approxflow.*;

// Run with --soot-experimental
//
// Soot does something with local variable slots that JBMC doesn't like. Reusing a slot with a different type, I
// believe.
//
// Problem: Soot doesn't seem to have an option to turn off slot recycling
//
// Variable slot handling is in
//      https://github.com/soot-oss/soot/blob/master/src/main/java/soot/baf/BafASMBackend.java line 223

public class Program {
    public static int someInt() { return 5; }

    public static void main(String[] args) {
        int i = someInt();
        System.out.println(i);

        // System.out.println(args[0]); // Uncommenting this forces i to take a new slot, and everything works fine
    }
}