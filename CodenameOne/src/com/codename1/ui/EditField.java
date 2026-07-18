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

package com.codename1.ui;

import com.codename1.ui.editor.EditorHost;
import com.codename1.ui.editor.EditorView;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.ActionSource;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.EventDispatcher;

/// A pure Codename One text field.
///
/// `EditField` is a drop-in alternative to `TextField` / `TextArea` that edits text entirely with the
/// framework's own text engine (the same engine that backs `CodeEditor` and `RichTextArea`) instead of
/// a native peer. Selection, caret movement, word / line navigation, undo and clipboard all work on
/// every port: on iOS, Android and JavaScript the platform soft keyboard, hardware keyboard and IME
/// drive it through the low-level text-input SPI; on the desktop ports the raw physical-keyboard path
/// is used. Because there is no native peer the field is a real, self-contained lightweight component
/// -- it never floats above the UI, scrolls perfectly with its parent and can live anywhere a
/// `Component` can.
///
/// It intentionally mirrors the common `TextField` / `TextArea` API (`#setText(String)`, `#getText()`,
/// `#setHint(String)`, `#setConstraint(int)`, `#addDataChangedListener(DataChangedListener)`,
/// `#addActionListener(ActionListener)`, `#setSingleLineTextArea(boolean)`) and implements the same
/// `TextHolder` and `ActionSource` interfaces those components do, so code written against those
/// interfaces (validators, form binding, generic listeners) works unchanged. It also offers the same
/// constructor families: the `TextField`-style constructors create single-line fields while the
/// `TextArea`-style `(rows, columns)` constructors create multi-line editors, making the single- vs
/// multi-line choice explicit at construction. It is still a distinct class -- not assignable to
/// `TextField` / `TextArea` -- so switching to the pure engine is always a deliberate, opt-in choice.
///
/// ```java
/// // single line, like TextField
/// EditField name = new EditField("", "Name", TextArea.EMAILADDR);
/// name.addActionListener(e -> submit(name.getText()));
/// form.add(name);
///
/// // multi line, like TextArea
/// EditField notes = new EditField(5, 30);
/// form.add(notes);
/// ```
public class EditField extends EditorView implements EditorHost, TextHolder, ActionSource {
    private String hint = "";
    private boolean singleLine = true;
    private int rows = 1;
    private int columns = 20;
    private int constraint = TextArea.ANY;
    private int actionType = TextInputConfig.ACTION_DONE;
    private final EventDispatcher dataListeners = new EventDispatcher();
    private final EventDispatcher actionListeners = new EventDispatcher();

    /// Creates an empty single-line field.
    public EditField() {
        this("");
    }

    /// Creates an empty single-line field with the given visible column count.
    ///
    /// #### Parameters
    ///
    /// - `columns`: the number of visible columns used to compute the preferred width
    public EditField(int columns) {
        this("");
        setColumns(columns);
    }

    /// Creates a single-line field with the given initial text.
    ///
    /// #### Parameters
    ///
    /// - `text`: the initial text
    public EditField(String text) {
        super(null, false);
        setEditorHost(this);
        setUIID("TextField");
        if (text != null && text.length() > 0) {
            setText(text);
        }
    }

    /// Creates a single-line field with the given text and visible column count.
    ///
    /// #### Parameters
    ///
    /// - `text`: the initial text
    ///
    /// - `columns`: the number of visible columns
    public EditField(String text, int columns) {
        this(text);
        setColumns(columns);
    }

    /// Creates a single-line field with the given text and hint.
    ///
    /// #### Parameters
    ///
    /// - `text`: the initial text
    ///
    /// - `hint`: the placeholder shown while empty
    public EditField(String text, String hint) {
        this(text);
        setHint(hint);
    }

    /// Creates a single-line field with the given text, hint and constraint.
    ///
    /// #### Parameters
    ///
    /// - `text`: the initial text
    ///
    /// - `hint`: the placeholder shown while empty
    ///
    /// - `constraint`: one of the `TextArea` constraint constants (e.g. `TextArea#EMAILADDR`)
    public EditField(String text, String hint, int constraint) {
        this(text);
        setHint(hint);
        setConstraint(constraint);
    }

