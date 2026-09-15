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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The engine differences, and the rewrite that hides them.
 *
 * <p>These are the cases that used to be a branch at every call site, plus the
 * ones a naive search-and-replace over the statement gets wrong. The rewrite runs
 * on every statement a server issues, so a mistake here is not a wrong answer to
 * one query: it is every query on one engine.
 */
class DialectTest {

    @Test
    @DisplayName("a portable statement becomes each engine's own")
    void rendersPlaceholdersPerEngine() throws Exception {
        String portable = "INSERT INTO notes (title, body) VALUES (?, ?)";
        assertEquals("INSERT INTO notes (title, body) VALUES ($1, $2)",
                Dialect.POSTGRES.bind(portable, 2));
        assertEquals(portable, Dialect.SQLITE.bind(portable, 2));
        assertEquals(portable, Dialect.MYSQL.bind(portable, 2));
    }

    @Test
    @DisplayName("a statement needing no change is not copied")
    void returnsTheSameStringWhenNothingChanges() throws Exception {
        // Every statement a server issues goes through this, so the case where
        // there is nothing to do has to cost nothing but the scan.
        String sql = "SELECT id FROM notes WHERE author = ?";
        assertSame(sql, Dialect.SQLITE.bind(sql, 1));
    }

    @Test
    @DisplayName("a question mark inside a literal is not a parameter")
    void skipsLiteralsAndComments() throws Exception {
        assertEquals("SELECT * FROM t WHERE a LIKE 'who?' AND b = $1",
                Dialect.POSTGRES.bind("SELECT * FROM t WHERE a LIKE 'who?' AND b = ?", 1));
        assertEquals("SELECT 'it''s ?', $1",
                Dialect.POSTGRES.bind("SELECT 'it''s ?', ?", 1));
        assertEquals("SELECT \"we?rd\" FROM t WHERE a = $1",
                Dialect.POSTGRES.bind("SELECT \"we?rd\" FROM t WHERE a = ?", 1));
        assertEquals("SELECT 1 -- is this ?\nWHERE a = $1",
                Dialect.POSTGRES.bind("SELECT 1 -- is this ?\nWHERE a = ?", 1));
        assertEquals("SELECT /* ? */ $1", Dialect.POSTGRES.bind("SELECT /* ? */ ?", 1));
        // PostgreSQL's block comments NEST, so the inner close does not end the
        // outer comment and the ? after it is still commented out.
        assertEquals("SELECT /* a /* ? */ ? */ $1",
                Dialect.POSTGRES.bind("SELECT /* a /* ? */ ? */ ?", 1));
        assertEquals("SELECT $$ a ? b $$, $1",
                Dialect.POSTGRES.bind("SELECT $$ a ? b $$, ?", 1));
        assertEquals("SELECT $tag$ ? $tag$, $1",
                Dialect.POSTGRES.bind("SELECT $tag$ ? $tag$, ?", 1));
    }

    @Test
    @DisplayName("a backslash escapes only inside an E'' literal")
    void readsBackslashEscapesOnlyWhereTheyAreOne() throws Exception {
        // standard_conforming_strings has been on for fifteen years, so a
        // backslash in an ordinary literal is a backslash. Reading it as an
        // escape would swallow the closing quote and the rest of the statement
        // with it.
        assertEquals("SELECT 'a\\', $1", Dialect.POSTGRES.bind("SELECT 'a\\', ?", 1));
        assertEquals("SELECT E'a\\'? b', $1", Dialect.POSTGRES.bind("SELECT E'a\\'? b', ?", 1));
        // And the E has to BE a prefix rather than the tail of an identifier.
        assertEquals("SELECT type'a\\', $1", Dialect.POSTGRES.bind("SELECT type'a\\', ?", 1));
    }

    @Test
    @DisplayName("?? is a literal question mark on every engine")
    void collapsesTheEscape() throws Exception {
        // PostgreSQL is where it is needed -- its jsonb operators are spelled ?,
        // ?| and ?& -- and it means the same thing everywhere so that a statement
        // means the same thing wherever it runs.
        assertEquals("SELECT data ? 'k'", Dialect.POSTGRES.bind("SELECT data ?? 'k'", 0));
        assertEquals("SELECT a ? b", Dialect.SQLITE.bind("SELECT a ?? b", 0));
        // Inside a literal there is nothing to escape, so both stay.
        assertEquals("SELECT '??'", Dialect.SQLITE.bind("SELECT '??'", 0));
    }

