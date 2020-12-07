package urbachyannick.approxflow.modelcounting;

import urbachyannick.approxflow.IOCallbacks;
import urbachyannick.approxflow.cnf.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.regex.*;

public class ScalMC implements ModelCounter {

    @Override
    public double count(ScopedMappedProblem problem, IOCallbacks ioCallbacks) throws ModelCountingException {
        Path cnfFilePath;
        Path outputPath;

        try {
            cnfFilePath = ioCallbacks.createTemporaryFile("scalmc-input.cnf");
            outputPath = ioCallbacks.createTemporaryFile("sclamc-log.txt");
            ioCallbacks.createTemporaryFile("scalmc-input.cnf.scope");

            IO.write(problem, cnfFilePath);
        } catch (IOException e) {
            throw new ModelCountingException("Can not write temporary CNF file", e);
        }

        try {
            ProcessBuilder.Redirect out = ProcessBuilder.Redirect.to(outputPath.toFile());

            new ProcessBuilder()
                    .command(Paths.get("util/scalmc").toAbsolutePath().toString(), cnfFilePath.toString())
                    .redirectOutput(out)
                    .redirectError(out)
                    .start()
                    .waitFor();

        } catch (IOException | InterruptedException e) {
            throw new ModelCountingException("Failed to run SAT solver", e);
        }

        Pattern pattern = Pattern.compile("Number of solutions is: (\\d+) x (\\d+)\\^(\\d+)");

        try {
            return Files.lines(outputPath)
                    .map(line -> pattern.matcher(line))
                    .filter(Matcher::find)
                    .map(matcher -> {
                        int multiplier = Integer.parseInt(matcher.group(1));
                        int base = Integer.parseInt(matcher.group(2));
                        int exponent = Integer.parseInt(matcher.group(3));

                        return multiplier * Math.pow(base, exponent);
                    })
                    .findFirst()
                    .orElseThrow(() -> new ModelCountingException("Failed to parse result of ScalMC"));

        } catch (IOException e) {
            throw new ModelCountingException("Failed to read SAT solver result", e);
        }
    }
}
