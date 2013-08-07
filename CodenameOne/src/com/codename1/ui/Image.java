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
import com.codename1.impl.CodenameOneImplementation;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

/**
 * Abstracts the underlying platform images allowing us to treat them as a uniform
 * object.
 * 
 * @author Chen Fishbein
 */
public class Image {
    private Object rgbCache;
    private Object image;   
    int transform;

    private boolean opaqueTested = false;
    private boolean opaque;
    private Object scaleCache;
    private boolean animated;
    private long imageTime = -1;
    private String svgBaseURL;
    private byte[] svgData;
    private String imageName;
    
    /** 
     * Subclasses may use this and point to an underlying native image which might be
     * null for a case of an image that doesn't use native drawing
     * 
     * @param image native image object passed to the Codename One implementation
     */
    protected Image(Object image) {
        this.image = image;
        animated = Display.getInstance().getImplementation().isAnimation(image);
    }

    /** Creates a new instance of ImageImpl */
    Image(int[] imageArray, int w, int h) {
        this(Display.getInstance().getImplementation().createImage(imageArray, w, h));
    }

    
    private Hashtable getScaleCache() {
        if(scaleCache == null) {
            Hashtable h = new Hashtable();
            scaleCache = Display.getInstance().createSoftWeakRef(h);
            return h;
        }
        Hashtable h = (Hashtable)Display.getInstance().extractHardRef(scaleCache);
        if(h == null) {
            h = new Hashtable();
            scaleCache = Display.getInstance().createSoftWeakRef(h);
        }
        return h;
    }

    /**
     * Returns a cached scaled image
     *
     * @param size the size of the cached image
     * @return cached image
     */
    Image getCachedImage(Dimension size) {
        Object w = getScaleCache().get(size);
        return (Image)Display.getInstance().extractHardRef(w);
    } 
    
    /**
     * Returns a cached scaled image
     * 
     * @param size the size of the cached image
     * @return cached image
     */
    void cacheImage(Dimension size, Image i) {
        Object w = Display.getInstance().createSoftWeakRef(i);
        getScaleCache().put(size, w);
    }
    
    /**
     * Async lock is the equivalent of a lock operation, however it uses the given image as
     * the hard cache and performs the actual image loading asynchronously. On completion this
     * method will invoke repaint on the main form if applicable.
     * 
     * @param internal the image to show while the actual image loads.
     */
    public void asyncLock(Image internal) {
    }

    /**
     * This callback indicates that a component pointing at this image is initialized, this allows
     * an image to make performance sensitive considerations e.g. an encoded image
     * might choose to cache itself in RAM.
     * This method may be invoked multiple times.
     */
    public void lock() {
    }

    /**
     * Returns true if the image is locked
     * @return false by default 
     */
    public boolean isLocked() {
        return false;
    }
    
    /**
     * This callback indicates that a component pointing at this image is now deinitilized
     * This method may be invoked multiple times.
     */
    public void unlock() {
    }

    void setImage(Object image) {
        this.image = image;
    }

    void setOpaque(boolean opaque) {
        this.opaque = opaque;
        opaqueTested = true;
    }
    
    /**
     * Indicates whether the underlying platform supports creating an SVG Image
     *
     * @return true if the method create SVG image would return a valid image object
     * from an SVG Input stream
     */
    public static boolean isSVGSupported() {
        return Display.getInstance().getImplementation().isSVGSupported();
    }

    /**
     * Returns a platform specific DOM object that can be manipulated by the user
     * to change the SVG Image
     *
     * @return Platform dependent object, when JSR 226 is supported an SVGSVGElement might
     * be returned.
     */
    public Object getSVGDocument() {
        return Display.getInstance().getImplementation().getSVGDocument(image);
    }

