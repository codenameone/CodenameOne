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
package com.codename1.impl.javase;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The simulator picks the build-hint manifest that belongs to THIS application.
 */
public class SimulatorAnnotationManifestTest {

    /**
     * The launcher's identity decides, not the settings file's.
     *
     * <p><code>process-annotations</code> stamps the manifest with the effective
     * main class -- the file with any <code>-Dcodename1.mainName</code> applied
     * -- and <code>SimulatorMojo</code> forwards that same pair to the fork.
     * Reading only the file disagreed with the stamp on an overridden build, so
     * the simulator refused the manifest and ran without the hints the device
     * build applies.</p>
     */
    @Test
    public void theForwardedMainClassOutranksTheSettingsFile(@TempDir File tmp)
            throws Exception {
        writeProjectSettings(tmp, "com.example", "FileApp");
        String mainBefore = System.getProperty("codename1.mainName");
        String pkgBefore = System.getProperty("codename1.packageName");
        System.setProperty("codename1.mainName", "OverriddenApp");
        System.setProperty("codename1.packageName", "com.example");
        try {
            assertEquals("com.example.OverriddenApp", Simulator.configuredMainClass(tmp));
        } finally {
            restore("codename1.mainName", mainBefore);
            restore("codename1.packageName", pkgBefore);
        }
        assertEquals("com.example.FileApp", Simulator.configuredMainClass(tmp));
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    private static void writeProjectSettings(File dir, String pkg, String main) throws Exception {
        Properties p = new Properties();
        p.setProperty("codename1.packageName", pkg);
        p.setProperty("codename1.mainName", main);
        FileOutputStream os =
                new FileOutputStream(new File(dir, "codenameone_settings.properties"));
        try {
            p.store(os, null);
        } finally {
            os.close();
        }
    }

    /**
     * A command-line alias claims the hint the annotation would publish.
     *
     * <p>An alias and its target are one effective setting. With
     * `-Dcodename1.arg.cn1.androidTheme` against an annotated `and.themeMode`
     * the canonical key looked unclaimed, so the annotation value was published
     * -- and `buildHint()` reads the canonical before the alias, so the
     * annotation beat the command line in the simulator while the device build
     * honoured it.</p>
     */
    @Test
    public void aCommandLineAliasClaimsTheHint() throws Exception {
        Properties manifest = new Properties();
        manifest.setProperty("cn1.buildHints.alias.and.themeMode", "cn1.androidTheme");

        String before = System.getProperty("codename1.arg.cn1.androidTheme");
        System.setProperty("codename1.arg.cn1.androidTheme", "legacy");
        try {
            assertEquals("codename1.arg.cn1.androidTheme",
                    Simulator.claimedBySystemProperty(manifest,
                            "codename1.arg.and.themeMode"));
        } finally {
            if (before == null) {
                System.clearProperty("codename1.arg.cn1.androidTheme");
            } else {
                System.setProperty("codename1.arg.cn1.androidTheme", before);
            }
        }

        // Nothing on the command line: the annotation publishes as before.
        assertNull(Simulator.claimedBySystemProperty(manifest,
                "codename1.arg.and.themeMode"));

        // A hint with no alias list is judged on its own key alone.
        assertNull(Simulator.claimedBySystemProperty(new Properties(),
                "codename1.arg.ios.pods"));
    }

    /** A manifest that names no main class -- not something the processor writes. */
    private static void unstampedManifestIn(File dir, String hint) throws Exception {
        File out = new File(dir, "META-INF/codenameone");
        out.mkdirs();
        Properties p = new Properties();
        p.setProperty("codename1.arg.desktop.titleBar", hint);
        FileOutputStream os = new FileOutputStream(new File(out, "build-hints.properties"));
        try {
            p.store(os, null);
        } finally {
            os.close();
        }
    }

    private static File manifestIn(File dir, String mainClass, String hint) throws Exception {
        File out = new File(dir, "META-INF/codenameone");
        out.mkdirs();
        File f = new File(out, "build-hints.properties");
        Properties p = new Properties();
        p.setProperty("cn1.buildHints.mainClass", mainClass);
        p.setProperty("codename1.arg.desktop.titleBar", hint);
        FileOutputStream os = new FileOutputStream(f);
        try {
            p.store(os, null);
        } finally {
            os.close();
        }
        return f;
    }

    /**
     * A reactor dependency or a stale output directory earlier on the classpath
     * can carry another application's manifest. Taking the first directory that
     * has one ended the search there, and the caller then saw a main class that
     * was not this one and published nothing -- so <code>cn1:run</code> silently
     * dropped every annotated hint while this application's own manifest sat in
     * a later classpath entry.
     */
    @Test
    public void aForeignDirectoryManifestIsPassedOver(@TempDir File tmp) throws Exception {
        File other = new File(tmp, "other-classes");
        File mine = new File(tmp, "my-classes");
        manifestIn(other, "com.other.TheirApp", "MINIMAL");
        manifestIn(mine, "com.example.MyApp", "NATIVE");

        String cp = other.getAbsolutePath() + File.pathSeparator + mine.getAbsolutePath();
        Simulator.FoundManifest found =
                Simulator.findAnnotationManifest(tmp, cp, "com.example.MyApp");
        assertNotNull(found);
        assertEquals("com.example.MyApp", found.hints.getProperty("cn1.buildHints.mainClass"));
        assertEquals("NATIVE", found.hints.getProperty("codename1.arg.desktop.titleBar"));
    }

    /**
     * An unstamped manifest anywhere on the classpath does not outrank this
     * application's own.
     *
     * <p>It used to be kept as a last resort "in case it predates the stamp",
     * and returned BEFORE the conventional <code>target/classes</code> lookup.
     * Nothing predates it -- the resource is written by
     * <code>process-annotations</code>, which always stamps it -- so what the
     * leniency really did was let a dependency's output directory, or a copy a
     * project keeps in <code>src/main/resources</code>, supply the hints while
     * this project's own properly stamped manifest was never looked at.</p>
     */
    @Test
    public void anUnstampedManifestDoesNotOutrankTheProjectsOwn(@TempDir File tmp)
            throws Exception {
        File stranger = new File(tmp, "dependency-classes");
        unstampedManifestIn(stranger, "MINIMAL");
        manifestIn(new File(tmp, "target/classes"), "com.example.MyApp", "NATIVE");

        Simulator.FoundManifest found = Simulator.findAnnotationManifest(
                tmp, stranger.getAbsolutePath(), "com.example.MyApp");
        assertNotNull(found);
        assertEquals("com.example.MyApp", found.hints.getProperty("cn1.buildHints.mainClass"));
        assertEquals("NATIVE", found.hints.getProperty("codename1.arg.desktop.titleBar"));
    }

    /**
     * ...and with no manifest of our own anywhere, an unstamped one is not
     * adopted either: nothing is published rather than a stranger's hints.
     */
    @Test
    public void anUnstampedManifestIsNotAdoptedOnItsOwn(@TempDir File tmp) throws Exception {
        File stranger = new File(tmp, "dependency-classes");
        unstampedManifestIn(stranger, "MINIMAL");
        assertNull(Simulator.findAnnotationManifest(
                tmp, stranger.getAbsolutePath(), "com.example.MyApp"));
    }

    /**
     * With no configured main class there is nothing to compare against, so the
     * first manifest on the classpath is still the answer.
     */
    @Test
    public void withoutAMainClassTheFirstManifestWins(@TempDir File tmp) throws Exception {
        File first = new File(tmp, "first");
        manifestIn(first, "com.other.TheirApp", "MINIMAL");
        Simulator.FoundManifest found =
                Simulator.findAnnotationManifest(tmp, first.getAbsolutePath(), null);
        assertNotNull(found);
        assertEquals("MINIMAL", found.hints.getProperty("codename1.arg.desktop.titleBar"));
    }

    /**
     * Only foreign manifests, and no conventional build: nothing is published
     * rather than another application's hints.
     */
    @Test
    public void onlyForeignManifestsFindNothing(@TempDir File tmp) throws Exception {
        File other = new File(tmp, "other-classes");
        manifestIn(other, "com.other.TheirApp", "MINIMAL");
        assertNull(Simulator.findAnnotationManifest(
                tmp, other.getAbsolutePath(), "com.example.MyApp"));
    }

    /**
     * Writes a jar holding the main class and the manifest, with the entry times
     * given.
     */
    private static File jarWith(File dir, String name, long classTime, long manifestTime)
            throws Exception {
        File jar = new File(dir, name);
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(jar));
        try {
            ZipEntry cls = new ZipEntry("com/example/MyApp.class");
            cls.setTime(classTime);
            out.putNextEntry(cls);
            out.write(CLASS_BYTES);
            out.closeEntry();

            ZipEntry res = new ZipEntry("META-INF/codenameone/build-hints.properties");
            res.setTime(manifestTime);
            out.putNextEntry(res);
            out.write("cn1.buildHints.mainClass=com.example.MyApp\n".getBytes("ISO-8859-1"));
            out.closeEntry();
        } finally {
            out.close();
        }
        return jar;
    }

