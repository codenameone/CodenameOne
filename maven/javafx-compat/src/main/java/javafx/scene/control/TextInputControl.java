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
package javafx.scene.control;

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.Fonts;
import com.codename1.fxcompat.runtime.FxBoolean;
import com.codename1.fxcompat.runtime.FxObject;
import com.codename1.fxcompat.runtime.FxString;
import com.codename1.ui.Component;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.plaf.Style;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

/// The base of the controls the user types text into.
///
/// The native component is a Codename One `TextField` or `TextArea`.
/// The text property and the native text follow each other: setting the
/// property shows the text, and whatever the user types, or anything
/// else that changes the native text, sets the property and tells its
/// listeners. The prompt text is the native hint, shown while the control
/// is empty.
///
/// #### Caret and selection
///
/// The caret position, anchor and selection are kept by this class.
/// A single line field shows the caret where the caret position says and
/// reports the caret the user placed after each edit; the native editors
/// have no selection highlight, so a selection is only what
/// [#replaceSelection(String)], [#getSelectedText()] and the other
/// methods here work on, never something the user sees or makes.
/// Setting the text moves the caret to the start, as in JavaFX.
///
/// #### Not part of this layer
///
/// The clipboard methods, undo and redo, word navigation and
/// `TextFormatter`.
///
/// #### Styling
///
/// Through `cn1ApplyStyle`, besides the region names:
///
/// - `-fx-text-fill`: `Paint`; only a plain `Color` is shown
/// - `-fx-prompt-text-fill`: `Paint`; only a plain `Color` is shown
/// - `-fx-font`: `Font`
/// - `-fx-font-size`: `Number`, in logical pixels
/// - `-fx-font-family`: `String`
/// - `-fx-font-weight`: `FontWeight`, a `Number` or its name as a `String`
/// - `-fx-font-style`: `FontPosture`, or `"italic"`, `"oblique"`,
///   `"normal"`
///
/// The pseudo-class `readonly` is set while the control is not editable.
public abstract class TextInputControl extends Control {

    private static final int TEXT = Dirty.USER;
    private static final int EDITABLE = Dirty.USER << 1;
    private static final PseudoClass READONLY = PseudoClass.getPseudoClass("readonly");

    private final StringProperty text = new FxString(this, "text", "", TEXT | Dirty.NATIVE);
    private final StringProperty promptText = new FxString(this, "promptText", "", Dirty.NATIVE);
    private final BooleanProperty editable = new FxBoolean(this, "editable", true, EDITABLE | Dirty.NATIVE);
    private final ObjectProperty<Font> font = new FxObject<Font>(this, "font", null, Dirty.NATIVE | Dirty.LAYOUT);
    private final ReadOnlyIntegerWrapper length = new ReadOnlyIntegerWrapper(this, "length", 0);
    private final ReadOnlyIntegerWrapper anchor = new ReadOnlyIntegerWrapper(this, "anchor", 0);
    private final ReadOnlyIntegerWrapper caretPosition = new ReadOnlyIntegerWrapper(this, "caretPosition", 0);
    private final ReadOnlyObjectWrapper<IndexRange> selection = new ReadOnlyObjectWrapper<IndexRange>(this,
            "selection", new IndexRange(0, 0));
    private final ReadOnlyStringWrapper selectedText = new ReadOnlyStringWrapper(this, "selectedText", "");
    private Paint textFill;
    private Paint promptTextFill;
    private boolean fillShown;
    private int themeFill;
    private int themeOpacity;
    private boolean syncing;
    private boolean fromNative;
    private int pendingCaret = -1;

    /// Creates a text input control.
    protected TextInputControl() {
        getStyleClass().add("text-input");
    }

    /// Returns the listener a subclass adds to its native text component:
    /// it copies the native text into the text property.
    final DataChangedListener textBridge() {
        return new DataChangedListener() {
            @Override
            public void dataChanged(int type, int index) {
                Component c = cn1NativeIfCreated();
                if (syncing || text.isBound() || !(c instanceof com.codename1.ui.TextArea)) {
                    return;
                }
                fromNative = true;
                try {
                    text.set(((com.codename1.ui.TextArea) c).getText());
                } finally {
                    fromNative = false;
                }
            }
        };
    }

