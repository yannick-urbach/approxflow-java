package urbachyannick.approxflow;

import urbachyannick.approxflow.javasignatures.JavaSignature;
import urbachyannick.approxflow.javasignatures.Signature;
import urbachyannick.approxflow.javasignatures.UnparsedSignature;

import java.util.function.IntUnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A variable line in a CNF file. Parsing of the literals is deferred to allow for faster search for a given signature.
 */
public class CnfVarLine {
    private final Signature signature;
    private final String literals;

    /**
     * Creates a variable line from its string representation.
     * @param line the string representation
     */
    public CnfVarLine(String line) {
        String[] parts = line.split(" ", 3);

        signature = Signature.parse(parts[1]);
        this.literals = parts[2];
    }

    /**
     * Creates a variable line from a signature, a modification index and literals
     * @param signature the signature
     * @param literals the literals
     */
    public CnfVarLine(Signature signature, IntStream literals) {
        this.signature = signature;
        this.literals = literals.mapToObj(CnfLiteral::toString).collect(Collectors.joining(" "));
    }

    /**
     * Gets the signature of this variable line, excluding the modifications index
     * @return the signature, excluding the modifications index
     */
    public Signature getSignature() {
        return signature;
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
        return new CnfVarLine(signature, getLiterals().map(renameFunction));
    }

    /**
     * Compares the generation indices of two variable lines
     * @param left the first variable line
     * @param right the second variable line
     * @return -1, 0 or 1 as the generation index of left is lesser, equal to, or greater than that of right, or 0 if either of the signatures is unparsed
     */
    public static int compareByGeneration(CnfVarLine left, CnfVarLine right) {
        if (left.signature instanceof UnparsedSignature || right.signature instanceof UnparsedSignature)
            return 0;

        return Integer.compare(
                ((JavaSignature)left.getSignature()).getIndices().getGeneration(),
                ((JavaSignature)right.getSignature()).getIndices().getGeneration()
        );
    }

    /**
     * Gets the string representation of this variable line
     * @return the string representation of this variable line
     */
    @Override
    public String toString() {
        return "c " + signature + " " + literals;
    }
}
