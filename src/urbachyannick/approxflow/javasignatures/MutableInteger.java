package urbachyannick.approxflow.javasignatures;

public class MutableInteger {
    private int value;

    public MutableInteger(int value) { this.value = value; }
    public MutableInteger(MutableInteger value) { this.value = value.value; } // copy

    public int get() { return value; }
    public MutableInteger set(int value) { this.value = value; return this; }
    public MutableInteger increment() { ++value; return this; }
    public MutableInteger add(int value) { this.value += value; return this; }
    @Override public int hashCode() { return value; }

    @Override
    public boolean equals(Object other) {
        if (other instanceof MutableInteger)
            return ((MutableInteger) other).value == value;

        if (other instanceof Integer)
            return (Integer) other == value;

        return false;
    }
}
