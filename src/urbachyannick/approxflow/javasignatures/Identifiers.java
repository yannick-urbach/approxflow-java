package urbachyannick.approxflow.javasignatures;

import java.util.*;

public class Identifiers {
    public static String tryParseUnqualified(String input, MutableInteger inoutOffset) {
        if (Character.isJavaIdentifierPart(input.charAt(input.length() - 1)))
            input = input + " "; // sentinel

        int offset = inoutOffset.get();
        int oldOffset = inoutOffset.get();

        if (Character.isJavaIdentifierStart(input.charAt(offset)))
            ++offset;

        while(Character.isJavaIdentifierPart(input.charAt(offset)))
            ++offset;

        if (offset == oldOffset)
            return null;

        inoutOffset.set(offset);
        return input.substring(oldOffset, offset);
    }

    public static String parseUnqualified(String input, MutableInteger inoutOffset) {
        String identifier = tryParseUnqualified(input, inoutOffset);

        if (identifier == null)
            throw new SignatureParseException("Expected identifier", inoutOffset.get());

        return identifier;
    }

    public static List<String> parseQualified(String input, MutableInteger inoutOffset, char separator, boolean excludeLast) {
        if (Character.isJavaIdentifierPart(input.charAt(input.length() - 1)))
            input = input + " "; // sentinel

        MutableInteger offset = new MutableInteger(inoutOffset);
        int excludeLastOffset = inoutOffset.get();

        List<String> parts = new ArrayList<>();

        while (true) {
            String part = tryParseUnqualified(input, offset);

            if (part == null && excludeLast) {
                parts.add(null);
                break;
            }

            parts.add(part);

            if (input.charAt(offset.get()) != separator)
                break;

            excludeLastOffset = offset.get();
            offset.increment();
        }

        if (excludeLast) {
            inoutOffset.set(excludeLastOffset);
            return parts.subList(0, parts.size() - 1);
        } else {
            inoutOffset.set(offset.get());
            return parts;
        }
    }
}
