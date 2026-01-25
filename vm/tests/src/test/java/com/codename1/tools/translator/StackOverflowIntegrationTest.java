package com.codename1.tools.translator;

import org.junit.jupiter.params.ParameterizedTest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StackOverflowIntegrationTest {

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void throwsAndRecoversFromStackOverflow(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("stack-overflow-source");
        Path classesDir = Files.createTempDirectory("stack-overflow-classes");
        Path javaApiDir = Files.createTempDirectory("java-api-classes");

        Files.write(sourceDir.resolve("StackOverflowApp.java"), appSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("native_report.c"), nativeReportSource().getBytes(StandardCharsets.UTF_8));

        assertTrue(CompilerHelper.isJavaApiCompatible(config),
                "JDK " + config.jdkVersion + " must target matching bytecode level for JavaAPI");
        CompilerHelper.compileJavaAPI(javaApiDir, config);

        List<String> compileArgs = new ArrayList<>();

        int jdkMajor = CompilerHelper.getJdkMajor(config);
        if (jdkMajor >= 9) {
            compileArgs.add("-source");
            compileArgs.add(config.targetVersion);
            compileArgs.add("-target");
            compileArgs.add(config.targetVersion);
            compileArgs.add("-classpath");
            compileArgs.add(javaApiDir.toString());
        } else {
            compileArgs.add("-source");
            compileArgs.add(config.targetVersion);
            compileArgs.add("-target");
            compileArgs.add(config.targetVersion);
            compileArgs.add("-bootclasspath");
            compileArgs.add(javaApiDir.toString());
            compileArgs.add("-Xlint:-options");
        }

        compileArgs.add("-d");
        compileArgs.add(classesDir.toString());
        compileArgs.add(sourceDir.resolve("StackOverflowApp.java").toString());

        int compileResult = CompilerHelper.compile(config.jdkHome, compileArgs);
        assertEquals(0, compileResult, "StackOverflowApp should compile with " + config);

        CompilerHelper.copyDirectory(javaApiDir, classesDir);
        Files.copy(sourceDir.resolve("native_report.c"), classesDir.resolve("native_report.c"));

        Path outputDir = Files.createTempDirectory("stack-overflow-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "StackOverflowApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project for StackOverflowApp");

        Path srcRoot = distDir.resolve("StackOverflowApp-src");
        CleanTargetIntegrationTest.patchCn1Globals(srcRoot);
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

        Path executable = buildDir.resolve("StackOverflowApp");
        String output = CleanTargetIntegrationTest.runCommand(Arrays.asList(executable.toString()), buildDir);

        assertTrue(output.contains("STACK_OVERFLOW_OK"),
                "StackOverflowError should be thrown and caught. Output was:\n" + output);
        assertTrue(output.contains("RECOVERY_OK:7"),
                "VM should recover after StackOverflowError. Output was:\n" + output);
    }

    private String appSource() {
        return "public class StackOverflowApp {\n" +
                "    private static native void report(String msg);\n" +
                "    private static void triggerOverflow() {\n" +
                "        triggerOverflow();\n" +
                "    }\n" +
                "    private static int postOverflow(int value) {\n" +
                "        if (value <= 0) {\n" +
                "            return 1;\n" +
                "        }\n" +
                "        return value + postOverflow(value - 1);\n" +
                "    }\n" +
                "    public static void main(String[] args) {\n" +
                "        boolean overflowed = false;\n" +
                "        try {\n" +
                "            triggerOverflow();\n" +
                "        } catch (StackOverflowError err) {\n" +
                "            overflowed = true;\n" +
                "        }\n" +
                "        report(overflowed ? \"STACK_OVERFLOW_OK\" : \"STACK_OVERFLOW_MISSING\");\n" +
                "        StringBuilder sb = new StringBuilder();\n" +
                "        sb.append(\"RECOVERY_OK:\");\n" +
                "        sb.append(postOverflow(3));\n" +
                "        report(sb.toString());\n" +
                "    }\n" +
                "}\n";
    }

    private String nativeReportSource() {
        return "#include \"cn1_globals.h\"\n" +
                "#include <stdio.h>\n" +
                "void StackOverflowApp_report___java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT msg) {\n" +
                "    struct String_Struct {\n" +
                "        JAVA_OBJECT header;\n" +
                "        JAVA_OBJECT value;\n" +
                "        JAVA_INT offset;\n" +
                "        JAVA_INT count;\n" +
                "    };\n" +
                "    struct String_Struct* str = (struct String_Struct*)msg;\n" +
                "    struct JavaArrayPrototype* arr = (struct JavaArrayPrototype*)str->value;\n" +
                "    if (arr) {\n" +
                "        JAVA_CHAR* chars = (JAVA_CHAR*)arr->data;\n" +
                "        int len = str->count;\n" +
                "        int off = str->offset;\n" +
                "        for (int i = 0; i < len; i++) {\n" +
                "            printf(\"%c\", (char)chars[off + i]);\n" +
                "        }\n" +
                "        printf(\"\\n\");\n" +
                "        fflush(stdout);\n" +
                "    }\n" +
                "}\n";
    }
}
