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
    private String projectDir;
    private String settings;
    private String pom;
    private String multimoduleRoot;
    private String sourceRoots;
    private String sourceEncoding;

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

    /// The compile source roots Maven RESOLVED, path-separator joined, or null
    /// when the launcher did not say.
    ///
    /// The tool can read a POM but it has no model: it cannot evaluate a profile
    /// activation, follow an inherited `<sourceDirectory>` or expand an
    /// arbitrary property. Where the launcher knows, it says, and the tool's own
    /// reading is the fallback.
    public String sourceRoots() {
        return sourceRoots;
    }

    /// The source encoding Maven resolved, or null when the launcher did not
    /// say.
    public String sourceEncoding() {
        return sourceEncoding;
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
                case "sourceRoots" -> b.sourceRoots = val;
                case "sourceEncoding" -> b.sourceEncoding = val;
                default -> {
                }
            }
        }
        return b;
    }
}