    @Test
    @DisplayName("engine-native SQL is passed through unchecked")
    void leavesHandWrittenDollarParametersAlone() throws Exception {
        // Existing code writes $1 for PostgreSQL, and the count it implies is the
        // server's business rather than ours: a statement with no ? in it is not
        // in the portable form at all.
        String native1 = "INSERT INTO a (b, c) VALUES ($1, $2)";
        assertEquals(native1, Dialect.POSTGRES.bind(native1, 2));
        assertEquals(native1, Dialect.POSTGRES.bind(native1, 0));
    }

    @Test
    @DisplayName("a parameter count that does not match is refused here")
    void refusesAMismatchedParameterCount() {
        // SQLite binds the missing ones to NULL and commits the row, MySQL reads
        // the surplus descriptors as something else, and PostgreSQL refuses. One
        // answer for all three, before the statement is sent.
        IOException err = assertThrows(IOException.class,
                () -> Dialect.SQLITE.bind("INSERT INTO a (b, c) VALUES (?, ?)", 1));
        assertTrue(err.getMessage().contains("2 parameter placeholders"), err.getMessage());
        assertTrue(err.getMessage().contains("1 value"), err.getMessage());
        assertThrows(IOException.class,
                () -> Dialect.POSTGRES.bind("SELECT ?, ?", 3));
    }

    @Test
    @DisplayName("a statement ending inside a literal is refused, not guessed at")
    void refusesAnUnterminatedLiteral() {
        assertThrows(IOException.class, () -> Dialect.POSTGRES.bind("SELECT 'abc", 0));
        assertThrows(IOException.class, () -> Dialect.POSTGRES.bind("SELECT /* abc", 0));
    }

    @Test
    @DisplayName("MySQL escapes with a backslash in every literal; the others do not")
    void readsEachEngineOwnStringEscapes() throws Exception {
        // MySQL's default SQL mode makes a backslash an escape everywhere, so
        // the literal here ends at the LAST quote and the ? inside it is not a
        // parameter. Read PostgreSQL's way, the literal ended at the escaped
        // quote and the ? that followed was counted, so a valid statement was
        // refused before MySQL ever saw it.
        assertEquals("SELECT 'it\\'s ?', ?", Dialect.MYSQL.bind("SELECT 'it\\'s ?', ?", 1));
        // And the other two keep their own rule: a backslash is an ordinary
        // character in SQLite and in an unprefixed PostgreSQL literal, so the
        // literal ends at the first unescaped quote.
        assertEquals("SELECT 'a\\', $1", Dialect.POSTGRES.bind("SELECT 'a\\', ?", 1));
        assertEquals("SELECT 'a\\', ?", Dialect.SQLITE.bind("SELECT 'a\\', ?", 1));
    }

    @Test
    @DisplayName("MySQL's hash comment is a comment")
    void readsHashComments() throws Exception {
        // # starts a line comment on MySQL alone. Unrecognised, the ? in the
        // comment was counted and the statement refused for having two
        // parameters when one value was supplied.
        assertEquals("SELECT 1 # why?\nWHERE a = ?",
                Dialect.MYSQL.bind("SELECT 1 # why?\nWHERE a = ?", 1));
        // Not on the others, where # is not a comment introducer at all.
        assertThrows(IOException.class, () -> Dialect.SQLITE.bind("SELECT 1 # why?\nWHERE a = ?", 1));
    }

    @Test
    @DisplayName("an insert with no columns is spelled per engine")
    void insertsDefaults() {
        // The entity whose only persisted field is a generated key. The obvious
        // construction, INSERT INTO t () VALUES (), is refused by two of the
        // three; MySQL is the one that wants it and has no DEFAULT VALUES.
        assertEquals("INSERT INTO \"t\" DEFAULT VALUES", Dialect.SQLITE.insertDefaults("\"t\""));
        assertEquals("INSERT INTO \"t\" DEFAULT VALUES", Dialect.POSTGRES.insertDefaults("\"t\""));
        assertEquals("INSERT INTO `t` () VALUES ()", Dialect.MYSQL.insertDefaults("`t`"));
    }

