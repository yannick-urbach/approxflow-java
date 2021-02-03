package urbachyannick.approxflow.blackboxes;

import java.util.*;
import java.util.stream.*;

public class SourcesSinksPair {
    private final Set<FlowSource> sources;
    private final Set<FlowSink> sinks;

    public SourcesSinksPair(Stream<FlowSource> sources, Stream<FlowSink> sinks) {
        this.sources = sources.collect(Collectors.toSet());
        this.sinks = sinks.collect(Collectors.toSet());
    }

    public Stream<FlowSource> getSources() {
        return sources.stream();
    }

    public boolean includesInput() {
        return sources.contains(Input.SINGLETON);
    }

    public Stream<BlackboxCall> getBlackboxSources() {
        return sources.stream().filter(s -> s instanceof BlackboxCall).map(s -> (BlackboxCall) s);
    }

    public Stream<FlowSink> getSinks() {
        return sinks.stream();
    }

    public boolean includesOutput() {
        return sinks.contains(Output.SINGLETON);
    }

    public Stream<BlackboxCall> getBlackboxSinks() {
        return sinks.stream().filter(s -> s instanceof BlackboxCall).map(s -> (BlackboxCall) s);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SourcesSinksPair)) return false;
        SourcesSinksPair other = (SourcesSinksPair) o;
        return sources.equals(other.sources) && sinks.equals(other.sinks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sources, sinks);
    }
}
