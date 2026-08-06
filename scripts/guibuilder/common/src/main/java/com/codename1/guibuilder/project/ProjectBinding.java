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

    public String projectDir() { return projectDir; }
    public String guiDir() { return guiDir; }
    public String sourceDir() { return sourceDir; }
    public String cssFile() { return cssFile; }
    public String initialForm() { return initialForm; }
    public boolean isValid() { return projectDir != null && guiDir != null; }

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
                case "projectDir" -> binding.projectDir = field;
                case "guiDir" -> binding.guiDir = field;
                case "sourceDir" -> binding.sourceDir = field;
                case "cssFile" -> binding.cssFile = field;
                case "initialForm" -> binding.initialForm = field;
                default -> { }
            }
        }
        return binding;
    }
}
