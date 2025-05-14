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

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.ImageFactory;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;

/**
 * <p>Customizable rounded rectangle border that breaks down the border into customizable pieces.
 * 
 * The background is inherited from the parent UIID but stroke and shadow can be customized via user settings.
 * </p>
 * <p>
 * <strong>IMPORTANT:</strong> {@code RoundRectBorder} instances can't be reused
 * you would need to create a separate instance for each style object!
 * See <a href="https://github.com/codenameone/CodenameOne/issues/2578#issuecomment-429554441">this issue</a> for further details.
 * </p>
 * <script src="https://gist.github.com/codenameone/27bd5a15c7000118089d8037e2dd9367.js"></script>
 * <img src="https://www.codenameone.com/img/blog/round-rect-sample.png" alt="Round Rect Border" />
 * 
 * @author Shai Almog
 */
public class RoundRectBorder extends Border {
    private static final String CACHE_KEY = "cn1$$-rrbcache";
    private boolean useCache = true;
    private boolean dirty=true; 
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
     * Var to explicitly set the position of the arrow when tracking a component. Values
     * between 0 and 1, with zero being the Top, and 1 being the bottom.  Default negative
     * value indicates that it should just calculate the position as normal, suing the
     * the provided tracking component bounds.
     * @since 7.0
     */
    private float trackComponentVerticalPosition = -1;
    
    /**
     * Var to explicitly set the position of the arrow when tracking a component. Values
     * between 0 and 1, with zero being the left, and 1 being the right.  Default negative
     * value indicates that it should just calculate the position as normal, suing the
     * the provided tracking component bounds.
     * @since 7.0
     */
    private float trackComponentHorizontalPosition = -1;
    
    /**
     * Var to explicitly set the position of the arrow when tracking a component. Acceptable
     * values {@link Component#TOP}, {@link Component#BOTTOM}, {@link Component#LEFT}, {@link Component#RIGHT}.
     * @since 7.0
     */
    private int trackComponentSide = -1;
    
    
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
     * The color of the shadow as an RRGGBB color (no alpha)
     */
    private int shadowColor = 0;
    
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
        
    // these allow us to have more than one border per component in cache which is important for selected/unselected/pressed values
    private static int instanceCounter;
    private final int instanceVal;

    private boolean topLeft = true, topRight = true, bottomLeft = true, bottomRight = true;

    private int arrowPosition = -1;
    private int arrowDirection = -1;
    
    
    private float arrowSize = 1.5f;
    
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
     * Change the size of the arrow used for component tracking.
     * @param size Size of arrow in millimeters.
     * @return a border instance
     * @since 7.0
     */
    public RoundRectBorder arrowSize(float size) {
        this.arrowSize = size;
        return this;
    }
    
    /**
     * Change the size of the arrow used for component tracking.
     * @param size Size of arrow in millimeters.
     * @since 7.0
     */
    public void setArrowSize(float size) {
        this.arrowSize = size;
    }
    
    /**
     * Sets the opacity of the stroke line around the border
     * @param strokeOpacity the opacity from 0-255 where 255 is completely opaque 
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder strokeOpacity(int strokeOpacity) {
        if (strokeOpacity != this.strokeOpacity) {
            this.strokeOpacity = strokeOpacity;
            dirty = true;
        }
        return this;
    }
    
    /**
     * Explicitly positions the arrow used for component tracking to a particular
     * side of the border. This can be used to override the default positioning, which is to place the arrow according to the position of the tracking component ({@link #setTrackComponent(com.codename1.ui.geom.Rectangle) }).
     * Use in conjunction with {@link #trackComponentHorizontalPosition(float) }, and {@link #trackComponentHorizontalPosition}.
     * @param side The side to place the tracking arrow on.  Values {@link Component#TOP}, {@link Component#BOTTOM}, {@link Component#LEFT},
     * or {@link Component#BOTTOM}.  Set negative value for default behaviour, which is to just calculate the arrow position
     * based on the tracking component bounds.
     * @return Self for chaining.
     * @since 7.0
     */
    public RoundRectBorder trackComponentSide(int side) {
        this.trackComponentSide = side;
        return this;
    }
    
