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
package com.codename1.impl.javase.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression coverage for {@link MavenUtils#findDesignerJarInM2(File)} and
 * {@link MavenUtils#isRunningInMaven()}.
 *
 * <p>Published {@code codenameone-designer-<v>-jar-with-dependencies.jar} artifacts
 * are not directly runnable: {@code maven/designer/pom.xml} renames the shaded
 * output to {@code designer_1.jar} and re-zips it, so the artifact in m2 is a
 * plain zip containing a single inner jar (no top-level {@code Main-Class}
 * manifest). Handing this wrapper zip to {@code java -jar} fails with "no main
 * manifest attribute", so the Component Inspector's Edit Style action, the one
 * thing that still launches the Resource Editor from the simulator, has to be
 * given the inner jar.</p>
 */
class MavenUtilsTest {
    /** Everything {@link MavenUtils#isRunningInMaven()} reads. */
    private static final String[] MAVEN_SIGNALS = {
        "user.dir", "cn1.library.path", "maven.home", "codename1.designer.jar"
    };

    /**
     * Runs {@code isRunningInMaven()} for a simulator started in {@code cwd}
     * with none of the launch properties set, then puts everything back.
     */
    private static boolean runningInMavenFrom(File cwd, String designerJarProperty) {
        String[] saved = new String[MAVEN_SIGNALS.length];
        for (int i = 0; i < MAVEN_SIGNALS.length; i++) {
            saved[i] = System.getProperty(MAVEN_SIGNALS[i]);
            System.clearProperty(MAVEN_SIGNALS[i]);
        }
        try {
            System.setProperty("user.dir", cwd.getAbsolutePath());
            if (designerJarProperty != null) {
                System.setProperty("codename1.designer.jar", designerJarProperty);
            }
            MavenUtils.reset();
            return MavenUtils.isRunningInMaven();
        } finally {
            for (int i = 0; i < MAVEN_SIGNALS.length; i++) {
                if (saved[i] == null) {
                    System.clearProperty(MAVEN_SIGNALS[i]);
                } else {
                    System.setProperty(MAVEN_SIGNALS[i], saved[i]);
                }
            }
            MavenUtils.reset();
        }
    }

    private static void writeText(File file, String content) throws Exception {
        Files.createDirectories(file.getParentFile().toPath());
        Files.write(file.toPath(), content.getBytes("UTF-8"));
    }

    @Test
    void mavenProjectLayoutIsRunningInMavenWithoutAnyLaunchProperty(@TempDir Path tempDir) throws Exception {
        // The generated project: exec:exec forks the simulator in javase/, and
        // no longer forwards -Dcodename1.designer.jar.
        File root = tempDir.toFile();
        writeText(new File(root, "pom.xml"), "<project/>");
        writeText(new File(root, "common/pom.xml"), "<project/>");
        writeText(new File(root, "common/codenameone_settings.properties"), "codename1.mainName=App\n");
        writeText(new File(root, "javase/pom.xml"), "<project/>");

        assertTrue(runningInMavenFrom(new File(root, "javase"), null), "from the javase module");
        assertTrue(runningInMavenFrom(new File(root, "common"), null), "from the common module");
        assertTrue(runningInMavenFrom(root, null), "from the project root");
    }

    @Test
    void gradleAndAntProjectsAreNotRunningInMaven(@TempDir Path tempDir) throws Exception {
        File gradle = tempDir.resolve("gradle-app").toFile();
        writeText(new File(gradle, "settings.gradle.kts"), "rootProject.name = \"app\"\n");
        writeText(new File(gradle, "build.gradle.kts"), "plugins { id(\"com.codenameone\") }\n");
        writeText(new File(gradle, "codenameone_settings.properties"), "codename1.mainName=App\n");
        assertFalse(runningInMavenFrom(gradle, null), "a Gradle project has its own launch path");

        File ant = tempDir.resolve("ant-app").toFile();
        writeText(new File(ant, "build.xml"), "<project/>");
        writeText(new File(ant, "codenameone_settings.properties"), "codename1.mainName=App\n");
        assertFalse(runningInMavenFrom(ant, null), "an Ant project is not a Maven one");

        File nothing = Files.createDirectories(tempDir.resolve("nothing")).toFile();
        assertFalse(runningInMavenFrom(nothing, null), "no project at all");
    }

    @Test
    void designerJarPropertyOfAnEarlierGeneratedProjectStillCounts(@TempDir Path tempDir) throws Exception {
        // Projects generated earlier keep forwarding the property, blank; with no
        // layout to recognise, it is still what says "Maven".
        File nothing = Files.createDirectories(tempDir.resolve("nothing")).toFile();
        assertTrue(runningInMavenFrom(nothing, ""));
    }

    @Test
    void resolvesInnerDesignerJarFromWrapperZip(@TempDir Path tempDir) throws Exception {
        String version = "8.0-SNAPSHOT";
        Path m2 = tempDir.resolve("m2/com/codenameone");
        Path coreDir = Files.createDirectories(m2.resolve("codenameone-core/" + version));
        Path designerDir = Files.createDirectories(m2.resolve("codenameone-designer/" + version));

        File coreJar = coreDir.resolve("codenameone-core-" + version + ".jar").toFile();
        // The coreJar file's existence isn't required by the resolver, but write
        // some bytes so a layout-aware check (e.g. "is this really a jar") would
        // pass if we ever add one.
        Files.write(coreJar.toPath(), new byte[]{0x50, 0x4B, 0x05, 0x06});

        byte[] innerJarPayload = ("MANIFEST-LIKE-MARKER-" + version).getBytes("UTF-8");
        File wrapperZip = designerDir.resolve(
                "codenameone-designer-" + version + "-jar-with-dependencies.jar").toFile();
        writeWrapperZip(wrapperZip, innerJarPayload);

        File resolved = MavenUtils.findDesignerJarInM2(coreJar);

        assertNotNull(resolved, "Expected resolver to find the inner designer jar");
        assertEquals("designer_1.jar", resolved.getName(),
                "Resolver must return the runnable inner jar, not the wrapper zip");
        assertTrue(resolved.isFile(), "Returned path must exist as a regular file");
        assertArrayEquals(innerJarPayload, Files.readAllBytes(resolved.toPath()),
                "Returned jar must be the inner jar extracted from the wrapper");
    }

    @Test
    void reusesExtractedInnerJarWhenWrapperHasNotChanged(@TempDir Path tempDir) throws Exception {
        String version = "8.0-SNAPSHOT";
        Path m2 = tempDir.resolve("m2/com/codenameone");
        Path coreDir = Files.createDirectories(m2.resolve("codenameone-core/" + version));
        Path designerDir = Files.createDirectories(m2.resolve("codenameone-designer/" + version));
        File coreJar = coreDir.resolve("codenameone-core-" + version + ".jar").toFile();
        Files.write(coreJar.toPath(), new byte[]{0x50, 0x4B, 0x05, 0x06});

        byte[] payload = "payload-v1".getBytes("UTF-8");
        File wrapperZip = designerDir.resolve(
                "codenameone-designer-" + version + "-jar-with-dependencies.jar").toFile();
        writeWrapperZip(wrapperZip, payload);

        File firstResolve = MavenUtils.findDesignerJarInM2(coreJar);
        assertNotNull(firstResolve);
        long extractedMtime = firstResolve.lastModified();

        // Make sure mtime comparison won't accidentally re-extract because of
        // FS resolution. A second resolve with no wrapper change should be a no-op.
        File secondResolve = MavenUtils.findDesignerJarInM2(coreJar);
        assertNotNull(secondResolve);
        assertEquals(firstResolve.getAbsolutePath(), secondResolve.getAbsolutePath());
        assertEquals(extractedMtime, secondResolve.lastModified(),
                "Inner jar should not be re-extracted when wrapper hasn't changed");
    }

    @Test
    void reExtractsWhenWrapperZipIsNewerThanExtractedJar(@TempDir Path tempDir) throws Exception {
        String version = "8.0-SNAPSHOT";
        Path m2 = tempDir.resolve("m2/com/codenameone");
        Path coreDir = Files.createDirectories(m2.resolve("codenameone-core/" + version));
        Path designerDir = Files.createDirectories(m2.resolve("codenameone-designer/" + version));
        File coreJar = coreDir.resolve("codenameone-core-" + version + ".jar").toFile();
        Files.write(coreJar.toPath(), new byte[]{0x50, 0x4B, 0x05, 0x06});
        File wrapperZip = designerDir.resolve(
                "codenameone-designer-" + version + "-jar-with-dependencies.jar").toFile();

        writeWrapperZip(wrapperZip, "payload-v1".getBytes("UTF-8"));
        File first = MavenUtils.findDesignerJarInM2(coreJar);
        assertNotNull(first);
        assertArrayEquals("payload-v1".getBytes("UTF-8"), Files.readAllBytes(first.toPath()));

        // Rewrite the wrapper with a new payload and bump its mtime past the
        // extracted inner jar. The resolver must notice and re-extract.
        writeWrapperZip(wrapperZip, "payload-v2".getBytes("UTF-8"));
        long bumped = first.lastModified() + 5_000L;
        assertTrue(wrapperZip.setLastModified(bumped),
                "FS must support setLastModified for this test");

        File second = MavenUtils.findDesignerJarInM2(coreJar);
        assertNotNull(second);
        assertArrayEquals("payload-v2".getBytes("UTF-8"), Files.readAllBytes(second.toPath()));
    }

    @Test
    void returnsNullForUnrelatedJarLocation(@TempDir Path tempDir) throws Exception {
        // Core jar living outside an m2 layout: resolver must give up rather
        // than return a phantom path. The Component Inspector then falls through
        // to its ~/.codenameone/designer_1.jar legacy fallback.
        File notInM2 = tempDir.resolve("build/codenameone-core.jar").toFile();
        Files.createDirectories(notInM2.getParentFile().toPath());
        Files.write(notInM2.toPath(), new byte[]{0x50, 0x4B, 0x05, 0x06});

        assertNull(MavenUtils.findDesignerJarInM2(notInM2));
    }

    @Test
    void refusesPathTraversalEntriesAndDoesNotWriteOutsideExtractDir(@TempDir Path tempDir) throws Exception {
        // CodeQL flagged the original extraction loop as a Zip Slip risk: it
        // built `new File(destDir, entry.getName())` from untrusted archive
        // metadata. Pack a wrapper whose only entry is a `../../etc/passwd`-
        // style traversal name and verify the resolver refuses to extract
        // (no file outside the extracted dir, no inner jar produced).
        String version = "8.0-SNAPSHOT";
        Path m2 = tempDir.resolve("m2/com/codenameone");
        Path coreDir = Files.createDirectories(m2.resolve("codenameone-core/" + version));
        Path designerDir = Files.createDirectories(m2.resolve("codenameone-designer/" + version));
        File coreJar = coreDir.resolve("codenameone-core-" + version + ".jar").toFile();
        Files.write(coreJar.toPath(), new byte[]{0x50, 0x4B, 0x05, 0x06});

        File wrapperZip = designerDir.resolve(
                "codenameone-designer-" + version + "-jar-with-dependencies.jar").toFile();
        File traversalSentinel = tempDir.resolve("escaped.txt").toFile();
        FileOutputStream fos = new FileOutputStream(wrapperZip);
        try {
            ZipOutputStream zos = new ZipOutputStream(fos);
            try {
                // The relative `../../escaped.txt` resolves to tempDir/escaped.txt
                // if the extractor were vulnerable: extractedDir lives at
                // designerDir/<wrapper>-extracted/, so two ".." pops back to
                // tempDir's root.
                zos.putNextEntry(new ZipEntry("../../escaped.txt"));
                zos.write("if-you-see-me-zip-slip-happened".getBytes("UTF-8"));
                zos.closeEntry();
            } finally {
                zos.close();
            }
        } finally {
            fos.close();
        }

        File resolved = MavenUtils.findDesignerJarInM2(coreJar);
        assertNull(resolved, "Resolver must report failure when the wrapper has no designer_1.jar");
        assertFalse(traversalSentinel.exists(),
                "Traversal entry must not be written outside the extraction directory");
    }

    @Test
    void skipsUnexpectedEntriesAndStillExtractsDesignerJar(@TempDir Path tempDir) throws Exception {
        // Hardening guard: even if a future wrapper variant adds extra files
        // alongside designer_1.jar, the extractor should ignore them and only
        // surface the canonical inner jar.
        String version = "8.0-SNAPSHOT";
        Path m2 = tempDir.resolve("m2/com/codenameone");
        Path coreDir = Files.createDirectories(m2.resolve("codenameone-core/" + version));
        Path designerDir = Files.createDirectories(m2.resolve("codenameone-designer/" + version));
        File coreJar = coreDir.resolve("codenameone-core-" + version + ".jar").toFile();
        Files.write(coreJar.toPath(), new byte[]{0x50, 0x4B, 0x05, 0x06});

        File wrapperZip = designerDir.resolve(
                "codenameone-designer-" + version + "-jar-with-dependencies.jar").toFile();
        byte[] expectedPayload = "real-designer-bytes".getBytes("UTF-8");
        FileOutputStream fos = new FileOutputStream(wrapperZip);
        try {
            ZipOutputStream zos = new ZipOutputStream(fos);
            try {
                zos.putNextEntry(new ZipEntry("README.txt"));
                zos.write("noise".getBytes("UTF-8"));
                zos.closeEntry();
                zos.putNextEntry(new ZipEntry("designer_1.jar"));
                zos.write(expectedPayload);
                zos.closeEntry();
                zos.putNextEntry(new ZipEntry("subdir/other.jar"));
                zos.write("nope".getBytes("UTF-8"));
                zos.closeEntry();
            } finally {
                zos.close();
            }
        } finally {
            fos.close();
        }

        File resolved = MavenUtils.findDesignerJarInM2(coreJar);
        assertNotNull(resolved);
        assertEquals("designer_1.jar", resolved.getName());
        assertArrayEquals(expectedPayload, Files.readAllBytes(resolved.toPath()));
        // The extra entries must not have been written to the extraction dir.
        File extractedDir = resolved.getParentFile();
        assertFalse(new File(extractedDir, "README.txt").exists(),
                "Unexpected entries must be skipped, not written");
        assertFalse(new File(extractedDir, "subdir").exists(),
                "Unexpected nested entries must be skipped, not written");
    }

    @Test
    void returnsNullWhenDesignerArtifactMissing(@TempDir Path tempDir) throws Exception {
        // Core jar lives in a valid m2 layout but the designer artifact has not
        // been resolved into the local repo (e.g. cn1lib project that doesn't
        // run the maven plugin). Resolver should report null, not throw.
        String version = "8.0-SNAPSHOT";
        Path coreDir = Files.createDirectories(
                tempDir.resolve("m2/com/codenameone/codenameone-core/" + version));
        File coreJar = coreDir.resolve("codenameone-core-" + version + ".jar").toFile();
        Files.write(coreJar.toPath(), new byte[]{0x50, 0x4B, 0x05, 0x06});

        assertNull(MavenUtils.findDesignerJarInM2(coreJar));
    }

    @Test
    void fallsBackToTheNewestDesignerWhenNoneMatchesTheCoreVersion(@TempDir Path tempDir) throws Exception {
        // The Resource Editor is frozen, so its version stops tracking the core's. The
        // Component Inspector's Edit Style action only needs *a* designer, and requiring an
        // exact match would leave that action dead on every project past the frozen editor.
        Path m2 = tempDir.resolve("m2/com/codenameone");
        Path coreDir = Files.createDirectories(m2.resolve("codenameone-core/8.0.5"));
        File coreJar = coreDir.resolve("codenameone-core-8.0.5.jar").toFile();
        Files.write(coreJar.toPath(), new byte[]{0x50, 0x4B, 0x05, 0x06});

        // Numeric ordering, not lexical: 7.0.263 must win over 7.0.9.
        for (String v : new String[]{"7.0.9", "7.0.263"}) {
            Path dir = Files.createDirectories(m2.resolve("codenameone-designer/" + v));
            writeWrapperZip(dir.resolve("codenameone-designer-" + v + "-jar-with-dependencies.jar").toFile(),
                    ("payload-" + v).getBytes("UTF-8"));
        }

        File resolved = MavenUtils.findDesignerJarInM2(coreJar);
        assertNotNull(resolved, "Expected the resolver to fall back to an available designer");
        assertArrayEquals("payload-7.0.263".getBytes("UTF-8"), Files.readAllBytes(resolved.toPath()),
                "Expected the newest available designer, ordered numerically");
    }

    @Test
    void comparesVersionSegmentsNumerically() {
        assertTrue(MavenUtils.compareVersions("7.0.263", "7.0.9") > 0);
        assertTrue(MavenUtils.compareVersions("7.0.9", "7.0.10") < 0);
        assertTrue(MavenUtils.compareVersions("8.0", "7.0.263") > 0);
        assertEquals(0, MavenUtils.compareVersions("7.0.263", "7.0.263"));
    }

    private static void writeWrapperZip(File wrapperZip, byte[] innerJarPayload) throws Exception {
        // Mirror the layout produced by maven/designer/pom.xml's antrun step:
        // a plain zip whose sole entry is a designer_1.jar file. No manifest,
        // not directly runnable.
        FileOutputStream fos = new FileOutputStream(wrapperZip);
        try {
            ZipOutputStream zos = new ZipOutputStream(fos);
            try {
                zos.putNextEntry(new ZipEntry("designer_1.jar"));
                zos.write(innerJarPayload);
                zos.closeEntry();
            } finally {
                zos.close();
            }
        } finally {
            fos.close();
        }
    }
}
