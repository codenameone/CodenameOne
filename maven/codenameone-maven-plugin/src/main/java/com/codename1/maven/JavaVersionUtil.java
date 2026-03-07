package com.codename1.maven;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JavaVersionUtil {
    private static final Pattern MAJOR_VERSION_PATTERN = Pattern.compile("^(?:1\\.)?(\\d+)");

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
}
