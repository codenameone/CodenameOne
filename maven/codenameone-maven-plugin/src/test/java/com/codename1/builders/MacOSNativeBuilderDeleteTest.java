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
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The bundle collector removes a stale output tree before writing a new one, and
 * refuses to descend through symlinks because a signed .app is full of them.
 *
 * <p>Telling a link from an ordinary directory used to be done by comparing the
 * canonical path with the absolute one, which answers a different question: "is
 * ANY component of this path a link". On macOS that is true of nearly every path
 * a build touches, because /var and /tmp are themselves links into /private. So
 * a staging directory under /var/folders was classified as a symlink, the
 * recursion declined to empty it, and deleting the directory it had just refused
 * to empty failed and aborted the build -- on every retried or incremental build
 * that reused a non-empty output directory.</p>
 */
class MacOSNativeBuilderDeleteTest {

    /// A tree reached through a symlinked ANCESTOR is still an ordinary tree and
    /// must be deleted. This is the macOS temp-directory shape, built explicitly
    /// so the test means the same thing on a platform where /tmp is real.
    @Test
    void aTreeUnderASymlinkedAncestorIsStillDeleted(@TempDir Path tmp) throws IOException {
        File real = new File(tmp.toFile(), "real");
        assertTrue(new File(real, "stage/nested").mkdirs());
        Files.write(new File(real, "stage/nested/file.txt").toPath(), new byte[] {1, 2, 3});

        File link = new File(tmp.toFile(), "link");
        assumeTrue(createSymlink(link, real), "the platform allows creating symlinks");

        File throughLink = new File(link, "stage");
        MacOSNativeBuilder.deleteRecursively(throughLink);

        assertFalse(throughLink.exists(), "the staging tree must be gone");
        assertFalse(new File(real, "stage").exists(), "and gone through its real path too");
        assertTrue(real.isDirectory(), "without taking the directory that held it");
    }

    /// The guard the whole thing exists for: a symlink is removed, and what it
    /// points at is left alone. A signed bundle carries these by the dozen, and
    /// descending one deletes outside the tree being removed.
    @Test
    void aSymlinkIsUnlinkedRatherThanFollowed(@TempDir Path tmp) throws IOException {
        File outside = new File(tmp.toFile(), "outside");
        assertTrue(outside.mkdirs());
        File keep = new File(outside, "keep.txt");
        Files.write(keep.toPath(), new byte[] {9});

        File bundle = new File(tmp.toFile(), "bundle");
        assertTrue(bundle.mkdirs());
        File link = new File(bundle, "Current");
        assumeTrue(createSymlink(link, outside), "the platform allows creating symlinks");

        MacOSNativeBuilder.deleteRecursively(bundle);

        assertFalse(bundle.exists(), "the bundle is removed");
        assertTrue(keep.isFile(), "what the link pointed at is untouched");
    }

    private static boolean createSymlink(File link, File target) {
        try {
            Files.createSymbolicLink(link.toPath(), target.toPath());
            return true;
        } catch (IOException | UnsupportedOperationException unsupported) {
            return false;
        }
    }
}
