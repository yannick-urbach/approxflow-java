package urbachyannick.approxflow.cnf;

import java.util.Arrays;
import java.util.stream.IntStream;

public class Scope {
    private final int[] variables;

    public Scope(int... variables) {
        this.variables = Arrays.stream(variables).distinct().toArray();
    }

    public Scope(IntStream variables) {
        this.variables = variables.distinct().toArray();
    }

    public IntStream getVariables() {
        return Arrays.stream(variables);
    }

    public Scope except(Scope other) {
        return new Scope(getVariables().filter(lhs -> other.getVariables().noneMatch(rhs -> lhs == rhs)));
    }
}
