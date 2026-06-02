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

    private String nativeReportSource(String className) {
        return "#include \"cn1_globals.h\"\n" +
                "#include <stdio.h>\n" +
                "void " + className + "_report___int(CODENAME_ONE_THREAD_STATE, JAVA_INT value) {\n" +
                "    printf(\"RESULT=%d\\n\", value);\n" +
                "}\n";
    }

    /**
     * Method references whose implementation parameter / return types differ
     * from the functional interface's signature force LambdaMetafactory to
     * insert box / unbox / widen / cast adaptation. The translator generates
     * the adapter class itself, so it has to replicate that adaptation -- a
     * regression here surfaced as iOS Xcode builds failing to compile (a
     * Double object passed where a primitive double was expected).
     */
    private String unboxAppSource() {
        return "public class UnboxApp {\n" +
                "    interface DoubleSink { void accept(Double value); }\n" +   // explicit wrapper -> unbox to double
                "    interface Sink<T> { void accept(T value); }\n" +           // erased Object -> checkcast Double + unbox
                "    interface DoubleSource { Double get(); }\n" +              // primitive double return -> box to Double
                "    interface IntToLong { long apply(int x); }\n" +           // widen int argument to long
                "\n" +
                "    static double accumulated;\n" +
                "\n" +
                "    void addValue(double v) { accumulated += v; }\n" +
                "    static double produceHalf() { return 0.5; }\n" +
                "    static long identityLong(long x) { return x; }\n" +
                "\n" +
                "    public static void main(String[] args) {\n" +
                "        UnboxApp app = new UnboxApp();\n" +
                "\n" +
                "        // 1. unbox: accept(Double) bound to addValue(double)\n" +
                "        DoubleSink sink = app::addValue;\n" +
                "        sink.accept(Double.valueOf(2.5));            // accumulated = 2.5\n" +
                "\n" +
                "        // 2. unbox via erased generic SAM: accept(Object) bound to addValue(double)\n" +
                "        Sink<Double> gsink = app::addValue;\n" +
                "        gsink.accept(Double.valueOf(4.0));           // accumulated = 6.5\n" +
                "\n" +
                "        // 3. box: produceHalf() double return bound to get() Double return\n" +
                "        DoubleSource src = UnboxApp::produceHalf;\n" +
                "        Double boxed = src.get();                    // 0.5 boxed\n" +
                "        app.addValue(boxed.doubleValue());           // accumulated = 7.0\n" +
                "\n" +
                "        // 4. widen: apply(int) bound to identityLong(long)\n" +
                "        IntToLong widener = UnboxApp::identityLong;\n" +
                "        long w = widener.apply(3);                   // 3\n" +
                "\n" +
                "        report((int)(accumulated * 10) + (int) w);   // 70 + 3 = 73\n" +
                "    }\n" +
                "\n" +
                "    private static native void report(int value);\n" +
                "}\n";
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.8"})
    void translatesLambdaBytecodeToLLVMExecutable(String targetVersion) throws Exception {
        translateBuildAndRun(targetVersion, "LambdaApp", appSource(), "RESULT=145");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.8"})
    void adaptsBoxingUnboxingInMethodReferences(String targetVersion) throws Exception {
        translateBuildAndRun(targetVersion, "UnboxApp", unboxAppSource(), "RESULT=73");
    }

    private void translateBuildAndRun(String targetVersion, String className, String source, String expectedOutput) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("lambda-integration-sources");
        Path classesDir = Files.createTempDirectory("lambda-integration-classes");
        Path javaApiDir = Files.createTempDirectory("java-api-classes");

        Files.write(sourceDir.resolve(className + ".java"), source.getBytes(StandardCharsets.UTF_8));

        CompilerHelper.CompilerConfig config = selectCompiler(targetVersion);
        if (config == null) {
            fail("No compiler available for target " + targetVersion);
        }

        assertTrue(CompilerHelper.isJavaApiCompatible(config),
                "JDK " + config.jdkVersion + " must target matching bytecode level for JavaAPI");

        CompilerHelper.compileJavaAPI(javaApiDir, config);

        Path nativeReport = sourceDir.resolve("native_report.c");
        Files.write(nativeReport, nativeReportSource(className).getBytes(StandardCharsets.UTF_8));

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
        compileArgs.add(sourceDir.resolve(className + ".java").toString());

        int compileResult = CompilerHelper.compile(config.jdkHome, compileArgs);
        assertEquals(0, compileResult, className + " should compile");

        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        Files.copy(nativeReport, classesDir.resolve("native_report.c"));

        Path outputDir = Files.createTempDirectory("lambda-integration-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, className);

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");

        Path srcRoot = distDir.resolve(className + "-src");

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

        Path executable = buildDir.resolve(className);
        String output = CleanTargetIntegrationTest.runCommand(Arrays.asList(executable.toString()), buildDir);
        assertTrue(output.contains(expectedOutput), "Compiled program should print the expected result: " + output);
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
