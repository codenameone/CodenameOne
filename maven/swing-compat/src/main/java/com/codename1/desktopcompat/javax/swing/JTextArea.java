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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.javax.swing.text.BadLocationException;
import com.codename1.desktopcompat.javax.swing.text.Document;
import com.codename1.desktopcompat.javax.swing.text.Element;
import com.codename1.desktopcompat.javax.swing.text.JTextComponent;
import com.codename1.desktopcompat.javax.swing.text.PlainDocument;
import com.codename1.desktopcompat.rt.TextAreaPeer;

/// Several lines of plain text, shown and edited by a Codename One text
/// area over a [PlainDocument].
///
/// The area grows with its text; put it in a [JScrollPane] to scroll it.
/// Moving the caret brings its line into view, so the usual
/// `setCaretPosition(getDocument().getLength())` after `append` follows a
/// log to its end. The Codename One widget always wraps lines at its
/// width: `setLineWrap(false)` is recorded but long lines still wrap, and
/// wrapping is at word boundaries whatever `setWrapStyleWord` says. Tab
/// size is recorded only.
public class JTextArea extends JTextComponent {

    private int rows;
    private int columns;
    private boolean wrap;
    private boolean word;
    private boolean revealPending;

    public JTextArea() {
        this(null, null, 0, 0);
    }

    public JTextArea(String text) {
        this(null, text, 0, 0);
    }

    public JTextArea(int rows, int columns) {
        this(null, null, rows, columns);
    }

    public JTextArea(String text, int rows, int columns) {
        this(null, text, rows, columns);
    }

    public JTextArea(Document doc) {
        this(doc, null, 0, 0);
    }

    public JTextArea(Document doc, String text, int rows, int columns) {
        if (rows < 0) {
            throw new IllegalArgumentException("rows: " + rows);
        }
        if (columns < 0) {
            throw new IllegalArgumentException("columns: " + columns);
        }
        this.rows = rows;
        this.columns = columns;
        if (doc == null) {
            doc = createDefaultModel();
        }
        setDocument(doc);
        if (text != null) {
            setText(text);
            select(0, 0);
        }
    }

    protected Document createDefaultModel() {
        return new PlainDocument();
    }

    @Override
    protected com.codename1.ui.Component cn1CreatePeer() {
        TextAreaPeer p = new TextAreaPeer(this);
        if (rows > 0) {
            p.setRows(rows);
        }
        if (columns > 0) {
            p.setColumns(columns);
        }
        return p;
    }

    @Override
    protected boolean cn1GrowsWithText() {
        return true;
    }

    // ------------------------------------------------------------ caret

    /// Asks for the caret's line to be shown in the viewport this area is
    /// in. A viewport that is waiting for a layout is laid out first, and
    /// that layout shows the caret within the size the text has now; one
    /// that is not is scrolled at once.
    @Override
    protected void cn1CaretMoved(int dot) {
        java.lang.Object p = getParent();
        if (!(p instanceof JViewport)) {
            return;
        }
        revealPending = true;
        JViewport viewport = (JViewport) p;
        if (viewport.getWidth() > 0 && viewport.getHeight() > 0) {
            // Does nothing when the viewport is laid out already, or is in
            // the middle of the layout this was called from.
            viewport.validate();
        }
        if (revealPending && cn1RevealCaret() && viewport.isValid()) {
            revealPending = false;
        }
    }

    /// Scrolls the viewport so that the caret's line shows, if that was
    /// asked for and the area has a size; answers whether it did. Called
    /// by the viewport once it gave the area its new size. The line's
    /// place is estimated from the hard line breaks; the end of the text
    /// is exact.
    public boolean cn1RevealCaret() {
        if (!revealPending || !(getParent() instanceof JViewport) || getHeight() <= 0) {
            return false;
        }
        Document doc = getDocument();
        int lines = getLineCount();
        int dot = getCaretPosition();
        int rowHeight = Math.max(1, getHeight() / Math.max(1, lines));
        int y;
        if (doc != null && dot >= doc.getLength()) {
            y = Math.max(0, getHeight() - rowHeight);
        } else {
            y = doc == null ? 0 : doc.getDefaultRootElement().getElementIndex(dot) * rowHeight;
        }
        JViewport.cn1ScrollRectToVisible(this, new Rectangle(0, y, 1, rowHeight));
        return true;
    }

    /// Forgets the pending request to show the caret, after a layout
    /// honoured it.
    public void cn1RevealDone() {
        revealPending = false;
    }

    // ------------------------------------------------------------ text

