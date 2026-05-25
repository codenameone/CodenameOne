package com.codename1.svg.transcoder.parser;

import java.util.HashMap;
import java.util.Map;

/**
 * Parses CSS-style color values used by SVG: #RGB, #RRGGBB, rgb(r,g,b),
 * rgba(r,g,b,a), or one of the named CSS colors. Returns an ARGB int.
 *
 * "none" and "currentColor" do not return a color — callers must check for
 * them up front via {@link #isNone(String)} and {@link #isCurrentColor(String)}.
 */
public final class ColorParser {

    public static final int TRANSPARENT = 0x00000000;
    public static final int BLACK = 0xFF000000;

    private static final Map<String, Integer> NAMED = new HashMap<String, Integer>();

    static {
        // CSS color names (subset of the SVG 1.1 named-color list, covers the common ones)
        NAMED.put("aliceblue", 0xFFF0F8FF);
        NAMED.put("antiquewhite", 0xFFFAEBD7);
        NAMED.put("aqua", 0xFF00FFFF);
        NAMED.put("aquamarine", 0xFF7FFFD4);
        NAMED.put("azure", 0xFFF0FFFF);
        NAMED.put("beige", 0xFFF5F5DC);
        NAMED.put("bisque", 0xFFFFE4C4);
        NAMED.put("black", 0xFF000000);
        NAMED.put("blanchedalmond", 0xFFFFEBCD);
        NAMED.put("blue", 0xFF0000FF);
        NAMED.put("blueviolet", 0xFF8A2BE2);
        NAMED.put("brown", 0xFFA52A2A);
        NAMED.put("burlywood", 0xFFDEB887);
        NAMED.put("cadetblue", 0xFF5F9EA0);
        NAMED.put("chartreuse", 0xFF7FFF00);
        NAMED.put("chocolate", 0xFFD2691E);
        NAMED.put("coral", 0xFFFF7F50);
        NAMED.put("cornflowerblue", 0xFF6495ED);
        NAMED.put("cornsilk", 0xFFFFF8DC);
        NAMED.put("crimson", 0xFFDC143C);
        NAMED.put("cyan", 0xFF00FFFF);
        NAMED.put("darkblue", 0xFF00008B);
        NAMED.put("darkcyan", 0xFF008B8B);
        NAMED.put("darkgoldenrod", 0xFFB8860B);
        NAMED.put("darkgray", 0xFFA9A9A9);
        NAMED.put("darkgrey", 0xFFA9A9A9);
        NAMED.put("darkgreen", 0xFF006400);
        NAMED.put("darkkhaki", 0xFFBDB76B);
        NAMED.put("darkmagenta", 0xFF8B008B);
        NAMED.put("darkolivegreen", 0xFF556B2F);
        NAMED.put("darkorange", 0xFFFF8C00);
        NAMED.put("darkorchid", 0xFF9932CC);
        NAMED.put("darkred", 0xFF8B0000);
        NAMED.put("darksalmon", 0xFFE9967A);
        NAMED.put("darkseagreen", 0xFF8FBC8F);
        NAMED.put("darkslateblue", 0xFF483D8B);
        NAMED.put("darkslategray", 0xFF2F4F4F);
        NAMED.put("darkslategrey", 0xFF2F4F4F);
        NAMED.put("darkturquoise", 0xFF00CED1);
        NAMED.put("darkviolet", 0xFF9400D3);
        NAMED.put("deeppink", 0xFFFF1493);
        NAMED.put("deepskyblue", 0xFF00BFFF);
        NAMED.put("dimgray", 0xFF696969);
        NAMED.put("dimgrey", 0xFF696969);
        NAMED.put("dodgerblue", 0xFF1E90FF);
        NAMED.put("firebrick", 0xFFB22222);
        NAMED.put("floralwhite", 0xFFFFFAF0);
        NAMED.put("forestgreen", 0xFF228B22);
        NAMED.put("fuchsia", 0xFFFF00FF);
        NAMED.put("gainsboro", 0xFFDCDCDC);
        NAMED.put("ghostwhite", 0xFFF8F8FF);
        NAMED.put("gold", 0xFFFFD700);
        NAMED.put("goldenrod", 0xFFDAA520);
        NAMED.put("gray", 0xFF808080);
        NAMED.put("grey", 0xFF808080);
        NAMED.put("green", 0xFF008000);
        NAMED.put("greenyellow", 0xFFADFF2F);
        NAMED.put("honeydew", 0xFFF0FFF0);
        NAMED.put("hotpink", 0xFFFF69B4);
        NAMED.put("indianred", 0xFFCD5C5C);
        NAMED.put("indigo", 0xFF4B0082);
        NAMED.put("ivory", 0xFFFFFFF0);
        NAMED.put("khaki", 0xFFF0E68C);
        NAMED.put("lavender", 0xFFE6E6FA);
        NAMED.put("lavenderblush", 0xFFFFF0F5);
        NAMED.put("lawngreen", 0xFF7CFC00);
        NAMED.put("lemonchiffon", 0xFFFFFACD);
        NAMED.put("lightblue", 0xFFADD8E6);
        NAMED.put("lightcoral", 0xFFF08080);
        NAMED.put("lightcyan", 0xFFE0FFFF);
        NAMED.put("lightgoldenrodyellow", 0xFFFAFAD2);
        NAMED.put("lightgray", 0xFFD3D3D3);
        NAMED.put("lightgrey", 0xFFD3D3D3);
        NAMED.put("lightgreen", 0xFF90EE90);
        NAMED.put("lightpink", 0xFFFFB6C1);
        NAMED.put("lightsalmon", 0xFFFFA07A);
        NAMED.put("lightseagreen", 0xFF20B2AA);
        NAMED.put("lightskyblue", 0xFF87CEFA);
        NAMED.put("lightslategray", 0xFF778899);
        NAMED.put("lightslategrey", 0xFF778899);
        NAMED.put("lightsteelblue", 0xFFB0C4DE);
        NAMED.put("lightyellow", 0xFFFFFFE0);
        NAMED.put("lime", 0xFF00FF00);
        NAMED.put("limegreen", 0xFF32CD32);
        NAMED.put("linen", 0xFFFAF0E6);
        NAMED.put("magenta", 0xFFFF00FF);
        NAMED.put("maroon", 0xFF800000);
        NAMED.put("mediumaquamarine", 0xFF66CDAA);
        NAMED.put("mediumblue", 0xFF0000CD);
        NAMED.put("mediumorchid", 0xFFBA55D3);
        NAMED.put("mediumpurple", 0xFF9370DB);
        NAMED.put("mediumseagreen", 0xFF3CB371);
        NAMED.put("mediumslateblue", 0xFF7B68EE);
        NAMED.put("mediumspringgreen", 0xFF00FA9A);
        NAMED.put("mediumturquoise", 0xFF48D1CC);
        NAMED.put("mediumvioletred", 0xFFC71585);
        NAMED.put("midnightblue", 0xFF191970);
        NAMED.put("mintcream", 0xFFF5FFFA);
        NAMED.put("mistyrose", 0xFFFFE4E1);
        NAMED.put("moccasin", 0xFFFFE4B5);
        NAMED.put("navajowhite", 0xFFFFDEAD);
        NAMED.put("navy", 0xFF000080);
        NAMED.put("oldlace", 0xFFFDF5E6);
        NAMED.put("olive", 0xFF808000);
        NAMED.put("olivedrab", 0xFF6B8E23);
        NAMED.put("orange", 0xFFFFA500);
        NAMED.put("orangered", 0xFFFF4500);
        NAMED.put("orchid", 0xFFDA70D6);
        NAMED.put("palegoldenrod", 0xFFEEE8AA);
        NAMED.put("palegreen", 0xFF98FB98);
        NAMED.put("paleturquoise", 0xFFAFEEEE);
        NAMED.put("palevioletred", 0xFFDB7093);
        NAMED.put("papayawhip", 0xFFFFEFD5);
        NAMED.put("peachpuff", 0xFFFFDAB9);
        NAMED.put("peru", 0xFFCD853F);
        NAMED.put("pink", 0xFFFFC0CB);
        NAMED.put("plum", 0xFFDDA0DD);
        NAMED.put("powderblue", 0xFFB0E0E6);
        NAMED.put("purple", 0xFF800080);
        NAMED.put("rebeccapurple", 0xFF663399);
        NAMED.put("red", 0xFFFF0000);
        NAMED.put("rosybrown", 0xFFBC8F8F);
        NAMED.put("royalblue", 0xFF4169E1);
        NAMED.put("saddlebrown", 0xFF8B4513);
        NAMED.put("salmon", 0xFFFA8072);
        NAMED.put("sandybrown", 0xFFF4A460);
        NAMED.put("seagreen", 0xFF2E8B57);
        NAMED.put("seashell", 0xFFFFF5EE);
        NAMED.put("sienna", 0xFFA0522D);
        NAMED.put("silver", 0xFFC0C0C0);
        NAMED.put("skyblue", 0xFF87CEEB);
        NAMED.put("slateblue", 0xFF6A5ACD);
        NAMED.put("slategray", 0xFF708090);
        NAMED.put("slategrey", 0xFF708090);
        NAMED.put("snow", 0xFFFFFAFA);
        NAMED.put("springgreen", 0xFF00FF7F);
        NAMED.put("steelblue", 0xFF4682B4);
        NAMED.put("tan", 0xFFD2B48C);
        NAMED.put("teal", 0xFF008080);
        NAMED.put("thistle", 0xFFD8BFD8);
        NAMED.put("tomato", 0xFFFF6347);
        NAMED.put("transparent", 0x00000000);
        NAMED.put("turquoise", 0xFF40E0D0);
        NAMED.put("violet", 0xFFEE82EE);
        NAMED.put("wheat", 0xFFF5DEB3);
        NAMED.put("white", 0xFFFFFFFF);
        NAMED.put("whitesmoke", 0xFFF5F5F5);
        NAMED.put("yellow", 0xFFFFFF00);
        NAMED.put("yellowgreen", 0xFF9ACD32);
    }

