package urbachyannick.approxflow.modelcounting;

import urbachyannick.approxflow.*;
import urbachyannick.approxflow.cnf.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;
import java.util.regex.*;

public class MaxCount implements MaxModelCounter {
    private final int k;
    private final double epsilon;
    private final double lowerBoundConfidence;
    private final double upperBoundConfidence;

    public MaxCount(int k, double epsilon, double lowerBoundConfidence, double upperBoundConfidence) {
        this.k = k;
        this.epsilon = epsilon;
        this.lowerBoundConfidence = lowerBoundConfidence;
        this.upperBoundConfidence = upperBoundConfidence;
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
            ProcessBuilder.Redirect out = ProcessBuilder.Redirect.to(outputPath.toFile());

            new ProcessBuilder()
                    .command(
                            "python",
                            "maxcount.py",
                            "--countingTolerance", Double.toString(epsilon),
                            "--upperBoundConfidence", Double.toString(upperBoundConfidence),
                            "--lowerBoundConfidence", Double.toString(lowerBoundConfidence),
                            cnfFilePath.toAbsolutePath().toString(),
                            "" + k
                    )
                    .directory(ioCallbacks.findInProgramDirectory(Paths.get("util", "meelgroup-maxcount")).toFile())
                    .redirectOutput(out)
                    .redirectError(out)
                    .start()
                    .waitFor();

        } catch (IOException | InterruptedException e) {
            throw new ModelCountingException("Failed to run SAT solver", e);
        }

        Pattern estimatedPattern = Pattern.compile("c Estimated max-count: ([\\d.]+) x (\\d+)\\^(\\d+)");
        Pattern lowerPattern = Pattern.compile("c Max-count is >= ([\\d.]+) x (\\d+)\\^(\\d+) with probability >= ([\\d.]+)");
        Pattern upperPattern = Pattern.compile("c Max-count is <= ([\\d.]+) x (\\d+)\\^(\\d+) with probability >= ([\\d.]+)");

        try {
            double estimated = readResult(estimatedPattern, outputPath)
                    .orElseThrow(() -> new ModelCountingException("Failed to parse result of MaxCount"));

            //readResult(lowerPattern, outputPath).ifPresent(l -> System.out.println("Lower bound: " + l));
            //readResult(upperPattern, outputPath).ifPresent(l -> System.out.println("Upper bound: " + l));

            return estimated;
        } catch (IOException e) {
            throw new ModelCountingException("Failed to read SAT solver result", e);
        }
    }

    private Optional<Double> readResult(Pattern pattern, Path outFile) throws IOException {
        return Files.lines(outFile)
                .map(pattern::matcher)
                .filter(Matcher::find)
                .map(matcher -> {
                    double multiplier = Double.parseDouble(matcher.group(1));
                    int base = Integer.parseInt(matcher.group(2));
                    int exponent = Integer.parseInt(matcher.group(3));

                    return multiplier * Math.pow(base, exponent);
                })
                .findFirst();
    }
}
