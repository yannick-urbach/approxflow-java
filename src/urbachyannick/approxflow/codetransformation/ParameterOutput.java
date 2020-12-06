package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import urbachyannick.approxflow.javasignatures.MutableInteger;
import urbachyannick.approxflow.javasignatures.PrimtiveType;
import urbachyannick.approxflow.javasignatures.TypeSpecifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static urbachyannick.approxflow.codetransformation.BytecodeUtil.*;

// For parameters "@PublicOutput(maxInstances = <maxInstances>) <type> <param>" of method <method>
// where <maxInstances> > 0 and <type> is primitive,
// - creates the following:
//     - "public static <type>[] <method>_<param>_array = new <type>[<maxInstances>];"
//     - "public static int <method>_<param>_counter = 0;"
//     - "public static void <method>_<param>_overflow(@PublicOutput(maxInstances = 0) <type> <param>) { }"
// - generates additional code at the start of the method that does the following:
//     if (<method>_<param>_counter < <maxInstances>) {
//         <method>_<param>_array[counter++] = <param>;
//     } else {
//         <method>_<param>_overflow(<param>);
//     }
//
// parameters with <maxInstances> <= 0 (including those of the overflow methods generated by this class) are handled by
// ParameterOutputOverApproximated.
public class ParameterOutput extends Transformation.PerClassNoExcept {

    @Override
    public ClassNode applyToClass(ClassNode sourceClass) {
        ClassNode targetClass = new ClassNode(Opcodes.ASM5);
        sourceClass.accept(targetClass);

        MethodNode classInit = targetClass.methods.stream()
                .filter(m -> m.name.equals("<clinit>"))
                .findFirst()
                .orElseGet(() -> {
                    MethodNode m = new MethodNode(
                            Opcodes.ASM5,
                            Opcodes.ACC_STATIC,
                            "<clinit>",
                            "()V",
                            null,
                            null
                    );
                    m.instructions.add(new InsnNode(Opcodes.RETURN));
                    targetClass.methods.add(m);
                    return m;
                });

        List<OutputParameter> parameters = getOutputParameters(targetClass).collect(Collectors.toList());

        for (OutputParameter parameter : parameters)
            applyForParameter(targetClass, classInit, parameter);

        return targetClass;
    }

    private static void applyForParameter(ClassNode class_, MethodNode classInit, OutputParameter parameter) {
        if (parameter.maxInstances <= 0)
            return;

        String methodQualifiedName = parameter.method.name + "_" + parameter.parameter.name;
        String arrayName = methodQualifiedName + "_array";
        String counterName = methodQualifiedName + "_counter";
        String arrayType = "[" + parameter.parameterType.asTypeSpecifierString();
        String overflowMethodName = methodQualifiedName + "_overflow";

        FieldNode outputArray = new FieldNode(
                Opcodes.ASM5,
                Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC,
                arrayName,
                arrayType,
                null,
                null
        );
        outputArray.visibleAnnotations = new ArrayList<>();
        outputArray.visibleAnnotations.add(new AnnotationNode(Opcodes.ASM5, "Lurbachyannick/approxflow/PublicOutput;"));
        class_.fields.add(outputArray);

        FieldNode counter = new FieldNode(
                Opcodes.ASM5,
                Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC,
                counterName,
                "I",
                null,
                null
        );
        class_.fields.add(counter);

        MethodNode overflowMethod = new MethodNode(
                Opcodes.ASM5,
                Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC,
                overflowMethodName,
                "(" + parameter.parameterType.asTypeSpecifierString() + ")V",
                null,
                null
        ) {{
            visibleParameterAnnotations = new List[]{
                    new ArrayList<AnnotationNode>() {{
                        add(new AnnotationNode(Opcodes.ASM5, "Lurbachyannick/approxflow/PublicOutput;") {{
                            values = new ArrayList<Object>() {{
                                add("maxInstances");
                                add(0);
                            }};
                        }});
                    }}
            };

            instructions.add(new LabelNode());
            instructions.add(new InsnNode(Opcodes.RETURN));
            instructions.add(new LabelNode());

            maxLocals = 1;
            localVariables = new ArrayList<LocalVariableNode>() {{
                add(new LocalVariableNode(
                        parameter.parameter.name,
                        parameter.parameterType.asTypeSpecifierString(),
                        null,
                        (LabelNode) instructions.getFirst(),
                        (LabelNode) instructions.getFirst(),
                        0
                ));
            }};

            parameters = new ArrayList<ParameterNode>() {{
                add(new ParameterNode(parameter.parameter.name, 0));
            }};
        }};

        class_.methods.add(overflowMethod);

        LabelNode startLabel = new LabelNode();
        LabelNode endLabel = new LabelNode();
        LabelNode elseLabel = new LabelNode();

        InsnList newInstructions = new InsnList() {{
            // if (counter < maxInstances) {
            //     array[counter++] = param;
            // } else {
            //     overflow(param);
            // }

            add(startLabel);

            // if
            add(new FieldInsnNode(Opcodes.GETSTATIC, class_.name, counterName, "I"));
            add(new LdcInsnNode(parameter.maxInstances));
            add(new JumpInsnNode(Opcodes.IF_ICMPGE, elseLabel));

            // then
            add(new FieldInsnNode(Opcodes.GETSTATIC, class_.name, arrayName, arrayType));
            add(new FieldInsnNode(Opcodes.GETSTATIC, class_.name, counterName, "I"));
            add(new InsnNode(Opcodes.DUP));
            add(new InsnNode(Opcodes.ICONST_1));
            add(new InsnNode(Opcodes.IADD));
            add(new FieldInsnNode(Opcodes.PUTSTATIC, class_.name, counterName, "I"));
            add(new VarInsnNode(parameter.parameterType.getLoadLocalOpcode(), parameter.parameterIndex));
            add(new InsnNode(parameter.parameterType.getArrayStoreOpcode()));
            add(new JumpInsnNode(Opcodes.GOTO, endLabel));

            // else
            add(elseLabel);
            add(new VarInsnNode(parameter.parameterType.getLoadLocalOpcode(), parameter.parameterIndex));
            add(new MethodInsnNode(Opcodes.INVOKESTATIC, class_.name, overflowMethod.name, overflowMethod.desc));

            add(endLabel);
        }};

        parameter.method.instructions.insertBefore(parameter.method.instructions.getFirst(), newInstructions);


        InsnList classInitInstructions = new InsnList() {{
            // array = new ParamType[maxInstances];
            add(new LdcInsnNode(parameter.maxInstances));
            add(new IntInsnNode(Opcodes.NEWARRAY, parameter.parameterType.getTypeOpcode()));
            add(new FieldInsnNode(Opcodes.PUTSTATIC, class_.name, arrayName, arrayType));
        }};


        classInit.instructions.insertBefore(classInit.instructions.getFirst(), classInitInstructions);
    }

