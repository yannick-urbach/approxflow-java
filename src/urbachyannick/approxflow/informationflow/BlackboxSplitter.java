package urbachyannick.approxflow.informationflow;

import org.objectweb.asm.tree.ClassNode;
import urbachyannick.approxflow.*;
import urbachyannick.approxflow.cnf.CnfException;
import urbachyannick.approxflow.cnf.*;
import urbachyannick.approxflow.codetransformation.*;
import urbachyannick.approxflow.javasignatures.*;
import urbachyannick.approxflow.modelcounting.ModelCounter;

import java.util.*;
import java.util.stream.*;

import static urbachyannick.approxflow.MiscUtil.parseLongFromTrivialLiterals;

public class BlackboxSplitter implements FlowAnalyzer {
    private final FlowAnalyzer intermediateAnalyzer;
    private final FlowAnalyzer finalAnalyzer;

    private final CnfGenerator callCountCheckCnfGenerator;

    private final List<Transformation> preSplitTransformations;

    public BlackboxSplitter(CnfGenerator cnfGenerator, ModelCounter modelCounter) {
        preSplitTransformations = new ArrayList<Transformation>() {{
            add(new MethodOfInterestTransform());
            add(new AssertToAssume());
            add(new AddDummyThrow());
            add(new UnrollLoops());
            add(new InlineMethods());
        }};

        intermediateAnalyzer = new DefaultAnalyzer(
                cnfGenerator,
                Stream.of(new ReturnValueInput()),
                Stream.of(new BlackboxIntermediateOutput()),
                modelCounter
        );

        finalAnalyzer = new DefaultAnalyzer(
                cnfGenerator,
                Stream.of(
                        new ReturnValueInput(),
                        new ParameterOutput()
                ),
                Stream.of(
                        new OutputVariable(),
                        new OutputArray(),
                        new ParameterOutputOverApproximated()
                ),
                modelCounter
        );

        callCountCheckCnfGenerator = cnfGenerator;
    }

    @Override
    public double analyzeInformationFlow(Stream<ClassNode> classes, IOCallbacks ioCallbacks) {
        return analyzeInformationFlowNoTransformations(transformClasses(classes, preSplitTransformations), ioCallbacks);
    }

    private double analyzeInformationFlowNoTransformations(Stream<ClassNode> classes, IOCallbacks ioCallbacks) {
        List<ClassNode> classList = classes.collect(Collectors.toList());

        int partCount = getBlackboxCallCount(classList.stream(), ioCallbacks) + 1;

        double flow = Double.POSITIVE_INFINITY;

        for (int i = 0; i < partCount - 1; ++i) {
            double partFlow = intermediateAnalyzer.analyzeInformationFlow(new BlackboxTransform(i).apply(classList.stream()), ioCallbacks);
            flow = Math.min(flow, partFlow);
        }

        double finalFlow = finalAnalyzer.analyzeInformationFlow(new BlackboxTransform(partCount - 1).apply(classList.stream()), ioCallbacks);
        flow = Math.min(flow, finalFlow);

        return flow;
    }

    private Stream<ClassNode> transformClasses(Stream<ClassNode> classes, List<Transformation> transformations) {
        try {
            for (Transformation t : transformations)
                classes = t.apply(classes);
        } catch (InvalidTransformationException e) {
            throw new Fail("Error during transformation", e);
        }

        return classes;
    }

    private int getBlackboxCallCount(Stream<ClassNode> classes, IOCallbacks ioCallbacks) {
        try {
            MappedProblem problem = callCountCheckCnfGenerator.generate(new BlackboxTransform(Integer.MAX_VALUE).apply(classes), ioCallbacks);

            JavaSignature fieldSignature = new JavaSignature(
                    new ClassName("BlackboxCounter"),
                    new FieldAccess("calls")
            );

            return problem
                    .getVariableTable()
                    .getMappings()
                    .filter(l -> fieldSignature.matches(l.getSignature()))
                    .max(VariableMapping::compareByGeneration)
                    .map(m -> {
                        List<MappingValue> mappingValues = m.getMappingValues().collect(Collectors.toList());

                        if (!mappingValues.stream().allMatch(MappingValue::isTrivial))
                            throw new Fail("Can not determine the number of blackbox calls");

                        return (int) parseLongFromTrivialLiterals(mappingValues.stream().map(v -> (TrivialMappingValue) v));
                    })
                    .orElse(0);

        } catch (CnfException e) {
            throw new Fail("Error while generating CNF for call count check", e);
        }
    }
}
