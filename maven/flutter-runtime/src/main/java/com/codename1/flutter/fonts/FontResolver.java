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

import com.codename1.flutter.FlutterAssets;
import com.codename1.flutter.FontWeight;
import com.codename1.ui.Font;

/**
 * Resolves a Flutter {@code fontFamily} to a real Codename One font loaded from
 * a bundled TrueType file.
 *
 * <p>Flutter's {@code google_fonts} package downloads a face at runtime or reads
 * one the app bundled; either way the text is painted in the family the design
 * asks for. This runtime used to discard {@code fontFamily} entirely and paint
 * every string in the platform default, which is the single largest visual
 * difference on any screen with a designed typeface — a study whose whole look
 * is Work Sans or Libre Franklin renders in Helvetica and every glyph is the
 * wrong shape, the wrong width and on the wrong baseline.</p>
 *
 * <p>Faces are looked up among the app's flattened Flutter assets, under the
 * folders {@link #assetFolders()} lists. An app that bundles its faces
 * elsewhere adds its own folder rather than renaming its assets.</p>
 */
public final class FontResolver {

    private FontResolver() {
    }

    /**
     * Asset folders searched for a face, in order.
     *
     * <p>The last entry is where the Flutter gallery's asset package keeps the
     * Google Fonts it ships; it is a default, not a special case, and an app
     * with its own layout appends to this list.</p>
     */
    private static final java.util.List<String> FOLDERS = new java.util.ArrayList<String>();

    static {
        FOLDERS.add("fonts/");
        FOLDERS.add("assets/fonts/");
        FOLDERS.add("fonts/google_fonts/");
        FOLDERS.add("packages/flutter_gallery_assets/fonts/google_fonts/");
    }

    /** The mutable search path; add a folder before the first text is painted. */
    public static java.util.List<String> assetFolders() {
        return FOLDERS;
    }

    /**
     * The google_fonts file-name suffix for each weight, heaviest last. A face
     * rarely ships every weight, so a miss falls back to the nearest one that
     * exists rather than to the platform font.
     */
    private static final String[] VARIANTS = {
        "Thin", "ExtraLight", "Light", "Regular", "Medium",
        "SemiBold", "Bold", "ExtraBold", "Black",
    };

    private static final java.util.Map<String, Font> CACHE =
            new java.util.HashMap<String, Font>();

    /**
     * The face for {@code family} at {@code weight}, or null when the app
     * bundles no such file (in which case the caller keeps the platform font).
     *
     * <p>Cached including the misses — a family with no bundled face is asked
     * for on every build of every Text that names it, and probing the asset
     * folders each time is a filesystem walk per string.</p>
     */
    public static Font resolve(String family, FontWeight weight, boolean italic) {
        if (family == null || family.length() == 0) {
            return null;
        }
        String key = family + '|' + (weight == null ? "w400" : weight.name()) + '|' + italic;
        if (CACHE.containsKey(key)) {
            return CACHE.get(key);
        }
        Font f = load(family, weight, italic);
        CACHE.put(key, f);
        return f;
    }

    /** Test hook: forgets what has been resolved so far. */
    public static void clearCache() {
        CACHE.clear();
    }

    private static Font load(String family, FontWeight weight, boolean italic) {
        String base = compact(family);
        int want = index(weight);
        // Nearest-weight order: the exact one, then outwards, so a family that
        // ships only Regular and Bold still answers a request for Medium.
        for (int distance = 0; distance < VARIANTS.length; distance++) {
            for (int sign = 0; sign < 2; sign++) {
                int i = sign == 0 ? want + distance : want - distance;
                if (i < 0 || i >= VARIANTS.length || (distance == 0 && sign == 1)) {
                    continue;
                }
                Font f = tryFile(base + '-' + VARIANTS[i] + (italic ? "Italic" : ""));
                if (f != null) {
                    return f;
                }
            }
        }
        // A face bundled without a variant suffix at all ("Foo.ttf").
        return tryFile(base);
    }

