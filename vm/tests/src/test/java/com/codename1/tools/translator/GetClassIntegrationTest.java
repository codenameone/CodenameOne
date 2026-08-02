package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Pins {@code Object.getClass()} and the behavior of Class objects as map keys
 * (issue #5482, where the reporter observed {@code getClass()} apparently returning
 * null while a dictionary load was allocating heavily).
 *
 * <p>Class objects are not ordinary heap objects in ParparVM: they are static
 * {@code struct clazz} instances, and {@code getClassImpl} hands the struct back
 * directly. That makes their identity, hash, and virtual {@code toString} dispatch worth
 * regression coverage independently of whatever else the reporter's program was doing.</p>
 */
class GetClassIntegrationTest {

    @Test
    void classObjectsBehaveLikeTheJvmUnderAllocationChurn() throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("get-class-sources");
        Path classesDir = Files.createTempDirectory("get-class-classes");
        Path javaApiDir = Files.createTempDirectory("get-class-java-api");

        Path source = sourceDir.resolve("GetClassApp.java");
        Files.write(source, loadAppSource().getBytes(StandardCharsets.UTF_8));

        CompilerHelper.CompilerConfig config = selectCompiler();
        if (config == null) {
            fail("No compatible compiler available for the getClass integration test");
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

        assertEquals(0, CompilerHelper.compile(config.jdkHome, compileArgs),
                "GetClassApp should compile against the JavaAPI");

        Map<String, String> expected = parseCases(runJavaMain(config, classesDir, javaApiDir));
        assertFalse(expected.isEmpty(), "JVM run should emit cases");

        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        Path outputDir = Files.createTempDirectory("get-class-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "GetClassApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, "GetClassApp-src");

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

        Path executable = buildDir.resolve("GetClassApp");
        String parparOutput = CleanTargetIntegrationTest.runCommand(
                Arrays.asList(executable.toString()), buildDir);
        assertTrue(parparOutput.contains("DONE"),
                "ParparVM run should complete. Output: " + parparOutput);

        Map<String, String> actual = parseCases(parparOutput);
        assertEquals(expected.keySet(), actual.keySet(), "ParparVM should emit the same cases");

        List<String> differences = new ArrayList<>();
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            if (!entry.getValue().equals(actual.get(entry.getKey()))) {
                differences.add(entry.getKey()
                        + "\n    jvm     : " + entry.getValue()
                        + "\n    parparvm: " + actual.get(entry.getKey()));
            }
        }
        assertTrue(differences.isEmpty(),
                "Class object behavior diverged from the JVM:\n" + String.join("\n", differences));

        // Stated explicitly so a regression names the reported symptom rather than a
        // generic diff.
        assertEquals("true", actual.get("notNull"), "getClass() must never return null");
        assertEquals("0", actual.get("churnFailures"),
                "getClass() must stay correct while the heap churns");
    }

    private Map<String, String> parseCases(String output) {
        Map<String, String> cases = new LinkedHashMap<>();
        for (String line : output.split("\\R")) {
            if (!line.startsWith("CASE|")) {
                continue;
            }
            String body = line.substring("CASE|".length());
            int separator = body.indexOf('|');
            assertTrue(separator > 0, "Malformed case line: " + line);
            cases.put(body.substring(0, separator), body.substring(separator + 1));
        }
        return cases;
    }

    private String loadAppSource() throws Exception {
        java.io.InputStream in = GetClassIntegrationTest.class
                .getResourceAsStream("/com/codename1/tools/translator/GetClassApp.java");
        assertNotNull(in, "GetClassApp.java test resource should exist");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n")) + "\n";
        }
    }

    private String runJavaMain(CompilerHelper.CompilerConfig config, Path classesDir, Path javaApiDir)
            throws Exception {
        String javaExe = config.jdkHome.resolve("bin").resolve(CompilerHelper.executableName("java")).toString();
        ProcessBuilder pb = new ProcessBuilder(
                javaExe,
                "-cp",
                classesDir + System.getProperty("path.separator") + javaApiDir,
                "GetClassApp"
        );
        pb.redirectErrorStream(true);

        Process process = pb.start();
        String output;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }
        assertEquals(0, process.waitFor(), "JVM run should exit cleanly. Output: " + output);
        return output;
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
