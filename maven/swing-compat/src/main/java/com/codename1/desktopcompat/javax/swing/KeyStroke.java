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

import com.codename1.desktopcompat.java.awt.AWTKeyStroke;
import com.codename1.desktopcompat.java.awt.event.KeyEvent;

/// A key action used as the key of an input map and as the accelerator of
/// a menu item. See `AWTKeyStroke` for the string form and for how the
/// modifiers are kept.
public class KeyStroke extends AWTKeyStroke {

    private KeyStroke(char keyChar, int keyCode, int modifiers, boolean onKeyRelease) {
        super(keyChar, keyCode, modifiers, onKeyRelease);
    }

    public static KeyStroke getKeyStroke(char keyChar) {
        return new KeyStroke(keyChar, KeyEvent.VK_UNDEFINED, 0, false);
    }

    public static KeyStroke getKeyStroke(Character keyChar, int modifiers) {
        if (keyChar == null) {
            throw new IllegalArgumentException("keyChar cannot be null");
        }
        return new KeyStroke(keyChar.charValue(), KeyEvent.VK_UNDEFINED, modifiers, false);
    }

    public static KeyStroke getKeyStroke(int keyCode, int modifiers, boolean onKeyRelease) {
        return new KeyStroke(KeyEvent.CHAR_UNDEFINED, keyCode, modifiers, onKeyRelease);
    }

    public static KeyStroke getKeyStroke(int keyCode, int modifiers) {
        return new KeyStroke(KeyEvent.CHAR_UNDEFINED, keyCode, modifiers, false);
    }

    /// The stroke a key event is, or `null` for an event of another kind.
    public static KeyStroke getKeyStrokeForEvent(KeyEvent anEvent) {
        AWTKeyStroke k = AWTKeyStroke.getAWTKeyStrokeForEvent(anEvent);
        return k == null ? null : new KeyStroke(k.getKeyChar(), k.getKeyCode(), k.getModifiers(), k.isOnKeyRelease());
    }

    /// Parses a stroke; unlike `getAWTKeyStroke(String)` it answers
    /// `null` for a string that is null, empty or not a stroke.
    public static KeyStroke getKeyStroke(String s) {
        if (s == null || s.length() == 0) {
            return null;
        }
        AWTKeyStroke k;
        try {
            k = AWTKeyStroke.getAWTKeyStroke(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return new KeyStroke(k.getKeyChar(), k.getKeyCode(), k.getModifiers(), k.isOnKeyRelease());
    }
}
