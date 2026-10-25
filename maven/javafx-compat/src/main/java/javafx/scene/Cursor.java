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
package javafx.scene;

import com.codename1.ui.Component;

/// The shape of the mouse pointer over a node or a scene. It is shown
/// where Codename One has a pointer to change, which is the desktop; a
/// cursor it has no shape for is shown as the default arrow. Custom image
/// cursors are not supported.
public abstract class Cursor {

    /// The platform's default arrow.
    public static final Cursor DEFAULT = new Standard("DEFAULT", Component.DEFAULT_CURSOR);
    /// A cross hair.
    public static final Cursor CROSSHAIR = new Standard("CROSSHAIR", Component.CROSSHAIR_CURSOR);
    /// The text insertion beam.
    public static final Cursor TEXT = new Standard("TEXT", Component.TEXT_CURSOR);
    /// The busy indicator.
    public static final Cursor WAIT = new Standard("WAIT", Component.WAIT_CURSOR);
    /// Resizing from the south west corner.
    public static final Cursor SW_RESIZE = new Standard("SW_RESIZE", Component.SW_RESIZE_CURSOR);
    /// Resizing from the south east corner.
    public static final Cursor SE_RESIZE = new Standard("SE_RESIZE", Component.SE_RESIZE_CURSOR);
    /// Resizing from the north west corner.
    public static final Cursor NW_RESIZE = new Standard("NW_RESIZE", Component.NW_RESIZE_CURSOR);
    /// Resizing from the north east corner.
    public static final Cursor NE_RESIZE = new Standard("NE_RESIZE", Component.NE_RESIZE_CURSOR);
    /// Resizing from the top edge.
    public static final Cursor N_RESIZE = new Standard("N_RESIZE", Component.N_RESIZE_CURSOR);
    /// Resizing from the bottom edge.
    public static final Cursor S_RESIZE = new Standard("S_RESIZE", Component.S_RESIZE_CURSOR);
    /// Resizing from the left edge.
    public static final Cursor W_RESIZE = new Standard("W_RESIZE", Component.W_RESIZE_CURSOR);
    /// Resizing from the right edge.
    public static final Cursor E_RESIZE = new Standard("E_RESIZE", Component.E_RESIZE_CURSOR);
    /// A hand with the fingers open.
    public static final Cursor OPEN_HAND = new Standard("OPEN_HAND", Component.HAND_CURSOR);
    /// A hand with the fingers closed.
    public static final Cursor CLOSED_HAND = new Standard("CLOSED_HAND", Component.MOVE_CURSOR);
    /// A pointing hand, as over a link.
    public static final Cursor HAND = new Standard("HAND", Component.HAND_CURSOR);
    /// Moving something.
    public static final Cursor MOVE = new Standard("MOVE", Component.MOVE_CURSOR);
    /// Something that cannot be dropped here; shown as the default arrow.
    public static final Cursor DISAPPEAR = new Standard("DISAPPEAR", Component.DEFAULT_CURSOR);
    /// Resizing horizontally.
    public static final Cursor H_RESIZE = new Standard("H_RESIZE", Component.E_RESIZE_CURSOR);
    /// Resizing vertically.
    public static final Cursor V_RESIZE = new Standard("V_RESIZE", Component.N_RESIZE_CURSOR);
    /// No pointer; shown as the default arrow.
    public static final Cursor NONE = new Standard("NONE", Component.DEFAULT_CURSOR);

    private static final Cursor[] ALL = {DEFAULT, CROSSHAIR, TEXT, WAIT, SW_RESIZE, SE_RESIZE, NW_RESIZE, NE_RESIZE,
        N_RESIZE, S_RESIZE, W_RESIZE, E_RESIZE, OPEN_HAND, CLOSED_HAND, HAND, MOVE, DISAPPEAR, H_RESIZE, V_RESIZE,
        NONE};

    Cursor() {
    }

    /// Returns the Codename One cursor constant of this cursor.
    public abstract int cn1Code();

    /// Returns the cursor of a name such as `HAND` or `hand`; the CSS
    /// spelling with dashes is accepted too.
    public static Cursor cursor(String identifier) {
        if (identifier == null) {
            throw new NullPointerException("The cursor identifier must not be null");
        }
        String wanted = identifier.trim().replace('-', '_');
        for (int i = 0; i < ALL.length; i++) {
            if (ALL[i].toString().equalsIgnoreCase(wanted)) {
                return ALL[i];
            }
        }
        throw new IllegalArgumentException("Unknown cursor: " + identifier);
    }

    private static final class Standard extends Cursor {
        private final String name;
        private final int code;

        Standard(String name, int code) {
            this.name = name;
            this.code = code;
        }

        @Override
        public int cn1Code() {
            return code;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
