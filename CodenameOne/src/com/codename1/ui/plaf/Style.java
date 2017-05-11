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
package com.codename1.ui.plaf;

import com.codename1.ui.*;
import com.codename1.ui.events.StyleListener;
import com.codename1.ui.util.EventDispatcher;

/**
 * Represents the look of a given component: colors, fonts, transparency, margin and padding &amp; images.
 * <p>Each Component contains a Style Object and allows Style modification in Runtime
 * by Using {@code cmp.getStyle()}
 * The style is also used in Themeing, when a Theme is Changed the Styles Objects are been 
 * updated automatically.
 * <p>When changing a theme the elements changed manually in a style will not be updated
 * by the theme change by default. There are two ways to change that behavior:
 * <ol><li>Use the set method that accepts a second boolean argument and set it to true.
 * <li>Create a new style object and pass all the options in the constructor (without invoking setters manually).
 * </ol>
 * <p>
 * The Margin and Padding is inspired by <a href="http://www.w3.org/TR/REC-CSS2/box.html">W3 Box Model</a>
 *  
 *<pre>
 *
 *       **************************
 *       *         Margin         *
 *       *  ********************  *
 *       *  *      Padding     *  *
 *       *  *    ***********   *  *
 *       *  *    * Content *   *  *
 *       *  *    ***********   *  *
 *       *  *      Padding     *  *
 *       *  ********************  *
 *       *         Margin         *
 *       **************************
 *</pre> 
 * @author Chen Fishbein
 */
public class Style {
    private Style[] proxyTo;
    
    /**
     * Background color attribute name for the theme hashtable 
     */
    public static final String BG_COLOR = "bgColor";

    /**
     * Foreground color attribute name for the theme hashtable 
     */
    public static final String FG_COLOR = "fgColor";

    /**
     * Background image attribute name for the theme hashtable 
     */
    public static final String BG_IMAGE = "bgImage";
    
    /**
     * Background attribute name for the theme hashtable
     */
    public static final String BACKGROUND_TYPE = "bgType";


    /**
     * Background attribute name for the theme hashtable
     */
    public static final String BACKGROUND_ALIGNMENT = "bgAlign";


    /**
     * Background attribute name for the theme hashtable
     */
    public static final String BACKGROUND_GRADIENT = "bgGradient";

    /**
     * Font attribute name for the theme hashtable 
     */
    public static final String FONT = "font";

    /**
     * Transparency attribute name for the theme hashtable 
     */
    public static final String TRANSPARENCY = "transparency";

    /**
     * Opacity attribute name for the theme hashtable 
     */
    public static final String OPACITY = "opacity";

    /**
     * Margin attribute name for the theme hashtable 
     */
    public static final String MARGIN = "margin";

    /**
     * Border attribute name for the theme hashtable 
     */
    public static final String BORDER = "border";

    /**
     * Padding attribute name for the theme hashtable 
     */
    public static final String PADDING = "padding";

    /**
     * Painter attribute name for the style event
     */
    public static final String PAINTER = "painter";

    /**
     * Alignment attribute for the style event
     */
    public static final String ALIGNMENT = "align";

    /**
     * Text decoration attribute for the style event
     */
    public static final String TEXT_DECORATION = "textDecoration";

    /**
     * The units of the padding
     */
    public static final String PADDING_UNIT = "padUnit";

    /**
     * The units of the margin
     */
    public static final String MARGIN_UNIT = "marUnit";

    /**
     * Indicates the background for the style would use a scaled image
     */
    public static final byte BACKGROUND_NONE = (byte)0;

    /**
     * Indicates the background for the style would use a scaled image
     */
    public static final byte BACKGROUND_IMAGE_SCALED = (byte)1;

    /**
     * Indicates the background for the style would use a tiled image on both axis
     */
    public static final byte BACKGROUND_IMAGE_TILE_BOTH = (byte)2;

    /**
     * Indicates the background for the style would use a vertical tiled image
     */
    public static final byte BACKGROUND_IMAGE_TILE_VERTICAL = (byte)3;

    /**
     * Indicates the background for the style would use a horizontal tiled image
     */
    public static final byte BACKGROUND_IMAGE_TILE_HORIZONTAL = (byte)4;

    /**
     * Indicates the background for the style would use an unscaled image with an alignment
     */
    private static final byte BACKGROUND_IMAGE_ALIGNED = (byte)5;

    /**
     * Indicates the background for the style would use an unscaled image with an alignment
     */
    public static final byte BACKGROUND_IMAGE_ALIGNED_TOP = (byte)20;

    /**
     * Indicates the background for the style would use an unscaled image with an alignment
     */
    public static final byte BACKGROUND_IMAGE_ALIGNED_BOTTOM = (byte)21;

    /**
     * Indicates the background for the style would use an unscaled image with an alignment
     */
    public static final byte BACKGROUND_IMAGE_ALIGNED_LEFT = (byte)22;

    /**
     * Indicates the background for the style would use an unscaled image with an alignment
     */
    public static final byte BACKGROUND_IMAGE_ALIGNED_RIGHT = (byte)23;

    /**
     * Indicates the background for the style would use an unscaled image with an alignment
     */
    public static final byte BACKGROUND_IMAGE_ALIGNED_CENTER = (byte)24;


    /**
     * Indicates the background for the style would use an unscaled image with an alignment
     */
    public static final byte BACKGROUND_IMAGE_ALIGNED_TOP_LEFT = (byte)25;

    /**
     * Indicates the background for the style would use an unscaled image with an alignment
     */
    public static final byte BACKGROUND_IMAGE_ALIGNED_TOP_RIGHT = (byte)26;

    /**
     * Indicates the background for the style would use an unscaled image with an alignment
     */
    public static final byte BACKGROUND_IMAGE_ALIGNED_BOTTOM_LEFT = (byte)27;

    /**
     * Indicates the background for the style would use an unscaled image with an alignment
     */
    public static final byte BACKGROUND_IMAGE_ALIGNED_BOTTOM_RIGHT = (byte)28;

    /**
     * Indicates the background for the style would use a horizontal tiled image
     */
    public static final byte BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_TOP = (byte)4;

    /**
     * Indicates the background for the style would use a horizontal tiled image
     */
    public static final byte BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_CENTER = (byte)29;

    /**
     * Indicates the background for the style would use a horizontal tiled image
     */
    public static final byte BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_BOTTOM = (byte)30;

    /**
     * Indicates the background for the style would use a horizontal tiled image
     */
    public static final byte BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_LEFT = BACKGROUND_IMAGE_TILE_VERTICAL;

    /**
     * Indicates the background for the style would use a horizontal tiled image
     */
    public static final byte BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_CENTER = (byte)31;

    /**
     * Indicates the background for the style would use a horizontal tiled image
     */
    public static final byte BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_RIGHT = (byte)32;

    /**
     * Indicates the background for the style would use a scaled image that fills all available space while 
     * maintaining aspect ratio
     */
    public static final byte BACKGROUND_IMAGE_SCALED_FILL = (byte)33;
    
    /**
     * Indicates the background for the style would use a scaled image that fits to available space while 
     * maintaining aspect ratio
     */
    public static final byte BACKGROUND_IMAGE_SCALED_FIT = (byte)34;

    /**
     * Indicates the background for the style would use a linear gradient
     */
    public static final byte BACKGROUND_GRADIENT_LINEAR_VERTICAL = (byte)6;

    /**
     * Indicates the background for the style would use a linear gradient
     */
    public static final byte BACKGROUND_GRADIENT_LINEAR_HORIZONTAL = (byte)7;

    /**
     * Indicates the background for the style would use a radial gradient
     */
    public static final byte BACKGROUND_GRADIENT_RADIAL = (byte)8;

    /**
     * Indicates the background alignment for use in tiling or aligned images
     */
    private static final byte BACKGROUND_IMAGE_ALIGN_TOP = (byte)0xa1;
    /**
     * Indicates the background alignment for use in tiling or aligned images
     */
    private static final byte BACKGROUND_IMAGE_ALIGN_BOTTOM = (byte)0xa2;
    /**
     * Indicates the background alignment for use in tiling or aligned images
     */
    private static final byte BACKGROUND_IMAGE_ALIGN_LEFT = (byte)0xa3;
    /**
     * Indicates the background alignment for use in tiling or aligned images
     */
    private static final byte BACKGROUND_IMAGE_ALIGN_RIGHT = (byte)0xa4;
    /**
     * Indicates the background alignment for use in tiling or aligned images
     */
    private static final byte BACKGROUND_IMAGE_ALIGN_CENTER = (byte)0xa5;

