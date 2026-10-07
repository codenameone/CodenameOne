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
package com.codename1.impl.migration;

import com.codename1.migration.MigrateResult;
import com.codename1.migration.MigrationContext;
import com.codename1.migration.MigrationException;
import com.codename1.migration.MigrationInfo;
import com.codename1.migration.MigrationSet;
import com.codename1.migration.MigrationState;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The migration algorithm, identical in an application and on a server: resolve the scripts
/// for this engine, read the history, check one against the other, apply what is missing.
///
/// Everything engine specific sits behind [MigrationTarget].
///
/// Internal migration runtime; not an application API.
/// @hidden
@com.codename1.impl.SharedWithBackend
public final class MigrationEngine {
    /// The settings of one run. A plain record the public `Migrator` fills in.
    public static final class Options {
        /// Overrides the set's history table when not null.
        public String table;
        /// Adopt a database that has tables and no history, instead of refusing it.
        public boolean baselineOnMigrate;
        /// The version an adopted database is taken to be at.
        public String baselineVersion = "1";
        /// The description of the baseline row.
        public String baselineDescription = "<< Flyway Baseline >>";
        /// Check the history against the scripts before applying anything.
        public boolean validateOnMigrate = true;
        /// Apply a migration older than the newest applied one instead of refusing it.
        public boolean outOfOrder;
        /// Refuse [MigrationEngine#clean()].
        public boolean cleanDisabled = true;
        /// The highest version to apply, or null for all.
        public String target;
        /// Tolerate a history newer than this build.
        public boolean ignoreFutureMigrations = true;
        /// The name recorded in the history, or null for the database user.
        public String installedBy;
        /// How long to wait for another process's migration, in roughly one-second slices.
        public int lockRetryCount = 50;
    }

    private final MigrationTarget target;
    private final MigrationSet set;
    private final Options options;
    private final HistoryTable history;

    /// Binds a set to a target.
    public MigrationEngine(MigrationTarget target, MigrationSet set, Options options) {
        this.target = target;
        this.set = set;
        this.options = options;
        this.history = new HistoryTable(target, options.table != null ? options.table : set.getTable());
    }

    // ------------------------------------------------------------------ resolution

    private List<MigrationEntry> resolve(boolean repeatable) throws MigrationException {
        MigrationEntry[] all = set.entries();
        String dialect = target.dialect();
        List<MigrationEntry> chosen = new ArrayList<MigrationEntry>();
        boolean[] seen = new boolean[all.length];
        for (int i = 0; i < all.length; i++) {
            if (seen[i] || (all[i].version() == null) != repeatable) {
                continue;
            }
            MigrationEntry common = null;
            MigrationEntry specific = null;
            for (int j = i; j < all.length; j++) {
                if (seen[j] || (all[j].version() == null) != repeatable || !same(all[i], all[j])) {
                    continue;
                }
                seen[j] = true;
                MigrationEntry entry = all[j];
                if (entry.dialect() == null) {
                    if (common != null) {
                        throw new MigrationException(MigrationException.INVALID_SET,
                                "Two migrations share " + label(entry) + " in set " + set.getName());
                    }
                    common = entry;
                } else if (entry.dialect().equals(dialect)) {
                    if (specific != null) {
                        throw new MigrationException(MigrationException.INVALID_SET, "Two " + dialect
                                + " migrations share " + label(entry) + " in set " + set.getName());
                    }
                    specific = entry;
                }
            }
            if (specific != null && common != null) {
                throw new MigrationException(MigrationException.INVALID_SET, label(all[i]) + " in set "
                        + set.getName() + " has both a script for every engine and one for " + dialect);
            }
            MigrationEntry pick = specific != null ? specific : common;
            if (pick == null) {
                throw new MigrationException(MigrationException.INVALID_SET, label(all[i]) + " in set "
                        + set.getName() + " has no script for " + dialect);
            }
            chosen.add(pick);
        }
        // Insertion sort: the list is short and usually already in order.
        for (int i = 1; i < chosen.size(); i++) {
            MigrationEntry moving = chosen.get(i);
            int at = i;
            while (at > 0 && order(chosen.get(at - 1), moving) > 0) {
                chosen.set(at, chosen.get(at - 1));
                at--;
            }
            chosen.set(at, moving);
        }
        return chosen;
    }

