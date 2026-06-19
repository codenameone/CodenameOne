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

    public String projectDir() {
        return projectDir;
    }

    public String packageName() {
        return packageName;
    }

    public String gamesDir() {
        return gamesDir;
    }

    public String sourceDir() {
        return sourceDir;
    }

    public String output() {
        return output;
    }

    public boolean isValid() {
        return gamesDir != null && !gamesDir.isEmpty();
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
                default -> {
                    // ignore unknown keys for forward-compatibility
                }
            }
        }
        return b;
    }
}