    /**
     * Indicates no text decoration
     */
    public static final byte TEXT_DECORATION_NONE = (byte)0;

    /**
     * Indicates underline 
     */
    public static final byte TEXT_DECORATION_UNDERLINE = (byte)1;

    /**
     * Indicates a strike-through line (usually used to denote deleted text)
     */
    public static final byte TEXT_DECORATION_STRIKETHRU = (byte)2;

    /**
     * Indicates overline
     */
    public static final byte TEXT_DECORATION_OVERLINE = (byte)4;

    /**
     * 3D text effect using a font shadow
     */
    public static final byte TEXT_DECORATION_3D = (byte)8;

    /**
     * 3D sunken text effect using a light font shadow
     */
    public static final byte TEXT_DECORATION_3D_LOWERED = (byte)16;

    /**
     * 3D text effect using a font shadow
     */
    public static final byte TEXT_DECORATION_3D_SHADOW_NORTH = (byte)32;

    /**
     * Indicates the unit type for padding/margin, the default is in device specific pixels
     */
    public static final byte UNIT_TYPE_PIXELS = 0;

    /**
     * Indicates the unit type for padding/margin in percentage of the size of the screen
     */
    public static final byte UNIT_TYPE_SCREEN_PERCENTAGE = 1;

    /**
     * Indicates the unit type for padding/margin in device independent pixels. Device independent pixels try to aim
     * at roughly 1 millimeter of the screen per DIP but make no guarantee for accuracy.
     */
    public static final byte UNIT_TYPE_DIPS = 2;


    private int fgColor = 0x000000;
    private int bgColor = 0xFFFFFF;
    private Font font = Font.getDefaultFont();
    private Image bgImage;
    int[] padding = new int[4];
    int[] margin = new int[4];

    /**
     * Indicates the units used for padding elements, if null pixels are used if not this is a 4 element array containing values
     * of UNIT_TYPE_PIXELS, UNIT_TYPE_DIPS or UNIT_TYPE_SCREEN_PERCENTAGE
     */
    byte[] paddingUnit;

    /**
     * Indicates the units used for margin elements, if null pixels are used if not this is a 4 element array containing values
     * of UNIT_TYPE_PIXELS, UNIT_TYPE_DIPS or UNIT_TYPE_SCREEN_PERCENTAGE
     */
    byte[] marginUnit;
    private byte transparency = (byte) 0xFF; //no transparency
    private byte opacity = (byte) 0xFF; //full opacity
    private Painter bgPainter;

    private byte backgroundType = BACKGROUND_IMAGE_SCALED;
    private byte backgroundAlignment = BACKGROUND_IMAGE_ALIGN_TOP;
    private Object[] backgroundGradient;

    private Border border = null;

    private int align = Component.LEFT;
    private int textDecoration; // Used for underline, strikethru etc. (See TEXT_DECORATION_* constants)

    /**
     * The modified flag indicates which portions of the style have changed using
     * bitmask values
     */
    private int modifiedFlag;
    /**
     * Used for modified flag
     */
    private static final int FG_COLOR_MODIFIED = 1;
    /**
     * Used for modified flag
     */
    private static final int BG_COLOR_MODIFIED = 2;

    /**
     * Used for modified flag
     */
    private static final int FONT_MODIFIED = 16;
    /**
     * Used for modified flag
     */
    private static final int BG_IMAGE_MODIFIED = 32;

    /**
     * Used for modified flag
     */
    private static final int TRANSPARENCY_MODIFIED = 128;
    /**
     * Used for modified flag
     */
    private static final int PADDING_MODIFIED = 256;
    /**
     * Used for modified flag
     */
    private static final int MARGIN_MODIFIED = 512;

    /**
     * Used for modified flag
     */
    private static final int BORDER_MODIFIED = 1024;

    private static final int BACKGROUND_TYPE_MODIFIED = 2048;

    private static final int BACKGROUND_ALIGNMENT_MODIFIED = 4096;

    private static final int BACKGROUND_GRADIENT_MODIFIED = 8192;

    private static final int ALIGNMENT_MODIFIED = 16384;
    private static final int OPACITY_MODIFIED = 32768;
    private static final int TEXT_DECORATION_MODIFIED = 64;


    private EventDispatcher listeners;

    Object roundRectCache;
    
    // used by the Android port, do not remove!
    Object nativeOSCache;
    boolean renderer;
    
    /**
     * Each component when it draw itself uses this Object 
     * to determine in what colors it should use.
     * When a Component is generated it construct a default Style Object.
     * The Default values for each Component can be changed by using the UIManager class
     */
    public Style() {
        setPadding(3, 3, 3, 3);
        setMargin(2, 2, 2, 2);
        modifiedFlag = 0;
    }

    /**
     * Disables native OS optimizations that might collide with cell renderers which do things like sharing style
     * objects
     */
    public void markAsRendererStyle() {
        renderer = true;
    }
    
    /**
     * Creates a "proxy" style whose setter methods map to the methods in the given styles passed and whose
     * getter methods are meaningless
     * 
     * @param styles the styles to which we will proxy
     * @return a proxy style object
     */
    public static Style createProxyStyle(Style... styles) {
        Style s = new Style();
        s.proxyTo = styles;
        return s;
    }
    
    /**
     * Creates a full copy of the given style. Notice that if the original style was modified 
     * manually (by invoking setters on it) it would not chnage when changing a theme/look and feel,
     * however this newly created style would change in such a case.
     * 
     * @param style the style to copy
     */
    public Style(Style style) {
        this(style.getFgColor(), style.getBgColor(), style.getFont(), style.getBgTransparency(),
                style.getBgImage());
        setPadding(style.padding[Component.TOP],
                style.padding[Component.BOTTOM],
                style.padding[Component.LEFT],
                style.padding[Component.RIGHT]);
        setMargin(style.margin[Component.TOP],
                style.margin[Component.BOTTOM],
                style.margin[Component.LEFT],
                style.margin[Component.RIGHT]);
        setPaddingUnit(style.paddingUnit);
        setMarginUnit(style.marginUnit);
        setBorder(style.getBorder());
        opacity = style.opacity;
        modifiedFlag = 0;
        align = style.align;
        backgroundType = style.backgroundType;
        backgroundAlignment = style.backgroundAlignment;
        textDecoration = style.textDecoration;
        if(style.backgroundGradient != null) {
            backgroundGradient = new Object[style.backgroundGradient.length];
            System.arraycopy(style.backgroundGradient, 0, backgroundGradient, 0, backgroundGradient.length);
        }
    }

    /**
     * Creates a new style with the given attributes
     *
     * @param fgColor foreground color
     * @param bgColor background color
     * @param f font
     * @param transparency transparency value
     */
    public Style(int fgColor, int bgColor, Font f, byte transparency) {
        this(fgColor, bgColor, f, transparency, null, BACKGROUND_IMAGE_SCALED);
    }

    /**
     * Creates a new style with the given attributes
     * 
     * @param fgColor foreground color
     * @param bgColor background color
     * @param f font
     * @param transparency transparency value
     * @param im background image
     */
    private Style(int fgColor, int bgColor, Font f, byte transparency, Image im) {
        this();
        this.fgColor = fgColor;
        this.bgColor = bgColor;
        this.font = f;
        this.transparency = transparency;
        this.bgImage = im;        
    }


    /**
     * Creates a new style with the given attributes
     *
     * @param fgColor foreground color
     * @param bgColor background color
     * @param f font
     * @param transparency transparency value
     * @param im background image
     * @param backgroundType one of:
     * BACKGROUND_IMAGE_SCALED, BACKGROUND_IMAGE_TILE_BOTH,
     * BACKGROUND_IMAGE_TILE_VERTICAL, BACKGROUND_IMAGE_TILE_HORIZONTAL,
     * BACKGROUND_IMAGE_ALIGNED, BACKGROUND_GRADIENT_LINEAR_HORIZONTAL,
     * BACKGROUND_GRADIENT_LINEAR_VERTICAL, BACKGROUND_GRADIENT_RADIAL 
     */
    public Style(int fgColor, int bgColor, Font f, byte transparency, Image im, byte backgroundType) {
        this();
        this.fgColor = fgColor;
        this.bgColor = bgColor;
        this.font = f;
        this.transparency = transparency;
        this.backgroundType = backgroundType;
        this.bgImage = im;
    }

