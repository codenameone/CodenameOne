package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StackOverflowHandlingTest {
    @Test
    void stackOverflowIsCatchableInCompiledApp() throws Exception {
        String code = "package test;\n" +
                "public class Main {\n" +
                "    private static native void print(String s);\n" +
                "    private static native void prepareOverflow();\n" +
                "    private static native void resetOverflow();\n" +
                "    private static int triggerOverflow() {\n" +
                "        return 1;\n" +
                "    }\n" +
                "    private static int safeRecurse(int depth) {\n" +
                "        if (depth <= 0) {\n" +
                "            return 0;\n" +
                "        }\n" +
                "        return 1 + safeRecurse(depth - 1);\n" +
                "    }\n" +
                "    public static void main(String[] args) {\n" +
                "        boolean overflowed = false;\n" +
                "        try {\n" +
                "            prepareOverflow();\n" +
                "            triggerOverflow();\n" +
                "        } catch (StackOverflowError err) {\n" +
                "            overflowed = true;\n" +
                "            resetOverflow();\n" +
                "        }\n" +
                "        if (overflowed && safeRecurse(10) == 10) {\n" +
                "            print(\"OVERFLOW_RECOVER\");\n" +
                "        }\n" +
                "    }\n" +
                "}";
        assertTrue(CompilerHelper.compileAndRun(code, "OVERFLOW_RECOVER"),
                "Compiled app should catch StackOverflowError and continue execution");
    }

    @Test
    void initMethodStackThrowsStackOverflowError() throws Exception {
        Path nativeMethods = Paths.get("..", "ByteCodeTranslator", "src", "nativeMethods.m")
                .toAbsolutePath()
                .normalize();
        String content = new String(Files.readAllBytes(nativeMethods), StandardCharsets.UTF_8);

        assertTrue(content.contains("__NEW_INSTANCE_java_lang_StackOverflowError"),
                "nativeMethods.m should reference StackOverflowError allocation");
        assertTrue(content.contains("throwException(threadStateData, __NEW_INSTANCE_java_lang_StackOverflowError"),
                "nativeMethods.m should throw StackOverflowError on overflow");
        assertFalse(content.contains("CODENAME_ONE_ASSERT(threadStateData->callStackOffset < CN1_MAX_STACK_CALL_DEPTH - 1)"),
                "nativeMethods.m should not assert on call stack overflow");

        Pattern earlyReturn = Pattern.compile(
                "StackOverflowError\\(threadStateData\\)\\);\\s*return;",
                Pattern.MULTILINE
        );
        assertTrue(earlyReturn.matcher(content).find(),
                "nativeMethods.m should return immediately after throwing StackOverflowError");
    }

    @Test
    void javaApiContainsVmErrors() throws Exception {
        Path stackOverflowError = Paths.get("..", "JavaAPI", "src", "java", "lang", "StackOverflowError.java")
                .toAbsolutePath()
                .normalize();
        Path virtualMachineError = Paths.get("..", "JavaAPI", "src", "java", "lang", "VirtualMachineError.java")
                .toAbsolutePath()
                .normalize();

        String stackOverflowContent = new String(Files.readAllBytes(stackOverflowError), StandardCharsets.UTF_8);
        String virtualMachineContent = new String(Files.readAllBytes(virtualMachineError), StandardCharsets.UTF_8);

        assertTrue(stackOverflowContent.contains("extends java.lang.VirtualMachineError"),
                "StackOverflowError should extend VirtualMachineError");
        assertTrue(virtualMachineContent.contains("class VirtualMachineError"),
                "VirtualMachineError should be present in JavaAPI");
    }
}
