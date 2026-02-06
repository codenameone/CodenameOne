/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui;

import com.codename1.impl.CodenameOneImplementation;

import java.util.HashMap;
import java.util.Hashtable;

/// Codename One currently supports 3 font types:
///
///
/// - **System fonts** - these are very simplistic builtin fonts. They work on all platforms and come
///                  in one of 3 sizes. However, they are ubiquitous and work in every platform in all languages.
///                    A system font can be created using `int, int)`.
///
///
/// - **TTF files** - you can just place a TTF file in the src directory of the project and it will appear in
///                  the Codename One Designer as an option. You can load such a font using `java.lang.String)`.
///
/// - **Native fonts** - these aren't supported on all platforms but generally they allow you to use a set
///                  of platform native good looking fonts. E.g. on Android the devices Roboto font will be used and on
///                  iOS Helvetica Neue will be used. You can load such a font using `java.lang.String)`.
///
///    **WARNING:** If you use a TTF file **MAKE SURE** not to delete the file when there **MIGHT**
///          be a reference to it. This can cause hard to track down issues!
///
///   **IMPORTANT:** due to copyright restrictions we cannot distribute Helvetica and thus can't simulate it.
///          In the simulator you will see Roboto as the fallback in some cases and not the device font unless you
///          are running on a Mac. Notice that the Roboto font from Google doesn't support all languages and thus
///          special characters might not work on the simulator but would work on the device.
///
/// The sample code below demonstrates a catalog of available fonts, the scr
///
/// ```java
/// private Label createForFont(Font fnt, String s) {
///   Label l = new Label(s);
///   l.getUnselectedStyle().setFont(fnt);
///   return l;
/// }
///
/// public void showForm() {
///   GridLayout gr = new GridLayout(5);
///   gr.setAutoFit(true);
///   Form hi = new Form("Fonts", gr);
///
///   int fontSize = Display.getInstance().convertToPixels(3);
///
///   // requires Handlee-Regular.ttf in the src folder root!
///   Font ttfFont = Font.createTrueTypeFont("Handlee", "Handlee-Regular.ttf").
///                       derive(fontSize, Font.STYLE_PLAIN);
///
///   Font smallPlainSystemFont = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
///   Font mediumPlainSystemFont = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
///   Font largePlainSystemFont = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE);
///   Font smallBoldSystemFont = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
///   Font mediumBoldSystemFont = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
///   Font largeBoldSystemFont = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE);
///   Font smallItalicSystemFont = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_ITALIC, Font.SIZE_SMALL);
///   Font mediumItalicSystemFont = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_ITALIC, Font.SIZE_MEDIUM);
///   Font largeItalicSystemFont = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_ITALIC, Font.SIZE_LARGE);
///
///   Font smallPlainMonospaceFont = Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_SMALL);
///   Font mediumPlainMonospaceFont = Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
///   Font largePlainMonospaceFont = Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_LARGE);
///   Font smallBoldMonospaceFont = Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_BOLD, Font.SIZE_SMALL);
///   Font mediumBoldMonospaceFont = Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
///   Font largeBoldMonospaceFont = Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_BOLD, Font.SIZE_LARGE);
///   Font smallItalicMonospaceFont = Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_ITALIC, Font.SIZE_SMALL);
///   Font mediumItalicMonospaceFont = Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_ITALIC, Font.SIZE_MEDIUM);
///   Font largeItalicMonospaceFont = Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_ITALIC, Font.SIZE_LARGE);
///
///   Font smallPlainProportionalFont = Font.createSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
///   Font mediumPlainProportionalFont = Font.createSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
///   Font largePlainProportionalFont = Font.createSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_LARGE);
///   Font smallBoldProportionalFont = Font.createSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_SMALL);
///   Font mediumBoldProportionalFont = Font.createSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
///   Font largeBoldProportionalFont = Font.createSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_LARGE);
///   Font smallItalicProportionalFont = Font.createSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_ITALIC, Font.SIZE_SMALL);
///   Font mediumItalicProportionalFont = Font.createSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_ITALIC, Font.SIZE_MEDIUM);
///   Font largeItalicProportionalFont = Font.createSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_ITALIC, Font.SIZE_LARGE);
///
///   String[] nativeFontTypes = {
///       "native:MainThin", "native:MainLight", "native:MainRegular", "native:MainBold", "native:MainBlack",
///       "native:ItalicThin", "native:ItalicLight", "native:ItalicRegular", "native:ItalicBold", "native:ItalicBlack"};
///
///   for(String s : nativeFontTypes) {
///       Font tt  = Font.createTrueTypeFont(s, s).derive(fontSize, Font.STYLE_PLAIN);
///       hi.add(createForFont(tt, s));
///   }
///
///   hi.add(createForFont(ttfFont, "Handlee TTF Font")).
///           add(createForFont(smallPlainSystemFont, "smallPlainSystemFont")).
///           add(createForFont(mediumPlainSystemFont, "mediumPlainSystemFont")).
///           add(createForFont(largePlainSystemFont, "largePlainSystemFont")).
///           add(createForFont(smallBoldSystemFont, "smallBoldSystemFont")).
///           add(createForFont(mediumBoldSystemFont, "mediumBoldSystemFont")).
///           add(createForFont(largeBoldSystemFont, "largeBoldSystemFont")).
///           add(createForFont(smallPlainSystemFont, "smallItalicSystemFont")).
///           add(createForFont(mediumItalicSystemFont, "mediumItalicSystemFont")).
///           add(createForFont(largeItalicSystemFont, "largeItalicSystemFont")).
///
///           add(createForFont(smallPlainMonospaceFont, "smallPlainMonospaceFont")).
///           add(createForFont(mediumPlainMonospaceFont, "mediumPlainMonospaceFont")).
///           add(createForFont(largePlainMonospaceFont, "largePlainMonospaceFont")).
///           add(createForFont(smallBoldMonospaceFont, "smallBoldMonospaceFont")).
///           add(createForFont(mediumBoldMonospaceFont, "mediumBoldMonospaceFont")).
///           add(createForFont(largeBoldMonospaceFont, "largeBoldMonospaceFont")).
///           add(createForFont(smallItalicMonospaceFont, "smallItalicMonospaceFont")).
///           add(createForFont(mediumItalicMonospaceFont, "mediumItalicMonospaceFont")).
///           add(createForFont(largeItalicMonospaceFont, "largeItalicMonospaceFont")).
///
///           add(createForFont(smallPlainProportionalFont, "smallPlainProportionalFont")).
///           add(createForFont(mediumPlainProportionalFont, "mediumPlainProportionalFont")).
///           add(createForFont(largePlainProportionalFont, "largePlainProportionalFont")).
///           add(createForFont(smallBoldProportionalFont, "smallBoldProportionalFont")).
///           add(createForFont(mediumBoldProportionalFont, "mediumBoldProportionalFont")).
///           add(createForFont(largeBoldProportionalFont, "largeBoldProportionalFont")).
///           add(createForFont(smallItalicProportionalFont, "smallItalicProportionalFont")).
///           add(createForFont(mediumItalicProportionalFont, "mediumItalicProportionalFont")).
///           add(createForFont(largeItalicProportionalFont, "largeItalicProportionalFont"));
///
///   hi.show();
/// }
/// ```
/// The demo code on the iPad simulator on a Mac
///
/// The demo code on an Android 5.1 OPO device (OnePlus One)
///
///    The Font class also supports bitmap fonts but this support is strictly aimed at legacy applications. We no longer
/// maintain that functionality.
public class Font extends CN {

