package com.codename1.tools.translator;

import com.codename1.tools.translator.bytecodes.Instruction;
import com.codename1.tools.translator.bytecodes.LabelInstruction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.LabelNode;
import static org.junit.jupiter.api.Assertions.*;

class ParserAnalysisLifetimeTest {
    @Test
    void retainedControlFlowLabelsDoNotRetainTemporaryAnalysisTrees() throws Exception {
        Parser.cleanup();
        Path directory = Files.createTempDirectory("parser-analysis-lifetime");
        Path source = directory.resolve("ControlFlow.java");
        Files.write(source, ("public class ControlFlow { static int run(int n) { int sum=0;"
                + "try { for(int i=0;i<n;i++) { if(i%2==0) sum+=i; else sum-=i; } }"
                + "catch(RuntimeException e) { return -1; } return sum; } }")
                .getBytes(StandardCharsets.UTF_8));
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-source", "8", "-target", "8", "-g", "-d", directory.toString(), source.toString()));
        try {
            Parser.parse(directory.resolve("ControlFlow.class").toFile());
            int labels = 0;
            for (BytecodeMethod method : Parser.getClassObject("ControlFlow").getMethods()) {
                for (Instruction instruction : method.getInstructions()) {
                    if (instruction instanceof LabelInstruction) {
                        labels++;
                        assertFalse(((LabelInstruction) instruction).getLabel().info instanceof LabelNode,
                                "A retained bytecode label still owns the linked ASM analysis tree");
                    }
                }
            }
            assertTrue(labels >= 5, "Exercise branches and exception-handler labels");
        } finally {
            Parser.cleanup();
        }
    }
}
