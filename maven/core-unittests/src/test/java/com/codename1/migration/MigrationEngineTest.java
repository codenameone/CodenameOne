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

import com.codename1.impl.migration.MigrationChecksum;
import com.codename1.impl.migration.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The migration algorithm against a database that models only what the algorithm depends on.
 * Real engines are exercised in the backend module and on devices; this pins the decisions:
 * what runs, in which order, what is refused and with which code.
 */
class MigrationEngineTest {

    private static MigrationSet.Builder two() {
        return MigrationSet.builder("default")
                .sql("1", "create notes", "CREATE TABLE notes (id INTEGER); CREATE INDEX n ON notes (id)")
                .sql("2", "add body", "ALTER TABLE notes ADD COLUMN body TEXT");
    }

    private static Migrator migrator(FakeMigrationTarget target, MigrationSet set) {
        return new Migrator(target, set);
    }

    private static int code(Migrator migrator) {
        MigrationException failure = assertThrows(MigrationException.class, migrator::migrate);
        return failure.getCode();
    }

    @Test
    void appliesEveryPendingMigrationInVersionOrderAndRecordsIt() throws IOException {
        FakeMigrationTarget target = new FakeMigrationTarget();
        // Registered out of order on purpose: order comes from the version, not the registration.
        MigrationSet set = MigrationSet.builder("default")
                .sql("10", "third", "S10")
                .sql("2", "second", "S2")
                .sql("1.5", "first", "S1")
                .build();
        MigrateResult result = migrator(target, set).migrate();
        assertEquals(3, result.getMigrationsExecuted());
        assertNull(result.getInitialVersion());
        assertEquals("10", result.getTargetVersion());
        assertEquals("[S1, S2, S10]", target.statements.toString());
        assertEquals(3, target.history.size());
        assertEquals("1.5", target.history.get(0).version);
        assertEquals("SQL", target.history.get(0).type);
        assertEquals("V1.5__first.sql", target.history.get(0).script);
        assertTrue(target.history.get(2).success);
        assertEquals(3, target.history.get(2).rank);
        assertEquals("[lock cn1_flyway_flyway_schema_history, unlock cn1_flyway_flyway_schema_history]",
                target.locks.toString());
    }

    @Test
    void aSecondRunDoesNothing() throws IOException {
        FakeMigrationTarget target = new FakeMigrationTarget();
        migrator(target, two().build()).migrate();
        int statements = target.statements.size();
        MigrateResult again = migrator(target, two().build()).migrate();
        assertEquals(0, again.getMigrationsExecuted());
        assertEquals("2", again.getInitialVersion());
        assertEquals("2", again.getTargetVersion());
        assertEquals(statements, target.statements.size());
        assertEquals(2, target.history.size());
    }

    @Test
    void onlyTheNewVersionRunsAfterAnUpgrade() throws IOException {
        FakeMigrationTarget target = new FakeMigrationTarget();
        migrator(target, two().build()).migrate();
        target.statements.clear();
        MigrateResult result = migrator(target, two().sql("3", "index", "S3").build()).migrate();
        assertEquals("[V3__index.sql]", result.getApplied().toString());
        assertEquals("[S3]", target.statements.toString());
        assertEquals("2", result.getInitialVersion());
        assertEquals("3", result.getTargetVersion());
    }

    @Test
    void aFailingScriptIsRolledBackAndLeavesNoRowWhereDdlIsTransactional() {
        FakeMigrationTarget target = new FakeMigrationTarget();
        MigrationSet set = two().sql("3", "broken", "S3a; FAIL; S3c").build();
        MigrationException failure = assertThrows(MigrationException.class, migrator(target, set)::migrate);
        assertEquals(MigrationException.SCRIPT_FAILED, failure.getCode());
        assertTrue(failure.getMessage().contains("V3__broken.sql"), failure.getMessage());
        assertTrue(failure.getMessage().contains("rolled back"), failure.getMessage());
        assertEquals(2, target.history.size());
        assertFalse(target.statements.contains("S3a"));
        assertFalse(target.isTransactionActive());
        assertEquals("unlock cn1_flyway_flyway_schema_history", target.locks.get(target.locks.size() - 1));
    }

