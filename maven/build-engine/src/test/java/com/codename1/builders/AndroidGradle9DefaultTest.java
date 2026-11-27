/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided by
 * Oracle in the LICENSE file that accompanied this code.
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
package com.codename1.builders;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Gradle 9 as the default: which builds take it and which keep Gradle 8. The cloud
/// builder's AndroidGradle9Test holds the same cases against its copy of the method.
class AndroidGradle9DefaultTest {

    /// Gradle 9 is the default, but only where it is known to work: anything an explicit
    /// android.gradleVersion=9 would refuse, and anything that would fail inside Gradle on
    /// AGP 9, keeps Gradle 8 instead. Moving the default turns no working build into a
    /// failing one.
    @Test
    void theGradle9DefaultIsAnAllowList() {
        assertNull(block("", "", ""), "an ordinary modern build takes Gradle 9");
        assertNull(block("classpath 'com.google.gms:google-services:4.5.0'",
                        "apply plugin: 'com.google.gms.google-services'", ""), "google-services at a version AGP 9 applies is fine");
        assertNull(block("", "", "<uses-sdk android:maxSdkVersion=\"34\"/>"), "a literal maxSdkVersion is moved by the build itself");
        assertNull(block("", "android { lint { abortOnError false } }", ""), "xgradle that applies no plugin is fine");

        assertTrue(AndroidGradleBuilder.gradle9DefaultBlocker(false, false, "", "", "", false)
                .contains("android.useGradle8=false"));
        assertTrue(AndroidGradleBuilder.gradle9DefaultBlocker(true, true, "", "", "", false)
                .contains("android.forceJava8Builder"));
        assertTrue(AndroidGradleBuilder.gradle9DefaultBlocker(true, false, "", "", "", true)
                .contains("Huawei"));
        assertTrue(block("", "apply plugin: 'kotlin-android'", "").contains("kotlin-android"));
        assertTrue(block("classpath \"io.fabric.tools:gradle:1.31.2\"", "", "")
                .contains("io.fabric.tools:gradle"));
        assertTrue(block("", "apply plugin: 'com.google.firebase.crashlytics'", "")
                .contains("com.google.firebase.crashlytics"));
        assertTrue(block("", "plugins { id 'com.example.thing' }", "")
                .contains("com.example.thing"));

        // What the parser cannot read keeps the build on Gradle 8.
        assertTrue(block("classpath group: 'io.fabric.tools', name: 'gradle', version: '1.31.2'", "", "")
                .contains("classpath group"));
        assertTrue(block("", "apply plugin: SomePluginClass", "").contains("SomePluginClass"));
        assertTrue(block("", "apply(plugin = \"com.example.kts\")", "").contains("com.example.kts"));
        assertTrue(block("", "apply plugin: 'com.google.gms.google-services'; apply plugin: 'other.plugin'", "")
                .contains("other.plugin"));
        assertTrue(block("classpath 'com.google.gms:google-services:4.5.0'; classpath 'a.b:c:1'", "", "")
                .contains("a.b:c"));
        assertTrue(block("", "apply from: 'extra.gradle'", "").contains("extra.gradle"));
        assertTrue(block("", "project.pluginManager.apply('x')", "").contains("pluginManager"));
        // android.gradlePlugin reaches the generated build.gradle too; the caller joins it to
        // android.xgradle before asking.
        assertTrue(block("", "\n" + "apply plugin: 'kotlin-android'", "").contains("kotlin-android"));
        assertTrue(block("", "android { }\napply plugin: 'com.google.firebase.crashlytics'", "")
                .contains("crashlytics"));
        assertNull(block("", "android { defaultConfig { resValue \"string\", \"id\", \"x\" } }", ""), "a resValue named id is not a plugin");
        String gms = "classpath 'com.google.gms:google-services:4.3.15'";
        assertEquals(AndroidGradleBuilder.agp9GoogleServicesRefusal(gms), block(gms, "", ""));
        String placeholder = "<uses-sdk android:maxSdkVersion=\"${maxSdk}\"/>";
        assertEquals(AndroidGradleBuilder.agp9MaxSdkRefusal(placeholder), block("", "", placeholder));
    }

    @Test
    void huaweiPushIsSeenBeforeTheSourcesAreUnpacked() throws IOException {
        assertTrue(AndroidGradleBuilder.huaweiPushPossible("hms", null));
        assertTrue(AndroidGradleBuilder.huaweiPushPossible("HUAWEI", null));
        assertFalse(AndroidGradleBuilder.huaweiPushPossible("auto", null));
        File dir = Files.createTempDirectory("g9zip").toFile();
        File with = new File(dir, "with.zip");
        File without = new File(dir, "without.zip");
        zip(with, "native/android/agconnect-services.json");
        zip(without, "native/android/google-services.json");
        assertTrue(AndroidGradleBuilder.huaweiPushPossible("auto", with));
        assertFalse(AndroidGradleBuilder.huaweiPushPossible("auto", without));
    }

    private static String block(String top, String xgradle, String xmanifest) {
        return AndroidGradleBuilder.gradle9DefaultBlocker(true, false, top, xgradle, xmanifest, false);
    }

    private static void zip(File target, String entry) throws IOException {
        java.util.zip.ZipOutputStream out = new java.util.zip.ZipOutputStream(
                new java.io.FileOutputStream(target));
        try {
            out.putNextEntry(new java.util.zip.ZipEntry(entry));
            out.write("{}".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        } finally {
            out.close();
        }
    }
}
