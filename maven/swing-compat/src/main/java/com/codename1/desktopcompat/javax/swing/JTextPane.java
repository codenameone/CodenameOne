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

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.javax.swing.event.DocumentEvent;
import com.codename1.desktopcompat.javax.swing.event.DocumentListener;
import com.codename1.desktopcompat.javax.swing.text.AbstractDocument;
import com.codename1.desktopcompat.javax.swing.text.AttributeSet;
import com.codename1.desktopcompat.javax.swing.text.BadLocationException;
import com.codename1.desktopcompat.javax.swing.text.DefaultStyledDocument;
import com.codename1.desktopcompat.javax.swing.text.Document;
import com.codename1.desktopcompat.javax.swing.text.MutableAttributeSet;
import com.codename1.desktopcompat.javax.swing.text.SimpleAttributeSet;
import com.codename1.desktopcompat.javax.swing.text.Style;
import com.codename1.desktopcompat.javax.swing.text.StyledDocument;
import com.codename1.desktopcompat.rt.StyledText;

/// An editor pane for styled text: its document is a [StyledDocument],
/// and the pane draws every run of it in the color, weight, slant, family
/// and size its attributes ask for, underlined or struck through, on the
/// background they name, in paragraphs aligned as theirs ask.
///
/// ## What differs from the desktop
///
///  - While the document has no attribute that changes how text looks,
///    the pane is the plain text area [JEditorPane] is, edited by the
///    platform's widget. Once it has one the pane draws the text itself
///    and the user can no longer type in it or select in it, whatever
///    `setEditable` says: the platform's widget edits text of one look.
///    The application changes the text as before, through the document
///    and through the pane.
///  - A paragraph asked to be justified is aligned left.
///  - Icons and components cannot be put in the text.
public class JTextPane extends JEditorPane {

    private final MutableAttributeSet input = new SimpleAttributeSet();
    private final Watch watch = new Watch();
    private StyledText layout;
    private int layoutRoom;
    private Font layoutFont;
    private Color layoutColor;
    /// Whether the document asks to be drawn styled; `null` until it is
    /// looked at again after a change.
    private Boolean styled;

    public JTextPane() {
        setDocument(new DefaultStyledDocument());
    }

    public JTextPane(StyledDocument doc) {
        setDocument(doc);
    }

    private final class Watch implements DocumentListener {

        @Override
        public void insertUpdate(DocumentEvent e) {
            changed();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            changed();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            changed();
        }

        private void changed() {
            boolean was = styled != null && styled.booleanValue();
            styled = null;
            layout = null;
            if (cn1Styled() != was) {
                cn1ApplyMode();
            }
            revalidate();
            repaint();
        }
    }

    /// Sets the document, which has to be a [StyledDocument].
    @Override
    public void setDocument(Document doc) {
        // The constructor of JEditorPane sets a plain one first, before
        // this class has its fields.
        if (watch != null && !(doc instanceof StyledDocument)) {
            throw new IllegalArgumentException("Model must be StyledDocument");
        }
        Document old = getDocument();
        if (old != null && watch != null) {
            old.removeDocumentListener(watch);
        }
        super.setDocument(doc);
        // Null while the constructor of JEditorPane sets its own.
        if (watch != null) {
            doc.addDocumentListener(watch);
            styled = null;
            layout = null;
            cn1ApplyMode();
        }
    }

    public void setStyledDocument(StyledDocument doc) {
        setDocument(doc);
    }

    public StyledDocument getStyledDocument() {
        Document doc = getDocument();
        return doc instanceof StyledDocument ? (StyledDocument) doc : null;
    }

    public Style addStyle(String nm, Style parent) {
        return getStyledDocument().addStyle(nm, parent);
    }

    public void removeStyle(String nm) {
        getStyledDocument().removeStyle(nm);
    }

    public Style getStyle(String nm) {
        return getStyledDocument().getStyle(nm);
    }

    public void setLogicalStyle(Style s) {
        getStyledDocument().setLogicalStyle(getCaretPosition(), s);
    }

    public Style getLogicalStyle() {
        return getStyledDocument().getLogicalStyle(getCaretPosition());
    }

    /// The attributes of the character at the caret.
    public AttributeSet getCharacterAttributes() {
        StyledDocument doc = getStyledDocument();
        return doc.getCharacterElement(getCaretPosition()).getAttributes();
    }