    @Test
    void aFailingScriptIsRecordedAsFailedWhereDdlCommits() throws IOException {
        FakeMigrationTarget target = new FakeMigrationTarget();
        target.dialect = "mysql";
        target.ddlTransactions = false;
        MigrationSet set = two().sql("3", "broken", "S3a; FAIL; S3c").build();
        MigrationException failure = assertThrows(MigrationException.class, migrator(target, set)::migrate);
        assertEquals(MigrationException.SCRIPT_FAILED, failure.getCode());
        assertTrue(failure.getMessage().contains("recorded as failed"), failure.getMessage());
        assertEquals(3, target.history.size());
        assertFalse(target.history.get(2).success);
        assertTrue(target.statements.contains("S3a"));

        // Every later run refuses until someone has looked.
        assertEquals(MigrationException.FAILED_MIGRATION_PRESENT, code(migrator(target, set)));
        MigrationInfo[] info = migrator(target, set).info();
        assertEquals(MigrationState.FAILED, info[2].getState());

        // repair() removes the marker; the corrected script then runs.
        migrator(target, set).repair();
        assertEquals(2, target.history.size());
        MigrationSet fixed = two().sql("3", "broken", "S3a; S3b").build();
        assertEquals(1, migrator(target, fixed).migrate().getMigrationsExecuted());
        assertTrue(target.history.get(2).success);
    }

    @Test
    void anEditedAppliedScriptFailsValidationAndRepairRealignsIt() throws IOException {
        FakeMigrationTarget target = new FakeMigrationTarget();
        migrator(target, two().build()).migrate();
        MigrationSet edited = MigrationSet.builder("default")
                .sql("1", "create notes", "CREATE TABLE notes (id INTEGER, extra TEXT)")
                .sql("2", "add body", "ALTER TABLE notes ADD COLUMN body TEXT")
                .build();
        MigrationException failure = assertThrows(MigrationException.class, migrator(target, edited)::migrate);
        assertEquals(MigrationException.VALIDATE_FAILED, failure.getCode());
        assertTrue(failure.getMessage().contains("checksum of version 1"), failure.getMessage());
        assertTrue(failure.getMessage().contains("was edited"), failure.getMessage());

        // With validation off the run goes ahead and applies nothing.
        assertEquals(0, migrator(target, edited).validateOnMigrate(false).migrate().getMigrationsExecuted());

        migrator(target, edited).repair();
        assertEquals(0, migrator(target, edited).migrate().getMigrationsExecuted());
        migrator(target, edited).validate();
    }

    @Test
    void aRemovedAppliedScriptFailsValidation() throws IOException {
        FakeMigrationTarget target = new FakeMigrationTarget();
        migrator(target, two().sql("3", "x", "S3").build()).migrate();
        MigrationSet without2 = MigrationSet.builder("default").sql("1", "create notes", "A").sql("3", "x", "S3")
                .build();
        MigrationException failure = assertThrows(MigrationException.class,
                migrator(target, without2).validateOnMigrate(true)::migrate);
        assertEquals(MigrationException.VALIDATE_FAILED, failure.getCode());
        assertTrue(failure.getMessage().contains("applied version 2"), failure.getMessage());
    }

    @Test
    void aDatabaseNewerThanTheBuildIsRefusedOrToleratedByChoice() throws IOException {
        FakeMigrationTarget target = new FakeMigrationTarget();
        migrator(target, two().sql("3", "new", "S3").build()).migrate();
        int statements = target.statements.size();

        // The older build: an application refuses before touching anything.
        MigrationException failure = assertThrows(MigrationException.class,
                migrator(target, two().build()).ignoreFutureMigrations(false)::migrate);
        assertEquals(MigrationException.FUTURE_SCHEMA, failure.getCode());
        assertTrue(failure.getMessage().contains("version 3"), failure.getMessage());
        assertEquals(statements, target.statements.size());
        assertEquals(3, target.history.size());

        // A server tolerates it and says so.
        MigrateResult result = migrator(target, two().build()).migrate();
        assertEquals(0, result.getMigrationsExecuted());
        assertEquals("[Applied version 3 is newer than this build]", result.getWarnings().toString());
        assertEquals(MigrationState.FUTURE, migrator(target, two().build()).info()[2].getState());
    }

