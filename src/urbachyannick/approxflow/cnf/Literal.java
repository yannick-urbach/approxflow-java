package urbachyannick.approxflow.cnf;

public class Literal implements MappingValue {
    private final int variable;
    private final boolean value;

    public Literal(int variable, boolean value) {
        if (variable <= 0)
            throw new IllegalArgumentException("Variable must be positive.");

        this.variable = variable;
        this.value = value;
    }

    public Literal(int literal) {
        if (literal == 0)
            throw new IllegalArgumentException("Literal must not be 0.");

        value = literal > 0;
        variable = Math.abs(literal);
    }

    public Literal negate() {
        return new Literal(variable, !value);
    }

    public int getVariable() {
        return variable;
    }

    public boolean getValue() {
        return value;
    }

    @Override
    public boolean isTrivial() {
        return false;
    }
}