    /// Gives the selection the attributes; with nothing selected they
    /// become the attributes text is inserted with.
    public void setCharacterAttributes(AttributeSet attr, boolean replace) {
        int p0 = getSelectionStart();
        int p1 = getSelectionEnd();
        if (p0 != p1) {
            getStyledDocument().setCharacterAttributes(p0, p1 - p0, attr, replace);
        } else {
            if (replace) {
                input.removeAttributes(input);
            }
            input.addAttributes(attr);
        }
    }

    public AttributeSet getParagraphAttributes() {
        return getStyledDocument().getParagraphElement(getCaretPosition()).getAttributes();
    }

    /// Gives the paragraphs the selection touches the attributes.
    public void setParagraphAttributes(AttributeSet attr, boolean replace) {
        int p0 = getSelectionStart();
        int p1 = getSelectionEnd();
        getStyledDocument().setParagraphAttributes(p0, p1 - p0, attr, replace);
    }

    /// The attributes text is inserted with by [#replaceSelection].
    public MutableAttributeSet getInputAttributes() {
        return input;
    }

    /// Puts `content` in place of the selection, with the input
    /// attributes.
    @Override
    public void replaceSelection(String content) {
        Document doc = getDocument();
        if (doc == null || !isEditable()) {
            return;
        }
        int p0 = getSelectionStart();
        int p1 = getSelectionEnd();
        AttributeSet a = input.getAttributeCount() == 0 ? null : input.copyAttributes();
        try {
            if (doc instanceof AbstractDocument) {
                ((AbstractDocument) doc).replace(p0, p1 - p0, content, a);
            } else {
                if (p0 != p1) {
                    doc.remove(p0, p1 - p0);
                }
                if (content != null && content.length() > 0) {
                    doc.insertString(p0, content, a);
                }
            }
        } catch (BadLocationException e) {
            // The selection is always inside the document.
            repaint();
        }
    }

    // ------------------------------------------------------------ drawing

    @Override
    boolean cn1Styled() {
        if (styled == null) {
            StyledDocument doc = getStyledDocument();
            styled = Boolean.valueOf(doc != null && StyledText.styled(doc));
        }
        return styled.booleanValue();
    }

    private Color cn1Ink() {
        com.codename1.ui.Component p = cn1PeerOrNull();
        return isForegroundSet() || p == null ? getForeground() : new Color(p.getStyle().getFgColor() & 0xffffff);
    }

    private static boolean same(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    /// The lines of the document at the width the pane has.
    private StyledText cn1Layout() {
        Insets in = cn1TextInsets();
        int room = Math.max(0, getWidth() - in.left - in.right);
        Font f = getFont();
        Color ink = cn1Ink();
        if (layout == null || room != layoutRoom || !same(f, layoutFont) || !same(ink, layoutColor)) {
            layout = new StyledText(getStyledDocument(), f, ink, room);
            layoutRoom = room;
            layoutFont = f;
            layoutColor = ink;
        }
        return layout;
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet() || !cn1Styled()) {
            return super.getPreferredSize();
        }
        Insets in = cn1TextInsets();
        StyledText whole = new StyledText(getStyledDocument(), getFont(), cn1Ink(), 0);
        int h = getWidth() - in.left - in.right > 0 ? cn1Layout().getHeight() : whole.getHeight();
        return new Dimension(whole.getWidth() + in.left + in.right, h + in.top + in.bottom);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!cn1Styled()) {
            super.paintComponent(g);
            return;
        }
        if (isOpaque()) {
            Color bg = getBackground();
            if (bg != null) {
                g.setColor(bg);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }
        Insets in = cn1TextInsets();
        cn1Layout().paint(g, in.left, in.top);
    }

    @Override
    protected int cn1ViewToModel(int x, int y) {
        if (!cn1Styled()) {
            return super.cn1ViewToModel(x, y);
        }
        if (getWidth() <= 0 || getHeight() <= 0) {
            return -1;
        }
        Insets in = cn1TextInsets();
        return cn1Layout().offsetAt(x - in.left, y - in.top);
    }

    @Override
    protected Rectangle cn1ModelToView(int pos) {
        if (!cn1Styled()) {
            return super.cn1ModelToView(pos);
        }
        if (getWidth() <= 0 || getHeight() <= 0) {
            return null;
        }
        Insets in = cn1TextInsets();
        Rectangle r = cn1Layout().caretAt(pos);
        r.x += in.left;
        r.y += in.top;
        return r;
    }
}
