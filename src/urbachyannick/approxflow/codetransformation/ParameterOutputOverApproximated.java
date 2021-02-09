package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.tree.ClassNode;
import urbachyannick.approxflow.cnf.*;

import java.util.List;
import java.util.stream.*;

public class ParameterOutputOverApproximated implements Scanner<IntStream> {

    private final OffsetMarker offsetMarker;

    public ParameterOutputOverApproximated(OffsetMarker offsetMarker) {
        this.offsetMarker = offsetMarker;
    }

    @Override
    public IntStream scan(Stream<ClassNode> sourceClasses, MappedProblem problem) {
        List<ClassNode> classes = sourceClasses.collect(Collectors.toList());

        int addressOffset = offsetMarker.getAddressOffset(problem.getVariableTable());

        return classes.stream().flatMapToInt(sourceClass ->
                ParameterOutput.getOutputParameters(sourceClass)
                        .filter(parameter -> parameter.maxInstances <= 0)
                        .flatMapToInt(parameter -> JavaVarToCnfVar.variablesForMethodParameter(
                                classes,
                                problem.getVariableTable(),
                                sourceClass,
                                parameter.method,
                                parameter.parameterIndex,
                                addressOffset
                        ))
        );
    }
}
