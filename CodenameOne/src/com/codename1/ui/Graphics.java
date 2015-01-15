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

import com.codename1.ui.geom.GeneralPath;
import com.codename1.impl.CodenameOneImplementation;
import com.codename1.ui.geom.Shape;
import com.codename1.ui.plaf.Style;

/**
 * Abstracts the underlying platform graphics context thus allowing us to achieve
 * portability between MIDP devices and CDC devices. This abstaction simplifies
 * and unifies the Graphics implementations of various platforms.
 * 
 * <p>A graphics instance should never be created by the developer and is always accessed
 * using either a paint callback or a mutable image. There is no supported  way to create this
 * object directly.
 */
public final class Graphics {
    private int xTranslate;
    private int yTranslate;
    private int color;
    private Font current = Font.getDefaultFont();

    private CodenameOneImplementation impl;
    private Object nativeGraphics;

    private Object[] nativeGraphicsState;
    private float scaleX = 1, scaleY = 1;
    
    
    /**
     * Constructing new graphics with a given javax.microedition.lcdui.Graphics 
     * @param g an implementation dependent native graphics instance
     */
    Graphics(Object nativeGraphics) {
        setGraphics(nativeGraphics);
        impl = Display.getInstance().getImplementation();
    }

    /**
     * Setting graphics with a given javax.microedition.lcdui.Graphics
     * 
     * @param g a given javax.microedition.lcdui.Graphics
     */
    void setGraphics(Object g) {
        this.nativeGraphics = g;
    }

    /**
     * Returns the underlying native graphics object
     * 
     * @return the underlying native graphics object
     */
    Object getGraphics() {
        return nativeGraphics;
    }

    /**
     * Translates the X/Y location for drawing on the underlying surface. Translation
     * is incremental so the new value will be added to the current translation and
     * in order to reset translation we have to invoke 
     * {@code translate(-getTranslateX(), -getTranslateY()) }
     * 
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public void translate(int x, int y) {
        if(impl.isTranslationSupported()) {
            impl.translate(nativeGraphics, x, y);
        } else {
            xTranslate += x;
            yTranslate += y;
        }
    }

    /**
     * Returns the current x translate value 
     * 
     * @return the current x translate value 
     */
    public int getTranslateX() {
        if(impl.isTranslationSupported()) {
            return impl.getTranslateX(nativeGraphics);
        } else {
            return xTranslate;
        }
    }

    /**
     * Returns the current y translate value 
     * 
     * @return the current y translate value 
     */
    public int getTranslateY() {
        if(impl.isTranslationSupported()) {
            return impl.getTranslateY(nativeGraphics);
        } else {
            return yTranslate;
        }
    }

    /**
     * Returns the current color
     * 
     * @return the RGB graphics color 
     */
    public int getColor() {
        return color;
    }

    /**
     * Sets the current rgb color while ignoring any potential alpha component within
     * said color value.
     * 
     * @param RGB the RGB value for the color.
     */
    public void setColor(int RGB) {
        color = 0xffffff & RGB;
        impl.setColor(nativeGraphics, color);
    }

    /**
     * Returns the font used with the drawString method calls 
     * 
     * @return the font used with the drawString method calls
     */
    public Font getFont() {
        return current;
    }

    /**
     * Sets the font to use with the drawString method calls 
     * 
     * @param font the font used with the drawString method calls
     */
    public void setFont(Font font) {
        this.current = font;
        if(!(font instanceof CustomFont)) {
            impl.setNativeFont(nativeGraphics, font.getNativeFont());
        }
    }

    /**
     * Returns the x clipping position
     * 
     * @return the x clipping position
     */
    public int getClipX() {
        return impl.getClipX(nativeGraphics) - xTranslate;
    }

    /**
     * Returns the clip as an x,y,w,h array
     * @return clip array copy
     */
    public int[] getClip()  {
        return new int[] {getClipX(), getClipY(), getClipWidth(), getClipHeight()};
    }

    /**
     * Sets the clip from an array containing x, y, width, height value
     * @param clip 4 element array
     */
    public void setClip(int[] clip) {
        setClip(clip[0], clip[1], clip[2], clip[3]);
    }

    /**
     * Returns the y clipping position
     * 
     * @return the y clipping position
     */
    public int getClipY() {
        return impl.getClipY(nativeGraphics) - yTranslate;
    }

    /**
     * Returns the clip width
     * 
     * @return the clip width
     */
    public int getClipWidth() {
        return impl.getClipWidth(nativeGraphics);
    }

    /**
     * Returns the clip height
     * 
     * @return the clip height
     */
    public int getClipHeight() {
        return impl.getClipHeight(nativeGraphics);
    }

