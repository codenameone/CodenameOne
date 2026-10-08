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
package com.codename1.fxcompat.runtime.css;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/// Reads the value of one style declaration into a [CssValue].
///
/// The build uses it for every declaration of a style sheet, and the device
/// for the declarations of an inline `style` string, which is the one piece
/// of CSS that only exists at run time. One parser for both is what keeps
/// `-fx-padding: 1em 4` meaning the same thing in a style sheet and in
/// `setStyle`. It therefore uses nothing a device lacks: no regular
/// expressions, no formatting, no locale sensitive case folding.
///
/// #### The grammar, by kind of property
///
/// - paint: `#rgb`, `#rgba`, `#rrggbb`, `#rrggbbaa`, a colour name,
///   `transparent`, `rgb()`, `rgba()`, `hsb()`, `hsba()`, `hsl()`,
///   `hsla()`, `derive(colour, n%)`, `linear-gradient(...)`,
///   `radial-gradient(...)`, or the name of a looked-up colour. `ladder()`
///   is not supported.
/// - length: a number with `px`, `pt`, `pc`, `in`, `cm`, `mm`, `em`, `ex`
///   or no unit (pixels).
/// - insets and radii: one to four lengths; radii also take percentages.
/// - font: `[italic] [bold | 100..900] size family`.
/// - flags, keywords, alignments and cursors: the word.
///
/// A value in several layers (`-fx-background-color: a, b`) has only its
/// last layer kept, since a node has one background and one border; that is
/// reported through [#warning()], not as a failure.
///
/// An instance is not shared between threads; it holds the outcome of the
/// last call.
public final class CssValueParser {

    private static final HashMap<String, Integer> NAMED = new HashMap<String, Integer>();

    private static final String NAMES = "aliceblue=f0f8ff antiquewhite=faebd7 aqua=00ffff aquamarine=7fffd4 "
            + "azure=f0ffff beige=f5f5dc bisque=ffe4c4 black=000000 blanchedalmond=ffebcd blue=0000ff "
            + "blueviolet=8a2be2 brown=a52a2a burlywood=deb887 cadetblue=5f9ea0 chartreuse=7fff00 "
            + "chocolate=d2691e coral=ff7f50 cornflowerblue=6495ed cornsilk=fff8dc crimson=dc143c cyan=00ffff "
            + "darkblue=00008b darkcyan=008b8b darkgoldenrod=b8860b darkgray=a9a9a9 darkgreen=006400 "
            + "darkgrey=a9a9a9 darkkhaki=bdb76b darkmagenta=8b008b darkolivegreen=556b2f darkorange=ff8c00 "
            + "darkorchid=9932cc darkred=8b0000 darksalmon=e9967a darkseagreen=8fbc8f darkslateblue=483d8b "
            + "darkslategray=2f4f4f darkslategrey=2f4f4f darkturquoise=00ced1 darkviolet=9400d3 "
            + "deeppink=ff1493 deepskyblue=00bfff dimgray=696969 dimgrey=696969 dodgerblue=1e90ff "
            + "firebrick=b22222 floralwhite=fffaf0 forestgreen=228b22 fuchsia=ff00ff gainsboro=dcdcdc "
            + "ghostwhite=f8f8ff gold=ffd700 goldenrod=daa520 gray=808080 green=008000 greenyellow=adff2f "
            + "grey=808080 honeydew=f0fff0 hotpink=ff69b4 indianred=cd5c5c indigo=4b0082 ivory=fffff0 "
            + "khaki=f0e68c lavender=e6e6fa lavenderblush=fff0f5 lawngreen=7cfc00 lemonchiffon=fffacd "
            + "lightblue=add8e6 lightcoral=f08080 lightcyan=e0ffff lightgoldenrodyellow=fafad2 "
            + "lightgray=d3d3d3 lightgreen=90ee90 lightgrey=d3d3d3 lightpink=ffb6c1 lightsalmon=ffa07a "
            + "lightseagreen=20b2aa lightskyblue=87cefa lightslategray=778899 lightslategrey=778899 "
            + "lightsteelblue=b0c4de lightyellow=ffffe0 lime=00ff00 limegreen=32cd32 linen=faf0e6 "
            + "magenta=ff00ff maroon=800000 mediumaquamarine=66cdaa mediumblue=0000cd mediumorchid=ba55d3 "
            + "mediumpurple=9370db mediumseagreen=3cb371 mediumslateblue=7b68ee mediumspringgreen=00fa9a "
            + "mediumturquoise=48d1cc mediumvioletred=c71585 midnightblue=191970 mintcream=f5fffa "
            + "mistyrose=ffe4e1 moccasin=ffe4b5 navajowhite=ffdead navy=000080 oldlace=fdf5e6 olive=808000 "
            + "olivedrab=6b8e23 orange=ffa500 orangered=ff4500 orchid=da70d6 palegoldenrod=eee8aa "
            + "palegreen=98fb98 paleturquoise=afeeee palevioletred=db7093 papayawhip=ffefd5 peachpuff=ffdab9 "
            + "peru=cd853f pink=ffc0cb plum=dda0dd powderblue=b0e0e6 purple=800080 red=ff0000 "
            + "rosybrown=bc8f8f royalblue=4169e1 saddlebrown=8b4513 salmon=fa8072 sandybrown=f4a460 "
            + "seagreen=2e8b57 seashell=fff5ee sienna=a0522d silver=c0c0c0 skyblue=87ceeb slateblue=6a5acd "
            + "slategray=708090 slategrey=708090 snow=fffafa springgreen=00ff7f steelblue=4682b4 tan=d2b48c "
            + "teal=008080 thistle=d8bfd8 tomato=ff6347 turquoise=40e0d0 violet=ee82ee wheat=f5deb3 "
            + "white=ffffff whitesmoke=f5f5f5 yellow=ffff00 yellowgreen=9acd32";

