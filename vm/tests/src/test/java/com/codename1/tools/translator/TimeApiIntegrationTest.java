package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class TimeApiIntegrationTest {

    @Test
    void timeEdgeCasesMatchBetweenJavaSEAndParparVM() throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("time-integration-sources");
        Path classesDir = Files.createTempDirectory("time-integration-classes");
        Path javaApiDir = Files.createTempDirectory("java-api-classes");

        Path source = sourceDir.resolve("TimeEdgeApp.java");
        Files.write(source, loadAppSource().getBytes(StandardCharsets.UTF_8));

        CompilerHelper.CompilerConfig config = selectCompiler();
        if (config == null) {
            fail("No compatible compiler available for time integration test");
        }

        assertTrue(CompilerHelper.isJavaApiCompatible(config),
                "JDK " + config.jdkVersion + " must target matching bytecode level for JavaAPI");

        CompilerHelper.compileJavaAPI(javaApiDir, config);

        List<String> compileArgs = new ArrayList<>();
        compileArgs.add("-source");
        compileArgs.add(config.targetVersion);
        compileArgs.add("-target");
        compileArgs.add(config.targetVersion);
        if (CompilerHelper.useClasspath(config)) {
            compileArgs.add("-classpath");
            compileArgs.add(javaApiDir.toString());
        } else {
            compileArgs.add("-bootclasspath");
            compileArgs.add(javaApiDir.toString());
            compileArgs.add("-Xlint:-options");
        }
        compileArgs.add("-d");
        compileArgs.add(classesDir.toString());
        compileArgs.add(source.toString());

        int compileResult = CompilerHelper.compile(config.jdkHome, compileArgs);
        assertEquals(0, compileResult, "TimeEdgeApp should compile. " + CompilerHelper.getLastErrorLog());

        String javaOutput = runJavaMain(config, classesDir, javaApiDir);
        String javaResult = extractResultLine(javaOutput);
        assertTrue(javaResult.startsWith("RESULT="), "JavaSE should produce a RESULT line. Output: " + javaOutput);

        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        Path outputDir = Files.createTempDirectory("time-integration-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "TimeEdgeApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");

        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, "TimeEdgeApp-src");

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

        Path executable = buildDir.resolve("TimeEdgeApp");
        assertTrue(Files.exists(executable), "ParparVM build should produce a runnable executable");
        String vmOutput = CleanTargetIntegrationTest.runCommand(Arrays.asList(executable.toString()), buildDir);
        String vmResult = extractResultLine(vmOutput);
        assertEquals(javaResult, vmResult, "ParparVM output should match JavaSE");
    }

    private String loadAppSource() throws Exception {
        java.io.InputStream in = TimeApiIntegrationTest.class.getResourceAsStream("/com/codename1/tools/translator/TimeEdgeApp.java");
        assertNotNull(in, "TimeEdgeApp.java test resource should exist");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n")) + "\n";
        }
    }

    private String runJavaMain(CompilerHelper.CompilerConfig config, Path classesDir, Path javaApiDir) throws Exception {
        String javaExe = config.jdkHome.resolve("bin").resolve("java").toString();
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            javaExe += ".exe";
        }

        ProcessBuilder pb = new ProcessBuilder(
                javaExe,
                "-cp",
                classesDir + System.getProperty("path.separator") + javaApiDir,
                "TimeEdgeApp"
        );
        pb.redirectErrorStream(true);

        Process process = pb.start();
        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }

        int exitCode = process.waitFor();
        assertEquals(0, exitCode, "JVM run should exit cleanly. Output: " + output);
        return output;
    }

    private String extractResultLine(String output) {
        for (String line : output.split("\\R")) {
            if (line.startsWith("RESULT=")) {
                return line.trim();
            }
        }
        return "";
    }

    private CompilerHelper.CompilerConfig selectCompiler() {
        String[] preferredTargets = {"11", "17", "21", "25", "1.8"};
        for (String target : preferredTargets) {
            List<CompilerHelper.CompilerConfig> configs = CompilerHelper.getAvailableCompilers(target);
            for (CompilerHelper.CompilerConfig config : configs) {
                if (CompilerHelper.isJavaApiCompatible(config)) {
                    return config;
                }
            }
        }
        return null;
    }
}
