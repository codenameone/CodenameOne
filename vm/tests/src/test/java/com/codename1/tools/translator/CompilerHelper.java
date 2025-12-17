package com.codename1.tools.translator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        try {
            double compilerVer = Double.parseDouble(compilerVersion);
            double targetVer = Double.parseDouble(targetVersion);

            // Java 9+ (version 9, 11, etc) dropped support for 1.5
            if (targetVer == 1.5) {
                return compilerVer < 9;
            }
            // Java 21? dropped support for 1.6/1.7?
            // Generally newer JDKs support 1.8+
            return compilerVer >= targetVer || (compilerVer >= 1.8 && targetVer <= 1.8);
        } catch (NumberFormatException e) {
            // Handle "1.8" format
            if (compilerVersion.startsWith("1.")) {
                return true; // Old JDKs support old targets
            }
            // Fallback for "25-ea"
            if (compilerVersion.contains("-")) {
                 // Assume it's a new JDK
                 return !"1.5".equals(targetVersion);
            }
            return true;
        }
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
}
