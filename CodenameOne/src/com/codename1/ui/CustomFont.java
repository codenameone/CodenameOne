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

import java.util.Hashtable;



/**
 * Implements a bitmap font that uses an image and sets of offsets to draw a font
 * with a given character set.
 *
 * @author Shai Almog
 */
class CustomFont extends Font {
    /**
     * Keep two colors in cache by default to allow faster selection colors
     */
    private static final int COLOR_CACHE_SIZE = 20;
    
    private Hashtable colorCache = new Hashtable();

    private String charsets;
    private int color;
    
    // package protected for the resource editor
    Image cache;
    
    /**
     * The offset in which to cut the character from the bitmap
     */
    int[] cutOffsets;

    /**
     * The width of the character when drawing... this should not be confused with
     * the number of cutOffset[o + 1] - cutOffset[o]. They are completely different
     * since a character can be "wider" and "seep" into the next region. This is
     * especially true with italic characters all of which "lean" outside of their 
     * bounds.
     */
    int[] charWidth;

    private int imageWidth;
    private int imageHeight;
    private Object imageArrayRef;
    
    
    private int[] getImageArray() {
        if(imageArrayRef != null) {
            int[] a = (int[])Display.getInstance().extractHardRef(imageArrayRef);
            if(a != null) {
                return a;
            }
        }
        int[] a = cache.getRGBCached();
        
        imageArrayRef = Display.getInstance().createSoftWeakRef(a);
        return a;
    }
    
    /**
     * Creates a bitmap font with the given arguments
     * 
     * @param bitmap a transparency map in red and black that indicates the characters
     * @param cutOffsets character offsets matching the bitmap pixels and characters in the font 
     * @param charWidth The width of the character when drawing... this should not be confused with
     *      the number of cutOffset[o + 1] - cutOffset[o]. They are completely different
     *      since a character can be "wider" and "seep" into the next region. This is
     *      especially true with italic characters all of which "lean" outside of their 
     *      bounds.
     * @param charsets the set of characters in the font
     * @return a font object to draw bitmap fonts
     */
    public CustomFont(Image bitmap, int[] cutOffsets, int[] charWidth, String charsets) {
        this.cutOffsets = cutOffsets;
        this.charWidth = charWidth;
        this.charsets = charsets;
        imageWidth = bitmap.getWidth();
        imageHeight = bitmap.getHeight();
        int[] imageArray = new int[imageWidth * imageHeight];
        
        // default to black colored font
        bitmap.getRGB(imageArray, 0, 0, 0, imageWidth, imageHeight);
        for(int iter = 0 ; iter < imageArray.length ; iter++) {
            // extract the red component from the font image
            // shift the alpha 8 bits to the left
            // apply the alpha to the image
            imageArray[iter] = ((imageArray[iter] & 0xff0000) << 8);
        }
        cache = Image.createImage(imageArray, imageWidth, imageHeight);
        imageArrayRef = Display.getInstance().createSoftWeakRef(imageArray);
    }
    
    /**
     * @inheritDoc
     */
    public int charWidth(char ch) {
        int i = charsets.indexOf(ch);
        if(i < 0) {
            return 0;
        }
        return charWidth[i];
    }

    /**
     * @inheritDoc
     */
    public int getHeight() {
        return imageHeight;
    }

    private boolean checkCacheCurrentColor(int newColor) {
        Integer currentColor = new Integer(color);
        Integer newColorKey = new Integer(newColor);
        if(colorCache.get(currentColor) == null){
            colorCache.put(currentColor, Display.getInstance().createSoftWeakRef(cache));
        }
        color = newColor;
        Object newCache = Display.getInstance().extractHardRef(colorCache.get(newColorKey));
        if(newCache != null) {
            Image i = (Image)newCache;
            if(i != null) {
                cache = i;
                if(colorCache.size() > COLOR_CACHE_SIZE) {
                    // remove a random cache element
                    colorCache.remove(colorCache.keys().nextElement());
                }
                return true;
            }else{
                colorCache.remove(newColorKey);
            }
        }
        if(colorCache.size() > COLOR_CACHE_SIZE) {
            // remove a random cache element
            colorCache.remove(colorCache.keys().nextElement());
        }        
        return false;
    }
    
