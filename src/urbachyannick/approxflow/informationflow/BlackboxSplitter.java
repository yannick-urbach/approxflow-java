package urbachyannick.approxflow.informationflow;

import org.objectweb.asm.tree.ClassNode;
import urbachyannick.approxflow.*;
import urbachyannick.approxflow.cnf.CnfException;
import urbachyannick.approxflow.cnf.*;
import urbachyannick.approxflow.codetransformation.*;
import urbachyannick.approxflow.javasignatures.*;
import urbachyannick.approxflow.modelcounting.MaxModelCounter;
import urbachyannick.approxflow.soot.LoopReplacer;

import java.util.*;
import java.util.stream.*;

import static urbachyannick.approxflow.MiscUtil.parseLongFromTrivialLiterals;

public class BlackboxSplitter implements FlowAnalyzer {
    private final FlowAnalyzer intermediateAnalyzer;
    private final FlowAnalyzer finalAnalyzer;

    private final CnfGenerator callCountCheckCnfGenerator;

    private final List<Transformation> preSplitTransformations;

    public BlackboxSplitter(CnfGenerator cnfGenerator, MaxModelCounter modelCounter) {
        preSplitTransformations = new ArrayList<Transformation>() {{
            add(new UnrollLoops());
            add(new InlineMethods());
            add(new LoopReplacer());
            add(new MethodOfInterestTransform());
            add(new AssertToAssume());
            add(new AddDummyThrow());
        }};

        intermediateAnalyzer = new DefaultAnalyzer(
                cnfGenerator,
                Stream.of(new ReturnValueInput()),
                Stream.of(new BlackboxIntermediateOutput()),
                Stream.of(new ReturnValuePublicInput()),
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
                Stream.of(new ReturnValuePublicInput()),
                modelCounter
        );

        callCountCheckCnfGenerator = cnfGenerator;
    }

    @Override
    public double analyzeInformationFlow(Stream<ClassNode> classes, IOCallbacks ioCallbacks) {
        try {
            return analyzeInformationFlowNoTransformations(transformClasses(classes, preSplitTransformations), ioCallbacks);
        } catch (InvalidTransformationException e) {
            throw new Fail("Error during transformation", e);
        }
    }

    private double analyzeInformationFlowNoTransformations(Stream<ClassNode> classes, IOCallbacks ioCallbacks) throws InvalidTransformationException {
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

    private Stream<ClassNode> transformClasses(Stream<ClassNode> classes, List<Transformation> transformations) throws InvalidTransformationException {
        for (Transformation t : transformations)
            classes = t.apply(classes);

        return classes;
    }

    private int getBlackboxCallCount(Stream<ClassNode> classes, IOCallbacks ioCallbacks) throws InvalidTransformationException {
        try {
            MappedProblem problem = callCountCheckCnfGenerator.generate(new BlackboxTransform(Integer.MAX_VALUE).apply(classes), ioCallbacks);

            JavaSignature fieldSignature = new JavaSignature(
                    new ClassName("$$BlackboxCounter"),
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