    private static boolean same(MigrationEntry a, MigrationEntry b) {
        if (a.version() == null) {
            return a.description().equals(b.description());
        }
        return MigrationVersion.compare(a.version(), b.version()) == 0;
    }

    private static int order(MigrationEntry a, MigrationEntry b) {
        if (a.version() == null) {
            return a.description().compareTo(b.description());
        }
        return MigrationVersion.compare(a.version(), b.version());
    }

    private static String label(MigrationEntry entry) {
        return entry.version() == null ? "repeatable migration '" + entry.description() + "'"
                : "version " + entry.version();
    }

    private static MigrationEntry find(List<MigrationEntry> resolved, String version) {
        for (MigrationEntry entry : resolved) {
            if (MigrationVersion.compare(entry.version(), version) == 0) {
                return entry;
            }
        }
        return null;
    }

    private static String highest(List<MigrationEntry> resolved) {
        return resolved.isEmpty() ? null : resolved.get(resolved.size() - 1).version();
    }

    // ------------------------------------------------------------------ history reading

    private static String baselineOf(List<HistoryTable.Row> rows) {
        for (HistoryTable.Row row : rows) {
            if (MigrationEntry.BASELINE.equals(row.type)) {
                return row.version;
            }
        }
        return null;
    }

    /// The newest version the history records as applied, baseline included.
    private static String current(List<HistoryTable.Row> rows) {
        String max = null;
        for (HistoryTable.Row row : rows) {
            if (row.version != null && row.success && MigrationVersion.isValid(row.version)
                    && (max == null || MigrationVersion.compare(row.version, max) > 0)) {
                max = row.version;
            }
        }
        return max;
    }

    private static boolean applied(List<HistoryTable.Row> rows, String version) {
        for (HistoryTable.Row row : rows) {
            if (row.version != null && row.success && !MigrationEntry.BASELINE.equals(row.type)
                    && MigrationVersion.isValid(row.version)
                    && MigrationVersion.compare(row.version, version) == 0) {
                return true;
            }
        }
        return false;
    }

    private static HistoryTable.Row latestRepeatable(List<HistoryTable.Row> rows, String description) {
        HistoryTable.Row latest = null;
        for (HistoryTable.Row row : rows) {
            if (row.version == null && row.success && description.equals(row.description)) {
                latest = row;
            }
        }
        return latest;
    }

    /// Whether the history's newest successful run of a repeatable migration is the script
    /// this build carries: the one test of "has nothing to do" that `migrate`, the check
    /// repeated inside the migration's transaction and `validate` all share.
    private static boolean upToDate(List<HistoryTable.Row> rows, MigrationEntry entry) {
        HistoryTable.Row latest = latestRepeatable(rows, entry.description());
        if (latest == null) {
            return false;
        }
        Integer checksum = entry.checksum();
        return checksum == null ? latest.checksum == null
                : latest.checksum != null && latest.checksum.intValue() == checksum.intValue();
    }

    private void refuseFailed(List<HistoryTable.Row> rows) throws MigrationException {
        for (HistoryTable.Row row : rows) {
            if (!row.success) {
                throw new MigrationException(MigrationException.FAILED_MIGRATION_PRESENT,
                        "Migration " + row.script + " failed on an earlier run and may be half applied. "
                                + "Remove what it left behind, then call repair() before migrating again.");
            }
        }
    }