    private static final Hashtable bitmapCache = new Hashtable();
    private static final HashMap<String, Font> derivedFontCache = new HashMap<String, Font>();
    private static Font defaultFont = new Font(null);
    private static boolean enableBitmapFont = true;
    private static float fontReturnedHeight;
    private Object font;
    private boolean ttf;
    private float pixelSize = -1;    // for derived fonts only, the size that was requested
    private String fontUniqueId;

    /// Creates a new Font
    Font() {
    }

    Font(Object nativeFont) {
        font = nativeFont;
    }

    Font(int face, int style, int size) {
        Display d = Display.getInstance();
        CodenameOneImplementation i = d.getImplementation();
        font = i.createFont(face, style, size);
    }

    /// Returns a previously loaded bitmap font from cache
    ///
    /// #### Parameters
    ///
    /// - `fontName`: the font name is the logical name of the font
    ///
    /// #### Returns
    ///
    /// the font object
    ///
    /// #### Deprecated
    ///
    /// bitmap font functionality is now deprecated
    ///
    /// #### See also
    ///
    /// - #clearBitmapCache
    public static Font getBitmapFont(String fontName) {
        return (Font) bitmapCache.get(fontName);
    }


    /// Bitmap fonts are cached this method allows us to flush the cache thus allows
    /// us to reload a font
    ///
    /// #### Deprecated
    ///
    /// bitmap font functionality is now deprecated
    public static void clearBitmapCache() {
        bitmapCache.clear();
    }