    /**
     * Gets the side that the tracking component should be displayed on if using explicit placement. 
     * @return The side that the arrow should be rendered on. Values {@link Component#TOP}, {@link Component#BOTTOM}, {@link Component#LEFT}, or a negative number to indicate that the position will be calculated based on the position of the tracking component.
     * or {@link Component#BOTTOM}. 
     * @since 7.0
     */
    public int getTrackComponentSide() {
        return trackComponentSide;
    }
    
    /**
     * Explicitly sets the vertical position of the tracking arrow.   This can be used to override the default positioning, which is to place the arrow according to the position of the tracking component ({@link #setTrackComponent(com.codename1.ui.geom.Rectangle) }).
     * @param pos Vertical position of the arrow.  Values between 0 and 1 will place the arrow in the range from top to bottom.  Negative values result in
     * default behaviour, which is to calculate the position based on the tracking component position.
     * @return Self for chainging.
     * @since 7.0
     */
    public RoundRectBorder trackComponentVerticalPosition(float pos) {
        this.trackComponentVerticalPosition = pos;
        return this;
    }
    
    /**
     * Gets the explicitly set vertical position of the tracking arrow.   This can be used to override the default positioning, which is to place the arrow according to the position of the tracking component ({@link #setTrackComponent(com.codename1.ui.geom.Rectangle) }).
     * @return Vertical position of the arrow.  Values between 0 and 1 will place the arrow in the range from top to bottom.  Negative values result in
     * default behaviour, which is to calculate the position based on the tracking component position.
     * @since 7.0
     */
    public float getTrackComponentVerticalPosition() {
        return trackComponentVerticalPosition;
    }
    
    /**
     * Explicitly sets the horizontal position of the tracking arrow.   This can be used to override the default positioning, which is to place the arrow according to the position of the tracking component ({@link #setTrackComponent(com.codename1.ui.geom.Rectangle) }).
     * @param pos Vertical position of the arrow.  Values between 0 and 1 will place the arrow in the range from left to right.  Negative values result in
     * default behaviour, which is to calculate the position based on the tracking component position.
     * @return Self for chainging.
     * @since 7.0
     */
    public RoundRectBorder trackComponentHorizontalPosition(float pos) {
        this.trackComponentHorizontalPosition = pos;
        return this;
    }
    
    /**
     * Gets the explicitly set horizontal position of the tracking arrow.   This can be used to override the default positioning, which is to place the arrow according to the position of the tracking component ({@link #setTrackComponent(com.codename1.ui.geom.Rectangle) }).
     * @return Vertical position of the arrow.  Values between 0 and 1 will place the arrow in the range from left to right.  Negative values result in
     * default behaviour, which is to calculate the position based on the tracking component position.
     * @since 7.0
     */
    public float getTrackComponentHorizontalPosition() {
        return trackComponentHorizontalPosition;
    }
    
    /**
     * Sets whether this RoundRectBorder instance should cache the border as a background image.
     * 
     * <p>This setting is on by default, but can be turned off, as some older, low-memory devices may run into
     * memory trouble if it is using a lot of RoundRectBorders.  Turn the cache off for low-memory devices.</p>
     * 
     * <p><strong>NOTE:</strong> Using the cache is required for gaussian blur to work.  If cache is disabled,
     * then gaussian blur settings will be ignored.</p>
     * 
     * @param useCache True to cache the border as a mutable image on the Component.
     * @return Self for chaining.
     * @since 8.0
     */
    public RoundRectBorder useCache(boolean useCache) {
        this.useCache = useCache;
        return this;
    }
    
