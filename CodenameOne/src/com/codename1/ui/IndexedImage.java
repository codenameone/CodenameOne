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

import com.codename1.ui.geom.Dimension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * An indexed image is an image "compressed" in memory to occupy as little memory 
 * as possible in this sense it is slower to draw and only a single indexed image
 * can be drawn at any given time. However, this allows images with low color counts
 * to use as little as one byte per pixel which can save up to 4 times of the memory
 * overhead. 
 *
 * @deprecated This class should no longer be referenced directly. Use Image.createIndexed instead
 * @author Shai Almog
 */
class IndexedImage extends Image {
    private int width;
    private int height;
    
    // package protected for access by the resource editor
    byte[] imageDataByte;
    int[] palette; 
    
    /**
     * Creates an indexed image with byte data
     * 
     * @param width image width
     * @param height image height
     * @param palette the color palette to use with the byte data
     * @param data byte data containing palette offsets to map to ARGB colors
     * @deprecated use Image.createIndexed instead
     */
    public IndexedImage(int width, int height, int[] palette, byte[] data) {
        super(null);
        this.width = width;
        this.height = height;
        this.palette = palette;
        this.imageDataByte = data;
        initOpaque();
    }

    private void initOpaque() {
        if(palette != null) {
            for(int iter = 0 ; iter < palette.length ; iter++) {
                if((palette[iter] & 0xff000000) != 0xff000000) {
                    setOpaque(false);
                    return;
                }
            }
            setOpaque(true);
        } else {
            setOpaque(false);
        }
    }

    /**
     * Converts an image to a package image after which the original image can be GC'd
     */
    private IndexedImage(int width, int height, int[] palette, int[] rgb) {
        super(null);
        
        this.width = width;
        this.height = height;
        this.palette = palette;
        
        // byte based package image
        imageDataByte = new byte[width * height];
        for(int iter = 0 ; iter < imageDataByte.length ; iter++) {
            imageDataByte[iter] = (byte)paletteOffset(rgb[iter]);
        }
        initOpaque();
    }

    /**
     * Finds the offset within the palette of the given rgb value
     * 
     * @param value ARGB value from the image
     * @return offset within the palette array
     */
    private int paletteOffset(int rgb) {
        for(int iter = 0 ; iter < palette.length ; iter++) {
            if(rgb == palette[iter]) {
                return iter;
            }
        }
        throw new IllegalStateException("Invalid palette request in paletteOffset");
    }

    /**
     * Packs the image loaded by MIDP
     * 
     * @param imageName a name to load using Image.createImage()
     * @return a packed image
     * @throws IOException when create fails
     */
    public static Image pack(String imageName) throws IOException {
        return pack(Image.createImage(imageName));
    }

    /**
     * @inheritDoc
     */
    public Image subImage(int x, int y, int width, int height, boolean processAlpha)  {
        byte[] arr = new byte[width * height];
        for(int iter = 0 ; iter < arr.length ; iter++) {
            int destY = iter / width;
            int destX = iter % width;
            int offset = x + destX + ((y + destY) * this.width);
            arr[iter] = imageDataByte[offset];
        }
        
        return new IndexedImage(width, height, palette, arr);
    }

    /**
     * Unsupported in the current version, this method will be implemented in a future release
     */
    public Image rotate(int degrees) {
        throw new RuntimeException("The rotate method is not supported by indexed images at the moment");
    }

    /**
     * @inheritDoc
     */
    public Image modifyAlpha(byte alpha) {
        int[] newPalette = new int[palette.length];
        System.arraycopy(palette, 0, newPalette, 0, palette.length);
        int alphaInt = (((int)alpha) << 24) & 0xff000000;
        for(int iter = 0 ; iter < palette.length ; iter++) {
            if((palette[iter] & 0xff000000) != 0) {
                newPalette[iter] = (palette[iter] & 0xffffff) | alphaInt;
            }
        }
        return new IndexedImage(width, height, newPalette, imageDataByte);
    }
    
    /**
     * This method is unsupported in this image type
     */
    public Graphics getGraphics() {
        throw new RuntimeException("Indexed image objects are immutable");
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
        // need to support scanlength???
        int startPoint = y * this.width + x;
        for(int rows = 0 ; rows < height ; rows++) {
            int currentRow = rows * width;
            for(int columns = 0 ; columns < width ; columns++) {
                int i = imageDataByte[startPoint + columns] & 0xff;
                rgbData[offset + currentRow + columns] = palette[i];
            }
            startPoint += this.width;
        }
    }
    
    
    /**
     * Packs the source rgba image and returns null if it fails
     * 
     * @param rgb array containing ARGB data
     * @param width width of the image in the rgb array
     * @param height height of the image
     * @return a packed image or null
     */
    public static IndexedImage pack(int[] rgb, int width, int height) {
        int arrayLength = width * height;
        
        // using a Vector is slower for a small scale device and this is mission critical code
        int[] tempPalette = new int[256];
        int paletteLocation = 0;
        for(int iter = 0 ; iter < arrayLength ; iter++) {
            int current = rgb[iter];
            if(!contains(tempPalette, paletteLocation, current)) {
                if(paletteLocation > 255) {
                    return null;
                }
                tempPalette[paletteLocation] = current;
                paletteLocation++;
            }
        }

        // we need to "shrink" the palette array
        if(paletteLocation != tempPalette.length) {
            int[] newArray = new int[paletteLocation];
            System.arraycopy(tempPalette, 0, newArray, 0, paletteLocation);
            tempPalette = newArray;
        }
        
        
        IndexedImage i = new IndexedImage(width, height, tempPalette, rgb);
        return i;
    }
    
