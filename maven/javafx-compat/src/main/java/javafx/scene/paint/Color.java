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
package javafx.scene.paint;

import java.util.HashMap;

import javafx.animation.Interpolatable;
import javafx.beans.NamedArg;

/// A colour in the sRGB space with an opacity. Each component is a number
/// from 0 to 1.
public final class Color extends Paint implements Interpolatable<Color> {

    private static final HashMap<String, Color> NAMED = new HashMap<String, Color>();
    private static final double DARKER_BRIGHTER_FACTOR = 0.7;
    private static final double SATURATE_DESATURATE_FACTOR = 0.7;

    /// A fully transparent colour.
    public static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    /// The colour `aliceblue`, `#F0F8FF`.
    public static final Color ALICEBLUE = named("aliceblue", 0xF0F8FF);

    /// The colour `antiquewhite`, `#FAEBD7`.
    public static final Color ANTIQUEWHITE = named("antiquewhite", 0xFAEBD7);

    /// The colour `aqua`, `#00FFFF`.
    public static final Color AQUA = named("aqua", 0x00FFFF);

    /// The colour `aquamarine`, `#7FFFD4`.
    public static final Color AQUAMARINE = named("aquamarine", 0x7FFFD4);

    /// The colour `azure`, `#F0FFFF`.
    public static final Color AZURE = named("azure", 0xF0FFFF);

    /// The colour `beige`, `#F5F5DC`.
    public static final Color BEIGE = named("beige", 0xF5F5DC);

    /// The colour `bisque`, `#FFE4C4`.
    public static final Color BISQUE = named("bisque", 0xFFE4C4);

    /// The colour `black`, `#000000`.
    public static final Color BLACK = named("black", 0x000000);

    /// The colour `blanchedalmond`, `#FFEBCD`.
    public static final Color BLANCHEDALMOND = named("blanchedalmond", 0xFFEBCD);

    /// The colour `blue`, `#0000FF`.
    public static final Color BLUE = named("blue", 0x0000FF);

    /// The colour `blueviolet`, `#8A2BE2`.
    public static final Color BLUEVIOLET = named("blueviolet", 0x8A2BE2);

    /// The colour `brown`, `#A52A2A`.
    public static final Color BROWN = named("brown", 0xA52A2A);

    /// The colour `burlywood`, `#DEB887`.
    public static final Color BURLYWOOD = named("burlywood", 0xDEB887);

    /// The colour `cadetblue`, `#5F9EA0`.
    public static final Color CADETBLUE = named("cadetblue", 0x5F9EA0);

    /// The colour `chartreuse`, `#7FFF00`.
    public static final Color CHARTREUSE = named("chartreuse", 0x7FFF00);

    /// The colour `chocolate`, `#D2691E`.
    public static final Color CHOCOLATE = named("chocolate", 0xD2691E);

    /// The colour `coral`, `#FF7F50`.
    public static final Color CORAL = named("coral", 0xFF7F50);

    /// The colour `cornflowerblue`, `#6495ED`.
    public static final Color CORNFLOWERBLUE = named("cornflowerblue", 0x6495ED);

    /// The colour `cornsilk`, `#FFF8DC`.
    public static final Color CORNSILK = named("cornsilk", 0xFFF8DC);

    /// The colour `crimson`, `#DC143C`.
    public static final Color CRIMSON = named("crimson", 0xDC143C);

    /// The colour `cyan`, `#00FFFF`.
    public static final Color CYAN = named("cyan", 0x00FFFF);

    /// The colour `darkblue`, `#00008B`.
    public static final Color DARKBLUE = named("darkblue", 0x00008B);

    /// The colour `darkcyan`, `#008B8B`.
    public static final Color DARKCYAN = named("darkcyan", 0x008B8B);

    /// The colour `darkgoldenrod`, `#B8860B`.
    public static final Color DARKGOLDENROD = named("darkgoldenrod", 0xB8860B);

    /// The colour `darkgray`, `#A9A9A9`.
    public static final Color DARKGRAY = named("darkgray", 0xA9A9A9);

    /// The colour `darkgreen`, `#006400`.
    public static final Color DARKGREEN = named("darkgreen", 0x006400);

    /// The colour `darkgrey`, `#A9A9A9`.
    public static final Color DARKGREY = named("darkgrey", 0xA9A9A9);

