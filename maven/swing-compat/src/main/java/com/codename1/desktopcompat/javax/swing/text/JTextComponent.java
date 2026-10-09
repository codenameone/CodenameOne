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
package com.codename1.desktopcompat.javax.swing.text;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.event.FocusEvent;
import com.codename1.desktopcompat.javax.swing.Action;
import com.codename1.desktopcompat.javax.swing.InputVerifier;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.JViewport;
import com.codename1.desktopcompat.javax.swing.Scrollable;
import com.codename1.desktopcompat.javax.swing.event.CaretEvent;
import com.codename1.desktopcompat.javax.swing.event.CaretListener;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;
import com.codename1.desktopcompat.javax.swing.event.DocumentEvent;
import com.codename1.desktopcompat.javax.swing.event.DocumentListener;
import com.codename1.desktopcompat.rt.Units;
import com.codename1.ui.Display;
import com.codename1.ui.events.DataChangedListener;

/// The base of the text components.
///
/// The [Document] is the model and the Codename One text widget is the
/// editor, and the two are kept the same in both directions. A change of
/// the document is written to the widget. What the user types into the
/// widget is compared with the document and applied to it as one
/// replacement of the characters that differ, through the document's
/// [DocumentFilter]; when the filter refused or altered the edit the
/// widget is set back to what the document then holds. Document listeners
/// therefore see insert and remove events with real offsets for native
/// typing.
///
/// The caret is a dot and a mark kept on this side. It follows the text
/// (an insertion at or before it pushes it along), and the selection it
/// describes drives `getSelectedText`, `replaceSelection`, cut, copy and
/// paste -- through the Codename One clipboard -- but is not highlighted
/// in the widget. [#getActions] answers the editing actions of
/// [DefaultEditorKit] that this layer carries out. Not supported: key maps,
/// highlighters, `modelToView`/`viewToModel`, reading and writing streams,
/// input methods and printing. [#setDragEnabled] is kept as a property: no
/// drag starts from the text on its own. The input verifier, which the
/// desktop keeps on `JComponent`, is implemented here.
public abstract class JTextComponent extends JComponent implements Scrollable {

    public static final String FOCUS_ACCELERATOR_KEY = "focusAcceleratorKey";

    public static final String DEFAULT_KEYMAP = "default";

    private final Sync sync = new Sync();
    private Document model;
    private Caret caret;
    private boolean editable = true;
    private Insets margin;
    private Color caretColor;
    private Color selectionColor;
    private Color selectedTextColor;
    private Color disabledTextColor;
    private char focusAccelerator;
    /// The document is being written to the widget.
    private boolean pushing;
    /// The widget's text is being applied to the document.
    private boolean pulling;
    private boolean dragEnabled;
    /// The text component that had the focus last; a menu's actions work
    /// on it while the menu is open.
    private static JTextComponent lastFocused;

    public JTextComponent() {
        setCaret(new DefaultCaret());
    }

    // ------------------------------------------------------------ peer

    @Override
    protected void cn1PeerCreated() {
        super.cn1PeerCreated();
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (p instanceof com.codename1.ui.TextArea) {
            com.codename1.ui.TextArea t = (com.codename1.ui.TextArea) p;
            pushing = true;
            try {
                t.setText(getText());
            } finally {
                pushing = false;
            }
            t.setEditable(editable);
            t.addDataChangedListener(new DataChangedListener() {
                @Override
                public void dataChanged(int type, int index) {
                    cn1PullText();
                }
            });
            applyMargin();
        }
    }

    private com.codename1.ui.TextArea nativeText() {
        com.codename1.ui.Component p = cn1PeerOrNull();
        return p instanceof com.codename1.ui.TextArea ? (com.codename1.ui.TextArea) p : null;
    }

    /// Whether the widget's preferred size follows its text, so that a
    /// change of the text needs a new layout.
    protected boolean cn1GrowsWithText() {
        return false;
    }

    /// Called when the caret moved; a text area brings it into view.
    protected void cn1CaretMoved(int dot) {
    }

    /// Writes the document to the widget if they differ.
    private void pushText() {
        com.codename1.ui.TextArea t = nativeText();
        if (t != null) {
            String s = getText();
            String shown = t.getText();
            if (!s.equals(shown == null ? "" : shown)) {
                pushing = true;
                try {
                    t.setText(s);
                } finally {
                    pushing = false;
                }
            }
        }
        if (cn1GrowsWithText()) {
            revalidate();
        }
        repaint();
    }

