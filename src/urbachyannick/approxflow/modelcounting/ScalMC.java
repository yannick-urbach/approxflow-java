package urbachyannick.approxflow.modelcounting;

import urbachyannick.approxflow.cnf.IO;
import urbachyannick.approxflow.cnf.ScopedMappedProblem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScalMC implements ModelCounter {

    @Override
    public double count(ScopedMappedProblem problem) throws ModelCountingException {
        Path cnfFilePath;

        try {
            cnfFilePath = Files.createTempFile("temporary-cnf-file", "");
            IO.write(problem, cnfFilePath);
        } catch (IOException e) {
            throw new ModelCountingException("Can not write temporary CNF file", e);
        }

        try {
            Process process = new ProcessBuilder()
                    .command(Paths.get("util/scalmc").toAbsolutePath().toString(), cnfFilePath.toString())
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;

                while ((line = reader.readLine()) != null) {
                    System.out.println(line);

                    if (line.startsWith("Number of solutions is:")) {
                        Matcher matcher = Pattern.compile("Number of solutions is: (\\d+) x (\\d+)\\^(\\d+)").matcher(line);

                        if (!matcher.find())
                            throw new ModelCountingException("Failed to parse result of ScalMC");

                        int multiplier = Integer.parseInt(matcher.group(1));
                        int base = Integer.parseInt(matcher.group(2));
                        int exponent = Integer.parseInt(matcher.group(3));
                        double solutions = multiplier * Math.pow(base, exponent);
                        return solutions;
                    }
                }
            } finally {
                process.waitFor();
            }
        } catch (IOException | InterruptedException e) {
            throw new ModelCountingException("Failed to run SAT solver", e);
        }

        try {
            Files.delete(cnfFilePath);
            Files.delete(cnfFilePath.resolveSibling(cnfFilePath.getFileName() + ".scope"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        throw new ModelCountingException("Failed to parse result of ScalMC");
    }
}