    /// Checks the history against the resolved scripts. A history newer than this build is
    /// reported under its own code, because an application handles it differently from drift.
    ///
    /// `repeatable` is this build's repeatable migrations when the caller is `validate`,
    /// and null when it is `migrate`, which is about to run whichever of them is due.
    /// `validate` answers whether the database is at this build's migrations, and a view
    /// or a procedure whose script changed, or never ran, is a database that is not: it
    /// used to pass, and a deploy check built on it accepted stale ones.
    ///
    /// A repeatable row whose script is gone is deliberately not a problem here. Retiring
    /// such a script is ordinary, `migrate` does not refuse it, and nothing removes the
    /// row -- so reporting it would fail every later check with no command that could
    /// clear it. `info` shows the row as missing.
    private void check(List<HistoryTable.Row> rows, List<MigrationEntry> resolved,
            List<MigrationEntry> repeatable, boolean pendingAllowed, boolean drift, List<String> warnings)
            throws MigrationException {
        String baseline = baselineOf(rows);
        String newest = highest(resolved);
        String current = current(rows);
        StringBuilder problems = new StringBuilder();
        for (HistoryTable.Row row : rows) {
            if (!row.success || row.version == null || MigrationEntry.BASELINE.equals(row.type)
                    || !MigrationVersion.isValid(row.version)) {
                continue;
            }
            MigrationEntry entry = find(resolved, row.version);
            if (entry == null) {
                if (newest == null || MigrationVersion.compare(row.version, newest) > 0) {
                    if (!options.ignoreFutureMigrations) {
                        throw new MigrationException(MigrationException.FUTURE_SCHEMA, "The database is at version "
                                + row.version + ", newer than the newest migration this build carries ("
                                + (newest == null ? "none" : newest) + ")");
                    }
                    warnings.add("Applied version " + row.version + " is newer than this build");
                } else if (drift && (baseline == null || MigrationVersion.compare(row.version, baseline) > 0)) {
                    problem(problems, "applied version " + row.version + " (" + row.script
                            + ") is not among this build's migrations");
                }
                continue;
            }
            if (!drift) {
                continue;
            }
            Integer checksum = entry.checksum();
            if (checksum != null && row.checksum != null && checksum.intValue() != row.checksum.intValue()) {
                problem(problems, "checksum of version " + row.version + " (" + entry.scriptName()
                        + ") is " + checksum + " and the database recorded " + row.checksum
                        + "; an applied migration was edited");
            }
            if (!entry.type().equals(row.type)
                    && !(entry.java() != null && "JAVA".equals(row.type))) {
                problem(problems, "version " + row.version + " was applied as " + row.type + " and is now "
                        + entry.type());
            }
            if (!entry.description().equals(row.description)) {
                problem(problems, "description of version " + row.version + " was " + row.description
                        + " and is now " + entry.description() + "; repair the history after confirming the rename");
            }
        }
        if (drift) {
            for (MigrationEntry entry : resolved) {
                if (applied(rows, entry.version())
                        || baseline != null && MigrationVersion.compare(entry.version(), baseline) <= 0) {
                    continue;
                }
                if (current != null && MigrationVersion.compare(entry.version(), current) < 0) {
                    if (!options.outOfOrder) {
                        problem(problems, "version " + entry.version() + " (" + entry.scriptName()
                                + ") was never applied and is older than the applied version " + current
                                + "; renumber it or allow out-of-order migrations");
                    }
                } else if (!pendingAllowed && !aboveTarget(entry)) {
                    problem(problems, "version " + entry.version() + " (" + entry.scriptName()
                            + ") has not been applied");
                }
            }
        }
        if (drift && !pendingAllowed && repeatable != null) {
            for (MigrationEntry entry : repeatable) {
                if (latestRepeatable(rows, entry.description()) == null) {
                    problem(problems, label(entry) + " (" + entry.scriptName() + ") has not been applied");
                } else if (!upToDate(rows, entry)) {
                    problem(problems, label(entry) + " (" + entry.scriptName()
                            + ") changed since it was applied and has not been applied again");
                }
            }
        }
        if (problems.length() > 0) {
            throw new MigrationException(MigrationException.VALIDATE_FAILED, "Schema history "
                    + history.name() + " does not match the migrations of set " + set.getName() + ": " + problems);
        }
    }

    private static void problem(StringBuilder problems, String text) {
        if (problems.length() > 0) {
            problems.append("; ");
        }
        problems.append(text);
    }

    private boolean aboveTarget(MigrationEntry entry) {
        return options.target != null && MigrationVersion.compare(entry.version(), options.target) > 0;
    }

    // ------------------------------------------------------------------ locking

    private void lock() throws IOException {
        if (!target.lock(lockName(), options.lockRetryCount)) {
            throw new MigrationException(MigrationException.LOCK_TIMEOUT, "Another process has been migrating "
                    + history.name() + " for more than " + options.lockRetryCount + " seconds");
        }
    }