    /**
     * Creates an SVG Image from the given byte array data and the base URL, this method
     * will throw an exception if SVG is unsupported.
     *
     * @param baseURL URL which is used to resolve relative references within the SVG file
     * @param animated indicates if the SVG features an animation
     * @param data the conten of the SVG file
     * @return an image object that can be used as any other image object.
     * @throws IOException if resource lookup fail SVG is unsupported
     */
    public static Image createSVG(String baseURL, boolean animated, byte[] data) throws IOException {
        Image i = new Image(Display.getInstance().getImplementation().createSVGImage(baseURL, data));
        i.animated = animated;
        i.svgBaseURL = baseURL;
        i.svgData = data;
        return i;
    }

    /**
     * Indicates if this image represents an SVG file or a bitmap file
     *
     * @return true if this is an SVG file
     */
    public boolean isSVG() {
        return svgData != null;
    }

    /**
     * Creates a mask from the given image, a mask can be used to apply an arbitrary
     * alpha channel to any image. A mask is derived from the blue channel (LSB) of
     * the given image.
     * The generated mask can be used with the apply mask method.
     * 
     * @return mask object that can be used with applyMask
     */
    public Object createMask() {
        int[] rgb = getRGBCached();
        byte[] mask = new byte[rgb.length];
        for(int iter = 0 ; iter < rgb.length ; iter++) {
            mask[iter] = (byte)(rgb[iter] & 0xff);
        }
        return new IndexedImage(getWidth(), getHeight(), null, mask);
    }

    /**
     * Applies the given alpha mask onto this image and returns the resulting image
     * see the createMask method for indication on how to convert an image into an alpha
     * mask.
     * 
     * @param mask mask object created by the createMask() method.
     * @param x starting x where to apply the mask
     * @param y starting y where to apply the mask
     * @return image masked based on the given object
     */
    public Image applyMask(Object mask, int x, int y) {
        int[] rgb = getRGB();
        byte[] maskData = ((IndexedImage)mask).getImageDataByte();
        int mWidth = ((IndexedImage)mask).getWidth();
        int mHeight = ((IndexedImage)mask).getHeight();
        int imgWidth = getWidth();
        int aWidth =  imgWidth - x;
        int aHeight = getHeight() - y;
        if(aWidth > mWidth) {
            aWidth = mWidth;
        }
        if(aHeight > mHeight) {
            aHeight = mHeight;
        }

        for(int xPos = 0 ; xPos < aWidth ; xPos++) {
            for(int yPos = 0 ; yPos < aHeight ; yPos++) {
                int aX = x + xPos;
                int aY = y + yPos;
                int imagePos = aX + aY * imgWidth;
                int maskAlpha = maskData[aX + aY * mWidth] & 0xff;
                maskAlpha = (maskAlpha << 24) & 0xff000000;
                rgb[imagePos] = (rgb[imagePos] & 0xffffff) | maskAlpha;

            }
        }
        return createImage(rgb, imgWidth, getHeight());
    }

    /**
     * Applies the given alpha mask onto this image and returns the resulting image
     * see the createMask method for indication on how to convert an image into an alpha
     * mask.
     *
     * @param mask mask object created by the createMask() method.
     * @return image masked based on the given object
     * @throws IllegalArgumentException if the image size doesn't match the mask size
     */
    public Image applyMask(Object mask) {
        int[] rgb = getRGB();
        byte[] maskData = ((IndexedImage)mask).getImageDataByte();
        int mWidth = ((IndexedImage)mask).getWidth();
        int mHeight = ((IndexedImage)mask).getHeight();
        if(mWidth != getWidth() || mHeight != getHeight()) {
            throw new IllegalArgumentException("Mask and image sizes don't match");
        }
        for(int iter = 0 ; iter < maskData.length ; iter++) {
            int maskAlpha = maskData[iter] & 0xff;
            maskAlpha = (maskAlpha << 24) & 0xff000000;
            rgb[iter] = (rgb[iter] & 0xffffff) | maskAlpha;
        }
        return createImage(rgb, mWidth, mHeight);
    }

