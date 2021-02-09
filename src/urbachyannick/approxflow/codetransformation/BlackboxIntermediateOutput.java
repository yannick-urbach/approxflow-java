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
        public TypeSpecifier returnType;
    }

    private final OffsetMarker offsetMarker;

    public BlackboxIntermediateOutput(OffsetMarker offsetMarker) {
        this.offsetMarker = offsetMarker;
    }

    @Override
    public IntStream scan(Stream<ClassNode> sourceClasses, MappedProblem problem) {

        List<ClassNode> classes = sourceClasses.collect(Collectors.toList());

        int addressOffset = offsetMarker.getAddressOffset(problem.getVariableTable());

        return classes.stream().flatMapToInt(sourceClass ->
                sourceClass.methods.stream()
                        .filter(m -> hasAnnotation(m.visibleAnnotations, "Lurbachyannick/approxflow/$$BlackboxOutput;"))
                        .flatMap(m -> {
                            List<TypeSpecifier> methodParameterTypes = getArgumentTypes(m).collect(Collectors.toList());
                            TypeSpecifier methodReturnType = getReturnType(m);

                            return IntStream
                                    .range(0, methodParameterTypes.size())
                                    .mapToObj(i ->
                                            new Parameter() {{
                                                method = m;
                                                parameterIndex = i;
                                                parameterType = methodParameterTypes.get(i);
                                                parameterTypes = methodParameterTypes;
                                                returnType = methodReturnType;
                                            }}
                                    );
                        })
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
