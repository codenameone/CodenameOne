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

/// A font family and style. Families map onto Codename One's native font
/// scheme (`native:Main*` / `native:Italic*`), which is each platform's
/// system font, so text looks native on every port as it does on Android.
public class Typeface {

    public static final int NORMAL = 0;
    public static final int BOLD = 1;
    public static final int ITALIC = 2;
    public static final int BOLD_ITALIC = 3;

    public static final Typeface DEFAULT = new Typeface("sans-serif", NORMAL, 400, null);
    public static final Typeface DEFAULT_BOLD = new Typeface("sans-serif", BOLD, 700, null);
    public static final Typeface SANS_SERIF = new Typeface("sans-serif", NORMAL, 400, null);
    public static final Typeface SERIF = new Typeface("serif", NORMAL, 400, null);
    public static final Typeface MONOSPACE = new Typeface("monospace", NORMAL, 400, null);

    private final String family;
    private final int style;
    private final int weight;
    /// A bundled font file (flat resource name), or null for a system family.
    private final String file;

    Typeface(String family, int style, int weight, String file) {
        this.family = family;
        this.style = style;
        this.weight = weight;
        this.file = file;
    }

    public int getStyle() {
        return style;
    }

    public final boolean isBold() {
        return (style & BOLD) != 0;
    }

    public final boolean isItalic() {
        return (style & ITALIC) != 0;
    }

    public int getWeight() {
        return weight;
    }

    public String getFamily() {
        return family;
    }

    public String getFile() {
        return file;
    }

    public static Typeface create(String familyName, int style) {
        String f = familyName == null ? "sans-serif" : familyName;
        int w = (style & BOLD) != 0 ? 700 : 400;
        if (f.endsWith("-medium")) {
            w = Math.max(w, 500);
            f = f.substring(0, f.length() - 7);
        } else if (f.endsWith("-light")) {
            w = (style & BOLD) != 0 ? 700 : 300;
            f = f.substring(0, f.length() - 6);
        } else if (f.endsWith("-thin")) {
            w = (style & BOLD) != 0 ? 700 : 100;
            f = f.substring(0, f.length() - 5);
        } else if (f.endsWith("-black")) {
            w = 900;
            f = f.substring(0, f.length() - 6);
        } else if (f.endsWith("-condensed")) {
            f = f.substring(0, f.length() - 10);
        }
        return new Typeface(f, style, w, null);
    }

    public static Typeface create(Typeface family, int style) {
        if (family == null) {
            family = DEFAULT;
        }
        int w = (style & BOLD) != 0 ? Math.max(family.weight, 700) : (family.weight >= 700 ? 400 : family.weight);
        return new Typeface(family.family, style, w, family.file);
    }

    public static Typeface create(Typeface family, int weight, boolean italic) {
        if (family == null) {
            family = DEFAULT;
        }
        int style = (weight >= 600 ? BOLD : NORMAL) | (italic ? ITALIC : NORMAL);
        return new Typeface(family.family, style, weight, family.file);
    }

    public static Typeface defaultFromStyle(int style) {
        return create(DEFAULT, style);
    }

    /// A typeface backed by a bundled font file, shipped under `flatName`.
    public static Typeface fromFile(String family, String flatName) {
        return new Typeface(family, NORMAL, 400, flatName);
    }

    public static Typeface createFromAsset(android.content.res.AssetManager mgr, String path) {
        return new Typeface(path, NORMAL, 400, com.codename1.androidcompat.runtime.Assets.flatName(path));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Typeface)) {
            return false;
        }
        Typeface t = (Typeface) o;
        return t.style == style && t.weight == weight && t.family.equals(family)
                && (file == null ? t.file == null : file.equals(t.file));
    }

    @Override
    public int hashCode() {
        return family.hashCode() * 31 + style * 7 + weight;
    }
}
