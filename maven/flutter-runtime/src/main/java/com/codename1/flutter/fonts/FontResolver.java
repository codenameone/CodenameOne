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
