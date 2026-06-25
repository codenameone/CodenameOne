/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui;

/// A diagnostic (error / warning / information) shown in a `CodeEditor` as a squiggly underline over a
/// source range, a marker in the line-number gutter and a tooltip carrying the message.
///
/// Positions are 1-based: line `1` is the first line and column `1` is the first character of a line,
/// matching the convention used by most compilers and language servers.
///
/// @author Shai Almog
public class CodeDiagnostic {
    /// Severity for a problem that should block / is an error.
    public static final String ERROR = "error";
    /// Severity for a non-blocking warning.
    public static final String WARNING = "warning";
    /// Severity for purely informational hints.
    public static final String INFO = "info";

    private final int line;
    private final int column;
    private int endLine;
    private int endColumn;
    private String severity = ERROR;
    private final String message;

    /// Creates a single-position diagnostic that underlines from the given column to the end of the
    /// token / line.
    ///
    /// #### Parameters
    ///
    /// - `line`: the 1-based line
    ///
    /// - `column`: the 1-based start column
    ///
    /// - `message`: the human readable message
    public CodeDiagnostic(int line, int column, String message) {
        this(line, column, line, column, message);
    }

    /// Creates a diagnostic spanning an explicit range.
    ///
    /// #### Parameters
    ///
    /// - `line`: the 1-based start line
    ///
    /// - `column`: the 1-based start column
    ///
    /// - `endLine`: the 1-based end line
    ///
    /// - `endColumn`: the 1-based end column (exclusive)
    ///
    /// - `message`: the human readable message
    public CodeDiagnostic(int line, int column, int endLine, int endColumn, String message) {
        this.line = line;
        this.column = column;
        this.endLine = endLine;
        this.endColumn = endColumn;
        this.message = message;
    }

    /// The 1-based start line.
    public int getLine() {
        return line;
    }

    /// The 1-based start column.
    public int getColumn() {
        return column;
    }

    /// The 1-based end line.
    public int getEndLine() {
        return endLine;
    }

    /// The 1-based end column (exclusive).
    public int getEndColumn() {
        return endColumn;
    }

    /// The severity, one of `#ERROR`, `#WARNING` or `#INFO`.
    public String getSeverity() {
        return severity;
    }

    /// Sets the severity. Returns this for chaining.
    ///
    /// #### Parameters
    ///
    /// - `severity`: one of `#ERROR`, `#WARNING`, `#INFO`
    public CodeDiagnostic setSeverity(String severity) {
        this.severity = severity == null ? ERROR : severity;
        return this;
    }

    /// The message shown in the tooltip.
    public String getMessage() {
        return message;
    }
}
