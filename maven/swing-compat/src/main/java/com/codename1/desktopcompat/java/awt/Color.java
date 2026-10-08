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

/// An sRGB color with an alpha channel; immutable.
public class Color implements Paint {

    public static final Color white = new Color(255, 255, 255);
    public static final Color WHITE = white;
    public static final Color lightGray = new Color(192, 192, 192);
    public static final Color LIGHT_GRAY = lightGray;
    public static final Color gray = new Color(128, 128, 128);
    public static final Color GRAY = gray;
    public static final Color darkGray = new Color(64, 64, 64);
    public static final Color DARK_GRAY = darkGray;
    public static final Color black = new Color(0, 0, 0);
    public static final Color BLACK = black;
    public static final Color red = new Color(255, 0, 0);
    public static final Color RED = red;
    public static final Color pink = new Color(255, 175, 175);
    public static final Color PINK = pink;
    public static final Color orange = new Color(255, 200, 0);
    public static final Color ORANGE = orange;
    public static final Color yellow = new Color(255, 255, 0);
    public static final Color YELLOW = yellow;
    public static final Color green = new Color(0, 255, 0);
    public static final Color GREEN = green;
    public static final Color magenta = new Color(255, 0, 255);
    public static final Color MAGENTA = magenta;
    public static final Color cyan = new Color(0, 255, 255);
    public static final Color CYAN = cyan;
    public static final Color blue = new Color(0, 0, 255);
    public static final Color BLUE = blue;

    private static final double FACTOR = 0.7;

    private final int value;

    public Color(int r, int g, int b) {
        this(r, g, b, 255);
    }

    public Color(int r, int g, int b, int a) {
        check(r);
        check(g);
        check(b);
        check(a);
        value = (a << 24) | (r << 16) | (g << 8) | b;
    }

    public Color(int rgb) {
        value = 0xff000000 | rgb;
    }

    public Color(int rgba, boolean hasalpha) {
        value = hasalpha ? rgba : 0xff000000 | rgba;
    }

    public Color(float r, float g, float b) {
        this((int) (r * 255 + 0.5), (int) (g * 255 + 0.5), (int) (b * 255 + 0.5));
    }

    public Color(float r, float g, float b, float a) {
        this((int) (r * 255 + 0.5), (int) (g * 255 + 0.5), (int) (b * 255 + 0.5), (int) (a * 255 + 0.5));
    }

    private static void check(int component) {
        if (component < 0 || component > 255) {
            throw new IllegalArgumentException("Color parameter outside of expected range");
        }
    }

    public int getRed() {
        return (value >> 16) & 0xff;
    }

    public int getGreen() {
        return (value >> 8) & 0xff;
    }

    public int getBlue() {
        return value & 0xff;
    }

    public int getAlpha() {
        return (value >> 24) & 0xff;
    }

    public int getRGB() {
        return value;
    }

    public Color brighter() {
        int r = getRed();
        int g = getGreen();
        int b = getBlue();
        int alpha = getAlpha();
        int i = (int) (1.0 / (1.0 - FACTOR));
        if (r == 0 && g == 0 && b == 0) {
            return new Color(i, i, i, alpha);
        }
        if (r > 0 && r < i) {
            r = i;
        }
        if (g > 0 && g < i) {
            g = i;
        }
        if (b > 0 && b < i) {
            b = i;
        }
        return new Color(Math.min((int) (r / FACTOR), 255), Math.min((int) (g / FACTOR), 255),
                Math.min((int) (b / FACTOR), 255), alpha);
    }

    public Color darker() {
        return new Color(Math.max((int) (getRed() * FACTOR), 0), Math.max((int) (getGreen() * FACTOR), 0),
                Math.max((int) (getBlue() * FACTOR), 0), getAlpha());
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Color && ((Color) obj).value == value;
    }

    @Override
    public String toString() {
        return "java.awt.Color[r=" + getRed() + ",g=" + getGreen() + ",b=" + getBlue() + "]";
    }

