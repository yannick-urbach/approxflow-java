package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import urbachyannick.approxflow.javasignatures.*;

import java.util.*;
import java.util.stream.*;

public class ReturnValueInput implements Transformation {
    @Override
    public Stream<ClassNode> apply(Stream<ClassNode> sourceClasses) throws InvalidTransformationException {
        Stream.Builder<ClassNode> streamBuilder = Stream.builder();

        List<ClassNode> sourceClassList = sourceClasses.collect(Collectors.toList());

        for (ClassNode sourceClass : sourceClassList)
            streamBuilder.accept(applyToClass(sourceClass, sourceClassList));

        return streamBuilder.build();
    }

    private ClassNode applyToClass(ClassNode sourceClass, List<ClassNode> classes) throws InvalidTransformationException {
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
            MethodNode targetMethod = targetClass.methods.get(i);

            targetMethod.instructions.clear();
            targetMethod.instructions.add(Nondet.generateNondetRecursive(returnType, classes.stream()));
            targetMethod.instructions.add(new InsnNode(returnType.asPrimitive().getReturnOpcode()));
        }

        return targetClass;
    }
}
