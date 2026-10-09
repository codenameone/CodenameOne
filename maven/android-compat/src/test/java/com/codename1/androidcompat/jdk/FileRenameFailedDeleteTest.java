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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// A move between directories copies and deletes. When the source cannot be
/// deleted the rename used to report success with the file at both paths;
/// it now fails and takes back the copy it made.
public class FileRenameFailedDeleteTest {

    private static final String SRC = "file:///data/a/src.txt";
    private static final String DST = "file:///data/b/dst.txt";

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @After
    public void reset() {
        HeadlessImplementation.fileSystem = false;
        HeadlessImplementation.UNDELETABLE.clear();
        HeadlessImplementation.FILES.remove(SRC);
        HeadlessImplementation.FILES.remove(DST);
    }

    private static void prepare() {
        AndroidTestSupport.context();
        HeadlessImplementation.fileSystem = true;
        HeadlessImplementation.FILES.put(SRC, new byte[]{1, 2, 3});
    }

    @Test
    public void aSourceThatCannotBeDeletedFailsTheRename() {
        prepare();
        HeadlessImplementation.UNDELETABLE.add(SRC);
        assertFalse(new File(SRC).renameTo(new File(DST)));
        assertTrue(HeadlessImplementation.FILES.containsKey(SRC));
        assertFalse("the copy is taken back", HeadlessImplementation.FILES.containsKey(DST));
    }

    @Test
    public void aMoveBetweenDirectoriesSucceeds() {
        prepare();
        assertTrue(new File(SRC).renameTo(new File(DST)));
        assertFalse(HeadlessImplementation.FILES.containsKey(SRC));
        assertArrayEquals(new byte[]{1, 2, 3}, HeadlessImplementation.FILES.get(DST));
    }
}
