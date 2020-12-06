package urbachyannick.approxflow.informationflow;

import org.objectweb.asm.tree.ClassNode;
import urbachyannick.approxflow.IOCallbacks;

import java.util.stream.Stream;

public interface FlowAnalyzer {
    double analyzeInformationFlow(Stream<ClassNode> classes, IOCallbacks ioCallbacks);
}
