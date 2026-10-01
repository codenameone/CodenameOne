/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.maven;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.codename1.build.BuildFailureException;

/// Java version parsing shared by the build tools.
public final class JavaVersionUtil {
    private static final Pattern MAJOR_VERSION_PATTERN = Pattern.compile("^(?:1\\.)?(\\d+)");

    /// The oldest JDK the simulator and desktop runs support.
    public static final int MIN_RUNTIME_JAVA_VERSION = 11;

    private JavaVersionUtil() {
    }

    /// The major version in `version` (`1.8` and `8` are both 8), or
    /// `defaultValue` when there is none.
    public static int parseJavaVersion(String version, int defaultValue) {
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
    public static int getRuntimeMajorVersion(int defaultValue) {
        String spec = System.getProperty("java.specification.version");
        int parsed = parseJavaVersion(spec, -1);
        if (parsed > 0) {
            return parsed;
        }
        return parseJavaVersion(System.getProperty("java.version"), defaultValue);
    }

    /**
     * Aborts the current goal or task with a friendly, actionable message when the JVM
     * Maven is running on is older than the supplied minimum.
     *
     * @param minimumMajorVersion smallest acceptable major version (e.g. 11)
     * @param operationLabel short description of what the user was trying to do (used in the error message)
     */
    public static void requireRuntimeJavaVersion(int minimumMajorVersion, String operationLabel) throws BuildFailureException {
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
        throw new BuildFailureException(msg.toString());
    }
}
