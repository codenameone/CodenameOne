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
package com.codename1.desktopcompat.java.awt;

import com.codename1.desktopcompat.java.awt.event.InputEvent;
import com.codename1.desktopcompat.java.awt.event.KeyEvent;
import com.codename1.desktopcompat.rt.KeyMap;

/// A key action: a key pressed or released with a set of modifiers, or a
/// character typed.
///
/// Like the JDK's, a stroke keeps its modifiers in both encodings, so one
/// made with `CTRL_MASK` equals one made with `CTRL_DOWN_MASK`. Strokes are
/// not cached: compare them with `equals`. Strokes are made by the static
/// factory methods; `registerSubclass` and serialization are absent.
public class AWTKeyStroke implements java.io.Serializable {

    private static final int ALL = InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK
            | InputEvent.META_DOWN_MASK | InputEvent.ALT_DOWN_MASK | InputEvent.ALT_GRAPH_DOWN_MASK
            | InputEvent.BUTTON1_DOWN_MASK | InputEvent.BUTTON2_DOWN_MASK | InputEvent.BUTTON3_DOWN_MASK
            | InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK | InputEvent.META_MASK | InputEvent.ALT_MASK
            | InputEvent.ALT_GRAPH_MASK | InputEvent.BUTTON1_MASK;

    private final char keyChar;
    private final int keyCode;
    private final int modifiers;
    private final boolean onKeyRelease;

    protected AWTKeyStroke() {
        this(KeyEvent.CHAR_UNDEFINED, KeyEvent.VK_UNDEFINED, 0, false);
    }

    protected AWTKeyStroke(char keyChar, int keyCode, int modifiers, boolean onKeyRelease) {
        this.keyChar = keyChar;
        this.keyCode = keyCode;
        this.modifiers = cn1Normalize(modifiers);
        this.onKeyRelease = onKeyRelease;
    }

    /// Completes a set of modifiers so that it carries both the old and
    /// the extended mask of every modifier in it.
    static int cn1Normalize(int m) {
        int r = m & ALL;
        if ((r & (InputEvent.SHIFT_MASK | InputEvent.SHIFT_DOWN_MASK)) != 0) {
            r |= InputEvent.SHIFT_MASK | InputEvent.SHIFT_DOWN_MASK;
        }
        if ((r & (InputEvent.CTRL_MASK | InputEvent.CTRL_DOWN_MASK)) != 0) {
            r |= InputEvent.CTRL_MASK | InputEvent.CTRL_DOWN_MASK;
        }
        if ((r & (InputEvent.META_MASK | InputEvent.META_DOWN_MASK)) != 0) {
            r |= InputEvent.META_MASK | InputEvent.META_DOWN_MASK;
        }
        if ((r & (InputEvent.ALT_MASK | InputEvent.ALT_DOWN_MASK)) != 0) {
            r |= InputEvent.ALT_MASK | InputEvent.ALT_DOWN_MASK;
        }
        if ((r & (InputEvent.ALT_GRAPH_MASK | InputEvent.ALT_GRAPH_DOWN_MASK)) != 0) {
            r |= InputEvent.ALT_GRAPH_MASK | InputEvent.ALT_GRAPH_DOWN_MASK;
        }
        if ((r & (InputEvent.BUTTON1_MASK | InputEvent.BUTTON1_DOWN_MASK)) != 0) {
            r |= InputEvent.BUTTON1_MASK | InputEvent.BUTTON1_DOWN_MASK;
        }
        return r;
    }

    public static AWTKeyStroke getAWTKeyStroke(char keyChar) {
        return new AWTKeyStroke(keyChar, KeyEvent.VK_UNDEFINED, 0, false);
    }

    public static AWTKeyStroke getAWTKeyStroke(Character keyChar, int modifiers) {
        if (keyChar == null) {
            throw new IllegalArgumentException("keyChar cannot be null");
        }
        return new AWTKeyStroke(keyChar.charValue(), KeyEvent.VK_UNDEFINED, modifiers, false);
    }

    public static AWTKeyStroke getAWTKeyStroke(int keyCode, int modifiers, boolean onKeyRelease) {
        return new AWTKeyStroke(KeyEvent.CHAR_UNDEFINED, keyCode, modifiers, onKeyRelease);
    }

    public static AWTKeyStroke getAWTKeyStroke(int keyCode, int modifiers) {
        return new AWTKeyStroke(KeyEvent.CHAR_UNDEFINED, keyCode, modifiers, false);
    }

    public static AWTKeyStroke getAWTKeyStrokeForEvent(KeyEvent anEvent) {
        int[] p = cn1ForEvent(anEvent);
        return p == null ? null : new AWTKeyStroke((char) p[0], p[1], p[2], p[3] != 0);
    }

    /// The parts of the stroke a key event is: character, key code,
    /// modifiers and whether it is a release; `null` for another event.
    static int[] cn1ForEvent(KeyEvent e) {
        int mods = e.getModifiers() | e.getModifiersEx();
        switch (e.getID()) {
            case KeyEvent.KEY_PRESSED:
                return new int[]{KeyEvent.CHAR_UNDEFINED, e.getKeyCode(), mods, 0};
            case KeyEvent.KEY_RELEASED:
                return new int[]{KeyEvent.CHAR_UNDEFINED, e.getKeyCode(), mods, 1};
            case KeyEvent.KEY_TYPED:
                return new int[]{e.getKeyChar(), KeyEvent.VK_UNDEFINED, mods, 0};
            default:
                return null;
        }
    }

