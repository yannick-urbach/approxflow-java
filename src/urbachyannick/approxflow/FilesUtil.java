package urbachyannick.approxflow;

import java.io.*;
import java.nio.file.*;
import java.util.regex.*;
import java.util.stream.Stream;

/**
 * Utility class providing miscellaneous functionality for working with files.
 */
public class FilesUtil {
    /**
     * Finds a new available file name based on a given path and returns that path with the filename substituted by the
     * new file name. The new file name is chosen according to the following rules:
     * <ul>
     * <li>If the previous file name without extension ends in a number, that number is removed.</li>
     * <li>A number is inserted before the extension (if any), such that there is no file at the resulting path, and the
     *    number is higher than the previous number (if any). The number inserted is the lowest positive number for
     *    which that is true.</li>
     * </ul>
     * Examples:
     * <ul>
     *     <li>"/a/path/Foo.txt" becomes "/a/path/Foo1.txt" if no file exists at that path</li>
     *     <li>"Foo1.txt" becomes "Foo2.txt" if no file exists at that path</li>
     *     <li>"Foo" becomes "Foo1" if no file exists at that path</li>
     *     <li>"Foo" becomes "Foo2" if there is already a file at Foo1</li>
     * </ul>
     * @param path the previous path
     * @return the new path
     */
    public static Path getNext(Path path) {
        Path directory = path.getParent();
        String fileName = path.getFileName().toString();
        String withoutExtension;
        String extension;
        String baseName;
        int number;

        Matcher extensionMatcher = Pattern.compile("^(.*?)(\\.\\w+)$").matcher(fileName);

        if (extensionMatcher.find()) {
            withoutExtension = extensionMatcher.group(1);
            extension = extensionMatcher.group(2);
        } else {
            withoutExtension = fileName;
            extension = "";
        }

        Matcher numberMatcher = Pattern.compile("^(.*?)(\\d+)$").matcher(withoutExtension);

        if (numberMatcher.find()) {
            baseName = numberMatcher.group(1);
            number = (Integer.parseInt(numberMatcher.group(2)));
        } else {
            baseName = withoutExtension;
            number = 0;
        }

        Path newPath;

        do {
            ++number;
            newPath = directory.resolve(baseName + number + extension);
        } while (Files.exists(newPath));

        return newPath;
    }

    /**
     * Writes lines to a buffered writer.
     * @param writer the writer
     * @param lines the lines to write
     * @throws IOException if the writer could not write
     */
    public static void writeLines(BufferedWriter writer, Stream<String> lines) throws IOException {
        try {
            lines.forEach(line -> {
                try {
                    writer.write(line);
                    writer.newLine();
                } catch (IOException e) {
                    throw new RuntimeException(e); // ugly workaround, part I (can't have checked exceptions in foreach)
                }
            });
        } catch (RuntimeException e) { // ugly workaround, part II
            if (e.getCause() instanceof IOException)
                throw (IOException) e.getCause();
            else throw e;
        }
    }
}
