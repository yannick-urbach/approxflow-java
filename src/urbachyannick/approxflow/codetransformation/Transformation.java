package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.tree.ClassNode;

import java.util.Iterator;
import java.util.stream.Stream;

@FunctionalInterface
public interface Transformation {
    Stream<ClassNode> apply(Stream<ClassNode> classes) throws InvalidTransformationException;

    abstract class PerClass implements Transformation {
        protected abstract ClassNode applyToClass(ClassNode sourceClass) throws InvalidTransformationException;

        @Override
        public Stream<ClassNode> apply(Stream<ClassNode> sourceClasses) throws InvalidTransformationException {
            Stream.Builder<ClassNode> streamBuilder = Stream.builder();
            Iterator<ClassNode> i = sourceClasses.iterator();

            while (i.hasNext())
                streamBuilder.accept(applyToClass(i.next()));

            return streamBuilder.build();
        }
    }

    abstract class PerClassNoExcept extends PerClass {
        protected abstract ClassNode applyToClass(ClassNode sourceClass);

        @Override
        public Stream<ClassNode> apply(Stream<ClassNode> sourceClasses) {
            return sourceClasses.map(this::applyToClass);
        }
    }
}
