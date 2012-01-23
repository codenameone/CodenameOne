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
package com.codename1.ui.html;

import com.codename1.ui.Font;
import java.util.Vector;

/**
 * This class is a wrapper class for a Codename One font object, that keeps additional info on the font as required for HTML rendering by HTMLComponent.<br>
 * The info kept on the font is its family, size and style. These attributes are available in Codename One only for system fonts,
 * but here they are made available for bitmap fonts using a key to describe the font (See HTMLComponent.addFont)<br>
 * <br>
 * In addition this class keeps track of "counterpart" fonts for this font in a space of 4 attributes: BOLD, ITALIC, BIG and SMALL.<br>
 * For example a bold font is the BOLD counterpart of a plain one. A font with size 16 is the BIG counterpart of a font the size of 12 - provided that all other attributes are the same (i.e. an arial.16 font is not the BIG counterpart of a courier.12 etc.)<br>
 *
 * @author Ofir Leitner
 */
class HTMLFont {

    boolean systemFont;
    Font font;
    String family;
    int size;
    int style;
    boolean bold;
    boolean italic;
    String tagFont;
    
    static final int BOLD = 0;
    static final int ITALIC = 1;
    static final int BIG = 2;
    static final int SMALL = 3;
    
    private HTMLFont[] counterpartFonts = new HTMLFont[4];

    /**
     * The following tags are tags that mainly define the style of their content, and one of the things they can change is the font
     * 
     */
    static private int[] SPECIAL_FONT_TAGS_ID = {
        HTMLElement.TAG_EM,    HTMLElement.TAG_STRONG,
        HTMLElement.TAG_DFN, HTMLElement.TAG_CODE,
        HTMLElement.TAG_SAMP, HTMLElement.TAG_KBD,
        HTMLElement.TAG_VAR, HTMLElement.TAG_CITE,
        HTMLElement.TAG_H1, HTMLElement.TAG_H2,
        HTMLElement.TAG_H3, HTMLElement.TAG_H4,
        HTMLElement.TAG_H5, HTMLElement.TAG_H6,
        HTMLElement.TAG_TT
    };

    private static final char TOKEN = '.';
    static Vector SPECIAL_FONT_TAGS = new Vector();

    /**
     * The static segment sets up the SPECIAL_FONT_TAGS vector with values from the SPECIAL_FONT_TAGS_ID array.
     * This vector is used for lookup later on.
     */
    static {
        for(int i=0;i<SPECIAL_FONT_TAGS_ID.length;i++) {
            SPECIAL_FONT_TAGS.addElement(HTMLElement.TAG_NAMES[SPECIAL_FONT_TAGS_ID[i]]);
        }
    }

    /**
     * Constructs the HTMLFont 
     * 
     * @param fontKey The key for this font (See class definition)
     * @param font The actual Codename One font object (Can be either system or bitmap font)
     */
    HTMLFont(String fontKey,Font font) {
        this.font=font;
        systemFont=(font.getCharset()==null); // A systemfont has no "supported" charset

        if (isSystemFont()) {
            bold=((font.getStyle() & Font.STYLE_BOLD)!=0);
            italic=((font.getStyle() & Font.STYLE_ITALIC)!=0);
            size=font.getSize();
            if (font.getFace()==Font.FACE_SYSTEM) {
                family="system";
            } else if (font.getFace()==Font.FACE_MONOSPACE) {
                family="monospace";
            } else if (font.getFace()==Font.FACE_PROPORTIONAL) {
                family="proportional";
            }
        } else { //bitmap font
            if (fontKey!=null) { //fontKey can be null in the case this is a default font (Though even then it is recommended to provide a font key so the engine can use this font in other cases)
                boolean sufficientInfo=false;
                int lastIndex=0;
                if (!fontKey.endsWith(".")) {
                    fontKey+='.';
                }
                int index=fontKey.indexOf(TOKEN);
                while(index!=-1) {
                    String str=fontKey.substring(lastIndex, index);
                    try {
                        int num=Integer.parseInt(str);
                        size=num;
                    } catch (NumberFormatException nfe) {
                        if (str.equalsIgnoreCase("bold")) {
                           bold=true;
                         } else if (str.equalsIgnoreCase("italic")) {
                            italic=true;
                        } else if (str.equalsIgnoreCase("plain")) {
                            // do nothing, but don't save as a family
                        } else if (SPECIAL_FONT_TAGS.contains(str)) {
                            HTMLComponent.fonts.put(str, this);
                            sufficientInfo=true;
                        } else {
                            family=str.toLowerCase();
                            sufficientInfo=true;
                        }

                    }
                    lastIndex=index+1;
                    index=fontKey.indexOf(TOKEN,lastIndex);
                }
                if (!sufficientInfo) {
                    System.out.println("WARNING: Font was added with key '"+fontKey+"' which doesn't contain info on the font's family or attributes it to a special tag. The font will probably be unusable by the font engine.");
                }
            }
        }

        if (bold) {
            style+=Font.STYLE_BOLD;
            counterpartFonts[BOLD]=this;
        }
        if (italic) {
            style+=Font.STYLE_ITALIC;
            counterpartFonts[ITALIC]=this;
        }

        if (isSystemFont()) {
            if (size==Font.SIZE_LARGE) {
                counterpartFonts[BIG]=this;
            } else if (size==Font.SIZE_SMALL) {
                counterpartFonts[SMALL]=this;
            }
        }

    }

