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

import com.codename1.migration.JavaMigration;

/// One registered migration: a script for one engine or for all of them, or a Java migration.
///
/// Internal migration runtime; not an application API.
/// @hidden
@com.codename1.impl.SharedWithBackend
public final class MigrationEntry {
    /// History type of a script.
    public static final String SQL = "SQL";
    /// History type of a Java migration.
    public static final String JAVA = "JAVA";
    /// History type of the baseline marker.
    public static final String BASELINE = "BASELINE";

    private final String version;
    private final String description;
    private final String scriptName;
    private final String dialect;
    private final String text;
    private final MigrationSource source;
    private final int sourceId;
    private final JavaMigration java;
    private Integer checksum;
    private boolean transactional = true;

    /// Creates an entry. Exactly one of text, source and java is set.
    public MigrationEntry(String version, String description, String scriptName, String dialect, String text,
            MigrationSource source, int sourceId, Integer checksum, JavaMigration java) {
        if (description != null && description.length() > 200) {
            throw new IllegalArgumentException("A migration description cannot exceed 200 characters");
        }
        this.version = version == null ? null : MigrationVersion.normalize(version);
        this.description = description;
        this.scriptName = scriptName;
        this.dialect = dialect;
        this.text = text;
        this.source = source;
        this.sourceId = sourceId;
        this.checksum = checksum;
        this.java = java;
    }

    /// The normalized version, or null for a repeatable migration.
    public String version() {
        return version;
    }

    /// The description.
    public String description() {
        return description;
    }

    /// The script file name or Java class name recorded in the history.
    public String scriptName() {
        return scriptName;
    }

    /// The one engine this entry is for, or null for every engine.
    public String dialect() {
        return dialect;
    }

    /// The history type.
    public String type() {
        return java == null ? SQL : JAVA;
    }

    /// The Java migration, or null for a script.
    public JavaMigration java() {
        return java;
    }

    /// Whether the engine wraps this migration in a transaction.
    public boolean transactional() {
        return transactional;
    }

    /// Sets whether the engine wraps this migration in a transaction.
    public void transactional(boolean value) {
        transactional = value;
    }

    /// The script text, materialized on demand; null for a Java migration.
    public String script() {
        if (text != null) {
            return text;
        }
        return source == null ? null : source.script(sourceId);
    }

    /// The checksum, computed on first use for a script registered as text; null for Java.
    public Integer checksum() {
        if (checksum == null && java == null) {
            checksum = Integer.valueOf(MigrationChecksum.of(script()));
        }
        return checksum;
    }
}
