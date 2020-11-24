package urbachyannick.approxflow.cnf;

public enum TrivialMappingValue implements MappingValue {
    FALSE(false), TRUE(true);

    private final boolean value;

    TrivialMappingValue(boolean value) {
        this.value = value;
    }

    public boolean get() {
        return value;
    }

    @Override
    public boolean isTrivial() {
        return true;
    }
}
