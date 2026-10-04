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
package com.codename1.androidcompat.runtime;

/// One resource directory's qualifiers, parsed from the canonical string the
/// build wrote, with Android's two rules for choosing among variants:
/// [#matches(DeviceConfig)] eliminates contradicting configurations and
/// [#isBetterThan(ResConfigSpec,DeviceConfig)] ranks the survivors, qualifier
/// by qualifier in Android's precedence order.
public final class ResConfigSpec {

    public static final int DENSITY_NONE = 0xffff;
    public static final int DENSITY_ANY = 0xfffe;

    final String canonical;
    boolean unsupported;
    String language;
    /// The four-letter script of a `b+` locale (`Hans` in `b+zh+Hans`), or
    /// null.
    String script;
    String region;
    int layoutDirection;
    int smallestWidthDp;
    int widthDp;
    int heightDp;
    int orientation;
    int night;
    int density;
    int sdkVersion;

    public ResConfigSpec(String canonical) {
        this.canonical = canonical;
        if (canonical.length() == 0) {
            return;
        }
        String[] tokens = split(canonical);
        for (int i = 0; i < tokens.length; i++) {
            String t = tokens[i];
            if (t.startsWith("x+")) {
                unsupported = true;
            } else if (t.startsWith("b+")) {
                String[] parts = splitPlus(t.substring(2));
                language = parts[0];
                for (int p = 1; p < parts.length; p++) {
                    if (parts[p].length() == 4) {
                        script = parts[p];
                    } else {
                        region = parts[p];
                    }
                }
            } else if (t.equals("ldltr")) {
                layoutDirection = 1;
            } else if (t.equals("ldrtl")) {
                layoutDirection = 2;
            } else if (t.startsWith("sw") && t.endsWith("dp")) {
                smallestWidthDp = Integer.parseInt(t.substring(2, t.length() - 2));
            } else if (t.startsWith("w") && t.endsWith("dp")) {
                widthDp = Integer.parseInt(t.substring(1, t.length() - 2));
            } else if (t.startsWith("h") && t.endsWith("dp")) {
                heightDp = Integer.parseInt(t.substring(1, t.length() - 2));
            } else if (t.equals("port")) {
                orientation = 1;
            } else if (t.equals("land")) {
                orientation = 2;
            } else if (t.equals("night")) {
                night = 2;
            } else if (t.equals("notnight")) {
                night = 1;
            } else if (t.endsWith("dpi")) {
                density = densityOf(t);
            } else if (t.length() > 1 && t.charAt(0) == 'v' && Character.isDigit(t.charAt(1))) {
                sdkVersion = Integer.parseInt(t.substring(1));
            } else if (t.length() == 2 || t.length() == 3) {
                language = t;
                if (i + 1 < tokens.length && tokens[i + 1].length() == 3 && tokens[i + 1].charAt(0) == 'r') {
                    region = tokens[i + 1].substring(1);
                    i++;
                }
            }
        }
    }

    private static int densityOf(String t) {
        if (t.equals("ldpi")) {
            return 120;
        }
        if (t.equals("mdpi")) {
            return 160;
        }
        if (t.equals("tvdpi")) {
            return 213;
        }
        if (t.equals("hdpi")) {
            return 240;
        }
        if (t.equals("xhdpi")) {
            return 320;
        }
        if (t.equals("xxhdpi")) {
            return 480;
        }
        if (t.equals("xxxhdpi")) {
            return 640;
        }
        if (t.equals("nodpi")) {
            return DENSITY_NONE;
        }
        if (t.equals("anydpi")) {
            return DENSITY_ANY;
        }
        return Integer.parseInt(t.substring(0, t.length() - 3));
    }

    public String getCanonical() {
        return canonical;
    }

    public int getDensity() {
        return density;
    }

