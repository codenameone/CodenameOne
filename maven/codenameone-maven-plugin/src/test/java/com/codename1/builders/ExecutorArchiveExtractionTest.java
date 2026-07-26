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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