    /// The colour `darkkhaki`, `#BDB76B`.
    public static final Color DARKKHAKI = named("darkkhaki", 0xBDB76B);

    /// The colour `darkmagenta`, `#8B008B`.
    public static final Color DARKMAGENTA = named("darkmagenta", 0x8B008B);

    /// The colour `darkolivegreen`, `#556B2F`.
    public static final Color DARKOLIVEGREEN = named("darkolivegreen", 0x556B2F);

    /// The colour `darkorange`, `#FF8C00`.
    public static final Color DARKORANGE = named("darkorange", 0xFF8C00);

    /// The colour `darkorchid`, `#9932CC`.
    public static final Color DARKORCHID = named("darkorchid", 0x9932CC);

    /// The colour `darkred`, `#8B0000`.
    public static final Color DARKRED = named("darkred", 0x8B0000);

    /// The colour `darksalmon`, `#E9967A`.
    public static final Color DARKSALMON = named("darksalmon", 0xE9967A);

    /// The colour `darkseagreen`, `#8FBC8F`.
    public static final Color DARKSEAGREEN = named("darkseagreen", 0x8FBC8F);

    /// The colour `darkslateblue`, `#483D8B`.
    public static final Color DARKSLATEBLUE = named("darkslateblue", 0x483D8B);

    /// The colour `darkslategray`, `#2F4F4F`.
    public static final Color DARKSLATEGRAY = named("darkslategray", 0x2F4F4F);

    /// The colour `darkslategrey`, `#2F4F4F`.
    public static final Color DARKSLATEGREY = named("darkslategrey", 0x2F4F4F);

    /// The colour `darkturquoise`, `#00CED1`.
    public static final Color DARKTURQUOISE = named("darkturquoise", 0x00CED1);

    /// The colour `darkviolet`, `#9400D3`.
    public static final Color DARKVIOLET = named("darkviolet", 0x9400D3);

    /// The colour `deeppink`, `#FF1493`.
    public static final Color DEEPPINK = named("deeppink", 0xFF1493);

    /// The colour `deepskyblue`, `#00BFFF`.
    public static final Color DEEPSKYBLUE = named("deepskyblue", 0x00BFFF);

    /// The colour `dimgray`, `#696969`.
    public static final Color DIMGRAY = named("dimgray", 0x696969);

    /// The colour `dimgrey`, `#696969`.
    public static final Color DIMGREY = named("dimgrey", 0x696969);

    /// The colour `dodgerblue`, `#1E90FF`.
    public static final Color DODGERBLUE = named("dodgerblue", 0x1E90FF);

    /// The colour `firebrick`, `#B22222`.
    public static final Color FIREBRICK = named("firebrick", 0xB22222);

    /// The colour `floralwhite`, `#FFFAF0`.
    public static final Color FLORALWHITE = named("floralwhite", 0xFFFAF0);

    /// The colour `forestgreen`, `#228B22`.
    public static final Color FORESTGREEN = named("forestgreen", 0x228B22);

    /// The colour `fuchsia`, `#FF00FF`.
    public static final Color FUCHSIA = named("fuchsia", 0xFF00FF);

    /// The colour `gainsboro`, `#DCDCDC`.
    public static final Color GAINSBORO = named("gainsboro", 0xDCDCDC);

    /// The colour `ghostwhite`, `#F8F8FF`.
    public static final Color GHOSTWHITE = named("ghostwhite", 0xF8F8FF);

    /// The colour `gold`, `#FFD700`.
    public static final Color GOLD = named("gold", 0xFFD700);

    /// The colour `goldenrod`, `#DAA520`.
    public static final Color GOLDENROD = named("goldenrod", 0xDAA520);

    /// The colour `gray`, `#808080`.
    public static final Color GRAY = named("gray", 0x808080);

    /// The colour `green`, `#008000`.
    public static final Color GREEN = named("green", 0x008000);

    /// The colour `greenyellow`, `#ADFF2F`.
    public static final Color GREENYELLOW = named("greenyellow", 0xADFF2F);

    /// The colour `grey`, `#808080`.
    public static final Color GREY = named("grey", 0x808080);

    /// The colour `honeydew`, `#F0FFF0`.
    public static final Color HONEYDEW = named("honeydew", 0xF0FFF0);

