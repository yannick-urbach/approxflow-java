package urbachyannick.approxflow.soot;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import soot.*;
import soot.baf.*;
import soot.options.Options;
import urbachyannick.approxflow.Fail;

import java.io.*;
import java.nio.file.Path;
import java.util.stream.Stream;

public class AsmSootConverter {
    public static void initSoot(Path resPath) {
        soot.G.reset();
        soot.G g = soot.G.v();

        soot.options.Options options = soot.options.Options.v();
        boolean b = options.parse(new String[]{
                "-w",
                "-keep-line-number",
                "-p", "jb", "use-original-names:true",
                "-p", "jb.ulp", "enabled:false",
                "-p", "jb.ule", "enabled:false",
                "-p", "jb.cp-ule", "enabled:false",
                "-p", "jb.lp", "enabled:false",
                "-p", "jj.ule", "enabled:false",
                "-p", "jj.ulp", "enabled:false",
                "-p", "jj.cp-ule", "enabled:false",
                "-p", "jj.lp", "enabled:false",
                "-p", "jop.ule", "enabled:false",
                "-p", "gb.ule", "enabled:false",
                "-p", "bb.ule", "enabled:false",
                "-p", "bb.lp", "enabled:false",
        });

        Scene scene = soot.Scene.v();
        scene.setSootClassPath(scene.defaultClassPath());
        scene.extendSootClassPath(resPath.toString());
        scene.extendSootClassPath(resPath.resolve("jbmc-core-models.jar").toString());
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
            if (!m.isConcrete())
                continue;

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
