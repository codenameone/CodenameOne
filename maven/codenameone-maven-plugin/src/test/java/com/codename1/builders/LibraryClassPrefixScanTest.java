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
package com.codename1.builders;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Finding a feature that only a submitted library uses.
 *
 * <p>The builders' own scanner reads the application's compiled classes and
 * never opens a jar, so before this a cn1lib was the one place an API could
 * be used with none of its permissions, services or native defines
 * switched on.</p>
 */
public class LibraryClassPrefixScanTest {

    private static final String[] PREFIXES = {
        "com/codename1/call/session/",
        "com/codename1/call/directory/",
        "com/codename1/vpn/profile/",
    };

    @Test
    public void findsAPrefixInsideAJar() throws Exception {
        File dir = tempDir();
        writeJar(new File(dir, "somelib.jar"), "com/codename1/call/session/Calls");
        Set<String> found = LibraryClassPrefixScan.prefixesFound(dir, PREFIXES);
        assertTrue(found.contains("com/codename1/call/session/"));
        assertFalse(found.contains("com/codename1/vpn/profile/"),
                "only what the library actually references");
    }

    @Test
    public void findsAPrefixInsideAnAarsNestedClassesJar() throws Exception {
        // An Android archive keeps its bytecode one level further in, which
        // is where a cn1lib's Android half lives.
        File dir = tempDir();
        File inner = new File(dir, "classes.jar");
        writeJar(inner, "com/codename1/vpn/profile/Vpn");
        byte[] innerBytes = readAll(inner);
        inner.delete();
        File aar = new File(dir, "somelib.aar");
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(aar));
        try {
            zip.putNextEntry(new ZipEntry("classes.jar"));
            zip.write(innerBytes);
            zip.closeEntry();
        } finally {
            zip.close();
        }
        Set<String> found = LibraryClassPrefixScan.prefixesFound(dir, PREFIXES);
        assertTrue(found.contains("com/codename1/vpn/profile/"));
    }

    @Test
    public void findsAPrefixInALooseClassFile() throws Exception {
        File dir = tempDir();
        File cls = new File(dir, "Whatever.class");
        FileOutputStream out = new FileOutputStream(cls);
        try {
            out.write("com/codename1/call/directory/CallDirectory".getBytes("UTF-8"));
        } finally {
            out.close();
        }
        assertTrue(LibraryClassPrefixScan.prefixesFound(dir, PREFIXES)
                .contains("com/codename1/call/directory/"));
    }

    @Test
    public void anEmptyOrAbsentFolderFindsNothing() throws Exception {
        assertTrue(LibraryClassPrefixScan.prefixesFound(null, PREFIXES).isEmpty());
        assertTrue(LibraryClassPrefixScan.prefixesFound(
                new File("/no/such/place"), PREFIXES).isEmpty());
        assertTrue(LibraryClassPrefixScan.prefixesFound(tempDir(), PREFIXES)
                .isEmpty());
    }

    @Test
    public void aBrokenArchiveIsSkippedRatherThanCharged() throws Exception {
        // Guessing that an unreadable file uses everything would charge the
        // whole apparatus to any app shipping a stray file.
        File dir = tempDir();
        File junk = new File(dir, "broken.jar");
        FileOutputStream out = new FileOutputStream(junk);
        try {
            out.write("this is not a zip".getBytes("UTF-8"));
        } finally {
            out.close();
        }
        assertTrue(LibraryClassPrefixScan.prefixesFound(dir, PREFIXES).isEmpty());
    }

    private static File tempDir() throws Exception {
        File dir = File.createTempFile("cn1scantest", "");
        dir.delete();
        dir.mkdirs();
        dir.deleteOnExit();
        return dir;
    }

    private static void writeJar(File target, String className) throws Exception {
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(target));
        try {
            zip.putNextEntry(new ZipEntry("some/Thing.class"));
            zip.write(className.getBytes("UTF-8"));
            zip.closeEntry();
        } finally {
            zip.close();
        }
    }

    private static byte[] readAll(File f) throws Exception {
        java.io.InputStream in = new java.io.FileInputStream(f);
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int r = in.read(buf);
            while (r > 0) {
                out.write(buf, 0, r);
                r = in.read(buf);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }
}
