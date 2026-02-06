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
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Shape;

/// Abstracts the underlying platform graphics context thus allowing us to achieve
/// portability between MIDP devices and CDC devices. This abstaction simplifies
/// and unifies the Graphics implementations of various platforms.
///
/// A graphics instance should never be created by the developer and is always accessed
/// using either a paint callback or a mutable image. There is no supported  way to create this
/// object directly.
public final class Graphics {

    /// Rendering hint to indicate that the context should prefer to render
    /// primitives in a quick way, at the cost of quality, if there is an
    /// expensive operation.
    ///
    /// #### Since
    ///
    /// 7.0
    ///
    /// #### See also
    ///
    /// - #setRenderingHints(int)
    ///
    /// - #getRenderingHints()
    public static final int RENDERING_HINT_FAST = 1;
    private final CodenameOneImplementation impl;
    /// Flag that specifies that native peers are rendered "behind" the this
    /// graphics context.  The main difference is that drawPeerComponent() will
    /// call clearRect() for its bounds to "poke a hole" in the graphics context
    /// to see through to the native layer.
    boolean paintPeersBehind;
    private int xTranslate;
    private int yTranslate;
    private Transform translation;
    private GeneralPath tmpClipShape;
    /// A buffer shape to use when we need to transform a shape
    private int color;
    private Paint paint;
    private Font current = Font.getDefaultFont();
    private Object nativeGraphics;
    private Object[] nativeGraphicsState;
    private float scaleX = 1;
    private float scaleY = 1;

    /// Constructing new graphics with a given javax.microedition.lcdui.Graphics
    ///
    /// #### Parameters
    ///
    /// - `g`: an implementation dependent native graphics instance
    Graphics(Object nativeGraphics) {
        setGraphics(nativeGraphics);
        impl = Display.impl;
    }