    /// Applies what the widget shows to the document: the characters
    /// between the common start and the common end of the two texts are
    /// replaced, through the document filter.
    protected void cn1PullText() {
        com.codename1.ui.TextArea t = nativeText();
        if (pushing || pulling || t == null || model == null) {
            return;
        }
        String shown = t.getText();
        if (shown == null) {
            shown = "";
        }
        String old = getText();
        if (shown.equals(old)) {
            return;
        }
        int oldLen = old.length();
        int newLen = shown.length();
        int max = Math.min(oldLen, newLen);
        int prefix = 0;
        while (prefix < max && old.charAt(prefix) == shown.charAt(prefix)) {
            prefix++;
        }
        int suffix = 0;
        while (suffix < max - prefix && old.charAt(oldLen - 1 - suffix) == shown.charAt(newLen - 1 - suffix)) {
            suffix++;
        }
        String inserted = shown.substring(prefix, newLen - suffix);
        int removed = oldLen - prefix - suffix;
        pulling = true;
        try {
            if (model instanceof AbstractDocument) {
                ((AbstractDocument) model).replace(prefix, removed, inserted, null);
            } else {
                if (removed > 0) {
                    model.remove(prefix, removed);
                }
                if (inserted.length() > 0) {
                    model.insertString(prefix, inserted, null);
                }
            }
        } catch (BadLocationException e) {
            // The document refused the edit; the widget is set back below.
            repaint();
        } finally {
            pulling = false;
        }
        String now = getText();
        if (now.equals(shown)) {
            caret.setDot(prefix + inserted.length());
            if (cn1GrowsWithText()) {
                revalidate();
            }
        } else {
            pushText();
        }
    }

    // ------------------------------------------------------------ document

    public Document getDocument() {
        return model;
    }

    public void setDocument(Document doc) {
        Document old = model;
        if (old != null) {
            old.removeDocumentListener(sync);
        }
        model = doc;
        if (doc != null) {
            doc.addDocumentListener(sync);
        }
        firePropertyChange("document", old, doc);
        if (caret != null) {
            caret.setDot(0);
        }
        pushText();
    }

    public String getText() {
        Document doc = model;
        if (doc == null) {
            return "";
        }
        try {
            return doc.getText(0, doc.getLength());
        } catch (BadLocationException e) {
            return "";
        }
    }

    public String getText(int offs, int len) throws BadLocationException {
        if (model == null) {
            throw new BadLocationException("No document", offs);
        }
        return model.getText(offs, len);
    }

    public void setText(String t) {
        Document doc = model;
        if (doc == null) {
            return;
        }
        try {
            if (doc instanceof AbstractDocument) {
                ((AbstractDocument) doc).replace(0, doc.getLength(), t, null);
            } else {
                doc.remove(0, doc.getLength());
                if (t != null && t.length() > 0) {
                    doc.insertString(0, t, null);
                }
            }
        } catch (BadLocationException e) {
            // Positions 0..length are always inside the document.
            repaint();
        }
    }

    private final class Sync implements DocumentListener, ChangeListener {

        /// The caret is being moved by an edit whose text the widget does
        /// not have yet.
        private boolean deferReveal;
        private boolean revealOwed;

        @Override
        public void insertUpdate(DocumentEvent e) {
            int offs = e.getOffset();
            int len = e.getLength();
            int dot = caret.getDot();
            int mark = caret.getMark();
            deferReveal = true;
            try {
                moveCaret(mark >= offs ? mark + len : mark, dot >= offs ? dot + len : dot);
            } finally {
                deferReveal = false;
            }
            changed();
            revealDeferred();
        }

        /// Tells the component that the caret moved once the widget has
        /// the text that moved it. Showing the caret before that would
        /// scroll within the size of the old text, one line short of the
        /// end of a log that is being appended to.
        private void revealDeferred() {
            if (revealOwed) {
                revealOwed = false;
                cn1CaretMoved(caret.getDot());
            }
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            int offs = e.getOffset();
            int len = e.getLength();
            deferReveal = true;
            try {
                moveCaret(after(caret.getMark(), offs, len), after(caret.getDot(), offs, len));
            } finally {
                deferReveal = false;
            }
            changed();
            revealDeferred();
        }

        private int after(int p, int offs, int len) {
            return p >= offs + len ? p - len : p > offs ? offs : p;
        }

        private void moveCaret(int mark, int dot) {
            if (mark != caret.getMark() || dot != caret.getDot()) {
                caret.setDot(mark);
                if (dot != mark) {
                    caret.moveDot(dot);
                }
            }
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            changed();
        }