    /**
     * Checks whether this RoundRectBorder instance caches the border as a background image.
     * @return 
     */
    public boolean isUseCache() {
        return this.useCache;
    }
    
    
    /**
     * Sets the stroke color of the border
     * @param strokeColor the color 
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder strokeColor(int strokeColor) {
        if (strokeColor != this.strokeColor) {
            this.strokeColor = strokeColor;
            dirty = true;
        }
        return this;
    }

    /**
     * Sets the stroke of the border
     * @param stroke the stroke object
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder stroke(Stroke stroke) {
    	if (stroke != null) {
    		strokeThickness = stroke.getLineWidth();
    		strokeMM = false;
    	}
        this.stroke = stroke;
        dirty = true;
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
        this.stroke = new Stroke(stroke, Stroke.CAP_SQUARE, Stroke.JOIN_MITER, 1);
        dirty = true;
        return this;
    }
    
    /**
     * Sets the spread of the shadow in millimeters i.e how much bigger is it than the actual border
     * @param shadowSpread the amount in millimeters representing the size of the shadow
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder shadowSpread(float shadowSpread) {
        if (shadowSpread != this.shadowSpread) {
            this.shadowSpread = shadowSpread;
            dirty = true;
        }
        return this;
    }

    /**
     * Sets the spread in pixels of the shadow i.e how much bigger is it than the actual border
     * @param shadowSpread the amount in pixels representing the size of the shadow
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder shadowSpread(int shadowSpread) {
        this.shadowSpread = shadowSpread * 100f/Display.getInstance().convertToPixels(100f);
        dirty = true;
        return this;
    }

    /**
     * Sets the opacity of the shadow from 0 - 255 where 0 means no shadow and 255 means opaque black shadow
     * @param shadowOpacity the opacity of the shadow
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder shadowOpacity(int shadowOpacity) {
        if (shadowOpacity != this.shadowOpacity) {
            this.shadowOpacity = shadowOpacity;
            dirty = true;
        }
        return this;
    }

    /**
     * Sets the color of the shadow as an RRGGBB color
     * @param shadowColor the color of the shadow
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder shadowColor(int shadowColor) {
        if (shadowColor != this.shadowColor) {
            this.shadowColor = shadowColor;
            dirty = true;
        }
        return this;
    }
    
    /**
     * The position of the shadow on the X axis where 0.5f means the center and higher values draw it to the right side
     * @param shadowX the position of the shadow between 0 - 1 where 0 equals left and 1 equals right
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder shadowX(float shadowX) {
        if (shadowX != this.shadowX) {
            this.shadowX = shadowX;
            dirty = true;
        }
        return this;
    }

    /**
     * The position of the shadow on the Y axis where 0.5f means the center and higher values draw it to the bottom
     * @param shadowY the position of the shadow between 0 - 1 where 0 equals top and 1 equals bottom
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder shadowY(float shadowY) {
        if (shadowY != this.shadowY) {
            this.shadowY = shadowY;
            dirty = true;
        }
        return this;
    }

    /**
     * The blur on the shadow this is the standard Gaussian blur radius
     * @param shadowBlur The blur on the shadow this is the standard Gaussian blur radius
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder shadowBlur(float shadowBlur) {
        if (shadowBlur != this.shadowBlur) {
            this.shadowBlur = shadowBlur;
            dirty = true;
        }
        return this;
    }
        
    /**
     * The radius of the corners in millimeters
     * 
     * @param cornerRadius the radius value
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder cornerRadius(float cornerRadius) {
        if (cornerRadius != this.cornerRadius) {
           this.cornerRadius = cornerRadius;
           dirty = true;
        }
        return this;
    }
    
    /**
     * True if the corners are Bezier curves, otherwise the corners are drawn as a regular arc
     * 
     * @param bezierCorners true if the corners use a bezier curve for drawing
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder bezierCorners(boolean bezierCorners) {
        if (bezierCorners != this.bezierCorners) {
            this.bezierCorners = bezierCorners;
            dirty = true;
        }
        return this;
    }
    

    /**
     * True to draw the top left corner rounded, false to draw it as a corner
     * 
     * @param topLeft true for round false for sharp
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder topLeftMode(boolean topLeft) {
        if (topLeft != this.topLeft) {
            this.topLeft = topLeft;
            dirty = true;
        }
        
        return this;
    }
    
    /**
     * True to draw the top right corner rounded, false to draw it as a corner
     * 
     * @param topRight true for round false for sharp
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder topRightMode(boolean topRight) {
        if (topRight != this.topRight) {
            this.topRight = topRight;
            dirty = true;
        }
        return this;
    }
    
    /**
     * True to draw the bottom left corner rounded, false to draw it as a corner
     * 
     * @param bottomLeft true for round false for sharp
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder bottomLeftMode(boolean bottomLeft) {
        if (bottomLeft != this.bottomLeft) {
            this.bottomLeft = bottomLeft;
            dirty = true;
        }
        return this;
    }
    
    /**
     * True to draw the bottom right corner rounded, false to draw it as a corner
     * 
     * @param bottomRight true for round false for sharp
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder bottomRightMode(boolean bottomRight) {
        if (bottomRight != this.bottomRight) {
            this.bottomRight = bottomRight;
            dirty = true;
        }
        return this;
    }
    
    
    
    /**
     * Special mode where only the top of the round rectangle is rounded and the bottom is a regular rectangle
     * 
     * @param topOnlyMode new value for top only mode
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder topOnlyMode(boolean topOnlyMode) {
        if(topOnlyMode) {
            topLeftMode(true);
            topRightMode(true);
            bottomLeftMode(false);
            bottomRightMode(false);
        } else {
            topLeftMode(false);
            topRightMode(false);

        }
        return this;
    }
    
    /**
     * Special mode where only the bottom of the round rectangle is rounded and the top is a regular rectangle
     * 
     * @param bottomOnlyMode new value for bottom only mode
     * @return border instance so these calls can be chained
     */
    public RoundRectBorder bottomOnlyMode(boolean bottomOnlyMode) {
        if(bottomOnlyMode) {
            topLeftMode(false);
            topRightMode(false);
            bottomLeftMode(true);
            bottomRightMode(true);
        } else {
            bottomLeftMode(false);
            bottomRightMode(false);

        }
        return this;
    }


