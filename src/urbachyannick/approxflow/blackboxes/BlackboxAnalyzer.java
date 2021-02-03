package urbachyannick.approxflow.blackboxes;

import org.objectweb.asm.tree.*;
import urbachyannick.approxflow.*;
import urbachyannick.approxflow.cnf.*;
import urbachyannick.approxflow.codetransformation.*;
import urbachyannick.approxflow.informationflow.*;
import urbachyannick.approxflow.modelcounting.MaxModelCounter;
import urbachyannick.approxflow.soot.LoopReplacer;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static urbachyannick.approxflow.codetransformation.BytecodeUtil.*;

public class BlackboxAnalyzer implements FlowAnalyzer {
    private final FlowAnalyzer partialAnalyzer;
    private final List<Transformation> preSplitTransformations;
    private final List<FlowConstraintAlgorithm> constraintAlgorithms;

    public BlackboxAnalyzer(
            CnfGenerator cnfGenerator,
            MaxModelCounter modelCounter,
            int defaultRecursionDepth,
            int defaultUnrollIterations,
            boolean defaultBlackboxLoops
    ) {
        preSplitTransformations = new ArrayList<Transformation>() {{
            add(new UnrollLoops(defaultBlackboxLoops ? null : defaultUnrollIterations));
            add(new InlineMethods(new InlinePreferences(true, defaultRecursionDepth, false)));
            add(new LoopReplacer(defaultBlackboxLoops));
            add(new MethodOfInterestTransform());
            add(new AssertToAssume());
            add(new ObjectInvariants());
            add(new AddDummyThrow());
        }};

        partialAnalyzer = new DefaultAnalyzer(
                cnfGenerator,
                Stream.of(
                        new ReturnValueInput(),
                        new ParameterOutput()
                ),
                Stream.of(
                        new BlackboxIntermediateOutput(),
                        new OutputVariable(),
                        new OutputArray(),
                        new ParameterOutputOverApproximated()
                ),
                Stream.of(new ReturnValuePublicInput()),
                modelCounter
        );

        constraintAlgorithms = new ArrayList<FlowConstraintAlgorithm>() {{
            add(new StagedFlow());
            // add(new FordFulkerson()); // does not seem to give a lower constraint, ever, so disable for now.
        }};
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
        List<BlackboxCall> blackboxCalls = findBlackboxCalls(classList);

        Map<SourcesSinksPair, Double> partialFlowsCache = new HashMap<>();

        ToDoubleFunction<SourcesSinksPair> getPartialFlow = p -> {
            Double flow = partialFlowsCache.get(p);

            if (flow == null) {
                Preprocess preprocessor = new Preprocess(p);
                Stream<ClassNode> transformed = preprocessor.apply(classList.stream());
                flow = partialAnalyzer.analyzeInformationFlow(transformed, ioCallbacks);
                partialFlowsCache.put(p, flow);
            }

            return flow;
        };

        double flow = Double.POSITIVE_INFINITY;

        for (FlowConstraintAlgorithm algorithm : constraintAlgorithms) {
            double flowConstraint = algorithm.findFlowUpperBound(blackboxCalls.stream(), getPartialFlow);
            flow = Math.min(flow, flowConstraint);
        }

        return flow;
    }

    private static List<BlackboxCall> findBlackboxCalls(List<ClassNode> classes) throws InvalidTransformationException {
        ClassNode mainClass = findClassWithMainMethod(classes.stream())
                .orElseThrow(() -> new InvalidTransformationException("No main method found"));

        MethodNode mainMethod = findMainMethod(mainClass)
                .orElseThrow(() -> new InvalidTransformationException("No main method found"));

        List<BlackboxCall> blackboxCalls = new ArrayList<>();

        for (int i = 0; i < mainMethod.instructions.size(); ++i) {
            AbstractInsnNode instruction = mainMethod.instructions.get(i);

            if (instruction.getType() != AbstractInsnNode.METHOD_INSN)
                continue;

            MethodInsnNode call = (MethodInsnNode) instruction;

            Optional<ClassNode> owner = findClass(classes.stream(), call.owner);

            if (!owner.isPresent())
                continue;

            Optional<MethodNode> method = findMethod(owner.get(), call.owner, call.name, call.desc);

            if (!method.isPresent())
                continue;

            if (hasAnnotation(method.get().visibleAnnotations, "Lurbachyannick/approxflow/Blackbox;"))
                blackboxCalls.add(new BlackboxCall(i));
        }

        return blackboxCalls;
    }

    private Stream<ClassNode> transformClasses(Stream<ClassNode> classes, List<Transformation> transformations) throws InvalidTransformationException {
        for (Transformation t : transformations)
            classes = t.apply(classes);

        return classes;
    }
}