    /**
     * Applies the given alpha mask onto this image and returns the resulting image
     * see the createMask method for indication on how to convert an image into an alpha
     * mask. If the image is of a different size it will be scaled to mask size.
     *
     * @param mask mask object created by the createMask() method.
     * @return image masked based on the given object
     */
    public Image applyMaskAutoScale(Object mask) {
        int mWidth = ((IndexedImage)mask).getWidth();
        int mHeight = ((IndexedImage)mask).getHeight();
        if(mWidth != getWidth() || mHeight != getHeight()) {
            return scaled(mWidth, mHeight).applyMask(mask);
        }
        return applyMask(mask);
    }

    /**
     * Extracts a subimage from the given image allowing us to breakdown a single large image
     * into multiple smaller images in RAM, this actually creates a standalone version
     * of the image for use. 
     * 
     * @param x the x offset from the image
     * @param y the y offset from the image
     * @param width the width of internal images
     * @param height the height of internal images
     * @param processAlpha whether alpha should be processed as well as part of the cutting
     * @return An array of all the possible images that can be created from the source
     */
    public Image subImage(int x, int y, int width, int height, boolean processAlpha)  {
        // we use the getRGB API rather than the mutable image API to allow translucency to
        // be maintained in the newly created image
        int[] arr = new int[width * height];
        getRGB(arr, 0, x, y, width, height);
        
        Image i = new Image(Display.getInstance().getImplementation().createImage(arr, width, height));
        i.opaque = opaque;
        i.opaqueTested = opaqueTested;
        return i;
    }
    
    /**
     * Creates a mirror image for the given image which is useful for some RTL scenarios. Notice that this
     * method isn't the most efficient way to perform this task and is designed for portability over efficiency.
     * @return a mirrored image
     */
    public Image mirror() {
        int width = getWidth();
        int height = getHeight();
        int[] tmp = getRGB();
        int[] arr = new int[width * height];
        for(int x = 0 ; x < width ; x++) {
            for(int y = 0 ; y < height ; y++) {
                arr[x + y * width] = tmp[width - x - 1 + y * width];
            }
        }
        Image i = new Image(Display.getInstance().getImplementation().createImage(arr, width, height));
        i.opaque = opaque;
        i.opaqueTested = opaqueTested;
        return i;
    }
    
    /**
     * Returns an instance of this image rotated by the given number of degrees. By default 90 degree
     * angle divisions are supported, anything else is implementation dependent. This method assumes 
     * a square image. Notice that it is inefficient in the current implementation to rotate to
     * non-square angles, 
     * <p>E.g. rotating an image to 45, 90 and 135 degrees is inefficient. Use rotatate to 45, 90
     * and then rotate the 45 to another 90 degrees to achieve the same effect with less memory.
     * 
     * @param degrees A degree in right angle must be larger than 0 and up to 359 degrees
     * @return new image instance with the closest possible rotation
     */
    public Image rotate(int degrees) {
        CodenameOneImplementation i = Display.getInstance().getImplementation();
        if(i.isRotationDrawingSupported()) {
            if(degrees >= 90) {
                int newTransform = 0;
                if(transform != 0) {
                    newTransform = (transform + degrees) % 360;
                } else {
                    newTransform = degrees % 360;
                }
                degrees %= 90;
                newTransform -= degrees;
                if(degrees != 0) {
                    Image newImage = new Image(Display.getInstance().getImplementation().rotate(image, degrees));
                    newImage.transform = newTransform;
                    return newImage;
                } else {
                    Image newImage = new Image(image);
                    newImage.transform = newTransform;
                    return newImage;
                }
            }
            if(degrees != 0) {
                return new Image(Display.getInstance().getImplementation().rotate(image, degrees));
            } 
            return this;
        } else {
            return new Image(Display.getInstance().getImplementation().rotate(image, degrees));
        }
    }

    /**
     * Creates an indexed image with byte data this method may return a native indexed image rather than
     * an instance of the IndexedImage class
     * 
     * @param width image width
     * @param height image height
     * @param palette the color palette to use with the byte data
     * @param data byte data containing palette offsets to map to ARGB colors
     * @deprecated try to avoid using indexed images explicitly
     */
    public static Image createIndexed(int width, int height, int[] palette, byte[] data) {
        IndexedImage i = new IndexedImage(width, height, palette, data);
        CodenameOneImplementation impl = Display.getInstance().getImplementation();
        if(impl.isNativeIndexed()) {
            return new Image(impl.createNativeIndexed(i));
        }
        return i;
    }
    