    /// Returns true if the underlying platform supports loading truetype fonts from
    /// a file.
    ///
    /// #### Returns
    ///
    /// @return true if the underlying platform supports loading truetype fonts from
    /// a file
    public static boolean isTrueTypeFileSupported() {
        return Display.impl.isTrueTypeSupported();
    }

    /// Returns true if the underlying platform allows creating a font based on a
    /// user submitted string.
    ///
    /// #### Returns
    ///
    /// @return true if the underlying platform allows creating a font based on a
    /// user submitted string
    public static boolean isCreationByStringSupported() {
        return Display.impl.isLookupFontSupported();
    }

    /// Indicates whether the implementation supports loading a font "natively" to handle one of the common
    /// native prefixes
    ///
    /// #### Returns
    ///
    /// true if the "native:" prefix is supported by loadTrueTypeFont
    public static boolean isNativeFontSchemeSupported() {
        return Display.impl.isNativeFontSchemeSupported();
    }

    /// Shorthand for `createTrueTypeFont(name, name)` which is useful
    /// for cases such as native: fonts. If a TTF file is passed this method will throw an exception!
    ///
    /// #### Parameters
    ///
    /// - `fontName`: the native font name. Notice that TTF file names are prohibited
    ///
    /// #### Returns
    ///
    /// a font object
    public static Font createTrueTypeFont(String fontName) {
        if (Display.getInstance().isSimulator() && !fontName.startsWith("native:")) {
            throw new IllegalArgumentException("Only native: fonts are supported by this method. To load a TTF use createTrueTypeFont(String, String)");
        }
        return createTrueTypeFont(fontName, fontName);
    }

    /// Shorthand for `createTrueTypeFont(name, name)` &
    /// `derive(size)` which is useful for cases such as native: fonts.
    ///
    /// #### Parameters
    ///
    /// - `fontName`: the native font name
    ///
    /// - `sizeMm`: the size in mm
    ///
    /// #### Returns
    ///
    /// a font object
    public static Font createTrueTypeFont(String fontName, float sizeMm) {
        return createTrueTypeFont(fontName, fontName).
                derive(Display.getInstance().convertToPixels(sizeMm), STYLE_PLAIN);
    }

    /// Shorthand for `createTrueTypeFont(name, name)` &
    /// `derive(size)` which is useful for cases such as native: fonts.
    ///
    /// #### Parameters
    ///
    /// - `fontName`: the native font name
    ///
    /// - `size`: the size in the specified unit.
    ///
    /// - `sizeUnit`: @param sizeUnit The unit type of the size.  One of `com.codename1.ui.plaf.Style#UNIT_TYPE_DIPS`,
    ///                 *                 `com.codename1.ui.plaf.Style#UNIT_TYPE_PIXELS`,
    ///                 *                 `com.codename1.ui.plaf.Style#UNIT_TYPE_REM`,
    ///                 *                 `com.codename1.ui.plaf.Style#UNIT_TYPE_VW`,
    ///                 *                 `com.codename1.ui.plaf.Style#UNIT_TYPE_VH`,
    ///                 *                 `com.codename1.ui.plaf.Style#UNIT_TYPE_VMIN`,
    ///                 *                 `com.codename1.ui.plaf.Style#UNIT_TYPE_VMAX`.
    ///
    /// #### Returns
    ///
    /// a font object
    ///
    /// #### Since
    ///
    /// 8.0
    public static Font createTrueTypeFont(String fontName, float size, byte sizeUnit) {
        return createTrueTypeFont(fontName, fontName).
                derive(Display.getInstance().convertToPixels(size, sizeUnit), STYLE_PLAIN);
    }

