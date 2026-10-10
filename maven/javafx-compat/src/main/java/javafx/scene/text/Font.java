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
package javafx.scene.text;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.codename1.fxcompat.runtime.FontFiles;
import com.codename1.fxcompat.runtime.Fonts;
import com.codename1.fxcompat.runtime.ResourceUrls;

import javafx.beans.NamedArg;

/// A font: a family, a weight, a posture and a size in logical pixels.
///
/// Fonts are those of the platform. A family the platform does not know
/// by name falls back to its default typeface, and every weight is drawn
/// with the nearest of the weights the platform font has.
public final class Font {

    private static final double DEFAULT_SIZE = 13;

    private final String family;
    private final String name;
    private final String style;
    private final double size;
    private final FontWeight weight;
    private final FontPosture posture;
    private final String lookup;
    private final int lookupWeight;
    private Object nativeFont;

    /// Creates the default typeface at a size.
    public Font(@NamedArg("size") double size) {
        this((String) null, size);
    }

    /// Creates a font by its full name, `Arial Bold`.
    public Font(@NamedArg("name") String name, @NamedArg("size") double size) {
        this(familyOf(name), weightOf(name), postureOf(name), size);
    }

    /// A font out of a file the application loaded: its names are the
    /// file's own.
    private Font(FontFiles.Face face, double size) {
        this.family = face.family();
        this.name = face.fullName();
        this.style = face.style();
        this.size = size < 0 ? DEFAULT_SIZE : size;
        this.weight = FontWeight.findByWeight(face.weight());
        this.posture = face.italic() ? FontPosture.ITALIC : FontPosture.REGULAR;
        this.lookup = face.fullName();
        this.lookupWeight = face.weight();
    }

    private Font(String family, FontWeight weight, FontPosture posture, double size) {
        this.lookup = null;
        this.lookupWeight = 0;
        this.family = family == null || family.length() == 0 ? "System" : family;
        this.weight = weight == null ? FontWeight.NORMAL : weight;
        this.posture = posture == null ? FontPosture.REGULAR : posture;
        this.size = size < 0 ? DEFAULT_SIZE : size;
        String s;
        if (this.weight == FontWeight.NORMAL) {
            s = this.posture == FontPosture.ITALIC ? "Italic" : "Regular";
        } else {
            s = styleName(this.weight) + (this.posture == FontPosture.ITALIC ? " Italic" : "");
        }
        this.style = s;
        this.name = this.family + " " + s;
    }

    private static String styleName(FontWeight w) {
        switch (w) {
            case THIN:
                return "Thin";
            case EXTRA_LIGHT:
                return "Extra Light";
            case LIGHT:
                return "Light";
            case MEDIUM:
                return "Medium";
            case SEMI_BOLD:
                return "Semi Bold";
            case BOLD:
                return "Bold";
            case EXTRA_BOLD:
                return "Extra Bold";
            case BLACK:
                return "Black";
            default:
                return "Regular";
        }
    }

    private static boolean endsWithWord(String name, String word) {
        int at = name.length() - word.length();
        return at > 0 && name.charAt(at - 1) == ' ' && name.regionMatches(true, at, word, 0, word.length());
    }

    private static String strip(String name) {
        String n = name;
        boolean again = true;
        while (again) {
            again = false;
            String[] words = {"Italic", "Regular", "Bold", "Light", "Thin", "Medium", "Black", "Semi", "Extra"};
            for (int i = 0; i < words.length; i++) {
                if (endsWithWord(n, words[i])) {
                    n = n.substring(0, n.length() - words[i].length()).trim();
                    again = true;
                }
            }
        }
        return n;
    }

    private static String familyOf(String name) {
        return name == null ? null : strip(name.trim());
    }

    private static FontWeight weightOf(String name) {
        if (name == null) {
            return FontWeight.NORMAL;
        }
        String rest = name.trim();
        if (endsWithWord(rest, "Italic")) {
            rest = rest.substring(0, rest.length() - 6).trim();
        }
        FontWeight[] all = FontWeight.values();
        for (int i = all.length - 1; i >= 0; i--) {
            if (all[i] != FontWeight.NORMAL && endsWithWord(rest, styleName(all[i]))) {
                return all[i];
            }
        }
        return FontWeight.NORMAL;
    }

    private static FontPosture postureOf(String name) {
        return name != null && endsWithWord(name.trim(), "Italic") ? FontPosture.ITALIC : FontPosture.REGULAR;
    }

