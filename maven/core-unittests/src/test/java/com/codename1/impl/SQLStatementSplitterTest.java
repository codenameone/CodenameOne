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
package com.codename1.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Splitting a script wrongly corrupts a schema silently, and miscounting placeholders makes a
 * statement run with arguments the caller never supplied, so both are pinned here rather than
 * left to the device suites to notice.
 */
class SQLStatementSplitterTest {

    @Test
    void splitsOnSemicolonsBetweenStatements() {
        String[] out = SQLStatementSplitter.split("CREATE TABLE a (x); CREATE TABLE b (y);");
        assertEquals(2, out.length);
        assertEquals("CREATE TABLE a (x)", out[0]);
        assertEquals("CREATE TABLE b (y)", out[1]);
    }

    @Test
    void treatsASingleStatementAsOne() {
        assertEquals(1, SQLStatementSplitter.countStatements("SELECT 1"));
        assertFalse(SQLStatementSplitter.isMultiStatement("SELECT 1"));
        assertFalse(SQLStatementSplitter.isMultiStatement("SELECT 1;"));
        assertTrue(SQLStatementSplitter.isMultiStatement("SELECT 1; SELECT 2"));
    }

    @Test
    void ignoresBlankAndCommentOnlyTrailers() {
        assertEquals(1, SQLStatementSplitter.countStatements("SELECT 1; -- done\n"));
        assertEquals(1, SQLStatementSplitter.countStatements("SELECT 1;;;"));
        assertEquals(0, SQLStatementSplitter.countStatements("   \n -- nothing here \n"));
        assertEquals(0, SQLStatementSplitter.countStatements("/* only a comment */"));
    }

    @Test
    void doesNotSplitOnASemicolonInsideQuotedText() {
        assertEquals(1, SQLStatementSplitter.countStatements("INSERT INTO t VALUES ('a;b')"));
        assertEquals(1, SQLStatementSplitter.countStatements("INSERT INTO t VALUES (\"a;b\")"));
        assertEquals(1, SQLStatementSplitter.countStatements("SELECT `a;b` FROM t"));
        assertEquals(1, SQLStatementSplitter.countStatements("SELECT [a;b] FROM t"));
        // The doubled quote is an escaped quote, not the end of the literal.
        assertEquals(1, SQLStatementSplitter.countStatements("INSERT INTO t VALUES ('it''s; ok')"));
    }

    @Test
    void doesNotSplitOnASemicolonInsideAComment() {
        assertEquals(1, SQLStatementSplitter.countStatements("SELECT 1 -- a; comment\n"));
        assertEquals(1, SQLStatementSplitter.countStatements("SELECT /* a; comment */ 1"));
    }

    @Test
    void keepsATriggerBodyWithItsStatement() {
        String trigger = "CREATE TRIGGER t AFTER INSERT ON a BEGIN "
                + "UPDATE b SET n = n + 1; "
                + "DELETE FROM c; "
                + "END;";
        assertEquals(1, SQLStatementSplitter.countStatements(trigger));
        assertEquals(2, SQLStatementSplitter.countStatements(trigger + " SELECT 1;"));
    }

    @Test
    void doesNotTreatColumnsNamedLikeDelimitersAsDelimiters() {
        // None of BEGIN, END or CASE is reserved in SQLite, so these are valid column names and
        // counting them as structure splits the trigger at the next semicolon.
        String trigger = "CREATE TRIGGER t AFTER INSERT ON a BEGIN "
                + "UPDATE b SET end=1; "
                + "UPDATE c SET begin = 2; "
                + "END;";
        assertEquals(1, SQLStatementSplitter.countStatements(trigger));
        assertEquals(2, SQLStatementSplitter.countStatements(trigger + " SELECT 1;"));
    }

