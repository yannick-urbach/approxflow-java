package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import urbachyannick.approxflow.javasignatures.*;

import java.util.*;
import java.util.stream.*;

import static urbachyannick.approxflow.codetransformation.BytecodeUtil.*;

public class MethodOfInterestTransform implements Transformation {

    @Override
    public Stream<ClassNode> apply(Stream<ClassNode> sourceClasses) throws InvalidTransformationException {
        Stream.Builder<ClassNode> streamBuilder = Stream.builder();

        List<ClassNode> sourceClassList = sourceClasses.collect(Collectors.toList());

        for (ClassNode sourceClass : sourceClassList)
            streamBuilder.accept(applyToClass(sourceClass, sourceClassList));

        return streamBuilder.build();
    }

    protected ClassNode applyToClass(ClassNode sourceClass, List<ClassNode> sourceClasses) throws InvalidTransformationException {
        ClassNode targetClass = new ClassNode(Opcodes.ASM5);
        sourceClass.accept(targetClass); // copy over

        // find methods with annotation
        List<MethodNode> methods = targetClass.methods.stream()
                .filter(m -> hasAnnotation(m.visibleAnnotations, "Lurbachyannick/approxflow/MethodOfInterest;"))
                .collect(Collectors.toList());

        if (methods.size() == 0)
            return targetClass;

        if (methods.size() > 1)
            throw new InvalidTransformationException("Must have at most one method of interest");

        if (methods.stream().anyMatch(m -> !hasFlag(m.access, Opcodes.ACC_STATIC)))
            throw new InvalidTransformationException("Method of interest must be static");

        if (findMainMethod(targetClass).isPresent())
            throw new InvalidTransformationException("Must not have a main method"); // because we'll generate one

        MethodNode method = methods.get(0);

        // create new main method
        MethodNode main = new MethodNode(Opcodes.ASM5, Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC, "main", "([Ljava/lang/String;)V", null, null);
        targetClass.methods.add(main);

        // add nondet*() calls for parameters
        for (Type t : Type.getArgumentTypes(method.desc)) {
            TypeSpecifier s = TypeSpecifier.parse(t.getDescriptor(), new MutableInteger(0));
            main.instructions.add(Nondet.generateNondetRecursive(s, sourceClasses.stream()));
        }

        // add call to method of interest
        main.instructions.add(
                new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        targetClass.name,
                        method.name,
                        method.desc
                )
        );

        // add output variable for return value (will be picked up by OutputVariable scanner)
        TypeSpecifier returnType = TypeSpecifier.parse(Type.getReturnType(method.desc).getDescriptor(), new MutableInteger(0));

        if (!returnType.isPrimitive())
            throw new InvalidTransformationException("Method of interest must have a primitive return type");

        FieldNode field = new FieldNode(
                Opcodes.ASM5,
                Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC,
                "$$val",
                returnType.asTypeSpecifierString(),
                null,
                null
        );

        field.visibleAnnotations = new ArrayList<>();
        field.visibleAnnotations.add(new AnnotationNode(Opcodes.ASM5, "Lurbachyannick/approxflow/PublicOutput;"));
        targetClass.fields.add(field);

        // add assignment to output variable
        main.instructions.add(
                new FieldInsnNode(
                        Opcodes.PUTSTATIC,
                        targetClass.name,
                        "$$val",
                        returnType.asTypeSpecifierString()
                )
        );

        main.instructions.add(new InsnNode(Opcodes.RETURN));

        return targetClass;
    }
}
