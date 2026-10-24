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

import com.codename1.desktopcompat.javax.swing.event.DocumentEvent;
import com.codename1.desktopcompat.javax.swing.event.DocumentListener;
import com.codename1.desktopcompat.javax.swing.event.UndoableEditEvent;
import com.codename1.desktopcompat.javax.swing.event.UndoableEditListener;
import com.codename1.desktopcompat.javax.swing.undo.CannotRedoException;
import com.codename1.desktopcompat.javax.swing.undo.CannotUndoException;
import com.codename1.desktopcompat.javax.swing.undo.CompoundEdit;
import com.codename1.desktopcompat.javax.swing.event.EventListenerList;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashMap;

/// The working part of a document: the characters, the listeners, the
/// positions that follow the text and the filter every change goes
/// through.
///
/// The text is one string buffer. There is no content interface, no
/// locking (documents belong to the event dispatch thread), no undo and no
/// attribute context, and a document cannot be made with a storage of the
/// caller's choosing; subclass [PlainDocument] rather than this class.
public abstract class AbstractDocument implements Document {

    protected static final String BAD_LOCATION = "document location failure";

    public static final String ParagraphElementName = "paragraph";

    public static final String ContentElementName = "content";

    public static final String SectionElementName = "section";

    public static final String BidiElementName = "bidi level";

    public static final String ElementNameAttribute = "$ename";

    protected EventListenerList listenerList = new EventListenerList();

    private final StringBuilder text = new StringBuilder();
    private final ArrayList<WeakReference<Sticky>> positions = new ArrayList<WeakReference<Sticky>>();
    private HashMap<Object, Object> properties;
    private DocumentFilter documentFilter;
    private DocumentFilter.FilterBypass filterBypass;
    private int modCount;
    /// An undo or a redo is changing the text.
    private boolean replaying;

    AbstractDocument() {
    }

    /// Counts the changes made so far; the line structure is computed
    /// again when it moved.
    int cn1ModCount() {
        return modCount;
    }

    /// The characters, for the classes of this package.
    CharSequence cn1Chars() {
        return text;
    }

    @Override
    public int getLength() {
        return text.length();
    }

    @Override
    public String getText(int offset, int length) throws BadLocationException {
        if (length < 0) {
            throw new BadLocationException("Length must be positive", length);
        }
        if (offset < 0 || offset + length > text.length()) {
            throw new BadLocationException("Invalid location", offset < 0 ? offset : offset + length);
        }
        return text.substring(offset, offset + length);
    }

    @Override
    public void getText(int offset, int length, Segment txt) throws BadLocationException {
        String s = getText(offset, length);
        txt.array = s.toCharArray();
        txt.offset = 0;
        txt.count = txt.array.length;
    }

    // ------------------------------------------------------------ changes

