/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
 * Please contact Codename One through http://www.codenameone.com/ if
 * you need additional information or have any questions.
 */
package com.codename1.impl.javase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Skins menu is built by resolving every entry of the {@code skins} preference
 * back to a file. An entry is a percent-encoded {@code file:} URI, so the resolution
 * has to decode -- when it did not, a skin saved anywhere with a space in the path
 * (a Windows profile like {@code C:\Users\First Last\Downloads}, or a browser's
 * {@code Pixel9 (1).skin} de-duplication suffix) silently vanished from the menu.
 * It still loaded and applied; it just could never be picked again, which reads as
 * "adding a skin does nothing".
 *
 * <p>The names below are the real shapes that reach this code, not invented ones:
 * the Skin Designer sanitises its download name to ASCII alphanumerics, so the
 * offending characters always arrive from the directory or from the browser.
 */
public class SkinPathTest {

    /**
     * Every path shape that has to survive the round trip preference -> file. A
     * failure here is a skin the user can add but never select.
     */
    private static final String[] ROUND_TRIP_NAMES = {
        "Pixel9.skin",                  // the plain case
        "Pixel 9.skin",                 // a space in the file name
        "Pixel9 (1).skin",              // what a browser calls a repeat download
        "Pixel#9.skin",                 // '#' - encoded, and a URL fragment if it is not
        "100%.skin",                    // '%' - encoded, and an escape if it is not
        "Pixel+9.skin",                 // '+' - NOT encoded, and not a space either
        "a;b.skin",                     // ';' - the separator of the preference itself
        "Pixel&9.skin",
        "Pixel=9.skin",
        "Pixel,9.skin",
        "Pixel@9.skin",
        "Pixel~9.skin",
        "Pixel'9.skin",
        "Pixel!9.skin",
        "skin with several  spaces.skin",
        "Pixel\u00e99.skin",            // non-ASCII: e-acute
        "\u05e2\u05d1\u05e8\u05d9\u05ea.skin", // non-ASCII: Hebrew, no Latin at all
    };

    /** Directory names that are as capable of breaking the round trip as file names. */
    private static final String[] ROUND_TRIP_DIRS = {
        "skins",
        "First Last",                   // a Windows profile with a space
        "my skins (2026)",
        "100%",
        "a;b",                          // the separator, one level up from the file
    };

    private static File write(File f) throws IOException {
        File parent = f.getParentFile();
        assertTrue(parent.exists() || parent.mkdirs(), "could not create " + parent);
        OutputStream out = new FileOutputStream(f);
        try {
            out.write("skin".getBytes("UTF-8"));
        } finally {
            out.close();
        }
        return f;
    }

    // ------------------------------------------------------------------
    //  toEntry / toFile round trip
    // ------------------------------------------------------------------

    @Test
    public void everyFileNameShapeRoundTrips(@TempDir Path tmp) throws Exception {
        for (String name : ROUND_TRIP_NAMES) {
            File skin = write(new File(tmp.toFile(), name));
            String entry = SkinPath.toEntry(skin);
            File resolved = SkinPath.toFile(entry);
            assertNotNull(resolved, "unresolvable entry for " + name + ": " + entry);
            assertTrue(resolved.exists(), "dropped from the menu: " + name + " -> " + entry);
            assertEquals(skin.getAbsoluteFile(), resolved.getAbsoluteFile(), name);
            assertEquals(name, SkinPath.displayName(entry), "menu label for " + name);
        }
    }

    @Test
    public void everyDirectoryNameShapeRoundTrips(@TempDir Path tmp) throws Exception {
        for (String dir : ROUND_TRIP_DIRS) {
            File skin = write(new File(new File(tmp.toFile(), dir), "Pixel9.skin"));
            String entry = SkinPath.toEntry(skin);
            File resolved = SkinPath.toFile(entry);
            assertNotNull(resolved, "unresolvable entry for dir " + dir);
            assertTrue(resolved.exists(), "dropped from the menu: dir " + dir + " -> " + entry);
            assertEquals(skin.getAbsoluteFile(), resolved.getAbsoluteFile(), dir);
            assertEquals("Pixel9.skin", SkinPath.displayName(entry));
        }
    }