    @Test
    @DisplayName("identifiers are quoted so their case survives PostgreSQL")
    void quotesIdentifiers() {
        assertEquals("\"createdAt\"", Dialect.POSTGRES.quote("createdAt"));
        assertEquals("\"we\"\"ird\"", Dialect.POSTGRES.quote("we\"ird"));
        assertEquals("`we``ird`", Dialect.MYSQL.quote("we`ird"));
        assertThrows(IllegalArgumentException.class, () -> Dialect.SQLITE.quote(""));
        assertThrows(IllegalArgumentException.class, () -> Dialect.SQLITE.quote("a\0b"));
    }

    @Test
    @DisplayName("each engine names the portable column kinds its own way")
    void namesColumnTypes() {
        assertEquals("TEXT", Dialect.SQLITE.columnType(Dialect.TEXT));
        assertEquals("TEXT", Dialect.POSTGRES.columnType(Dialect.TEXT));
        // LONGTEXT, not TEXT, and with a binary collation: both halves of this
        // expectation were superseded. MySQL's TEXT holds 65,535 bytes while
        // the other two are unbounded, and its default collation is case and
        // accent insensitive, so eq() matched a different row here than there.
        // See stringColumnsAreNotCappedOnMySql below.
        assertEquals("LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin",
                Dialect.MYSQL.columnType(Dialect.TEXT));
        assertEquals("INTEGER", Dialect.SQLITE.columnType(Dialect.BIGINT));
        assertEquals("BIGINT", Dialect.POSTGRES.columnType(Dialect.BIGINT));
        assertEquals("BIGINT", Dialect.MYSQL.columnType(Dialect.BIGINT));
        assertEquals("REAL", Dialect.SQLITE.columnType(Dialect.REAL));
        assertEquals("DOUBLE PRECISION", Dialect.POSTGRES.columnType(Dialect.REAL));
        assertEquals("DOUBLE", Dialect.MYSQL.columnType(Dialect.REAL));
        assertEquals("BLOB", Dialect.SQLITE.columnType(Dialect.BLOB));
        assertEquals("BYTEA", Dialect.POSTGRES.columnType(Dialect.BLOB));
        assertEquals("LONGBLOB", Dialect.MYSQL.columnType(Dialect.BLOB));
        // A boolean and a timestamp are integers on every engine, so that one
        // decoding path reads them back. See the note on the constants.
        assertEquals("SMALLINT", Dialect.POSTGRES.columnType(Dialect.BOOLEAN));
        assertEquals("BIGINT", Dialect.POSTGRES.columnType(Dialect.TIMESTAMP));
    }

    @Test
    @DisplayName("a generated key is declared the way each engine can generate one")
    void declaresGeneratedKeys() {
        assertEquals("INTEGER PRIMARY KEY AUTOINCREMENT",
                Dialect.SQLITE.generatedKeyColumn(Dialect.BIGINT));
        assertEquals("BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY",
                Dialect.POSTGRES.generatedKeyColumn(Dialect.BIGINT));
        assertEquals("BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY",
                Dialect.MYSQL.generatedKeyColumn(Dialect.BIGINT));
        // PostgreSQL is the one engine with no last-insert-id, so it is the one
        // that has to read the key back out of the INSERT itself.
        assertTrue(Dialect.POSTGRES.generatedKeysThroughReturning());
        assertTrue(!Dialect.SQLITE.generatedKeysThroughReturning());
        assertTrue(!Dialect.MYSQL.generatedKeysThroughReturning());
        // MySQL cannot index a TEXT column without a prefix length, so an
        // application-assigned string key is a VARCHAR there and TEXT elsewhere.
        // NOT NULL because SQLite would otherwise admit a null key; see
        // assignedKeysAreNotNull.
        assertEquals("VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL PRIMARY KEY",
                Dialect.MYSQL.assignedKeyColumn(Dialect.TEXT));
        assertEquals("TEXT NOT NULL PRIMARY KEY", Dialect.POSTGRES.assignedKeyColumn(Dialect.TEXT));
    }