    // --- following class and method shared with ParameterOutputOverApproximated; default access is intentional. ---

    static class OutputParameter {
        public MethodNode method;
        public TypeSpecifier[] parameterTypes;
        public TypeSpecifier returnType;
        public ParameterNode parameter;
        public int parameterIndex;
        public int maxInstances;
        public PrimtiveType parameterType;
        public boolean isInvalid; // can't throw in stream operations...
    }

    static Stream<OutputParameter> getOutputParameters(ClassNode class_) {
        return class_.methods.stream()
                .filter(method ->
                        method.visibleParameterAnnotations != null &&
                        method.parameters != null
                )
                .flatMap(method -> {
                    Type[] parameterTypes = Type.getArgumentTypes(method.desc);

                    TypeSpecifier[] parameterTypeSpecifiers = Arrays.stream(parameterTypes)
                            .map(t -> TypeSpecifier.parse(t.getDescriptor(), new MutableInteger(0)))
                            .toArray(TypeSpecifier[]::new);

                    TypeSpecifier returnType = TypeSpecifier.parse(Type.getReturnType(method.desc).getDescriptor(), new MutableInteger(0));

                    return IntStream.range(0, method.parameters.size())
                            .filter(i -> hasAnnotation(method.visibleParameterAnnotations[i], "Lurbachyannick/approxflow/PublicOutput;"))
                            .mapToObj(i -> {
                                AnnotationNode annotationNode = getAnnotation(method.visibleParameterAnnotations[i], "Lurbachyannick/approxflow/PublicOutput;").get();

                                OutputParameter result = new OutputParameter();
                                result.method = method;
                                result.parameterTypes = parameterTypeSpecifiers;
                                result.returnType = returnType;
                                result.parameter = method.parameters.get(i);
                                result.parameterIndex = i;
                                result.maxInstances = (int) (Integer) getAnnotationValue(annotationNode, "maxInstances").orElse(16);

                                TypeSpecifier parameterType = parameterTypeSpecifiers[i];

                                if (!parameterType.isPrimitive()) {
                                    result.isInvalid = true;
                                    return result;
                                }

                                result.parameterType = parameterType.asPrimitive();

                                return result;
                            });
                })
                .filter(parameter -> !parameter.isInvalid);
    }
}