    @Test
    void anOlderUnappliedVersionIsRefusedUnlessOutOfOrderIsAllowed() throws IOException {
        FakeMigrationTarget target = new FakeMigrationTarget();
        MigrationSet first = MigrationSet.builder("default").sql("1", "a", "S1").sql("3", "c", "S3").build();
        migrator(target, first).migrate();
        MigrationSet late = MigrationSet.builder("default").sql("1", "a", "S1").sql("2", "b", "S2")
                .sql("3", "c", "S3").build();
        MigrationException failure = assertThrows(MigrationException.class, migrator(target, late)::migrate);
        assertEquals(MigrationException.VALIDATE_FAILED, failure.getCode());
        assertTrue(failure.getMessage().contains("older than the applied version 3"), failure.getMessage());
        assertEquals(MigrationState.IGNORED, migrator(target, late).info()[2].getState());

        MigrateResult result = migrator(target, late).outOfOrder(true).migrate();
        assertEquals("[V2__b.sql]", result.getApplied().toString());
        assertEquals("3", result.getTargetVersion());
    }

    @Test
    void aSchemaWithTablesAndNoHistoryIsRefusedUnlessBaselined() throws IOException {
        FakeMigrationTarget target = new FakeMigrationTarget();
        target.userObjects = true;
        MigrationSet set = two().sql("3", "new", "S3").build();
        assertEquals(MigrationException.NON_EMPTY_SCHEMA, code(migrator(target, set)));
        assertFalse(target.historyExists);

        MigrateResult result = migrator(target, set).baselineOnMigrate(true).baselineVersion("2").migrate();
        assertEquals("[V3__new.sql]", result.getApplied().toString());
        assertEquals("2", result.getInitialVersion());
        assertEquals("BASELINE", target.history.get(0).type);
        assertEquals("2", target.history.get(0).version);
        assertEquals("[S3]", target.statements.toString());
        MigrationInfo[] info = migrator(target, set).info();
        assertEquals(MigrationState.BASELINE, info[0].getState());
        assertEquals(MigrationState.SUCCESS, info[1].getState());
        assertEquals(MigrationState.BELOW_BASELINE, info[2].getState());
        assertEquals(MigrationState.BELOW_BASELINE, info[3].getState());
    }

    @Test
    void aLibrarySetIgnoresApplicationTablesAndKeepsItsOwnHistory() throws IOException {
        FakeMigrationTarget target = new FakeMigrationTarget();
        target.userObjects = true;
        MigrationSet library = MigrationSet.builder("security").sql("1", "users", "L1").build();
        assertEquals("cn1_security_schema_history", library.getTable());
        assertEquals(1, migrator(target, library).migrate().getMigrationsExecuted());
        assertEquals("lock cn1_flyway_cn1_security_schema_history", target.locks.get(0));
    }

    @Test
    void explicitBaselineMarksAnEmptyHistoryOnce() throws IOException {
        FakeMigrationTarget target = new FakeMigrationTarget();
        target.userObjects = true;
        migrator(target, two().build()).baselineVersion("1").baseline();
        assertEquals(1, target.history.size());
        // The same baseline again is a no-op; a different one is refused.
        migrator(target, two().build()).baselineVersion("1").baseline();
        assertEquals(1, target.history.size());
        assertThrows(MigrationException.class, migrator(target, two().build()).baselineVersion("5")::baseline);
        assertEquals("[V2__add_body.sql]", migrator(target, two().build()).migrate().getApplied().toString());
    }

    @Test
    void targetStopsAtAVersion() throws IOException {
        FakeMigrationTarget target = new FakeMigrationTarget();
        MigrationSet set = two().sql("3", "new", "S3").build();
        MigrateResult result = migrator(target, set).target("2").migrate();
        assertEquals(2, result.getMigrationsExecuted());
        assertEquals(MigrationState.ABOVE_TARGET, migrator(target, set).target("2").info()[2].getState());
        assertEquals(MigrationState.PENDING, migrator(target, set).info()[2].getState());
        assertThrows(MigrationException.class, migrator(target, set)::validate);
        migrator(target, set).target("2").validate();
    }