    private static Properties stampedFor(String mainClass) {
        Properties p = new Properties();
        p.setProperty("cn1.buildHints.mainClass", mainClass);
        return p;
    }

    /**
     * Sharing an archive does not mean sharing a build. Nothing deletes an old
     * manifest from <code>target/classes</code>, so a recompiled main class and
     * last week's resource get packaged into the same jar -- and skipping the
     * staleness check for jars published the old annotation values.
     */
    @Test
    public void aStaleManifestInsideAJarIsDetected(@TempDir File tmp) throws Exception {
        long manifest = 1600000000000L;
        long newerClass = manifest + 60000L;
        File jar = jarWith(tmp, "app-stale.jar", newerClass, manifest);

        assertEquals("com/example/MyApp.class", Simulator.classNewerThanManifestInJar(
                stampedFor("com.example.MyApp"), jar));
    }

    /** A manifest written after the class it describes is current. */
    @Test
    public void aCurrentManifestInsideAJarIsAccepted(@TempDir File tmp) throws Exception {
        long cls = 1600000000000L;
        File jar = jarWith(tmp, "app-current.jar", cls, cls + 60000L);

        assertNull(Simulator.classNewerThanManifestInJar(
                stampedFor("com.example.MyApp"), jar));
    }

    /** No class of that name in the jar is nothing to compare against. */
    @Test
    public void aJarWithoutTheMainClassIsNotJudged(@TempDir File tmp) throws Exception {
        long cls = 1600000000000L;
        File jar = jarWith(tmp, "app-other.jar", cls + 60000L, cls);

        assertNull(Simulator.classNewerThanManifestInJar(
                stampedFor("com.other.TheirApp"), jar));
    }

