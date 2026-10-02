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
package com.codename1.certificatewizard.project;

public final class ProjectBinding {
    private String projectDir;
    private String settings;
    private String outputDir;
    private String user;
    private String token;
    private String baseUrl;
    private String buildSystem;
    private String kind;
    private String rootDir;
    private String dependencyFile;

    public String projectDir() {
        return projectDir;
    }

    public String settings() {
        return settings;
    }

    public String outputDir() {
        return outputDir;
    }

    public String user() {
        return user;
    }

    public String token() {
        return token;
    }

    public String baseUrl() {
        return baseUrl;
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

    public boolean isValid() {
        return settings != null && !settings.isEmpty();
    }

    public static ProjectBinding parse(String content) {
        ProjectBinding b = new ProjectBinding();
        if (content == null) {
            return b;
        }
        String[] lines = content.replace("\r\n", "\n").split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
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
                case "outputDir" -> b.outputDir = val;
                case "user" -> b.user = val;
                case "token" -> b.token = val;
                case "baseUrl" -> b.baseUrl = val;
                case "buildSystem" -> b.buildSystem = val;
                case "kind" -> b.kind = val;
                case "rootDir" -> b.rootDir = val;
                case "dependencyFile" -> b.dependencyFile = val;
                default -> {
                }
            }
        }
        return b;
    }
}
