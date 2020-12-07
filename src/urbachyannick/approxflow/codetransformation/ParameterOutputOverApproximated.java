package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.tree.ClassNode;
import urbachyannick.approxflow.cnf.*;
import urbachyannick.approxflow.javasignatures.*;

import java.util.stream.*;

public class ParameterOutputOverApproximated implements Scanner<IntStream> {

    @Override
    public IntStream scan(Stream<ClassNode> sourceClasses, MappedProblem problem) {

        return sourceClasses.flatMapToInt(sourceClass ->
                ParameterOutput.getOutputParameters(sourceClass)
                .filter(parameter -> parameter.maxInstances <= 0)
                .flatMapToInt(parameter -> {

                    JavaSignature signature = new JavaSignature(
                            ClassName.tryParseFromTypeSpecifier("L" + sourceClass.name + ";", new MutableInteger(0)),
                            new FunctionCall(
                                    parameter.method.name,
                                    parameter.parameterTypes,
                                    parameter.returnType,
                                    new AnonymousParameter(0, parameter.parameterType)
                            )
                    );

                    return problem
                            .getVariableTable()
                            .getMatching(signature)
                            .flatMapToInt(variable ->
                                    variable.getMappingValues()
                                            .filter(value -> !value.isTrivial())
                                            .mapToInt(value -> ((Literal) value).getVariable())
                            );
                })
        );
    }
}
