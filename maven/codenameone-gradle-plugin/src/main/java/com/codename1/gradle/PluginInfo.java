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
package com.codename1.gradle;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/// Facts about this plugin build.
final class PluginInfo {
    /// Where Codename One publishes, the plugin and the framework alike.
    static final String REPOSITORY_URL = "https://repo.codenameone.com/maven2";
    /// The Codename One group id.
    static final String GROUP = "com.codenameone";

    private static String version;

    private PluginInfo() {
    }

    private static Properties properties;

    private static synchronized Properties properties() {
        if (properties == null) {
            Properties p = new Properties();
            InputStream in = PluginInfo.class.getResourceAsStream("plugin.properties");
            try {
                if (in != null) {
                    p.load(in);
                }
            } catch (IOException ignored) {
                // Falls through to the defaults of each reader below.
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ignored) {
                        // A classpath resource; nothing to recover.
                    }
                }
            }
            properties = p;
        }
        return properties;
    }

    /// The plugin's version, which is also the Codename One version a project
    /// builds against unless it sets `codenameone { version = ... }`.
    static synchronized String version() {
        if (version == null) {
            String v = properties().getProperty("version");
            version = v == null || v.startsWith("${") ? "8.0-SNAPSHOT" : v.trim();
        }
        return version;
    }

    /// A version the plugin was built with (`ffmpeg.version`, `javacpp.version`),
    /// or null when the plugin runs from classes nothing filtered.
    static String builtWith(String key) {
        String v = properties().getProperty(key);
        return v == null || v.trim().length() == 0 || v.startsWith("${") ? null : v.trim();
    }

    /// The bytedeco platform names of the native jars the simulator can load on
    /// this machine -- the same table as the profiles of `cn1-binaries-javase`,
    /// which Maven evaluates and Gradle does not. A Mac gets both architectures:
    /// the JVM that configures the build and the one that runs the simulator are
    /// routinely different there (an x64 JDK under Rosetta beside an arm64 one).
    /// Empty where bytedeco has no build; the simulator then looks for ffmpeg on
    /// `PATH`.
    static java.util.List<String> hostFfmpegPlatforms(String osName, String osArch) {
        String os = osName == null ? "" : osName.toLowerCase(java.util.Locale.ROOT);
        String arch = osArch == null ? "" : osArch.toLowerCase(java.util.Locale.ROOT);
        if (os.contains("mac") || os.contains("darwin")) {
            return java.util.Arrays.asList("macosx-arm64", "macosx-x86_64");
        }
        if (os.contains("win")) {
            return java.util.Collections.singletonList("windows-x86_64");
        }
        if (os.contains("linux")) {
            if ("amd64".equals(arch) || "x86_64".equals(arch)) {
                return java.util.Collections.singletonList("linux-x86_64");
            }
            if ("aarch64".equals(arch) || "arm64".equals(arch)) {
                return java.util.Collections.singletonList("linux-arm64");
            }
        }
        return java.util.Collections.<String>emptyList();
    }
}
