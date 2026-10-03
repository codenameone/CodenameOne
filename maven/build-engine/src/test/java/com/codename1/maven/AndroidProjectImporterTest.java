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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AndroidProjectImporterTest {

    @Test
    public void readsGroovyBuildScript() {
        AndroidProjectImporter.Result r = new AndroidProjectImporter.Result();
        AndroidProjectImporter.readGradle("android {\n    namespace 'com.example.droid'\n"
                + "    defaultConfig {\n        applicationId \"com.example.droid.app\"\n"
                + "        versionCode 3\n        versionName \"1.2\"\n    }\n}\n"
                + "dependencies {\n    implementation 'androidx.appcompat:appcompat:1.7.0'\n"
                + "    testImplementation 'junit:junit:4.13.2'\n}\n", r);
        assertEquals("com.example.droid", r.namespace);
        assertEquals("com.example.droid.app", r.applicationId);
        assertEquals("3", r.versionCode);
        assertEquals("1.2", r.versionName);
    }

    @Test
    public void readsKotlinBuildScript() {
        AndroidProjectImporter.Result r = new AndroidProjectImporter.Result();
        AndroidProjectImporter.readGradle("android {\n    namespace = \"com.example.kts\"\n"
                + "    defaultConfig {\n        versionCode = 12\n        versionName = \"2.0\"\n    }\n}\n", r);
        assertEquals("com.example.kts", r.namespace);
        assertEquals("12", r.versionCode);
        assertEquals("2.0", r.versionName);
        assertNull(r.applicationId);
    }

    @Test
    public void addsMissingManifestAttributes() {
        String m = "<?xml version=\"1.0\"?>\n<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                + "    <application/>\n</manifest>\n";
        String merged = AndroidProjectImporter.addManifestAttribute(m, "package", "com.example.droid");
        merged = AndroidProjectImporter.addManifestAttribute(merged, "android:versionCode", "3");
        assertTrue(merged, merged.contains("<manifest android:versionCode=\"3\" package=\"com.example.droid\" "
                + "xmlns:android="));
        assertTrue(merged, merged.contains("<application/>"));
    }

    @Test
    public void keepsWhatTheManifestAlreadySays() {
        String m = "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    package=\"com.example.own\" android:versionName=\"9\">\n</manifest>\n";
        assertEquals(m, AndroidProjectImporter.addManifestAttribute(m, "package", "com.example.droid"));
        assertEquals(m, AndroidProjectImporter.addManifestAttribute(m, "android:versionName", "1.2"));
    }

    /// A module's own src/main, built where it stands, takes its package from
    /// the module's build script, as the Android Gradle plugin does.
    @Test
    public void runnerReadsTheModuleNamespace() throws Exception {
        java.io.File module = java.nio.file.Files.createTempDirectory("module").toFile();
        java.io.File main = new java.io.File(module, "src/main");
        assertTrue(main.mkdirs());
        java.nio.file.Files.write(new java.io.File(module, "build.gradle").toPath(),
                "android {\n    namespace 'com.example.where'\n}\n".getBytes("UTF-8"));
        assertEquals("com.example.where", AndroidResourceRunner.gradleNamespace(main));
        assertNull(AndroidResourceRunner.gradleNamespace(new java.io.File(module, "src/main/android")));
    }
}
