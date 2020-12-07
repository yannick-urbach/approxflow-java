package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import urbachyannick.approxflow.CnfException;
import urbachyannick.approxflow.cnf.*;
import urbachyannick.approxflow.javasignatures.*;

import java.util.stream.*;

import static urbachyannick.approxflow.codetransformation.BytecodeUtil.*;

public class OutputVariable implements Scanner<IntStream> {

    @Override
    public IntStream scan(Stream<ClassNode> sourceClasses, MappedProblem problem) {
        return sourceClasses.flatMapToInt(sourceClass ->
                sourceClass.fields.stream()
                        .filter(f -> (
                                hasFlag(f.access, Opcodes.ACC_STATIC) &&
                                TypeSpecifier.parse(f.desc, new MutableInteger(0)).isPrimitive() &&
                                hasAnnotation(f.visibleAnnotations, "Lurbachyannick/approxflow/PublicOutput;")
                        ))
                        .map(f -> new JavaSignature(
                                ClassName.tryParseFromTypeSpecifier("L" + sourceClass.name + ";", new MutableInteger(0)),
                                new FieldAccess(f.name)
                        ))
                        .flatMapToInt(s -> getVariablesForSignature(problem, s))
        );
    }

    private IntStream getVariablesForSignature(MappedProblem problem, JavaSignature signature) {
        try {
            return problem
                    .getVariableTable()
                    .getMatching(signature)
                    .max(VariableMapping::compareByGeneration)
                    .orElseThrow(() -> new CnfException("missing variable line for " + signature.toString()))
                    .getMappingValues()
                    .filter(v -> !v.isTrivial())
                    .mapToInt(v -> ((Literal) v).getVariable());
        } catch (CnfException e) {
            System.err.println("Can not find variable line for " + signature.toString());
            return IntStream.empty();
        }
    }
}
