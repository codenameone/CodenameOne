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
package com.codename1.migration;

import com.codename1.impl.migration.MigrationTarget;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A database that understands exactly the statements the history table issues and records
 * everything else. It models the one property of an engine the algorithm depends on: whether a
 * rollback undoes schema changes.
 */
final class FakeMigrationTarget implements MigrationTarget {
    static final class HistoryRow {
        long rank;
        String version;
        String description;
        String type;
        String script;
        Long checksum;
        long time;
        boolean success;

        HistoryRow copy() {
            HistoryRow c = new HistoryRow();
            c.rank = rank;
            c.version = version;
            c.description = description;
            c.type = type;
            c.script = script;
            c.checksum = checksum;
            c.time = time;
            c.success = success;
            return c;
        }
    }

    String dialect = "sqlite";
    boolean ddlTransactions = true;
    boolean userObjects;
    boolean lockAvailable = true;
    boolean historyExists;
    List<HistoryRow> history = new ArrayList<HistoryRow>();
    List<String> statements = new ArrayList<String>();
    List<String> locks = new ArrayList<String>();
    boolean dropped;
    /// Run once, as the next transaction opens and before it is one: what another process
    /// committed between this run reading the history and this run taking the write lock.
    Runnable beforeNextTransaction;
    /// Run once, as the engine asks whether the database has tables of its own: what
    /// another process did after this run looked for the history table and found none.
    Runnable beforeUserObjectsAnswer;
    private boolean transaction;
    private boolean savedExists;
    private List<HistoryRow> savedHistory;
    private List<String> savedStatements;

    @Override
    public String dialect() {
        return dialect;
    }

    @Override
    public String[] split(String script) {
        List<String> out = new ArrayList<String>();
        for (String part : script.split(";")) {
            if (part.trim().length() > 0) {
                out.add(part.trim());
            }
        }
        return out.toArray(new String[0]);
    }

    private boolean isHistory(String sql) {
        return sql.contains("schema_history") || sql.contains("my_history");
    }

    @Override
    public void execute(String sql, Object[] params) throws IOException {
        if (sql.startsWith("CREATE TABLE") && isHistory(sql)) {
            historyExists = true;
            return;
        }
        if (sql.startsWith("CREATE INDEX") && isHistory(sql)) {
            return;
        }
        if (sql.startsWith("INSERT INTO") && isHistory(sql)) {
            HistoryRow row = new HistoryRow();
            row.rank = ((Long) params[0]).longValue();
            row.version = (String) params[1];
            row.description = (String) params[2];
            row.type = (String) params[3];
            row.script = (String) params[4];
            row.checksum = (Long) params[5];
            row.time = ((Long) params[7]).longValue();
            row.success = sql.endsWith("1)") || sql.endsWith("TRUE)");
            history.add(row);
            return;
        }
        if (sql.startsWith("DELETE FROM") && isHistory(sql)) {
            List<HistoryRow> kept = new ArrayList<HistoryRow>();
            for (HistoryRow row : history) {
                if (row.success) {
                    kept.add(row);
                }
            }
            history = kept;
            return;
        }
        if (sql.startsWith("UPDATE") && isHistory(sql)) {
            for (HistoryRow row : history) {
                if (row.rank == ((Long) params[2]).longValue()) {
                    row.checksum = (Long) params[0];
                    row.description = (String) params[1];
                }
            }
            return;
        }
        if (sql.startsWith("BEGIN")) {
            transaction = true;
            return;
        }
        if (sql.startsWith("COMMIT")) {
            transaction = false;
            return;
        }
        if (sql.contains("FAIL")) {
            throw new IOException("syntax error near FAIL");
        }
        statements.add(sql);
    }

    @Override
    public List<String[]> query(String sql, Object[] params) {
        List<String[]> out = new ArrayList<String[]>();
        if (sql.startsWith("SELECT COUNT(*)")) {
            int count = 0;
            for (HistoryRow row : history) {
                if (params[0].equals(row.version)) {
                    count++;
                }
            }
            out.add(new String[] {String.valueOf(count)});
        } else if (sql.startsWith("SELECT MAX(")) {
            Long max = null;
            for (HistoryRow row : history) {
                if (max == null || row.rank > max.longValue()) {
                    max = Long.valueOf(row.rank);
                }
            }
            out.add(new String[] {max == null ? null : max.toString()});
        } else if (isHistory(sql)) {
            for (HistoryRow row : history) {
                out.add(new String[] {String.valueOf(row.rank), row.version, row.description, row.type, row.script,
                        row.checksum == null ? null : row.checksum.toString(), "2026-01-01 00:00:00",
                        String.valueOf(row.time), row.success ? "1" : "0"});
            }
        }
        return out;
    }

    @Override
    public boolean tableExists(String table) {
        return historyExists;
    }

    @Override
    public boolean hasUserObjects() {
        if (beforeUserObjectsAnswer != null) {
            Runnable doneElsewhere = beforeUserObjectsAnswer;
            beforeUserObjectsAnswer = null;
            doneElsewhere.run();
        }
        return userObjects;
    }

    @Override
    public boolean supportsDdlTransactions() {
        return ddlTransactions;
    }

    @Override
    public void begin() throws IOException {
        if (transaction) {
            throw new IOException("nested transaction");
        }
        if (beforeNextTransaction != null) {
            Runnable committedElsewhere = beforeNextTransaction;
            beforeNextTransaction = null;
            committedElsewhere.run();
        }
        transaction = true;
        savedExists = historyExists;
        savedHistory = new ArrayList<HistoryRow>();
        for (HistoryRow row : history) {
            savedHistory.add(row.copy());
        }
        savedStatements = new ArrayList<String>(statements);
    }

    @Override
    public void commit() {
        transaction = false;
    }

    @Override
    public void rollback() {
        transaction = false;
        if (ddlTransactions && savedHistory != null) {
            historyExists = savedExists;
            history = savedHistory;
            statements = savedStatements;
        }
    }

    @Override
    public boolean isTransactionActive() {
        return transaction;
    }

    @Override
    public boolean lock(String name, int waitSeconds) {
        if (lockAvailable) {
            locks.add("lock " + name);
        }
        return lockAvailable;
    }

    @Override
    public void unlock(String name) {
        locks.add("unlock " + name);
    }

    @Override
    public String currentUser() {
        return "tester";
    }

    @Override
    public void dropAllObjects() {
        dropped = true;
        historyExists = false;
        history.clear();
    }

    @Override
    public Object connection() {
        return this;
    }

    int doneCalls;

    @Override
    public void done() {
        doneCalls++;
    }
}