    // PMD thinks we need to override finalize. Ugh.
    @SuppressWarnings("PMD.MissingOverride")
    protected void finalize() {
        if (nativeGraphics != null) {
            impl.disposeGraphics(nativeGraphics);
        }
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

    /// Returns the underlying native graphics object
    ///
    /// #### Returns
    ///
    /// the underlying native graphics object
    Object getGraphics() {
        return nativeGraphics;
    }

    /// Setting graphics with a given javax.microedition.lcdui.Graphics
    ///
    /// #### Parameters
    ///
    /// - `g`: a given javax.microedition.lcdui.Graphics
    void setGraphics(Object g) {
        this.nativeGraphics = g;
    }

    /// Translates the X/Y location for drawing on the underlying surface. Translation
    /// is incremental so the new value will be added to the current translation and
    /// in order to reset translation we have to invoke
    /// `translate(-getTranslateX(), -getTranslateY())`
    ///
    /// #### Parameters
    ///
    /// - `x`: the x coordinate
    ///
    /// - `y`: the y coordinate
    public void translate(int x, int y) {
        if (impl.isTranslationSupported()) {
            impl.translate(nativeGraphics, x, y);
        } else {
            xTranslate += x;
            yTranslate += y;
        }
    }

    /// Returns the current x translate value
    ///
    /// #### Returns
    ///
    /// the current x translate value
    public int getTranslateX() {
        if (impl.isTranslationSupported()) {
            return impl.getTranslateX(nativeGraphics);
        } else {
            return xTranslate;
        }
    }

    /// Returns the current y translate value
    ///
    /// #### Returns
    ///
    /// the current y translate value
    public int getTranslateY() {
        if (impl.isTranslationSupported()) {
            return impl.getTranslateY(nativeGraphics);
        } else {
            return yTranslate;
        }
    }

    /// Returns the current color
    ///
    /// #### Returns
    ///
    /// the RGB graphics color
    public int getColor() {
        return color;
    }

    /// Sets the current rgb color while ignoring any potential alpha component within
    /// said color value.
    ///
    /// #### Parameters
    ///
    /// - `rgb`: the RGB value for the color.
    public void setColor(int rgb) {
        paint = null;
        color = 0xffffff & rgb;
        impl.setColor(nativeGraphics, color);
    }

    /// Sets paint to be used for filling shapes.  This is only used for the `#fillShape(com.codename1.ui.geom.Shape)` method.
    ///
    /// #### Parameters
    ///
    /// - `paint`
    ///
    /// #### Since
    ///
    /// 7.0
    ///
    /// #### See also
    ///
    /// - LinearGradientPaint
    public void setColor(Paint paint) {
        this.paint = paint;
    }

    /// Gets the current `Paint` that is set to be used for filling shapes.
    ///
    /// #### Returns
    ///
    /// The paint that is to be used for filling shapes.
    ///
    /// #### Since
    ///
    /// 7.0
    ///
    /// #### See also
    ///
    /// - LinearGradientPaint
    public Paint getPaint() {
        return paint;
    }

    /// Sets the current rgb color while ignoring any potential alpha component within
    /// said color value.
    ///
    /// #### Parameters
    ///
    /// - `rgb`: the RGB value for the color.
    ///
    /// #### Returns
    ///
    /// The previous color value.
    ///
    /// #### Since
    ///
    /// 8.0
    public int setAndGetColor(int rgb) {
        int old = getColor();
        setColor(rgb);
        return old;
    }

    /// Returns the font used with the drawString method calls
    ///
    /// #### Returns
    ///
    /// the font used with the drawString method calls
    public Font getFont() {
        return current;
    }

    /// Sets the font to use with the drawString method calls
    ///
    /// #### Parameters
    ///
    /// - `font`: the font used with the drawString method calls
    public void setFont(Font font) {

        this.current = font;
        if (!(font instanceof CustomFont)) {
            impl.setNativeFont(nativeGraphics, font.getNativeFont());
        }
    }

    /// Returns the x clipping position
    ///
    /// #### Returns
    ///
    /// the x clipping position
    public int getClipX() {
        return impl.getClipX(nativeGraphics) - xTranslate;
    }

    /// Returns the clip as an x,y,w,h array
    ///
    /// #### Returns
    ///
    /// clip array copy
    public int[] getClip() {
        return new int[]{getClipX(), getClipY(), getClipWidth(), getClipHeight()};
    }

    /// Sets the clip from an array containing x, y, width, height value
    ///
    /// #### Parameters
    ///
    /// - `clip`: 4 element array
    public void setClip(int[] clip) {
        setClip(clip[0], clip[1], clip[2], clip[3]);
    }

    /// Clips the Graphics context to the Shape.
    ///
    /// This is not supported on all platforms and contexts currently.
    /// Use `#isShapeClipSupported` to check if the current
    /// context supports clipping shapes.
    ///
    /// ```java
    /// Image duke = null;
    /// try {
    ///     // duke.png is just the default Codename One icon copied into place
    ///     duke = Image.createImage("/duke.png");
    /// } catch(IOException err) {
    ///     Log.e(err);
    /// }
    /// final Image finalDuke = duke;
    ///
    /// Form hi = new Form("Shape Clip");
    ///
    /// // We create a 50 x 100 shape, this is arbitrary since we can scale it easily
    /// GeneralPath path = new GeneralPath();
    /// path.moveTo(20,0);
    /// path.lineTo(30, 0);
    /// path.lineTo(30, 100);
    /// path.lineTo(20, 100);
    /// path.lineTo(20, 15);
    /// path.lineTo(5, 40);
    /// path.lineTo(5, 25);
    /// path.lineTo(20,0);
    ///
    /// Stroke stroke = new Stroke(0.5f, Stroke.CAP_ROUND, Stroke.JOIN_ROUND, 4);
    /// hi.getContentPane().getUnselectedStyle().setBgPainter((Graphics g, Rectangle rect) -> {
    ///     g.setColor(0xff);
    ///     float widthRatio = ((float)rect.getWidth()) / 50f;
    ///     float heightRatio = ((float)rect.getHeight()) / 100f;
    ///     g.scale(widthRatio, heightRatio);
    ///     g.translate((int)(((float)rect.getX()) / widthRatio), (int)(((float)rect.getY()) / heightRatio));
    ///     g.setClip(path);
    ///     g.setAntiAliased(true);
    ///     g.drawImage(finalDuke, 0, 0, 50, 100);
    ///     g.setClip(path.getBounds());
    ///     g.drawShape(path, stroke);
    ///     g.translate(-(int)(((float)rect.getX()) / widthRatio), -(int)(((float)rect.getY()) / heightRatio));
    ///     g.resetAffine();
    /// });
    ///
    /// hi.show();
    /// ```
    ///
    /// #### Parameters
    ///
    /// - `shape`: The shape to clip.
    ///
    /// #### See also
    ///
    /// - #isShapeClipSupported
    public void setClip(Shape shape) {
        if (xTranslate != 0 || yTranslate != 0) {
            GeneralPath p = tmpClipShape();
            p.setShape(shape, translation());
            shape = p;
        }
        impl.setClip(nativeGraphics, shape);
    }

    /// Returns the y clipping position
    ///
    /// #### Returns
    ///
    /// the y clipping position
    public int getClipY() {
        return impl.getClipY(nativeGraphics) - yTranslate;
    }

    /// Returns the clip width
    ///
    /// #### Returns
    ///
    /// the clip width
    public int getClipWidth() {
        return impl.getClipWidth(nativeGraphics);
    }

    /// Returns the clip height
    ///
    /// #### Returns
    ///
    /// the clip height
    public int getClipHeight() {
        return impl.getClipHeight(nativeGraphics);
    }

    /// Clips the given rectangle by intersecting with the current clipping region, this
    /// method can thus only shrink the clipping region and never increase it.
    ///
    /// #### Parameters
    ///
    /// - `x`: the x coordinate of the rectangle to intersect the clip with
    ///
    /// - `y`: the y coordinate of the rectangle to intersect the clip with
    ///
    /// - `width`: the width of the rectangle to intersect the clip with
    ///
    /// - `height`: the height of the rectangle to intersect the clip with
    public void clipRect(int x, int y, int width, int height) {
        impl.clipRect(nativeGraphics, xTranslate + x, yTranslate + y, width, height);
    }

    /// Updates the clipping region to match the given region exactly
    ///
    /// #### Parameters
    ///
    /// - `x`: the x coordinate of the new clip rectangle.
    ///
    /// - `y`: the y coordinate of the new clip rectangle.
    ///
    /// - `width`: the width of the new clip rectangle.
    ///
    /// - `height`: the height of the new clip rectangle.
    public void setClip(int x, int y, int width, int height) {
        impl.setClip(nativeGraphics, xTranslate + x, yTranslate + y, width, height);
    }

    /// Pushes the current clip onto the clip stack.  It can later be restored
    /// using `#popClip`.
    public void pushClip() {
        impl.pushClip(nativeGraphics);
    }

    /// Pops the top clip from the clip stack and sets it as the current clip.
    public void popClip() {
        impl.popClip(nativeGraphics);
    }

    /// Draws a line between the 2 X/Y coordinates
    ///
    /// #### Parameters
    ///
    /// - `x1`: first x position
    ///
    /// - `y1`: first y position
    ///
    /// - `x2`: second x position
    ///
    /// - `y2`: second y position
    public void drawLine(int x1, int y1, int x2, int y2) {
        impl.drawLine(nativeGraphics, xTranslate + x1, yTranslate + y1, xTranslate + x2, yTranslate + y2);

    }

    /// Fills the rectangle from the given position according to the width/height
    /// minus 1 pixel according to the convention in Java.
    ///
    /// #### Parameters
    ///
    /// - `x`: the x coordinate of the rectangle to be filled.
    ///
    /// - `y`: the y coordinate of the rectangle to be filled.
    ///
    /// - `width`: the width of the rectangle to be filled.
    ///
    /// - `height`: the height of the rectangle to be filled.
    public void fillRect(int x, int y, int width, int height) {
        impl.fillRect(nativeGraphics, xTranslate + x, yTranslate + y, width, height);
    }

    /// #### Deprecated
    ///
    /// this method should have been internals
    public void drawShadow(Image img, int x, int y, int offsetX, int offsetY, int blurRadius, int spreadRadius, int color, float opacity) {
        impl.drawShadow(nativeGraphics, img.getImage(), xTranslate + x, yTranslate + y, offsetX, offsetY, blurRadius, spreadRadius, color, opacity);
    }

    /// Clears rectangular area of the graphics context.  This will remove any color
    /// information that has already been drawn to the graphics context making it transparent.
    ///
    /// The difference between this method and say `int, int, int)` with alpha=0 is
    /// that fillRect() will just blend with the colors underneath (and thus `int, int, int)`
    /// with an alpha of 0 actually does nothing.
    ///
    /// NOTE: In contrast to other drawing methods, coordinates input here
    /// are absolute and will not be adjusted by the xTranslate and yTranslate values
    ///
    /// This method is designed to be used by `#drawPeerComponent(com.codename1.ui.PeerComponent)` only.
    ///
    /// #### Parameters
    ///
    /// - `x`: The x-coordinate of the box to clear.  In screen coordinates.
    ///
    /// - `y`: The y-coordinate of the box to clear.  In screen coordinates.
    ///
    /// - `width`: The width of the box to clear.
    ///
    /// - `height`: The height of the box to clear.
    public void clearRect(int x, int y, int width, int height) {
        clearRectImpl(xTranslate + x, yTranslate + y, width, height);
    }

    /// Clears rectangular area of the graphics context.  This will remove any color
    /// information that has already been drawn to the graphics context making it transparent.
    ///
    /// The difference between this method and say `int, int, int)` with alpha=0 is
    /// that fillRect() will just blend with the colors underneath (and thus `int, int, int)`
    /// with an alpha of 0 actually does nothing.
    ///
    /// NOTE: In contrast to other drawing methods, coordinates input here
    /// are absolute and will not be adjusted by the xTranslate and yTranslate values
    ///
    /// This method is designed to be used by `#drawPeerComponent(com.codename1.ui.PeerComponent)` only.
    ///
    /// #### Parameters
    ///
    /// - `x`: The x-coordinate of the box to clear.  In screen coordinates.
    ///
    /// - `y`: The y-coordinate of the box to clear.  In screen coordinates.
    ///
    /// - `width`: The width of the box to clear.
    ///
    /// - `height`: The height of the box to clear.
    private void clearRectImpl(int x, int y, int width, int height) {
        impl.clearRect(nativeGraphics, x, y, width, height);
    }

    /// Draws a rectangle in the given coordinates
    ///
    /// #### Parameters
    ///
    /// - `x`: the x coordinate of the rectangle to be drawn.
    ///
    /// - `y`: the y coordinate of the rectangle to be drawn.
    ///
    /// - `width`: the width of the rectangle to be drawn.
    ///
    /// - `height`: the height of the rectangle to be drawn.
    public void drawRect(int x, int y, int width, int height) {
        impl.drawRect(nativeGraphics, xTranslate + x, yTranslate + y, width, height);
    }

    /// Draws a rectangle in the given coordinates with the given thickness
    ///
    /// #### Parameters
    ///
    /// - `x`: the x coordinate of the rectangle to be drawn.
    ///
    /// - `y`: the y coordinate of the rectangle to be drawn.
    ///
    /// - `width`: the width of the rectangle to be drawn.
    ///
    /// - `height`: the height of the rectangle to be drawn.
    ///
    /// - `thickness`: the thickness in pixels
    public void drawRect(int x, int y, int width, int height, int thickness) {
        impl.drawRect(nativeGraphics, xTranslate + x, yTranslate + y, width, height, thickness);
    }

    /// Draws a rounded corner rectangle in the given coordinates with the arcWidth/height
    /// matching the last two arguments respectively.
    ///
    /// #### Parameters
    ///
    /// - `x`: the x coordinate of the rectangle to be drawn.
    ///
    /// - `y`: the y coordinate of the rectangle to be drawn.
    ///
    /// - `width`: the width of the rectangle to be drawn.
    ///
    /// - `height`: the height of the rectangle to be drawn.
    ///
    /// - `arcWidth`: the horizontal diameter of the arc at the four corners.
    ///
    /// - `arcHeight`: the vertical diameter of the arc at the four corners.
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        impl.drawRoundRect(nativeGraphics, xTranslate + x, yTranslate + y, width, height, arcWidth, arcHeight);
    }

