package urbachyannick.approxflow.soot;

import soot.*;
import soot.toolkits.graph.*;
import urbachyannick.approxflow.trees.Tree;

import java.util.List;
import java.util.stream.Stream;

public class LoopReplacer extends SootTransformation {
    @Override
    protected Stream<SootClass> applySoot(Stream<SootClass> classes) {
        classes.forEach(this::apply);
        return classes;
    }

    private void apply(SootClass class_) {
        class_.getMethods().forEach(this::apply);
    }

    private void apply(SootMethod method) {
        Body body = method.retrieveActiveBody();
        BlockGraph graph = new BriefBlockGraph(body);

        List<Tree<Loop>> loopHierarchy = LoopFinder.findLoops(graph);

        // apply to topmost loops
        loopHierarchy.forEach(l -> apply(method, l.getValue()));
    }

    private void apply(SootMethod method, Loop loop) {
        // apply to loop
    }
}
