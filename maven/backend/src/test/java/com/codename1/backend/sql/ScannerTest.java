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

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    @DisplayName("an insert with no tuple list is one row, or an unknown number")
    void insertsWithoutATupleList() throws Exception {
        // -1 means "cannot tell", and Database.insert refuses it. Which shapes
        // land there matters in both directions: a query-sourced insert that
        // slipped through returned one engine-specific key for however many rows
        // it wrote, and a single-row form wrongly landing there would refuse an
        // ordinary insert.
        rows("INSERT INTO t DEFAULT VALUES", 1, 1, 1);
        // MySQL's single-row form, which names no VALUES at all.
        rows("INSERT INTO t SET a = ?", 1, 1, 1);
        // The rows come from a query: only the server knows how many.
        rows("INSERT INTO t (a) SELECT b FROM u", -1, -1, -1);
        rows("INSERT INTO t SELECT * FROM u", -1, -1, -1);
        rows("INSERT INTO t (a) TABLE u", -1, -1, -1);
        rows("WITH c AS (SELECT 1) INSERT INTO t (a) SELECT * FROM c", -1, -1, -1);
        // A SUBQUERY IS NOT A SOURCE OF ROWS. The parentheses are what say so,
        // and without the depth test this single row read as unbounded.
        rows("INSERT INTO t SET a = (SELECT max(x) FROM u)", 1, 1, 1);
        rows("INSERT INTO t DEFAULT VALUES RETURNING id", 1, 1, 1);
        // And the word inside a literal is not the keyword.
        rows("INSERT INTO t SET a = 'select one'", 1, 1, 1);
    }

    @Test
    @DisplayName("a dollar after a non-ASCII letter is part of the identifier")
    void nonAsciiIdentifiersReachTheirDollar() throws Exception {
        // PostgreSQL's unquoted identifiers carry the letters of the server
        // encoding, so "caf\u00e9$usd$" is one column name. Reading the token
        // boundary as ASCII-only made the $ look like a dollar-quote opener,
        // and the statement was refused as an unterminated literal -- a valid
        // query that never ran, with a message about a string it does not have.
        String sql = "SELECT caf\u00e9$usd$ FROM totals WHERE id = ?";
        assertEquals("SELECT caf\u00e9$usd$ FROM totals WHERE id = $1",
                Dialect.POSTGRES.bind(sql, 1));
        // The same name on the engines that have no dollar quoting at all.
        assertEquals(sql, Dialect.MYSQL.bind(sql, 1));
        assertEquals(sql, Dialect.SQLITE.bind(sql, 1));

        // A REAL dollar-quoted body still opens, because it follows a boundary
        // rather than a letter -- which is the distinction being made.
        assertEquals("SELECT $tag$ ? $tag$, $1",
                Dialect.POSTGRES.bind("SELECT $tag$ ? $tag$, ?", 1));
        // And a keyword is not one when a non-ASCII letter runs into it.
        assertEquals(1, Dialect.POSTGRES.countInsertRows(
                "INSERT INTO t (a) VALUES (?) /* caf\u00e9values (2) */"));
    }

    @Test
    @DisplayName("an upsert is recognised as SQL, not found as text")
    void upsertsAreRecognised() throws Exception {
        // Database refuses one of these on the engines that read the generated
        // key from connection state, so a miss returns another row's key and a
        // false positive refuses a plain insert. Both halves matter.
        assertTrue(Dialect.SQLITE.updatesOnConflict(
                "INSERT INTO t (a) VALUES (?) ON CONFLICT (a) DO UPDATE SET b = 1"));
        assertTrue(Dialect.MYSQL.updatesOnConflict(
                "INSERT INTO t (a) VALUES (?) ON DUPLICATE KEY UPDATE b = 1"));
        // Case and spacing are the statement's to choose, and a comment can sit
        // between the words.
        assertTrue(Dialect.SQLITE.updatesOnConflict(
                "insert into t (a) values (?) on conflict do\n  update set b = 1"));
        assertTrue(Dialect.MYSQL.updatesOnConflict(
                "INSERT INTO t (a) VALUES (?) ON DUPLICATE /* here */ KEY UPDATE b = 1"));

        // DO NOTHING is not an update: nothing is written, the row count is
        // zero, and insert() already answers zero for that.
        assertFalse(Dialect.SQLITE.updatesOnConflict(
                "INSERT INTO t (a) VALUES (?) ON CONFLICT DO NOTHING"));
        assertFalse(Dialect.SQLITE.updatesOnConflict("INSERT INTO t (a) VALUES (?)"));
        // REPLACE inserts a row, so last_insert_rowid() does answer for it.
        assertFalse(Dialect.SQLITE.updatesOnConflict("REPLACE INTO t (a) VALUES (?)"));

        // PostgreSQL's OVERRIDING clause puts the singular keyword in FRONT of
        // the real VALUES, so latching the first match counted one row for a
        // statement that writes two -- and the multi-row refusal never fired,
        // leaving PostgreSQL to object after both rows had committed.
        rows("INSERT INTO t(id, a) OVERRIDING USER VALUE VALUES (?, ?), (?, ?)", 2, 2, 2);
        rows("INSERT INTO t(id, a) OVERRIDING SYSTEM VALUE VALUES (?, ?)", 1, 1, 1);
        // MySQL's singular row clause is still a row clause, because a tuple
        // follows it -- which is the test that tells the two apart.
        rows("INSERT INTO t (a) VALUE (?), (?)", 2, 2, 2);

        // And the words are read as SQL: inside a literal or a comment they are
        // not keywords, and a longer word that merely contains one is not it.
        assertFalse(Dialect.SQLITE.updatesOnConflict(
                "INSERT INTO t (a) VALUES ('do update')"));
        assertFalse(Dialect.SQLITE.updatesOnConflict(
                "INSERT INTO t (a) VALUES (?) -- do update"));
        assertFalse(Dialect.MYSQL.updatesOnConflict(
                "INSERT INTO t (a) VALUES (?) /* on duplicate key update */"));
        assertFalse(Dialect.SQLITE.updatesOnConflict(
                "INSERT INTO t (redo, updated) VALUES (?, ?)"));

        // A GATED upsert reads as one. Unlike the tuple count and the parameter
        // count, "might it update?" has a safe answer when the server version is
        // unknown -- and it is yes, so this needs no VERSION_GATED case of its
        // own: a statement that updates on some servers has no reliable key on
        // any of them.
        assertTrue(Dialect.MYSQL.updatesOnConflict(
                "INSERT INTO t (a) VALUES (?) /*!50100 ON DUPLICATE KEY UPDATE b = 1 */"));
    }

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

        // A placeholder INSIDE a version gate is refused rather than counted:
        // MySQL prepares "SELECT 1 /*!99999 + ? */" with no parameters at all on
        // a server the gate excludes, so counting the one supplied here reaches
        // the server as a parameter the statement does not have.
        assertThrows(IOException.class,
                () -> Dialect.MYSQL.bind("SELECT 1 /*!99999 + ? */", 1));
        assertThrows(IOException.class,
                () -> Dialect.MYSQL.bind("SELECT ? /*!50100 , ? */", 2));
        // A gate with no placeholder in it is nobody's problem, and a
        // placeholder OUTSIDE one beside it is still a placeholder.
        assertEquals("SELECT ? /*!99999 + 1 */", Dialect.MYSQL.bind("SELECT ? /*!99999 + 1 */", 1));
        // And the gate means nothing to the other two, where "/*!99999 + ? */"
        // is a comment and the ? inside it is not a parameter at all.
        assertEquals("SELECT 1 /*!99999 + ? */",
                Dialect.SQLITE.bind("SELECT 1 /*!99999 + ? */", 0));

        // GATE-SHAPED TEXT IS NOT A GATE. The detector used to search for "/*!"
        // with indexOf and argued a false positive was cheap -- but the refusal
        // falls on a VALID statement, so ordinary data that happens to start
        // this way could not be inserted at all. It is scanned as SQL now, so a
        // literal and a comment are what they are.
        rows("INSERT INTO t(a) VALUES ('/*!99999 not a comment')", 1, 1, 1);
        rows("INSERT INTO t(a) VALUES (?) -- /*!99999 nor is this", 1, 1, 1);
        // MySQL only: PostgreSQL nests block comments, so one "*/" would leave
        // this genuinely unterminated there and refusing it is correct.
        assertEquals(1, Dialect.MYSQL.countInsertRows(
                "INSERT INTO t(a) VALUES (?) /* /*!99999 nor this */"));
        // And binding agrees: a ? inside that literal is data, not a parameter,
        // and the gate text does not make the real one unknowable.
        assertEquals("INSERT INTO t(a, b) VALUES (?, '/*!99999 ?')",
                Dialect.MYSQL.bind("INSERT INTO t(a, b) VALUES (?, '/*!99999 ?')", 1));

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