    /// The colour `hotpink`, `#FF69B4`.
    public static final Color HOTPINK = named("hotpink", 0xFF69B4);

    /// The colour `indianred`, `#CD5C5C`.
    public static final Color INDIANRED = named("indianred", 0xCD5C5C);

    /// The colour `indigo`, `#4B0082`.
    public static final Color INDIGO = named("indigo", 0x4B0082);

    /// The colour `ivory`, `#FFFFF0`.
    public static final Color IVORY = named("ivory", 0xFFFFF0);

    /// The colour `khaki`, `#F0E68C`.
    public static final Color KHAKI = named("khaki", 0xF0E68C);

    /// The colour `lavender`, `#E6E6FA`.
    public static final Color LAVENDER = named("lavender", 0xE6E6FA);

    /// The colour `lavenderblush`, `#FFF0F5`.
    public static final Color LAVENDERBLUSH = named("lavenderblush", 0xFFF0F5);

    /// The colour `lawngreen`, `#7CFC00`.
    public static final Color LAWNGREEN = named("lawngreen", 0x7CFC00);

    /// The colour `lemonchiffon`, `#FFFACD`.
    public static final Color LEMONCHIFFON = named("lemonchiffon", 0xFFFACD);

    /// The colour `lightblue`, `#ADD8E6`.
    public static final Color LIGHTBLUE = named("lightblue", 0xADD8E6);

    /// The colour `lightcoral`, `#F08080`.
    public static final Color LIGHTCORAL = named("lightcoral", 0xF08080);

    /// The colour `lightcyan`, `#E0FFFF`.
    public static final Color LIGHTCYAN = named("lightcyan", 0xE0FFFF);

    /// The colour `lightgoldenrodyellow`, `#FAFAD2`.
    public static final Color LIGHTGOLDENRODYELLOW = named("lightgoldenrodyellow", 0xFAFAD2);

    /// The colour `lightgray`, `#D3D3D3`.
    public static final Color LIGHTGRAY = named("lightgray", 0xD3D3D3);

    /// The colour `lightgreen`, `#90EE90`.
    public static final Color LIGHTGREEN = named("lightgreen", 0x90EE90);

    /// The colour `lightgrey`, `#D3D3D3`.
    public static final Color LIGHTGREY = named("lightgrey", 0xD3D3D3);

    /// The colour `lightpink`, `#FFB6C1`.
    public static final Color LIGHTPINK = named("lightpink", 0xFFB6C1);

    /// The colour `lightsalmon`, `#FFA07A`.
    public static final Color LIGHTSALMON = named("lightsalmon", 0xFFA07A);

    /// The colour `lightseagreen`, `#20B2AA`.
    public static final Color LIGHTSEAGREEN = named("lightseagreen", 0x20B2AA);

    /// The colour `lightskyblue`, `#87CEFA`.
    public static final Color LIGHTSKYBLUE = named("lightskyblue", 0x87CEFA);

    /// The colour `lightslategray`, `#778899`.
    public static final Color LIGHTSLATEGRAY = named("lightslategray", 0x778899);

    /// The colour `lightslategrey`, `#778899`.
    public static final Color LIGHTSLATEGREY = named("lightslategrey", 0x778899);

    /// The colour `lightsteelblue`, `#B0C4DE`.
    public static final Color LIGHTSTEELBLUE = named("lightsteelblue", 0xB0C4DE);

    /// The colour `lightyellow`, `#FFFFE0`.
    public static final Color LIGHTYELLOW = named("lightyellow", 0xFFFFE0);

    /// The colour `lime`, `#00FF00`.
    public static final Color LIME = named("lime", 0x00FF00);

    /// The colour `limegreen`, `#32CD32`.
    public static final Color LIMEGREEN = named("limegreen", 0x32CD32);

    /// The colour `linen`, `#FAF0E6`.
    public static final Color LINEN = named("linen", 0xFAF0E6);

    /// The colour `magenta`, `#FF00FF`.
    public static final Color MAGENTA = named("magenta", 0xFF00FF);

    /// The colour `maroon`, `#800000`.
    public static final Color MAROON = named("maroon", 0x800000);

    /// The colour `mediumaquamarine`, `#66CDAA`.
    public static final Color MEDIUMAQUAMARINE = named("mediumaquamarine", 0x66CDAA);

