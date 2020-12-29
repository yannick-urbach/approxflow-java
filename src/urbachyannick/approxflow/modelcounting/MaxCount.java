package urbachyannick.approxflow.modelcounting;

import urbachyannick.approxflow.IOCallbacks;
import urbachyannick.approxflow.cnf.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.regex.*;

public class MaxCount implements MaxModelCounter {
    private final int k;

    public MaxCount(int k) {
        this.k = k;
    }

    @Override
    public double count(MaxModelCountingProblem problem, IOCallbacks ioCallbacks) throws ModelCountingException {
        Path cnfFilePath;
        Path outputPath;

        try {
            cnfFilePath = ioCallbacks.createTemporaryFile("maxcount-input.cnf");
            outputPath = ioCallbacks.createTemporaryFile("maxcount-log.txt");
            ioCallbacks.createTemporaryFile("maxcount-input.cnf.scope");

            IO.write(problem, cnfFilePath);
        } catch (IOException e) {
            throw new ModelCountingException("Can not write temporary CNF file", e);
        }

        try {
            Files.createSymbolicLink(Paths.get("util/maxcount/scalmc"), Paths.get("scalmc-binaries/x86_64-linux/scalmc"));
        } catch (IOException | UnsupportedOperationException e) {
            throw new ModelCountingException("Failed to link scalmc binary", e);
        }


        try {
            ProcessBuilder.Redirect out = ProcessBuilder.Redirect.to(outputPath.toFile());

            new ProcessBuilder()
                    .command(
                            "python",
                            "maxcount.py",
                            cnfFilePath.toAbsolutePath().toString(),
                            "" + k
                    )
                    .directory(Paths.get("util/maxcount").toAbsolutePath().toFile())
                    .redirectOutput(out)
                    .redirectError(out)
                    .start()
                    .waitFor();

        } catch (IOException | InterruptedException e) {
            throw new ModelCountingException("Failed to run SAT solver", e);
        } finally {
            try {
                Files.deleteIfExists(Paths.get("util/maxcount/scalmc"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
//
        Pattern pattern = Pattern.compile("c Estimated max-count: ([\\d.]+) x (\\d+)\\^(\\d+)");

        try {
            return Files.lines(outputPath)
                    .map(pattern::matcher)
                    .filter(Matcher::find)
                    .map(matcher -> {
                        int multiplier = Integer.parseInt(matcher.group(1));
                        int base = Integer.parseInt(matcher.group(2));
                        int exponent = Integer.parseInt(matcher.group(3));

                        return multiplier * Math.pow(base, exponent);
                    })
                    .findFirst()
                    .orElseThrow(() -> new ModelCountingException("Failed to parse result of MaxCount"));

        } catch (IOException e) {
            throw new ModelCountingException("Failed to read SAT solver result", e);
        }
    }
}
