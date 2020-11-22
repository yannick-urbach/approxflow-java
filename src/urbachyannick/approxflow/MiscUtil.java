package urbachyannick.approxflow;

import java.util.PrimitiveIterator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class MiscUtil {

    // Weirdly seems to be in 16 bit words, most significant 16-bit word first, but least significant bit first within words
    public static long parseAddressFromTrivialLiterals(IntStream literals) {
        PrimitiveIterator.OfInt iterator = literals.iterator();

        long result = 0;

        for (int word = 0; word < 4; ++word) {
            for (int bit = 0; bit < 16; ++bit) {
                if (!iterator.hasNext())
                    throw new IllegalArgumentException("Must have exactly 64 literals");

                int literal = iterator.next();

                if (CnfLiteral.isNonTrivial(literal))
                    throw new IllegalArgumentException("Literals must be trivial (TRUE or FALSE)");

                result |= (long)(literal == CnfLiteral.TRUE ? 1 : 0) << (16 * (3 - word)) << bit;
            }
        }

        if (iterator.hasNext())
            throw new IllegalArgumentException("Must have exactly 64 literals");

        return result;
    }
}
