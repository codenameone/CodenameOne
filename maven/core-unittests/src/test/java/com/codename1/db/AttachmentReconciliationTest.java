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

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the reconciliation does with an attachment it cannot hold.
 *
 * <p>An ATTACH cannot always be keyed exactly before it runs -- a relative name resolves against
 * the process directory for SQLite and against the port's database directory here, and a bound
 * parameter is not read at all -- so the reconciliation afterwards is the last line of defence.
 * When it finds a live attachment on a file something else has claimed, it has to take the
 * attachment off the connection: leaving it means a delete or a key change proceeds against a
 * file SQLite still holds.
 *
 * <p>The awkward part is that one file can be attached under several schema names at once, and
 * the schemas are the only handles a DETACH can be written against. Detaching one of them and
 * calling the file released leaves the other live -- exactly the state being undone.
 *
 * <p>No engine is involved: the engine's answer is fed in, so the bookkeeping is pinned here
 * rather than left to the device suites, which cannot arrange the claim.
 */
class AttachmentReconciliationTest extends UITestBase {

    /** One row of {@code PRAGMA database_list}. */
    private static final class Attachment {
        private final String schema;
        private final String file;

        Attachment(String schema, String file) {
            this.schema = schema;
            this.file = file;
        }
    }

    /** Answers the reconciliation's query from a fixed list and records what it runs. */
    private static final class ReconcilingDatabase extends Database {
        private final List<Attachment> attached = new ArrayList<Attachment>();
        private final List<String> statements = new ArrayList<String>();

        @Override
        public void execute(String sql) throws IOException {
            statements.add(sql);
        }

        @Override
        public void execute(String sql, String[] params) throws IOException {
            statements.add(sql);
        }

        @Override
        public Cursor executeQuery(String sql) throws IOException {
            requireQueryStatement(sql);
            return new ListCursor(attached);
        }

        @Override
        public Cursor executeQuery(String sql, String[] params) throws IOException {
            return executeQuery(sql);
        }

        @Override
        public void beginTransaction() throws IOException {
        }

        @Override
        public void commitTransaction() throws IOException {
        }

        @Override
        public void rollbackTransaction() throws IOException {
        }

        @Override
        public void close() throws IOException {
        }

        /** The call every port makes after running a script, which is what reconciles. */
        void ran(String sql) {
            noteScriptTransactionControl(sql);
        }

        /** The call every port makes before running one, which is what reports a refusal. */
        void about(String sql) throws IOException {
            reserveAttachments(sql);
        }

        List<String> detaches() {
            List<String> found = new ArrayList<String>();
            for (String sql : statements) {
                if (sql.toUpperCase().startsWith("DETACH")) {
                    found.add(sql);
                }
            }
            return found;
        }
    }

    /** A cursor over the rows PRAGMA database_list would have returned. */
    private static final class ListCursor implements Cursor, Row {
        private final List<Attachment> rows;
        private int position = -1;

        ListCursor(List<Attachment> rows) {
            this.rows = rows;
        }

        public boolean first() {
            position = 0;
            return !rows.isEmpty();
        }

        public boolean last() {
            position = rows.size() - 1;
            return !rows.isEmpty();
        }

        public boolean next() {
            position++;
            return position < rows.size();
        }

        public boolean prev() {
            position--;
            return position >= 0;
        }

        public int getPosition() {
            return position;
        }

        public boolean position(int row) {
            position = row;
            return row >= 0 && row < rows.size();
        }

        public void close() {
        }

        public Row getRow() {
            return this;
        }

        public int getColumnCount() {
            return 3;
        }

        public String getColumnName(int index) {
            return index == 1 ? "name" : (index == 2 ? "file" : "seq");
        }

        public int getColumnIndex(String columnName) {
            return "name".equals(columnName) ? 1 : ("file".equals(columnName) ? 2 : 0);
        }

        public byte[] getBlob(int index) {
            return null;
        }

        public double getDouble(int index) {
            return 0;
        }

        public float getFloat(int index) {
            return 0;
        }

