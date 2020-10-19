package urbachyannick.approxflow;

import java.util.function.IntUnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * An ApproxMC-py scope line in a CNF file. Parsing is deferred.
 */
public class CnfCrLine {
    private final String line;

    /**
     * Creates a scope line from its string representation.
     * @param line the string representation
     */
    public CnfCrLine(String line) {
        this.line = line;
    }

    /**
     * Creates a scope line from the given variables.
     * @param literals the variables to include
     */
    public CnfCrLine(IntStream literals) {
        line = "cr " + literals.mapToObj(CnfLiteral::toString).collect(Collectors.joining(" "));
    }

    /**
     * Gets the variables in this scope line
     * @return the variables in this scope line
     */
    public IntStream getLiterals() {
        return Pattern.compile(" ")
                .splitAsStream(line)
                .skip(1)
                .mapToInt(CnfLiteral::parse)
                .distinct();
    }

    /**
     * Creates a new scope line that is identical to this scope line, except that all variables are renamed by passing
     * them through the given renaming function (called by {@link CnfFile#renameVariables(IntUnaryOperator)}).
     *
     * @param renameFunction the renaming function
     * @return the new scope line with renamed variables
     */
    public CnfClauseLine renameLiterals(IntUnaryOperator renameFunction) {
        return new CnfClauseLine(getLiterals().map(renameFunction));
    }

    /**
     * Gets the string representation of this scope line
     * @return the string representation of this scope line
     */
    @Override
    public String toString() {
        return line;
    }
}