    /// The colour `mediumblue`, `#0000CD`.
    public static final Color MEDIUMBLUE = named("mediumblue", 0x0000CD);

    /// The colour `mediumorchid`, `#BA55D3`.
    public static final Color MEDIUMORCHID = named("mediumorchid", 0xBA55D3);

    /// The colour `mediumpurple`, `#9370DB`.
    public static final Color MEDIUMPURPLE = named("mediumpurple", 0x9370DB);

    /// The colour `mediumseagreen`, `#3CB371`.
    public static final Color MEDIUMSEAGREEN = named("mediumseagreen", 0x3CB371);

    /// The colour `mediumslateblue`, `#7B68EE`.
    public static final Color MEDIUMSLATEBLUE = named("mediumslateblue", 0x7B68EE);

    /// The colour `mediumspringgreen`, `#00FA9A`.
    public static final Color MEDIUMSPRINGGREEN = named("mediumspringgreen", 0x00FA9A);

    /// The colour `mediumturquoise`, `#48D1CC`.
    public static final Color MEDIUMTURQUOISE = named("mediumturquoise", 0x48D1CC);

    /// The colour `mediumvioletred`, `#C71585`.
    public static final Color MEDIUMVIOLETRED = named("mediumvioletred", 0xC71585);

    /// The colour `midnightblue`, `#191970`.
    public static final Color MIDNIGHTBLUE = named("midnightblue", 0x191970);

    /// The colour `mintcream`, `#F5FFFA`.
    public static final Color MINTCREAM = named("mintcream", 0xF5FFFA);

    /// The colour `mistyrose`, `#FFE4E1`.
    public static final Color MISTYROSE = named("mistyrose", 0xFFE4E1);

    /// The colour `moccasin`, `#FFE4B5`.
    public static final Color MOCCASIN = named("moccasin", 0xFFE4B5);

    /// The colour `navajowhite`, `#FFDEAD`.
    public static final Color NAVAJOWHITE = named("navajowhite", 0xFFDEAD);

    /// The colour `navy`, `#000080`.
    public static final Color NAVY = named("navy", 0x000080);

    /// The colour `oldlace`, `#FDF5E6`.
    public static final Color OLDLACE = named("oldlace", 0xFDF5E6);

    /// The colour `olive`, `#808000`.
    public static final Color OLIVE = named("olive", 0x808000);

    /// The colour `olivedrab`, `#6B8E23`.
    public static final Color OLIVEDRAB = named("olivedrab", 0x6B8E23);

    /// The colour `orange`, `#FFA500`.
    public static final Color ORANGE = named("orange", 0xFFA500);

    /// The colour `orangered`, `#FF4500`.
    public static final Color ORANGERED = named("orangered", 0xFF4500);

    /// The colour `orchid`, `#DA70D6`.
    public static final Color ORCHID = named("orchid", 0xDA70D6);

    /// The colour `palegoldenrod`, `#EEE8AA`.
    public static final Color PALEGOLDENROD = named("palegoldenrod", 0xEEE8AA);

    /// The colour `palegreen`, `#98FB98`.
    public static final Color PALEGREEN = named("palegreen", 0x98FB98);

    /// The colour `paleturquoise`, `#AFEEEE`.
    public static final Color PALETURQUOISE = named("paleturquoise", 0xAFEEEE);

    /// The colour `palevioletred`, `#DB7093`.
    public static final Color PALEVIOLETRED = named("palevioletred", 0xDB7093);

    /// The colour `papayawhip`, `#FFEFD5`.
    public static final Color PAPAYAWHIP = named("papayawhip", 0xFFEFD5);

    /// The colour `peachpuff`, `#FFDAB9`.
    public static final Color PEACHPUFF = named("peachpuff", 0xFFDAB9);

    /// The colour `peru`, `#CD853F`.
    public static final Color PERU = named("peru", 0xCD853F);

    /// The colour `pink`, `#FFC0CB`.
    public static final Color PINK = named("pink", 0xFFC0CB);

    /// The colour `plum`, `#DDA0DD`.
    public static final Color PLUM = named("plum", 0xDDA0DD);

    /// The colour `powderblue`, `#B0E0E6`.
    public static final Color POWDERBLUE = named("powderblue", 0xB0E0E6);