    private void unlock() {
        try {
            target.unlock(lockName());
        } catch (IOException ignored) {
            // The lock belongs to the connection; closing it releases what this could not.
        }
    }

    private String lockName() {
        return "cn1_flyway_" + history.name();
    }

    // ------------------------------------------------------------------ commands

    /// Applies every pending migration.
    public MigrateResult migrate() throws IOException {
        List<MigrationEntry> versioned = resolve(false);
        List<MigrationEntry> repeatable = resolve(true);
        if (options.target != null && !MigrationVersion.isValid(options.target)) {
            throw new MigrationException(MigrationException.INVALID_SET, "Not a version: " + options.target);
        }
        try {
            lock();
            try {
                return migrateLocked(versioned, repeatable);
            } finally {
                unlock();
            }
        } finally {
            target.done();
        }
    }

    private MigrateResult migrateLocked(List<MigrationEntry> versioned, List<MigrationEntry> repeatable)
            throws IOException {
        List<String> warnings = new ArrayList<String>();
        List<String> ran = new ArrayList<String>();
        ensureHistory();
        List<HistoryTable.Row> rows = history.read();
        refuseFailed(rows);
        check(rows, versioned, null, true, options.validateOnMigrate, warnings);
        String baseline = baselineOf(rows);
        String initial = current(rows);
        String user = options.installedBy != null ? options.installedBy : target.currentUser();
        for (MigrationEntry entry : versioned) {
            if (applied(rows, entry.version())
                    || baseline != null && MigrationVersion.compare(entry.version(), baseline) <= 0
                    || aboveTarget(entry)) {
                continue;
            }
            if (initial != null && MigrationVersion.compare(entry.version(), initial) < 0 && !options.outOfOrder) {
                warnings.add("Skipped " + entry.scriptName() + ": older than the applied version " + initial);
                continue;
            }
            if (apply(entry, user)) {
                ran.add(entry.scriptName());
            }
        }
        for (MigrationEntry entry : repeatable) {
            if (upToDate(rows, entry)) {
                continue;
            }
            if (apply(entry, user)) {
                ran.add(entry.scriptName());
            }
        }
        return new MigrateResult(initial, ran.isEmpty() ? initial : current(history.read()), ran, warnings);
    }

    /// Creates the history table when it is missing, adopting an existing schema if asked to.
    private void ensureHistory() throws IOException {
        if (history.exists()) {
            return;
        }
        boolean baseline = false;
        if (set.isDefault() && target.hasUserObjects()) {
            // Looked for again before the tables are called somebody else's schema. Two
            // processes starting together both find no history; one creates it and
            // applies its first script, and the other, arriving here a moment later,
            // finds that script's table and no memory of the history it did not see.
            // It refused to start over tables its own twin had just made.
            if (history.exists()) {
                return;
            }
            if (!options.baselineOnMigrate) {
                throw new MigrationException(MigrationException.NON_EMPTY_SCHEMA, "The database already has "
                        + "tables and no " + history.name() + " table. Call baseline() or enable "
                        + "baselineOnMigrate to adopt it at version " + options.baselineVersion + ".");
            }
            baseline = true;
        }
        createHistory(baseline);
    }

    private void createHistory(boolean baseline) throws IOException {
        boolean transaction = target.supportsDdlTransactions();
        try {
            if (transaction) {
                target.begin();
            }
            // Asked again under the transaction: on an engine whose write transaction is the
            // only lock, another process may have created the table while this one waited.
            if (!history.exists()) {
                history.create();
                if (baseline) {
                    insertBaseline();
                }
            }
            if (transaction) {
                target.commit();
            }
        } catch (IOException failure) {
            rollbackQuietly();
            throw failure;
        } catch (RuntimeException failure) {
            rollbackQuietly();
            throw failure;
        }
    }

    private void insertBaseline() throws IOException {
        if (!MigrationVersion.isValid(options.baselineVersion)) {
            throw new MigrationException(MigrationException.INVALID_SET, "Not a version: "
                    + options.baselineVersion);
        }
        String user = options.installedBy != null ? options.installedBy : target.currentUser();
        history.insert(MigrationVersion.normalize(options.baselineVersion), options.baselineDescription,
                MigrationEntry.BASELINE, options.baselineDescription, null, user, 0, true);
    }

