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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutorArchiveExtractionTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void resolvesNestedEntryInsideDestination() throws Exception {
        File destination = temporaryDirectory.toFile();

        File resolved = Executor.resolveArchiveEntry(destination,
                "nested/model.tflite");

        assertEquals(new File(destination, "nested/model.tflite")
                .getCanonicalFile(), resolved);
    }

    @Test
    void rejectsParentTraversal() {
        File destination = temporaryDirectory.resolve("archive").toFile();

        assertThrows(IOException.class, () -> Executor.resolveArchiveEntry(
                destination, "../escaped.txt"));
    }

    @Test
    void rejectsAbsoluteDestination() {
        File destination = temporaryDirectory.resolve("archive").toFile();
        String absoluteEntry = temporaryDirectory.resolve("escaped.txt")
                .toAbsolutePath().toString();

        assertThrows(IOException.class, () -> Executor.resolveArchiveEntry(
                destination, absoluteEntry));
    }

    @Test
    void rejectsSiblingWithMatchingPrefix() {
        File destination = temporaryDirectory.resolve("archive").toFile();

        assertThrows(IOException.class, () -> Executor.resolveArchiveEntry(
                destination, "../archive-escape/payload.bin"));
    }

    @Test
    void extractionFilterMayRouteSafeEntryToSibling() throws Exception {
        File project = temporaryDirectory.resolve("project").toFile();
        File source = new File(project, "src");
        File settings = new File(project,
                "codenameone_settings.properties");
        source.mkdirs();

        new IPhoneBuilder().extractZip(
                new ByteArrayInputStream(zipEntry(
                        "codenameone_settings.properties",
                        "codename1.mainName=MyApp")),
                source, (path, fileName) -> settings);

        assertEquals("codename1.mainName=MyApp",
                new String(Files.readAllBytes(settings.toPath()),
                        StandardCharsets.UTF_8));
    }

    @Test
    void rejectsTraversalBeforeArchiveSpecificRouting() throws Exception {
        assertSpecialEntryRejected("html/../../payload", "html.tar");
        assertSpecialEntryRejected("javase.lib/../../payload",
                "javase.lib.tar");
    }

    @Test
    void rejectsTraversalInStrippedTarMemberName() throws Exception {
        assertSpecialEntryRejected("html/../payload", "html.tar");
        assertSpecialEntryRejected("podspecs/../payload", "podspecs.tar");
        assertSpecialEntryRejected("javase.lib/../payload",
                "javase.lib.tar");
        assertSpecialEntryRejected("/html/../payload", "html.tar");
        assertSpecialEntryRejected("/podspecs/../payload", "podspecs.tar");
        assertSpecialEntryRejected("/javase.lib/../payload",
                "javase.lib.tar");
    }

    @Test
    void preservesSupportedLeadingSlashVirtualEntries() throws Exception {
        assertSpecialEntryAccepted("/html/index.html", "html.tar");
        assertSpecialEntryAccepted("/podspecs/library.podspec",
                "podspecs.tar");
        assertSpecialEntryAccepted("/javase.lib/library.jar",
                "javase.lib.tar");
    }

    private static byte[] zipEntry(String name, String contents)
            throws IOException {
        byte[] payload = contents.getBytes(StandardCharsets.UTF_8);
        CRC32 checksum = new CRC32();
        checksum.update(payload);
        ZipEntry entry = new ZipEntry(name);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(payload.length);
        entry.setCompressedSize(payload.length);
        entry.setCrc(checksum.getValue());

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(bytes);
        zip.putNextEntry(entry);
        zip.write(payload);
        zip.closeEntry();
        zip.close();
        return bytes.toByteArray();
    }

    private void assertSpecialEntryRejected(String entryName,
            String generatedArchive) throws Exception {
        File root = specialRoot(entryName);
        File resources = unzipSpecialEntry(root, entryName);

        assertFalse(new File(resources, generatedArchive).exists(),
                "Unsafe entry must not create " + generatedArchive);
    }

    private void assertSpecialEntryAccepted(String entryName,
            String generatedArchive) throws Exception {
        File root = specialRoot(entryName);
        File resources = unzipSpecialEntry(root, entryName);

        assertTrue(new File(resources, generatedArchive).isFile(),
                "Supported entry must create " + generatedArchive);
    }

    private File specialRoot(String entryName) {
        return temporaryDirectory.resolve("special-"
                + Math.abs(entryName.hashCode())).toFile();
    }

    private static File unzipSpecialEntry(File root, String entryName)
            throws Exception {
        File classes = new File(root, "classes");
        File resources = new File(root, "resources");
        File sources = new File(root, "sources");
        classes.mkdirs();
        resources.mkdirs();
        sources.mkdirs();

        new IPhoneBuilder().unzip(
                new ByteArrayInputStream(zipEntry(entryName, "payload")),
                classes, resources, sources);
        return resources;
    }
}
