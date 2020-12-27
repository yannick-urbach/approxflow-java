package urbachyannick.approxflow.soot;

import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.tagkit.*;
import soot.toolkits.graph.*;
import soot.toolkits.scalar.*;
import urbachyannick.approxflow.codetransformation.InvalidTransformationException;
import urbachyannick.approxflow.trees.Tree;

import java.util.*;
import java.util.stream.*;

import static urbachyannick.approxflow.soot.SootUtil.getUnits;

public class LoopReplacer extends SootTransformation {
    private int methodsGenerated = 0;

    @Override
    protected Stream<SootClass> applySoot(Stream<SootClass> classes) throws InvalidTransformationException {
        List<SootClass> classList = classes.collect(Collectors.toList());

        int classCount = classList.size();

        for (int i = 0; i < classCount; ++i)
            apply(classList.get(i), classList);

        return classList.stream();
    }

    private void apply(SootClass class_, List<SootClass> classes) throws InvalidTransformationException {
        List<SootMethod> methods = new ArrayList<>(class_.getMethods());

        for (SootMethod m : methods)
            apply(m, classes);
    }

    private void apply(SootMethod method, List<SootClass> classes) throws InvalidTransformationException {
        Body body = method.retrieveActiveBody();
        BlockGraph graph = new BriefBlockGraph(body);

        List<Tree<Loop>> loopHierarchy = LoopFinder.findLoops(graph);

        // apply to topmost loops
        for (Tree<Loop> t : loopHierarchy) {
            apply(method, t.getValue(), classes);
        }
    }

    private static class UsesAnalysis {
        public List<Value> accessed;
        public List<Value> modified;

        public UsesAnalysis(Stream<Block> blocks, LocalUses localUses, Stream<Value> variables) {
            List<UnitValueBoxPair> variableUses = getVariableUsesInBlocks(blocks, localUses, variables)
                    .collect(Collectors.toList());

            modified = variableUses.stream()
                    .filter(UsesAnalysis::isModification)
                    .map(u -> u.valueBox.getValue())
                    .distinct()
                    .collect(Collectors.toList());

            accessed = variableUses.stream()
                    .filter(UsesAnalysis::isAccess)
                    .map(u -> u.valueBox.getValue())
                    .distinct()
                    .collect(Collectors.toList());
        }

        private static Stream<UnitValueBoxPair> getVariableUsesInBlocks(Stream<Block> blocks, LocalUses localUses, Stream<Value> variables) {
            Set<Value> variablesSet = variables.collect(Collectors.toSet());

            List<Block> blockList = blocks.collect(Collectors.toList());

            return blockList.stream()
                    .flatMap(block -> getUnits(block).flatMap(u -> localUses.getUsesOf(u).stream()))
                    .filter(uvbp -> uvbp.valueBox instanceof ImmediateBox)
                    .filter(uvbp -> variablesSet.contains(uvbp.valueBox.getValue()));
        }

        private static boolean isAccess(UnitValueBoxPair use) {
            return !isModification(use);
        }

        private static boolean isModification(UnitValueBoxPair use) {
            return (
                    use.unit instanceof JAssignStmt &&
                    ((JAssignStmt) use.unit).leftBox.getValue().equals(use.valueBox.getValue())
            );
        }
    }

    private void apply(SootMethod method, Loop loop, List<SootClass> classes) throws InvalidTransformationException {
        Body body = method.retrieveActiveBody();
        BlockGraph graph = new BriefBlockGraph(body);
        LocalUses localUses = LocalUses.Factory.newLocalUses(body);

        // find variables used in loop
        List<Value> variables = body.getLocals().stream().map(l -> (Value) l).collect(Collectors.toList());
        List<Block> loopBodyBlocks = loop.getLoopBodyBlocks().collect(Collectors.toList());

        List<Block> otherBlocks = graph.getBlocks().stream()
                .filter(b -> !loopBodyBlocks.contains(b))
                .collect(Collectors.toList());

        List<Unit> loopBodyUnits = loopBodyBlocks.stream().flatMap(SootUtil::getUnits).collect(Collectors.toList());
        List<Unit> otherUnits = otherBlocks.stream().flatMap(SootUtil::getUnits).collect(Collectors.toList());

        List<Value> externalVariableAccesses = otherBlocks.stream()
                .flatMap(block -> getUnits(block).flatMap(u -> localUses.getUsesOf(u).stream()))
                .filter(uvbp -> loopBodyUnits.contains(uvbp.unit))
                .map(uvbp -> uvbp.valueBox.getValue())
                .distinct()
                .collect(Collectors.toList());

        List<Value> externalVariableModifications = loopBodyBlocks.stream()
                .flatMap(block -> getUnits(block).flatMap(u -> localUses.getUsesOf(u).stream()))
                .filter(uvbp -> otherUnits.contains(uvbp.unit))
                .map(uvbp -> uvbp.valueBox.getValue())
                .distinct()
                .collect(Collectors.toList());

        // create return class
        SootClass returnClass = new SootClass("BlackboxReturnType$$" + methodsGenerated, Modifier.PUBLIC);
        Scene.v().addClass(returnClass);

        // create blackbox method
        SootMethod blackboxMethod = createBlackboxMethod(
                externalVariableAccesses.stream().map(Value::getType),
                returnClass.getType()
        );

        classes.add(returnClass);
        method.getDeclaringClass().addMethod(blackboxMethod);

        // generate blackbox method call
        Local returnObject = Jimple.v().newLocal("returnValue$$" + methodsGenerated, returnClass.getType());
        body.getLocals().add(returnObject);

        Value blackboxCallExpression = Jimple.v().newStaticInvokeExpr(blackboxMethod.makeRef(), externalVariableAccesses);

        Unit blackboxCallStatement = Jimple.v().newAssignStmt(
                returnObject,
                blackboxCallExpression
        );
        body.getUnits().insertBefore(blackboxCallStatement, loop.getHeader().getHead());

        int i = 0;
        for (Value variable : externalVariableModifications) {
            SootField field = new SootField("field" + (i++), variable.getType(), Modifier.PUBLIC);
            returnClass.addField(field);
            Value fieldAccessStatement = Jimple.v().newInstanceFieldRef(returnObject, field.makeRef());
            Unit assignStatement = Jimple.v().newAssignStmt(variable, fieldAccessStatement);
            body.getUnits().insertBefore(assignStatement, loop.getHeader().getHead());
        }

        loop.getHeader().getHead().redirectJumpsToThisTo(blackboxCallStatement);

        for (Block b : loopBodyBlocks)
            getUnits(b).forEach(u -> body.getUnits().remove(u));

        methodsGenerated++;
    }

    private SootMethod createBlackboxMethod(Stream<Type> parameters, Type returnType) {
        String name = "blackbox$$" + (methodsGenerated);
        SootMethod method = new SootMethod(name, parameters.collect(Collectors.toList()), returnType, Modifier.STATIC | Modifier.PUBLIC);
        VisibilityAnnotationTag vat = new VisibilityAnnotationTag(AnnotationConstants.RUNTIME_VISIBLE);
        vat.addAnnotation(new AnnotationTag("Lurbachyannick/approxflow/Blackbox;"));
        method.addTag(vat);
        JimpleBody body = Jimple.v().newBody(method);
        method.setActiveBody(body);
        body.getUnits().add(Jimple.v().newReturnStmt(NullConstant.v()));
        return method;
    }
}
