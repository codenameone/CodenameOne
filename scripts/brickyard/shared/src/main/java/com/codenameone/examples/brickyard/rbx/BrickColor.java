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
package com.codenameone.examples.brickyard.rbx;

import com.codenameone.examples.brickyard.luau.Interp;

/// A named palette color. Place files store these by number; the palette
/// below is the commonly used subset, and an unknown number or name maps to
/// the nearest entry so an old place still loads.
public final class BrickColor extends RbxType {
    private static final Object[][] PALETTE = {
        {1, "White", 0xF2F3F3}, {5, "Brick yellow", 0xD7C59A}, {9, "Light reddish violet", 0xE8BAC8},
        {18, "Nougat", 0xCC8E69}, {21, "Bright red", 0xC4281C}, {23, "Bright blue", 0x0D69AC},
        {24, "Bright yellow", 0xF5CD30}, {26, "Black", 0x1B2A35}, {28, "Dark green", 0x287F47},
        {37, "Bright green", 0x4B974B}, {38, "Dark orange", 0xA05F35}, {45, "Light blue", 0xB4D2E4},
        {101, "Medium red", 0xDA867A}, {102, "Medium blue", 0x6E99CA}, {104, "Bright violet", 0x6B327C},
        {105, "Br. yellowish orange", 0xE29B40}, {106, "Bright orange", 0xDA8541}, {107, "Bright bluish green", 0x008F9C},
        {119, "Br. yellowish green", 0xA4BD47}, {125, "Light orange", 0xEAB892}, {135, "Sand blue", 0x74869D},
        {141, "Earth green", 0x27462D}, {151, "Sand green", 0x789082}, {153, "Sand red", 0x957977},
        {192, "Reddish brown", 0x694028}, {194, "Medium stone grey", 0xA3A2A5}, {199, "Dark stone grey", 0x635F62},
        {208, "Light stone grey", 0xE5E4DF}, {217, "Brown", 0x7C5C46}, {226, "Cool yellow", 0xFDEA8D},
        {268, "Medium lilac", 0x342B75}, {1001, "Institutional white", 0xF8F8F8}, {1002, "Mid gray", 0xCDCDCD},
        {1003, "Really black", 0x111111}, {1004, "Really red", 0xFF0000}, {1005, "Deep orange", 0xFFB000},
        {1006, "Alder", 0xB480FF}, {1007, "Dusty Rose", 0xA34B4B}, {1009, "New Yeller", 0xFFFF00},
        {1010, "Really blue", 0x0000FF}, {1011, "Navy blue", 0x002060}, {1012, "Deep blue", 0x2154B9},
        {1013, "Cyan", 0x04AFEC}, {1014, "CGA brown", 0xAA5500}, {1015, "Magenta", 0xAA00AA},
        {1016, "Pink", 0xFF66CC}, {1017, "Deep orange", 0xFFAF00}, {1018, "Teal", 0x12EED4},
        {1019, "Toothpaste", 0x00FFFF}, {1020, "Lime green", 0x00FF00}, {1021, "Camo", 0x3A7D15},
        {1022, "Grime", 0x7F8E64}, {1023, "Lavender", 0x8C5B9F}, {1024, "Pastel light blue", 0xAFDDFF},
        {1025, "Pastel orange", 0xFFC9C9}, {1026, "Pastel violet", 0xB1A7FF}, {1027, "Pastel blue-green", 0x9FF3E9},
        {1028, "Pastel green", 0xCCFFCC}, {1029, "Pastel yellow", 0xFFFFCC}, {1030, "Pastel brown", 0xFFCC99},
        {1031, "Royal purple", 0x6225D1}, {1032, "Hot pink", 0xFF00BF},
    };

    public final int number;
    public final String name;
    public final int rgb;

    private BrickColor(int number, String name, int rgb) {
        this.number = number;
        this.name = name;
        this.rgb = rgb;
    }

    private static BrickColor of(int i) {
        Object[] e = PALETTE[i];
        return new BrickColor(((Integer) e[0]).intValue(), (String) e[1], ((Integer) e[2]).intValue());
    }

    public static BrickColor byNumber(int n) {
        for (int i = 0; i < PALETTE.length; i++) {
            if (((Integer) PALETTE[i][0]).intValue() == n) {
                return of(i);
            }
        }
        return mediumStoneGrey();
    }

    public static BrickColor byName(String n) {
        for (int i = 0; i < PALETTE.length; i++) {
            if (PALETTE[i][1].equals(n)) {
                return of(i);
            }
        }
        return mediumStoneGrey();
    }

    public static BrickColor mediumStoneGrey() {
        return byNumber(194);
    }

    /// The palette entry nearest to `c`.
    public static BrickColor nearest(Color3 c) {
        int best = 0;
        double bd = Double.MAX_VALUE;
        for (int i = 0; i < PALETTE.length; i++) {
            int v = ((Integer) PALETTE[i][2]).intValue();
            double dr = ((v >> 16) & 0xff) / 255.0 - c.r;
            double dg = ((v >> 8) & 0xff) / 255.0 - c.g;
            double db = (v & 0xff) / 255.0 - c.b;
            double d = dr * dr + dg * dg + db * db;
            if (d < bd) {
                bd = d;
                best = i;
            }
        }
        return of(best);
    }

    /// A deterministic palette entry for `BrickColor.random()` given a seed.
    public static BrickColor at(int index) {
        return of(Math.abs(index) % PALETTE.length);
    }

    public Color3 color() {
        return Color3.fromInt(rgb);
    }

    @Override
    public String typeName() {
        return "BrickColor";
    }

    @Override
    public Object get(Interp in, String k) {
        switch (k) {
            case "Name":
                return name;
            case "Number":
                return Args.n(number);
            case "Color":
                return color();
            case "r":
                return Args.n(color().r);
            case "g":
                return Args.n(color().g);
            case "b":
                return Args.n(color().b);
            default:
                return super.get(in, k);
        }
    }

    @Override
    public boolean eq(Object o) {
        return o instanceof BrickColor && ((BrickColor) o).number == number;
    }

    @Override
    public boolean equals(Object o) {
        return eq(o);
    }

    @Override
    public int hashCode() {
        return number;
    }

    @Override
    public String str() {
        return name;
    }
}
