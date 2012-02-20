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

import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Painter;
import com.codename1.ui.RGBImage;
import com.codename1.ui.geom.Rectangle;

/**
 * Base class that allows us to render a border for a component, a border is drawn before
 * the component and is drawn within the padding region of the component. It is the
 * responsibility of the component not to draw outside of the border line.
 * <p>This class can be extended to provide additional border types and custom made
 * border types.
 * <p>A border can optionally paint the background of the component, this depends on
 * the border type and is generally required for rounded borders that "know" the area
 * that should be filled.
 *
 * @author Shai Almog
 */
public class Border {
    private static Border defaultBorder = Border.createEtchedRaised(0x020202, 0xBBBBBB);
    
    private static final int TYPE_EMPTY = 0;
    private static final int TYPE_LINE = 1;
    private static final int TYPE_ROUNDED = 2;
    private static final int TYPE_ROUNDED_PRESSED = 3;
    private static final int TYPE_ETCHED_LOWERED = 4;
    private static final int TYPE_ETCHED_RAISED = 5;
    private static final int TYPE_BEVEL_RAISED = 6;
    private static final int TYPE_BEVEL_LOWERED = 7;
    private static final int TYPE_IMAGE = 8;
    private static final int TYPE_COMPOUND = 9;
    private static final int TYPE_IMAGE_HORIZONTAL = 10;
    private static final int TYPE_IMAGE_VERTICAL = 11;
    private static final int TYPE_DASHED = 12;
    private static final int TYPE_DOTTED = 13;
    private static final int TYPE_DOUBLE = 14;
    private static final int TYPE_GROOVE = 15;
    private static final int TYPE_RIDGE = 16;
    private static final int TYPE_INSET = 17;
    private static final int TYPE_OUTSET = 18;
    private static final int TYPE_IMAGE_SCALED = 19;

    // variables are package protected for the benefit of the resource editor!
    int type;
    Image[] images;

    private Image[] specialTile;
    private Component trackComponent;

    /**
     * Indicates whether theme colors should be used or whether colors are specified
     * in the border
     */
    boolean themeColors;
    
    int colorA;
    int colorB;
    int colorC;
    int colorD;
    int thickness = 0;
    int arcWidth;
    int arcHeight;
    boolean outline = true;
    Border pressedBorder;
    Border focusBorder;
    Border [] compoundBorders;
    Border outerBorder; // A border added outside of this border (Used for CSS outline property, but can also be used for other purposes)
    String borderTitle; // border title, currently supported only for line borders
    private boolean paintOuterBorderFirst;
    
    private static final int TITLE_MARGIN = 10;
    private static final int TITLE_SPACE = 5;


    private static Border empty;
    
    /**
     * Prevents usage of new operator, use the factory methods in the class or subclass
     * to create new border types.
     */
    protected Border() {
    }

    /**
     * This method is designed mainly for the purpose of creating an arrow that will track a specific component using the image border
     * the tile given would be an arrow like image just like the ones used for the top/bottom/left/right images. This image would be positioned
     * so it points at the center of the track component
     *
     * @param tileTop an image that will replace one of the tiles on the top if the track component is there
     * @param tileBottom an image that will replace one of the tiles on the bottom if the track component is there
     * @param tileLeft an image that will replace one of the tiles on the left if the track component is there
     * @param tileRight an image that will replace one of the tiles on the right if the track component is there
     * @param trackComponent the component that will be tracked for the positioning of the tile
     */
    public void setImageBorderSpecialTile(Image tileTop, Image tileBottom, Image tileLeft, Image tileRight, Component trackComponent) {
        specialTile = new Image[] {tileTop, tileBottom, tileLeft, tileRight};
        this.trackComponent = trackComponent;
    }

    /**
     * Cleans the tile tracking state allowing the garbage collector to pick up the component and the image data
     */
    public void clearImageBorderSpecialTile() {
        specialTile = null;
        trackComponent = null;
    }

    /**
     * Adss a border that wraps this border
     * 
     * @param outer The outer border
     */
    public void addOuterBorder(Border outer) {
        outerBorder=outer;
    }

    /**
     * Returns the minimum size required to properly display this border, normally this
     * is 0 but a border might deem itself undisplayable with too small a size e.g. for
     * the case of an image border the minimum height would be top + bottom and the minimum
     * width would be left+right
     *
     * @return 0 if not applicable or a dimension if it is.
     */
    public int getMinimumHeight() {
        if(images != null) {
            if(images.length < 4) {
                if(type == TYPE_IMAGE_HORIZONTAL) {
                    return images[0].getHeight();
                } else {
                    return images[0].getHeight() + images[1].getHeight();
                }
            }
            Image topLeft = images[4];
            Image bottomRight = images[7];
            return topLeft.getHeight() + bottomRight.getHeight();
        }
        return 0;
    }

    /**
     * Returns the minimum size required to properly display this border, normally this
     * is 0 but a border might deem itself undisplayable with too small a size e.g. for
     * the case of an image border the minimum height would be top + bottom and the minimum
     * width would be left+right
     *
     * @return 0 if not applicable or a dimension if it is.
     */
    public int getMinimumWidth() {
        if(images != null) {
            if(images.length < 4) {
                if(type == TYPE_IMAGE_HORIZONTAL) {
                    return images[0].getWidth() + images[1].getWidth();
                } else {
                    return images[0].getWidth();
                }
            }
            Image topLeft = images[4];
            Image topRight = images[5];
            return topLeft.getWidth() + topRight.getWidth();
        }
        return 0;
    }

    /**
     * Returns an empty border, this is mostly useful for overriding components that
     * have a border by default
     * 
     * @return a border than draws nothing
     * @deprecated use createEmpty instead
     */
    public static Border getEmpty() {
        if(empty == null) {
            empty = new Border();
        }
        return empty;
    }
    
    /**
     * Creates an empty border, this is useful where we don't want a border for a 
     * component but want a focus border etc...
     * 
     * @return a border than draws nothing
     */
    public static Border createEmpty() {
        return new Border();
    }
    
    /**
     * The given top/bottom/left/right images are tiled appropriately across the matching sides of the border and the corners are placed
     * as expected in the four corners. The background image is optional and it will be tiled in  the background if necessary.
     *
     * <p>By default this border does not override background unless a background image is specified
     * 
     * @param top the image of the top line
     * @param bottom the image of the bottom line
     * @param left the image of the left line
     * @param right the image of the right line
     * @param topLeft the image of the top left corner
     * @param topRight the image of the top right corner
     * @param bottomLeft the image of the bottom left corner
     * @param bottomRight the image of the bottom right corner
     * @param background the image of the background (optional)
     * @return new border instance
     */
    public static Border createImageBorder(Image top, Image bottom, Image left, Image right, Image topLeft, Image topRight,
        Image bottomLeft, Image bottomRight, Image background) {
        Border b = new Border();
        b.type = TYPE_IMAGE;
        b.images = new Image[] {top, bottom, left, right, topLeft, topRight, bottomLeft, 
                        bottomRight, background};
        return b;
    }
    
