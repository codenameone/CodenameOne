package com.codename1.tools.translator;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LambdaIntegrationTest {

    private String appSource() {
        return "public class LambdaApp {\n" +
                "    interface MyFunctionalInterface {\n" +
                "        int apply(int x);\n" +
                "    }\n" +
                "    \n" +
                "    public static void main(String[] args) {\n" +
                "        // Lambda\n" +
                "        MyFunctionalInterface adder = (x) -> x + 10;\n" +
                "        int result1 = adder.apply(5); // 15\n" +
                "        \n" +
                "        // Method reference static\n" +
                "        MyFunctionalInterface multiplier = LambdaApp::multiplyByTwo;\n" +
                "        int result2 = multiplier.apply(5); // 10\n" +
                "        \n" +
                "        // Method reference instance\n" +
                "        LambdaApp app = new LambdaApp();\n" +
                "        MyFunctionalInterface subtractor = app::subtractFive;\n" +
                "        int result3 = subtractor.apply(20); // 15\n" +
                "\n" +
                "        // Capturing lambda\n" +
                "        int captured = 100;\n" +
                "        MyFunctionalInterface capturer = (x) -> x + captured;\n" +
                "        int result4 = capturer.apply(5); // 105\n" +
                "        \n" +
                "        report(result1 + result2 + result3 + result4); // 15 + 10 + 15 + 105 = 145\n" +
                "    }\n" +
                "    \n" +
                "    static int multiplyByTwo(int x) {\n" +
                "        return x * 2;\n" +
                "    }\n" +
                "    \n" +
                "    int subtractFive(int x) {\n" +
                "        return x - 5;\n" +
                "    }\n" +
                "\n" +
                "    private static native void report(int value);\n" +
                "}\n";
    }

    private String nativeReportSource() {
        return "#include \"cn1_globals.h\"\n" +
                "#include <stdio.h>\n" +
                "void LambdaApp_report___int(CODENAME_ONE_THREAD_STATE, JAVA_INT value) {\n" +
                "    printf(\"RESULT=%d\\n\", value);\n" +
                "}\n";
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.8"})
    void translatesLambdaBytecodeToLLVMExecutable(String targetVersion) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("lambda-integration-sources");
        Path classesDir = Files.createTempDirectory("lambda-integration-classes");
        Path javaApiDir = Files.createTempDirectory("java-api-classes");

        Files.write(sourceDir.resolve("LambdaApp.java"), appSource().getBytes(StandardCharsets.UTF_8));

        CompilerHelper.CompilerConfig config = selectCompiler(targetVersion);
        if (config == null) {
            fail("No compiler available for target " + targetVersion);
        }

        assertTrue(CompilerHelper.isJavaApiCompatible(config),
                "JDK " + config.jdkVersion + " must target matching bytecode level for JavaAPI");

        CompilerHelper.compileJavaAPI(javaApiDir, config);

        Path nativeReport = sourceDir.resolve("native_report.c");
        Files.write(nativeReport, nativeReportSource().getBytes(StandardCharsets.UTF_8));

        // Compile App using JavaAPI as bootclasspath
        List<String> compileArgs = new ArrayList<>();
        compileArgs.add("-source");
        compileArgs.add(targetVersion);
        compileArgs.add("-target");
        compileArgs.add(targetVersion);
        if (CompilerHelper.useClasspath(config)) {
            compileArgs.add("-classpath");
            compileArgs.add(javaApiDir.toString());
        } else {
            compileArgs.add("-bootclasspath");
            compileArgs.add(javaApiDir.toString());
        }
        compileArgs.add("-d");
        compileArgs.add(classesDir.toString());
        compileArgs.add(sourceDir.resolve("LambdaApp.java").toString());

        int compileResult = CompilerHelper.compile(config.jdkHome, compileArgs);
        assertEquals(0, compileResult, "LambdaApp should compile");

        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        Files.copy(nativeReport, classesDir.resolve("native_report.c"));

        Path outputDir = Files.createTempDirectory("lambda-integration-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "LambdaApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");

        Path srcRoot = distDir.resolve("LambdaApp-src");
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

        Path executable = buildDir.resolve("LambdaApp");
        String output = CleanTargetIntegrationTest.runCommand(Arrays.asList(executable.toString()), buildDir);
        assertTrue(output.contains("RESULT=145"), "Compiled program should print the expected result: " + output);
    }

    private CompilerHelper.CompilerConfig selectCompiler(String targetVersion) {
        List<CompilerHelper.CompilerConfig> configs = CompilerHelper.getAvailableCompilers(targetVersion);
        if (configs.isEmpty()) {
            return null;
        }
        int targetMajor = CompilerHelper.parseJavaMajor(targetVersion);
        if (targetMajor < 9) {
            for (CompilerHelper.CompilerConfig config : configs) {
                if (CompilerHelper.getJdkMajor(config) < 9) {
                    return config;
                }
            }
        }
        return configs.get(0);
    }

}
