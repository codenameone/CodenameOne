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

import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.ImageFactory;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;

/// A border that can either be a circle or a circular rectangle which is a rectangle whose sides are circles.
/// This border can optionally have a drop shadow associated with it.
///
/// **IMPORTANT:** `RoundRectBorder` instances can't be reused
/// you would need to create a separate instance for each style object!
/// See [this issue](https://github.com/codenameone/CodenameOne/issues/2578#issuecomment-429554441) for further details.
///
/// ```java
/// Form hi = new Form("Round", new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER));
///
/// Button ok = new Button("OK");
/// Button cancel = new Button("Cancel");
///
/// Label loginLabel = new Label("Login", "Container");
/// loginLabel.getAllStyles().setAlignment(Component.CENTER);
///
/// Label passwordLabel = new Label("Password", "Container");
/// passwordLabel.getAllStyles().setAlignment(Component.CENTER);
///
/// TextField login = new TextField("", "Login", 20, TextArea.ANY);
/// TextField password = new TextField("", "Password", 20, TextArea.PASSWORD);
/// Style loginStyle = login.getAllStyles();
/// Stroke borderStroke = new Stroke(2, Stroke.CAP_SQUARE, Stroke.JOIN_MITER, 1);
/// loginStyle.setBorder(RoundBorder.create().
///         rectangle(true).
///         color(0xffffff).
///         strokeColor(0).
///         strokeOpacity(120).
///         stroke(borderStroke));
/// loginStyle.setMarginUnit(Style.UNIT_TYPE_DIPS);
/// loginStyle.setMargin(Component.BOTTOM, 3);
/// Style passwordStyle = password.getAllStyles();
/// passwordStyle.setBorder(RoundBorder.create().
///         rectangle(true).
///         color(0xffffff).
///         strokeColor(0).
///         strokeOpacity(120).
///         stroke(borderStroke));
///
/// Container box = BoxLayout.encloseY(
///         loginLabel,
///         login,
///         passwordLabel,
///         password,
///             GridLayout.encloseIn(2, cancel, ok));
///
/// Button closeButton = new Button();
/// Style closeStyle = closeButton.getAllStyles();
/// closeStyle.setFgColor(0xffffff);
/// closeStyle.setBgTransparency(0);
/// closeStyle.setPaddingUnit(Style.UNIT_TYPE_DIPS);
/// closeStyle.setPadding(3, 3, 3, 3);
/// closeStyle.setBorder(RoundBorder.create().shadowOpacity(100));
/// FontImage.setMaterialIcon(closeButton, FontImage.MATERIAL_CLOSE);
///
/// Container layers = LayeredLayout.encloseIn(box, FlowLayout.encloseRight(closeButton));
/// Style boxStyle = box.getUnselectedStyle();
/// boxStyle.setBgTransparency(255);
/// boxStyle.setBgColor(0xeeeeee);
/// boxStyle.setMarginUnit(Style.UNIT_TYPE_DIPS);
/// boxStyle.setPaddingUnit(Style.UNIT_TYPE_DIPS);
/// boxStyle.setMargin(4, 3, 3, 3);
/// boxStyle.setPadding(2, 2, 2, 2);
///
/// hi.add(BorderLayout.CENTER, layers);
///
/// hi.show();
/// ```
///
/// @author Shai Almog
public final class RoundBorder extends Border {
    private static final String CACHE_KEY = "cn1$$-rbcache";
    // these allow us to have more than one border per component in cache which is important for selected/unselected/pressed values
    private static int instanceCounter;
    private final int instanceVal;
    private long modificationTime;
    /// The color of the border background
    private int color = 0xd32f2f;
    /// The opacity (transparency) of the border background
    private int opacity = 255;
    /// The color of the edge of the border if applicable
    private int strokeColor;
    /// The opacity of the edge of the border if applicable
    private int strokeOpacity = 255;
    private Stroke stroke;
    /// The thickness of the edge of the border if applicable, 0 if no stroke is needed
    private float strokeThickness;
    /// True if the thickness of the stroke is in millimeters
    private boolean strokeMM;
    /// The spread of the shadow in pixels of millimeters
    private int shadowSpread;
    /// The opacity of the shadow between 0 and 255
    private int shadowOpacity = 0;
    /// X axis bias of the shadow between 0 and 1 where 0 is to the top and 1 is to the bottom, defaults to 0.5
    private float shadowX = 0.5f;
    /// Y axis bias of the shadow between 0 and 1 where 0 is to the left and 1 is to the right, defaults to 0.5
    private float shadowY = 0.5f;
    /// The Gaussian blur size
    private float shadowBlur = 10;
    /// True if the shadow spread is in millimeters
    private boolean shadowMM;
    /// True if this border grows into a rectangle horizontally or keeps growing as a circle
    private boolean rectangle;
    /// Forces a special case of the rectangle mode that renders the right side as
    /// square. This is ignored when the rectangle mode is false
    private boolean onlyLeftRounded;
    /// Forces a special case of the rectangle mode that renders the left side as
    /// square. This is ignored when the rectangle mode is false
    private boolean onlyRightRounded;
    private boolean uiid;