        private void changed() {
            if (!pulling) {
                pushText();
            }
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            int dot = caret.getDot();
            com.codename1.ui.Component p = cn1PeerOrNull();
            if (!pulling && p instanceof com.codename1.ui.TextField) {
                com.codename1.ui.TextField f = (com.codename1.ui.TextField) p;
                String s = f.getText();
                if (dot <= (s == null ? 0 : s.length()) && f.getCursorPosition() != dot) {
                    f.setCursorPosition(dot);
                }
            }
            fireCaretUpdate(new Moved(JTextComponent.this, dot, caret.getMark()));
            if (deferReveal) {
                revealOwed = true;
            } else {
                cn1CaretMoved(dot);
            }
        }
    }

    private static final class Moved extends CaretEvent {
        private static final long serialVersionUID = 1L;
        private final int dot;
        private final int mark;

        Moved(Object source, int dot, int mark) {
            super(source);
            this.dot = dot;
            this.mark = mark;
        }

        @Override
        public int getDot() {
            return dot;
        }

        @Override
        public int getMark() {
            return mark;
        }
    }

    // ------------------------------------------------------------ caret

    public Caret getCaret() {
        return caret;
    }

    public void setCaret(Caret c) {
        Caret old = caret;
        if (old != null) {
            old.removeChangeListener(sync);
            old.deinstall(this);
        }
        caret = c;
        if (c != null) {
            c.install(this);
            c.addChangeListener(sync);
        }
        firePropertyChange("caret", old, c);
    }

    public void addCaretListener(CaretListener listener) {
        listenerList.add(CaretListener.class, listener);
    }

    public void removeCaretListener(CaretListener listener) {
        listenerList.remove(CaretListener.class, listener);
    }

    public CaretListener[] getCaretListeners() {
        return listenerList.getListeners(CaretListener.class);
    }

    protected void fireCaretUpdate(CaretEvent e) {
        CaretListener[] ls = getCaretListeners();
        for (int i = ls.length - 1; i >= 0; i--) {
            ls[i].caretUpdate(e);
        }
    }

    private int length() {
        return model == null ? 0 : model.getLength();
    }

    public int getCaretPosition() {
        return caret.getDot();
    }

    public void setCaretPosition(int position) {
        if (position > length() || position < 0) {
            throw new IllegalArgumentException("bad position: " + position);
        }
        caret.setDot(position);
    }

    public void moveCaretPosition(int pos) {
        if (pos > length() || pos < 0) {
            throw new IllegalArgumentException("bad position: " + pos);
        }
        caret.moveDot(pos);
    }

    public int getSelectionStart() {
        return Math.min(caret.getDot(), caret.getMark());
    }

    public void setSelectionStart(int selectionStart) {
        select(selectionStart, getSelectionEnd());
    }

    public int getSelectionEnd() {
        return Math.max(caret.getDot(), caret.getMark());
    }

    public void setSelectionEnd(int selectionEnd) {
        select(getSelectionStart(), selectionEnd);
    }

    public void select(int selectionStart, int selectionEnd) {
        int len = length();
        if (selectionStart < 0) {
            selectionStart = 0;
        }
        if (selectionStart > len) {
            selectionStart = len;
        }
        if (selectionEnd > len) {
            selectionEnd = len;
        }
        if (selectionEnd < selectionStart) {
            selectionEnd = selectionStart;
        }
        setCaretPosition(selectionStart);
        moveCaretPosition(selectionEnd);
    }

    public void selectAll() {
        if (model != null) {
            setCaretPosition(0);
            moveCaretPosition(model.getLength());
        }
    }

