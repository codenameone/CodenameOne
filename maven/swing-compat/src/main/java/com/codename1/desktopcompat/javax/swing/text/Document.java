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

import com.codename1.desktopcompat.javax.swing.event.DocumentListener;

/// The text a text component shows and edits.
///
/// Undoable edits and rendering are not part of this layer, so the methods
/// for them are absent.
public interface Document {

    String StreamDescriptionProperty = "stream";

    String TitleProperty = "title";

    int getLength();

    void addDocumentListener(DocumentListener listener);

    void removeDocumentListener(DocumentListener listener);

    Object getProperty(Object key);

    void putProperty(Object key, Object value);

    void remove(int offs, int len) throws BadLocationException;

    void insertString(int offset, String str, AttributeSet a) throws BadLocationException;

    String getText(int offset, int length) throws BadLocationException;

    Position getStartPosition();

    Position getEndPosition();

    Position createPosition(int offs) throws BadLocationException;

    Element[] getRootElements();

    Element getDefaultRootElement();
}
