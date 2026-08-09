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
package com.codename1.db;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A transaction opened by {@code execute("BEGIN")} is a real transaction.
 *
 * <p>The API offers two ways to say the same thing and only one of them went through
 * {@code beginTransaction()}, so without this the two disagree. Both directions of that
 * disagreement lose something: an unseen transaction lets {@code changeKey()} replace the database
 * underneath one, installing uncommitted rows; and a flag left set over a transaction that has
 * already ended makes the next commit address one that is not there.
 *
 * <p>The tracking is on {@code Database} itself and needs no engine, so it is pinned here rather
 * than left to the device suites.
 */
class ScriptTransactionTrackingTest {

    /** Exposes the protected hooks; nothing here reaches an engine. */
    private static class TrackingDatabase extends Database {
        @Override
        public void execute(String sql) throws IOException {
        }

        @Override
        public void execute(String sql, String[] params) throws IOException {
        }

        @Override
        public Cursor executeQuery(String sql, String[] params) throws IOException {
            return null;
        }

        @Override
        public Cursor executeQuery(String sql) throws IOException {
            return null;
        }

        @Override
        public void beginTransaction() throws IOException {
            checkBeginTransaction();
        }

        @Override
        public void commitTransaction() throws IOException {
            checkEndTransaction();
            markTransactionEnded();
        }

        @Override
        public void rollbackTransaction() throws IOException {
            checkEndTransaction();
            markTransactionEnded();
        }

        @Override
        public void close() throws IOException {
        }

        void ran(String sql) {
            noteScriptTransactionControl(sql);
        }

        void ranFirstStatementOnly(String sql) {
            noteFirstStatementTransactionControl(sql);
        }

        void failedPartway(String sql) {
            // The same call the ports make from their finally block. A failed script has already
            // run everything before the statement that failed, so its control statements are read
            // exactly as a completed script's are.
            noteScriptTransactionControl(sql);
        }
    }

    /** As Android is: the wrapper ref-counts begins and ends. */
    private static final class NestingDatabase extends TrackingDatabase {
        @Override
        protected boolean supportsNestedTransactions() {
            return true;
        }
    }

    private final TrackingDatabase db = new TrackingDatabase();

    @Test
    void aScriptOpensAndClosesATransaction() {
        db.ran("BEGIN");
        assertTrue(db.isInTransaction(), "execute(\"BEGIN\") opens a real transaction");

        db.ran("COMMIT");
        assertFalse(db.isInTransaction());

        db.ran("BEGIN TRANSACTION");
        assertTrue(db.isInTransaction());
        db.ran("ROLLBACK");
        assertFalse(db.isInTransaction());
    }

    @Test
    void aScriptClosesATransactionTheApiOpened() throws IOException {
        // The direction that goes wrong quietly: the flag stays set over a transaction that has
        // already ended, and the next commit addresses one that is not there.
        db.beginTransaction();
        db.ran("COMMIT");
        assertFalse(db.isInTransaction(), "the transaction ended, whoever said so");
    }

    @Test
    void aCommentBeforeTheKeywordIsStillTheKeyword() {
        // SQLite reads through a leading comment, and the Android port relies on that deliberately
        // to get a ROLLBACK past a statement classifier that reads the first three characters.
        db.ran("/* migration */ BEGIN");
        assertTrue(db.isInTransaction(), "a block comment does not hide the BEGIN");

        db.ran("-- done now\nCOMMIT");
        assertFalse(db.isInTransaction(), "nor does a line comment hide the COMMIT");
    }

    @Test
    void rollingBackToASavepointDoesNotEndTheTransaction() {
        db.ran("BEGIN");
        db.ran("ROLLBACK TO SAVEPOINT one");
        assertTrue(db.isInTransaction(), "unwinding to a savepoint leaves the transaction open");

        db.ran("ROLLBACK");
        assertFalse(db.isInTransaction());
    }

    @Test
    void anOptionalTransactionKeywordDoesNotHideTheSavepoint() {
        // SQLite accepts ROLLBACK TRANSACTION TO [SAVEPOINT] name. Reading only the second word
        // sees TRANSACTION, calls it a bare rollback, and clears the flag while SQLite stays
        // inside the transaction -- and on the simulator discards the whole transaction with it.
        String[] savepointRollbacks = {
            "ROLLBACK TRANSACTION TO one",
            "ROLLBACK TRANSACTION TO SAVEPOINT one",
            "ROLLBACK  TRANSACTION\n TO one",
        };
        for (String statement : savepointRollbacks) {
            db.ran("BEGIN");
            db.ran(statement);
            assertTrue(db.isInTransaction(), "still inside the transaction after: " + statement);
            db.ran("ROLLBACK");
        }
        // But a bare ROLLBACK TRANSACTION does end it.
        db.ran("BEGIN");
        db.ran("ROLLBACK TRANSACTION");
        assertFalse(db.isInTransaction());
    }

