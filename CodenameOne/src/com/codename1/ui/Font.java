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

/**
 * A simple abstraction of platform fonts and library fonts that enables the
 * library to use more elaborate fonts unsupported by a specific device.
 * This abstraction also supports bitmap fonts using an Ant task (more details
 * about the unifier are explained in the javadoc overview document).
 * <p>A bitmap font can be created manually but that is tedious, normally you would use
 * the Ant task as illustrated bellow to produce a resource file containing
 * the supported bitmap font. For further detail read the overview document and 
 * {@link com.codename1.ui.util.Resources}.
<pre>
&lt;target name="pre-init"&gt;
     &lt;taskdef classpath="ImageUnifier.jar" classname="com.sun.jwt.resource.Builder" name="build" /&gt;
     &lt;build dest="src/font.res"&gt;
        &lt;font src="images/arial.ttf" bold="true" italic="true" size="11" /&gt;
        &lt;font logicalName="Dialog" /&gt;
    &lt;/build&gt;
&lt;/target&gt;
</pre>
 * <p>The following attributes can be expressed for a font ant task:
 * <ul>
 * <li>name - name for the font to load from the resource file (optional: defaults to logical name or file name).
 * <li>charset - defaults to the English alphabet, numbers and common signs. 
 * Should contain a list of all characters that should be supported by a font. E.g. if a font would always be
 * used for uppercase letters then it would save space to define the charset as: {@code "ABCDEFGHIJKLMNOPQRSTUVWXYZ" }
 * <li>src - font file in the case of using a file, defaults to TrueType font
 * <li>size - floating point size of the font
 * <li>bold - defaults to false indicates if the font should be bold
 * <li>italic - defaults to false indicates if the font should be italic
 * <li>trueType - defaults to true, relevant only when src is used. If set to false type 1 fonts are assumed.
 * <li>antiAliasing - defaults to true otherwise fonts will be aliased
 * <li>logicalName - logical name of the font as specified by java.awt.Font in Java SE: 
 * {@code Dialog, DialogInput, Monospaced, Serif, or SansSerif }
 * </ul>
 */
public class Font {
    /**
     * Constant allowing us to author portable system fonts
     */
    public static final int FACE_MONOSPACE = 32;

    /**
     * Constant allowing us to author portable system fonts
     */
    public static final int FACE_PROPORTIONAL = 64;

    /**
     * Constant allowing us to author portable system fonts
     */
    public static final int FACE_SYSTEM = 0;

    /**
     * Constant allowing us to author portable system fonts
     */
    public static final int SIZE_LARGE = 16;

    /**
     * Constant allowing us to author portable system fonts
     */
    public static final int SIZE_MEDIUM = 0;

    /**
     * Constant allowing us to author portable system fonts
     */
    public static final int SIZE_SMALL = 8;

    /**
     * Constant allowing us to author portable system fonts
     */
    public static final int STYLE_BOLD = 1;

    /**
     * Constant allowing us to author portable system fonts
     */
    public static final int STYLE_ITALIC = 2;
    
    /**
     * Constant allowing us to author portable system fonts
     */
    public static final int STYLE_UNDERLINED = 4;

    /**
     * Constant allowing us to author portable system fonts
     */
    public static final int STYLE_PLAIN = 0;
    
    private static Font defaultFont = new Font(null);
    
    private static Hashtable bitmapCache = new Hashtable();

    private static boolean enableBitmapFont = true;

    private Object font;

    private boolean ttf;
    
    private float pixelSize=-1;	// for derived fonts only, the size that was requested
    
    private String fontUniqueId;

    private static HashMap<String, Font> derivedFontCache = new HashMap<String, Font>();
    private static float fontReturnedHeight;
    
    /**
     * Creates a new Font
     */
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

    /**
     * Returns a previously loaded bitmap font from cache
     * 
     * @param fontName the font name is the logical name of the font 
     * @return the font object
     * @see #clearBitmapCache
     * @deprecated bitmap font functionality is now deprecated
     */
    public static Font getBitmapFont(String fontName) {
        return (Font)bitmapCache.get(fontName);
    }
    
    
    /**
     * Bitmap fonts are cached this method allows us to flush the cache thus allows
     * us to reload a font
     * @deprecated bitmap font functionality is now deprecated
     */
    public static void clearBitmapCache() {
        bitmapCache.clear();
    }

    /**
     * Returns true if the underlying platform supports loading truetype fonts from
     * a file.
     * 
     * @return true if the underlying platform supports loading truetype fonts from
     * a file
     */
    public static boolean isTrueTypeFileSupported() {
        return Display.impl.isTrueTypeSupported();
    }

