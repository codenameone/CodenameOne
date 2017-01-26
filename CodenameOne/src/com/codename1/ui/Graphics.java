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
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Shape;
import java.util.ArrayList;

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
    
    boolean paintPeersBehind;
    
    /**
     * Flag that indicates that this graphics object should buffer all operations
     * that occur after a peer component is drawn to the front graphics buffer so that
     * it can be optionally rendered to the front graphics context if the platform supports it.
     * 
     * 
     * 
     * @see CodenameOneImplementation#isFrontGraphicsSupported() 
     * @see #flush() 
     */
    boolean enableFrontGraphics;
    
    /**
     * A buffer for graphics operations that should be deferred to the front layer. Only used
     * when {@link #enableFrontGraphics} is on.
     * @see #flush(int, int, int, int) 
     */
    private java.util.List<BufferedOp> frontBuffer;
    
    /**
     * A list of peer components that are to be drawn in this cycle.  Only used when 
     * {@link #enableFrontGraphics} is on.
     */
    private java.util.List<PeerComponent> peerComponents;
    
    /**
     * Flag set when graphics operations should be deferred to the top layer.  Only used
     * when {@link #enableFrontGraphics} is on.
     * 
     * @see #flush() 
     * @see #enableFrontGraphics
     * @see #frontBuffer
     * @see #drawPeerComponent(com.codename1.ui.PeerComponent) 
     * @see CodenameOneImplementation#isFrontGraphicsSupported() 
     */
    private boolean frontBufferActive;
    
    private int xTranslate;
    private int yTranslate;
    private Transform translation;
    private GeneralPath tmpClipShape; /// A buffer shape to use when we need to transform a shape
    private int color;
    private Font current = Font.getDefaultFont();

    private CodenameOneImplementation impl;
    private Object nativeGraphics;

    private Object[] nativeGraphicsState;
    private float scaleX = 1, scaleY = 1;
    
    /**
     * Initialization operation that is added to the front buffer first.
     * It sets the graphics context settings to match the settings of the
     * graphics context at the point where the front buffer is initialized.
     */
    private class InitOp extends BufferedOp {
        int xTranslate;
        int yTranslate;
        int color;
        Font font;
        float scaleX;
        float scaleY;
        Transform transform;
        int clipX;
        int clipY;
        int clipW;
        int clipH;
        int alpha;
        boolean antialias;
        boolean antialiasText;
        
        
        @Override
        void execute(Graphics g) {
            
            g.translate(xTranslate-g.getTranslateX(), yTranslate-g.getTranslateY());
            g.setColor(color);
            g.setFont(font);
            g.scale(scaleX/g.getScaleX(), scaleY/g.getScaleY());
            g.setAlpha(alpha);
            g.setTransform(transform);
            g.setClip(clipX, clipY, clipW, clipH);
            g.setAntiAliased(antialias);
            g.setAntiAliasedText(antialiasText);
            
        }
        
    }
    
    /**
     * Base class for buffered drawing operations.  These are only used when {@link #enableFrontGraphics} is true.
     * 
     * @see CodenameOneImplementation#isFrontGraphicsSupported() 
     */
    private abstract class BufferedOp {
        private boolean hasBounds;
        private int x1;
        private int y1;
        private int _w;
        private int _h;
        abstract void execute(Graphics g);
        
        /**
         * Sets the bounds of this drawing operation.  These bounds are used by
         * {@link #intersects(int, int, int, int) } when checking if the drawing
         * operation intersects a peer component.
         * @param x1 The x-coordinate (in screen coordinates).
         * @param y1 The y-coordinate (in screen coordinates)
         * @param w The width of the bounds
         * @param h The height of the bounds.
         */
        void setBounds(int x1, int y1, int w, int h) {
            this.x1 = x1;
            this._w = w;
            this.y1 = y1;
            this._h = h;
            hasBounds = true;
        }
        
        /**
         * Checks to see if this drawing operation intersects a given box.  This is used
         * to see if the drawing operation intersects any peer components that are to be drawn.
         * 
         * <p>If a drawing operation has no bounds specified (i.e. {@link #hasBounds} is false, then
         * this will always return false.  Only drawing operations that correspond to drawing something
         * to the screen are considered to have bounds.  E.g. {@link #setColor(int) } will not have
         * any bounds, but {@link #fillRect(int, int, int, int) } has bounds.</p>
         * 
         * <p>{@link ClearRect} is a special case that has no bounds because it is used specially by
         * the {@link #drawPeerComponent(com.codename1.ui.PeerComponent) } method to clear space
         * when the component is drawn, and we don't want this to trigger an intersection with its own 
         * peer component.</p>
         * @param x The x-coordinate to check (screen coordinates).
         * @param y The y-coordinate to check (screen coordinates).
         * @param w The width to check.
         * @param h The height to check.
         * @return True if the operation has bounds and intersects the given box.
         */
        boolean intersects(int x, int y, int w, int h) {
            if (!hasBounds) return false;
            return Rectangle.intersects(x, y, w, h, x1, y1, _w, _h);
        }
    }
    
    /**
     * Constructing new graphics with a given javax.microedition.lcdui.Graphics 
     * @param g an implementation dependent native graphics instance
     */
    Graphics(Object nativeGraphics) {
        setGraphics(nativeGraphics);
        impl = Display.impl;
    }
    
    private Transform translation() {
        if (translation == null) {
            translation = Transform.makeTranslation(xTranslate, yTranslate);
        } else {
            translation.setTranslation(xTranslate, yTranslate);
        }
        return translation;
    }
    
    private GeneralPath tmpClipShape() {
        if (tmpClipShape == null) {
            tmpClipShape = new GeneralPath();
        }
        return tmpClipShape;
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
        if (frontBufferActive) {
            Translate t = new Translate();
            t.x = x;
            t.y = y;
            frontBuffer.add(t);
        }
    }

    private class Translate extends BufferedOp {
        int x;
        int y;

        @Override
        void execute(Graphics g) {
            g.translate(x, y);
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
        if (frontBufferActive) {
            SetColor o = new SetColor();
            o.RGB = RGB;
            frontBuffer.add(o);
        }
    }
    
    private class SetColor extends BufferedOp {
        int RGB;

        @Override
        void execute(Graphics g) {
            g.setColor(RGB);
        }
        
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
        if (frontBufferActive) {
            SetFont o = new SetFont();
            o.font = font;
            frontBuffer.add(o);
        }
    }
    
    private class SetFont extends BufferedOp {
        Font font;

        @Override
        void execute(Graphics g) {
            g.setFont(font);
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
    
    private class SetClip extends BufferedOp {
        int x;
        int y;
        int w;
        int h;
        
        @Override
        void execute(Graphics g) {
            g.setClip(x, y, w, h);
        }
        
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
        if (frontBufferActive) {
            ClipRect o = new ClipRect();
            o.x = x;
            o.y = y;
            o.width = width;
            o.height = height;
            frontBuffer.add(o);
        }
    }

    private class ClipRect extends BufferedOp {
        int x;
        int y;
        int width;
        int height;

        @Override
        void execute(Graphics g) {
            g.clipRect(x, y, width, height);
        }
        
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
        if (frontBufferActive) {
            SetClip o = new SetClip();
            o.x = x;
            o.y = y;
            o.w = width;
            o.h = height;
            frontBuffer.add(o);
        }
    }

    
    
    /**
     * Clips the Graphics context to the Shape.
     * <p>This is not supported on all platforms and contexts currently.  
     * Use {@link #isShapeClipSupported} to check if the current 
     * context supports clipping shapes.</p>
     * 
     * <script src="https://gist.github.com/codenameone/65f531adae2e8c22afc8.js"></script>
     * <img src="https://www.codenameone.com/img/blog/shaped-clipping.png" alt="Shaped clipping in action" />
     * 
     * @param shape The shape to clip.
     * @see #isShapeClipSupported
     */
    public void setClip(Shape shape) {
        if (xTranslate != 0 || yTranslate != 0) {
            GeneralPath p = tmpClipShape();
            p.setShape(shape, translation());
            shape = p;
        }
        impl.setClip(nativeGraphics, shape);
        if (frontBufferActive) {
            SetClipShape o = new SetClipShape();
            o.shape = shape;
            frontBuffer.add(o);
        }
    }
    
    private class SetClipShape extends BufferedOp {

        Shape shape;
        
        @Override
        void execute(Graphics g) {
            g.setClip(shape);
        }
    
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
        if (frontBufferActive) {
            DrawLine o = new DrawLine();
            o.x1 = x1;
            o.y1 = y1;
            o.x2 = x2;
            o.y2 = y2;
            o.setBounds(x1+xTranslate, y1+yTranslate, x2-x1, y2-y1);
            frontBuffer.add(o);
            return;
        }
        impl.drawLine(nativeGraphics, xTranslate + x1, yTranslate + y1, xTranslate + x2, yTranslate + y2);
        
    }
    
    private class DrawLine extends BufferedOp {
        int x1;
        int x2;
        int y1;
        int y2;

        @Override
        void execute(Graphics g) {
            g.drawLine(x1, y1, x2, y2);
        }
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
        if (frontBufferActive) {
            FillRect o = new FillRect();
            o.x = x;
            o.y = y;
            o.width = width;
            o.height = height;
            o.setBounds(x + xTranslate, y+yTranslate, width, height);
            frontBuffer.add(o);
            
            return;
        }
        impl.fillRect(nativeGraphics, xTranslate + x, yTranslate + y, width, height);
    }
    
    private class FillRect extends BufferedOp {
        int x;
        int y;
        int width;
        int height;

        
        @Override
        void execute(Graphics g) {
            g.fillRect(x, y, width, height);
        }
    }
    
    /**
     * Clears rectangular area of the graphics context.  This will remove any color
     * information that has already been drawn to the graphics context making it transparent.
     * <p>The difference between this method and say {@link #fillRect(int, int, int, int) } with alpha=0 is
     * that fillRect() will just blend with the colors underneath (and thus {@link #fillRect(int, int, int, int) }
     * with an alpha of 0 actually does nothing.</p>
     * NOTE: In contrast to other drawing methods, coordinates input here
     * are absolute and will not be adjusted by the xTranslate and yTranslate values
     * 
     * <p>This method is designed to be used by {@link #drawPeerComponent(com.codename1.ui.PeerComponent) } only.</p>
     * @param x The x-coordinate of the box to clear.  In screen coordinates.
     * @param y The y-coordinate of the box to clear.  In screen coordinates.
     * @param width The width of the box to clear.
     * @param height The height of the box to clear.
     */
    private void clearRectImpl(int x, int y, int width, int height) {
        if (frontBufferActive) {
            ClearRect o = new ClearRect();
            o.x = x;
            o.y = y;
            o.width = width;
            o.height = height;
            frontBuffer.add(o);
            return;
        }
        impl.clearRect(nativeGraphics, x, y, width, height);
    }
    
    private class ClearRect extends BufferedOp {
        int x;
        int y;
        int width;
        int height;

        
        @Override
        void execute(Graphics g) {
            g.clearRectImpl(x, y, width, height);
        }
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
        if (frontBufferActive) {
            DrawRect o = new DrawRect();
            o.x = x;
            o.y = y;
            o.width = width;
            o.height = height;
            o.setBounds(x+xTranslate, y+yTranslate, width, height);
            frontBuffer.add(o);
            return;
        }
        impl.drawRect(nativeGraphics, xTranslate + x, yTranslate + y, width, height);
    }

    private class DrawRect extends BufferedOp {
        int x;
        int y;
        int width;
        int height;
        

        @Override
        void execute(Graphics g) {
            g.drawRect(x, y, width, height);
        }
    }
    
    private class DrawRect2 extends BufferedOp {
        int x;
        int y;
        int width;
        int height;
        int thickness;

        @Override
        void execute(Graphics g) {
            g.drawRect(x, y, width, height, thickness);
        }
        
        
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
        if (frontBufferActive) {
            DrawRect2 o = new DrawRect2();
            o.x = x;
            o.y = y;
            o.width = width;
            o.height = height;
            o.thickness = thickness;
            o.setBounds(x+xTranslate, y+yTranslate, width, height);
            frontBuffer.add(o);
            return;
        }
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
        if (frontBufferActive) {
            DrawRoundRect o = new DrawRoundRect();
            o.x = x;
            o.y = y;
            o.width = width;
            o.height = height;
            o.arcHeight = arcHeight;
            o.arcWidth = arcWidth;
            o.setBounds(x+xTranslate, y+yTranslate, width, height);
            frontBuffer.add(o);
            return;
        }
        impl.drawRoundRect(nativeGraphics, xTranslate + x, yTranslate + y, width, height, arcWidth, arcHeight);
    }
    
    private class DrawRoundRect extends BufferedOp {
        int x;
        int y;
        int width;
        int height;
        int arcWidth;
        int arcHeight;

        @Override
        void execute(Graphics g) {
            g.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
        }
        
        
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
        if (frontBufferActive) {
            FillRoundRect o = new FillRoundRect();
            o.x = x;
            o.y = y;
            o.width = width;
            o.height = height;
            o.arcWidth = arcWidth;
            o.arcHeight = arcHeight;
            o.setBounds(x+xTranslate, y+yTranslate, width, height);
            frontBuffer.add(o);
            return;
        }
        impl.fillRoundRect(nativeGraphics, xTranslate + x, yTranslate + y, width, height, arcWidth, arcHeight);
    }
    
    
    private class FillRoundRect extends BufferedOp {
        int x;
        int y;
        int width;
        int height;
        int arcWidth;
        int arcHeight;

        @Override
        void execute(Graphics g) {
            g.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
        }

        
        
    }

    /**
     * Fills a circular or elliptical arc based on the given angles and bounding 
     * box. The resulting arc begins at startAngle and extends for arcAngle 
     * degrees. Usage:
     * 
     * <script src="https://gist.github.com/codenameone/31a32bdcf014a9e55a95.js"></script>
     * 
     * @param x the x coordinate of the upper-left corner of the arc to be filled.
     * @param y the y coordinate of the upper-left corner of the arc to be filled.
     * @param width the width of the arc to be filled.
     * @param height the height of the arc to be filled.
     * @param startAngle the beginning angle.
     * @param arcAngle the angular extent of the arc, relative to the start angle.
     */
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        if (frontBufferActive) {
            FillArc o = new FillArc();
            o.x = x;
            o.y = y;
            o.width = width;
            o.height = height;
            o.startAngle = startAngle;
            o.arcAngle = arcAngle;
            o.setBounds(x+xTranslate, y+yTranslate, width, height);
            frontBuffer.add(o);
            return;
        }
        impl.fillArc(nativeGraphics, xTranslate + x, yTranslate + y, width, height, startAngle, arcAngle);
    }

    private class FillArc extends BufferedOp {
        int x;
        int y;
        int width;
        int height;
        int startAngle;
        int arcAngle;

        @Override
        void execute(Graphics g) {
            g.fillArc(x, y, width, height, startAngle, arcAngle);
        }


        
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
        if (frontBufferActive) {
            DrawArc o = new DrawArc();
            o.x = x;
            o.y = y;
            o.width = width;
            o.height = height;
            o.startAngle = startAngle;
            o.arcAngle = arcAngle;
            o.setBounds(x+xTranslate, y+yTranslate, width, height);
            frontBuffer.add(o);
            return;
        }
        impl.drawArc(nativeGraphics, xTranslate + x, yTranslate + y, width, height, startAngle, arcAngle);
    }
    
    
    private class DrawArc extends BufferedOp {
        int x;
        int y;
        int width;
        int height;
        int startAngle;
        int arcAngle;

        @Override
        void execute(Graphics g) {
            g.drawArc(x, y, width, height, startAngle, arcAngle);
        }
    }

    private void drawStringImpl(String str, int x, int y) {
        // remove a commonly used trick to create a spacer label from the paint queue
        if(str.length() == 0 || (str.length() == 1 && str.charAt(0) == ' ')) {
            return;
        }
        if(!(current instanceof CustomFont)) {
            impl.drawString(nativeGraphics, str, x + xTranslate, y + yTranslate);
        } else {
            current.drawString(this, str, x, y);
        }
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
    public void drawString(String str, int x, int y,int textDecoration) {
        // remove a commonly used trick to create a spacer label from the paint queue
        if(str.length() == 0 || (str.length() == 1 && str.charAt(0) == ' ')) {
            return;
        }
        
        if (frontBufferActive) {
            DrawString o = new DrawString();
            o.str = str;
            o.x = x;
            o.y = y;
            o.textDecoration = textDecoration;
            o.setBounds(x+xTranslate, y+yTranslate, getFont().stringWidth(str), getFont().getHeight());
            frontBuffer.add(o);
            return;
        }
        Object nativeFont = null;
        if(current != null) {
            nativeFont = current.getNativeFont();
        }
        if (current instanceof CustomFont) {
            current.drawString(this, str, x, y);
        } else {
            impl.drawString(nativeGraphics, nativeFont, str, x + xTranslate, y + yTranslate, textDecoration);
        }
    }
    
    private class DrawString extends BufferedOp {
        String str;
        int x;
        int y;
        int textDecoration;

        @Override
        void execute(Graphics g) {
            g.drawString(str, x, y, textDecoration);
        }
    }
    
    /**
     * Draws a string using baseline coordinates. 
     * @param str The string to be drawn.
     * @param x The x-coordinate of the start of left edge of the text block.
     * @param y The y-coordinate of the baseline of the text.
     * @see #drawString(java.lang.String, int, int) 
     */
    public void drawStringBaseline(String str, int x, int y){
        drawString(str, x, y-current.getAscent());
    }
    
    /**
     * Draws a string using baseline coordinates. 
     * @param str The string to be drawn.
     * @param x The x-coordinate of the start of left edge of the text block.
     * @param y The y-coordinate of the baseline of the text.
     * @param textDecoration Text decoration bitmask (See Style's TEXT_DECORATION_* constants)
     * @see #drawString(java.lang.String, int, int, int) 
     */
    public void drawStringBaseline(String str, int x, int y, int textDecoration){
        drawString(str, x, y-current.getAscent(), textDecoration);
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
        if (frontBufferActive) {
            DrawChars o = new DrawChars();
            o.data = data;
            o.offset = offset;
            o.length = length;
            o.x = x;
            o.y = y;
            o.setBounds(x+xTranslate, y+yTranslate, getFont().charsWidth(data, 0, data.length), getFont().getHeight());
            frontBuffer.add(o);
            return;
        }
        if(!(current instanceof CustomFont)) {
            drawString(new String(data, offset, length), x, y);
        } else {
            CustomFont f = (CustomFont)current;
            f.drawChars(this, data, offset, length, x, y);
        }
    }

    private class DrawChars extends BufferedOp {
        char[] data;
        int offset;
        int length;
        int x;
        int y;
        
        @Override
        void execute(Graphics g) {
            g.drawChars(data, offset, length, x, y);
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
        if (frontBufferActive) {
            DrawImage o = new DrawImage();
            o.img = img;
            o.x = x;
            o.y = y;
            o.setBounds(x+xTranslate, y+yTranslate, img.getWidth(), img.getHeight());
            frontBuffer.add(o);
            return;
        }
        img.drawImage(this, nativeGraphics, x, y);
    }
    
    private class DrawImage extends BufferedOp {
        Image img;
        int x;
        int y;

        @Override
        void execute(Graphics g) {
            g.drawImage(img, x, y);
        }
        
        
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
        if (frontBufferActive) {
            DrawImageScaled o = new DrawImageScaled();
            o.img = img;
            o.x = x;
            o.y = y;
            o.w = w;
            o.h = h;
            o.setBounds(x+xTranslate, y+yTranslate, w, h);
            frontBuffer.add(o);
            return;
        }
        if(impl.isScaledImageDrawingSupported()) {
            img.drawImage(this, nativeGraphics, x, y, w, h);
        } else {
            drawImage(img.scaled(w, h), x, y);
        }
    }
    
    private class DrawImageScaled extends BufferedOp {
        Image img;
        int x;
        int y; 
        int w;
        int h;

        @Override
        void execute(Graphics g) {
            g.drawImage(img, x, y, w, h);
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
     * 
     * <script src="https://gist.github.com/codenameone/3f2f8cdaabb7780eae6f.js"></script>
     * <img src="https://www.codenameone.com/img/developer-guide/graphics-shape-fill.png" alt="Fill a shape general path" />
     * 
     * 
     * @param shape The shape to be drawn.
     * @param stroke the stroke to use
     * 
     * @see #setStroke
     * @see #isShapeSupported
     */
    public void drawShape(Shape shape, Stroke stroke){
        if (frontBufferActive) {
            DrawShape o = new DrawShape();
            o.shape = new GeneralPath(shape);
            o.stroke = stroke;
            Rectangle bounds = shape.getBounds();
            bounds.setX(bounds.getX() + xTranslate);
            bounds.setY(bounds.getY() + yTranslate);
            o.setBounds(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
            frontBuffer.add(o);
            return;
        }
        if ( isShapeSupported()){
            if ( xTranslate != 0 || yTranslate != 0 ){
                GeneralPath p = tmpClipShape();
                p.setShape(shape, translation());
                shape = p;
            }
            impl.drawShape(nativeGraphics, shape, stroke);
        }
       
    }
    
    private class DrawShape extends BufferedOp {
        Shape shape;
        Stroke stroke;

        @Override
        void execute(Graphics g) {
            g.drawShape(shape, stroke);
        }
    }
    
    /**
     * Fills the given shape using the current alpha and color settings.
     *  <p>This is not supported on
     * all platforms and contexts currently.  Use {@link #isShapeSupported} to check if the current 
     * context supports drawing shapes.</p>
     * 
     * <script src="https://gist.github.com/codenameone/3f2f8cdaabb7780eae6f.js"></script>
     * <img src="https://www.codenameone.com/img/developer-guide/graphics-shape-fill.png" alt="Fill a shape general path" />
     * 
     * 
     * @param shape The shape to be filled.
     * 
     * @see #isShapeSupported
     */
    public void fillShape(Shape shape){
        if (frontBufferActive) {
            FillShape o = new FillShape();
            o.shape = new GeneralPath(shape);
            Rectangle bounds = shape.getBounds();
            o.setBounds(bounds.getX()+xTranslate, bounds.getY()+yTranslate, bounds.getWidth(), bounds.getHeight());
            frontBuffer.add(o);
            return;
        }
        if ( isShapeSupported() ){
            if ( xTranslate != 0 || yTranslate != 0 ){
                GeneralPath p = tmpClipShape();
                p.setShape(shape, translation());
                shape = p;
            }
        
            impl.fillShape(nativeGraphics, shape);
        }
    }
    
    private class FillShape extends BufferedOp {
        Shape shape;

        @Override
        void execute(Graphics g) {
            g.fillShape(shape);
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
     * <p>Checks to see if this graphics context supports drawing shapes (i.e. {@link #drawShape}
     * and {@link #fillShape} methods. If this returns {@literal false}, and you call {@link #drawShape} or {@link #fillShape}, then
     * nothing will be drawn.</p>
     * @return {@literal true} If {@link #drawShape} and {@link #fillShape} are supported.  
     * @see #drawShape
     * @see #fillShape
     */
    public boolean isShapeSupported(){
        return impl.isShapeSupported(nativeGraphics);
    }
    
    /**
     * Checks to see if this graphics context supports clip Shape.
     * If this returns {@literal false}, calling setClip(Shape) will have no effect on the Graphics clipping area
     * @return {@literal true} If setClip(Shape) is supported.  
     */
    public boolean isShapeClipSupported(){
        return impl.isShapeClipSupported(nativeGraphics);
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
        if (frontBufferActive) {
            SetTransform o = new SetTransform();
            o.transform = transform.copy();
            frontBuffer.add(o);
        }
        
    }
    
    private class SetTransform extends BufferedOp {
        Transform transform;

        @Override
        void execute(Graphics g) {
            g.setTransform(transform);
        }
        
    }
    
    /**
     * Gets the transformation matrix that is currently applied to this graphics context.
     * @return The current transformation matrix.
     * @see #setTransform
     * @deprecated Use {@link #getTransform(com.codename1.ui.Transform) } instead.
     */
    public Transform getTransform(){
        return impl.getTransform(nativeGraphics);
        
    }
    
    /**
     * Loads the provided transform with the current transform applied to this graphics context.
     * @param t An "out" parameter to be filled with the current transform.
     */
    public void getTransform(Transform t) {
        impl.getTransform(nativeGraphics, t);
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
        if (frontBufferActive) {
            FillTriangle o = new FillTriangle();
            o.x1 = x1;
            o.y1 = y1;
            o.x2 = x2;
            o.y2 = y2;
            o.x3 = x3;
            o.y3 = y3;
            int minX = Math.min(Math.min(x1, x2), x3);
            int maxX = Math.max(Math.max(x1, x2), x3);
            int minY = Math.min(Math.min(y1, y2), y3);
            int maxY = Math.max(Math.max(y1, y2), y3);
            o.setBounds(minX +xTranslate, minY + yTranslate, maxX-minX, maxY-minY);
            frontBuffer.add(o);
            return;
        }
        impl.fillTriangle(nativeGraphics, xTranslate + x1, yTranslate + y1, xTranslate + x2, yTranslate + y2, xTranslate + x3, yTranslate + y3);
    }

    private class FillTriangle extends BufferedOp {
        int x1;
        int y1;
        int x2;
        int y2;
        int x3;
        int y3;

        @Override
        void execute(Graphics g) {
            g.fillTriangle(x1, y1, x2, y2, x3, y3);
        }
        
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
        if (frontBufferActive) {
            DrawRGB o = new DrawRGB();
            o.rgbData = rgbData;
            o.offset = offset;
            o.x = x;
            o.y = y;
            o.w = w;
            o.h = h;
            o.processAlpha = processAlpha;
            o.setBounds(x+xTranslate, y+yTranslate, w, h);
            frontBuffer.add(o);
            return;
        }
        impl.drawRGB(nativeGraphics, rgbData, offset, x + xTranslate, y + yTranslate, w, h, processAlpha);
    }
    
    private class DrawRGB extends BufferedOp {
        int[] rgbData;
        int offset;
        int x;
        int y;
        int w;
        int h;
        boolean processAlpha;

        @Override
        void execute(Graphics g) {
            g.drawRGB(rgbData, offset, x, y, w, h, processAlpha);
        }
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
        if (frontBufferActive) {
            FillRadialGradient o = new FillRadialGradient();
            o.startColor = startColor;
            o.endColor = endColor;
            o.x = x;
            o.y = y;
            o.width = width;
            o.height = height;
            o.setBounds(x+xTranslate, y+yTranslate, width, height);
            frontBuffer.add(o);
            return;
                    
        }
        impl.fillRadialGradient(nativeGraphics, startColor, endColor, x + xTranslate, y + yTranslate, width, height);
    }

    private class FillRadialGradient extends BufferedOp {
        int startColor;
        int endColor;
        int x;
        int y;
        int width;
        int height;
        
        @Override
        void execute(Graphics g) {
            g.fillRadialGradient(startColor, endColor, x, y, width, height);
        }
        
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
     * @param startAngle the beginning angle.  Zero is at 3 o'clock.  Positive angles are counter-clockwise.
     * @param arcAngle the angular extent of the arc, relative to the start angle. Positive angles are counter-clockwise.
     */
    public void fillRadialGradient(int startColor, int endColor, int x, int y, int width, int height, int startAngle, int arcAngle) {
        if (frontBufferActive) {
            FillRadialGradient2 o = new FillRadialGradient2();
            o.startColor = startColor;
            o.endColor = endColor;
            o.x = x;
            o.y = y;
            o.width = width;
            o.height = height;
            o.startAngle = startAngle;
            o.arcAngle = arcAngle;
            o.setBounds(x+xTranslate, y+yTranslate, width, height);
            frontBuffer.add(o);
            return;
        }
        impl.fillRadialGradient(nativeGraphics, startColor, endColor, x + xTranslate, y + yTranslate, width, height, startAngle, arcAngle);
    }
    
    private class FillRadialGradient2 extends BufferedOp {
        int startColor;
        int endColor;
        int x;
        int y;
        int width;
        int height;
        int startAngle;
        int arcAngle;
        
        @Override
        void execute(Graphics g) {
            g.fillRadialGradient(startColor, endColor, x, y, width, height, startAngle, arcAngle);
        }
        
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
        // people do that a lot sadly...
        if(startColor == endColor) {
            setColor(startColor);
            fillRect(x, y, width, height, (byte)0xff);
            return;
        }
        if (frontBufferActive) {
            FillRectRadialGradient o = new FillRectRadialGradient();
            o.startColor = startColor;
            o.endColor = endColor;
            o.x = x;
            o.y = y;
            o.width = width;
            o.height = height;
            o.relativeX = relativeX;
            o.relativeY = relativeY;
            o.relativeSize = relativeSize;
            o.setBounds(x+xTranslate, y+yTranslate, width, height);
            frontBuffer.add(o);
            return;
        }
        impl.fillRectRadialGradient(nativeGraphics, startColor, endColor, x + xTranslate, y + yTranslate, width, height, relativeX, relativeY, relativeSize);
    }
    
    private class FillRectRadialGradient extends BufferedOp {
        int startColor;
        int endColor;
        int x;
        int y;
        int width;
        int height;
        float relativeX;
        float relativeY;
        float relativeSize;

        @Override
        void execute(Graphics g) {
            g.fillRectRadialGradient(startColor, endColor, x, y, width, height, relativeX, relativeY, relativeSize);
        }
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
        // people do that a lot sadly...
        if(startColor == endColor) {
            setColor(startColor);
            fillRect(x, y, width, height, (byte)0xff);
            return;
        }
        if (frontBufferActive) {
            FillLinearGradient o = new FillLinearGradient();
            o.startColor = startColor;
            o.endColor = endColor;
            o.x = x;
            o.y = y;
            o.width = width;
            o.height = height;
            o.horizontal = horizontal;
            o.setBounds(x+xTranslate, y+yTranslate, width, height);
            frontBuffer.add(o);
            return;
        }
        impl.fillLinearGradient(nativeGraphics, startColor, endColor, x + xTranslate, y + yTranslate, width, height, horizontal);
    }

    private class FillLinearGradient extends BufferedOp {
        int startColor;
        int endColor;
        int x;
        int y;
        int width;
        int height;
        boolean horizontal;
        
        @Override
        void execute(Graphics g) {
            g.fillLinearGradient(startColor, endColor, x, y, width, height, horizontal);
        }
        
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
        if (frontBufferActive) {
            FillRectAlpha o = new FillRectAlpha();
            o.x = x;
            o.y = y;
            o.width = w;
            o.height = h;
            o.alpha = alpha;
            o.setBounds(x, y, w, h);
            frontBuffer.add(o);
            return;
                   
        }
        impl.fillRect(nativeGraphics, x, y, w, h, alpha);
    }
    
    private class FillRectAlpha extends BufferedOp {
        int x;
        int y;
        int width;
        int height;
        byte alpha;

        
        @Override
        void execute(Graphics g) {
            g.fillRect(x, y, width, height, alpha);
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
        
        if (frontBufferActive) {
            FillPolygon o = new FillPolygon();
            o.xPoints = xPoints;
            o.yPoints = yPoints;
            o.nPoints = nPoints;
            if (nPoints > 0) {
                int minX=xPoints[0];
                int minY=yPoints[0];
                int maxX=minX;
                int maxY=minY;
                for (int iter=1; iter<nPoints; iter++) {
                    minX = Math.min(xPoints[iter], minX);
                    maxX = Math.max(xPoints[iter], maxX);
                    minY = Math.min(yPoints[iter], minY);
                    maxY = Math.min(yPoints[iter], maxY);
                }
                o.setBounds(xTranslate+minX, yTranslate+minY, maxX-minX, maxY-minY);
            }
            frontBuffer.add(o);
            return;
        }
        
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
    
    private class FillPolygon extends BufferedOp {
        int[] xPoints;
        int[] yPoints;
        int nPoints;

        @Override
        void execute(Graphics g) {
            g.fillPolygon(xPoints, yPoints, nPoints);
        }
        
        
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
        if (frontBufferActive) {
            DrawPolygon o = new DrawPolygon();
            o.xPoints = xPoints;
            o.yPoints = yPoints;
            o.nPoints = nPoints;
            if (nPoints > 0) {
                int minX=xPoints[0];
                int minY=yPoints[0];
                int maxX=minX;
                int maxY=minY;
                for (int iter=1; iter<nPoints; iter++) {
                    minX = Math.min(xPoints[iter], minX);
                    maxX = Math.max(xPoints[iter], maxX);
                    minY = Math.min(yPoints[iter], minY);
                    maxY = Math.min(yPoints[iter], maxY);
                }
                o.setBounds(xTranslate+minX, yTranslate+minY, maxX-minX, maxY-minY);
            }
            frontBuffer.add(o);
            return;
        }
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
    
    private class DrawPolygon extends BufferedOp {
        int[] xPoints;
        int[] yPoints;
        int nPoints;

        @Override
        void execute(Graphics g) {
            g.drawPolygon(xPoints, yPoints, nPoints);
        }
        
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
        if (frontBufferActive) {
            SetAlpha o = new SetAlpha();
            o.a = a;
            frontBuffer.add(o);
        }
    }

    private class SetAlpha extends BufferedOp {
        int a;

        @Override
        void execute(Graphics g) {
            g.setAlpha(a);
        }
        
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
        if (frontBufferActive) {
            SetAntiAliased o = new SetAntiAliased();
            o.a = a;
            frontBuffer.add(o);
        }
    }
    
    private class SetAntiAliased extends BufferedOp {
        boolean a;

        @Override
        void execute(Graphics g) {
            g.setAntiAliased(a);
        }
    }
    
    /**
     * Set whether anti-aliasing for text is active,
     * notice that text anti-aliasing is a separate attribute from standard anti-alisaing.
     * 
     * @param a true if text anti aliasing is supported
     */
    public void setAntiAliasedText(boolean a) {
        impl.setAntiAliasedText(nativeGraphics, a);
        if (frontBufferActive) {
            SetAntiAliasedText o = new SetAntiAliasedText();
            o.a = a;
            frontBuffer.add(o);
        }
    }
    
    private class SetAntiAliasedText extends BufferedOp {
        boolean a;

        @Override
        void execute(Graphics g) {
            g.setAntiAliasedText(a);
        }
        
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
        if (frontBufferActive) {
            ResetAffine o = new ResetAffine();
            frontBuffer.add(o);
        }
    }
    
    private class ResetAffine extends BufferedOp {

        @Override
        void execute(Graphics g) {
            g.resetAffine();
        }
        
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
        if (frontBufferActive) {
            Scale o = new Scale();
            o.x = x;
            o.y = y;
            frontBuffer.add(o);
        }
    }
    
    private class Scale extends BufferedOp {
        float x;
        float y;

        @Override
        void execute(Graphics g) {
            g.scale(x, y);
        }
        
    }

    /**
     * Rotates the coordinate system around a radian angle using the affine transform
     *
     * @param angle the rotation angle in radians
     */
    public void rotate(float angle) {
        impl.rotate(nativeGraphics, angle);
        if (frontBufferActive) {
            Rotate o = new Rotate();
            o.angle = angle;
            frontBuffer.add(o);
        }
    }
    
    private class Rotate extends BufferedOp {
        float angle;
        @Override
        void execute(Graphics g) {
            g.rotate(angle);
        }
        
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
        if (frontBufferActive) {
            RotateAtPivot o = new RotateAtPivot();
            o.angle = angle;
            o.pivotX = pivotX;
            o.pivotY = pivotY;
            frontBuffer.add(o);
        }
    }
    
    private class RotateAtPivot extends BufferedOp {
        float angle;
        int pivotX;
        int pivotY;

        @Override
        void execute(Graphics g) {
            g.rotate(angle, pivotX, pivotY);
        }
        
        
    }

    /**
     * Shear the graphics coordinate system using the affine transform
     *
     * @param x shear factor for x
     * @param y shear factor for y
     */
    public void shear(float x, float y) {
        impl.shear(nativeGraphics, x, y);
        if (frontBufferActive) {
            Shear o = new Shear();
            o.x = x;
            o.y = y;
            frontBuffer.add(o);
        }
    }

    private class Shear extends BufferedOp {
        float x;
        float y;

        @Override
        void execute(Graphics g) {
            g.shear(x, y);
        }
        
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
        if (frontBufferActive) {
            TileImage o = new TileImage();
            o.img = img;
            o.x = x;
            o.y = y;
            o.w = w;
            o.h = h;
            o.setBounds(x+xTranslate, y+yTranslate, w, h);
            frontBuffer.add(o);
            return;
        }
        if(img.requiresDrawImage()) {
            int iW = img.getWidth();
            int iH = img.getHeight();
            int clipX = getClipX();
            int clipW = getClipWidth();
            int clipY = getClipY();
            int clipH = getClipHeight();
            clipRect(x, y, w, h);
            for (int xPos = 0; xPos <= w; xPos += iW) {
                for (int yPos = 0; yPos < h; yPos += iH) {
                    int actualX = xPos + x;
                    int actualY = yPos + y;
                    if(actualX > clipX + clipW) {
                        continue;
                    }
                    if(actualX + iW < clipX) {
                        continue;
                    }
                    if(actualY > clipY + clipH) {
                        continue;
                    }
                    if(actualY + iH < clipY) {
                        continue;
                    }
                    drawImage(img, actualX, actualY);
                }
            }
            setClip(clipX, clipY, clipW, clipH);
        } else {
            impl.tileImage(nativeGraphics, img.getImage(), x + xTranslate, y + yTranslate, w, h);
        }
    }
    
    private class TileImage extends BufferedOp {
        Image img;
        int x;
        int y;
        int w;
        int h;
        @Override
        void execute(Graphics g) {
            g.tileImage(img, x, y, w, h);
        }
        
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
    
    /**
     * Draws a peer component.  This doesn't actually draw anything, it just activates
     * the front graphics buffer and begins redirecting drawing operations to that buffer.
     * <p>This is only used on platforms where {@link CodenameOneImplementation#isFrontGraphicsSupported() } is enabled.</p>
     * @param peer The peer component to be drawn.
     */
    void drawPeerComponent(PeerComponent peer) {
        if (enableFrontGraphics && !frontBufferActive) {
            if (frontBuffer != null) {
                frontBuffer.clear();
            } else {
                frontBuffer = new ArrayList<BufferedOp>();
            }
            if (peerComponents != null) {
                peerComponents.clear();
            } else {
                peerComponents = new ArrayList<PeerComponent>();
            }
            peerComponents.add(peer);
            frontBufferActive = true;
            InitOp o = new InitOp();
            o.alpha = getAlpha();
            o.clipH = getClipHeight();
            o.clipW = getClipWidth();
            o.clipX = getClipX();
            o.clipY = getClipY();
            o.color = getColor();
            o.font = getFont();
            o.scaleX = getScaleX();
            o.scaleY = getScaleY();
            o.xTranslate = getTranslateX();
            o.yTranslate = getTranslateY();
            o.transform = getTransform().copy();
            o.antialias = isAntiAliased();
            o.antialiasText = isAntiAliasedText();
            frontBuffer.add(o);
        }
        if (frontBufferActive || paintPeersBehind) {
            // If we are drawing to the front buffer, we need to clear the pixels 
            // from whatever was underneath the peer component so that the front 
            // graphics layer doesn't get artifacts from previous frames.
            clearRectImpl(peer.getAbsoluteX(), peer.getAbsoluteY(), peer.getWidth(), peer.getHeight());
        }
        
    }
        
    /**
     * Flushes any pending drawing operations.  If this graphics context supports 
     * front graphics (i.e. drawing on top of peer components), then this will render
     * buffered drawing operations to the front graphics.  Otherwise it will just
     * render the operations to itself.
     * 
     * <P>We keep this package private because this should only be called internally.<p>
     * 
     */
    void flush(int x, int y, int width, int height) {
        if (shouldRenderFrontBuffer()) {
            Graphics g = impl.getFrontGraphics();
            if (g == null) {
                renderFrontBuffer(this, x, y, width, height);
            } else {
                renderFrontBuffer(g, x, y, width, height);
            }
        } else {
            impl.clearFrontGraphics();
            renderFrontBuffer(this, x, y, width, height);
        }
    }
    
    /**
     * Checks to see if the front buffer should be rendered to front graphics.
     * This will check to see if any of the drawing operations in the front buffer
     * intersect the peer components that were drawn, and only return true if there
     * is an overlap.
     * 
     * <p>If no peer components were rendered, this will always return false.</p>
     * 
     * @see #renderFrontBuffer(com.codename1.ui.Graphics) 
     * @return 
     */
    private boolean shouldRenderFrontBuffer() {
        if (frontBufferActive && !peerComponents.isEmpty()) {
            Rectangle[] peerRects = new Rectangle[peerComponents.size()];
            int iter = 0;
            for (PeerComponent c : peerComponents) {
                peerRects[iter++] = new Rectangle(c.getAbsoluteX(), c.getAbsoluteY(), c.getWidth(), c.getHeight());
            }
            
            for (BufferedOp op : frontBuffer) {
                for (Rectangle r : peerRects) {
                    if (op.intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    
    /**
     * Renders the buffered operations to the provided graphics context.  The idea
     * is that if {@link #shouldRenderFrontBuffer() } returns true, then the provided
     * graphics context will be for the front buffer (so that it is drawn in front
     * of peer components).
     * @param g The graphics context to render the front buffer to.
     */
    private void renderFrontBuffer(Graphics g, int x, int y, int width, int height) {
        if (frontBufferActive && !peerComponents.isEmpty()) {
            frontBufferActive = false;
            for (BufferedOp op : frontBuffer) {
                op.execute(g);
            }
            frontBuffer.clear();
            peerComponents.clear();
            if (g != this) {
                impl.setFrontGraphicsVisible(true);
                impl.flushFrontGraphics(x, y, width, height);
            } else {
                impl.setFrontGraphicsVisible(false);
            }
        } else {
            impl.setFrontGraphicsVisible(false);
        }
    }
    
    /**
     * Checks to see if drawing operations are currently being redirected to the
     * front graphics layer buffer.  This is important since legacy renderers for 
     * label and button need to be used while drawing to the front buffer.
     * <p>This is only relevant for platforms where {@link CodenameOneImplementation#isFrontGraphicsSupported() }
     * is on.</p>
     * 
     * @return 
     * 
     */
    boolean isDrawingToFrontBuffer() {
        return frontBufferActive;
    }
    
    
    
}