    /// The colour `purple`, `#800080`.
    public static final Color PURPLE = named("purple", 0x800080);

    /// The colour `red`, `#FF0000`.
    public static final Color RED = named("red", 0xFF0000);

    /// The colour `rosybrown`, `#BC8F8F`.
    public static final Color ROSYBROWN = named("rosybrown", 0xBC8F8F);

    /// The colour `royalblue`, `#4169E1`.
    public static final Color ROYALBLUE = named("royalblue", 0x4169E1);

    /// The colour `saddlebrown`, `#8B4513`.
    public static final Color SADDLEBROWN = named("saddlebrown", 0x8B4513);

    /// The colour `salmon`, `#FA8072`.
    public static final Color SALMON = named("salmon", 0xFA8072);

    /// The colour `sandybrown`, `#F4A460`.
    public static final Color SANDYBROWN = named("sandybrown", 0xF4A460);

    /// The colour `seagreen`, `#2E8B57`.
    public static final Color SEAGREEN = named("seagreen", 0x2E8B57);

    /// The colour `seashell`, `#FFF5EE`.
    public static final Color SEASHELL = named("seashell", 0xFFF5EE);

    /// The colour `sienna`, `#A0522D`.
    public static final Color SIENNA = named("sienna", 0xA0522D);

    /// The colour `silver`, `#C0C0C0`.
    public static final Color SILVER = named("silver", 0xC0C0C0);

    /// The colour `skyblue`, `#87CEEB`.
    public static final Color SKYBLUE = named("skyblue", 0x87CEEB);

    /// The colour `slateblue`, `#6A5ACD`.
    public static final Color SLATEBLUE = named("slateblue", 0x6A5ACD);

    /// The colour `slategray`, `#708090`.
    public static final Color SLATEGRAY = named("slategray", 0x708090);

    /// The colour `slategrey`, `#708090`.
    public static final Color SLATEGREY = named("slategrey", 0x708090);

    /// The colour `snow`, `#FFFAFA`.
    public static final Color SNOW = named("snow", 0xFFFAFA);

    /// The colour `springgreen`, `#00FF7F`.
    public static final Color SPRINGGREEN = named("springgreen", 0x00FF7F);

    /// The colour `steelblue`, `#4682B4`.
    public static final Color STEELBLUE = named("steelblue", 0x4682B4);

    /// The colour `tan`, `#D2B48C`.
    public static final Color TAN = named("tan", 0xD2B48C);

    /// The colour `teal`, `#008080`.
    public static final Color TEAL = named("teal", 0x008080);

    /// The colour `thistle`, `#D8BFD8`.
    public static final Color THISTLE = named("thistle", 0xD8BFD8);

    /// The colour `tomato`, `#FF6347`.
    public static final Color TOMATO = named("tomato", 0xFF6347);

    /// The colour `turquoise`, `#40E0D0`.
    public static final Color TURQUOISE = named("turquoise", 0x40E0D0);

    /// The colour `violet`, `#EE82EE`.
    public static final Color VIOLET = named("violet", 0xEE82EE);

    /// The colour `wheat`, `#F5DEB3`.
    public static final Color WHEAT = named("wheat", 0xF5DEB3);

    /// The colour `white`, `#FFFFFF`.
    public static final Color WHITE = named("white", 0xFFFFFF);

    /// The colour `whitesmoke`, `#F5F5F5`.
    public static final Color WHITESMOKE = named("whitesmoke", 0xF5F5F5);

    /// The colour `yellow`, `#FFFF00`.
    public static final Color YELLOW = named("yellow", 0xFFFF00);

    /// The colour `yellowgreen`, `#9ACD32`.
    public static final Color YELLOWGREEN = named("yellowgreen", 0x9ACD32);

    static {
        NAMED.put("transparent", TRANSPARENT);
    }

    private final float red;
    private final float green;
    private final float blue;
    private final float opacity;

    /// Creates a colour from its components, each from 0 to 1.
    public Color(@NamedArg("red") double red, @NamedArg("green") double green, @NamedArg("blue") double blue,
            @NamedArg(value = "opacity", defaultValue = "1") double opacity) {
        check(red, "red");
        check(green, "green");
        check(blue, "blue");
        check(opacity, "opacity");
        this.red = (float) red;
        this.green = (float) green;
        this.blue = (float) blue;
        this.opacity = (float) opacity;
    }

