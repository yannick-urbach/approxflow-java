package urbachyannick.approxflow.javasignatures;

import java.util.HashSet;

public class ParseUtil {
    private static final HashSet<Character> hexDigits = new HashSet<Character>() {{
        add('0'); add('1'); add('2'); add('3'); add('4'); add('5'); add('6'); add('7'); add('8'); add('9');
        add('A'); add('B'); add('C'); add('D'); add('E'); add('F');
        add('a'); add('b'); add('c'); add('d'); add('e'); add('f');
    }};
    
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

    public static long parseHexNumber(String input, MutableInteger inoutOffset) {
        int start = inoutOffset.get();
        int offset = inoutOffset.get();

        while (hexDigits.contains(input.charAt(offset)))
            ++offset;

        if (start == offset)
            throw new SignatureParseException("Expected hex digit, got \"" + input.charAt(offset) + "\"", offset);

        inoutOffset.set(offset);

        return Long.parseLong(input.substring(start, offset), 16);
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
