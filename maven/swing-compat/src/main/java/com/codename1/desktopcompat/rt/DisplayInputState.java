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
package com.codename1.desktopcompat.rt;

import com.codename1.desktopcompat.java.awt.event.InputEvent;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.ui.Display;
import com.codename1.ui.events.PointerEvent;

/// The input state the display reports.
///
/// The modifiers are the union of what the pointer event being delivered
/// carries and what `Display.isShiftKeyDown()` and its siblings answer.
/// Both are filled in by the desktop ports only: on a phone, and on any
/// port whose implementation does not track the keyboard, no modifier is
/// ever reported, for key events as for mouse events.
final class DisplayInputState implements InputState {

    @Override
    public int modifiers() {
        if (!Display.isInitialized()) {
            return 0;
        }
        Display d = Display.getInstance();
        PointerEvent pe = d.getCurrentPointerEvent();
        int m = 0;
        if (d.isShiftKeyDown() || (pe != null && pe.isShiftDown())) {
            m |= InputEvent.SHIFT_DOWN_MASK;
        }
        if (d.isControlKeyDown() || (pe != null && pe.isControlDown())) {
            m |= InputEvent.CTRL_DOWN_MASK;
        }
        if (d.isAltKeyDown() || (pe != null && pe.isAltDown())) {
            m |= InputEvent.ALT_DOWN_MASK;
        }
        if (d.isMetaKeyDown() || (pe != null && pe.isMetaDown())) {
            m |= InputEvent.META_DOWN_MASK;
        }
        if (d.isAltGraphKeyDown()) {
            m |= InputEvent.ALT_GRAPH_DOWN_MASK;
        }
        return m;
    }

    @Override
    public int button() {
        if (!Display.isInitialized()) {
            return MouseEvent.BUTTON1;
        }
        PointerEvent pe = Display.getInstance().getCurrentPointerEvent();
        if (pe == null) {
            return MouseEvent.BUTTON1;
        }
        switch (pe.getButton()) {
            case PointerEvent.BUTTON_SECONDARY:
                return MouseEvent.BUTTON3;
            case PointerEvent.BUTTON_MIDDLE:
                return MouseEvent.BUTTON2;
            default:
                return MouseEvent.BUTTON1;
        }
    }

    @Override
    public boolean touch() {
        if (!Display.isInitialized()) {
            return false;
        }
        PointerEvent pe = Display.getInstance().getCurrentPointerEvent();
        return pe != null && !pe.isMouse();
    }
}
