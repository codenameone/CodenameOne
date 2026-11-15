/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.rbx;

import com.codename1.luau.Interp;

/// An immutable RGB color with components in 0..1.
public final class Color3 extends RbxType {
    public final double r;
    public final double g;
    public final double b;

    public Color3(double r, double g, double b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public static Color3 rgb(int r, int g, int b) {
        return new Color3(r / 255.0, g / 255.0, b / 255.0);
    }

    public static Color3 fromInt(int rgb) {
        return rgb((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff);
    }

    /// 0xRRGGBB, each channel rounded and clamped.
    public int toInt() {
        return (ch(r) << 16) | (ch(g) << 8) | ch(b);
    }

    private static int ch(double v) {
        int i = (int) Math.floor(v * 255 + 0.5);
        return i < 0 ? 0 : i > 255 ? 255 : i;
    }

    public static Color3 hsv(double h, double s, double v) {
        h = h - Math.floor(h);
        double c = v * s;
        double hp = h * 6;
        double xx = c * (1 - Math.abs(hp % 2 - 1));
        double r1;
        double g1;
        double b1;
        if (hp < 1) {
            r1 = c;
            g1 = xx;
            b1 = 0;
        } else if (hp < 2) {
            r1 = xx;
            g1 = c;
            b1 = 0;
        } else if (hp < 3) {
            r1 = 0;
            g1 = c;
            b1 = xx;
        } else if (hp < 4) {
            r1 = 0;
            g1 = xx;
            b1 = c;
        } else if (hp < 5) {
            r1 = xx;
            g1 = 0;
            b1 = c;
        } else {
            r1 = c;
            g1 = 0;
            b1 = xx;
        }
        double m = v - c;
        return new Color3(r1 + m, g1 + m, b1 + m);
    }

    public double[] toHsv() {
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double d = max - min;
        double h = 0;
        if (d > 0) {
            if (max == r) {
                h = ((g - b) / d) % 6;
            } else if (max == g) {
                h = (b - r) / d + 2;
            } else {
                h = (r - g) / d + 4;
            }
            h /= 6;
            if (h < 0) {
                h += 1;
            }
        }
        return new double[] {h, max == 0 ? 0 : d / max, max};
    }

    public static Color3 hex(String s) {
        String t = s.startsWith("#") ? s.substring(1) : s;
        if (t.length() == 3) {
            t = "" + t.charAt(0) + t.charAt(0) + t.charAt(1) + t.charAt(1) + t.charAt(2) + t.charAt(2);
        }
        return fromInt(Integer.parseInt(t, 16));
    }

    @Override
    public String typeName() {
        return "Color3";
    }

    private static final Method LERP = new Method("Lerp", 0);
    private static final Method TOHSV = new Method("ToHSV", 1);
    private static final Method TOHEX = new Method("ToHex", 2);

    @Override
    public Object get(Interp in, String k) {
        switch (k) {
            case "R":
            case "r":
                return Args.n(r);
            case "G":
            case "g":
                return Args.n(g);
            case "B":
            case "b":
                return Args.n(b);
            case "Lerp":
                return LERP;
            case "ToHSV":
                return TOHSV;
            case "ToHex":
                return TOHEX;
            default:
                return super.get(in, k);
        }
    }

    @Override
    protected Object[] invoke(int id, Interp in, Object[] a) {
        switch (id) {
            case 0: {
                Object o = Args.at(a, 1);
                if (!(o instanceof Color3)) {
                    throw Args.bad(a, 1, "Lerp", "Color3");
                }
                Color3 c = (Color3) o;
                double t = Args.num(a, 2, "Lerp");
                return ret(new Color3(r + (c.r - r) * t, g + (c.g - g) * t, b + (c.b - b) * t));
            }
            case 1: {
                double[] h = toHsv();
                return new Object[] {Args.n(h[0]), Args.n(h[1]), Args.n(h[2])};
            }
            case 2: {
                String s = Integer.toHexString(toInt() | 0x1000000).substring(1);
                return ret(s);
            }
            default:
                return super.invoke(id, in, a);
        }
    }

    @Override
    public boolean eq(Object o) {
        if (!(o instanceof Color3)) {
            return false;
        }
        Color3 c = (Color3) o;
        return c.r == r && c.g == g && c.b == b;
    }

    @Override
    public boolean equals(Object o) {
        return eq(o);
    }

    @Override
    public int hashCode() {
        return toInt();
    }

    @Override
    public String str() {
        return Interp.numberToString(r) + ", " + Interp.numberToString(g) + ", " + Interp.numberToString(b);
    }
}
