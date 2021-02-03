package urbachyannick.approxflow.cnf;

import urbachyannick.approxflow.javasignatures.*;

import java.util.Arrays;
import java.util.stream.Stream;

public class VariableMapping {
    private final Signature signature;
    private final MappingValue[] values;

    public VariableMapping(Signature signature, MappingValue... values) {
        this.signature = signature;
        this.values = Arrays.copyOf(values, values.length);
    }

    public VariableMapping(Signature signature, Stream<MappingValue> values) {
        this.signature = signature;
        this.values = values.toArray(MappingValue[]::new);
    }

    public Signature getSignature() {
        return signature;
    }

    public Stream<MappingValue> getMappingValues() {
        return Arrays.stream(values);
    }

    public static int compareByGeneration(VariableMapping m1, VariableMapping m2) {
        if (m1.getSignature() instanceof JavaSignature && m2.getSignature() instanceof JavaSignature)
            return Integer.compare(((JavaSignature) m1.getSignature()).getIndices().getGeneration(), ((JavaSignature) m2.getSignature()).getIndices().getGeneration());

        if (m1.getSignature() instanceof DynamicArraySignature && m2.getSignature() instanceof DynamicArraySignature)
            return Integer.compare(((DynamicArraySignature) m1.getSignature()).getIndices().getGeneration(), ((DynamicArraySignature) m2.getSignature()).getIndices().getGeneration());

        if (m1.getSignature() instanceof DynamicObjectSignature && m2.getSignature() instanceof DynamicObjectSignature)
            return Integer.compare(((DynamicObjectSignature) m1.getSignature()).getIndices().getGeneration(), ((DynamicObjectSignature) m2.getSignature()).getIndices().getGeneration());

        return 0;
    }
}