    /// Parses a decimal, `0x`/`#` hexadecimal or `0` octal number as an
    /// opaque color.
    public static Color decode(String nm) throws NumberFormatException {
        String s = nm.trim();
        int radix = 10;
        if (s.startsWith("0x") || s.startsWith("0X")) {
            s = s.substring(2);
            radix = 16;
        } else if (s.startsWith("#")) {
            s = s.substring(1);
            radix = 16;
        } else if (s.startsWith("0") && s.length() > 1) {
            s = s.substring(1);
            radix = 8;
        }
        int i = (int) Long.parseLong(s, radix);
        return new Color((i >> 16) & 0xff, (i >> 8) & 0xff, i & 0xff);
    }

    public static int HSBtoRGB(float hue, float saturation, float brightness) {
        int r = 0;
        int g = 0;
        int b = 0;
        if (saturation == 0) {
            r = (int) (brightness * 255.0f + 0.5f);
            g = r;
            b = r;
        } else {
            float h = (hue - (float) Math.floor(hue)) * 6.0f;
            float f = h - (float) Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            float fr;
            float fg;
            float fb;
            switch ((int) h) {
                case 0:
                    fr = brightness;
                    fg = t;
                    fb = p;
                    break;
                case 1:
                    fr = q;
                    fg = brightness;
                    fb = p;
                    break;
                case 2:
                    fr = p;
                    fg = brightness;
                    fb = t;
                    break;
                case 3:
                    fr = p;
                    fg = q;
                    fb = brightness;
                    break;
                case 4:
                    fr = t;
                    fg = p;
                    fb = brightness;
                    break;
                default:
                    fr = brightness;
                    fg = p;
                    fb = q;
                    break;
            }
            r = (int) (fr * 255.0f + 0.5f);
            g = (int) (fg * 255.0f + 0.5f);
            b = (int) (fb * 255.0f + 0.5f);
        }
        return 0xff000000 | (r << 16) | (g << 8) | b;
    }

    public static float[] RGBtoHSB(int r, int g, int b, float[] hsbvals) {
        float[] out = hsbvals == null ? new float[3] : hsbvals;
        int cmax = Math.max(r, Math.max(g, b));
        int cmin = Math.min(r, Math.min(g, b));
        float brightness = cmax / 255.0f;
        float saturation = cmax != 0 ? (cmax - cmin) / (float) cmax : 0;
        float hue = 0;
        if (saturation != 0) {
            float span = cmax - cmin;
            float redc = (cmax - r) / span;
            float greenc = (cmax - g) / span;
            float bluec = (cmax - b) / span;
            if (r == cmax) {
                hue = bluec - greenc;
            } else if (g == cmax) {
                hue = 2.0f + redc - bluec;
            } else {
                hue = 4.0f + greenc - redc;
            }
            hue = hue / 6.0f;
            if (hue < 0) {
                hue = hue + 1.0f;
            }
        }
        out[0] = hue;
        out[1] = saturation;
        out[2] = brightness;
        return out;
    }

    public static Color getHSBColor(float h, float s, float b) {
        return new Color(HSBtoRGB(h, s, b));
    }

    public float[] getRGBComponents(float[] compArray) {
        float[] f = compArray == null ? new float[4] : compArray;
        f[0] = getRed() / 255f;
        f[1] = getGreen() / 255f;
        f[2] = getBlue() / 255f;
        f[3] = getAlpha() / 255f;
        return f;
    }

    public float[] getRGBColorComponents(float[] compArray) {
        float[] f = compArray == null ? new float[3] : compArray;
        f[0] = getRed() / 255f;
        f[1] = getGreen() / 255f;
        f[2] = getBlue() / 255f;
        return f;
    }

    @Override
    public int getTransparency() {
        int alpha = getAlpha();
        return alpha == 0xff ? OPAQUE : alpha == 0 ? BITMASK : TRANSLUCENT;
    }
}