    @Test
    void doesNotTreatColumnReferencesNamedEndAsDelimiters() {
        // The forms a next-character rule could not tell apart: one is followed by a keyword, the
        // other by the statement's own semicolon.
        String selecting = "CREATE TRIGGER t AFTER INSERT ON a BEGIN "
                + "SELECT end FROM t; "
                + "END;";
        assertEquals(1, SQLStatementSplitter.countStatements(selecting));
        assertEquals(2, SQLStatementSplitter.countStatements(selecting + " SELECT 1;"));

        String assigning = "CREATE TRIGGER t AFTER INSERT ON a BEGIN "
                + "UPDATE t SET x = end; "
                + "END;";
        assertEquals(1, SQLStatementSplitter.countStatements(assigning));
        assertEquals(2, SQLStatementSplitter.countStatements(assigning + " SELECT 1;"));
    }

    @Test
    void doesNotLetAColumnNamedBeginReopenTheBody() {
        String trigger = "CREATE TRIGGER t AFTER INSERT ON a BEGIN "
                + "UPDATE t SET begin = 1; "
                + "END;";
        assertEquals(1, SQLStatementSplitter.countStatements(trigger));
        assertEquals(2, SQLStatementSplitter.countStatements(trigger + " SELECT 1;"));
    }

    @Test
    void stillTracksRealTriggerStructure() {
        String withCase = "CREATE TRIGGER t AFTER INSERT ON a BEGIN "
                + "UPDATE b SET n = CASE WHEN n > 0 THEN 1 ELSE 2 END; "
                + "END;";
        assertEquals(1, SQLStatementSplitter.countStatements(withCase));
        assertEquals(2, SQLStatementSplitter.countStatements(withCase + " SELECT 1;"));
    }

    @Test
    void countsPositionalPlaceholders() {
        assertEquals(0, SQLStatementSplitter.countParameters("SELECT 1"));
        assertEquals(1, SQLStatementSplitter.countParameters("SELECT * FROM t WHERE a=?"));
        assertEquals(2, SQLStatementSplitter.countParameters(
                "INSERT INTO t (a, b) VALUES (?, ?)"));
        assertEquals(0, SQLStatementSplitter.countParameters(null));
    }

    @Test
    void doesNotCountAQuestionMarkThatIsNotAPlaceholder() {
        assertEquals(0, SQLStatementSplitter.countParameters(
                "INSERT INTO t VALUES ('is it? yes')"));
        assertEquals(1, SQLStatementSplitter.countParameters(
                "INSERT INTO t VALUES ('is it? yes', ?)"));
        assertEquals(0, SQLStatementSplitter.countParameters("SELECT \"a?b\" FROM t"));
        assertEquals(0, SQLStatementSplitter.countParameters("SELECT [a?b] FROM t"));
        assertEquals(0, SQLStatementSplitter.countParameters("SELECT 1 -- why? \n"));
        assertEquals(0, SQLStatementSplitter.countParameters("SELECT /* why? */ 1"));
    }

    @Test
    void countsANamedPlaceholderOncePerName() {
        // The count is the number of distinct names, not of markers, which is what
        // sqlite3_bind_parameter_count reports. Counting markers would demand two arguments for
        // one parameter; counting nothing at all would let one argument satisfy two.
        assertEquals(1, SQLStatementSplitter.countParameters(
                "SELECT * FROM t WHERE a=:x OR b=:x"));
        assertEquals(2, SQLStatementSplitter.countParameters(
                "INSERT INTO t (a, b) VALUES (:a, :b)"));
        assertEquals(2, SQLStatementSplitter.countParameters(
                "INSERT INTO t (a, b) VALUES (@a, @b)"));
        assertEquals(2, SQLStatementSplitter.countParameters(
                "INSERT INTO t (a, b) VALUES ($a, $b)"));
        // Different sigils are different parameters, and names are case sensitive.
        assertEquals(2, SQLStatementSplitter.countParameters(
                "SELECT * FROM t WHERE a=:x OR b=@x"));
        assertEquals(2, SQLStatementSplitter.countParameters(
                "SELECT * FROM t WHERE a=:x OR b=:X"));
    }

    @Test
    void countsANumberedPlaceholderByItsIndex() {
        // ?NNN takes that index outright, so the count is the largest index and not the number of
        // markers: "?3" alone is a three parameter statement with two slots nobody binds.
        assertEquals(1, SQLStatementSplitter.countParameters(
                "SELECT * FROM t WHERE a=?1 AND b=?1"));
        assertEquals(3, SQLStatementSplitter.countParameters("SELECT ?3"));
        assertEquals(3, SQLStatementSplitter.countParameters(
                "SELECT * FROM t WHERE a=?1 AND b=?3"));
        // A bare ? takes the next index after the largest so far.
        assertEquals(4, SQLStatementSplitter.countParameters("SELECT ?3, ?"));
    }

