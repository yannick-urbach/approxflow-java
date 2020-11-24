package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import urbachyannick.approxflow.*;
import urbachyannick.approxflow.cnf.Literal;
import urbachyannick.approxflow.cnf.MappedProblem;
import urbachyannick.approxflow.cnf.TrivialMappingValue;
import urbachyannick.approxflow.cnf.VariableMapping;
import urbachyannick.approxflow.javasignatures.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static urbachyannick.approxflow.codetransformation.BytecodeUtil.hasAnnotation;
import static urbachyannick.approxflow.codetransformation.BytecodeUtil.hasFlag;

public class OutputArray extends Scanner<IntStream> {
    @Override
    public IntStream scan(ClassNode sourceClass, MappedProblem problem) throws IOException {
        List<VariableMapping> mappings = problem.getVariableTable().getMappings().collect(Collectors.toList());

        return sourceClass.fields.stream().flatMapToInt(field -> {

                if (!hasFlag(field.access, Opcodes.ACC_STATIC))
                    return IntStream.empty();

                if (!hasAnnotation(field.visibleAnnotations, "Lurbachyannick/approxflow/PublicOutput;"))
                    return IntStream.empty();

                TypeSpecifier fieldType = TypeSpecifier.parse(field.desc, new MutableInteger(0));

                if (!(fieldType instanceof ArrayType))
                    return IntStream.empty();

                TypeSpecifier elementType = ((ArrayType) fieldType).getElementType();

                if (!elementType.isPrimitive())
                    return IntStream.empty();

                JavaSignature fieldSignature = new JavaSignature(
                        ClassName.tryParseFromTypeSpecifier("L" + sourceClass.name + ";", new MutableInteger(0)),
                        new FieldAccess(field.name)
                );

                Optional<VariableMapping> varLine = mappings.stream()
                        .filter(l -> fieldSignature.matches(l.getSignature()))
                        .max(VariableMapping::compareByGeneration);

                if (!varLine.isPresent()) {
                    System.err.println("Can not find variable line for " + fieldSignature.toString());
                    return IntStream.empty();
                }

                long address = MiscUtil.parseAddressFromTrivialLiterals(varLine.get().getMappingValues().map(v -> (TrivialMappingValue) v));

                // all lines of elements of the array with this address, grouped by the element index
                Map<Long, List<VariableMapping>> elementLines = mappings.stream()
                        .filter(l -> (
                                l.getSignature() instanceof DynamicArraySignature &&
                                ((DynamicArraySignature) l.getSignature()).getAddress() == address
                        ))
                        .collect(Collectors.groupingBy(l -> ((DynamicArraySignature) l.getSignature()).getElementIndex()));

                // the literals of the last generation of each individual element index
                IntStream literals = elementLines.values().stream()
                        .flatMapToInt(
                                l -> l.stream()
                                        .max(VariableMapping::compareByGeneration)
                                        .get()
                                        .getMappingValues()
                                        .filter(v -> !v.isTrivial())
                                        .mapToInt(v -> ((Literal) v).getVariable())
                        );

                return literals;
        });
    }
}
