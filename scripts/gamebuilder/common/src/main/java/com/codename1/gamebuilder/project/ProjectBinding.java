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
package com.codename1.gamebuilder.project;

/// The project binding the `cn1:gamebuilder` Maven goal writes to
/// {@code ~/.gameBuilder/gamebuilder.input}: where the editor should read and write a
/// project's game scenes. Pure value object with a tolerant {@code key=value} parser so
/// it is unit-testable without a filesystem.
public final class ProjectBinding {
    private String projectDir;
    private String gamesDir;
    private String sourceDir;
    private String packageName;
    private String output;
    private String buildSystem;
    private String kind;
    private String rootDir;
    private String dependencyFile;

    public String projectDir() {
        return projectDir;
    }

    public String packageName() {
        return packageName;
    }

    /// The scene directory: `gamesDir=` when the launcher named it, otherwise
    /// `games/` in the resources directory the binding's `buildSystem=`
    /// implies (`src/` under Ant, `src/main/resources` under Maven's `common/`
    /// and Gradle), so a bare project descriptor is enough to open a project.
    public String gamesDir() {
        if (gamesDir != null || projectDir == null || buildSystem == null) {
            return gamesDir;
        }
        return projectDir + (isAnt() ? "/src/games" : "/src/main/resources/games");
    }

    /// The Java source root, `sourceDir=` or else the build system's convention.
    public String sourceDir() {
        if (sourceDir != null || projectDir == null || buildSystem == null) {
            return sourceDir;
        }
        return projectDir + (isAnt() ? "/src" : "/src/main/java");
    }

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

    private boolean isAnt() {
        return "ANT".equals(buildSystem);
    }

    public String output() {
        return output;
    }

    public boolean isValid() {
        String dir = gamesDir();
        return dir != null && !dir.isEmpty();
    }

    /// Parses the {@code key=value} descriptor (lines starting with {@code #} are ignored).
    public static ProjectBinding parse(String content) {
        ProjectBinding b = new ProjectBinding();
        if (content == null) {
            return b;
        }
        String[] lines = content.replace("\r\n", "\n").split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = line.substring(0, eq).trim();
            String val = line.substring(eq + 1).trim();
            switch (key) {
                case "projectDir" -> b.projectDir = val;
                case "gamesDir" -> b.gamesDir = val;
                case "sourceDir" -> b.sourceDir = val;
                case "packageName" -> b.packageName = val;
                case "output" -> b.output = val;
                case "buildSystem" -> b.buildSystem = val;
                case "kind" -> b.kind = val;
                case "rootDir" -> b.rootDir = val;
                case "dependencyFile" -> b.dependencyFile = val;
                default -> {
                    // ignore unknown keys for forward-compatibility
                }
            }
        }
        return b;
    }
}
