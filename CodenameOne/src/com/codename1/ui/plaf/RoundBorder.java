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
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;

/**
 * <p>A border that can either be a circle or a circular rectangle which is a rectangle whose sides are circles.
 * This border can optionally have a drop shadow associated with it.</p>
 * <script src="https://gist.github.com/codenameone/3e91e5eab4e677e6b03962e78ae99e07.js"></script>
 * <img src="https://www.codenameone.com/img/blog/round-border.png" alt="Round Border" />
 * 
 *
 * @author Shai Almog
 */
public class RoundBorder extends Border {
    private static final String CACHE_KEY = "cn1$$-rbcache";
    
    /**
     * The color of the border background
     */
    private int color = 0xd32f2f;
    
    /**
     * The opacity (transparency) of the border background
     */
    private int opacity = 255;
    
    /**
     * The color of the edge of the border if applicable
     */
    private int strokeColor;
    
    /**
     * The opacity of the edge of the border if applicable
     */
    private int strokeOpacity = 255;

    private Stroke stroke;

    
    /**
     * The thickness of the edge of the border if applicable, 0 if no stroke is needed
     */
    private float strokeThickness;

    /**
     * True if the thickness of the stroke is in millimeters
     */
    private boolean strokeMM;

    /**
     * The spread of the shadow in pixels of millimeters
     */
    private int shadowSpread;

    /**
     * The opacity of the shadow between 0 and 255
     */
    private int shadowOpacity = 0;

    /**
     * X axis bias of the shadow between 0 and 1 where 0 is to the top and 1 is to the bottom, defaults to 0.5
     */
    private float shadowX = 0.5f;

    /**
     * Y axis bias of the shadow between 0 and 1 where 0 is to the left and 1 is to the right, defaults to 0.5
     */
    private float shadowY = 0.5f;

    /**
     * The Gaussian blur size
     */
    private float shadowBlur = 10;

    /**
     * True if the shadow spread is in millimeters
     */
    private boolean shadowMM;

    /**
     * True if this border grows into a rectangle horizontally or keeps growing as a circle
     */
    private boolean rectangle;
    
    // these allow us to have more than one border per component in cache which is important for selected/unselected/pressed values
    private static int instanceCounter;
    private final int instanceVal;
    
    private boolean uiid;
    
    private RoundBorder() {
        shadowSpread = Display.getInstance().convertToPixels(2);
        instanceCounter++;
        instanceVal = instanceCounter;
    }
    
    /**
     * Creates a flat round border with no stroke and no shadow and the default color, this call can
     * be chained with the other calls to mutate the color/opacity etc.
     * @return a border instance
     */
    public static RoundBorder create() {
        return new RoundBorder();
    }
    
    /**
     * <p>Uses the style of the components UIID to draw the background of the border, this effectively overrides all
     * other style settings but allows the full power of UIID drawing including gradients, background images 
     * etc.</p>
     * <p><strong>Notice: </strong>this flag will only work when shaped clipping is supported. That feature
     * isn't available in all platforms...</p>
     * 
     * 
     * @param uiid true to use the background of the component setting
     * @return border instance so these calls can be chained
     */
    public RoundBorder uiid(boolean uiid) {
        this.uiid = uiid;
        return this;
    }
    
    /**
     * True is we use the background of the component setting to draw
     * @return true if we draw based on the component UIID
     */
    public boolean getUIID() {
        return uiid;
    }
    
    /**
     * Sets the background color of the circle/rectangle
     * @param color the color 
     * @return border instance so these calls can be chained
     */
    public RoundBorder color(int color) {
        this.color = color;
        return this;
    }

    /**
     * Sets the background opacity of the circle/rectangle
     * @param opacity the background opacity from 0-255 where 255 is completely opaque 
     * @return border instance so these calls can be chained
     */
    public RoundBorder opacity(int opacity) {
        this.opacity = opacity;
        return this;
    }

    /**
     * Sets the opacity of the stroke line around the circle/rectangle
     * @param strokeOpacity the opacity from 0-255 where 255 is completely opaque 
     * @return border instance so these calls can be chained
     */
    public RoundBorder strokeOpacity(int strokeOpacity) {
        this.strokeOpacity = strokeOpacity;
        return this;
    }
    