    @Override
    protected void cn1SyncNative() {
        super.cn1SyncNative();
        Component c = cn1NativeIfCreated();
        if (!(c instanceof com.codename1.ui.TextArea)) {
            return;
        }
        com.codename1.ui.TextArea area = (com.codename1.ui.TextArea) c;
        syncing = true;
        try {
            String t = getText();
            if (t == null) {
                t = "";
            }
            if (!t.equals(area.getText())) {
                area.setText(t);
            }
            String prompt = getPromptText();
            if (prompt == null) {
                prompt = "";
            }
            if (!prompt.equals(area.getHint()) && (prompt.length() > 0 || area.getHint() != null)) {
                area.setHint(prompt);
            }
            if (area.isEditable() != isEditable()) {
                area.setEditable(isEditable());
            }
            Style style = com.codename1.fxcompat.runtime.PeerPaint.allStyles(area);
            if (textFill instanceof Color) {
                if (!fillShown) {
                    // Remembered so that withdrawing the fill shows the
                    // theme's colour again.
                    Style theme = area.getUnselectedStyle();
                    themeFill = theme.getFgColor();
                    themeOpacity = theme.getOpacity();
                    fillShown = true;
                }
                int argb = ((Color) textFill).cn1Argb();
                style.setFgColor(argb & 0xffffff);
                style.setOpacity(argb >>> 24);
            } else if (fillShown) {
                fillShown = false;
                style.setFgColor(themeFill);
                style.setOpacity(themeOpacity);
            }
            Font f = font.get();
            if (f != null) {
                style.setFont(Fonts.of(f));
            }
            com.codename1.ui.Label hint = area.getHintLabel();
            if (hint != null && promptTextFill instanceof Color) {
                hint.getAllStyles().setFgColor(((Color) promptTextFill).cn1Argb() & 0xffffff);
            }
            if (hint != null && f != null) {
                // The prompt is written in the font of the control: the
                // hint is a label of its own, which otherwise keeps the
                // theme's size beside a field given a larger one.
                hint.getAllStyles().setFont(Fonts.of(f));
            }
        } finally {
            syncing = false;
        }
        showCaret();
    }

    private void showCaret() {
        Component c = cn1NativeIfCreated();
        if (c instanceof com.codename1.ui.TextField && !fromNative) {
            com.codename1.ui.TextField field = (com.codename1.ui.TextField) c;
            if (field.getCursorPosition() != getCaretPosition()) {
                field.setCursorPosition(getCaretPosition());
            }
        }
    }

    @Override
    public void cn1Invalidated(int what) {
        if ((what & TEXT) != 0) {
            String t = text.get();
            int len = t == null ? 0 : t.length();
            length.set(len);
            int caret = 0;
            if (pendingCaret >= 0) {
                caret = pendingCaret;
            } else if (fromNative) {
                Component c = cn1NativeIfCreated();
                caret = c instanceof com.codename1.ui.TextField
                        ? ((com.codename1.ui.TextField) c).getCursorPosition() : len;
            }
            moveSelection(caret, caret);
        }
        if ((what & EDITABLE) != 0) {
            pseudoClassStateChanged(READONLY, !isEditable());
        }
        if ((what & Dirty.NATIVE) != 0 && (what & Dirty.LAYOUT) != 0) {
            Component c = cn1NativeIfCreated();
            if (c != null) {
                c.setShouldCalcPreferredSize(true);
            }
        }
        super.cn1Invalidated(what);
    }

