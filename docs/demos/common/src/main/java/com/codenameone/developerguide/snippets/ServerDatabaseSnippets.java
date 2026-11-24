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
package com.codenameone.developerguide.snippets;

import com.codename1.db.Cursor;
import com.codename1.db.Database;
import com.codename1.db.PostgresDatabase;
import com.codename1.db.Row;
import com.codename1.io.Log;
import com.codename1.io.Preferences;
import com.codename1.ui.CN;

import java.io.IOException;

public class ServerDatabaseSnippets {
    public void listCheapItems() {
        // tag::server-database-001[]
        final String password = Preferences.get("dbPassword", "");
        CN.startThread(() -> {
            try {
                Database db = new PostgresDatabase("db.example.com", 5432, "inventory",
                        "app", password);
                try {
                    Cursor c = db.executeQuery(
                            "SELECT id, name FROM item WHERE price < ?", 10.0);
                    while (c.next()) {
                        Row r = c.getRow();
                        Log.p(r.getLong(0) + " " + r.getString(1));
                    }
                    c.close();
                } finally {
                    db.close();
                }
            } catch (IOException err) {
                Log.e(err);
            }
        }, "inventory").start();
        // end::server-database-001[]
    }
}