    /**
     * Returns true if the underlying platform allows creating a font based on a
     * user submitted string.
     *
     * @return true if the underlying platform allows creating a font based on a
     * user submitted string
     */
    public static boolean isCreationByStringSupported() {
        return Display.impl.isLookupFontSupported();
    }

    /**
     * Indicates whether the implementation supports loading a font "natively" to handle one of the common
     * native prefixes
     * @return true if the "native:" prefix is supported by loadTrueTypeFont
     */
    public static boolean isNativeFontSchemeSupported() {
        return Display.impl.isNativeFontSchemeSupported();
    }
    
    /**
     * Creates a true type font with the given name/filename (font name might be different from the file name
     * and is required by some devices e.g. iOS). The font file must reside in the src root of the project in
     * order to be detectable. The file name should contain no slashes or any such value.<br>
     * <b>Important</b> some platforms e.g. iOS don't support changing the weight of the font and require you 
     * to use the font name matching the weight, so the weight argument to derive will be ignored!<br>
     * This system also supports a special "native:" prefix that uses system native fonts e.g. HelveticaNeue
     * on iOS and Roboto on Android. It supports the following types:
     * native:MainThin, native:MainLight, native:MainRegular, native:MainBold, native:MainBlack,
     * native:ItalicThin, native:ItalicLight, native:ItalicRegular, native:ItalicBold, native:ItalicBlack.
     * <b>Important</b> due to copyright restrictions we cannot distribute Helvetica and thus can't simulate it.
     * In the simulator you will always see Roboto and not the device font
     * 
     * @param fontName the name of the font
     * @param fileName the file name of the font as it appears in the src directory of the project, it MUST end with the .ttf extension!
     * @return the font object created or null if true type fonts aren't supported on this platform
     */
    public static Font createTrueTypeFont(String fontName, String fileName) {
        String alreadyLoaded = fileName + "_" + fontReturnedHeight + "_"+ Font.STYLE_PLAIN;
        Font f = derivedFontCache.get(alreadyLoaded);
        if(f != null) {
            return f;
        }
        if(fontName.startsWith("native:")) {
            if(!Display.impl.isNativeFontSchemeSupported()) {
                return null;
            }
        } else {
            if(fileName != null && (fileName.indexOf('/') > -1 || fileName.indexOf('\\') > -1 || !fileName.endsWith(".ttf"))) {
                throw new IllegalArgumentException("The font file name must be relative to the root and end with ttf: " + fileName);
            }
        }
        Object font = Display.impl.loadTrueTypeFont(fontName, fileName);
        if(font == null) {
            return null;
        }
        f = new Font(font);
        f.ttf = true;
        f.fontUniqueId = fontName;
        float h = f.getHeight();
        fontReturnedHeight = h;
        derivedFontCache.put(fileName + "_" + h + "_"+ Font.STYLE_PLAIN, f);
        return f;
    }
    
    /**
     * Creates a font based on this truetype font with the given pixel, <b>WARNING</b>! This method
     * will only work in the case of truetype fonts!<br>
     * <b>Important</b> some platforms e.g. iOS don't support changing the weight of the font and require you 
     * to use the font name matching the weight, so the weight argument to derive will be ignored!
     * @param sizePixels the size of the font in pixels
     * @param weight PLAIN, BOLD or ITALIC weight based on the constants in this class
     * @return scaled font instance
     */
    public Font derive(float sizePixels, int weight) {
        if(fontUniqueId != null) {
            // derive should recycle instances of Font to allow smarter caching and logic on the native side of the fence
            String key = fontUniqueId + "_" + sizePixels + "_"+ weight;
            Font f = derivedFontCache.get(key);
            if(f != null) {
                return f;
            }
            f = new Font(Display.impl.deriveTrueTypeFont(font, sizePixels, weight));
            f.pixelSize = sizePixels;
            f.ttf = true;
            derivedFontCache.put(key, f);
            return f;
        } else {
            // not sure if this ever happens but don't want to break that code
        	Font f = new Font(Display.impl.deriveTrueTypeFont(font, sizePixels, weight));
        	f.pixelSize = sizePixels;
        	f.ttf = true;
            return f;
        }
    }

    /**
     * Indicates if this is a TTF native font that can be derived and manipulated. This is true for a font loaded from
     * file (TTF) or using the native: font name
     * 
     * @return true if this is a native font
     */
    public boolean isTTFNativeFont() {
        return ttf;
    }
    
