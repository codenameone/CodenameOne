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
package com.codename1.ui.plaf;

import com.codename1.io.Util;
import com.codename1.l10n.L10NManager;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Image;
import com.codename1.ui.util.Resources;
import com.codename1.util.CaseInsensitiveOrder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses Style strings into StyleInfo objects, which can be converted to Style objects at runtime. 
 * <p>This class
 * is the basis for the inline style functionality: </p>
 * <ul>
 * <li>{@link Component#setInlineAllStyles(java.lang.String) }</li>
 * <li>{@link Component#getInlineAllStyles() }</li>
 * <li>{@link Component#setInlineSelectedStyles(java.lang.String) }</li>
 * <li>{@link Component#getInlineSelectedStyles() }</li>
 * <li>etc..</li>
 * </ul>
 * 
 * <p>Style strings are strings which describe a style, and are in a particular format that {@link StyleParser} knows how to 
 * parse.  The general format of a style string is {@literal key1:value1; key2:value2; ... keyn:valuen;}.  I.e. a set of key-value pairs 
 * with pairs separated by semi-colons, and keys and values separated by a colon.  This is very similar to CSS, but it is <strong>NOT</strong> CSS.
 * Style string keys and values are closely related to the properties of the {@link Style} class and their associated values.
 * </p>
 * <h3>Supported Keys</h3>
 * 
 * <p>The following keys are supported:</p>
 * <ul>
 *   <li>{@literal fgColor} - The foreground color as a hex string.  E.g. {@literal ff0000}.</li>
 *   <li>{@literal bgColor} - The background color as a hex string. E.g. {@literal ff0000}.</li>
 *   <li>{@literal transparency} - The background transparency as an integer. 0-255.</li>
 *   <li>{@literal textDecoration} - The text decoration.  One of {@literal underline}, {@literal overline}, 
 *      {@literal 3d}, {@literal 3d_lowered}, {@literal 3d_shadow_north}, {@literal strikethru}, or {@literal none}.</li>
 *   <li>{@literal opacity} - The opacity as an integer.  0-255</li>
 *   <li>{@literal padding} - The padding as a sequence of 1, 2, 3, or 4 values.  See "Padding and Margin Strings" below for details on the format.</li>
 *   <li>{@literal margin} - The margin as a sequence of 1, 2, 3, or 4, values. See "Padding and Margin Strings" below for details on the format.</li>
 *   <li>{@literal font} - The font.  See "Font Strings" below for details on the format.</li>
 *   <li>{@literal border} - The border.  See "Border Strings" below for details on the format.</li>
 *   <li>{@literal bgType} - The background type. See "Background Type Values" below for details on the available options.</li>
 * </ul>
 * 
 * <h3>Padding and Margin Strings</h3>
 * <p>The {@literal padding} and {@literal margin} keys can take values the same format as is used for CSS {@literal margin} and {@literal padding} directives. That is the value
 *   can be expressed as a space-separated sequence of scalar values (i.e. float values with a unit suffix).  Some examples:</p>
 * <ul>
 * <li>{@literal padding:0px} - Sets all padding to zero pixels.</li>
 * <li>{@literal padding:2mm 1px} - Sets vertical padding to 2 millimetres, and horizontal padding to 1 pixel.</li>
 * <li>{@literal padding:2mm 1px inherit} - Sets top padding to 1 millimetres,  horizontal padding to 1 pixel, and bottom padding to inherit the parent style's bottom padding.</li>
 * <li>{@literal padding:1mm 2px inherit 4mm} - Top padding=1 millimetre.  Right padding=2 pixels.  Bottom padding inherits parent style's bottom padding. Left padding=4 millimetres.</li>
 * </ul>
 * 
 * <p>All of the examples above use {@literal padding}, but the same format is used for {@literal margin}.  They demonstrate the use of 1, 2, 3, and 4 value sequences, and their meaning. In general terms
 * these formats can be described as:</p>
 * <ul>
 * <li>{@literal <value> } - Sets padding on all sites to {@literal <value>}</li>
 * <li>{@literal <vertical> <horizontal> } - Top and bottom padding set to {@literal <vertical>}.  Left and right padding set to {@literal <horizontal>}</li>
 * <li>{@literal <top> <horizontal> <bottom> } - Top={@literal <top>}.  Left and right = {@literal <horizontal>}.  Bottom = {@literal bottom}.</li>
 * <li>{@literal <top> <right> <bottom> <left>} - Top={@literal <top>}. Right={@literal right}. Bottom={@literal bottom}. Left={@literal left}.  In other words, values applied clock-wise, starting on top side.</li>
 * </ul>
 * 
 * <h3>Font Strings</h3>
 * <p>Fonts strings can take any of the following formats:</p>
 * <ul>
 * <li>{@literal <size> <fontName> <fontFile>} - E.g. {@literal 3mm Arial.ttf /Arial.ttf} or {@literal 12px native:MainRegular native:MainRegular}</li>
 * <li>{@literal <size> <fontName>} - E.g. {@literal 3mm Arial.ttf} or {@literal 3mm native:ItalicBlack}.</li>
 * <li>{@literal <size>} - E.g. {@literal 3mm} or {@literal 12px}.  When only specifying the size, the font family will be dictated by the parent style.</li>
 * <li>{@literal <fontName> <fontFile>} - E.g. {@literal Arial.ttf /Arial.ttf} or {@literal native:MainBold native:MainBold}.  When omitting font size (as this format does), the size
 * is dictated by the parent style.</li>
 * <li>{@literal <fontName>|<fontFile>} - E.g. {@literal Arial.ttf} or {@literal /Arial.ttf}, or {@literal native:MainRegular}  Strings starting with a {@literal /} are assumed to be files.  The corresponding font name
 * is then derived by removing the slash and the trailing {@literal .ttf}.  When omitting font size (as this format does), the size is dictated by the parent style.</li>
 * </ul>
 * 
 * <h3>Border Strings</h3>
 * <p>The {@literal border} property accepts several different formats for its value.  This is due to the many different kinds of 
 * borders that can be created.  The following are some of the formats.</p>
 * 
 * <p><strong>Line Border</strong></p>
 * 
 * <p>{@literal <thickness> solid <color>} - E.g. {@literal 1mm solid ff0000}.  {@literal <thickness>} should be expressed as a scalar value with unit.  E.g. {@literal 1mm}, or {@literal 2px}.
 * {@literal <color>} should be an RGB hex string.  E.g {@literal ff0000} for red.</p>
 * 
 * <p><strong>Dashed Border</strong></p>
 * 
 * <p>{@literal <thickness> dashed <color>} - E.g. {@literal 1mm dashed ff0000}.  {@literal <thickness>} should be expressed as a scalar value with unit.  E.g. {@literal 1mm}, or {@literal 2px}.
 * {@literal <color>} should be an RGB hex string.  E.g {@literal ff0000} for red.</p>
 * 
 * <p><strong>Dotted Border</strong></p>
 * 
 * <p>{@literal <thickness> dotted <color>} - E.g. {@literal 1mm dotted ff0000}.  {@literal <thickness>} should be expressed as a scalar value with unit.  E.g. {@literal 1mm}, or {@literal 2px}.
 * {@literal <color>} should be an RGB hex string.  E.g {@literal ff0000} for red.</p>
 * 
 * <p><strong>Underline Border</strong></p>
 * 
 * <p>{@literal <thickness> underline <color>} - E.g. {@literal 1mm underline ff0000}.  {@literal <thickness>} should be expressed as a scalar value with unit.  E.g. {@literal 1mm}, or {@literal 2px}.
 * {@literal <color>} should be an RGB hex string.  E.g {@literal ff0000} for red.</p>
 * 
 * <p><strong>Image Border</strong></p>
 * 
 * <p>{@literal image <image1> <image2> ... <image9>} - A 9-piece image border.  The {@literal <image1>} .. {@literal <image9>} values are strings which refer to images either on the classpath, or in the theme resource file.
 * If the image string starts with {@literal /}, then it is assumed to be on the classpath.  The order of the images corresponds to the parameters of {@link Border#createImageBorder(com.codename1.ui.Image, com.codename1.ui.Image, com.codename1.ui.Image, com.codename1.ui.Image, com.codename1.ui.Image, com.codename1.ui.Image, com.codename1.ui.Image, com.codename1.ui.Image, com.codename1.ui.Image) }.</p>
 * 
 * <p>{@literal image <image1> <image2> <image3>} - A 9-piece image border, but with the images corresponding to the parameters of {@link Border#createImageBorder(com.codename1.ui.Image, com.codename1.ui.Image, com.codename1.ui.Image) }.</p>
 * <p>{@literal horizontalImage <leftImage> <rightImage> <centerImage>} - A 3-piece horizontal image border.  Image parameters correspond with {@link Border#createHorizonalImageBorder(com.codename1.ui.Image, com.codename1.ui.Image, com.codename1.ui.Image) } parameters.</p>
 * <p>{@literal verticalImage <topImage> <bottomImage> <centerImage>} - A 3-piece horizontal image border.  Image parameters correspond with {@link Border#createVerticalImageBorder(com.codename1.ui.Image, com.codename1.ui.Image, com.codename1.ui.Image) } parameters.</p>
 * <p>{@literal splicedImage <image> <topInset> <rightInset> <bottomInset> <leftInset>} - A 9-piece image border that is generated from a single image, but with inset values specifying where the image should be sliced to create the 9 sub-images.
 * <br>
 * <strong>Parameters:</strong>
 * </p>
 * <ul>
 * <li>{@literal <image>} The image to use.  If this begins with {@literal /}, then the image will be found on the classpath.  Otherwise it will be found in the theme resource file.</li>
 * <li>{@literal <topInset>}, {@literal <rightInset>}, {@literal <bottomInset>}, {@literal <leftInset>} - The insets along which {@literal <image>} is sliced to generate the 9-subimages.  These values are 
 * expressed as a floating point number between 0.0 and 1.0, where 1.0 is the full width or height of the image depending on the orientation (horizontal or vertical) or the inset.  If {@literal image}
 * is 100 pixels by 100 pixels, then a top inset of 0.4 would cause a slice to occur at 40 pixels from the top of the image (i.e. the top-left, top, and top-right slices would each be 40 pixels high.</li>
 * </ul>
 * 
 * @author shannah
 */
public class StyleParser {
    
    public static final byte UNIT_INHERIT=99;
    
    /**
     * Encapsulates a scalar value with a unit.
     */
    public static class ScalarValue {
        private byte unit;
        private double value;
        

        /**
         * Creates a new scalar value given magnitude and unit.
         * @param value The value to set.
         * @param unit The unit of the value. One of {@link #UNIT_INHERIT}, {@link Style#UNIT_TYPE_DIPS}, {@link Style#UNIT_TYPE_PIXELS}, or {@link Style#UNIT_TYPE_SCREEN_PERCENTAGE}.
         */
        public ScalarValue(double value, byte unit) {
            this.value = value;
            this.unit = unit;
        }
        
        public ScalarValue() {
        }
        
        /**
         * @return the unit of the value.  One of {@link #UNIT_INHERIT}, {@link Style#UNIT_TYPE_DIPS}, {@link Style#UNIT_TYPE_PIXELS}, or {@link Style#UNIT_TYPE_SCREEN_PERCENTAGE}.
         */
        public byte getUnit() {
            return unit;
        }

        /**
         * @param unit the unit of the value.  One of {@link #UNIT_INHERIT}, {@link Style#UNIT_TYPE_DIPS}, {@link Style#UNIT_TYPE_PIXELS}, or {@link Style#UNIT_TYPE_SCREEN_PERCENTAGE}.
         */
        public void setUnit(byte unit) {
            this.unit = unit;
        }

        /**
         * @return the value of the scalar.
         */
        public double getValue() {
            return value;
        }

        /**
         * @param value the value of the scalar.
         */
        public void setValue(double value) {
            this.value = value;
        }

        /**
         * Returns the scalar value in CN1 style string format.  E.g.  12mm, 3px, 5%, or inherit
         * @return 
         */
        @Override
        public String toString() {
            switch (unit) {
                case UNIT_INHERIT:
                    return "inherit";
                case Style.UNIT_TYPE_DIPS:
                    return value + "mm";
                case Style.UNIT_TYPE_SCREEN_PERCENTAGE:
                    return value + "%";
                default:
                    return ((int)Math.round(value))+"px";
            }
        }
        
        
    }
    
    /**
     * Encapculates a style string in structured format.
     */
    public static class StyleInfo {
        Map<String,String> values;
        
        /**
         * Parses the given style strings and encapsulates their details in 
         * a StyleInfo object.
         * @param styleString One or more style strings.
         */
        public StyleInfo(String... styleString) {
            if (styleString == null) {
                this.values = new HashMap<String,String>();
            } else {
                HashMap<String,String> vals = new HashMap<String,String>();
                for (String str : styleString) {
                    if (str != null && str.length() > 0) {
                        parseString(vals, str);
                    }
                }
                this.values = vals;
            }
        }
        
        /**
         * Creates a new StyleInfo given the parsed Map of keys and values.
         * @param values 
         */
        public StyleInfo(Map<String,String> values) {
            this.values = values;
        }
        
        /**
         * Creates a new StyleInfo.
         */
        public StyleInfo() {
            this(new HashMap<String,String>());
        }
        
        /**
         * Creates a new style info by copying styles from existing style info.
         * @param info Style to copy.
         */
        public StyleInfo(StyleInfo info) {
            this(info.toStyleString());
        }
        
        /**
         * 
         * @return The padding of the style.  Will return {@literal null} if padding wasn't specified.
         */
        public PaddingInfo getPadding() {
            if (values.containsKey("padding")) {
                return parsePadding(values.get("padding"));
            }
            return null;
        }
        
        /**
         * 
         * @return The margin of the style.  Will return {@literal null} if margin wasn't specified.
         */
        public MarginInfo getMargin() {
            if (values.containsKey("margin")) {
                return parseMargin(values.get("margin"));
            }
            return null;
        }
        
        /**
         * 
         * @return The border of the style.  Will return {@literal null} if border wasn't specified.
         */
        public BorderInfo getBorder() {
            if (values.containsKey("border")) {
                return parseBorder(new BorderInfo(), values.get("border"));
            }
            return null;
        }
        
        /**
         * 
         * @return The font of the style.  Will return {@literal null} if font wasn't specified.
         */
        public FontInfo getFont() {
            if (values.containsKey("font")) {
                return parseFont(new FontInfo(), values.get("font"));
            }
            return null;
        }
        
        /**
         * Sets the font in the style info.
         * @param font A valid font style string.  E.g. "2mm native:MainRegular"
         * @return Self for chaining.
         */
        public StyleInfo setFont(String font) {
            values.put("font", font);
            return this;
        }
        
        /**
         * Sets the font size in the style.
         * @param fontSize A valid font size.  E.g. "2mm"
         * @return Self for chaining.
         */
        public StyleInfo setFontSize(String fontSize) {
            FontInfo finfo = getFont();
            if (finfo == null) {
                finfo = parseFont(new FontInfo(), fontSize);
                if (finfo != null) {
                    setFont(finfo.toString());
                }
            } else {
                FontInfo tmp = parseFont(new FontInfo(), fontSize);
                if (tmp != null) {
                    finfo.setSize(tmp.getSize());
                    finfo.setSizeUnit(tmp.getSizeUnit());
                    setFont(finfo.toString());
                }
            }
            
            return this;
        }
        
        /**
         * Sets the font name.
         * @param fontName A valid font name.  E.g. "native:MainRegular"
         * @return Self for chaining.
         */
        public StyleInfo setFontName(String fontName) {
            FontInfo finfo = getFont();
            if (finfo == null) {
                finfo = parseFont(new FontInfo(), fontName);
                if (finfo != null) {
                    setFont(finfo.toString());
                }
            } else {
                FontInfo tmp = parseFont(new FontInfo(), fontName);
                if (tmp != null) {
                    finfo.setName(tmp.getName());
                    finfo.setFile(tmp.getFile());
                    setFont(finfo.toString());
                } else {
                    finfo.setName(null);
                    finfo.setFile(null);
                    setFont(finfo.toString());
                }
            }
            return this;
        }
        
        
        
        /**
         * 
         * @return The background color of the style.  Will return {@literal null} if bgColor wasn't specified.
         */
        public Integer getBgColor() {
            if (values.containsKey("bgColor")) {
                return Integer.parseInt(values.get("bgColor"), 16);
            }
            return null;
        }
        
        /**
         * 
         * @return The foreground color of the style.  Will return {@literal null} if fgColor wasn't specified.
         */
        public Integer getFgColor() {
            if (values.containsKey("fgColor")) {
                return Integer.parseInt(values.get("fgColor"), 16);
            }
            return null;
        }
        
        /**
         * Sets the foreground color.
         * @param fgColor A valid color string.  E.g. "ff0000"
         * @return Self for chaining.
         */
        public StyleInfo setFgColor(String fgColor) {
            if (fgColor == null || fgColor.trim().length() == 0) {
                values.remove("fgColor");
            } else {
                values.put("fgColor", fgColor);
            }
            return this;
        }
        
        /**
         * Sets the background color.
         * @param bgColor A valid color string.  E.g. "ff0000"
         * @return 
         */
        public StyleInfo setBgColor(String bgColor) {
            if (bgColor == null || bgColor.trim().length() == 0) {
                values.remove("bgColor");
            } else {
                values.put("bgColor", bgColor);
            }
            return this;
        }
        
        /**
         * Sets the transparency.
         * @param transparency A valid transparency string (0-255).  E.g. "255"
         * @return Self for chaining.
         */
        public StyleInfo setTransparency(String transparency) {
            if (transparency == null || transparency.trim().length() == 0) {
                values.remove("transparency");
            } else {
                values.put("transparency", transparency);
            }
            return this;
        }
        
        /**
         * Sets the opacity.
         * @param opacity A valid opacity string (0-255).  E.g. "255"
         * @return Self for chaining.
         */
        public StyleInfo setOpacity(String opacity) {
            if (opacity == null || opacity.trim().length() == 0) {
                values.remove("opacity");
            } else {
                values.put("opacity", opacity);
            }
            return this;
        }
        
        /**
         * Sets the padding.
         * @param padding A valid padding string.  E.g. "1mm", or "2mm 3px 0 5mm"
         * @return Self for chaining.
         */
        public StyleInfo setPadding(String padding) {
            if (padding == null || padding.trim().length() == 0) {
                values.remove("padding");
            } else {
                values.put("padding", padding);
            }
            return this;
        }
        
        /**
         * Sets the margin.
         * @param margin A valid margin string.  E.g. "1mm", or "2mm 3px 0 0"
         * @return Self for chaining.
         */
        public StyleInfo setMargin(String margin) {
            if (margin == null || margin.trim().length() == 0) {
                values.remove("margin");
            } else {
                values.put("margin", margin);
            }
            return this;
        }
        
        /**
         * Sets the border
         * @param border A valid border string. E.g. "1px solid ff0000", or "splicedImage notes.png 0.25 0.25 0.25 0.25"
         * @return Self for chaining.
         */
        public StyleInfo setBorder(String border) {
            if (border == null || border.trim().length() == 0) {
                values.remove("border");
            } else {
                values.put("border", border);
            }
            return this;
        }
        
        /**
         * 
         * @return The background transparency of the style.  Will return {@literal null} if transparency wasn't specified.
         */
        public Integer getTransparency() {
            if (values.containsKey("transparency")) {
                return Integer.parseInt(values.get("transparency"));
            }
            return null;
        }
        
        /**
         * 
         * @return The opacity of the style.  Will return {@literal null} if the opacity wasn't specified.
         */
        public Integer getOpacity() {
            if (values.containsKey("opacity")) {
                return Integer.parseInt(values.get("opacity"));
            }
            return null;
        }
        
        /**
         * 
         * @return The alignment of the style.  One of {@link Component#LEFT}, {@link Component#RIGHT}, {@link Component#CENTER}.  Or {@literal null} if 
         * alignment wasn't specified.
         */
        public Integer getAlignment() {
            if (values.containsKey("alignment")) {
                return parseAlignment(values.get("alignment"));
            }
            return null;
        }
        
        /**
         * Returns the alignment as a string.  "center", "left", "right", or null.
         * @return 
         */
        public String getAlignmentAsString() {
            return getAlignmentString(getAlignment());
        }
        
        /**
         * Gets the background type.  Will return {@literal null} if bgType wasn't specified.  Returns one of {@literal Style.BACKGROUND_XXX} constants.
         * @return 
         */
        public Integer getBgType() {
            if (values.containsKey("bgType")) {
                return parseBgType(values.get("bgType"));
            }
            return null;
        }
        
        /**
         * Sets the background type as a string.
         * @param type A valid background type string.  E.g. "image_scaled_fit"
         * @return Self for chaining.
         */
        public StyleInfo setBgType(String type) {
            values.put("bgType", type);
            return this;
        }
        
        /**
         * Sets the background type as one of the Style.BACKGROUND_XXX constants.
         * @param i A background type.  One of Style.BACKGROUND_XXX constants.
         * @return Self for chaining.
         */
        public StyleInfo setBgType(Integer i) {
            setBgType(flip(bgTypes).get(i));
            return this;
        }
        
        /**
         * Gets the bgType as a string.
         * @return 
         */
        public String getBgTypeAsString() {
            Integer bgType = getBgType();
            if (bgType == null) {
                return null;
            }
            Map<Integer,String> inverseMap = flip(bgTypes());
            if (inverseMap.containsKey(bgType)) {
                return inverseMap.get(bgType);
            }
            return null;
        }
        
        /**
         * 
         * @return The background image for the style.  Will return {@literal null} if bgImage wasn't specified.
         */
        public ImageInfo getBgImage() {
            if (values.containsKey("bgImage")) {
                ImageInfo out = new ImageInfo(values.get("bgImage"));
                
                return out;
            }
            return null;
            
        }
        
        /**
         * Sets the background image.
         * @param bgImage A valid image string.  E.g. "notes.png" (to refer to the notes.png image in the theme), or "/notes.png" to refer to notes.png in the src directory.
         * @return Self for chaining.
         */
        public StyleInfo setBgImage(String bgImage) {
            values.put("bgImage", bgImage);
            return this;
        }
        
        /**
         * 
         * @return The text decoration of the style.  Will return {@literal null} if textDecoration wasn't specified.  Returns one of {@literal Style.TEXT_DECORATION_XXX} constants.
         */
        public Integer getTextDecoration() {
            if (values.containsKey("textDecoration")) {
                String val = values.get("textDecoration");
                if ("3d".equalsIgnoreCase(val)) {
                    return (int)Style.TEXT_DECORATION_3D;
                }
                
                if ("3d_lowered".equalsIgnoreCase(val)) {
                    return (int)Style.TEXT_DECORATION_3D_LOWERED;
                }
                
                if ("3D_SHADOW_NORTH".equalsIgnoreCase(val)) {
                    return (int)Style.TEXT_DECORATION_3D_SHADOW_NORTH;
                }
                
                if ("none".equalsIgnoreCase(val)) {
                    return (int)Style.TEXT_DECORATION_NONE;
                }
                
                if ("overline".equalsIgnoreCase(val)) {
                    return (int)Style.TEXT_DECORATION_OVERLINE;
                }
                
                if ("strikethru".equalsIgnoreCase(val)) {
                    return (int)Style.TEXT_DECORATION_STRIKETHRU;
                }
                
                if ("underine".equalsIgnoreCase(val)) {
                    return (int)Style.TEXT_DECORATION_UNDERLINE;
                }
                
            }
            return null;
        }
        
        /**
         * Gets the text decoration as a string.
         * @return A valid text decoration string or null.  Text decoration strings include "3d", "3d_lowered", "3d_shadow_north", "none", "strikethru", "overline", and "underline"
         */
        public String getTextDecorationAsString() {
            if (values.containsKey("textDecoration")) {
                return values.get("textDecoration");
            }
            return null;
        }
        
        /**
         * Builds a style string that is encapsulated by this style object.  The output of this can be passed to methods like {@link Component#setInlineAllStyles(java.lang.String) }
         * @return A style string.
         */
        public String toStyleString() {
            StringBuilder sb = new StringBuilder();
            FontInfo finfo = getFont();
            if (finfo != null) {
                sb.append("font:").append(finfo.toString()).append("; ");
            }
            BorderInfo binfo = getBorder();
            if (binfo != null) {
                sb.append("border:").append(binfo.toString()).append("; ");
            }
            
            Integer bgColor = getBgColor();
            if (bgColor != null) {
                sb.append("bgColor:").append(Integer.toHexString(bgColor)).append("; ");
            }
            
            Integer fgColor = getFgColor();
            if (fgColor != null) {
                sb.append("fgColor:").append(Integer.toHexString(fgColor)).append("; ");
            }
            
            Integer transparency = getTransparency();
            if (transparency != null) {
                sb.append("transparency:").append(transparency).append("; ");
            }
            
            Integer opacity = getOpacity();
            if (opacity != null) {
                sb.append("opacity:").append(opacity).append("; ");
            }
            
            Integer bgType = getBgType();
            if (bgType != null) {
                sb.append("bgType:").append(getBgTypeAsString()).append("; ");
            }
            
            ImageInfo image = getBgImage();
            if (image != null) {
                sb.append("bgImage:").append(getBgImage().toString()).append("; ");
            }
            
            Integer alignment = getAlignment();
            if (alignment != null) {
                sb.append("alignment:").append(getAlignmentAsString()).append("; ");
            }
            
            MarginInfo margin = getMargin();
            if (margin != null) {
                sb.append("margin:").append(margin.toString()).append("; ");
            }
            
            PaddingInfo padding = getPadding();
            if (padding != null) {
                sb.append("padding:").append(padding.toString()).append("; ");
            }
            
            Integer textDecoration = getTextDecoration();
            if (textDecoration != null) {
                sb.append("textDecoration:").append(getTextDecorationAsString()).append("; ");
            }
            
            return sb.toString().trim();
        }
    }
    
    /**
     * Encapsulates an image that is referenced by a style string.
     */
    public static class ImageInfo {
        private String image;
        
        /**
         * Creates an ImageInfo to wrap the specified image.
         * @param image Either the path to an image on the classpath (signified by a leading {@literal '/'}, or
         * the name of an image that can be found in the theme resource file.
         */
        public ImageInfo(String image) {
            this.image = image;
        }
        
        @Override
        public String toString() {
            return image;
        }
        
        /**
         * Gets the image object that this image info references.
         * @param theme The theme resource file to use to get the image.  Note: If the image name has a leading {@literal '/'}, then 
         * the image will be loaded from the classpath.  Otherwise the theme resource file will be used.
         * @return 
         */
        public Image getImage(Resources theme) {
            return StyleParser.getImage(theme, image);
        }
    }
    
    
    /**
     * Parses a style string into a Map
     * @param out
     * @param str
     * @return 
     */
    static StyleInfo parseString(StyleInfo out, String str) {
        Map<String,String> map = parseString(new HashMap<String,String>(), str);
        for (Map.Entry<String,String> e : map.entrySet()) {
            out.values.put(e.getKey(), e.getValue());
        }
        return out;
    }
    
    
    static StyleInfo parseString(String str) {
        return parseString(new StyleInfo(), str);
    }
    
    
    static Map<String,String> parseString(Map<String,String> out, String str) {
        String[] rules = Util.split(str, ";");
        for (String rule : rules) {
            rule = rule.trim();
            if (rule.length() == 0) {
                continue;
            }
            int pos = rule.indexOf(":");
            if (pos == -1) {
                continue;
            }
            String key = rule.substring(0, pos);
            out.put(key, rule.substring(pos+1));

        }
        return out;
    }
    
    /**
     * Base class for style values that consist of 4 scalar values, such as padding and margin.
     */
    public static class BoxInfo {
        protected ScalarValue[] values;

        /**
         * Creates a new box with the specified scalar values.
         * @param values A 4-element array of scalar values.
         */
        public BoxInfo(ScalarValue[] values) {
            if (values.length != 4) throw new IllegalArgumentException("BoxInfo expected 4-element array");
            this.values = values;
        }

        /**
         * Returns string of values in {@literal <top> <right> <bottom> <left>} format.
         * @return 
         */
        @Override
        public String toString() {
            return toString(Component.TOP)+" "+toString(Component.RIGHT)+" "+toString(Component.BOTTOM)+" "+toString(Component.LEFT);
        }
        
        /**
         * Returns the string representation of one of the sides of the box.
         * @param side One of {@link Component#TOP}, {@link Component#RIGHT}, {@link Component#BOTTOM}, {@link Component#LEFT}.
         * @return 
         */
        public String toString(int side) {
            return values[side].toString();
        }
        
        /**
         * Gets the scalar values of this box as a 4-element array.
         * @return 
         */
        public ScalarValue[] getValues() {
            return values;
        }
        
        /**
         * Sets the scalar values of this box as a 4-element array.
         * @param values 
         */
        public void setValues(ScalarValue[] values) {
            if (values.length != 4) {
                throw new IllegalArgumentException("BoxInfo requires array with 4 values.");
            }
            this.values = values;
        }
        
        /**
         * Gets a value for a side.
         * @param side One of {@link Component#TOP}, {@link Component#RIGHT}, {@link Component#BOTTOM}, {@link Component#LEFT}.
         * @return The value portion of the scalar value.
         */
        public ScalarValue getValue(int side) {
            return values[side];
        }
        
    }
    
    /**
     * Encapsulates information about the padding in a style string.
     */
    public static class PaddingInfo extends BoxInfo {
        
        /**
         * Creates a new PaddingInfo.
         * @param values 4-element array of scalar values.  Indices are {@link Component#TOP}, {@link Component#BOTTOM}, {@link Component#LEFT} and {@link Component#RIGHT}.
         */
        public PaddingInfo(ScalarValue[] values) {
            super(values);
        }
        
        /**
         * Generates a 4-element float array with the padding values - in the same format as used in the theme resource files.  This will use the provided base style
         * to calculate the padding for sides that were specified as {@literal inherit}.
         * @param baseStyle The base style used to get the padding on sides where the style string specified {@literal inherit}.
         * @return 4-element float array.
         */
        float[] createPadding(Style baseStyle) {
            float[] out = new float[4];
            for (int i=0; i<4; i++) {
                out[i] = createPadding(baseStyle, i);
            }
            return out;
            
        }
        
        float createPadding(Style baseStyle, int side) {
            ScalarValue v = values[side];
            switch (v.unit) {
                case UNIT_INHERIT: {
                    int px = baseStyle.getPadding(side);
                    byte[] units = baseStyle.getPaddingUnit();
                    switch (units[side]) {
                        case Style.UNIT_TYPE_DIPS:
                            return px / (float)Display.getInstance().convertToPixels(1f);
                        default:
                            return px;
                    }
                }
                default:
                    return (float)v.value;
                    
            }
                    
        }
        
        byte[] createPaddingUnit(Style baseStyle) {
            byte[] out = new byte[4];
            for (int i=0; i<4; i++) {
                out[i] = createPaddingUnit(baseStyle, i);
            }
            return out;
            
        }
        
        byte createPaddingUnit(Style baseStyle, int side) {
            ScalarValue v = values[side];
            switch (v.unit) {
                case UNIT_INHERIT: {
                    byte[] units = baseStyle.getPaddingUnit();
                    return units[side];
                }
                default:
                    return v.unit;
                    
            }
        }
    }
    
    /**
     * Encapsulates information about the padding in a style string.
     */
    public static class MarginInfo extends BoxInfo {
        /**
         * Creates a new MarginInfo.
         * @param values 4-element array of scalar values.  Indices are {@link Component#TOP}, {@link Component#BOTTOM}, {@link Component#LEFT} and {@link Component#RIGHT}.
         */
        public MarginInfo(ScalarValue[] values) {
            super(values);
        }
        
        float[] createMargin(Style baseStyle) {
            float[] out = new float[4];
            for (int i=0; i<4; i++) {
                out[i] = createMargin(baseStyle, i);
            }
            return out;
            
        }
        
        float createMargin(Style baseStyle, int side) {
            ScalarValue v = values[side];
            switch (v.unit) {
                case UNIT_INHERIT: {
                    int px = baseStyle.getPadding(side);
                    byte[] units = baseStyle.getMarginUnit();
                    switch (units[side]) {
                        case Style.UNIT_TYPE_DIPS:
                            return px / (float)Display.getInstance().convertToPixels(1f);
                        default:
                            return px;
                    }
                }
                default:
                    return (float)v.value;
                    
            }
                    
        }
        
        byte[] createMarginUnit(Style baseStyle) {
            byte[] out = new byte[4];
            for (int i=0; i<4; i++) {
                out[i] = createMarginUnit(baseStyle, i);
            }
            return out;
            
        }
        
        byte createMarginUnit(Style baseStyle, int side) {
            ScalarValue v = values[side];
            switch (v.unit) {
                case UNIT_INHERIT: {
                    byte[] units = baseStyle.getMarginUnit();
                    return units[side];
                }
                default:
                    return v.unit;
                    
            }
        }
    }
    
    static MarginInfo parseMargin(MarginInfo out, String margin) {
        out.setValues(parseTRBLValue(margin));
        return out;
    }
    
    static MarginInfo parseMargin(String margin) {
        return new MarginInfo(parseTRBLValue(margin));
    }
    
    static String parseMargin(Style baseStyle, String margin) {
        ScalarValue[] vals = parseTRBLValue(margin);
        StringBuilder sb = new StringBuilder();
        if (vals[Component.TOP].getUnit() == UNIT_INHERIT) {
            sb.append(baseStyle == null ? 0 : baseStyle.margin[Component.TOP]);
        } else {
            sb.append(vals[Component.TOP].getValue());
        }
        sb.append(",");
        if (vals[Component.BOTTOM].getUnit() == UNIT_INHERIT) {
            sb.append(baseStyle == null ? 0 : baseStyle.margin[Component.BOTTOM]);
        } else {
            sb.append(vals[Component.BOTTOM].getValue());
        }
        sb.append(",");
        if (vals[Component.LEFT].getUnit() == UNIT_INHERIT) {
            sb.append(baseStyle == null ? 0 : baseStyle.margin[Component.LEFT]);
        } else {
            sb.append(vals[Component.LEFT].getValue());
        }
        sb.append(",");
        if (vals[Component.RIGHT].getUnit() == UNIT_INHERIT) {
            sb.append(baseStyle == null ? 0 : baseStyle.margin[Component.RIGHT]);
        } else {
            sb.append(vals[Component.RIGHT].getValue());
        }
        return sb.toString();
    }
    
    static PaddingInfo parsePadding(PaddingInfo out, String padding) {
        out.setValues(parseTRBLValue(padding));
        return out;
    }
    
    static PaddingInfo parsePadding(String padding) {
        return new PaddingInfo(parseTRBLValue(padding));
    }
    
    static String parsePadding(Style baseStyle, String padding) {
        ScalarValue[] vals = parseTRBLValue(padding);
        StringBuilder sb = new StringBuilder();
        if (vals[Component.TOP].getUnit() == UNIT_INHERIT) {
            sb.append(baseStyle == null ? 0 : baseStyle.padding[Component.TOP]);
        } else {
            sb.append(vals[Component.TOP].getValue());
        }
        sb.append(",");
        if (vals[Component.BOTTOM].getUnit() == UNIT_INHERIT) {
            sb.append(baseStyle == null ? 0 : baseStyle.padding[Component.BOTTOM]);
        } else {
            sb.append(vals[Component.BOTTOM].getValue());
        }
        sb.append(",");
        if (vals[Component.LEFT].getUnit() == UNIT_INHERIT) {
            sb.append(baseStyle == null ? 0 : baseStyle.padding[Component.LEFT]);
        } else {
            sb.append(vals[Component.LEFT].getValue());
        }
        sb.append(",");
        if (vals[Component.RIGHT].getUnit() == UNIT_INHERIT) {
            sb.append(baseStyle == null ? 0 : baseStyle.padding[Component.RIGHT]);
        } else {
            sb.append(vals[Component.RIGHT].getValue());
        }
        return sb.toString();
    }
    
    
    
    static byte[] parsePaddingUnit(Style baseStyle, String padding) {
        ScalarValue[] vals = parseTRBLValue(padding);
        byte[] out = new byte[4];
        for (int i=0; i<4; i++) {
            if (vals[i].getUnit() == UNIT_INHERIT) {
                out[i] = baseStyle == null ? Style.UNIT_TYPE_PIXELS : baseStyle.paddingUnit[i];
            } else {
                out[i] = vals[i].getUnit();
            }
        }
        return out;
    }
    
    static byte[] parseMarginUnit(Style baseStyle, String margin) {
        ScalarValue[] vals = parseTRBLValue(margin);
        byte[] out = new byte[4];
        for (int i=0; i<4; i++) {
            if (vals[i].getUnit() == UNIT_INHERIT) {
                out[i] = baseStyle == null ? Style.UNIT_TYPE_PIXELS : baseStyle.marginUnit[i];
            } else {
                out[i] = vals[i].getUnit();
            }
        }
        return out;
    }
    
    static Integer parseAlignment(String alignment) {
        if (Character.isDigit(alignment.charAt(0))) {
            return Integer.parseInt(alignment);
        } else if ("center".equalsIgnoreCase(alignment)) {
            return Component.CENTER;
        } else if ("left".equalsIgnoreCase(alignment)) {
            return Component.LEFT;
        } else if ("right".equalsIgnoreCase(alignment)) {
            return Component.RIGHT;
        }
        return null;
    }
    
    private static String getAlignmentString(Integer alignment) {
        if (alignment == null) {
            return null;
        }
        switch (alignment) {
            case Component.CENTER: return "center";
            case Component.LEFT: return "left";
            case Component.RIGHT: return "right";
        }
        return null;
    }
    
    private static int getPixelValue(String val) {
        ScalarValue v = parseSingleTRBLValue(val);
        switch (v.getUnit()) {
            case Style.UNIT_TYPE_PIXELS:
                return (int)Math.round(v.getValue());
            case Style.UNIT_TYPE_DIPS:
                return Display.getInstance().convertToPixels((float)v.getValue());
            case Style.UNIT_TYPE_SCREEN_PERCENTAGE :
                return (int)Math.round(Display.getInstance().getDisplayWidth() * v.getValue() / 100.0);
        }
        return 0;
    }
    static BorderInfo parseBorder(BorderInfo out, String args) {
        if (args == null) {
            out.setType("empty");
            return out;
        }
        args = args.trim();
        if ("none".equals(args)) {
            out.setType("empty");
            return out;
        }
        String[] parts1 = Util.split(args, " ");
        int plen = parts1.length;
        if (plen == 0) {
            out.setType("empty");
            return out;
        }
        
        if (plen > 3 && ("image".equals(parts1[0]) || "horizontalImage".equals(parts1[0]) || "verticalImage".equals(parts1[0]))) {
            out.setType(parts1[0]);

            out.setImages(new String[plen-1]);
            for (int i=1; i<plen; i++) {
                out.getImages()[i-1] = parts1[i];
            }
            return out;
        }
        
        if ("splicedImage".equals(parts1[0]) && plen == 6) {
            out.setType(parts1[0]);
            out.setSpliceImage(parts1[1]);
            out.setSpliceInsets(parts1[2]+" "+parts1[3]+" "+parts1[4]+" "+parts1[5]);
            return out;
        }
        
        if (plen == 3) {
            String type = parts1[1];
            out.setColor((Integer) Integer.parseInt(parts1[2], 16));
            ScalarValue thicknessVal = parseSingleTRBLValue(parts1[0]);
            out.setWidth((Float) (float)thicknessVal.getValue());
            out.setWidthUnit(thicknessVal.getUnit());
            if (("solid".equals(type) || "line".equals(type))) {
                out.setType("line");
            } else if ("dashed".equals(type)) {
                out.setType("dashed");
            } else if ("dotted".equals(type)) {
                out.setType("dotted");
            } else if ("underline".equals(type)) {
                out.setType("underline");
            }
            return out;
        }
        
        out.setType("empty");
        return out;
        
    }
    static Border parseBorder(Resources theme, String args) {
        BorderInfo info = parseBorder(new BorderInfo(), args);
        if (info == null) {
            return null;
        }
        return info.createBorder(theme);
       
    }
    
    private static Image getImage(Resources theme, String imageStr) {
        Image im = null;
                
        try {

            if (imageStr.startsWith("/")) {
                im = Image.createImage(imageStr);
            } else {
                im = theme.getImage((String) imageStr);
            }


        } catch (IOException ex) {
            System.out.println("failed to parse image");
        }
        return im;
    }
    
    static Integer parseTextDecoration(String decoration) {
        return null;
    }
    
    /**
     * Encapsulates information about the {@literal border} property of a style string.
     */
    public static class BorderInfo {
        /**
         * The type of the border.  E.g. {@literal line}, {@literal dashed}, {@literal image}, etc..
         */
        private String type;
        
        /**
         * Used only by splicedImage border.  The name/path of the image to use for the image border.
         */
        private String spliceImage;
        
        /**
         * Used by image, horizontalImage, and verticalImage borders.  The names of images to use for these types of borders.
         */
        private String[] images;
        
        /**
         * The inset string for a splicedImage border.
         */
        private String spliceInsets;
        
        /**
         * The thickness for line/dashed/dotted/underline border
         */
        private Float width;
        
        /**
         * The unit for line/dashed/dotted/underline border
         */
        private byte widthUnit;
        
        /**
         * The color for a line/dashed/dotted/underline border.
         */
        private Integer color;

        /**
         * Returns the border as a style string value.  This value is formatted in a way that can be parsed by the StyleParser.
         * @return 
         */
        @Override
        public String toString() {
            if ("splicedImage".equals(getType())) {
                return getType() + " " + getSpliceImage() + " "+BorderInfo.this.getSpliceInsets();
            } else if ("image".equals(getType()) || "horizontalImage".equals(getType()) || "verticalImage".equals(getType())) {
                StringBuilder sb = new StringBuilder();
                sb.append(getType()).append(" ");
                for (String img: getImages()) {
                    sb.append(img).append(" ");
                }
                return sb.toString().trim();
            } else if ("line".equals(getType()) || "dashed".equals(getType()) || "dotted".equals(getType()) || "underline".equals(getType())) {
                int color = getColor() == null ? 0 : getColor();
                
                return widthString()+" "+lineTypeString()+" "+Integer.toHexString(color);
            } else {
                return "none";
            }
        }
        
        /**
         * Returns width as a string, including units.  If the width isn't set, this outputs "1px".
         * @return 
         */
        public String widthString() {
            if (width == null) {
                return "1px";
            }
            return getWidth() + widthUnitString();
        }
        
        /**
         * Returns the border color as a hex string.  If no color is set, this returns the empty string.
         * @return 
         */
        public String colorString() {
            if (color == null) {
                return "";
            }
            return Integer.toHexString(color);
        }
        
        private String widthUnitString() {
            switch (getWidthUnit()) {
                case Style.UNIT_TYPE_DIPS:
                    return "mm";
                default:
                    return "px";
            }
        }
        
        private String lineTypeString() {
            if ("line".equals(getType())) return "solid";
            return getType();
        }
        
        /**
         * Creates the border that is described by this border info.
         * @param theme Theme resource file used to load images that are referenced.
         * @return A Border.
         */
        public Border createBorder(Resources theme) {
            if (getType() == null || "empty".equals(getType()) || "none".equals(getType())) {
                return Border.createEmpty();
            }
            if ("line".equals(getType())) {
                if (this.getWidthUnit() == Style.UNIT_TYPE_DIPS) {
                    return Border.createLineBorder((float)getWidth(), getColor());
                } else {
                    return Border.createLineBorder(getWidth().intValue(), getColor());
                }
            }
            if ("dashed".equals(getType())) {
                if (this.getWidthUnit() == Style.UNIT_TYPE_DIPS) {
                    return Border.createDashedBorder(Display.getInstance().convertToPixels(getWidth()), getColor());
                } else {
                    return Border.createDashedBorder(getWidth().intValue(), getColor());
                }
            }
            if ("dotted".equals(getType())) {
                if (this.getWidthUnit() == Style.UNIT_TYPE_DIPS) {
                    return Border.createDottedBorder(Display.getInstance().convertToPixels(getWidth()), getColor());
                } else {
                    return Border.createDottedBorder(getWidth().intValue(), getColor());
                }
            }
            if ("underline".equals(getType())) {
                if (this.getWidthUnit() == Style.UNIT_TYPE_DIPS) {
                    return Border.createUnderlineBorder(Display.getInstance().convertToPixels(getWidth()), getColor());
                } else {
                    return Border.createUnderlineBorder(getWidth().intValue(), getColor());
                }
            }
            if ("image".equals(getType())) {
                int ilen = getImages().length;
                if (ilen == 9) {
                    return Border.createImageBorder(getImage(theme, getImages()[0]), 
                            getImage(theme, getImages()[1]), 
                            getImage(theme, getImages()[2]), 
                            getImage(theme, getImages()[3]), 
                            getImage(theme, getImages()[4]), 
                            getImage(theme, getImages()[5]), 
                            getImage(theme, getImages()[6]), 
                            getImage(theme, getImages()[7]), 
                            getImage(theme, getImages()[8])
                    );
                }
                if (ilen == 3) {
                    return Border.createImageBorder(getImage(theme, getImages()[0]), 
                            getImage(theme, getImages()[1]), 
                            getImage(theme, getImages()[2])
                    );
                }
                
                
            }
            
            if ("horizontalImage".equals(getType())) {
                return Border.createHorizonalImageBorder(getImage(theme, getImages()[0]), 
                        getImage(theme, getImages()[1]), 
                        getImage(theme, getImages()[2])
                );
            }
            
            if ("verticalImage".equals(getType())) {
                return Border.createVerticalImageBorder(getImage(theme, getImages()[0]), 
                        getImage(theme, getImages()[1]), 
                        getImage(theme, getImages()[2])
                );
            }
            
            if ("splicedImage".equals(getType())) {
                double[] insets = getSpliceInsets(new double[4]);
                return Border.createImageSplicedBorder(getImage(theme, getSpliceImage()), insets[Component.TOP], insets[Component.RIGHT], insets[Component.BOTTOM], insets[Component.LEFT]);
            }
            
            return Border.createEmpty();
             
        }
        
        /**
         * For a splicedImage border, this gets the spliced insets as a 4-element array of double values.
         * @param out An out parameter.  4-element array of insets.  Indices {@link Component#TOP}, {@link Component#BOTTOM}, {@link Component#LEFT}, and {@link Component#RIGHT}.
         * @return 4-element array of insets.  Indices {@link Component#TOP}, {@link Component#BOTTOM}, {@link Component#LEFT}, and {@link Component#RIGHT}.
         */
        public double[] getSpliceInsets(double[] out) {
            String[] parts = Util.split(BorderInfo.this.getSpliceInsets(), " ");
            
            out[Component.TOP] = Double.parseDouble(parts[0]);
            out[Component.RIGHT] = Double.parseDouble(parts[1]);
            out[Component.BOTTOM] = Double.parseDouble(parts[2]);
            out[Component.LEFT] = Double.parseDouble(parts[3]);
            return out;
        }

        /**
         * The border type.  E.g. line, dashed, dotted, underline, image, horizontalImage, verticalImage, splicedImage.
         * @return the type
         */
        public String getType() {
            return type;
        }

        /**
         * Sets the border type.
         * @param type the type to set. E.g. line, dashed, dotted, underline, image, horizontalImage, verticalImage, splicedImage.
         */
        public void setType(String type) {
            this.type = type;
        }

        /**
         * The image to use for a splicedImage border.
         * @return the spliceImage
         */
        public String getSpliceImage() {
            return spliceImage;
        }

        /**
         * Sets the image to use for a splicedImage border.
         * @param spliceImage the spliceImage to set
         */
        public void setSpliceImage(String spliceImage) {
            this.spliceImage = spliceImage;
        }

        /**
         * Gets the images to use for image, horizontalImage, and verticalImage borders.
         * @return the images
         */
        public String[] getImages() {
            return images;
        }

        /**
         * Sets the images used by image, horizontalImage, and verticalImage borders.
         * @param images the images to set
         */
        public void setImages(String[] images) {
            this.images = images;
        }

        /**
         * For splicedImage border, this gets the splice insets as a single string.
         * @return the spliceInsets
         */
        public String getSpliceInsets() {
            return spliceInsets;
        }

        /**
         * Sest the splice insets for a splicedImage border.
         * @param spliceInsets the spliceInsets to set
         */
        public void setSpliceInsets(String spliceInsets) {
            this.spliceInsets = spliceInsets;
        }
        
        /**
         * For a splicedImage border, sets the splice insets as a 4-element array.
         * @param insets 4-element array.  Indices {@literal Component#TOP} , {@literal Component#BOTTOM}, {@literal Component#LEFT}, and {@literal Component#RIGHT}.
         */
        public void setSpliceInsets(double[] insets) {
            this.spliceInsets = insets[Component.TOP] + " "+insets[Component.RIGHT] +" "+ insets[Component.BOTTOM]+" "+insets[Component.LEFT];
        }
        
        /**
         * Sets the splicedImage border insets as a 4-element array and rounds each entry to the specified number of decimal places
         * @param insets 4-element array of insets.Indices {@literal Component#TOP} , {@literal Component#BOTTOM}, {@literal Component#LEFT}, and {@literal Component#RIGHT}.
         * @param decimalPlaces Number of decimal places to round to.
         */
        public void setSpliceInsets(double[] insets, int decimalPlaces) {
            L10NManager l = L10NManager.getInstance();
            this.spliceInsets = 
                    round(insets[Component.TOP], decimalPlaces) + " "+
                    round(insets[Component.RIGHT], decimalPlaces) +" "+ 
                    round(insets[Component.BOTTOM], decimalPlaces) +" "+
                    round(insets[Component.LEFT], decimalPlaces);
        }
        
        private static double round(double d, int decimalPlaces) {
            for (int i=0; i<decimalPlaces; i++) {
                d *= 10;
            }
            d = Math.round(d);
            for (int i=0; i<decimalPlaces; i++) {
                d /= 10;
            }
            
            String dStr = String.valueOf(d);
            int decPos = dStr.indexOf(".");
            if (decPos != -1) {
                int decLen = dStr.length() - decPos;
                if (decLen > decimalPlaces) {
                    dStr = dStr.substring(0, dStr.length() - (decLen - decimalPlaces));
                    d = Double.parseDouble(dStr);
                }
            }
            
            return d;
        }

        /**
         * For a line/dashed/dotted/underline border, the thickness value.
         * @return the width
         * @see #getWidthUnit() 
         */
        public Float getWidth() {
            return width;
        }
        
        /**
         * For line/dashed/dotted/underline border.  The thickness in pixels.
         * @return The thickness in pixels of the border line.
         * @asee #getWidth()
         */
        public Integer getWidthInPixels() {
            if (width == null) {
                return null;
            }
            if (widthUnit == Style.UNIT_TYPE_DIPS) {
                return Display.getInstance().convertToPixels(width);
            } else {
                return width.intValue();
            }
        }

        /**
         * For a line/dashed/dotted/underline border, gets the thickness value.
         * @param width the width to set
         * @see #setWidthUnit(byte) 
         */
        public void setWidth(Float width) {
            this.width = width;
        }

        /**
         * For a line/dashed/dotted/underline border, gets the unit of the thickness value.
         * @return the widthUnit
         * @see #getWidth() 
         */
        public byte getWidthUnit() {
            return widthUnit;
        }

        /**
         * For a line/dashed/dotted/underline border, sets the unit of the thickness value.
         * 
         * @param widthUnit the widthUnit to set
         * @see #setWidth(java.lang.Float) 
         */
        public void setWidthUnit(byte widthUnit) {
            this.widthUnit = widthUnit;
        }

        /**
         * For a line/dashed/dotted/underline border, sets the color.
         * @return the color
         * 
         */
        public Integer getColor() {
            return color;
        }

        /**
         * For a line/dashed/dotted/underline border, gets the color.
         * @param color the color to set
         */
        public void setColor(Integer color) {
            this.color = color;
        }
        
    }
    
    /**
     * Encapsulates the value of the {@literal font} property in a style string.
     */
    public static class FontInfo {
        private Float size;
        private byte sizeUnit;
        private String name;
        private String file;

        /**
         * Returns the font in a format that can be used as the value of the {@literal font} property of a style string.
         * @return 
         */
        @Override
        public String toString() {
            return (sizeString("")+nameString(" ")+fileString(" ")).trim();
        }
        
        /**
         * Gets the font size as a style string.  E.g. 18mm, or 12px.  If unit is inherit, this will just return an empty string.
         * @param prefix String to prefix to the size.
         * @return 
         */
        public String sizeString(String prefix) {
            if (getSize() == null) return prefix;
            if (getSizeUnit() == Style.UNIT_TYPE_DIPS) {
                return prefix + getSize() + unitString();
            } else if (getSizeUnit() == StyleParser.UNIT_INHERIT) {
                return prefix;
            } else {
                return prefix + getSize().intValue() + unitString();
            }
        }
        
        /**
         * Gets the size of the font in pixels.
         * @param baseStyle The base style to use in case the font size isn't specified in the style string.
         * @return 
         */
        public float getSizeInPixels(Style baseStyle) {
            if (getSize() == null) {
                Font f = baseStyle.getFont();
                if (f == null) f = Font.getDefaultFont();
                
                float pixS = f.getPixelSize();
                if (pixS < 1) {
                    pixS = f.getHeight();
                }
                return pixS;
                
            } else {
                switch (getSizeUnit()) {
                    case Style.UNIT_TYPE_DIPS:
                        return Display.getInstance().convertToPixels(getSize());
                    default:
                        return getSize();
                }
            }
        }
        
        private String unitString() {
            switch (getSizeUnit()) {
                case Style.UNIT_TYPE_DIPS: return "mm";
                case Style.UNIT_TYPE_PIXELS: return "px";
                    
            }
            return "px";
        }
        
        private String nameString(String prefix) {
            if (getName() == null) return prefix;
            return prefix + getName();
        }
        
        private String fileString(String prefix) {
            if (getFile() == null) return prefix;
            return prefix + getFile();
        }

        /**
         * Gets the font size.  
         * @return the size
         * @see #getSizeUnit() 
         */
        public Float getSize() {
            return size;
        }

        /**
         * Sets the font size.
         * @param size the size to set
         * @see #setSizeUnit(byte) 
         */
        public void setSize(Float size) {
            this.size = size;
        }

        /**
         * Gets the font size unit.  One of {@link Style#UNIT_TYPE_DIPS}, {@link Style#UNIT_TYPE_PIXELS}, or {@link #UNIT_INHERIT}.
         * @return the sizeUnit
         */
        public byte getSizeUnit() {
            return sizeUnit;
        }

        /**
         * Sets the font size unit.  One of {@link Style#UNIT_TYPE_DIPS}, {@link Style#UNIT_TYPE_PIXELS}, or {@link #UNIT_INHERIT}.
         * @param sizeUnit the sizeUnit to set
         */
        public void setSizeUnit(byte sizeUnit) {
            this.sizeUnit = sizeUnit;
        }

        /**
         * Gets the name of the font.
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the name of the font.
         * @param name the name to set
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Gets the font file name.  Should start with "/".
         * @return the file
         */
        public String getFile() {
            return file;
        }

        /**
         * Sets the font file name.  Should start with "/".
         * @param file the file to set
         */
        public void setFile(String file) {
            this.file = file;
        }
        
        /**
         * Creates a font based on this font information.
         * @param baseStyle The base style to use in cases where aspects of the font aren't explicitly specified in the style string.
         * @return A font.
         */
        public Font createFont(Style baseStyle) {
            Font f = name == null ? baseStyle.getFont() : Font.createTrueTypeFont(name, file);
            if (f == null) {
                f = Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR, Font.NATIVE_MAIN_REGULAR);
                
            }
            return f.derive(getSizeInPixels(baseStyle), f.getStyle());
        }
    }
    
    private static FontInfo parseFontSize(FontInfo out, String arg) {
        arg = arg.trim();
        ScalarValue sizeVal = parseSingleTRBLValue(arg);
        out.setSize((Float) (float)sizeVal.getValue());
        out.setSizeUnit(sizeVal.getUnit());
        return out;
    }
    
    private static FontInfo parseFontName(FontInfo out, String arg) {
        arg = arg.trim();
        if (arg.length() > 0 && arg.charAt(0) == '/') {
            arg = arg.substring(1);
        }
        if (arg.indexOf('/') != -1) {
            arg = arg.substring(0, arg.indexOf('/')).trim();
        } else {
            int len = arg.length();
            if (len > 3 && arg.charAt(len-4) == '.') {
                arg = arg.substring(0, len-4);
            }
        }
        out.setName(arg);
        
        return out;
    }
    
    private static FontInfo parseFontFile(FontInfo out, String arg) {
        arg = arg.trim();
        if (arg.indexOf('/') != -1) {
            arg = arg.substring(arg.indexOf('/'));
        }
        if (arg.length() > 0 && arg.charAt(0) == '/') {
            arg = arg.substring(1);
        } else {
            arg = arg.indexOf("native:") == 0 ? arg : 
                    arg.indexOf(".ttf") != arg.length()-4 ? arg + ".ttf" : 
                    arg;
        }
        out.setFile(arg);
        return out;
    }
    
    private static boolean isFontSizeArg(String arg) {
        return arg != null && arg.length() > 0 && Character.isDigit(arg.charAt(0));
    }
    
    static FontInfo parseFont(FontInfo out, String font) {
        if (font == null || font.trim().length() == 0) {
            return null;
        }
        font = font.trim();
        String[] args = Util.split(font, " ");
        int len = args.length;
        if (len == 1) {
            String arg = args[0].trim();
            if (arg.length() == 0) {
                out.setSize(null);
                out.setSizeUnit(UNIT_INHERIT);
                out.setFile(null);
                out.setName(null);
                return out;
            }
            if (isFontSizeArg(arg)) {
                // This is a size
                parseFontSize(out, arg);
                out.setName(null);
                out.setFile(null);
                return out;
                
            } else {
                out.setSizeUnit(UNIT_INHERIT);
                out.setSize(null);
                parseFontName(out, arg);
                parseFontFile(out, arg);
                
            }
        } else if (len == 2) {
            if (isFontSizeArg(args[0])) {
                parseFontSize(out, args[0]);
                parseFontName(out, args[1]);
                parseFontFile(out, args[1]);
                return out;
            } else {
                out.setSize(null);
                out.setSizeUnit(UNIT_INHERIT);
                parseFontName(out, args[0]);
                parseFontFile(out, args[1]);
                return out;
                
            }

        } else if (len == 3) {
            parseFontSize(out, args[0]);
            parseFontName(out, args[1]);
            parseFontFile(out, args[2]);
            return out;
        } else {
            throw new IllegalArgumentException("Failed to parse font");
        }
        return out;
    }
    
    static Font parseFont(Style baseStyle, String font) {
        if (font == null || font.trim().length() == 0) {
            return null;
        }
        FontInfo finfo = parseFont(new FontInfo(), font);
        return finfo.createFont(baseStyle);
        
    }
    
    
    
    private static ScalarValue[] parseTRBLValue(String val) {
        ScalarValue[] out = new ScalarValue[4];
        String[] parts = Util.split(val, " ");
        int len = parts.length;
        switch (len) {
            case 1:
                ScalarValue v = parseSingleTRBLValue(parts[0]);
                for (int i=0; i<4; i++) {
                    out[i] = v;
                }
                return out;
            
            case 2: {
                ScalarValue v1 = parseSingleTRBLValue(parts[0]);
                ScalarValue v2 = parseSingleTRBLValue(parts[1]);
                out[Component.TOP] = out[Component.BOTTOM] = v1;
                out[Component.LEFT] = out[Component.RIGHT] = v2;
                return out;
            }
            case 3: {
                ScalarValue v1 = parseSingleTRBLValue(parts[0]);
                ScalarValue v2 = parseSingleTRBLValue(parts[1]);
                ScalarValue v3 = parseSingleTRBLValue(parts[2]);
                out[Component.TOP] = v1;
                out[Component.LEFT] = out[Component.RIGHT] = v2;
                out[Component.BOTTOM] = v3;
                return out;
            }
            case 4: {
                ScalarValue v1 = parseSingleTRBLValue(parts[0]);
                ScalarValue v2 = parseSingleTRBLValue(parts[1]);
                ScalarValue v3 = parseSingleTRBLValue(parts[2]);
                ScalarValue v4 = parseSingleTRBLValue(parts[3]);
                out[Component.TOP] = v1;
                out[Component.RIGHT] = v2;
                out[Component.BOTTOM] = v3;
                out[Component.LEFT] = v4;
                return out;
            }


        }
        return null;
    }

    private static ScalarValue parseSingleTRBLValue(String val) {
        val = val.trim();
        int plen = val.length();
        StringBuilder sb = new StringBuilder();
        ScalarValue out = new ScalarValue();
        boolean parsedValue = false;
        for (int i=0; i<plen; i++) {
            char c = val.charAt(i);
            if (!parsedValue && (c == '%' || c == 'm' || c == 'p' || c == 'i')) {
                out.setValue((sb.length() > 0) ? Double.parseDouble(sb.toString()) : 0);
                parsedValue = true;
                sb.setLength(0);
                sb.append(c);
            } else {
                sb.append(c);
            }
        }

        if (parsedValue) {
            String unitStr = sb.toString();
            out.setUnit("mm".equals(unitStr) ? Style.UNIT_TYPE_DIPS : 
                    "%".equals(unitStr) ? Style.UNIT_TYPE_SCREEN_PERCENTAGE :
                            "inherit".equals(unitStr) ? UNIT_INHERIT :
                                    Style.UNIT_TYPE_PIXELS);
        } else {
            out.setUnit(Style.UNIT_TYPE_PIXELS);
            out.setValue(Double.parseDouble(sb.toString()));
        }
        return out;

    }
    
    static Image parseBgImage(Resources theme, String val) {
        if (val == null || val.trim().length() == 0) {
            return null;
        }
        return getImage(theme, val);
    }
    
    private static Map<String,Integer> bgTypes;
    
    private static Map<String,Integer> bgTypes() {
        if (bgTypes == null) {
            bgTypes = new HashMap<String,Integer>();
            Object[] types = new Object[] {
                "image_aligned_bottom", (int)Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM,
                "image_aligned_top", (int)Style.BACKGROUND_IMAGE_ALIGNED_TOP,
                "image_aligned_bottom_right", (int)Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM_RIGHT,
                "image_aligned_bottom_left", (int)Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM_LEFT,
                "image_aligned_top_right", (int)Style.BACKGROUND_IMAGE_ALIGNED_TOP_RIGHT,
                "image_aligned_top_left", (int)Style.BACKGROUND_IMAGE_ALIGNED_TOP_LEFT,
                "image_aligned_left", (int)Style.BACKGROUND_IMAGE_ALIGNED_LEFT,
                "image_aligned_right", (int)Style.BACKGROUND_IMAGE_ALIGNED_RIGHT,
                "image_aligned_center", (int)Style.BACKGROUND_IMAGE_ALIGNED_CENTER,
                "image_scaled", (int)Style.BACKGROUND_IMAGE_SCALED,
                "image_scaled_fill", (int)Style.BACKGROUND_IMAGE_SCALED_FILL,
                "image_scaled_fit", (int)Style.BACKGROUND_IMAGE_SCALED_FIT,
                "image_tile_both", (int)Style.BACKGROUND_IMAGE_TILE_BOTH,
                "image_tile_horizontal", (int)Style.BACKGROUND_IMAGE_TILE_HORIZONTAL,
                "image_tile_vertical", (int)Style.BACKGROUND_IMAGE_TILE_VERTICAL,
                "image_tile_horizontal_align_bottom", (int)Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_BOTTOM,
                "image_tile_horizontal_align_top", (int)Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_TOP,
                "image_tile_horizontal_align_center", (int)Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_CENTER,
                "image_tile_vertical_align_left", (int)Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_LEFT,
                "image_tile_vertical_align_right", (int)Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_RIGHT,
                "image_tile_vertical_align_center", (int)Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_CENTER,
                "gradient_radial", (int)Style.BACKGROUND_GRADIENT_RADIAL,
                "gradient_linear_horizontal", (int)Style.BACKGROUND_GRADIENT_LINEAR_HORIZONTAL,
                "gradient_linear_vertical", (int)Style.BACKGROUND_GRADIENT_LINEAR_VERTICAL,
                "none", (int)Style.BACKGROUND_NONE
            };
            int len = types.length;
            for (int i=0; i<len; i+=2) {
                bgTypes.put((String)types[i], (Integer)types[i+1]);
            }
            
        }
        return bgTypes;
    }
    
    /**
     * Gets the available background type strings (which can be passed to {@link StyleInfo#setBgType(java.lang.String) }
     * @return 
     */
    public static List<String> getBackgroundTypes() {
        ArrayList<String> out = new ArrayList<String>();
        out.addAll(bgTypes().keySet());
        Collections.sort(out, new CaseInsensitiveOrder());
        return out;
    }
    
    private static <T,V> Map<T,V> flip(Map<V,T> map) {
        Map<T,V> out = new HashMap<T,V>();
        for (Map.Entry<V,T> e : map.entrySet()) {
            out.put(e.getValue(), e.getKey());
            
        }
        return out;
    }
    
    static Integer parseBgType(String val) {
        
        if (val == null || val.length() == 0) {
            return null;
        }
        if (Character.isDigit(val.charAt(0))) {
            return Integer.parseInt(val);
        }
        Map<String,Integer> bgTypes = bgTypes();
        if (bgTypes.containsKey(val)) {
            return bgTypes.get(val);
        }
        
        return null;
    }
    
    
    /**
     * Checks if a string is a valid scalar value.  A scalar value should be in the 
     * format {@literal <magnitude><unit>} where {@literal <magnitude>} is an integer
     * or decimal number, and {@literal <unit>} is a unit - one of {@literal mm}, {@literal px}, or {@literal %}.
     * 
     * <p>There is one special value: "inherit" which indicates that the scalar value just inherits from its
     * parent.</p>
     * @param val String value to validate.
     * @return True if the value is a valid scalar value.
     */
    public static boolean validateScalarValue(String val) {
        try {
            parseSingleTRBLValue(val);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
    
    /**
     * Parses a string into a scalar value.  A scalar value should be in the 
     * format {@literal <magnitude><unit>} where {@literal <magnitude>} is an integer
     * or decimal number, and {@literal <unit>} is a unit - one of {@literal mm}, {@literal px}, or {@literal %}.
     * 
     * <p>There is one special value: "inherit" which indicates that the scalar value just inherits from its
     * parent.</p>
     * @param val String that should be a valid scalar value.
     * @return 
     */
    public static ScalarValue parseScalarValue(String val) {
        return parseSingleTRBLValue(val);
    }
    
    
    /**
     * Gets a list of the background types that are supported.
     * @return 
     */
    public static List<String> getSupportedBackgroundTypes() {
        ArrayList<String> out = new ArrayList<String>();
        out.addAll(bgTypes.keySet());
        return out;
    }
    
    
}