    /**
     * Creates a new image instance with the alpha channel of opaque/translucent 
     * pixels within the image using the new alpha value. Transparent (alpha == 0)
     * pixels remain transparent. All other pixels will have the new alpha value.
     * 
     * @param alpha New value for the entire alpha channel
     * @return Translucent/Opaque image based on the alpha value and the pixels of 
     * this image
     */
    public Image modifyAlpha(byte alpha) {
        int w = getWidth();
        int h = getHeight();
        int size = w * h;
        int[] arr = getRGB();
        int alphaInt = (((int)alpha) << 24) & 0xff000000;
        for(int iter = 0 ; iter < size ; iter++) {
            int currentAlpha = (arr[iter] >> 24) & 0xff;
            if(currentAlpha != 0) {
                arr[iter] = (arr[iter] & 0xffffff) | alphaInt;
            }
        }
        Image i = new Image(arr, w, h);
        i.opaqueTested = true;
        i.opaque = false;
        return i;
    }
    
    /**
     * Creates a new image instance with the alpha channel of opaque
     * pixels within the image using the new alpha value. Transparent (alpha == 0)
     * pixels remain transparent. Semi translucent pixels will be multiplied by the
     * ratio difference and their translucency reduced appropriately.
     *
     * @param alpha New value for the entire alpha channel
     * @return Translucent/Opaque image based on the alpha value and the pixels of
     * this image
     */
    public Image modifyAlphaWithTranslucency(byte alpha) {
        int w = getWidth();
        int h = getHeight();
        int size = w * h;
        int[] arr = getRGB();
        int alphaInt = (((int)alpha) << 24) & 0xff000000;
        float alphaRatio = (alpha & 0xff);
        alphaRatio = (alpha & 0xff) / 255.0f;
        for(int iter = 0 ; iter < size ; iter++) {
            int currentAlpha = (arr[iter] >> 24) & 0xff;
            if(currentAlpha != 0) {
                if(currentAlpha == 0xff) {
                    arr[iter] = (arr[iter] & 0xffffff) | alphaInt;
                } else {
                    int relative = (int)(currentAlpha * alphaRatio);
                    relative = (relative << 24) & 0xff000000;
                    arr[iter] = (arr[iter] & 0xffffff) | relative;
                }
            }
        }
        Image i = new Image(arr, w, h);
        i.opaqueTested = true;
        i.opaque = false;
        return i;
    }

    /**
     * Creates a new image instance with the alpha channel of opaque/translucent 
     * pixels within the image using the new alpha value. Transparent (alpha == 0)
     * pixels remain transparent. All other pixels will have the new alpha value.
     * 
     * @param alpha New value for the entire alpha channel
     * @param removeColor pixels matching this color are made transparent (alpha channel ignored)
     * @return Translucent/Opaque image based on the alpha value and the pixels of 
     * this image
     */
    public Image modifyAlpha(byte alpha, int removeColor) {
        removeColor = removeColor & 0xffffff;
        int w = getWidth();
        int h = getHeight();
        int size = w * h;
        int[] arr = new int[size];
        getRGB(arr, 0, 0, 0, w, h);
        int alphaInt = (((int)alpha) << 24) & 0xff000000;
        for(int iter = 0 ; iter < size ; iter++) {
            if((arr[iter] & 0xff000000) != 0) {
                arr[iter] = (arr[iter] & 0xffffff) | alphaInt;
                if(removeColor == (0xffffff & arr[iter])) {
                    arr[iter] = 0;
                }
            }   
        }
        Image i = new Image(arr, w, h);
        i.opaqueTested = true;
        i.opaque = false;
        return i;
    }
    
