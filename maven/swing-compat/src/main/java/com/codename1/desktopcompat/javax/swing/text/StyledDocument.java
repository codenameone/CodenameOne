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
import com.codename1.desktopcompat.java.awt.Font;

/// A document whose text has attributes: by stretch of characters, and
/// by paragraph.
public interface StyledDocument extends Document {

    Style addStyle(String nm, Style parent);

    void removeStyle(String nm);

    Style getStyle(String nm);

    /// Gives the characters from `offset` on the attributes in `s`, in
    /// place of the ones they have with `replace` and added to them
    /// without.
    void setCharacterAttributes(int offset, int length, AttributeSet s, boolean replace);

    /// As [#setCharacterAttributes], for the paragraphs the range touches.
    void setParagraphAttributes(int offset, int length, AttributeSet s, boolean replace);

    /// Sets the style the paragraph at `pos` resolves its attributes in.
    void setLogicalStyle(int pos, Style s);

    Style getLogicalStyle(int p);

    Element getParagraphElement(int pos);

    /// The stretch of characters with the same attributes that `pos` is
    /// in.
    Element getCharacterElement(int pos);

    Color getForeground(AttributeSet attr);

    Color getBackground(AttributeSet attr);

    Font getFont(AttributeSet attr);
}