    /// Creates a true type font with the given name/filename (font name might be different from the file name
    /// and is required by some devices e.g. iOS). The font file must reside in the src root of the project in
    /// order to be detectable. The file name should contain no slashes or any such value.
    ///
    /// **Important** some platforms e.g. iOS don't support changing the weight of the font and require you
    /// to use the font name matching the weight, so the weight argument to derive will be ignored!
    ///
    /// This system also supports a special "native:" prefix that uses system native fonts e.g. HelveticaNeue
    /// on iOS and Roboto on Android. It supports the following types:
    /// native:MainThin, native:MainLight, native:MainRegular, native:MainBold, native:MainBlack,
    /// native:ItalicThin, native:ItalicLight, native:ItalicRegular, native:ItalicBold, native:ItalicBlack.
    /// **Important** due to copyright restrictions we cannot distribute Helvetica and thus can't simulate it.
    /// In the simulator you will see Roboto and not the device font unless you are running on a Mac
    ///
    /// #### Parameters
    ///
    /// - `fontName`: the name of the font
    ///
    /// - `fileName`: the file name of the font as it appears in the src directory of the project, it MUST end with the .ttf extension!
    ///
    /// #### Returns
    ///
    /// the font object created or null if true type fonts aren't supported on this platform
    public static Font createTrueTypeFont(String fontName, String fileName) {
        String alreadyLoaded = fileName + "_" + fontReturnedHeight + "_" + STYLE_PLAIN;
        Font f = derivedFontCache.get(alreadyLoaded);
        if (f != null) {
            return f;
        }
        if (fontName.startsWith("native:")) {
            if (!Display.impl.isNativeFontSchemeSupported()) {
                return null;
            }
        } else {
            if (fileName != null && (fileName.indexOf('/') > -1 || fileName.indexOf('\\') > -1 || !fileName.endsWith(".ttf"))) {
                throw new IllegalArgumentException("The font file name must be relative to the root and end with ttf: " + fileName);
            }
        }
        Object font = Display.impl.loadTrueTypeFont(fontName, fileName);
        if (font == null) {
            return null;
        }
        f = new Font(font);
        f.ttf = true;
        f.fontUniqueId = fontName;
        float h = f.getHeight();
        fontReturnedHeight = h;
        derivedFontCache.put(fileName + "_" + h + "_" + Font.STYLE_PLAIN, f);
        return f;
    }

    /// Creates a new font instance based on the platform specific string name of the
    /// font. This method isn't supported on some platforms.
    ///
    /// #### Parameters
    ///
    /// - `lookup`: @param lookup a set of platform specific names delimited by commas, the first succefully
    ///               loaded font will be used
    ///
    /// #### Returns
    ///
    /// newly created font or null if creation failed
    public static Font create(String lookup) {
        // for general convenience
        if (lookup.startsWith("native:")) {
            return createTrueTypeFont(lookup, lookup);
        }
        Object n = Display.impl.loadNativeFont(lookup);
        if (n == null) {
            return null;
        }
        return new Font(n);
    }

    /// Creates a bitmap font with the given arguments and places said font in the cache
    ///
    /// #### Parameters
    ///
    /// - `name`: the name for the font in the cache
    ///
    /// - `bitmap`: a transparency map in red and black that indicates the characters
    ///
    /// - `cutOffsets`: character offsets matching the bitmap pixels and characters in the font
    ///
    /// - `charWidth`: @param charWidth  The width of the character when drawing... this should not be confused with
    ///                   the number of cutOffset[o + 1] - cutOffset[o]. They are completely different
    ///                   since a character can be "wider" and "seep" into the next region. This is
    ///                   especially true with italic characters all of which "lean" outside of their
    ///                   bounds.
    ///
    /// - `charsets`: the set of characters in the font
    ///
    /// #### Returns
    ///
    /// a font object to draw bitmap fonts
    ///
    /// #### Deprecated
    ///
    /// bitmap font functionality is now deprecated
    public static Font createBitmapFont(String name, Image bitmap, int[] cutOffsets, int[] charWidth, String charsets) {
        Font f = createBitmapFont(bitmap, cutOffsets, charWidth, charsets);
        bitmapCache.put(name, f);
        return f;
    }

