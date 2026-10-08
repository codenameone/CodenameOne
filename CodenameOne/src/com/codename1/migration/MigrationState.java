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

/// Where one migration stands, as reported by [Migrator#info()].
@com.codename1.impl.SharedWithBackend
public enum MigrationState {
    /// Carried by this build and not applied yet.
    PENDING,
    /// Applied.
    SUCCESS,
    /// Applied and failed; the database may hold half of it.
    FAILED,
    /// The baseline marker: everything up to this version is taken as already present.
    BASELINE,
    /// At or below the baseline, so it is never applied.
    BELOW_BASELINE,
    /// Applied, and no longer carried by this build.
    MISSING,
    /// Applied by a newer build: its version is above everything this build carries.
    FUTURE,
    /// Not applied, and older than the newest applied version.
    IGNORED,
    /// A repeatable migration whose script changed since it last ran.
    OUTDATED,
    /// An earlier run of a repeatable migration that a later run replaced.
    SUPERSEDED,
    /// Above the configured target version.
    ABOVE_TARGET
}
