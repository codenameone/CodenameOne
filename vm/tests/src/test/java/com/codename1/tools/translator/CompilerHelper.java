package com.codename1.tools.translator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Helper class to manage external JDK compilers.
 */
public class CompilerHelper {

    private static final Map<String, Path> availableJdks = new TreeMap<>();

    static {
        // Detect JDKs from environment variables set by CI or local setup
        checkAndAddJdk("8", System.getenv("JDK_8_HOME"));
        checkAndAddJdk("11", System.getenv("JDK_11_HOME"));
        checkAndAddJdk("17", System.getenv("JDK_17_HOME"));
        checkAndAddJdk("21", System.getenv("JDK_21_HOME"));
        checkAndAddJdk("25", System.getenv("JDK_25_HOME"));

        // Fallback: If no env vars, assume current JVM is JDK 8 (or whatever is running)
        // This ensures tests pass locally or in environments not fully configured with all JDKs
        if (availableJdks.isEmpty()) {
            String currentJavaHome = System.getProperty("java.home");
            // If it's a JRE, try to find JDK
            if (currentJavaHome.endsWith("jre")) {
                currentJavaHome = currentJavaHome.substring(0, currentJavaHome.length() - 4);
            }
            availableJdks.put(System.getProperty("java.specification.version"), Paths.get(currentJavaHome));
        }
    }

    private static void checkAndAddJdk(String version, String path) {
        if (path != null && !path.isEmpty() && new File(path).exists()) {
            availableJdks.put(version, Paths.get(path));
        }
    }

    public static List<CompilerConfig> getAvailableCompilers(String targetVersion) {
        List<CompilerConfig> compilers = new ArrayList<>();

        for (Map.Entry<String, Path> entry : availableJdks.entrySet()) {
            String jdkVersion = entry.getKey();
            Path jdkHome = entry.getValue();

            if (canCompile(jdkVersion, targetVersion)) {
                compilers.add(new CompilerConfig(jdkVersion, jdkHome, targetVersion));
            }
        }

        // If we are running in a constrained environment (e.g. local dev without env vars),
        // we might not have found the specific JDK requested.
        // If the list is empty, and target is 1.5 or 1.8, and we have *some* JDK, try to use it
        // if it supports the target.
        if (compilers.isEmpty() && !availableJdks.isEmpty()) {
             Map.Entry<String, Path> defaultJdk = availableJdks.entrySet().iterator().next();
             if (canCompile(defaultJdk.getKey(), targetVersion)) {
                 compilers.add(new CompilerConfig(defaultJdk.getKey(), defaultJdk.getValue(), targetVersion));
             }
        }

        return compilers;
    }

    private static boolean canCompile(String compilerVersion, String targetVersion) {
        int compilerMajor = parseJavaMajor(compilerVersion);
        int targetMajor = parseJavaMajor(targetVersion);

        if (compilerMajor == 0 || targetMajor == 0) {
            return true;
        }
        if (compilerMajor >= 9 && targetMajor < 9) {
            return false;
        }
        // Java 9+ (version 9, 11, etc) dropped support for 1.5
        if (targetMajor == 5) {
            return compilerMajor < 9;
        }
        // Generally newer JDKs support 1.8+
        return compilerMajor >= targetMajor || (compilerMajor >= 8 && targetMajor <= 8);
    }

    public static int parseJavaMajor(String version) {
        if (version == null || version.isEmpty()) {
            return 0;
        }
        String normalized = version.trim();
        if (normalized.startsWith("1.")) {
            normalized = normalized.substring(2);
        }
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (Character.isDigit(ch)) {
                digits.append(ch);
            } else {
                break;
            }
        }
        if (digits.length() == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static int getJdkMajor(CompilerConfig config) {
        return parseJavaMajor(config.jdkVersion);
    }

    public static int getTargetMajor(CompilerConfig config) {
        return parseJavaMajor(config.targetVersion);
    }

    /**
     * JavaAPI includes java.lang sources; on JDK 9+ they must be compiled with --patch-module.
     * JDK 9+ rejects --patch-module when targeting < 9 bytecode, so those permutations are skipped.
     */
    public static boolean isJavaApiCompatible(CompilerConfig config) {
        int jdkMajor = getJdkMajor(config);
        int targetMajor = getTargetMajor(config);
        return jdkMajor < 9 || targetMajor >= 9;
    }

    public static boolean useClasspath(CompilerConfig config) {
        return getJdkMajor(config) >= 9;
    }

    public static int compile(Path jdkHome, List<String> args) throws IOException, InterruptedException {
        String javac = jdkHome.resolve("bin").resolve("javac").toString();
        // On Windows it might be javac.exe
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            javac += ".exe";
        }

        List<String> command = new ArrayList<>();
        command.add(javac);

        // Filter out flags that might be unsupported on newer JDKs if target is old,
        // but generally we rely on the caller to provide correct flags.
        // However, we might need to suppress warnings for obsolete targets.
        // args.add("-Xlint:-options"); // Added by caller?

        command.addAll(args);

        ProcessBuilder pb = new ProcessBuilder(command);
        // Inherit IO so we see errors in the log
        pb.inheritIO();
        Process p = pb.start();
        return p.waitFor();
    }

