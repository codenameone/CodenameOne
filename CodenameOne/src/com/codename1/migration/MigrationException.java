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

import java.io.IOException;

/// A migration run that was refused or failed. [#getCode()] says which, so a caller can tell
/// a database that is newer than the application from a script that did not run.
@com.codename1.impl.SharedWithBackend
public class MigrationException extends IOException {
    /// The applied history does not match the migrations this build carries.
    public static final int VALIDATE_FAILED = 1;
    /// A previous run left a migration marked failed; call [Migrator#repair()] after cleaning up.
    public static final int FAILED_MIGRATION_PRESENT = 2;
    /// The database was migrated by a newer build than this one.
    public static final int FUTURE_SCHEMA = 3;
    /// [Migrator#clean()] was called while clean is disabled.
    public static final int CLEAN_DISABLED = 4;
    /// Another process held the migration lock for longer than this run was willing to wait.
    public static final int LOCK_TIMEOUT = 5;
    /// A migration script or Java migration threw.
    public static final int SCRIPT_FAILED = 6;
    /// The database has tables and no history, and no baseline was requested.
    public static final int NON_EMPTY_SCHEMA = 7;
    /// The migration set itself is wrong: a duplicate version, a script missing for this engine.
    public static final int INVALID_SET = 8;

    private final int code;

    /// Creates a failure of the given kind.
    /// @param code one of the constants of this class
    /// @param message what was refused and what to do about it
    public MigrationException(int code, String message) {
        super(message);
        this.code = code;
    }

    /// Creates a failure of the given kind, keeping the database error that caused it.
    /// @param code one of the constants of this class
    /// @param message what was refused and what to do about it
    /// @param cause the underlying failure
    public MigrationException(int code, String message, Throwable cause) {
        super(message);
        this.code = code;
        if (cause != null) {
            initCause(cause);
        }
    }

    /// The kind of failure.
    /// @return one of the constants of this class
    public int getCode() {
        return code;
    }
}
