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
package com.codename1.ui.editor;

import com.codename1.ui.Component;

/// The pure Codename One editor backend. It owns an `EditorView` and translates the semantic
/// command / query vocabulary spoken by `RichTextArea` and `CodeEditor` (the same strings the
/// `BrowserComponent` backend understands) into operations on the pure text engine.
///
/// This base class handles the shared, plain text portion of the vocabulary. The rich text and code
/// feature layers subclass it (and subclass `EditorView`) to add styling, syntax highlighting, the
/// gutter, completion and diagnostics.
public class PureEditor {
    private final EditorView view;
    private final boolean codeMode;

    /// Creates a pure editor backend.
    ///
    /// #### Parameters
    ///
    /// - `host`: the bridge to the owning editor component
    ///
    /// - `editorType`: `"code"` or `"richtext"`
    public PureEditor(EditorHost host, String editorType) {
        this.codeMode = "code".equals(editorType);
        this.view = createView(host, codeMode);
    }

    /// Creates the editor view. Subclasses override to supply a code or rich text view.
    protected EditorView createView(EditorHost host, boolean codeMode) {
        return new EditorView(host, codeMode);
    }

    /// Returns the editing surface component to place in the editor container.
    public Component getView() {
        return view;
    }

    /// Returns the underlying editor view.
    protected EditorView view() {
        return view;
    }

    /// True for a code editor backend.
    protected boolean isCodeMode() {
        return codeMode;
    }

    /// Executes a one way command. Unknown commands are ignored so subclasses can add vocabulary without
    /// breaking the base.
    ///
    /// #### Parameters
    ///
    /// - `name`: the command name
    ///
    /// - `arg`: the optional argument, may be null
    public void cmd(String name, String arg) {
        if ("setText".equals(name) || "setHtml".equals(name)) {
            view.setText(arg == null ? "" : arg);
            return;
        }
        if ("insertText".equals(name) || "insertHtml".equals(name)) {
            view.insertText(arg == null ? "" : arg);
            return;
        }
        if ("setEditable".equals(name)) {
            view.setEditableState("1".equals(arg));
            return;
        }
        if ("focus".equals(name)) {
            view.requestFocus();
            return;
        }
        if ("blur".equals(name)) {
            view.blur();
            return;
        }
        if ("undo".equals(name)) {
            view.performUndo();
            return;
        }
        if ("redo".equals(name)) {
            view.performRedo();
            return;
        }
        // remaining rich / code commands are handled by subclasses; ignore here
    }

    /// Executes a query returning a string result. Unknown queries return an empty string.
    ///
    /// #### Parameters
    ///
    /// - `name`: the query name
    ///
    /// - `arg`: the optional argument, may be null
    public String query(String name, String arg) {
        if ("getText".equals(name)) {
            return view.getText();
        }
        if ("getHtml".equals(name)) {
            return view.getText();
        }
        if ("getCursor".equals(name)) {
            return String.valueOf(view.getCaretOffset());
        }
        if ("state".equals(name)) {
            return "0";
        }
        return "";
    }
}
