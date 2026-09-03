/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ai.whisper;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AndroidWhisperAarProjectTest {

    /**
     * Minimum {@code p_align} Google Play requires of a {@code PT_LOAD}
     * segment in a 64-bit shared library.
     */
    private static final long REQUIRED_PAGE_ALIGNMENT = 0x4000L;

    private static final int ELFCLASS64 = 2;
    private static final int PT_LOAD = 1;

    @Test
    void androidJniBridgeExportsTimedSegmentsForAllAndroidAbis() throws Exception {
        String cpp = read("../android-aar/cn1-ai-whisper-android/src/main/cpp/native_whisper_jni.cpp");
        assertTrue(cpp.contains("NativeWhisperRecognizerImpl_nativeTranscribeSegments"), cpp);
        assertTrue(cpp.contains("whisper_full_get_segment_t0"), cpp);
        assertTrue(cpp.contains("whisper_full_get_segment_t1"), cpp);
        assertTrue(cpp.contains("whisper_init_from_file_with_params"), cpp);

        String gradle = read("../android-aar/cn1-ai-whisper-android/build.gradle");
        assertTrue(gradle.contains("abiFilters 'armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64'"), gradle);
        assertTrue(gradle.contains("ndkVersion '28.2.13676358'"), gradle);
    }

    /**
     * The NDK is what supplies 16 KB aligned {@code libc++_shared.so} and
     * {@code libomp.so}: those are prebuilts copied into the AAR, so no linker
     * flag set here can realign them and only r28 or newer is compliant. The
     * CMake flags cover the one library this project links itself, for a
     * standalone CMake run or an older NDK.
     */
    @Test
    void nativeBuildIsConfiguredForSixteenKilobytePages() throws Exception {
        String gradle = read("../android-aar/cn1-ai-whisper-android/build.gradle");
        assertTrue(gradle.contains("ndkVersion '28.2.13676358'"), gradle);
        // 21 is what the native code always required: the NDK floor has been
        // API 21 since r26, so the old `minSdk 19` was a manifest claim the
        // .so files never honoured.
        assertTrue(gradle.contains("minSdk 21"), gradle);

        String cmake = read("../android-aar/cn1-ai-whisper-android/src/main/cpp/CMakeLists.txt");
        assertTrue(cmake.contains("-Wl,-z,max-page-size=16384"), cmake);
        assertTrue(cmake.contains("-Wl,-z,common-page-size=16384"), cmake);
    }

    @Test
    void stagedAndroidAarContainsJniSlicesWhenPresent() throws Exception {
        File aar = new File("../android/src/main/resources/cn1-ai-whisper-android.aar");
        if (!aar.isFile()) {
            return;
        }
        ZipFile zip = new ZipFile(aar);
        try {
            assertNotNull(zip.getEntry("jni/armeabi-v7a/libcn1aiwhisper.so"));
            assertNotNull(zip.getEntry("jni/arm64-v8a/libcn1aiwhisper.so"));
            assertNotNull(zip.getEntry("jni/x86/libcn1aiwhisper.so"));
            assertNotNull(zip.getEntry("jni/x86_64/libcn1aiwhisper.so"));
        } finally {
            zip.close();
        }
    }

    /**
     * Every 64-bit library in the staged AAR has to be linked for 16 KB memory
     * pages, which Google Play requires of API 35+ uploads since 2025-11-01 and
     * of Wear OS uploads from 2026-09-15. Alignment is fixed at link time, so
     * an application cannot repair a Codename One artifact that got it wrong;
     * the app simply builds, installs on the 4 KB devices the developer owns,
     * and is rejected or fails to load on a 16 KB one.
     *
     * <p>32-bit slices are deliberately not checked. 16 KB pages are a 64-bit
     * feature and {@code armeabi-v7a}/{@code x86} never run on such a device.
     * The same rule is enforced across the whole tree by
     * {@code scripts/check-16k-page-alignment.py}.</p>
     */
    @Test
    void stagedAndroidAarIsSixteenKilobytePageAlignedWhenPresent() throws Exception {
        File aar = new File("../android/src/main/resources/cn1-ai-whisper-android.aar");
        if (!aar.isFile()) {
            return;
        }
        List<String> misaligned = new ArrayList<String>();
        int checked = 0;
        ZipFile zip = new ZipFile(aar);
        try {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".so")) {
                    continue;
                }
                byte[] elf = readFully(zip, entry);
                long[] alignments = loadSegmentAlignments(elf);
                if (alignments == null) {
                    // A 32-bit slice, which the rule does not cover.
                    continue;
                }
                checked++;
                for (long alignment : alignments) {
                    if (alignment < REQUIRED_PAGE_ALIGNMENT) {
                        misaligned.add(entry.getName() + " p_align 0x"
                                + Long.toHexString(alignment));
                        break;
                    }
                }
            }
        } finally {
            zip.close();
        }
        assertTrue(checked > 0,
                "No 64-bit libraries found in the AAR; the arm64-v8a and x86_64 "
                        + "slices are required for a Play upload");
        assertTrue(misaligned.isEmpty(),
                "Rebuild the AAR with NDK r28 or newer -- these 64-bit libraries "
                        + "are not aligned to 0x4000: " + misaligned);
    }

    /**
     * Reads the {@code p_align} of every {@code PT_LOAD} program header, or
     * returns null when the bytes are not a 64-bit ELF.
     */
    private static long[] loadSegmentAlignments(byte[] elf) {
        if (elf.length < 64 || elf[0] != 0x7f || elf[1] != 'E' || elf[2] != 'L' || elf[3] != 'F') {
            return null;
        }
        if ((elf[4] & 0xff) != ELFCLASS64) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(elf);
        buffer.order((elf[5] & 0xff) == 1 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        long phoff = buffer.getLong(0x20);
        int phentsize = buffer.getShort(0x36) & 0xffff;
        int phnum = buffer.getShort(0x38) & 0xffff;
        assertTrue(phentsize >= 56 && phnum > 0
                        && phoff + (long) phnum * phentsize <= elf.length,
                "Malformed ELF program header table");
        List<Long> alignments = new ArrayList<Long>();
        for (int i = 0; i < phnum; i++) {
            int offset = (int) (phoff + (long) i * phentsize);
            if (buffer.getInt(offset) != PT_LOAD) {
                continue;
            }
            alignments.add(Long.valueOf(buffer.getLong(offset + 0x30)));
        }
        assertTrue(!alignments.isEmpty(), "ELF has no PT_LOAD segments");
        long[] result = new long[alignments.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = alignments.get(i).longValue();
        }
        return result;
    }

    private static byte[] readFully(ZipFile zip, ZipEntry entry) throws Exception {
        InputStream input = zip.getInputStream(entry);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = input.read(chunk)) > 0) {
                out.write(chunk, 0, read);
            }
            return out.toByteArray();
        } finally {
            input.close();
        }
    }

    private static String read(String path) throws Exception {
        File file = new File(path);
        assertTrue(file.isFile(), "Missing " + file.getAbsolutePath());
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