    /**
     * Merges the new style with the current style without changing the elements that
     * were modified.
     * 
     * @param style new values of styles from the current theme
     */
    public void merge(Style style) {
        int tmp = modifiedFlag;

        if ((modifiedFlag & FG_COLOR_MODIFIED) == 0) {
            setFgColor(style.getFgColor());
        }
        if ((modifiedFlag & BG_COLOR_MODIFIED) == 0) {
            setBgColor(style.getBgColor());
        }
        if ((modifiedFlag & BG_IMAGE_MODIFIED) == 0) {
            setBgImage(style.getBgImage());
        }
        if ((modifiedFlag & BACKGROUND_TYPE_MODIFIED) == 0) {
            setBackgroundType(style.getBackgroundType());
        }
        if ((modifiedFlag & BACKGROUND_ALIGNMENT_MODIFIED) == 0) {
            setBackgroundAlignment(style.getBackgroundAlignment());
        }
        if ((modifiedFlag & BACKGROUND_GRADIENT_MODIFIED) == 0) {
            setBackgroundGradientStartColor(style.getBackgroundGradientStartColor());
            setBackgroundGradientEndColor(style.getBackgroundGradientEndColor());
            setBackgroundGradientRelativeX(style.getBackgroundGradientRelativeX());
            setBackgroundGradientRelativeY(style.getBackgroundGradientRelativeY());
            setBackgroundGradientRelativeSize(style.getBackgroundGradientRelativeSize());
        }
        if ((modifiedFlag & FONT_MODIFIED) == 0) {
            setFont(style.getFont());
        }

        if ((modifiedFlag & TRANSPARENCY_MODIFIED) == 0) {
            setBgTransparency(style.getBgTransparency());
        }

        if ((modifiedFlag & OPACITY_MODIFIED) == 0) {
            setOpacity(style.getOpacity());
        }

        if ((modifiedFlag & PADDING_MODIFIED) == 0) {
            setPadding(style.padding[Component.TOP],
                    style.padding[Component.BOTTOM],
                    style.padding[Component.LEFT],
                    style.padding[Component.RIGHT]);
            setPaddingUnit(paddingUnit);
        }

        if ((modifiedFlag & MARGIN_MODIFIED) == 0) {
            setMargin(style.margin[Component.TOP],
                    style.margin[Component.BOTTOM],
                    style.margin[Component.LEFT],
                    style.margin[Component.RIGHT]);
            setMarginUnit(style.marginUnit);
        }
        
        if ((modifiedFlag & BORDER_MODIFIED) == 0) {
            setBorder(style.getBorder());
        }

        if((modifiedFlag & TEXT_DECORATION_MODIFIED) == 0) {
            setTextDecoration(style.getTextDecoration());
        }

        if((modifiedFlag & ALIGNMENT_MODIFIED) == 0) {
            setAlignment(style.getAlignment());
        }

        this.bgPainter = style.bgPainter;
        modifiedFlag = tmp;
    }

    /**
     * Returns true if the style was modified manually after it was created by the
     * look and feel. If the style was modified manually (by one of the set methods)
     * then it should be merged rather than overwritten.
     * 
     * @return true if the style was modified
     */
    public boolean isModified() {
        return modifiedFlag != 0;
    }

    /**
     * Background color for the component
     *
     * @return the background color for the component
     */
    public int getBgColor() {
        return bgColor;
    }

    /**
     * Background image for the component
     *
     * @return the background image for the component
     */
    public Image getBgImage() {
        return bgImage;
    }

    /**
     * The type of the background defaults to BACKGROUND_IMAGE_SCALED
     * 
     * @return one of: 
     * BACKGROUND_IMAGE_SCALED, BACKGROUND_IMAGE_TILE_BOTH, 
     * BACKGROUND_IMAGE_TILE_VERTICAL, BACKGROUND_IMAGE_TILE_HORIZONTAL, 
     * BACKGROUND_IMAGE_ALIGNED, BACKGROUND_GRADIENT_LINEAR_HORIZONTAL, 
     * BACKGROUND_GRADIENT_LINEAR_VERTICAL, BACKGROUND_GRADIENT_RADIAL 
     */
    public byte getBackgroundType() {
        return backgroundType;
    }

    /**
     * Return the alignment for the image or tiled image
     *
     * @return one of:
     * BACKGROUND_IMAGE_ALIGN_TOP, BACKGROUND_IMAGE_ALIGN_BOTTOM,
     * BACKGROUND_IMAGE_ALIGN_LEFT, BACKGROUND_IMAGE_ALIGN_RIGHT,
     * BACKGROUND_IMAGE_ALIGN_CENTER
     * @deprecated the functionality of this method is now covered by background type
     */
    private byte getBackgroundAlignment() {
        return backgroundAlignment;
    }

    /**
     * Start color for the radial/linear gradient
     *
     * @return the start color for the radial/linear gradient
     */
    public int getBackgroundGradientStartColor() {
        if(backgroundGradient != null && backgroundGradient.length > 1) {
            return ((Integer)backgroundGradient[0]).intValue();
        }
        return 0xffffff;
    }

    /**
     * End color for the radial/linear gradient
     *
     * @return the end color for the radial/linear gradient
     */
    public int getBackgroundGradientEndColor() {
        if(backgroundGradient != null && backgroundGradient.length > 1) {
            return ((Integer)backgroundGradient[1]).intValue();
        }
        return 0;
    }

    /**
     * Background radial gradient relative center position X
     *
     * @return value between 0 and 1 with 0.5 representing the center of the component
     */
    public float getBackgroundGradientRelativeX() {
        if(backgroundGradient != null && backgroundGradient.length > 2) {
            return ((Float)backgroundGradient[2]).floatValue();
        }
        return 0.5f;
    }

    /**
     * Background radial gradient relative center position Y
     *
     * @return value between 0 and 1 with 0.5 representing the center of the component
     */
    public float getBackgroundGradientRelativeY() {
        if(backgroundGradient != null && backgroundGradient.length > 3) {
            return ((Float)backgroundGradient[3]).floatValue();
        }
        return 0.5f;
    }

    /**
     * Background radial gradient relative size
     *
     * @return value representing the relative size of the gradient
     */
    public float getBackgroundGradientRelativeSize() {
        if(backgroundGradient != null && backgroundGradient.length > 4) {
            return ((Float)backgroundGradient[4]).floatValue();
        }
        return 1f;
    }

    /**
     * Foreground color for the component
     *
     * @return the foreground color for the component
     */
    public int getFgColor() {
        return fgColor;
    }

    /**
     * Font for the component
     *
     * @return the font for the component
     */
    public Font getFont() {
        return font;
    }

    /**
     * Sets the background color for the component
     * 
     * @param bgColor RRGGBB color that ignors the alpha component
     */
    public void setBgColor(int bgColor) {
        setBgColor(bgColor, false);
    }

    /**
     * Sets the Alignment of the Label to one of: CENTER, LEFT, RIGHT
     *
     * @param align alignment value
     * @see com.codename1.ui.Component#CENTER
     * @see com.codename1.ui.Component#LEFT
     * @see com.codename1.ui.Component#RIGHT
     */
    public void setAlignment(int align){
        setAlignment(align, false);
    }