    /// Makes the current color slightly lighter, this is useful for many visual effects
    ///
    /// #### Parameters
    ///
    /// - `factor`: the degree of lightening a color per channel a number from 1 to 255
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

    /// Makes the current color slightly darker, this is useful for many visual effects
    ///
    /// #### Parameters
    ///
    /// - `factor`: the degree of lightening a color per channel a number from 1 to 255
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

    /// Fills a rounded rectangle in the same way as drawRoundRect
    ///
    /// #### Parameters
    ///
    /// - `x`: the x coordinate of the rectangle to be filled.
    ///
    /// - `y`: the y coordinate of the rectangle to be filled.
    ///
    /// - `width`: the width of the rectangle to be filled.
    ///
    /// - `height`: the height of the rectangle to be filled.
    ///
    /// - `arcWidth`: the horizontal diameter of the arc at the four corners.
    ///
    /// - `arcHeight`: the vertical diameter of the arc at the four corners.
    ///
    /// #### See also
    ///
    /// - #drawRoundRect
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        impl.fillRoundRect(nativeGraphics, xTranslate + x, yTranslate + y, width, height, arcWidth, arcHeight);
    }

    /// Fills a circular or elliptical arc based on the given angles and bounding
    /// box. The resulting arc begins at startAngle and extends for arcAngle
    /// degrees. Usage:
    ///
    /// ```java
    /// Painter p = new Painter(cmp) {
    ///     public void paint(Graphics g, Rectangle rect) {
    ///         boolean antiAliased = g.isAntiAliased();
    ///         g.setAntiAliased(true);
    ///         int r = Math.min(rect.getWidth(), rect.getHeight())/2;
    ///         int x = rect.getX() + rect.getWidth()/2 - r;
    ///         int y = rect.getY() + rect.getHeight()/2 - r;
    ///         switch (style) {
    ///             case CircleButtonStrokedDark:
    ///             case CircleButtonStrokedLight: {
    ///                 if (cmp.getStyle().getBgTransparency() != 0) {
    ///                     int alpha = cmp.getStyle().getBgTransparency();
    ///                     if (alpha <0) {
    ///                         alpha = 0xff;
    ///                     }
    ///                     g.setColor(cmp.getStyle().getBgColor());
    ///                     g.setAlpha(alpha);
    ///                     g.fillArc(x, y, 2*r-1, 2*r-1, 0, 360);
    ///                     g.setAlpha(0xff);
    ///                 }
    ///                 g.setColor(cmp.getStyle().getFgColor());
    ///                 g.drawArc(x, y, 2*r-1, 2*r-1, 0, 360);
    ///                 break;
    ///             }
    ///             case CircleButtonFilledDark:
    ///             case CircleButtonFilledLight:
    ///             case CircleButtonTransparentDark:
    ///             case CircleButtonTransparentLight: {
    ///                 int alpha = cmp.getStyle().getBgTransparency();
    ///                 if (alpha < 0) {
    ///                     alpha = 0xff;
    ///                 }
    ///                 g.setAlpha(alpha);
    ///                 g.setColor(cmp.getStyle().getBgColor());
    ///                 g.fillArc(x, y, 2*r, 2*r, 0, 360);
    ///                 g.setAlpha(0xff);
    ///                 break;
    ///             }
    ///         }
    ///
    ///         g.setAntiAliased(antiAliased);
    ///     }
    /// };
    /// cmp.getAllStyles().setBgPainter(p);
    /// ```
    ///
    /// #### Parameters
    ///
    /// - `x`: the x coordinate of the upper-left corner of the arc to be filled.
    ///
    /// - `y`: the y coordinate of the upper-left corner of the arc to be filled.
    ///
    /// - `width`: the width of the arc to be filled, must be 1 or more.
    ///
    /// - `height`: the height of the arc to be filled, must be 1 or more.
    ///
    /// - `startAngle`: the beginning angle.
    ///
    /// - `arcAngle`: the angular extent of the arc, relative to the start angle.
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Width & Height of fillAsrc must be greater than 0");
        }
        impl.fillArc(nativeGraphics, xTranslate + x, yTranslate + y, width, height, startAngle, arcAngle);
    }

    /// Draws a circular or elliptical arc based on the given angles and bounding
    /// box
    ///
    /// #### Parameters
    ///
    /// - `x`: the x coordinate of the upper-left corner of the arc to be drawn.
    ///
    /// - `y`: the y coordinate of the upper-left corner of the arc to be drawn.
    ///
    /// - `width`: the width of the arc to be drawn.
    ///
    /// - `height`: the height of the arc to be drawn.
    ///
    /// - `startAngle`: the beginning angle.
    ///
    /// - `arcAngle`: the angular extent of the arc, relative to the start angle.
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        impl.drawArc(nativeGraphics, xTranslate + x, yTranslate + y, width, height, startAngle, arcAngle);
    }

    /// Draw a string using the current font and color in the x,y coordinates. The font is drawn
    /// from the top position and not the baseline.
    ///
    /// #### Parameters
    ///
    /// - `str`: the string to be drawn.
    ///
    /// - `x`: the x coordinate.
    ///
    /// - `y`: the y coordinate.
    ///
    /// - `textDecoration`: Text decoration bitmask (See Style's TEXT_DECORATION_* constants)
    public void drawString(String str, int x, int y, int textDecoration) {
        // remove a commonly used trick to create a spacer label from the paint queue
        if (str.length() == 0 || (str.length() == 1 && str.charAt(0) == ' ')) {
            return;
        }

        Object nativeFont = null;
        if (current != null) {
            nativeFont = current.getNativeFont();
        }
        if (current instanceof CustomFont) {
            current.drawString(this, str, x, y);
        } else {
            impl.drawString(nativeGraphics, nativeFont, str, x + xTranslate, y + yTranslate, textDecoration);
        }
    }

    /// Draws a string using baseline coordinates.
    ///
    /// #### Parameters
    ///
    /// - `str`: The string to be drawn.
    ///
    /// - `x`: The x-coordinate of the start of left edge of the text block.
    ///
    /// - `y`: The y-coordinate of the baseline of the text.
    ///
    /// #### See also
    ///
    /// - #drawString(java.lang.String, int, int)
    public void drawStringBaseline(String str, int x, int y) {
        drawString(str, x, y - current.getAscent());
    }

    /// Draws a string using baseline coordinates.
    ///
    /// #### Parameters
    ///
    /// - `str`: The string to be drawn.
    ///
    /// - `x`: The x-coordinate of the start of left edge of the text block.
    ///
    /// - `y`: The y-coordinate of the baseline of the text.
    ///
    /// - `textDecoration`: Text decoration bitmask (See Style's TEXT_DECORATION_* constants)
    ///
    /// #### See also
    ///
    /// - #drawString(java.lang.String, int, int, int)
    public void drawStringBaseline(String str, int x, int y, int textDecoration) {
        drawString(str, x, y - current.getAscent(), textDecoration);
    }

    /// Draw a string using the current font and color in the x,y coordinates. The font is drawn
    /// from the top position and not the baseline.
    ///
    /// #### Parameters
    ///
    /// - `str`: the string to be drawn.
    ///
    /// - `x`: the x coordinate.
    ///
    /// - `y`: the y coordinate.
    public void drawString(String str, int x, int y) {
        drawString(str, x, y, 0);
    }

    /// Draw the given char using the current font and color in the x,y
    /// coordinates. The font is drawn from the top position and not the
    /// baseline.
    ///
    /// #### Parameters
    ///
    /// - `character`: - the character to be drawn
    ///
    /// - `x`: the x coordinate of the baseline of the text
    ///
    /// - `y`: the y coordinate of the baseline of the text
    ///
    /// #### Deprecated
    ///
    /// use drawString instead, this method is inefficient
    public void drawChar(char character, int x, int y) {
        drawString("" + character, x, y);
    }

    /// Draw the given char array using the current font and color in the x,y coordinates. The font is drawn
    /// from the top position and not the baseline.
    ///
    /// #### Parameters
    ///
    /// - `data`: the array of characters to be drawn
    ///
    /// - `offset`: the start offset in the data
    ///
    /// - `length`: the number of characters to be drawn
    ///
    /// - `x`: the x coordinate of the baseline of the text
    ///
    /// - `y`: the y coordinate of the baseline of the text
    ///
    /// #### Deprecated
    ///
    /// use drawString instead, this method is inefficient
    public void drawChars(char[] data, int offset, int length, int x, int y) {
        if (!(current instanceof CustomFont)) {
            drawString(new String(data, offset, length), x, y);
        } else {
            CustomFont f = (CustomFont) current;
            f.drawChars(this, data, offset, length, x, y);
        }
    }

    /// Draws the image so its top left coordinate corresponds to x/y
    ///
    /// #### Parameters
    ///
    /// - `img`: @param img the specified image to be drawn. This method does
    ///            nothing if img is null.
    ///
    /// - `x`: the x coordinate.
    ///
    /// - `y`: the y coordinate.
    public void drawImage(Image img, int x, int y) {
        img.drawImage(this, nativeGraphics, x, y);
    }

    /// Draws the image so its top left coordinate corresponds to x/y and scales it to width/height
    ///
    /// #### Parameters
    ///
    /// - `img`: @param img the specified image to be drawn. This method does
    ///            nothing if img is null.
    ///
    /// - `x`: the x coordinate.
    ///
    /// - `y`: the y coordinate.
    ///
    /// - `w`: the width to occupy
    ///
    /// - `h`: the height to occupy
    public void drawImage(Image img, int x, int y, int w, int h) {
        if (impl.isScaledImageDrawingSupported()) {
            img.drawImage(this, nativeGraphics, x, y, w, h);
        } else {
            drawImage(img.scaled(w, h), x, y);
        }
    }


    void drawImageWH(Object nativeImage, int x, int y, int w, int h) {
        impl.drawImage(nativeGraphics, nativeImage, x + xTranslate, y + yTranslate, w, h);
    }

    void drawImage(Object img, int x, int y) {
        impl.drawImage(nativeGraphics, img, x + xTranslate, y + yTranslate);
    }

    /// Draws an image with a MIDP trasnform for fast rotation
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


    /// Draws a outline shape inside the specified bounding box.  The bounding box will resize the shape to fit in its dimensions.
    ///
    /// This is not supported on
    /// all platforms and contexts currently.  Use `#isShapeSupported` to check if the current
    /// context supports drawing shapes.
    ///
    /// ```java
    /// Form hi = new Form("Shape");
    ///
    /// // We create a 50 x 100 shape, this is arbitrary since we can scale it easily
    /// GeneralPath path = new GeneralPath();
    /// path.moveTo(20,0);
    /// path.lineTo(30, 0);
    /// path.lineTo(30, 100);
    /// path.lineTo(20, 100);
    /// path.lineTo(20, 15);
    /// path.lineTo(5, 40);
    /// path.lineTo(5, 25);
    /// path.lineTo(20,0);
    ///
    /// hi.getContentPane().getUnselectedStyle().setBgPainter((Graphics g, Rectangle rect) -> {
    ///     g.setColor(0xff);
    ///     float widthRatio = ((float)rect.getWidth()) / 50f;
    ///     float heightRatio = ((float)rect.getHeight()) / 100f;
    ///     g.scale(widthRatio, heightRatio);
    ///     g.translate((int)(((float)rect.getX()) / widthRatio), (int)(((float)rect.getY()) / heightRatio));
    ///     g.fillShape(path);
    ///     g.resetAffine();
    /// });
    ///
    /// hi.show();
    /// ```
    ///
    /// #### Parameters
    ///
    /// - `shape`: The shape to be drawn.
    ///
    /// - `stroke`: the stroke to use
    ///
    /// #### See also
    ///
    /// - #setStroke
    ///
    /// - #isShapeSupported
    public void drawShape(Shape shape, Stroke stroke) {
        if (isShapeSupported()) {
            if (xTranslate != 0 || yTranslate != 0) {
                GeneralPath p = tmpClipShape();
                p.setShape(shape, translation());
                shape = p;
            }
            impl.drawShape(nativeGraphics, shape, stroke);
        }

    }

    /// Fills the given shape using the current alpha and color settings.
    ///
    /// This is not supported on
    /// all platforms and contexts currently.  Use `#isShapeSupported` to check if the current
    /// context supports drawing shapes.
    ///
    /// ```java
    /// Form hi = new Form("Shape");
    ///
    /// // We create a 50 x 100 shape, this is arbitrary since we can scale it easily
    /// GeneralPath path = new GeneralPath();
    /// path.moveTo(20,0);
    /// path.lineTo(30, 0);
    /// path.lineTo(30, 100);
    /// path.lineTo(20, 100);
    /// path.lineTo(20, 15);
    /// path.lineTo(5, 40);
    /// path.lineTo(5, 25);
    /// path.lineTo(20,0);
    ///
    /// hi.getContentPane().getUnselectedStyle().setBgPainter((Graphics g, Rectangle rect) -> {
    ///     g.setColor(0xff);
    ///     float widthRatio = ((float)rect.getWidth()) / 50f;
    ///     float heightRatio = ((float)rect.getHeight()) / 100f;
    ///     g.scale(widthRatio, heightRatio);
    ///     g.translate((int)(((float)rect.getX()) / widthRatio), (int)(((float)rect.getY()) / heightRatio));
    ///     g.fillShape(path);
    ///     g.resetAffine();
    /// });
    ///
    /// hi.show();
    /// ```
    ///
    /// Note: You can specify a custom `Paint` to use for filling the shape using the `#setColor(com.codename1.ui.Paint)`
    /// method.  This is useful for filling the shape with a `LinearGradientPaint`, for example.
    ///
    /// #### Parameters
    ///
    /// - `shape`: The shape to be filled.
    ///
    /// #### See also
    ///
    /// - #isShapeSupported
    public void fillShape(Shape shape) {
        if (isShapeSupported()) {
            if (paint != null) {
                int clipX = getClipX();
                int clipY = getClipY();
                int clipW = getClipWidth();
                int clipH = getClipHeight();
                setClip(shape);
                clipRect(clipX, clipY, clipW, clipH);
                if (xTranslate != 0 || yTranslate != 0) {
                    GeneralPath p = tmpClipShape();
                    p.setShape(shape, translation());
                    shape = p;
                }
                Rectangle bounds = shape.getBounds();
                paint.paint(this, bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
                setClip(clipX, clipY, clipW, clipH);
                return;

            }
            if (xTranslate != 0 || yTranslate != 0) {
                GeneralPath p = tmpClipShape();
                p.setShape(shape, translation());
                shape = p;
            }

            impl.fillShape(nativeGraphics, shape);
        }
    }

    /// Checks to see if `com.codename1.ui.geom.Matrix` transforms are supported by this graphics context.
    ///
    /// #### Returns
    ///
    /// @return true if this graphics context supports `com.codename1.ui.geom.Matrix` transforms.
    ///
    /// Note that this method only confirms that 2D transforms are supported.  If you need to perform 3D
    /// transformations, you should use the `#isPerspectiveTransformSupported` method.
    ///
    /// #### See also
    ///
    /// - #setTransform
    ///
    /// - #getTransform
    ///
    /// - #isPerspectiveTransformSupported
    public boolean isTransformSupported() {
        return impl.isTransformSupported(nativeGraphics);
    }

    /// Checks to see if perspective (3D) `com.codename1.ui.geom.Matrix` transforms are supported by this graphics
    /// context.  If 3D transforms are supported, you can use a 4x4 transformation `com.codename1.ui.geom.Matrix`
    /// via `#setTransform` to perform 3D transforms.
    ///
    /// Note: It is possible for 3D transforms to not be supported but Affine (2D)
    /// transforms to be supported.  In this case you would be limited to a 3x3 transformation
    /// matrix in `#setTransform`.  You can check for 2D transformation support using the `#isTransformSupported` method.
    ///
    /// #### Returns
    ///
    /// true if Perspective (3D) transforms are supported.  false otherwise.
    ///
    /// #### See also
    ///
    /// - #isTransformSupported
    ///
    /// - #setTransform
    ///
    /// - #getTransform
    public boolean isPerspectiveTransformSupported() {
        return impl.isPerspectiveTransformSupported(nativeGraphics);
    }

    /// Checks to see if this graphics context supports drawing shapes (i.e. `#drawShape`
    /// and `#fillShape` methods. If this returns false, and you call `#drawShape` or `#fillShape`, then
    /// nothing will be drawn.
    ///
    /// #### Returns
    ///
    /// true If `#drawShape` and `#fillShape` are supported.
    ///
    /// #### See also
    ///
    /// - #drawShape
    ///
    /// - #fillShape
    public boolean isShapeSupported() {
        return impl.isShapeSupported(nativeGraphics);
    }

    /// Checks to see if this graphics context supports clip Shape.
    /// If this returns false, calling setClip(Shape) will have no effect on the Graphics clipping area
    ///
    /// #### Returns
    ///
    /// true If setClip(Shape) is supported.
    public boolean isShapeClipSupported() {
        return impl.isShapeClipSupported(nativeGraphics);
    }

    /// Concatenates the given transform to the context's transform.
    ///
    /// #### Parameters
    ///
    /// - `transform`: The transform to concatenate.
    ///
    /// #### Since
    ///
    /// 7.0
    public void transform(Transform transform) {
        Transform existing = getTransform();
        existing.concatenate(transform);
        setTransform(existing);
    }

    /// Gets the transformation matrix that is currently applied to this graphics context.
    ///
    /// #### Returns
    ///
    /// The current transformation matrix.
    ///
    /// #### Deprecated
    ///
    /// Use `#getTransform(com.codename1.ui.Transform)` instead.
    ///
    /// #### See also
    ///
    /// - #setTransform
    public Transform getTransform() {
        return impl.getTransform(nativeGraphics);

    }

    /// Sets the transformation `com.codename1.ui.geom.Matrix` to apply to drawing in this graphics context.
    /// In order to use this for 2D/Affine transformations you should first check to
    /// make sure that transforms are supported by calling the `#isTransformSupported`
    /// method.  For 3D/Perspective transformations, you should first check to
    /// make sure that 3D/Perspective transformations are supported by calling the
    /// `#isPerspectiveTransformSupported`.
    ///
    /// Transformations are applied with (0,0) as the origin.  So rotations and
    /// scales are anchored at this point on the screen.  You can use a different
    /// anchor point by either embedding it in the transformation matrix (i.e. pre-transform the `com.codename1.ui.geom.Matrix` to anchor at a different point)
    /// or use the `int, int)` variation that allows you to explicitly set the
    /// anchor point.
    ///
    /// #### Parameters
    ///
    /// - `transform`: @param transform The transformation `com.codename1.ui.geom.Matrix` to use for drawing.  2D/Affine transformations
    ///                  can be achieved using a 3x3 transformation `com.codename1.ui.geom.Matrix`.  3D/Perspective transformations
    ///                  can be achieved using a 4x3 transformation `com.codename1.ui.geom.Matrix`.
    ///
    /// #### See also
    ///
    /// - #isTransformSupported
    ///
    /// - #isPerspectiveTransformSupported
    ///
    /// - #setTransform(com.codename1.ui.geom.Matrix, int, int)
    public void setTransform(Transform transform) {
        impl.setTransform(nativeGraphics, transform);
    }

    /// Loads the provided transform with the current transform applied to this graphics context.
    ///
    /// #### Parameters
    ///
    /// - `t`: An "out" parameter to be filled with the current transform.
    public void getTransform(Transform t) {
        impl.getTransform(nativeGraphics, t);
    }

    //--------------------------------------------------------------------------
    // END SHAPE DRAWING METHODS
    //--------------------------------------------------------------------------

    /// Draws a filled triangle with the given coordinates
    ///
    /// #### Parameters
    ///
    /// - `x1`: the x coordinate of the first vertex of the triangle
    ///
    /// - `y1`: the y coordinate of the first vertex of the triangle
    ///
    /// - `x2`: the x coordinate of the second vertex of the triangle
    ///
    /// - `y2`: the y coordinate of the second vertex of the triangle
    ///
    /// - `x3`: the x coordinate of the third vertex of the triangle
    ///
    /// - `y3`: the y coordinate of the third vertex of the triangle
    public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3) {
        impl.fillTriangle(nativeGraphics, xTranslate + x1, yTranslate + y1, xTranslate + x2, yTranslate + y2, xTranslate + x3, yTranslate + y3);
    }

    /// Draws the RGB values based on the MIDP API of a similar name. Renders a
    /// series of device-independent RGB+transparency values in a specified
    /// region. The values are stored in rgbData in a format with 24 bits of
    /// RGB and an eight-bit alpha value (0xAARRGGBB), with the first value
    /// stored at the specified offset. The scanlength  specifies the relative
    /// offset within the array between the corresponding pixels of consecutive
    /// rows. Any value for scanlength is acceptable (even negative values)
    /// provided that all resulting references are within the bounds of the
    /// rgbData array. The ARGB data is rasterized horizontally from left to
    /// right within each row. The ARGB values are rendered in the region
    /// specified by x, y, width and height, and the operation is subject
    /// to the current clip region and translation for this Graphics object.
    ///
    /// #### Parameters
    ///
    /// - `rgbData`: an array of ARGB values in the format 0xAARRGGBB
    ///
    /// - `offset`: the array index of the first ARGB value
    ///
    /// - `x`: the horizontal location of the region to be rendered
    ///
    /// - `y`: the vertical location of the region to be rendered
    ///
    /// - `w`: the width of the region to be rendered
    ///
    /// - `h`: the height of the region to be rendered
    ///
    /// - `processAlpha`: @param processAlpha true if rgbData has an alpha channel, false if
    ///                     all pixels are fully opaque
    void drawRGB(int[] rgbData, int offset, int x, int y, int w, int h, boolean processAlpha) {
        impl.drawRGB(nativeGraphics, rgbData, offset, x + xTranslate, y + yTranslate, w, h, processAlpha);
    }

    /// Draws a radial gradient in the given coordinates with the given colors,
    /// doesn't take alpha into consideration when drawing the gradient.
    /// Notice that a radial gradient will result in a circular shape, to create
    /// a square use fillRect or draw a larger shape and clip to the appropriate size.
    ///
    /// #### Parameters
    ///
    /// - `startColor`: the starting RGB color
    ///
    /// - `endColor`: the ending RGB color
    ///
    /// - `x`: the x coordinate
    ///
    /// - `y`: the y coordinate
    ///
    /// - `width`: the width of the region to be filled
    ///
    /// - `height`: the height of the region to be filled
    public void fillRadialGradient(int startColor, int endColor, int x, int y, int width, int height) {
        impl.fillRadialGradient(nativeGraphics, startColor, endColor, x + xTranslate, y + yTranslate, width, height);
    }

    /// Draws a radial gradient in the given coordinates with the given colors,
    /// doesn't take alpha into consideration when drawing the gradient.
    /// Notice that a radial gradient will result in a circular shape, to create
    /// a square use fillRect or draw a larger shape and clip to the appropriate size.
    ///
    /// #### Parameters
    ///
    /// - `startColor`: the starting RGB color
    ///
    /// - `endColor`: the ending RGB color
    ///
    /// - `x`: the x coordinate
    ///
    /// - `y`: the y coordinate
    ///
    /// - `width`: the width of the region to be filled
    ///
    /// - `height`: the height of the region to be filled
    ///
    /// - `startAngle`: the beginning angle.  Zero is at 3 o'clock.  Positive angles are counter-clockwise.
    ///
    /// - `arcAngle`: the angular extent of the arc, relative to the start angle. Positive angles are counter-clockwise.
    public void fillRadialGradient(int startColor, int endColor, int x, int y, int width, int height, int startAngle, int arcAngle) {
        impl.fillRadialGradient(nativeGraphics, startColor, endColor, x + xTranslate, y + yTranslate, width, height, startAngle, arcAngle);
    }

    /// Draws a radial gradient in the given coordinates with the given colors,
    /// doesn't take alpha into consideration when drawing the gradient. Notice that this method
    /// differs from fillRadialGradient since it draws a square gradient at all times
    /// and can thus be cached
    /// Notice that a radial gradient will result in a circular shape, to create
    /// a square use fillRect or draw a larger shape and clip to the appropriate size.
    ///
    /// #### Parameters
    ///
    /// - `startColor`: the starting RGB color
    ///
    /// - `endColor`: the ending RGB color
    ///
    /// - `x`: the x coordinate
    ///
    /// - `y`: the y coordinate
    ///
    /// - `width`: the width of the region to be filled
    ///
    /// - `height`: the height of the region to be filled
    ///
    /// - `relativeX`: indicates the relative position of the gradient within the drawing region
    ///
    /// - `relativeY`: indicates the relative position of the gradient within the drawing region
    ///
    /// - `relativeSize`: indicates the relative size of the gradient within the drawing region
    public void fillRectRadialGradient(int startColor, int endColor, int x, int y, int width, int height, float relativeX, float relativeY, float relativeSize) {
        // people do that a lot sadly...
        if (startColor == endColor) {
            setColor(startColor);
            fillRect(x, y, width, height, (byte) 0xff);
            return;
        }
        impl.fillRectRadialGradient(nativeGraphics, startColor, endColor, x + xTranslate, y + yTranslate, width, height, relativeX, relativeY, relativeSize);
    }

    /// Draws a linear gradient in the given coordinates with the given colors,
    /// doesn't take alpha into consideration when drawing the gradient
    ///
    /// #### Parameters
    ///
    /// - `startColor`: the starting RGB color
    ///
    /// - `endColor`: the ending RGB color
    ///
    /// - `x`: the x coordinate
    ///
    /// - `y`: the y coordinate
    ///
    /// - `width`: the width of the region to be filled
    ///
    /// - `height`: the height of the region to be filled
    ///
    /// - `horizontal`: indicating wheter it is a horizontal fill or vertical
    public void fillLinearGradient(int startColor, int endColor, int x, int y, int width, int height, boolean horizontal) {
        // people do that a lot sadly...
        if (startColor == endColor) {
            setColor(startColor);
            fillRect(x, y, width, height, (byte) 0xff);
            return;
        }
        impl.fillLinearGradient(nativeGraphics, startColor, endColor, x + xTranslate, y + yTranslate, width, height, horizontal);
    }

    /// Fills a rectangle with an optionally translucent fill color
    ///
    /// #### Parameters
    ///
    /// - `x`: the x coordinate of the rectangle to be filled
    ///
    /// - `y`: the y coordinate of the rectangle to be filled
    ///
    /// - `w`: the width of the rectangle to be filled
    ///
    /// - `h`: the height of the rectangle to be filled
    ///
    /// - `alpha`: the alpha values specify semitransparency
    public void fillRect(int x, int y, int w, int h, byte alpha) {
        impl.fillRect(nativeGraphics, x + xTranslate, y + yTranslate, w, h, alpha);
    }

    /// Fills a closed polygon defined by arrays of x and y coordinates.
    /// Each pair of (x, y) coordinates defines a point.
    ///
    /// #### Parameters
    ///
    /// - `xPoints`: - a an array of x coordinates.
    ///
    /// - `yPoints`: - a an array of y coordinates.
    ///
    /// - `nPoints`: - a the total number of points.
    public void fillPolygon(int[] xPoints,
                            int[] yPoints,
                            int nPoints) {
        int[] cX = xPoints;
        int[] cY = yPoints;
        if ((!impl.isTranslationSupported()) && (xTranslate != 0 || yTranslate != 0)) {
            cX = new int[nPoints];
            cY = new int[nPoints];
            System.arraycopy(xPoints, 0, cX, 0, nPoints);
            System.arraycopy(yPoints, 0, cY, 0, nPoints);
            for (int iter = 0; iter < nPoints; iter++) {
                cX[iter] += xTranslate;
                cY[iter] += yTranslate;
            }
        }
        impl.fillPolygon(nativeGraphics, cX, cY, nPoints);
    }

    /// Draws a region of an image in the given x/y coordinate
    ///
    /// #### Parameters
    ///
    /// - `img`: the image to draw
    ///
    /// - `x`: x location for the image
    ///
    /// - `y`: y location for the image
    ///
    /// - `imageX`: location within the image to draw
    ///
    /// - `imageY`: location within the image to draw
    ///
    /// - `imageWidth`: size of the location within the image to draw
    ///
    /// - `imageHeight`: size of the location within the image to draw
    void drawImageArea(Image img, int x, int y, int imageX, int imageY, int imageWidth, int imageHeight) {
        img.drawImageArea(this, nativeGraphics, x, y, imageX, imageY, imageWidth, imageHeight);
    }

    /// Draws a closed polygon defined by arrays of x and y coordinates.
    /// Each pair of (x, y) coordinates defines a point.
    ///
    /// #### Parameters
    ///
    /// - `xPoints`: - an array of x coordinates.
    ///
    /// - `yPoints`: - an array of y coordinates.
    ///
    /// - `nPoints`: - the total number of points.
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        int[] cX = xPoints;
        int[] cY = yPoints;
        if ((!impl.isTranslationSupported()) && (xTranslate != 0 || yTranslate != 0)) {
            cX = new int[nPoints];
            cY = new int[nPoints];
            System.arraycopy(xPoints, 0, cX, 0, nPoints);
            System.arraycopy(yPoints, 0, cY, 0, nPoints);
            for (int iter = 0; iter < nPoints; iter++) {
                cX[iter] += xTranslate;
                cY[iter] += yTranslate;
            }
        }
        impl.drawPolygon(nativeGraphics, cX, cY, nPoints);
    }

    /// Indicates whether invoking set/getAlpha would have an effect on all further
    /// rendering from this graphics object.
    ///
    /// #### Returns
    ///
    /// false if setAlpha has no effect true if it applies to everything some effect
    public boolean isAlphaSupported() {
        return impl.isAlphaGlobal();
    }

    /// Sets alpha as a value between 0-255 (0 - 0xff) where 255 is completely opaque
    /// and 0 is completely transparent
    ///
    /// #### Parameters
    ///
    /// - `a`: the alpha value
    ///
    /// #### Returns
    ///
    /// The previous alpha value.
    public int setAndGetAlpha(int a) {
        int old = getAlpha();
        setAlpha(a);
        return old;
    }

    /// Concatenates the given alpha value to the current alpha setting, and returns the previous alpha
    /// setting.
    ///
    /// #### Parameters
    ///
    /// - `a`: Alpha value to concatenate (0-255).
    ///
    /// #### Returns
    ///
    /// The previous alpha setting (0-255).
    ///
    /// #### Since
    ///
    /// 7.0
    public int concatenateAlpha(int a) {
        if (a == 255) {
            return getAlpha();
        }

        int oldAlpha = getAlpha();
        setAlpha((int) (oldAlpha * (a / 255f)));
        return oldAlpha;
    }

    /// Returns the alpha as a value between 0-255 (0 - 0xff) where 255 is completely opaque
    /// and 0 is completely transparent
    ///
    /// #### Returns
    ///
    /// the alpha value
    public int getAlpha() {
        return impl.getAlpha(nativeGraphics);
    }

    /// Sets alpha as a value between 0-255 (0 - 0xff) where 255 is completely opaque
    /// and 0 is completely transparent
    ///
    /// #### Parameters
    ///
    /// - `a`: the alpha value
    public void setAlpha(int a) {
        impl.setAlpha(nativeGraphics, a);
    }

    /// Returns true if antialiasing for standard rendering operations is supported,
    /// notice that text antialiasing is a separate attribute.
    ///
    /// #### Returns
    ///
    /// true if antialiasing is supported
    public boolean isAntiAliasingSupported() {
        return impl.isAntiAliasingSupported(nativeGraphics);
    }

    /// Returns true if antialiasing for text is supported,
    /// notice that text antialiasing is a separate attribute from standard anti-alisaing.
    ///
    /// #### Returns
    ///
    /// true if text antialiasing is supported
    public boolean isAntiAliasedTextSupported() {
        return impl.isAntiAliasedTextSupported(nativeGraphics);
    }


    /// Returns true if antialiasing for standard rendering operations is turned on.
    ///
    /// #### Returns
    ///
    /// true if antialiasing is active
    public boolean isAntiAliased() {
        return impl.isAntiAliased(nativeGraphics);
    }

    /// Set whether antialiasing for standard rendering operations is turned on.
    ///
    /// #### Parameters
    ///
    /// - `a`: true if antialiasing is active
    public void setAntiAliased(boolean a) {
        impl.setAntiAliased(nativeGraphics, a);
    }

    /// Indicates whether antialiasing for text is active,
    /// notice that text antialiasing is a separate attribute from standard anti-alisaing.
    ///
    /// #### Returns
    ///
    /// true if text antialiasing is supported
    public boolean isAntiAliasedText() {
        return impl.isAntiAliasedText(nativeGraphics);
    }

    /// Set whether antialiasing for text is active,
    /// notice that text antialiasing is a separate attribute from standard anti-alisaing.
    ///
    /// #### Parameters
    ///
    /// - `a`: true if text antialiasing is supported
    public void setAntiAliasedText(boolean a) {
        impl.setAntiAliasedText(nativeGraphics, a);
    }

    /// Indicates whether the underlying implementation can draw using an affine
    /// transform hence methods such as rotate, scale and shear would work
    ///
    /// #### Returns
    ///
    /// true if an affine transformation matrix is present
    public boolean isAffineSupported() {
        return impl.isAffineSupported();
    }

    /// Resets the affine transform to the default value
    public void resetAffine() {
        impl.resetAffine(nativeGraphics);
        scaleX = 1;
        scaleY = 1;
    }

    /// Scales the coordinate system using the affine transform
    ///
    /// #### Parameters
    ///
    /// - `x`: scale factor for x
    ///
    /// - `y`: scale factor for y
    public void scale(float x, float y) {
        impl.scale(nativeGraphics, x, y);
        scaleX = x;
        scaleY = y;
    }

    /// Rotates the coordinate system around a radian angle using the affine transform
    ///
    /// #### Parameters
    ///
    /// - `angle`: the rotation angle in radians about the screen origin.
    ///
    /// #### Deprecated
    ///
    /// @deprecated The behaviour of this method is inconsistent with the rest of the API, in that it doesn't
    /// take into account the current Graphics context's translation.  Rotation is performed around the Screen's origin
    /// rather than the current Graphics context's translated origin.  Prefer to use `#rotateRadians(float)`
    /// which pivots around the context's translated origin.
    ///
    /// #### See also
    ///
    /// - #rotateRadians(float)
    public void rotate(float angle) {
        impl.rotate(nativeGraphics, angle);
    }

    /// RRotates the coordinate system around a radian angle using the affine transform
    ///
    /// #### Parameters
    ///
    /// - `angle`: the rotation angle in radians about graphics context's translated origin.
    ///
    /// #### Since
    ///
    /// 6.0
    public void rotateRadians(float angle) {
        rotateRadians(angle, 0, 0);
    }

    /// Rotates the coordinate system around a radian angle using the affine transform
    ///
    /// #### Parameters
    ///
    /// - `angle`: the rotation angle in radians
    ///
    /// - `pivotX`: the pivot point In absolute coordinates.
    ///
    /// - `pivotY`: the pivot point In absolute coordinates.
    ///
    /// #### Deprecated
    ///
    /// @deprecated The behaviour of this method is inconsistent with the rest of the API, in that the pivotX and pivotY parameters
    /// are expressed in absolute screen coordinates and don't take into account the current Graphics context's translation.  Prefer
    /// to use `int, int)` whose pivot coordinates are relative to the current translation.
    ///
    /// #### See also
    ///
    /// - #rotateRadians(float, int, int)
    public void rotate(float angle, int pivotX, int pivotY) {
        impl.rotate(nativeGraphics, angle, pivotX, pivotY);
    }

    /// Rotates the coordinate system around a radian angle using the affine transform
    ///
    /// #### Parameters
    ///
    /// - `angle`: the rotation angle in radians
    ///
    /// - `pivotX`: the pivot point relative to the current graphics context's translation.
    ///
    /// - `pivotY`: the pivot point relative to the current graphics context's translation.
    ///
    /// #### Since
    ///
    /// 6.0
    public void rotateRadians(float angle, int pivotX, int pivotY) {
        impl.rotate(nativeGraphics, angle, pivotX + xTranslate, pivotY + yTranslate);
    }

    /// Shear the graphics coordinate system using the affine transform
    ///
    /// #### Parameters
    ///
    /// - `x`: shear factor for x
    ///
    /// - `y`: shear factor for y
    public void shear(float x, float y) {
        impl.shear(nativeGraphics, x, y);
    }

    /// Starts accessing the native graphics in the underlying OS, when accessing
    /// the native graphics Codename One shouldn't be used! The native graphics is unclipped
    /// and untranslated by default and its the responsibility of the caller to clip/translate
    /// appropriately.
    ///
    /// When finished with the native graphics it is essential to **invoke endNativeGraphicsAccess**
    ///
    /// #### Returns
    ///
    /// an instance of the underlying native graphics object
    public Object beginNativeGraphicsAccess() {
        if (nativeGraphicsState != null) {
            throw new IllegalStateException("beginNativeGraphicsAccess invoked twice in a row");
        }
        Boolean a = Boolean.FALSE;
        Boolean b = Boolean.FALSE;
        if (isAntiAliasedText()) {
            b = Boolean.TRUE;
        }
        if (isAntiAliased()) {
            a = Boolean.TRUE;
        }

        nativeGraphicsState = new Object[]{
                Integer.valueOf(getTranslateX()),
                Integer.valueOf(getTranslateY()),
                Integer.valueOf(getColor()),
                Integer.valueOf(getAlpha()),
                Integer.valueOf(getClipX()),
                Integer.valueOf(getClipY()),
                Integer.valueOf(getClipWidth()),
                Integer.valueOf(getClipHeight()),
                a, b
        };
        translate(-getTranslateX(), -getTranslateY());
        setAlpha(255);
        setClip(0, 0, Display.getInstance().getDisplayWidth(), Display.getInstance().getDisplayHeight());
        return nativeGraphics;
    }

    /// Invoke this to restore Codename One's graphics settings into the native graphics
    public void endNativeGraphicsAccess() {
        translate(((Integer) nativeGraphicsState[0]).intValue(), ((Integer) nativeGraphicsState[1]).intValue());
        setColor(((Integer) nativeGraphicsState[2]).intValue());
        setAlpha(((Integer) nativeGraphicsState[3]).intValue());
        setClip(((Integer) nativeGraphicsState[4]).intValue(),
                ((Integer) nativeGraphicsState[5]).intValue(),
                ((Integer) nativeGraphicsState[6]).intValue(),
                ((Integer) nativeGraphicsState[7]).intValue());
        setAntiAliased(((Boolean) nativeGraphicsState[8]).booleanValue());
        setAntiAliasedText(((Boolean) nativeGraphicsState[9]).booleanValue());
        nativeGraphicsState = null;
    }

    /// Allows an implementation to optimize image tiling rendering logic
    ///
    /// #### Parameters
    ///
    /// - `img`: the image
    ///
    /// - `x`: coordinate to tile the image along
    ///
    /// - `y`: coordinate to tile the image along
    ///
    /// - `w`: coordinate to tile the image along
    ///
    /// - `h`: coordinate to tile the image along
    public void tileImage(Image img, int x, int y, int w, int h) {
        if (img.requiresDrawImage()) {
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
                    if (actualX > clipX + clipW) {
                        continue;
                    }
                    if (actualX + iW < clipX) {
                        continue;
                    }
                    if (actualY > clipY + clipH) {
                        continue;
                    }
                    if (actualY + iH < clipY) {
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

    /// Returns the affine X scale
    ///
    /// #### Returns
    ///
    /// the current scale
    public float getScaleX() {
        return scaleX;
    }

    /// Returns the affine Y scale
    ///
    /// #### Returns
    ///
    /// the current scale
    public float getScaleY() {
        return scaleY;
    }

    /// Draws a peer component.  This doesn't actually draw anything, it just activates
    /// the front graphics buffer and begins redirecting drawing operations to that buffer.
    ///
    /// This is only used on platforms where `CodenameOneImplementation#isFrontGraphicsSupported()` is enabled.
    ///
    /// #### Parameters
    ///
    /// - `peer`: The peer component to be drawn.
    void drawPeerComponent(PeerComponent peer) {
        if (paintPeersBehind) {
            clearRectImpl(peer.getAbsoluteX(), peer.getAbsoluteY(), peer.getWidth(), peer.getHeight());
        }

    }

    /// Gets the current rendering hints for this context.
    ///
    /// #### Returns
    ///
    /// The rendering hints.
    ///
    /// #### See also
    ///
    /// - #RENDERING_HINT_FAST
    public int getRenderingHints() {
        return impl.getRenderingHints(nativeGraphics);
    }

    /// Sets rendering hints for this context.
    ///
    /// #### Parameters
    ///
    /// - `hints`: int of rendering hints produced by logical AND on all applicable hints.
    ///
    /// #### Since
    ///
    /// 7.0
    ///
    /// #### See also
    ///
    /// - #RENDERING_HINT_FAST
    ///
    /// - #getRenderingHints()
    public void setRenderingHints(int hints) {
        impl.setRenderingHints(nativeGraphics, hints);
    }
}
