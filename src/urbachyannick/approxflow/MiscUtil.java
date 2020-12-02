package urbachyannick.approxflow;

import urbachyannick.approxflow.cnf.TrivialMappingValue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Optional;
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

    // available as member function in Java 9, but not in Java 8
    public static <T> Optional<T> or(Optional<T> o1, Optional<T> o2) {
        if (o1.isPresent())
            return o1;
        else
            return o2;
    }

    public static String throwableToString(Throwable t) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        t.printStackTrace(printWriter);
        return stringWriter.toString();
    }
}
