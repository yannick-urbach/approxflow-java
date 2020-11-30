package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.tree.ClassNode;
import urbachyannick.approxflow.cnf.Literal;
import urbachyannick.approxflow.cnf.MappedProblem;
import urbachyannick.approxflow.javasignatures.*;

import java.io.IOException;
import java.util.stream.IntStream;

public class ParameterOutputOverApproximated extends Scanner<IntStream> {

    @Override
    public IntStream scan(ClassNode sourceClass, MappedProblem problem) throws IOException {

        return ParameterOutput.getOutputParameters(sourceClass)
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
                });
    }
}
