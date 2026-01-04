package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StackOverflowErrorCompilationTest {

    @Test
    void javaApiBuildProducesStackOverflowAndVirtualMachineErrors() throws Exception {
        Path outputDir = Files.createTempDirectory("java-api-classes");

        CompilerHelper.compileJavaAPI(outputDir);

        Path vmErrorClass = outputDir.resolve("java/lang/VirtualMachineError.class");
        Path stackOverflowClass = outputDir.resolve("java/lang/StackOverflowError.class");

        assertTrue(Files.exists(vmErrorClass), "VirtualMachineError class should be generated");
        assertTrue(Files.exists(stackOverflowClass), "StackOverflowError class should be generated");

        ClassReader vmErrorReader = new ClassReader(Files.readAllBytes(vmErrorClass));
        assertEquals("java/lang/Error", vmErrorReader.getSuperName(),
                "VirtualMachineError should extend Error");

        ClassReader stackOverflowReader = new ClassReader(Files.readAllBytes(stackOverflowClass));
        assertEquals("java/lang/VirtualMachineError", stackOverflowReader.getSuperName(),
                "StackOverflowError should extend VirtualMachineError");
    }

    @Test
    void translatedRuntimeRaisesStackOverflowOnDeepRecursion() throws Exception {
        String code = "package test;\n" +
                "public class Main {\n" +
                "    private static native void print(String s);\n" +
                "    static long ping(int depth, long salt) {\n" +
                "        long guard = salt ^ depth;\n" +
                "        byte[] pad = new byte[32];\n" +
                "        pad[(depth & 7)] = (byte)guard;\n" +
                "        if (depth <= 0) return guard + pad[0];\n" +
                "        long branch = pong(depth - 1, guard + pad[0]);\n" +
                "        return branch + pad[1] + guard;\n" +
                "    }\n" +
                "    static long pong(int depth, long salt) {\n" +
                "        long shuffle = salt + depth;\n" +
                "        return ping(depth - 1, shuffle) ^ shuffle;\n" +
                "    }\n" +
                "    public static void main(String[] args) {\n" +
                "        try {\n" +
                "            print(String.valueOf(ping(1600, System.nanoTime())));\n" +
                "        } catch (StackOverflowError err) {\n" +
                "            print(\"Caught: \" + err.getClass().getName());\n" +
                "            print(err.toString());\n" +
                "        }\n" +
                "    }\n" +
                "}\n";

        CompilerHelper.ExecutionResult result = CompilerHelper.compileAndRunForResult(code);

        assertTrue(result.exitCode != 0, "Translated binary should surface the overflow instead of running to completion");
        assertNotEquals(139, result.exitCode, "Stack overflow should be surfaced as an exception, not a crash");
        assertTrue(result.output.contains("StackOverflowError"),
                () -> "Runtime output should mention StackOverflowError\nOutput:\n" + result.output);
    }
}