    private static final byte[] CLASS_BYTES =
            new byte[] {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE};

    /** SHA-256 of what jarWith puts in the class entry, hex. */
    private static String classBytesDigest() throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        md.update(CLASS_BYTES);
        StringBuilder hex = new StringBuilder();
        for (byte b : md.digest()) {
            hex.append(Character.forDigit((b >> 4) & 0xF, 16));
            hex.append(Character.forDigit(b & 0xF, 16));
        }
        return hex.toString();
    }

    private static Properties stamped(String mainClass, String classDigest) {
        Properties p = stampedFor(mainClass);
        if (classDigest != null) {
            p.setProperty("cn1.buildHints.classDigest", classDigest);
        }
        return p;
    }

    /**
     * Zip records entry times to two seconds, and a build configured for
     * reproducible output stamps every entry identically -- so a stale manifest
     * and the class it no longer describes can compare EQUAL, which made the
     * timestamp rule inert rather than merely coarse. The recorded contents of
     * the class settle it.
     */
    @Test
    public void aStaleManifestIsDetectedWhenTheTimestampsAreEqual(@TempDir File tmp)
            throws Exception {
        long same = 1600000000000L;
        File jar = jarWith(tmp, "app-reproducible.jar", same, same);

        Simulator.FoundManifest found = new Simulator.FoundManifest(
                stamped("com.example.MyApp", "0000deadbeef"), null, jar, "app-reproducible.jar");
        assertEquals("does not describe the compiled com/example/MyApp.class",
                Simulator.staleManifestReason(found.hints, found));
    }

    /** ...and a digest that matches settles it the other way, timestamps aside. */
    @Test
    public void aMatchingClassDigestOutranksTheTimestamps(@TempDir File tmp) throws Exception {
        long manifest = 1600000000000L;
        File jar = jarWith(tmp, "app-touched.jar", manifest + 60000L, manifest);

        Simulator.FoundManifest found = new Simulator.FoundManifest(
                stamped("com.example.MyApp", classBytesDigest()), null, jar, "app-touched.jar");
        assertNull(Simulator.staleManifestReason(found.hints, found));
    }

    /** A manifest an older plugin wrote records no digest, so timestamps still decide. */
    @Test
    public void withoutARecordedDigestTheTimestampsStillDecide(@TempDir File tmp)
            throws Exception {
        long manifest = 1600000000000L;
        File jar = jarWith(tmp, "app-old.jar", manifest + 60000L, manifest);

        Simulator.FoundManifest found = new Simulator.FoundManifest(
                stamped("com.example.MyApp", null), null, jar, "app-old.jar");
        assertEquals("is older than com/example/MyApp.class",
                Simulator.staleManifestReason(found.hints, found));
    }

    /** A directory holding a class file and a manifest that does or does not describe it. */
    private static File outputDir(File parent, String name, String hint, boolean current)
            throws Exception {
        File dir = new File(parent, name);
        File cls = new File(dir, "com/example/MyApp.class");
        cls.getParentFile().mkdirs();
        FileOutputStream cs = new FileOutputStream(cls);
        try {
            cs.write(CLASS_BYTES);
        } finally {
            cs.close();
        }
        File out = new File(dir, "META-INF/codenameone");
        out.mkdirs();
        Properties p = new Properties();
        p.setProperty("cn1.buildHints.mainClass", "com.example.MyApp");
        p.setProperty("cn1.buildHints.classDigest",
                current ? classBytesDigest() : "0000notthisbuild");
        p.setProperty("codename1.arg.desktop.titleBar", hint);
        FileOutputStream os = new FileOutputStream(new File(out, "build-hints.properties"));
        try {
            p.store(os, null);
        } finally {
            os.close();
        }
        return dir;
    }

    /**
     * A leftover output directory earlier on the classpath carries a manifest
     * stamped for this same main class. Taking it ended the search, and the
     * caller then reported it stale and published nothing -- while the current
     * manifest sat in a later entry, so <code>cn1:run</code> dropped every
     * annotated hint.
     */
    @Test
    public void aStaleManifestForThisApplicationIsPassedOver(@TempDir File tmp) throws Exception {
        File old = outputDir(tmp, "stale-classes", "MINIMAL", false);
        File now = outputDir(tmp, "current-classes", "NATIVE", true);

        String cp = old.getAbsolutePath() + File.pathSeparator + now.getAbsolutePath();
        Simulator.FoundManifest found =
                Simulator.findAnnotationManifest(tmp, cp, "com.example.MyApp");
        assertNotNull(found);
        assertEquals("NATIVE", found.hints.getProperty("codename1.arg.desktop.titleBar"));
    }

    /**
     * With nothing current anywhere the stale one is still returned, so the
     * caller can say which file it is and why it was not used.
     */
    @Test
    public void withNothingCurrentTheStaleOneIsStillReported(@TempDir File tmp) throws Exception {
        File old = outputDir(tmp, "only-stale", "MINIMAL", false);

        Simulator.FoundManifest found =
                Simulator.findAnnotationManifest(tmp, old.getAbsolutePath(), "com.example.MyApp");
        assertNotNull(found);
        assertEquals("MINIMAL", found.hints.getProperty("codename1.arg.desktop.titleBar"));
        assertEquals("does not describe the compiled com/example/MyApp.class",
                Simulator.staleManifestReason(found.hints, found));
    }
}
