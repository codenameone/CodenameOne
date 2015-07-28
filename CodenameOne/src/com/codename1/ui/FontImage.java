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
import com.codename1.ui.util.ImageIO;

/**
 * An image based on icon fonts that adapts its environment
 * 
 * @author Shai Almog
 */
public class FontImage extends Image {

    /**
     * Default factor for image size, icons without a given size are sized as defaultSize X default font height.
     * @return the defaultSize
     */
    public static float getDefaultSize() {
        return defaultSize;
    }

    /**
     * Default factor for image size, icons without a given size are sized as defaultSize X default font height.
     * @param aDefaultSize the defaultSize to set
     */
    public static void setDefaultSize(float aDefaultSize) {
        defaultSize = aDefaultSize;
    }
    
    
    private static int defaultPadding = 1;

    /**
     * Indicates the default value for the padding in millimeters 
     * @return the defaultPadding
     */
    public static int getDefaultPadding() {
        return defaultPadding;
    }

    /**
     * Indicates the default value for the padding in millimeters
     * @param aDefaultPadding the defaultPadding to set
     */
    public static void setDefaultPadding(int aDefaultPadding) {
        defaultPadding = aDefaultPadding;
    }

    /**
     * The padding for the image in millimeters
     */
    private int padding = defaultPadding;
    
    private int width;
    private int height;
    private int color;
    private Font fnt;
    private String text;
    private int rotated;
    private int backgroundColor;
    private byte backgroundOpacity;
    
    /**
     * Default factor for image size, icons without a given size are sized as defaultSize X default font height.
     */
    private static float defaultSize = 2.5f;
    
    private FontImage() {
        super(null);
    }
    
    /**
     * Creates a font image
     * @param text the text of the font image
     * @param fnt the font
     * @param color the color for the image foreground
     * @param width the width in pixels
     * @param height the height in pixels
     * @return the image instance
     */
    public static FontImage createFixed(String text, Font fnt, int color, int width, int height) {
        FontImage f = new FontImage();
        f.text = text;
        f.color = color;
        f.width = width;
        f.fnt = sizeFont(fnt, Math.min(width, height), f.padding);
        f.height = height;
        return f;
    }
    
    /**
     * Creates the font image
     * @param text the text for the font image
     * @param s the style
     * @return the font image
     */
    public static FontImage create(String text, Style s) {
        FontImage f = new FontImage();
        f.backgroundOpacity = s.getBgTransparency();
        f.backgroundColor = s.getBgColor();
        f.text = text;
        f.color = s.getFgColor();
        int w = (int)(((float)Font.getDefaultFont().getHeight()) * defaultSize);
        f.fnt = sizeFont(s.getFont(), w, f.padding);
        f.width = w;
        f.height = w;
        return f;
    }

    private static Font sizeFont(Font fnt, int w, int padding) {
        int paddingPixels = Display.getInstance().convertToPixels(padding, true);
        w -= paddingPixels;
        int h = fnt.getHeight();
        if(h != w) {
            return fnt.derive(w, Font.STYLE_PLAIN);
        }
        return fnt;
    }
    
    /**
     * Throws a runtime exception
     */
    public Graphics getGraphics() {
        throw new RuntimeException();
    }

    /**
     * Returns the width of the image
     * 
     * @return the width of the image
     */
    public int getWidth() {
        return width;
    }
        
    /**
     * Returns the height of the image
     * 
     * @return the height of the image
     */
    public int getHeight() {
        return height;
    }

    /**
     * @inheritDoc
     */
    protected void drawImage(Graphics g, Object nativeGraphics, int x, int y) {
        int oldColor = g.getColor();
        Font oldFont = g.getFont();
        
        if(backgroundOpacity != 0) {
            g.setColor(backgroundColor);
            g.fillRect(0, 0, width, height, (byte)backgroundOpacity);
        }
        
        g.setColor(color);
        g.setFont(fnt);
        int w = fnt.stringWidth(text);
        int paddingPixels = Display.getInstance().convertToPixels(padding, true);
        if(rotated != 0) {
            int tX = g.getTranslateX();
            int tY = g.getTranslateY();
            g.translate(-tX, -tY);
            g.rotate((float)Math.toRadians(rotated % 360), tX + x + width / 2, tY + y + height / 2 + paddingPixels);
            g.drawString(text, tX + x + width / 2 - w / 2, tY + y + paddingPixels);
            g.resetAffine();
            g.translate(tX, tY);
        } else {
            g.drawString(text, x + width / 2 - w / 2, y + paddingPixels);
        }
        g.setFont(oldFont);
        g.setColor(oldColor);
    }