    @Test
    @DisplayName("dollar quoting is PostgreSQL's, and a $ elsewhere is an identifier")
    void readsDollarQuotingOnlyWherePostgres() throws Exception {
        // MySQL allows $ inside an unquoted identifier. Scanning for a
        // dollar-quoted body everywhere read total$usd$ as opening one that
        // never closed, and the statement was refused before the server saw it.
        assertEquals("SELECT total$usd$ FROM t WHERE id = ?",
                Dialect.MYSQL.bind("SELECT total$usd$ FROM t WHERE id = ?", 1));
        assertEquals("SELECT total$usd$ FROM t WHERE id = ?",
                Dialect.SQLITE.bind("SELECT total$usd$ FROM t WHERE id = ?", 1));
        // And PostgreSQL still reads its own: the ? inside the body is not a
        // parameter.
        assertEquals("SELECT $tag$ ? $tag$, $1",
                Dialect.POSTGRES.bind("SELECT $tag$ ? $tag$, ?", 1));
    }

    @Test
    @DisplayName("an insert's rows are counted, and only the real VALUES counts")
    void countsInsertRows() throws Exception {
        assertEquals(1, Dialect.SQLITE.countInsertRows("INSERT INTO t (a) VALUES (?)"));
        assertEquals(2, Dialect.SQLITE.countInsertRows("INSERT INTO t (a) VALUES (?), (?)"));
        assertEquals(3, Dialect.SQLITE.countInsertRows(
                "INSERT INTO t (a, b) VALUES (?, ?), (?, ?), (?, ?);"));
        // A shape with no tuples to count says so rather than guessing.
        assertEquals(-1, Dialect.SQLITE.countInsertRows("INSERT INTO t (a) SELECT b FROM u"));
        // The word inside a literal, inside a comment, or inside an identifier is
        // not the keyword.
        assertEquals(1, Dialect.SQLITE.countInsertRows(
                "INSERT INTO t (a) VALUES ('VALUES (1), (2)')"));
        assertEquals(1, Dialect.POSTGRES.countInsertRows(
                "INSERT INTO t (a) VALUES ($1) -- VALUES (1), (2)"));
        assertEquals(-1, Dialect.SQLITE.countInsertRows("INSERT INTO revalues (a) SELECT 1"));
        // A nested VALUES belongs to the subquery, not to this insert.
        assertEquals(1, Dialect.SQLITE.countInsertRows(
                "INSERT INTO t (a) VALUES ((SELECT max(x) FROM (VALUES (1), (2)) v))"));
    }

    @Test
    @DisplayName("MySQL's VALUES() expression does not become the insert's clause")
    void countsFromTheFirstValuesClause() throws Exception {
        // MySQL's ON DUPLICATE KEY UPDATE uses a VALUES(column) EXPRESSION, and
        // it sits at the top level. Counting from the LAST match found that
        // function call, saw one group, and let a two-row insert through.
        assertEquals(2, Dialect.MYSQL.countInsertRows(
                "INSERT INTO t (a) VALUES (?), (?) ON DUPLICATE KEY UPDATE a = VALUES(a)"));
        assertEquals(1, Dialect.MYSQL.countInsertRows(
                "INSERT INTO t (a) VALUES (?) ON DUPLICATE KEY UPDATE a = VALUES(a)"));
    }

    @Test
    @DisplayName("MySQL's double quotes are a string, with escapes")
    void readsMySqlDoubleQuotedStrings() throws Exception {
        // In its default SQL mode MySQL reads "..." as a string literal with the
        // same backslash escapes its single-quoted strings have. Scanned as an
        // identifier without escapes, this ended at the escaped quote and counted
        // the ? that followed.
        assertEquals("SELECT \"it\\\"s ?\", ?",
                Dialect.MYSQL.bind("SELECT \"it\\\"s ?\", ?", 1));
        // On the other two a double-quoted run is an IDENTIFIER, where a
        // backslash is an ordinary character.
        assertEquals("SELECT \"we?rd\" FROM t WHERE a = $1",
                Dialect.POSTGRES.bind("SELECT \"we?rd\" FROM t WHERE a = ?", 1));
    }

    @Test
    @DisplayName("a dollar quote opens only at a token boundary")
    void requiresABoundaryBeforeADollarQuote() throws Exception {
        // PostgreSQL allows a dollar after the first character of an unquoted
        // identifier, and a dollar-quoted string may not adjoin one -- so
        // price$usd$ is an identifier, and reading it as an opener left a body
        // unterminated and refused a valid statement.
        assertEquals("SELECT price$usd$ FROM totals WHERE id = $1",
                Dialect.POSTGRES.bind("SELECT price$usd$ FROM totals WHERE id = ?", 1));
        // And a real one, at a boundary, is still a literal: the ? inside is not
        // a parameter.
        assertEquals("SELECT $tag$ ? $tag$, $1",
                Dialect.POSTGRES.bind("SELECT $tag$ ? $tag$, ?", 1));
    }