    private void initColor(Graphics g) {
        int newColor = g.getColor();
        
        if(newColor != color && !checkCacheCurrentColor(newColor)) {
            color = newColor & 0xffffff;
            int[] imageArray = getImageArray();
            for(int iter = 0 ; iter < imageArray.length ; iter++) {
                // extract the red component from the font image
                // shift the alpha 8 bits to the left
                // apply the alpha to the image
                imageArray[iter] = color | (imageArray[iter] & 0xff000000);
            }
            cache = Image.createImage(imageArray, imageWidth, imageHeight);
        }
    }
    
    /**
     * @inheritDoc
     */
    void drawChar(Graphics g, char character, int x, int y) {
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipWidth = g.getClipWidth();
        int clipHeight = g.getClipHeight();
        //g.pushClip();
        int i = charsets.indexOf(character);
        if(i > -1) {
            initColor(g);
            
            // draw region is flaky on some devices, use setClip instead
            g.clipRect(x, y, charWidth[i], imageHeight);
            g.drawImage(cache, x - cutOffsets[i], y);
            //g.drawRegion(cache, cutOffsets[i], 0, charWidth[i], imageHeight, x, y);
        }

        // restore the clip
        g.setClip(clipX, clipY, clipWidth, clipHeight);
        //g.popClip();
    }

    /**
     * @inheritDoc
     */
    public void addContrast(byte value) {
        int[] imageArray = getImageArray();
        for(int iter = 0 ; iter < imageArray.length ; iter++) {
            int alpha = (imageArray[iter] >> 24) & 0xff;
            if(alpha != 0) {
                alpha = Math.min(alpha + value, 255);
                imageArray[iter] = ((alpha << 24) & 0xff000000) | color;
            }
        }
    }

    /**
     * Override this frequently used method for a slight performance boost...
     * 
     * @param g the component graphics
     * @param data the chars to draw
     * @param offset the offset to draw the chars 
     * @param length the length of chars 
     * @param x the x coordinate to draw the chars
     * @param y the y coordinate to draw the chars
     */
    void drawChars(Graphics g, char[] data, int offset, int length, int x, int y) {
        if(Display.getInstance().isBidiAlgorithm()) {
            for(int i = offset ; i < length ; i++) {
                if(Display.getInstance().isRTL(data[i])) {
                    String s = Display.getInstance().convertBidiLogicalToVisual(new String(data, offset, length));
                    data = s.toCharArray();
                    offset = 0;
                    length = s.length();
                    break;
                }
            }
        }
        initColor(g);
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipWidth = g.getClipWidth();
        int clipHeight = g.getClipHeight();
        if(clipY <= y + getHeight() && clipY + clipHeight >= y) {
            char c;
            for ( int i = 0; i < length; i++ ) {
                c = data[offset+i];
                int position = charsets.indexOf(c);
                if(position < 0) {
                    continue;
                }
                // draw region is flaky on some devices, use setClip instead
                //g.pushClip();
                g.clipRect(x, y, charWidth[position], imageHeight);
                if(g.getClipWidth() > 0 && g.getClipHeight() > 0) {
                    g.drawImage(cache, x - cutOffsets[position], y);
                }
                x += charWidth[position];
                //g.popClip();
                g.setClip(clipX, clipY, clipWidth, clipHeight);
                
            }
        }
    }

    /**
     * @inheritDoc
     */
    public String getCharset() {
        return charsets;
    }

    /**
     * @inheritDoc
     */
    public int charsWidth(char[] ch, int offset, int length){
        int retVal = 0;
        for(int i=0; i<length; i++){
            retVal += charWidth(ch[i + offset]);
        }
        return retVal;
    }


    /**
     * @inheritDoc
     */
    public int substringWidth(String str, int offset, int len){
        return charsWidth(str.toCharArray(), offset, len);
    }

    /**
     * @inheritDoc
     */
    public int stringWidth(String str){
        if( str==null || str.length()==0)
            return 0;
        return substringWidth(str, 0, str.length());
    }

    /**
     * @inheritDoc
     */
    public int getFace(){
        return 0;
    }

    /**
     * @inheritDoc
     */
    public int getSize(){
        return 0;
    }

    /**
     * @inheritDoc
     */
    public int getStyle() {
        return 0;
    }
    
    /**
    * @inheritDoc
    */
   public boolean equals(Object o) {
       if(o == this) {
           return true;
       }
       if(o != null && o.getClass() == getClass()) {
           CustomFont f = (CustomFont)o;
           if(charsets.equals(f.charsets)) {
               for(int iter = 0 ; iter < cutOffsets.length ; iter++) {
                   if(cutOffsets[iter] != f.cutOffsets[iter]) {
                       return false;
                   }
               }
               return true;
           }
       }
       return false;
   }
}
