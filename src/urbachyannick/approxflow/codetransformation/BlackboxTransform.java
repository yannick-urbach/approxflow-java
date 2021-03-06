package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import urbachyannick.approxflow.javasignatures.*;

import java.util.*;
import java.util.stream.*;

import static urbachyannick.approxflow.codetransformation.BytecodeUtil.*;

public class BlackboxTransform implements Transformation {
    private final int callIndex;

    public BlackboxTransform(int callIndex) {
        this.callIndex = callIndex;
    }

    @Override
    public Stream<ClassNode> apply(Stream<ClassNode> classes) throws InvalidTransformationException {
        List<ClassNode> classList = classes.collect(Collectors.toList());
        Stream.Builder<ClassNode> streamBuilder = Stream.builder();

        for (ClassNode sourceClass : classList) {
            streamBuilder.accept(applyToClass(sourceClass, classList));
        }

        streamBuilder.accept(blackboxCounterClass());

        return streamBuilder.build();
    }

    private ClassNode applyToClass(ClassNode sourceClass, List<ClassNode> sourceClasses) throws InvalidTransformationException {
        ClassNode targetClass = new ClassNode(Opcodes.ASM5);
        sourceClass.accept(targetClass);

        for (int i = 0; i < targetClass.methods.size(); ++i) {
            MethodNode method = targetClass.methods.get(i);

            if (isMainMethod(method))
                applyToMainMethod(method, sourceClasses);
            else if (hasAnnotation(method.visibleAnnotations, "Lurbachyannick/approxflow/Blackbox;"))
                applyToBlackboxMethod(targetClass, method, sourceClasses);
        }

        return targetClass;
    }

    private void applyToMainMethod(MethodNode mainMethod, List<ClassNode> sourceClasses) {

    }

    private ClassNode blackboxCounterClass() {
        ClassNode class_ = new ClassNode(Opcodes.ASM5);
        class_.name = "$$BlackboxCounter";
        class_.version = 50;
        class_.superName = "java/lang/Object";

        FieldNode callCounter = new FieldNode(
                Opcodes.ASM5,
                Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC,
                "calls",
                "I",
                null,
                0
        );
        class_.fields.add(callCounter);

        return class_;
    }

    private void applyToBlackboxMethod(ClassNode class_, MethodNode method, List<ClassNode> sourceClasses) throws InvalidTransformationException {
        String parameterOutputMethodName = "$$" + method.name + "$$paramout";

        List<TypeSpecifier> argumentTypes = getArgumentTypes(method).collect(Collectors.toList());
        TypeSpecifier returnType = getReturnType(method);

        MethodNode parameterOutputMethod = new MethodNode(
                Opcodes.ASM5,
                Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC,
                parameterOutputMethodName,
                String.format("(%s)V",
                        argumentTypes.stream()
                                .map(TypeSpecifier::asTypeSpecifierString)
                                .collect(Collectors.joining())
                ),
                null,
                null
        ) {{
            visibleAnnotations = new ArrayList<AnnotationNode>() {{
                add(new AnnotationNode(Opcodes.ASM5, "Lurbachyannick/approxflow/$$BlackboxOutput;"));
            }};

            instructions.add(new LabelNode());
            instructions.add(new InsnNode(Opcodes.RETURN));
            instructions.add(new LabelNode());

            maxLocals = argumentTypes.size();
        }};
        class_.methods.add(parameterOutputMethod);

        LabelNode elseLabel = new LabelNode();

        InsnList blackboxCode = new InsnList() {{
            // if (counter++ < callIndex) {
            //     return nondet();
            // } else {
            //     paramout(...);
            //     throw new AssertionError();
            // }

            add(new FieldInsnNode(Opcodes.GETSTATIC, "$$BlackboxCounter", "calls", "I"));
            add(new InsnNode(Opcodes.DUP));
            add(new InsnNode(Opcodes.ICONST_1));
            add(new InsnNode(Opcodes.IADD));
            add(new FieldInsnNode(Opcodes.PUTSTATIC, "$$BlackboxCounter", "calls", "I"));
            add(new LdcInsnNode(callIndex));
            add(new JumpInsnNode(Opcodes.IF_ICMPGE, elseLabel));

            // then
            add(Nondet.generateNondetRecursive(returnType, sourceClasses.stream()));
            add(new InsnNode(returnType.asPrimitive().getReturnOpcode()));

            // else
            add(elseLabel);
            IntStream // load all parameters
                    .range(0, argumentTypes.size())
                    .mapToObj(i -> new VarInsnNode(argumentTypes.get(i).asPrimitive().getLoadLocalOpcode(), i))
                    .collect(Collectors.toList())
                    .forEach(this::add);
            add(new MethodInsnNode(Opcodes.INVOKESTATIC, class_.name, parameterOutputMethod.name, parameterOutputMethod.desc));
            add(AddDummyThrow.dummyThrow());
        }};

        method.instructions = blackboxCode;
    }
}