    /**
     * Clips the given rectangle by intersecting with the current clipping region, this
     * method can thus only shrink the clipping region and never increase it.
     * 
     * @param x the x coordinate of the rectangle to intersect the clip with
     * @param y the y coordinate of the rectangle to intersect the clip with
     * @param width the width of the rectangle to intersect the clip with
     * @param height the height of the rectangle to intersect the clip with
     */
    public void clipRect(int x, int y, int width, int height) {
        impl.clipRect(nativeGraphics, xTranslate + x, yTranslate + y, width, height);
    }

    /**
     * Updates the clipping region to match the given region exactly
     * 
     * @param x the x coordinate of the new clip rectangle.
     * @param y the y coordinate of the new clip rectangle.
     * @param width the width of the new clip rectangle.
     * @param height the height of the new clip rectangle.
     */
    public void setClip(int x, int y, int width, int height) {
        impl.setClip(nativeGraphics, xTranslate + x, yTranslate + y, width, height);
    }
    
    /**
     * Pushes the current clip onto the clip stack.  It can later be restored 
     * using {@link #popClip}.
     */
    public void pushClip(){
        impl.pushClip(nativeGraphics);
    }
    
    /**
     * Pops the top clip from the clip stack and sets it as the current clip.
     */
    public void popClip(){
        impl.popClip(nativeGraphics);
    }

    /**
     * Draws a line between the 2 X/Y coordinates
     * 
     * @param x1 first x position
     * @param y1 first y position
     * @param x2 second x position
     * @param y2 second y position
     */
    public void drawLine(int x1, int y1, int x2, int y2) {
        impl.drawLine(nativeGraphics, xTranslate + x1, yTranslate + y1, xTranslate + x2, yTranslate + y2);
    }

    /**
     * Fills the rectangle from the given position according to the width/height
     * minus 1 pixel according to the convention in Java.
     * 
     * @param x the x coordinate of the rectangle to be filled.
     * @param y the y coordinate of the rectangle to be filled.
     * @param width the width of the rectangle to be filled.
     * @param height the height of the rectangle to be filled.
     */
    public void fillRect(int x, int y, int width, int height) {
        impl.fillRect(nativeGraphics, xTranslate + x, yTranslate + y, width, height);
    }

    /**
     * Draws a rectangle in the given coordinates
     * 
     * @param x the x coordinate of the rectangle to be drawn.
     * @param y the y coordinate of the rectangle to be drawn.
     * @param width the width of the rectangle to be drawn.
     * @param height the height of the rectangle to be drawn.
     */
    public void drawRect(int x, int y, int width, int height) {
        impl.drawRect(nativeGraphics, xTranslate + x, yTranslate + y, width, height);
    }

    /**
     * Draws a rectangle in the given coordinates with the given thickness
     * 
     * @param x the x coordinate of the rectangle to be drawn.
     * @param y the y coordinate of the rectangle to be drawn.
     * @param width the width of the rectangle to be drawn.
     * @param height the height of the rectangle to be drawn.
     * @param thickness the thickness in pixels
     */
    public void drawRect(int x, int y, int width, int height, int thickness) {
        impl.drawRect(nativeGraphics, xTranslate + x, yTranslate + y, width, height, thickness);
    }
    
