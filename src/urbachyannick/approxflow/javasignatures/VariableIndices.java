package urbachyannick.approxflow.javasignatures;

import java.text.ParseException;
import java.util.Objects;

public class VariableIndices {
    // numbers appended to signature in !a@b#c format, as described in cbmc's src/goto-symex/goto_symex_state.h

    // a: thread number (L0)
    private final int thread;
    // b: instance number (L1); incremented for every loop iteration or recursive function invocation
    private final int instance;
    // c: generation number (L2); incremented for every potential modification
    private final int generation;

    public VariableIndices(int thread, int instance, int generation) {
        this.thread = thread;
        this.instance = instance;
        this.generation = generation;
    }

    public static VariableIndices parse(String input, MutableInteger inoutOffset) throws ParseException {
        input = input + " "; // sentinel
        int thread = -1;
        int instance = -1;
        int generation = -1;
        MutableInteger offset = new MutableInteger(inoutOffset);

        if (input.charAt(offset.get()) == '!')
            thread = ParseUtil.parseNumber(input, offset.increment());

        if (input.charAt(offset.get()) == '@')
            instance = ParseUtil.parseNumber(input, offset.increment());

        if (input.charAt(offset.get()) == '#')
            generation = ParseUtil.parseNumber(input, offset.increment());

        inoutOffset.set(offset.get());
        return new VariableIndices(thread, instance, generation);
    }

    public int getThread() { return thread; }
    public int getInstance() { return instance; }
    public int getGeneration() { return generation; }

    @Override
    public int hashCode() {
        return Objects.hash(thread, instance, generation);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VariableIndices))
            return false;

        VariableIndices other = (VariableIndices) o;

        return other.thread == thread && other.instance == instance && other.generation == generation;
    }

    @Override
    public String toString() {
        return (
                (thread >= 0 ? "!" + thread : "") +
                (instance >= 0 ? "@" + instance : "") +
                (generation >= 0 ? "#" + generation : "")
        );
    }
}
