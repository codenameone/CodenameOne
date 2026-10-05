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
import static org.junit.Assert.assertFalse;
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
    public void coveredDependenciesMatchTheWholeCoordinate() {
        AndroidProjectImporter.Result r = new AndroidProjectImporter.Result();
        AndroidProjectImporter.readGradle("dependencies {\n"
                + "    implementation 'androidx.core:core:1.13.1'\n"
                + "    implementation 'androidx.core:core-splashscreen:1.0.1'\n"
                + "    implementation(\"androidx.appcompat:appcompat-resources:1.7.0\")\n}\n", r);
        assertEquals(1, r.covered.size());
        assertTrue(r.covered.toString(), r.covered.get(0).startsWith("androidx.core:core ("));
        assertTrue(r.uncovered.toString(), r.uncovered.contains("androidx.core:core-splashscreen"));
        assertTrue(r.uncovered.toString(), r.uncovered.contains("androidx.appcompat:appcompat-resources"));
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

    /// The application writes its own main class after a build generated one:
    /// the generated copy in the persistent generated-sources tree must go,
    /// even on a run the unchanged Android inputs would otherwise skip, or
    /// javac sees two definitions until a clean build.
    @Test
    public void ownMainClassRetiresTheGeneratedOne() throws Exception {
        java.io.File root = java.nio.file.Files.createTempDirectory("mainclass").toFile();
        java.io.File android = new java.io.File(root, "src/main/android");
        assertTrue(android.mkdirs());
        java.nio.file.Files.write(new java.io.File(android, "AndroidManifest.xml").toPath(),
                ("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" package=\"com.example.app\">"
                        + "<application/></manifest>").getBytes("UTF-8"));
        java.io.File jar = new java.io.File(root, "compat.jar");
        java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(jar));
        zip.putNextEntry(new java.util.zip.ZipEntry(com.codename1.android.rescompiler.ResourceCompiler.FRAMEWORK_SYMBOLS_RESOURCE));
        zip.closeEntry();
        zip.close();
        java.io.File javaSrc = new java.io.File(root, "src/main/java");
        java.io.File javaOut = new java.io.File(root, "target/generated-sources/android");
        java.util.List<java.io.File> roots = java.util.Arrays.asList(javaSrc, javaOut);
        AndroidResourceRunner runner = new AndroidResourceRunner(android, javaOut, new java.io.File(root, "target/classes"),
                new java.io.File(root, "target"), jar, null, "com.example", "MyApp", roots,
                new com.codename1.build.SystemStreamLog());
        assertTrue(runner.run());
        java.io.File generated = new java.io.File(javaOut, "com/example/MyApp.java");
        assertTrue("the first build generates the entry point", generated.isFile());

        java.io.File own = new java.io.File(javaSrc, "com/example/MyApp.java");
        assertTrue(own.getParentFile().mkdirs());
        java.nio.file.Files.write(own.toPath(), "package com.example; public class MyApp {}".getBytes("UTF-8"));
        assertTrue(runner.run());
        assertFalse("the generated entry point stayed beside the application's own", generated.isFile());
    }
}