    public boolean matches(DeviceConfig d) {
        if (unsupported) {
            return false;
        }
        if (language != null) {
            if (!language.equals(d.language)) {
                return false;
            }
            // A device whose script is unknown accepts any; otherwise a
            // Traditional variant must not serve a Simplified device.
            if (script != null && d.script != null && !script.equalsIgnoreCase(d.script)) {
                return false;
            }
            if (region != null && !region.equals(d.region)) {
                return false;
            }
        }
        if (layoutDirection != 0 && layoutDirection != (d.rtl ? 2 : 1)) {
            return false;
        }
        if (smallestWidthDp != 0 && smallestWidthDp > d.smallestWidthDp) {
            return false;
        }
        if (widthDp != 0 && widthDp > d.widthDp) {
            return false;
        }
        if (heightDp != 0 && heightDp > d.heightDp) {
            return false;
        }
        if (orientation != 0 && orientation != d.orientation) {
            return false;
        }
        if (night != 0 && night != d.night) {
            return false;
        }
        if (sdkVersion != 0 && sdkVersion > d.sdkVersion) {
            return false;
        }
        return true;
    }

    /// True when this configuration is a better match for `d` than `o`; both
    /// are assumed to match. Follows `ResTable_config::isBetterThan`.
    public boolean isBetterThan(ResConfigSpec o, DeviceConfig d) {
        if (o == null) {
            return true;
        }
        // Locale: a language beats none; then a script; then a region.
        if (language != null || o.language != null) {
            boolean mine = language != null;
            boolean theirs = o.language != null;
            if (mine != theirs) {
                return mine;
            }
            boolean myScript = script != null;
            boolean theirScript = o.script != null;
            if (myScript != theirScript) {
                return myScript;
            }
            boolean myRegion = region != null;
            boolean theirRegion = o.region != null;
            if (myRegion != theirRegion) {
                return myRegion;
            }
        }
        if (layoutDirection != o.layoutDirection) {
            return layoutDirection != 0;
        }
        if (smallestWidthDp != o.smallestWidthDp) {
            return smallestWidthDp > o.smallestWidthDp;
        }
        if (widthDp != o.widthDp || heightDp != o.heightDp) {
            if (widthDp != o.widthDp) {
                return widthDp > o.widthDp;
            }
            return heightDp > o.heightDp;
        }
        if (orientation != o.orientation) {
            return orientation != 0;
        }
        if (night != o.night) {
            return night != 0;
        }
        if (density != o.density) {
            return densityBetter(density, o.density, d.densityDpi);
        }
        if (sdkVersion != o.sdkVersion) {
            return sdkVersion > o.sdkVersion;
        }
        return false;
    }

    /// Android's density preference: anydpi wins outright; otherwise prefer
    /// the bucket that scales down rather than up, and among those the
    /// nearest.
    static boolean densityBetter(int mine, int theirs, int requested) {
        if (mine == DENSITY_ANY) {
            return true;
        }
        if (theirs == DENSITY_ANY) {
            return false;
        }
        int h = mine == 0 ? 160 : (mine == DENSITY_NONE ? requested : mine);
        int l = theirs == 0 ? 160 : (theirs == DENSITY_NONE ? requested : theirs);
        boolean imBigger = true;
        if (l > h) {
            int t = h;
            h = l;
            l = t;
            imBigger = false;
        }
        int req = requested == 0 ? 160 : requested;
        if (req >= h) {
            return imBigger;
        }
        if (l >= req) {
            return !imBigger;
        }
        if (((2L * l) - req) * h > (long) req * req) {
            return !imBigger;
        }
        return imBigger;
    }

    private static String[] split(String s) {
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        int start = 0;
        for (int i = 0; i <= s.length(); i++) {
            if (i == s.length() || s.charAt(i) == '-') {
                out.add(s.substring(start, i));
                start = i + 1;
            }
        }
        return out.toArray(new String[out.size()]);
    }

    private static String[] splitPlus(String s) {
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        int start = 0;
        for (int i = 0; i <= s.length(); i++) {
            if (i == s.length() || s.charAt(i) == '+') {
                out.add(s.substring(start, i));
                start = i + 1;
            }
        }
        return out.toArray(new String[out.size()]);
    }
}
