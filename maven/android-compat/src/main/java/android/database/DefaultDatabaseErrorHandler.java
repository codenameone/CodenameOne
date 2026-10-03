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
package android.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/// The default corruption handler. Android deletes the database file; the
/// Codename One storage layer owns the file, so this logs the corruption and
/// closes the database instead, leaving the decision to the application.
public final class DefaultDatabaseErrorHandler implements DatabaseErrorHandler {

    @Override
    public void onCorruption(SQLiteDatabase dbObj) {
        Log.e("DefaultDatabaseErrorHandler", "Corruption reported by sqlite on database: " + dbObj.getPath());
        if (dbObj.isOpen()) {
            dbObj.close();
        }
    }
}
