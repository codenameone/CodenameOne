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
package android.graphics;

/// ARGB color ints and conversions.
public class Color {

    public static final int BLACK = 0xFF000000;
    public static final int DKGRAY = 0xFF444444;
    public static final int GRAY = 0xFF888888;
    public static final int LTGRAY = 0xFFCCCCCC;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int RED = 0xFFFF0000;
    public static final int GREEN = 0xFF00FF00;
    public static final int BLUE = 0xFF0000FF;
    public static final int YELLOW = 0xFFFFFF00;
    public static final int CYAN = 0xFF00FFFF;
    public static final int MAGENTA = 0xFFFF00FF;
    public static final int TRANSPARENT = 0;

    public static int alpha(int color) {
        return color >>> 24;
    }

    public static int red(int color) {
        return (color >> 16) & 0xFF;
    }

    public static int green(int color) {
        return (color >> 8) & 0xFF;
    }

    public static int blue(int color) {
        return color & 0xFF;
    }

    public static int rgb(int red, int green, int blue) {
        return 0xff000000 | (red << 16) | (green << 8) | blue;
    }

    public static int argb(int alpha, int red, int green, int blue) {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public static int rgb(float red, float green, float blue) {
        return 0xff000000 | ((int) (red * 255.0f + 0.5f) << 16) | ((int) (green * 255.0f + 0.5f) << 8)
                | (int) (blue * 255.0f + 0.5f);
    }

    public static int argb(float alpha, float red, float green, float blue) {
        return ((int) (alpha * 255.0f + 0.5f) << 24) | ((int) (red * 255.0f + 0.5f) << 16)
                | ((int) (green * 255.0f + 0.5f) << 8) | (int) (blue * 255.0f + 0.5f);
    }

    public static float luminance(int color) {
        double r = linear(red(color) / 255.0);
        double g = linear(green(color) / 255.0);
        double b = linear(blue(color) / 255.0);
        return (float) ((0.2126 * r) + (0.7152 * g) + (0.0722 * b));
    }

    private static double linear(double c) {
        return c <= 0.04045 ? c / 12.92 : pow((c + 0.055) / 1.055, 2.4);
    }

    private static double pow(double a, double b) {
        return com.codename1.util.MathUtil.pow(a, b);
    }

    /// `#RRGGBB`, `#AARRGGBB` and the named colors Android accepts.
    public static int parseColor(String colorString) {
        if (colorString.length() > 0 && colorString.charAt(0) == '#') {
            long color = Long.parseLong(colorString.substring(1), 16);
            if (colorString.length() == 7) {
                color |= 0x00000000ff000000L;
            } else if (colorString.length() != 9) {
                throw new IllegalArgumentException("Unknown color");
            }
            return (int) color;
        }
        String n = asciiLower(colorString);
        if (n.equals("black")) {
            return BLACK;
        }
        if (n.equals("darkgray")) {
            return DKGRAY;
        }
        if (n.equals("gray") || n.equals("grey")) {
            return GRAY;
        }
        if (n.equals("lightgray") || n.equals("lightgrey")) {
            return LTGRAY;
        }
        if (n.equals("white")) {
            return WHITE;
        }
        if (n.equals("red")) {
            return RED;
        }
        if (n.equals("green")) {
            return GREEN;
        }
        if (n.equals("blue")) {
            return BLUE;
        }
        if (n.equals("yellow")) {
            return YELLOW;
        }
        if (n.equals("cyan") || n.equals("aqua")) {
            return CYAN;
        }
        if (n.equals("magenta") || n.equals("fuchsia")) {
            return MAGENTA;
        }
        if (n.equals("lime")) {
            return 0xFF00FF00;
        }
        if (n.equals("maroon")) {
            return 0xFF800000;
        }
        if (n.equals("navy")) {
            return 0xFF000080;
        }
        if (n.equals("olive")) {
            return 0xFF808000;
        }
        if (n.equals("purple")) {
            return 0xFF800080;
        }
        if (n.equals("silver")) {
            return 0xFFC0C0C0;
        }
        if (n.equals("teal")) {
            return 0xFF008080;
        }
        throw new IllegalArgumentException("Unknown color");
    }

    public static void colorToHSV(int color, float[] hsv) {
        RGBToHSV(red(color), green(color), blue(color), hsv);
    }

    public static void RGBToHSV(int red, int green, int blue, float[] hsv) {
        float r = red / 255f;
        float g = green / 255f;
        float b = blue / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h;
        if (d == 0) {
            h = 0;
        } else if (max == r) {
            h = 60 * (((g - b) / d) % 6);
        } else if (max == g) {
            h = 60 * (((b - r) / d) + 2);
        } else {
            h = 60 * (((r - g) / d) + 4);
        }
        if (h < 0) {
            h += 360;
        }
        hsv[0] = h;
        hsv[1] = max == 0 ? 0 : d / max;
        hsv[2] = max;
    }

    public static int HSVToColor(float[] hsv) {
        return HSVToColor(0xFF, hsv);
    }

    public static int HSVToColor(int alpha, float[] hsv) {
        float h = hsv[0];
        float s = hsv[1];
        float v = hsv[2];
        float c = v * s;
        float hp = (h % 360) / 60f;
        float x = c * (1 - Math.abs(hp % 2 - 1));
        float r = 0;
        float g = 0;
        float b = 0;
        if (hp < 1) {
            r = c;
            g = x;
        } else if (hp < 2) {
            r = x;
            g = c;
        } else if (hp < 3) {
            g = c;
            b = x;
        } else if (hp < 4) {
            g = x;
            b = c;
        } else if (hp < 5) {
            r = x;
            b = c;
        } else {
            r = c;
            b = x;
        }
        float m = v - c;
        return argb(alpha, Math.round((r + m) * 255), Math.round((g + m) * 255), Math.round((b + m) * 255));
    }

    private static String asciiLower(String s) {
        char[] c = s.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] >= 'A' && c[i] <= 'Z') {
                c[i] = (char) (c[i] + 32);
            }
        }
        return new String(c);
    }
}
