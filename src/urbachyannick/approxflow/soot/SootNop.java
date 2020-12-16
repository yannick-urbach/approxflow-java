package urbachyannick.approxflow.soot;

import soot.SootClass;

import java.util.stream.Stream;

public class SootNop extends SootTransformation {

    @Override
    protected Stream<SootClass> applySoot(Stream<SootClass> classes) {
        return classes;
    }
}
