package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.stream.*;

import static urbachyannick.approxflow.codetransformation.BytecodeUtil.*;

public class CountMaxIOInstances implements Transformation {
    @Override
    public Stream<ClassNode> apply(Stream<ClassNode> classes) throws InvalidTransformationException {
        List<ClassNode> classesList = classes.collect(Collectors.toList());
        ClassNode mainClass = findClassWithMainMethod(classesList.stream()).orElseThrow(() -> new InvalidTransformationException("Program must have a main method"));
        MethodNode mainMethod = findMainMethod(mainClass).get();

        return classesList.stream().map(c -> applyToClass(c, mainMethod.instructions));
    }

    private ClassNode applyToClass(ClassNode sourceClass, InsnList mainMethodInstructions) {
        ClassNode targetClass = new ClassNode(Opcodes.ASM5);
        sourceClass.accept(targetClass);
        targetClass.methods.forEach(m -> applyToMethod(targetClass, m, mainMethodInstructions));
        return targetClass;
    }

    private void applyToMethod(ClassNode owner, MethodNode method, InsnList mainMethodInstructions) {
        if (
                !anyHasAnnotation(method.visibleParameterAnnotations, "Lurbachyannick/approxflow/PublicOutput;") &&
                !hasAnnotation(method.visibleAnnotations, "Lurbachyannick/approxflow/PublicInput;")
        ) return;

        int callCount = 0;

        for (AbstractInsnNode i = mainMethodInstructions.getFirst(); i != null; i = i.getNext()) {
            if (i.getType() != AbstractInsnNode.METHOD_INSN)
                continue;

            MethodInsnNode mi = (MethodInsnNode) i;

            if (!(owner.name.equals(mi.owner) && method.name.equals(mi.name) && method.desc.equals(mi.desc)))
                continue;

            callCount++;
        }

        if (method.visibleAnnotations != null)
            applyMaxInstances(method.visibleAnnotations, "Lurbachyannick/approxflow/PublicInput;", callCount);

        if (method.visibleParameterAnnotations != null) {
            for (List<AnnotationNode> annotations : method.visibleParameterAnnotations)
                applyMaxInstances(annotations, "Lurbachyannick/approxflow/PublicOutput;", callCount);
        }
    }

    private void applyMaxInstances(List<AnnotationNode> annotations, String annotationType, int maxInstances) {
        Optional<AnnotationNode> annotation = getAnnotation(annotations, annotationType);

        if (!annotation.isPresent())
            return;

        int maxInstancesOld = annotation
                .flatMap(a -> getAnnotationValue(a, "maxInstances"))
                .map(v -> (Integer) v)
                .orElse(-1);

        if (maxInstancesOld >= 0) {
            if (maxInstancesOld > maxInstances)
                System.err.println("maxInstances set higher than maximum call count, using maximum call count");
            else
                return;
        }

        annotations.remove(annotation.get());

        AnnotationNode newAnnotation = new AnnotationNode(annotationType);
        newAnnotation.values = new ArrayList<Object>() {{
            add("maxInstances");
            add(maxInstances);
        }};

        annotations.add(newAnnotation);
    }
}