    /**
     * Sets the stroke color of the circle/rectangle
     * @param strokeColor the color 
     * @return border instance so these calls can be chained
     */
    public RoundBorder strokeColor(int strokeColor) {
        this.strokeColor = strokeColor;
        return this;
    }

    /**
     * Sets the stroke of the circle/rectangle
     * @param stroke the stroke object
     * @return border instance so these calls can be chained
     */
    public RoundBorder stroke(Stroke stroke) {
        this.stroke = stroke;
        return this;
    }

    /**
     * Sets the stroke of the circle/rectangle
     * @param stroke the thickness of the stroke object
     * @param mm set to true to indicate the value is in millimeters, false indicates pixels
     * @return border instance so these calls can be chained
     */
    public RoundBorder stroke(float stroke, boolean mm) {
        strokeThickness = stroke;
        if(strokeThickness == 0) {
            this.stroke = null;
            return this;
        }
        strokeMM = mm;
        if(mm) {
            stroke = Display.getInstance().convertToPixels(stroke);
        }
        return stroke(new Stroke(stroke, Stroke.CAP_SQUARE, Stroke.JOIN_MITER, 1));
    }
    
    /**
     * Sets the spread in pixels of the shadow i.e how much bigger is it than the actual circle/rectangle
     * @param shadowSpread the amount in pixels representing the size of the shadow
     * @param mm set to true to indicate the value is in millimeters, false indicates pixels
     * @return border instance so these calls can be chained
     */
    public RoundBorder shadowSpread(int shadowSpread, boolean mm) {
        this.shadowMM = mm;
        this.shadowSpread = shadowSpread;
        return this;
    }

    /**
     * Sets the spread in pixels of the shadow i.e how much bigger is it than the actual circle/rectangle
     * @param shadowSpread the amount in pixels representing the size of the shadow
     * @return border instance so these calls can be chained
     */
    public RoundBorder shadowSpread(int shadowSpread) {
        this.shadowSpread = shadowSpread;
        return this;
    }

    /**
     * Sets the opacity of the shadow from 0 - 255 where 0 means no shadow and 255 means opaque black shadow
     * @param shadowOpacity the opacity of the shadow
     * @return border instance so these calls can be chained
     */
    public RoundBorder shadowOpacity(int shadowOpacity) {
        this.shadowOpacity = shadowOpacity;
        return this;
    }

    /**
     * The position of the shadow on the X axis where 0.5f means the center and higher values draw it to the right side
     * @param shadowX the position of the shadow between 0 - 1 where 0 equals left and 1 equals right
     * @return border instance so these calls can be chained
     */
    public RoundBorder shadowX(float shadowX) {
        this.shadowX = shadowX;
        return this;
    }

    /**
     * The position of the shadow on the Y axis where 0.5f means the center and higher values draw it to the bottom
     * @param shadowY the position of the shadow between 0 - 1 where 0 equals top and 1 equals bottom
     * @return border instance so these calls can be chained
     */
    public RoundBorder shadowY(float shadowY) {
        this.shadowY = shadowY;
        return this;
    }

    /**
     * The blur on the shadow this is the standard Gaussian blur radius
     * @param shadowBlur The blur on the shadow this is the standard Gaussian blur radius
     * @return border instance so these calls can be chained
     */
    public RoundBorder shadowBlur(float shadowBlur) {
        this.shadowBlur = shadowBlur;
        return this;
    }

    /**
     * When set to true this border grows into a rectangle when the space isn't perfectly circular
     * @param rectangle When set to true this border grows into a rectangle when the space isn't perfectly circular
     * @return border instance so these calls can be chained
     */
    public RoundBorder rectangle(boolean rectangle) {
        this.rectangle = rectangle;
        return this;
    }
        
