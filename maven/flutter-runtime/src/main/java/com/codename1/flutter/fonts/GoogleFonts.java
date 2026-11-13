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

    public static TextStyle aBeeZee(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("ABeeZee", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme aBeeZeeTextTheme(TextTheme textTheme) {
        return themed("ABeeZee", textTheme);
    }

    public static TextStyle abel(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Abel", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme abelTextTheme(TextTheme textTheme) {
        return themed("Abel", textTheme);
    }

    public static TextStyle abrilFatface(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Abril Fatface", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme abrilFatfaceTextTheme(TextTheme textTheme) {
        return themed("Abril Fatface", textTheme);
    }

    public static TextStyle alegreya(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Alegreya", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme alegreyaTextTheme(TextTheme textTheme) {
        return themed("Alegreya", textTheme);
    }

    public static TextStyle alegreyaSans(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Alegreya Sans", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme alegreyaSansTextTheme(TextTheme textTheme) {
        return themed("Alegreya Sans", textTheme);
    }

    public static TextStyle alfaSlabOne(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Alfa Slab One", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme alfaSlabOneTextTheme(TextTheme textTheme) {
        return themed("Alfa Slab One", textTheme);
    }

    public static TextStyle amaticSC(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Amatic SC", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme amaticSCTextTheme(TextTheme textTheme) {
        return themed("Amatic SC", textTheme);
    }

    public static TextStyle anton(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Anton", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme antonTextTheme(TextTheme textTheme) {
        return themed("Anton", textTheme);
    }

    public static TextStyle archivo(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Archivo", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme archivoTextTheme(TextTheme textTheme) {
        return themed("Archivo", textTheme);
    }

    public static TextStyle arimo(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Arimo", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme arimoTextTheme(TextTheme textTheme) {
        return themed("Arimo", textTheme);
    }

    public static TextStyle arvo(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Arvo", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme arvoTextTheme(TextTheme textTheme) {
        return themed("Arvo", textTheme);
    }

    public static TextStyle asap(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Asap", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme asapTextTheme(TextTheme textTheme) {
        return themed("Asap", textTheme);
    }

    public static TextStyle assistant(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Assistant", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme assistantTextTheme(TextTheme textTheme) {
        return themed("Assistant", textTheme);
    }

    public static TextStyle barlow(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Barlow", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme barlowTextTheme(TextTheme textTheme) {
        return themed("Barlow", textTheme);
    }

    public static TextStyle barlowCondensed(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Barlow Condensed", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme barlowCondensedTextTheme(TextTheme textTheme) {
        return themed("Barlow Condensed", textTheme);
    }

    public static TextStyle bebasNeue(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Bebas Neue", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme bebasNeueTextTheme(TextTheme textTheme) {
        return themed("Bebas Neue", textTheme);
    }

    public static TextStyle bitter(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Bitter", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme bitterTextTheme(TextTheme textTheme) {
        return themed("Bitter", textTheme);
    }

    public static TextStyle cabin(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Cabin", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme cabinTextTheme(TextTheme textTheme) {
        return themed("Cabin", textTheme);
    }

    public static TextStyle cairo(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Cairo", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme cairoTextTheme(TextTheme textTheme) {
        return themed("Cairo", textTheme);
    }

    public static TextStyle caveat(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Caveat", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme caveatTextTheme(TextTheme textTheme) {
        return themed("Caveat", textTheme);
    }

    public static TextStyle cinzel(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Cinzel", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme cinzelTextTheme(TextTheme textTheme) {
        return themed("Cinzel", textTheme);
    }

    public static TextStyle comfortaa(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Comfortaa", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme comfortaaTextTheme(TextTheme textTheme) {
        return themed("Comfortaa", textTheme);
    }

    public static TextStyle cormorantGaramond(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Cormorant Garamond", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme cormorantGaramondTextTheme(TextTheme textTheme) {
        return themed("Cormorant Garamond", textTheme);
    }

    public static TextStyle crimsonText(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Crimson Text", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme crimsonTextTextTheme(TextTheme textTheme) {
        return themed("Crimson Text", textTheme);
    }

    public static TextStyle dancingScript(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Dancing Script", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme dancingScriptTextTheme(TextTheme textTheme) {
        return themed("Dancing Script", textTheme);
    }

    public static TextStyle dmSans(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("DM Sans", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme dmSansTextTheme(TextTheme textTheme) {
        return themed("DM Sans", textTheme);
    }

    public static TextStyle dmSerifDisplay(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("DM Serif Display", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme dmSerifDisplayTextTheme(TextTheme textTheme) {
        return themed("DM Serif Display", textTheme);
    }

    public static TextStyle dosis(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Dosis", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme dosisTextTheme(TextTheme textTheme) {
        return themed("Dosis", textTheme);
    }

    public static TextStyle ebGaramond(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("EB Garamond", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme ebGaramondTextTheme(TextTheme textTheme) {
        return themed("EB Garamond", textTheme);
    }

    public static TextStyle exo2(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Exo 2", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme exo2TextTheme(TextTheme textTheme) {
        return themed("Exo 2", textTheme);
    }

    public static TextStyle firaCode(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Fira Code", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme firaCodeTextTheme(TextTheme textTheme) {
        return themed("Fira Code", textTheme);
    }

    public static TextStyle firaSans(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Fira Sans", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme firaSansTextTheme(TextTheme textTheme) {
        return themed("Fira Sans", textTheme);
    }

    public static TextStyle fjallaOne(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Fjalla One", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme fjallaOneTextTheme(TextTheme textTheme) {
        return themed("Fjalla One", textTheme);
    }

    public static TextStyle greatVibes(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Great Vibes", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme greatVibesTextTheme(TextTheme textTheme) {
        return themed("Great Vibes", textTheme);
    }

    public static TextStyle heebo(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Heebo", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme heeboTextTheme(TextTheme textTheme) {
        return themed("Heebo", textTheme);
    }

    public static TextStyle hind(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Hind", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme hindTextTheme(TextTheme textTheme) {
        return themed("Hind", textTheme);
    }

    public static TextStyle ibmPlexMono(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("IBM Plex Mono", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme ibmPlexMonoTextTheme(TextTheme textTheme) {
        return themed("IBM Plex Mono", textTheme);
    }

    public static TextStyle ibmPlexSans(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("IBM Plex Sans", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme ibmPlexSansTextTheme(TextTheme textTheme) {
        return themed("IBM Plex Sans", textTheme);
    }

    public static TextStyle ibmPlexSerif(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("IBM Plex Serif", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme ibmPlexSerifTextTheme(TextTheme textTheme) {
        return themed("IBM Plex Serif", textTheme);
    }

    public static TextStyle inconsolata(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Inconsolata", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme inconsolataTextTheme(TextTheme textTheme) {
        return themed("Inconsolata", textTheme);
    }

    public static TextStyle indieFlower(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Indie Flower", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme indieFlowerTextTheme(TextTheme textTheme) {
        return themed("Indie Flower", textTheme);
    }

    public static TextStyle inter(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Inter", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme interTextTheme(TextTheme textTheme) {
        return themed("Inter", textTheme);
    }

    public static TextStyle josefinSans(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Josefin Sans", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme josefinSansTextTheme(TextTheme textTheme) {
        return themed("Josefin Sans", textTheme);
    }

    public static TextStyle jost(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Jost", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme jostTextTheme(TextTheme textTheme) {
        return themed("Jost", textTheme);
    }

    public static TextStyle kanit(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Kanit", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme kanitTextTheme(TextTheme textTheme) {
        return themed("Kanit", textTheme);
    }

    public static TextStyle karla(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Karla", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme karlaTextTheme(TextTheme textTheme) {
        return themed("Karla", textTheme);
    }

    public static TextStyle lato(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Lato", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme latoTextTheme(TextTheme textTheme) {
        return themed("Lato", textTheme);
    }

    public static TextStyle lexend(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Lexend", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme lexendTextTheme(TextTheme textTheme) {
        return themed("Lexend", textTheme);
    }

    public static TextStyle libreBaskerville(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Libre Baskerville", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme libreBaskervilleTextTheme(TextTheme textTheme) {
        return themed("Libre Baskerville", textTheme);
    }

    public static TextStyle lobster(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Lobster", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme lobsterTextTheme(TextTheme textTheme) {
        return themed("Lobster", textTheme);
    }

    public static TextStyle lora(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Lora", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme loraTextTheme(TextTheme textTheme) {
        return themed("Lora", textTheme);
    }

    public static TextStyle manrope(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Manrope", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme manropeTextTheme(TextTheme textTheme) {
        return themed("Manrope", textTheme);
    }

    public static TextStyle mavenPro(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Maven Pro", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme mavenProTextTheme(TextTheme textTheme) {
        return themed("Maven Pro", textTheme);
    }

    public static TextStyle mukta(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Mukta", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme muktaTextTheme(TextTheme textTheme) {
        return themed("Mukta", textTheme);
    }

    public static TextStyle mulish(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Mulish", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme mulishTextTheme(TextTheme textTheme) {
        return themed("Mulish", textTheme);
    }

    public static TextStyle notoSans(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Noto Sans", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme notoSansTextTheme(TextTheme textTheme) {
        return themed("Noto Sans", textTheme);
    }

    public static TextStyle notoSerif(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Noto Serif", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme notoSerifTextTheme(TextTheme textTheme) {
        return themed("Noto Serif", textTheme);
    }

    public static TextStyle nunito(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Nunito", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme nunitoTextTheme(TextTheme textTheme) {
        return themed("Nunito", textTheme);
    }

    public static TextStyle nunitoSans(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Nunito Sans", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme nunitoSansTextTheme(TextTheme textTheme) {
        return themed("Nunito Sans", textTheme);
    }

    public static TextStyle openSans(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Open Sans", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme openSansTextTheme(TextTheme textTheme) {
        return themed("Open Sans", textTheme);
    }

    public static TextStyle outfit(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Outfit", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme outfitTextTheme(TextTheme textTheme) {
        return themed("Outfit", textTheme);
    }

    public static TextStyle overpass(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Overpass", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme overpassTextTheme(TextTheme textTheme) {
        return themed("Overpass", textTheme);
    }

    public static TextStyle oxygen(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Oxygen", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme oxygenTextTheme(TextTheme textTheme) {
        return themed("Oxygen", textTheme);
    }

    public static TextStyle pacifico(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Pacifico", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme pacificoTextTheme(TextTheme textTheme) {
        return themed("Pacifico", textTheme);
    }

    public static TextStyle permanentMarker(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Permanent Marker", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme permanentMarkerTextTheme(TextTheme textTheme) {
        return themed("Permanent Marker", textTheme);
    }

    public static TextStyle play(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Play", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme playTextTheme(TextTheme textTheme) {
        return themed("Play", textTheme);
    }

    public static TextStyle playfairDisplay(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Playfair Display", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme playfairDisplayTextTheme(TextTheme textTheme) {
        return themed("Playfair Display", textTheme);
    }

    public static TextStyle plusJakartaSans(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Plus Jakarta Sans", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme plusJakartaSansTextTheme(TextTheme textTheme) {
        return themed("Plus Jakarta Sans", textTheme);
    }

    public static TextStyle poppins(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Poppins", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme poppinsTextTheme(TextTheme textTheme) {
        return themed("Poppins", textTheme);
    }

    public static TextStyle prompt(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Prompt", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme promptTextTheme(TextTheme textTheme) {
        return themed("Prompt", textTheme);
    }

    public static TextStyle ptSans(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("PT Sans", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme ptSansTextTheme(TextTheme textTheme) {
        return themed("PT Sans", textTheme);
    }

    public static TextStyle ptSerif(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("PT Serif", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme ptSerifTextTheme(TextTheme textTheme) {
        return themed("PT Serif", textTheme);
    }

    public static TextStyle quicksand(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Quicksand", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme quicksandTextTheme(TextTheme textTheme) {
        return themed("Quicksand", textTheme);
    }

    public static TextStyle raleway(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Raleway", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextStyle righteous(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Righteous", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme righteousTextTheme(TextTheme textTheme) {
        return themed("Righteous", textTheme);
    }

    public static TextStyle roboto(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Roboto", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme robotoTextTheme(TextTheme textTheme) {
        return themed("Roboto", textTheme);
    }

    public static TextStyle robotoSlab(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Roboto Slab", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme robotoSlabTextTheme(TextTheme textTheme) {
        return themed("Roboto Slab", textTheme);
    }

    public static TextStyle rubik(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Rubik", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextStyle satisfy(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Satisfy", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme satisfyTextTheme(TextTheme textTheme) {
        return themed("Satisfy", textTheme);
    }

    public static TextStyle shadowsIntoLight(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Shadows Into Light", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme shadowsIntoLightTextTheme(TextTheme textTheme) {
        return themed("Shadows Into Light", textTheme);
    }

    public static TextStyle signika(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Signika", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme signikaTextTheme(TextTheme textTheme) {
        return themed("Signika", textTheme);
    }

    public static TextStyle sourceCodePro(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Source Code Pro", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme sourceCodeProTextTheme(TextTheme textTheme) {
        return themed("Source Code Pro", textTheme);
    }

    public static TextStyle sourceSans3(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Source Sans 3", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme sourceSans3TextTheme(TextTheme textTheme) {
        return themed("Source Sans 3", textTheme);
    }

    public static TextStyle spaceGrotesk(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Space Grotesk", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme spaceGroteskTextTheme(TextTheme textTheme) {
        return themed("Space Grotesk", textTheme);
    }

    public static TextStyle spaceMono(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Space Mono", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme spaceMonoTextTheme(TextTheme textTheme) {
        return themed("Space Mono", textTheme);
    }

    public static TextStyle teko(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Teko", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme tekoTextTheme(TextTheme textTheme) {
        return themed("Teko", textTheme);
    }

    public static TextStyle titilliumWeb(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Titillium Web", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme titilliumWebTextTheme(TextTheme textTheme) {
        return themed("Titillium Web", textTheme);
    }

    public static TextStyle ubuntu(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Ubuntu", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme ubuntuTextTheme(TextTheme textTheme) {
        return themed("Ubuntu", textTheme);
    }

    public static TextStyle urbanist(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Urbanist", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme urbanistTextTheme(TextTheme textTheme) {
        return themed("Urbanist", textTheme);
    }

    public static TextStyle varelaRound(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Varela Round", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme varelaRoundTextTheme(TextTheme textTheme) {
        return themed("Varela Round", textTheme);
    }

    public static TextStyle yanoneKaffeesatz(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Yanone Kaffeesatz", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme yanoneKaffeesatzTextTheme(TextTheme textTheme) {
        return themed("Yanone Kaffeesatz", textTheme);
    }

    public static TextStyle zillaSlab(double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style("Zilla Slab", fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    public static TextTheme zillaSlabTextTheme(TextTheme textTheme) {
        return themed("Zilla Slab", textTheme);
    }

    /// {@code GoogleFonts.getFont(family, ...)}: any family by its Google Fonts name. The
    /// face is the one the app bundled under that name (see FontResolver), else the
    /// platform face.
    public static TextStyle getFont(String fontFamily, double fontSize, FontWeight fontWeight, Color color,
            Double letterSpacing, Double height,
            com.codename1.flutter.TextStyle textStyle, Object fontStyle, Object decoration,
            Double wordSpacing) {
        return style(fontFamily, fontSize, fontWeight, color, letterSpacing, height, textStyle, fontStyle,
                decoration, wordSpacing);
    }

    /** {@code GoogleFonts.getTextTheme(family, [textTheme])}. */
    public static TextTheme getTextTheme(String fontFamily, TextTheme textTheme) {
        return themed(fontFamily, textTheme);
    }
}
