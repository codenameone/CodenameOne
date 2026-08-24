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
}