    /**
     * The regression proper. The old reading of an entry was
     * {@code new File(new URL(entry).getFile())}, which does not decode -- this
     * asserts the shapes that reading got wrong, so a revert to it fails here
     * rather than in a support thread.
     */
    @ParameterizedTest
    @ValueSource(strings = {"Pixel 9.skin", "Pixel9 (1).skin", "Pixel#9.skin", "100%.skin"})
    public void percentEncodedEntriesDefeatTheLegacyReading(String name, @TempDir Path tmp)
            throws Exception {
        File skin = write(new File(tmp.toFile(), name));
        String entry = SkinPath.toEntry(skin);
        assertTrue(entry.contains("%"), "expected an encoded entry, got " + entry);
        assertFalse(new File(new URL(entry).getFile()).exists(),
                "this name no longer exercises the bug: " + name);
        assertTrue(SkinPath.toFile(entry).exists(), name);
    }

    // ------------------------------------------------------------------
    //  the other entry shapes the preference holds
    // ------------------------------------------------------------------

    @Test
    public void classpathEntriesAreNotFiles() {
        // Resolvable as a File object, but nothing exists there: that is exactly
        // how classifySkin tells a bundled skin from one on disk.
        File f = SkinPath.toFile("/iPhoneX.skin");
        assertNotNull(f);
        assertFalse(f.exists());
        assertEquals("iPhoneX.skin", SkinPath.displayName("/iPhoneX.skin"));
    }

    @Test
    public void remoteEntriesAreNotFiles() {
        assertNull(SkinPath.toFile("http://www.codenameone.com/OTA/Nexus5.skin"));
        assertNull(SkinPath.toFile("https://www.codenameone.com/OTA/Nexus5.skin"));
        assertNull(SkinPath.toFile("jar:file:/x.jar!/iPhoneX.skin"));
        assertNull(SkinPath.toFile("ftp://example.com/Nexus5.skin"));
    }

    /**
     * A Windows drive letter is not a URI scheme. Asserted on every platform because
     * the rule is in our code, not the platform's: a one-character scheme is a drive,
     * anything longer is a scheme, and a preference written on Windows has to survive
     * being read anywhere.
     */
    @Test
    public void windowsDrivePathsAreNotMistakenForSchemes() {
        assertNotNull(SkinPath.toFile("C:\\skins\\Pixel9.skin"));
        assertNotNull(SkinPath.toFile("C:/skins/Pixel9.skin"));
        assertNotNull(SkinPath.toFile("D:\\my skins (2026)\\Pixel9.skin"));
        assertEquals("Pixel9.skin", SkinPath.displayName("file:/C:/skins/Pixel9.skin"));
        assertEquals("Pixel 9.skin", SkinPath.displayName("file:/C:/First%20Last/Pixel%209.skin"));
    }

    @Test
    public void bareFilesystemPathsStillResolve(@TempDir Path tmp) throws Exception {
        // Older versions stored the raw path rather than a URI, and those entries
        // are still in people's preference nodes.
        File skin = write(new File(tmp.toFile(), "Legacy.skin"));
        File resolved = SkinPath.toFile(skin.getAbsolutePath());
        assertNotNull(resolved);
        assertTrue(resolved.exists());
        assertEquals("Legacy.skin", SkinPath.displayName(skin.getAbsolutePath()));
    }

    @Test
    public void unencodedFileUriStillResolves(@TempDir Path tmp) throws Exception {
        // A hand-edited preference, or one written before toEntry existed: not a
        // legal URI, so new URI(..) throws and only the legacy reading finds it.
        File skin = write(new File(tmp.toFile(), "Pixel 9.skin"));
        File resolved = SkinPath.toFile("file:" + skin.getAbsolutePath());
        assertNotNull(resolved, "an unencoded file: entry must still resolve");
        assertTrue(resolved.exists());
    }