    public static class CompilerConfig {
        public final String jdkVersion;
        public final Path jdkHome;
        public final String targetVersion;

        public CompilerConfig(String jdkVersion, Path jdkHome, String targetVersion) {
            this.jdkVersion = jdkVersion;
            this.jdkHome = jdkHome;
            this.targetVersion = targetVersion;
        }

        @Override
        public String toString() {
            return "JDK " + jdkVersion + " (Target " + targetVersion + ")";
        }
    }

    public static boolean compileAndRun(String code, String expectedOutput) throws Exception {
        // Find a suitable compiler (e.g. JDK 8 targeting 1.8)
        List<CompilerConfig> compilers = getAvailableCompilers("1.8");
        if (compilers.isEmpty()) {
            // Fallback for environment where maybe we just run with what we have
            compilers = getAvailableCompilers("1.5");
        }
        if (compilers.isEmpty()) {
             throw new RuntimeException("No suitable compiler found");
        }
        CompilerConfig config = compilers.get(0);

        java.nio.file.Path sourceDir = java.nio.file.Files.createTempDirectory("executor-test-src");
        java.nio.file.Path classesDir = java.nio.file.Files.createTempDirectory("executor-test-classes");
        java.nio.file.Path outputDir = java.nio.file.Files.createTempDirectory("executor-test-output");

        try {
            java.nio.file.Files.write(sourceDir.resolve("Main.java"), code.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            java.nio.file.Path javaApiDir = java.nio.file.Files.createTempDirectory("java-api-classes");
            if (!isJavaApiCompatible(config)) {
                throw new IllegalStateException("JDK " + config.jdkVersion + " must target matching bytecode level for JavaAPI");
            }
            compileJavaAPI(javaApiDir, config);

            List<String> compileArgs = new ArrayList<>();
            compileArgs.add("-source");
            compileArgs.add(config.targetVersion);
            compileArgs.add("-target");
            compileArgs.add(config.targetVersion);
            if (useClasspath(config)) {
                compileArgs.add("-classpath");
                compileArgs.add(javaApiDir.toString());
            } else {
                compileArgs.add("-bootclasspath");
                compileArgs.add(javaApiDir.toString());
            }
            compileArgs.add("-d");
            compileArgs.add(classesDir.toString());
            compileArgs.add(sourceDir.resolve("Main.java").toString());

            if (compile(config.jdkHome, compileArgs) != 0) {
                return false;
            }

            // Merge javaApiDir into classesDir so translator finds dependencies
            copyDirectory(javaApiDir, classesDir);

            CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "ExecutorApp");

            java.nio.file.Path distDir = outputDir.resolve("dist");

            CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(outputDir.resolve("dist").resolve("CMakeLists.txt"), "ExecutorApp-src");

            java.nio.file.Path buildDir = distDir.resolve("build");
            java.nio.file.Files.createDirectories(buildDir);

            CleanTargetIntegrationTest.runCommand(Arrays.asList(
                    "cmake",
                    "-S", distDir.toString(),
                    "-B", buildDir.toString(),
                    "-DCMAKE_C_COMPILER=clang",
                    "-DCMAKE_OBJC_COMPILER=clang"
            ), distDir);

            CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

            java.nio.file.Path executable = buildDir.resolve("ExecutorApp");
            String output = CleanTargetIntegrationTest.runCommand(Arrays.asList(executable.toString()), buildDir);
            return output.contains(expectedOutput);

        } finally {
            // cleanup?
        }
    }

    public static void compileJavaAPI(Path outputDir, CompilerConfig config) throws IOException, InterruptedException {
        Files.createDirectories(outputDir);
        Path javaApiRoot = Paths.get("..", "JavaAPI", "src").normalize().toAbsolutePath();
        List<String> sources = new ArrayList<>();
        Files.walk(javaApiRoot)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(p -> sources.add(p.toString()));

        List<String> args = new ArrayList<>();

        int jdkMajor = getJdkMajor(config);
        int targetMajor = getTargetMajor(config);

        if (jdkMajor >= 9) {
            if (targetMajor < 9) {
                throw new IllegalArgumentException("Cannot compile JavaAPI with --patch-module for target " + config.targetVersion);
            }
            args.add("--patch-module");
            args.add("java.base=" + javaApiRoot.toString());
        }

        args.add("-source");
        args.add(config.targetVersion);
        args.add("-target");
        args.add(config.targetVersion);

        args.add("-d");
        args.add(outputDir.toString());
        args.addAll(sources);

        int result = compile(config.jdkHome, args);
        if (result != 0) {
            throw new IOException("JavaAPI compilation failed with exit code " + result);
        }
    }

    public static void copyDirectory(Path sourceDir, Path targetDir) throws IOException {
        Files.walk(sourceDir).forEach(source -> {
            try {
                Path destination = targetDir.resolve(sourceDir.relativize(source));
                if (Files.isDirectory(source)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
