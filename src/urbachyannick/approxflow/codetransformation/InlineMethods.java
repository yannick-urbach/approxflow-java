package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.*;
import urbachyannick.approxflow.javasignatures.*;

import java.util.*;
import java.util.stream.*;

import static urbachyannick.approxflow.codetransformation.BytecodeUtil.*;

public class InlineMethods implements Transformation {

    private final InlinePreferences prefs;

    public InlineMethods() {
        prefs = new InlinePreferences(false, 3, false);
    }

    public InlineMethods(InlinePreferences prefs) {
        this.prefs = prefs;
    }

    @Override
    public Stream<ClassNode> apply(Stream<ClassNode> sourceClasses) throws InvalidTransformationException {
        List<ClassNode> classList = sourceClasses.collect(Collectors.toList());

        try {
            return classList.stream().map(sourceClass -> {
                ClassNode targetClass = new ClassNode(Opcodes.ASM5);
                CV visitor = new CV(sourceClass, classList, Opcodes.ASM5, targetClass);
                sourceClass.accept(visitor);

                return targetClass;
            });
        } catch (RuntimeInvalidTransformationException e) {
            throw new InvalidTransformationException(e);
        }
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

    private class CV extends ClassVisitor {
        private final List<ClassNode> classes;
        private final ClassNode class_;

        public CV(ClassNode class_, List<ClassNode> classes, int api, ClassVisitor classVisitor) {
            super(api, classVisitor);
            this.classes = classes;
            this.class_ = class_;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodNode method = findMethod(class_, class_.name, name, descriptor)
                    .orElseThrow(() -> new RuntimeInvalidTransformationException("Can not find visited method"));

            MV visitor = new MV(class_, method, classes, api, super.visitMethod(access, name, descriptor, signature, exceptions));
            LocalVariablesSorter sorter = new LocalVariablesSorter(access, descriptor, visitor);
            visitor.sorter = sorter;
            return sorter;
        }
    }

    private static class Candidate {
        public ClassNode containingClass;
        public MethodNode method;

        private Candidate(ClassNode containingClass, MethodNode method) {
            this.containingClass = containingClass;
            this.method = method;
        }

        public static List<Candidate> getCandidates(List<ClassNode> classes, ClassNode containingClass, MethodNode method) {
            List<Candidate> candidates = new ArrayList<>();
            Iterator<ClassNode> i = findDerived(containingClass, classes).iterator();

            while (i.hasNext()) {
                ClassNode derived = i.next();
                Optional<MethodNode> override = findMethod(derived, derived.name, method.name, method.desc);

                if (override.isPresent() && hasBody(override.get()))
                    candidates.add(new Candidate(derived, override.get()));
            }

            return candidates;
        }
    }

    private class MV extends MethodVisitor {
        public LocalVariablesSorter sorter;
        private final RecursionDepthManager recursionDepths;
        private final List<ClassNode> classes;
        private final ClassNode class_;
        private final MethodNode method;

        public MV(ClassNode class_, MethodNode method, List<ClassNode> classes, int api, MethodVisitor methodVisitor) {
            super(api, methodVisitor);
            this.classes = classes;
            recursionDepths = new RecursionDepthManager();
            this.class_ = class_;
            this.method = method;
        }

        private void remapAndWriteArguments(boolean hasThis, ClassNode owner, MethodNode method, Map<Integer, Integer> varMap) {
            int varIndex = 0;

            List<TypeSpecifier> argumentTypes = getArgumentTypes(method);

            // remap this
            if (hasThis) {
                varMap.put(varIndex, sorter.newLocal(Type.getObjectType(owner.name)));
                ++varIndex;
            }

            // remap arguments
            for (TypeSpecifier argumentType : argumentTypes) {
                varMap.put(varIndex, sorter.newLocal(Type.getType(argumentType.asTypeSpecifierString())));
                ++varIndex;
            }

            // write arguments to variables (inverse order)
            for (int i = argumentTypes.size() - 1; i >= 0; --i) {
                --varIndex;
                super.visitVarInsn(argumentTypes.get(i).asPrimitive().getStoreLocalOpcode(), varMap.get(varIndex));
            }

            // write this to variable
            if (hasThis) {
                --varIndex;
                super.visitVarInsn(PrimitiveType.ADDRESS.getStoreLocalOpcode(), varMap.get(varIndex));
            }

            assert varIndex == 0;
        }

        private void remapLocalVariables(MethodNode method, Map<Integer, Integer> varMap) {
            for (LocalVariableNode v : method.localVariables) {
                if (varMap.containsKey(v.index))
                    continue;

                varMap.put(v.index, sorter.newLocal(Type.getType(v.desc)));
            }
        }

        private List<TypeSpecifier> getArgumentTypes(MethodNode method) {
            return Arrays
                    .stream(Type.getArgumentTypes(method.desc))
                    .map(t -> TypeSpecifier.parse(t.getDescriptor(), new MutableInteger(0)))
                    .collect(Collectors.toList());
        }

        private boolean methodBlacklisted(MethodNode method) {
            if (hasAnnotation(method.visibleAnnotations, "Lurbachyannick/approxflow/PrivateInput;"))
                return true;

            if (hasAnnotation(method.visibleAnnotations, "Lurbachyannick/approxflow/PublicInput;"))
                return true;

            if (
                    method.visibleParameterAnnotations != null &&
                    Arrays.stream(method.visibleParameterAnnotations)
                            .anyMatch(a -> hasAnnotation(a, "Lurbachyannick/approxflow/PublicOutput;"))
            ) {
                return true;
            }

            return false;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {

            if (opcode == Opcodes.INVOKEDYNAMIC) {
                // not supported for now
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                return;
            }

            Optional<ClassNode> ownerClassOptional = findClass(classes.stream(), owner);
            Optional<MethodNode> calledMethodOptional = ownerClassOptional.flatMap(c -> findMethod(c, owner, name, descriptor));

            if (!calledMethodOptional.isPresent() || methodBlacklisted(calledMethodOptional.get())) {
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                return;
            }

            InlinePreferences preferences = InlinePreferences.get(
                    prefs,
                    ownerClassOptional.get(),
                    calledMethodOptional.get(),
                    class_,
                    method
            );

            if (!preferences.shouldInline() || recursionDepths.get(calledMethodOptional.get()) >= preferences.getRecursions()) {
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                return;
            }

            ClassNode ownerClass = ownerClassOptional.get();
            MethodNode calledMethod = calledMethodOptional.get();

            List<Candidate> candidates = null;

            if (!(opcode == Opcodes.INVOKESTATIC || opcode == Opcodes.INVOKESPECIAL)) {
                // need virtual call resolution
                // find candidates ahead of time to be able to abort in time if there is none
                candidates = Candidate.getCandidates(classes, ownerClassOptional.get(), calledMethodOptional.get());

                if (candidates.size() == 0) {
                    // leave call as it is
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                    return;
                }
            }

            Label returnLabel = new Label(); // label at the end of the inlined code to return to
            Map<Integer, Integer> varMap = new HashMap<>();
            remapAndWriteArguments(opcode != Opcodes.INVOKESTATIC, ownerClass, calledMethod, varMap);

            if (opcode == Opcodes.INVOKESTATIC || opcode == Opcodes.INVOKESPECIAL) {
                // no virtual call resolution
                inlineCall(ownerClass, calledMethod, varMap, returnLabel);
            } else {
                // virtual call resolution
                Label nullSkipLabel = new Label();
                super.visitVarInsn(PrimitiveType.ADDRESS.getLoadLocalOpcode(), varMap.get(0));
                super.visitJumpInsn(Opcodes.IFNONNULL, nullSkipLabel);
                emitException("java/lang/NullPointerException");
                super.visitLabel(nullSkipLabel);

                for (Candidate candidate : candidates) {
                    Label skipLabel = new Label();
                    super.visitVarInsn(PrimitiveType.ADDRESS.getLoadLocalOpcode(), varMap.get(0));
                    super.visitTypeInsn(Opcodes.INSTANCEOF, candidate.containingClass.name);
                    super.visitJumpInsn(Opcodes.IFEQ, skipLabel);
                    inlineCall(candidate.containingClass, candidate.method, varMap, returnLabel);
                    super.visitLabel(skipLabel);
                }
            }

            // throw if none of the candidates matches
            emitException("java/lang/AssertionError");

            super.visitLabel(returnLabel);
        }

        private void emitException(String type) {
            super.visitTypeInsn(Opcodes.NEW, type);
            super.visitInsn(Opcodes.DUP);
            super.visitMethodInsn(
                    Opcodes.INVOKESPECIAL,
                    type,
                    "<init>",
                    "()V",
                    false
            );
            super.visitInsn(Opcodes.ATHROW);
        }

        private void inlineCall(ClassNode owner, MethodNode method, Map<Integer, Integer> varMap, Label returnLabel) {
            varMap = new HashMap<>(varMap);

            recursionDepths.descend(method);

            TypeSpecifier returnType = getReturnType(method);
            remapLocalVariables(method, varMap);
            copyOver(method.instructions, varMap, returnLabel, returnType.asPrimitive());

            recursionDepths.ascend(method);
        }

        private void copyOver(InsnList instructionsToCopy, Map<Integer, Integer> variableMap, Label returnLabel, PrimitiveType returnType) {
            Map<LabelNode, Label> labelMap = new HashMap<>();

            Set<Integer> missingVariables = new HashSet<>();

            for (AbstractInsnNode instruction : instructionsToCopy) {
                switch(instruction.getType()) {
                    // need to remap local variables

                    case AbstractInsnNode.VAR_INSN: {
                        VarInsnNode varInsnNode = (VarInsnNode) instruction;

                        Integer mapped = variableMap.get(varInsnNode.var);

                        if (mapped == null) {
                            // track as missing to check for reads later
                            missingVariables.add(varInsnNode.var);
                            super.visitInsn(Opcodes.POP);
                        } else {
                            super.visitVarInsn(varInsnNode.getOpcode(), variableMap.get(varInsnNode.var));
                        }

                        // check if read from missing
                        if (
                                isLoadLocalOpcode(instruction.getOpcode()) &&
                                missingVariables.contains(varInsnNode.var)
                        ) {
                            throw new RuntimeInvalidTransformationException("Read from variable without debug information");
                        }

                        break;
                    }

                    case AbstractInsnNode.IINC_INSN: {
                        IincInsnNode iincInsnNode = (IincInsnNode) instruction;

                        Integer mapped = variableMap.get(iincInsnNode.var);

                        if (mapped == null) {
                            // track as missing to check for reads later
                            missingVariables.add(iincInsnNode.var);
                        } else {
                            super.visitIincInsn(variableMap.get(iincInsnNode.var), iincInsnNode.incr);
                        }

                        break;
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
                            instruction.accept(this); // recurses into this.visitMethodInsn for calls
                        break;
                    }
                }
            }
        }
    }
}
