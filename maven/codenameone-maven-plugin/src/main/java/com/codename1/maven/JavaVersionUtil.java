package com.codename1.maven;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoFailureException;

final class JavaVersionUtil {
    private static final Pattern MAJOR_VERSION_PATTERN = Pattern.compile("^(?:1\\.)?(\\d+)");

    static final int MIN_RUNTIME_JAVA_VERSION = 11;

    private JavaVersionUtil() {
    }

    static int parseJavaVersion(String version, int defaultValue) {
        if (version == null) {
            return defaultValue;
        }
        String normalized = version.trim();
        if (normalized.isEmpty()) {
            return defaultValue;
        }
        Matcher matcher = MAJOR_VERSION_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * Returns the major Java version of the JVM running this code (e.g. 8, 11, 17).
     * Falls back to {@code defaultValue} if the version cannot be parsed.
     */
    static int getRuntimeMajorVersion(int defaultValue) {
        String spec = System.getProperty("java.specification.version");
        int parsed = parseJavaVersion(spec, -1);
        if (parsed > 0) {
            return parsed;
        }
        return parseJavaVersion(System.getProperty("java.version"), defaultValue);
    }

    /**
     * Aborts the current Maven goal with a friendly, actionable message when the JVM
     * Maven is running on is older than the supplied minimum.
     *
     * @param minimumMajorVersion smallest acceptable major version (e.g. 11)
     * @param operationLabel short description of what the user was trying to do (used in the error message)
     */
    static void requireRuntimeJavaVersion(int minimumMajorVersion, String operationLabel) throws MojoFailureException {
        int current = getRuntimeMajorVersion(-1);
        if (current >= minimumMajorVersion) {
            return;
        }
        String detected = current > 0
                ? "Java " + current
                : "an unknown Java version (java.version=" + System.getProperty("java.version") + ")";
        String javaHome = System.getProperty("java.home");
        StringBuilder msg = new StringBuilder();
        msg.append('\n');
        msg.append("Codename One supports JDK ").append(minimumMajorVersion).append(" through 25 to ")
                .append(operationLabel).append(".\n");
        msg.append("Detected ").append(detected);
        if (javaHome != null) {
            msg.append(" at ").append(javaHome);
        }
        msg.append(".\n\n");
        msg.append("Install JDK ").append(minimumMajorVersion).append(" or newer (e.g. Eclipse Temurin\n")
                .append("from https://adoptium.net), point JAVA_HOME at it, and re-run this goal.\n");
        throw new MojoFailureException(msg.toString());
    }
}
