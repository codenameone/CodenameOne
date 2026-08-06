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
    void countsPositionalPlaceholders() {
        assertEquals(0, SQLStatementSplitter.countPositionalParameters("SELECT 1"));
        assertEquals(1, SQLStatementSplitter.countPositionalParameters("SELECT * FROM t WHERE a=?"));
        assertEquals(2, SQLStatementSplitter.countPositionalParameters(
                "INSERT INTO t (a, b) VALUES (?, ?)"));
        assertEquals(0, SQLStatementSplitter.countPositionalParameters(null));
    }

    @Test
    void doesNotCountAQuestionMarkThatIsNotAPlaceholder() {
        assertEquals(0, SQLStatementSplitter.countPositionalParameters(
                "INSERT INTO t VALUES ('is it? yes')"));
        assertEquals(1, SQLStatementSplitter.countPositionalParameters(
                "INSERT INTO t VALUES ('is it? yes', ?)"));
        assertEquals(0, SQLStatementSplitter.countPositionalParameters("SELECT \"a?b\" FROM t"));
        assertEquals(0, SQLStatementSplitter.countPositionalParameters("SELECT [a?b] FROM t"));
        assertEquals(0, SQLStatementSplitter.countPositionalParameters("SELECT 1 -- why? \n"));
        assertEquals(0, SQLStatementSplitter.countPositionalParameters("SELECT /* why? */ 1"));
    }

    @Test
    void refusesToGuessAtNamedOrNumberedPlaceholders() {
        // One name can repeat and a ?NNN can skip an index, so the marker count is not the
        // parameter count. Reporting that plainly beats rejecting a valid statement.
        assertEquals(SQLStatementSplitter.PARAMETER_COUNT_UNKNOWN,
                SQLStatementSplitter.countPositionalParameters("SELECT * FROM t WHERE a=?1 AND b=?1"));
        assertEquals(SQLStatementSplitter.PARAMETER_COUNT_UNKNOWN,
                SQLStatementSplitter.countPositionalParameters("SELECT * FROM t WHERE a=:name"));
        assertEquals(SQLStatementSplitter.PARAMETER_COUNT_UNKNOWN,
                SQLStatementSplitter.countPositionalParameters("SELECT * FROM t WHERE a=@name"));
        assertEquals(SQLStatementSplitter.PARAMETER_COUNT_UNKNOWN,
                SQLStatementSplitter.countPositionalParameters("SELECT * FROM t WHERE a=$name"));
    }

    @Test
    void treatsAStandaloneColonAsOrdinarySyntax() {
        // A colon with no name after it is not a parameter, so this stays countable.
        assertEquals(1, SQLStatementSplitter.countPositionalParameters(
                "SELECT CAST(a AS TEXT) FROM t WHERE b=? AND c IN ('x:')"));
    }
}
