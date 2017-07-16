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
 * <p>Customizable rounded rectangle border that breaks down the border into customizable pieces</p>
 * 
 *
 * @author Shai Almog
 */
public class RoundRectBorder extends Border {
    private static final String CACHE_KEY = "cn1$$-rbcache";
        
    /**
     * The color of the edge of the border if applicable
     */
    private int strokeColor = 0;
    
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
     * The spread of the shadow in millimeters
     */
    private float shadowSpread;

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
     * The radius of the corners in millimeters
     */
    private float cornerRadius = 2;
    
    /**
     * True if the corners are bezier curves, otherwise the corners are drawn as a regular arc
     */
    private boolean bezierCorners;
    
    /**
     * Special mode where only the top of the round rectangle is rounded and the bottom is a regular rectangle
     */
    private boolean topOnlyMode;
    
    /**
     * Special mode where only the bottom of the round rectangle is rounded and the top is a regular rectangle
     */
    private boolean bottomOnlyMode;
    
    // these allow us to have more than one border per component in cache which is important for selected/unselected/pressed values
    private static int instanceCounter;
    private final int instanceVal;
    
    private RoundRectBorder() {
        shadowSpread = Display.getInstance().convertToPixels(0.2f);
        instanceCounter++;
        instanceVal = instanceCounter;
    }
    
    /**
     * Creates a flat border with styles derived from the component UIID
     * @return a border instance
     */
    public static RoundRectBorder create() {
        return new RoundRectBorder();
    }
    
