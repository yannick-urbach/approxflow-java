package urbachyannick.approxflow.blackboxes;

public enum Input implements FlowSource {
    SINGLETON;

    @Override
    public String toString() {
        return "in";
    }
}