    private ColorParser() { }

    public static boolean isNone(String value) {
        return value != null && "none".equalsIgnoreCase(value.trim());
    }

    public static boolean isCurrentColor(String value) {
        return value != null && "currentColor".equalsIgnoreCase(value.trim());
    }

    /**
     * Parse a color value. Returns ARGB int with alpha set to 0xFF for opaque
     * formats. Throws IllegalArgumentException for unknown values.
     */
    public static int parse(String value) {
        if (value == null) throw new IllegalArgumentException("null color");
        String v = value.trim();
        if (v.isEmpty()) throw new IllegalArgumentException("empty color");
        if (v.charAt(0) == '#') return parseHex(v);
        if (v.startsWith("rgb")) return parseRgb(v);
        Integer named = NAMED.get(v.toLowerCase());
        if (named != null) return named;
        throw new IllegalArgumentException("Unrecognized color: " + value);
    }

    /** Same as {@link #parse} but returns {@code fallback} on unknown input. */
    public static int parseOrDefault(String value, int fallback) {
        try {
            return parse(value);
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static int parseHex(String v) {
        String hex = v.substring(1);
        int r, g, b, a = 0xFF;
        if (hex.length() == 3) {
            r = nib(hex.charAt(0)); r |= r << 4;
            g = nib(hex.charAt(1)); g |= g << 4;
            b = nib(hex.charAt(2)); b |= b << 4;
        } else if (hex.length() == 4) {
            r = nib(hex.charAt(0)); r |= r << 4;
            g = nib(hex.charAt(1)); g |= g << 4;
            b = nib(hex.charAt(2)); b |= b << 4;
            a = nib(hex.charAt(3)); a |= a << 4;
        } else if (hex.length() == 6) {
            r = (nib(hex.charAt(0)) << 4) | nib(hex.charAt(1));
            g = (nib(hex.charAt(2)) << 4) | nib(hex.charAt(3));
            b = (nib(hex.charAt(4)) << 4) | nib(hex.charAt(5));
        } else if (hex.length() == 8) {
            r = (nib(hex.charAt(0)) << 4) | nib(hex.charAt(1));
            g = (nib(hex.charAt(2)) << 4) | nib(hex.charAt(3));
            b = (nib(hex.charAt(4)) << 4) | nib(hex.charAt(5));
            a = (nib(hex.charAt(6)) << 4) | nib(hex.charAt(7));
        } else {
            throw new IllegalArgumentException("Bad hex color: " + v);
        }
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    private static int nib(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        throw new IllegalArgumentException("Bad hex digit: " + c);
    }

    private static int parseRgb(String v) {
        boolean hasAlpha = v.startsWith("rgba");
        int open = v.indexOf('(');
        int close = v.lastIndexOf(')');
        if (open < 0 || close < 0 || close <= open) {
            throw new IllegalArgumentException("Bad rgb color: " + v);
        }
        String inside = v.substring(open + 1, close);
        String[] parts = inside.split(",");
        if (parts.length < 3) throw new IllegalArgumentException("Bad rgb color: " + v);
        int r = component(parts[0]);
        int g = component(parts[1]);
        int b = component(parts[2]);
        int a = 0xFF;
        if (hasAlpha && parts.length >= 4) {
            float af = Float.parseFloat(parts[3].trim());
            a = Math.round(af * 255f) & 0xFF;
        }
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    private static int component(String s) {
        String t = s.trim();
        if (t.endsWith("%")) {
            float p = Float.parseFloat(t.substring(0, t.length() - 1).trim());
            return clamp(Math.round(p * 255f / 100f));
        }
        return clamp((int) Math.round(Double.parseDouble(t)));
    }

    private static int clamp(int v) {
        if (v < 0) return 0;
        if (v > 255) return 255;
        return v;
    }
}
