package urbachyannick.approxflow.soot;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import soot.*;
import soot.baf.*;
import soot.options.Options;
import urbachyannick.approxflow.Fail;

import java.io.*;
import java.util.stream.Stream;

public class AsmSootConverter {
    public static void initSoot() {
        soot.G.reset();
        soot.G g = soot.G.v();

        soot.options.Options options = soot.options.Options.v();
        options.parse(new String[]{ "-keep-line-number", "-p", "jb", "use-original-names:true" });

        Scene scene = soot.Scene.v();
        scene.setSootClassPath(scene.defaultClassPath());
        scene.extendSootClassPath("/home/yannick/Bachelorarbeit/approxflow-java/res/");
        scene.extendSootClassPath("/home/yannick/Bachelorarbeit/approxflow-java/res/jbmc-core-models.jar");
    }

    public static SootClass toSoot(ClassNode asmClass) {
        SootClass sootClass = SootClassAdapter.toSoot(asmClass);
        Scene.v().loadNecessaryClasses();
        return sootClass;
    }

    public static Stream<SootClass> toSoot(Stream<ClassNode> asmClasses) {
        return asmClasses.map(AsmSootConverter::toSoot);
    }

    public static ClassNode toAsm(SootClass sootClass) {
        for (SootMethod m : sootClass.getMethods()) {
            m.retrieveActiveBody();
        }

        AbstractASMBackend asmBackend = new BafASMBackend(sootClass, Options.java_version_1_8);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            asmBackend.generateClassFile(outputStream);

            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
                ClassReader reader = new ClassReader(inputStream);
                ClassNode asmClass = new ClassNode(Opcodes.ASM5);
                reader.accept(asmClass, ClassReader.EXPAND_FRAMES);
                return asmClass;
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw new Fail("Should not happen; Not actually doing IO.");
        }
    }

    public static Stream<ClassNode> toAsm(Stream<SootClass> sootClasses) {
        return sootClasses.map(AsmSootConverter::toAsm);
    }
}