    /// This is useful for showing an Uber like stroke effect progress bar
    private int strokeAngle = 360;

    private RoundBorder() {
        shadowSpread = Display.getInstance().convertToPixels(2);
        instanceCounter++;
        instanceVal = instanceCounter;
    }

    /// Creates a flat round border with no stroke and no shadow and the default color, this call can
    /// be chained with the other calls to mutate the color/opacity etc.
    ///
    /// #### Returns
    ///
    /// a border instance
    public static RoundBorder create() {
        return new RoundBorder();
    }

    /// Uses the style of the components UIID to draw the background of the border, this effectively overrides all
    /// other style settings but allows the full power of UIID drawing including gradients, background images
    /// etc.
    ///
    /// **Notice: **this flag will only work when shaped clipping is supported. That feature
    /// isn't available in all platforms...
    ///
    /// #### Parameters
    ///
    /// - `uiid`: true to use the background of the component setting
    ///
    /// #### Returns
    ///
    /// border instance so these calls can be chained
    public RoundBorder uiid(boolean uiid) {
        this.uiid = uiid;
        modificationTime = System.currentTimeMillis();
        return this;
    }

    /// True is we use the background of the component setting to draw
    ///
    /// #### Returns
    ///
    /// true if we draw based on the component UIID
    public boolean getUIID() {
        return uiid;
    }

    /// Sets the background color of the circle/rectangle
    ///
    /// #### Parameters
    ///
    /// - `color`: the color
    ///
    /// #### Returns
    ///
    /// border instance so these calls can be chained
    public RoundBorder color(int color) {
        this.color = color;
        modificationTime = System.currentTimeMillis();
        return this;
    }

    /// Sets the background opacity of the circle/rectangle
    ///
    /// #### Parameters
    ///
    /// - `opacity`: the background opacity from 0-255 where 255 is completely opaque
    ///
    /// #### Returns
    ///
    /// border instance so these calls can be chained
    public RoundBorder opacity(int opacity) {
        this.opacity = opacity;
        modificationTime = System.currentTimeMillis();
        return this;
    }

    /// Sets the opacity of the stroke line around the circle/rectangle
    ///
    /// #### Parameters
    ///
    /// - `strokeOpacity`: the opacity from 0-255 where 255 is completely opaque
    ///
    /// #### Returns
    ///
    /// border instance so these calls can be chained
    public RoundBorder strokeOpacity(int strokeOpacity) {
        this.strokeOpacity = strokeOpacity;
        modificationTime = System.currentTimeMillis();
        return this;
    }

    /// Sets the stroke color of the circle/rectangle
    ///
    /// #### Parameters
    ///
    /// - `strokeColor`: the color
    ///
    /// #### Returns
    ///
    /// border instance so these calls can be chained
    public RoundBorder strokeColor(int strokeColor) {
        this.strokeColor = strokeColor;
        modificationTime = System.currentTimeMillis();
        return this;
    }

    /// Sets the stroke of the circle/rectangle
    ///
    /// #### Parameters
    ///
    /// - `stroke`: the stroke object
    ///
    /// #### Returns
    ///
    /// border instance so these calls can be chained
    public RoundBorder stroke(Stroke stroke) {
        this.stroke = stroke;
        modificationTime = System.currentTimeMillis();
        return this;
    }