    private Image createTargetComponentImage(final Component c, final int w, final int h, final boolean fast) {
        return new com.codename1.ui.ComponentImage(new Component() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                try {
                    g.translate(getX(), getY());
                    int shapeX = 0;
                    int shapeY = 0;
                    int shapeW = w;
                    int shapeH = h;

                    Graphics tg = g;
                    tg.setAntiAliased(true);

                    int shadowSpreadL = Display.getInstance().convertToPixels(shadowSpread);

                    if (shadowOpacity > 0) {
                        shapeW -= shadowSpreadL;
                        shapeH -= shadowSpreadL;
                        shapeX += Math.round(((float) shadowSpreadL) * shadowX);
                        shapeY += Math.round(((float) shadowSpreadL) * shadowY);

                        // draw a gradient of sort for the shadow
                        for (int iter = shadowSpreadL - 1; iter >= 0; iter--) {
                            tg.translate(iter, iter);
                            int iterOpacity = Math.max(0, Math.min(255, (int) (shadowOpacity * (shadowSpreadL - iter) / (float) shadowSpreadL)));
                            drawShape(tg, shadowColor, shadowOpacity - iterOpacity, w - (iter * 2), h - (iter * 2));
                            tg.translate(-iter, -iter);
                        }
                    }
                    tg.translate(shapeX, shapeY);

                    GeneralPath gp = createShape(shapeW, shapeH);
                    Style s = c.getStyle();
                    if (s.getBgImage() == null) {
                        byte type = s.getBackgroundType();
                        if (type == Style.BACKGROUND_IMAGE_SCALED || type == Style.BACKGROUND_NONE) {
                            byte bgt = c.getStyle().getBgTransparency();
                            if (bgt != 0) {
                                tg.setAlpha(bgt & 0xff);
                                tg.setColor(s.getBgColor());
                                tg.fillShape(gp);
                            }
                            if (RoundRectBorder.this.stroke != null && strokeOpacity > 0 && strokeThickness > 0) {
                                tg.setAlpha(strokeOpacity);
                                tg.setColor(strokeColor);
                                tg.drawShape(gp, RoundRectBorder.this.stroke);
                            }
                            return;
                        }
                    }

                    c.getStyle().setBorder(Border.createEmpty());
                    tg.setClip(gp);
                    s.getBgPainter().paint(tg, new Rectangle(0, 0, w, h));
                    if (RoundRectBorder.this.stroke != null && strokeOpacity > 0 && strokeThickness > 0) {
                        tg.setClip(0, 0, w, h);
                        tg.setAlpha(strokeOpacity);
                        tg.setColor(strokeColor);
                        tg.drawShape(gp, RoundRectBorder.this.stroke);
                    }
                    c.getStyle().setBorder(RoundRectBorder.this);
                    return;
                } finally {
                    g.translate(-getX(), -getY());
                }
            }
        }, w, h);


    }

    
    
    
    private Image createTargetImage(Component c, int w, int h, boolean fast) {
        if (!useCache) {
            return createTargetComponentImage(c, w, h, fast);
        }
        Image target = ImageFactory.createImage(c, w, h, 0);
        
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
                        
            // draw a gradient of sort for the shadow
            for(int iter = shadowSpreadL - 1 ; iter >= 0 ; iter--) {            
                tg.translate(iter, iter);
                int iterOpacity = Math.max(0, Math.min(255, (int)(shadowOpacity * (shadowSpreadL - iter)/(float)shadowSpreadL)));
                drawShape(tg, shadowColor, shadowOpacity-iterOpacity, w - (iter * 2), h - (iter * 2));
                tg.translate(-iter, -iter);
            }
            
            if(Display.getInstance().isGaussianBlurSupported() && !fast) {
                Image blured = Display.getInstance().gaussianBlurImage(target, shadowBlur/2);
                target = ImageFactory.createImage(c, w, h, 0);
                tg = target.getGraphics();
                tg.drawImage(blured, 0, 0);
                tg.setAntiAliased(true);
            }
        }
        tg.translate(shapeX, shapeY);

        GeneralPath gp = createShape(shapeW, shapeH);
        Style s = c.getStyle();
        if(s.getBgImage() == null ) {
            byte type = s.getBackgroundType();
            if(type == Style.BACKGROUND_IMAGE_SCALED || type == Style.BACKGROUND_NONE) {
                byte bgt = c.getStyle().getBgTransparency();
                if(bgt != 0) {
                    tg.setAlpha(bgt &0xff);
                    tg.setColor(s.getBgColor());
                    tg.fillShape(gp);
                }
                if(this.stroke != null && strokeOpacity > 0 && strokeThickness > 0) {
                    tg.setAlpha(strokeOpacity);
                    tg.setColor(strokeColor);
                    tg.drawShape(gp, this.stroke);
                }            
                return target;
            }
        }
        
        c.getStyle().setBorder(Border.createEmpty());
        tg.setClip(gp);
        s.getBgPainter().paint(tg, new Rectangle(0, 0, w, h));
        if(this.stroke != null && strokeOpacity > 0 && strokeThickness > 0) {
            tg.setClip(0, 0, w, h);
            tg.setAlpha(strokeOpacity);
            tg.setColor(strokeColor);
            tg.drawShape(gp, this.stroke);
        }            
        c.getStyle().setBorder(this);
        return target;
    }    
    
    @Override
    public void paintBorderBackground(Graphics g, final Component c) {
        if(getTrackComponent() != null) {
            int cabsY = c.getAbsoluteY();
            int trackY = getTrackComponent().getY();
            int trackX = getTrackComponent().getX();
            int cabsX = c.getAbsoluteX();
            int arrowWH = CN.convertToPixels(arrowSize);
            if(cabsY >= trackY + getTrackComponent().getHeight()) {
                // we are below the component
                arrowDirection = CN.TOP;
                arrowPosition = (trackX + getTrackComponent().getWidth() / 2) - cabsX - arrowWH / 2;
            } else {    
                if(trackComponentSide == CN.BOTTOM || cabsY + c.getHeight() <= trackY) {
                    // we are above the component
                    arrowDirection = CN.BOTTOM;
                    arrowPosition = (trackX + getTrackComponent().getWidth() / 2) - cabsX - arrowWH / 2;
                }  else {
                    if(cabsX >= trackX + getTrackComponent().getWidth()) {
                        // we are to the right of the component
                        arrowDirection = CN.LEFT;
                        arrowPosition = (trackY + getTrackComponent().getHeight() / 2) - cabsY - arrowWH / 2;
                    } else {
                        if(cabsX + c.getWidth() <= trackX) {
                            // we are to the left of the component
                            arrowDirection = CN.RIGHT;
                            arrowPosition = (trackY + getTrackComponent().getHeight() / 2) - cabsY - arrowWH / 2;
                        }
                    }
                }
            }
        } else if (trackComponentSide >= 0) {
            switch (trackComponentSide) {
                case CN.TOP:
                    arrowDirection = CN.TOP;
                    arrowPosition = 0;
                    if (trackComponentHorizontalPosition >= 0) {
                        arrowPosition = (int)(c.getWidth() * trackComponentHorizontalPosition);
                    }   break;
                case CN.BOTTOM:
                    arrowDirection = CN.BOTTOM;
                    arrowPosition = 0;
                    if (trackComponentHorizontalPosition >= 0) {
                        arrowPosition = (int)(c.getWidth() * trackComponentHorizontalPosition);
                    }   break;
                case CN.LEFT:
                    arrowDirection = CN.LEFT;
                    arrowPosition = 0;
                    if (trackComponentVerticalPosition >= 0) {
                        arrowPosition = (int)(c.getHeight() * trackComponentVerticalPosition);
                    }   break;
                case CN.RIGHT:
                    arrowDirection = CN.RIGHT;
                    arrowPosition = 0;
                    if (trackComponentVerticalPosition >= 0) {
                        arrowPosition = (int)(c.getHeight() * trackComponentVerticalPosition);
                    }   break;
                default:
                    break;
            }
        }
        
        final int w = c.getWidth();
        final int h = c.getHeight();
        int x = c.getX();
        int y = c.getY();
        boolean antiAliased = g.isAntiAliased();
        g.setAntiAliased(true);
        try {
            if(shadowOpacity == 0) {
                Style s = c.getStyle();
                if(s.getBgImage() == null ) {
                    byte type = s.getBackgroundType();
                    if(type == Style.BACKGROUND_IMAGE_SCALED || type == Style.BACKGROUND_NONE) {
                        GeneralPath gp = createShape(w, h);
                        byte bgt = c.getStyle().getBgTransparency();
                        if(bgt != 0) {
                            int a = g.getAlpha();
                            g.setAlpha(bgt &0xff);
                            g.setColor(s.getBgColor());
                            g.translate(x, y);
                            g.fillShape(gp);
                            if(this.stroke != null && strokeOpacity > 0 && strokeThickness > 0) {
                                g.setAlpha(strokeOpacity);
                                g.setColor(strokeColor);
                                g.drawShape(gp, this.stroke);
                            }            
                            g.translate(-x, -y);
                            g.setAlpha(a);
                        }
                        if(this.stroke != null && strokeOpacity > 0 && strokeThickness > 0) {
                            int a = g.getAlpha();
                            g.setAlpha(strokeOpacity);
                            g.setColor(strokeColor);
                            g.translate(x, y);
                            g.drawShape(gp, this.stroke);
                            g.translate(-x, -y);
                            g.setAlpha(a);
                        }      
                        return;
                    }
                }        
            }
            if(w > 0 && h > 0) {
                Image background = (Image)c.getClientProperty(CACHE_KEY + instanceVal);
                if(!dirty && background != null && background.getWidth() == w && background.getHeight() == h) {
                    g.drawImage(background, x, y);
                    return;
                }
            } else {
                return;
            }
            Image target = createTargetImage(c, w, h, true);
            g.drawImage(target, x, y);
            c.putClientProperty(CACHE_KEY + instanceVal, target);
            dirty = false;
            // update the cache with a more refined version and repaint
            if (!useCache) {
                Display.getInstance().callSeriallyOnIdle(new Runnable() {
                    public void run() {
                        if (w == c.getWidth() && h == c.getHeight()) {
                            Image target = createTargetImage(c, w, h, false);
                            c.putClientProperty(CACHE_KEY + instanceVal, target);
                            c.repaint();
                        }
                    }
                });
            }
        } finally {
            g.setAntiAliased(antiAliased);
        }
    }
    
    private GeneralPath createShape(int shapeW, int shapeH) {
        GeneralPath gp = new GeneralPath();
        float radius = Display.getInstance().convertToPixels(cornerRadius);
        float x = 0;
        float y = 0;
        float widthF = shapeW;
        float heightF = shapeH;
        
        if(getTrackComponent() != null || trackComponentSide >= 0) {
            int ah = CN.convertToPixels(arrowSize);
            switch(arrowDirection) {
                case CN.TOP:
                    y = ah;
                    // intentional fall through to the next statement...
                case CN.BOTTOM:
                    heightF -= ah;
                    break;
                case CN.LEFT:
                    x = ah;
                    // intentional fall through to the next statement...
                case CN.RIGHT:
                    widthF -= ah;
                    break;
            }
        }
        
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
                        
        if(topLeft) {
            gp.moveTo(x + radius, y);
        } else {
            gp.moveTo(x, y);            
        }
        if((trackComponentSide >= 0 || getTrackComponent() != null) && arrowDirection == CN.TOP) {
            int actualArrowPosition = (int)
                Math.min(x + widthF,
                    Math.max(arrowPosition, x + radius));
            gp.lineTo(actualArrowPosition, y);
            int ah = CN.convertToPixels(arrowSize);
            gp.lineTo(actualArrowPosition + ah / 2 - 4, 4);
            gp.quadTo(actualArrowPosition + ah / 2, 4, actualArrowPosition + ah / 2 + 4, 4);
            gp.lineTo(actualArrowPosition + ah, y);            
            
            gp.lineTo(x + widthF - radius, y);            
            gp.quadTo(x + widthF, y, x + widthF, y + radius);
        } else {
            if(topRight) {
                gp.lineTo(x + widthF - radius, y);            
                gp.quadTo(x + widthF, y, x + widthF, y + radius);
            } else {
                gp.lineTo(x + widthF, y);
            }
        }
        
        if(bottomRight) {
            gp.lineTo(x + widthF, y + heightF - radius);
            gp.quadTo(x + widthF, y + heightF, x + widthF - radius, y + heightF);
        } else {
            gp.lineTo(x + widthF, y + heightF);
        }
        
        
        
        if((trackComponentSide >= 0 || getTrackComponent() != null) && arrowDirection == CN.BOTTOM) {
            int actualArrowPosition = (int)
                Math.min(x + widthF,
                    Math.max(arrowPosition, x + radius));
            gp.lineTo(actualArrowPosition, y + heightF);
            int ah = CN.convertToPixels(arrowSize);
            gp.lineTo(actualArrowPosition + ah / 2 - 4, y + heightF + ah - 4);
            gp.quadTo(actualArrowPosition + ah / 2, y + heightF + ah - 4, actualArrowPosition + ah / 2 + 4, y + heightF + ah - 4);
            gp.lineTo(actualArrowPosition + ah, y + heightF);            
            
            gp.lineTo(x + radius, y + heightF);
            gp.quadTo(x, y + heightF, x, y + heightF - radius);
        } else {
            if(bottomLeft) {
                gp.lineTo(x + radius, y + heightF);
                gp.quadTo(x, y + heightF, x, y + heightF - radius);
            } else {
                gp.lineTo(x, y + heightF);
            }
        }
        
        
        if(topLeft) {
            gp.lineTo(x, y + radius);
            gp.quadTo(x, y, x + radius, y);
        } else {
            gp.lineTo(x, y);            
        }
        
        
        gp.closePath();  
        
        if((trackComponentSide >= 0 || getTrackComponent() != null) && arrowDirection == CN.LEFT) {
            int ah = CN.convertToPixels(arrowSize);
            int actualArrowPosition = (int)
                Math.max(y,
                    Math.min(arrowPosition, y + heightF - radius - 4));

            gp.moveTo(0, actualArrowPosition);
            gp.lineTo(x, actualArrowPosition - ah / 2);
            gp.lineTo(x, actualArrowPosition + ah/2); 
            gp.lineTo(0, actualArrowPosition);
            gp.closePath();
        }
        if((trackComponentSide >= 0 || getTrackComponent() != null) && arrowDirection == CN.RIGHT) {
            int ah = CN.convertToPixels(arrowSize);
            int actualArrowPosition = (int)
                Math.max(y,
                    Math.min(arrowPosition, y + heightF - radius - 4));

            gp.moveTo(x + widthF + ah, actualArrowPosition);
            gp.lineTo(x + widthF, actualArrowPosition + ah/2);
            gp.lineTo(x + widthF, actualArrowPosition - ah/2);
            gp.lineTo(x + widthF + ah, actualArrowPosition);
            gp.closePath();
            
        } 
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

    
    
    private Stroke stroke1;
    
    private void drawShape(Graphics g, int color, int opacity, int width, int height) {
        g.setColor(color);
        g.setAlpha(opacity);
        GeneralPath gp = createShape(width, height);
        if (stroke1 == null) {
            stroke1 = new Stroke(1f, Stroke.CAP_ROUND, Stroke.JOIN_MITER, 1f);
        }
        g.drawShape(gp, stroke1);
                   
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
     * The color of the shadow as an RRGGBB color
     * @return the shadowColor
     */
    public int getShadowColor() {
        return shadowColor;
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
        return topLeft && topRight && (!bottomLeft) && (!bottomRight);
    }
    
    /**
     * Special mode where only the bottom of the round rectangle is rounded and the top is a regular rectangle
     * 
     * @return whether this is the bottom only mode
     */
    public boolean isBottomOnlyMode() {
        return (!topLeft) && (!topRight) && bottomLeft && bottomRight;
    }
    
    

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }

    

    /**
     * Returns true if this border corner is round and false if it's square
     * @return the topLeft value
     */
    public boolean isTopLeft() {
        return topLeft;
    }

    /**
     * Returns true if this border corner is round and false if it's square
     * @return the topRight value
     */
    public boolean isTopRight() {
        return topRight;
    }

    /**
     * Returns true if this border corner is round and false if it's square
     * @return the bottomLeft value
     */
    public boolean isBottomLeft() {
        return bottomLeft;
    }

    /**
     * Returns true if this border corner is round and false if it's square
     * @return the bottomRight value
     */
    public boolean isBottomRight() {
        return bottomRight;
    }
    
    
}
