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

    /**
     * Every nested entry is charged, not only the ones extracted.
     *
     * <p>The budget bounded bytes long before it bounded entries, and an entry
     * that is drained rather than kept carries no data at all -- so a nested
     * archive of empty or directory entries cost nothing and could be repeated
     * without limit, while its ZIP headers compressed to almost nothing inside
     * the enclosing archive. The byte budgets cannot see that case, which is
     * why the count is asserted on its own here.</p>
     */
    @Test
    void chargesEveryEntryAgainstTheCountBudget() throws Exception {
        Executor.PermScanBudget budget = new Executor.PermScanBudget();
        for (int i = 0; i < Executor.PERM_SCAN_MAX_ENTRIES; i++) {
            budget.entry("entry" + i + "/");
        }

        IOException tooMany = assertThrows(IOException.class,
                () -> budget.entry("one-past-the-budget/"));
        assertTrue(tooMany.getMessage().contains("refusing to keep scanning"),
                "the message should say why it stopped, got: " + tooMany.getMessage());
    }

    /**
     * A drained entry is charged exactly like an extracted one. Counting only
     * what reaches disk is the shape of the original bug.
     */
    @Test
    void aDrainedEntryCostsTheSameAsAnExtractedOne() throws Exception {
        Executor.PermScanBudget drained = new Executor.PermScanBudget();
        Executor.PermScanBudget extracted = new Executor.PermScanBudget();
        for (int i = 0; i < Executor.PERM_SCAN_MAX_ENTRIES; i++) {
            drained.entry("directory" + i + "/");
            extracted.entry("Class" + i + ".class");
        }

        assertThrows(IOException.class, () -> drained.entry("next/"));
        assertThrows(IOException.class, () -> extracted.entry("Next.class"));
    }

    /**
     * The real nested-scan loop refuses an archive made of empty entries.
     *
     * <p>This is the assertion that constrains the fix rather than the helper:
     * charging the budget is only useful if the loop does the charging, and a
     * test that calls {@code entry()} itself would still pass with the call
     * removed. The archive here carries no class data at all, so every byte
     * budget stays untouched and only the entry count can stop it.</p>
     */
    @Test
    void aNestedArchiveOfEmptyEntriesIsRefused() throws Exception {
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(raw);
        for (int i = 0; i < Executor.PERM_SCAN_MAX_ENTRIES + 10; i++) {
            zos.putNextEntry(new ZipEntry("d" + i + "/"));
            zos.closeEntry();
        }
        zos.close();

        File scratch = temporaryDirectory.resolve("nested-scan").toFile();
        assertTrue(scratch.mkdirs());

        IOException refused = assertThrows(IOException.class,
                () -> Executor.extractNestedClassesForPermissions(
                        new ByteArrayInputStream(raw.toByteArray()), scratch,
                        new Executor.PermScanBudget()));
        assertTrue(refused.getMessage().contains("refusing to keep scanning"),
                "expected the entry-count refusal, got: " + refused.getMessage());
    }

    /**
     * The outer archive is bounded too, not just its nested jars.
     *
     * <p>Skipping an entry in the outer loop is cheap, because ZipFile is
     * random access -- but cheap per entry is not the same as unbounded, and
     * the outer central directory comes from the same untrusted upload. The
     * first fix charged only the nested loop and left this one free to run past
     * the advertised limit.</p>
     */
    @Test
    void anOuterArchiveOfDirectoryEntriesIsRefused() throws Exception {
        File archive = temporaryDirectory.resolve("wide.jar").toFile();
        ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(archive.toPath()));
        for (int i = 0; i < Executor.PERM_SCAN_MAX_ENTRIES + 10; i++) {
            zos.putNextEntry(new ZipEntry("d" + i + "/"));
            zos.closeEntry();
        }
        zos.close();

        File scratch = temporaryDirectory.resolve("outer-scan").toFile();
        assertTrue(scratch.mkdirs());

        java.util.zip.ZipFile zip = new java.util.zip.ZipFile(archive);
        try {
            IOException refused = assertThrows(IOException.class,
                    () -> Executor.scanArchiveEntriesForPermissions(
                            zip, scratch, archive.getName(), new StringBuilder()));
            assertTrue(refused.getMessage().contains("refusing to keep scanning"),
                    "expected the entry-count refusal, got: " + refused.getMessage());
        } finally {
            zip.close();
        }
    }

    /**
     * Entry headers are charged, not only entry payloads.
     *
     * <p>ZipInputStream.getNextEntry() inflates and parses each local header --
     * the name included, and a name may run to 64KB -- before any per-entry
     * budget can charge for it. An archive of directory entries with very long
     * names therefore costs gigabytes of work while spending no payload bytes
     * and, on its own, staying inside the entry count. The stream bound is what
     * closes that, so the archive here is built entirely out of metadata.</p>
     */
    @Test
    void nestedEntryHeadersAreBoundedToo() throws Exception {
        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
            longName.append('n');
        }

        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(raw);
        for (int i = 0; i < 200; i++) {
            zos.putNextEntry(new ZipEntry(longName + "-" + i + "/"));
            zos.closeEntry();
        }
        zos.close();

        File scratch = temporaryDirectory.resolve("header-scan").toFile();
        assertTrue(scratch.mkdirs());

        IOException refused = assertThrows(IOException.class,
                () -> Executor.extractNestedClassesForPermissions(
                        new ByteArrayInputStream(raw.toByteArray()), scratch,
                        new Executor.PermScanBudget(4096L)));
        assertTrue(refused.getMessage().contains("refusing to keep reading"),
                "expected the stream bound to stop it, got: " + refused.getMessage());
    }

    /**
     * The bound counts every route bytes can leave the stream by. skip() is the
     * one that matters here: ZipInputStream uses it to step over entry data.
     */
    @Test
    void theStreamBoundChargesReadsAndSkipsAlike() throws Exception {
        byte[] payload = new byte[64];

        Executor.BoundedInputStream viaArray = new Executor.BoundedInputStream(
                new ByteArrayInputStream(payload), new Executor.PermScanBudget(16L));
        assertThrows(IOException.class, () -> viaArray.read(new byte[64], 0, 64));

        Executor.BoundedInputStream viaSkip = new Executor.BoundedInputStream(
                new ByteArrayInputStream(payload), new Executor.PermScanBudget(16L));
        assertThrows(IOException.class, () -> viaSkip.skip(64));

        Executor.BoundedInputStream viaSingle = new Executor.BoundedInputStream(
                new ByteArrayInputStream(payload), new Executor.PermScanBudget(4L));
        assertThrows(IOException.class, () -> {
            for (int i = 0; i < 64; i++) {
                viaSingle.read();
            }
        });
    }

    /**
     * One allowance across every nested archive, not one per archive.
     *
     * <p>A per-stream bound resets on each nested jar, so an aar that spreads
     * its metadata over many of them paid the limit once per jar and the total
     * went unbounded again. The allowance here is set to the whole archive's
     * length, which a single pass never reaches -- ZipInputStream stops at the
     * last local header without reading the central directory -- so a per-jar
     * bound would let this loop run forever. Only a shared one stops it.</p>
     */
    @Test
    void theStreamAllowanceIsSharedAcrossNestedArchives() throws Exception {
        byte[] archive = nestedArchiveOfDirectoryEntries(60);
        File scratch = temporaryDirectory.resolve("shared-allowance").toFile();
        assertTrue(scratch.mkdirs());

        Executor.PermScanBudget budget = new Executor.PermScanBudget(archive.length);

        int passes = 0;
        IOException refused = null;
        for (int i = 0; i < 50 && refused == null; i++) {
            try {
                Executor.extractNestedClassesForPermissions(
                        new ByteArrayInputStream(archive), scratch, budget);
                passes++;
            } catch (IOException ex) {
                refused = ex;
            }
        }

        assertTrue(passes >= 1, "one archive alone must fit inside the allowance");
        assertTrue(refused != null,
                "repeated nested archives must exhaust the shared allowance");
        assertTrue(refused.getMessage().contains("refusing to keep reading"),
                "expected the stream allowance to stop it, got: " + refused.getMessage());
    }

    private static byte[] nestedArchiveOfDirectoryEntries(int count) throws IOException {
        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            longName.append('n');
        }
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(raw);
        for (int i = 0; i < count; i++) {
            zos.putNextEntry(new ZipEntry(longName + "-" + i + "/"));
            zos.closeEntry();
        }
        zos.close();
        return raw.toByteArray();
    }
}
