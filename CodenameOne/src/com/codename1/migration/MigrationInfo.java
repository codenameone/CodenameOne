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

/// One row of [Migrator#info()]: a migration this build carries, one the database recorded, or
/// both.
@com.codename1.impl.SharedWithBackend
public final class MigrationInfo {
    private final String version;
    private final String description;
    private final String type;
    private final String script;
    private final Integer checksum;
    private final MigrationState state;
    private final int installedRank;
    private final String installedOn;
    private final int executionTime;

    /// Creates a row. Built by the engine; an application reads these.
    /// @param version the version, or null for a repeatable migration
    /// @param description the description
    /// @param type `SQL`, `JAVA` or `BASELINE`
    /// @param script the script or class name
    /// @param checksum the checksum, or null where there is none
    /// @param state where the migration stands
    /// @param installedRank the order it was applied in, or -1 if it was not
    /// @param installedOn when it was applied, as the database prints it, or null
    /// @param executionTime how long it took in milliseconds, or -1
    public MigrationInfo(String version, String description, String type, String script, Integer checksum,
            MigrationState state, int installedRank, String installedOn, int executionTime) {
        this.version = version;
        this.description = description;
        this.type = type;
        this.script = script;
        this.checksum = checksum;
        this.state = state;
        this.installedRank = installedRank;
        this.installedOn = installedOn;
        this.executionTime = executionTime;
    }

    /// The version.
    /// @return the version, or null for a repeatable migration
    public String getVersion() {
        return version;
    }

    /// The description.
    /// @return the description from the file name or the registration
    public String getDescription() {
        return description;
    }

    /// The kind of migration.
    /// @return `SQL`, `JAVA` or `BASELINE`
    public String getType() {
        return type;
    }

    /// The script file or migration class.
    /// @return the name recorded in the history
    public String getScript() {
        return script;
    }

    /// The checksum of the script.
    /// @return the checksum, or null for a Java migration and a baseline
    public Integer getChecksum() {
        return checksum;
    }

    /// Where the migration stands.
    /// @return the state
    public MigrationState getState() {
        return state;
    }

    /// The order the migration was applied in.
    /// @return the rank, or -1 if it has not been applied
    public int getInstalledRank() {
        return installedRank;
    }

    /// When the migration was applied.
    /// @return the timestamp as the database prints it, or null if it has not been applied
    public String getInstalledOn() {
        return installedOn;
    }

    /// How long the migration took.
    /// @return milliseconds, or -1 if it has not been applied
    public int getExecutionTime() {
        return executionTime;
    }

    /// One line per migration, for a log.
    /// @return version, description, type and state
    @Override
    public String toString() {
        return (version == null ? "R" : version) + " " + description + " [" + type + "] " + state;
    }
}
