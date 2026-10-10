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
package com.codename1.desktopcompat.rt;

import com.codename1.compat.jdk.ResourceNames;
import com.codename1.compat.jdk.Resources;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

/// The font files an application carries: what `Font.createFont` makes a
/// font of, and what `GraphicsEnvironment.registerFont` lets a family
/// name find.
///
/// `Font.createFont` is handed bytes, and a platform opens a font by the
/// name of its FILE -- the flat name the build shipped the resource
/// under -- and, on iOS, by the name the font gives ITSELF. So the bytes
/// are read for the names in their `name` table, and the application's
/// font resources are read for the one that names itself the same way;
/// that resource is the file the platform is asked to open.
///
/// Bytes that match no resource of the application -- a font read from
/// the network or from the file system -- still make a font with the
/// right names, which is drawn with the platform's own typeface, as is a
/// font on a port that cannot open font files. The platform is asked for
/// a file at the root of the bundle whose name ends in `.ttf`.
public final class FontFiles {

    /// One font file.
    public static final class Face {

        private final String family;
        private final String fullName;
        private final String postScriptName;
        private String file;
        private com.codename1.ui.Font base;
        private boolean tried;

        Face(String family, String fullName, String postScriptName) {
            this.family = family;
            this.fullName = fullName;
            this.postScriptName = postScriptName;
        }

        /// The family, `Material Icons`.
        public String family() {
            return family;
        }

        /// The full name, `Material Icons Regular`.
        public String fullName() {
            return fullName;
        }

        /// The name the font is installed under, `MaterialIcons-Regular`.
        public String postScriptName() {
            return postScriptName;
        }

        /// The flat name of the file in the bundle; `null` for bytes no
        /// resource was matched to.
        public String file() {
            return file;
        }

        /// The platform's font of this file, at no particular size;
        /// `null` where there is none.
        public com.codename1.ui.Font base() {
            if (!tried) {
                tried = true;
                if (file != null && file.indexOf('/') < 0 && file.endsWith(".ttf")
                        && com.codename1.ui.Font.isTrueTypeFileSupported()) {
                    try {
                        base = com.codename1.ui.Font.createTrueTypeFont(postScriptName, file);
                    } catch (RuntimeException refused) {
                        // The platform would not open the file: the text
                        // is drawn with its own typeface.
                        com.codename1.io.Log.p("Font " + file + ": " + refused.getMessage());
                        base = null;
                    }
                }
            }
            return base;
        }
    }

    private static final HashMap<String, Face> BY_PATH = new HashMap<String, Face>();
    private static final HashMap<String, Face> REGISTERED = new HashMap<String, Face>();
    private static final ArrayList<String> FAMILIES = new ArrayList<String>();
    private static boolean scanned;

    private FontFiles() {
    }

    /// Font names are compared without regard to case, folded by hand:
    /// they are ASCII, and the locale must not come into it.
    private static String fold(String s) {
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

    /// Reads the names out of a TrueType or OpenType file (the first face
    /// of a collection). `null` for bytes that are no such file.
    public static Face parse(byte[] d) {
        if (d == null || d.length < 12) {
            return null;
        }
        int base = 0;
        int magic = u32(d, 0);
        if (magic == 0x74746366) {
            // 'ttcf': a collection; the directory of its first face.
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
        for (int i = 0; i < tables; i++) {
            int entry = base + 12 + 16 * i;
            if (entry + 16 > d.length) {
                return null;
            }
            if (u32(d, entry) == 0x6e616d65) {
                nameAt = u32(d, entry + 8);
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
        String family = found[1] != null ? found[1] : found[16];
        String full = found[4];
        if (family == null) {
            family = full;
        }
        if (family == null) {
            return null;
        }
        if (full == null) {
            String style = found[2] != null ? found[2] : found[17];
            full = style == null || "Regular".equals(style) ? family : family + " " + style;
        }
        return new Face(family, full, found[6] != null ? found[6] : full);
    }

    /// Everything a stream has left.
    public static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        for (int n = in.read(chunk); n >= 0; n = in.read(chunk)) {
            out.write(chunk, 0, n);
        }
        return out.toByteArray();
    }

    private static boolean isFontPath(String path) {
        int n = path.length();
        return n > 4 && (path.regionMatches(true, n - 4, ".ttf", 0, 4) || path.regionMatches(true, n - 4, ".otf", 0, 4)
                || path.regionMatches(true, n - 4, ".ttc", 0, 4));
    }

    private static void scan() {
        if (scanned) {
            return;
        }
        scanned = true;
        String[] paths = Resources.cn1ResourcePaths();
        for (int i = 0; i < paths.length; i++) {
            if (!isFontPath(paths[i])) {
                continue;
            }
            InputStream in = Resources.open(paths[i]);
            if (in == null) {
                continue;
            }
            Face face = null;
            try {
                face = parse(readAll(in));
            } catch (IOException unreadable) {
                face = null;
            }
            try {
                in.close();
            } catch (IOException ignored) {
                // Nothing to do about a stream that will not close.
                continue;
            }
            if (face != null) {
                face.file = ResourceNames.flatName(paths[i]);
                BY_PATH.put(paths[i], face);
            }
        }
    }

    /// The face these bytes are: the application's resource that names
    /// itself as they do, or a face without a file. `null` for bytes that
    /// are no font.
    public static Face fromBytes(byte[] data) {
        Face parsed = parse(data);
        if (parsed == null) {
            return null;
        }
        scan();
        for (Face candidate : BY_PATH.values()) {
            if (candidate.postScriptName.equals(parsed.postScriptName) && candidate.fullName.equals(parsed.fullName)) {
                return candidate;
            }
        }
        return parsed;
    }

    /// Lets the family, the full name and the PostScript name of a face
    /// find it. `false` when one of them finds another face already.
    public static boolean register(Face face) {
        String[] names = {face.family, face.fullName, face.postScriptName};
        for (int i = 0; i < names.length; i++) {
            Face there = REGISTERED.get(fold(names[i]));
            if (there != null && there != face && !there.postScriptName.equals(face.postScriptName)) {
                return false;
            }
        }
        for (int i = 0; i < names.length; i++) {
            REGISTERED.put(fold(names[i]), face);
        }
        if (!FAMILIES.contains(face.family)) {
            FAMILIES.add(face.family);
        }
        return true;
    }

    /// The registered face a font name finds, or `null`.
    public static Face registered(String name) {
        if (name == null || REGISTERED.isEmpty()) {
            return null;
        }
        return REGISTERED.get(fold(name));
    }

    /// The families that were registered, in the order they were.
    public static String[] families() {
        return FAMILIES.toArray(new String[FAMILIES.size()]);
    }

    /// Forgets every face; for tests.
    public static void reset() {
        BY_PATH.clear();
        REGISTERED.clear();
        FAMILIES.clear();
        scanned = false;
        Fonts.forgetFaces();
    }
}