    private static final String[] POSITIONS = {
        "top-left", "top-center", "top-right", "center-left", "center", "center-right", "bottom-left",
        "bottom-center", "bottom-right", "baseline-left", "baseline-center", "baseline-right",
    };

    private static final String[] WEIGHTS = {
        "normal", "bold", "bolder", "lighter", "thin", "extra-light", "ultra-light", "light", "medium", "semi-bold",
        "demi-bold", "extra-bold", "ultra-bold", "black", "heavy",
    };

    private static final String[] SIZE_WORDS = {
        "xx-small", "x-small", "small", "medium", "large", "x-large", "xx-large", "smaller", "larger",
    };

    private static final double[] SIZE_FACTORS = {0.6, 0.75, 0.89, 1, 1.2, 1.5, 2, 0.83, 1.2};

    static {
        int i = 0;
        int n = NAMES.length();
        while (i < n) {
            int eq = NAMES.indexOf('=', i);
            int end = NAMES.indexOf(' ', eq);
            if (end < 0) {
                end = n;
            }
            NAMED.put(NAMES.substring(i, eq), Integer.valueOf(0xff000000 | hexInt(NAMES, eq + 1, end)));
            i = end + 1;
        }
    }

    private String error;
    private String warning;

    /// Creates a parser.
    public CssValueParser() {
        // Nothing to set up: the state is the outcome of a call.
    }

    /// Why the last [#parse(String, String)] answered `null`.
    public String error() {
        return error;
    }

    /// What the last successful [#parse(String, String)] left out of the
    /// value it answered, or `null` when it kept everything.
    public String warning() {
        return warning;
    }

    /// The kind [#parse(String, String)] reads a value of `property` as:
    /// its kind in [CssProperties], or [CssProperties#LOOKUP_DEFINITION]
    /// for a name the table does not have.
    public static int kindOf(String property) {
        int kind = CssProperties.kind(property);
        return kind == CssProperties.UNKNOWN ? CssProperties.LOOKUP_DEFINITION : kind;
    }

