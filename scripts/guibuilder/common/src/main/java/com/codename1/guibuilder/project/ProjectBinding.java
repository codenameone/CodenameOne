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

package com.codename1.guibuilder.project;

public final class ProjectBinding {
    private String projectDir;
    private String guiDir;
    private String sourceDir;
    private String cssFile;
    private String initialForm;
    private String buildSystem;
    private String kind;
    private String rootDir;
    private String dependencyFile;

    public String projectDir() { return projectDir; }

    /// The `.gui` directory: `guiDir=` when the launcher named it, otherwise
    /// the conventional one for the binding's `buildSystem=` (`res/guibuilder`
    /// under Ant, `src/main/guibuilder` under Maven's `common/` and Gradle), so
    /// a bare project descriptor is enough to open a project.
    public String guiDir() {
        if (guiDir != null || projectDir == null || buildSystem == null) return guiDir;
        return projectDir + (isAnt() ? "/res/guibuilder" : "/src/main/guibuilder");
    }

    /// The Java source root, `sourceDir=` or else the build system's convention.
    public String sourceDir() {
        if (sourceDir != null || projectDir == null || buildSystem == null) return sourceDir;
        return projectDir + (isAnt() ? "/src" : "/src/main/java");
    }

    /// The theme stylesheet, `cssFile=` or else the build system's convention.
    public String cssFile() {
        if (cssFile != null || projectDir == null || buildSystem == null) return cssFile;
        return projectDir + (isAnt() ? "/css/theme.css" : "/src/main/css/theme.css");
    }

    public String initialForm() { return initialForm; }
    public boolean isValid() { return projectDir != null && guiDir() != null; }

    /// `ANT`, `MAVEN` or `GRADLE`: the standard `buildSystem=` key of the
    /// project descriptor the build writes, or null when the launcher did not
    /// say (a Maven launcher that predates the key).
    public String buildSystem() {
        return buildSystem;
    }

    /// `APP`, `LIB` or `BACKEND`, or null when the launcher did not say.
    public String kind() {
        return kind;
    }

    /// The top of the build (Maven multi-module root, Gradle root project or
    /// Ant project), falling back to [projectDir()] when the launcher did not
    /// say.
    public String rootDir() {
        return rootDir != null && rootDir.length() > 0 ? rootDir : projectDir;
    }

    /// The build file the project's dependencies are declared in (the
    /// `pom.xml` or `build.gradle.kts`), or null when the launcher did not say.
    public String dependencyFile() {
        return dependencyFile;
    }

    private boolean isAnt() { return "ANT".equals(buildSystem); }

    public static ProjectBinding parse(String content) {
        ProjectBinding binding = new ProjectBinding();
        if (content == null) return binding;
        for (String line : content.replace("\r\n", "\n").split("\n")) {
            String value = line.trim();
            if (value.length() == 0 || value.startsWith("#")) continue;
            int split = value.indexOf('=');
            if (split < 1) continue;
            String key = value.substring(0, split).trim();
            String field = value.substring(split + 1).trim();
            switch (key) {
                case "projectDir":
                    binding.projectDir = field;
                    break;
                case "guiDir":
                    binding.guiDir = field;
                    break;
                case "sourceDir":
                    binding.sourceDir = field;
                    break;
                case "cssFile":
                    binding.cssFile = field;
                    break;
                case "initialForm":
                    binding.initialForm = field;
                    break;
                case "buildSystem":
                    binding.buildSystem = field;
                    break;
                case "kind":
                    binding.kind = field;
                    break;
                case "rootDir":
                    binding.rootDir = field;
                    break;
                case "dependencyFile":
                    binding.dependencyFile = field;
                    break;
                default:
                    break;
            }
        }
        return binding;
    }
}
