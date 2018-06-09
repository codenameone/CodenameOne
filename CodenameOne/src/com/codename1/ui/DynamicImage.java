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

import com.codename1.ui.plaf.Style;

/**
 * A base class for images that dynamically painted, just like a normal component. Subclasses
 * just need to implement the {@link #drawImageImpl(com.codename1.ui.Graphics, java.lang.Object, int, int, int, int) }
 * method.
 * @author shannah
 */
public abstract class DynamicImage extends Image {

    private int w = 250, h = 250;
    private Style style;

    /**
     * Constructor.  Creates image of default size: 250x250 pixels.
     */
    public DynamicImage() {
        super(null);
    }

    /**
     * Constructor with width and height.
     * @param w The width of the image.
     * @param h The height of the image.
     */
    public DynamicImage(int w, int h) {
        super(null);
        this.w = w;
        this.h = h;
    }
    
    /**
     * Sets the style to be used for drawing the image.  The {@link #drawImageImpl(com.codename1.ui.Graphics, java.lang.Object, int, int, int, int) }
     * can use any aspect of this style to customize the drawing.
     * @param s The style.
     */
    public void setStyle(Style s) {
        style = s == null ? null : new Style(s);
    }
    
    /**
     * Gets the style for this image.
     * @return 
     */
    public Style getStyle() {
        return style;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int getWidth() {
        return w;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int getHeight() {
        return h;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void scale(int width, int height) {
        w = width;
        h = height;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Image fill(int width, int height) {
        try {
            DynamicImage img = (DynamicImage) this.getClass().newInstance();
            img.w = width;
            img.h = height;
            img.setStyle(style);
            return img;
        } catch (Throwable t) {
            throw new RuntimeException(t.getMessage());
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Image applyMask(Object mask) {
        return fill(w, h);
    }

    @Override
    public boolean requiresDrawImage() {
        return true;
    }

    /**
     * This method should be overridden by subclasses to perform the actual drawing of 
     * the image on a graphics context.
     * @param g The graphics context
     * @param nativeGraphics THe native graphics context.
     * @param x x-coordinate of the bounds to draw.
     * @param y y-coordinate of the bounds to draw.
     * @param w width of the bounds
     * @param h height of the bounds
     */
    protected abstract void drawImageImpl(Graphics g, Object nativeGraphics, int x, int y, int w, int h);

    /**
     * {@inheritDoc }
     */
    @Override
    protected void drawImage(Graphics g, Object nativeGraphics, int x, int y) {
        drawImageImpl(g, nativeGraphics, x, y, w, h);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    protected void drawImage(Graphics g, Object nativeGraphics, int x, int y, int w, int h) {
        drawImageImpl(g, nativeGraphics, x, y, w, h);
    }
    
    /**
     * Sets the given image as the icon for the specified label.  This will link the 
     * label's style with the image just before each call the {@link #drawImageImpl(com.codename1.ui.Graphics, java.lang.Object, int, int, int, int) }
     * so that the image can correctly adapt to the label's style.
     */
    public static void setIcon(final Label lbl, final DynamicImage img) {
        
        DynamicImage wrapper = new DynamicImage() {

            @Override
            protected void drawImageImpl(Graphics g, Object nativeGraphics, int x, int y, int w, int h) {
                img.setStyle(lbl.getStyle());
                img.drawImageImpl(g, nativeGraphics, x, y, w, h);
            }
            
        };
        wrapper.w = img.w;
        wrapper.h = img.h;
        lbl.setIcon(wrapper);
    }

};