    /**
     * creates an image from the given path based on MIDP's createImage(path)
     * 
     * @param path 
     * @throws java.io.IOException 
     * @return newly created image object
     */
    public static Image createImage(String path) throws IOException {
        try {
            return new Image(Display.getInstance().getImplementation().createImage(path));
        } catch(OutOfMemoryError err) {
            // Images have a major bug on many phones where they sometimes throw 
            // an OOM with no reason. A system.gc followed by the same call over
            // solves the problem. This has something to do with the fact that 
            // there is no Image.dispose method in existance.
            System.gc();System.gc();
            return new Image(Display.getInstance().getImplementation().createImage(path));
        }
    }
    
    /**
     * creates an image from the given native image (e.g. MIDP image object)
     *
     * @param nativeImage
     * @return newly created Codename One image object
     */
    public static Image createImage(Object nativeImage) {
        return new Image(nativeImage);
    }

    /**
     * creates an image from an InputStream
     * 
     * @param stream a given InputStream
     * @throws java.io.IOException 
     * @return the newly created image
     */
    public static Image createImage(InputStream stream) throws IOException {
        try {
            return new Image(Display.getInstance().getImplementation().createImage(stream));
        } catch(OutOfMemoryError err) {
            // Images have a major bug on many phones where they sometimes throw 
            // an OOM with no reason. A system.gc followed by the same call over
            // solves the problem. This has something to do with the fact that 
            // there is no Image.dispose method in existance.
            System.gc();System.gc();
            return new Image(Display.getInstance().getImplementation().createImage(stream));
        }
    }
    
    /**
     * creates an image from an RGB image
     * 
     * @param rgb the RGB image array data
     * @param width the image width
     * @param height the image height
     * @return an image from an RGB image
     */
    public static Image createImage(int[] rgb, int width, int height) {
        try {
            Image i = new Image(Display.getInstance().getImplementation().createImage(rgb, width, height));
            return i;
        } catch(OutOfMemoryError err) {
            // Images have a major bug on many phones where they sometimes throw 
            // an OOM with no reason. A system.gc followed by the same call over
            // solves the problem. This has something to do with the fact that 
            // there is no Image.dispose method in existance.
            System.gc();System.gc();
            return new Image(Display.getInstance().getImplementation().createImage(rgb, width, height));
        }
    }

    /**
     * Creates a mutable image that may be manipulated using getGraphics
     * 
     * @param width the image width
     * @param height the image height
     * @return an image in a given width and height dimension
     */
    public static Image createImage(int width, int height) {
        return createImage(width, height, 0xffffffff);
    }
    
    /**
     * Returns true if mutable images support alpha transparency
     * 
     * @return true if mutable images support alpha in their fillColor argument
     */
    public static boolean isAlphaMutableImageSupported() {
        return Display.getInstance().getImplementation().isAlphaMutableImageSupported();
    }
    
    /**
     * Creates a mutable image that may be manipulated using getGraphics
     * 
     * @param width the image width
     * @param height the image height
     * @param fillColor the color with which the image should be initially filled
     * @return an image in a given width and height dimension
     */
    public static Image createImage(int width, int height, int fillColor) {
        try {
            return new Image(Display.getInstance().getImplementation().createMutableImage(width, height, fillColor));
        } catch(OutOfMemoryError err) {
            // Images have a major bug on many phones where they sometimes throw 
            // an OOM with no reason. A system.gc followed by the same call over
            // solves the problem. This has something to do with the fact that 
            // there is no Image.dispose method in existance.
            System.gc();System.gc();
            return new Image(Display.getInstance().getImplementation().createMutableImage(width, height, fillColor));
        }
    }
    
    
    /**
     * creates an image from a given byte array data
     * 
     * @param bytes the array of image data in a supported image format
     * @param offset the offset of the start of the data in the array
     * @param len the length of the data in the array
     * @return the newly created image
     */
    public static Image createImage(byte[] bytes,int offset,int len) {
        try {
            Object o = Display.getInstance().getImplementation().createImage(bytes, offset, len);
            if(o == null) {
                throw new IllegalArgumentException("create image failed for the given image data of length: " + len);
            }
            return new Image(o);
        } catch(OutOfMemoryError err) {
            // Images have a major bug on many phones where they sometimes throw 
            // an OOM with no reason. A system.gc followed by the same call over
            // solves the problem. This has something to do with the fact that 
            // there is no Image.dispose method in existance.
            System.gc();System.gc();
            return new Image(Display.getInstance().getImplementation().createImage(bytes, offset, len));
        }
    }

