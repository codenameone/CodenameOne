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
package android.database.sqlite;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.DatabaseErrorHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/// A context whose databases are JDBC SQLite files in a temporary
/// directory, so a database outlives the helper that opened it, for tests
/// outside this package (Room's).
public final class JdbcContexts {

    private JdbcContexts() {
    }

    public static Context create() {
        final File dir;
        try {
            dir = Files.createTempDirectory("room-test").toFile();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        dir.deleteOnExit();
        return new ContextWrapper(null) {
            @Override
            public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory,
                                                       DatabaseErrorHandler errorHandler) {
                File f = new File(dir, name);
                f.deleteOnExit();
                try {
                    return SQLiteDatabase.wrap(new JdbcDatabase("jdbc:sqlite:" + f.getAbsolutePath()), name,
                            factory, errorHandler);
                } catch (IOException e) {
                    throw new SQLiteException(e.getMessage());
                }
            }

            @Override
            public boolean deleteDatabase(String name) {
                return new File(dir, name).delete();
            }
        };
    }
}