    /// Reads `text` as a value of `property`, whose name is in lower case.
    /// Answers `null` when the text is not a value of that property, with
    /// the reason in [#error()].
    public CssValue parse(String property, String text) {
        error = null;
        warning = null;
        int kind = kindOf(property);
        String value = text == null ? "" : text.trim();
        if (value.length() == 0) {
            return fail("the value is empty");
        }
        List<String> layers = split(value, ',');
        if (layers == null) {
            return fail("unbalanced parentheses or quotes");
        }
        if (kind == CssProperties.FONT_FAMILY || kind == CssProperties.FONT) {
            // The further families are fallbacks; a device has one font per name.
            value = layers.get(0);
        } else if (layers.size() > 1) {
            if (kind != CssProperties.PAINT && kind != CssProperties.INSETS && kind != CssProperties.RADII
                    && kind != CssProperties.LOOKUP_DEFINITION) {
                return fail("a list of values is not accepted here");
            }
            warning = "layered values are not supported; only the last layer is used";
            value = layers.get(layers.size() - 1);
        }
        List<String> terms = split(value, ' ');
        if (terms == null || terms.isEmpty()) {
            return fail("the value is empty");
        }
        String first = terms.get(0);
        switch (kind) {
            case CssProperties.LOOKUP_DEFINITION: {
                if (terms.size() != 1) {
                    return fail("not a known property, and not a colour that could be looked up");
                }
                CssValue paint = paint(first);
                if (paint == null) {
                    error = "not a known property, and its value is not a colour that could be looked up";
                }
                return paint;
            }
            case CssProperties.PAINT:
                if (terms.size() != 1 && terms.size() != 4) {
                    return fail("expected one paint");
                }
                if (terms.size() == 4) {
                    warning = "a paint per side is not supported; the first is used for every side";
                }
                return paint(first);
            case CssProperties.LENGTH: {
                CssValue length = one(terms) ? length(first, false) : null;
                return length == null ? fail("expected a length") : length;
            }
            case CssProperties.NUMBER: {
                CssValue number = one(terms) ? number(first) : null;
                return number == null ? fail("expected a number") : number;
            }
            case CssProperties.INSETS:
                return sizes(terms, false);
            case CssProperties.RADII:
                return sizes(terms, true);
            case CssProperties.BOOLEAN: {
                String word = CssProperties.lower(first);
                if (!one(terms) || !("true".equals(word) || "false".equals(word))) {
                    return fail("expected true or false");
                }
                return CssValue.bool("true".equals(word));
            }
            case CssProperties.KEYWORD: {
                String word = CssProperties.lower(first);
                if (!CssProperties.accepts(property, word)) {
                    return fail("'" + first + "' is not a value of " + property);
                }
                return CssValue.text(CssValue.KEYWORD, word);
            }
            case CssProperties.POS:
                return word(terms, POSITIONS, "an alignment");
            case CssProperties.CURSOR: {
                if (!one(terms) || !isIdent(first)) {
                    return fail("expected a cursor name");
                }
                return CssValue.text(CssValue.KEYWORD, CssProperties.lower(first));
            }
            case CssProperties.FONT:
                return font(terms);
            case CssProperties.FONT_SIZE: {
                CssValue size = one(terms) ? fontSize(first) : null;
                return size == null ? fail("expected a font size") : size;
            }
            case CssProperties.FONT_FAMILY:
                return CssValue.text(CssValue.STRING, family(terms, 0));
            case CssProperties.FONT_WEIGHT: {
                CssValue weight = one(terms) ? weight(first) : null;
                return weight == null ? fail("expected a font weight") : weight;
            }
            case CssProperties.FONT_STYLE: {
                String word = CssProperties.lower(first);
                if (!one(terms) || !("normal".equals(word) || "italic".equals(word) || "oblique".equals(word))) {
                    return fail("expected normal, italic or oblique");
                }
                return CssValue.text(CssValue.KEYWORD, word);
            }
            default:
                return fail("unsupported property");
        }
    }

    /// Reads a colour that needs no node to resolve -- a name, a hash form
    /// or one of the colour functions -- as `0xAARRGGBB`. Answers `null`
    /// for anything else, a looked-up name included.
    public Integer constantColor(String text) {
        CssValue v = text == null ? null : paint(text.trim());
        return v != null && v.type() == CssValue.COLOR ? Integer.valueOf(v.flags()) : null;
    }

    private CssValue fail(String message) {
        error = message;
        return null;
    }

    private static boolean one(List<String> terms) {
        return terms.size() == 1;
    }

    private CssValue word(List<String> terms, String[] words, String what) {
        String word = CssProperties.lower(terms.get(0));
        if (one(terms)) {
            for (int i = 0; i < words.length; i++) {
                if (words[i].equals(word)) {
                    return CssValue.text(CssValue.KEYWORD, word);
                }
            }
        }
        return fail("expected " + what);
    }

    // ------------------------------------------------------------ splitting

