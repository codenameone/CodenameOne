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

/// A pointer shape. Cursors are recorded on components but the pointer's
/// shape is never changed.
public class Cursor {

    public static final int DEFAULT_CURSOR = 0;
    public static final int CROSSHAIR_CURSOR = 1;
    public static final int TEXT_CURSOR = 2;
    public static final int WAIT_CURSOR = 3;
    public static final int SW_RESIZE_CURSOR = 4;
    public static final int SE_RESIZE_CURSOR = 5;
    public static final int NW_RESIZE_CURSOR = 6;
    public static final int NE_RESIZE_CURSOR = 7;
    public static final int N_RESIZE_CURSOR = 8;
    public static final int S_RESIZE_CURSOR = 9;
    public static final int W_RESIZE_CURSOR = 10;
    public static final int E_RESIZE_CURSOR = 11;
    public static final int HAND_CURSOR = 12;
    public static final int MOVE_CURSOR = 13;
    public static final int CUSTOM_CURSOR = -1;

    private static final String[] NAMES = {"Default Cursor", "Crosshair Cursor", "Text Cursor", "Wait Cursor",
        "Southwest Resize Cursor", "Southeast Resize Cursor", "Northwest Resize Cursor", "Northeast Resize Cursor",
        "North Resize Cursor", "South Resize Cursor", "West Resize Cursor", "East Resize Cursor", "Hand Cursor",
        "Move Cursor"};

    private static final Cursor[] PREDEFINED = new Cursor[14];

    protected String name;
    private final int type;

    public Cursor(int type) {
        if (type < DEFAULT_CURSOR || type > MOVE_CURSOR) {
            throw new IllegalArgumentException("illegal cursor type");
        }
        this.type = type;
        this.name = NAMES[type];
    }

    protected Cursor(String name) {
        this.type = CUSTOM_CURSOR;
        this.name = name;
    }

    public static Cursor getPredefinedCursor(int type) {
        if (type < DEFAULT_CURSOR || type > MOVE_CURSOR) {
            throw new IllegalArgumentException("illegal cursor type");
        }
        Cursor c = PREDEFINED[type];
        if (c == null) {
            c = new Cursor(type);
            PREDEFINED[type] = c;
        }
        return c;
    }

    public static Cursor getDefaultCursor() {
        return getPredefinedCursor(DEFAULT_CURSOR);
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[" + getName() + "]";
    }
}
