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
package com.codename1.impl.javase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An archive must not be able to write outside the directory it is unpacked into.
 *
 * `unzip` used to build each output path by concatenating the destination with
 * `ZipEntry.getName()`, unchecked. An entry named `../../x` therefore wrote
 * wherever the archive asked -- and both callers unpack a DOWNLOADED zip (Groovy
 * for the console, JavaFX for the browser component), so the archive is not
 * something the user authored. That is CWE-22, and CodeQL's java/zipslip.
 *
 * The malicious archive is built here rather than checked in as a fixture: a
 * committed zip that escapes its destination is an awkward thing to have in a
 * repository, and building it makes the attack visible in the test itself.
 */
class UnzipUtilityZipSlipTest {

    @Test
    void anEntryThatEscapesTheDestinationIsRefused(@TempDir Path tmp) throws IOException {
        Path dest = tmp.resolve("dest");
        Path outside = tmp.resolve("outside.txt");
        File zip = tmp.resolve("evil.zip").toFile();

        // "../outside.txt" resolves out of dest and into tmp.
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            out.putNextEntry(new ZipEntry("harmless.txt"));
            out.write("ok".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            out.putNextEntry(new ZipEntry("../outside.txt"));
            out.write("pwned".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }

        IOException e = assertThrows(IOException.class,
                () -> new UnzipUtility().unzip(zip.getAbsolutePath(), dest.toString()),
                "an entry escaping the destination must be refused, not written");
        assertTrue(e.getMessage().contains("escapes the destination"),
                "the refusal should say why: " + e.getMessage());
        assertFalse(Files.exists(outside),
                "the escaping entry must not have been written to " + outside);
    }

    @Test
    void anEntryEscapingIntoASiblingWithTheSamePrefixIsRefused(@TempDir Path tmp) throws IOException {
        // "dest-evil" shares a character prefix with "dest" but is a different
        // directory. A containment check written as a plain string startsWith
        // accepts this; a component-wise Path comparison rejects it.
        Path dest = tmp.resolve("dest");
        Path evilDir = tmp.resolve("dest-evil");
        Path sibling = evilDir.resolve("loot.txt");
        File zip = tmp.resolve("sibling.zip").toFile();

        // The target directory must already EXIST, or unguarded code fails with a
        // FileNotFoundException -- which is an IOException, so assertThrows would be
        // satisfied by the write merely failing rather than by the check refusing it.
        // That is exactly how this test passed against the vulnerable version on its
        // first run.
        Files.createDirectories(evilDir);

        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            out.putNextEntry(new ZipEntry("../dest-evil/loot.txt"));
            out.write("pwned".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }

        IOException e = assertThrows(IOException.class,
                () -> new UnzipUtility().unzip(zip.getAbsolutePath(), dest.toString()),
                "a sibling directory sharing a prefix is still outside the destination");
        assertTrue(e.getMessage() != null && e.getMessage().contains("escapes the destination"),
                "must be refused by the containment check, not by an incidental IO failure: "
                        + e);
        assertFalse(Files.exists(sibling), "nothing should have been written to " + sibling);
    }

    @Test
    void ordinaryArchivesStillExtract(@TempDir Path tmp) throws IOException {
        Path dest = tmp.resolve("dest");
        File zip = tmp.resolve("plain.zip").toFile();

        // Includes a nested entry whose directory is never declared: the extractor
        // has to create the parent itself, which the containment fix also had to
        // keep working.
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            out.putNextEntry(new ZipEntry("top.txt"));
            out.write("one".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            out.putNextEntry(new ZipEntry("nested/deep/leaf.txt"));
            out.write("two".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }

        new UnzipUtility().unzip(zip.getAbsolutePath(), dest.toString());

        assertEquals("one", new String(Files.readAllBytes(dest.resolve("top.txt")),
                StandardCharsets.UTF_8));
        assertEquals("two", new String(
                Files.readAllBytes(dest.resolve("nested").resolve("deep").resolve("leaf.txt")),
                StandardCharsets.UTF_8));
    }
}