    @Test
    void mixesTheThreeFormsTheWayTheEngineDoes() {
        // A name takes the next free index just as a bare ? does, so these interleave.
        assertEquals(3, SQLStatementSplitter.countParameters(
                "SELECT * FROM t WHERE a=? AND b=:name AND c=?"));
        assertEquals(2, SQLStatementSplitter.countParameters(
                "SELECT * FROM t WHERE a=? AND b=:name AND c=:name"));
    }

    @Test
    void readsTheWholeNameTheEngineWouldRead() {
        // A name is more than its run of identifier characters, and stopping early splits one
        // parameter into two -- which rejects a valid single-argument call. Every expectation here
        // was taken from sqlite3_bind_parameter_count on a real engine, not from reading the
        // grammar.
        //
        // "::" continues a name rather than ending it.
        assertEquals(1, SQLStatementSplitter.countParameters("SELECT $foo::bar"));
        assertEquals(1, SQLStatementSplitter.countParameters("SELECT :foo::bar"));
        assertEquals(1, SQLStatementSplitter.countParameters("SELECT @foo::bar"));
        assertEquals(1, SQLStatementSplitter.countParameters("SELECT $foo::bar::baz"));
        assertEquals(1, SQLStatementSplitter.countParameters("SELECT $foo::"));
        assertEquals(1, SQLStatementSplitter.countParameters("SELECT $foo::bar, $foo::bar"));
        assertEquals(2, SQLStatementSplitter.countParameters("SELECT $foo::bar, $foo"));
        assertEquals(2, SQLStatementSplitter.countParameters("SELECT $foo::bar, :foo::bar"));

        // A parenthesized suffix is part of the name, so it tells two parameters apart.
        assertEquals(1, SQLStatementSplitter.countParameters("SELECT $foo(suffix)"));
        assertEquals(1, SQLStatementSplitter.countParameters("SELECT $foo()"));
        assertEquals(1, SQLStatementSplitter.countParameters("SELECT $foo(a::b)"));
        assertEquals(1, SQLStatementSplitter.countParameters("SELECT :foo::bar(baz)"));
        assertEquals(2, SQLStatementSplitter.countParameters("SELECT $foo(a), $foo(b)"));
        assertEquals(1, SQLStatementSplitter.countParameters("SELECT $foo(a), $foo(a)"));
        assertEquals(2, SQLStatementSplitter.countParameters(
                "INSERT INTO t VALUES ($foo(x), $foo(y), $foo(x))"));

        // "#" is a parameter sigil too, and "$" is an identifier character.
        assertEquals(1, SQLStatementSplitter.countParameters("SELECT #foo"));
        assertEquals(1, SQLStatementSplitter.countParameters("SELECT $a$b"));
        assertEquals(4, SQLStatementSplitter.countParameters("SELECT ? , :n , @m , $k"));

        // And none of it applies inside a literal.
        assertEquals(1, SQLStatementSplitter.countParameters(
                "SELECT $foo::bar FROM t WHERE a = '$foo::baz'"));
        assertEquals(0, SQLStatementSplitter.countParameters("SELECT \"$foo::bar\" FROM t"));
    }

    @Test
    void reportsNoCountForANameTheEngineWouldNotTokenize() {
        // Whitespace inside the suffix, or no closing parenthesis, makes the token unparseable.
        // Reporting a count there would replace the engine's syntax error with a parameter count
        // error that says nothing about the real problem.
        assertEquals(SQLStatementSplitter.PARAMETER_COUNT_UNKNOWN,
                SQLStatementSplitter.countParameters("SELECT $foo(a b)"));
        assertEquals(SQLStatementSplitter.PARAMETER_COUNT_UNKNOWN,
                SQLStatementSplitter.countParameters("SELECT $foo(unterminated"));
    }

