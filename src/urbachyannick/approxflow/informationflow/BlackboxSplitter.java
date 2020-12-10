package urbachyannick.approxflow.informationflow;

import org.objectweb.asm.tree.*;
import urbachyannick.approxflow.*;
import urbachyannick.approxflow.cnf.*;
import urbachyannick.approxflow.codetransformation.*;
import urbachyannick.approxflow.modelcounting.*;

import java.util.*;
import java.util.stream.*;

import static urbachyannick.approxflow.codetransformation.BytecodeUtil.*;

public class BlackboxSplitter implements FlowAnalyzer {
    private final FlowAnalyzer intermediateAnalyzer;
    private final FlowAnalyzer finalAnalyzer;

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
                Stream.of(
                        new ReturnValueInput()
                ),
                Stream.of(
                        new BlackboxIntermediateOutput()
                ),
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
    }

    @Override
    public double analyzeInformationFlow(Stream<ClassNode> classes, IOCallbacks ioCallbacks) {
        return analyzeInformationFlowNoTransformations(transformClasses(classes, preSplitTransformations), ioCallbacks);
    }

    private double analyzeInformationFlowNoTransformations(Stream<ClassNode> classes, IOCallbacks ioCallbacks) {
        List<ClassNode> classList = classes.collect(Collectors.toList());

        int partCount = 1;

        for (ClassNode class_ : classList) {
            for (int i = 0; i < class_.methods.size(); ++i) {
                MethodNode method = class_.methods.get(i);

                Optional<Integer> callCount = getAnnotation(method.visibleAnnotations, "Lurbachyannick/approxflow/Blackbox;")
                        .flatMap(a -> getAnnotationValue(a, "calls"))
                        .map(v -> (Integer) v);

                if (callCount.isPresent())
                    partCount += callCount.get();
            }
        }

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
}