    /**
     * The given top/bottom/left/right images are scaled appropriately across the matching sides of the border and the corners are placed
     * as expected in the four corners. The background image is optional and it will be tiled in  the background if necessary.
     *
     * <p>By default this border does not override background unless a background image is specified
     *
     * @param top the image of the top line
     * @param bottom the image of the bottom line
     * @param left the image of the left line
     * @param right the image of the right line
     * @param topLeft the image of the top left corner
     * @param topRight the image of the top right corner
     * @param bottomLeft the image of the bottom left corner
     * @param bottomRight the image of the bottom right corner
     * @param background the image of the background (optional)
     * @return new border instance
     */
    public static Border createImageScaledBorder(Image top, Image bottom, Image left, Image right, Image topLeft, Image topRight,
        Image bottomLeft, Image bottomRight, Image background) {
        Border b = new Border();
        b.type = TYPE_IMAGE_SCALED;
        b.images = new Image[] {top, bottom, left, right, topLeft, topRight, bottomLeft,
                        bottomRight, background};
        return b;
    }

    /**
     * This is an image border that can only grow horizontally
     *
     * @param left the image of the left side
     * @param right the image of the right side
     * @param center the image of the center
     * @return new border instance
     */
    public static Border createHorizonalImageBorder(Image left, Image right, Image center) {
        Border b = new Border();
        b.type = TYPE_IMAGE_HORIZONTAL;
        b.images = new Image[] {left, right, center};
        return b;
    }

    /**
     * This is an image border that can only grow vertically
     *
     * @param top the image of the top
     * @param bottom the image of the bottom
     * @param center the image of the center
     * @return new border instance
     */
    public static Border createVerticalImageBorder(Image top, Image bottom, Image center) {
        Border b = new Border();
        b.type = TYPE_IMAGE_VERTICAL;
        b.images = new Image[] {top, bottom, center};
        return b;
    }

    /**
     * The given images are tiled appropriately across the matching side of the border, rotated and placed
     * as expected in the four corners. The background image is optional and it will be tiled in
     * the background if necessary.
     * <p>By default this border does not override background unless a background image is specified.
     * <p>Notice that this version of the method is potentially much more efficient since images
     * are rotated internally and this might save quite a bit of memory!
     * <p><b>The top and topLeft images must be square!</b> The width and height of these images
     * must be equal otherwise rotation won't work as you expect.
     * 
     * @param top the image of the top line
     * @param topLeft the image of the top left corner
     * @param background the image of the background (optional)
     * @return new border instance
     */
    public static Border createImageBorder(Image top, Image topLeft, Image background) {
        Border b = new Border();
        b.type = TYPE_IMAGE;
        b.images = new Image[] {top, top.rotate(180), top.rotate(270), top.rotate(90), topLeft, topLeft.rotate(90), 
                topLeft.rotate(270), topLeft.rotate(180), background};
        return b;
    }

    /**
     * Creates a line border that uses the color of the component foreground for drawing
     * 
     * @param thickness thickness of the border in pixels
     * @return new border instance
     */
    public static Border createLineBorder(int thickness) {
        Border b = new Border();
        b.type = TYPE_LINE;
        b.themeColors = true;
        b.thickness = thickness;
        return b;
    }

    /**
     * Creates a dotted border with the specified thickness and color
     *
     * @param thickness The border thickness in pixels
     * @param color The border color
     * @return The border
     */
    public static Border createDottedBorder(int thickness,int color) {
        return createCSSBorder(TYPE_DOTTED, thickness, color);
    }

    /**
     * Creates a dashed border with the specified thickness and color
     *
     * @param thickness The border thickness in pixels
     * @param color The border color
     * @return The border
     */
    public static Border createDashedBorder(int thickness,int color) {
        return createCSSBorder(TYPE_DASHED, thickness, color);
    }

    /**
     * Creates a double border with the specified thickness and color
     *
     * @param thickness The border thickness in pixels
     * @param color The border color
     * @return The border
     */
    public static Border createDoubleBorder(int thickness,int color) {
        return createCSSBorder(TYPE_DOUBLE, thickness, color);
    }

    /**
     * Creates a dotted border with the specified thickness and the theme colors
     *
     * @param thickness The border thickness in pixels
     * @return The border
     */
    public static Border createDottedBorder(int thickness) {
        return createCSSBorder(TYPE_DOTTED, thickness);
    }

    /**
     * Creates a dashed border with the specified thickness and the theme colors
     *
     * @param thickness The border thickness in pixels
     * @return The border
     */
    public static Border createDashedBorder(int thickness) {
        return createCSSBorder(TYPE_DASHED, thickness);
    }

    /**
     * Creates a double border with the specified thickness and color
     *
     * @param thickness The border thickness in pixels
     * @return The border
     */
    public static Border createDoubleBorder(int thickness) {
        return createCSSBorder(TYPE_DOUBLE, thickness);
    }



    /**
     * Creates an outset border with the specified thickness and theme colors
     * 
     * @param thickness The border thickness in pixels
     * @return The border
     */
    public static Border createOutsetBorder(int thickness) {
        return createCSSBorder(TYPE_OUTSET, thickness);
    }

    /**
     * Creates an outset border with the specified thickness and color
     *
     * @param thickness The border thickness in pixels
     * @param color The border color
     * @return The border
     */
    public static Border createOutsetBorder(int thickness,int color) {
        return createCSSBorder(TYPE_OUTSET, thickness,color);
    }

    /**
     * Creates an inset border with the specified thickness and theme colors
     *
     * @param thickness The border thickness in pixels
     * @return The border
     */
    public static Border createInsetBorder(int thickness) {
        return createCSSBorder(TYPE_INSET, thickness);
    }

    /**
     * Creates an inset border with the specified thickness and color
     *
     * @param thickness The border thickness in pixels
     * @param color The border color
     * @return The border
     */
    public static Border createInsetBorder(int thickness,int color) {
        return createCSSBorder(TYPE_INSET, thickness,color);
    }

    /**
     * Creates a groove border with the specified thickness and theme colors
     *
     * @param thickness The border thickness in pixels
     * @return The border
     */
    public static Border createGrooveBorder(int thickness) {
        return createCSSBorder(TYPE_GROOVE, thickness);
    }

    /**
     * Creates a groove border with the specified thickness and color
     *
     * @param thickness The border thickness in pixels
     * @param color The border color
     * @return The border
     */
    public static Border createGrooveBorder(int thickness,int color) {
        return createCSSBorder(TYPE_GROOVE, thickness,color);
    }

    /**
     * Creates a ridge border with the specified thickness and theme colors
     *
     * @param thickness The border thickness in pixels
     * @return The border
     */
    public static Border createRidgeBorder(int thickness) {
        return createCSSBorder(TYPE_RIDGE, thickness);
    }

    /**
     * Creates a ridge border with the specified thickness and color
     *
     * @param thickness The border thickness in pixels
     * @param color The border color
     * @return The border
     */
    public static Border createRidgeBorder(int thickness,int color) {
        return createCSSBorder(TYPE_RIDGE, thickness,color);
    }


    private static Border createCSSBorder(int type,int thickness) {
        Border b = new Border();
        b.type = type;
        b.themeColors = true;
        b.thickness = thickness;
        return b;
    }

    private static Border createCSSBorder(int type,int thickness,int color) {
        Border b = new Border();
        b.type = type;
        b.colorA = color;
        b.thickness = thickness;
        return b;
    }



    /**
     * Creates a line border with the specified title
     *
     * @param thickness thickness of the border in pixels
     * @param title The border's title
     * @return new border instance
     */
    public static Border createLineBorder(int thickness, String title) {
        Border b = new Border();
        b.type = TYPE_LINE;
        b.themeColors = true;
        b.thickness = thickness;
        b.borderTitle=title;
        return b;
    }