    /**
     * Sets the Alignment of the Label to one of: CENTER, LEFT, RIGHT
     *
     * @param align alignment value
     * @param override If set to true allows the look and feel/theme to override
     * the value in this attribute when changing a theme/look and feel
     * @see com.codename1.ui.Component#CENTER
     * @see com.codename1.ui.Component#LEFT
     * @see com.codename1.ui.Component#RIGHT
     */
    public void setAlignment(int align, boolean override){
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setAlignment(align, override);
            }
            return;
        }
        if (this.align != align) {
            this.align = align;
            if(!override){
                modifiedFlag |= ALIGNMENT_MODIFIED;
            }
            firePropertyChanged(ALIGNMENT);
        }
    }

    /**
     * Returns the alignment of the Label
     *
     * @return the alignment of the Label one of: CENTER, LEFT, RIGHT
     * @see com.codename1.ui.Component#CENTER
     * @see com.codename1.ui.Component#LEFT
     * @see com.codename1.ui.Component#RIGHT
     */
    public int getAlignment() {
        return align;
    }


    /**
     * Sets the background image for the component
     * 
     * @param bgImage background image
     */
    public void setBgImage(Image bgImage) {
        setBgImage(bgImage, false);
    }

    /**
     * Sets the background type for the component
     *
     * @param backgroundType one of BACKGROUND_IMAGE_SCALED, BACKGROUND_IMAGE_TILE_BOTH,
     * BACKGROUND_IMAGE_TILE_VERTICAL, BACKGROUND_IMAGE_TILE_HORIZONTAL,
     * BACKGROUND_IMAGE_ALIGNED, BACKGROUND_GRADIENT_LINEAR_HORIZONTAL,
     * BACKGROUND_GRADIENT_LINEAR_VERTICAL, BACKGROUND_GRADIENT_RADIAL
     */
    public void setBackgroundType(byte backgroundType) {
        setBackgroundType(backgroundType, false);
    }


    /**
     * Sets the background alignment for the component
     *
     * @param backgroundAlignment one of:
     * BACKGROUND_IMAGE_ALIGN_TOP, BACKGROUND_IMAGE_ALIGN_BOTTOM,
     * BACKGROUND_IMAGE_ALIGN_LEFT, BACKGROUND_IMAGE_ALIGN_RIGHT,
     * BACKGROUND_IMAGE_ALIGN_CENTER
     * @deprecated the functionality of this method is now covered by background type
     */
    private void setBackgroundAlignment(byte backgroundAlignment) {
        setBackgroundAlignment(backgroundAlignment, false);
    }


    /**
     * Sets the background color for the component
     *
     * @param backgroundGradientStartColor start color for the linear/radial gradient
     */
    public void setBackgroundGradientStartColor(int backgroundGradientStartColor) {
        setBackgroundGradientStartColor(backgroundGradientStartColor, false);
    }

    /**
     * Sets the background color for the component
     *
     * @param backgroundGradientEndColor end color for the linear/radial gradient
     */
    public void setBackgroundGradientEndColor(int backgroundGradientEndColor) {
        setBackgroundGradientEndColor(backgroundGradientEndColor, false);
    }


    /**
     * Background radial gradient relative center position X
     *
     * @param backgroundGradientRelativeX x position of the radial gradient center
     */
    public void setBackgroundGradientRelativeX(float backgroundGradientRelativeX) {
        setBackgroundGradientRelativeX(backgroundGradientRelativeX, false);
    }

    /**
     * Background radial gradient relative center position Y
     *
     * @param backgroundGradientRelativeY y position of the radial gradient center
     */
    public void setBackgroundGradientRelativeY(float backgroundGradientRelativeY) {
        setBackgroundGradientRelativeY(backgroundGradientRelativeY, false);
    }

    /**
     * Background radial gradient relative size
     *
     * @param backgroundGradientRelativeSize the size of the radial gradient
     */
    public void setBackgroundGradientRelativeSize(float backgroundGradientRelativeSize) {
        setBackgroundGradientRelativeSize(backgroundGradientRelativeSize, false);
    }

    /**
     * Sets the foreground color for the component
     * 
     * @param fgColor foreground color
     */
    public void setFgColor(int fgColor) {
        setFgColor(fgColor, false);
    }

    /**
     * Sets the font for the component
     * 
     * @param font the font
     */
    public void setFont(Font font) {
        setFont(font, false);
    }


    /**
     * Sets the underline text decoration for this style
     *
     * @param underline true to turn underline on, false to turn it off
     */
    public void setUnderline(boolean underline) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setUnderline(underline);
            }
            return;
        }
        if (underline!=isUnderline()) {
            if (underline) {
                textDecoration|=TEXT_DECORATION_UNDERLINE;
            } else {
                textDecoration-=TEXT_DECORATION_UNDERLINE;
            }
        }
    }

    /**
     * Returns true if the underline text decoration is on, false otherwise
     * 
     * @return true if the underline text decoration is on, false otherwise
     */
    public boolean isUnderline() {
        return ((textDecoration & TEXT_DECORATION_UNDERLINE)!=0);
    }

    /**
     * Sets the 3D text decoration for this style
     *
     * @param t true to turn 3d shadow effect on, false to turn it off
     * @param raised indicates a raised or lowered effect
     */
    public void set3DText(boolean t, boolean raised) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.set3DText(t, raised);
            }
            return;
        }
        if(raised) {
            if (t!=isRaised3DText()) {
                textDecoration = textDecoration & (~TEXT_DECORATION_3D_LOWERED);
                if (t) {
                    textDecoration|=TEXT_DECORATION_3D;
                } else {
                    textDecoration-=TEXT_DECORATION_3D;
                }
            }
        } else {
            if (t!=isLowered3DText()) {
                textDecoration = textDecoration & (~TEXT_DECORATION_3D);
                if (t) {
                    textDecoration|=TEXT_DECORATION_3D_LOWERED;
                } else {
                    textDecoration-=TEXT_DECORATION_3D_LOWERED;
                }
            }
        }
    }

    /**
     * Sets the text decoration to 3D text 
     * @param north true to enable 3d text with the shadow on top false otherwise
     */
    public void set3DTextNorth(boolean north) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.set3DTextNorth(north);
            }
            return;
        }
        textDecoration = TEXT_DECORATION_3D_SHADOW_NORTH;
    }
    
    /**
     * Returns the text decoration state for the north
     * @return true if that is used
     */
    public boolean is3DTextNorth() {
        return (textDecoration & TEXT_DECORATION_3D_SHADOW_NORTH) == TEXT_DECORATION_3D_SHADOW_NORTH;
    }
    
    /**
     * Returns true if the 3D text decoration is on, false otherwise
     *
     * @return true if the 3D text decoration is on, false otherwise
     */
    public boolean isRaised3DText() {
        return ((textDecoration & TEXT_DECORATION_3D)!=0);
    }

    /**
     * Returns true if the 3D text decoration is on, false otherwise
     *
     * @return true if the 3D text decoration is on, false otherwise
     */
    public boolean isLowered3DText() {
        return ((textDecoration & TEXT_DECORATION_3D_LOWERED)!=0);
    }

    /**
     * Sets the overline text decoration for this style
     *
     * @param overline true to turn overline on, false to turn it off
     */
    public void setOverline(boolean overline) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setOverline(overline);
            }
            return;
        }
        if (overline!=isOverline()) {
            if (overline) {
                textDecoration|=TEXT_DECORATION_OVERLINE;
            } else {
                textDecoration-=TEXT_DECORATION_OVERLINE;
            }
        }
    }

    /**
     * Returns true if the overline text decoration is on, false otherwise
     *
     * @return true if the overline text decoration is on, false otherwise
     */
    public boolean isOverline() {
        return ((textDecoration & TEXT_DECORATION_OVERLINE)!=0);
    }


    /**
     * Sets the strike through text decoration for this style
     * 
     * @param strikethru true to turn strike through on, false to turn it off
     */
    public void setStrikeThru(boolean strikethru) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setStrikeThru(strikethru);
            }
            return;
        }
        if (strikethru!=isStrikeThru()) {
            if (strikethru) {
                textDecoration|=TEXT_DECORATION_STRIKETHRU;
            } else {
                textDecoration-=TEXT_DECORATION_STRIKETHRU;
            }
        }
    }

    /**
     * Returns true if the strike through text decoration is on, false otherwise
     *
     * @return true if the strike through  text decoration is on, false otherwise
     */
    public boolean isStrikeThru() {
        return ((textDecoration & TEXT_DECORATION_STRIKETHRU)!=0);
    }


    /**
     * Returns the text decoration of this style
     * 
     * @return the text decoration of this style (bitmask of the TEXT_DECORATION_* constants)
     */
    public int getTextDecoration() {
        return textDecoration;
    }

    /**
     * Sets the text decoration of this style
     * 
     * @param textDecoration the textDecoration to set (bitmask of the TEXT_DECORATION_* constants)
     */
    public void setTextDecoration(int textDecoration) {
        setTextDecoration(textDecoration, false);
    }

    /**
     * Sets the text decoration of this style
     *
     * @param textDecoration the textDecoration to set (bitmask of the TEXT_DECORATION_* constants)
     * @param override If set to true allows the look and feel/theme to override
     * the value in this attribute when changing a theme/look and feel
     */
    public void setTextDecoration(int textDecoration, boolean override) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setTextDecoration(textDecoration, override);
            }
            return;
        }
        this.textDecoration = textDecoration;
        if (this.textDecoration != textDecoration) {
            this.textDecoration = textDecoration;
            if(!override){
                modifiedFlag |= TEXT_DECORATION_MODIFIED;
            }
            firePropertyChanged(TEXT_DECORATION);
        }
    }

    /**
     * Returns the transparency (opacity) level of the Component, zero indicates fully
     * transparent and FF indicates fully opaque. 
     * 
     * @return  the transparency level of the Component
     */
    public byte getBgTransparency() {
        if(bgImage != null && backgroundType <= BACKGROUND_IMAGE_TILE_BOTH && backgroundType != BACKGROUND_NONE && (bgImage.isAnimation() || bgImage.isOpaque())) {
            return (byte)0xff;
        }
        return transparency;
    }

    /**
     * Sets the Component transparency (opacity) level of the Component, zero indicates fully
     * transparent and FF indicates fully opaque. 
     * 
     * @param transparency transparency level as byte
     */
    public void setBgTransparency(byte transparency) {
        setBgTransparency(transparency & 0xFF, false);
    }

    /**
     * Returns the opacity value for the component
     * 
     * @return the opacity value
     */
    public int getOpacity() {
        return opacity & 0xff;
    }
    
    /**
     * Set the opacity value
     * 
     * @param opacity the opacity value
     */
    public void setOpacity(int opacity) {
        setOpacity(opacity, false);
    }
    
    /**
     * Sets the Component transparency level. Valid values should be a 
     * number between 0-255
     * 
     * @param opacity  int value between 0-255
     * @param override If set to true allows the look and feel/theme to override 
     * the value in this attribute when changing a theme/look and feel
     */
    public void setOpacity(int opacity, boolean override) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setOpacity(opacity, override);
            }
            return;
        }
        if (opacity < 0 || opacity > 255) {
            throw new IllegalArgumentException("valid values are between 0-255: " + opacity);
        }
        if (this.opacity != (byte) opacity) {
            this.opacity = (byte) opacity;

            if (!override) {
                modifiedFlag |= OPACITY_MODIFIED;
            }
            firePropertyChanged(OPACITY);
        }
    }
    
    
    /**
     * Sets the Component transparency level. Valid values should be a 
     * number between 0-255
     * @param transparency int value between 0-255
     */
    public void setBgTransparency(int transparency) {
        setBgTransparency(transparency, false);
    }

    /**
     * Sets the Style Padding
     *  
     * @param top number of pixels to padd
     * @param bottom number of pixels to padd
     * @param left number of pixels to padd
     * @param right number of pixels to padd
     */
    public void setPadding(int top, int bottom, int left, int right) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setPadding(top, bottom, left, right);
            }
            return;
        }
        if (top < 0 || left < 0 || right < 0 || bottom < 0) {
            throw new IllegalArgumentException("padding cannot be negative");
        }
        if (padding[Component.TOP] != top ||
                padding[Component.BOTTOM] != bottom ||
                padding[Component.LEFT] != left ||
                padding[Component.RIGHT] != right) {
            padding[Component.TOP] = top;
            padding[Component.BOTTOM] = bottom;
            padding[Component.LEFT] = left;
            padding[Component.RIGHT] = right;

            modifiedFlag |= PADDING_MODIFIED;
            firePropertyChanged(PADDING);
        }
    }

    /**
     * Sets the Style Padding
     * 
     * @param orientation one of: Component.TOP, Component.BOTTOM, Component.LEFT, Component.RIGHT
     * @param gap number of pixels to padd
     */
    public void setPadding(int orientation, int gap) {
        setPadding(orientation, gap, false);
    }

    /**
     * Sets the Style Margin
     *  
     * @param top number of margin using the current unit
     * @param bottom number of margin using the current unit
     * @param left number of margin using the current unit
     * @param right number of margin using the current unit
     */
    public void setMargin(int top, int bottom, int left, int right) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setMargin(top, bottom, left, right);
            }
            return;
        }
        if (top < 0 || left < 0 || right < 0 || bottom < 0) {
            throw new IllegalArgumentException("margin cannot be negative");
        }
        if (margin[Component.TOP] != top ||
                margin[Component.BOTTOM] != bottom ||
                margin[Component.LEFT] != left ||
                margin[Component.RIGHT] != right) {
            margin[Component.TOP] = top;
            margin[Component.BOTTOM] = bottom;
            margin[Component.LEFT] = left;
            margin[Component.RIGHT] = right;

            modifiedFlag |= MARGIN_MODIFIED;
            firePropertyChanged(MARGIN);
        }
    }

    /**
     * Sets the Style Margin
     * 
     * @param orientation one of: Component.TOP, Component.BOTTOM, Component.LEFT, Component.RIGHT
     * @param gap number of margin using the current unit
     */
    public void setMargin(int orientation, int gap) {
        setMargin(orientation, gap, false);
    }
    /**
     * Returns the Padding in the internal value regardless of the unit
     *
     * @param rtl flag indicating whether the padding is for an RTL bidi component
     * @param orientation one of: Component.TOP, Component.BOTTOM, Component.LEFT, Component.RIGHT
     * @return number of padding pixels in the given orientation
     */
    public int getPaddingValue(boolean rtl, int orientation) {
        if (orientation < Component.TOP || orientation > Component.RIGHT) {
            throw new IllegalArgumentException("wrong orientation " + orientation);
        }

        if (rtl) {
        	switch(orientation) {
                case Component.LEFT:
                    orientation = Component.RIGHT;
                    break;
                case Component.RIGHT:
                    orientation = Component.LEFT;
                    break;
            }
        }

        return padding[orientation];
    }
    
    /**
     * Returns the left padding in pixel or right padding in an RTL situation
     * @param rtl indicates a right to left language
     * @return the padding in pixels
     */
    public int getPaddingLeft(boolean rtl) {
        if (rtl) {
            return convertUnit(paddingUnit, padding[Component.RIGHT], Component.RIGHT);
        }
        return convertUnit(paddingUnit, padding[Component.LEFT], Component.LEFT);
    }
    
    
    /**
     * Returns the right padding in pixel or left padding in an RTL situation
     * @param rtl indicates a right to left language
     * @return the padding in pixels
     */
    public int getPaddingRight(boolean rtl) {
        if (rtl) {
            return convertUnit(paddingUnit, padding[Component.LEFT], Component.LEFT);
        }
        return convertUnit(paddingUnit, padding[Component.RIGHT], Component.RIGHT);
    }
    
    /**
     * Returns the top padding in pixel 
     * @return the padding in pixels
     */
    public int getPaddingTop() {
        return convertUnit(paddingUnit, padding[Component.TOP], Component.TOP);
    }

    /**
     * Sets the Style Padding on the top, this is equivalent to calling {@code setPadding(Component.TOP, gap, false);}
     * 
     * @param gap number of pixels to pad
     */
    public void setPaddingTop(int gap) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setPaddingTop(gap);
            }
            return;
        }
        if (gap < 0) {
            throw new IllegalArgumentException("padding cannot be negative");
        }
        if (padding[Component.TOP] != gap) {
            padding[Component.TOP] = gap;
            modifiedFlag |= PADDING_MODIFIED;
            firePropertyChanged(PADDING);
        }
    }
    
    /**
     * Sets the Style Padding on the bottom, this is equivalent to calling {@code setPadding(Component.BOTTOM, gap, false);}
     * 
     * @param gap number of pixels to pad
     */
    public void setPaddingBottom(int gap) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setPaddingBottom(gap);
            }
            return;
        }
        if (gap < 0) {
            throw new IllegalArgumentException("padding cannot be negative");
        }
        if (padding[Component.BOTTOM] != gap) {
            padding[Component.BOTTOM] = gap;
            modifiedFlag |= PADDING_MODIFIED;
            firePropertyChanged(PADDING);
        }
    }
    
    /**
     * Sets the Style Padding on the left, this is equivalent to calling {@code setPadding(Component.LEFT, gap, false);}
     * 
     * @param gap number of pixels to pad
     */
    public void setPaddingLeft(int gap) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setPaddingLeft(gap);
            }
            return;
        }
        if (gap < 0) {
            throw new IllegalArgumentException("padding cannot be negative");
        }
        if (padding[Component.LEFT] != gap) {
            padding[Component.LEFT] = gap;
            modifiedFlag |= PADDING_MODIFIED;
            firePropertyChanged(PADDING);
        }
    }
    
    /**
     * Sets the Style Padding on the right, this is equivalent to calling {@code setPadding(Component.RIGHT, gap, false);}
     * 
     * @param gap number of pixels to pad
     */
    public void setPaddingRight(int gap) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setPaddingRight(gap);
            }
            return;
        }
        if (gap < 0) {
            throw new IllegalArgumentException("padding cannot be negative");
        }
        if (padding[Component.RIGHT] != gap) {
            padding[Component.RIGHT] = gap;
            modifiedFlag |= PADDING_MODIFIED;
            firePropertyChanged(PADDING);
        }
    }


    /**
     * Sets the Style margin on the top, this is equivalent to calling {@code setMargin(Component.TOP, gap, false);}
     * 
     * @param gap number of pixels to pad
     */
    public void setMarginTop(int gap) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setMarginTop(gap);
            }
            return;
        }
        if (gap < 0) {
            throw new IllegalArgumentException("Margin cannot be negative");
        }
        if (margin[Component.TOP] != gap) {
            margin[Component.TOP] = gap;
            modifiedFlag |= MARGIN_MODIFIED;
            firePropertyChanged(MARGIN);
        }
    }
    
    /**
     * Sets the Style Margin on the bottom, this is equivalent to calling {@code setMargin(Component.BOTTOM, gap, false);}
     * 
     * @param gap number of pixels to pad
     */
    public void setMarginBottom(int gap) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setMarginBottom(gap);
            }
            return;
        }
        if (gap < 0) {
            throw new IllegalArgumentException("Margin cannot be negative");
        }
        if (margin[Component.BOTTOM] != gap) {
            margin[Component.BOTTOM] = gap;
            modifiedFlag |= MARGIN_MODIFIED;
            firePropertyChanged(MARGIN);
        }
    }
    
    /**
     * Sets the Style Margin on the left, this is equivalent to calling {@code setMargin(Component.LEFT, gap, false);}
     * 
     * @param gap number of pixels to pad
     */
    public void setMarginLeft(int gap) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setMarginLeft(gap);
            }
            return;
        }
        if (gap < 0) {
            throw new IllegalArgumentException("Margin cannot be negative");
        }
        if (margin[Component.LEFT] != gap) {
            margin[Component.LEFT] = gap;
            modifiedFlag |= MARGIN_MODIFIED;
            firePropertyChanged(MARGIN);
        }
    }
    
    /**
     * Sets the Style Margin on the right, this is equivalent to calling {@code setMargin(Component.RIGHT, gap, false);}
     * 
     * @param gap number of pixels to pad
     */
    public void setMarginRight(int gap) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setMarginRight(gap);
            }
            return;
        }
        if (gap < 0) {
            throw new IllegalArgumentException("Margin cannot be negative");
        }
        if (margin[Component.RIGHT] != gap) {
            margin[Component.RIGHT] = gap;
            modifiedFlag |= MARGIN_MODIFIED;
            firePropertyChanged(MARGIN);
        }
    }

    /**
     * Returns the bottom padding in pixel 
     * @return the padding in pixels
     */
    public int getPaddingBottom() {
        return convertUnit(paddingUnit, padding[Component.BOTTOM], Component.BOTTOM);
    }
    
    /**
     * The equivalent of getMarginLeft + getMarginRight
     * @return the side margin
     */
    public int getHorizontalMargins() {
        return convertUnit(marginUnit, margin[Component.RIGHT], Component.RIGHT) +
                convertUnit(marginUnit, margin[Component.LEFT], Component.LEFT);
    }

    /**
     * The equivalent of getMarginTop + getMarginBottom
     * @return the vertical margin
     */
    public int getVerticalMargins() {
        return convertUnit(marginUnit, margin[Component.TOP], Component.TOP) +
                convertUnit(marginUnit, margin[Component.BOTTOM], Component.BOTTOM);
    }
    
    /**
     * The equivalent of getPaddingLeft + getPaddingRight
     * @return the side padding
     */
    public int getHorizontalPadding() {
        return convertUnit(paddingUnit, padding[Component.RIGHT], Component.RIGHT) +
                convertUnit(paddingUnit, padding[Component.LEFT], Component.LEFT);
    }

    /**
     * The equivalent of getPaddingTop + getPaddingBottom
     * @return the vertical padding
     */
    public int getVerticalPadding() {
        return convertUnit(paddingUnit, padding[Component.TOP], Component.TOP) +
                convertUnit(paddingUnit, padding[Component.BOTTOM], Component.BOTTOM);
    }
    
    /**
     * Returns the right margin in pixels ignoring RTL
     * @return the margin in pixels
     */
    public int getMarginRightNoRTL() {
        return convertUnit(marginUnit, margin[Component.RIGHT], Component.RIGHT);
    }

    /**
     * Returns the left margin in pixels ignoring RTL
     * @return the margin in pixels
     */
    public int getMarginLeftNoRTL() {
        return convertUnit(marginUnit, margin[Component.LEFT], Component.LEFT);
    }
    
    /**
     * Returns the right padding in pixels ignoring RTL
     * @return the padding in pixels
     */
    public int getPaddingRightNoRTL() {
        return convertUnit(paddingUnit, padding[Component.RIGHT], Component.RIGHT);
    }

    /**
     * Returns the left padding in pixels ignoring RTL
     * @return the padding in pixels
     */
    public int getPaddingLeftNoRTL() {
        return convertUnit(paddingUnit, padding[Component.LEFT], Component.LEFT);
    }
    
    /**
     * Returns the right margin in pixel or left margin in an RTL situation
     * @param rtl indicates a right to left language
     * @return the margin in pixels
     */
    public int getMarginRight(boolean rtl) {
        if (rtl) {
            return convertUnit(marginUnit, margin[Component.LEFT], Component.LEFT);
        }
        return convertUnit(marginUnit, margin[Component.RIGHT], Component.RIGHT);
    }

    /**
     * Returns the left margin in pixel or right margin in an RTL situation
     * @param rtl indicates a right to left language
     * @return the margin in pixels
     */
    public int getMarginLeft(boolean rtl) {
        if (rtl) {
            return convertUnit(marginUnit, margin[Component.RIGHT], Component.RIGHT);
        }
        return convertUnit(marginUnit, margin[Component.LEFT], Component.LEFT);
    }
    
    /**
     * Returns the top margin in pixel 
     * @return the margin in pixels
     */
    public int getMarginTop() {
        return convertUnit(marginUnit, margin[Component.TOP], Component.TOP);
    }
    
    /**
     * Returns the bottom margin in pixel 
     * @return the margin in pixels
     */
    public int getMarginBottom() {
        return convertUnit(marginUnit, margin[Component.BOTTOM], Component.BOTTOM);
    }

    /**
     * Returns the Padding in using the current unit
     *
     * @param rtl flag indicating whether the padding is for an RTL bidi component
     * @param orientation one of: Component.TOP, Component.BOTTOM, Component.LEFT, Component.RIGHT
     * @return number of padding pixels in the given orientation
     */
    public int getPadding(boolean rtl, int orientation) {
        int v = getPaddingValue(rtl, orientation);
        return convertUnit(paddingUnit, v, orientation);
    }

    private int convertUnit(byte[] unitType, int v, int orientation) {
        if(unitType != null) {
            switch(unitType[orientation]) {
                case UNIT_TYPE_DIPS:
                    return Display.getInstance().convertToPixels(v, orientation == Component.LEFT || orientation == Component.RIGHT);
                case UNIT_TYPE_SCREEN_PERCENTAGE:
                    if(orientation == Component.TOP || orientation == Component.BOTTOM) {
                        float h = Display.getInstance().getDisplayHeight();
                        h = h / 100.0f * ((float)v);
                        return (int)h;
                    } else {
                        float w = Display.getInstance().getDisplayWidth();
                        w = w / 100.0f * ((float)v);
                        return (int)w;
                    }
                default:
                    return v;
            }
        }
        return v;
    }

    /**
     * Returns the Padding
     *
     * @param orientation one of: Component.TOP, Component.BOTTOM, Component.LEFT, Component.RIGHT
     * @return number of padding pixels in the given orientation
     */
    public int getPadding(int orientation) {
        return getPadding(UIManager.getInstance().getLookAndFeel().isRTL(), orientation);
    }

    /**
     * Returns the Margin
     *
     * @param orientation one of: Component.TOP, Component.BOTTOM, Component.LEFT, Component.RIGHT
     * @return number of margin using the current unit in the given orientation
     */
    public int getMargin(int orientation) {
        return getMargin(UIManager.getInstance().getLookAndFeel().isRTL(), orientation);
    }

    /**
     * Returns the Margin
     *
     * @param rtl flag indicating whether the padding is for an RTL bidi component
     * @param orientation one of: Component.TOP, Component.BOTTOM, Component.LEFT, Component.RIGHT
     * @return number of margin using the current unit in the given orientation
     */
    public int getMargin(boolean rtl, int orientation) {
        int v = getMarginValue(rtl, orientation);
        return convertUnit(marginUnit, v, orientation);
    }

    /**
     * Returns the Margin
     * 
     * @param rtl flag indicating whether the padding is for an RTL bidi component
     * @param orientation one of: Component.TOP, Component.BOTTOM, Component.LEFT, Component.RIGHT
     * @return number of margin using the current unit in the given orientation
     */
    public int getMarginValue(boolean rtl, int orientation) {
        if (orientation < Component.TOP || orientation > Component.RIGHT) {
            throw new IllegalArgumentException("wrong orientation " + orientation);
        }
        if (rtl) {
        	switch(orientation) {
                case Component.LEFT:
                    orientation = Component.RIGHT;
                    break;
                case Component.RIGHT:
                    orientation = Component.LEFT;
                    break;
            }
        }
        return margin[orientation];
    }

    /**
     * Sets the background color for the component
     * 
     * @param bgColor RRGGBB color that ignors the alpha component
     * @param override If set to true allows the look and feel/theme to override 
     * the value in this attribute when changing a theme/look and feel
     */
    public void setBgColor(int bgColor, boolean override) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setBgColor(bgColor, override);
            }
            return;
        }
        if (this.bgColor != bgColor) {
            this.bgColor = bgColor;
            if (!override) {
                modifiedFlag |= BG_COLOR_MODIFIED;
            }
            firePropertyChanged(BG_COLOR);
        }
    }

    /**
     * Sets the background image for the component
     * 
     * @param bgImage background image
     * @param override If set to true allows the look and feel/theme to override 
     * the value in this attribute when changing a theme/look and feel
     */
    public void setBgImage(Image bgImage, boolean override) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setBgImage(bgImage, override);
            }
            return;
        }
        if (this.bgImage != bgImage) {
            this.bgImage = bgImage;
            if(!override){
                modifiedFlag |= BG_IMAGE_MODIFIED;
            }
            firePropertyChanged(BG_IMAGE);
        }
    }

    /**
     * Sets the background type for the component
     *
     * @param backgroundType one of BACKGROUND_IMAGE_SCALED, BACKGROUND_IMAGE_TILE_BOTH,
     * BACKGROUND_IMAGE_TILE_VERTICAL, BACKGROUND_IMAGE_TILE_HORIZONTAL,
     * BACKGROUND_IMAGE_ALIGNED, BACKGROUND_GRADIENT_LINEAR_HORIZONTAL,
     * BACKGROUND_GRADIENT_LINEAR_VERTICAL, BACKGROUND_GRADIENT_RADIAL
     * @param override If set to true allows the look and feel/theme to override
     * the value in this attribute when changing a theme/look and feel
     */
    public void setBackgroundType(byte backgroundType, boolean override) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setBackgroundType(backgroundType, override);
            }
            return;
        }
        if (this.backgroundType != backgroundType) {
            this.backgroundType = backgroundType;
            if(!override){
                modifiedFlag |= BACKGROUND_TYPE_MODIFIED;
            }
            firePropertyChanged(BACKGROUND_TYPE);
        }
    }


    /**
     * Sets the background alignment for the component
     *
     * @param backgroundAlignment one of:
     * BACKGROUND_IMAGE_ALIGN_TOP, BACKGROUND_IMAGE_ALIGN_BOTTOM,
     * BACKGROUND_IMAGE_ALIGN_LEFT, BACKGROUND_IMAGE_ALIGN_RIGHT,
     * BACKGROUND_IMAGE_ALIGN_CENTER
     * @param override If set to true allows the look and feel/theme to override
     * the value in this attribute when changing a theme/look and feel
     * @deprecated the functionality of this method is now covered by background type
     */
    private void setBackgroundAlignment(byte backgroundAlignment, boolean override) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setBackgroundAlignment(backgroundAlignment, override);
            }
            return;
        }
        if (this.backgroundAlignment != backgroundAlignment) {
            this.backgroundAlignment = backgroundAlignment;
            if(!override){
                modifiedFlag |= BACKGROUND_ALIGNMENT_MODIFIED;
            }
            firePropertyChanged(BACKGROUND_ALIGNMENT);
        }
    }

    /**
     * Returns the background gradient array which includes the start/end color  
     * and optionally the x/y relative anchor for the radial gradient
     * 
     * @return the background gradient array which includes the start/end color  
     * and optionally the x/y relative anchor for the radial gradient
     */
    Object[] getBackgroundGradient() {
        if(backgroundGradient == null) {
            Float c = new Float(0.5f);
            backgroundGradient = new Object[] {new Integer(0xffffff), new Integer(0), c, c, new Float(1)};
        }
        return backgroundGradient;
    }

    /**
     * Internal use background gradient setter
     */
    void setBackgroundGradient(Object[] backgroundGradient) {
        this.backgroundGradient = backgroundGradient;
    }

    /**
     * Sets the background color for the component
     *
     * @param backgroundGradientStartColor start color for the linear/radial gradient
     * @param override If set to true allows the look and feel/theme to override
     * the value in this attribute when changing a theme/look and feel
     */
    public void setBackgroundGradientStartColor(int backgroundGradientStartColor, boolean override) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setBackgroundGradientStartColor(backgroundGradientStartColor, override);
            }
            return;
        }
        if (((Integer) getBackgroundGradient()[0]).intValue() != backgroundGradientStartColor) {
            getBackgroundGradient()[0] = new Integer(backgroundGradientStartColor);
            if (!override) {
                modifiedFlag |= BACKGROUND_GRADIENT_MODIFIED;
            }
            firePropertyChanged(BACKGROUND_GRADIENT);
        }
    }

    /**
     * Sets the background color for the component
     *
     * @param backgroundGradientEndColor end color for the linear/radial gradient
     * @param override If set to true allows the look and feel/theme to override
     * the value in this attribute when changing a theme/look and feel
     */
    public void setBackgroundGradientEndColor(int backgroundGradientEndColor, boolean override) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setBackgroundGradientEndColor(backgroundGradientEndColor, override);
            }
            return;
        }
        if (((Integer) getBackgroundGradient()[1]).intValue() != backgroundGradientEndColor) {
            getBackgroundGradient()[1] = new Integer(backgroundGradientEndColor);
            if (!override) {
                modifiedFlag |= BACKGROUND_GRADIENT_MODIFIED;
            }
            firePropertyChanged(BACKGROUND_GRADIENT);
        }
    }


    /**
     * Background radial gradient relative center position X
     *
     * @param backgroundGradientRelativeX x position of the radial gradient center
     * @param override If set to true allows the look and feel/theme to override
     * the value in this attribute when changing a theme/look and feel
     */
    public void setBackgroundGradientRelativeX(float backgroundGradientRelativeX, boolean override) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setBackgroundGradientRelativeX(backgroundGradientRelativeX, override);
            }
            return;
        }
        if (((Float) getBackgroundGradient()[2]).floatValue() != backgroundGradientRelativeX) {
            getBackgroundGradient()[2] = new Float(backgroundGradientRelativeX);
            if (!override) {
                modifiedFlag |= BACKGROUND_GRADIENT_MODIFIED;
            }
            firePropertyChanged(BACKGROUND_GRADIENT);
        }
    }

    /**
     * Background radial gradient relative center position Y
     *
     * @param backgroundGradientRelativeY y position of the radial gradient center
     * @param override If set to true allows the look and feel/theme to override
     * the value in this attribute when changing a theme/look and feel
     */
    public void setBackgroundGradientRelativeY(float backgroundGradientRelativeY, boolean override) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setBackgroundGradientRelativeY(backgroundGradientRelativeY, override);
            }
            return;
        }
        if (((Float) getBackgroundGradient()[3]).floatValue() != backgroundGradientRelativeY) {
            getBackgroundGradient()[3] = new Float(backgroundGradientRelativeY);
            if (!override) {
                modifiedFlag |= BACKGROUND_GRADIENT_MODIFIED;
            }
            firePropertyChanged(BACKGROUND_GRADIENT);
        }
    }

    /**
     * Background radial gradient relative size
     *
     * @param backgroundGradientRelativeSize the size of the radial gradient relative to the screens
     * larger dimension
     * @param override If set to true allows the look and feel/theme to override
     * the value in this attribute when changing a theme/look and feel
     */
    public void setBackgroundGradientRelativeSize(float backgroundGradientRelativeSize, boolean override) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setBackgroundGradientRelativeSize(backgroundGradientRelativeSize, override);
            }
            return;
        }
        if (((Float) getBackgroundGradient()[4]).floatValue() != backgroundGradientRelativeSize) {
            getBackgroundGradient()[4] = new Float(backgroundGradientRelativeSize);
            if (!override) {
                modifiedFlag |= BACKGROUND_GRADIENT_MODIFIED;
            }
            firePropertyChanged(BACKGROUND_GRADIENT);
        }
    }

    /**
     * Sets the foreground color for the component
     * 
     * @param fgColor foreground color
     * @param override If set to true allows the look and feel/theme to override 
     * the value in this attribute when changing a theme/look and feel
     */
    public void setFgColor(int fgColor, boolean override) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setFgColor(fgColor, override);
            }
            return;
        }
        if (this.fgColor != fgColor) {
            this.fgColor = fgColor;
            if (!override) {
                modifiedFlag |= FG_COLOR_MODIFIED;
            }
            firePropertyChanged(FG_COLOR);
        }
    }

    /**
     * Sets the font for the component
     * 
     * @param font the font
     * @param override If set to true allows the look and feel/theme to override 
     * the value in this attribute when changing a theme/look and feel
     */
    public void setFont(Font font, boolean override) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setFont(font, override);
            }
            return;
        }
        if (this.font == null && font != null ||
                (this.font != null && !this.font.equals(font))) {
            this.font = font;
            if (!override) {
                modifiedFlag |= FONT_MODIFIED;
            }
            firePropertyChanged(FONT);
        }
    }


    /**
     * Sets the Component transparency level. Valid values should be a 
     * number between 0-255
     * 
     * @param transparency int value between 0-255
     * @param override If set to true allows the look and feel/theme to override 
     * the value in this attribute when changing a theme/look and feel
     */
    public void setBgTransparency(int transparency, boolean override) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setBgTransparency(transparency, override);
            }
            return;
        }
        if (transparency < 0 || transparency > 255) {
            throw new IllegalArgumentException("valid values are between 0-255");
        }
        if (this.transparency != (byte) transparency) {
            this.transparency = (byte) transparency;

            if (!override) {
                modifiedFlag |= TRANSPARENCY_MODIFIED;
            }
            firePropertyChanged(TRANSPARENCY);
        }
    }


    /**
     * Sets the Style Padding
     * 
     * @param orientation one of: Component.TOP, Component.BOTTOM, Component.LEFT, Component.RIGHT
     * @param gap number of pixels to padd
     * @param override If set to true allows the look and feel/theme to override 
     * the value in this attribute when changing a theme/look and feel
     */
    public void setPadding(int orientation, int gap,boolean override) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setPadding(orientation, gap, override);
            }
            return;
        }
        if (orientation < Component.TOP || orientation > Component.RIGHT) {
            throw new IllegalArgumentException("wrong orientation " + orientation);
        }
        if (gap < 0) {
            throw new IllegalArgumentException("padding cannot be negative");
        }
        if (padding[orientation] != gap) {
            padding[orientation] = gap;

            if (!override) {
                modifiedFlag |= PADDING_MODIFIED;
            }
            firePropertyChanged(PADDING);
        }
    }


    /**
     * Sets the Style Margin
     * 
     * @param orientation one of: Component.TOP, Component.BOTTOM, Component.LEFT, Component.RIGHT
     * @param gap number of margin using the current unit
     * @param override If set to true allows the look and feel/theme to override 
     * the value in this attribute when changing a theme/look and feel
     */
    public void setMargin(int orientation, int gap, boolean override) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setMargin(orientation, gap, override);
            }
            return;
        }
        if (orientation < Component.TOP || orientation > Component.RIGHT) {
            throw new IllegalArgumentException("wrong orientation " + orientation);
        }
        if (gap < 0) {
            throw new IllegalArgumentException("margin cannot be negative");
        }
        if (margin[orientation] != gap) {
            margin[orientation] = gap;
            if (!override) {
                modifiedFlag |= MARGIN_MODIFIED;
            }
            firePropertyChanged(MARGIN);
        }
    }

    
    private void firePropertyChanged(String propertName) {
        roundRectCache = null;
        nativeOSCache = null;
        if (listeners == null) {
            return;
        }
        listeners.fireStyleChangeEvent(propertName, this);
    }

    /**
     * Adds a Style Listener to the Style Object.
     * 
     * @param l a style listener
     */
    public void addStyleListener(StyleListener l) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.addStyleListener(l);
            }
            return;
        }
        if (listeners == null) {
            listeners = new EventDispatcher();
        }
        listeners.addListener(l);
    }

    /**
     * Removes a Style Listener from the Style Object.
     * 
     * @param l a style listener
     */
    public void removeStyleListener(StyleListener l) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.removeStyleListener(l);
            }
            return;
        }
        if (listeners != null) {
            listeners.removeListener(l);
        }
    }
    
    /**
     * This method removes all Listeners from the Style
     */
    public void removeListeners(){
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.removeListeners();
            }
            return;
        }
        if (listeners != null) {
            listeners = null;
        }
    }
    
    void resetModifiedFlag() {
        modifiedFlag = 0;
    }

    /**
     * Sets the border for the style
     * 
     * @param border new border object for the component
     */
    public void setBorder(Border border) {
        setBorder(border, false);
    }

    /**
     * Sets the border for the style
     * 
     * @param border new border object for the component
     * @param override If set to true allows the look and feel/theme to override 
     * the value in this attribute when changing a theme/look and feel
     */
    public void setBorder(Border border, boolean override) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setBorder(border, override);
            }
            return;
        }
        if ((this.border == null && border != null) ||
                (this.border != null && !this.border.equals(border))) {
            this.border = border;
            if (!override) {
                modifiedFlag |= BORDER_MODIFIED;
            }
            firePropertyChanged(BORDER);
        }
    }
    
    /**
     * Returns the border for the style
     * 
     * @return the border
     */
    public Border getBorder() {
        return border;
    }
    
    /**
     * Return the background painter for this style, normally this would be 
     * the internal image/color painter but can be user defined 
     * 
     * @return the background painter
     */
    public Painter getBgPainter() {
        return bgPainter;
    }

    /**
     * Defines the background painter for this style, normally this would be 
     * the internal image/color painter but can be user defined 
     * 
     * @param bgPainter new painter to install into the style
     */
    public void setBgPainter(Painter bgPainter) {
        if(proxyTo != null) {
            for(Style s : proxyTo) {
                s.setBgPainter(bgPainter);
            }
            return;
        }
        this.bgPainter = bgPainter;
        firePropertyChanged(PAINTER);
    }

    /**
     * Indicates the units used for padding elements, if null pixels are used if not this is a 4 element array containing values
     * of UNIT_TYPE_PIXELS, UNIT_TYPE_DIPS or UNIT_TYPE_SCREEN_PERCENTAGE
     * @return the paddingUnit
     */
    public byte[] getPaddingUnit() {
        return paddingUnit;
    }

    /**
     * Indicates the units used for padding elements, if null pixels are used if not this is a 4 element array containing values
     * of UNIT_TYPE_PIXELS, UNIT_TYPE_DIPS or UNIT_TYPE_SCREEN_PERCENTAGE
     * @param paddingUnit the paddingUnit to set
     */
    public void setPaddingUnit(byte... paddingUnit) {
        if(proxyTo != null) {
            if(paddingUnit != null && paddingUnit.length < 4) {
                paddingUnit = new byte[]{paddingUnit[0], paddingUnit[0], paddingUnit[0], paddingUnit[0]};
            }
            for(Style s : proxyTo) {
                s.setPaddingUnit(paddingUnit);
            }
            return;
        }
        if(paddingUnit != null && paddingUnit.length < 4) {
            this.paddingUnit = new byte[]{paddingUnit[0], paddingUnit[0], paddingUnit[0], paddingUnit[0]};
        } else {
            this.paddingUnit = paddingUnit;
        }
    }

    /**
     * Indicates the units used for margin elements, if null pixels are used if not this is a 4 element array containing values
     * of UNIT_TYPE_PIXELS, UNIT_TYPE_DIPS or UNIT_TYPE_SCREEN_PERCENTAGE
     * @return the marginUnit
     */
    public byte[] getMarginUnit() {
        return marginUnit;
    }

    /**
     * Indicates the units used for margin elements, if null pixels are used if not this is a 4 element array containing values
     * of UNIT_TYPE_PIXELS, UNIT_TYPE_DIPS or UNIT_TYPE_SCREEN_PERCENTAGE
     * @param marginUnit the marginUnit to set
     */
    public void setMarginUnit(byte... marginUnit) {
        if(proxyTo != null) {
            if(marginUnit != null && marginUnit.length < 4) {
                marginUnit = new byte[]{marginUnit[0], marginUnit[0], marginUnit[0], marginUnit[0]};
            }
            for(Style s : proxyTo) {
                s.setMarginUnit(marginUnit);
            }
            return;
        }
        if(marginUnit != null && marginUnit.length < 4) {
            this.marginUnit = new byte[]{marginUnit[0], marginUnit[0], marginUnit[0], marginUnit[0]};
        } else {
            this.marginUnit = marginUnit;
        }
    }
}