    /**
     * Tries to pack the given image and would return the packed image or source
     * image if packing failed
     * 
     * @param sourceImage the image which would be converted to a packed image if possible
     * @return the source image if packing failed or a newly packed image if it succeeded
     */
    public static Image pack(final Image sourceImage) {
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();
        int[] rgb = sourceImage.getRGBCached();
        
        Image i = pack(rgb, width, height);
        if(i == null) {
            return sourceImage;
        }
        return i;
    }
    
    
    /**
     * Searches the array up to "length" and returns true if value is within the
     * array up to that point.
     */
    private static boolean contains(int[] array, int length, int value) {
        for(int iter = 0 ; iter < length ; iter++) {
            if(array[iter] == value) {
                return true;
            }
        }
        return false;
    }
    
    static int[] lineCache;
    
    /**
     * @inheritDoc
     */
    protected void drawImage(Graphics g, Object nativeGraphics, int x, int y) {
         if(lineCache == null || lineCache.length < width * 3) {
             lineCache = new int[width * 3];
         }
        
        // for performance we can calculate the visible drawing area so we don't have to
        // calculate the whole array
        int clipY = g.getClipY();
        int clipBottomY = g.getClipHeight() + clipY;
        int firstLine = 0;
        int lastLine = height;
        if(clipY > y) {
            firstLine = clipY - y;
        } 
        if(clipBottomY < y + height) {
            lastLine = clipBottomY - y;
        }
        
        
        for(int line = firstLine ; line < lastLine ; line += 3) {
            int currentPos = line * width;
            int rowsToDraw = Math.min(3, height - line);
            int amount = width * rowsToDraw;
            for(int position = 0 ; position < amount ; position++) {
                int i = imageDataByte[position + currentPos] & 0xff;                
                lineCache[position] = palette[i];
            }
            g.drawRGB(lineCache, 0, x, y + line, width, rowsToDraw, true);
        }
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
    public void scale(int width, int height) {
        IndexedImage p = (IndexedImage)scaled(width, height);
        this.imageDataByte = p.imageDataByte;
        this.width = width;
        this.height = height;
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
        Dimension d = new Dimension(width, height);
        Image i = getCachedImage(d);
        // currently we only support byte data...
        i = new IndexedImage(width, height, palette, scaleArray(imageDataByte, width, height));
        cacheImage(d, i);
        return i;
    }
    
    byte[] scaleArray(byte[] sourceArray, int width, int height) {
        int srcWidth = getWidth();
        int srcHeight = getHeight();

        // no need to scale
        if(srcWidth == width && srcHeight == height){
            return sourceArray;
        }
        byte[] destinationArray = new byte[width * height];
        
        //Horizontal Resize
        int yRatio = (srcHeight << 16) / height;
        int xRatio = (srcWidth << 16) / width;
        int xPos = xRatio / 2;
        int yPos = yRatio / 2;
        for (int x = 0; x < width; x++) {
            int srcX = xPos >> 16;
            for(int y = 0 ; y < height ; y++) {
                int srcY = yPos >> 16;
                int destPixel = x + y * width;
                int srcPixel = srcX + srcY * srcWidth;
                if((destPixel >= 0 && destPixel < destinationArray.length) && 
                    (srcPixel >= 0 && srcPixel < sourceArray.length)) {
                    destinationArray[destPixel] = sourceArray[srcPixel];
                }
                yPos += yRatio;
            }
            yPos = yRatio / 2;
            xPos += xRatio;
        }
        return destinationArray;
    }
    
    /**
     * @inheritDoc
     */
    int[] getRGBImpl() {
        int[] rgb = new int[width * height];
        for(int iter = 0 ; iter < rgb.length ; iter++) {
            int i = imageDataByte[iter] & 0xff;
            rgb[iter] = palette[i];
        }
        return rgb;
    }

    /**
     * Retrieves the palette for the indexed image drawing
     *
     * @return the palette data
     */
    public final int[] getPalette() {
        return palette;
    }

    /**
     * Retrieves the image data as offsets into the palette array
     *
     * @return the image data
     */
    public final byte[] getImageDataByte() {
        return imageDataByte;
    }
    
    /**
     * This method allows us to store a package image into a persistent stream easily
     * thus allowing us to store the image in RMS.
     * 
     * @return a byte array that can be loaded using the load method
     */
    public byte[] toByteArray() {
        try {
            ByteArrayOutputStream array = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(array);
            out.writeShort(width);
            out.writeShort(height);
            out.writeByte(palette.length);
            for (int iter = 0; iter < palette.length; iter++) {
                out.writeInt(palette[iter]);
            }
            out.write(imageDataByte);
            out.close();
            return array.toByteArray();
        } catch (IOException ex) {
            // will never happen since IO is purely in memory
            ex.printStackTrace();
            return null;
        }
    }
    
    /**
     * Loads a packaged image that was stored in a stream using the toByteArray method
     * 
     * @param data previously stored image data
     * @return newly created packed image
     */
    public static IndexedImage load(byte[] data) {
        try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(data));
            int width = input.readShort();
            int height = input.readShort();
            int[] palette = new int[input.readByte() & 0xff];
            for (int iter = 0; iter < palette.length; iter++) {
                palette[iter] = input.readInt();
            }
            byte[] arr = new byte[width * height];
            input.readFully(arr);
            return new IndexedImage(width, height, palette, arr);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
