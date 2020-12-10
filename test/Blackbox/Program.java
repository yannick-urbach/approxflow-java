import urbachyannick.approxflow.*;

// run
//          approxflow [--keep-intermediate] --blackbox-experimental test/Blackbox

public class Program {
    /*

    ----- Explanation -----

        For each call to a blackbox method, the program is analyzed with the following
        -   public outputs:
            -   the parameters to this call
        -   private inputs:
            -   all marked private inputs (see current limitations)
            -   the return values of all previous calls to blackbox methods
        This results in the amount of information leaked FROM previous blackbox calls OR actual inputs INTO this
        blackbox method call.

        Subsequently, the program is analyzed once more with the following
        -   public outputs:
            -   all marked public outputs (see current limitations)
        -   private inputs:
            -   all marked private inputs (see current limitations)
            -   the return values of all calls to blackbox methods
        This results in the amount of information leaked FROM previous blackbox calls OR actual inputs INTO actual
        outputs.

        The minimum of those results is an upper bound to the information leaked (in fact, it should even be the exact
        maximum possible leakage among all possible blackbox method implementations).


    ----- Current limitations -----

        -   The EXACT number of times a blackbox method will be called MUST be given via the annotation.
        -   The code executed after a blackbox method call MUST NOT depend on any marked private inputs nor the return
            value of other blackbox method calls except through this blackbox method call.

     */


    @Blackbox(calls = 2)
    public static int blackbox(int input) {
        return input; // don't know what happens in here
    }

    @PrivateInput
    public static int in() {
        return 5;
    }

    public static void out(@PublicOutput(maxInstances = 5) int value) {

    }

    public static void main(String[] args) {
        int input = in();

        int intermediate1 = blackbox(input & 0b1111);

        int intermediate2 = blackbox(intermediate1 & 0b11);

        out(intermediate2);
    }


    /*
    effective transformed:

    public static int blackboxCallCounter = 0;

    public static int blackbox(int input) {
        if (blackboxCallCounter++ < partIndex) {
            return in();
        } else {
            out(input);
            throw new AssertionError();
        }
    }
    */
}