    @Test
    public void nullAndEmptyAreNotFiles() {
        assertNull(SkinPath.toFile(null));
        assertNull(SkinPath.toFile(""));
        assertNull(SkinPath.toEntry(null));
    }

    // ------------------------------------------------------------------
    //  the preference is a ';'-separated list, and ';' is a legal filename
    // ------------------------------------------------------------------

    @Test
    public void separatorInAFileNameIsEscaped(@TempDir Path tmp) throws Exception {
        File skin = write(new File(tmp.toFile(), "a;b.skin"));
        String entry = SkinPath.toEntry(skin);
        assertFalse(entry.contains(";"),
                "a raw ';' splits this entry into two that resolve to nothing: " + entry);
        List<String> entries = SkinPath.split(SkinPath.append("/iPhoneX.skin;", entry));
        assertEquals(Arrays.asList("/iPhoneX.skin", entry), entries);
        assertTrue(SkinPath.toFile(entry).exists());
        assertEquals("a;b.skin", SkinPath.displayName(entry));
    }

    @Test
    public void splitDropsTheEmptyEntriesOlderWritersLeft() {
        assertEquals(Arrays.asList("/iPhoneX.skin"), SkinPath.split("/iPhoneX.skin;"));
        assertEquals(Arrays.asList("/iPhoneX.skin", "file:/tmp/a.skin"),
                SkinPath.split("/iPhoneX.skin;;file:/tmp/a.skin"));
        assertEquals(0, SkinPath.split("").size());
        assertEquals(0, SkinPath.split(null).size());
        assertEquals(0, SkinPath.split(";;;").size());
    }

    @Test
    public void appendDeduplicatesOnWholeEntriesNotSubstrings() {
        String pref = SkinPath.append("/iPhoneX.skin;", "file:/tmp/MyAppleWatch45mm.skin");
        // The bundled skin is a substring of the user's: a contains() test skips it.
        String withBundled = SkinPath.append(pref, "/AppleWatch45mm.skin");
        assertTrue(SkinPath.contains(withBundled, "/AppleWatch45mm.skin"),
                "the bundled watch skin was swallowed by a substring match: " + withBundled);
        assertEquals(3, SkinPath.split(withBundled).size());
        // And an exact repeat is still a no-op.
        assertEquals(withBundled, SkinPath.append(withBundled, "/AppleWatch45mm.skin"));
    }

    @Test
    public void containsComparesWholeEntries() {
        String pref = "/iPhoneX.skin;file:/tmp/skins/Pixel9.skin";
        assertTrue(SkinPath.contains(pref, "/iPhoneX.skin"));
        assertTrue(SkinPath.contains(pref, "file:/tmp/skins/Pixel9.skin"));
        assertFalse(SkinPath.contains(pref, "Pixel9.skin"));
        assertFalse(SkinPath.contains(pref, "/tmp/skins/Pixel9.skin"));
        assertFalse(SkinPath.contains(pref, null));
    }

    @Test
    public void appendIgnoresNothingToAdd() {
        assertEquals("/iPhoneX.skin;", SkinPath.append("/iPhoneX.skin;", null));
        assertEquals("/iPhoneX.skin;", SkinPath.append("/iPhoneX.skin;", ""));
        assertEquals("a", SkinPath.append(null, "a"));
    }

    // ------------------------------------------------------------------
    //  the entry has to be openable, not merely resolvable
    // ------------------------------------------------------------------

    @Test
    public void everyRoundTrippedEntryIsAlsoReadable(@TempDir Path tmp) throws Exception {
        for (String name : ROUND_TRIP_NAMES) {
            File skin = write(new File(new File(tmp.toFile(), "my skins"), name));
            File resolved = SkinPath.toFile(SkinPath.toEntry(skin));
            assertNotNull(resolved, name);
            InputStream in = new java.io.FileInputStream(resolved);
            try {
                assertEquals('s', in.read(), name);
            } finally {
                in.close();
            }
        }
    }
}
