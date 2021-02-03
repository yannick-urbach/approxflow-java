package urbachyannick.approxflow.informationflow;

import org.objectweb.asm.tree.ClassNode;
import urbachyannick.approxflow.*;
import urbachyannick.approxflow.cnf.CnfException;
import urbachyannick.approxflow.cnf.*;
import urbachyannick.approxflow.codetransformation.*;
import urbachyannick.approxflow.modelcounting.*;

import java.util.List;
import java.util.stream.*;

public class DefaultAnalyzer implements FlowAnalyzer {
    private final CnfGenerator cnfGenerator;
    private final List<Transformation> transformations;
    private final List<Scanner<IntStream>> countVarScanners;
    private final List<Scanner<IntStream>> maxVarScanners;
    private final MaxModelCounter modelCounter;

    public DefaultAnalyzer(
            CnfGenerator cnfGenerator,
            Stream<Transformation> transformations,
            Stream<Scanner<IntStream>> countVarScanners,
            Stream<Scanner<IntStream>> maxVarScanners,
            MaxModelCounter modelCounter
    ) {
        this.cnfGenerator = cnfGenerator;
        this.transformations = transformations.collect(Collectors.toList());
        this.countVarScanners = countVarScanners.collect(Collectors.toList());
        this.maxVarScanners = maxVarScanners.collect(Collectors.toList());
        this.modelCounter = modelCounter;
    }

    @Override
    public double analyzeInformationFlow(Stream<ClassNode> classes, IOCallbacks ioCallbacks) {
        List<ClassNode> classList = transformClasses(classes).collect(Collectors.toList());

        MappedProblem problem = generateCnf(classList.stream(), ioCallbacks);
        Scope countVars = getScope(countVarScanners.stream(), classList.stream(), problem);
        Scope maxVars = getScope(maxVarScanners.stream(), classList.stream(), problem);

        MaxModelCountingProblem countingProblem = new MaxModelCountingProblem(problem, countVars, maxVars);

        if (!countingProblem.getCountVars().getVariables().findAny().isPresent())
            return 0;

        double solutions = countSolutions(countingProblem, ioCallbacks);

        return Math.log(solutions) / Math.log(2);
    }

    private Stream<ClassNode> transformClasses(Stream<ClassNode> classes) {
        try {
            for (Transformation t : transformations)
                classes = t.apply(classes);
        } catch (InvalidTransformationException e) {
            throw new Fail("Error during transformation", e);
        }

        return classes;
    }

    private MappedProblem generateCnf(Stream<ClassNode> classes, IOCallbacks ioCallbacks) {
        try {
            return cnfGenerator.generate(classes, ioCallbacks);
        } catch (CnfException e) {
            throw new Fail("Failed to generate CNF", e);
        }
    }

    private Scope getScope(Stream<Scanner<IntStream>> scanners, Stream<ClassNode> classes, MappedProblem problem) {
        List<ClassNode> classList = classes.collect(Collectors.toList());

        return new Scope(
                scanners
                        .flatMap(s -> s.scan(classList.stream(), problem).boxed())
                        .collect(Collectors.toList())
                        .stream()
                        .mapToInt(Integer::intValue)
        );
    }

    private double countSolutions(MaxModelCountingProblem problem, IOCallbacks ioCallbacks) {
        try {
            return modelCounter.count(problem, ioCallbacks);
        } catch (ModelCountingException e) {
            throw new Fail("Error during model counting", e);
        }
    }
}