    @Override
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
        if (str == null || str.length() == 0) {
            return;
        }
        if (documentFilter != null) {
            documentFilter.insertString(getFilterBypass(), offs, str, a);
        } else {
            handleInsertString(offs, str, a);
        }
    }

    @Override
    public void remove(int offs, int len) throws BadLocationException {
        if (documentFilter != null) {
            documentFilter.remove(getFilterBypass(), offs, len);
        } else {
            handleRemove(offs, len);
        }
    }

    /// Removes `length` characters at `offset` and puts `text` there, as
    /// one pass through the filter.
    public void replace(int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        if (length == 0 && (text == null || text.length() == 0)) {
            return;
        }
        if (documentFilter != null) {
            documentFilter.replace(getFilterBypass(), offset, length, text, attrs);
        } else {
            if (length > 0) {
                handleRemove(offset, length);
            }
            if (text != null && text.length() > 0) {
                handleInsertString(offset, text, attrs);
            }
        }
    }

    /// The text that really goes in for an insertion of `str`; the same
    /// length, so offsets computed before it stay right.
    String cn1FilterInsert(String str) {
        return str;
    }

    void handleInsertString(int offs, String str, AttributeSet a) throws BadLocationException {
        if (str == null || str.length() == 0) {
            return;
        }
        if (offs < 0 || offs > text.length()) {
            throw new BadLocationException("Invalid insert", offs);
        }
        str = cn1FilterInsert(str);
        text.insert(offs, str);
        modCount++;
        int len = str.length();
        for (int i = positions.size() - 1; i >= 0; i--) {
            Sticky p = positions.get(i).get();
            if (p == null) {
                positions.remove(i);
            } else if (p.offset > offs || (p.offset == offs && offs != 0)) {
                p.offset += len;
            }
        }
        DefaultDocumentEvent e = new DefaultDocumentEvent(offs, len, DocumentEvent.EventType.INSERT);
        e.cn1Text = str;
        insertUpdate(e, a);
        e.end();
        fireInsertUpdate(e);
        cn1PostEdit(e);
    }

    void handleRemove(int offs, int len) throws BadLocationException {
        if (len <= 0) {
            return;
        }
        if (offs < 0 || offs + len > text.length()) {
            throw new BadLocationException("Invalid remove", offs < 0 ? offs : offs + len);
        }
        DefaultDocumentEvent e = new DefaultDocumentEvent(offs, len, DocumentEvent.EventType.REMOVE);
        e.cn1Text = text.substring(offs, offs + len);
        removeUpdate(e);
        text.delete(offs, offs + len);
        modCount++;
        for (int i = positions.size() - 1; i >= 0; i--) {
            Sticky p = positions.get(i).get();
            if (p == null) {
                positions.remove(i);
            } else if (p.offset >= offs + len) {
                p.offset -= len;
            } else if (p.offset > offs) {
                p.offset = offs;
            }
        }
        postRemoveUpdate(e);
        e.end();
        fireRemoveUpdate(e);
        cn1PostEdit(e);
    }

    /// Posts the edit of a change to the undoable edit listeners, unless
    /// the change is itself an undo or a redo.
    private void cn1PostEdit(DefaultDocumentEvent e) {
        if (!replaying && listenerList.getListenerCount(UndoableEditListener.class) > 0) {
            fireUndoableEditUpdate(new UndoableEditEvent(this, e));
        }
    }

    /// Takes an edit back or applies it again: the plain insertion or
    /// removal, with the document listeners told and no new edit posted.
    void cn1Replay(boolean insert, int offs, String str) {
        boolean was = replaying;
        replaying = true;
        try {
            if (insert) {
                handleInsertString(Math.min(offs, text.length()), str, null);
            } else {
                int from = Math.min(offs, text.length());
                handleRemove(from, Math.min(str.length(), text.length() - from));
            }
        } catch (BadLocationException e) {
            // The offsets were clamped to the document above.
            throw new IllegalStateException(e.getMessage());
        } finally {
            replaying = was;
        }
    }

    /// Called after text went in and before the listeners hear of it.
    protected void insertUpdate(DefaultDocumentEvent chng, AttributeSet attr) {
    }

    /// Called before text is taken out.
    protected void removeUpdate(DefaultDocumentEvent chng) {
    }

    /// Called after text was taken out and before the listeners hear of
    /// it.
    protected void postRemoveUpdate(DefaultDocumentEvent chng) {
    }

    // ------------------------------------------------------------ filter

    public void setDocumentFilter(DocumentFilter filter) {
        documentFilter = filter;
    }

    public DocumentFilter getDocumentFilter() {
        return documentFilter;
    }

    private DocumentFilter.FilterBypass getFilterBypass() {
        if (filterBypass == null) {
            filterBypass = new Bypass();
        }
        return filterBypass;
    }

    private final class Bypass extends DocumentFilter.FilterBypass {

        @Override
        public Document getDocument() {
            return AbstractDocument.this;
        }

        @Override
        public void remove(int offset, int length) throws BadLocationException {
            handleRemove(offset, length);
        }

        @Override
        public void insertString(int offset, String string, AttributeSet attr) throws BadLocationException {
            handleInsertString(offset, string, attr);
        }

        @Override
        public void replace(int offset, int length, String string, AttributeSet attrs) throws BadLocationException {
            handleRemove(offset, length);
            handleInsertString(offset, string, attrs);
        }
    }

    // ------------------------------------------------------------ positions

    private static final class Sticky implements Position {
        int offset;

        Sticky(int offset) {
            this.offset = offset;
        }

        @Override
        public int getOffset() {
            return offset;
        }

        @Override
        public String toString() {
            return Integer.toString(offset);
        }
    }

    @Override
    public Position createPosition(int offs) throws BadLocationException {
        if (offs < 0 || offs > text.length()) {
            throw new BadLocationException("Invalid position", offs);
        }
        Sticky p = new Sticky(offs);
        positions.add(new WeakReference<Sticky>(p));
        return p;
    }

    @Override
    public final Position getStartPosition() {
        return new Sticky(0);
    }

    /// The position after the last character, which follows the end of the
    /// text. Unlike on the desktop there is no implied line break behind
    /// the text, so this is the length and not one more.
    @Override
    public final Position getEndPosition() {
        return new Position() {
            @Override
            public int getOffset() {
                return getLength();
            }
        };
    }

    // ------------------------------------------------------------ structure

    @Override
    public Element[] getRootElements() {
        return new Element[]{getDefaultRootElement()};
    }

    @Override
    public abstract Element getDefaultRootElement();

    public abstract Element getParagraphElement(int pos);

    // ------------------------------------------------------------ properties

    @Override
    public final Object getProperty(Object key) {
        return properties == null ? null : properties.get(key);
    }

    @Override
    public final void putProperty(Object key, Object value) {
        if (properties == null) {
            properties = new HashMap<Object, Object>();
        }
        if (value == null) {
            properties.remove(key);
        } else {
            properties.put(key, value);
        }
    }

    // ------------------------------------------------------------ listeners

    @Override
    public void addDocumentListener(DocumentListener listener) {
        listenerList.add(DocumentListener.class, listener);
    }

    @Override
    public void removeDocumentListener(DocumentListener listener) {
        listenerList.remove(DocumentListener.class, listener);
    }

    @Override
    public void addUndoableEditListener(UndoableEditListener listener) {
        listenerList.add(UndoableEditListener.class, listener);
    }

    @Override
    public void removeUndoableEditListener(UndoableEditListener listener) {
        listenerList.remove(UndoableEditListener.class, listener);
    }

    public UndoableEditListener[] getUndoableEditListeners() {
        return listenerList.getListeners(UndoableEditListener.class);
    }

    protected void fireUndoableEditUpdate(UndoableEditEvent e) {
        UndoableEditListener[] ls = listenerList.getListeners(UndoableEditListener.class);
        for (int i = ls.length - 1; i >= 0; i--) {
            ls[i].undoableEditHappened(e);
        }
    }

    public DocumentListener[] getDocumentListeners() {
        return listenerList.getListeners(DocumentListener.class);
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }

    protected void fireInsertUpdate(DocumentEvent e) {
        DocumentListener[] ls = getDocumentListeners();
        for (int i = ls.length - 1; i >= 0; i--) {
            ls[i].insertUpdate(e);
        }
    }

    protected void fireRemoveUpdate(DocumentEvent e) {
        DocumentListener[] ls = getDocumentListeners();
        for (int i = ls.length - 1; i >= 0; i--) {
            ls[i].removeUpdate(e);
        }
    }

    protected void fireChangedUpdate(DocumentEvent e) {
        DocumentListener[] ls = getDocumentListeners();
        for (int i = ls.length - 1; i >= 0; i--) {
            ls[i].changedUpdate(e);
        }
    }

    /// The event a document sends for one change. It is also the edit
    /// that takes the change back: undoing an insertion removes the text
    /// again and undoing a removal puts it back, and the document
    /// listeners hear of either as an ordinary change.
    public class DefaultDocumentEvent extends CompoundEdit implements DocumentEvent {

        private final int offset;
        private final int length;
        private final DocumentEvent.EventType type;
        /// The characters that went in or came out.
        String cn1Text = "";

        public DefaultDocumentEvent(int offs, int len, DocumentEvent.EventType type) {
            offset = offs;
            length = len;
            this.type = type;
        }

        @Override
        public int getOffset() {
            return offset;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public Document getDocument() {
            return AbstractDocument.this;
        }

        @Override
        public DocumentEvent.EventType getType() {
            return type;
        }

        @Override
        public DocumentEvent.ElementChange getChange(Element elem) {
            return null;
        }

        @Override
        public void undo() throws CannotUndoException {
            super.undo();
            if (type == DocumentEvent.EventType.INSERT) {
                cn1Replay(false, offset, cn1Text);
            } else if (type == DocumentEvent.EventType.REMOVE) {
                cn1Replay(true, offset, cn1Text);
            }
        }

        @Override
        public void redo() throws CannotRedoException {
            super.redo();
            if (type == DocumentEvent.EventType.INSERT) {
                cn1Replay(true, offset, cn1Text);
            } else if (type == DocumentEvent.EventType.REMOVE) {
                cn1Replay(false, offset, cn1Text);
            }
        }

        @Override
        public boolean isSignificant() {
            return true;
        }

        @Override
        public String getPresentationName() {
            if (type == DocumentEvent.EventType.INSERT) {
                return "addition";
            }
            if (type == DocumentEvent.EventType.REMOVE) {
                return "deletion";
            }
            return "style change";
        }

        @Override
        public String getUndoPresentationName() {
            return UndoName + " " + getPresentationName();
        }

        @Override
        public String getRedoPresentationName() {
            return RedoName + " " + getPresentationName();
        }

        @Override
        public String toString() {
            return type + " offset=" + offset + " length=" + length;
        }
    }
}