    /// Creates a single-line field with the given text, hint, visible column count and constraint.
    ///
    /// #### Parameters
    ///
    /// - `text`: the initial text
    ///
    /// - `hint`: the placeholder shown while empty
    ///
    /// - `columns`: the number of visible columns
    ///
    /// - `constraint`: one of the `TextArea` constraint constants (e.g. `TextArea#EMAILADDR`)
    public EditField(String text, String hint, int columns, int constraint) {
        this(text);
        setHint(hint);
        setColumns(columns);
        setConstraint(constraint);
    }

    /// Creates an empty multi-line field with the given visible row and column counts (the `TextArea`
    /// equivalent). Using this constructor sets the field to multi-line mode.
    ///
    /// #### Parameters
    ///
    /// - `rows`: the number of visible rows used to compute the preferred height
    ///
    /// - `columns`: the number of visible columns
    public EditField(int rows, int columns) {
        this("");
        setSingleLineTextArea(false);
        setRows(rows);
        setColumns(columns);
    }

    /// Creates a multi-line field with the given text, visible row and column counts (the `TextArea`
    /// equivalent). Using this constructor sets the field to multi-line mode.
    ///
    /// #### Parameters
    ///
    /// - `text`: the initial text
    ///
    /// - `rows`: the number of visible rows
    ///
    /// - `columns`: the number of visible columns
    public EditField(String text, int rows, int columns) {
        this(text);
        setSingleLineTextArea(false);
        setRows(rows);
        setColumns(columns);
    }

    /// Creates a multi-line field with the given text, visible row and column counts and constraint
    /// (the `TextArea` equivalent). Using this constructor sets the field to multi-line mode.
    ///
    /// #### Parameters
    ///
    /// - `text`: the initial text
    ///
    /// - `rows`: the number of visible rows
    ///
    /// - `columns`: the number of visible columns
    ///
    /// - `constraint`: one of the `TextArea` constraint constants
    public EditField(String text, int rows, int columns, int constraint) {
        this(text);
        setSingleLineTextArea(false);
        setRows(rows);
        setColumns(columns);
        setConstraint(constraint);
    }

    // ---- EditorHost bridge to the platform text-input SPI ----

    @Override
    public boolean isTextInputSupported() {
        return Display.impl.isTextInputSupported();
    }

    @Override
    public Object startTextInput(TextInputClient client, TextInputConfig config) {
        return Display.impl.startTextInput(client, config);
    }

    @Override
    public void updateTextInputState(Object handle, TextInputState state) {
        Display.impl.updateTextInputState(handle, state);
    }

    @Override
    public void stopTextInput(Object handle) {
        Display.impl.stopTextInput(handle);
    }

    @Override
    public void editorChanged() {
        dataListeners.fireDataChangeEvent(getCaretOffset(), DataChangedListener.CHANGED);
    }

    @Override
    public void fireEditorEvent(String type, String value) {
        // EditField exposes typed listeners (data change, action) rather than the semantic event
        // channel the rich/code editors use, so there is nothing to route here.
    }

    // ---- single line behavior ----

    @Override
    public TextInputConfig getConfig() {
        TextInputConfig cfg = new TextInputConfig();
        cfg.setMultiline(!singleLine);
        cfg.setConstraint(constraint);
        if (singleLine) {
            cfg.setActionType(actionType);
        }
        return cfg;
    }

    @Override
    public void onEditorAction(int action) {
        if (singleLine) {
            fireAction();
            return;
        }
        super.onEditorAction(action);
    }

    @Override
    protected boolean handleTypedText(String text) {
        if (singleLine && text != null && (text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0)) {
            // a single-line field never contains a line break: a bare newline (the return key) fires the
            // action, a pasted multi-line value keeps only its first line
            boolean bareNewline = text.length() == 1;
            String cleaned = stripLineBreaks(text);
            if (cleaned.length() > 0) {
                insertText(cleaned);
            }
            if (bareNewline) {
                fireAction();
            }
            return true;
        }
        return super.handleTypedText(text);
    }

    private static String stripLineBreaks(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != '\n' && c != '\r') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // ---- TextField-like API ----

    /// Sets the placeholder text shown while the field is empty.
    ///
    /// #### Parameters
    ///
    /// - `hint`: the placeholder, or null / empty for none
    public EditField setHint(String hint) {
        this.hint = hint == null ? "" : hint;
        repaint();
        return this;
    }

    /// Returns the placeholder text.
    @Override
    public String getHint() {
        return hint;
    }

