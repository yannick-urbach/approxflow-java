package urbachyannick.approxflow.blackboxes;

public enum Output implements FlowSink {
    SINGLETON;

    @Override
    public String toString() {
        return "out";
    }
}
