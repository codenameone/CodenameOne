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
package com.codename1.designer.css;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for two long-standing logic bugs in CN1CSSCLI, surfaced by review
 * when the class moved into this module. Both predate the move: the file was relocated
 * with a zero-line content diff.
 */
class CN1CSSCLILogicTest {

    private static Object invoke(String name, Class<?>[] types, Object... args) throws Exception {
        Method m = CN1CSSCLI.class.getDeclaredMethod(name, types);
        m.setAccessible(true);
        return m.invoke(null, args);
    }

    /**
     * getMergedFile() read the cn1.cssMergeFile override into a bare expression and
     * discarded it, so callers setting the property silently got the derived path.
     */
    @Test
    void honoursTheCssMergeFileOverride() throws Exception {
        String previous = System.getProperty("cn1.cssMergeFile");
        try {
            System.setProperty("cn1.cssMergeFile", "/tmp/explicit-merge-target.css");
            Object result = invoke("getMergedFile", new Class<?>[]{String.class}, "/somewhere/theme.css");
            assertEquals("/tmp/explicit-merge-target.css", result,
                    "cn1.cssMergeFile must win over the path derived from the input");
        } finally {
            if (previous == null) {
                System.clearProperty("cn1.cssMergeFile");
            } else {
                System.setProperty("cn1.cssMergeFile", previous);
            }
        }
    }

    /**
     * contains() recursed with directory2 as the first argument, dropping directory1 from
     * the comparison after one level -- so it only ever detected a direct parent, and the
     * copy-into-itself guard that uses it could be walked straight past.
     */
    @Test
    void detectsContainmentBeyondADirectParent(@TempDir Path tempDir) throws Exception {
        File root = Files.createDirectories(tempDir.resolve("root")).toFile();
        File child = Files.createDirectories(tempDir.resolve("root/a")).toFile();
        File grandchild = Files.createDirectories(tempDir.resolve("root/a/b")).toFile();
        File greatGrandchild = Files.createDirectories(tempDir.resolve("root/a/b/c")).toFile();
        File sibling = Files.createDirectories(tempDir.resolve("elsewhere")).toFile();

        Class<?>[] sig = {File.class, File.class};
        assertTrue((Boolean) invoke("contains", sig, root, child), "direct parent");
        assertTrue((Boolean) invoke("contains", sig, root, grandchild), "two levels down");
        assertTrue((Boolean) invoke("contains", sig, root, greatGrandchild), "three levels down");
        assertFalse((Boolean) invoke("contains", sig, root, sibling), "unrelated directory");
        assertFalse((Boolean) invoke("contains", sig, child, root), "containment is not symmetric");
    }
}