    /**
     * Creates a new font instance based on the platform specific string name of the
     * font. This method isn't supported on some platforms.
     *
     * @param lookup a set of platform specific names delimited by commas, the first succefully
     * loaded font will be used
     * @return newly created font or null if creation failed
     */
    public static Font create(String lookup) {
        Object n = Display.impl.loadNativeFont(lookup);
        if(n == null) {
            return null;
        }
        return new Font(n);
    }

    /**
     * Increase the contrast of the bitmap font for rendering on top of a surface
     * whose color is darker. This is useful when drawing anti-aliased bitmap fonts using a light color
     * (e.g. white) on top of a dark surface (e.g. black), the font often breaks down if its contrast is not
     * increased due to the way alpha blending appears to the eye.
     * <p>Notice that this method only works in one way, contrast cannot be decreased
     * properly in a font and it should be cleared and reloaed with a Look and Feel switch.
     * 
     * @param value the value to increase 
     * @deprecated bitmap font functionality is now deprecated
     */
    public void addContrast(byte value) {
    }

    /**
     * Creates a bitmap font with the given arguments and places said font in the cache
     * 
     * @param name the name for the font in the cache
     * @param bitmap a transparency map in red and black that indicates the characters
     * @param cutOffsets character offsets matching the bitmap pixels and characters in the font 
     * @param charWidth The width of the character when drawing... this should not be confused with
     *      the number of cutOffset[o + 1] - cutOffset[o]. They are completely different
     *      since a character can be "wider" and "seep" into the next region. This is
     *      especially true with italic characters all of which "lean" outside of their 
     *      bounds.
     * @param charsets the set of characters in the font
     * @return a font object to draw bitmap fonts
     * @deprecated bitmap font functionality is now deprecated
     */
    public static Font createBitmapFont(String name, Image bitmap, int[] cutOffsets, int[] charWidth, String charsets) {
        Font f = createBitmapFont(bitmap, cutOffsets, charWidth, charsets);
        bitmapCache.put(name, f);
        return f;
    }
        
    /**
     * Creates a bitmap font with the given arguments
     * 
     * @param bitmap a transparency map in red and black that indicates the characters
     * @param cutOffsets character offsets matching the bitmap pixels and characters in the font 
     * @param charWidth The width of the character when drawing... this should not be confused with
     *      the number of cutOffset[o + 1] - cutOffset[o]. They are completely different
     *      since a character can be "wider" and "seep" into the next region. This is
     *      especially true with italic characters all of which "lean" outside of their 
     *      bounds.
     * @param charsets the set of characters in the font
     * @return a font object to draw bitmap fonts
     * @deprecated bitmap font functionality is now deprecated
     */
    public static Font createBitmapFont(Image bitmap, int[] cutOffsets, int[] charWidth, String charsets) {
        return new CustomFont(bitmap, cutOffsets, charWidth, charsets);
    }
    
    /**
     * Creates a system native font in a similar way to common MIDP fonts
     * 
     * @param face One of FACE_SYSTEM, FACE_PROPORTIONAL, FACE_MONOSPACE
     * @param style one of STYLE_PLAIN, STYLE_ITALIC, STYLE_BOLD
     * @param size One of SIZE_SMALL, SIZE_MEDIUM, SIZE_LARGE
     * @return A newly created system font instance 
     */
    public static Font createSystemFont(int face, int style, int size) {
        return new Font(face, style, size);
    }
    
    /**
     * Return the width of the given characters in this font instance
     * 
     * @param ch array of characters
     * @param offset characters offsets
     * @param length characters length
     * @return the width of the given characters in this font instance
     */
    public int charsWidth(char[] ch, int offset, int length){
        return Display.impl.charsWidth(font, ch, offset, length);
    }
    
    /**
     * Return the width of the given string subset in this font instance
     * 
     * @param str the given string
     * @param offset the string offset
     * @param len the len od string
     * @return the width of the given string subset in this font instance
     */
    public int substringWidth(String str, int offset, int len){
        return Display.impl.stringWidth(font, str.substring(offset, offset + len));
    }
    
    /**
     * Return the width of the given string in this font instance
     * 
     * @param str the given string     * 
     * @return the width of the given string in this font instance
     */
    public int stringWidth(String str) {
        // this happens often for icons without text and can cost a bit more in the native platform
        if(str == null || str.length() == 0) {
            return 0;
        }
        // Its common to use a space character to create a label that takes up space but the value
        // of string width in this case becomes less important
        if(str == " ") {
            return 5;
        }
        return Display.impl.stringWidth(font, str);
    }
    