    @Test
    void aSavepointRollbackIsRecognisedAcrossAnyWhitespace() {
        // SQL separates words with any whitespace or a comment, so these are all the same
        // statement to the engine. Reading only " TO" would clear the flag on all but the first
        // while SQLite stayed inside the transaction.
        String[] sameStatement = {
            "ROLLBACK TO checkpoint",
            "ROLLBACK\nTO checkpoint",
            "ROLLBACK\tTO checkpoint",
            "ROLLBACK /* here */ TO checkpoint",
            "ROLLBACK -- here\nTO checkpoint",
        };
        for (String statement : sameStatement) {
            db.ran("BEGIN");
            db.ran(statement);
            assertTrue(db.isInTransaction(), "still inside the transaction after: " + statement);
            db.ran("ROLLBACK");
        }
    }

    @Test
    void aTriggerBodyIsNotTransactionControl() {
        // CREATE TRIGGER ... BEGIN ... END is one statement, and its BEGIN is not a transaction.
        db.ran("CREATE TRIGGER t AFTER INSERT ON x BEGIN UPDATE y SET a = 1; END");
        assertFalse(db.isInTransaction(), "a trigger body is not a transaction");
    }

    @Test
    void aScriptThatFailedPartwayLeavesItsTransactionOpen() {
        // "BEGIN; INSERT INTO missing_table VALUES(1)" opens a real transaction and then throws.
        // The BEGIN ran, so the transaction is open, and a conversion must not replace the
        // database underneath it.
        db.failedPartway("BEGIN; INSERT INTO missing_table VALUES(1)");
        assertTrue(db.isInTransaction(), "the BEGIN ran, so the transaction is open");
    }

    @Test
    void aScriptThatFailedAfterCommittingIsNotStillInATransaction() {
        // "BEGIN; COMMIT; INSERT INTO missing_table VALUES(1)" committed before it failed, so the
        // engine is back in autocommit. Reading any BEGIN as still open would hold the flag over a
        // committed transaction and block every key change until the connection was closed.
        db.failedPartway("BEGIN; COMMIT; INSERT INTO missing_table VALUES(1)");
        assertFalse(db.isInTransaction(), "the COMMIT ran too");
    }

    @Test
    void aFailedScriptWithNoBeginChangesNothing() {
        db.failedPartway("INSERT INTO missing_table VALUES(1)");
        assertFalse(db.isInTransaction());
    }

    @Test
    void anOutermostSavepointIsATransaction() {
        // SAVEPOINT outside a transaction starts a real one, held open until that same savepoint is
        // released. Reading it as nothing lets a key change replace the database underneath writes
        // that were never committed.
        db.ran("SAVEPOINT outer");
        assertTrue(db.isInTransaction(), "an outermost savepoint opens a transaction");

        db.ran("SAVEPOINT inner");
        db.ran("RELEASE inner");
        assertTrue(db.isInTransaction(), "releasing an inner savepoint leaves it open");

        db.ran("RELEASE outer");
        assertFalse(db.isInTransaction(), "releasing the outermost one ends it");
    }

    @Test
    void aSavepointInsideATransactionIsJustAMark() {
        db.ran("BEGIN");
        db.ran("SAVEPOINT s");
        db.ran("RELEASE s");
        assertTrue(db.isInTransaction(), "the BEGIN still owns the transaction");
        db.ran("COMMIT");
        assertFalse(db.isInTransaction());
    }

    @Test
    void aSavepointNameIsReadWithoutCaseOrQuotes() {
        // SQLite compares savepoint names as identifiers, so these all name the one savepoint.
        String[] releases = {"RELEASE outer", "RELEASE OUTER", "RELEASE SAVEPOINT outer",
            "RELEASE \"Outer\"", "RELEASE [outer]", "RELEASE `OUTER`", "RELEASE 'outer'"};
        for (String release : releases) {
            db.ran("SAVEPOINT Outer");
            assertTrue(db.isInTransaction(), "opened before: " + release);
            db.ran(release);
            assertFalse(db.isInTransaction(), "ended by: " + release);
        }
    }

    @Test
    void aNonAsciiSavepointNameIsReadWhole() {
        // SQLite takes anything above ASCII as part of an identifier. Reading only the ASCII prefix
        // makes two savepoints that share one look like the same savepoint, so releasing the inner
        // one ends the outer while SQLite still holds the transaction.
        String outer = "caf\u00e9";
        String inner = "caf\u540d";
        db.ran("SAVEPOINT " + outer);
        db.ran("SAVEPOINT " + inner);
        db.ran("RELEASE " + inner);
        assertTrue(db.isInTransaction(), "releasing the inner savepoint left the outer one open");
        db.ran("RELEASE " + outer);
        assertFalse(db.isInTransaction());
    }