    /**
     * If this is a mutable image a graphics object allowing us to draw on it
     * is returned.
     * 
     * @return Graphics object allowing us to manipulate the content of a mutable image
     */
    public Graphics getGraphics() {
        return new Graphics(Display.getInstance().getImplementation().getNativeGraphics(image));
    }
    
    /**
     * Returns the width of the image
     * 
     * @return the width of the image
     */
    public int getWidth() {
        if(transform != 0) {
            if(transform == 90 || transform == 270) {
                return Display.getInstance().getImplementation().getImageHeight(image);
            }
        }
        return Display.getInstance().getImplementation().getImageWidth(image);
    }
    
    /**
     * Returns the height of the image
     * 
     * @return the height of the image
     */
    public int getHeight() {
        if(transform != 0) {
            if(transform == 90 || transform == 270) {
                return Display.getInstance().getImplementation().getImageWidth(image);
            }
        }
        return Display.getInstance().getImplementation().getImageHeight(image);
    }

    /**
     * Callback invoked internally by Codename One to draw the image/frame onto the display.
     * Image subclasses can override this method to perform drawing of custom image types.
     *
     * @param g the graphics object
     * @param nativeGraphics the underlying native graphics which might be essential for some image types
     * @param x the x coordinate
     * @param y the y coordinate
     */
    protected void drawImage(Graphics g, Object nativeGraphics, int x, int y) {
        g.drawImage(image, x, y, transform);
    }

    /**
     * Callback invoked internally by Codename One to draw the image/frame onto the display.
     * Image subclasses can override this method to perform drawing of custom image types.
     *
     * @param g the graphics object
     * @param nativeGraphics the underlying native graphics which might be essential for some image types
     * @param x the x coordinate
     * @param y the y coordinate
     * @param w the width to occupy
     * @param h the height to occupy
     */
    protected void drawImage(Graphics g, Object nativeGraphics, int x, int y, int w, int h) {
        g.drawImageWH(image, x, y, w, h);
    }
    
    /**
     * Callback invoked internally by Codename One to draw a portion of the image onto the display.
     * Image subclasses can override this method to perform drawing of custom image types.
     *
     * @param g the graphics object
     * @param nativeGraphics the underlying native graphics which might be essential for some image types
     * @param x the x coordinate
     * @param y the y coordinate
     * @param imageX location within the image to draw
     * @param imageY location within the image to draw
     * @param imageWidth size of the location within the image to draw
     * @param imageHeight size of the location within the image to draw
     */
    void drawImageArea(Graphics g, Object nativeGraphics, int x, int y, int imageX, int imageY, int imageWidth, int imageHeight) {
        Display.getInstance().getImplementation().drawImageArea(nativeGraphics, image, x, y, imageX, imageY, imageWidth, imageHeight);
    }

    /**
     * Obtains ARGB pixel data from the specified region of this image and 
     * stores it in the provided array of integers. Each pixel value is 
     * stored in 0xAARRGGBB format, where the high-order byte contains the 
     * alpha channel and the remaining bytes contain color components for red, 
     * green and blue, respectively. The alpha channel specifies the opacity of 
     * the pixel, where a value of 0x00  represents a pixel that is fully 
     * transparent and a value of 0xFF  represents a fully opaque pixel. 
     * The rgb information contained within the image, this method ignors 
     * rotation and mirroring in some/most situations and cannot be 
     * used in such cases.
     * 
     * @param rgbData an array of integers in which the ARGB pixel data is 
     * stored
     * @param offset the index into the array where the first ARGB value is 
     * stored
     * @param scanlength the relative offset in the array between 
     * corresponding pixels in consecutive rows of the region
     * @param x the x-coordinate of the upper left corner of the region
     * @param y the y-coordinate of the upper left corner of the region
     * @param width the width of the region
     * @param height the height of the region
     */
    void getRGB(int[] rgbData,
            int offset,
            int x,
            int y,
            int width,
            int height){
        Display.getInstance().getImplementation().getRGB(image, rgbData, offset, x, y, width, height);
    }
    
    