    private void rollbackQuietly() {
        if (target.isTransactionActive()) {
            try {
                target.rollback();
            } catch (IOException ignored) {
                // The failure being reported is the one that matters.
            }
        }
    }

    /// Runs one migration and records it. Answers false when another process recorded it first.
    private boolean apply(MigrationEntry entry, String user) throws IOException {
        boolean transaction = entry.transactional();
        String[] statements = null;
        if (entry.java() == null) {
            String script = entry.script();
            if (script == null) {
                throw new MigrationException(MigrationException.INVALID_SET, entry.scriptName() + " has no text");
            }
            statements = target.split(script);
            if (transaction) {
                for (String statement : statements) {
                    if (controlsTransaction(statement)) {
                        throw new MigrationException(MigrationException.SCRIPT_FAILED, entry.scriptName()
                                + " opens or ends a transaction itself. The engine already runs it in one; "
                                + "remove the statement, or mark the migration as running outside a transaction.");
                    }
                }
            }
        }
        long start = System.currentTimeMillis();
        try {
            if (transaction) {
                target.begin();
            }
            // Asked again under the transaction, for both kinds. The history this run
            // decided from was read before any lock an engine such as SQLite has -- its
            // write transaction -- so a second process starting beside this one read the
            // same history and decided the same. A version is found by its number; a
            // repeatable migration has none, and is found by the checksum of its newest
            // run. Without that half a repeatable ran once per process: a duplicate row,
            // and its inserts twice.
            //
            // A migration marked as running outside a transaction gets this look and no
            // more, and on SQLite that is not serialization: the script opens its own
            // transactions, so the engine holds no write lock across it, and SQLite has
            // no lock that outlasts a transaction to take in its place. Two processes
            // starting together on one SQLite file can both run such a script. That is
            // left as it is, deliberately: holding other processes off would take a lock
            // file beside the database or a lock row that a crashed process leaves
            // behind, for a combination -- several processes, one SQLite file, a script
            // that manages its own transaction -- that Flyway, whose behaviour this
            // follows, does not serialize for any SQLite migration at all. PostgreSQL
            // and MySQL hold the engine's session lock around the whole run, so there
            // the mark changes nothing. The guide says to apply such a script from one
            // process.
            if (entry.version() != null ? history.applied(entry.version())
                    : upToDate(history.read(), entry)) {
                rollbackQuietly();
                return false;
            }
            if (statements == null) {
                entry.java().migrate(new Context(target));
            } else {
                for (String statement : statements) {
                    target.execute(statement, null);
                }
            }
            if (!transaction && target.isTransactionActive()) {
                throw new IOException("the script left a transaction open");
            }
            history.insert(entry.version(), entry.description(), entry.type(), entry.scriptName(), entry.checksum(),
                    user, (int) (System.currentTimeMillis() - start), true);
            if (transaction) {
                target.commit();
            }
            return true;
        } catch (IOException failure) {
            throw failed(entry, user, start, failure);
        } catch (RuntimeException failure) {
            throw failed(entry, user, start, failure);
        }
    }

    private MigrationException failed(MigrationEntry entry, String user, long start, Exception failure) {
        rollbackQuietly();
        boolean recorded = false;
        if (!entry.transactional() || !target.supportsDdlTransactions()) {
            // Nothing guarantees the statements that did run were undone, so the history has to
            // say so: every later run refuses to continue until someone has looked.
            try {
                history.insert(entry.version(), entry.description(), entry.type(), entry.scriptName(),
                        entry.checksum(), user, (int) (System.currentTimeMillis() - start), false);
                recorded = true;
            } catch (IOException ignored) {
                // Reported below as not recorded.
            } catch (RuntimeException ignored) {
                // Reported below as not recorded.
            }
        }
        return new MigrationException(MigrationException.SCRIPT_FAILED, "Migration " + entry.scriptName()
                + " failed: " + failure.getMessage() + (recorded ? " (recorded as failed; the database may hold "
                + "part of it)" : " (rolled back)"), failure);
    }

