package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.stream.*;

import static urbachyannick.approxflow.codetransformation.BytecodeUtil.*;

public class IOOrderOutput implements Transformation {
    private static final String outputMethodName = "$$ioOrder";
    private static final String outputMethodDesc = "(I)V";

    @Override
    public Stream<ClassNode> apply(Stream<ClassNode> classes) throws InvalidTransformationException {
        List<ClassNode> classesList = classes.collect(Collectors.toList());
        return classesList.stream().map(c -> findMainMethod(c).map(m -> applyToMainClass(c, classesList)).orElse(c));
    }

    private ClassNode applyToMainClass(ClassNode sourceClass, List<ClassNode> classes) {
        ClassNode targetClass = new ClassNode(Opcodes.ASM5);
        sourceClass.accept(targetClass);

        MethodNode mainMethod = findMainMethod(targetClass).get();
        applyToMainMethod(targetClass, mainMethod, classes);
        return targetClass;
    }

    private void applyToMainMethod(ClassNode mainClass, MethodNode mainMethod, List<ClassNode> classes) {
        Map<MethodNode, Integer> ids = new HashMap<>();
        int callCount = 0;

        for (AbstractInsnNode i = mainMethod.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i.getType() != AbstractInsnNode.METHOD_INSN)
                continue;

            MethodInsnNode mi = (MethodInsnNode) i;

            if (mainClass.name.equals(mi.owner) && outputMethodName.equals(mi.name) && outputMethodDesc.equals(mi.desc))
                continue;

            Optional<ClassNode> owner = findClass(classes.stream(), mi.owner);

            if (!owner.isPresent())
                continue;

            Optional<MethodNode> method = findMethod(owner.get(), mi.owner, mi.name, mi.desc);

            if (!method.isPresent())
                continue;

            if (method.get().visibleParameterAnnotations == null)
                continue;

            if (
                    !anyHasAnnotation(method.get().visibleParameterAnnotations, "Lurbachyannick/approxflow/PublicOutput;") &&
                    !hasAnnotation(method.get().visibleAnnotations, "Lurbachyannick/approxflow/PublicInput;")
            ) continue;

            // add 1 so 0 isn't used; 0 is the default, so no IO at end would otherwise be indistinguishable from IO
            // with index 0.
            Integer id = ids.computeIfAbsent(method.get(), m -> ids.size() + 1);

            mainMethod.instructions.insert(i, new InsnList() {{
                add(new LdcInsnNode(id));
                add(new MethodInsnNode(Opcodes.INVOKESTATIC, mainClass.name, outputMethodName, outputMethodDesc));
            }});

            callCount++;
        }

        addIOOrderOutputMethod(mainClass, callCount);
    }

    private void addIOOrderOutputMethod(ClassNode mainClass, int callCount) {
        MethodNode outputMethod = new MethodNode(
                Opcodes.ASM5,
                Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC,
                outputMethodName,
                "(I)V",
                null,
                null
        ) {{
            visibleParameterAnnotations = new List[]{
                    new ArrayList<AnnotationNode>() {{
                        add(new AnnotationNode(Opcodes.ASM5, "Lurbachyannick/approxflow/PublicOutput;") {{
                            values = new ArrayList<Object>() {{
                                add("maxInstances");
                                add(callCount);
                            }};
                        }});
                    }}
            };

            instructions.add(new LabelNode());
            instructions.add(new InsnNode(Opcodes.RETURN));
            instructions.add(new LabelNode());

            maxLocals = 1;
        }};

        mainClass.methods.add(outputMethod);
    }
}
