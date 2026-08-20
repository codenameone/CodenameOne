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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A database is more than its file, and the order the pieces go in decides what a failed delete
 * means. Removing the database first and then failing on a companion reported a failure over a
 * database that was already gone: the caller is told to retry something that cannot be retried,
 * and reasonably reads the error as its data being intact.
 */
public class JavaSEPortDatabaseDeleteOrderTest {

    private JavaSEPort originalInstance;

    private boolean originalExposeFilesystem;

    private JavaSEPort port;

    @BeforeEach
    public void setUp() {
        // The constructor overwrites the global instance, as the other port tests here note.
        originalInstance = JavaSEPort.instance;
        originalExposeFilesystem = JavaSEPort.isExposeFilesystem();
        port = new JavaSEPort();
        // With the filesystem exposed a name carrying a separator is used as the path itself, so
        // these run against a temporary directory rather than the simulator's storage.
        JavaSEPort.setExposeFilesystem(true);
    }

    @AfterEach
    public void tearDown() {
        JavaSEPort.instance = originalInstance;
        JavaSEPort.setExposeFilesystem(originalExposeFilesystem);
    }

    @Test
    public void aDeleteThatFailsOnACompanionLeavesTheDatabase(@TempDir Path dir) throws IOException {
        File database = write(new File(dir.toFile(), "ordering.db"));
        // A companion that cannot be removed. A non-empty directory under the journal's name is
        // the portable way to make File.delete() fail without touching permissions, and what it
        // stands in for is the real thing: a companion held open, or on a filesystem that refuses.
        File journal = new File(dir.toFile(), "ordering.db-journal");
        assertTrue(journal.mkdirs());
        write(new File(journal, "blocker"));

        assertThrows(IOException.class, () -> port.deleteDB(database.getAbsolutePath()),
                "a companion that cannot be removed has to be reported");
        assertTrue(database.exists(),
                "the database must still be there, because the caller was told the delete failed");
    }

    @Test
    public void anOrdinaryDeleteTakesTheDatabaseAndItsCompanions(@TempDir Path dir)
            throws IOException {
        File database = write(new File(dir.toFile(), "clean.db"));
        File journal = write(new File(dir.toFile(), "clean.db-journal"));
        File wal = write(new File(dir.toFile(), "clean.db-wal"));

        port.deleteDB(database.getAbsolutePath());

        assertTrue(!database.exists() && !journal.exists() && !wal.exists(),
                "a delete that reports success leaves nothing of the database behind");
    }

    private static File write(File f) throws IOException {
        OutputStream out = new FileOutputStream(f);
        try {
            out.write(new byte[] {1, 2, 3});
        } finally {
            out.close();
        }
        return f;
    }
}
