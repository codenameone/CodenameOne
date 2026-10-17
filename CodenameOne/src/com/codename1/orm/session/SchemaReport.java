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
package com.codename1.orm.session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// What [Session#schemaReport()] found when it compared the registered mappings
/// with the tables that already exist. Reading it changes nothing in the
/// database.
///
/// Every difference is one of three kinds, because they call for different fixes:
///
/// - [Kind#MIGRATION_REQUIRED]: the mapping is fine, the schema is behind it -- a
///   table or column is missing. A migration brings the database to the mapping.
/// - [Kind#UNSUPPORTED]: the existing column cannot hold what the mapping would
///   write, or a key the mapping does not know identifies the rows. Data would be
///   misread or refused, so the mapping has to change -- a different storage
///   type, `@Temporal` for a native timestamp, an explicit `@Column` name -- or
///   the column has to be converted deliberately.
/// - [Kind#CONSTRAINT_MISMATCH]: a column's nullability differs from the
///   mapping. Rows still read and write, but the database accepts a null the
///   mapping cannot hold, or refuses one it can. A schema that is deliberately
///   stricter than the mapping is legitimate, which is why this is kept apart.
///
/// ```java
/// SchemaReport report = session.schemaReport();
/// for (SchemaReport.Issue issue : report.getIssues()) {
///     Log.p(issue.getKind() + " " + issue.getTable() + "." + issue.getColumn()
///             + ": " + issue.getMessage());
/// }
/// ```
@com.codename1.impl.SharedWithBackend
public final class SchemaReport {
    /// How an [Issue] is resolved.
    public enum Kind {
        /// The schema lacks something the mapping declares; a migration fixes it.
        MIGRATION_REQUIRED,
        /// The mapping cannot be used with the column as it stands.
        UNSUPPORTED,
        /// The column's nullability differs from the mapping's.
        CONSTRAINT_MISMATCH
    }

    /// One difference between a mapping and the existing schema.
    public static final class Issue {
        private final Kind kind;
        private final String table;
        private final String column;
        private final String message;

        /// Creates an issue.
        /// @param kind how the issue is resolved
        /// @param table the table it was found in
        /// @param column the column, or null for a table-level issue
        /// @param message a description of the difference
        public Issue(Kind kind, String table, String column, String message) {
            this.kind = kind;
            this.table = table;
            this.column = column;
            this.message = message;
        }

        /// How the issue is resolved.
        public Kind getKind() {
            return kind;
        }

        /// The table the issue was found in.
        public String getTable() {
            return table;
        }

        /// The column, or null when the whole table is missing.
        public String getColumn() {
            return column;
        }

        /// A description of the difference.
        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return kind + ": " + message;
        }
    }

    private final List<Issue> issues;

    /// Creates a report holding `issues`, in the order they were found.
    /// @param issues the differences found; copied
    public SchemaReport(List<Issue> issues) {
        this.issues = Collections.unmodifiableList(new ArrayList<Issue>(issues));
    }

    /// True when the existing schema matches every mapping.
    public boolean isCompatible() {
        return issues.isEmpty();
    }

    /// Every difference found, unmodifiable.
    public List<Issue> getIssues() {
        return issues;
    }

    /// The differences of one kind.
    /// @param kind the kind to select
    /// @return the matching issues, in the order they were found
    public List<Issue> getIssues(Kind kind) {
        List<Issue> result = new ArrayList<Issue>();
        for (Issue issue : issues) {
            if (issue.kind == kind) {
                result.add(issue);
            }
        }
        return result;
    }

    /// A multi-line summary: unsupported mappings first, then required
    /// migrations, then constraint differences.
    @Override
    public String toString() {
        if (issues.isEmpty()) {
            return "Schema matches the mappings";
        }
        StringBuilder out = new StringBuilder();
        appendKind(out, Kind.UNSUPPORTED, "Unsupported mappings (change the mapping or convert the column):");
        appendKind(out, Kind.MIGRATION_REQUIRED, "Migrations required:");
        appendKind(out, Kind.CONSTRAINT_MISMATCH, "Nullability differs from the mapping:");
        return out.toString();
    }

    private void appendKind(StringBuilder out, Kind kind, String heading) {
        List<Issue> selected = getIssues(kind);
        if (selected.isEmpty()) {
            return;
        }
        if (out.length() > 0) {
            out.append('\n');
        }
        out.append(heading);
        for (Issue issue : selected) {
            out.append("\n  ").append(issue.message);
        }
    }
}
