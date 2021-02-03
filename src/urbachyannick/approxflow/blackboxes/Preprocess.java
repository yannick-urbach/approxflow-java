package urbachyannick.approxflow.blackboxes;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import urbachyannick.approxflow.codetransformation.*;
import urbachyannick.approxflow.javasignatures.TypeSpecifier;

import java.util.*;
import java.util.stream.*;

import static urbachyannick.approxflow.codetransformation.BytecodeUtil.*;

public class Preprocess implements Transformation {
    private final SourcesSinksPair sourcesSinks;

    public Preprocess(SourcesSinksPair sourcesSinks) {
        this.sourcesSinks = sourcesSinks;
    }

    @Override
    public Stream<ClassNode> apply(Stream<ClassNode> classes) {
        List<ClassNode> targetClasses = classes
                .map(this::applyToClass)
                .collect(Collectors.toList());


        Map<MethodNode, MethodNode> blackboxInputMethods = new HashMap<>();

        Iterator<BlackboxCallInfo> i = sourcesSinks
                .getBlackboxSources()
                .map(c -> c.getInfo(targetClasses))
                .iterator();

        while (i.hasNext())
            replaceWithInputMethod(i.next(), targetClasses, blackboxInputMethods);


        Map<MethodNode, MethodNode> blackboxOutputMethods = new HashMap<>();

        Iterator<BlackboxCallInfo> j = sourcesSinks
                .getBlackboxSinks()
                .map(c -> c.getInfo(targetClasses))
                .iterator();

        while (j.hasNext())
            replaceWithOutputMethod(j.next(), targetClasses, blackboxOutputMethods);

        return targetClasses.stream();
    }

    private ClassNode applyToClass(ClassNode sourceClass) {
        ClassNode targetClass = new ClassNode(Opcodes.ASM5);
        sourceClass.accept(targetClass);
        targetClass.methods.forEach(this::applyToMethod);
        targetClass.fields.forEach(this::applyToField);
        return targetClass;
    }

    private void applyToMethod(MethodNode method) {
        if (!sourcesSinks.includesInput()) {
            replaceAnnotation(
                    method.visibleAnnotations,
                    "Lurbachyannick/approxflow/PrivateInput;",
                    () -> new AnnotationNode("Lurbachyannick/approxflow/PublicInput;")
            );
        }

        if (!sourcesSinks.includesOutput() && method.visibleParameterAnnotations != null) {
            for (List<AnnotationNode> annotations : method.visibleParameterAnnotations) {
                removeAnnotation(
                        annotations,
                        "Lurbachyannick/approxflow/PublicOutput;"
                );
            }
        }

        replaceAnnotation(
                method.visibleAnnotations,
                "Lurbachyannick/approxflow/Blackbox;",
                () -> new AnnotationNode("Lurbachyannick/approxflow/PublicInput;")
        );
    }

    private void applyToField(FieldNode field) {
        if (!sourcesSinks.includesOutput()) {
            removeAnnotation(
                    field.visibleAnnotations,
                    "Lurbachyannick/approxflow/PublicOutput;"
            );
        }
    }


    private static MethodNode replaceWithInputMethod(BlackboxCallInfo call, List<ClassNode> classes, Map<MethodNode, MethodNode> blackboxInputMethods) {

        MethodNode inputMethod = blackboxInputMethods.get(call.getCalledMethod());

        if (inputMethod == null) {

            TypeSpecifier returnType = getReturnType(call.getCalledMethod());

            inputMethod = new MethodNode(
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                    call.getCalledMethod().name + "$$input",
                    call.getCalledMethod().desc,
                    null,
                    null
            );

            inputMethod.visibleAnnotations = new ArrayList<AnnotationNode>() {{
                add(new AnnotationNode(Opcodes.ASM5, "Lurbachyannick/approxflow/PrivateInput;"));
            }};

            // this will be replaced by ReturnValueInput, but we have to return something.
            inputMethod.instructions.add(new LdcInsnNode(0));
            inputMethod.instructions.add(new InsnNode(returnType.asPrimitive().getReturnOpcode()));

            call.getCalledMethodOwner().methods.add(inputMethod);
            blackboxInputMethods.put(call.getCalledMethod(), inputMethod);
        }

        call.getCall().name = inputMethod.name;

        return inputMethod;
    }

    private static MethodNode replaceWithOutputMethod(BlackboxCallInfo call, List<ClassNode> classes, Map<MethodNode, MethodNode> blackboxOutputMethods) {

        MethodNode outputMethod = blackboxOutputMethods.get(call.getCalledMethod());

        if (outputMethod == null) {

            List<TypeSpecifier> argumentTypes = getArgumentTypes(call.getCalledMethod()).collect(Collectors.toList());
            TypeSpecifier returnType = getReturnType(call.getCalledMethod());

            outputMethod = new MethodNode(
                    Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC,
                    call.getCalledMethod().name + "$$output",
                    call.getCalledMethod().desc,
                    null,
                    null
            );

            outputMethod.visibleAnnotations = new ArrayList<AnnotationNode>() {{
                add(new AnnotationNode(Opcodes.ASM5, "Lurbachyannick/approxflow/$$BlackboxOutput;"));
                add(new AnnotationNode(Opcodes.ASM5, "Lurbachyannick/approxflow/PublicInput;"));
            }};

            // this will be replaced by ReturnValueInput, but we have to return something.
            outputMethod.instructions.add(new LdcInsnNode(0));
            outputMethod.instructions.add(new InsnNode(returnType.asPrimitive().getReturnOpcode()));

            outputMethod.maxLocals = argumentTypes.size();

            call.getCalledMethodOwner().methods.add(outputMethod);
            blackboxOutputMethods.put(call.getCalledMethod(), outputMethod);
        }

        call.getCall().name = outputMethod.name;

        return outputMethod;
    }
}
