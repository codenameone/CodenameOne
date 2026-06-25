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

/// A single code completion proposal returned by a `CodeCompletionProvider` and shown in the
/// `CodeEditor` completion popup.
///
/// @author Shai Almog
public class CodeCompletion {
    private final String displayText;
    private final String insertText;
    private String type;
    private String detail;

    /// Creates a completion whose displayed and inserted text are identical.
    ///
    /// #### Parameters
    ///
    /// - `text`: the text shown in the popup and inserted when chosen
    public CodeCompletion(String text) {
        this(text, text);
    }

    /// Creates a completion with distinct display and insertion text.
    ///
    /// #### Parameters
    ///
    /// - `displayText`: the text shown in the completion popup
    ///
    /// - `insertText`: the text inserted into the editor when this completion is chosen
    public CodeCompletion(String displayText, String insertText) {
        this.displayText = displayText;
        this.insertText = insertText;
    }

    /// The text shown to the user in the completion popup.
    public String getDisplayText() {
        return displayText;
    }

    /// The text inserted into the editor when this completion is accepted.
    public String getInsertText() {
        return insertText;
    }

    /// An optional category used to badge / icon the entry, e.g. `"method"`, `"keyword"`,
    /// `"variable"`, `"class"`, `"snippet"`.
    public String getType() {
        return type;
    }

    /// Sets the completion category. Returns this for chaining.
    ///
    /// #### Parameters
    ///
    /// - `type`: the completion category
    public CodeCompletion setType(String type) {
        this.type = type;
        return this;
    }

    /// Optional secondary detail (e.g. a method signature or return type) shown alongside the entry.
    public String getDetail() {
        return detail;
    }

    /// Sets the optional secondary detail. Returns this for chaining.
    ///
    /// #### Parameters
    ///
    /// - `detail`: the detail string
    public CodeCompletion setDetail(String detail) {
        this.detail = detail;
        return this;
    }
}