    @Test
    @DisplayName("MySQL needs whitespace after a double dash before it is a comment")
    void readsMySqlDashComments() throws Exception {
        // "5--1" is five minus minus-one on MySQL, where only "-- " opens a
        // comment. Read as a comment, the second tuple of this insert vanished
        // and the multi-row check saw one row.
        assertEquals(2, Dialect.MYSQL.countInsertRows("INSERT INTO t (a) VALUES (5--1), (2)"));
        // With the space it IS a comment there, and the other two take the two
        // dashes whatever follows.
        assertEquals(1, Dialect.MYSQL.countInsertRows("INSERT INTO t (a) VALUES (5) -- , (2)"));
        assertEquals(1, Dialect.SQLITE.countInsertRows("INSERT INTO t (a) VALUES (5--1), (2)"));
        // A ? after a MySQL dash-comment-that-is-not-one is still a parameter.
        assertEquals("SELECT 5--1, ?", Dialect.MYSQL.bind("SELECT 5--1, ?", 1));
    }

    @Test
    @DisplayName("a statement ends before its terminator and its trailing comment")
    void findsWhereTheStatementEnds() throws Exception {
        // Where PostgreSQL's RETURNING has to be inserted. After the end it is
        // either a second statement (past a semicolon) or commented out.
        String plain = "INSERT INTO t (a) VALUES ($1)";
        assertEquals(plain.length(), Dialect.POSTGRES.endOfStatement(plain));
        assertEquals(plain.length(), Dialect.POSTGRES.endOfStatement(plain + ";"));
        assertEquals(plain.length(), Dialect.POSTGRES.endOfStatement(plain + "  ;  "));
        assertEquals(plain.length(), Dialect.POSTGRES.endOfStatement(plain + " -- why"));
        assertEquals(plain.length(), Dialect.POSTGRES.endOfStatement(plain + "; -- why"));
        assertEquals(plain.length(), Dialect.POSTGRES.endOfStatement(plain + " /* why */"));
        // A semicolon or a double dash INSIDE a literal ends nothing.
        String quoted = "INSERT INTO t (a) VALUES ('x; -- y')";
        assertEquals(quoted.length(), Dialect.POSTGRES.endOfStatement(quoted + ";"));
    }

    @Test
    @DisplayName("an application-assigned key cannot be null, including on SQLite")
    void assignedKeysAreNotNull() {
        // SQLite admits a null -- and several nulls -- in a PRIMARY KEY column
        // that is not INTEGER PRIMARY KEY, where the other two refuse. Without
        // this, an insert that forgot its key succeeded in development, failed in
        // production, and left a row the generated id = ? predicate cannot find.
        assertTrue(Dialect.SQLITE.assignedKeyColumn(Dialect.TEXT).contains("NOT NULL"));
        assertTrue(Dialect.POSTGRES.assignedKeyColumn(Dialect.TEXT).contains("NOT NULL"));
        assertTrue(Dialect.MYSQL.assignedKeyColumn(Dialect.TEXT).contains("NOT NULL"));
        assertEquals("TEXT NOT NULL PRIMARY KEY", Dialect.SQLITE.assignedKeyColumn(Dialect.TEXT));
        assertEquals("VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL PRIMARY KEY",
                Dialect.MYSQL.assignedKeyColumn(Dialect.TEXT));
    }

    @Test
    @DisplayName("an offset with no limit is spelled the way each parser accepts")
    void limitsAndOffsets() {
        assertEquals(" LIMIT 10", Dialect.SQLITE.limit(10, 0));
        assertEquals(" LIMIT 10 OFFSET 5", Dialect.SQLITE.limit(10, 5));
        assertEquals("", Dialect.SQLITE.limit(-1, 0));
        // MySQL will not take an OFFSET without a LIMIT, and this is the value
        // its own documentation gives for "the rest of them".
        assertEquals(" LIMIT 18446744073709551615 OFFSET 5", Dialect.MYSQL.limit(-1, 5));
        assertEquals(" LIMIT ALL OFFSET 5", Dialect.POSTGRES.limit(-1, 5));
    }