    /// Returns the default font, the platform typeface at the default
    /// size.
    public static Font getDefault() {
        return Default.FONT;
    }

    /// Holds the default font, created when first asked for.
    private static final class Default {
        static final Font FONT = new Font("System", FontWeight.NORMAL, FontPosture.REGULAR, DEFAULT_SIZE);

        private Default() {
        }
    }

    /// Returns the font of a family with a weight, a posture and a size.
    public static Font font(String family, FontWeight weight, FontPosture posture, double size) {
        return new Font(family, weight, posture, size);
    }

    /// Returns the font of a family with a weight and a size.
    public static Font font(String family, FontWeight weight, double size) {
        return new Font(family, weight, null, size);
    }

    /// Returns the font of a family with a posture and a size.
    public static Font font(String family, FontPosture posture, double size) {
        return new Font(family, null, posture, size);
    }

    /// Returns the font of a family at a size.
    public static Font font(String family, double size) {
        return new Font(family, null, null, size);
    }

    /// Returns the font of a family at the default size.
    public static Font font(String family) {
        return new Font(family, null, null, -1);
    }

    /// Returns the default typeface at a size.
    public static Font font(double size) {
        return new Font(null, null, null, size);
    }

    /// Loads a font file the application carries and answers the font at
    /// a size. The URL is the one `getResource(...).toExternalForm()` gave;
    /// the file has to be one of the application's resources.
    ///
    /// From then on the font answers to its family and to its full name,
    /// in [#font(String, double)] and in a style sheet's `-fx-font-family`,
    /// which is what an application loads one for. The names are the
    /// file's own, read from it.
    ///
    /// The answer is `null` for a URL that names no resource or a file that
    /// is not a TrueType or OpenType font, as in JavaFX. A platform that
    /// cannot open the file still answers a font with the file's names; its
    /// text is drawn with the system face of the same weight.
    public static Font loadFont(String urlStr, double size) {
        if (urlStr == null) {
            return null;
        }
        FontFiles.Face face = FontFiles.loadUrl(urlStr);
        return face == null ? null : new Font(face, size);
    }

    /// Loads a font from a stream, which is read to its end and left
    /// open; see [#loadFont(String, double)]. A platform opens a font by
    /// its file, so the font drawn is the application resource that names
    /// itself as these bytes do, and the system face when there is none.
    public static Font loadFont(InputStream in, double size) {
        if (in == null) {
            return null;
        }
        byte[] data;
        try {
            data = ResourceUrls.readAll(in);
        } catch (IOException unreadable) {
            return null;
        }
        FontFiles.Face face = FontFiles.loadBytes(data);
        return face == null ? null : new Font(face, size);
    }

    /// Returns the family names known by name to every platform.
    public static List<String> getFamilies() {
        ArrayList<String> families = new ArrayList<String>();
        families.add("System");
        families.add("SansSerif");
        families.add("Serif");
        families.add("Monospaced");
        return families;
    }

    /// Returns the family, `Arial`.
    public final String getFamily() {
        return family;
    }

    /// Returns the full name, `Arial Bold`.
    public final String getName() {
        return name;
    }

    /// Returns the style part of the name, `Bold Italic`.
    public final String getStyle() {
        return style;
    }

    /// Returns the size in logical pixels.
    public final double getSize() {
        return size;
    }

    /// Returns the weight this font was asked for.
    public FontWeight cn1Weight() {
        return weight;
    }

    /// Returns the posture this font was asked for.
    public FontPosture cn1Posture() {
        return posture;
    }

    /// Returns the Codename One font that draws this one at the current
    /// scale, a `com.codename1.ui.Font`.
    public Object cn1Native() {
        if (nativeFont == null || !Fonts.isCurrent(nativeFont)) {
            nativeFont = lookup != null ? Fonts.create(lookup, lookupWeight, posture == FontPosture.ITALIC, size)
                    : Fonts.create(family, weight.getWeight(), posture == FontPosture.ITALIC, size);
        }
        return nativeFont;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Font) {
            Font other = (Font) obj;
            return Double.compare(size, other.size) == 0 && name.equals(other.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode() * 37 + (int) (size * 16);
    }

    @Override
    public String toString() {
        return "Font[name=" + name + ", family=" + family + ", style=" + style + ", size=" + size + "]";
    }
}
