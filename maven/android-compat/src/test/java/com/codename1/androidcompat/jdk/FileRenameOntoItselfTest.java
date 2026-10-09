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
package com.codename1.androidcompat.jdk;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.HeadlessImplementation;
import com.codename1.androidcompat.testing.MainThreadRule;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/// Renaming a file onto itself used to take the copy path, which opened the
/// same storage path for reading and writing -- truncating it -- and then
/// deleted it. It now succeeds and leaves the file alone.
public class FileRenameOntoItselfTest {

    private static final String SRC = "file:///data/a/src.txt";

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @After
    public void reset() {
        HeadlessImplementation.fileSystem = false;
        HeadlessImplementation.FILES.remove(SRC);
    }

    private static void prepare() {
        AndroidTestSupport.context();
        HeadlessImplementation.fileSystem = true;
        HeadlessImplementation.FILES.put(SRC, new byte[]{1, 2, 3});
    }

    @Test
    public void theSamePathKeepsTheFile() {
        prepare();
        assertTrue(new File(SRC).renameTo(new File(SRC)));
        assertArrayEquals(new byte[]{1, 2, 3}, HeadlessImplementation.FILES.get(SRC));
    }

    @Test
    public void aDifferentSpellingOfTheSamePathKeepsTheFile() {
        prepare();
        assertTrue(new File(SRC).renameTo(new File("file:///data/b/../a/src.txt")));
        assertArrayEquals(new byte[]{1, 2, 3}, HeadlessImplementation.FILES.get(SRC));
    }
}
