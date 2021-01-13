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
        if (class_.isInterface())
            return;

        List<SootMethod> methods = new ArrayList<>(class_.getMethods());

        for (SootMethod m : methods)
            apply(m, classes);
    }

    private void apply(SootMethod method, List<SootClass> classes) throws InvalidTransformationException {
        if (!method.isConcrete())
            return;

        Body body = method.retrieveActiveBody();

        List<Tree<Loop>> loopHierarchy;

        // apply to topmost loops
        // regenerate block graph after every loop because the body changes
        while (true) {
            BlockGraph graph = new BriefBlockGraph(body);
            loopHierarchy = LoopFinder.findLoops(graph);

            if (loopHierarchy.size() == 0)
                break;

            apply(method, graph, loopHierarchy.get(0).getValue(), classes);
        }
    }

    private void apply(SootMethod method, BlockGraph graph, Loop loop, List<SootClass> classes) throws InvalidTransformationException {
        Body body = method.retrieveActiveBody();
        LocalUses localUses = LocalUses.Factory.newLocalUses(body);

        // find variables used in loop
        Unit loopHeadUnit = loop.getHeader().getHead();
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
        body.getUnits().insertBefore(blackboxCallStatement, loopHeadUnit);

        int i = 0;
        for (Value variable : externalVariableModifications) {
            SootField field = new SootField("field" + (i++), variable.getType(), Modifier.PUBLIC);
            returnClass.addField(field);
            Value fieldAccessStatement = Jimple.v().newInstanceFieldRef(returnObject, field.makeRef());
            Unit assignStatement = Jimple.v().newAssignStmt(variable, fieldAccessStatement);
            body.getUnits().insertBefore(assignStatement, loopHeadUnit);
        }

        loopHeadUnit.redirectJumpsToThisTo(blackboxCallStatement);
        loopBodyUnits.forEach(u -> body.getUnits().remove(u));

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