    public String getSelectedText() {
        int p0 = getSelectionStart();
        int p1 = getSelectionEnd();
        if (p0 == p1 || model == null) {
            return null;
        }
        try {
            return model.getText(p0, p1 - p0);
        } catch (BadLocationException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /// Puts `content` in place of the selection, or at the caret when
    /// nothing is selected. Nothing happens when the component is not
    /// editable.
    public void replaceSelection(String content) {
        Document doc = model;
        if (doc == null || !editable) {
            return;
        }
        int p0 = getSelectionStart();
        int p1 = getSelectionEnd();
        try {
            if (doc instanceof AbstractDocument) {
                ((AbstractDocument) doc).replace(p0, p1 - p0, content, null);
            } else {
                if (p0 != p1) {
                    doc.remove(p0, p1 - p0);
                }
                if (content != null && content.length() > 0) {
                    doc.insertString(p0, content, null);
                }
            }
        } catch (BadLocationException e) {
            // The selection is always inside the document.
            repaint();
        }
    }

    // ------------------------------------------------------------ actions

    /// The editing commands of this component: cut, copy, paste, select
    /// all, insert break and insert tab, under their `DefaultEditorKit`
    /// names. Each works on the text component it is fired from, or on the
    /// one that had the focus last.
    public Action[] getActions() {
        return new DefaultEditorKit().getActions();
    }

    /// Records whether dragging the selection out of the component is
    /// wanted. No drag is started by the layer.
    public void setDragEnabled(boolean b) {
        dragEnabled = b;
    }

    public boolean getDragEnabled() {
        return dragEnabled;
    }

    /// The text component that has, or last had, the focus; null when
    /// none did or it is no longer showing.
    static JTextComponent cn1LastFocused() {
        JTextComponent c = lastFocused;
        return c != null && c.isShowing() ? c : null;
    }

    private static void cn1RememberFocus(JTextComponent c) {
        lastFocused = c;
    }

    // ------------------------------------------------------------ clipboard

    public void copy() {
        String s = getSelectedText();
        if (s != null && Display.isInitialized()) {
            Display.getInstance().copyToClipboard(s);
        }
    }

    public void cut() {
        if (editable && isEnabled() && getSelectedText() != null) {
            copy();
            replaceSelection("");
        }
    }

    public void paste() {
        if (editable && isEnabled() && Display.isInitialized()) {
            Object o = Display.getInstance().getPasteDataFromClipboard();
            if (o instanceof String) {
                replaceSelection((String) o);
            }
        }
    }

    // ------------------------------------------------------------ properties

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean b) {
        if (b != editable) {
            boolean old = editable;
            editable = b;
            com.codename1.ui.TextArea t = nativeText();
            if (t != null) {
                t.setEditable(b);
            }
            firePropertyChange("editable", old, b);
            repaint();
        }
    }

    public Insets getMargin() {
        return margin;
    }

    /// The space between the text and the edge of the widget, which
    /// becomes the widget's padding.
    public void setMargin(Insets m) {
        Insets old = margin;
        margin = m;
        firePropertyChange("margin", old, m);
        applyMargin();
        revalidate();
    }

    private void applyMargin() {
        com.codename1.ui.TextArea t = nativeText();
        if (t != null && margin != null) {
            com.codename1.ui.plaf.Style s = t.getAllStyles();
            s.setPaddingUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS);
            s.setPadding(Units.toDevice(margin.top), Units.toDevice(margin.bottom), Units.toDevice(margin.left),
                    Units.toDevice(margin.right));
        }
    }

    public Color getCaretColor() {
        return caretColor;
    }

    /// Recorded only.
    public void setCaretColor(Color c) {
        caretColor = c;
    }

    public Color getSelectionColor() {
        return selectionColor;
    }

    /// Recorded only.
    public void setSelectionColor(Color c) {
        selectionColor = c;
    }

    public Color getSelectedTextColor() {
        return selectedTextColor;
    }

    /// Recorded only.
    public void setSelectedTextColor(Color c) {
        selectedTextColor = c;
    }

    public Color getDisabledTextColor() {
        return disabledTextColor;
    }

    /// Recorded only.
    public void setDisabledTextColor(Color c) {
        disabledTextColor = c;
    }

    /// Recorded only.
    public void setFocusAccelerator(char aKey) {
        focusAccelerator = aKey;
    }

    public char getFocusAccelerator() {
        return focusAccelerator;
    }

    // ------------------------------------------------------------ verifier

    /// Asks the input verifier before the focus leaves: when it does not
    /// yield, the focus comes back here.
    @Override
    protected void processFocusEvent(FocusEvent e) {
        super.processFocusEvent(e);
        if (e.getID() == FocusEvent.FOCUS_GAINED) {
            cn1RememberFocus(this);
        }
        InputVerifier inputVerifier = getInputVerifier();
        if (e.getID() == FocusEvent.FOCUS_LOST && e.getOppositeComponent() == null && inputVerifier != null
                && !inputVerifier.shouldYieldFocus(this)) {
            requestFocusInWindow();
        }
    }

    // ------------------------------------------------------------ scrollable

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        int span = orientation == com.codename1.desktopcompat.javax.swing.SwingConstants.VERTICAL
                ? visibleRect.height : visibleRect.width;
        return Math.max(1, span / 10);
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return orientation == com.codename1.desktopcompat.javax.swing.SwingConstants.VERTICAL
                ? visibleRect.height : visibleRect.width;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return getParent() instanceof JViewport && getParent().getWidth() > getPreferredSize().width;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getParent() instanceof JViewport && getParent().getHeight() > getPreferredSize().height;
    }

    @Override
    protected String paramString() {
        return super.paramString() + ",editable=" + editable;
    }
}
