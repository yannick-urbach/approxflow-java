package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.tree.ClassNode;
import urbachyannick.approxflow.cnf.*;
import urbachyannick.approxflow.javasignatures.*;

import java.util.List;
import java.util.stream.*;

import static urbachyannick.approxflow.codetransformation.BytecodeUtil.*;

public class ReturnValuePublicInput implements Scanner<IntStream> {

    @Override
    public IntStream scan(Stream<ClassNode> sourceClasses, MappedProblem problem) {
        return sourceClasses.flatMapToInt(sourceClass ->
                sourceClass.methods.stream()
                        .filter(m -> (
                                getReturnType(m).isPrimitive() &&
                                hasAnnotation(m.visibleAnnotations, "Lurbachyannick/approxflow/PublicInput;")
                        ))
                        .map(m -> new JavaSignature(
                                ClassName.tryParseFromTypeSpecifier("L" + sourceClass.name + ";", new MutableInteger(0)),
                                new FunctionCall(m.name, getArgumentTypes(m).toArray(TypeSpecifier[]::new), getReturnType(m), new ReturnValue())
                        ))
                        .flatMapToInt(s -> getVariablesForSignature(problem, s))
        );
    }

    private IntStream getVariablesForSignature(MappedProblem problem, JavaSignature signature) {
        return problem
                .getVariableTable()
                .getMatching(signature)
                .collect(Collectors.groupingBy(this::getInstance))
                .values().stream().map(List::stream)
                .flatMapToInt(this::getVariablesForMappings);
    }

    private IntStream getVariablesForMappings(Stream<VariableMapping> mappings) {
        return mappings
                .max(VariableMapping::compareByGeneration)
                .map(m -> m
                        .getMappingValues()
                        .filter(v -> !v.isTrivial())
                        .mapToInt(v -> ((Literal) v).getVariable()))
                .orElse(IntStream.empty());
    }

    private int getInstance(VariableMapping mapping) {
        return ((JavaSignature) mapping.getSignature()).getIndices().getInstance();
    }
}