    @Test
    void theEngineSpecificScriptWinsAndAMissingOneIsAnError() throws IOException {
        MigrationSet set = MigrationSet.builder("default")
                .sql("1", "t", "sqlite", "LITE")
                .sql("1", "t", "postgresql", "PG")
                .sql("2", "common", "BOTH")
                .build();
        FakeMigrationTarget lite = new FakeMigrationTarget();
        migrator(lite, set).migrate();
        assertEquals("[LITE, BOTH]", lite.statements.toString());
        FakeMigrationTarget pg = new FakeMigrationTarget();
        pg.dialect = "postgresql";
        migrator(pg, set).migrate();
        assertEquals("[PG, BOTH]", pg.statements.toString());
        assertTrue(pg.history.isEmpty() || pg.history.get(0).success);

        FakeMigrationTarget my = new FakeMigrationTarget();
        my.dialect = "mysql";
        MigrationException failure = assertThrows(MigrationException.class, migrator(my, set)::migrate);
        assertEquals(MigrationException.INVALID_SET, failure.getCode());
        assertTrue(failure.getMessage().contains("version 1"), failure.getMessage());
        assertTrue(failure.getMessage().contains("no script for mysql"), failure.getMessage());
        assertFalse(my.historyExists);
    }

    @Test
    void duplicateVersionsAreRefusedBeforeAnythingRuns() {
        MigrationSet set = MigrationSet.builder("default").sql("1", "a", "A").sql("1.0", "b", "B").build();
        FakeMigrationTarget target = new FakeMigrationTarget();
        MigrationException failure = assertThrows(MigrationException.class, migrator(target, set)::migrate);
        assertEquals(MigrationException.INVALID_SET, failure.getCode());
        assertTrue(failure.getMessage().contains("share version"), failure.getMessage());
        assertTrue(target.statements.isEmpty());
    }

    @Test
    void repeatableMigrationsRunLastAndAgainOnlyWhenTheyChange() throws IOException {
        FakeMigrationTarget target = new FakeMigrationTarget();
        MigrationSet set = MigrationSet.builder("default")
                .repeatable("views", "CREATE VIEW v1")
                .sql("1", "t", "S1")
                .build();
        assertEquals("[V1__t.sql, R__views.sql]", migrator(target, set).migrate().getApplied().toString());
        assertNull(target.history.get(1).version);
        assertEquals(0, migrator(target, set).migrate().getMigrationsExecuted());

        MigrationSet changed = MigrationSet.builder("default")
                .repeatable("views", "CREATE VIEW v2")
                .sql("1", "t", "S1")
                .build();
        MigrationInfo[] before = migrator(target, changed).info();
        assertEquals(MigrationState.OUTDATED, before[1].getState());
        assertEquals("[R__views.sql]", migrator(target, changed).migrate().getApplied().toString());
        MigrationInfo[] after = migrator(target, changed).info();
        assertEquals(MigrationState.SUPERSEDED, after[1].getState());
        assertEquals(MigrationState.SUCCESS, after[2].getState());
    }

    /** One class for every version of a set, as a library's set is written. */
    private static final class Step implements JavaMigration {
        private final String statement;

        Step(String statement) {
            this.statement = statement;
        }

        @Override
        public void migrate(MigrationContext context) throws IOException {
            context.execute(statement, null);
        }
    }

