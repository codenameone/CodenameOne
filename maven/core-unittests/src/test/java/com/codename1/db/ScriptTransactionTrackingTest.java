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

import static org.junit.jupiter.api.Assertions.assertFalse;
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
    private static final class TrackingDatabase extends Database {
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

        void failedPartway(String sql) {
            noteScriptTransactionControl(sql, false);
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
    void aTriggerBodyIsNotTransactionControl() {
        // CREATE TRIGGER ... BEGIN ... END is one statement, and its BEGIN is not a transaction.
        db.ran("CREATE TRIGGER t AFTER INSERT ON x BEGIN UPDATE y SET a = 1; END");
        assertFalse(db.isInTransaction(), "a trigger body is not a transaction");
    }

    @Test
    void aScriptThatFailedPartwayIsAssumedToHaveOpenedItsTransaction() {
        // "BEGIN; INSERT INTO missing_table VALUES(1)" opens a real transaction and then throws.
        // Nothing here can see how far it got, and assuming it closed is the half of that
        // uncertainty that lets a conversion replace the database underneath an open transaction.
        db.failedPartway("BEGIN; INSERT INTO missing_table VALUES(1)");
        assertTrue(db.isInTransaction(),
                "a failed script that contains a BEGIN may have left one open");
    }

    @Test
    void aFailedScriptWithNoBeginChangesNothing() {
        db.failedPartway("INSERT INTO missing_table VALUES(1)");
        assertFalse(db.isInTransaction());
    }

    @Test
    void anOrdinaryScriptLeavesTheStateAlone() {
        db.ran("CREATE TABLE t (a INTEGER); INSERT INTO t VALUES (1)");
        assertFalse(db.isInTransaction());
        db.ran(null);
        assertFalse(db.isInTransaction());
    }
}
