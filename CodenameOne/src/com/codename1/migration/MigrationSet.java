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

import com.codename1.impl.migration.HistoryTable;
import com.codename1.impl.migration.MigrationEntry;
import com.codename1.impl.migration.MigrationSource;
import com.codename1.impl.migration.MigrationVersion;
import java.util.ArrayList;
import java.util.List;

/// A named group of migrations with its own history table.
///
/// An application has one, called `default`, which the build fills from the migration files in
/// the project. A library that needs tables of its own registers a second set under its own name
/// and history table, so its versions never collide with the application's.
///
/// ```java
/// MigrationSet set = MigrationSet.builder("default")
///         .sql("1", "create notes", "CREATE TABLE notes (id INTEGER PRIMARY KEY, body TEXT)")
///         .sql("2", "add created", "ALTER TABLE notes ADD COLUMN created INTEGER")
///         .build();
/// ```
@com.codename1.impl.SharedWithBackend
public final class MigrationSet {
    /// The name of the application's own set.
    public static final String DEFAULT_NAME = "default";
    /// The history table of the application's own set.
    public static final String DEFAULT_TABLE = "flyway_schema_history";

    private final String name;
    private final String table;
    private final MigrationEntry[] entries;

    private MigrationSet(String name, String table, MigrationEntry[] entries) {
        this.name = name;
        this.table = table;
        this.entries = entries;
    }

    /// Starts a set.
    /// @param name `default` for the application's own migrations, otherwise a short lower-case
    ///     identifier naming the library
    /// @return a builder
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /// The name of the set.
    /// @return the name it was built with
    public String getName() {
        return name;
    }

    /// The history table of the set.
    /// @return `flyway_schema_history` for the default set and `cn1_<name>_schema_history` for
    ///     any other, unless the builder named one
    public String getTable() {
        return table;
    }

    /// Whether this is the application's own set.
    /// @return true for the set called `default`
    public boolean isDefault() {
        return DEFAULT_NAME.equals(name);
    }

    /// The registered migrations, for the engine.
    /// @return the entries in registration order
    /// @hidden
    public MigrationEntry[] entries() {
        return entries.clone();
    }

    /// Collects the migrations of a set.
    public static final class Builder {
        private final String name;
        private String table;
        private final List<MigrationEntry> entries = new ArrayList<MigrationEntry>();
        private MigrationEntry last;

        Builder(String name) {
            if (name == null || name.length() == 0) {
                throw new IllegalArgumentException("A migration set needs a name");
            }
            this.name = name;
        }

        /// Names the history table.
        /// @param table a plain identifier
        /// @return this builder
        public Builder table(String table) {
            HistoryTable.checkName(table);
            this.table = table;
            return this;
        }

        /// Adds a versioned script that runs on every engine.
        /// @param version the version, such as `3` or `2026.05.21.1`
        /// @param description what it does
        /// @param script one or more SQL statements
        /// @return this builder
        public Builder sql(String version, String description, String script) {
            return sql(version, description, null, script);
        }

        /// Adds a versioned script for one engine. A version needs either one script for every
        /// engine or one per engine the application runs on.
        /// @param version the version
        /// @param description what it does
        /// @param dialect `sqlite`, `postgresql` or `mysql`, or null for every engine
        /// @param script one or more SQL statements
        /// @return this builder
        public Builder sql(String version, String description, String dialect, String script) {
            requireVersion(version);
            return add(new MigrationEntry(version, description, "V" + version + "__" + file(description) + ".sql",
                    dialect, script, null, 0, null, null));
        }

        /// Adds a repeatable script: it runs after the versioned ones, and again whenever its
        /// text changes. Views and seed data are the usual use.
        /// @param description what it does; repeatable scripts run in description order
        /// @param script one or more SQL statements
        /// @return this builder
        public Builder repeatable(String description, String script) {
            return repeatable(description, null, script);
        }

        /// Adds a repeatable script for one engine.
        /// @param description what it does
        /// @param dialect `sqlite`, `postgresql` or `mysql`, or null for every engine
        /// @param script one or more SQL statements
        /// @return this builder
        public Builder repeatable(String description, String dialect, String script) {
            return add(new MigrationEntry(null, description, "R__" + file(description) + ".sql", dialect, script,
                    null, 0, null, null));
        }

        /// Adds a migration written in Java.
        /// @param version the version
        /// @param description what it does
        /// @param migration the code to run
        /// @return this builder
        public Builder java(String version, String description, JavaMigration migration) {
            requireVersion(version);
            if (migration == null) {
                throw new IllegalArgumentException("A Java migration is required");
            }
            // A nested class is spelled Outer$Inner by one runtime and Outer.Inner by the
            // translated one. The history is compared across them, so it gets one spelling.
            return add(new MigrationEntry(version, description, migration.getClass().getName().replace('$', '.'),
                    null, null, null, 0, null, migration));
        }

        /// Marks the migration added last as running outside a transaction. A script that must
        /// manage its own, such as a SQLite table rebuild that switches foreign keys off, needs
        /// this.
        /// @return this builder
        public Builder outsideTransaction() {
            if (last == null) {
                throw new IllegalStateException("No migration to mark");
            }
            last.transactional(false);
            return this;
        }

        /// Adds a script the build compiled in. Called by generated code.
        /// @param version the version, or null for a repeatable script
        /// @param description the description
        /// @param scriptName the file name
        /// @param dialect the engine, or null for every engine
        /// @param checksum the checksum the build computed
        /// @param transactional false for a script that manages its own transaction
        /// @param source where the text comes from
        /// @param id the number of the script within the source
        /// @return this builder
        /// @hidden
        public Builder compiled(String version, String description, String scriptName, String dialect, int checksum,
                boolean transactional, MigrationSource source, int id) {
            if (version != null) {
                requireVersion(version);
            }
            add(new MigrationEntry(version, description, scriptName, dialect, null, source, id,
                    Integer.valueOf(checksum), null));
            last.transactional(transactional);
            return this;
        }

        /// Builds the set.
        /// @return the set, ready to register or to hand to a migrator
        public MigrationSet build() {
            String history = table;
            if (history == null) {
                history = DEFAULT_NAME.equals(name) ? DEFAULT_TABLE : "cn1_" + name + "_schema_history";
                HistoryTable.checkName(history);
            }
            return new MigrationSet(name, history, entries.toArray(new MigrationEntry[entries.size()]));
        }

        private Builder add(MigrationEntry entry) {
            if (entry.description() == null) {
                throw new IllegalArgumentException("A migration needs a description");
            }
            entries.add(entry);
            last = entry;
            return this;
        }

        private static void requireVersion(String version) {
            if (!MigrationVersion.isValid(version)) {
                throw new IllegalArgumentException("Not a migration version: " + version);
            }
        }

        private static String file(String description) {
            return description == null ? "" : description.replace(' ', '_');
        }
    }
}