    /// Creates a bitmap font with the given arguments
    ///
    /// #### Parameters
    ///
    /// - `bitmap`: a transparency map in red and black that indicates the characters
    ///
    /// - `cutOffsets`: character offsets matching the bitmap pixels and characters in the font
    ///
    /// - `charWidth`: @param charWidth  The width of the character when drawing... this should not be confused with
    ///                   the number of cutOffset[o + 1] - cutOffset[o]. They are completely different
    ///                   since a character can be "wider" and "seep" into the next region. This is
    ///                   especially true with italic characters all of which "lean" outside of their
    ///                   bounds.
    ///
    /// - `charsets`: the set of characters in the font
    ///
    /// #### Returns
    ///
    /// a font object to draw bitmap fonts
    ///
    /// #### Deprecated
    ///
    /// bitmap font functionality is now deprecated
    public static Font createBitmapFont(Image bitmap, int[] cutOffsets, int[] charWidth, String charsets) {
        return new CustomFont(bitmap, cutOffsets, charWidth, charsets);
    }

    /// Creates a system native font in a similar way to common MIDP fonts
    ///
    /// #### Parameters
    ///
    /// - `face`: One of FACE_SYSTEM, FACE_PROPORTIONAL, FACE_MONOSPACE
    ///
    /// - `style`: one of STYLE_PLAIN, STYLE_ITALIC, STYLE_BOLD
    ///
    /// - `size`: One of SIZE_SMALL, SIZE_MEDIUM, SIZE_LARGE
    ///
    /// #### Returns
    ///
    /// A newly created system font instance
    public static Font createSystemFont(int face, int style, int size) {
        return new Font(face, style, size);
    }

    /// Return the global default font instance
    ///
    /// #### Returns
    ///
    /// the global default font instance
    public static Font getDefaultFont() {
        return defaultFont;
    }

    /// Sets the global default font instance
    ///
    /// #### Parameters
    ///
    /// - `f`: the global default font instance
    public static void setDefaultFont(Font f) {
        if (f != null) {
            defaultFont = f;
        }
    }

    /// Indicates whether bitmap fonts should be enabled when loading or
    /// the fallback system font should be used instead. This allows easy toggling
    /// of font loading.
    ///
    /// #### Returns
    ///
    /// true by default indicating that bitmap font loading is enabled
    public static boolean isBitmapFontEnabled() {
        return enableBitmapFont;
    }

    /// Indicates whether bitmap fonts should be enabled by default when loading or
    /// the fallback system font should be used instead. This allows easy toggling
    /// of font loading.
    ///
    /// #### Parameters
    ///
    /// - `enabled`: true to enable bitmap font loading (if they exist in the resource)
    public static void setBitmapFontEnabled(boolean enabled) {
        enableBitmapFont = enabled;
    }

    /// Creates a font based on this truetype font with the given pixel, **WARNING**! This method
    /// will only work in the case of truetype fonts!
    ///
    /// **Important** some platforms e.g. iOS don't support changing the weight of the font and require you
    /// to use the font name matching the weight, so the weight argument to derive will be ignored!
    ///
    /// #### Parameters
    ///
    /// - `size`: the size of the font in the specified unit type.
    ///
    /// - `weight`: PLAIN, BOLD or ITALIC weight based on the constants in this class
    ///
    /// - `unitType`: @param unitType The unit type of the size.  One of `com.codename1.ui.plaf.Style#UNIT_TYPE_DIPS`,
    ///                 `com.codename1.ui.plaf.Style#UNIT_TYPE_PIXELS`,
    ///                 `com.codename1.ui.plaf.Style#UNIT_TYPE_REM`,
    ///                 `com.codename1.ui.plaf.Style#UNIT_TYPE_VW`,
    ///                 `com.codename1.ui.plaf.Style#UNIT_TYPE_VH`,
    ///                 `com.codename1.ui.plaf.Style#UNIT_TYPE_VMIN`,
    ///                 `com.codename1.ui.plaf.Style#UNIT_TYPE_VMAX`.
    ///
    /// #### Returns
    ///
    /// scaled font instance
    ///
    /// #### Since
    ///
    /// 8.0
    public Font derive(float size, int weight, byte unitType) {
        return derive(Display.getInstance().convertToPixels(size, unitType), weight);
    }