    /// Sets the stroke of the circle/rectangle
    ///
    /// #### Parameters
    ///
    /// - `stroke`: the thickness of the stroke object
    ///
    /// - `mm`: set to true to indicate the value is in millimeters, false indicates pixels
    ///
    /// #### Returns
    ///
    /// border instance so these calls can be chained
    public RoundBorder stroke(float stroke, boolean mm) {
        strokeThickness = stroke;
        if (strokeThickness == 0) {
            this.stroke = null;
            return this;
        }
        strokeMM = mm;
        if (mm) {
            stroke = Display.getInstance().convertToPixels(stroke);
        }
        return stroke(new Stroke(stroke, Stroke.CAP_SQUARE, Stroke.JOIN_MITER, 1));
    }

    /// Sets the stroke angle of the circle, this only applies to circular versions
    ///
    /// #### Parameters
    ///
    /// - `strokeAngle`: the stroke angle in degrees
    ///
    /// #### Returns
    ///
    /// border instance so these calls can be chained
    public RoundBorder strokeAngle(int strokeAngle) {
        this.strokeAngle = strokeAngle;
        modificationTime = System.currentTimeMillis();
        return this;
    }

    /// Sets the spread in pixels of the shadow i.e how much bigger is it than the actual circle/rectangle
    ///
    /// #### Parameters
    ///
    /// - `shadowSpread`: the amount in pixels representing the size of the shadow
    ///
    /// - `mm`: set to true to indicate the value is in millimeters, false indicates pixels
    ///
    /// #### Returns
    ///
    /// border instance so these calls can be chained
    public RoundBorder shadowSpread(int shadowSpread, boolean mm) {
        this.shadowMM = mm;
        this.shadowSpread = shadowSpread;
        modificationTime = System.currentTimeMillis();
        return this;
    }

    /// Sets the spread in pixels of the shadow i.e how much bigger is it than the actual circle/rectangle
    ///
    /// #### Parameters
    ///
    /// - `shadowSpread`: the amount in pixels representing the size of the shadow
    ///
    /// #### Returns
    ///
    /// border instance so these calls can be chained
    public RoundBorder shadowSpread(int shadowSpread) {
        this.shadowSpread = shadowSpread;
        modificationTime = System.currentTimeMillis();
        return this;
    }

    /// Sets the opacity of the shadow from 0 - 255 where 0 means no shadow and 255 means opaque black shadow
    ///
    /// #### Parameters
    ///
    /// - `shadowOpacity`: the opacity of the shadow
    ///
    /// #### Returns
    ///
    /// border instance so these calls can be chained
    public RoundBorder shadowOpacity(int shadowOpacity) {
        this.shadowOpacity = shadowOpacity;
        modificationTime = System.currentTimeMillis();
        return this;
    }

    /// The position of the shadow on the X axis where 0.5f means the center and higher values draw it to the right side
    ///
    /// #### Parameters
    ///
    /// - `shadowX`: the position of the shadow between 0 - 1 where 0 equals left and 1 equals right
    ///
    /// #### Returns
    ///
    /// border instance so these calls can be chained
    public RoundBorder shadowX(float shadowX) {
        this.shadowX = shadowX;
        modificationTime = System.currentTimeMillis();
        return this;
    }

    /// The position of the shadow on the Y axis where 0.5f means the center and higher values draw it to the bottom
    ///
    /// #### Parameters
    ///
    /// - `shadowY`: the position of the shadow between 0 - 1 where 0 equals top and 1 equals bottom
    ///
    /// #### Returns
    ///
    /// border instance so these calls can be chained
    public RoundBorder shadowY(float shadowY) {
        this.shadowY = shadowY;
        modificationTime = System.currentTimeMillis();
        return this;
    }

    /// The blur on the shadow this is the standard Gaussian blur radius
    ///
    /// #### Parameters
    ///
    /// - `shadowBlur`: The blur on the shadow this is the standard Gaussian blur radius
    ///
    /// #### Returns
    ///
    /// border instance so these calls can be chained
    public RoundBorder shadowBlur(float shadowBlur) {
        this.shadowBlur = shadowBlur;
        modificationTime = System.currentTimeMillis();
        return this;
    }

