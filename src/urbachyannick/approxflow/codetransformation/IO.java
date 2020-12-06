package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import urbachyannick.approxflow.javasignatures.ClassName;
import urbachyannick.approxflow.javasignatures.MutableInteger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.Stream;

public class IO {
    public static void write(ClassNode class_, Path path) throws IOException {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        class_.accept(writer);
        Files.write(path, writer.toByteArray());
    }

    public static ClassNode read(Path path) throws IOException {
        try(InputStream inputStream = Files.newInputStream(path)) {
            ClassReader reader = new ClassReader(inputStream);
            ClassNode class_ = new ClassNode(Opcodes.ASM5);
            reader.accept(class_, ClassReader.EXPAND_FRAMES);
            return class_;
        }
    }

    public static void writeAll(Stream<ClassNode> classes, Path path) throws IOException {
        Iterator<ClassNode> i = classes.iterator();

        while(i.hasNext()) {
            ClassNode class_ = i.next();
            ClassName name = ClassName.tryParseFromTypeSpecifier("L" + class_.name + ";", new MutableInteger(0));
            Path targetPath = path.resolve(name.asPath(".class"));
            Files.createDirectories(targetPath.getParent());
            write(class_, targetPath);
        }
    }

    public static Stream<ClassNode> readAll(Path path) throws IOException {
        Iterator<Path> i = Files
                .walk(path)
                .filter(f -> f.toString().endsWith(".class"))
                .iterator();

        Stream.Builder<ClassNode> builder = Stream.builder();

        while (i.hasNext())
            builder.accept(read(i.next()));

        return builder.build();
    }
}
