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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.LinkedHashMap;
import java.util.Map;

import com.codename1.compat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;

public class SQLiteQueryBuilderTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void buildsTheSameStringAsAndroid() {
        assertEquals("SELECT DISTINCT a, b FROM t WHERE x=? GROUP BY a HAVING count(*)>1 ORDER BY b LIMIT 5",
                SQLiteQueryBuilder.buildQueryString(true, "t", new String[] {"a", "b"}, "x=?", "a", "count(*)>1",
                        "b", "5"));
        assertEquals("SELECT * FROM t", SQLiteQueryBuilder.buildQueryString(false, "t", null, null, null, null,
                null, null));
        assertEquals("SELECT * FROM t LIMIT 10, 5", SQLiteQueryBuilder.buildQueryString(false, "t", null, "", null,
                null, null, "10, 5"));
    }

    @Test
    public void rejectsHavingWithoutGroupByAndBadLimits() {
        try {
            SQLiteQueryBuilder.buildQueryString(false, "t", null, null, null, "x", null, null);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("HAVING clauses are only permitted when using a groupBy clause", expected.getMessage());
        }
        try {
            SQLiteQueryBuilder.buildQueryString(false, "t", null, null, null, null, null, "5; DROP TABLE t");
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("invalid LIMIT clauses:5; DROP TABLE t", expected.getMessage());
        }
    }

    @Test
    public void combinesBaseWhereProjectionMapAndSelection() {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables("notes n");
        qb.appendWhere("n.deleted=0");
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("_id", "n._id AS _id");
        map.put("title", "n.title AS title");
        qb.setProjectionMap(map);
        assertEquals("SELECT n.title AS title FROM notes n WHERE (n.deleted=0) AND (title LIKE ?) ORDER BY title",
                qb.buildQuery(new String[] {"title"}, "title LIKE ?", null, null, "title", null));
        assertEquals("SELECT n._id AS _id, n.title AS title FROM notes n WHERE (n.deleted=0)",
                qb.buildQuery(null, null, null, null, null, null));
        qb.setStrict(true);
        try {
            qb.buildQuery(new String[] {"secret AS x"}, null, null, null, null, null);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("Invalid column secret AS x", expected.getMessage());
        }
    }

    @Test
    public void countsParametersOutsideLiteralsAndComments() {
        assertEquals(2, SQLiteProgram.countParameters("SELECT * FROM t WHERE a=? AND b='?' AND c=?"));
        assertEquals(1, SQLiteProgram.countParameters("SELECT \"a?\" FROM t -- ?\n WHERE x = ? /* ? */"));
        assertEquals(4, SQLiteProgram.countParameters("SELECT ?3, ?"));
        assertEquals(-1, SQLiteProgram.countParameters("SELECT * FROM t WHERE a=:name"));
        assertEquals(0, SQLiteProgram.countParameters("SELECT 'it''s ?'"));
    }

    @Test
    public void convertsTextLikeSqlite() {
        assertEquals(12, SQLiteCursor.toLong("12.9"));
        assertEquals(-3, SQLiteCursor.toLong(" -3 "));
        assertEquals(0, SQLiteCursor.toLong("abc"));
        assertEquals(42, SQLiteCursor.toLong("42abc"));
        assertEquals(1500, SQLiteCursor.toLong("1.5e3"));
        assertEquals(2.5, SQLiteCursor.toDouble("2.5xyz"), 0);
        assertEquals(0, SQLiteCursor.toDouble("."), 0);
        assertEquals(1, SQLiteCursor.numericKind("-17"));
        assertEquals(2, SQLiteCursor.numericKind("1e5"));
        assertEquals(2, SQLiteCursor.numericKind("3.25"));
        assertEquals(0, SQLiteCursor.numericKind("1e"));
        assertEquals(0, SQLiteCursor.numericKind("12 apples"));
    }
}
