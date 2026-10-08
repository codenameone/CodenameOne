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
package com.codename1.android.rescompiler;

import java.util.ArrayList;
import java.util.List;

/// The configuration qualifiers of one resource directory (`values-night-v21`,
/// `drawable-xxhdpi`, `layout-sw600dp-land`), parsed and put back together in
/// a canonical order.
///
/// The canonical string is the contract with the runtime: the compiler writes
/// it into the resource table and the runtime's `ResConfig` parses the same
/// grammar to pick the best match for the device. Only the qualifiers a Codename
/// One device can answer are kept; a directory naming any other valid Android
/// qualifier (`round`, `television`, `mcc310`) is reported and its resources
/// never match, which is what they would do on a phone.
public final class ResConfig {

    public static final int DENSITY_ANY = 0;
    public static final int DENSITY_NONE = 0xffff;
    public static final int DENSITY_ANYDPI = 0xfffe;

    public String language;
    public String region;
    public String script;
    /// 0 unspecified, 1 ldltr, 2 ldrtl.
    public int layoutDirection;
    public int smallestWidthDp;
    public int widthDp;
    public int heightDp;
    /// 0 unspecified, 1 port, 2 land.
    public int orientation;
    /// 0 unspecified, 1 notnight, 2 night.
    public int night;
    public int density;
    public int sdkVersion;
    /// A valid Android qualifier that no Codename One device can satisfy.
    public String unsupported;

    public static final ResConfig DEFAULT = new ResConfig();

    /// Parses the qualifier part of a directory name (everything after the
    /// first dash). Returns null and adds to `errors` when a token is not a
    /// qualifier at all or appears out of Android's order.
    public static ResConfig parse(String qualifiers, List<String> errors) {
        ResConfig c = new ResConfig();
        if (qualifiers == null || qualifiers.length() == 0) {
            return c;
        }
        String[] tokens = qualifiers.split("-");
        int stage = 0;
        for (int i = 0; i < tokens.length; i++) {
            String t = tokens[i];
            String lower = asciiLower(t);
            int s;
            if (lower.startsWith("mcc") || lower.startsWith("mnc")) {
                s = 1;
                c.unsupported = t;
            } else if (lower.startsWith("b+")) {
                s = 2;
                String[] parts = t.substring(2).split("\\+");
                c.language = asciiLower(parts[0]);
                for (int p = 1; p < parts.length; p++) {
                    if (parts[p].length() == 4) {
                        c.script = parts[p];
                    } else {
                        c.region = asciiUpper(parts[p]);
                    }
                }
            } else if (isLanguage(t) && stage < 2 && !lower.equals("car")) {
                s = 2;
                c.language = lower;
                if (i + 1 < tokens.length && tokens[i + 1].length() == 3
                        && (tokens[i + 1].charAt(0) == 'r') && isAlpha(tokens[i + 1].substring(1))) {
                    c.region = asciiUpper(tokens[i + 1].substring(1));
                    i++;
                }
            } else if (lower.equals("ldltr") || lower.equals("ldrtl")) {
                s = 3;
                c.layoutDirection = lower.equals("ldltr") ? 1 : 2;
            } else if (lower.startsWith("sw") && lower.endsWith("dp") && isDigits(lower, 2, lower.length() - 2)) {
                s = 4;
                c.smallestWidthDp = Integer.parseInt(lower.substring(2, lower.length() - 2));
            } else if (lower.startsWith("w") && lower.endsWith("dp") && isDigits(lower, 1, lower.length() - 2)) {
                s = 5;
                c.widthDp = Integer.parseInt(lower.substring(1, lower.length() - 2));
            } else if (lower.startsWith("h") && lower.endsWith("dp") && isDigits(lower, 1, lower.length() - 2)) {
                s = 6;
                c.heightDp = Integer.parseInt(lower.substring(1, lower.length() - 2));
            } else if (lower.equals("small") || lower.equals("normal") || lower.equals("large") || lower.equals("xlarge")) {
                s = 7;
                c.unsupported = t;
            } else if (lower.equals("long") || lower.equals("notlong")) {
                s = 8;
                c.unsupported = t;
            } else if (lower.equals("round") || lower.equals("notround")) {
                s = 9;
                c.unsupported = t;
            } else if (lower.equals("widecg") || lower.equals("nowidecg") || lower.equals("highdr") || lower.equals("lowdr")) {
                s = 10;
                c.unsupported = t;
            } else if (lower.equals("port") || lower.equals("land")) {
                s = 11;
                c.orientation = lower.equals("port") ? 1 : 2;
            } else if (lower.equals("car") || lower.equals("desk") || lower.equals("television")
                    || lower.equals("appliance") || lower.equals("watch") || lower.equals("vrheadset")) {
                s = 12;
                c.unsupported = t;
            } else if (lower.equals("night") || lower.equals("notnight")) {
                s = 13;
                c.night = lower.equals("night") ? 2 : 1;
            } else if (densityOf(lower) >= 0) {
                s = 14;
                c.density = densityOf(lower);
            } else if (lower.equals("notouch") || lower.equals("finger") || lower.equals("stylus")) {
                s = 15;
                if (!lower.equals("finger")) {
                    c.unsupported = t;
                }
            } else if (lower.equals("keysexposed") || lower.equals("keyshidden") || lower.equals("keyssoft")
                    || lower.equals("nokeys") || lower.equals("qwerty") || lower.equals("12key")
                    || lower.equals("navexposed") || lower.equals("navhidden") || lower.equals("nonav")
                    || lower.equals("dpad") || lower.equals("trackball") || lower.equals("wheel")) {
                s = 16;
                // A touch phone is keyssoft/nokeys/nonav; claim those and
                // treat the hardware-keyboard qualifiers as unavailable.
                if (!(lower.equals("keyssoft") || lower.equals("nokeys") || lower.equals("nonav")
                        || lower.equals("navhidden") || lower.equals("keyshidden"))) {
                    c.unsupported = t;
                }
            } else if (lower.length() > 1 && lower.charAt(0) == 'v' && isDigits(lower, 1, lower.length())) {
                s = 17;
                c.sdkVersion = Integer.parseInt(lower.substring(1));
            } else {
                errors.add("'" + t + "' is not a resource qualifier");
                return null;
            }
            if (s < stage) {
                errors.add("qualifier '" + t + "' is out of order in '" + qualifiers + "'");
                return null;
            }
            stage = s;
        }
        return c;
    }

