package urbachyannick.approxflow;

import java.util.function.IntUnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A clause line in a CNF file. Parsing is deferred.
 */
public class CnfClauseLine {
    private final String line;

    /**
     * Creates a clause line from its string representation.
     * @param line the string representation
     */
    public CnfClauseLine(String line) {
        this.line = line;
    }

    /**
     * Creates a clause line from the given literals.
     * @param literals the literals to include
     */
    public CnfClauseLine(IntStream literals) {
        line = literals.mapToObj(CnfLiteral::toString).collect(Collectors.joining(" ")) + " 0";
    }

    /**
     * Gets the literals in this clause line
     * @return the literals in this clause line
     */
    public IntStream getLiterals() {
        return Pattern.compile(" ")
                .splitAsStream(line)
                .mapToInt(CnfLiteral::parse)
                .filter(literal -> literal != 0)
                .distinct();
    }

    /**
     * Creates a new clause line that is identical to this clause line, except that all variables are renamed by passing
     * the literals through the given renaming function (called by {@link CnfFile#renameVariables(IntUnaryOperator)}).
     *
     * @param renameFunction the renaming function
     * @return the new clause line with renamed variables
     */
    public CnfClauseLine renameLiterals(IntUnaryOperator renameFunction) {
        return new CnfClauseLine(getLiterals().map(renameFunction));
    }

    /**
     * Gets the string representation of this clause line
     * @return the string representation of this clause line
     */
    @Override
    public String toString() {
        return line;
    }
}
