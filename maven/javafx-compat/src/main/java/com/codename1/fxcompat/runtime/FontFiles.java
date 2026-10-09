/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.fxcompat.runtime;

import com.codename1.compat.jdk.ResourceNames;
import com.codename1.compat.jdk.Resources;
import com.codename1.ui.Font;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

/// The font files an application carries and has loaded: what
/// `Font.loadFont` and a style sheet's `@font-face` register, and what a
/// family name is looked up in before the platform's own faces.
///
/// A platform opens a font file by the name of the FILE, which is the flat
/// name the build shipped the resource under, and -- on iOS -- by the name
/// the font gives ITSELF, which only the file says. So a face is read
/// here: the `name` table gives the family, the style, the full name and
/// the PostScript name, and the `OS/2` table the weight and the slant. The
/// names are what `Font.getFamily()` and `Font.getName()` then answer, as
/// in JavaFX, and what a style sheet's `-fx-font-family` is matched
/// against.
///
/// A port that cannot open a font file, or a file that is not at the root
/// of the bundle under a name ending in `.ttf`, leaves the face without a
/// native font; text in it is drawn with the system face of its weight.
public final class FontFiles {

    /// One face of one font file.
    public static final class Face {
        private final String family;
        private final String style;
        private final String fullName;
        private final String postScriptName;
        private final int weight;
        private final boolean italic;
        private String file;
        private Font base;
        private boolean failed;

        Face(String family, String style, String fullName, String postScriptName, int weight, boolean italic) {
            this.family = family;
            this.style = style;
            this.fullName = fullName;
            this.postScriptName = postScriptName;
            this.weight = weight;
            this.italic = italic;
        }

        /// The family, `Clear Sans`.
        public String family() {
            return family;
        }

        /// The style, `Bold Italic`.
        public String style() {
            return style;
        }

        /// The full name, `Clear Sans Bold`.
        public String fullName() {
            return fullName;
        }

        /// The name the font is installed under, `ClearSans-Bold`.
        public String postScriptName() {
            return postScriptName;
        }

        /// The weight, 100 to 900.
        public int weight() {
            return weight;
        }

        /// Whether the face is slanted.
        public boolean italic() {
            return italic;
        }

        /// The flat name of the file in the bundle, or `null` for a face
        /// that came from a stream no resource could be matched to.
        public String file() {
            return file;
        }

        /// The platform's font of this file, at no particular size; `null`
        /// where the platform cannot open it.
        Font base() {
            if (base == null && !failed) {
                failed = true;
                if (file != null && file.indexOf('/') < 0 && file.endsWith(".ttf") && Font.isTrueTypeFileSupported()) {
                    try {
                        base = Font.createTrueTypeFont(postScriptName, file);
                    } catch (RuntimeException unavailable) {
                        // The platform refused the file: the system face is
                        // drawn instead, which is what a missing font gets.
                        base = null;
                    }
                }
            }
            return base;
        }
    }

    private static final HashMap<String, Face> BY_PATH = new HashMap<String, Face>();
    private static final HashMap<String, ArrayList<Face>> BY_NAME = new HashMap<String, ArrayList<Face>>();
    private static boolean scanned;

    private FontFiles() {
    }