    /// When set to true this border grows into a rectangle when the space isn't perfectly circular
    ///
    /// #### Parameters
    ///
    /// - `rectangle`: When set to true this border grows into a rectangle when the space isn't perfectly circular
    ///
    /// #### Returns
    ///
    /// border instance so these calls can be chained
    public RoundBorder rectangle(boolean rectangle) {
        this.rectangle = rectangle;
        modificationTime = System.currentTimeMillis();
        return this;
    }

    /// Forces a special case of the rectangle mode that renders the right side as
    /// square. This is ignored when the rectangle mode is false
    ///
    /// #### Parameters
    ///
    /// - `onlyLeftRounded`: the new state of this mode
    ///
    /// #### Returns
    ///
    /// border instance so these calls can be chained
    public RoundBorder onlyLeftRounded(boolean onlyLeftRounded) {
        this.onlyLeftRounded = onlyLeftRounded;
        return this;
    }


    /// Checks if only left side is rounded.
    ///
    /// #### Returns
    ///
    /// True if only left side is rounded.
    ///
    /// #### Since
    ///
    /// 7.0
    public boolean isOnlyLeftRounded() {
        return onlyLeftRounded;
    }

    /// Forces a special case of the rectangle mode that renders the left side as
    /// square. This is ignored when the rectangle mode is false
    ///
    /// #### Parameters
    ///
    /// - `onlyRightRounded`: the new state of this mode
    ///
    /// #### Returns
    ///
    /// border instance so these calls can be chained
    public RoundBorder onlyRightRounded(boolean onlyRightRounded) {
        this.onlyRightRounded = onlyRightRounded;
        return this;
    }

    /// Checks if only right side is rounded.
    ///
    /// #### Returns
    ///
    /// True if only right side is rounded.
    ///
    /// #### Since
    ///
    /// 7.0
    public boolean isOnlyRightRounded() {
        return onlyRightRounded;
    }


    private Image createTargetImage(Component c, int w, int h, boolean fast) {
        Image target = ImageFactory.createImage(c, w, h, 0);

        int shapeX = 0;
        int shapeY = 0;
        int shapeW = w;
        int shapeH = h;

        Graphics tg = target.getGraphics();
        tg.setAntiAliased(true);

        int shadowSpreadL = shadowSpread;
        if (shadowMM) {
            shadowSpreadL = Display.getInstance().convertToPixels(shadowSpreadL);
        }

        if (shadowOpacity > 0) {
            shapeW -= shadowSpreadL;
            shapeW -= (shadowBlur / 2);
            shapeH -= shadowSpreadL;
            shapeH -= (shadowBlur / 2);
            shapeX += Math.round((shadowSpreadL + (shadowBlur / 2)) * shadowX);
            shapeY += Math.round((shadowSpreadL + (shadowBlur / 2)) * shadowY);

            // draw a gradient of sort for the shadow
            for (int iter = shadowSpreadL - 1; iter >= 0; iter--) {
                tg.translate(iter, iter);
                fillShape(tg, 0, shadowOpacity / shadowSpreadL, w - (iter * 2), h - (iter * 2), false);
                tg.translate(-iter, -iter);
            }
            if (Display.getInstance().isGaussianBlurSupported() && !fast) {
                Image blured = Display.getInstance().gaussianBlurImage(target, shadowBlur / 2);
                target = ImageFactory.createImage(c, w, h, 0);
                tg = target.getGraphics();
                tg.drawImage(blured, 0, 0);
                tg.setAntiAliased(true);
            }
        }
        tg.translate(shapeX, shapeY);
        if (uiid && tg.isShapeClipSupported()) {
            c.getStyle().setBorder(Border.createEmpty());

            GeneralPath gp = new GeneralPath();
            if (rectangle) {
                float sw = this.stroke != null ? this.stroke.getLineWidth() : 0;
                gp.moveTo(shapeH / 2.0, sw);
                if (onlyLeftRounded) {
                    gp.lineTo(shapeW, sw);
                    gp.lineTo(shapeW, shapeH - sw);
                } else {
                    gp.lineTo(shapeW - (shapeH / 2.0), sw);
                    gp.arcTo(shapeW - (shapeH / 2.0), shapeH / 2.0, shapeW - (shapeH / 2.0), shapeH - sw, true);
                }
                if (onlyRightRounded) {
                    gp.lineTo(sw, shapeH - sw);
                    gp.lineTo(sw, sw);
                } else {
                    gp.lineTo(shapeH / 2.0, shapeH - sw);
                    gp.arcTo(shapeH / 2.0, shapeH / 2.0, shapeH / 2.0, sw, true);
                }
                gp.closePath();
            } else {
                int size = shapeW;
                int xPos = 0;
                int yPos = 0;
                if (shapeW != shapeH) {
                    if (shapeW > shapeH) {
                        size = shapeH;
                        xPos = (shapeW - shapeH) / 2;
                    } else {
                        size = shapeW;
                        yPos = (shapeH - shapeW) / 2;
                    }
                }
                gp.arc(xPos, yPos, size, size, 0, 2 * Math.PI);
            }

            tg.setClip(gp);
            c.getStyle().getBgPainter().paint(tg, new Rectangle(0, 0, w, h));
            c.getStyle().setBorder(this);
            if (strokeOpacity > 0 && this.stroke != null) {
                tg.setColor(strokeColor);
                tg.setAlpha(strokeOpacity);
                tg.setAntiAliased(true);
                tg.drawShape(gp, stroke);
            }
        } else {
            fillShape(tg, color, opacity, shapeW, shapeH, true);
        }
        return target;
    }