    /**
     * Draws a rounded corner rectangle in the given coordinates with the arcWidth/height
     * matching the last two arguments respectively.
     * 
     * @param x the x coordinate of the rectangle to be drawn.
     * @param y the y coordinate of the rectangle to be drawn.
     * @param width the width of the rectangle to be drawn.
     * @param height the height of the rectangle to be drawn.
     * @param arcWidth the horizontal diameter of the arc at the four corners.
     * @param arcHeight the vertical diameter of the arc at the four corners.
     */
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        impl.drawRoundRect(nativeGraphics, xTranslate + x, yTranslate + y, width, height, arcWidth, arcHeight);
    }

    /**
     * Makes the current color slightly lighter, this is useful for many visual effects
     * 
     * @param factor the degree of lightening a color per channel a number from 1 to 255
     */
    public void lighterColor(int factor) {
        int color = getColor();
        int r = color >> 16 & 0xff;
        int g = color >> 8 & 0xff;
        int b = color & 0xff;
        r = Math.min(0xff, r + factor);
        g = Math.min(0xff, g + factor);
        b = Math.min(0xff, b + factor);
        setColor(((r << 16) & 0xff0000) | ((g << 8) & 0xff00) | (b & 0xff));
    }

    /**
     * Makes the current color slightly darker, this is useful for many visual effects
     * 
     * @param factor the degree of lightening a color per channel a number from 1 to 255
     */
    public void darkerColor(int factor) {
        int color = getColor();
        int r = color >> 16 & 0xff;
        int g = color >> 8 & 0xff;
        int b = color & 0xff;
        r = Math.max(0, r - factor);
        g = Math.max(0, g - factor);
        b = Math.max(0, b - factor);
        setColor(((r << 16) & 0xff0000) | ((g << 8) & 0xff00) | (b & 0xff));
    }

    /**
     * Fills a rounded rectangle in the same way as drawRoundRect
     * 
     * @param x the x coordinate of the rectangle to be filled.
     * @param y the y coordinate of the rectangle to be filled.
     * @param width the width of the rectangle to be filled.
     * @param height the height of the rectangle to be filled.
     * @param arcWidth the horizontal diameter of the arc at the four corners.
     * @param arcHeight the vertical diameter of the arc at the four corners.
     * @see #drawRoundRect
     */
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        impl.fillRoundRect(nativeGraphics, xTranslate + x, yTranslate + y, width, height, arcWidth, arcHeight);
    }

    /**
     * Fills a circular or elliptical arc based on the given angles and bounding 
     * box. The resulting arc begins at startAngle and extends for arcAngle 
     * degrees.
     * 
     * @param x the x coordinate of the upper-left corner of the arc to be filled.
     * @param y the y coordinate of the upper-left corner of the arc to be filled.
     * @param width the width of the arc to be filled.
     * @param height the height of the arc to be filled.
     * @param startAngle the beginning angle.
     * @param arcAngle the angular extent of the arc, relative to the start angle.
     */
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        impl.fillArc(nativeGraphics, xTranslate + x, yTranslate + y, width, height, startAngle, arcAngle);
    }

    /**
     * Draws a circular or elliptical arc based on the given angles and bounding 
     * box
     * 
     * @param x the x coordinate of the upper-left corner of the arc to be drawn.
     * @param y the y coordinate of the upper-left corner of the arc to be drawn.
     * @param width the width of the arc to be drawn.
     * @param height the height of the arc to be drawn.
     * @param startAngle the beginning angle.
     * @param arcAngle the angular extent of the arc, relative to the start angle.
     */
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        impl.drawArc(nativeGraphics, xTranslate + x, yTranslate + y, width, height, startAngle, arcAngle);
    }

    private void drawStringImpl(String str, int x, int y) {
        // remove a commonly used trick to create a spacer label from the paint queue
        if(str.length() == 0 || str == " ") {
            return;
        }
        if(!(current instanceof CustomFont)) {
            impl.drawString(nativeGraphics, str, x + xTranslate, y + yTranslate);
        } else {
            current.drawString(this, str, x, y);
        }
    }

    public void drawString(String str, int x, int y,int textDecoration){
        drawString(str, x, y, textDecoration, Display.TEXT_VALIGN_TOP);
    }
    
    /**
     * Draw a string using the current font and color in the x,y coordinates. The font is drawn
     * from the top position and not the baseline.
     * 
     * @param str the string to be drawn.
     * @param x the x coordinate.
     * @param y the y coordinate.
     * @param textDecoration Text decoration bitmask (See Style's TEXT_DECORATION_* constants)
     */
    public void drawString(String str, int x, int y,int textDecoration, int valign) {
        if(str.length() == 0) {
            return;
        }
        if ( !Display.getInstance().getImplementation().isBaselineTextSupported() && valign == Display.TEXT_VALIGN_BASELINE ){
            throw new RuntimeException("Baseline Text is not supported in this port");
        }
        int ascentShift = valign == Display.TEXT_VALIGN_BASELINE ? -getFont().getAscent() : 0;
        
        // this if has only the minor effect of providing a slighly faster execution path
        if(textDecoration != 0) {
            boolean raised = (textDecoration & Style.TEXT_DECORATION_3D)!=0;
            boolean lowerd = (textDecoration & Style.TEXT_DECORATION_3D_LOWERED)!=0;
            boolean north = (textDecoration & Style.TEXT_DECORATION_3D_SHADOW_NORTH)!=0;
            if (raised || lowerd || north) {
                textDecoration = textDecoration & (~Style.TEXT_DECORATION_3D) & (~Style.TEXT_DECORATION_3D_LOWERED) & (~Style.TEXT_DECORATION_3D_SHADOW_NORTH);
                int c = getColor();
                int a = getAlpha();
                int newColor = 0;
                int offset = -2;
                if(lowerd) {
                    offset  = 2;
                    newColor = 0xffffff;
                } else {
                    if(north) {
                        offset  = 2;
                    }
                }
                setColor(newColor);
                if(a == 0xff) {
                    setAlpha(140);
                }
                drawString(str, x, y + offset + ascentShift, textDecoration);
                setAlpha(a);
                setColor(c);
                drawString(str, x, y + ascentShift, textDecoration);
                return;
            }
            drawStringImpl(str, x, y);
            if ((textDecoration & Style.TEXT_DECORATION_UNDERLINE)!=0) {
                drawLine(x, y+current.getHeight()-1, x+current.stringWidth(str), y+current.getHeight()-1);
            }
            if ((textDecoration & Style.TEXT_DECORATION_STRIKETHRU)!=0) {
                drawLine(x, y+current.getHeight()/2, x+current.stringWidth(str), y+current.getHeight()/2);
            }
            if ((textDecoration & Style.TEXT_DECORATION_OVERLINE)!=0) {
                drawLine(x, y, x+current.stringWidth(str), y);
            }
        } else {
            drawStringImpl(str, x, y + ascentShift);
        }
    }

    /**
     * Draw a string using the current font and color in the x,y coordinates. The font is drawn
     * from the top position and not the baseline.
     *
     * @param str the string to be drawn.
     * @param x the x coordinate.
     * @param y the y coordinate.
     */
    public void drawString(String str, int x, int y) {
        drawString(str, x, y, 0);
    }

    /**
     * Draw the given char using the current font and color in the x,y 
     * coordinates. The font is drawn from the top position and not the 
     * baseline.
     * 
     * @param character - the character to be drawn
     * @param x the x coordinate of the baseline of the text
     * @param y the y coordinate of the baseline of the text
     */
    public void drawChar(char character, int x, int y) {
        drawString("" + character, x, y);
    }

    /**
     * Draw the given char array using the current font and color in the x,y coordinates. The font is drawn
     * from the top position and not the baseline.
     * 
     * @param data the array of characters to be drawn
     * @param offset the start offset in the data
     * @param length the number of characters to be drawn
     * @param x the x coordinate of the baseline of the text
     * @param y the y coordinate of the baseline of the text
     */
    public void drawChars(char[] data, int offset, int length, int x, int y) {
        if(!(current instanceof CustomFont)) {
            drawString(new String(data, offset, length), x, y);
        } else {
            CustomFont f = (CustomFont)current;
            f.drawChars(this, data, offset, length, x, y);
        }
    }

    /**
     * Draws the image so its top left coordinate corresponds to x/y
     * 
     * @param img the specified image to be drawn. This method does 
     * nothing if img is null.
     * @param x the x coordinate.
     * @param y the y coordinate.
     */
    public void drawImage(Image img, int x, int y) {
        img.drawImage(this, nativeGraphics, x, y);
    }
    
    /**
     * Draws the image so its top left coordinate corresponds to x/y and scales it to width/height
     *
     * @param img the specified image to be drawn. This method does
     * nothing if img is null.
     * @param x the x coordinate.
     * @param y the y coordinate.
     * @param w the width to occupy
     * @param h the height to occupy
     */
    public void drawImage(Image img, int x, int y, int w ,int h) {
        if(impl.isScaledImageDrawingSupported()) {
            img.drawImage(this, nativeGraphics, x, y, w, h);
        } else {
            drawImage(img.scaled(w, h), x, y);
        }
    }

    void drawImageWH(Object nativeImage, int x, int y, int w ,int h) {
        impl.drawImage(nativeGraphics, nativeImage, x + xTranslate, y + yTranslate, w, h);
    }

    void drawImage(Object img, int x, int y) {
        impl.drawImage(nativeGraphics, img, x + xTranslate, y + yTranslate);
    }

    /**
     * Draws an image with a MIDP trasnform for fast rotation
     */
    void drawImage(Object img, int x, int y, int transform) {
        if (transform != 0) {
            impl.drawImageRotated(nativeGraphics, img, x + xTranslate, y + yTranslate, transform);
        } else {
            drawImage(img, x, y);
        }
    }
    
    
    //--------------------------------------------------------------------------
    // START SHAPE DRAWING STUFF
    //--------------------------------------------------------------------------
    
 
    /**
     * Draws a outline shape inside the specified bounding box.  The bounding box will resize the shape to fit in its dimensions.
     * <p>This is not supported on
     * all platforms and contexts currently.  Use {@link #isShapeSupported} to check if the current 
     * context supports drawing shapes.</p>
     * @param shape The shape to be drawn.
     * 
     * @see #setStroke
     * @see #isShapeSupported
     */
    public void drawShape(Shape shape, Stroke stroke){
        if ( isShapeSupported()){
            if ( xTranslate != 0 || yTranslate != 0 ){
                GeneralPath p = new GeneralPath();
                Transform t = Transform.makeTranslation(xTranslate, yTranslate, 0);
                p.append(shape.getPathIterator(t), true);
                shape = p;
            }
            impl.drawShape(nativeGraphics, shape, stroke);
        }
       
    }
    
    /**
     * Fills the given shape using the current alpha and color settings.
     *  <p>This is not supported on
     * all platforms and contexts currently.  Use {@link #isShapeSupported} to check if the current 
     * context supports drawing shapes.</p>
     * @param shape The shape to be filled.
     * 
     * @see #isShapeSupported
     */
    public void fillShape(Shape shape){
        
        if ( isShapeSupported() ){
            if ( xTranslate != 0 || yTranslate != 0 ){
                GeneralPath p = new GeneralPath();
                Transform t = Transform.makeTranslation(xTranslate, yTranslate, 0);
                p.append(shape.getPathIterator(t), true);
                shape = p;
            }
        
            impl.fillShape(nativeGraphics, shape);
        }
    }
    
    /**
     * Checks to see if {@link com.codename1.ui.geom.Matrix} transforms are supported by this graphics context.
     * @return {@literal true} if this graphics context supports {@link com.codename1.ui.geom.Matrix} transforms. 
     * <p>Note that this method only confirms that 2D transforms are supported.  If you need to perform 3D 
     * transformations, you should use the {@link #isPerspectiveTransformSupported} method.</p>
     * @see #setTransform
     * @see #getTransform
     * @see #isPerspectiveTransformSupported
     */
    public boolean isTransformSupported(){
        return impl.isTransformSupported(nativeGraphics);
    }
    
    /**
     * Checks to see if perspective (3D) {@link com.codename1.ui.geom.Matrix} transforms are supported by this graphics
     * context.  If 3D transforms are supported, you can use a 4x4 transformation {@link com.codename1.ui.geom.Matrix}
     * via {@link #setTransform} to perform 3D transforms.
     * 
     * <p>Note: It is possible for 3D transforms to not be supported but Affine (2D) 
     * transforms to be supported.  In this case you would be limited to a 3x3 transformation
     * matrix in {@link #setTransform}.  You can check for 2D transformation support using the {@link #isTransformSupported} method.</p>
     * 
     * @return {@literal true} if Perspective (3D) transforms are supported.  {@literal false} otherwise.
     * @see #isTransformSupported
     * @see #setTransform
     * @see #getTransform
     */
    public boolean isPerspectiveTransformSupported(){
        return impl.isPerspectiveTransformSupported(nativeGraphics);
    }
    
    /**
     * Checks to see if this graphics context supports drawing shapes (i.e. {@link #drawShape}
     * and {@link #fillShape} methods. If this returns {@literal false}, and you call {@link #drawShape} or {@link #fillShape}, then
     * nothing will be drawn.
     * @return {@literal true} If {@link #drawShape} and {@link #fillShape} are supported.  
     * @see #drawShape
     * @see #fillShape
     */
    public boolean isShapeSupported(){
        return impl.isShapeSupported(nativeGraphics);
    }
    
    
    
    /**
     * Sets the transformation {@link com.codename1.ui.geom.Matrix} to apply to drawing in this graphics context.
     * In order to use this for 2D/Affine transformations you should first check to 
     * make sure that transforms are supported by calling the {@link #isTransformSupported}
     * method.  For 3D/Perspective transformations, you should first check to
     * make sure that 3D/Perspective transformations are supported by calling the 
     * {@link #isPerspectiveTransformSupported}.
     * 
     * <p>Transformations are applied with {@literal (0,0)} as the origin.  So rotations and
     * scales are anchored at this point on the screen.  You can use a different
     * anchor point by either embedding it in the transformation matrix (i.e. pre-transform the {@link com.codename1.ui.geom.Matrix} to anchor at a different point)
     * or use the {@link #setTransform(com.codename1.ui.geom.Matrix,int,int)} variation that allows you to explicitly set the 
     * anchor point.</p>
     * @param transform The transformation {@link com.codename1.ui.geom.Matrix} to use for drawing.  2D/Affine transformations
     * can be achieved using a 3x3 transformation {@link com.codename1.ui.geom.Matrix}.  3D/Perspective transformations
     * can be achieved using a 4x3 transformation {@link com.codename1.ui.geom.Matrix}.
     * 
     * @see #isTransformSupported
     * @see #isPerspectiveTransformSupported
     * @see #setTransform(com.codename1.ui.geom.Matrix,int,int)
     */
    public void setTransform(Transform transform){
        impl.setTransform(nativeGraphics, transform);
        
    }
    
    /**
     * Gets the transformation matrix that is currently applied to this graphics context.
     * @return The current transformation matrix.
     * @see #setTransform
     */
    public Transform getTransform(){
        return impl.getTransform(nativeGraphics);
        
    }
    
    
    
    //--------------------------------------------------------------------------
    // END SHAPE DRAWING METHODS
    //--------------------------------------------------------------------------
    /**
     * Draws a filled triangle with the given coordinates
     * 
     * @param x1 the x coordinate of the first vertex of the triangle
     * @param y1 the y coordinate of the first vertex of the triangle
     * @param x2 the x coordinate of the second vertex of the triangle
     * @param y2 the y coordinate of the second vertex of the triangle
     * @param x3 the x coordinate of the third vertex of the triangle
     * @param y3 the y coordinate of the third vertex of the triangle
     */
    public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3) {
        impl.fillTriangle(nativeGraphics, xTranslate + x1, yTranslate + y1, xTranslate + x2, yTranslate + y2, xTranslate + x3, yTranslate + y3);
    }

    /**
     * Draws the RGB values based on the MIDP API of a similar name. Renders a 
     * series of device-independent RGB+transparency values in a specified 
     * region. The values are stored in rgbData in a format with 24 bits of 
     * RGB and an eight-bit alpha value (0xAARRGGBB), with the first value 
     * stored at the specified offset. The scanlength  specifies the relative 
     * offset within the array between the corresponding pixels of consecutive 
     * rows. Any value for scanlength is acceptable (even negative values) 
     * provided that all resulting references are within the bounds of the 
     * rgbData array. The ARGB data is rasterized horizontally from left to 
     * right within each row. The ARGB values are rendered in the region 
     * specified by x, y, width and height, and the operation is subject 
     * to the current clip region and translation for this Graphics object.
     * 
     * @param rgbData an array of ARGB values in the format 0xAARRGGBB
     * @param offset the array index of the first ARGB value
     * @param x the horizontal location of the region to be rendered
     * @param y the vertical location of the region to be rendered
     * @param w the width of the region to be rendered
     * @param h the height of the region to be rendered
     * @param processAlpha true if rgbData has an alpha channel, false if
     * all pixels are fully opaque
     */
    void drawRGB(int[] rgbData, int offset, int x, int y, int w, int h, boolean processAlpha) {
        impl.drawRGB(nativeGraphics, rgbData, offset, x + xTranslate, y + yTranslate, w, h, processAlpha);
    }

    /**
     * Draws a radial gradient in the given coordinates with the given colors, 
     * doesn't take alpha into consideration when drawing the gradient.
     * Notice that a radial gradient will result in a circular shape, to create
     * a square use fillRect or draw a larger shape and clip to the appropriate size.
     * 
     * @param startColor the starting RGB color
     * @param endColor  the ending RGB color
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the width of the region to be filled
     * @param height the height of the region to be filled
     */
    public void fillRadialGradient(int startColor, int endColor, int x, int y, int width, int height) {
        impl.fillRadialGradient(nativeGraphics, startColor, endColor, x + xTranslate, y + yTranslate, width, height);
    }

    /**
     * Draws a radial gradient in the given coordinates with the given colors,
     * doesn't take alpha into consideration when drawing the gradient. Notice that this method
     * differs from fillRadialGradient since it draws a square gradient at all times
     * and can thus be cached
     * Notice that a radial gradient will result in a circular shape, to create
     * a square use fillRect or draw a larger shape and clip to the appropriate size.
     *
     * @param startColor the starting RGB color
     * @param endColor  the ending RGB color
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the width of the region to be filled
     * @param height the height of the region to be filled
     * @param relativeX indicates the relative position of the gradient within the drawing region
     * @param relativeY indicates the relative position of the gradient within the drawing region
     * @param relativeSize  indicates the relative size of the gradient within the drawing region
     */
    public void fillRectRadialGradient(int startColor, int endColor, int x, int y, int width, int height, float relativeX, float relativeY, float relativeSize) {
        impl.fillRectRadialGradient(nativeGraphics, startColor, endColor, x + xTranslate, y + yTranslate, width, height, relativeX, relativeY, relativeSize);
    }

    /**
     * Draws a linear gradient in the given coordinates with the given colors, 
     * doesn't take alpha into consideration when drawing the gradient
     * 
     * @param startColor the starting RGB color
     * @param endColor  the ending RGB color
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the width of the region to be filled
     * @param height the height of the region to be filled
     * @param horizontal indicating wheter it is a horizontal fill or vertical
     */
    public void fillLinearGradient(int startColor, int endColor, int x, int y, int width, int height, boolean horizontal) {
        impl.fillLinearGradient(nativeGraphics, startColor, endColor, x + xTranslate, y + yTranslate, width, height, horizontal);
    }

    /**
     * Fills a rectangle with an optionally translucent fill color
     * 
     * @param x the x coordinate of the rectangle to be filled
     * @param y the y coordinate of the rectangle to be filled
     * @param w the width of the rectangle to be filled
     * @param h the height of the rectangle to be filled
     * @param alpha the alpha values specify semitransparency
     */
    public void fillRect(int x, int y, int w, int h, byte alpha) {
        if(alpha != 0) {
            int oldAlpha = impl.getAlpha(nativeGraphics);
            impl.setAlpha(nativeGraphics, alpha & 0xff);
            impl.fillRect(nativeGraphics, x + xTranslate, y + yTranslate, w, h);
            impl.setAlpha(nativeGraphics, oldAlpha);
        }
    }

    /**
     *  Fills a closed polygon defined by arrays of x and y coordinates. 
     *  Each pair of (x, y) coordinates defines a point.
     * 
     *  @param xPoints - a an array of x coordinates.
     *  @param yPoints - a an array of y coordinates.
     *  @param nPoints - a the total number of points.
     */
    public void fillPolygon(int[] xPoints,
            int[] yPoints,
            int nPoints) {
        int[] cX = xPoints;
        int[] cY = yPoints;
        if((!impl.isTranslationSupported()) && (xTranslate != 0 || yTranslate != 0)) {
            cX = new int[nPoints];
            cY = new int[nPoints];
            System.arraycopy(xPoints, 0, cX, 0, nPoints);
            System.arraycopy(yPoints, 0, cY, 0, nPoints);
            for(int iter = 0 ; iter < nPoints ; iter++) {
                cX[iter] += xTranslate;
                cY[iter] += yTranslate;
            }
        }
        impl.fillPolygon(nativeGraphics, cX, cY, nPoints);
    }

    /**
     * Draws a region of an image in the given x/y coordinate
     * 
     * @param img the image to draw
     * @param x x location for the image
     * @param y y location for the image
     * @param imageX location within the image to draw
     * @param imageY location within the image to draw
     * @param imageWidth size of the location within the image to draw
     * @param imageHeight size of the location within the image to draw
     */
    void drawImageArea(Image img, int x, int y, int imageX, int imageY, int imageWidth, int imageHeight) {
        img.drawImageArea(this, nativeGraphics, x, y, imageX, imageY, imageWidth, imageHeight);
    }

    /**
     *  Draws a closed polygon defined by arrays of x and y coordinates. 
     *  Each pair of (x, y) coordinates defines a point.
     * 
     *  @param xPoints - a an array of x coordinates.
     *  @param yPoints - a an array of y coordinates.
     *  @param nPoints - a the total number of points.
     */
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        int[] cX = xPoints;
        int[] cY = yPoints;
        if((!impl.isTranslationSupported()) && (xTranslate != 0 || yTranslate != 0)) {
            cX = new int[nPoints];
            cY = new int[nPoints];
            System.arraycopy(xPoints, 0, cX, 0, nPoints);
            System.arraycopy(yPoints, 0, cY, 0, nPoints);
            for(int iter = 0 ; iter < nPoints ; iter++) {
                cX[iter] += xTranslate;
                cY[iter] += yTranslate;
            }
        }
        impl.drawPolygon(nativeGraphics, cX, cY, nPoints);
    }
    
    /**
     * Indicates whether invoking set/getAlpha would have an effect on all further
     * rendering from this graphics object.
     * 
     * @return false if setAlpha has no effect true if it applies to everything some effect
     */
    public boolean isAlphaSupported() {
       return impl.isAlphaGlobal(); 
    }
    
    /**
     * Sets alpha as a value between 0-255 (0 - 0xff) where 255 is completely opaque
     * and 0 is completely transparent
     * 
     * @param a the alpha value
     */
    public void setAlpha(int a) {
        impl.setAlpha(nativeGraphics, a);
    }

    
    /**
     * Returnes the alpha as a value between 0-255 (0 - 0xff) where 255 is completely opaque
     * and 0 is completely transparent
     * 
     * @return the alpha value
     */
    public int getAlpha() {
        return impl.getAlpha(nativeGraphics);
    }
    
    /**
     * Returns true if anti-aliasing for standard rendering operations is supported,
     * notice that text anti-aliasing is a separate attribute.
     * 
     * @return true if anti aliasing is supported
     */
    public boolean isAntiAliasingSupported() {
        return impl.isAntiAliasingSupported();
    }
    
    /**
     * Returns true if anti-aliasing for text is supported,
     * notice that text anti-aliasing is a separate attribute from standard anti-alisaing.
     * 
     * @return true if text anti aliasing is supported
     */
    public boolean isAntiAliasedTextSupported() {
        return impl.isAntiAliasedTextSupported();
    }

    
    /**
     * Returns true if anti-aliasing for standard rendering operations is turned on.
     * 
     * @return true if anti aliasing is active
     */
    public boolean isAntiAliased() {
        return impl.isAntiAliased(nativeGraphics);
    }

    /**
     * Set whether anti-aliasing for standard rendering operations is turned on.
     * 
     * @param a true if anti aliasing is active
     */
    public void setAntiAliased(boolean a) {
        impl.setAntiAliased(nativeGraphics, a);
    }
    
    /**
     * Set whether anti-aliasing for text is active,
     * notice that text anti-aliasing is a separate attribute from standard anti-alisaing.
     * 
     * @param a true if text anti aliasing is supported
     */
    public void setAntiAliasedText(boolean a) {
        impl.setAntiAliasedText(nativeGraphics, a);
    }
    
    /**
     * Indicates whether anti-aliasing for text is active,
     * notice that text anti-aliasing is a separate attribute from standard anti-alisaing.
     * 
     * @return true if text anti aliasing is supported
     */
    public boolean isAntiAliasedText() {
        return impl.isAntiAliasedText(nativeGraphics);
    }

    /**
     * Indicates whether the underlying implementation can draw using an affine
     * transform hence methods such as rotate, scale and shear would work
     *
     * @return true if an affine transformation matrix is present
     */
    public boolean isAffineSupported() {
        return impl.isAffineSupported();
    }

    /**
     * Resets the affine transform to the default value
     */
    public void resetAffine() {
        impl.resetAffine(nativeGraphics);
        scaleX = 1;
        scaleY = 1;
    }

    /**
     * Scales the coordinate system using the affine transform
     *
     * @param x scale factor for x
     * @param y scale factor for y
     */
    public void scale(float x, float y) {
        impl.scale(nativeGraphics, x, y);
        scaleX = x;
        scaleY = y;
    }

    /**
     * Rotates the coordinate system around a radian angle using the affine transform
     *
     * @param angle the rotation angle in radians
     */
    public void rotate(float angle) {
        impl.rotate(nativeGraphics, angle);
    }

    /**
     * Rotates the coordinate system around a radian angle using the affine transform
     *
     * @param angle the rotation angle in radians
     * @param pivotX the pivot point
     * @param pivotY the pivot point
     */
    public void rotate(float angle, int pivotX, int pivotY) {
        impl.rotate(nativeGraphics, angle, pivotX, pivotY);
    }

    /**
     * Shear the graphics coordinate system using the affine transform
     *
     * @param x shear factor for x
     * @param y shear factor for y
     */
    public void shear(float x, float y) {
        impl.shear(nativeGraphics, x, y);
    }

    /**
     * Starts accessing the native graphics in the underlying OS, when accessing
     * the native graphics Codename One shouldn't be used! The native graphics is unclipped
     * and untranslated by default and its the responsibility of the caller to clip/translate
     * appropriately.
     * <p>When finished with the native graphics it is essential to <b>invoke endNativeGraphicsAccess</b>
     *
     * @return an instance of the underlying native graphics object
     */
    public Object beginNativeGraphicsAccess() {
        if(nativeGraphicsState != null) {
            throw new IllegalStateException("beginNativeGraphicsAccess invoked twice in a row");
        }
        Boolean a = Boolean.FALSE, b = Boolean.FALSE;
        if(isAntiAliasedText()) {
            b = Boolean.TRUE;
        }
        if(isAntiAliased()) {
            a = Boolean.TRUE;
        }

        nativeGraphicsState = new Object[] {
            new Integer(getTranslateX()),
            new Integer(getTranslateY()),
            new Integer(getColor()),
            new Integer(getAlpha()),
            new Integer(getClipX()),
            new Integer(getClipY()),
            new Integer(getClipWidth()),
            new Integer(getClipHeight()),
            a, b
        };
        translate(-getTranslateX(), -getTranslateY());
        setAlpha(255);
        setClip(0, 0, Display.getInstance().getDisplayWidth(), Display.getInstance().getDisplayHeight());
        return nativeGraphics;
    }

    /**
     * Invoke this to restore Codename One's graphics settings into the native graphics
     */
    public void endNativeGraphicsAccess() {
        translate(((Integer)nativeGraphicsState[0]).intValue(), ((Integer)nativeGraphicsState[1]).intValue());
        setColor(((Integer)nativeGraphicsState[2]).intValue());
        setAlpha(((Integer)nativeGraphicsState[3]).intValue());
        setClip(((Integer)nativeGraphicsState[4]).intValue(),
                ((Integer)nativeGraphicsState[5]).intValue(),
                ((Integer)nativeGraphicsState[6]).intValue(),
                ((Integer)nativeGraphicsState[7]).intValue());
        setAntiAliased(((Boolean)nativeGraphicsState[8]).booleanValue());
        setAntiAliasedText(((Boolean)nativeGraphicsState[9]).booleanValue());
        nativeGraphicsState = null;
    }

    /**
     * Allows an implementation to optimize image tiling rendering logic
     * 
     * @param img the image
     * @param x coordinate to tile the image along
     * @param y coordinate to tile the image along
     * @param w coordinate to tile the image along
     * @param h coordinate to tile the image along 
     */
    public void tileImage(Image img, int x, int y, int w, int h) {
        impl.tileImage(nativeGraphics, img.getImage(), x + xTranslate, y + yTranslate, w, h);
    }
    
    /**
     * Returns the affine X scale
     * @return the current scale
     */
    public float getScaleX() {
        return scaleX;
    }

    /**
     * Returns the affine Y scale
     * @return the current scale
     */
    public float getScaleY() {
        return scaleY;
    }
}