    /// Whether a statement begins with a word that opens or ends a transaction.
    static boolean controlsTransaction(String statement) {
        return transactionControl(statement) != 0;
    }

    /// What a statement does to the transaction state, judged by its first word: 1 when it
    /// opens one, -1 when it ends one, 0 otherwise. A target whose connection does not track
    /// transactions a script opens for itself uses this to track them.
    public static int transactionControl(String statement) {
        int i = 0;
        int length = statement.length();
        while (i < length) {
            char c = statement.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                i++;
            } else if (c == '-' && i + 1 < length && statement.charAt(i + 1) == '-') {
                while (i < length && statement.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '/' && i + 1 < length && statement.charAt(i + 1) == '*') {
                int end = statement.indexOf("*/", i + 2);
                if (end < 0) {
                    return 0;
                }
                i = end + 2;
            } else {
                break;
            }
        }
        if (word(statement, i, "BEGIN") || word(statement, i, "START")) {
            return 1;
        }
        if (word(statement, i, "COMMIT") || word(statement, i, "ROLLBACK") || word(statement, i, "END")) {
            return -1;
        }
        return 0;
    }

    private static boolean word(String statement, int at, String word) {
        int end = at + word.length();
        if (end > statement.length() || !statement.regionMatches(true, at, word, 0, word.length())) {
            return false;
        }
        if (end == statement.length()) {
            return true;
        }
        char next = statement.charAt(end);
        return !(next == '_' || next >= '0' && next <= '9' || next >= 'a' && next <= 'z'
                || next >= 'A' && next <= 'Z');
    }

    /// Checks the history against this build's migrations and throws on any difference,
    /// pending migrations included.
    public void validate() throws IOException {
        List<MigrationEntry> versioned = resolve(false);
        List<MigrationEntry> repeatable = resolve(true);
        try {
            List<HistoryTable.Row> rows = history.exists() ? history.read() : new ArrayList<HistoryTable.Row>();
            refuseFailed(rows);
            check(rows, versioned, repeatable, false, true, new ArrayList<String>());
        } finally {
            target.done();
        }
    }

    /// Reports every migration, applied or not, without changing anything.
    public MigrationInfo[] info() throws IOException {
        List<MigrationEntry> versioned = resolve(false);
        List<MigrationEntry> repeatable = resolve(true);
        List<HistoryTable.Row> rows;
        try {
            rows = history.exists() ? history.read() : new ArrayList<HistoryTable.Row>();
        } finally {
            target.done();
        }
        String baseline = baselineOf(rows);
        String newest = highest(versioned);
        String current = current(rows);
        List<MigrationInfo> out = new ArrayList<MigrationInfo>();
        for (int i = 0; i < rows.size(); i++) {
            HistoryTable.Row row = rows.get(i);
            MigrationState state;
            if (!row.success) {
                state = MigrationState.FAILED;
            } else if (MigrationEntry.BASELINE.equals(row.type)) {
                state = MigrationState.BASELINE;
            } else if (row.version == null) {
                state = repeatableState(rows, i, repeatable);
            } else if (!MigrationVersion.isValid(row.version)) {
                state = MigrationState.MISSING;
            } else if (find(versioned, row.version) != null) {
                state = MigrationState.SUCCESS;
            } else if (newest == null || MigrationVersion.compare(row.version, newest) > 0) {
                state = MigrationState.FUTURE;
            } else {
                state = MigrationState.MISSING;
            }
            out.add(new MigrationInfo(row.version, row.description, row.type, row.script, row.checksum, state,
                    row.rank, row.installedOn, row.executionTime));
        }
        for (MigrationEntry entry : versioned) {
            if (applied(rows, entry.version())) {
                continue;
            }
            MigrationState state;
            if (baseline != null && MigrationVersion.compare(entry.version(), baseline) <= 0) {
                state = MigrationState.BELOW_BASELINE;
            } else if (aboveTarget(entry)) {
                state = MigrationState.ABOVE_TARGET;
            } else if (current != null && MigrationVersion.compare(entry.version(), current) < 0
                    && !options.outOfOrder) {
                state = MigrationState.IGNORED;
            } else {
                state = MigrationState.PENDING;
            }
            out.add(pending(entry, state));
        }
        for (MigrationEntry entry : repeatable) {
            if (latestRepeatable(rows, entry.description()) == null) {
                out.add(pending(entry, MigrationState.PENDING));
            }
        }
        return out.toArray(new MigrationInfo[out.size()]);
    }

