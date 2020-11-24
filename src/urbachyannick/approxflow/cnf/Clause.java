package urbachyannick.approxflow.cnf;

import java.util.Arrays;
import java.util.stream.Stream;

public class Clause {
    private final Literal[] literals;

    public Clause(Literal... literals) {
        this.literals = Arrays.copyOf(literals, literals.length);
    }

    public Clause(Stream<Literal> literals) {
        this.literals = literals.toArray(Literal[]::new);
    }

    public Stream<Literal> getLiterals() {
        return Arrays.stream(literals);
    }
}