    /// Creates a font based on this truetype font with the given pixel, **WARNING**! This method
    /// will only work in the case of truetype fonts!
    ///
    /// **Important** some platforms e.g. iOS don't support changing the weight of the font and require you
    /// to use the font name matching the weight, so the weight argument to derive will be ignored!
    ///
    /// #### Parameters
    ///
    /// - `sizePixels`: the size of the font in pixels
    ///
    /// - `weight`: PLAIN, BOLD or ITALIC weight based on the constants in this class
    ///
    /// #### Returns
    ///
    /// scaled font instance
    public Font derive(float sizePixels, int weight) {
        if (fontUniqueId != null) {
            // derive should recycle instances of Font to allow smarter caching and logic on the native side of the fence
            String key = fontUniqueId + "_" + sizePixels + "_" + weight;
            Font f = derivedFontCache.get(key);
            if (f != null) {
                return f;
            }
            f = new Font(Display.impl.deriveTrueTypeFont(font, sizePixels, weight));
            f.pixelSize = sizePixels;
            f.fontUniqueId = fontUniqueId;
            f.ttf = true;
            derivedFontCache.put(key, f);
            return f;
        } else {
            // not sure if this ever happens but don't want to break that code
            if (font != null) {
                Font f = new Font(Display.impl.deriveTrueTypeFont(font, sizePixels, weight));
                f.pixelSize = sizePixels;
                f.ttf = true;
                return f;
            } else {
                if (!ttf) {
                    throw new IllegalArgumentException("Cannot derive font " + this + " because it is not a truetype font");
                } else {
                    throw new IllegalArgumentException("Cannot derive font " + this + " because its native font representation is null.");
                }
            }

        }
    }

    /// Indicates if this is a TTF native font that can be derived and manipulated. This is true for a font loaded from
    /// file (TTF) or using the native: font name
    ///
    /// #### Returns
    ///
    /// true if this is a native font
    public boolean isTTFNativeFont() {
        return ttf;
    }

    /// Increase the contrast of the bitmap font for rendering on top of a surface
    /// whose color is darker. This is useful when drawing anti-aliased bitmap fonts using a light color
    /// (e.g. white) on top of a dark surface (e.g. black), the font often breaks down if its contrast is not
    /// increased due to the way alpha blending appears to the eye.
    ///
    /// Notice that this method only works in one way, contrast cannot be decreased
    /// properly in a font and it should be cleared and reloaed with a Look and Feel switch.
    ///
    /// #### Parameters
    ///
    /// - `value`: the value to increase
    ///
    /// #### Deprecated
    ///
    /// bitmap font functionality is now deprecated
    public void addContrast(byte value) {
    }

    /// Return the width of the given characters in this font instance
    ///
    /// #### Parameters
    ///
    /// - `ch`: array of characters
    ///
    /// - `offset`: characters offsets
    ///
    /// - `length`: characters length
    ///
    /// #### Returns
    ///
    /// the width of the given characters in this font instance
    public int charsWidth(char[] ch, int offset, int length) {
        return Display.impl.charsWidth(font, ch, offset, length);
    }

    /// Return the width of the given string subset in this font instance
    ///
    /// #### Parameters
    ///
    /// - `str`: the given string
    ///
    /// - `offset`: the string offset
    ///
    /// - `len`: the len od string
    ///
    /// #### Returns
    ///
    /// the width of the given string subset in this font instance
    public int substringWidth(String str, int offset, int len) {
        return Display.impl.stringWidth(font, str.substring(offset, offset + len));
    }

    /// Return the width of the given string in this font instance
    ///
    /// #### Parameters
    ///
    /// - `str`: the given string     *
    ///
    /// #### Returns
    ///
    /// the width of the given string in this font instance
    public int stringWidth(String str) {
        // this happens often for icons without text and can cost a bit more in the native platform
        if (str == null || str.length() == 0) {
            return 0;
        }
        // Its common to use a space character to create a label that takes up space but the value
        // of string width in this case becomes less important
        if (" ".equals(str)) {
            return 5;
        }
        return Display.impl.stringWidth(font, str);
    }

