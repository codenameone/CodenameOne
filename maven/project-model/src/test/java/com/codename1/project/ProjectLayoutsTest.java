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
package com.codename1.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectLayoutsTest {
    @TempDir
    File tmp;

    private static File touch(File base, String path, String content) throws IOException {
        File f = new File(base, path);
        f.getParentFile().mkdirs();
        Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return f;
    }

    private static File touch(File base, String path) throws IOException {
        return touch(base, path, "");
    }

    private static File dir(File base, String path) {
        File f = new File(base, path);
        f.mkdirs();
        return f;
    }

    private File mavenApp() throws IOException {
        File root = new File(tmp, "mavenapp");
        touch(root, "pom.xml");
        touch(root, "common/pom.xml");
        touch(root, "common/codenameone_settings.properties");
        dir(root, "common/src/main/java/com/example");
        touch(root, "android/pom.xml");
        dir(root, "android/src/main/java");
        touch(root, "backend/pom.xml");
        touch(root, "backend/application.properties");
        dir(root, "backend/src/main/java");
        return root;
    }

    private File gradleApp() throws IOException {
        File root = new File(tmp, "gradleapp");
        touch(root, "settings.gradle.kts", "plugins { id(\"com.codenameone\") version \"1.0\" }\n");
        touch(root, "codenameone_settings.properties");
        dir(root, "src/main/java/com/example");
        return root;
    }

    private File antApp() throws IOException {
        File root = new File(tmp, "antapp");
        touch(root, "build.xml");
        touch(root, "codenameone_settings.properties");
        dir(root, "src/com/example");
        dir(root, "nbproject");
        return root;
    }

    @Test
    void mavenAppResolvesFromEveryModule() throws IOException {
        File root = mavenApp();
        for (String start : new String[]{"", "common", "common/src/main/java/com/example", "android/src/main/java"}) {
            ProjectLayout l = ProjectLayouts.detect(new File(root, start));
            assertNotNull(l, start);
            assertEquals(BuildSystem.MAVEN, l.buildSystem(), start);
            assertEquals(ProjectKind.APP, l.kind(), start);
            assertEquals(root.getCanonicalFile(), l.rootDir(), start);
            assertEquals(new File(root, "common").getCanonicalFile(), l.projectDir(), start);
        }
    }

    @Test
    void mavenBackendModuleResolvesToTheBackend() throws IOException {
        File root = mavenApp();
        ProjectLayout l = ProjectLayouts.detect(new File(root, "backend/src/main/java"));
        assertEquals(BuildSystem.MAVEN, l.buildSystem());
        assertEquals(ProjectKind.BACKEND, l.kind());
        assertEquals(new File(root, "backend").getCanonicalFile(), l.projectDir());
        assertEquals(new File(root, "backend/application.properties").getCanonicalFile(), l.settingsFile());
    }

    @Test
    void mavenPathsMatchTheArchetype() throws IOException {
        File root = mavenApp().getCanonicalFile();
        ProjectLayout l = ProjectLayouts.detect(root);
        File common = new File(root, "common");
        assertEquals(new File(common, "codenameone_settings.properties"), l.settingsFile());
        assertEquals(Arrays.asList(new File(common, "src/main/java")), l.sourceRoots());
        assertEquals(new File(common, "src/main/css/theme.css"), l.themeCss());
        assertEquals(new File(common, "target/classes"), l.classesDir());
        assertEquals(new File(root, "android/src/main/java"), l.nativeSourceDir(NativePlatform.ANDROID));
        assertEquals(new File(root, "ios/src/main/objectivec"), l.nativeSourceDir(NativePlatform.IOS));
        assertEquals(new File(root, "cn1libs"), l.legacyCn1libDir());
        assertEquals(new File(common, "pom.xml"), l.dependencyFile());
        assertEquals(new File(root, "backend"), l.backendDir());
        assertEquals(new File(common, "target/css/theme.css.merged"), l.cssMergeFile(l.themeCss()));
    }

    @Test
    void gradleAppIsNotMistakenForAnt() throws IOException {
        File root = gradleApp().getCanonicalFile();
        ProjectLayout l = ProjectLayouts.detect(new File(root, "src/main/java/com/example"));
        assertEquals(BuildSystem.GRADLE, l.buildSystem());
        assertEquals(ProjectKind.APP, l.kind());
        assertEquals(root, l.rootDir());
        assertEquals(root, l.projectDir());
        assertEquals(new File(root, "src/android/java"), l.nativeSourceDir(NativePlatform.ANDROID));
        assertEquals(new File(root, "src/ios/objectivec"), l.nativeSourceDir(NativePlatform.IOS));
        assertEquals(new File(root, "src/win/c"), l.nativeSourceDir(NativePlatform.WIN));
        assertEquals(new File(root, "build/classes/java/main"), l.classesDir());
        assertEquals(new File(root, "build/resources/main"), l.resourcesOutputDir());
        assertEquals(new File(root, "build.gradle.kts"), l.dependencyFile());
        assertNull(l.legacyCn1libDir());
        assertNull(l.backendDir());
        assertTrue(l.existingNativePlatforms().isEmpty());
    }

    @Test
    void gradleNativeDirIsOnlyCreatedOnRequest() throws IOException {
        File root = gradleApp();
        ProjectLayout l = ProjectLayouts.detect(root);
        assertFalse(l.nativeSourceDir(NativePlatform.JAVASE).exists());
        File created = l.ensureNativeSourceDir(NativePlatform.JAVASE);
        assertTrue(created.isDirectory());
        assertEquals(Arrays.asList(NativePlatform.JAVASE), l.existingNativePlatforms());
    }

    @Test
    void gradleBackendSubprojectAndBackendOnlyRoot() throws IOException {
        File root = gradleApp();
        touch(root, "backend/application.properties");
        dir(root, "backend/src/main/java");
        ProjectLayout sub = ProjectLayouts.detect(new File(root, "backend/src/main/java"));
        assertEquals(ProjectKind.BACKEND, sub.kind());
        assertEquals(root.getCanonicalFile(), sub.rootDir());
        assertEquals(":backend:cn1Compile", sub.gradleTaskPath("cn1Compile"));
        assertEquals(new File(root, "backend").getCanonicalFile(), ProjectLayouts.detect(root).backendDir());

        File service = new File(tmp, "service");
        touch(service, "settings.gradle.kts", "plugins { id(\"com.codenameone\") version \"1.0\" }\n");
        touch(service, "application.properties");
        ProjectLayout only = ProjectLayouts.detect(service);
        assertEquals(BuildSystem.GRADLE, only.buildSystem());
        assertEquals(ProjectKind.BACKEND, only.kind());
        assertEquals(only.projectDir(), only.backendDir());
        assertEquals("cn1Compile", only.gradleTaskPath("cn1Compile"));
    }

    @Test
    void gradleSubprojectApplyingThePluginItselfKeepsTheRoot() throws IOException {
        File root = new File(tmp, "multi");
        touch(root, "settings.gradle.kts", "pluginManagement { plugins { id(\"com.codenameone\") } }\ninclude(\"backend\")\n");
        touch(root, "codenameone_settings.properties");
        touch(root, "backend/build.gradle.kts", "plugins { id(\"com.codenameone\") }\n");
        touch(root, "backend/application.properties");
        ProjectLayout l = ProjectLayouts.detect(new File(root, "backend"));
        assertEquals(root.getCanonicalFile(), l.rootDir());
        assertEquals(ProjectKind.BACKEND, l.kind());
    }

    @Test
    void anIncludedAppSubprojectIsItsOwnProjectDirectory() throws IOException {
        File root = new File(tmp, "multiapp");
        touch(root, "settings.gradle.kts", "include(\"app\")\n");
        touch(root, "build.gradle.kts", "plugins { id(\"com.codenameone\") version \"1.0\" apply false }\n");
        touch(root, "app/build.gradle.kts", "plugins { id(\"com.codenameone\") }\n");
        touch(root, "app/codenameone_settings.properties");
        dir(root, "app/src/main/java");
        ProjectLayout l = ProjectLayouts.detect(new File(root, "app/src/main/java"));
        assertEquals(root.getCanonicalFile(), l.rootDir());
        assertEquals(new File(root, "app").getCanonicalFile(), l.projectDir());
        assertEquals(ProjectKind.APP, l.kind());
        assertEquals(new File(root, "app/codenameone_settings.properties").getCanonicalFile(), l.settingsFile());
        assertEquals(":app:cn1Compile", l.gradleTaskPath("cn1Compile"));
    }

    @Test
    void gradleCn1lib() throws IOException {
        File root = new File(tmp, "lib");
        touch(root, "settings.gradle.kts", "plugins { id(\"com.codenameone\") version \"1.0\" }\n");
        touch(root, "codenameone_library_appended.properties");
        ProjectLayout l = ProjectLayouts.detect(root);
        assertEquals(ProjectKind.LIB, l.kind());
        assertEquals(new File(root, "codenameone_library_appended.properties").getCanonicalFile(), l.settingsFile());
    }

    @Test
    void antPaths() throws IOException {
        File root = antApp().getCanonicalFile();
        ProjectLayout l = ProjectLayouts.detect(new File(root, "src/com/example"));
        assertEquals(BuildSystem.ANT, l.buildSystem());
        assertEquals(Arrays.asList(new File(root, "src")), l.sourceRoots());
        assertEquals(new File(root, "src"), l.resourcesDir());
        assertEquals(new File(root, "css/theme.css"), l.themeCss());
        assertEquals(new File(root, "native/javase"), l.nativeSourceDir(NativePlatform.JAVASE));
        assertEquals(new File(root, "lib/impl/css"), l.libraryCssDir());
        assertEquals(new File(root, "css/theme.css.merged"), l.cssMergeFile(l.themeCss()));
        assertEquals(new File(root, "lib"), l.legacyCn1libDir());
    }

    @Test
    void mavenCn1lib() throws IOException {
        File root = new File(tmp, "mavenlib");
        touch(root, "pom.xml");
        touch(root, "common/pom.xml");
        touch(root, "common/codenameone_library_appended.properties");
        ProjectLayout l = ProjectLayouts.detect(new File(root, "common"));
        assertEquals(BuildSystem.MAVEN, l.buildSystem());
        assertEquals(ProjectKind.LIB, l.kind());
    }

    @Test
    void outsideAnyProject() {
        assertNull(ProjectLayouts.detect(dir(tmp, "nothing/here")));
        assertNull(ProjectLayouts.detect(null));
    }

    @Test
    void compileCommandsPerBuildTool() throws IOException {
        File g = gradleApp();
        touch(g, "gradlew");
        List<String> gradle = ProjectLayouts.detect(g).compileCommand(null, false);
        assertTrue(gradle.get(0).endsWith("gradlew"), gradle.toString());
        assertEquals("cn1Compile", gradle.get(gradle.size() - 1));
        assertEquals(g.getCanonicalFile(), ProjectLayouts.detect(g).compileWorkingDir());

        File m = mavenApp();
        List<String> maven = ProjectLayouts.detect(m).compileCommand(null, false);
        assertEquals("mvn", maven.get(0));
        assertEquals("compile", maven.get(1));
        assertEquals(new File(m, "common").getCanonicalFile(), ProjectLayouts.detect(m).compileWorkingDir());

        assertEquals(Arrays.asList("ant", "compile"), ProjectLayouts.detect(antApp()).compileCommand(null, false));
    }

    @Test
    void descriptorRoundTripAndOverride() throws IOException {
        File root = gradleApp();
        ProjectLayout l = ProjectLayouts.detect(root);
        File generated = dir(root, "build/generated/sources/cn1");
        ProjectLayout resolved = l.withSourceRoots(Arrays.asList(l.javaSourceDir(), generated.getCanonicalFile()));
        ProjectDescriptor d = ProjectDescriptor.fromLayout(resolved).set("mainName", "MyApp");
        d.write(l.descriptorFile());

        ProjectDescriptor back = ProjectDescriptor.read(l.descriptorFile());
        assertEquals("MyApp", back.get("mainName"));
        assertEquals("GRADLE", back.get("buildSystem"));
        assertNull(back.get("pom"));
        assertEquals(resolved, back.toLayout());
        assertEquals(resolved.sourceRoots(), ProjectLayouts.detectWithDescriptor(root).sourceRoots());
    }

    @Test
    void descriptorKeepsWindowsPathsAndMavenKeys() throws IOException {
        ProjectDescriptor d = ProjectDescriptor.parse("# c\r\nsettings=C:\\work\\app\\common\\x.properties\r\nsourceRoot=a\r\nsourceRoot=b\r\nbogus\r\n");
        assertEquals("C:\\work\\app\\common\\x.properties", d.get("settings"));
        assertEquals(Arrays.asList("a", "b"), d.getAll("sourceRoot"));
        assertNull(d.toLayout());

        ProjectDescriptor maven = ProjectDescriptor.fromLayout(ProjectLayouts.detect(mavenApp()));
        assertNotNull(maven.get("pom"));
        assertEquals(maven.get("pom"), maven.get("dependencyFile"));
        assertNotNull(maven.get("multimoduleRoot"));
    }
}