    /// Parses `<modifiers>* (<typedID> | <pressedReleasedID>)`: modifiers
    /// are `shift`, `control`, `ctrl`, `meta`, `alt`, `altGraph`, `button1`
    /// to `button3`; then `typed <char>`, or an optional `pressed` or
    /// `released` and the name of a `VK_` constant without the prefix.
    public static AWTKeyStroke getAWTKeyStroke(String s) {
        int[] p = cn1Parse(s);
        return new AWTKeyStroke((char) p[0], p[1], p[2], p[3] != 0);
    }

    /// The parts of the stroke a string describes, as [#cn1ForEvent]
    /// answers them.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if the string is null or not a stroke
    static int[] cn1Parse(String s) {
        if (s == null) {
            throw new IllegalArgumentException("String cannot be null");
        }
        int mask = 0;
        boolean released = false;
        boolean typed = false;
        boolean pressed = false;
        int n = s.length();
        int i = 0;
        while (i < n) {
            while (i < n && s.charAt(i) == ' ') {
                i++;
            }
            int start = i;
            while (i < n && s.charAt(i) != ' ') {
                i++;
            }
            if (start == i) {
                break;
            }
            String token = s.substring(start, i);
            boolean last = true;
            for (int j = i; j < n; j++) {
                if (s.charAt(j) != ' ') {
                    last = false;
                    break;
                }
            }
            if (typed) {
                if (token.length() != 1 || !last) {
                    throw new IllegalArgumentException("String formatted incorrectly");
                }
                return new int[]{token.charAt(0), KeyEvent.VK_UNDEFINED, mask, 0};
            }
            if (pressed || released || last) {
                if (!last) {
                    throw new IllegalArgumentException("String formatted incorrectly");
                }
                int code = KeyMap.codeOf(token);
                if (code == KeyEvent.VK_UNDEFINED) {
                    throw new IllegalArgumentException("String formatted incorrectly");
                }
                return new int[]{KeyEvent.CHAR_UNDEFINED, code, mask, released ? 1 : 0};
            }
            if ("released".equals(token)) {
                released = true;
            } else if ("pressed".equals(token)) {
                pressed = true;
            } else if ("typed".equals(token)) {
                typed = true;
            } else if ("shift".equals(token)) {
                mask |= InputEvent.SHIFT_DOWN_MASK;
            } else if ("control".equals(token) || "ctrl".equals(token)) {
                mask |= InputEvent.CTRL_DOWN_MASK;
            } else if ("meta".equals(token)) {
                mask |= InputEvent.META_DOWN_MASK;
            } else if ("alt".equals(token)) {
                mask |= InputEvent.ALT_DOWN_MASK;
            } else if ("altGraph".equals(token)) {
                mask |= InputEvent.ALT_GRAPH_DOWN_MASK;
            } else if ("button1".equals(token)) {
                mask |= InputEvent.BUTTON1_DOWN_MASK;
            } else if ("button2".equals(token)) {
                mask |= InputEvent.BUTTON2_DOWN_MASK;
            } else if ("button3".equals(token)) {
                mask |= InputEvent.BUTTON3_DOWN_MASK;
            } else {
                throw new IllegalArgumentException("String formatted incorrectly");
            }
        }
        throw new IllegalArgumentException("String formatted incorrectly");
    }

    public final char getKeyChar() {
        return keyChar;
    }

    public final int getKeyCode() {
        return keyCode;
    }

    public final int getModifiers() {
        return modifiers;
    }

    public final boolean isOnKeyRelease() {
        return onKeyRelease;
    }

    public final int getKeyEventType() {
        if (keyCode == KeyEvent.VK_UNDEFINED) {
            return KeyEvent.KEY_TYPED;
        }
        return onKeyRelease ? KeyEvent.KEY_RELEASED : KeyEvent.KEY_PRESSED;
    }

    @Override
    public int hashCode() {
        return ((keyChar + 1) * (2 * (keyCode + 1)) * (modifiers + 1)) + (onKeyRelease ? 1 : 2);
    }

    @Override
    public final boolean equals(Object anObject) {
        if (anObject instanceof AWTKeyStroke) {
            AWTKeyStroke ks = (AWTKeyStroke) anObject;
            return ks.keyChar == keyChar && ks.keyCode == keyCode && ks.onKeyRelease == onKeyRelease
                    && ks.modifiers == modifiers;
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0) {
            sb.append("shift ");
        }
        if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0) {
            sb.append("ctrl ");
        }
        if ((modifiers & InputEvent.META_DOWN_MASK) != 0) {
            sb.append("meta ");
        }
        if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0) {
            sb.append("alt ");
        }
        if ((modifiers & InputEvent.ALT_GRAPH_DOWN_MASK) != 0) {
            sb.append("altGraph ");
        }
        if ((modifiers & InputEvent.BUTTON1_DOWN_MASK) != 0) {
            sb.append("button1 ");
        }
        if ((modifiers & InputEvent.BUTTON2_DOWN_MASK) != 0) {
            sb.append("button2 ");
        }
        if ((modifiers & InputEvent.BUTTON3_DOWN_MASK) != 0) {
            sb.append("button3 ");
        }
        if (keyCode == KeyEvent.VK_UNDEFINED) {
            return sb.append("typed ").append(keyChar).toString();
        }
        String name = KeyMap.nameOf(keyCode);
        return sb.append(onKeyRelease ? "released " : "pressed ").append(name == null ? "UNKNOWN" : name).toString();
    }
}
