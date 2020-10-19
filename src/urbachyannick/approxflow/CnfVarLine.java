package urbachyannick.approxflow;

import java.util.function.IntUnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A variable line in a CNF file. Parsing of the literals is deferred to allow for faster search for a given signature.
 * This is simplified for now, everything in the signature except the modification index is just a string. Should parse
 * into a path-like structure in the future instead.
 */
public class CnfVarLine {
    private final String signature;
    private final int modificationIndex;
    private final String literals;

    /**
     * Creates a variable line from its string representation.
     * @param line the string representation
     */
    public CnfVarLine(String line) {
        String[] parts = line.split(" ", 3);

        if (parts[1].matches(".*?#\\d+")) {
            int separatorIndex = parts[1].lastIndexOf('#');
            signature = parts[1].substring(0, separatorIndex);
            modificationIndex = Integer.parseInt(parts[1].substring(separatorIndex + 1));
        } else {
            signature = parts[1];
            modificationIndex = 0;
        }

        this.literals = parts[2];
    }

    /**
     * Creates a variable line from a signature, a modification index and literals
     * @param signature the signature, excluding the modifications index
     * @param modificationIndex the modifications index
     * @param literals the literals
     */
    public CnfVarLine(String signature, int modificationIndex, IntStream literals) {
        this.signature = signature;
        this.modificationIndex = modificationIndex;
        this.literals = literals.mapToObj(CnfLiteral::toString).collect(Collectors.joining(" "));
    }

    /**
     * Gets the signature of this variable line, excluding the modifications index
     * @return the signature, excluding the modifications index
     */
    public String getSignature() {
        return signature;
    }

    /**
     * Gets the modifications index of this variable line
     * @return the modifications index
     */
    public int getModificationIndex() {
        return modificationIndex;
    }

    /**
     * Gets the literals in this variable line
     * @return the literals in this variable line
     */
    public IntStream getLiterals() {
        return Pattern.compile(" ")
                .splitAsStream(literals)
                .mapToInt(CnfLiteral::parse);
    }

    /**
     * Creates a new variable line that is identical to this variable line, except that all variables are renamed by
     * passing the literals through the given renaming function (called by
     * {@link CnfFile#renameVariables(IntUnaryOperator)}).
     *
     * @param renameFunction the renaming function
     * @return the new variable line with renamed variables
     */
    public CnfVarLine renameLiterals(IntUnaryOperator renameFunction) {
        return new CnfVarLine(signature, modificationIndex, getLiterals().map(renameFunction));
    }

    /**
     * Compares the modification indices of two variable lines
     * @param left the first variable line
     * @param right the second variable line
     * @return -1, 0 or 1 as the modification index of left is lesser, equal to, or greater than that of right
     */
    public static int compareByIndex(CnfVarLine left, CnfVarLine right) {
        return Integer.compare(left.getModificationIndex(), right.getModificationIndex());
    }

    /**
     * Gets the string representation of this variable line
     * @return the string representation of this variable line
     */
    @Override
    public String toString() {
        return "c " + signature + (modificationIndex == 0 ? "" : "#" + modificationIndex) + " " + literals;
    }
}
