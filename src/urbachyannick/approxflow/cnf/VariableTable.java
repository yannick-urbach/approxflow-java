package urbachyannick.approxflow.cnf;

import urbachyannick.approxflow.javasignatures.Signature;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public class VariableTable implements Function<Signature, VariableMapping> {
    private final Map<Signature, VariableMapping> map;
    private final VariableMapping[] mappings;

    public VariableTable(Stream<VariableMapping> mappings) {
        this.mappings = mappings.toArray(VariableMapping[]::new);

        map = new HashMap<>();

        for (VariableMapping m : this.mappings)
            map.put(m.getSignature(), m);
    }

    public VariableMapping get(Signature signature) {
        return map.get(signature);
    }

    @Override
    public VariableMapping apply(Signature signature) {
        return map.get(signature);
    }

    public Stream<VariableMapping> getMappings() {
        return Arrays.stream(mappings);
    }

    public Stream<VariableMapping> getMatching(Signature signature) {
        return Arrays.stream(mappings).filter(m -> m.getSignature().matches(signature));
    }
}
