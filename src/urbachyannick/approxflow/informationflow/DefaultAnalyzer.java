package urbachyannick.approxflow.informationflow;

import org.objectweb.asm.tree.ClassNode;
import urbachyannick.approxflow.Fail;
import urbachyannick.approxflow.IOCallbacks;
import urbachyannick.approxflow.cnf.*;
import urbachyannick.approxflow.codetransformation.InvalidTransformationException;
import urbachyannick.approxflow.codetransformation.Scanner;
import urbachyannick.approxflow.codetransformation.Transformation;
import urbachyannick.approxflow.modelcounting.ModelCounter;
import urbachyannick.approxflow.modelcounting.ModelCountingException;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class DefaultAnalyzer implements FlowAnalyzer {
    private final CnfGenerator cnfGenerator;
    private final List<Transformation> transformations;
    private final List<Scanner<IntStream>> scanners;
    private final ModelCounter modelCounter;

    public DefaultAnalyzer(
            CnfGenerator cnfGenerator,
            Stream<Transformation> transformations,
            Stream<Scanner<IntStream>> scanners,
            ModelCounter modelCounter
    ) {
        this.cnfGenerator = cnfGenerator;
        this.transformations = transformations.collect(Collectors.toList());
        this.scanners = scanners.collect(Collectors.toList());
        this.modelCounter = modelCounter;
    }

    @Override
    public double analyzeInformationFlow(Stream<ClassNode> classes, IOCallbacks ioCallbacks) {
        List<ClassNode> classList = transformClasses(classes).collect(Collectors.toList());

        MappedProblem problem = generateCnf(classList.stream(), ioCallbacks);
        Scope scope = getScope(classList.stream(), problem);
        ScopedMappedProblem scopedMappedProblem = new ScopedMappedProblem(problem, scope);
        double solutions = countSolutions(scopedMappedProblem, ioCallbacks);

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

    private Scope getScope(Stream<ClassNode> classes, MappedProblem problem) {
        List<ClassNode> classList = classes.collect(Collectors.toList());

        return new Scope(
                scanners.stream()
                        .flatMap(s -> s.scan(classList.stream(), problem).boxed())
                        .collect(Collectors.toList())
                        .stream()
                        .mapToInt(Integer::intValue)
        );
    }

    private double countSolutions(ScopedMappedProblem problem, IOCallbacks ioCallbacks) {
        try {
            return modelCounter.count(problem, ioCallbacks);
        } catch (ModelCountingException e) {
            throw new Fail("Error during model counting", e);
        }
    }
}
