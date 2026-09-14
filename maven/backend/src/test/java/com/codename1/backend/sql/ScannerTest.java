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
package com.codename1.backend.sql;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Every statement shape the scanner has to read, against every dialect.
 *
 * <p>The companion to ValuesTest, and here for the same reason: the findings in
 * this file were all one mistake wearing different clothes -- a token rule
 * applied to one dialect and not its neighbour. Dollar quoting turned off for
 * MySQL while PostgreSQL kept the same identifier problem; the double dash
 * treated as a comment everywhere while MySQL wants whitespace after it; VALUES
 * recognised while MySQL's singular VALUE was not.
 *
 * <p>Each row is a statement and what the three dialects make of it: where the
 * statement ENDS (which is where PostgreSQL's RETURNING has to go) and how many
 * rows its VALUES clause inserts (-1 when it has none to count). Where the three
 * disagree, they disagree on purpose and the row says so.
 */
class ScannerTest {

    @Test
    @DisplayName("where a statement ends, per dialect")
    void statementEnds() throws Exception {
        // Nothing after the statement: the end is its length.
        ends("INSERT INTO t (a) VALUES (?)", 28, 28, 28);
        // A terminator and a trailing comment are not the statement.
        ends("INSERT INTO t (a) VALUES (?);", 28, 28, 28);
        ends("INSERT INTO t (a) VALUES (?)  ;  ", 28, 28, 28);
        ends("INSERT INTO t (a) VALUES (?) -- why", 28, 28, 28);
        ends("INSERT INTO t (a) VALUES (?); -- why", 28, 28, 28);
        ends("INSERT INTO t (a) VALUES (?) /* why */", 28, 28, 28);
        // # is a comment on MySQL alone, so only there does the statement stop
        // before it.
        ends("INSERT INTO t (a) VALUES (?) # why", 34, 34, 28);
        // A semicolon or a double dash inside a literal ends nothing.
        ends("INSERT INTO t (a) VALUES ('x; -- y');", 36, 36, 36);
    }

    @Test
    @DisplayName("how many rows an insert writes, per dialect")
    void insertRows() throws Exception {
        rows("INSERT INTO t (a) VALUES (?)", 1, 1, 1);
        rows("INSERT INTO t (a) VALUES (?), (?)", 2, 2, 2);
        rows("INSERT INTO t (a) VALUES(?),(?)", 2, 2, 2);
        rows("INSERT INTO t (a) values (?) , (?)", 2, 2, 2);
        // MySQL's singular spelling of the same clause. The other two reject the
        // word outright, so counting a statement they will refuse costs nothing.
        rows("INSERT INTO t (a) VALUE (?), (?)", 2, 2, 2);
        // No VALUES clause to count.
        rows("INSERT INTO t (a) SELECT b FROM u", -1, -1, -1);
        rows("INSERT INTO revalues (a) SELECT 1", -1, -1, -1);
        // The word inside a literal, a comment, or a subquery is not the clause.
        rows("INSERT INTO t (a) VALUES ('VALUES (1), (2)')", 1, 1, 1);
        rows("INSERT INTO t (a) VALUES ((SELECT max(x) FROM (VALUES (1),(2)) v))", 1, 1, 1);
        // MySQL's VALUES(column) EXPRESSION is not the insert's clause, and the
        // tuple list ends before it.
        rows("INSERT INTO t (a) VALUES (?) ON DUPLICATE KEY UPDATE a = VALUES(a)", 1, 1, 1);
        rows("INSERT INTO t (a) VALUES (?), (?) ON DUPLICATE KEY UPDATE a = VALUES(a)",
                2, 2, 2);
        // "5--1" is five minus minus-one on MySQL, where a comment needs
        // whitespace after the dashes; it is a comment on the other two, which
        // swallows the second tuple.
        rows("INSERT INTO t (a) VALUES (5--1), (2)", 1, 1, 2);
        // A trailing clause after the tuples is not a tuple.
        rows("INSERT INTO t (a) VALUES (?) RETURNING id", 1, 1, 1);
    }

    @Test
    @DisplayName("identifier and comment forms only one dialect has")
    void dialectOnlyForms() throws Exception {
        // SQLite accepts [identifier]. A ? inside one is not a parameter there.
        assertEquals("SELECT [question?] FROM t WHERE id = ?",
                Dialect.SQLITE.bind("SELECT [question?] FROM t WHERE id = ?", 1));
        // And NOT on PostgreSQL, where brackets are an array subscript: a[1]
        // must survive, and the ? inside a bracket IS a parameter.
        assertEquals("SELECT a[1] FROM t WHERE id = $1",
                Dialect.POSTGRES.bind("SELECT a[1] FROM t WHERE id = ?", 1));

        // MySQL writes tuples as ROW(...) since 8.0.19, and the keyword is
        // optional: requiring the parenthesis made this "cannot tell" and the
        // multi-row insert ran.
        rows("INSERT INTO t (a) VALUES ROW(?), ROW(?)", 2, 2, 2);
        rows("INSERT INTO t (a) VALUES ROW(?)", 1, 1, 1);

        // MySQL's /*! ... */ is executable: what is inside RUNS, so the second
        // tuple here is a second row.
        rows("INSERT INTO t (a) VALUES (1) /*! , (2) */", 1, 1, 2);
        // A VERSION-GATED one is a third answer. "/*!50100 ..." runs on 5.1 and
        // up and "/*!99999 ..." on nothing, so what the statement inserts is a
        // property of the server this connection happens to reach. A count and a
        // silence would both be guesses, so the scanner says it cannot know and
        // Database refuses the insert rather than reporting one key for two rows.
        rows("INSERT INTO t (a) VALUES (1) /*!50100 , (2) */", 1, 1, Dialect.VERSION_GATED);
        rows("INSERT INTO t (a) VALUES (1) /*!99999 , (2) */", 1, 1, Dialect.VERSION_GATED);

        // An ordinary block comment is still ignored on all three.
        rows("INSERT INTO t (a) VALUES (1) /* , (2) */", 1, 1, 1);
        // And a placeholder inside an executable comment is a placeholder there.
        assertEquals("SELECT 1 /*! , ? */", Dialect.MYSQL.bind("SELECT 1 /*! , ? */", 1));
    }

    private static void ends(String sql, int sqlite, int postgres, int mysql) throws Exception {
        assertEquals(sqlite, Dialect.SQLITE.endOfStatement(sql), "SQLite: " + sql);
        assertEquals(postgres, Dialect.POSTGRES.endOfStatement(sql), "PostgreSQL: " + sql);
        assertEquals(mysql, Dialect.MYSQL.endOfStatement(sql), "MySQL: " + sql);
    }

    private static void rows(String sql, int sqlite, int postgres, int mysql) throws Exception {
        assertEquals(sqlite, Dialect.SQLITE.countInsertRows(sql), "SQLite: " + sql);
        assertEquals(postgres, Dialect.POSTGRES.countInsertRows(sql), "PostgreSQL: " + sql);
        assertEquals(mysql, Dialect.MYSQL.countInsertRows(sql), "MySQL: " + sql);
    }
}
