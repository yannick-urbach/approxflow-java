package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import urbachyannick.approxflow.Unreachable;
import urbachyannick.approxflow.javasignatures.*;

import java.text.ParseException;
import java.util.stream.Stream;
import static urbachyannick.approxflow.codetransformation.BytecodeUtil.*;

public class OutputVariable extends Scanner<Stream<ParsedSignature>> {

    @Override
    public Stream<ParsedSignature> scan(ClassNode sourceClass) {
        return sourceClass.fields.stream()
                .filter(f -> {
                        try {
                            return (
                                    hasFlag(f.access, Opcodes.ACC_STATIC) &&
                                    TypeSpecifier.parse(f.desc, new MutableInteger(0)).isPrimitive() &&
                                    hasAnnotation(f.visibleAnnotations, "Lurbachyannick/approxflow/PublicOutput;")
                            );
                        } catch (ParseException e) {
                            throw new Unreachable();
                        }
                })
                .map(f -> {
                        try {
                            return new ParsedSignature(
                                    ClassName.tryParseFromTypeSpecifier("L" + sourceClass.name + ";", new MutableInteger(0)),
                                    new FieldAccess(f.name)
                            );
                        } catch (ParseException e) {
                            throw new Unreachable();
                        }
                });
    }
}
