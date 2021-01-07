package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.stream.*;

import static urbachyannick.approxflow.codetransformation.BytecodeUtil.*;

public class ObjectInvariants extends Transformation.PerClass {

    @Override
    protected ClassNode applyToClass(ClassNode sourceClass) throws InvalidTransformationException {
        ClassNode targetClass = new ClassNode(Opcodes.ASM5);
        sourceClass.accept(targetClass);

        List<MethodNode> invariants = targetClass.methods.stream()
                .filter(m -> hasAnnotation(m.visibleAnnotations, "Lurbachyannick/approxflow/Invariant;"))
                .collect(Collectors.toList());

        if (invariants.size() == 0)
            return targetClass;

        for(MethodNode method : invariants)
            checkSignature(method);

        checkForPublicFields(targetClass);

        MethodNode checkInvariant = generateCheckInvariant(targetClass, invariants.stream());

        targetClass.methods.stream()
                .filter(m -> hasBody(m) && !hasFlag(m.access, Opcodes.ACC_STATIC) && !invariants.contains(m))
                .forEach(m -> applyToMethod(targetClass, m, checkInvariant));

        targetClass.methods.add(checkInvariant);

        return targetClass;
    }

    private void applyToMethod(ClassNode class_, MethodNode method, MethodNode checkInvariant) {
        InsnList body = method.instructions;

        for (AbstractInsnNode n = body.getFirst(); n != null; n = n.getNext()) {
            if (isReturnOpcode(n.getOpcode())) {
                InsnList instructions = new InsnList() {{
                    add(new VarInsnNode(Opcodes.ALOAD, 0)); // load this
                    add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, class_.name, checkInvariant.name, "()V")); // check invariant
                }};

                method.instructions.insertBefore(n, instructions);
            }
        }
    }

    private void checkSignature(MethodNode method) throws InvalidTransformationException {
        if (hasFlag(method.access, Opcodes.ACC_STATIC))
            throw new InvalidTransformationException("Object invariant must not be static");

        if (!method.desc.equals("()Z"))
            throw new InvalidTransformationException("Object invariant must be parameterless and return boolean");
    }

    private void checkForPublicFields(ClassNode class_) {
        boolean hasNonPrivateFields = class_.fields.stream()
                .anyMatch(f ->
                        hasFlag(f.access, Opcodes.ACC_PUBLIC) ||
                        hasFlag(f.access, Opcodes.ACC_PROTECTED)
                );

        if (hasNonPrivateFields) {
            System.err.println(class_.name + " has non-private fields. Invariant handling might not work correctly.");
        }
    }

    private MethodNode generateCheckInvariant(ClassNode class_, Stream<MethodNode> invariants) {
        MethodNode result = new MethodNode(Opcodes.ASM5, Opcodes.ACC_PUBLIC, "$$checkInvariant", "()V", null, null);

        result.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // load this

        invariants.forEach(m -> {
            result.instructions.add(new InsnNode(Opcodes.DUP)); // duplicate this
            result.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, class_.name, m.name, "()Z")); // invoke invariant
            result.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "org/cprover/CProver", "assume", "(Z)V")); // assume result
        });

        result.instructions.add(new InsnNode(Opcodes.POP)); // pop unused this
        result.instructions.add(new InsnNode(Opcodes.RETURN)); // return

        return result;
    }
}