    /**
     * @inheritDoc
     */
    protected void drawImage(Graphics g, Object nativeGraphics, int x, int y, int w, int h) {
        if(w == width && h == height) {
            drawImage(g, nativeGraphics, x, y);
            return;
        }
        int oldColor = g.getColor();
        
        if(backgroundOpacity != 0) {
            g.setColor(backgroundColor);
            g.fillRect(0, 0, w, h, (byte)backgroundOpacity);
        }        
        
        Font oldFont = g.getFont();
        Font t = sizeFont(fnt, Math.min(h, w), padding);
        g.setColor(color);
        g.setFont(t);
        int ww = t.stringWidth(text);
        int paddingPixels = Display.getInstance().convertToPixels(padding, true);
        if(rotated != 0) {
            int tX = g.getTranslateX();
            int tY = g.getTranslateY();
            g.translate(-tX, -tY);
            g.rotate((float)Math.toRadians(rotated % 360), tX + x + w / 2, tY + y + h / 2 + paddingPixels);
            g.drawString(text, tX + x + w / 2 - ww / 2, tY + y + paddingPixels);
            g.resetAffine();
            g.translate(tX, tY);
        } else {
            g.drawString(text, x + w / 2 - ww / 2, y + paddingPixels);
        }
        g.setFont(oldFont);
        g.setColor(oldColor);
    }

    /**
     * The padding for the image in millimeters
     * @return the padding
     */
    public int getPadding() {
        return padding;
    }

    /**
     * The padding for the image in millimeters
     * @param padding the padding to set
     */
    public void setPadding(int padding) {
        if(this.padding != padding) {
            this.padding = padding;
            fnt = sizeFont(fnt, Math.min(width, height), padding);
        }
    }
    
    /**
     * Useful method to reuse the Font object when creating multiple image objects
     * @return the font used
     */
    public Font getFont() {
        return fnt;
    }
    
    void getRGB(int[] rgbData,
            int offset,
            int x,
            int y,
            int width,
            int height){
        throw new RuntimeException("Unsupported Operation");
    }
    
    int[] getRGBImpl() {
        throw new RuntimeException("Unsupported Operation");
    }

    Image scaledImpl(int width, int height) {        
        return createFixed(text, fnt, color, width, height);
    }
    
    /**
     * @inheritDoc 
     */
    public boolean isAnimation() {
        return false;
    }

    /**
     * @inheritDoc 
     */
    public boolean animate() {
        return false;
    }
    
    /**
     * @inheritDoc 
     */
    public boolean isOpaque() {
        return false;
    }
    
    /**
     * @inheritDoc 
     */
    public String getImageName() {
        return text;
    }    

    /**
     * Does nothing
     */
    public void dispose(){
    }

    /**
     * @inheritDoc 
     */
    public Image rotate(int degrees) {
        FontImage f = createFixed(text, fnt, color, width, height);
        f.rotated = degrees;
        return f;
    }
    
    /**
     * Converts the icon image to an encoded image if possible
     * @return the encoded image or null if the operation failed
     */
    public EncodedImage toEncodedImage() {
        ImageIO io = ImageIO.getImageIO();
        if(io != null && io.isFormatSupported(ImageIO.FORMAT_PNG)) {
            Image img = toImage();
            if(img != null) {
                return EncodedImage.createFromImage(img, false);
            }
        }
        return null;
    } 
    
    /**
     * Converts the icon image to an image if possible
     * @return the encoded image or null if the operation failed
     */
    public Image toImage() {
        if(Image.isAlphaMutableImageSupported()) {
            Image img = Image.createImage(width, height, 0);
            Graphics g = img.getGraphics();
            g.drawImage(this, 0, 0);
            return img;
        }
        return null;
    }
}
