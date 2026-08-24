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
            out.write(new byte[] {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE});
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
}
