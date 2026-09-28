/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.settings.project;

public final class ProjectBinding {
    /// The build tools a binding can name in `buildSystem=`, the values the
    /// launcher's `ProjectDescriptor` writes.
    public static final String BUILD_ANT = "ANT";
    public static final String BUILD_MAVEN = "MAVEN";
    public static final String BUILD_GRADLE = "GRADLE";

    private String projectDir;
    private String settings;
    private String pom;
    private String buildSystem;
    private String kind;
    private String rootDir;
    private String dependencyFile;
    private String multimoduleRoot;
    private final java.util.List<String> sourceRoots = new java.util.ArrayList<>();
    private String sourceEncoding;
    private String mainName;
    private String packageName;

    public String projectDir() {
        return projectDir;
    }

    public String settings() {
        return settings;
    }

    public String pom() {
        return pom;
    }

    public String multimoduleRoot() {
        return multimoduleRoot;
    }

    /// `ANT`, `MAVEN` or `GRADLE` as the launcher wrote it, or null when an
    /// older launcher did not say. A binding with no `buildSystem=` but a
    /// `pom=` came from the Maven plugin, which is the only launcher that
    /// predates the key, so [buildSystem()] answers `MAVEN` for it.
    public String buildSystem() {
        if (buildSystem != null && buildSystem.length() > 0) {
            return buildSystem;
        }
        return pom != null && pom.length() > 0 ? BUILD_MAVEN : null;
    }

    /// Whether the project is built by the Gradle plugin.
    public boolean isGradle() {
        return BUILD_GRADLE.equals(buildSystem());
    }

    /// Whether the project is built by Maven.
    public boolean isMaven() {
        return BUILD_MAVEN.equals(buildSystem());
    }

    /// `APP`, `LIB` or `BACKEND`, or null when the launcher did not say.
    public String kind() {
        return kind;
    }

    /// The top of the build (the Maven multi-module root, the Gradle root
    /// project or the Ant project). Falls back to `multimoduleRoot=` and then
    /// `projectDir=` for a binding written before the key existed.
    public String rootDir() {
        if (rootDir != null && rootDir.length() > 0) {
            return rootDir;
        }
        if (multimoduleRoot != null && multimoduleRoot.length() > 0) {
            return multimoduleRoot;
        }
        return projectDir;
    }

    /// The build file the project's own dependencies are declared in: the
    /// `common/pom.xml` of a Maven project or the `build.gradle.kts` of a
    /// Gradle one. Falls back to `pom=` for a binding that predates the key.
    public String dependencyFile() {
        if (dependencyFile != null && dependencyFile.length() > 0) {
            return dependencyFile;
        }
        return pom;
    }

    /// The compile source roots Maven RESOLVED, one per `sourceRoot=` line, or
    /// empty when the launcher did not say.
    ///
    /// The tool can read a POM but it has no model: it cannot evaluate a profile
    /// activation, follow an inherited `<sourceDirectory>` or expand an
    /// arbitrary property. Where the launcher knows, it says, and the tool's own
    /// reading is the fallback.
    public java.util.List<String> sourceRoots() {
        return sourceRoots;
    }

    /// The source encoding Maven resolved, or null when the launcher did not
    /// say.
    public String sourceEncoding() {
        return sourceEncoding;
    }

    /// The main class name Maven RESOLVED, or null when the launcher did not
    /// say.
    ///
    /// `codename1.mainName` can be overridden with `-D`, and the overlay is what
    /// `process-annotations` stamps the manifest with and what `CN1BuildMojo`
    /// expects. Reading the settings file here looked at a different class than
    /// the build does, and an annotation on the class the build actually selected
    /// was reported as absent -- which is the state that lets Add write the
    /// duplicate the next build refuses.
    public String mainName() {
        return mainName;
    }

    /// The package name Maven resolved, or null when the launcher did not say.
    ///
    /// Taken together with [mainName] or not at all: a resolved main class with
    /// no package means a project that has none, not one whose package should
    /// come from the file.
    public String packageName() {
        return packageName;
    }

    public boolean isValid() {
        return settings != null && settings.length() > 0;
    }

    public static ProjectBinding parse(String content) {
        ProjectBinding b = new ProjectBinding();
        if (content == null) {
            return b;
        }
        String[] lines = content.replace("\r\n", "\n").split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() == 0 || trimmed.startsWith("#")) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = trimmed.substring(0, eq).trim();
            String val = trimmed.substring(eq + 1).trim();
            switch (key) {
                case "projectDir" -> b.projectDir = val;
                case "settings" -> b.settings = val;
                case "pom" -> b.pom = val;
                case "multimoduleRoot" -> b.multimoduleRoot = val;
                case "buildSystem" -> b.buildSystem = val;
                case "kind" -> b.kind = val;
                case "rootDir" -> b.rootDir = val;
                case "dependencyFile" -> b.dependencyFile = val;
                case "sourceRoot" -> {
                    if (!val.isEmpty()) {
                        b.sourceRoots.add(val);
                    }
                }
                case "sourceEncoding" -> b.sourceEncoding = val;
                case "mainName" -> b.mainName = val;
                case "packageName" -> b.packageName = val;
                default -> {
                }
            }
        }
        return b;
    }
}
