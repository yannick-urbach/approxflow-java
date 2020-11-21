package urbachyannick.approxflow.javasignatures;

import java.text.ParseException;

public class ParseUtil {
    public static long parseNumber(String input, MutableInteger inoutOffset) {
        int start = inoutOffset.get();
        int offset = inoutOffset.get();

        while (Character.isDigit(input.charAt(offset)))
            ++offset;

        if (start == offset)
            throw new SignatureParseException("Expected digit, got \"" + input.charAt(offset) + "\"", offset);

        inoutOffset.set(offset);

        return Long.parseLong(input.substring(start, offset));
    }

    public static Long tryParseNumber(String input, MutableInteger inoutOffset) {
        int start = inoutOffset.get();
        int offset = inoutOffset.get();

        while (Character.isDigit(input.charAt(offset)))
            ++offset;

        if (start == offset)
            return null;

        inoutOffset.set(offset);

        return Long.valueOf(input.substring(start, offset));
    }

    public static boolean checkConstant(String input, String constant, MutableInteger offset) {
        if (!input.regionMatches(offset.get(), constant, 0, constant.length()))
            return false;

        offset.add(constant.length());
        return true;
    }
}
