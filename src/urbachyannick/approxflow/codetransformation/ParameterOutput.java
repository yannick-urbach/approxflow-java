package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import urbachyannick.approxflow.javasignatures.MutableInteger;
import urbachyannick.approxflow.javasignatures.PrimtiveType;
import urbachyannick.approxflow.javasignatures.TypeSpecifier;

import static urbachyannick.approxflow.codetransformation.BytecodeUtil.getAnnotation;
import static urbachyannick.approxflow.codetransformation.BytecodeUtil.hasAnnotation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ParameterOutput extends Transformation {

    @Override
    public void apply(ClassNode sourceClass, ClassNode targetClass) throws IOException, InvalidTransformationException {
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

        for (MethodNode method : targetClass.methods) {
            if (method.visibleParameterAnnotations == null)
                continue;

            if (method.parameters == null)
                continue;

            Type[] parameterTypes = Type.getArgumentTypes(method.desc);

            for (int i = 0; i < method.parameters.size(); ++i) {
                Optional<AnnotationNode> annotationNodeOptional = getAnnotation(method.visibleParameterAnnotations[i], "Lurbachyannick/approxflow/PublicOutput;");

                if (!annotationNodeOptional.isPresent())
                    continue;

                List<Object> annotationValues = annotationNodeOptional.get().values;
                int maxInstances = 16;

                for (int j = 0; j < annotationValues.size(); j += 2) {
                    if (annotationValues.get(j).equals("maxInstances"))
                        maxInstances = (Integer) annotationValues.get(j + 1);
                }

                if (!hasAnnotation(method.visibleParameterAnnotations[i], "Lurbachyannick/approxflow/PublicOutput;"))
                    continue;

                String parameterTypeString = parameterTypes[i].getDescriptor();
                TypeSpecifier parameterType = TypeSpecifier.parse(parameterTypeString, new MutableInteger(0));

                if (!parameterType.isPrimitive())
                    throw new InvalidTransformationException("Output may only be of primitive type");

                PrimtiveType primitiveType = parameterType.asPrimitive();

                String methodQualifiedName = method.name + "_" + method.parameters.get(i).name;
                String arrayName = methodQualifiedName + "_array";
                String counterName = methodQualifiedName + "_counter";
                String arrayType = "[" + parameterTypeString;

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
                targetClass.fields.add(outputArray);

                FieldNode counter = new FieldNode(
                        Opcodes.ASM5,
                        Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC,
                        counterName,
                        "I",
                        null,
                        null
                );
                targetClass.fields.add(counter);

                InsnList newInstructions = new InsnList();
                newInstructions.add(new FieldInsnNode(Opcodes.GETSTATIC, targetClass.name, arrayName, arrayType));
                newInstructions.add(new FieldInsnNode(Opcodes.GETSTATIC, targetClass.name, counterName, "I"));
                newInstructions.add(new InsnNode(Opcodes.DUP));
                newInstructions.add(new InsnNode(Opcodes.ICONST_1));
                newInstructions.add(new InsnNode(Opcodes.IADD));
                newInstructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, targetClass.name, counterName, "I"));
                newInstructions.add(new IntInsnNode(primitiveType.getLoadLocalOpcode(), i));
                newInstructions.add(new InsnNode(primitiveType.getArrayStoreOpcode()));

                method.instructions.insertBefore(method.instructions.getFirst(), newInstructions);


                InsnList classInitInstructions = new InsnList();
                classInitInstructions.add(new LdcInsnNode(maxInstances));
                classInitInstructions.add(new IntInsnNode(Opcodes.NEWARRAY, primitiveType.getTypeOpcode()));
                classInitInstructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, targetClass.name, arrayName, arrayType));

                classInit.instructions.insertBefore(classInit.instructions.getFirst(), classInitInstructions);
            }
        }
    }
}