    @Test
    void aNamedJavaMigrationIsRecordedUnderItsNameAndAnOlderHistoryStillValidates() throws IOException {
        FakeMigrationTarget target = new FakeMigrationTarget();
        // As it was written before the versions had names: the class name, three times.
        MigrationSet unnamed = MigrationSet.builder("default")
                .java("1", "a", new Step("J1"))
                .java("2", "b", new Step("J2"))
                .build();
        assertEquals("[" + Step.class.getName().replace('$', '.') + ", "
                + Step.class.getName().replace('$', '.') + "]",
                migrator(target, unnamed).migrate().getApplied().toString());

        // The same versions, named now, and one more.
        MigrationSet named = MigrationSet.builder("default")
                .java("1", "a", "lib.V1__a", new Step("J1"))
                .java("2", "b", "lib.V2__b", new Step("J2"))
                .java("3", "c", "lib.V3__c", new Step("J3"))
                .build();
        // Pending version 3 is all validate may object to; the renamed rows are not drift.
        MigrationException pending = assertThrows(MigrationException.class,
                () -> migrator(target, named).validate());
        assertEquals(MigrationException.VALIDATE_FAILED, pending.getCode());
        assertTrue(pending.getMessage().contains("version 3 (lib.V3__c) has not been applied"),
                pending.getMessage());
        assertFalse(pending.getMessage().contains("version 1"), pending.getMessage());
        assertFalse(pending.getMessage().contains("version 2"), pending.getMessage());

        target.statements.clear();
        MigrateResult result = migrator(target, named).migrate();
        assertEquals("[lib.V3__c]", result.getApplied().toString());
        assertEquals("[J3]", target.statements.toString());
        migrator(target, named).validate();
        // What was written stays as it was written; only the new row has the new name.
        assertEquals(Step.class.getName().replace('$', '.'), target.history.get(0).script);
        assertEquals("lib.V3__c", target.history.get(2).script);

        assertThrows(IllegalArgumentException.class,
                () -> MigrationSet.builder("default").java("1", "a", "", new Step("J1")));
    }

    @Test
    void aJavaMigrationRunsInOrderAndIsRecordedWithoutAChecksum() throws IOException {
        FakeMigrationTarget target = new FakeMigrationTarget();
        final String[] seen = new String[1];
        MigrationSet set = MigrationSet.builder("default")
                .sql("1", "t", "S1")
                .java("2", "recompute", new JavaMigration() {
                    @Override
                    public void migrate(MigrationContext context) throws IOException {
                        seen[0] = context.dialect();
                        context.execute("J2", null);
                        assertTrue(context.connection() instanceof FakeMigrationTarget);
                    }
                })
                .sql("3", "u", "S3")
                .build();
        migrator(target, set).migrate();
        assertEquals("sqlite", seen[0]);
        assertEquals("[S1, J2, S3]", target.statements.toString());
        assertEquals("JAVA", target.history.get(1).type);
        assertNull(target.history.get(1).checksum);
    }

    @Test
    void aTransactionalScriptThatControlsItsOwnTransactionIsRefusedUntouched() {
        FakeMigrationTarget target = new FakeMigrationTarget();
        MigrationSet set = MigrationSet.builder("default")
                .sql("1", "rebuild", "S0; -- note\n  begin; S1; COMMIT").build();
        MigrationException failure = assertThrows(MigrationException.class, migrator(target, set)::migrate);
        assertEquals(MigrationException.SCRIPT_FAILED, failure.getCode());
        assertTrue(failure.getMessage().contains("opens or ends a transaction itself"), failure.getMessage());
        assertTrue(target.statements.isEmpty());
        assertTrue(target.history.isEmpty());
    }

    @Test
    void aScriptMarkedOutsideATransactionMayManageItsOwn() throws IOException {
        FakeMigrationTarget target = new FakeMigrationTarget();
        MigrationSet set = MigrationSet.builder("default")
                .sql("1", "rebuild", "BEGIN; S1; COMMIT").outsideTransaction().build();
        assertEquals(1, migrator(target, set).migrate().getMigrationsExecuted());
        assertEquals("[S1]", target.statements.toString());

        // One that leaves its transaction open is failed and recorded, on every engine.
        FakeMigrationTarget open = new FakeMigrationTarget();
        MigrationSet leaky = MigrationSet.builder("default")
                .sql("1", "leak", "BEGIN; S1").outsideTransaction().build();
        MigrationException failure = assertThrows(MigrationException.class, migrator(open, leaky)::migrate);
        assertTrue(failure.getMessage().contains("left a transaction open"), failure.getMessage());
        assertFalse(open.isTransactionActive());
        assertEquals(1, open.history.size());
        assertFalse(open.history.get(0).success);
    }

