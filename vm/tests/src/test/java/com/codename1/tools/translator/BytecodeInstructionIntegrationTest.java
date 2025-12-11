package com.codename1.tools.translator;
import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeInstructionIntegrationTest {

    @Test
    void translatesOptimizedBytecodeToLLVMExecutable() throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("bytecode-integration-sources");
        Path classesDir = Files.createTempDirectory("bytecode-integration-classes");

        Files.createDirectories(sourceDir.resolve("java/lang"));
        Files.write(sourceDir.resolve("BytecodeInstructionApp.java"), appSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/Object.java"), CleanTargetIntegrationTest.javaLangObjectSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/String.java"), CleanTargetIntegrationTest.javaLangStringSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/Class.java"), CleanTargetIntegrationTest.javaLangClassSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/Throwable.java"), CleanTargetIntegrationTest.javaLangThrowableSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/Exception.java"), CleanTargetIntegrationTest.javaLangExceptionSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/RuntimeException.java"), CleanTargetIntegrationTest.javaLangRuntimeExceptionSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/NullPointerException.java"), CleanTargetIntegrationTest.javaLangNullPointerExceptionSource().getBytes(StandardCharsets.UTF_8));

        Path nativeReport = sourceDir.resolve("native_report.c");
        Files.write(nativeReport, nativeReportSource().getBytes(StandardCharsets.UTF_8));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "A JDK is required to compile test sources");

        int compileResult = compiler.run(
                null,
                null,
                null,
                "-d", classesDir.toString(),
                sourceDir.resolve("BytecodeInstructionApp.java").toString(),
                sourceDir.resolve("java/lang/Object.java").toString(),
                sourceDir.resolve("java/lang/String.java").toString(),
                sourceDir.resolve("java/lang/Class.java").toString(),
                sourceDir.resolve("java/lang/Throwable.java").toString(),
                sourceDir.resolve("java/lang/Exception.java").toString(),
                sourceDir.resolve("java/lang/RuntimeException.java").toString(),
                sourceDir.resolve("java/lang/NullPointerException.java").toString()
        );
        assertEquals(0, compileResult, "BytecodeInstructionApp should compile");

        Files.copy(nativeReport, classesDir.resolve("native_report.c"));

        Path outputDir = Files.createTempDirectory("bytecode-integration-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "CustomBytecodeApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project for the optimized sample");

        Path srcRoot = distDir.resolve("CustomBytecodeApp-src");
        CleanTargetIntegrationTest.patchCn1Globals(srcRoot);
        CleanTargetIntegrationTest.writeRuntimeStubs(srcRoot);

        Path generatedSource = findGeneratedSource(srcRoot);
        String generatedCode = new String(Files.readAllBytes(generatedSource), StandardCharsets.UTF_8);
        assertTrue(generatedCode.contains("CustomJump */"), "Optimized comparisons should emit CustomJump code");
        assertTrue(generatedCode.contains("BC_IINC"), "Increment operations should translate to BC_IINC macro");
        assertTrue(generatedCode.contains("VarOp.assignFrom"), "Optimized stores should rely on CustomIntruction output");
        assertTrue(generatedCode.contains("switch((*SP).data.i)"), "SwitchInstruction should emit a native switch statement");
        assertTrue(generatedCode.contains("BC_DUP(); /* DUP */"), "DupExpression should translate DUP operations to BC_DUP");

        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, srcRoot.getFileName().toString());

        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);

        CleanTargetIntegrationTest.runCommand(Arrays.asList(
                "cmake",
                "-S", distDir.toString(),
                "-B", buildDir.toString(),
                "-DCMAKE_C_COMPILER=clang",
                "-DCMAKE_OBJC_COMPILER=clang"
        ), distDir);

        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

        Path executable = buildDir.resolve("CustomBytecodeApp");
        String output = CleanTargetIntegrationTest.runCommand(Arrays.asList(executable.toString()), buildDir);
        assertTrue(output.contains("RESULT=54"), "Compiled program should print the expected arithmetic result");
    }

    private Path findGeneratedSource(Path srcRoot) throws Exception {
        try (Stream<Path> paths = Files.walk(srcRoot)) {
            return paths
                    .filter(p -> p.getFileName().toString().startsWith("BytecodeInstructionApp") && p.getFileName().toString().endsWith(".c"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Translated source for BytecodeInstructionApp not found"));
        }
    }

    private String appSource() {
        return "public class BytecodeInstructionApp {\n" +
                "    private static native void report(int value);\n" +
                "    private static int optimizedComputation(int a, int b) {\n" +
                "        int counter = a;\n" +
                "        counter++;\n" +
                "        int branchBase = counter;\n" +
                "        int min;\n" +
                "        if (branchBase < b) {\n" +
                "            min = branchBase;\n" +
                "        } else {\n" +
                "            min = b + 2;\n" +
                "        }\n" +
                "        int result = min + counter;\n" +
                "        return result;\n" +
                "    }\n" +
                "    private static int switchComputation(int value) {\n" +
                "        switch (value) {\n" +
                "            case 4:\n" +
                "                return value + 10;\n" +
                "            case 10:\n" +
                "                return value + 5;\n" +
                "            default:\n" +
                "                return value - 1;\n" +
                "        }\n" +
                "    }\n" +
                "    private static int synchronizedIncrement(int base) {\n" +
                "        Object lock = new Object();\n" +
                "        int result = base;\n" +
                "        synchronized (lock) {\n" +
                "            result++;\n" +
                "        }\n" +
                "        return result;\n" +
                "    }\n" +
                "    public static void main(String[] args) {\n" +
                "        int first = optimizedComputation(1, 3);\n" +
                "        int second = optimizedComputation(5, 2);\n" +
                "        int switched = switchComputation(first) + switchComputation(second);\n" +
                "        int synchronizedValue = synchronizedIncrement(second);\n" +
                "        report(first + second + switched + synchronizedValue);\n" +
                "    }\n" +
                "}\n";
    }

    private String nativeReportSource() {
        return "#include \"cn1_globals.h\"\n" +
                "#include <stdio.h>\n" +
                "void BytecodeInstructionApp_report___int(CODENAME_ONE_THREAD_STATE, JAVA_INT value) {\n" +
                "    printf(\"RESULT=%d\\n\", value);\n" +
                "}\n";
    }
}
