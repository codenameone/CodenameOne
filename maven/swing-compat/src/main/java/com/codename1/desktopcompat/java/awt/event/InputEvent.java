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
package com.codename1.desktopcompat.java.awt.event;

import com.codename1.desktopcompat.java.awt.Component;

/// The root of the events that come from the keyboard and the mouse.
///
/// Modifiers exist in two encodings: the old masks (`SHIFT_MASK`, ...) and the
/// extended "down" masks (`SHIFT_DOWN_MASK`, ...). Like the JDK, the key and
/// mouse event constructors accept either and keep both, so `getModifiers()`
/// and `getModifiersEx()` answer for the same event.
public abstract class InputEvent extends ComponentEvent {

    public static final int SHIFT_MASK = 1;

    public static final int CTRL_MASK = 2;

    public static final int META_MASK = 4;

    public static final int ALT_MASK = 8;

    public static final int ALT_GRAPH_MASK = 32;

    public static final int BUTTON1_MASK = 16;

    public static final int BUTTON2_MASK = 8;

    public static final int BUTTON3_MASK = 4;

    public static final int SHIFT_DOWN_MASK = 64;

    public static final int CTRL_DOWN_MASK = 128;

    public static final int META_DOWN_MASK = 256;

    public static final int ALT_DOWN_MASK = 512;

    public static final int BUTTON1_DOWN_MASK = 1024;

    public static final int BUTTON2_DOWN_MASK = 2048;

    public static final int BUTTON3_DOWN_MASK = 4096;

    public static final int ALT_GRAPH_DOWN_MASK = 8192;

    /// The old encoding occupies the bits below `SHIFT_DOWN_MASK`.
    static final int JDK_1_3_MODIFIERS = SHIFT_DOWN_MASK - 1;

    /// Bits from the fourteenth up are the extra mouse buttons, which have no
    /// old encoding.
    static final int HIGH_MODIFIERS = ~((1 << 14) - 1);

    private static final int BUTTON_COUNT = 20;

    private final long when;

    int modifiers;

    InputEvent(Component source, int id, long when, int modifiers) {
        super(source, id);
        this.when = when;
        this.modifiers = modifiers;
    }

    /// Fills in whichever encoding of the modifiers the caller left out.
    static int normalize(int m, boolean mouse) {
        boolean old = (m & (JDK_1_3_MODIFIERS | HIGH_MODIFIERS)) != 0;
        boolean extended = (m & ~JDK_1_3_MODIFIERS) != 0;
        int r = m;
        if (old && !extended) {
            if ((m & SHIFT_MASK) != 0) {
                r |= SHIFT_DOWN_MASK;
            }
            if ((m & CTRL_MASK) != 0) {
                r |= CTRL_DOWN_MASK;
            }
            if ((m & ALT_GRAPH_MASK) != 0) {
                r |= ALT_GRAPH_DOWN_MASK;
            }
            if (mouse) {
                if ((m & BUTTON1_MASK) != 0) {
                    r |= BUTTON1_DOWN_MASK;
                }
                if ((m & BUTTON2_MASK) != 0) {
                    r |= BUTTON2_DOWN_MASK;
                }
                if ((m & BUTTON3_MASK) != 0) {
                    r |= BUTTON3_DOWN_MASK;
                }
            } else {
                if ((m & ALT_MASK) != 0) {
                    r |= ALT_DOWN_MASK;
                }
                if ((m & META_MASK) != 0) {
                    r |= META_DOWN_MASK;
                }
            }
        } else if (extended && !old) {
            if ((m & SHIFT_DOWN_MASK) != 0) {
                r |= SHIFT_MASK;
            }
            if ((m & CTRL_DOWN_MASK) != 0) {
                r |= CTRL_MASK;
            }
            if ((m & ALT_GRAPH_DOWN_MASK) != 0) {
                r |= ALT_GRAPH_MASK;
            }
            if ((m & ALT_DOWN_MASK) != 0) {
                r |= ALT_MASK;
            }
            if ((m & META_DOWN_MASK) != 0) {
                r |= META_MASK;
            }
            if (mouse) {
                if ((m & BUTTON1_DOWN_MASK) != 0) {
                    r |= BUTTON1_MASK;
                }
                if ((m & BUTTON2_DOWN_MASK) != 0) {
                    r |= BUTTON2_MASK;
                }
                if ((m & BUTTON3_DOWN_MASK) != 0) {
                    r |= BUTTON3_MASK;
                }
            }
        }
        return r;
    }

    private static int buttonMask(int index) {
        if (index == 0) {
            return BUTTON1_DOWN_MASK;
        }
        if (index == 1) {
            return BUTTON2_DOWN_MASK;
        }
        if (index == 2) {
            return BUTTON3_DOWN_MASK;
        }
        return 1 << (11 + index);
    }

    public boolean isShiftDown() {
        return (modifiers & SHIFT_DOWN_MASK) != 0;
    }

    public boolean isControlDown() {
        return (modifiers & CTRL_DOWN_MASK) != 0;
    }

    public boolean isMetaDown() {
        return (modifiers & META_DOWN_MASK) != 0;
    }

    public boolean isAltDown() {
        return (modifiers & ALT_DOWN_MASK) != 0;
    }

    public boolean isAltGraphDown() {
        return (modifiers & ALT_GRAPH_DOWN_MASK) != 0;
    }

    public long getWhen() {
        return when;
    }

    public int getModifiers() {
        return modifiers & (JDK_1_3_MODIFIERS | HIGH_MODIFIERS);
    }

    public int getModifiersEx() {
        return modifiers & ~JDK_1_3_MODIFIERS;
    }

    @Override
    public void consume() {
        consumed = true;
    }

    @Override
    public boolean isConsumed() {
        return consumed;
    }

    /// The extended mask of a mouse button, counting from one.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if the button number is not positive or
    ///   beyond the twenty buttons supported
    public static int getMaskForButton(int button) {
        if (button <= 0 || button > BUTTON_COUNT) {
            throw new IllegalArgumentException("button doesn't exist " + button);
        }
        return buttonMask(button - 1);
    }

    public static String getModifiersExText(int modifiers) {
        StringBuilder buf = new StringBuilder();
        if ((modifiers & META_DOWN_MASK) != 0) {
            add(buf, "Meta");
        }
        if ((modifiers & CTRL_DOWN_MASK) != 0) {
            add(buf, "Ctrl");
        }
        if ((modifiers & ALT_DOWN_MASK) != 0) {
            add(buf, "Alt");
        }
        if ((modifiers & SHIFT_DOWN_MASK) != 0) {
            add(buf, "Shift");
        }
        if ((modifiers & ALT_GRAPH_DOWN_MASK) != 0) {
            add(buf, "Alt Graph");
        }
        for (int i = 0; i < BUTTON_COUNT; i++) {
            if ((modifiers & buttonMask(i)) != 0) {
                add(buf, "Button" + (i + 1));
            }
        }
        return buf.toString();
    }

    private static void add(StringBuilder buf, String name) {
        if (buf.length() > 0) {
            buf.append('+');
        }
        buf.append(name);
    }
}
