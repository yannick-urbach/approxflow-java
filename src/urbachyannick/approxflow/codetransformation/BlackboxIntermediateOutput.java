package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.tree.*;
import urbachyannick.approxflow.cnf.*;
import urbachyannick.approxflow.javasignatures.*;

import java.util.*;
import java.util.stream.*;

import static urbachyannick.approxflow.codetransformation.BytecodeUtil.*;

public class BlackboxIntermediateOutput implements Scanner<IntStream> {

    private static class Parameter {
        public MethodNode method;
        public int parameterIndex;
        public TypeSpecifier parameterType;
        public List<TypeSpecifier> parameterTypes;
        public String parameterName;
    }

    @Override
    public IntStream scan(Stream<ClassNode> sourceClasses, MappedProblem problem) {

        return sourceClasses.flatMapToInt(sourceClass ->
                sourceClass.methods.stream()
                        .filter(m -> hasAnnotation(m.visibleAnnotations, "Lurbachyannick/approxflow/$$BlackboxOutput;"))
                        .flatMap(m -> {
                            List<TypeSpecifier> methodParameterTypes = getArgumentTypes(m).collect(Collectors.toList());

                            return IntStream
                                    .range(0, methodParameterTypes.size())
                                    .mapToObj(i ->
                                            new Parameter() {{
                                                method = m;
                                                parameterIndex = i;
                                                parameterType = methodParameterTypes.get(i);
                                                parameterTypes = methodParameterTypes;
                                                parameterName = m.parameters.get(i).name;
                                            }}
                                    );
                        })
                        .flatMapToInt(parameter -> {
                            JavaSignature signature = new JavaSignature(
                                    ClassName.tryParseFromTypeSpecifier("L" + sourceClass.name + ";", new MutableInteger(0)),
                                    new FunctionCall(
                                            parameter.method.name,
                                            parameter.parameterTypes.toArray(new TypeSpecifier[0]),
                                            PrimtiveType.VOID,
                                            new NamedLocal(parameter.parameterName)
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
