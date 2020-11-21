package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import urbachyannick.approxflow.javasignatures.*;

import java.util.stream.Stream;
import static urbachyannick.approxflow.codetransformation.BytecodeUtil.*;

public class OutputVariable extends Scanner<Stream<JavaSignature>> {

    @Override
    public Stream<JavaSignature> scan(ClassNode sourceClass) {
        return sourceClass.fields.stream()
                .filter(f -> (
                        hasFlag(f.access, Opcodes.ACC_STATIC) &&
                        TypeSpecifier.parse(f.desc, new MutableInteger(0)).isPrimitive() &&
                        hasAnnotation(f.visibleAnnotations, "Lurbachyannick/approxflow/PublicOutput;")
                ))
                .map(f -> new JavaSignature(
                        ClassName.tryParseFromTypeSpecifier("L" + sourceClass.name + ";", new MutableInteger(0)),
                        new FieldAccess(f.name)
                ));
    }
}