    /**
     * Checks if this is a system font
     *
     * @return true if the font is a system font, false otherwise
     */
    boolean isSystemFont() {
        return systemFont;
    }

    /**
     * Checks if this is a bold font
     *
     * @return true if the font is bold, false otherwise
     */
    boolean isBold() {
        return bold;
    }

    /**
     * Checks if this is an Italic font
     * 
     * @return true if the font is italic, false otherwise
     */
    boolean isItalic() {
        return italic;
    }

    /**
     * Checks if the given font is the counterpart of the current form in the sense of the given attribute
     * 
     * @param attribute The attribute, can be one of BOLD, ITALIC, BIG or SMALL
     * @param font The font to check
     * @return true if the font is a counterpart, false otherwise
     */
    boolean isCounterpart(int attribute,HTMLFont font) {
        switch(attribute) {
            case BOLD: return isBoldCounterpart(font);
            case ITALIC: return isItalicCounterpart(font);
            case BIG: return isBigCounterpart(font);
            case SMALL: return isSmallCounterpart(font);
            default:
                return false;
        }
    }

    /**
     * Utility method that first checks that neither font family is null and then compares them
     * 
     * @param font The other font to check
     * @return true if the family of the compared font and this font are identical (and both are not null), false otherwise
     */
    boolean isSameFamily(HTMLFont font) {
        return ((family!=null) && (font.getFamily()!=null) && (family.equals(font.getFamily())));
    }


    /**
     * Checks if the specified font is a big counterpart of this font.
     * 
     * @param font The font to check
     * @return true if this is a big couterpart, false otherwise
     */
    private boolean isBigCounterpart(HTMLFont font) {
        if (size<=font.getSize()) {
            return false;
        }
        return  (isSameFamily(font) && (style==font.getStyle()));
    }

    /**
     * Checks if the specified font is a small counterpart of this font.
     *
     * @param font The font to check
     * @return true if this is a small couterpart, false otherwise
     */
    private boolean isSmallCounterpart(HTMLFont font) {
        if (size>=font.getSize()) {
            return false;
        }
        return  (isSameFamily(font) && (style==font.getStyle()));
    }

    /**
     * Checks if the specified font is a bold counterpart of this font.
     *
     * @param font The font to check
     * @return true if this is a bold couterpart, false otherwise
     */

    private boolean isBoldCounterpart(HTMLFont font) {
        if (!bold) {
            return false;
        }
        return  ((size==font.getSize()) && (isSameFamily(font)) && (italic==font.isItalic()));
    }

