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

import com.codename1.flutter.Brightness;
import com.codename1.flutter.Color;

/**
 * The resolved iOS theme, mirroring Flutter's {@code CupertinoThemeData}:
 * brightness, a few key colors and the {@link CupertinoTextThemeData}. Named
 * constructor parameters and {@link #copyWith} arrive as setters / a
 * positional copy respectively.
 */
public class CupertinoThemeData {

    private Brightness brightness;
    private Color primaryColor;
    private Color primaryContrastingColor;
    private Color scaffoldBackgroundColor;
    private Color barBackgroundColor;
    private CupertinoTextThemeData textTheme;

    public void brightness(Brightness v) {
        this.brightness = v;
    }

    public void primaryColor(Color v) {
        this.primaryColor = v;
    }

    public void primaryContrastingColor(Color v) {
        this.primaryContrastingColor = v;
    }

    public void scaffoldBackgroundColor(Color v) {
        this.scaffoldBackgroundColor = v;
    }

    public void barBackgroundColor(Color v) {
        this.barBackgroundColor = v;
    }

    public void textTheme(CupertinoTextThemeData v) {
        this.textTheme = v;
    }

    public Brightness brightness() {
        return brightness;
    }

    public Color primaryColor() {
        return primaryColor != null ? primaryColor : CupertinoColors.systemBlue;
    }

    public Color scaffoldBackgroundColor() {
        return scaffoldBackgroundColor != null ? scaffoldBackgroundColor : CupertinoColors.systemBackground;
    }

    public Color barBackgroundColor() {
        return barBackgroundColor != null ? barBackgroundColor : CupertinoColors.systemBackground;
    }

    public CupertinoTextThemeData textTheme() {
        return textTheme != null ? textTheme : new CupertinoTextThemeData();
    }

    /**
     * Returns a copy with the supplied (non-null) fields overridden;
     * parameters follow the order declared in the Dart stub.
     */
    public CupertinoThemeData copyWith(Brightness brightness, Color primaryColor,
                                       Color primaryContrastingColor, Color scaffoldBackgroundColor,
                                       Color barBackgroundColor, CupertinoTextThemeData textTheme) {
        CupertinoThemeData c = new CupertinoThemeData();
        c.brightness = brightness != null ? brightness : this.brightness;
        c.primaryColor = primaryColor != null ? primaryColor : this.primaryColor;
        c.primaryContrastingColor = primaryContrastingColor != null ? primaryContrastingColor : this.primaryContrastingColor;
        c.scaffoldBackgroundColor = scaffoldBackgroundColor != null ? scaffoldBackgroundColor : this.scaffoldBackgroundColor;
        c.barBackgroundColor = barBackgroundColor != null ? barBackgroundColor : this.barBackgroundColor;
        c.textTheme = textTheme != null ? textTheme : this.textTheme;
        return c;
    }
}