    /**
     * Creates a line border that uses the given color for the component
     * 
     * @param thickness thickness of the border in pixels
     * @param color the color for the border
     * @param title The border's title
     * @return new border instance
     */
    public static Border createLineBorder(int thickness, int color, String title) {
        Border b = new Border();
        b.type = TYPE_LINE;
        b.themeColors = false;
        b.thickness = thickness;
        b.colorA = color;
        b.borderTitle=title;
        return b;
    }

    /**
     * Creates a line border that uses the given color for the component
     * 
     * @param thickness thickness of the border in pixels
     * @param color the color for the border
     * @return new border instance
     */
    public static Border createLineBorder(int thickness, int color) {
        Border b = new Border();
        b.type = TYPE_LINE;
        b.themeColors = false;
        b.thickness = thickness;
        b.colorA = color;
        return b;
    }
    
    /**
     * Creates a rounded corner border that uses the color of the component foreground for drawing.
     * Due to technical issues (lack of shaped clipping) performance and memory overhead of round 
     * borders can be low if used with either a bgImage or translucency! 
     * <p>This border overrides any painter used on the component and would ignor such a painter.
     * 
     * @param arcWidth the horizontal diameter of the arc at the four corners.
     * @param arcHeight the vertical diameter of the arc at the four corners.
     * @return new border instance
     */
    public static Border createRoundBorder(int arcWidth, int arcHeight) {
        Border b = new Border();
        b.type = TYPE_ROUNDED;
        b.themeColors = true;
        b.arcHeight = arcHeight;
        b.arcWidth = arcWidth;
        return b;
    }

    /**
     * Creates a rounded corner border that uses the color of the component foreground for drawing.
     * Due to technical issues (lack of shaped clipping) performance and memory overhead of round
     * borders can be low if used with either a bgImage or translucency!
     * <p>This border overrides any painter used on the component and would ignor such a painter.
     *
     * @param arcWidth the horizontal diameter of the arc at the four corners.
     * @param arcHeight the vertical diameter of the arc at the four corners.
     * @param outline whether the round rect border outline should be drawn
     * @return new border instance
     */
    public static Border createRoundBorder(int arcWidth, int arcHeight, boolean outline) {
        Border b = createRoundBorder(arcWidth, arcHeight);
        b.outline = outline;
        return b;
    }

    /**
     * Creates a rounded border that uses the given color for the component.
     * Due to technical issues (lack of shaped clipping) performance and memory overhead of round 
     * borders can be low if used with either a bgImage or translucency! 
     * <p>This border overrides any painter used on the component and would ignor such a painter.
     * 
     * @param arcWidth the horizontal diameter of the arc at the four corners.
     * @param arcHeight the vertical diameter of the arc at the four corners.
     * @param color the color for the border
     * @return new border instance
     */
    public static Border createRoundBorder(int arcWidth, int arcHeight, int color) {
        Border b = new Border();
        b.type = TYPE_ROUNDED;
        b.themeColors = false;
        b.colorA = color;
        b.arcHeight = arcHeight;
        b.arcWidth = arcWidth;
        return b;
    }
    
    /**
     * Creates a rounded border that uses the given color for the component.
     * Due to technical issues (lack of shaped clipping) performance and memory overhead of round
     * borders can be low if used with either a bgImage or translucency!
     * <p>This border overrides any painter used on the component and would ignor such a painter.
     *
     * @param arcWidth the horizontal diameter of the arc at the four corners.
     * @param arcHeight the vertical diameter of the arc at the four corners.
     * @param color the color for the border
     * @param outline whether the round rect border outline should be drawn
     * @return new border instance
     */
    public static Border createRoundBorder(int arcWidth, int arcHeight, int color, boolean outline) {
        Border b = createRoundBorder(arcWidth, arcHeight, color);
        b.outline = outline;
        return b;
    }

    /**
     * Creates a lowered etched border with default colors, highlight is derived
     * from the component and shadow is a plain dark color
     * 
     * @return new border instance
     */
    public static Border createEtchedLowered() {
        Border b = new Border();
        b.type = TYPE_ETCHED_LOWERED;
        b.themeColors = true;
        return b;
    }

    /**
     * Creates a raised etched border with the given colors
     * 
     * @param highlight color RGB value
     * @param shadow color RGB value
     * @return new border instance
     */
    public static Border createEtchedLowered(int highlight, int shadow) {
        Border b = new Border();
        b.type = TYPE_ETCHED_LOWERED;
        b.themeColors = false;
        b.colorA = shadow;
        b.colorB = highlight;
        return b;
    }

    
    /**
     * Creates a lowered etched border with default colors, highlight is derived
     * from the component and shadow is a plain dark color
     * 
     * @return new border instance
     */
    public static Border createEtchedRaised() {
        Border b = new Border();
        b.type = TYPE_ETCHED_RAISED;
        b.themeColors = true;
        b.thickness = 2;
        return b;
    }

