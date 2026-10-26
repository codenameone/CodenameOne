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
package com.codename1.desktopcompat.javax.swing.text.html;

import com.codename1.desktopcompat.javax.swing.event.DocumentListener;
import com.codename1.desktopcompat.javax.swing.event.UndoableEditListener;
import com.codename1.desktopcompat.javax.swing.text.AttributeSet;
import com.codename1.desktopcompat.javax.swing.text.BadLocationException;
import com.codename1.desktopcompat.javax.swing.text.Document;
import com.codename1.desktopcompat.javax.swing.text.Element;
import com.codename1.desktopcompat.javax.swing.text.PlainDocument;
import com.codename1.desktopcompat.javax.swing.text.Position;
import com.codename1.desktopcompat.javax.swing.text.Segment;

/// The type of an HTML document, for code that tests for it. This layer
/// builds none -- see the package description -- so there is no public
/// constructor, no pane hands one out, and the test is false.
public class HTMLDocument implements Document {

    /// The text, should the layer ever make one of these.
    private final PlainDocument text = new PlainDocument();

    HTMLDocument() {
    }

    /// Shows the target of a link in the frame it names. There are no
    /// frames in a document here, so there is no frame to change.
    public void processHTMLFrameHyperlinkEvent(HTMLFrameHyperlinkEvent e) {
        if (e == null) {
            throw new NullPointerException();
        }
    }

    @Override
    public int getLength() {
        return text.getLength();
    }

    @Override
    public void addDocumentListener(DocumentListener listener) {
        text.addDocumentListener(listener);
    }

    @Override
    public void removeDocumentListener(DocumentListener listener) {
        text.removeDocumentListener(listener);
    }

    @Override
    public void addUndoableEditListener(UndoableEditListener listener) {
        text.addUndoableEditListener(listener);
    }

    @Override
    public void removeUndoableEditListener(UndoableEditListener listener) {
        text.removeUndoableEditListener(listener);
    }

    @Override
    public Object getProperty(Object key) {
        return text.getProperty(key);
    }

    @Override
    public void putProperty(Object key, Object value) {
        text.putProperty(key, value);
    }

    @Override
    public void remove(int offs, int len) throws BadLocationException {
        text.remove(offs, len);
    }

    @Override
    public void insertString(int offset, String str, AttributeSet a) throws BadLocationException {
        text.insertString(offset, str, a);
    }

    @Override
    public String getText(int offset, int length) throws BadLocationException {
        return text.getText(offset, length);
    }

    @Override
    public void getText(int offset, int length, Segment txt) throws BadLocationException {
        text.getText(offset, length, txt);
    }

    @Override
    public Position getStartPosition() {
        return text.getStartPosition();
    }

    @Override
    public Position getEndPosition() {
        return text.getEndPosition();
    }

    @Override
    public Position createPosition(int offs) throws BadLocationException {
        return text.createPosition(offs);
    }

    @Override
    public Element[] getRootElements() {
        return text.getRootElements();
    }

    @Override
    public Element getDefaultRootElement() {
        return text.getDefaultRootElement();
    }
}
