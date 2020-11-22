package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import urbachyannick.approxflow.CnfException;
import urbachyannick.approxflow.CnfFile;
import urbachyannick.approxflow.CnfVarLine;
import urbachyannick.approxflow.javasignatures.*;

import java.io.IOException;
import java.util.stream.IntStream;
import static urbachyannick.approxflow.codetransformation.BytecodeUtil.*;

public class OutputVariable extends Scanner<IntStream> {

    @Override
    public IntStream scan(ClassNode sourceClass, CnfFile cnfFile) throws IOException {
        return sourceClass.fields.stream()
                .filter(f -> (
                        hasFlag(f.access, Opcodes.ACC_STATIC) &&
                        TypeSpecifier.parse(f.desc, new MutableInteger(0)).isPrimitive() &&
                        hasAnnotation(f.visibleAnnotations, "Lurbachyannick/approxflow/PublicOutput;")
                ))
                .map(f -> new JavaSignature(
                        ClassName.tryParseFromTypeSpecifier("L" + sourceClass.name + ";", new MutableInteger(0)),
                        new FieldAccess(f.name)
                ))
                .flatMapToInt(s -> getVariablesForSignature(cnfFile, s));
    }

    private IntStream getVariablesForSignature(CnfFile cnfFile, JavaSignature signature) {
        try {
            return cnfFile
                    .getVarLines()
                    .filter(line -> signature.matches(line.getSignature()))
                    .max(CnfVarLine::compareByGeneration)
                    .orElseThrow(() -> new CnfException("missing variable line for " + signature.toString()))
                    .getLiterals();
        } catch (CnfException e) {
            System.err.println("Can not find variable line for " + signature.toString());
            return IntStream.empty();
        } catch (IOException e) {
            System.err.println("Can not read variable lines");
            return IntStream.empty();
        }
    }
}