    static String fold(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            out.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
        }
        return out.toString().trim();
    }

    private static int u16(byte[] d, int at) {
        return ((d[at] & 0xff) << 8) | (d[at + 1] & 0xff);
    }

    private static int u32(byte[] d, int at) {
        return (u16(d, at) << 16) | u16(d, at + 2);
    }

    private static boolean word(String style, String word) {
        return fold(style).indexOf(word) >= 0;
    }

    /// Reads the names, the weight and the slant out of a TrueType or
    /// OpenType file (the first face of a collection). Answers `null` for
    /// bytes that are no such file.
    public static Face parse(byte[] d) {
        if (d == null || d.length < 12) {
            return null;
        }
        int base = 0;
        int magic = u32(d, 0);
        if (magic == 0x74746366) {
            // 'ttcf': a collection; the first face's directory.
            if (d.length < 16) {
                return null;
            }
            base = u32(d, 12);
            if (base < 0 || base + 12 > d.length) {
                return null;
            }
            magic = u32(d, base);
        }
        if (magic != 0x00010000 && magic != 0x4f54544f && magic != 0x74727565) {
            return null;
        }
        int tables = u16(d, base + 4);
        int nameAt = -1;
        int os2At = -1;
        for (int i = 0; i < tables; i++) {
            int entry = base + 12 + 16 * i;
            if (entry + 16 > d.length) {
                return null;
            }
            int tag = u32(d, entry);
            int offset = u32(d, entry + 8);
            if (tag == 0x6e616d65) {
                nameAt = offset;
            } else if (tag == 0x4f532f32) {
                os2At = offset;
            }
        }
        if (nameAt < 0 || nameAt + 6 > d.length) {
            return null;
        }
        int count = u16(d, nameAt + 2);
        int strings = nameAt + u16(d, nameAt + 4);
        // Per name id: the string, and how good its record was. A Windows
        // Unicode record in English beats any other Unicode one, which
        // beats a Macintosh Roman one.
        String[] found = new String[18];
        int[] rank = new int[18];
        for (int i = 0; i < count; i++) {
            int record = nameAt + 6 + 12 * i;
            if (record + 12 > d.length) {
                break;
            }
            int platform = u16(d, record);
            int encoding = u16(d, record + 2);
            int language = u16(d, record + 4);
            int id = u16(d, record + 6);
            int length = u16(d, record + 8);
            int at = strings + u16(d, record + 10);
            if (id >= found.length || at < 0 || at + length > d.length) {
                continue;
            }
            boolean wide = platform == 0 || (platform == 3 && (encoding == 0 || encoding == 1 || encoding == 10));
            boolean roman = platform == 1 && encoding == 0;
            if (!wide && !roman) {
                continue;
            }
            int r = wide ? (platform == 3 && language == 0x409 ? 3 : 2) : 1;
            if (r <= rank[id]) {
                continue;
            }
            StringBuilder s = new StringBuilder();
            if (wide) {
                for (int k = 0; k + 1 < length; k += 2) {
                    s.append((char) u16(d, at + k));
                }
            } else {
                for (int k = 0; k < length; k++) {
                    int b = d[at + k] & 0xff;
                    s.append(b < 0x80 ? (char) b : '?');
                }
            }
            if (s.length() > 0) {
                found[id] = s.toString();
                rank[id] = r;
            }
        }
        // 16 and 17 are the family and style a font with more than four
        // styles gives besides the four-style names 1 and 2; JavaFX
        // reports 1 and 2, and so does this.
        String family = found[1] != null ? found[1] : found[16];
        String full = found[4];
        if (family == null) {
            family = full;
        }
        if (family == null) {
            return null;
        }
        String style = found[2] != null ? found[2] : (found[17] != null ? found[17] : "Regular");
        if (full == null) {
            full = "Regular".equals(style) ? family : family + " " + style;
        }
        String postScript = found[6] != null ? found[6] : full;
        int weight = -1;
        boolean italic = word(style, "italic") || word(style, "oblique");
        if (os2At >= 0 && os2At + 64 <= d.length) {
            weight = u16(d, os2At + 4);
            italic = italic || (u16(d, os2At + 62) & 1) != 0;
        }
        if (weight < 1 || weight > 1000) {
            weight = word(style, "black") || word(style, "heavy") ? 900
                    : (word(style, "bold") ? 700 : (word(style, "light") ? 300 : (word(style, "thin") ? 100 : 400)));
        }
        return new Face(family, style, full, postScript, weight, italic);
    }

    private static void file(String name, Face face) {
        if (name == null) {
            return;
        }
        String key = fold(name);
        ArrayList<Face> faces = BY_NAME.get(key);
        if (faces == null) {
            faces = new ArrayList<Face>();
            BY_NAME.put(key, faces);
        }
        for (int i = 0; i < faces.size(); i++) {
            if (faces.get(i) == face) {
                return;
            }
        }
        faces.add(face);
    }

    private static void register(Face face) {
        if (BY_NAME.get(fold(face.fullName)) == null || BY_NAME.get(fold(face.family)) == null) {
            // Text already drawn in this family got the system face.
            Fonts.flush();
        }
        file(face.family, face);
        file(face.fullName, face);
        file(face.postScriptName, face);
    }

    /// Loads the font file at a resource path (no leading slash) and
    /// registers it under its names. Answers `null` when the application
    /// ships no such resource or it is not a font.
    public static Face load(String path) {
        Face known = BY_PATH.get(path);
        if (known != null) {
            register(known);
            return known;
        }
        Face face = read(path);
        if (face != null) {
            register(face);
        }
        return face;
    }

    /// Reads a font resource without registering it.
    private static Face read(String path) {
        Face known = BY_PATH.get(path);
        if (known != null) {
            return known;
        }
        InputStream in = Resources.open(path);
        if (in == null) {
            return null;
        }
        Face face;
        try {
            face = parse(ResourceUrls.readAll(in));
        } catch (IOException unreadable) {
            face = null;
        }
        close(in);
        if (face != null) {
            face.file = ResourceNames.flatName(path);
            BY_PATH.put(path, face);
        }
        return face;
    }

    private static void close(InputStream in) {
        try {
            in.close();
        } catch (IOException ignored) {
            // Nothing to do about a stream that will not close.
            return;
        }
    }

    /// Loads the font file a URL string names; see [#load(String)].
    public static Face loadUrl(String url) {
        String path = ResourceUrls.find(url);
        return path == null ? null : load(path);
    }

    private static boolean isFontPath(String path) {
        int n = path.length();
        return n > 4 && (path.regionMatches(true, n - 4, ".ttf", 0, 4) || path.regionMatches(true, n - 4, ".otf", 0, 4)
                || path.regionMatches(true, n - 4, ".ttc", 0, 4));
    }

    /// Registers the font whose bytes these are. A stream carries no name,
    /// and a platform opens a font by its file, so the file is found by
    /// reading the application's font resources for the one that names
    /// itself the same way. With none, the face is registered without a
    /// file and drawn with the system font. Answers `null` for bytes that
    /// are no font.
    public static Face loadBytes(byte[] data) {
        Face parsed = parse(data);
        if (parsed == null) {
            return null;
        }
        if (!scanned) {
            scanned = true;
            String[] paths = Resources.cn1ResourcePaths();
            for (int i = 0; i < paths.length; i++) {
                if (isFontPath(paths[i])) {
                    read(paths[i]);
                }
            }
        }
        for (Face candidate : BY_PATH.values()) {
            if (candidate.postScriptName.equals(parsed.postScriptName) && candidate.fullName.equals(parsed.fullName)) {
                register(candidate);
                return candidate;
            }
        }
        ArrayList<Face> same = BY_NAME.get(fold(parsed.fullName));
        if (same != null) {
            for (int i = 0; i < same.size(); i++) {
                if (same.get(i).postScriptName.equals(parsed.postScriptName)) {
                    return same.get(i);
                }
            }
        }
        register(parsed);
        return parsed;
    }

    /// Makes a loaded face answer to one more family name, the one an
    /// `@font-face` rule declares it as.
    public static void alias(String family, Face face) {
        if (face != null) {
            file(family, face);
        }
    }

    /// The loaded face a name asks for: `name` is a family, a full name or
    /// a PostScript name, compared without regard to ASCII case, and among
    /// the faces that answer to it the one nearest the weight and slant
    /// wins. `null` when no loaded font answers to the name.
    public static Face find(String name, int weight, boolean italic) {
        if (name == null || BY_NAME.isEmpty()) {
            return null;
        }
        ArrayList<Face> faces = BY_NAME.get(fold(name));
        if (faces == null) {
            return null;
        }
        Face best = null;
        int bestScore = Integer.MAX_VALUE;
        for (int i = 0; i < faces.size(); i++) {
            Face f = faces.get(i);
            int score = Math.abs(f.weight - weight) + (f.italic == italic ? 0 : 1000);
            if (score < bestScore) {
                best = f;
                bestScore = score;
            }
        }
        return best;
    }

    /// Forgets every loaded font. For tests.
    public static void reset() {
        BY_PATH.clear();
        BY_NAME.clear();
        scanned = false;
    }
}