    public void insert(String str, int pos) {
        Document doc = getDocument();
        if (doc != null) {
            try {
                doc.insertString(pos, str, null);
            } catch (BadLocationException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    public void append(String str) {
        Document doc = getDocument();
        if (doc != null) {
            try {
                doc.insertString(doc.getLength(), str, null);
            } catch (BadLocationException e) {
                // The end of the document is always a valid position.
                repaint();
            }
        }
    }

    public void replaceRange(String str, int start, int end) {
        if (end < start) {
            throw new IllegalArgumentException("end before start");
        }
        Document doc = getDocument();
        if (doc != null) {
            try {
                if (doc instanceof com.codename1.desktopcompat.javax.swing.text.AbstractDocument) {
                    ((com.codename1.desktopcompat.javax.swing.text.AbstractDocument) doc).replace(start,
                            end - start, str, null);
                } else {
                    doc.remove(start, end - start);
                    doc.insertString(start, str, null);
                }
            } catch (BadLocationException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    public int getLineCount() {
        Document doc = getDocument();
        return doc == null ? 0 : doc.getDefaultRootElement().getElementCount();
    }

    public int getLineOfOffset(int offset) throws BadLocationException {
        Document doc = getDocument();
        if (offset < 0) {
            throw new BadLocationException("Can't translate offset to line", -1);
        } else if (doc == null || offset > doc.getLength()) {
            throw new BadLocationException("Can't translate offset to line", doc == null ? 0 : doc.getLength() + 1);
        }
        return doc.getDefaultRootElement().getElementIndex(offset);
    }

    public int getLineStartOffset(int line) throws BadLocationException {
        Element e = line(line);
        return e.getStartOffset();
    }

    public int getLineEndOffset(int line) throws BadLocationException {
        Element e = line(line);
        int end = e.getEndOffset();
        return line == getLineCount() - 1 ? end - 1 : end;
    }

    private Element line(int line) throws BadLocationException {
        int count = getLineCount();
        if (line < 0) {
            throw new BadLocationException("Negative line", -1);
        } else if (line >= count) {
            throw new BadLocationException("No such line", getDocument() == null ? 0 : getDocument().getLength() + 1);
        }
        return getDocument().getDefaultRootElement().getElement(line);
    }

    // ------------------------------------------------------------ shape

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        if (rows < 0) {
            throw new IllegalArgumentException("rows less than zero.");
        }
        if (rows != this.rows) {
            this.rows = rows;
            com.codename1.ui.Component p = cn1PeerOrNull();
            if (p instanceof com.codename1.ui.TextArea && rows > 0) {
                ((com.codename1.ui.TextArea) p).setRows(rows);
            }
            revalidate();
        }
    }

    public int getColumns() {
        return columns;
    }

    public void setColumns(int columns) {
        if (columns < 0) {
            throw new IllegalArgumentException("columns less than zero.");
        }
        if (columns != this.columns) {
            this.columns = columns;
            com.codename1.ui.Component p = cn1PeerOrNull();
            if (p instanceof com.codename1.ui.TextArea && columns > 0) {
                ((com.codename1.ui.TextArea) p).setColumns(columns);
            }
            revalidate();
        }
    }

    protected int getRowHeight() {
        Font f = getFont();
        return f == null ? 0 : getFontMetrics(f).getHeight();
    }

    protected int getColumnWidth() {
        Font f = getFont();
        return f == null ? 0 : getFontMetrics(f).charWidth('m');
    }

    public boolean getLineWrap() {
        return wrap;
    }

    public void setLineWrap(boolean wrap) {
        boolean old = this.wrap;
        this.wrap = wrap;
        firePropertyChange("lineWrap", old, wrap);
    }

    public boolean getWrapStyleWord() {
        return word;
    }

    public void setWrapStyleWord(boolean word) {
        boolean old = this.word;
        this.word = word;
        firePropertyChange("wrapStyleWord", old, word);
    }

    public int getTabSize() {
        Document doc = getDocument();
        Object size = doc == null ? null : doc.getProperty(PlainDocument.tabSizeAttribute);
        return size instanceof Integer ? ((Integer) size).intValue() : 8;
    }

    public void setTabSize(int size) {
        Document doc = getDocument();
        if (doc != null) {
            int old = getTabSize();
            doc.putProperty(PlainDocument.tabSizeAttribute, Integer.valueOf(size));
            firePropertyChange("tabSize", old, size);
        }
    }

    // ------------------------------------------------------------ scrolling

    /// The widget wraps at its width, so in a viewport the area is always
    /// as wide as the viewport and scrolls only vertically.
    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        Dimension d = getPreferredSize();
        if (rows > 0 && getRowHeight() > 0) {
            d.height = rows * getRowHeight();
        }
        return d;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        int h = getRowHeight();
        return orientation == SwingConstants.VERTICAL && h > 0 ? h
                : super.getScrollableUnitIncrement(visibleRect, orientation, direction);
    }

    @Override
    protected String paramString() {
        return super.paramString() + ",columns=" + columns + ",rows=" + rows;
    }
}