    /// Return the width of the specific character when rendered alone
    ///
    /// #### Parameters
    ///
    /// - `ch`: the specific character
    ///
    /// #### Returns
    ///
    /// the width of the specific character when rendered alone
    public int charWidth(char ch) {
        return Display.impl.charWidth(font, ch);
    }

    /// Return the total height of the font
    ///
    /// #### Returns
    ///
    /// the total height of the font
    public int getHeight() {
        return Display.impl.getHeight(font);
    }

    /// Draw the given char using the current font and color in the x,y
    /// coordinates.
    ///
    /// #### Parameters
    ///
    /// - `g`: the graphics object
    ///
    /// - `character`: the given character
    ///
    /// - `x`: the x coordinate to draw the char
    ///
    /// - `y`: the y coordinate to draw the char
    void drawChar(Graphics g, char character, int x, int y) {
    }

    /// Draw the given char array using the current font and color in the x,y
    /// coordinates
    ///
    /// #### Parameters
    ///
    /// - `g`: the graphics object
    ///
    /// - `str`: the given string
    ///
    /// - `x`: the x coordinate to draw the string
    ///
    /// - `y`: the y coordinate to draw the string
    void drawString(Graphics g, String str, int x, int y) {
    }

    /// Draw the given char array using the current font and color in the x,y
    /// coordinates
    ///
    /// #### Parameters
    ///
    /// - `g`: the graphics object
    ///
    /// - `data`: the given char array
    ///
    /// - `offset`: the offset in the given char array
    ///
    /// - `length`: the number of chars to draw
    ///
    /// - `x`: the x coordinate to draw the char
    ///
    /// - `y`: the y coordinate to draw the char
    void drawChars(Graphics g, char[] data, int offset, int length, int x, int y) {
    }

    /// Return Optional operation returning the font face for system fonts
    ///
    /// #### Returns
    ///
    /// Optional operation returning the font face for system fonts
    public int getFace() {
        return Display.impl.getFace(font);
    }

    /// Return Optional operation returning the font size for system fonts
    ///
    /// #### Returns
    ///
    /// Optional operation returning the font size for system fonts
    public int getSize() {
        return Display.impl.getSize(font);
    }

    /// Return Optional operation returning the font style for system fonts
    ///
    /// #### Returns
    ///
    /// Optional operation returning the font style for system fonts
    public int getStyle() {
        return Display.impl.getStyle(font);
    }

    /// Returns a string containing all the characters supported by this font.
    /// Will return null for system fonts.
    ///
    /// #### Returns
    ///
    /// @return String containing the characters supported by a bitmap font or
    /// null otherwise.
    public String getCharset() {
        return null;
    }

    /// Returns the internal implementation specific font object
    ///
    /// #### Returns
    ///
    /// platform specific font object for use by implementation classes or native code
    public Object getNativeFont() {
        return font;
    }

    /// {@inheritDoc}
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Font)) {
            return false;
        }
        Font f = (Font) o;
        if (ttf || f.ttf) {
            if (!ttf || !f.ttf) {
                return false;
            }
            if (font == null) {
                return f.font == null;
            }
            return font.equals(f.font);
        }
        if (f.getClass() != getClass()) {
            return false;
        }
        return f.getFace() == getFace() && f.getSize() == getSize() && f.getStyle() == getStyle();
    }

    /// {@inheritDoc}
    @Override
    public int hashCode() {
        if (ttf && font != null) {
            return font.hashCode();
        }
        return getFace() ^ getSize() ^ getStyle();
    }

    /// The ascent is the amount by which the character ascends above the baseline.
    ///
    /// #### Returns
    ///
    /// the ascent in pixels
    public int getAscent() {
        return Display.impl.getFontAscent(font);
    }

    /// The descent is the amount by which the character descends below the baseline
    ///
    /// #### Returns
    ///
    /// the descent in pixels
    public int getDescent() {
        return Display.impl.getFontDescent(font);
    }

    /// Returns the size with which the font object was created in case of truetype fonts/derived fonts. This
    /// is useful since a platform might change things slightly based on platform constraints but this value should
    /// be 100% consistent
    ///
    /// #### Returns
    ///
    /// the size requested in the derive method
    public float getPixelSize() {
        return pixelSize;
    }
}
