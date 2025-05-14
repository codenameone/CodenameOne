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
package com.codename1.ui;


/**
 * A utility wrapper that allows a Component to be used as an Image so that it can 
 * be set as the icon for a Label or button.
 *
 * @author shannah
 * @since 8.0
 */
public class ComponentImage extends Image {

    private Component cmp;
    private int w;
    private int h;
    private boolean pulsingAnimation;
    private double minPulsingAlpha = 0.2, maxPulsingAlpha = 1.0;
    private double pulsingStepSize = 1;
    private double pulsingCurrStep = 0;
    private boolean animation;

    /**
     * Creates a new image that renders the given component.
     * @param cmp The component to render.
     * @param w The width of the image.
     * @param h The height of the image.
     */
    public ComponentImage(Component cmp, int w, int h) {
        super(null);
        this.cmp = cmp;
        this.w = w;
        this.h = h;
    }

    /**
     * Creates a new image that renders the given component.  Uses the components current
     * dimensions as the image width and height.
     * @param cmp The component to render.
     */
    ComponentImage(Component cmp) {
        super(null);
        this.cmp = cmp;
        w = cmp.getWidth();
        h = cmp.getHeight();
    }

    /**
     * Creates a new image that renders an empty component.
     * @param w The width of the image
     * @param h The height of the image.
     */
    ComponentImage(int w, int h) {
        super(null);
        cmp = new Label();
        this.w = w;
        this.h = h;
    }

    /**
     * Gets the wrapped component that is rendered by this image.
     * @return 
     */
    public Component getComponent() {
        return cmp;
    }

    /**
     * Enables a pulsing animation on the image.
     * @param currStep The current step.
     * @param stepSize The step size.
     * @param minAlpha The min alpha
     * @param maxAlpha The max alpha
     */
    public void enablePulsingAnimation(double currStep, double stepSize, double minAlpha, double maxAlpha) {
        minAlpha = Math.min(1, Math.max(0, minAlpha));
        maxAlpha = Math.min(1, Math.max(0, maxAlpha));
        this.pulsingAnimation = true;
        this.pulsingCurrStep = currStep;
        this.pulsingStepSize = stepSize;
        this.minPulsingAlpha = minAlpha;
        this.maxPulsingAlpha = maxAlpha;
    }

    /**
     * Disables the pulsing animation.
     */
    public void disablePulsingAnimation() {
        pulsingAnimation = false;

    }

    /**
     * Checks if pulsing animation is enabled.
     * @return 
     */
    public boolean isPulsingAnimationEnabled() {
        return pulsingAnimation;
    }

    /**
     * {@inheritDoc }
     * @return 
     */
    @Override
    public int getWidth() {
        return w;
    }

    /**
     * {@inheritDoc }
     * @return 
     */
    @Override
    public int getHeight() {
        return h;
    }

    /**
     * {@inheritDoc }
     * @param width
     * @param height 
     */
    @Override
    public void scale(int width, int height) {
        w = width;
        h = height;
    }

    @Override
    public Image fill(int width, int height) {
        ComponentImage out = new ComponentImage(cmp);
        out.w = width;
        out.h = height;
        return out;
    }

    /**
     * {@inheritDoc }
     * @param mask
     * @return 
     */
    @Override
    public Image applyMask(Object mask) {
        return new ComponentImage(cmp, w, h);
    }

    /**
     * Sets whether this in an animation.
     * @param anim True to make this an animated image
     */
    public void setAnimation(boolean anim) {
        this.animation = anim;
    }

    /**
     * Checks if this is an animation.
     * @return 
     */
    @Override
    public boolean isAnimation() {
        return animation || pulsingAnimation;
    }

    /**
     * Overridden to always return true so that the paint() method is called
     * on this image.
     * @return 
     */
    @Override
    public boolean requiresDrawImage() {
        return true;
    }

    /**
     * Draws the image.
     * @param g The graphics context
     * @param nativeGraphics The native graphics context
     * @param x The x coordinate at which to draw the image.
     * @param y The y coordinate at which to draw the image.
     */
    @Override
    protected void drawImage(Graphics g, Object nativeGraphics, int x, int y) {
        int tx = g.getTranslateX();
        int ty = g.getTranslateY();
        
        int oldX = cmp.getX();
        int oldY = cmp.getY();
        int oldW = cmp.getWidth();
        int oldH = cmp.getHeight();
        cmp.setX(x);
        cmp.setY(y);
        cmp.setWidth(w);
        cmp.setHeight(h);
        int col = g.getColor();
        g.setColor(0x00ff00);
        
        g.setColor(col);

        boolean antialias = g.isAntiAliased();
        g.setAntiAliased(true);

        int alpha = g.getAlpha();
        if (pulsingAnimation) {
            double sinVal = (Math.sin(pulsingCurrStep) + 1)/2;
            sinVal = minPulsingAlpha + (maxPulsingAlpha - minPulsingAlpha) * sinVal;
            g.setAlpha((int)Math.round(sinVal * alpha));
        }
        Font font = g.getFont();
        int color = g.getColor();
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipW = g.getClipWidth();
        int clipH = g.getClipHeight();

        cmp.paintComponent(g, true);
        g.setFont(font);
        g.setColor(color);
        g.setClip(clipX, clipY, clipW, clipH);

        if (pulsingAnimation) {
            g.setAlpha(alpha);
        }
        g.setAntiAliased(antialias);
        cmp.setX(oldX);
        cmp.setY(oldY);
        cmp.setWidth(oldW);
        cmp.setHeight(oldH);
        
    }

    /**
     * {@inheritDoc }
     */
    @Override
    protected void drawImage(Graphics g, Object nativeGraphics, int x, int y, int w, int h) {
        int oldW = this.w;
        int oldH = this.h;
        drawImage(g, nativeGraphics, x, y);
        this.w = oldW;
        this.h = oldH;
    }

    /**
     * {@inheritDoc }
     * 
     */
    @Override
    public Image scaled(int width, int height) {
        return new ComponentImage(cmp, width, height);
    }



    /**
     * {@inheritDoc }
     */
    @Override
    public boolean animate() {
        if (pulsingAnimation) {
            pulsingCurrStep += pulsingStepSize;
            if (pulsingCurrStep >= Math.PI * 2) {
                pulsingCurrStep -= Math.PI * 2;
            }
        }
        cmp.animate();
        return pulsingAnimation || animation;
    }

    /**
     * Converts to an encoded image.
     * @return 
     */
    public EncodedImage toEncodedImage() {
        return new EncodedWrapper();
    }


    /**
     * A wrapper for ComponentImage to convert it to an EncodedImage.
     */
    public class EncodedWrapper extends EncodedImage {

        EncodedWrapper() {
            super(ComponentImage.this.getWidth(), ComponentImage.this.getHeight());
        }

        /**
         * {@inheritDoc }
         */
        @Override
        public EncodedImage scaledEncoded(int width, int height) {
            return new ComponentImage(cmp, width, height).toEncodedImage();

        }

        /**
         * {@inheritDoc }
         */
        @Override
        public Image scaled(int width, int height) {
            return new ComponentImage(cmp, width, height).toEncodedImage();
        }



        /**
         * {@inheritDoc }
         */
        @Override
        protected Image getInternal() {
            return ComponentImage.this;
        }

    }


};