    @Test
    void aHeldLockTimesOutWithItsOwnCode() {
        FakeMigrationTarget target = new FakeMigrationTarget();
        target.lockAvailable = false;
        assertEquals(MigrationException.LOCK_TIMEOUT, code(migrator(target, two().build())));
        assertTrue(target.statements.isEmpty());
    }

    @Test
    void cleanIsRefusedUnlessEnabled() throws IOException {
        FakeMigrationTarget target = new FakeMigrationTarget();
        migrator(target, two().build()).migrate();
        MigrationException failure = assertThrows(MigrationException.class, migrator(target, two().build())::clean);
        assertEquals(MigrationException.CLEAN_DISABLED, failure.getCode());
        assertFalse(target.dropped);
        migrator(target, two().build()).cleanDisabled(false).clean();
        assertTrue(target.dropped);
        assertEquals(2, migrator(target, two().build()).migrate().getMigrationsExecuted());
    }

    @Test
    void aCustomHistoryTableIsUsedForEveryStatement() throws IOException {
        FakeMigrationTarget target = new FakeMigrationTarget();
        migrator(target, two().build()).table("my_history").migrate();
        assertEquals("lock cn1_flyway_my_history", target.locks.get(0));
        assertThrows(IllegalArgumentException.class, () -> migrator(target, two().build()).table("bad name"));
        assertThrows(IllegalArgumentException.class, () -> migrator(target, two().build()).table("x\"; DROP"));
    }

    @Test
    void versionsCompareNumericallyPartByPart() {
        assertTrue(MigrationVersion.compare("1.10", "1.9") > 0);
        assertEquals(0, MigrationVersion.compare("1.0", "1"));
        assertEquals(0, MigrationVersion.compare("1_2", "1.2"));
        assertTrue(MigrationVersion.compare("2026.05.21.1", "2026.05.21") > 0);
        assertTrue(MigrationVersion.compare("2", "10") < 0);
        assertEquals("1.2", MigrationVersion.normalize("1_2"));
        assertTrue(MigrationVersion.isValid("20260521123456"));
        assertFalse(MigrationVersion.isValid(""));
        assertFalse(MigrationVersion.isValid("1..2"));
        assertFalse(MigrationVersion.isValid("1."));
        assertFalse(MigrationVersion.isValid(".1"));
        assertFalse(MigrationVersion.isValid("1a"));
        assertFalse(MigrationVersion.isValid("1234567890123456789"));
        assertThrows(IllegalArgumentException.class, () -> MigrationSet.builder("default").sql("x", "d", "S"));
    }

    /** Flyway's algorithm, written against the JDK: the build computes its literals this way. */
    private static int reference(String script) {
        CRC32 crc = new CRC32();
        String text = script.startsWith("\ufeff") ? script.substring(1) : script;
        for (String line : text.split("\r\n|\r|\n", -1)) {
            crc.update(line.getBytes(StandardCharsets.UTF_8));
        }
        return (int) crc.getValue();
    }

    @Test
    void theChecksumMatchesTheJdkAndIgnoresLineEndings() {
        String unix = "CREATE TABLE t (\n  id INTEGER, -- caf\u00e9 \u4e2d\u6587 \ud83d\ude00\n  n TEXT\n);\n";
        String windows = unix.replace("\n", "\r\n");
        assertEquals(reference(unix), MigrationChecksum.of(unix));
        assertEquals(MigrationChecksum.of(unix), MigrationChecksum.of(windows));
        assertEquals(MigrationChecksum.of(unix), MigrationChecksum.of("\ufeff" + unix));
        assertFalse(MigrationChecksum.of(unix) == MigrationChecksum.of(unix.replace("INTEGER", "TEXT")));
        assertEquals(0, MigrationChecksum.of(""));
        // Longer than the internal buffer, with multi-byte characters straddling its edge.
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 3000; i++) {
            big.append("\u4e2dx\ud83d\ude00\n");
        }
        assertEquals(reference(big.toString()), MigrationChecksum.of(big.toString()));
    }
}