    /** Flutter family names carry spaces ("Work Sans"); the files do not. */
    private static String compact(String family) {
        StringBuilder sb = new StringBuilder(family.length());
        for (int i = 0; i < family.length(); i++) {
            char c = family.charAt(i);
            if (c != ' ') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static int index(FontWeight weight) {
        if (weight == null) {
            return 3;   // Regular
        }
        switch (weight) {
            case w100: return 0;
            case w200: return 1;
            case w300: return 2;
            case w400: return 3;
            case w500: return 4;
            case w600: return 5;
            case w700: return 6;
            case w800: return 7;
            case w900: return 8;
            default: return 3;
        }
    }

    /// Cached line-height ratios, keyed by the same name tryFile() looks a face up by.
    private static final java.util.HashMap<String, Double> RATIOS =
            new java.util.HashMap<String, Double>();

    /**
     * The line height this face should lay out at, as a multiple of the font size, or 0
     * when it cannot be read.
     *
     * <p>A text style that names no height gets the FONT's line height, and which of a
     * font's several vertical metrics that means is not settled between platforms.
     * Codename One reports the typographic pair (sTypoAscender + sTypoDescender), which
     * for Work Sans is 1.17 em. Flutter lays the same face out at about 1.35, and the
     * difference is not cosmetic: the mail study's card is three lines of text, so each
     * card came out 26 device pixels short and the list drifted further out of place the
     * further down it went -- 11.9% of that screen wrong, the worst route in the sweep,
     * for a reason that has nothing to do with the widgets.</p>
     *
     * <p>The pair that reproduces it is the WINDOW ascent with the typographic descent,
     * which is what the reference measures to within a pixel over three lines. Read from
     * the face itself rather than assumed, so a font with different metrics gets its own
     * answer instead of this one's.</p>
     */
    public static double lineHeightRatio(String family, FontWeight weight, boolean italic) {
        if (family == null || family.length() == 0) {
            return 0;
        }
        String key = family + '|' + (weight == null ? "w400" : weight.name()) + '|' + italic;
        synchronized (RATIOS) {
            Double cached = RATIOS.get(key);
            if (cached != null) {
                return cached.doubleValue();
            }
        }
        // The same candidate order load() uses, so the metrics come from the FACE that
        // was actually resolved rather than from whichever file happens to be found first.
        double ratio = 0;
        String base = compact(family);
        int want = index(weight);
        outer:
        for (int distance = 0; distance < VARIANTS.length; distance++) {
            for (int sign = 0; sign < 2; sign++) {
                int i = sign == 0 ? want + distance : want - distance;
                if (i < 0 || i >= VARIANTS.length || (distance == 0 && sign == 1)) {
                    continue;
                }
                ratio = readRatio(base + '-' + VARIANTS[i] + (italic ? "Italic" : ""));
                if (ratio > 0) {
                    break outer;
                }
            }
        }
        if (ratio <= 0) {
            ratio = readRatio(base);
        }
        synchronized (RATIOS) {
            RATIOS.put(key, Double.valueOf(ratio));
        }
        return ratio;
    }

    private static double readRatio(String baseName) {
        for (int i = 0; i < FOLDERS.size(); i++) {
            for (int e = 0; e < EXTENSIONS.length; e++) {
                String flat = FlutterAssets.flatName(
                        FOLDERS.get(i) + baseName + EXTENSIONS[e]);
                byte[] data = readAll(flat);
                if (data != null) {
                    double r = metrics(data);
                    if (r > 0) {
                        return r;
                    }
                }
            }
        }
        return 0;
    }

    private static byte[] readAll(String flatName) {
        java.io.InputStream in = null;
        try {
            in = com.codename1.ui.Display.getInstance()
                    .getResourceAsStream(FontResolver.class, "/" + flatName);
            if (in == null) {
                return null;
            }
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } catch (Throwable t) {
            return null;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (java.io.IOException ignore) {
                // closing a font we already read is not worth failing over
            }
        }
    }

    private static int u16(byte[] d, int o) {
        return ((d[o] & 0xff) << 8) | (d[o + 1] & 0xff);
    }

    private static int s16(byte[] d, int o) {
        int v = u16(d, o);
        return v > 0x7fff ? v - 0x10000 : v;
    }

    /// (usWinAscent + |sTypoDescender|) / unitsPerEm, or 0 when the tables are absent.
    private static double metrics(byte[] d) {
        try {
            int tables = u16(d, 4);
            int head = -1;
            int os2 = -1;
            for (int i = 0; i < tables; i++) {
                int rec = 12 + 16 * i;
                String tag = new String(d, rec, 4, "ISO-8859-1");
                int off = (u16(d, rec + 8) << 16) | u16(d, rec + 10);
                if ("head".equals(tag)) {
                    head = off;
                } else if ("OS/2".equals(tag)) {
                    os2 = off;
                }
            }
            if (head < 0 || os2 < 0) {
                return 0;
            }
            int upem = u16(d, head + 18);
            if (upem <= 0) {
                return 0;
            }
            int typoDescender = s16(d, os2 + 70);
            int winAscent = u16(d, os2 + 74);
            if (winAscent <= 0) {
                return 0;
            }
            return (winAscent + Math.abs(typoDescender)) / (double) upem;
        } catch (Throwable t) {
            return 0;
        }
    }

    private static final String[] EXTENSIONS = {".ttf", ".otf"};

    private static Font tryFile(String baseName) {
        for (int i = 0; i < FOLDERS.size(); i++) {
            for (int e = 0; e < EXTENSIONS.length; e++) {
                String asset = FOLDERS.get(i) + baseName + EXTENSIONS[e];
                String flat = FlutterAssets.flatName(asset);
                if (!exists(flat)) {
                    continue;
                }
                try {
                    // The FILE name is flattened; the FONT name is the face's
                    // own, because iOS resolves a bundled font by name rather
                    // than by path and will not find "cn1f_...ttf".
                    Font f = Font.createTrueTypeFont(baseName, flat);
                    if (f != null) {
                        return f;
                    }
                } catch (Throwable ignore) {
                    // an unreadable or unsupported face is a miss, not a crash
                }
            }
        }
        return null;
    }

    private static boolean exists(String flatName) {
        try {
            java.io.InputStream in = com.codename1.ui.Display.getInstance()
                    .getResourceAsStream(FontResolver.class, "/" + flatName);
            if (in == null) {
                return false;
            }
            in.close();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
