package urbachyannick.approxflow.soot;

import org.objectweb.asm.tree.ClassNode;
import soot.*;
import urbachyannick.approxflow.codetransformation.*;

import java.util.stream.Stream;

public abstract class SootTransformation implements Transformation {
    protected abstract Stream<SootClass> applySoot(Stream<SootClass> classes) throws InvalidTransformationException;

    @Override
    public Stream<ClassNode> apply(Stream<ClassNode> asmInputClasses) throws InvalidTransformationException {
        AsmSootConverter.initSoot();
        Stream<SootClass> sootInputClasses = AsmSootConverter.toSoot(asmInputClasses);
        Stream<SootClass> sootOutputClasses = applySoot(sootInputClasses);
        return AsmSootConverter.toAsm(sootOutputClasses);
    }
}