    private static void check(double value, String name) {
        if (!(value >= 0 && value <= 1)) {
            throw new IllegalArgumentException("Color's " + name + " value (" + value + ") must be in the range 0.0-1.0");
        }
    }

    private static Color named(String name, int rgb) {
        Color c = rgb((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff);
        NAMED.put(name, c);
        return c;
    }

    /// Creates a colour from components from 0 to 1.
    public static Color color(double red, double green, double blue, double opacity) {
        return new Color(red, green, blue, opacity);
    }

    /// Creates an opaque colour from components from 0 to 1.
    public static Color color(double red, double green, double blue) {
        return new Color(red, green, blue, 1);
    }

    /// Creates a colour from components from 0 to 255 and an opacity.
    public static Color rgb(int red, int green, int blue, double opacity) {
        checkByte(red, "red");
        checkByte(green, "green");
        checkByte(blue, "blue");
        return new Color(red / 255.0, green / 255.0, blue / 255.0, opacity);
    }

    /// Creates an opaque colour from components from 0 to 255.
    public static Color rgb(int red, int green, int blue) {
        return rgb(red, green, blue, 1);
    }

    private static void checkByte(int value, String name) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException("Color.rgb's " + name + " parameter (" + value + ") expects color values 0-255");
        }
    }

    /// Creates an opaque grey from 0 to 255.
    public static Color grayRgb(int gray) {
        return rgb(gray, gray, gray);
    }

    /// Creates a grey from 0 to 255 with an opacity.
    public static Color grayRgb(int gray, double opacity) {
        return rgb(gray, gray, gray, opacity);
    }

    /// Creates a grey from 0 to 1 with an opacity.
    public static Color gray(double gray, double opacity) {
        return new Color(gray, gray, gray, opacity);
    }

    /// Creates an opaque grey from 0 to 1.
    public static Color gray(double gray) {
        return gray(gray, 1);
    }

    /// Creates a colour from hue in degrees, saturation and brightness.
    public static Color hsb(double hue, double saturation, double brightness, double opacity) {
        if (!(saturation >= 0 && saturation <= 1)) {
            throw new IllegalArgumentException("Color.hsb's saturation parameter (" + saturation + ") expects values 0.0-1.0");
        }
        if (!(brightness >= 0 && brightness <= 1)) {
            throw new IllegalArgumentException("Color.hsb's brightness parameter (" + brightness + ") expects values 0.0-1.0");
        }
        double[] rgb = hsbToRgb(hue, saturation, brightness);
        return new Color(rgb[0], rgb[1], rgb[2], opacity);
    }

    /// Creates an opaque colour from hue, saturation and brightness.
    public static Color hsb(double hue, double saturation, double brightness) {
        return hsb(hue, saturation, brightness, 1);
    }

    /// Parses a colour as written in HTML or CSS: a name, `#rgb`,
    /// `#rrggbb`, `#rrggbbaa`, `0x...`, `rgb(...)`, `rgba(...)`,
    /// `hsl(...)` or `hsla(...)`; the opacity scales the result.
    public static Color web(String colorString, double opacity) {
        if (colorString == null) {
            throw new NullPointerException("The color components or name must be specified");
        }
        String color = lower(colorString.trim());
        if (color.length() == 0) {
            throw new IllegalArgumentException("Invalid color specification");
        }
        if (color.startsWith("#")) {
            return hex(color.substring(1), opacity);
        }
        if (color.startsWith("0x")) {
            return hex(color.substring(2), opacity);
        }
        if (color.startsWith("rgba(") || color.startsWith("rgb(") || color.startsWith("hsla(")
                || color.startsWith("hsl(")) {
            return functional(color, opacity);
        }
        Color named = NAMED.get(color);
        if (named != null) {
            if (opacity == 1) {
                return named;
            }
            return new Color(named.red, named.green, named.blue, named.opacity * opacity);
        }
        return hex(color, opacity);
    }

    /// Parses a colour as written in HTML or CSS.
    public static Color web(String colorString) {
        return web(colorString, 1);
    }

    /// Parses a colour; the same as [#web(String)].
    public static Color valueOf(String value) {
        if (value == null) {
            throw new NullPointerException("color must be specified");
        }
        return web(value);
    }

    private static String lower(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            out.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
        }
        return out.toString();
    }

    private static int digit(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        throw new IllegalArgumentException("Invalid color specification");
    }

    private static Color hex(String h, double opacity) {
        int len = h.length();
        int r;
        int g;
        int b;
        int a = 255;
        if (len == 3 || len == 4) {
            r = digit(h.charAt(0)) * 17;
            g = digit(h.charAt(1)) * 17;
            b = digit(h.charAt(2)) * 17;
            if (len == 4) {
                a = digit(h.charAt(3)) * 17;
            }
        } else if (len == 6 || len == 8) {
            r = digit(h.charAt(0)) * 16 + digit(h.charAt(1));
            g = digit(h.charAt(2)) * 16 + digit(h.charAt(3));
            b = digit(h.charAt(4)) * 16 + digit(h.charAt(5));
            if (len == 8) {
                a = digit(h.charAt(6)) * 16 + digit(h.charAt(7));
            }
        } else {
            throw new IllegalArgumentException("Invalid color specification");
        }
        return rgb(r, g, b, opacity * a / 255.0);
    }

    private static Color functional(String color, double opacity) {
        int open = color.indexOf('(');
        int close = color.lastIndexOf(')');
        if (close < open) {
            throw new IllegalArgumentException("Invalid color specification");
        }
        boolean hsl = color.startsWith("hsl");
        boolean alpha = color.charAt(open - 1) == 'a';
        String[] parts = new String[4];
        int count = 0;
        int start = open + 1;
        for (int i = open + 1; i <= close; i++) {
            if (i == close || color.charAt(i) == ',') {
                if (count == 4) {
                    throw new IllegalArgumentException("Invalid color specification");
                }
                parts[count++] = color.substring(start, i).trim();
                start = i + 1;
            }
        }
        if (count != (alpha ? 4 : 3)) {
            throw new IllegalArgumentException("Invalid color specification");
        }
        double a = alpha ? component(parts[3], 1, false) : 1;
        if (hsl) {
            double h = number(parts[0]);
            double s = component(parts[1], 1, true);
            double l = component(parts[2], 1, true);
            // Lightness to brightness.
            double v = l + s * Math.min(l, 1 - l);
            double sv = v == 0 ? 0 : 2 * (1 - l / v);
            return hsb(h, clamp(sv), clamp(v), clamp(a * opacity));
        }
        return new Color(component(parts[0], 255, false), component(parts[1], 255, false),
                component(parts[2], 255, false), clamp(a * opacity));
    }

    private static double number(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException malformed) {
            throw new IllegalArgumentException("Invalid color specification");
        }
    }

    private static double component(String s, double range, boolean percentOnly) {
        if (s.endsWith("%")) {
            return clamp(number(s.substring(0, s.length() - 1).trim()) / 100.0);
        }
        if (percentOnly) {
            throw new IllegalArgumentException("Invalid color specification");
        }
        return clamp(number(s) / range);
    }

    private static double clamp(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    private static double[] hsbToRgb(double hue, double saturation, double brightness) {
        double h = hue - Math.floor(hue / 360.0) * 360.0;
        h = h / 60.0;
        int sector = (int) Math.floor(h);
        double f = h - sector;
        double p = brightness * (1 - saturation);
        double q = brightness * (1 - saturation * f);
        double t = brightness * (1 - saturation * (1 - f));
        switch (sector) {
            case 0:
                return new double[] {brightness, t, p};
            case 1:
                return new double[] {q, brightness, p};
            case 2:
                return new double[] {p, brightness, t};
            case 3:
                return new double[] {p, q, brightness};
            case 4:
                return new double[] {t, p, brightness};
            default:
                return new double[] {brightness, p, q};
        }
    }

    private double[] toHsb() {
        double max = Math.max(red, Math.max(green, blue));
        double min = Math.min(red, Math.min(green, blue));
        double brightness = max;
        double saturation = max == 0 ? 0 : (max - min) / max;
        double hue = 0;
        if (saturation != 0) {
            double delta = max - min;
            if (red == max) {
                hue = (green - blue) / delta;
            } else if (green == max) {
                hue = 2.0 + (blue - red) / delta;
            } else {
                hue = 4.0 + (red - green) / delta;
            }
            hue *= 60;
            if (hue < 0) {
                hue += 360;
            }
        }
        return new double[] {hue, saturation, brightness};
    }

    /// Returns the hue in degrees, 0 to 360.
    public double getHue() {
        return toHsb()[0];
    }

    /// Returns the saturation, 0 to 1.
    public double getSaturation() {
        return toHsb()[1];
    }

    /// Returns the brightness, 0 to 1.
    public double getBrightness() {
        return toHsb()[2];
    }

    /// Returns a colour derived by shifting the hue and scaling the
    /// saturation, brightness and opacity.
    public Color deriveColor(double hueShift, double saturationFactor, double brightnessFactor, double opacityFactor) {
        double[] hsb = toHsb();
        double b = hsb[2];
        if (b == 0 && brightnessFactor > 1.0) {
            b = 0.05;
        }
        double h = hsb[0] + hueShift;
        h = h - Math.floor(h / 360.0) * 360.0;
        double s = clamp(hsb[1] * saturationFactor);
        b = clamp(b * brightnessFactor);
        double a = clamp(opacity * opacityFactor);
        return hsb(h, s, b, a);
    }

    /// Returns a brighter version of this colour.
    public Color brighter() {
        return deriveColor(0, 1.0, 1.0 / DARKER_BRIGHTER_FACTOR, 1.0);
    }

    /// Returns a darker version of this colour.
    public Color darker() {
        return deriveColor(0, 1.0, DARKER_BRIGHTER_FACTOR, 1.0);
    }

    /// Returns a more saturated version of this colour.
    public Color saturate() {
        return deriveColor(0, 1.0 / SATURATE_DESATURATE_FACTOR, 1.0, 1.0);
    }

    /// Returns a less saturated version of this colour.
    public Color desaturate() {
        return deriveColor(0, SATURATE_DESATURATE_FACTOR, 1.0, 1.0);
    }

    /// Returns the grey of the same perceived brightness.
    public Color grayscale() {
        double gray = 0.21 * red + 0.71 * green + 0.07 * blue;
        return new Color(gray, gray, gray, opacity);
    }

    /// Returns the colour with each component inverted.
    public Color invert() {
        return new Color(1.0 - red, 1.0 - green, 1.0 - blue, opacity);
    }

    /// Returns the red component, 0 to 1.
    public final double getRed() {
        return red;
    }

    /// Returns the green component, 0 to 1.
    public final double getGreen() {
        return green;
    }

    /// Returns the blue component, 0 to 1.
    public final double getBlue() {
        return blue;
    }

    /// Returns the opacity, 0 to 1.
    public final double getOpacity() {
        return opacity;
    }

    @Override
    public final boolean isOpaque() {
        return opacity >= 1f;
    }

    @Override
    public Color interpolate(Color endValue, double t) {
        if (t <= 0.0) {
            return this;
        }
        if (t >= 1.0) {
            return endValue;
        }
        float ft = (float) t;
        return new Color(red + (endValue.red - red) * ft, green + (endValue.green - green) * ft,
                blue + (endValue.blue - blue) * ft, opacity + (endValue.opacity - opacity) * ft);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Color) {
            Color other = (Color) obj;
            return cn1Argb() == other.cn1Argb() && Float.floatToIntBits(red) == Float.floatToIntBits(other.red)
                    && Float.floatToIntBits(green) == Float.floatToIntBits(other.green)
                    && Float.floatToIntBits(blue) == Float.floatToIntBits(other.blue)
                    && Float.floatToIntBits(opacity) == Float.floatToIntBits(other.opacity);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return cn1Argb();
    }

    /// Returns the colour as `0xrrggbbaa`.
    @Override
    public String toString() {
        int argb = cn1Argb();
        String hex = "0123456789abcdef";
        StringBuilder out = new StringBuilder("0x");
        int rgba = (argb << 8) | (argb >>> 24);
        for (int shift = 28; shift >= 0; shift -= 4) {
            out.append(hex.charAt((rgba >>> shift) & 0xf));
        }
        return out.toString();
    }

    /// Returns the colour as a packed `0xAARRGGBB` integer, the form
    /// Codename One takes.
    public int cn1Argb() {
        int r = (int) Math.round(red * 255.0);
        int g = (int) Math.round(green * 255.0);
        int b = (int) Math.round(blue * 255.0);
        int a = (int) Math.round(opacity * 255.0);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