    /// The canonical spelling, which is also what the runtime parses. Empty for
    /// the default configuration.
    public String canonical() {
        List<String> out = new ArrayList<String>();
        if (unsupported != null) {
            // The runtime treats any config carrying this marker as never
            // matching; keep the original token for diagnostics.
            out.add("x+" + unsupported);
        }
        if (language != null) {
            if (script != null) {
                out.add("b+" + language + "+" + script + (region == null ? "" : "+" + region));
            } else {
                out.add(region == null ? language : language + "-r" + region);
            }
        }
        if (layoutDirection != 0) {
            out.add(layoutDirection == 1 ? "ldltr" : "ldrtl");
        }
        if (smallestWidthDp > 0) {
            out.add("sw" + smallestWidthDp + "dp");
        }
        if (widthDp > 0) {
            out.add("w" + widthDp + "dp");
        }
        if (heightDp > 0) {
            out.add("h" + heightDp + "dp");
        }
        if (orientation != 0) {
            out.add(orientation == 1 ? "port" : "land");
        }
        if (night != 0) {
            out.add(night == 2 ? "night" : "notnight");
        }
        if (density != DENSITY_ANY) {
            out.add(densityName(density));
        }
        if (sdkVersion > 0) {
            out.add("v" + sdkVersion);
        }
        StringBuilder sb = new StringBuilder();
        for (String s : out) {
            if (sb.length() > 0) {
                sb.append('-');
            }
            sb.append(s);
        }
        return sb.toString();
    }

    static int densityOf(String lower) {
        if (lower.equals("ldpi")) {
            return 120;
        }
        if (lower.equals("mdpi")) {
            return 160;
        }
        if (lower.equals("tvdpi")) {
            return 213;
        }
        if (lower.equals("hdpi")) {
            return 240;
        }
        if (lower.equals("xhdpi")) {
            return 320;
        }
        if (lower.equals("xxhdpi")) {
            return 480;
        }
        if (lower.equals("xxxhdpi")) {
            return 640;
        }
        if (lower.equals("nodpi")) {
            return DENSITY_NONE;
        }
        if (lower.equals("anydpi")) {
            return DENSITY_ANYDPI;
        }
        if (lower.endsWith("dpi") && lower.length() > 3 && isDigits(lower, 0, lower.length() - 3)) {
            return Integer.parseInt(lower.substring(0, lower.length() - 3));
        }
        return -1;
    }

    static String densityName(int d) {
        switch (d) {
            case 120: return "ldpi";
            case 160: return "mdpi";
            case 213: return "tvdpi";
            case 240: return "hdpi";
            case 320: return "xhdpi";
            case 480: return "xxhdpi";
            case 640: return "xxxhdpi";
            case DENSITY_NONE: return "nodpi";
            case DENSITY_ANYDPI: return "anydpi";
            default: return d + "dpi";
        }
    }

    private static boolean isLanguage(String t) {
        return (t.length() == 2 || t.length() == 3) && isAlpha(t);
    }

    private static boolean isAlpha(String t) {
        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);
            if (!((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z'))) {
                return false;
            }
        }
        return t.length() > 0;
    }

    private static boolean isDigits(String s, int from, int to) {
        if (to <= from) {
            return false;
        }
        for (int i = from; i < to; i++) {
            char ch = s.charAt(i);
            if (ch < '0' || ch > '9') {
                return false;
            }
        }
        return true;
    }

    static String asciiLower(String s) {
        char[] c = s.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] >= 'A' && c[i] <= 'Z') {
                c[i] = (char) (c[i] + ('a' - 'A'));
            }
        }
        return new String(c);
    }

    static String asciiUpper(String s) {
        char[] c = s.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] >= 'a' && c[i] <= 'z') {
                c[i] = (char) (c[i] - ('a' - 'A'));
            }
        }
        return new String(c);
    }
}
