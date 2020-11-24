package urbachyannick.approxflow.codetransformation;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import urbachyannick.approxflow.cnf.MappedProblem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class Scanner<T> {
    public abstract T scan(ClassNode sourceClass, MappedProblem problem) throws IOException;

    public T scan(Path path, MappedProblem problem) throws IOException {
        ClassNode sourceClass = new ClassNode(Opcodes.ASM5); // Java 8

        InputStream inputStream = Files.newInputStream(path);
        ClassReader reader = new ClassReader(inputStream);
        reader.accept(sourceClass, 0); // read source class from input file
        inputStream.close();

        return scan(sourceClass, problem);
    }
}