    @Override
    public void paintBorderBackground(Graphics g, final Component c) {
        final int w = c.getWidth();
        final int h = c.getHeight();
        int x = c.getX();
        int y = c.getY();
        if (w > 0 && h > 0) {
            Object k = c.getClientProperty(CACHE_KEY + instanceVal);
            if (k instanceof CacheValue) {
                CacheValue val = (CacheValue) k;
                if (val.modificationTime == modificationTime &&
                        val.img.getWidth() == w && val.img.getHeight() == h) {
                    g.drawImage(val.img, x, y);
                    return;
                }
            }
        } else {
            return;
        }

        Image target = createTargetImage(c, w, h, true);
        g.drawImage(target, x, y);
        c.putClientProperty(CACHE_KEY + instanceVal, new CacheValue(target, modificationTime));

        // update the cache with a more refined version and repaint
        Display.getInstance().callSeriallyOnIdle(new Runnable() {
            @Override
            public void run() {
                if (w == c.getWidth() && h == c.getHeight()) {
                    Image target = createTargetImage(c, w, h, false);
                    c.putClientProperty(CACHE_KEY + instanceVal, new CacheValue(target, modificationTime));
                    c.repaint();
                }
            }
        });
    }

    @Override
    public int getMinimumHeight() {
        return shadowSpread + Math.round(shadowBlur) + Display.getInstance().convertToPixels(1);
    }

    @Override
    public int getMinimumWidth() {
        return shadowSpread + Math.round(shadowBlur) + Display.getInstance().convertToPixels(1);
    }