    /**
     * Sets the opacity of the stroke line around the border
     * @param strokeOpacity the opacity from 0-255 where 255 is completely opaque 
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder strokeOpacity(int strokeOpacity) {
        this.strokeOpacity = strokeOpacity;
        return this;
    }
    
    /**
     * Sets the stroke color of the border
     * @param strokeColor the color 
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder strokeColor(int strokeColor) {
        this.strokeColor = strokeColor;
        return this;
    }

    /**
     * Sets the stroke of the border
     * @param stroke the stroke object
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder stroke(Stroke stroke) {
        this.stroke = stroke;
        return this;
    }

    /**
     * Sets the stroke of the border
     * @param stroke the thickness of the stroke object
     * @param mm set to true to indicate the value is in millimeters, false indicates pixels
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder stroke(float stroke, boolean mm) {
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
     * Sets the spread of the shadow in millimeters i.e how much bigger is it than the actual border
     * @param shadowSpread the amount in millimeters representing the size of the shadow
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder shadowSpread(float shadowSpread) {
        this.shadowSpread = shadowSpread;
        return this;
    }

    /**
     * Sets the spread in pixels of the shadow i.e how much bigger is it than the actual border
     * @param shadowSpread the amount in pixels representing the size of the shadow
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder shadowSpread(int shadowSpread) {
        this.shadowSpread = shadowSpread;
        return this;
    }

    /**
     * Sets the opacity of the shadow from 0 - 255 where 0 means no shadow and 255 means opaque black shadow
     * @param shadowOpacity the opacity of the shadow
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder shadowOpacity(int shadowOpacity) {
        this.shadowOpacity = shadowOpacity;
        return this;
    }

    /**
     * The position of the shadow on the X axis where 0.5f means the center and higher values draw it to the right side
     * @param shadowX the position of the shadow between 0 - 1 where 0 equals left and 1 equals right
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder shadowX(float shadowX) {
        this.shadowX = shadowX;
        return this;
    }

    /**
     * The position of the shadow on the Y axis where 0.5f means the center and higher values draw it to the bottom
     * @param shadowY the position of the shadow between 0 - 1 where 0 equals top and 1 equals bottom
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder shadowY(float shadowY) {
        this.shadowY = shadowY;
        return this;
    }

    /**
     * The blur on the shadow this is the standard Gaussian blur radius
     * @param shadowBlur The blur on the shadow this is the standard Gaussian blur radius
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder shadowBlur(float shadowBlur) {
        this.shadowBlur = shadowBlur;
        return this;
    }
        
    /**
     * The radius of the corners in millimeters
     * 
     * @param cornerRadius the radius value
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder cornerRadius(float cornerRadius) {
        this.cornerRadius = cornerRadius;
        return this;
    }
    
    /**
     * True if the corners are Bezier curves, otherwise the corners are drawn as a regular arc
     * 
     * @param bezierCorners true if the corners use a bezier curve for drawing
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder bezierCorners(boolean bezierCorners) {
        this.bezierCorners = bezierCorners;
        return this;
    }
    
    
    /**
     * Special mode where only the top of the round rectangle is rounded and the bottom is a regular rectangle
     * 
     * @param topOnlyMode new value for top only mode
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder topOnlyMode(boolean topOnlyMode) {
        this.topOnlyMode = topOnlyMode;
        return this;
    }
    
    /**
     * Special mode where only the bottom of the round rectangle is rounded and the top is a regular rectangle
     * 
     * @param bottomOnlyMode new value for bottom only mode
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder bottomOnlyMode(boolean bottomOnlyMode) {
        this.bottomOnlyMode = bottomOnlyMode;
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
                
        int shadowSpreadL =  Display.getInstance().convertToPixels(shadowSpread);
        
        if(shadowOpacity > 0) {
            shapeW -= shadowSpreadL;
            shapeH -= shadowSpreadL;
            shapeX += Math.round(((float)shadowSpreadL) * shadowX);
            shapeY += Math.round(((float)shadowSpreadL) * shadowY);
            
            int initialOffsetX = Math.round(((float)shadowSpreadL) * (1 - shadowX));
            int initialOffsetY = Math.round(((float)shadowSpreadL) * (1 - shadowY));
            tg.translate(initialOffsetX, initialOffsetY);
            
            // draw a gradient of sort for the shadow
            for(int iter = shadowSpreadL - 1 ; iter >= 0 ; iter--) {            
                tg.translate(iter, iter);
                fillShape(tg, 0, shadowOpacity / shadowSpreadL, w - (iter * 2), h - (iter * 2), false);
                tg.translate(-iter, -iter);
            }
            tg.translate(-initialOffsetX, -initialOffsetY);
            
            if(Display.getInstance().isGaussianBlurSupported()) {
                Image blured = Display.getInstance().gaussianBlurImage(target, shadowBlur/2);
                target = Image.createImage(w, h, 0);
                tg = target.getGraphics();
                tg.drawImage(blured, 0, 0);
                tg.setAntiAliased(true);
            }
        }
        tg.translate(shapeX, shapeY);
        c.getStyle().setBorder(Border.createEmpty());

        GeneralPath gp = createShape(shapeW, shapeH);
        tg.setClip(gp);
        c.getStyle().getBgPainter().paint(tg, new Rectangle(0, 0, w, h));
        if(this.stroke != null && strokeOpacity > 0 && strokeThickness > 0) {
            tg.setClip(0, 0, w, h);
            tg.setAlpha(strokeOpacity);
            tg.setColor(strokeColor);
            tg.drawShape(gp, this.stroke);
        }            
        c.getStyle().setBorder(this);
        g.drawImage(target, x, y);
        c.putClientProperty(CACHE_KEY + instanceVal, target);
    }
    
    private GeneralPath createShape(int shapeW, int shapeH) {
        GeneralPath gp = new GeneralPath();
        float radius = Display.getInstance().convertToPixels(cornerRadius);
        float x = 0;
        float y = 0;
        float widthF = shapeW;
        float heightF = shapeH;
        
        if(this.stroke != null && strokeOpacity > 0 && strokeThickness > 0) {
            int strokePx = (int)strokeThickness;
            if(strokeMM) {
                strokePx = Display.getInstance().convertToPixels(strokeThickness);
            }
            widthF -= strokePx;
            heightF -= strokePx;
            x += strokePx / 2;
            y += strokePx / 2;
            
            if(strokePx % 2 == 1) {
                x += 0.5f;
                y += 0.5f;
            }
        }            
        
        gp.moveTo(x + radius, y);
        gp.lineTo(x + widthF - radius, y);
        gp.quadTo(x + widthF, y, x + widthF, y + radius);
        gp.lineTo(x + widthF, y + heightF - radius);
        gp.quadTo(x + widthF, y + heightF, x + widthF - radius, y + heightF);
        gp.lineTo(x + radius, y + heightF);
        gp.quadTo(x, y + heightF, x, y + heightF - radius);
        gp.lineTo(x, y + radius);
        gp.quadTo(x, y, x + radius, y);
        gp.closePath();            
        return gp;
    }

    @Override
    public int getMinimumHeight() {
        return Display.getInstance().convertToPixels(shadowSpread) + Display.getInstance().convertToPixels(cornerRadius) * 2;
    }

    @Override
    public int getMinimumWidth() {
        return Display.getInstance().convertToPixels(shadowSpread) + Display.getInstance().convertToPixels(cornerRadius) * 2;
    }

    
    private void fillShape(Graphics g, int color, int opacity, int width, int height, boolean stroke) {
        g.setColor(color);
        g.setAlpha(opacity);
        GeneralPath gp = createShape(width, height);
        g.fillShape(gp);
        if(stroke && this.stroke != null) {
            g.setAlpha(strokeOpacity);
            g.setColor(strokeColor);
            g.drawShape(gp, this.stroke);
        }            
    }
    
    @Override
    public boolean isBackgroundPainter() {
        return true;
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
     * True if the corners are bezier curves, otherwise the corners are drawn as a regular arc
     * 
     * @return true if the corners are a curve
     */
    public boolean isBezierCorners() {
        return bezierCorners;
    }
    
    /**
     * The spread of the shadow in pixels of millimeters
     * @return the shadowSpread
     */
    public float getShadowSpread() {
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
     * The radius of the corners in millimeters
     * 
     * @return the radius
     */
    public float getCornerRadius() {
        return cornerRadius;
    }

    
    /**
     * Special mode where only the top of the round rectangle is rounded and the bottom is a regular rectangle
     * 
     * @return whether this is the top only mode
     */
    public boolean isTopOnlyMode() {
        return topOnlyMode;
    }
    
    /**
     * Special mode where only the bottom of the round rectangle is rounded and the top is a regular rectangle
     * 
     * @return whether this is the bottom only mode
     */
    public boolean isBottomOnlyMode() {
        return bottomOnlyMode;
    }
    
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 43 * hash + strokeColor;
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
        final RoundRectBorder other = (RoundRectBorder) obj;
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
        if (this.bezierCorners != other.bezierCorners) {
            return false;
        }
        return true;
    }
    
    
}