    private void moveSelection(int newAnchor, int newCaret) {
        int len = getLength();
        int a = Math.max(0, Math.min(len, newAnchor));
        int c = Math.max(0, Math.min(len, newCaret));
        anchor.set(a);
        caretPosition.set(c);
        IndexRange range = IndexRange.normalize(a, c);
        if (!range.equals(selection.get())) {
            selection.set(range);
        }
        String t = text.get();
        selectedText.set(t == null || range.getLength() == 0 ? "" : t.substring(range.getStart(), range.getEnd()));
    }

    // -------------------------------------------------------------- text

    /// Returns the text.
    public final String getText() {
        return text.get();
    }

    /// Sets the text and moves the caret to its start.
    public final void setText(String value) {
        text.set(value);
    }

    /// The text of the control.
    public final StringProperty textProperty() {
        return text;
    }

    /// Returns part of the text.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when the start is after the end
    /// - `IndexOutOfBoundsException`: when either is outside the text
    public String getText(int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException("The start must be <= the end");
        }
        if (start < 0 || end > getLength()) {
            throw new IndexOutOfBoundsException();
        }
        String t = text.get();
        return t == null ? "" : t.substring(start, end);
    }

    /// Returns the number of characters.
    public final int getLength() {
        return length.get();
    }

    /// The number of characters.
    public final ReadOnlyIntegerProperty lengthProperty() {
        return length.getReadOnlyProperty();
    }

    /// Adds text at the end and puts the caret after it.
    public void appendText(String text) {
        insertText(getLength(), text);
    }

    /// Inserts text at an index and puts the caret after it.
    public void insertText(int index, String text) {
        replaceText(index, index, text);
    }

    /// Deletes a range of the text.
    public void deleteText(IndexRange range) {
        replaceText(range, "");
    }

    /// Deletes the text between two indexes.
    public void deleteText(int start, int end) {
        replaceText(start, end, "");
    }

    /// Replaces a range of the text and puts the caret after the new
    /// text.
    public void replaceText(IndexRange range, String text) {
        replaceText(range.getStart(), range.getEnd(), text);
    }

    /// Replaces the text between two indexes and puts the caret after the
    /// new text. Does nothing while the text property is bound.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when the start is after the end
    /// - `NullPointerException`: when the text is `null`
    /// - `IndexOutOfBoundsException`: when an index is outside the text
    public void replaceText(int start, int end, String text) {
        if (start > end) {
            throw new IllegalArgumentException("The start must be <= the end");
        }
        if (text == null) {
            throw new NullPointerException("The text must not be null");
        }
        if (start < 0 || end > getLength()) {
            throw new IndexOutOfBoundsException();
        }
        if (this.text.isBound()) {
            return;
        }
        String old = this.text.get();
        if (old == null) {
            old = "";
        }
        int caret = start + text.length();
        pendingCaret = caret;
        try {
            this.text.set(old.substring(0, start) + text + old.substring(end));
        } finally {
            pendingCaret = -1;
        }
        selectRange(caret, caret);
    }

    /// Removes all the text.
    public void clear() {
        deselect();
        if (!text.isBound()) {
            setText("");
        }
    }

    // ---------------------------------------------------- caret, selection

    /// Returns the position of the caret.
    public final int getCaretPosition() {
        return caretPosition.get();
    }

    /// The position of the caret.
    public final ReadOnlyIntegerProperty caretPositionProperty() {
        return caretPosition.getReadOnlyProperty();
    }

    /// Returns the end of the selection that is not the caret.
    public final int getAnchor() {
        return anchor.get();
    }

    /// The end of the selection that is not the caret.
    public final ReadOnlyIntegerProperty anchorProperty() {
        return anchor.getReadOnlyProperty();
    }

    /// Returns the selected range; empty at the caret without one.
    public final IndexRange getSelection() {
        return selection.get();
    }

    /// The selected range.
    public final ReadOnlyObjectProperty<IndexRange> selectionProperty() {
        return selection.getReadOnlyProperty();
    }

    /// Returns the selected text; empty without a selection.
    public final String getSelectedText() {
        return selectedText.get();
    }

    /// The selected text.
    public final ReadOnlyStringProperty selectedTextProperty() {
        return selectedText.getReadOnlyProperty();
    }

    /// Selects from an anchor to a caret position; both are kept inside
    /// the text.
    public void selectRange(int anchor, int caretPosition) {
        moveSelection(anchor, caretPosition);
        showCaret();
    }

    /// Moves the caret, dropping the selection.
    public void positionCaret(int pos) {
        selectRange(pos, pos);
    }

    /// Moves the caret, keeping the anchor.
    public void selectPositionCaret(int pos) {
        selectRange(getAnchor(), pos);
    }

    /// Extends the selection to a position: the end of the selection
    /// farther from it becomes the anchor.
    public void extendSelection(int pos) {
        int p = Math.max(0, Math.min(getLength(), pos));
        int dot = getCaretPosition();
        int mark = getAnchor();
        int start = Math.min(dot, mark);
        int end = Math.max(dot, mark);
        if (p < start) {
            selectRange(end, p);
        } else {
            selectRange(start, p);
        }
    }

    /// Selects all the text.
    public void selectAll() {
        selectRange(0, getLength());
    }

    /// Drops the selection, leaving the caret where it is.
    public void deselect() {
        selectRange(getCaretPosition(), getCaretPosition());
    }

    /// Moves the caret to the start.
    public void home() {
        selectRange(0, 0);
    }

    /// Moves the caret to the end.
    public void end() {
        int len = getLength();
        selectRange(len, len);
    }

    /// Moves the caret to the start, keeping the anchor.
    public void selectHome() {
        selectRange(getAnchor(), 0);
    }

    /// Moves the caret to the end, keeping the anchor.
    public void selectEnd() {
        selectRange(getAnchor(), getLength());
    }

    /// Moves the caret one character on; with a selection, to its end.
    public void forward() {
        int len = getLength();
        int dot = getCaretPosition();
        int mark = getAnchor();
        if (dot != mark) {
            int pos = Math.max(dot, mark);
            selectRange(pos, pos);
        } else if (dot < len) {
            selectRange(dot + 1, dot + 1);
        }
    }

    /// Moves the caret one character back; with a selection, to its
    /// start.
    public void backward() {
        int dot = getCaretPosition();
        int mark = getAnchor();
        if (dot != mark) {
            int pos = Math.min(dot, mark);
            selectRange(pos, pos);
        } else if (dot > 0) {
            selectRange(dot - 1, dot - 1);
        }
    }

    /// Moves the caret one character on, keeping the anchor.
    public void selectForward() {
        if (getCaretPosition() < getLength()) {
            selectRange(getAnchor(), getCaretPosition() + 1);
        }
    }

    /// Moves the caret one character back, keeping the anchor.
    public void selectBackward() {
        if (getCaretPosition() > 0) {
            selectRange(getAnchor(), getCaretPosition() - 1);
        }
    }

    /// Replaces the selection, or inserts at the caret without one.
    public void replaceSelection(String replacement) {
        IndexRange range = getSelection();
        replaceText(range.getStart(), range.getEnd(), replacement);
    }

    /// Deletes the selection, or without one the character before the
    /// caret. Answers whether anything was deleted.
    public boolean deletePreviousChar() {
        if (!isEditable() || isDisabled()) {
            return false;
        }
        IndexRange range = getSelection();
        if (range.getLength() > 0) {
            replaceText(range.getStart(), range.getEnd(), "");
            return true;
        }
        int dot = getCaretPosition();
        if (dot == 0) {
            return false;
        }
        replaceText(dot - 1, dot, "");
        return true;
    }

    /// Deletes the selection, or without one the character after the
    /// caret. Answers whether anything was deleted.
    public boolean deleteNextChar() {
        if (!isEditable() || isDisabled()) {
            return false;
        }
        IndexRange range = getSelection();
        if (range.getLength() > 0) {
            replaceText(range.getStart(), range.getEnd(), "");
            return true;
        }
        int dot = getCaretPosition();
        if (dot >= getLength()) {
            return false;
        }
        replaceText(dot, dot + 1, "");
        return true;
    }

    // -------------------------------------------------------- properties

    /// Returns the text shown while the control is empty.
    public final String getPromptText() {
        return promptText.get();
    }

    /// Sets the text shown while the control is empty.
    public final void setPromptText(String value) {
        promptText.set(value);
    }

    /// The text shown while the control is empty.
    public final StringProperty promptTextProperty() {
        return promptText;
    }

    /// Returns whether the user can change the text.
    public final boolean isEditable() {
        return editable.get();
    }

    /// Sets whether the user can change the text.
    public final void setEditable(boolean value) {
        editable.set(value);
    }

    /// Whether the user can change the text.
    public final BooleanProperty editableProperty() {
        return editable;
    }

    /// Returns the font; the default font when none was set.
    public final Font getFont() {
        Font f = font.get();
        return f == null ? Font.getDefault() : f;
    }

    /// Sets the font.
    public final void setFont(Font value) {
        font.set(value);
    }

    /// The font of the text.
    public final ObjectProperty<Font> fontProperty() {
        return font;
    }

    // ------------------------------------------------------------ styling

    private void restyleNative() {
        cn1Invalidated(Dirty.NATIVE);
    }

    @Override
    protected Object cn1StyleValue(String property) {
        if ("-fx-text-fill".equals(property)) {
            return textFill;
        } else if ("-fx-prompt-text-fill".equals(property)) {
            return promptTextFill;
        } else if ("-fx-font".equals(property) || "-fx-font-size".equals(property)
                || "-fx-font-family".equals(property) || "-fx-font-weight".equals(property)
                || "-fx-font-style".equals(property)) {
            return font.get();
        }
        return super.cn1StyleValue(property);
    }

    private static FontWeight weight(Object value) {
        if (value instanceof FontWeight) {
            return (FontWeight) value;
        } else if (value instanceof Number) {
            return FontWeight.findByWeight(((Number) value).intValue());
        } else if (value instanceof String) {
            return FontWeight.findByName(((String) value).trim());
        }
        return null;
    }

    private static FontPosture posture(Object value) {
        if (value instanceof FontPosture) {
            return (FontPosture) value;
        } else if (value instanceof String) {
            String s = ((String) value).trim();
            if ("italic".equalsIgnoreCase(s) || "oblique".equalsIgnoreCase(s)) {
                return FontPosture.ITALIC;
            } else if ("normal".equalsIgnoreCase(s) || "regular".equalsIgnoreCase(s)) {
                return FontPosture.REGULAR;
            }
        }
        return null;
    }

    @Override
    protected boolean cn1SetStyleValue(String property, Object value) {
        if ("-fx-text-fill".equals(property)) {
            if (value != null && !(value instanceof Paint)) {
                return false;
            }
            textFill = (Paint) value;
            restyleNative();
        } else if ("-fx-prompt-text-fill".equals(property)) {
            if (value != null && !(value instanceof Paint)) {
                return false;
            }
            promptTextFill = (Paint) value;
            restyleNative();
        } else if (property.startsWith("-fx-font")) {
            // Restoring any of the font names restores the whole font.
            if (value == null || value instanceof Font) {
                setFont((Font) value);
                return true;
            }
            Font base = getFont();
            FontWeight w = base.cn1Weight();
            FontPosture p = base.cn1Posture();
            String family = base.getFamily();
            double size = base.getSize();
            if ("-fx-font-size".equals(property) && value instanceof Number) {
                size = ((Number) value).doubleValue();
            } else if ("-fx-font-family".equals(property) && value instanceof String) {
                family = (String) value;
            } else if ("-fx-font-weight".equals(property) && weight(value) != null) {
                w = weight(value);
            } else if ("-fx-font-style".equals(property) && posture(value) != null) {
                p = posture(value);
            } else {
                return false;
            }
            setFont(Font.font(family, w, p, size));
        } else {
            return super.cn1SetStyleValue(property, value);
        }
        return true;
    }
}
