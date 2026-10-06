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
package android.content;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// The private-file methods take a bare name. A backslash is a separator to
/// the remapped `java.io.File`, so it is refused like `/`; it used to be let
/// through and resolve outside the files directory.
public class ContextFileNameTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void backslashIsRefusedLikeASlash() throws Exception {
        final java.io.File files = java.nio.file.Files.createTempDirectory("ctxfiles").toFile();
        Context ctx = new ContextWrapper(AndroidTestSupport.context()) {
            @Override
            public java.io.File getFilesDir() {
                return files;
            }
        };
        String[] bad = {"../prefs", "..\\shared_prefs\\prefs", "a\\b"};
        for (String name : bad) {
            try {
                ctx.getFileStreamPath(name);
                fail("accepted " + name);
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().indexOf("path separator") >= 0);
            }
            try {
                ctx.deleteFile(name);
                fail("deleteFile accepted " + name);
            } catch (IllegalArgumentException expected) {
                // refused
            }
        }
    }

    /// A database name follows the same rule: the JavaSE simulator reads a
    /// name holding a backslash as a file system path, so it used to open a
    /// database outside the database directory.
    @Test
    public void backslashIsRefusedInDatabaseNames() {
        Context ctx = AndroidTestSupport.context();
        String[] bad = {"../outside.db", "..\\outside.db", "a\\b.db"};
        for (String name : bad) {
            try {
                ctx.openOrCreateDatabase(name, Context.MODE_PRIVATE, null);
                fail("openOrCreateDatabase accepted " + name);
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().indexOf("path separator") >= 0);
            }
            try {
                ctx.deleteDatabase(name);
                fail("deleteDatabase accepted " + name);
            } catch (IllegalArgumentException expected) {
                // refused
            }
        }
    }
}
