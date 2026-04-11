package com.codename1.tools.translator;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("benchmark")
class Base64PerformanceIntegrationTest {

    @Test
    void base64BenchmarkProducesComparableResultsInParparVm() throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("base64-perf-sources");
        Path classesDir = Files.createTempDirectory("base64-perf-classes");
        Path javaApiDir = Files.createTempDirectory("base64-perf-javaapi");
        Path coreJar = findClasspathJar("codenameone-core");
        assertNotNull(coreJar, "codenameone-core jar should be present on the test classpath");

        Path source = sourceDir.resolve("Base64PerfApp.java");
        Files.write(source, loadAppSource().getBytes(StandardCharsets.UTF_8));

        CompilerHelper.CompilerConfig config = selectCompiler();
        if (config == null) {
            fail("No compatible compiler available for Base64 performance integration test");
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
            compileArgs.add(javaApiDir + System.getProperty("path.separator") + coreJar);
        } else {
            compileArgs.add("-bootclasspath");
            compileArgs.add(javaApiDir + System.getProperty("path.separator") + coreJar);
            compileArgs.add("-Xlint:-options");
        }
        compileArgs.add("-d");
        compileArgs.add(classesDir.toString());
        compileArgs.add(source.toString());

        int compileResult = CompilerHelper.compile(config.jdkHome, compileArgs);
        assertEquals(0, compileResult, "Base64PerfApp should compile. " + CompilerHelper.getLastErrorLog());

        String javaOutput = runJavaMain(config, classesDir, javaApiDir, coreJar);
        String javaResult = extractLine(javaOutput, "RESULT=");
        assertTrue(javaResult.startsWith("RESULT="), "JavaSE should produce RESULT=. Output: " + javaOutput);

        CompilerHelper.copyDirectory(javaApiDir, classesDir);
        unzipAllClasses(coreJar, classesDir);

        Path outputDir = Files.createTempDirectory("base64-perf-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "Base64PerfApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");

        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, "Base64PerfApp-src");

        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);

        CleanTargetIntegrationTest.runCommand(Arrays.asList(
                "cmake",
                "-S", distDir.toString(),
                "-B", buildDir.toString(),
                "-DCMAKE_BUILD_TYPE=Release",
                "-DCMAKE_C_COMPILER=clang",
                "-DCMAKE_OBJC_COMPILER=clang"
        ), distDir);

        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

        Path executable = buildDir.resolve("Base64PerfApp");
        assertTrue(Files.exists(executable), "ParparVM build should produce a runnable executable");
        String vmOutput = runVmBenchmarkWithRetry(executable, buildDir);
        String vmResult = extractLine(vmOutput, "RESULT=");
        assertEquals(javaResult, vmResult, "JavaSE and ParparVM should produce identical RESULT signatures");

        assertTrue(extractLine(vmOutput, "ENCODE_MS=").startsWith("ENCODE_MS="),
                "ParparVM output should include ENCODE_MS timing. Output: " + vmOutput);
        assertTrue(extractLine(vmOutput, "DECODE_MS=").startsWith("DECODE_MS="),
                "ParparVM output should include DECODE_MS timing. Output: " + vmOutput);
    }

    private String loadAppSource() throws Exception {
        java.io.InputStream in = Base64PerformanceIntegrationTest.class.getResourceAsStream("/com/codename1/tools/translator/Base64PerfApp.java");
        assertNotNull(in, "Base64PerfApp.java test resource should exist");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n")) + "\n";
        }
    }

    private String runJavaMain(CompilerHelper.CompilerConfig config, Path classesDir, Path javaApiDir, Path coreJar) throws Exception {
        String javaExe = config.jdkHome.resolve("bin").resolve("java").toString();
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            javaExe += ".exe";
        }

        ProcessBuilder pb = new ProcessBuilder(
                javaExe,
                "-cp",
                classesDir + System.getProperty("path.separator") + javaApiDir + System.getProperty("path.separator") + coreJar,
                "Base64PerfApp"
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

    private String extractLine(String output, String prefix) {
        for (String line : output.split("\\R")) {
            if (line.startsWith(prefix)) {
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

    private Path findClasspathJar(String namePart) {
        String classpath = System.getProperty("java.class.path");
        String[] entries = classpath.split(System.getProperty("path.separator"));
        for (String entry : entries) {
            Path p = Paths.get(entry);
            if (Files.isRegularFile(p) && p.getFileName().toString().contains(namePart)) {
                return p.toAbsolutePath().normalize();
            }
        }
        return null;
    }

    private void unzipAllClasses(Path zipFile, Path outputDir) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                    continue;
                }
                Path out = outputDir.resolve(entry.getName());
                Files.createDirectories(out.getParent());
                Files.copy(zis, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private String runVmBenchmarkWithRetry(Path executable, Path workingDir) throws Exception {
        final int maxAttempts = 4;
        AssertionFailedError lastSegfaultFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return CleanTargetIntegrationTest.runCommand(Arrays.asList(executable.toString()), workingDir);
            } catch (AssertionFailedError failure) {
                if (!looksLikeSegmentationFault(failure)) {
                    throw failure;
                }
                lastSegfaultFailure = failure;
                if (attempt < maxAttempts) {
                    Thread.sleep(100L * attempt);
                }
            }
        }
        throw lastSegfaultFailure;
    }

    private boolean looksLikeSegmentationFault(AssertionFailedError failure) {
        String message = failure.getMessage();
        if (message == null) {
            return false;
        }
        return message.contains("but was: <139>") || message.contains("Segmentation fault");
    }
}
