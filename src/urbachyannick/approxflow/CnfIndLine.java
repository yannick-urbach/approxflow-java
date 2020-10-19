package urbachyannick.approxflow;

import java.util.function.IntUnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * An ApproxMC scope line in a CNF file. Parsing is deferred.
 */
public class CnfIndLine {
    private final String line;

    /**
     * Creates a scope line from its string representation.
     * @param line the string representation
     */
    public CnfIndLine(String line) {
        this.line = line;
    }

    /**
     * Creates a scope line from the given variables.
     * @param literals the variables to include
     */
    public CnfIndLine(IntStream literals) {
        line = "c ind " + literals.mapToObj(CnfLiteral::toString).collect(Collectors.joining(" ")) + " 0";
    }

    /**
     * Gets the variables in this scope line
     * @return the variables in this scope line
     */
    public IntStream getLiterals() {
        return Pattern.compile(" ")
                .splitAsStream(line)
                .skip(2)
                .mapToInt(CnfLiteral::parse)
                .filter(literal -> literal != 0)
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
     * Gets the string representation of this clause line
     * @return the string representation of this scope line
     */
    @Override
    public String toString() {
        return line;
    }
}
