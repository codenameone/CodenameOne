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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IosLocationHintsTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File settings(String text) throws Exception {
        File common = tmp.newFolder("common");
        File android = new File(common, "src/main/android");
        assertTrue(android.mkdirs());
        File f = new File(common, "codenameone_settings.properties");
        Files.write(f.toPath(), text.getBytes("ISO-8859-1"));
        assertEquals(f.getCanonicalFile(), IosLocationHints.settingsFor(android).getCanonicalFile());
        return f;
    }

    private static String read(File f) throws Exception {
        return new String(Files.readAllBytes(f.toPath()), "ISO-8859-1");
    }

    @Test
    public void foregroundLocationAddsOnlyTheWhenInUseDescription() throws Exception {
        File f = settings("# keep me\ncodename1.mainName=Main\n");
        List<String> changed = IosLocationHints.apply(f,
                Arrays.asList("android.permission.ACCESS_FINE_LOCATION"), null, null);
        assertEquals(Collections.singletonList(IosLocationHints.WHEN_IN_USE), changed);
        String text = read(f);
        assertTrue(text.startsWith("# keep me\ncodename1.mainName=Main\n"));
        assertTrue(text.contains(IosLocationHints.WHEN_IN_USE + "="));
        assertFalse(text.contains("background_modes"));
        assertTrue("a second run changes nothing",
                IosLocationHints.apply(f, Arrays.asList("android.permission.ACCESS_FINE_LOCATION"), null, null)
                        .isEmpty());
    }

    @Test
    public void backgroundLocationExtendsBackgroundModesAndKeepsTheDevelopersText() throws Exception {
        File f = settings("codename1.arg.ios.NSLocationWhenInUseUsageDescription=Mine\r\n"
                + "codename1.arg.ios.background_modes=fetch\r\n");
        IosLocationHints.apply(f, Arrays.asList("android.permission.ACCESS_COARSE_LOCATION",
                "android.permission.ACCESS_BACKGROUND_LOCATION"), null, null);
        String text = read(f);
        assertTrue(text.contains("NSLocationWhenInUseUsageDescription=Mine\r\n"));
        assertEquals(1, text.split("NSLocationWhenInUseUsageDescription", -1).length - 1);
        assertTrue(text.contains("codename1.arg.ios.background_modes=fetch,location\r\n"));
        assertTrue(text.contains(IosLocationHints.ALWAYS + "="));
        assertTrue(text.contains(IosLocationHints.ALWAYS_AND_WHEN_IN_USE + "="));
        assertFalse("line endings are kept", text.replace("\r\n", "").contains("\n"));
    }

    @Test
    public void anAnnotationDeclarationIsNotDuplicated() throws Exception {
        File f = settings("");
        File src = tmp.newFolder("src");
        Files.write(new File(src, "Main.java").toPath(), ("@IosPrivacy(locationWhenInUseUsageDescription = \"x\")\n"
                + "public class Main {}\n").getBytes("UTF-8"));
        IosLocationHints.apply(f, Arrays.asList("android.permission.FOREGROUND_SERVICE_LOCATION"),
                Collections.singletonList(src), null);
        String text = read(f);
        assertFalse(text.contains(IosLocationHints.WHEN_IN_USE));
        assertTrue(text.contains(IosLocationHints.BACKGROUND_MODES + "=location"));
    }

    @Test
    public void noLocationPermissionLeavesTheFileAlone() throws Exception {
        File f = settings("a=b\n");
        assertTrue(IosLocationHints.apply(f, Arrays.asList("android.permission.INTERNET"), null, null).isEmpty());
        assertEquals("a=b\n", read(f));
    }
}