    @Test
    void theSameSavepointNameStacks() {
        // SQLite allows the name twice and RELEASE takes the nearest one, so the first release
        // leaves the outer savepoint -- and the transaction -- in place.
        db.ran("SAVEPOINT s");
        db.ran("SAVEPOINT s");
        db.ran("RELEASE s");
        assertTrue(db.isInTransaction(), "the outer savepoint of the same name is still open");
        db.ran("RELEASE s");
        assertFalse(db.isInTransaction(), "now both are released");
    }

    @Test
    void aTypedEndingForgetsTheSavepoint() throws IOException {
        // The savepoint opened it and commitTransaction() ended it. Remembering the name past that
        // makes the next transaction end at the first release of a name that has nothing to do
        // with it.
        db.ran("SAVEPOINT s");
        db.commitTransaction();
        assertFalse(db.isInTransaction());

        db.beginTransaction();
        db.ran("SAVEPOINT s");
        db.ran("RELEASE s");
        assertTrue(db.isInTransaction(), "the BEGIN owns this one, not the savepoint");
        db.commitTransaction();
        assertFalse(db.isInTransaction());
    }

    @Test
    void anIntermediateReleaseTakesTheSavepointsAboveIt() {
        // SQLite releases every savepoint above the one named, including a reuse of the outer
        // name. Counting one name instead of keeping the stack never reached zero here, so the
        // transaction stayed reported open after the engine had ended it -- and every later begin
        // and key change was refused until the connection closed.
        db.ran("SAVEPOINT s");
        db.ran("SAVEPOINT t");
        db.ran("SAVEPOINT s");
        db.ran("RELEASE t");
        assertTrue(db.isInTransaction(), "the outermost s is still open");
        db.ran("RELEASE s");
        assertFalse(db.isInTransaction(), "releasing it ended the transaction");
    }

    @Test
    void rollbackToTakesTheSavepointsAboveItAndKeepsItsOwn() {
        // ROLLBACK TO unwinds to its savepoint and releases everything above it, but keeps the one
        // it names so the caller can roll back to it again. Ignoring it left stale entries below,
        // so the final release never emptied the stack and the transaction was reported open after
        // SQLite had ended it.
        db.ran("SAVEPOINT s");
        db.ran("SAVEPOINT t");
        db.ran("SAVEPOINT s");
        db.ran("ROLLBACK TO t");
        assertTrue(db.isInTransaction(), "t and the outer s are still open");
        db.ran("RELEASE s");
        assertFalse(db.isInTransaction(), "releasing the outermost s ended the transaction");
    }

    @Test
    void rollingBackToASavepointTwiceKeepsIt() {
        // The savepoint survives its own rollback, so a second one is legal and ends nothing.
        db.ran("SAVEPOINT outer");
        db.ran("ROLLBACK TO outer");
        db.ran("ROLLBACK TRANSACTION TO SAVEPOINT outer");
        assertTrue(db.isInTransaction(), "the savepoint it names is still open");
        db.ran("RELEASE outer");
        assertFalse(db.isInTransaction());
    }

    @Test
    void savepointsUnderABeginEndNothing() {
        // Under a BEGIN the savepoints are marks inside a transaction the BEGIN owns. Releasing
        // all of them ends none of it.
        db.ran("BEGIN");
        db.ran("SAVEPOINT s");
        db.ran("SAVEPOINT t");
        db.ran("RELEASE s");
        assertTrue(db.isInTransaction(), "the BEGIN still owns the transaction");
        db.ran("COMMIT");
        assertFalse(db.isInTransaction());
    }

    @Test
    void releasingAnUnopenedSavepointReleasesNothing() {
        // SQLite reports an error and releases nothing, so neither does the tracking.
        db.ran("SAVEPOINT s");
        db.ran("RELEASE nosuch");
        assertTrue(db.isInTransaction(), "nothing was released");
        db.ran("RELEASE s");
        assertFalse(db.isInTransaction());
    }

    @Test
    void releasingSomethingElseLeavesTheTransactionOpen() {
        db.ran("SAVEPOINT outer");
        db.ran("RELEASE unrelated");
        assertTrue(db.isInTransaction(), "that was not this transaction's ending");
        db.ran("ROLLBACK");
        assertFalse(db.isInTransaction());
    }