    /**
     * Creates a raised etched border with the given colors
     * 
     * @param highlight color RGB value
     * @param shadow color RGB value
     * @return new border instance
     */
    public static Border createEtchedRaised(int highlight, int shadow) {
        Border b = new Border();
        b.type = TYPE_ETCHED_RAISED;
        b.themeColors = false;
        b.colorA = highlight;
        b.colorB = shadow;
        b.thickness = 2;
        return b;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass() == getClass()) {
            Border b = (Border)obj;

            if ((b.type==TYPE_COMPOUND) && (type==TYPE_COMPOUND)) {
                for(int i=Component.TOP;i<=Component.RIGHT;i++) {
                    if (!isSame(compoundBorders[i], b.compoundBorders[i])) {
                        return false;
                    }
                }
                return true;
            }

            boolean v = ((themeColors==b.themeColors) &&
                (type==b.type) &&
                (thickness==b.thickness) &&
                (colorA==b.colorA) &&
                (colorB==b.colorB) &&
                (colorC==b.colorC) &&
                (colorD==b.colorD) &&
                (arcWidth==b.arcWidth) &&
                (arcHeight==b.arcHeight) &&
                (outline==b.outline) &&
                (isSame(borderTitle, b.borderTitle)) &&
                (isSame(outerBorder, b.outerBorder))
                );
            if(v && type == TYPE_IMAGE || type == TYPE_IMAGE_HORIZONTAL || type == TYPE_IMAGE_VERTICAL || type == TYPE_IMAGE_SCALED) {
                for(int iter = 0 ; iter < images.length ; iter++) {
                    if(images[iter] != b.images[iter]) {
                        return false;
                    }
                }
            }
            return v;
        }
        return false;
    }

    /**
     * Compares two object including the scenario one of them is null (thus avoiding equals pitfalls)
     * 
     * @param obj1 The first object to compare
     * @param obj2 The second object to compare
     * @return true if the two object are equal (or both null), false otherwise
     */
    private static boolean isSame(Object obj1,Object obj2) {
        if (obj1==null) {
            return (obj2==null);
        } else if (obj2==null) {
            return (obj1==null);
        }
        return obj1.equals(obj2);
    }

    public static Border createCompoundBorder(Border top, Border bottom, Border left, Border right) {

        if ((top != null && !top.isRectangleType()) || 
                (bottom != null && !bottom.isRectangleType()) ||
                (left != null && !left.isRectangleType()) ||
                (right != null && !right.isRectangleType())) {
            throw new IllegalArgumentException("Compound Border can be created "
                    + "only from Rectangle types Borders");
        }
        if ((isSame(top, bottom)) && (isSame(top, left)) && (isSame(top, right))) {
            return top; // Borders are all the same, returning one of them instead of creating a compound border which is more resource consuming
        }

        Border b = new Border();
        b.type = TYPE_COMPOUND;
        b.compoundBorders = new Border[4];
        b.compoundBorders[Component.TOP] = top;
        b.compoundBorders[Component.BOTTOM] = bottom;
        b.compoundBorders[Component.LEFT] = left;
        b.compoundBorders[Component.RIGHT] = right;

        // Calculates the thickness of the entire border as the maximum of all 4 sides
        b.thickness=0;
        for(int i=Component.TOP;i<=Component.RIGHT;i++) {
            if (b.compoundBorders[i]!=null) {
                int sideThickness=b.compoundBorders[i].thickness;
                if (sideThickness>b.thickness) {
                    b.thickness=sideThickness;
                }
            }
        }

        return b;
    }

    /**
     * Returns true if installing this border will override the painting of the component background
     * 
     * @return true if this border replaces the painter
     */
    public boolean isBackgroundPainter() {
        return type == TYPE_ROUNDED || type == TYPE_ROUNDED_PRESSED || type == TYPE_IMAGE
                 || type == TYPE_IMAGE_HORIZONTAL || type == TYPE_IMAGE_VERTICAL || type == TYPE_IMAGE_SCALED;
    }

    /**
     * Returns true if this border type is a rectangle border.
     *
     * @return true if this border is rectangle
     */
    public boolean isRectangleType() {
        return type == TYPE_LINE || type == TYPE_BEVEL_LOWERED ||
                type == TYPE_BEVEL_RAISED || type == TYPE_ETCHED_LOWERED ||
                type == TYPE_ETCHED_RAISED || type == TYPE_COMPOUND 
                || type == TYPE_EMPTY || type==TYPE_DOTTED || type==TYPE_DASHED || type==TYPE_DOUBLE
                || type == TYPE_OUTSET || type == TYPE_INSET || type == TYPE_GROOVE || type == TYPE_RIDGE;
    }

    /**
     * Creates a lowered bevel border with default colors, highlight is derived
     * from the component and shadow is a plain dark color
     * 
     * @return new border instance
     */
    public static Border createBevelLowered() {
        Border b = new Border();
        b.type = TYPE_BEVEL_LOWERED;
        b.themeColors = true;
        b.thickness = 2;
        return b;
    }

    /**
     * Creates a raised bevel border with the given colors
     * 
     * @param highlightOuter  RGB of the outer edge of the highlight area
     * @param highlightInner  RGB of the inner edge of the highlight area
     * @param shadowOuter     RGB of the outer edge of the shadow area
     * @param shadowInner     RGB of the inner edge of the shadow area
     * @return new border instance
     */
    public static Border createBevelLowered(int highlightOuter, int highlightInner,
                        int shadowOuter, int shadowInner) {
        Border b = new Border();
        b.type = TYPE_BEVEL_LOWERED;
        b.themeColors = false;
        b.colorA = highlightOuter;
        b.colorB = highlightInner;
        b.colorC = shadowOuter;
        b.colorD = shadowInner;
        b.thickness = 2;
        return b;
    }

    
    /**
     * Creates a lowered bevel border with default colors, highlight is derived
     * from the component and shadow is a plain dark color
     * 
     * @return new border instance
     */
    public static Border createBevelRaised() {
        Border b = new Border();
        b.type = TYPE_BEVEL_RAISED;
        b.themeColors = true;
        b.thickness = 2;
        return b;
    }

    /**
     * Creates a raised bevel border with the given colors
     * 
     * @param highlightOuter  RGB of the outer edge of the highlight area
     * @param highlightInner  RGB of the inner edge of the highlight area
     * @param shadowOuter     RGB of the outer edge of the shadow area
     * @param shadowInner     RGB of the inner edge of the shadow area
     * @return new border instance
     */
    public static Border createBevelRaised(int highlightOuter, int highlightInner,
                        int shadowOuter, int shadowInner) {
        Border b = new Border();
        b.type = TYPE_BEVEL_RAISED;
        b.themeColors = false;
        b.colorA = highlightOuter;
        b.colorB = highlightInner;
        b.colorC = shadowOuter;
        b.colorD = shadowInner;
        b.thickness = 2;
        return b;
    }
        
    /**
     * Allows us to define a border that will act as the pressed version of this border
     * 
     * @param pressed a border that will be returned by the pressed version method
     */
    public void setPressedInstance(Border pressed) {
        pressedBorder = pressed;
    }
    
    /**
     * Allows us to define a border that will act as the focused version of this border
     * 
     * @param focused a border that will be returned by the focused version method
     * @deprecated use the getSelectedStyle() method in the component class
     */
    public void setFocusedInstance(Border focused) {
        focusBorder = focused;
    }
    
    /**
     * Returns the focused version of the border if one is installed
     * 
     * @return a focused version of this border if one exists
     * @deprecated use the getSelectedStyle() method in the component class
     */
    public Border getFocusedInstance() {
        if(focusBorder != null) {
            return focusBorder;
        }
        return this;
    }
    /**
     * Returns the pressed version of the border if one is set by the user
     * 
     * @return the pressed instance of this border
     */
    public Border getPressedInstance() {
        if(pressedBorder != null) {
            return pressedBorder;
        }
        return this;
    }
    
    /**
     * When applied to buttons borders produce a version that reverses the effects 
     * of the border providing a pressed feel
     * 
     * @return a border that will make the button feel pressed
     */
    public Border createPressedVersion() {
        if(pressedBorder != null) {
            return pressedBorder;
        }
        switch(type) {
            case TYPE_LINE:
                return createLineBorder(thickness + 1, colorA);
            case TYPE_ETCHED_LOWERED: {
                Border b = createEtchedRaised(colorA, colorB);
                b.themeColors = themeColors;
                return b;
            }
            case TYPE_ETCHED_RAISED: {
                Border b = createEtchedLowered(colorA, colorB);
                b.themeColors = themeColors;
                return b;
            }
            case TYPE_BEVEL_RAISED: {
                Border b = createBevelLowered(colorA, colorB, colorC, colorD);
                b.themeColors = themeColors;
                return b;
            }
            case TYPE_BEVEL_LOWERED: {
                Border b = createBevelRaised(colorA, colorB, colorC, colorD);
                b.themeColors = themeColors;
                return b;
            }
            case TYPE_ROUNDED: {
                Border b = createRoundBorder(arcWidth, arcHeight, colorA);
                b.themeColors = themeColors;
                b.type = TYPE_ROUNDED_PRESSED;
                return b;
            }
            case TYPE_ROUNDED_PRESSED: {
                Border b = createRoundBorder(arcWidth, arcHeight, colorA);
                b.themeColors = themeColors;
                return b;
            }
        }
        return this;
    }
    
    /**
     * Has effect when the border demands responsibility for background painting
     * normally the painter will perform this work but in this case the border might
     * do it instead.
     * 
     * @param g graphics context to draw onto
     * @param c component whose border should be drawn
     */
    public void paintBorderBackground(Graphics g, Component c) {
        int x = c.getX();
        int y = c.getY();
        int width = c.getWidth();
        int height = c.getHeight();
        if (outerBorder != null) {
            if (paintOuterBorderFirst) {
                outerBorder.paintBorderBackground(g, c);
                paintBorderBackground(g, x + thickness, y + thickness, width - thickness * 2, height - thickness * 2, c);
            } else {
                paintBorderBackground(g, x + thickness, y + thickness, width - thickness * 2, height - thickness * 2, c);
                outerBorder.paintBorderBackground(g, c);
           }
        } else {
            paintBorderBackground(g, x, y, width, height, c);
        }
    }

   private void paintBorderBackground(Graphics g, final int xParameter, final int yParameter,
            final int widthParameter, final int heightParameter, Component c) {
        int originalColor = g.getColor();
        int x = xParameter;
        int y = yParameter;
       int width = widthParameter;
        int height = heightParameter;
        switch(type) {
            case TYPE_ROUNDED_PRESSED:
                x++;
                y++;
                width -= 2;
                height -= 2;
            case TYPE_ROUNDED:
                // Removing this due to issue 301, not sure regarding this...
                //width--;
                //height--;
                // rounded is also responsible for drawing the background
                Style s = c.getStyle();
                if((s.getBgImage() != null && s.getBackgroundType() == Style.BACKGROUND_IMAGE_SCALED) ||
                    s.getBackgroundType() > 1) {
                    Object w = s.roundRectCache;
                    Image i = null;
                    if(w != null) {
                        i = (Image)Display.getInstance().extractHardRef(w);
                    }
                    if(i != null && i.getWidth() == width && i.getHeight() == height) {
                        g.drawImage(i, x, y);
                    } else {
                        // we need to draw a background image!
                        i = Image.createImage(width, height);
                        Graphics imageG = i.getGraphics();
                        imageG.setColor(0);
                        imageG.fillRoundRect(0, 0, width, height, arcWidth, arcHeight);
                        int[] rgb = i.getRGBCached();
                        int transColor = rgb[0];
                        int[] imageRGB;
                        if(s.getBackgroundType() == Style.BACKGROUND_IMAGE_SCALED) {
                            imageRGB = s.getBgImage().scaled(width, height).getRGBCached();
                        } else {
                            Image bgPaint = Image.createImage(width, height);
                            Painter p = s.getBgPainter();

                            // might occur during temporary conditions in the theme switching
                            if(p == null) {
                                return;
                            }
                            p.paint(bgPaint.getGraphics(), new Rectangle(0, 0, width, height));
                            imageRGB = bgPaint.getRGB();
                        }
                        for(int iter = 0 ; iter < rgb.length ; iter++) {
                            if(rgb[iter] == transColor) {
                                imageRGB[iter] = 0;
                            }
                        }
                        i = Image.createImage(imageRGB, width, height);
                        s.roundRectCache = Display.getInstance().createSoftWeakRef(i);
                        g.drawImage(i, x, y);
                    }
                } else {
                    int foreground = g.getColor();
                    g.setColor(s.getBgColor());

                    // Its opaque much easier job!
                    if(s.getBgTransparency() == ((byte)0xff)) {
                        g.fillRoundRect(x, y , width, height, arcWidth, arcHeight);
                    } else {
                        if(g.isAlphaSupported()) {
                            int alpha = g.getAlpha();
                            g.setAlpha(s.getBgTransparency() & 0xff);
                            g.fillRoundRect(x, y , width, height, arcWidth, arcHeight);
                            g.setAlpha(alpha);
                        } else {
                            // if its transparent we don't need to do anything, if its
                            // translucent... well....
                            if(s.getBgTransparency() != 0) {
                                Image i = Image.createImage(width, height);
                                int[] imageRgb;
                                if(g.getColor() != 0xffffff) {
                                    Graphics imageG = i.getGraphics();
                                    imageG.setColor(g.getColor());
                                    imageG.fillRoundRect(0, 0 , width, height, arcWidth, arcHeight);
                                    imageRgb = i.getRGBCached();
                                } else {
                                    // background color is white we need to remove a different color
                                    // black is the only other "reliable" color on the device
                                    Graphics imageG = i.getGraphics();
                                    imageG.setColor(0);
                                    imageG.fillRect(0, 0, width, height);
                                    imageG.setColor(g.getColor());
                                    imageG.fillRoundRect(0, 0 , width, height, arcWidth, arcHeight);
                                    imageRgb = i.getRGBCached();
                                }
                                int removeColor = imageRgb[0];
                                int size = width * height;
                                int alphaInt = ((s.getBgTransparency() & 0xff) << 24) & 0xff000000;
                                for(int iter = 0 ; iter < size ; iter++) {
                                    if(removeColor == imageRgb[iter]) {
                                            imageRgb[iter] = 0;
                                            continue;
                                    }
                                    if((imageRgb[iter] & 0xff000000) != 0) {
                                        imageRgb[iter] = (imageRgb[iter] & 0xffffff) | alphaInt;
                                    }   
                                }
                                g.drawImage(new RGBImage(imageRgb, width, height), x, y);
                            } 
                        }
                    }
                    
                    g.setColor(foreground);
                }
                break;
            case TYPE_IMAGE: {
                int clipX = g.getClipX();
                int clipY = g.getClipY();
                int clipWidth = g.getClipWidth();
                int clipHeight = g.getClipHeight();
                Image topLeft = images[4]; 
                Image topRight = images[5];
                Image bottomLeft = images[6];
                Image center = images[8];
                x += topLeft.getWidth();
                y += topLeft.getHeight();
                height -= (topLeft.getHeight() + bottomLeft.getHeight());
                width -= (topLeft.getWidth() + topRight.getWidth());
                g.clipRect(x, y, width, height);
                if(center != null){
                    g.tileImage(center, x, y, width, height);
                }
                Image top = images[0];  Image bottom = images[1];
                Image left = images[2]; Image right = images[3];
                Image bottomRight = images[7];
                
                g.setClip(clipX, clipY, clipWidth, clipHeight);
                
                x = xParameter;
                y = yParameter;
                width = widthParameter;
                height = heightParameter;
                
                g.drawImage(topLeft, x, y);
                g.drawImage(bottomLeft, x, y + height - bottomLeft.getHeight());
                g.drawImage(topRight, x + width - topRight.getWidth(), y);
                g.drawImage(bottomRight, x + width - bottomRight.getWidth(), y + height - bottomRight.getHeight());

                Image arrowDownImage = null;
                Image arrowUpImage = null;
                Image arrowLeftImage = null;
                Image arrowRightImage = null;
                int arrowPosition = 0;

                // we need to draw an arrow on one of the sides
                if(trackComponent != null) {
                    int cabsY = c.getAbsoluteY();
                    int trackY = trackComponent.getAbsoluteY();
                    int trackX = trackComponent.getAbsoluteX();
                    int cabsX = c.getAbsoluteX();
                    if(cabsY >= trackY + trackComponent.getHeight()) {
                        // we are bellow the component
                        arrowUpImage = specialTile[0];
                        arrowPosition = (trackX + trackComponent.getWidth() / 2) - cabsX - arrowUpImage.getWidth() / 2;
                    } else {    
                        if(cabsY + c.getHeight() <= trackY) {
                            // we are above the component
                            arrowDownImage = specialTile[1];
                            arrowPosition = (trackX + trackComponent.getWidth() / 2) - cabsX - arrowDownImage.getWidth() / 2;
                        }  else {
                            if(cabsX >= trackX + trackComponent.getWidth()) {
                                // we are to the right of the component
                                arrowLeftImage = specialTile[2];
                                arrowPosition = (trackY + trackComponent.getHeight() / 2) - cabsY - arrowLeftImage.getHeight() / 2;
                            } else {
                                if(cabsX + c.getWidth() <= trackX) {
                                    // we are to the left of the component
                                    arrowRightImage = specialTile[3];
                                    arrowPosition = (trackY + trackComponent.getHeight() / 2) - cabsY - arrowRightImage.getHeight() / 2;
                                }
                            }
                        }
                    }
                }

                g.setClip(clipX, clipY, clipWidth, clipHeight);
                drawImageBorderLine(g, topLeft, topRight, top, x, y, width, arrowUpImage, arrowPosition);
                g.setClip(clipX, clipY, clipWidth, clipHeight);
                drawImageBorderLine(g, bottomLeft, bottomRight, bottom, x, y + height - bottom.getHeight(), width, arrowDownImage, arrowPosition);
                g.setClip(clipX, clipY, clipWidth, clipHeight);
                drawImageBorderColumn(g, topLeft, bottomLeft, left, x, y, height, arrowLeftImage, arrowPosition);
                g.setClip(clipX, clipY, clipWidth, clipHeight);
                drawImageBorderColumn(g, topRight, bottomRight, right, x + width - right.getWidth(), y, height, arrowRightImage, arrowPosition);
                                
                g.setClip(clipX, clipY, clipWidth, clipHeight);
                break;
            }
            case TYPE_IMAGE_SCALED: {
                int clipX = g.getClipX();
                int clipY = g.getClipY();
                int clipWidth = g.getClipWidth();
                int clipHeight = g.getClipHeight();
                Image topLeft = images[4];
                Image topRight = images[5];
                Image bottomLeft = images[6];
                Image center = images[8];
                x += topLeft.getWidth();
                y += topLeft.getHeight();
                height -= (topLeft.getHeight() + bottomLeft.getHeight());
                width -= (topLeft.getWidth() + topRight.getWidth());
                g.clipRect(x, y, width, height);
                if(center != null && width > 0 && height > 0){
                    int centerWidth = center.getWidth();
                    int centerHeight = center.getHeight();
                    g.drawImage(center, x, y, width, height);
                }
                Image top = images[0];  Image bottom = images[1];
                Image left = images[2]; Image right = images[3];
                Image bottomRight = images[7];

                g.setClip(clipX, clipY, clipWidth, clipHeight);

                x = xParameter;
                y = yParameter;
                width = widthParameter;
                height = heightParameter;

                g.drawImage(topLeft, x, y);
                g.drawImage(bottomLeft, x, y + height - bottomLeft.getHeight());
                g.drawImage(topRight, x + width - topRight.getWidth(), y);
                g.drawImage(bottomRight, x + width - bottomRight.getWidth(), y + height - bottomRight.getHeight());

                drawImageBorderLineScale(g, topLeft, topRight, top, x, y, width);
                drawImageBorderLineScale(g, bottomLeft, bottomRight, bottom, x, y + height - bottom.getHeight(), width);
                drawImageBorderColumnScale(g, topLeft, bottomLeft, left, x, y, height);
                drawImageBorderColumnScale(g, topRight, bottomRight, right, x + width - right.getWidth(), y, height);

                g.setClip(clipX, clipY, clipWidth, clipHeight);
                break;
            }
            case TYPE_IMAGE_HORIZONTAL: {
                int clipX = g.getClipX();
                int clipY = g.getClipY();
                int clipWidth = g.getClipWidth();
                int clipHeight = g.getClipHeight();
                Image left = images[0];
                Image right = images[1];
                Image center = images[2];

                if(c.getUIManager().isThemeConstant("centerAlignHBorderBool", false)) {
                    y += Math.max(0, height / 2 - center.getHeight() / 2);
                }

                g.drawImage(left, x, y);
                g.drawImage(right, x + width - right.getWidth(), y);
                x += left.getWidth();
                width -= (left.getWidth() + right.getWidth());
                g.clipRect(x, y, width, height);
                int centerWidth = center.getWidth();
                for(int xCount = x ; xCount < x + width ; xCount += centerWidth) {
                    g.drawImage(center, xCount, y);
                }
                g.setClip(clipX, clipY, clipWidth, clipHeight);
                break;
            }
            case TYPE_IMAGE_VERTICAL: {
                int clipX = g.getClipX();
                int clipY = g.getClipY();
                int clipWidth = g.getClipWidth();
                int clipHeight = g.getClipHeight();
                Image top = images[0];
                Image bottom = images[1];
                Image center = images[2];
                g.drawImage(top, x, y);
                g.drawImage(bottom, x, y + height - bottom.getHeight());
                y += top.getHeight();
                height -= (top.getHeight() + bottom.getHeight());
                g.clipRect(x, y, width, height);
                int centerHeight = center.getHeight();
                for(int yCount = y ; yCount < y + height ; yCount += centerHeight) {
                    g.drawImage(center, x, yCount);
                }
                g.setClip(clipX, clipY, clipWidth, clipHeight);
                break;
            }
        }
        g.setColor(originalColor);
    }
    
    /**
     * Draws the border for the given component, this method is called before a call to
     * background painting is made.
     * 
     * @param g graphics context to draw onto
     * @param c component whose border should be drawn
     */
    public void paint(Graphics g, Component c) {
        int x = c.getX();
        int y = c.getY();
        int width = c.getWidth();
        int height = c.getHeight();
         if (outerBorder!=null) {
            if(paintOuterBorderFirst) {
                outerBorder.paint(g, x, y, width, height, c);
                paint(g, x+thickness, y+thickness, width-thickness*2, height-thickness*2, c);
            } else {
                paint(g, x+thickness, y+thickness, width-thickness*2, height-thickness*2, c);
                outerBorder.paint(g, x, y, width, height, c);
            }
        } else {
            paint(g, x, y, width, height, c);
        }
    }
    
    void paint(Graphics g,int x,int y,int width,int height,Component c) {
        int originalColor = g.getColor();
        if(!themeColors) {
            g.setColor(colorA);
        } 
        switch(type) {
            case TYPE_LINE:
                if (borderTitle==null) {
                    width--;
                    height--;
                    for(int iter = 0 ; iter < thickness ; iter++) {
                        g.drawRect(x + iter, y + iter, width, height);
                        width -= 2; height -= 2;
                    }
                } else {
                    Font f=c.getStyle().getFont();
                    int titleW=f.stringWidth(borderTitle);
                    int topPad=c.getStyle().getPadding(Component.TOP);
                    int topY=y+(topPad-thickness)/2;
                    if (c.isRTL()) {
                        g.fillRect(x+width-TITLE_MARGIN, topY, TITLE_MARGIN , thickness); //top (segment before the title)
                        g.fillRect(x, topY, width-(TITLE_MARGIN +titleW+TITLE_SPACE*2), thickness); //top (segment after the title)
                        g.drawString(borderTitle, x+width-(TITLE_MARGIN +titleW+TITLE_SPACE), y+(topPad-f.getHeight())/2);
                    } else {
                        g.fillRect(x, topY, TITLE_MARGIN , thickness); //top (segment before the title)
                        g.fillRect(x+TITLE_MARGIN +titleW+TITLE_SPACE*2, topY, width-(TITLE_MARGIN +titleW+TITLE_SPACE*2), thickness); //top (segment after the title)
                        g.drawString(borderTitle, x+TITLE_MARGIN+TITLE_SPACE, y+(topPad-f.getHeight())/2);
                    }

                    g.fillRect(x, y+height-thickness, width, thickness); //bottom
                    g.fillRect(x, topY, thickness, height); //left
                    g.fillRect(x+width-thickness, topY, thickness, height); //right
                    
                }
                break;
            case TYPE_DASHED:
            case TYPE_DOTTED:
                int segWidth=thickness;
                if (type==TYPE_DASHED) {
                    segWidth=thickness*3;
                }
                int ix=x;
                for (;ix<x+width;ix+=segWidth*2) {
                    g.fillRect(ix, y, segWidth, thickness);
                    g.fillRect(ix, y+height-thickness, segWidth, thickness);
                }
                if (ix-segWidth<x+width) { //fill in the gap if any
                    g.fillRect(ix-segWidth, y, x+width-ix+segWidth, thickness);
                    g.fillRect(ix-segWidth, y+height-thickness, x+width-ix+segWidth, thickness);
                }

                int iy=y;
                for (;iy<y+height;iy+=segWidth*2) {
                    g.fillRect(x, iy, thickness, segWidth);
                    g.fillRect(x+width-thickness, iy, thickness, segWidth);
                }
                if (iy-segWidth<y+height) { //fill in the gap if any
                    g.fillRect(x, iy-segWidth, thickness, y+height-iy+segWidth);
                    g.fillRect(x+width-thickness, iy-segWidth, thickness, y+height-iy+segWidth);
                }


                break;
            case TYPE_DOUBLE:
                    width--;
                    height--;
                    for(int iter = 0 ; iter < thickness ; iter++) {
                        if ((iter*100/thickness<=33) || (iter*100/thickness>=66)) {
                            g.drawRect(x + iter, y + iter, width, height);
                        }
                        width -= 2; height -= 2;
                   }
                    break;
            case TYPE_INSET:
            case TYPE_OUTSET:
                for(int i=0;i<thickness;i++) {
                    g.drawLine(x+i, y+i, x+i, y+height-i);
                    g.drawLine(x+i, y+i, x+width-i, y+i);
                }

                if (type==TYPE_INSET) {
                    g.lighterColor(50);
                } else {
                    g.darkerColor(50);
                }
                for(int i=0;i<thickness;i++) {
                    g.drawLine(x+i, y+height-i, x+width-i, y+height-i);
                    g.drawLine(x+width-i, y+i, x+width-i, y+height-i);
                }
                break;
            case TYPE_GROOVE:
            case TYPE_RIDGE:
                for(int i=0;i<thickness/2;i++) {
                    g.drawLine(x+i, y+i, x+i, y+height-i);
                    g.drawLine(x+i, y+i, x+width-i, y+i);
                }
                for(int i=thickness/2;i<thickness;i++) {
                    g.drawLine(x+i, y+height-i, x+width-i, y+height-i);
                    g.drawLine(x+width-i, y+i, x+width-i, y+height-i);
                }

                if (type==TYPE_GROOVE) {
                    g.lighterColor(50);
                } else {
                    g.darkerColor(50);
                }
                for(int i=0;i<thickness/2;i++) {
                    g.drawLine(x+i, y+height-i, x+width-i, y+height-i);
                    g.drawLine(x+width-i, y+i, x+width-i, y+height-i);
                }
                for(int i=thickness/2;i<thickness;i++) {
                    g.drawLine(x+i, y+i, x+i, y+height-i);
                    g.drawLine(x+i, y+i, x+width-i, y+i);
                }
                break;
            case TYPE_ROUNDED_PRESSED:
                x++;
                y++;
                width -= 2;
                height -= 2;
            case TYPE_ROUNDED:
                width--;
                height--;

                if(outline) {
                    g.drawRoundRect(x, y , width, height, arcWidth, arcHeight);
                }
                break;
            case TYPE_ETCHED_LOWERED:
            case TYPE_ETCHED_RAISED:
                g.drawRect(x, y, width - 2, height - 2);

                if(themeColors) {
                    if(type == TYPE_ETCHED_LOWERED) {
                        g.lighterColor(40);
                    } else {
                        g.darkerColor(40);
                    }
                } else {
                    g.setColor(colorB);
                }
                g.drawLine(x + 1, y + height - 3, x + 1, y +1);
                g.drawLine(x + 1, y + 1, x + width - 3, y + 1);

                g.drawLine(x, y + height - 1, x + width - 1, y + height - 1);
                g.drawLine(x + width - 1, y + height - 1, x + width - 1, y);
                break;
            case TYPE_BEVEL_RAISED:
                if(themeColors) {
                    g.setColor(getBackgroundColor(c));
                    g.lighterColor(50);
                } else {
                    g.setColor(colorA);
                }
                g.drawLine(x, y, x, y + height - 2);
                g.drawLine(x + 1, y, x + width - 2, y);

                if(themeColors) {
                    g.darkerColor(25);
                } else {
                    g.setColor(colorB);
                }
                g.drawLine(x + 1, y + 1, x + 1, y + height - 3);
                g.drawLine(x + 2, y + 1, x + width - 3, y + 1);

                if(themeColors) {
                    g.darkerColor(50);
                } else {
                    g.setColor(colorC);
                }
                g.drawLine(x, y + height - 1, x + width - 1, y + height - 1);
                g.drawLine(x + width - 1, y, x + width - 1, y + height - 2);

                if(themeColors) {
                    g.darkerColor(25);
                } else {
                    g.setColor(colorD);
                }
                g.drawLine(x + 1, y + height - 2, x + width - 2, y + height - 2);
                g.drawLine(x + width - 2, y + 1, x + width - 2, y + height - 3);
                break;
            case TYPE_BEVEL_LOWERED: 
                if(themeColors) {
                    g.setColor(getBackgroundColor(c));
                    g.darkerColor(50);
                } else {
                    g.setColor(colorD);
                }
                g.drawLine(x, y, x, y + height - 1);
                g.drawLine(x + 1, y, x + width - 1, y);

                if(themeColors) {
                    g.lighterColor(25);
                } else {
                    g.setColor(colorC);
                }
                g.drawLine(x + 1, y + 1, x + 1, y + height - 2);
                g.drawLine(x + 2, y + 1, x + width - 2, y + 1);

                if(themeColors) {
                    g.lighterColor(50);
                } else {
                    g.setColor(colorC);
                }
                g.drawLine(x + 1, y + height - 1, x + width - 1, y + height - 1);
                g.drawLine(x + width - 1, y + 1, x + width - 1, y + height - 2);

                if(themeColors) {
                    g.lighterColor(25);
                } else {
                    g.setColor(colorA);
                }
                g.drawLine(x + 2, y + height - 2, x + width - 2, y + height - 2);
                g.drawLine(x + width - 2, y + 2, x + width - 2, y + height - 3);
                break;
            case TYPE_COMPOUND:
                Style style = c.getStyle();
                boolean drawLeft = true;
                boolean drawRight = true;

                if (c.getUIManager().getLookAndFeel().isRTL()) {
                    boolean temp = drawLeft;
                    drawLeft = drawRight;
                    drawRight = temp;
                }
                Border top = compoundBorders[Component.TOP];
                Border bottom = compoundBorders[Component.BOTTOM];
                Border left = compoundBorders[Component.LEFT];
                Border right = compoundBorders[Component.RIGHT];
                int topThickness = 0;
                int bottomThickness = 0;

                if (top != null) {
                    Rectangle clip = saveClip(g);
                    topThickness = top.thickness;
                    g.clipRect(x, y, width, topThickness);
                    top.paint(g, x, y, width, height, c); //top.paint(g, c);
                    restoreClip(g, clip);
                }

                if (bottom != null) {
                    Rectangle clip = saveClip(g);
                    bottomThickness = bottom.thickness;
                    g.clipRect(x, y + height - bottomThickness, width, bottomThickness);
                    bottom.paint(g, x, y, width, height, c); //bottom.paint(g, c);
                    restoreClip(g, clip);
                }

                if ((drawLeft) && (left != null)) {
                    Rectangle clip = saveClip(g);
                    g.clipRect(x, y + topThickness,
                            left.thickness,
                            height - topThickness - bottomThickness);
                    left.paint(g, x, y, width, height, c); //left.paint(g, c);
                    restoreClip(g, clip);
                }
                if ((drawRight) && (right != null)) {
                    Rectangle clip = saveClip(g);
                    g.clipRect(x + width - right.thickness,
                            y + topThickness,
                            right.thickness,
                            height - topThickness - bottomThickness);
                    right.paint(g, x, y, width, height, c); //right.paint(g, c);
                    restoreClip(g, clip);
                }
                break;
            case TYPE_IMAGE:
            case TYPE_IMAGE_SCALED:
            case TYPE_IMAGE_HORIZONTAL:
            case TYPE_IMAGE_VERTICAL:
                break;
        }
        g.setColor(originalColor);
    }

    /**
     * Utility method used to save the current clip area
     *
     * @param g The graphics to obtain the clip area from
     * @return A Rectangle object representing the current clip area
     */
    private Rectangle saveClip(Graphics g) {
        return new Rectangle(g.getClipX(), g.getClipY(), g.getClipWidth(), g.getClipHeight());
    }

    /**
     * Utility method used to restore a previously saved clip area
     *
     * @param g The graphics to apply the clip area on
     * @param rect A Rectangle representing the saved clip area
     */
    private void restoreClip(Graphics g,Rectangle rect) {
        g.setClip(rect.getX(), rect.getY(), rect.getSize().getWidth(), rect.getSize().getHeight());
    }

    private int getBackgroundColor(Component c) {
        return c.getStyle().getBgColor();
    }
    
    private void drawImageBorderLine(Graphics g, Image left, Image right, Image center, final int x, int y, int width, Image arrow, int imagePosition) {
        int currentWidth = width - right.getWidth();
        if(currentWidth > 0) {
            int currentX = x;
            currentX += left.getWidth();
            g.clipRect(currentX, y, currentWidth - left.getWidth(), center.getHeight());
            if(arrow != null) {
                int destX = currentX + currentWidth;
                int centerWidth = center.getWidth();
                for(; currentX < destX ; currentX += centerWidth) {
                    g.drawImage(center, currentX, y);
                }
                imagePosition = Math.max(imagePosition, left.getWidth());
                imagePosition = Math.min(imagePosition, destX - x - arrow.getWidth() - right.getWidth());
                g.drawImage(arrow, x + imagePosition, y);
            } else {
                g.tileImage(center, currentX, y, currentWidth, center.getHeight());
            }
        }
    }

    private void drawImageBorderColumn(Graphics g, Image top, Image bottom, Image center, int x, final int y, int height, Image arrow, int imagePosition) {
        int currentHeight = height - bottom.getHeight();
        if(currentHeight > 0) {
            int currentY = y + top.getHeight();
            g.clipRect(x, currentY, center.getWidth(), currentHeight - top.getHeight());
            if(arrow != null) {
                int destY = currentY + currentHeight;
                int centerHeight = center.getHeight();
                for(; currentY < destY ; currentY += centerHeight) {
                    g.drawImage(center, x, currentY);
                }
                imagePosition = Math.max(imagePosition, top.getHeight());
                imagePosition = Math.min(imagePosition, destY - y - arrow.getHeight() - bottom.getHeight());
                g.drawImage(arrow, x, y + imagePosition);
            } else {
                g.tileImage(center, x, currentY, center.getWidth(), currentHeight);
            }
        }
    }

    private void drawImageBorderLineScale(Graphics g, Image left, Image right, Image center, int x, int y, int width) {
        int currentWidth = width - right.getWidth() - left.getWidth();
        if(currentWidth > 0) {
            x += left.getWidth();
            g.drawImage(center, x, y, currentWidth, center.getHeight());
        }
    }

    private void drawImageBorderColumnScale(Graphics g, Image top, Image bottom, Image center, int x, int y, int height) {
        int currentHeight = height - bottom.getHeight() - top.getHeight();
        if(currentHeight > 0) {
            y += top.getHeight();
            g.drawImage(center, x, y, center.getWidth(), currentHeight);
        }
    }
    
    /**
     * Sets the default border to the given value
     * 
     * @param border new border value
     */
    public static void setDefaultBorder(Border border) {
        defaultBorder = border;
    }

    /**
     * Gets the default border to the given value
     * 
     * @return the default border
     */
    public static Border getDefaultBorder() {
        return defaultBorder;
    }

    /**
     * This method returns how thick is the border in pixels, notice this doesn't apply to most border types
     * @return the Border thickness
     */
    public int getThickness() {
        return thickness;
    }

    /**
     * This method returns sets the border thickness in pixels, notice this doesn't apply to most border types
     * @param thickness  the Border thickness
     */
    public void setThickness(int thickness) {
        this.thickness = thickness;
    }

    /**
     * Allows toggling the order in which the outer and inner borders are painted for the Outer border type
     * @param paintOuterBorderFirst whether the outside border should be painter first
     */
    public void setPaintOuterBorderFirst(boolean paintOuterBorderFirst) {
        this.paintOuterBorderFirst = paintOuterBorderFirst;
    }

    /**
     * Allows toggling the order in which the outer and inner borders are painted for the Outer border type
     * @return whether the outside border should be painter first
     */
    public boolean isPaintOuterBorderFirst() {
        return paintOuterBorderFirst;
    }

    /**
     * This method returns the Compound Borders array.
     * The array size is 4 and the borders arranged as follows :
     * Border[] borders = getCompoundBorders();
     * Border top = borders[Component.TOP];
     * Border bottom = borders[Component.BOTTOM];
     * Border left = borders[Component.LEFT];
     * Border right = borders[Component.RIGHT];
     *
     * @return the borders array or null if this is not a Compound Border
     */
    public Border[] getCompoundBorders() {
        return compoundBorders;
    }


    /**
     * This callback indicates that a component pointing at this border is initialized, this
     * method is useful for image borders whose lock methods are implicitly invoked.
     * This method may be invoked multiple times.
     */
    public void lock() {
        if(images != null) {
            for(int iter = 0 ; iter < images.length ; iter++) {
                if(images[iter] != null) {
                    images[iter].lock();
                }
            }
        }
    }

    /**
     * This callback indicates that a component pointing at this border is now deinitilized
     * This method may be invoked multiple times.
     */
    public void unlock() {
        if(images != null) {
            for(int iter = 0 ; iter < images.length ; iter++) {
                if(images[iter] != null) {
                    images[iter].unlock();
                }
            }
        }
    }    
}
