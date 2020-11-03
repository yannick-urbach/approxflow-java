package urbachyannick.approxflow.javasignatures;

import java.text.ParseException;

public class ParseUtil {
    public static int parseNumber(String input, MutableInteger inoutOffset) throws ParseException {
        int start = inoutOffset.get();
        int offset = inoutOffset.get();

        while (Character.isDigit(input.charAt(offset)))
            ++offset;

        if (start == offset)
            throw new ParseException("Expected digit, got \"" + input.charAt(offset) + "\"", offset);

        inoutOffset.set(offset);

        return Integer.parseInt(input.substring(start, offset));
    }

    public static Integer tryParseNumber(String input, MutableInteger inoutOffset) {
        int start = inoutOffset.get();
        int offset = inoutOffset.get();

        while (Character.isDigit(input.charAt(offset)))
            ++offset;

        if (start == offset)
            return null;

        inoutOffset.set(offset);

        return Integer.valueOf(input.substring(start, offset));
    }
}
