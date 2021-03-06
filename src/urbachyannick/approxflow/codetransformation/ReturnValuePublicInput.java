package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.tree.*;
import urbachyannick.approxflow.cnf.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.*;

import static urbachyannick.approxflow.codetransformation.BytecodeUtil.*;

public class ReturnValuePublicInput implements Scanner<IntStream> {

    private final OffsetMarker offsetMarker;

    public ReturnValuePublicInput(OffsetMarker offsetMarker) {
        this.offsetMarker = offsetMarker;
    }

    @Override
    public IntStream scan(Stream<ClassNode> sourceClasses, MappedProblem problem) {
        List<ClassNode> classList = sourceClasses.collect(Collectors.toList());
        List<IntStream> variableStreams = new ArrayList<>();

        int addressOffset = offsetMarker.getAddressOffset(problem.getVariableTable());

        for (ClassNode sourceClass : classList) {
            for (MethodNode sourceMethod : sourceClass.methods) {
                if (!hasAnnotation(sourceMethod.visibleAnnotations, "Lurbachyannick/approxflow/PublicInput;"))
                    continue;

                variableStreams.add(
                        JavaVarToCnfVar.variablesForMethodReturnValues(
                            classList,
                            problem.getVariableTable(),
                            sourceClass,
                            sourceMethod,
                            addressOffset
                        )
                );
            }
        }

        return variableStreams.stream().flatMapToInt(Function.identity());
    }
}