    private static MigrationState repeatableState(List<HistoryTable.Row> rows, int index,
            List<MigrationEntry> repeatable) {
        HistoryTable.Row row = rows.get(index);
        for (int later = index + 1; later < rows.size(); later++) {
            HistoryTable.Row other = rows.get(later);
            if (other.version == null && other.success && row.description.equals(other.description)) {
                return MigrationState.SUPERSEDED;
            }
        }
        for (MigrationEntry entry : repeatable) {
            if (entry.description().equals(row.description)) {
                Integer checksum = entry.checksum();
                boolean same = checksum == null ? row.checksum == null
                        : row.checksum != null && row.checksum.intValue() == checksum.intValue();
                return same ? MigrationState.SUCCESS : MigrationState.OUTDATED;
            }
        }
        return MigrationState.MISSING;
    }

    private static MigrationInfo pending(MigrationEntry entry, MigrationState state) {
        return new MigrationInfo(entry.version(), entry.description(), entry.type(), entry.scriptName(),
                entry.checksum(), state, -1, null, -1);
    }

    /// Marks an existing database as being at the baseline version.
    public void baseline() throws IOException {
        try {
            lock();
        } catch (IOException failure) {
            target.done();
            throw failure;
        }
        try {
            if (history.exists()) {
                List<HistoryTable.Row> rows = history.read();
                if (rows.isEmpty()) {
                    insertBaseline();
                    return;
                }
                String existing = baselineOf(rows);
                if (existing != null && MigrationVersion.isValid(options.baselineVersion)
                        && MigrationVersion.compare(existing, options.baselineVersion) == 0) {
                    return;
                }
                throw new MigrationException(MigrationException.VALIDATE_FAILED, history.name()
                        + " already records migrations; a baseline can only be set on an empty history");
            }
            createHistory(true);
        } finally {
            unlock();
            target.done();
        }
    }

    /// Removes failed rows and brings recorded checksums and descriptions in line with the
    /// scripts this build carries.
    public void repair() throws IOException {
        List<MigrationEntry> versioned = resolve(false);
        try {
            lock();
        } catch (IOException failure) {
            target.done();
            throw failure;
        }
        try {
            if (!history.exists()) {
                return;
            }
            history.deleteFailed();
            for (HistoryTable.Row row : history.read()) {
                if (row.version == null || MigrationEntry.BASELINE.equals(row.type)
                        || !MigrationVersion.isValid(row.version)) {
                    continue;
                }
                MigrationEntry entry = find(versioned, row.version);
                if (entry == null) {
                    continue;
                }
                Integer checksum = entry.checksum();
                boolean sameChecksum = checksum == null ? row.checksum == null
                        : row.checksum != null && row.checksum.intValue() == checksum.intValue();
                if (!sameChecksum || !entry.description().equals(row.description)) {
                    history.realign(row.rank, checksum, entry.description());
                }
            }
        } finally {
            unlock();
            target.done();
        }
    }

    /// Drops everything in the database, the history included.
    public void clean() throws IOException {
        if (options.cleanDisabled) {
            throw new MigrationException(MigrationException.CLEAN_DISABLED, "clean() drops every table in the "
                    + "database and is disabled; enable it explicitly to use it");
        }
        try {
            lock();
        } catch (IOException failure) {
            target.done();
            throw failure;
        }
        try {
            target.dropAllObjects();
        } finally {
            unlock();
            target.done();
        }
    }

    /// The view of the target a Java migration gets.
    private static final class Context implements MigrationContext {
        private final MigrationTarget target;

        Context(MigrationTarget target) {
            this.target = target;
        }

        @Override
        public String dialect() {
            return target.dialect();
        }

        @Override
        public void execute(String sql, Object[] params) throws IOException {
            target.execute(sql, params);
        }

        @Override
        public List<String[]> query(String sql, Object[] params) throws IOException {
            return target.query(sql, params);
        }

        @Override
        public Object connection() {
            return target.connection();
        }
    }
}
