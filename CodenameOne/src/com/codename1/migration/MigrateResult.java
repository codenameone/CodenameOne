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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// What one [Migrator#migrate()] call did.
@com.codename1.impl.SharedWithBackend
public final class MigrateResult {
    private final String initialVersion;
    private final String targetVersion;
    private final int migrationsExecuted;
    private final List<String> applied;
    private final List<String> warnings;

    /// Creates a result. Built by the engine; an application reads these.
    /// @param initialVersion the schema version before the run, or null for an empty history
    /// @param targetVersion the schema version after it, or null
    /// @param applied the script name of every migration that ran, in order
    /// @param warnings things the run tolerated and the operator should know
    public MigrateResult(String initialVersion, String targetVersion, List<String> applied, List<String> warnings) {
        this.initialVersion = initialVersion;
        this.targetVersion = targetVersion;
        this.applied = Collections.unmodifiableList(new ArrayList<String>(applied));
        this.warnings = Collections.unmodifiableList(new ArrayList<String>(warnings));
        this.migrationsExecuted = applied.size();
    }

    /// The schema version before the run.
    /// @return the version, or null if nothing had been applied
    public String getInitialVersion() {
        return initialVersion;
    }

    /// The schema version after the run.
    /// @return the version, or null if nothing versioned has been applied
    public String getTargetVersion() {
        return targetVersion;
    }

    /// How many migrations ran.
    /// @return the count; 0 when the database was already current
    public int getMigrationsExecuted() {
        return migrationsExecuted;
    }

    /// The migrations that ran.
    /// @return their script names in the order they ran
    public List<String> getApplied() {
        return applied;
    }

    /// What the run tolerated: a history newer than this build, for one.
    /// @return the warnings, empty when there were none
    public List<String> getWarnings() {
        return warnings;
    }
}
