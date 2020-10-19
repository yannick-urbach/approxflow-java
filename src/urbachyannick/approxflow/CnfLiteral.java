package urbachyannick.approxflow;

/**
 * Utility class for handling CNF literals. Basically just takes care of the special TRUE and FALSE literals in variable
 * lines.
 */
public class CnfLiteral {
    /**
     * Special value to represent true; occurs only in variable lines
     */
    public static final int TRUE = Integer.MAX_VALUE;

    /**
     * Special value to represent false; occurs only in variable lines
     */
    public static final int FALSE = -TRUE;

    /**
     * Parses a CNF literal, taking care of the special TRUE and FALSE literals.
     * @param literal the string to parse
     * @return the parsed literal
     */
    public static int parse(String literal) {
        return
                literal.equals("TRUE")  ? TRUE :
                literal.equals("FALSE") ? FALSE :
                /* else */                Integer.parseInt(literal);
    }

    /**
     * Converts a CNF literal to its representation, taking care of the special TRUE and FALSE literals.
     * @param literal the literal to convert
     * @return the string representation
     */
    public static String toString(int literal) {
        return
                literal == TRUE  ? "TRUE" :
                literal == FALSE ? "FALSE" :
                /* else */         Integer.toString(literal);
    }

    /**
     * Checks whether a given CNF literal is non-trivial (i.e. neither {@link #TRUE} nor {@link #FALSE}).
     * @param literal the literal to check
     * @return false, if the literal is either TRUE or FALSE, true otherwise.
     */
    public static boolean isNonTrivial(int literal) {
        return literal != TRUE && literal != FALSE;
    }
}
