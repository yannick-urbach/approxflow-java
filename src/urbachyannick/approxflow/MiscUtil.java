package urbachyannick.approxflow;

import urbachyannick.approxflow.cnf.MappingValue;
import urbachyannick.approxflow.cnf.TrivialMappingValue;

import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MiscUtil {
    // Weirdly seems to be in 16 bit words, most significant 16-bit word first, but least significant bit first within words
    public static long parseAddressFromTrivialLiterals(Stream<TrivialMappingValue> literals) {
        Iterator<TrivialMappingValue> iterator = literals.iterator();

        long result = 0;

        for (int word = 0; word < 4; ++word) {
            for (int bit = 0; bit < 16; ++bit) {
                if (!iterator.hasNext())
                    throw new IllegalArgumentException("Must have exactly 64 literals");

                TrivialMappingValue literal = iterator.next();

                result |= (long)(literal.get() ? 1 : 0) << (16 * (3 - word)) << bit;
            }
        }

        if (iterator.hasNext())
            throw new IllegalArgumentException("Must have exactly 64 literals");

        return result;
    }
}