    @Override
    public void paintBorderBackground(Graphics g, Component c) {
        int w = c.getWidth();
        int h = c.getHeight();
        int x = c.getX();
        int y = c.getY();
        if(w > 0 && h > 0) {
            Image background = (Image)c.getClientProperty(CACHE_KEY + instanceVal);
            if(background != null && background.getWidth() == w && background.getHeight() == h) {
                g.drawImage(background, x, y);
                return;
            }
        } else {
            return;
        }
                
        Image target = Image.createImage(w, h, 0);
        
        int shapeX = 0;
        int shapeY = 0;
        int shapeW = w;
        int shapeH = h;
        
        Graphics tg = target.getGraphics();
        tg.setAntiAliased(true);
                
        int shadowSpreadL =  shadowSpread;
        if(shadowMM) {
            shadowSpreadL = Display.getInstance().convertToPixels(shadowSpreadL);
        }
        
        if(shadowOpacity > 0) {
            shapeW -= shadowSpreadL;
            shapeW -= (shadowBlur / 2);
            shapeH -= shadowSpreadL;
            shapeH -= (shadowBlur / 2);
            shapeX += Math.round((shadowSpreadL + (shadowBlur / 2)) * shadowX);
            shapeY += Math.round((shadowSpreadL + (shadowBlur / 2)) * shadowY);
            
            // draw a gradient of sort for the shadow
            for(int iter = shadowSpreadL - 1 ; iter >= 0 ; iter--) {            
                tg.translate(iter, iter);
                fillShape(tg, 0, shadowOpacity / shadowSpreadL, w - (iter * 2), h - (iter * 2), false);
                tg.translate(-iter, -iter);
            }
            if(Display.getInstance().isGaussianBlurSupported()) {
                Image blured = Display.getInstance().gaussianBlurImage(target, shadowBlur/2);
                target = Image.createImage(w, h, 0);
                tg = target.getGraphics();
                tg.drawImage(blured, 0, 0);
                tg.setAntiAliased(true);
            }
        }
        tg.translate(shapeX, shapeY);
        if(uiid && tg.isShapeClipSupported()) {
            c.getStyle().setBorder(Border.createEmpty());
            
            GeneralPath gp = new GeneralPath();
            if(rectangle) {
                float sw = this.stroke != null ? this.stroke.getLineWidth() : 0;
                gp.moveTo(shapeH / 2.0, sw);
                gp.lineTo(shapeW - (shapeH / 2.0), sw);
                gp.arcTo(shapeW - (shapeH / 2.0), shapeH / 2.0, shapeW - (shapeH / 2.0), shapeH-sw, true);
                gp.lineTo(shapeH / 2.0, shapeH-sw);
                gp.arcTo(shapeH / 2.0, shapeH / 2.0, shapeH / 2.0, sw, true);
                gp.closePath();
            } else {
                int size = shapeW;
                int xPos = 0;
                int yPos = 0;
                if(shapeW != shapeH) {
                    if(shapeW > shapeH) {
                        size = shapeH;
                        xPos = (shapeW - shapeH) / 2;
                    } else {
                        size = shapeW;
                        yPos = (shapeH - shapeW) / 2;
                    }
                }
                gp.arc(xPos, yPos, size, size, 0, 2*Math.PI);
            }
            
            tg.setClip(gp);
            c.getStyle().getBgPainter().paint(tg, new Rectangle(0, 0, w, h));
            c.getStyle().setBorder(this);
        } else {
            fillShape(tg, color, opacity, shapeW, shapeH, true);
        }
        g.drawImage(target, x, y);
        c.putClientProperty(CACHE_KEY + instanceVal, target);
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
        if(!rectangle || width <= height) {
            int x = 0; 
            int y = 0;
            int size = width;
            if(width != height) {
                if(width > height) {
                    size = height;
                    x = (width - height) / 2;
                } else {
                    size = width;
                    y = (height - width) / 2;
                }
            }
            if(stroke && this.stroke != null) {
                int sw = (int)Math.ceil((stroke && this.stroke != null) ? this.stroke.getLineWidth() : 0);
                GeneralPath arc = new GeneralPath();
                arc.arc(x+sw/2, y+sw/2, size-2*sw, size-2*sw, 0, 2*Math.PI);
                g.fillShape(arc);
                g.setColor(strokeColor);
                g.setAlpha(strokeOpacity);
                g.drawShape(arc, this.stroke);
            } else {
                g.fillArc(x, y, size, size, 0, 360);
            }
        } else {
            GeneralPath gp = new GeneralPath();
            float sw = (stroke && this.stroke != null) ? this.stroke.getLineWidth() : 0;
            gp.moveTo(height / 2.0, sw);
            gp.lineTo(width - (height / 2.0), sw);
            gp.arcTo(width - (height / 2.0), height / 2.0, width - (height / 2.0), height-sw, true);
            gp.lineTo(height / 2.0, height-sw);
            gp.arcTo(height / 2.0, height / 2.0, height / 2.0, sw, true);
            gp.closePath();
            g.fillShape(gp);
            if(stroke && this.stroke != null) {
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

    /**
     * The color of the border background
     * @return the color
     */
    public int getColor() {
        return color;
    }

    /**
     * The opacity (transparency) of the border background
     * @return the opacity
     */
    public int getOpacity() {
        return opacity;
    }

    /**
     * The color of the edge of the border if applicable
     * @return the strokeColor
     */
    public int getStrokeColor() {
        return strokeColor;
    }

    /**
     * The opacity of the edge of the border if applicable
     * @return the strokeOpacity
     */
    public int getStrokeOpacity() {
        return strokeOpacity;
    }

    /**
     * The thickness of the edge of the border if applicable, 0 if no stroke is needed
     * @return the strokeThickness
     */
    public float getStrokeThickness() {
        return strokeThickness;
    }

    /**
     * True if the thickness of the stroke is in millimeters
     * @return the strokeMM
     */
    public boolean isStrokeMM() {
        return strokeMM;
    }

    /**
     * The spread of the shadow in pixels of millimeters
     * @return the shadowSpread
     */
    public int getShadowSpread() {
        return shadowSpread;
    }

    /**
     * The opacity of the shadow between 0 and 255
     * @return the shadowOpacity
     */
    public int getShadowOpacity() {
        return shadowOpacity;
    }

    /**
     * X axis bias of the shadow between 0 and 1 where 0 is to the top and 1 is to the bottom, defaults to 0.5
     * @return the shadowX
     */
    public float getShadowX() {
        return shadowX;
    }

    /**
     * Y axis bias of the shadow between 0 and 1 where 0 is to the left and 1 is to the right, defaults to 0.5
     * @return the shadowY
     */
    public float getShadowY() {
        return shadowY;
    }

    /**
     * The Gaussian blur size
     * @return the shadowBlur
     */
    public float getShadowBlur() {
        return shadowBlur;
    }

    /**
     * True if the shadow spread is in millimeters
     * @return the shadowMM
     */
    public boolean isShadowMM() {
        return shadowMM;
    }

    /**
     * True if this border grows into a rectangle horizontally or keeps growing as a circle
     * @return the rectangle
     */
    public boolean isRectangle() {
        return rectangle;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 43 * hash + this.color;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RoundBorder other = (RoundBorder) obj;
        if (this.color != other.color) {
            return false;
        }
        if (this.opacity != other.opacity) {
            return false;
        }
        if (this.strokeColor != other.strokeColor) {
            return false;
        }
        if (this.strokeOpacity != other.strokeOpacity) {
            return false;
        }
        if (this.strokeThickness != other.strokeThickness) {
            return false;
        }
        if (this.strokeMM != other.strokeMM) {
            return false;
        }
        if (this.shadowSpread != other.shadowSpread) {
            return false;
        }
        if (this.shadowOpacity != other.shadowOpacity) {
            return false;
        }
        if (this.shadowX != other.shadowX) {
            return false;
        }
        if (this.shadowY != other.shadowY) {
            return false;
        }
        if (this.shadowBlur != other.shadowBlur) {
            return false;
        }
        if (this.shadowMM != other.shadowMM) {
            return false;
        }
        if (this.rectangle != other.rectangle) {
            return false;
        }
        return true;
    }
    
    
}