    /// Splits at `separator` outside quotes and parentheses; a space stands
    /// for any run of white space. Answers `null` when a quote or a
    /// parenthesis is left open.
    static List<String> split(String s, char separator) {
        ArrayList<String> out = new ArrayList<String>();
        int depth = 0;
        char quote = 0;
        int start = 0;
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if (quote != 0) {
                if (c == '\\') {
                    i++;
                } else if (c == quote) {
                    quote = 0;
                }
                continue;
            }
            if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth < 0) {
                    return null;
                }
            } else if (depth == 0 && (c == separator || (separator == ' ' && isSpace(c)))) {
                addTerm(out, s, start, i, separator);
                start = i + 1;
            }
        }
        if (depth != 0 || quote != 0) {
            return null;
        }
        addTerm(out, s, start, n, separator);
        return out;
    }

    private static void addTerm(List<String> out, String s, int start, int end, char separator) {
        String term = s.substring(start, end).trim();
        // An empty term between spaces is a run of spaces; between commas it
        // is a mistake the caller reports.
        if (term.length() > 0 || separator != ' ') {
            out.add(term);
        }
    }

    static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
    }

    private static boolean isIdent(String s) {
        if (s.length() == 0) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean letter = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '-';
            if (!letter && !(i > 0 && c >= '0' && c <= '9')) {
                return false;
            }
        }
        return true;
    }

    // -------------------------------------------------------------- numbers

    /// The number a term starts with, and where it ends, in `end[0]`; NaN
    /// when the term does not start with one.
    private static double numberPrefix(String s, int[] end) {
        int n = s.length();
        int i = 0;
        if (i < n && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
            i++;
        }
        int digits = 0;
        while (i < n && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
            i++;
            digits++;
        }
        if (i < n && s.charAt(i) == '.') {
            i++;
            while (i < n && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
                i++;
                digits++;
            }
        }
        end[0] = i;
        if (digits == 0) {
            return Double.NaN;
        }
        String text = s.charAt(0) == '+' ? s.substring(1, i) : s.substring(0, i);
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    /// A number with a length unit, a percentage when `percent` allows it.
    private static CssValue length(String term, boolean percent) {
        // "-fx-max-width: infinity" is how a style sheet lets a control grow.
        if ("infinity".equalsIgnoreCase(term)) {
            return CssValue.number(Double.MAX_VALUE, CssValue.UNIT_PX);
        }
        if ("-infinity".equalsIgnoreCase(term)) {
            return CssValue.number(-Double.MAX_VALUE, CssValue.UNIT_PX);
        }
        int[] end = new int[1];
        double v = numberPrefix(term, end);
        if (v != v) {
            return null;
        }
        String unit = CssProperties.lower(term.substring(end[0]));
        if (unit.length() == 0 || "px".equals(unit)) {
            return CssValue.number(v, CssValue.UNIT_PX);
        } else if ("em".equals(unit)) {
            return CssValue.number(v, CssValue.UNIT_EM);
        } else if ("ex".equals(unit)) {
            return CssValue.number(v / 2, CssValue.UNIT_EM);
        } else if ("pt".equals(unit)) {
            return CssValue.number(v * 4 / 3, CssValue.UNIT_PX);
        } else if ("pc".equals(unit)) {
            return CssValue.number(v * 16, CssValue.UNIT_PX);
        } else if ("in".equals(unit)) {
            return CssValue.number(v * 96, CssValue.UNIT_PX);
        } else if ("cm".equals(unit)) {
            return CssValue.number(v * 96 / 2.54, CssValue.UNIT_PX);
        } else if ("mm".equals(unit)) {
            return CssValue.number(v * 96 / 25.4, CssValue.UNIT_PX);
        } else if (percent && "%".equals(unit)) {
            return CssValue.number(v, CssValue.UNIT_PERCENT);
        }
        return null;
    }

    /// A number without a unit; an angle is converted to degrees.
    private static CssValue number(String term) {
        int[] end = new int[1];
        double v = numberPrefix(term, end);
        if (v != v) {
            return null;
        }
        String unit = CssProperties.lower(term.substring(end[0]));
        if (unit.length() == 0 || "deg".equals(unit) || "px".equals(unit)) {
            return CssValue.number(v, CssValue.UNIT_NONE);
        } else if ("rad".equals(unit)) {
            return CssValue.number(v * 180 / Math.PI, CssValue.UNIT_NONE);
        } else if ("grad".equals(unit)) {
            return CssValue.number(v * 0.9, CssValue.UNIT_NONE);
        } else if ("turn".equals(unit)) {
            return CssValue.number(v * 360, CssValue.UNIT_NONE);
        }
        return null;
    }

    private CssValue sizes(List<String> terms, boolean radii) {
        int count = terms.size();
        for (int i = 0; i < count; i++) {
            if ("/".equals(terms.get(i))) {
                // Vertical radii: a node has one radius per corner.
                warning = "elliptical radii are not supported; the horizontal radii are used";
                count = i;
            }
        }
        if (count < 1 || count > 4) {
            return fail("expected one to four " + (radii ? "radii" : "lengths"));
        }
        double[] nums = new double[count];
        byte[] units = new byte[count];
        for (int i = 0; i < count; i++) {
            CssValue v = length(terms.get(i), radii);
            if (v == null) {
                return fail("'" + terms.get(i) + "' is not a " + (radii ? "radius" : "length"));
            }
            nums[i] = v.num(0);
            units[i] = v.unit(0);
        }
        return new CssValue(CssValue.SIZES, 0, nums, units, null, null);
    }

    // ---------------------------------------------------------------- fonts

    private static CssValue fontSize(String term) {
        String word = CssProperties.lower(term);
        for (int i = 0; i < SIZE_WORDS.length; i++) {
            if (SIZE_WORDS[i].equals(word)) {
                return CssValue.number(SIZE_FACTORS[i], CssValue.UNIT_EM);
            }
        }
        return length(term, true);
    }

    private static CssValue weight(String term) {
        String word = CssProperties.lower(term);
        for (int i = 0; i < WEIGHTS.length; i++) {
            if (WEIGHTS[i].equals(word)) {
                return CssValue.text(CssValue.KEYWORD, word);
            }
        }
        int[] end = new int[1];
        double v = numberPrefix(term, end);
        if (v == v && end[0] == term.length() && v >= 100 && v <= 900) {
            return CssValue.number(v, CssValue.UNIT_NONE);
        }
        return null;
    }

    private static int weightNumber(CssValue weight) {
        if (weight.type() == CssValue.NUMBER) {
            return (int) weight.num(0);
        }
        String w = weight.text();
        if ("bold".equals(w) || "bolder".equals(w)) {
            return 700;
        } else if ("thin".equals(w)) {
            return 100;
        } else if ("extra-light".equals(w) || "ultra-light".equals(w)) {
            return 200;
        } else if ("light".equals(w) || "lighter".equals(w)) {
            return 300;
        } else if ("medium".equals(w)) {
            return 500;
        } else if ("semi-bold".equals(w) || "demi-bold".equals(w)) {
            return 600;
        } else if ("extra-bold".equals(w) || "ultra-bold".equals(w)) {
            return 800;
        } else if ("black".equals(w) || "heavy".equals(w)) {
            return 900;
        }
        return 400;
    }

    private static String family(List<String> terms, int from) {
        StringBuilder s = new StringBuilder();
        for (int i = from; i < terms.size(); i++) {
            if (s.length() > 0) {
                s.append(' ');
            }
            s.append(terms.get(i));
        }
        return unquote(s.toString());
    }

    /// `s` without the quotes around it, and with its backslash escapes
    /// taken.
    static String unquote(String s) {
        int n = s.length();
        if (n >= 2 && (s.charAt(0) == '"' || s.charAt(0) == '\'') && s.charAt(n - 1) == s.charAt(0)) {
            StringBuilder out = new StringBuilder();
            for (int i = 1; i < n - 1; i++) {
                char c = s.charAt(i);
                if (c == '\\' && i + 1 < n - 1) {
                    i++;
                    c = s.charAt(i);
                }
                out.append(c);
            }
            return out.toString();
        }
        return s;
    }

    private CssValue font(List<String> terms) {
        int i = 0;
        int posture = 0;
        int weight = 0;
        CssValue size = null;
        while (i < terms.size() && size == null) {
            String term = terms.get(i);
            String word = CssProperties.lower(term);
            // The line height of "12px/1.5" has nothing to apply to.
            int slash = term.indexOf('/');
            String sizeTerm = slash > 0 ? term.substring(0, slash) : term;
            boolean bareNumber = weight(term) != null && weight(term).type() == CssValue.NUMBER;
            if ("italic".equals(word) || "oblique".equals(word)) {
                posture = 2;
            } else if ("normal".equals(word)) {
                // Stands for whichever of posture and weight was left out.
                posture = posture == 0 ? 1 : posture;
            } else if (weight(term) != null && !bareNumber) {
                weight = weightNumber(weight(term));
            } else if (bareNumber && i + 2 < terms.size() && fontSize(terms.get(i + 1)) != null) {
                // "600 12px Arial": a bare number before a size is a weight.
                weight = weightNumber(weight(term));
            } else {
                size = fontSize(sizeTerm);
                if (size == null) {
                    return fail("'" + term + "' is not a font style, weight or size");
                }
            }
            i++;
        }
        if (size == null || i >= terms.size()) {
            return fail("a font needs a size and a family");
        }
        return new CssValue(CssValue.FONT, weight | (posture << 16), new double[] {size.num(0)},
                new byte[] {size.unit(0)}, family(terms, i), null);
    }

    // --------------------------------------------------------------- paints

    private CssValue paint(String term) {
        if (term.length() == 0) {
            return fail("expected a paint");
        }
        if (term.charAt(0) == '#') {
            if (!isHashColor(term)) {
                return fail("'" + term + "' is not a colour");
            }
            return CssValue.color(hashColor(term));
        }
        int open = term.indexOf('(');
        if (open > 0 && term.charAt(term.length() - 1) == ')') {
            String name = CssProperties.lower(term.substring(0, open).trim());
            List<String> args = split(term.substring(open + 1, term.length() - 1), ',');
            if (args == null) {
                return fail("unbalanced parentheses in '" + term + "'");
            }
            if ("rgb".equals(name) || "rgba".equals(name)) {
                return rgb(args, term);
            } else if ("hsb".equals(name) || "hsba".equals(name)) {
                return hsx(args, term, true);
            } else if ("hsl".equals(name) || "hsla".equals(name)) {
                return hsx(args, term, false);
            } else if ("derive".equals(name)) {
                return derive(args, term);
            } else if ("linear-gradient".equals(name)) {
                return linear(args);
            } else if ("radial-gradient".equals(name)) {
                return radial(args);
            } else if ("ladder".equals(name)) {
                return fail("ladder() is not supported");
            }
            return fail("'" + name + "()' is not a paint");
        }
        if (!isIdent(term)) {
            return fail("'" + term + "' is not a paint");
        }
        String word = CssProperties.lower(term);
        if ("transparent".equals(word)) {
            return CssValue.color(0);
        }
        Integer named = NAMED.get(word);
        if (named != null) {
            return CssValue.color(named.intValue());
        }
        return CssValue.text(CssValue.LOOKUP, word);
    }

    private static int hexInt(String s, int from, int to) {
        int v = 0;
        for (int i = from; i < to; i++) {
            char c = s.charAt(i);
            int d;
            if (c >= '0' && c <= '9') {
                d = c - '0';
            } else if (c >= 'a' && c <= 'f') {
                d = c - 'a' + 10;
            } else if (c >= 'A' && c <= 'F') {
                d = c - 'A' + 10;
            } else {
                return -1;
            }
            v = (v << 4) | d;
        }
        return v;
    }

    private static boolean isHashColor(String term) {
        int n = term.length() - 1;
        if (n != 3 && n != 4 && n != 6 && n != 8) {
            return false;
        }
        for (int i = 1; i <= n; i++) {
            if (hexInt(term, i, i + 1) < 0) {
                return false;
            }
        }
        return true;
    }

    /// The colour of `#rgb`, `#rgba`, `#rrggbb` or `#rrggbbaa`, which the
    /// caller has checked `term` to be.
    private static int hashColor(String term) {
        int n = term.length() - 1;
        int r;
        int g;
        int b;
        int a = 255;
        if (n <= 4) {
            r = hexInt(term, 1, 2) * 17;
            g = hexInt(term, 2, 3) * 17;
            b = hexInt(term, 3, 4) * 17;
            if (n == 4) {
                a = hexInt(term, 4, 5) * 17;
            }
        } else {
            r = hexInt(term, 1, 3);
            g = hexInt(term, 3, 5);
            b = hexInt(term, 5, 7);
            if (n == 8) {
                a = hexInt(term, 7, 9);
            }
        }
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int channel(double v) {
        int i = (int) Math.floor(v + 0.5);
        return i < 0 ? 0 : i > 255 ? 255 : i;
    }

    private static int argb(double r, double g, double b, double a) {
        return (channel(a * 255) << 24) | (channel(r * 255) << 16) | (channel(g * 255) << 8) | channel(b * 255);
    }

    /// A plain number or a percentage of `whole`; NaN for anything else.
    private static double component(String term, double whole) {
        int[] end = new int[1];
        double v = numberPrefix(term, end);
        String rest = term.substring(end[0]);
        if (v != v) {
            return Double.NaN;
        }
        if ("%".equals(rest)) {
            return v * whole / 100;
        }
        return rest.length() == 0 ? v : Double.NaN;
    }

    private static double alpha(List<String> args) {
        if (args.size() == 3) {
            return 1;
        }
        double a = component(args.get(3), 1);
        return a < 0 ? 0 : a > 1 ? 1 : a;
    }

    private CssValue rgb(List<String> args, String term) {
        if (args.size() != 3 && args.size() != 4) {
            return fail("'" + term + "' needs three or four components");
        }
        double r = component(args.get(0), 255);
        double g = component(args.get(1), 255);
        double b = component(args.get(2), 255);
        double a = alpha(args);
        if (r != r || g != g || b != b || a != a) {
            return fail("'" + term + "' is not a colour");
        }
        return CssValue.color(argb(r / 255, g / 255, b / 255, a));
    }

    private CssValue hsx(List<String> args, String term, boolean hsb) {
        if (args.size() != 3 && args.size() != 4) {
            return fail("'" + term + "' needs three or four components");
        }
        double h = component(args.get(0), 360);
        double s = component(args.get(1), 1);
        double v = component(args.get(2), 1);
        double a = alpha(args);
        if (h != h || s != s || v != v || a != a) {
            return fail("'" + term + "' is not a colour");
        }
        s = s < 0 ? 0 : s > 1 ? 1 : s;
        v = v < 0 ? 0 : v > 1 ? 1 : v;
        if (!hsb) {
            // HSL to HSB: the same hue, the lightness spread differently.
            double brightness = v + s * (v < 0.5 ? v : 1 - v);
            s = brightness == 0 ? 0 : 2 * (1 - v / brightness);
            v = brightness;
        }
        h = h - 360 * Math.floor(h / 360);
        double sector = h / 60;
        int i = (int) Math.floor(sector);
        double f = sector - i;
        double p = v * (1 - s);
        double q = v * (1 - s * f);
        double t = v * (1 - s * (1 - f));
        double r;
        double g;
        double b;
        switch (i % 6) {
            case 0:
                r = v;
                g = t;
                b = p;
                break;
            case 1:
                r = q;
                g = v;
                b = p;
                break;
            case 2:
                r = p;
                g = v;
                b = t;
                break;
            case 3:
                r = p;
                g = q;
                b = v;
                break;
            case 4:
                r = t;
                g = p;
                b = v;
                break;
            default:
                r = v;
                g = p;
                b = q;
                break;
        }
        return CssValue.color(argb(r, g, b, a));
    }

    private CssValue colorOnly(String term) {
        CssValue v = paint(term);
        if (v != null && v.type() != CssValue.COLOR && v.type() != CssValue.LOOKUP && v.type() != CssValue.DERIVE) {
            return fail("'" + term + "' is not a colour");
        }
        return v;
    }

    private CssValue derive(List<String> args, String term) {
        if (args.size() != 2) {
            return fail("'" + term + "' needs a colour and a percentage");
        }
        CssValue base = colorOnly(args.get(0));
        if (base == null) {
            return null;
        }
        String amount = args.get(1);
        double pct = amount.endsWith("%") ? component(amount, 100) : Double.NaN;
        if (pct != pct) {
            return fail("'" + amount + "' is not a percentage");
        }
        return new CssValue(CssValue.DERIVE, 0, new double[] {pct}, null, null, new CssValue[] {base});
    }

    /// Reads the stops from `args[from]` on into `nums` from `at` on, and
    /// answers their colours; `null` after a failure.
    private CssValue[] stops(List<String> args, int from, double[] nums, int at) {
        int count = args.size() - from;
        if (count < 2) {
            fail("a gradient needs at least two stops");
            return null;
        }
        CssValue[] colors = new CssValue[count];
        for (int i = 0; i < count; i++) {
            List<String> terms = split(args.get(from + i), ' ');
            if (terms == null || terms.isEmpty() || terms.size() > 2) {
                fail("'" + args.get(from + i) + "' is not a gradient stop");
                return null;
            }
            colors[i] = colorOnly(terms.get(0));
            if (colors[i] == null) {
                return null;
            }
            double offset = Double.NaN;
            if (terms.size() == 2) {
                String o = terms.get(1);
                offset = o.endsWith("%") ? component(o, 1) : Double.NaN;
                if (offset != offset) {
                    fail("a stop offset must be a percentage, not '" + o + "'");
                    return null;
                }
            }
            nums[at + i] = offset;
        }
        return colors;
    }

    /// Reads a point as two lengths or two percentages into `out[at]` and
    /// `out[at + 1]`; answers 1 for percentages, 0 for lengths and -1 for
    /// anything else.
    private static int point(String x, String y, double[] out, int at) {
        CssValue px = length(x, true);
        CssValue py = length(y, true);
        if (px == null || py == null || px.unit(0) == CssValue.UNIT_EM || py.unit(0) == CssValue.UNIT_EM) {
            return -1;
        }
        boolean percentX = px.unit(0) == CssValue.UNIT_PERCENT;
        if (percentX != (py.unit(0) == CssValue.UNIT_PERCENT)) {
            return -1;
        }
        out[at] = percentX ? px.num(0) / 100 : px.num(0);
        out[at + 1] = percentX ? py.num(0) / 100 : py.num(0);
        return percentX ? 1 : 0;
    }

    private static int cycle(String arg) {
        String word = CssProperties.lower(arg);
        return "repeat".equals(word) ? CssValue.FLAG_REPEAT : "reflect".equals(word) ? CssValue.FLAG_REFLECT : -1;
    }

    private CssValue linear(List<String> args) {
        int next = 0;
        double[] geometry = {0, 0, 0, 1};
        int flags = CssValue.FLAG_PROPORTIONAL;
        List<String> head = args.isEmpty() ? null : split(args.get(0), ' ');
        if (head != null && !head.isEmpty()) {
            String lead = CssProperties.lower(head.get(0));
            if ("from".equals(lead)) {
                if (head.size() != 6 || !"to".equals(CssProperties.lower(head.get(3)))) {
                    return fail("expected 'from x y to x y'");
                }
                int a = point(head.get(1), head.get(2), geometry, 0);
                int b = point(head.get(4), head.get(5), geometry, 2);
                if (a < 0 || a != b) {
                    return fail("the points of a gradient are all lengths or all percentages");
                }
                flags = a == 1 ? CssValue.FLAG_PROPORTIONAL : 0;
                next = 1;
            } else if ("to".equals(lead)) {
                geometry[3] = 0;
                for (int i = 1; i < head.size(); i++) {
                    String side = CssProperties.lower(head.get(i));
                    if ("top".equals(side)) {
                        geometry[1] = 1;
                        geometry[3] = 0;
                    } else if ("bottom".equals(side)) {
                        geometry[1] = 0;
                        geometry[3] = 1;
                    } else if ("left".equals(side)) {
                        geometry[0] = 1;
                        geometry[2] = 0;
                    } else if ("right".equals(side)) {
                        geometry[0] = 0;
                        geometry[2] = 1;
                    } else {
                        return fail("'" + head.get(i) + "' is not a side");
                    }
                }
                if (head.size() < 2 || head.size() > 3) {
                    return fail("expected 'to' and one or two sides");
                }
                next = 1;
            }
        }
        if (next < args.size() && cycle(args.get(next)) >= 0) {
            flags |= cycle(args.get(next));
            next++;
        }
        double[] nums = new double[4 + Math.max(0, args.size() - next)];
        System.arraycopy(geometry, 0, nums, 0, 4);
        CssValue[] colors = stops(args, next, nums, 4);
        return colors == null ? null : new CssValue(CssValue.LINEAR, flags, nums, null, null, colors);
    }

    private CssValue radial(List<String> args) {
        int next = 0;
        double[] geometry = {0, 0, 0.5, 0.5, 0.5};
        int centre = -1;
        int radius = -1;
        while (next < args.size()) {
            List<String> terms = split(args.get(next), ' ');
            String lead = terms == null || terms.isEmpty() ? "" : CssProperties.lower(terms.get(0));
            if ("focus-angle".equals(lead) && terms.size() == 2) {
                CssValue angle = number(terms.get(1));
                if (angle == null) {
                    return fail("'" + terms.get(1) + "' is not an angle");
                }
                geometry[0] = angle.num(0);
            } else if ("focus-distance".equals(lead) && terms.size() == 2) {
                double d = terms.get(1).endsWith("%") ? component(terms.get(1), 1) : Double.NaN;
                if (d != d) {
                    return fail("the focus distance is a percentage");
                }
                geometry[1] = d;
            } else if ("center".equals(lead) && terms.size() == 3) {
                centre = point(terms.get(1), terms.get(2), geometry, 2);
                if (centre < 0) {
                    return fail("the centre is two lengths or two percentages");
                }
            } else if ("radius".equals(lead) && terms.size() == 2) {
                CssValue r = length(terms.get(1), true);
                if (r == null || r.unit(0) == CssValue.UNIT_EM) {
                    return fail("'" + terms.get(1) + "' is not a radius");
                }
                radius = r.unit(0) == CssValue.UNIT_PERCENT ? 1 : 0;
                geometry[4] = radius == 1 ? r.num(0) / 100 : r.num(0);
            } else {
                break;
            }
            next++;
        }
        if (radius < 0) {
            return fail("a radial gradient needs a radius");
        }
        if (centre >= 0 && centre != radius || centre < 0 && radius == 0) {
            return fail("the centre and the radius are all lengths or all percentages");
        }
        int flags = radius == 1 ? CssValue.FLAG_PROPORTIONAL : 0;
        if (next < args.size() && cycle(args.get(next)) >= 0) {
            flags |= cycle(args.get(next));
            next++;
        }
        double[] nums = new double[5 + Math.max(0, args.size() - next)];
        System.arraycopy(geometry, 0, nums, 0, 5);
        CssValue[] colors = stops(args, next, nums, 5);
        return colors == null ? null : new CssValue(CssValue.RADIAL, flags, nums, null, null, colors);
    }
}
