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

import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;

/// The insertion point of a text component, and with its mark the
/// selection.
public interface Caret {

    void install(JTextComponent c);

    void deinstall(JTextComponent c);

    void paint(Graphics g);

    void addChangeListener(ChangeListener l);

    void removeChangeListener(ChangeListener l);

    boolean isVisible();

    void setVisible(boolean v);

    boolean isSelectionVisible();

    void setSelectionVisible(boolean v);

    void setMagicCaretPosition(Point p);

    Point getMagicCaretPosition();

    void setBlinkRate(int rate);

    int getBlinkRate();

    int getDot();

    int getMark();

    void setDot(int dot);

    void moveDot(int dot);
}
