package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import urbachyannick.approxflow.javasignatures.*;

import java.util.Iterator;
import java.util.stream.Stream;

public class ReturnValueInput implements Transformation {
    @Override
    public Stream<ClassNode> apply(Stream<ClassNode> sourceClasses) throws InvalidTransformationException {
        Stream.Builder<ClassNode> streamBuilder = Stream.builder();
        Iterator<ClassNode> i = sourceClasses.iterator();

        while (i.hasNext())
            streamBuilder.accept(applyToClass(i.next()));

        return streamBuilder.build();
    }

    private ClassNode applyToClass(ClassNode sourceClass) throws InvalidTransformationException {
        ClassNode targetClass = new ClassNode(Opcodes.ASM5);

        sourceClass.accept(targetClass);

        for (int i = 0; i < sourceClass.methods.size(); ++i) {
            MethodNode sourceMethod = sourceClass.methods.get(i);

            if (sourceMethod.visibleAnnotations == null)
                continue;

            boolean hasPrivateInputAnnotation = sourceMethod.visibleAnnotations.stream()
                    .anyMatch(a -> a.desc.equals("Lurbachyannick/approxflow/PrivateInput;"));

            if (!hasPrivateInputAnnotation)
                continue;

            TypeSpecifier returnType = TypeSpecifier.parse(Type.getReturnType(sourceMethod.desc).getDescriptor(), new MutableInteger(0));

            if (!returnType.isPrimitive())
                throw new InvalidTransformationException("Private inputs must be of primitive types (for now).");

            MethodNode targetMethod = targetClass.methods.get(i);

            targetMethod.instructions.clear();
            targetMethod.instructions.add(
                    new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "org/cprover/CProver",
                            "nondet" + returnType.asPrimitive().getName(),
                            "()" + returnType.asTypeSpecifierString()
                    )
            );

            targetMethod.instructions.add(new InsnNode(returnType.asPrimitive().getReturnOpcode()));
        }

        return targetClass;
    }
}
