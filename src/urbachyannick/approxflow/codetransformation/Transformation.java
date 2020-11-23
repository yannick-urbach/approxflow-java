package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class Transformation {
    public abstract void apply(ClassNode sourceClass, ClassNode targetClass) throws IOException, InvalidTransformationException;

    public void apply(Path input, Path output) throws IOException, InvalidTransformationException {
        ClassNode sourceClass = new ClassNode(Opcodes.ASM5); // Java 8
        ClassNode targetClass = new ClassNode(Opcodes.ASM5); // Java 8

        InputStream inputStream = Files.newInputStream(input);
        ClassReader reader = new ClassReader(inputStream);
        reader.accept(sourceClass, 0); // read source class from input file
        inputStream.close();

        apply(sourceClass, targetClass);

        ClassWriter writer = new ClassWriter(0);
        targetClass.accept(writer);
        Files.write(output, writer.toByteArray());
    }

    public void apply(Path input) throws IOException, InvalidTransformationException {
        apply(input, input);
    }

    public static void applyMultiple(Path input, Path output, Transformation... transformations) throws IOException, InvalidTransformationException {
        ClassNode current = new ClassNode(Opcodes.ASM5); // Java 8

        InputStream inputStream = Files.newInputStream(input);
        ClassReader reader = new ClassReader(inputStream);
        reader.accept(current, 0); // read source class from input file
        inputStream.close();

        for (Transformation t : transformations) {
            ClassNode transformed = new ClassNode();
            t.apply(current, transformed);
            current = transformed;
        }

        ClassWriter writer = new ClassWriter(0);
        current.accept(writer);
        Files.write(output, writer.toByteArray());
    }
}