    /// True for a single-line field (the default). A single-line field fires its action listeners on the
    /// return key instead of inserting a line break.
    public boolean isSingleLineTextArea() {
        return singleLine;
    }

    /// Sets whether this is a single-line field. Passing false makes it a multi-line editor.
    ///
    /// #### Parameters
    ///
    /// - `singleLine`: true for a single line, false for multi-line
    public void setSingleLineTextArea(boolean singleLine) {
        this.singleLine = singleLine;
        if (singleLine && rows > 1) {
            rows = 1;
        }
    }

    /// Sets the input constraint (one of the `TextArea` constraint constants) that hints the platform
    /// keyboard (e.g. email, numeric, URL).
    ///
    /// #### Parameters
    ///
    /// - `constraint`: a `TextArea` constraint constant
    public void setConstraint(int constraint) {
        this.constraint = constraint;
    }

    /// Returns the input constraint.
    public int getConstraint() {
        return constraint;
    }

    /// Sets the soft-keyboard action button for a single-line field (one of the
    /// `TextInputConfig#ACTION_DONE` family).
    public void setActionType(int actionType) {
        this.actionType = actionType;
    }

    /// Sets the number of visible rows used to compute the preferred height (multi-line fields).
    public void setRows(int rows) {
        this.rows = Math.max(1, rows);
        if (this.rows > 1) {
            singleLine = false;
        }
    }

    /// Returns the number of visible rows.
    public int getRows() {
        return rows;
    }

    /// Sets the number of visible columns used to compute the preferred width.
    public void setColumns(int columns) {
        this.columns = Math.max(1, columns);
    }

    /// Returns the number of visible columns.
    public int getColumns() {
        return columns;
    }

    /// Sets whether the field is editable. A non-editable field still allows selection and copying.
    ///
    /// #### Parameters
    ///
    /// - `editable`: true to allow editing
    public void setEditable(boolean editable) {
        setEditableState(editable);
    }

    /// Returns whether the field is editable.
    @Override
    public boolean isEditable() {
        return isEditableState();
    }

    /// Adds a listener notified whenever the text changes.
    public void addDataChangedListener(DataChangedListener l) {
        dataListeners.addListener(l);
    }

    /// Removes a data changed listener.
    public void removeDataChangedListener(DataChangedListener l) {
        dataListeners.removeListener(l);
    }

    /// Adds a listener notified when the action key (return on a single-line field) is pressed.
    public void addActionListener(ActionListener l) {
        actionListeners.addListener(l);
    }

    /// Removes an action listener.
    public void removeActionListener(ActionListener l) {
        actionListeners.removeListener(l);
    }

    private void fireAction() {
        if (actionListeners.hasListeners()) {
            actionListeners.fireActionEvent(new ActionEvent(this, ActionEvent.Type.Edit));
        }
    }

    // ---- rendering / sizing ----

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (hint.length() > 0 && getText().length() == 0 && !hasFocus()) {
            paintPlaceholder(g);
        }
    }

    private void paintPlaceholder(Graphics g) {
        Style s = getStyle();
        Font f = s.getFont();
        g.setColor(hintColor(s));
        g.setFont(f);
        int x = getX() + s.getPaddingLeftNoRTL();
        int y = getY() + s.getPaddingTop();
        int baseline = getHeight() - s.getPaddingTop() - s.getPaddingBottom() - f.getHeight();
        g.drawString(hint, x, y + Math.max(0, baseline / 2));
    }

    private int hintColor(Style s) {
        // a muted version of the foreground; falls back to a mid grey
        int fg = s.getFgColor();
        int r = (fg >> 16) & 0xff;
        int gg = (fg >> 8) & 0xff;
        int b = fg & 0xff;
        r = (r + 0x80) / 2;
        gg = (gg + 0x80) / 2;
        b = (b + 0x80) / 2;
        return (r << 16) | (gg << 8) | b;
    }

    @Override
    protected Dimension calcPreferredSize() {
        Style s = getStyle();
        Font f = s.getFont();
        int w = columns * f.charWidth('m') + s.getPaddingLeftNoRTL() + s.getPaddingRightNoRTL();
        int lineHeight = getLineHeight();
        int visibleRows = singleLine ? 1 : Math.max(1, rows);
        int h = visibleRows * lineHeight + s.getPaddingTop() + s.getPaddingBottom();
        return new Dimension(w, h);
    }
}