    @Test
    void refusesToGuessAtAnIndexTheEngineWouldReject() {
        // ?0 is not a parameter SQLite accepts, so there is no count to report and the caller
        // skips the check rather than rejecting a statement it cannot judge.
        assertEquals(SQLStatementSplitter.PARAMETER_COUNT_UNKNOWN,
                SQLStatementSplitter.countParameters("SELECT ?0"));
        assertEquals(SQLStatementSplitter.PARAMETER_COUNT_UNKNOWN,
                SQLStatementSplitter.countParameters("SELECT ?999999999"));
    }

    @Test
    void doesNotCountANamedPlaceholderInsideAStringOrComment() {
        assertEquals(0, SQLStatementSplitter.countParameters(
                "INSERT INTO t VALUES ('call me :name')"));
        assertEquals(0, SQLStatementSplitter.countParameters("SELECT 1 -- :name \n"));
        assertEquals(0, SQLStatementSplitter.countParameters("SELECT /* :name */ 1"));
        assertEquals(1, SQLStatementSplitter.countParameters(
                "INSERT INTO t VALUES ('call me :name', :real)"));
    }

    @Test
    void readsWhetherAStatementWrites() {
        // Decides whether a cursor may re-execute its statement to move backwards. A write read as
        // a query is run twice by an ordinary getCount(), so the wrong answer here costs data.
        assertFalse(SQLStatementSplitter.writesData("SELECT * FROM t"));
        assertFalse(SQLStatementSplitter.writesData(
                "SELECT * FROM (SELECT id FROM t WHERE v = 'insert')"));
        assertFalse(SQLStatementSplitter.writesData("WITH c AS (SELECT 1) SELECT * FROM c"));
        assertFalse(SQLStatementSplitter.writesData("SELECT 'delete from t'"));
        assertFalse(SQLStatementSplitter.writesData("SELECT \"update\" FROM t"));
        assertFalse(SQLStatementSplitter.writesData("SELECT 1 -- insert into t\n"));

        assertTrue(SQLStatementSplitter.writesData(
                "INSERT INTO t (v) VALUES ('a') RETURNING id"));
        assertTrue(SQLStatementSplitter.writesData("update t set v = 1 returning id"));
        assertTrue(SQLStatementSplitter.writesData("DELETE FROM t RETURNING *"));
        assertTrue(SQLStatementSplitter.writesData(
                "WITH c AS (SELECT id FROM src) INSERT INTO t SELECT * FROM c RETURNING id"));
        assertTrue(SQLStatementSplitter.writesData("CREATE TABLE t (id INTEGER)"));
    }

    @Test
    void readsWhichPragmasWrite() {
        // A cursor may only re-run its statement if running it again changes nothing. Most
        // pragmas change something -- these are the ones that report.
        assertFalse(SQLStatementSplitter.writesData("PRAGMA table_info(t)"));
        assertFalse(SQLStatementSplitter.writesData("pragma main.page_count"));
        assertFalse(SQLStatementSplitter.writesData("PRAGMA integrity_check"));
        assertFalse(SQLStatementSplitter.writesData("PRAGMA foreign_key_list(t)"));

        assertTrue(SQLStatementSplitter.writesData("PRAGMA incremental_vacuum(1)"));
        assertTrue(SQLStatementSplitter.writesData("PRAGMA wal_checkpoint(FULL)"));
        assertTrue(SQLStatementSplitter.writesData("PRAGMA optimize"));
        assertTrue(SQLStatementSplitter.writesData("PRAGMA user_version = 4"));
        assertTrue(SQLStatementSplitter.writesData("PRAGMA main.journal_mode = WAL"));
        // Unknown to the list, so treated as a write: losing backward movement beats running it
        // a second time.
        assertTrue(SQLStatementSplitter.writesData("PRAGMA something_new_in_sqlite"));
    }

    @Test
    void treatsAStandaloneColonAsOrdinarySyntax() {
        // A colon with no name after it is not a parameter, so this stays countable.
        assertEquals(1, SQLStatementSplitter.countParameters(
                "SELECT CAST(a AS TEXT) FROM t WHERE b=? AND c IN ('x:')"));
    }
}
