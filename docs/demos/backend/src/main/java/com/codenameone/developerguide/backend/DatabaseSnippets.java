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
package com.codenameone.developerguide.backend;

import com.codename1.backend.DataSource;
import com.codename1.backend.Database;
import com.codename1.backend.sql.Dialect;
import java.io.IOException;
import java.util.List;

/** The Backend chapter's database examples, compiled so they cannot drift. */
public final class DatabaseSnippets {

    private DatabaseSnippets() {
    }

    public static List open() throws IOException {
// tag::backend-database[]
DataSource db = DataSource.open(System.getenv("DATABASE_URL"));   // or ":memory:"

List rows = db.query("SELECT id, body FROM note WHERE id > ?",
                     new Object[] { Integer.valueOf(10) });
// end::backend-database[]
        return rows;
    }

    public static long insert(DataSource db) throws IOException {
// tag::backend-database-insert[]
long id = db.insert("INSERT INTO note (body) VALUES (?)",
                    new Object[] { "first" }, "id");
// end::backend-database-insert[]
        return id;
    }

    public static void transaction(DataSource db) throws Exception {
// tag::backend-database-transaction[]
db.inTransaction(new DataSource.Work() {
    public Object run(Database connection) throws Exception {
        connection.execute("UPDATE account SET balance = balance - ? WHERE id = ?",
                           new Object[] { Integer.valueOf(100), Integer.valueOf(1) });
        connection.execute("UPDATE account SET balance = balance + ? WHERE id = ?",
                           new Object[] { Integer.valueOf(100), Integer.valueOf(2) });
        return null;
    }
});
// end::backend-database-transaction[]
    }

    public static String schema(DataSource db) {
// tag::backend-database-dialect[]
Dialect dialect = db.dialect();
String create = "CREATE TABLE IF NOT EXISTS " + dialect.quote("note") + " ("
        + dialect.quote("id") + " " + dialect.generatedKeyColumn(Dialect.BIGINT) + ", "
        + dialect.quote("body") + " " + dialect.columnType(Dialect.TEXT) + ")";
// end::backend-database-dialect[]
        return create;
    }
}
