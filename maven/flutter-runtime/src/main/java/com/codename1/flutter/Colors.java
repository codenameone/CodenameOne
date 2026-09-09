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
package com.codename1.flutter;

import com.codename1.generated.flutter.MaterialAccentColor;
import com.codename1.generated.flutter.MaterialColor;

/**
 * The material color swatch primaries (500 values), accent variants, and the
 * black/white opacity constants, mirroring Flutter's {@code Colors}.
 *
 * <p>The named primaries are typed {@link MaterialColor} and the accents
 * {@link MaterialAccentColor} (both extend {@link Color}) so the colors demo can
 * index their shades — matching Flutter, where {@code Colors.red} is a swatch,
 * not a plain color.</p>
 */
public final class Colors {

    private Colors() {
    }

    public static final Color transparent = new Color(0x00000000);

    public static final MaterialColor red = new MaterialColor(0xFFF44336);
    public static final MaterialAccentColor redAccent = new MaterialAccentColor(0xFFFF5252);
    public static final MaterialColor pink = new MaterialColor(0xFFE91E63);
    public static final MaterialAccentColor pinkAccent = new MaterialAccentColor(0xFFFF4081);
    public static final MaterialColor purple = new MaterialColor(0xFF9C27B0);
    public static final MaterialAccentColor purpleAccent = new MaterialAccentColor(0xFFE040FB);
    public static final MaterialColor deepPurple = new MaterialColor(0xFF673AB7);
    public static final MaterialAccentColor deepPurpleAccent = new MaterialAccentColor(0xFF7C4DFF);
    public static final MaterialColor indigo = new MaterialColor(0xFF3F51B5);
    public static final MaterialAccentColor indigoAccent = new MaterialAccentColor(0xFF536DFE);
    public static final MaterialColor blue = new MaterialColor(0xFF2196F3);
    public static final MaterialAccentColor blueAccent = new MaterialAccentColor(0xFF448AFF);
    public static final MaterialColor lightBlue = new MaterialColor(0xFF03A9F4);
    public static final MaterialAccentColor lightBlueAccent = new MaterialAccentColor(0xFF40C4FF);
    public static final MaterialColor cyan = new MaterialColor(0xFF00BCD4);
    public static final MaterialAccentColor cyanAccent = new MaterialAccentColor(0xFF18FFFF);
    public static final MaterialColor teal = new MaterialColor(0xFF009688);
    public static final MaterialAccentColor tealAccent = new MaterialAccentColor(0xFF64FFDA);
    public static final MaterialColor green = new MaterialColor(0xFF4CAF50);
    public static final MaterialAccentColor greenAccent = new MaterialAccentColor(0xFF69F0AE);
    public static final MaterialColor lightGreen = new MaterialColor(0xFF8BC34A);
    public static final MaterialAccentColor lightGreenAccent = new MaterialAccentColor(0xFFB2FF59);
    public static final MaterialColor lime = new MaterialColor(0xFFCDDC39);
    public static final MaterialAccentColor limeAccent = new MaterialAccentColor(0xFFEEFF41);
    public static final MaterialColor yellow = new MaterialColor(0xFFFFEB3B);
    public static final MaterialAccentColor yellowAccent = new MaterialAccentColor(0xFFFFFF00);
    public static final MaterialColor amber = new MaterialColor(0xFFFFC107);
    public static final MaterialAccentColor amberAccent = new MaterialAccentColor(0xFFFFD740);
    public static final MaterialColor orange = new MaterialColor(0xFFFF9800);
    public static final MaterialAccentColor orangeAccent = new MaterialAccentColor(0xFFFFAB40);
    public static final MaterialColor deepOrange = new MaterialColor(0xFFFF5722);
    public static final MaterialAccentColor deepOrangeAccent = new MaterialAccentColor(0xFFFF6E40);
    public static final MaterialColor brown = new MaterialColor(0xFF795548);
    public static final MaterialColor grey = new MaterialColor(0xFF9E9E9E);
    public static final MaterialColor blueGrey = new MaterialColor(0xFF607D8B);

    public static final Color white = new Color(0xFFFFFFFF);
    public static final Color white70 = new Color(0xB3FFFFFF);
    public static final Color white60 = new Color(0x99FFFFFF);
    public static final Color white54 = new Color(0x8AFFFFFF);
    public static final Color white38 = new Color(0x62FFFFFF);
    public static final Color white30 = new Color(0x4DFFFFFF);
    public static final Color white24 = new Color(0x3DFFFFFF);
    public static final Color white12 = new Color(0x1FFFFFFF);
    public static final Color white10 = new Color(0x1AFFFFFF);

    public static final Color black = new Color(0xFF000000);
    public static final Color black87 = new Color(0xDD000000);
    public static final Color black54 = new Color(0x8A000000);
    public static final Color black45 = new Color(0x73000000);
    public static final Color black38 = new Color(0x61000000);
    public static final Color black26 = new Color(0x42000000);
    public static final Color black12 = new Color(0x1F000000);
}
