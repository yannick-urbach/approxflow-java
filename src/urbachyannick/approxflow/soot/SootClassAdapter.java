package urbachyannick.approxflow.soot;

import org.objectweb.asm.tree.ClassNode;
import soot.SootClass;
import soot.asm.*;

class SootClassAdapter extends SootClassBuilder {
    private SootClassAdapter(ClassNode class_) {
        super(new SootClass(AsmUtil.toQualifiedName(class_.name)));
        class_.accept(this);
    }

    public static SootClass toSoot(ClassNode class_) {
        return new SootClassAdapter(class_).getKlass();
    }
}