    /**
     * Extracts data from this image into the given RGBImage
     * 
     * @param image RGBImage that would receive pixel data
     * @param destX x location within RGBImage into which the data will
     *      be written
     * @param destY y location within RGBImage into which the data will
     *      be written
     * @param x location within the source image
     * @param y location within the source image
     * @param width size of the image to extract from the source image
     * @param height size of the image to extract from the source image
     */
    public void toRGB(RGBImage image,
            int destX,
            int destY,
            int x,
            int y,
            int width,
            int height){
        getRGB(image.getRGB(), destX * destY, x, y, width, height);
    }
    
    /**
     * Returns the content of this image as a newly created ARGB array.
     * 
     * @return new array instance containing the ARGB data within this image
     */
    public int[] getRGB() {
        return getRGBImpl();
    }

    /**
     * Returns the content of this image as a newly created ARGB array or a cached
     * instance if possible. Note that cached instances may be garbage collected.
     *
     * @return array instance containing the ARGB data within this image
     */
    public int[] getRGBCached() {
        int[] r = getRGBCache();
        if(r == null) {
            r = getRGBImpl();
            rgbCache = Display.getInstance().createSoftWeakRef(r);
        }
        return r;
    }

    int[] getRGBCache() {
        if(rgbCache != null) {
            int[] rgb = (int[])Display.getInstance().extractHardRef(rgbCache);
            return rgb;
        }
        return null;
    }
    
    int[] getRGBImpl() {
        int width = getWidth();
        int height = getHeight();
        int[] rgbData = new int[width * height];
        getRGB(rgbData, 0, 0, 0, width, height);
        return rgbData;
    }
    
    /**
     * Scales the image to the given width while updating the height based on the
     * aspect ratio of the width
     * 
     * @param width the given new image width
     * @return the newly created image
     */
    public Image scaledWidth(int width) {
        float ratio = ((float)width) / ((float)getWidth());
        return scaled(width, Math.max(1, (int)(getHeight() * ratio)));
    }

    /**
     * Scales the image to the given height while updating the width based on the
     * aspect ratio of the height
     * 
     * @param height the given new image height
     * @return the newly created image
     */
    public Image scaledHeight(int height) {
        float ratio = ((float)height) / ((float)getHeight());
        return scaled(Math.max(1, (int)(getWidth() * ratio)), height);
    }
    
    /**
     * Scales the image while maintaining the aspect ratio to the smaller size 
     * image
     * 
     * @param width the given new image width
     * @param height the given new image height
     * @return the newly created image
     */
    public Image scaledSmallerRatio(int width, int height) {
        float hRatio = ((float)height) / ((float)getHeight());
        float wRatio = ((float)width) / ((float)getWidth());
        if(hRatio < wRatio) {
            return scaled((int)(getWidth() * hRatio), (int)(getHeight() * hRatio));
        } else {
            return scaled((int)(getWidth() * wRatio), (int)(getHeight() * wRatio));
        }
    }

    /**
     * Returns a scaled version of this image image using the given width and height, 
     * this is a fast algorithm that preserves translucent information.
     * The method accepts -1 to preserve aspect ratio in the given axis.
     * 
     * @param width width for the scaling
     * @param height height of the scaled image
     * @return new image instance scaled to the given height and width
     */
    public Image scaled(int width, int height) {
        if(width == getWidth() && height == getHeight()) {
            return this;
        }
        if(width == -1) {
            return scaledHeight(height);
        } 
        if(height == -1) {
            return scaledWidth(width);
        }
        Dimension d = new Dimension(width, height);
        Image i = getCachedImage(d);
        if(i != null) {
            return i;
        }
        
        if(svgData != null){
            try {
                i = createSVG(svgBaseURL, animated, svgData);
            } catch (IOException ex) {
                i = new Image(this.image);
            }
        }else{
            i = new Image(this.image);
        }
        i.scaleCache = scaleCache;
        i.scale(width, height);
        i.transform = this.transform;
        i.animated = animated;
        i.svgBaseURL = svgBaseURL;
        i.svgData = svgData;        
        cacheImage(new Dimension(width, height), i);
        return i;
    }

