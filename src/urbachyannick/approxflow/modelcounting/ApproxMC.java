package urbachyannick.approxflow.modelcounting;

import urbachyannick.approxflow.IOCallbacks;
import urbachyannick.approxflow.cnf.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.regex.*;

public class ApproxMC implements ModelCounter {

    @Override
    public double count(ModelCountingProblem problem, IOCallbacks ioCallbacks) throws ModelCountingException {
        Path cnfFilePath;
        Path outputPath;

        try {
            cnfFilePath = ioCallbacks.createTemporaryFile("approxmc-input.cnf");
            outputPath = ioCallbacks.createTemporaryFile("approxmc-log.txt");
            ioCallbacks.createTemporaryFile("approxmc-input.cnf.scope");

            IO.write(problem, cnfFilePath);
        } catch (IOException e) {
            throw new ModelCountingException("Can not write temporary CNF file", e);
        }

        try {
            ProcessBuilder.Redirect out = ProcessBuilder.Redirect.to(outputPath.toFile());

            new ProcessBuilder()
                    .command("approxmc", cnfFilePath.toString())
                    .redirectOutput(out)
                    .redirectError(out)
                    .start()
                    .waitFor();

        } catch (IOException | InterruptedException e) {
            throw new ModelCountingException("Failed to run SAT solver", e);
        }

        Pattern pattern = Pattern.compile("s mc (\\d+)");

        try {
            return Files.lines(outputPath)
                    .map(pattern::matcher)
                    .filter(Matcher::find)
                    .map(matcher -> Double.parseDouble(matcher.group(1)))
                    .findFirst()
                    .orElseThrow(() -> new ModelCountingException("Failed to parse result of ApproxMC"));

        } catch (IOException e) {
            throw new ModelCountingException("Failed to read SAT solver result", e);
        }
    }
}
