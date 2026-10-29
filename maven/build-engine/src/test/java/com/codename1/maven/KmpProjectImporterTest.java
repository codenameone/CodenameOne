/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KmpProjectImporterTest {
    @TempDir
    Path tmp;

    private static void write(File f, String text) throws IOException {
        f.getParentFile().mkdirs();
        Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
    }

    private static String read(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    @Test
    void importsSharedSourcesAndLeavesOutTheDesktopWindow() throws Exception {
        File source = tmp.resolve("kmp").toFile();
        File module = new File(source, "composeApp");
        write(new File(module, "src/commonMain/kotlin/demo/App.kt"),
                "package demo\n\n@Composable\nfun App() {\n}\n");
        write(new File(module, "src/desktopMain/kotlin/demo/Main.kt"),
                "package demo\n\nimport androidx.compose.ui.window.application\n\nfun main() = application { }\n");
        write(new File(module, "src/desktopMain/kotlin/demo/Platform.kt"), "package demo\n\nval name = \"jvm\"\n");
        write(new File(module, "src/iosMain/kotlin/demo/Ios.kt"), "package demo\n");
        File common = tmp.resolve("app/common").toFile();
        common.mkdirs();

        KmpProjectImporter.Result r = new KmpProjectImporter().importProject(source, null, common, null,
                "com.acme", "Main");

        assertEquals(module, r.getModule());
        assertEquals("demo.App", r.getEntry());
        File kmp = KmpProjectImporter.kmpDir(common);
        assertTrue(new File(kmp, "commonMain/kotlin/demo/App.kt").isFile());
        assertTrue(new File(kmp, "jvmMain/kotlin/demo/Platform.kt").isFile());
        assertFalse(new File(kmp, "jvmMain/kotlin/demo/Main.kt").exists(), "the desktop window was copied");
        assertEquals(1, r.getSkippedEntryPoints().size());
        assertTrue(r.getSkippedSourceSets().contains("iosMain"));
        assertTrue(read(r.getMainClass()).contains("extends Lifecycle"));
    }

    @Test
    void conversionAppliesTheComposeCompilerAtTheKotlinVersion() throws Exception {
        File script = tmp.resolve("build.gradle.kts").toFile();
        write(script, "// header\n\ndependencies {\n}\n");
        GradleConversion.addKotlinPlugin(script, true);
        GradleConversion.addComposePlugin(script);
        GradleConversion.addComposePlugin(script);
        String text = read(script);
        assertTrue(text.contains("plugins {\n    kotlin(\"jvm\") version \"" + GradleConversion.KOTLIN_VERSION
                + "\"\n    id(\"org.jetbrains.kotlin.plugin.compose\") version \""
                + GradleConversion.KOTLIN_VERSION + "\"\n}"), text);
        assertEquals(text.indexOf("plugin.compose"), text.lastIndexOf("plugin.compose"), "added twice");
    }
}