    /**
     * Return the width of the specific character when rendered alone
     * 
     * @param ch the specific character
     * @return the width of the specific character when rendered alone
     */
    public int charWidth(char ch) {
        return Display.impl.charWidth(font, ch);
    }
    
    /**
     * Return the total height of the font
     * 
     * @return the total height of the font
     */
    public int getHeight() {
        return Display.impl.getHeight(font);
    }
    
    /**
     * Draw the given char using the current font and color in the x,y 
     * coordinates.
     * 
     * @param g the graphics object
     * @param character the given character
     * @param x the x coordinate to draw the char
     * @param y the y coordinate to draw the char
     */
    void drawChar(Graphics g, char character, int x, int y) {
    }
    
    /**
     * Return the global default font instance
     * 
     * @return the global default font instance
     */
    public static Font getDefaultFont(){
        return defaultFont;
    }

    /**
     * Sets the global default font instance 
     * 
     * @param f the global default font instance 
     */
    public static void setDefaultFont(Font f) {
        if(f != null) {
            defaultFont = f;
        }
    }
    
    /**
     * Draw the given char array using the current font and color in the x,y 
     * coordinates
     * 
     * @param g the graphics object
     * @param str the given string 
     * @param x the x coordinate to draw the string
     * @param y the y coordinate to draw the string
     */
    void drawString(Graphics g, String str, int x, int y) {
    }
    
    /**
     * Draw the given char array using the current font and color in the x,y 
     * coordinates
     * 
     * @param g the graphics object
     * @param data the given char array 
     * @param offset the offset in the given char array
     * @param length the number of chars to draw
     * @param x the x coordinate to draw the char
     * @param y the y coordinate to draw the char
     */
    void drawChars(Graphics g, char[] data, int offset, int length, int x, int y) {
    }

    /**
     * Return Optional operation returning the font face for system fonts
     * 
     * @return Optional operation returning the font face for system fonts
     */
    public int getFace(){
        return Display.impl.getFace(font);
    }
    
    /**
     * Return Optional operation returning the font size for system fonts
     * 
     * @return Optional operation returning the font size for system fonts
     */
    public int getSize(){
        return Display.impl.getSize(font);
    }

    /**
     * Return Optional operation returning the font style for system fonts
     * 
     * @return Optional operation returning the font style for system fonts
     */
    public int getStyle() {
        return Display.impl.getStyle(font);
    }
    
    /**
     * Returns a string containing all the characters supported by this font.
     * Will return null for system fonts.
     * 
     * @return String containing the characters supported by a bitmap font or
     * null otherwise.
     */
    public String getCharset() {
        return null;
    }

    /**
     * Indicates whether bitmap fonts should be enabled by default when loading or
     * the fallback system font should be used instead. This allows easy toggling
     * of font loading.
     *
     * @param enabled true to enable bitmap font loading (if they exist in the resource)
     */
    public static void setBitmapFontEnabled(boolean enabled) {
        enableBitmapFont = enabled;
    }


    /**
     * Indicates whether bitmap fonts should be enabled when loading or
     * the fallback system font should be used instead. This allows easy toggling
     * of font loading.
     *
     * @return true by default indicating that bitmap font loading is enabled
     */
    public static boolean isBitmapFontEnabled() {
        return enableBitmapFont;
    }


    /**
     * Returns the internal implementation specific font object
     *
     * @return platform specific font object for use by implementation classes or native code
     */
    public Object getNativeFont() {
        return font;
    }
    
    /**
    * {@inheritDoc}
    */
   public boolean equals(Object o) {
       if(ttf) {
           return ((Font)o).font != null && ((Font)o).ttf && ((Font)o).font.equals(font);
       }
       if(o != null && o.getClass() == getClass()) {
           Font f = (Font)o;
           return !f.ttf && f.getFace() == getFace() && f.getSize() == getSize() && f.getStyle() == getStyle();
       }
       return false;
   }
   
   /**
    * The ascent is the amount by which the character ascends above the baseline. 
    * @return the ascent in pixels
    */
   public int getAscent() {
       return Display.impl.getFontAscent(font);
   }
   
   /**
    * The descent is the amount by which the character descends below the baseline
    * @return the descent in pixels
    */
   public int getDescent() {
       return Display.impl.getFontDescent(font);
   }
   
   /**
    * Returns the size with which the font object was created in case of truetype fonts/derived fonts. This
    * is useful since a platform might change things slightly based on platform constraints but this value should
    * be 100% consistent
    * @return the size requested in the derive method
    */
    public float getPixelSize() { 
        return pixelSize; 
    }
}