    @Test
    @DisplayName("a scheme names its engine, ignoring case")
    void resolvesByName() {
        assertSame(Dialect.POSTGRES, Dialect.forName("postgres"));
        assertSame(Dialect.POSTGRES, Dialect.forName("PostgreSQL"));
        // MARIADB, not MYSQL: this expectation is superseded. The two have no
        // case-sensitive NO PAD collation in common, so the name has to answer
        // its own dialect. See namesMariaDbSeparately below.
        assertSame(Dialect.MARIADB, Dialect.forName("mariadb"));
        assertSame(Dialect.SQLITE, Dialect.forName("SQLite"));
        assertEquals(null, Dialect.forName("oracle"));
    }

    @Test
    @DisplayName("a string column is unbounded on every engine, MySQL included")
    void stringColumnsAreNotCappedOnMySql() {
        // MySQL's TEXT holds 65,535 BYTES while the other two leave a string
        // column unbounded, so a document body past that limit stored on two
        // engines and was rejected -- or silently truncated -- by the third.
        // The blob branch of this very switch already took LONGBLOB for the
        // same reason; the string branch had been left on TEXT.
        assertEquals("LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin",
                Dialect.MYSQL.columnType(Dialect.TEXT));
        assertEquals("LONGBLOB", Dialect.MYSQL.columnType(Dialect.BLOB));
        assertEquals("TEXT", Dialect.SQLITE.columnType(Dialect.TEXT));
        assertEquals("TEXT", Dialect.POSTGRES.columnType(Dialect.TEXT));

        // A KEY is the exception and stays bounded, because MySQL cannot index
        // an unbounded column at all. See Table.MAX_ASSIGNED_TEXT_KEY. It also
        // pins a BINARY collation: MySQL's default is case and accent
        // insensitive, so "A" and "a" were the same primary key there while
        // SQLite and PostgreSQL stored two rows. _0900_ rather than the older
        // utf8mb4_bin because that one is still PAD SPACE: under it "token" and
        // "token " were one key as well.
        assertEquals("VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL PRIMARY KEY",
                Dialect.MYSQL.assignedKeyColumn(Dialect.TEXT));
    }

    @Test
    @DisplayName("a null sorts lowest on every engine, however the engine spells it")
    void ordersNullsTheSameWay() {
        // Measured over one null and two values, the defaults disagreed: SQLite
        // and MySQL put the null first ascending, PostgreSQL put it last. So
        // orderBy(field, true).first() answered a different row after a move to
        // production. NULL sorts LOWEST now, which is what two of the three
        // already did.
        assertEquals("\"v\" ASC NULLS FIRST", Dialect.SQLITE.orderBy("\"v\"", true));
        assertEquals("\"v\" DESC NULLS LAST", Dialect.SQLITE.orderBy("\"v\"", false));
        assertEquals("\"v\" ASC NULLS FIRST", Dialect.POSTGRES.orderBy("\"v\"", true));
        assertEquals("\"v\" DESC NULLS LAST", Dialect.POSTGRES.orderBy("\"v\"", false));
        // MySQL answers a syntax error to NULLS FIRST, so the same placement is
        // spelled with the boolean term its own documentation gives.
        assertEquals("(`v` IS NULL) DESC, `v` ASC", Dialect.MYSQL.orderBy("`v`", true));
        assertEquals("(`v` IS NULL) ASC, `v` DESC", Dialect.MYSQL.orderBy("`v`", false));
    }

    @Test
    @DisplayName("the MariaDB name answers the MariaDB dialect")
    void namesMariaDbSeparately() {
        // Same wire protocol, not the same dialect: the two families have no
        // case-sensitive NO PAD collation in common, so a caller that asked by
        // name and got MYSQL emitted DDL MariaDB 10.11 refuses outright.
        assertSame(Dialect.MARIADB, Dialect.forName("mariadb"));
        assertSame(Dialect.MARIADB, Dialect.forName("MariaDB"));
        assertSame(Dialect.MYSQL, Dialect.forName("mysql"));
        // The engine NAME stays "mysql" for both, because everything that
        // branches on it means the family and the SQL is the same.
        assertEquals("mysql", Dialect.MARIADB.getName());
        assertEquals("mysql", Dialect.MYSQL.getName());
        // And the one difference is the collation.
        assertEquals("LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_nopad_bin",
                Dialect.MARIADB.columnType(Dialect.TEXT));
        assertEquals("LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin",
                Dialect.MYSQL.columnType(Dialect.TEXT));
    }
}
