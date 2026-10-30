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
package com.codename1.backend.security.migration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// What an [IdentityMigration] did with every record it was given, and what it
/// could not carry at all.
///
/// Every record lands in exactly one entry: migrated as it was, migrated with an
/// action its owner must take (a password that has to be reset, a second factor
/// to enrol again), or skipped with the reason. Nothing is dropped without an
/// entry, and nothing in an entry is secret -- usernames, providers, credential
/// ids and reasons, never a hash, a key or a TOTP seed.
public final class MigrationReport {
    /// What happened to one record.
    public enum Outcome {
        /// Carried over unchanged; its owner signs in as before.
        MIGRATED,
        /// Carried over, and its owner has to act: see the reason.
        NEEDS_ACTION,
        /// Not carried over: see the reason.
        SKIPPED,
        /// State the migration never carries, and why. Not a record.
        NOT_CARRIED
    }

    /// One line of the report.
    public static final class Entry {
        private final String kind;
        private final String id;
        private final Outcome outcome;
        private final String reason;

        Entry(String kind, String id, Outcome outcome, String reason) {
            this.kind = kind;
            this.id = id;
            this.outcome = outcome;
            this.reason = reason;
        }

        /// `user`, `federated-identity`, `passkey`, `totp`, or for
        /// [Outcome#NOT_CARRIED] the kind of state.
        public String getKind() {
            return kind;
        }

        /// The record: a username, `provider/subject`, a credential id.
        public String getId() {
            return id;
        }

        public Outcome getOutcome() {
            return outcome;
        }

        /// Why, for anything but [Outcome#MIGRATED]; null for that.
        public String getReason() {
            return reason;
        }

        @Override
        public String toString() {
            return outcome + " " + kind + " " + id + (reason == null ? "" : ": " + reason);
        }
    }

    private final List<Entry> entries = new ArrayList<Entry>();
    private final boolean dryRun;

    MigrationReport(boolean dryRun) {
        this.dryRun = dryRun;
    }

    void add(String kind, String id, Outcome outcome, String reason) {
        entries.add(new Entry(kind, id, outcome, reason));
    }

    /// Whether nothing was written: the report says what a real run would do.
    public boolean isDryRun() {
        return dryRun;
    }

    public List<Entry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /// The entries with this outcome.
    public List<Entry> getEntries(Outcome outcome) {
        List<Entry> out = new ArrayList<Entry>();
        for (Entry e : entries) {
            if (e.getOutcome() == outcome) {
                out.add(e);
            }
        }
        return out;
    }

    public int count(Outcome outcome) {
        return getEntries(outcome).size();
    }

    /// True when no record was skipped.
    public boolean isComplete() {
        return count(Outcome.SKIPPED) == 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(dryRun ? "Identity migration (dry run): " : "Identity migration: ")
                .append(count(Outcome.MIGRATED)).append(" migrated, ")
                .append(count(Outcome.NEEDS_ACTION)).append(" need action, ")
                .append(count(Outcome.SKIPPED)).append(" skipped\n");
        for (Entry e : entries) {
            if (e.getOutcome() != Outcome.MIGRATED) {
                sb.append("  ").append(e).append('\n');
            }
        }
        return sb.toString();
    }
}