    /**
     * Checks if the specified font is an italic counterpart of this font.
     *
     * @param font The font to check
     * @return true if this is an italic couterpart, false otherwise
     */
    private boolean isItalicCounterpart(HTMLFont font) {
        if (!italic) {
            return false;
        }
        return  ((size==font.getSize()) && (isSameFamily(font)) && (bold==font.isBold()));
    }


    /**
     * Returns the font family (i.e. Arial, Times New Roman)
     * 
     * @return the string describing this font family
     */
    String getFamily() {
        return family;
    }

    /**
     * Returns the Codename One font wrapped in this HTMLFont
     * 
     * @return the Codename One font wrapped in this HTMLFont
     */
    Font getFont() {
        return font;
    }

    /**
     * Returns the counterpart font for this font in the given attribute.
     * This method either creates it or fetches it if it was already set. It handles both system and bitmap fonts.
     * Note that a counterpart font can be the font itself if no other suitable font was found.
     *
     * @param attribute The requested counterpart attribute
     * @return the counterpart font for this font in the given attribute
     */
    HTMLFont getCounterpartFont(int attribute) {
        if ((systemFont) && (counterpartFonts[attribute]==null)) { //Note that bold counterpart for a bold font is already set as the font itself in the constructor and so on for all attributes
            switch(attribute) {
                case BOLD:
                    counterpartFonts[attribute]=new HTMLFont(null, Font.createSystemFont(font.getFace(), style+Font.STYLE_BOLD, size));
                    break;
                case ITALIC:
                    counterpartFonts[attribute]=new HTMLFont(null, Font.createSystemFont(font.getFace(), style+Font.STYLE_ITALIC, size));
                    break;
                case BIG:
                    //If current form's size is medium then the big counterpart is large, if it's small then medium
                    int counterpartSize=(size==Font.SIZE_SMALL)?Font.SIZE_MEDIUM:Font.SIZE_LARGE;
                    counterpartFonts[attribute]=new HTMLFont(null, Font.createSystemFont(font.getFace(), style, counterpartSize));
                    break;
                case SMALL:
                    //If current form's size is medium then the small counterpart is small, if it's large then medium
                    counterpartSize=(size==Font.SIZE_LARGE)?Font.SIZE_MEDIUM:Font.SIZE_SMALL;
                    counterpartFonts[attribute]=new HTMLFont(null, Font.createSystemFont(font.getFace(), style, counterpartSize));
                    break;
            }
        }
        return counterpartFonts[attribute];
    }

    /**
     * Sets the specified font as the counterpart of this font in the given attribute
     *
     * @param attribute The attribute in which the specified font is the counterpart of this font.
     * @param counterpartFont The counter part font
     */
    void setCounterpartFont(int attribute,HTMLFont counterpartFont) {
        counterpartFonts[attribute]=counterpartFont;
    }



    // font "interface" methods

    /**
     * Return the total height of the font
     *
     * @return the total height of the font
     */
    int getHeight() {
        return font.getHeight();
    }

    /**
     * Return the width of the given string in this font instance
     *
     * @param str the given string     *
     * @return the width of the given string in this font instance
     */
    int stringWidth(String str) {
        return font.stringWidth(str);
    }

    /**
     * Return Optional operation returning the font face for system fonts
     *
     * @return Optional operation returning the font face for system fonts
     */
    int getFace() {
        return font.getFace();
    }

    /**
     * Returns the style of the font
     * 
     * @return STYLE_PLAIN, STYLE_BOLD or STYLE_ITALIC or STYLE_BOLD | STYLE_ITALIC
     */
    int getStyle() {
        return style;
    }

    /**
     * Returns the font size. For system fonts this would be either SIZE_SMALL, SIZE_MEDIUM or SIZE_LARGE.
     * For bitmap font this would be the exact size as defined in the font key.
     * 
     * @return the font size
     */
    int getSize() {
        return size;
    }

    /**
     * Returns the font size in pixels. For system fonts an estimation is made.
     *
     * @return the font size in pixels
     */
    int getSizeInPixels() {
        if (systemFont) {
            return font.getHeight()-2; //estimation - usually a font's hright is bigger in a few pixels than the size
        } else {
            return size;
        }
    }


}
