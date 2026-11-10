/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;

/**
 * A {@link Color} that, in Flutter, resolves to a different concrete color
 * depending on the ambient {@code CupertinoTheme} brightness, accessibility
 * contrast and elevation. This runtime is single-appearance, so
 * {@link #resolveFrom(BuildContext)} returns this same color — enough for the
 * demos, which call {@code resolveFrom(context)} purely to obtain a plain
 * Color.
 */
public class CupertinoDynamicColor extends Color {

    private Color dark;

    public CupertinoDynamicColor(long value) {
        super(value);
    }

    /** Flutter's {@code CupertinoDynamicColor.withBrightness}: a light and a dark variant. */
    public static CupertinoDynamicColor withBrightness(Color color, Color darkColor) {
        CupertinoDynamicColor c = new CupertinoDynamicColor(color.value());
        c.dark = darkColor;
        return c;
    }

    /** The light, normal-contrast variant: this color itself. */
    public Color color() {
        return new Color(value());
    }

    /** The dark variant; the light one when none was given. */
    public Color darkColor() {
        return dark == null ? color() : dark;
    }

    /**
     * Resolves against the given context; this single-appearance runtime
     * returns the color unchanged.
     */
    public Color resolveFrom(BuildContext context) {
        return this;
    }
}