    private void fillShape(Graphics g, int color, int opacity, int width, int height, boolean stroke) {
        g.setColor(color);
        g.setAlpha(opacity);
        if (!rectangle || width <= height) {

            int x = 0;
            int y = 0;
            int size = width;
            if (width != height) {
                if (width > height) {
                    size = height;
                    x = (width - height) / 2;
                } else {
                    size = width;
                    y = (height - width) / 2;
                }
            }
            if (size < 5) {
                // probably won't be visible anyway so do nothing, otherwise it might throw an exception
                return;
            }
            if (stroke && this.stroke != null) {
                int sw = (int) Math.ceil(this.stroke.getLineWidth());
                GeneralPath arc = new GeneralPath();
                arc.arc(x + sw / 2, y + sw / 2, size - sw, size - sw, 0, 2 * Math.PI);
                g.fillShape(arc);
                g.setColor(strokeColor);
                g.setAlpha(strokeOpacity);
                if (strokeAngle != 360) {
                    arc = new GeneralPath();
                    arc.arc(x + sw / 2, y + sw / 2, size - sw, size - sw, Math.PI / 2, -Math.toRadians(strokeAngle));
                }
                g.drawShape(arc, this.stroke);
            } else {
                g.fillArc(x, y, size, size, 0, 360);
            }
        } else {
            GeneralPath gp = new GeneralPath();
            float sw = (stroke && this.stroke != null) ? this.stroke.getLineWidth() : 0;
            gp.moveTo(height / 2.0, sw);
            if (onlyLeftRounded) {
                gp.lineTo(width, sw);
                gp.lineTo(width, height - sw);
            } else {
                gp.lineTo(width - (height / 2.0), sw);
                gp.arcTo(width - (height / 2.0), height / 2.0, width - (height / 2.0), height - sw, true);
            }
            if (onlyRightRounded) {
                gp.lineTo(sw, height - sw);
                gp.lineTo(sw, sw);
            } else {
                gp.lineTo(height / 2.0, height - sw);
                gp.arcTo(height / 2.0, height / 2.0, height / 2.0, sw, true);
            }
            gp.closePath();
            g.fillShape(gp);
            if (stroke && this.stroke != null) {
                g.setAlpha(strokeOpacity);
                g.setColor(strokeColor);
                g.drawShape(gp, this.stroke);
            }
        }
    }

    @Override
    public boolean isBackgroundPainter() {
        return true;
    }

    /// The color of the border background
    ///
    /// #### Returns
    ///
    /// the color
    public int getColor() {
        return color;
    }

    /// The opacity (transparency) of the border background
    ///
    /// #### Returns
    ///
    /// the opacity
    public int getOpacity() {
        return opacity;
    }

    /// The color of the edge of the border if applicable
    ///
    /// #### Returns
    ///
    /// the strokeColor
    public int getStrokeColor() {
        return strokeColor;
    }

    /// The opacity of the edge of the border if applicable
    ///
    /// #### Returns
    ///
    /// the strokeOpacity
    public int getStrokeOpacity() {
        return strokeOpacity;
    }

    /// The thickness of the edge of the border if applicable, 0 if no stroke is needed
    ///
    /// #### Returns
    ///
    /// the strokeThickness
    public float getStrokeThickness() {
        return strokeThickness;
    }

    /// True if the thickness of the stroke is in millimeters
    ///
    /// #### Returns
    ///
    /// the strokeMM
    public boolean isStrokeMM() {
        return strokeMM;
    }

    /// The spread of the shadow in pixels of millimeters
    ///
    /// #### Returns
    ///
    /// the shadowSpread
    public int getShadowSpread() {
        return shadowSpread;
    }

    /// The opacity of the shadow between 0 and 255
    ///
    /// #### Returns
    ///
    /// the shadowOpacity
    public int getShadowOpacity() {
        return shadowOpacity;
    }

    /// X axis bias of the shadow between 0 and 1 where 0 is to the top and 1 is to the bottom, defaults to 0.5
    ///
    /// #### Returns
    ///
    /// the shadowX
    public float getShadowX() {
        return shadowX;
    }

    /// Y axis bias of the shadow between 0 and 1 where 0 is to the left and 1 is to the right, defaults to 0.5
    ///
    /// #### Returns
    ///
    /// the shadowY
    public float getShadowY() {
        return shadowY;
    }

    /// The Gaussian blur size
    ///
    /// #### Returns
    ///
    /// the shadowBlur
    public float getShadowBlur() {
        return shadowBlur;
    }

    /// True if the shadow spread is in millimeters
    ///
    /// #### Returns
    ///
    /// the shadowMM
    public boolean isShadowMM() {
        return shadowMM;
    }

    /// True if this border grows into a rectangle horizontally or keeps growing as a circle
    ///
    /// #### Returns
    ///
    /// the rectangle
    public boolean isRectangle() {
        return rectangle;
    }


    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    static class CacheValue {
        Image img;
        long modificationTime;

        public CacheValue() {
        }

        public CacheValue(Image img, long modificationTime) {
            this.img = img;
            this.modificationTime = modificationTime;
        }
    }
}
