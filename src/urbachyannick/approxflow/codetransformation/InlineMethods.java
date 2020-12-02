package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.*;
import urbachyannick.approxflow.javasignatures.MutableInteger;
import urbachyannick.approxflow.javasignatures.PrimtiveType;
import urbachyannick.approxflow.javasignatures.TypeSpecifier;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static urbachyannick.approxflow.MiscUtil.or;
import static urbachyannick.approxflow.codetransformation.BytecodeUtil.*;

public class InlineMethods extends Transformation {

    @Override
    public void apply(ClassNode sourceClass, ClassNode targetClass) throws IOException, InvalidTransformationException {
        CV visitor = new CV(sourceClass, Opcodes.ASM5, targetClass);
        sourceClass.accept(visitor);
    }

    private static class RecursionDepthManager {
        private final Map<MethodNode, Integer> recursionDepths;

        public RecursionDepthManager() {
            recursionDepths = new HashMap<>();
        }

        public int get(MethodNode method) { return recursionDepths.getOrDefault(method, 0); }
        public void descend(MethodNode method) { recursionDepths.put(method, get(method) + 1); }
        public void ascend(MethodNode method) { recursionDepths.put(method, get(method) - 1); }
    }

    private static class CV extends ClassVisitor {
        private final ClassNode class_;
        private final Optional<Integer> maxRecursions;

        public CV(ClassNode class_, int api, ClassVisitor classVisitor) {
            super(api, classVisitor);
            this.class_ = class_;

            Optional<AnnotationNode> annotationNode = getAnnotation(class_.visibleAnnotations, "Lurbachyannick/approxflow/Inline;");
            maxRecursions = annotationNode.flatMap(a -> getAnnotationValue(a, "recursions")).map(v -> (int) v);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MV visitor = new MV(class_, maxRecursions, api, super.visitMethod(access, name, descriptor, signature, exceptions));
            LocalVariablesSorter sorter = new LocalVariablesSorter(access, descriptor, visitor);
            visitor.sorter = sorter;
            return sorter;
        }
    }

    private static class MV extends MethodVisitor {
        public LocalVariablesSorter sorter;
        private final RecursionDepthManager recursionDepths;
        private final ClassNode class_;
        private final Optional<Integer> classMaxRecursions;

        public MV(ClassNode class_, Optional<Integer> classMaxRecursions, int api, MethodVisitor methodVisitor) {
            super(api, methodVisitor);
            this.class_ = class_;
            recursionDepths = new RecursionDepthManager();
            this.classMaxRecursions = classMaxRecursions;
        }

        private List<Integer> remapVariables(MethodNode method) {
            return method.localVariables.stream()
                    .map(v -> sorter.newLocal(Type.getType(v.desc)))
                    .collect(Collectors.toList());
        }

        private List<TypeSpecifier> getArgumentTypes(MethodNode method) {
            return Arrays
                    .stream(Type.getArgumentTypes(method.desc))
                    .map(t -> TypeSpecifier.parse(t.getDescriptor(), new MutableInteger(0)))
                    .collect(Collectors.toList());
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {

            Optional<MethodNode> calledMethodOptional = findMethod(class_, owner, name, descriptor);
            Optional<AnnotationNode> annotationNode = calledMethodOptional.flatMap(m -> getAnnotation(m.visibleAnnotations, "Lurbachyannick/approxflow/Inline;"));
            Optional<Integer> maxRecursions = or(
                    annotationNode.flatMap(a -> getAnnotationValue(a, "recursions")).map(v -> (int) v),
                    classMaxRecursions
            );

            if (
                    !calledMethodOptional.isPresent() ||
                    !maxRecursions.isPresent() ||
                    recursionDepths.get(calledMethodOptional.get()) >= maxRecursions.get()
            ) {
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                return;
            }

            MethodNode calledMethod = calledMethodOptional.get();

            recursionDepths.descend(calledMethod);

            List<Integer> variableMap = remapVariables(calledMethod);
            List<TypeSpecifier> argumentTypes = getArgumentTypes(calledMethod);
            TypeSpecifier returnType = TypeSpecifier.parse(Type.getReturnType(calledMethod.desc).getDescriptor(), new MutableInteger(0));

            int argLocalVariableIndex = 0;

            // write this to first local variable
            if (opcode == Opcodes.INVOKEVIRTUAL)
                visitVarInsn(PrimtiveType.ADDRESS.getStoreLocalOpcode(), variableMap.get(argLocalVariableIndex++));

            // write arguments to subsequent local variables
            for (TypeSpecifier argumentType : argumentTypes)
                visitVarInsn(argumentType.asPrimitive().getStoreLocalOpcode(), variableMap.get(argLocalVariableIndex++));

            // label at the end of the inlined code to return to
            Label returnLabel = new Label();

            Map<LabelNode, Label> labelMap = new HashMap<>();

            for (AbstractInsnNode instruction : calledMethod.instructions) {
                switch(instruction.getType()) {
                    // need to remap local variables
                    case AbstractInsnNode.VAR_INSN: {
                        VarInsnNode varInsnNode = (VarInsnNode) instruction;
                        super.visitVarInsn(varInsnNode.getOpcode(), variableMap.get(varInsnNode.var));
                        break;
                    }

                    case AbstractInsnNode.IINC_INSN: {
                        IincInsnNode iincInsnNode = (IincInsnNode) instruction;
                        super.visitIincInsn(variableMap.get(iincInsnNode.var), iincInsnNode.incr);
                    }


                    // need to remap labels
                    case AbstractInsnNode.LABEL: {
                        LabelNode labelNode = (LabelNode) instruction;
                        Label mapped = labelMap.get(labelNode);

                        if (mapped == null) {
                            mapped = new Label();
                            labelMap.put(labelNode, mapped);
                        }

                        visitLabel(mapped);
                        break;
                    }

                    case AbstractInsnNode.JUMP_INSN: {
                        JumpInsnNode jumpInsnNode = (JumpInsnNode) instruction;
                        Label mapped = labelMap.get(jumpInsnNode.label);

                        if (mapped == null) {
                            mapped = new Label();
                            labelMap.put(jumpInsnNode.label, mapped);
                        }

                        visitJumpInsn(jumpInsnNode.getOpcode(), mapped);
                        break;
                    }

                    // somehow messes things up, so skip for now (not essential)
                    case AbstractInsnNode.LINE:
                        break;

                    // simply copy, unless it's a return
                    default: {
                        if (instruction.getOpcode() == returnType.asPrimitive().getReturnOpcode())
                            visitJumpInsn(Opcodes.GOTO, returnLabel);
                        else
                            instruction.accept(this);
                        break;
                    }
                }
            }

            super.visitLabel(returnLabel);
            recursionDepths.ascend(calledMethod);
        }
    }
}