    @Test
    void aBeginModeIsReadFromTheKeywordAfterBegin() {
        // The words are ordinary text anywhere else, and a port that searched the statement for
        // them would take a write lock that the same SQL takes on no other platform.
        assertEquals("DEFERRED", Database.beginTransactionMode("BEGIN"));
        assertEquals("DEFERRED", Database.beginTransactionMode("BEGIN TRANSACTION"));
        assertEquals("IMMEDIATE", Database.beginTransactionMode("BEGIN IMMEDIATE"));
        assertEquals("EXCLUSIVE", Database.beginTransactionMode("begin exclusive transaction"));
        assertEquals("IMMEDIATE", Database.beginTransactionMode("BEGIN /* go */ IMMEDIATE"));
        assertEquals("DEFERRED", Database.beginTransactionMode("/* IMMEDIATE migration */ BEGIN"));
        assertEquals("DEFERRED",
                Database.beginTransactionMode("BEGIN /* EXCLUSIVE note */ TRANSACTION"));
        assertEquals("DEFERRED", Database.beginTransactionMode(null));
    }

    @Test
    void aPathIsReducedToOneSpellingForTheRegistry() {
        // Two names for one file have to be one registry entry, or the claim a key change takes
        // does not cover the connection filed under the other name.
        assertEquals("/data/app.db", Database.normalizeDatabasePathKey("/data/./app.db"));
        assertEquals("/data/app.db", Database.normalizeDatabasePathKey("/data//app.db"));
        assertEquals("/data/app.db", Database.normalizeDatabasePathKey("/data/tmp/../app.db"));
        assertEquals("/app.db", Database.normalizeDatabasePathKey("/../app.db"));
        assertEquals("data/app.db", Database.normalizeDatabasePathKey("data/app.db"));
        assertEquals("/", Database.normalizeDatabasePathKey("/"));
        assertNull(Database.normalizeDatabasePathKey(null));
        assertEquals("/data/app.db", Database.normalizeDatabasePathKey("/data/app.db"));
    }

    @Test
    void nestedLegacyTransactionsEndOneAtATime() throws IOException {
        // Under the legacy hint Android allows nesting because it used to, and its engine
        // ref-counts. Clearing the flag on the first commit would report no transaction while the
        // outer one still held uncommitted rows, and a key change would be allowed over them.
        NestingDatabase nesting = new NestingDatabase();
        Database.setLegacyBehavior(true);
        try {
            nesting.beginTransaction();
            nesting.beginTransaction();
            nesting.commitTransaction();
            assertTrue(nesting.isInTransaction(), "the outer transaction is still open");
            nesting.commitTransaction();
            assertFalse(nesting.isInTransaction(), "now both have ended");
        } finally {
            Database.setLegacyBehavior(false);
        }
    }

    @Test
    void aScriptCommitEndsEveryNestedBegin() throws IOException {
        // A COMMIT in a script ends the transaction outright, whatever the count says: the engine
        // has ended it, so holding the flag would block every key change until the connection
        // closed.
        NestingDatabase nesting = new NestingDatabase();
        Database.setLegacyBehavior(true);
        try {
            nesting.beginTransaction();
            nesting.beginTransaction();
            nesting.ran("COMMIT");
            assertFalse(nesting.isInTransaction(), "the engine ended it, count or no count");
        } finally {
            Database.setLegacyBehavior(false);
        }
    }

    @Test
    void theLegacyPathTracksTheStatementItActuallyRan() {
        // Under the legacy hint a script runs as far as its first statement and the rest is
        // discarded. Reading the whole string would credit a COMMIT that never executed and report
        // no transaction over one that is open -- which is what lets a key change run across it.
        db.ranFirstStatementOnly("BEGIN");
        assertTrue(db.isInTransaction(), "the BEGIN it ran opened a transaction");

        db.ran("ROLLBACK");
        db.ranFirstStatementOnly("BEGIN; COMMIT");
        assertTrue(db.isInTransaction(), "only the BEGIN ran, so the transaction is still open");

        db.ranFirstStatementOnly("COMMIT");
        assertFalse(db.isInTransaction());

        db.ranFirstStatementOnly("");
        db.ranFirstStatementOnly(null);
        assertFalse(db.isInTransaction(), "nothing to read, nothing to change");
    }

    @Test
    void anEngineThatCannotNestStillRejectsTheSecondBegin() throws IOException {
        // The legacy hint does not make nesting work where it never worked. On those engines the
        // second BEGIN reaches SQLite and fails, and the port clears its flag on the way out --
        // so accepting the call here would report no transaction over one that is still open.
        Database.setLegacyBehavior(true);
        try {
            db.beginTransaction();
            boolean rejected = false;
            try {
                db.beginTransaction();
            } catch (IOException expected) {
                rejected = true;
            }
            assertTrue(rejected, "a port that cannot nest rejects the second begin, hint or no hint");
            assertTrue(db.isInTransaction(), "and the first transaction is untouched");
        } finally {
            Database.setLegacyBehavior(false);
        }
    }

    @Test
    void anOrdinaryScriptLeavesTheStateAlone() {
        db.ran("CREATE TABLE t (a INTEGER); INSERT INTO t VALUES (1)");
        assertFalse(db.isInTransaction());
        db.ran(null);
        assertFalse(db.isInTransaction());
    }
}
