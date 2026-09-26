/*
 * Copyright (c) 2021, 2026, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.ant.SortedProperties;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CN1BuildMojoTest {

    @Test
    public void mergeRequiredPropertiesAllowsLowerJavaVersionLibrary() throws Exception {
        CN1BuildMojo mojo = new CN1BuildMojo();
        Method method = CN1BuildMojo.class.getDeclaredMethod("mergeRequiredProperties", String.class, Properties.class, Properties.class);
        method.setAccessible(true);

        SortedProperties projectProps = new SortedProperties();
        projectProps.setProperty("codename1.arg.java.version", "17");

        SortedProperties libProps = new SortedProperties();
        libProps.setProperty("codename1.arg.java.version", "8");

        SortedProperties merged = (SortedProperties) method.invoke(mojo, "test-lib", libProps, projectProps);
        assertEquals("17", merged.getProperty("codename1.arg.java.version"));
    }

    @Test
    public void mergeRequiredPropertiesStillFailsOnOtherConflicts() throws Exception {
        CN1BuildMojo mojo = new CN1BuildMojo();
        Method method = CN1BuildMojo.class.getDeclaredMethod("mergeRequiredProperties", String.class, Properties.class, Properties.class);
        method.setAccessible(true);

        SortedProperties projectProps = new SortedProperties();
        projectProps.setProperty("codename1.arg.java.version", "17");
        projectProps.setProperty("codename1.arg.test", "project");

        SortedProperties libProps = new SortedProperties();
        libProps.setProperty("codename1.arg.java.version", "8");
        libProps.setProperty("codename1.arg.test", "lib");

        try {
            method.invoke(mojo, "test-lib", libProps, projectProps);
            fail("Expected a property conflict exception");
        } catch (InvocationTargetException ex) {
            assertTrue(ex.getCause().getMessage().contains("Property codename1.arg.test has a conflict"));
        }
    }

    @Test
    public void stripsTheFrameworkTheServerReSupplies() {
        assertTrue(CN1BuildMojo.isStrippedFromStagedJar("com.codenameone", "codenameone-core", "provided", "ios-device"));
        assertTrue(CN1BuildMojo.isStrippedFromStagedJar("com.codenameone", "codenameone-core", "provided", "ios-source"));
        assertTrue(CN1BuildMojo.isStrippedFromStagedJar("com.codenameone", "java-runtime", "provided", "android-device"));
    }

    @Test
    public void keepsTheFrameworkForLocalJavascriptBuilds() {
        // ParparVM translates locally for these, so it needs every class in the jar.
        assertFalse(CN1BuildMojo.isStrippedFromStagedJar("com.codenameone", "codenameone-core", "provided", "local-javascript"));
        assertFalse(CN1BuildMojo.isStrippedFromStagedJar("com.codenameone", "java-runtime", "provided", "local-javascript"));
    }

    @Test
    public void stripsKotlinStdlibOnlyForServerBuilds() {
        assertTrue(CN1BuildMojo.isStrippedFromStagedJar("org.jetbrains.kotlin", "kotlin-stdlib", "compile", "ios-device"));
        // ios-source generates the Xcode project on this machine, so it is a local
        // target and bundling kotlin-stdlib is simpler than having it re-supplied.
        assertFalse(CN1BuildMojo.isStrippedFromStagedJar("org.jetbrains.kotlin", "kotlin-stdlib", "compile", "ios-source"));
    }

    @Test
    public void keepsTheApplicationsOwnCompileDependencies() {
        assertFalse(CN1BuildMojo.isStrippedFromStagedJar("com.mycompany", "myproject-common", "compile", "ios-source"));
        assertFalse(CN1BuildMojo.isStrippedFromStagedJar("com.codenameone", "cn1-admob-lib", "compile", "ios-device"));
        assertTrue(CN1BuildMojo.isStrippedFromStagedJar("org.junit.jupiter", "junit-jupiter", "test", "ios-device"));
    }

    @Test
    public void keepsArtifactsWithoutAScope() {
        // A null scope is not a statement that the artifact is outside the application.
        assertFalse(CN1BuildMojo.isStrippedFromStagedJar("com.mycompany", "some-lib", null, "ios-device"));
    }

    @Test
    public void neverStagesTheDesktopRuntimeBinaries() {
        // The aggregator itself, whatever scope an older generated pom gave it.
        assertTrue(CN1BuildMojo.isDesktopRuntimeBinary("com.codenameone", "cn1-binaries-javase", null));
        // What it pulls in: ffmpeg and its per-platform natives, at compile scope.
        List<String> viaAggregator = Arrays.asList(
                "com.example:myapp-javase:jar:1.0-SNAPSHOT",
                "com.codenameone:cn1-binaries-javase:pom:8.0-SNAPSHOT",
                "org.bytedeco:ffmpeg-platform:jar:7.1-1.5.11",
                "org.bytedeco:ffmpeg:jar:windows-x86_64:7.1-1.5.11");
        assertTrue(CN1BuildMojo.isDesktopRuntimeBinary("org.bytedeco", "ffmpeg", viaAggregator));
    }

    @Test
    public void keepsAnOrgBytedecoDependencyTheAppDeclaresItself() {
        List<String> direct = Arrays.asList(
                "com.example:myapp-javase:jar:1.0-SNAPSHOT",
                "org.bytedeco:javacv:jar:1.5.11");
        assertFalse(CN1BuildMojo.isDesktopRuntimeBinary("org.bytedeco", "javacv", direct));
        assertFalse(CN1BuildMojo.isDesktopRuntimeBinary("org.bytedeco", "javacv", null));
        // Another com.codenameone artifact whose name merely starts the same way.
        assertFalse(CN1BuildMojo.isDesktopRuntimeBinary("com.codenameone", "cn1-binaries-javase-extra",
                Arrays.asList("com.codenameone:cn1-binaries-javase-extra:jar:1.0")));
    }

    @Test
    public void keepsADesktopRuntimeArtifactSomethingElseNeeds() {
        // Maven kept the aggregator's trail for ffmpeg, but an application library needs
        // the same artifact through a longer path: it must stay in the upload.
        String ffmpeg = CN1BuildMojo.dependencyKey("org.bytedeco", "ffmpeg", "windows-x86_64");
        Set<String> needed = new HashSet<String>(Arrays.asList(ffmpeg));
        assertFalse(CN1BuildMojo.isStrippedAsDesktopRuntime(ffmpeg, needed));
        // Needed by nothing else: stripped.
        String linux = CN1BuildMojo.dependencyKey("org.bytedeco", "ffmpeg", "linux-x86_64");
        assertTrue(CN1BuildMojo.isStrippedAsDesktopRuntime(linux, needed));
        // Classifier is part of the identity.
        assertTrue(CN1BuildMojo.isStrippedAsDesktopRuntime(
                CN1BuildMojo.dependencyKey("org.bytedeco", "ffmpeg", null), needed));
        // Unknown: keep everything rather than risk shipping without needed classes.
        assertFalse(CN1BuildMojo.isStrippedAsDesktopRuntime(linux, null));
    }

    @Test
    public void excludesTheDesktopRuntimeBelowEveryCollectedDependency() {
        org.eclipse.aether.graph.Exclusion own = new org.eclipse.aether.graph.Exclusion("org.slf4j", "slf4j-api", "*", "*");
        org.eclipse.aether.graph.Dependency library = new org.eclipse.aether.graph.Dependency(
                new org.eclipse.aether.artifact.DefaultArtifact("com.example:some-cn1lib:1.0"), "compile",
                false, Arrays.asList(own));
        org.eclipse.aether.graph.Dependency result = CN1BuildMojo.withoutDesktopRuntime(library);
        boolean aggregatorExcluded = false;
        for (org.eclipse.aether.graph.Exclusion e : result.getExclusions()) {
            if ("com.codenameone".equals(e.getGroupId()) && "cn1-binaries-javase".equals(e.getArtifactId())
                    && "*".equals(e.getClassifier()) && "*".equals(e.getExtension())) {
                aggregatorExcluded = true;
            }
        }
        assertTrue(aggregatorExcluded);
        // The library's own exclusions survive.
        assertTrue(result.getExclusions().contains(own));
    }

    @Test
    public void reusesAStagedJarOnlyWhenItsInputsMatch() {
        long start = 1000000L;
        assertTrue(CN1BuildMojo.mayReuseStagedJar("/a.jar\n/b.jar", "/a.jar\n/b.jar\n", 5, start));
        assertFalse(CN1BuildMojo.mayReuseStagedJar("/a.jar\n/ffmpeg.jar", "/a.jar\n", start + 5, start));
    }

    @Test
    public void honoursAnUnrecordedJarOnlyWhenThisRunProducedIt() {
        long start = 1000000L;
        // The project's own pom wrote it during this run: a deliberate override.
        assertTrue(CN1BuildMojo.mayReuseStagedJar(null, "/a.jar\n", start + 1, start));
        // Left over from before this run, e.g. by a failed build on an older plugin.
        assertFalse(CN1BuildMojo.mayReuseStagedJar(null, "/a.jar\n", start - 1, start));
    }

    @Test
    public void discardsAStaleStagedJar() throws Exception {
        java.io.File dir = java.nio.file.Files.createTempDirectory("staged").toFile();
        java.io.File jar = new java.io.File(dir, "app-mac-os-x-desktop-jar-with-dependencies.jar");
        assertTrue(jar.createNewFile());
        CN1BuildMojo.discardStagedJar(jar);
        assertFalse(jar.exists());
        assertTrue(dir.delete());
    }

    @Test
    public void failsWhenAStaleStagedJarCannotBeDeleted() throws Exception {
        java.io.File dir = java.nio.file.Files.createTempDirectory("staged").toFile();
        java.io.File jar = new java.io.File(dir, "app-mac-os-x-desktop-jar-with-dependencies.jar");
        assertTrue(jar.createNewFile());
        // A read-only directory stands in for a jar held open on Windows.
        assertTrue(dir.setWritable(false));
        try {
            org.junit.Assume.assumeFalse("a privileged user can delete from a read-only directory",
                    jar.delete());
            try {
                CN1BuildMojo.discardStagedJar(jar);
                fail("a stale jar that could not be deleted must stop the build");
            } catch (org.apache.maven.plugin.MojoExecutionException expected) {
                assertTrue(expected.getMessage().contains("would be uploaded as it is"));
            }
            assertTrue(jar.exists());
        } finally {
            dir.setWritable(true);
            jar.delete();
            dir.delete();
        }
    }

    @Test
    public void toleratesAnAbsentBuildTarget() {
        assertTrue(CN1BuildMojo.isSuppliedByBuildServer("com.codenameone", "codenameone-core", null));
        assertFalse(CN1BuildMojo.isSuppliedByBuildServer("com.thirdparty", "some-api", null));
        assertFalse(CN1BuildMojo.isLocalJavascriptBuild(null));
        assertTrue(CN1BuildMojo.isStrippedFromStagedJar("com.codenameone", "codenameone-core", "provided", null));
    }

    @Test
    public void onlyTheFrameworkCountsAsSuppliedByTheBuildServer() {
        assertTrue(CN1BuildMojo.isSuppliedByBuildServer("com.codenameone", "codenameone-core", "ios-device"));
        assertTrue(CN1BuildMojo.isSuppliedByBuildServer("com.codenameone", "java-runtime", "ios-source"));
        assertTrue(CN1BuildMojo.isSuppliedByBuildServer("org.jetbrains.kotlin", "kotlin-stdlib", "ios-device"));
        assertFalse(CN1BuildMojo.isSuppliedByBuildServer("com.codenameone", "codenameone-core", "local-javascript"));
        // A `provided` scope third party dependency is stripped from the jar but nobody
        // puts it back, so a reference into it stays reportable.
        assertTrue(CN1BuildMojo.isStrippedFromStagedJar("com.thirdparty", "some-api", "provided", "ios-device"));
        assertFalse(CN1BuildMojo.isSuppliedByBuildServer("com.thirdparty", "some-api", "ios-device"));
    }
}
