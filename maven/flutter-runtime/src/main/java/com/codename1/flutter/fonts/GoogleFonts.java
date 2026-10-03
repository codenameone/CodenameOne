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
package com.codename1.flutter.fonts;

import com.codename1.flutter.Color;
import com.codename1.flutter.FontWeight;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.material.TextTheme;

/**
 * A no-op equivalent of the {@code google_fonts} package. Flutter's GoogleFonts
 * downloads/registers web fonts and returns a styled {@link TextStyle}; here we
 * return a TextStyle carrying the requested size/weight/color and fall back to
 * the platform font (faithful web-font loading is a later pass). The
 * {@code *TextTheme} helpers pass the supplied theme straight through.
 */
public abstract class GoogleFonts {

    /** {@code GoogleFonts.config} — a static getter, hence a static field here. */
    public static final GoogleFontsConfig config = new GoogleFontsConfig();

    private GoogleFonts() {
    }

    /// {@code textStyle:} is the BASE the rest are layered onto - google_fonts copies the
    /// given style and overrides only what was named. Ignoring it dropped whichever theme
    /// style the caller was extending, so the text kept the font and lost everything else.
    private static TextStyle style(String family, double fontSize, FontWeight fontWeight,
            Color color, Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        TextStyle t = textStyle != null
                ? textStyle.copyWith(null, null, null, null, null, null, null, null, null, null,
                        null, null, null)
                : new TextStyle();
        if (fontSize > 0) {
            t.fontSize(fontSize);
        }
        if (fontWeight != null) {
            t.fontWeight(fontWeight);
        }
        if (color != null) {
            t.color(color);
        }
        if (letterSpacing != null) {
            t.letterSpacing(letterSpacing.doubleValue());
        }
        if (height != null) {
            t.height(height.doubleValue());
        }
        // NAMING the family is the whole point of the call. It used to be
        // dropped, so every google_fonts style resolved to the platform face
        // and the studies rendered in the wrong typeface throughout.
        t.fontFamily(family);
        return t;
    }

    public static TextStyle eczar(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Eczar", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextStyle libreFranklin(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("LibreFranklin", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextStyle merriweather(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Merriweather", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextStyle montserrat(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Montserrat", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextStyle oswald(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Oswald", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextStyle robotoCondensed(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("RobotoCondensed", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextStyle robotoMono(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("RobotoMono", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextStyle workSans(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("WorkSans", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    /**
     * {@code GoogleFonts.<family>TextTheme(theme)} — the given text theme with
     * every role re-pointed at that family, which is how a study sets its
     * typeface once for a whole app. Passing the theme through unchanged (what
     * this used to do) left every one of those roles on the platform face.
     */
    private static TextTheme themed(String family, TextTheme textTheme) {
        TextTheme base = textTheme != null ? textTheme : new TextTheme();
        return base.apply(family, null, null, null, null, null, null);
    }

    public static TextTheme ralewayTextTheme(TextTheme textTheme) {
        return themed("Raleway", textTheme);
    }

    public static TextTheme rubikTextTheme(TextTheme textTheme) {
        return themed("Rubik", textTheme);
    }

    public static TextTheme workSansTextTheme(TextTheme textTheme) {
        return themed("WorkSans", textTheme);
    }
}
