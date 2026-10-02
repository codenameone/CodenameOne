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
package com.codename1.designer.css;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Where CN1CSSCLI puts the library CSS, the merged stylesheet and its
/// checksums for each build tool. The Ant and Maven answers are the ones the
/// CLI gave before it resolved projects through codenameone-project-model.
class CN1CSSCLIProjectLayoutTest {

    private static Object invoke(String name, Class<?>[] types, Object... args) throws Exception {
        Method m = CN1CSSCLI.class.getDeclaredMethod(name, types);
        m.setAccessible(true);
        return m.invoke(null, args);
    }

    private static File libCssDir(File input) throws Exception {
        return (File) invoke("getLibCSSDirectory", new Class<?>[]{File.class}, input);
    }

    private static String mergedFile(File input) throws Exception {
        return (String) invoke("getMergedFile", new Class<?>[]{String.class}, input.getPath());
    }

    private static File checksums(File baseDir) throws Exception {
        return (File) invoke("getChecksumsFile", new Class<?>[]{File.class}, baseDir);
    }

    private static String relativePath(File file, File relativeTo) throws Exception {
        return (String) invoke("getRelativePath", new Class<?>[]{File.class, File.class}, file, relativeTo);
    }

    private static File write(File f, String content) throws Exception {
        f.getParentFile().mkdirs();
        Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return f;
    }

    @Test
    void antKeepsEverythingBesideTheSources(@TempDir Path tmp) throws Exception {
        File root = tmp.toFile();
        write(new File(root, "codenameone_settings.properties"), "codename1.cssTheme=true\n");
        File theme = write(new File(root, "css/theme.css"), "");

        assertEquals(new File(root, "lib/impl/css"), libCssDir(theme));
        assertEquals(theme.getPath() + ".merged", mergedFile(theme));
        assertEquals(new File(root, ".cn1_css_checksums"), checksums(theme.getParentFile().getParentFile()));
    }

    @Test
    void mavenWritesUnderCommonTarget(@TempDir Path tmp) throws Exception {
        File root = tmp.toFile();
        write(new File(root, "pom.xml"), "<project/>");
        File common = new File(root, "common");
        write(new File(common, "pom.xml"), "<project/>");
        write(new File(common, "codenameone_settings.properties"), "codename1.cssTheme=true\n");
        File theme = write(new File(common, "src/main/css/theme.css"), "");

        File lib = new File(common, "target/css");
        assertEquals(lib, libCssDir(theme));
        File merged = new File(lib, "theme.css.merged");
        assertEquals(merged.getAbsolutePath(), mergedFile(theme));
        assertEquals(new File(common, "target/.cn1_css_checksums"),
                checksums(theme.getParentFile().getParentFile()));
        // The temp dir is a symlinked path on macOS (/var -> /private/var); the
        // project dir must stay in the caller's spelling or this throws.
        assertEquals("../../src/main/css", relativePath(theme.getParentFile(), merged));
    }

    @Test
    void gradleWritesUnderBuild(@TempDir Path tmp) throws Exception {
        File root = tmp.toFile();
        write(new File(root, "settings.gradle.kts"), "rootProject.name = \"app\"\n");
        write(new File(root, "build.gradle.kts"), "plugins { id(\"com.codenameone\") }\n");
        write(new File(root, "codenameone_settings.properties"), "codename1.cssTheme=true\n");
        File theme = write(new File(root, "src/main/css/theme.css"), "");

        File lib = new File(root, "build/css");
        assertEquals(lib, libCssDir(theme));
        assertEquals(new File(lib, "theme.css.merged").getAbsolutePath(), mergedFile(theme));
        assertEquals(new File(root, "build/.cn1_css_checksums"),
                checksums(theme.getParentFile().getParentFile()));
    }

    @Test
    void libCssDirOverrideStillMovesTheMergedFile(@TempDir Path tmp) throws Exception {
        File root = tmp.toFile();
        write(new File(root, "settings.gradle.kts"), "plugins { id(\"com.codenameone\") }\n");
        write(new File(root, "codenameone_settings.properties"), "");
        File theme = write(new File(root, "src/main/css/theme.css"), "");
        File override = new File(root, "elsewhere");
        String previous = System.getProperty("cn1.libCSSDir");
        try {
            System.setProperty("cn1.libCSSDir", override.getPath());
            assertEquals(override, libCssDir(theme));
            assertEquals(new File(override, "theme.css.merged").getAbsolutePath(), mergedFile(theme));
        } finally {
            if (previous == null) {
                System.clearProperty("cn1.libCSSDir");
            } else {
                System.setProperty("cn1.libCSSDir", previous);
            }
        }
    }
}
