package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import urbachyannick.approxflow.cnf.*;

import java.util.List;
import java.util.stream.*;

import static urbachyannick.approxflow.codetransformation.BytecodeUtil.*;

public class OutputVariable implements Scanner<IntStream> {

    private final OffsetMarker offsetMarker;

    public OutputVariable(OffsetMarker offsetMarker) {
        this.offsetMarker = offsetMarker;
    }

    @Override
    public IntStream scan(Stream<ClassNode> sourceClasses, MappedProblem problem) {
        List<ClassNode> classes = sourceClasses.collect(Collectors.toList());

        int addressOffset = offsetMarker.getAddressOffset(problem.getVariableTable());

        return classes.stream().flatMapToInt(class_ ->
                class_.fields.stream()
                        .filter(field ->
                                hasFlag(field.access, Opcodes.ACC_STATIC) &&
                                hasAnnotation(field.visibleAnnotations, "Lurbachyannick/approxflow/PublicOutput;")
                        )
                        .flatMapToInt(field ->
                                JavaVarToCnfVar.variablesForStaticField(classes, problem.getVariableTable(), class_, field, addressOffset)
                        )
        );
    }
}
