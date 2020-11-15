package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import urbachyannick.approxflow.Unreachable;
import urbachyannick.approxflow.javasignatures.MutableInteger;
import urbachyannick.approxflow.javasignatures.TypeSpecifier;

import java.io.IOException;
import java.text.ParseException;

public class ReturnValueInput extends Transformation {
    @Override
    public void apply(ClassNode sourceClass, ClassNode targetClass) throws IOException, InvalidTransformationException {
        sourceClass.accept(targetClass);

        for (int i = 0; i < sourceClass.methods.size(); ++i) {
            MethodNode sourceMethod = sourceClass.methods.get(i);

            if (sourceMethod.visibleAnnotations == null)
                continue;

            boolean hasPrivateInputAnnotation = sourceMethod.visibleAnnotations.stream()
                    .anyMatch(a -> a.desc.equals("Lurbachyannick/approxflow/PrivateInput;"));

            if (!hasPrivateInputAnnotation)
                continue;

            TypeSpecifier returnType = null;
            try {
                returnType = TypeSpecifier.parse(Type.getReturnType(sourceMethod.desc).getDescriptor(), new MutableInteger(0));
            } catch (ParseException e) {
                throw new Unreachable();
            }

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
    }
}