    /**
     * Returns the platform specific image implementation, <strong>warning</strong> the
     * implementation class can change between revisions of Codename One and platforms.
     *
     * @return platform specific native implementation for this image object
     */
    public Object getImage() {
        return image;
    }
    
    /**
     * Scale the image to the given width and height, this is a fast algorithm
     * that preserves translucent information
     * 
     * @param width width for the scaling
     * @param height height of the scaled image
     * 
     * @deprecated scale should return an image rather than modify the image in place
     * use scaled(int, int) instead
     */
    public void scale(int width, int height) {
        image = Display.getInstance().getImplementation().scale(image, width, height);
    }//resize image
    
    boolean scaleArray(int srcWidth, int srcHeight, int height, int width, int[] currentArray, int[] destinationArray) {
        // Horizontal Resize
        int yRatio = (srcHeight << 16) / height;
        int xRatio = (srcWidth << 16) / width;
        int xPos = xRatio / 2;
        int yPos = yRatio / 2;

        // if there is more than 16bit color there is no point in using mutable
        // images since they won't save any memory
        boolean testOpaque = Display.getInstance().numColors() <= 65536 && (!opaqueTested);
        boolean currentOpaque = true;
        for (int y = 0; y < height; y++) {
            int srcY = yPos >> 16;
            getRGB(currentArray, 0, 0, srcY, srcWidth, 1);
            for (int x = 0; x < width; x++) {
                int srcX = xPos >> 16;
                int destPixel = x + y * width;
                if ((destPixel >= 0 && destPixel < destinationArray.length) && (srcX < currentArray.length)) {
                    destinationArray[destPixel] = currentArray[srcX];

                    // if all the pixels have an opaque alpha channel then the image is opaque
                    currentOpaque = testOpaque && currentOpaque && (currentArray[srcX] & 0xff000000) == 0xff000000;
                }
                xPos += xRatio;
            }
            yPos += yRatio;
            xPos = xRatio / 2;
        }
        if(testOpaque) {
            this.opaque = currentOpaque;
        }
        return opaque;
    }
    
    /**
     * Returns true if this is an animated image
     * 
     * @return true if this image represents an animation
     */
    public boolean isAnimation() {
        return animated;
    }

    /**
     * @inheritDoc 
     */
    public boolean animate() {
        if(imageTime == -1) {
            imageTime = System.currentTimeMillis();
        }
        boolean val = Display.getInstance().getImplementation().animateImage(image, imageTime);
        imageTime = System.currentTimeMillis();
        return val;
    }

    /**
     * Indicates whether this image is opaque or not
     * 
     * @return true if the image is completely opqaque which allows for some heavy optimizations
     */
    public boolean isOpaque() {
        if(!opaqueTested) {
            opaque = Display.getInstance().getImplementation().isOpaque(this, image);
            opaqueTested = true;
        }
        return opaque;
    }

    /**
     * The name of the image is set for some images mostly to ease the debugging of Codename One application
     * @return the imageName
     */
    public String getImageName() {
        return imageName;
    }

    /**
     * The name of the image is set for some images mostly to ease the debugging of Codename One application
     * @param imageName the imageName to set
     */
    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    /**
     * DO NOT CALL THIS METHOD UNLESS YOU KNOW WHAT YOU ARE DOING! Images dispose
     * automatically for most cases except for very rare special cases.
     * Images on devices usually holds a native memory, some platforms garbage 
     * collectors might fail to release the native and to fail with out of memory
     * errors.
     * Use this method to make sure the image will be released from memory, after 
     * calling this the image will become unusable.
     */
    public void dispose(){
        if(image != null) {
            Display.getInstance().getImplementation().releaseImage(image);
        }
        image = null;
    }
    
    
}
