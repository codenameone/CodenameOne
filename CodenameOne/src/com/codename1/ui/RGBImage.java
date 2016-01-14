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

/**
 * An image that stores its data as an integer RGB array internally,
 * this image cannot be manipulated via Graphics primitives however its
 * array is accessible and modifiable programmatically. This is very useful
 * for 2 distinct use cases. 
 * <p>The first use case allows us to manipulate images in 
 * a custom way while still preserving alpha information where applicable.
 * <p>The second use case allows us to store images in the Java heap which is useful
 * for some constrained devices. In small devices images are often stored 
 * in a separate "heap" which runs out eventually, this allows us to place
 * the image in the Java heap which is potentially more wasteful but might
 * sometimes be more abundant. 
 * <p>Note that unless specified otherwise most methods inherited from Image will
 * fail when invoked on this subclass often with a NullPointerException. This
 * image can be drawn on graphics as usual
 * 
 * @author Shai Almog
 */
public class RGBImage extends Image {
    private int width;
    private int height;
    private int[] rgb;
    private boolean opaque;
    
    /**
     * Converts an image to an RGB image after which the original image can be GC'd
     * 
     * @param img the image to convert to an RGB image
     */
    public RGBImage(Image img) {
        super(null);
        width = img.getWidth();
        height = img.getHeight();
        rgb = img.getRGBCached();
    }

    /**
     * Creates an RGB image from scratch the array isn't copied and can be saved
     * and manipulated
     * 
     * @param rgb AARRGGBB array
     * @param width width of image
     * @param height height of image
     */
    public RGBImage(int[] rgb, int width, int height) {
        super(null);
        this.width = width;
        this.height = height;
        this.rgb = rgb;
    }

    /**
     * @inheritDoc
     */
    public Image subImage(int x, int y, int width, int height, boolean processAlpha)  {
        int[] arr = new int[width * height];
        int alen = arr.length;
        for(int iter = 0 ; iter < alen ; iter++) {
            int destY = iter / width;
            int destX = iter % width;
            int offset = x + destX + ((y + destY) * this.width);
            arr[iter] = rgb[offset];
        }
        
        return new RGBImage(arr, width, height);
    }

    /**
     * @inheritDoc
     */
    public Image scaled(int width, int height) {
        int srcWidth = getWidth();
        int srcHeight = getHeight();

        // no need to scale
        if(srcWidth == width && srcHeight == height){
            return this;
        }
        int[] currentArray = new int[srcWidth];
        int[] destinationArray = new int[width * height];
        scaleArray(srcWidth, srcHeight, height, width, currentArray, destinationArray);

        // currently we only support byte data...
        return new RGBImage(destinationArray, width, height);
    }

    /**
     * @inheritDoc
     */
    public void scale(int width, int height) {
        int srcWidth = getWidth();
        int srcHeight = getHeight();

        // no need to scale
        if(srcWidth == width && srcHeight == height){
            return;
        }
        int[] currentArray = new int[srcWidth];
        int[] destinationArray = new int[width * height];
        scaleArray(srcWidth, srcHeight, height, width, currentArray, destinationArray);

        this.width = width;
        this.height = height;
        this.rgb = destinationArray;
    }

    /**
     * Unsupported in the current version, this method will be implemented in a future release
     */
    public Image rotate(int degrees) {
        throw new RuntimeException("The rotate method is not supported by RGB images at the moment");
    }
    
    /**
     * @inheritDoc
     */
    public Image modifyAlpha(byte alpha) {
        int[] arr = new int[rgb.length];
        System.arraycopy(rgb, 0, arr, 0, rgb.length);
        int alphaInt = (((int)alpha) << 24) & 0xff000000;
        int rlen = rgb.length;
        for(int iter = 0 ; iter < rlen ; iter++) {
            if((arr[iter] & 0xff000000) != 0) {
                arr[iter] = (arr[iter] & 0xffffff) | alphaInt;
            }
        }
        return new RGBImage(arr, width, height);
    }
    
    /**
     * This method is unsupported in this image type
     */
    public Graphics getGraphics() {
        throw new RuntimeException("RGBImage objects can't be modified via graphics");
    }
    
    
    /**
     * Returns a mutable array that can be used to change the appearance of the image
     * arranged as AARRGGBB.
     * 
     * @return ARGB int array
     */
    public int[] getRGB() {
        return rgb;
    }

    /**
     * @inheritDoc
     */
    void getRGB(int[] rgbData,
            int offset,
            int x,
            int y,
            int width,
            int height){
        int startPoint = y * this.width + x;
        for(int rows = 0 ; rows < height ; rows++) {
            int currentRow = rows * width;
            for(int columns = 0 ; columns < width ; columns++) {
                rgbData[offset + currentRow + columns] = rgb[startPoint + columns];
            }
            startPoint += this.width;
        }
    }

    /**
     * @inheritDoc
     */
    protected void drawImage(Graphics g, Object nativeGraphics, int x, int y) {
       g.drawRGB(rgb, 0, x, y, width, height, !opaque);
    }    

    /**
     * @inheritDoc
     */
    public void setOpaque(boolean opaque) {
        this.opaque = opaque;
    }

    /**
     * Indicates if an image should be treated as opaque, this can improve support
     * for fast drawing of RGB images without alpha support.
     */
    public boolean isOpaque() {
        return opaque;
    }
    
    /**
     * @inheritDoc
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * @inheritDoc
     */
    public int getHeight() {
        return height;
    }

    /**
     * @inheritDoc
     */
    @Override
    protected boolean requiresDrawImage() {
        return true;
    }
}