        public int getInteger(int index) {
            return position;
        }

        public long getLong(int index) {
            return position;
        }

        public short getShort(int index) {
            return (short) position;
        }

        public String getString(int index) {
            Attachment row = rows.get(position);
            return index == 1 ? row.schema : (index == 2 ? row.file : String.valueOf(position));
        }
    }

    @FormTest
    void everySchemaOnAnUnholdableFileIsDetached() throws Exception {
        ReconcilingDatabase db = new ReconcilingDatabase();
        // The same file, attached twice: SQLite allows it, and PRAGMA database_list reports one
        // row per schema with the one path between them.
        db.attached.add(new Attachment("aux1", "/data/shared.db"));
        db.attached.add(new Attachment("aux2", "/data/shared.db"));
        String key = "file:///data/shared.db";

        // Something else has the file: a key change holds it for the whole rewrite, which is the
        // claim a reconciliation can run into without any way to have known beforehand.
        Database.requireSoleConnectionForKeyChange(key);
        try {
            db.ran("ATTACH DATABASE 'shared.db' AS aux1");

            List<String> detaches = db.detaches();
            assertEquals(2, detaches.size(),
                    "both schemas on the claimed file are detached, not just the last one seen: "
                    + detaches);
            assertTrue(detaches.get(0).indexOf("aux1") > 0, "the first schema: " + detaches.get(0));
            assertTrue(detaches.get(1).indexOf("aux2") > 0, "the second: " + detaches.get(1));
        } finally {
            Database.releaseKeyChangeClaim(key);
        }
    }

    @FormTest
    void theRefusalIsReportedFromTheNextStatement() throws Exception {
        ReconcilingDatabase db = new ReconcilingDatabase();
        db.attached.add(new Attachment("aux", "/data/claimed.db"));
        String key = "file:///data/claimed.db";

        Database.requireSoleConnectionForKeyChange(key);
        try {
            // Ports reconcile from a finally, so this call cannot be the one that throws -- it
            // would replace whatever failure the statement itself was reporting.
            db.ran("ATTACH DATABASE 'claimed.db' AS aux");

            IOException refusal = assertThrows(IOException.class, () -> db.about("SELECT 1"),
                    "the next statement on the connection reports it");
            assertNotNull(refusal.getMessage());
            assertTrue(refusal.getMessage().indexOf("claimed.db") > 0,
                    "and names the database: " + refusal.getMessage());

            // Reported once. A refusal that never cleared would fail every later statement on a
            // connection whose attachment was already dealt with.
            db.about("SELECT 2");
        } finally {
            Database.releaseKeyChangeClaim(key);
        }
    }

    @FormTest
    void anAttachmentThatCanBeHeldIsLeftAlone() throws Exception {
        ReconcilingDatabase db = new ReconcilingDatabase();
        db.attached.add(new Attachment("aux", "/data/free.db"));

        db.ran("ATTACH DATABASE 'free.db' AS aux");

        assertEquals(0, db.detaches().size(), "nothing claims the file, so it stays attached");
        db.about("SELECT 1");
        try {
            // Registered by the reconciliation, so it is now protected the way an opened database
            // is -- which is the point of noticing it at all.
            Database.releaseOpenDatabase("file:///data/free.db");
        } finally {
            db.close();
        }
    }

