package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import urbachyannick.approxflow.javasignatures.*;

import java.util.*;
import java.util.stream.*;

import static urbachyannick.approxflow.codetransformation.BytecodeUtil.*;

public class Nondet {
    public static InsnList generateNondetRecursive(TypeSpecifier type, Stream<ClassNode> classes) throws InvalidTransformationException {
        InsnList instructions = new InsnList();
        generateNondetRecursive(type, classes.collect(Collectors.toList()), instructions);
        return instructions;
    }

    private static void generateNondetRecursive(TypeSpecifier type, List<ClassNode> classes, InsnList instructions) throws InvalidTransformationException {
        if (type.isPrimitive()) {
            instructions.add(
                    new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "org/cprover/CProver",
                            "nondet" + type.asPrimitive().getName(),
                            "()" + type.asTypeSpecifierString()
                    )
            );

            return;
        }

        if (type instanceof ClassName) {
            ClassName className = ((ClassName) type);

            ClassNode classNode = findClass(classes.stream(), className.asQualifiedName())
                    .orElseThrow(() -> new InvalidTransformationException("Can not generate nondet for " + type.asTypeSpecifierString()));

            /*
            MethodNode constructor = findNoArgConstructor(classNode)
                    .orElseThrow(() -> new InvalidTransformationException("Class " + type.asTypeSpecifierString() + " is missing parameterless constructor"));
            */
            instructions.add(new TypeInsnNode(Opcodes.NEW, className.asQualifiedName()));
            instructions.add(new InsnNode(Opcodes.DUP));
            instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, classNode.name, "<init>", "()V"));

            for (FieldNode field : classNode.fields) {
                instructions.add(new InsnNode(Opcodes.DUP));
                TypeSpecifier fieldType = TypeSpecifier.parse(field.desc, new MutableInteger(0));
                generateNondetRecursive(fieldType, classes, instructions);
                instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, classNode.name, field.name, field.desc));
            }

            Optional<MethodNode> checkInvariant = findMethod(classNode, classNode.name, "$$checkInvariant", "()V");
            checkInvariant.ifPresent(m -> {
                instructions.add(new InsnNode(Opcodes.DUP));
                instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, classNode.name, m.name, "()V"));
            });

            return;
        }

        throw new InvalidTransformationException("Can not generate nondet for " + type.asTypeSpecifierString());
    }
}
