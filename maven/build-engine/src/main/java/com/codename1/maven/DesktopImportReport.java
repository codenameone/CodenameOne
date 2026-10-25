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

/// What an import tells the developer about the project it imported.
public final class DesktopImportReport {

    /// The newest Java release whose class files a Codename One build reads.
    public static final int HIGHEST_JAVA = 17;

    private DesktopImportReport() {
    }

    /// What to tell the developer about a project that declares Java
    /// `level`; null when the build takes that level as it is, or the project
    /// does not say (0).
    public static String javaLevelWarning(int level) {
        if (level <= HIGHEST_JAVA) {
            return null;
        }
        return "The project is built for Java " + level + "; a Codename One application is compiled as Java "
                + HIGHEST_JAVA + ". The imported sources are compiled with -source/-target " + HIGHEST_JAVA
                + ", so a language feature newer than that is a compile error to rewrite, and API added to the JDK "
                + "after " + HIGHEST_JAVA + " is reported by the compliance check like any other a device lacks.";
    }
}