    @FormTest
    void anAttachIsFoundPastTheCommentsAroundIt() throws Exception {
        // Every one of these is a statement SQLite runs and an attachment it makes. The reserve
        // has to see the target in all of them, because a reservation is the only refusal that
        // can still stop the attach -- afterwards there is nothing left to refuse.
        String[] scripts = {
            "ATTACH DATABASE '/data/guarded.db' AS aux",
            "/* migration */ ATTACH DATABASE '/data/guarded.db' AS aux",
            "-- migration\nATTACH DATABASE '/data/guarded.db' AS aux",
            "  \n\t ATTACH DATABASE '/data/guarded.db' AS aux",
            "ATTACH /* which */ DATABASE /* one */ '/data/guarded.db' AS aux",
            "ATTACH DATABASE/* no space */'/data/guarded.db' AS aux",
        };
        String key = "file:///data/guarded.db";
        for (String script : scripts) {
            ReconcilingDatabase db = new ReconcilingDatabase();
            // Claimed for a key change, so reserving the target is refused -- which is how this
            // can tell the target was read at all.
            Database.requireSoleConnectionForKeyChange(key);
            try {
                final ReconcilingDatabase reserving = db;
                final String sql = script;
                assertThrows(IOException.class, () -> reserving.about(sql),
                        "the target of this ATTACH was never reserved: " + script);
            } finally {
                Database.releaseKeyChangeClaim(key);
            }
        }
    }

    @FormTest
    void anAttachNamedByAnExpressionIsRefusedBeforeItRuns() throws Exception {
        // SQLite evaluates an expression where the filename goes, and both of these attach a real
        // file: "'/tmp/' || 'b.db'" and "replace('/tmp/XXX.db','XXX','b')". Reading the first
        // token called the first one "/tmp/" -- a reservation on the wrong file, which reads as
        // protection and is not -- and the second one nothing at all. There is no moment after
        // this to refuse: the engine holds the file, and inside a transaction that has written
        // through the attachment even the detach is rejected.
        String[] expressions = {
            "ATTACH '/data/' || 'b.db' AS aux",
            "ATTACH DATABASE replace('/data/XXX.db','XXX','b') AS aux",
            "ATTACH some_column AS aux",
            // A double quoted name is an identifier to SQLite, read as a filename only when no
            // column answers to it -- which cannot be known from here either.
            "ATTACH \"/data/b.db\" AS aux",
            // No whitespace anywhere, which SQLite attaches perfectly well.
            "ATTACH('/data/' || 'b.db')AS aux",
            // The reviewer's script, where the attach sits between other statements.
            "BEGIN; ATTACH '/data/' || 'b.db' AS aux; CREATE TABLE aux.t(x);",
        };
        for (String expression : expressions) {
            final ReconcilingDatabase db = new ReconcilingDatabase();
            final String sql = expression;
            IOException refused = assertThrows(IOException.class, () -> db.about(sql),
                    "an ATTACH this cannot resolve has to be refused: " + expression);
            assertTrue(refused.getMessage().indexOf("literal") > 0,
                    "and say what to write instead: " + refused.getMessage());
        }

        // The forms that can be accounted for still run. A literal is resolved outright, a
        // placeholder is covered by the parameterized overload, and :memory: has no file at all.
        String[] allowed = {
            "ATTACH '/data/plain.db' AS aux",
            "ATTACH DATABASE ? AS aux",
            "ATTACH :name AS aux",
            "ATTACH '/data/it''s.db' AS aux",
            "ATTACH ':memory:' AS aux",
        };
        for (String statement : allowed) {
            new ReconcilingDatabase().about(statement);
        }
    }

    @FormTest
    void theLegacyHintKeepsAnExpressionAttachWorking() throws Exception {
        // The refusal is new, and the hint restores what used to work: before the registry
        // existed an expression attached like any other statement.
        Database.setLegacyBehavior(true);
        try {
            new ReconcilingDatabase().about("ATTACH '/data/' || 'b.db' AS aux");
        } finally {
            Database.setLegacyBehavior(false);
        }
    }

    @FormTest
    void attachmentControlCannotBeRunThroughExecuteQuery() throws Exception {
        ReconcilingDatabase db = new ReconcilingDatabase();

        IOException refused = assertThrows(IOException.class,
                () -> db.executeQuery("ATTACH DATABASE 'other.db' AS aux"),
                "a cursor runs its statement when it is stepped, which is after the bookkeeping");
        assertTrue(refused.getMessage().indexOf("executeQuery") >= 0, refused.getMessage());

        assertThrows(IOException.class, () -> db.executeQuery("DETACH DATABASE aux"),
                "and a detach loses a reservation the same way");
    }
}
