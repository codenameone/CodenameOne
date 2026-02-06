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
        assertEquals(0, compileResult, "StackOverflowApp should compile with " + config +
                " error: " + CompilerHelper.getLastErrorLog());

        CompilerHelper.copyDirectory(javaApiDir, classesDir);
        Files.copy(sourceDir.resolve("native_report.c"), classesDir.resolve("native_report.c"));

        Path outputDir = Files.createTempDirectory("stack-overflow-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "StackOverflowApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project for StackOverflowApp");

        Path srcRoot = distDir.resolve("StackOverflowApp-src");
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
        ProcessResult probeResult = runProcess(Arrays.asList(executable.toString()), buildDir);
        String probeDiagnostics = buildDiagnostics(srcRoot, executable, probeResult);
        assertEquals(0, probeResult.exitCode,
                "StackOverflowApp probe run exited with code " + probeResult.exitCode
                        + ". Output:\n" + probeResult.output
                        + probeDiagnostics);
        ProcessResult smokeResult = runProcess(Arrays.asList(executable.toString(), "smoke"), buildDir);
        String smokeDiagnostics = buildDiagnostics(srcRoot, executable, smokeResult);
        assertEquals(0, smokeResult.exitCode,
                "StackOverflowApp smoke run exited with code " + smokeResult.exitCode
                        + ". Output:\n" + smokeResult.output
                        + smokeDiagnostics);

        ProcessResult result = runProcess(Arrays.asList(executable.toString(), "overflow", "run"), buildDir);
        String diagnostics = buildDiagnostics(srcRoot, executable, result);
        assertEquals(0, result.exitCode,
                "StackOverflowApp exited with code " + result.exitCode
                        + ". Output:\n" + result.output
                        + diagnostics);
        assertTrue(result.output.contains("STACK_OVERFLOW_OK"),
                "StackOverflowError should be thrown and caught. Output was:\n" + result.output + diagnostics);
    }

    private String appSource() {
        return "public class StackOverflowApp {\n" +
                "    private static int counter;\n" +
                "    private static native void report(String msg);\n" +
                "    private static void triggerOverflow() {\n" +
                "        if (counter > 256) {\n" +
                "            throw new StackOverflowError();\n" +
                "        }\n" +
                "        String txt = new StringBuilder()\n" +
                "                .append(\"Calling ...\")\n" +
                "                .append(counter)\n" +
                "                .toString();\n" +
                "        counter++;\n" +
                "        report(txt);\n" +
                "        triggerOverflow();\n" +
                "    }\n" +
                "    private static int boundedRecursion(int depth) {\n" +
                "        if (depth <= 0) {\n" +
                "            return 1;\n" +
                "        }\n" +
                "        return depth + boundedRecursion(depth - 1);\n" +
                "    }\n" +
                "    public static void main(String[] args) {\n" +
                "        report(\"Starting test...\");\n" +
                "        if (args != null && args.length > 0 && \"smoke\".equals(args[0])) {\n" +
                "            report(\"SMOKE_OK\");\n" +
                "            return;\n" +
                "        }\n" +
                "        if (args == null || args.length == 0 || \"overflow\".equals(args[0])) {\n" +
                "            try {\n" +
                "                triggerOverflow();\n" +
                "            } catch (StackOverflowError err) {\n" +
                "                report(\"STACK_OVERFLOW_OK\");\n" +
                "            }\n" +
                "        }\n" +
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

    private String buildDiagnostics(Path srcRoot, Path executable, ProcessResult result) throws Exception {
        StringBuilder diagnostics = new StringBuilder();
        diagnostics.append("\nExecutable: ").append(executable);
        if (Files.exists(executable)) {
            diagnostics.append("\nExecutable size: ").append(Files.size(executable)).append(" bytes");
        }

        Path cn1Globals = srcRoot.resolve("cn1_globals.h");
        if (Files.exists(cn1Globals)) {
            String globalsSource = new String(Files.readAllBytes(cn1Globals), StandardCharsets.UTF_8);
            diagnostics.append("\n").append(extractLine(globalsSource, "CN1_STACK_OVERFLOW_CALL_DEPTH_LIMIT"));
        }

        Path nativeMethods = srcRoot.resolve("nativeMethods.c");
        if (Files.exists(nativeMethods)) {
            String nativeMethodsSource = new String(Files.readAllBytes(nativeMethods), StandardCharsets.UTF_8);
            diagnostics.append("\nContains java_lang_StackOverflowError.h: ")
                    .append(nativeMethodsSource.contains("java_lang_StackOverflowError.h"));
            diagnostics.append("\nContains CN1_STACK_OVERFLOW_CALL_DEPTH_LIMIT: ")
                    .append(nativeMethodsSource.contains("CN1_STACK_OVERFLOW_CALL_DEPTH_LIMIT"));
            diagnostics.append("\ninitMethodStack snippet: ")
                    .append(extractSnippet(nativeMethodsSource, "initMethodStack", 120));
        }

        Path appSource = srcRoot.resolve("StackOverflowApp.c");
        if (Files.exists(appSource)) {
            String appSourceText = new String(Files.readAllBytes(appSource), StandardCharsets.UTF_8);
            diagnostics.append("\ntriggerOverflow snippet: ")
                    .append(extractSnippet(appSourceText, "StackOverflowApp_triggerOverflow__", 120));
            diagnostics.append("\nboundedRecursion snippet: ")
                    .append(extractSnippet(appSourceText, "StackOverflowApp_boundedRecursion___int_R_int", 120));
            diagnostics.append("\nmain snippet: ")
                    .append(extractSnippet(appSourceText, "StackOverflowApp_main___java_lang_String_1ARRAY", 160));
            diagnostics.append("\nreport snippet: ")
                    .append(extractSnippet(appSourceText, "StackOverflowApp_report___java_lang_String", 120));
        }
        if (result.output.isEmpty()) {
            diagnostics.append("\n***No output printed");
        } else {
            diagnostics.append("\nOutput length: ").append(result.output.length());
        }
        return diagnostics.toString();
    }

    private String extractLine(String source, String token) {
        int idx = source.indexOf(token);
        if (idx < 0) {
            return "Missing " + token;
        }
        int lineStart = source.lastIndexOf('\n', idx);
        int lineEnd = source.indexOf('\n', idx);
        if (lineStart < 0) {
            lineStart = 0;
        } else {
            lineStart += 1;
        }
        if (lineEnd < 0) {
            lineEnd = source.length();
        }
        return source.substring(lineStart, lineEnd).trim();
    }

    private String extractSnippet(String source, String token, int radius) {
        int idx = source.indexOf(token);
        if (idx < 0) {
            return "Missing " + token;
        }
        int start = Math.max(0, idx - radius);
        int end = Math.min(source.length(), idx + radius);
        return source.substring(start, end).replace("\n", "\\n");
    }

    private ProcessResult runProcess(List<String> command, Path workingDir) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDir.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output;
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            output = reader.lines().collect(java.util.stream.Collectors.joining("\n"));
        }
        int exit = process.waitFor();
        return new ProcessResult(exit, output);
    }

    private static final class ProcessResult {
        private final int exitCode;
        private final String output;

        private ProcessResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }
}
