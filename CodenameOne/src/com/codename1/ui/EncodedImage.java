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

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.ui.util.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p>{@code EncodedImage} is the workhorse of Codename One. Images returned from resource files are 
 * {@code EncodedImage} and many API's expect it.</p>
 * 
 * <p>{@code EncodedImage} is effectively a an image that is "hidden" and extracted as needed to remove the 
 * memory overhead associated with loaded image. When creating an {@code EncodedImage} only the PNG 
 * (or JPEG etc.) is loaded to an array in RAM. Normally such images are very small (relatively) so they can be 
 * kept in memory without much overhead.</p>
 * 
 * <p>When image information is needed (pixels) the image is decoded into RAM and kept in a weak/sort 
 * reference (see {@link com.codename1.ui.Display#createSoftWeakRef(java.lang.Object)}). This allows the 
 * image to be cached for performance and allows the garbage collector to reclaim it when the memory becomes 
 * scarce.</p>
 * 
 * <p>Since the fully decoded image can be pretty big ({@code width X height X 4}) the ability to store just the 
 * encoded image can be pretty stark. E.g. A standard 50x100 image will take up 20,000 bytes of RAM for a 
 * standard image but an {@code EncodedImage} can reduce that to 1kb-2kb of RAM.</p>
 * 
 * <p>When drawing an {@code EncodedImage} it checks the weak reference cache and if the image is cached then 
 * it is shown  otherwise the image is loaded the encoded image cache it then drawn.</p>
 * 
 * <p>{@code EncodedImage} is not final and can be derived to produce complex image fetching strategies 
 * e.g. the {@link com.codename1.ui.URLImage} class that can dynamically download its content from the web.</p>
 * 
 * <p>{@code EncodedImage} can be instantiated via the create methods in the class. Pretty much any image 
 * can be converted into an `EncodedImage` via the  {@link #createFromImage(com.codename1.ui.Image, boolean)} 
 * method.</p>
 * 
 * <h3>EncodedImage Locking</h3>
 * <p>Naturally loading the image is more expensive so we want the images that are on the current form to remain in 
 * cache (otherwise GC will thrash a lot). That's where {@link #lock()} kicks in, when {@link #lock()} is active we 
 * keep a hard reference to the actual native image so it won't get GC'd. This significantly improves performance!</p>
 * 
 * <p>Internally this is invoked automatically for background images, icons etc. which results in a huge performance 
 * boost. This makes sense since these images are currently showing and they will be in RAM anyway. However, 
 * if you use a complex renderer or custom drawing UI you should {@link #lock()} your images where possible!</p>
 * 
 * <p>To verify that locking might be a problem you can launch the performance monitor tool (accessible from 
 * the simulator menu), if you get log messages that indicate that an unlocked image was drawn you might 
 * have a problem.</p>
 *
 * @author Shai Almog
 */
public class EncodedImage extends Image {
    private byte[][] imageData;
    private int[] dpis;
    private int lastTestedDPI = -1;
    private int width = -1;
    private int height = -1;
    private boolean opaqueChecked = false;
    private boolean opaque = false;
    private Object cache;
    private Image hardCache;
    private int locked;
    
    private EncodedImage(byte[][] imageData) {
        super(null);
        this.imageData = imageData;
    }

    /**
     * Allows subclasses to create more advanced variations of this class that
     * lazily store the data in an arbitrary location.
     *
     * @param width -1 if unknown ideally the width/height should be known in advance
     * @param height  -1 if unknown ideally the width/height should be known in advance
     */
    protected EncodedImage(int width, int height) {
        super(null);
        this.width = width;
        this.height = height;
    }

    /**
     * A subclass might choose to load asynchroniously and reset the cache when the image is ready.
     */
    protected void resetCache() {
        cache = null;
        hardCache = null;
    }

    /**
     * Creates an encoded image that acts as a multi-image, DO NOT USE THIS METHOD. Its for internal
     * use to improve the user experience of the simulator
     * 
     * @param dpis device DPI's
     * @param data the data matching each multi-image DPI
     * @return an encoded image that acts as a multi-image in runtime
     * @deprecated this method is meant for internal use only, it would be very expensive to use
     * this method for real applications. Its here for simulators and development purposes where 
     * screen DPI/resolution can vary significantly in runtime (something that just doesn't happen on devices).
     */
    public static EncodedImage createMulti(int[] dpis, byte[][] data) {
        EncodedImage e = new EncodedImage(data);
        e.dpis = dpis;
        return e;
    }
    
    /**
     * Converts an image to encoded image
     * @param i image
     * @param jpeg true to try and set jpeg, will do a best effort but this isn't guaranteed
     * @return an encoded image or null
     */
    public static EncodedImage createFromImage(Image i, boolean jpeg) {
        if(i instanceof EncodedImage) {
            return ((EncodedImage)i);
        }
        ImageIO io = ImageIO.getImageIO();
        if(io != null) {
            String format;
            if(jpeg) {
                if(!io.isFormatSupported(ImageIO.FORMAT_JPEG)) {
                    format = ImageIO.FORMAT_PNG; 
                } else {
                    format = ImageIO.FORMAT_JPEG; 
                }
            } else {
                if(!io.isFormatSupported(ImageIO.FORMAT_PNG)) {
                    format = ImageIO.FORMAT_JPEG;
                } else {
                    format = ImageIO.FORMAT_PNG;
                }
            }
            try {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                io.save(i, bo, format, 0.9f);
                EncodedImage enc = EncodedImage.create(bo.toByteArray());
                Util.cleanup(bo);
                enc.width = i.getWidth();
                enc.height = i.getHeight();
                if(format == ImageIO.FORMAT_JPEG) {
                    enc.opaque = true;
                    enc.opaqueChecked = true;
                }
                enc.cache = Display.getInstance().createSoftWeakRef(i);
                return enc;
            } catch(IOException err) {
                Log.e(err);
            }            
        }
        return null;
    }
    
    /**
     * Tries to create an encoded image from RGB which is more efficient,
     * however if this fails it falls back to regular RGB image. This method
     * is slower than creating an RGB image (not to be confused with the RGBImage class which is
     * something ENTIRELY different!).
     * 
     * @param argb an argb array
     * @param width the width for the image
     * @param height the height for the image
     * @param jpeg uses jpeg format internally which is opaque and could be faster/smaller
     * @return an image which we hope is an encoded image
     */
    public static Image createFromRGB(int[] argb, int width, int height, boolean jpeg) {
        Image i = Image.createImage(argb, width, height);
        ImageIO io = ImageIO.getImageIO();
        if(io != null) {
            String format;
            if(jpeg) {
                if(!io.isFormatSupported(ImageIO.FORMAT_JPEG)) {
                    return i;
                }
                format = ImageIO.FORMAT_JPEG;
            } else {
                if(!io.isFormatSupported(ImageIO.FORMAT_PNG)) {
                    return i;
                }
                format = ImageIO.FORMAT_PNG;
            }
            try {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                io.save(i, bo, format, 0.9f);
                EncodedImage enc = EncodedImage.create(bo.toByteArray());
                Util.cleanup(bo);
                enc.width = width;
                enc.height = height;
                if(jpeg) {
                    enc.opaque = true;
                    enc.opaqueChecked = true;
                }
                enc.cache = Display.getInstance().createSoftWeakRef(i);
                return enc;
            } catch(IOException err) {
                Log.e(err);
            }
            
        }
        return i;
    }
    
    /**
     * Returns the byte array data backing the image allowing the image to be stored
     * and discarded completely from RAM.
     * 
     * @return byte array used to create the image, e.g. encoded PNG, JPEG etc.
     */
    public byte[] getImageData() {
        if(imageData.length == 1) {
            return imageData[0];
        }
        int dpi = Display.getInstance().getDeviceDensity();
        int bestFitOffset = 0;
        int bestFitDPI = 0;
        int dlen = dpis.length;
        for(int iter = 0 ; iter < dlen ; iter++) {
            int currentDPI = dpis[iter];
            if(dpi == currentDPI) {
                bestFitOffset = iter;
                break;
            }
            if(bestFitDPI != dpi && dpi >= currentDPI && currentDPI >= bestFitDPI) {
                bestFitDPI = currentDPI;
                bestFitOffset = iter;
            }
        }
        lastTestedDPI = dpi;
        return imageData[bestFitOffset];
    }

    /**
     * Creates an image from the given byte array
     * 
     * @param data the data of the image
     * @return newly created encoded image
     */
    public static EncodedImage create(byte[] data) {
        if(data == null) {
            throw new NullPointerException();
        }
        return new EncodedImage(new byte[][] {data});
    }

    /**
     * Creates an image from the given byte array with the variables set appropriately.
     * This saves LWUIT allot of resources since it doesn't need to actually traverse the 
     * pixels of an image to find out details about it.
     * 
     * @param data the data of the image
     * @param width the width of the image
     * @param height the height of the image
     * @param opacity true for an opaque image
     * @return newly created encoded image
     */
    public static EncodedImage create(byte[] data, int width, int height, boolean opacity) {
        if(data == null) {
            throw new NullPointerException();
        }
        EncodedImage e = new EncodedImage(new byte[][] {data});
        e.width = width;
        e.height = height;
        e.opaque = opacity;
        e.opaqueChecked = true;
        return e;
    }
    
    /**
     * {@inheritDoc}
     */
    public Object getImage() {
        return getInternalImpl().getImage();
    }
    
    /**
     * Creates an image from the input stream 
     * 
     * @param i the input stream
     * @return newly created encoded image
     * @throws java.io.IOException if thrown by the input stream
     */
    public static EncodedImage create(InputStream i) throws IOException {
        byte[] buffer = Util.readInputStream(i);
        if(buffer.length > 200000) {
            System.out.println("Warning: loading large images using EncodedImage.create(InputStream) might lead to memory issues, try using EncodedImage.create(InputStream, int)");
        }
        return new EncodedImage(new byte[][] {buffer});
    }

    /**
     * Creates an image from the input stream, this version of the method is somewhat faster
     * than the version that doesn't accept size
     * 
     * @param i the input stream
     * @param size the size of the stream
     * @return newly created encoded image
     * @throws java.io.IOException if thrown by the input stream
     */
    public static EncodedImage create(InputStream i, int size) throws IOException {
        byte[] buffer = new byte[size];
        Util.readFully(i, buffer);
        return new EncodedImage(new byte[][] {buffer});
    }

    private Image getInternalImpl() {
        
        if(imageData != null && imageData.length > 1 && lastTestedDPI != Display.getInstance().getDeviceDensity()) {
            hardCache = null;
            cache = null;
            width = -1;
            height = -1;
        }
        if(hardCache != null) {
            return hardCache;
        }
        Image i = getInternal();
        if(locked > 0) {
            hardCache = i;
        }
        return i;
    }

    /**
     * Returns the actual image represented by the encoded image, this image will
     * be cached in a weak/soft reference internally. This method is useful to detect
     * when the system actually created an image instance. You shouldn't invoke this
     * method manually!
     *
     * @return drawable image instance
     */
    protected Image getInternal() {
        if(cache != null) {
            Image i = (Image)Display.getInstance().extractHardRef(cache);
            if(i != null) {
                return i;
            }
        }
        Image i;
        try {
            byte[] b = getImageData();
            i = Image.createImage(b, 0, b.length);
            if(opaqueChecked) {
                i.setOpaque(opaque);
            }
            CodenameOneImplementation impl = Display.impl;
            impl.setImageName(i.getImage(), getImageName());
        } catch(Exception err) {
            Log.e(err);
            i = Image.createImage(5, 5);
        }
        cache = Display.getInstance().createSoftWeakRef(i);
        return i;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLocked() {
        return locked > 0;
    }
    
    /**
     * {@inheritDoc}
     */
    public void asyncLock(final Image internal) {
        if(locked <= 0) {
            locked = 1;
            if(cache != null) {
                hardCache = (Image)Display.getInstance().extractHardRef(cache);
                if(hardCache != null) {
                    return;
                }
            }
            hardCache = internal;
            Display.getInstance().scheduleBackgroundTask(new Runnable() {
                public void run() {
                    try {
                        byte[] b = getImageData();
                        final Image i = Image.createImage(b, 0, b.length);
                        if(opaqueChecked) {
                            i.setOpaque(opaque);
                        }
                        CodenameOneImplementation impl = Display.impl;
                        impl.setImageName(i.getImage(), getImageName());
                        Display.getInstance().callSerially(new Runnable() {
                            public void run() {
                                if(locked > 0) {
                                    hardCache = i;
                                }
                                cache = Display.getInstance().createSoftWeakRef(i);
                                Display.getInstance().getCurrent().repaint();                                
                                width = i.getWidth();
                                height = i.getHeight();
                            }
                        });
                    } catch(Exception err) {
                        Log.e(err);
                    }
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     */
    public void lock() {
        if(locked < 1) {
            locked = 1;
            if(cache != null) {
                hardCache = (Image)Display.getInstance().extractHardRef(cache);
            }
        } else {
            locked ++;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void unlock() {
        locked--;
        if(locked < 1) {
            if(hardCache != null) {
                if(cache == null || Display.getInstance().extractHardRef(cache) == null) {
                    cache = Display.getInstance().createSoftWeakRef(hardCache);
                }
                hardCache = null;
            }
            locked = 0;
        }
    }

    /**
     * Creates an image from the input stream 
     * 
     * @param i the resource
     * @return newly created encoded image
     * @throws java.io.IOException if thrown by the input stream
     */
    public static EncodedImage create(String i) throws IOException {
        return create(Display.getInstance().getResourceAsStream(EncodedImage.class, i));
    }

    /**
     * {@inheritDoc}
     */
    public Image subImage(int x, int y, int width, int height, boolean processAlpha)  {
        return getInternalImpl().subImage(x, y, width, height, processAlpha);
    }

    /**
     * {@inheritDoc}
     */
    public Image rotate(int degrees) {
        return getInternalImpl().rotate(degrees);
    }
    
    /**
     * {@inheritDoc}
     */
    public Image modifyAlpha(byte alpha) {
        return getInternalImpl().modifyAlpha(alpha);
    }
    
    /**
     * {@inheritDoc}
     */
    public Image modifyAlpha(byte alpha, int removeColor) {
        return getInternalImpl().modifyAlpha(alpha, removeColor);
    }

    /**
     * {@inheritDoc}
     */
    public Graphics getGraphics() {        
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public int getWidth() {
        if(width > -1) {
            return width;
        }
        width = getInternalImpl().getWidth();
        return width;
    }

    /**
     * {@inheritDoc}
     */
    public int getHeight() {
        if(height > -1) {
            return height;
        }
        height = getInternalImpl().getHeight();
        return height;
    }

    /**
     * {@inheritDoc}
     */
    protected void drawImage(Graphics g, Object nativeGraphics, int x, int y) {
        Display.impl.drawingEncodedImage(this);
        Image internal = getInternalImpl();
        if(width > -1 && height > -1 && (internal.getWidth() != width || internal.getHeight() != height)) {
            internal.drawImage(g, nativeGraphics, x, y, width, height);
        } else {
            internal.drawImage(g, nativeGraphics, x, y);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void drawImage(Graphics g, Object nativeGraphics, int x, int y, int w, int h) {
        Display.impl.drawingEncodedImage(this);
        getInternalImpl().drawImage(g, nativeGraphics, x, y, w, h);
    }

    /**
     * {@inheritDoc}
     */
    void getRGB(int[] rgbData,
            int offset,
            int x,
            int y,
            int width,
            int height) {
        getInternalImpl().getRGB(rgbData, offset, x, y, width, height);
    }

    /**
     * {@inheritDoc}
     */
    public void toRGB(RGBImage image,
            int destX,
            int destY,
            int x,
            int y,
            int width,
            int height) {
        getInternalImpl().toRGB(image, destX, destY, x, y, width, height);
    }

    /**
     * {@inheritDoc}
     */
    public Image scaledWidth(int width) {
        return getInternalImpl().scaledWidth(width);
    }

    /**
     * {@inheritDoc}
     */
    public Image scaledHeight(int height) {
        return getInternalImpl().scaledHeight(height);
    }

    /**
     * {@inheritDoc}
     */
    public Image scaledSmallerRatio(int width, int height) {
        return getInternalImpl().scaledSmallerRatio(width, height);
    }

    /**
     * Performs scaling using ImageIO to generate an encoded Image
     * @param width the width of the image, -1 to scale based on height and preserve aspect ratio
     * @param height the height of the image, -1 to scale based on width and preserve aspect ratio
     * @return new encoded image
     */
    public EncodedImage scaledEncoded(int width, int height) {
        if(width == getWidth() && height == getHeight()) {
            return this;
        }
        
        if(width < 0) {
            float ratio = ((float)height) / ((float)getHeight());
            width = Math.max(1, (int)(getWidth() * ratio));
        } else {
            if(height < 0) {
                float ratio = ((float)width) / ((float)getWidth());
                height = Math.max(1, (int)(getHeight() * ratio));
            }
        }
        
        try {
            ImageIO io = ImageIO.getImageIO();
            if(io != null) {
                String format = ImageIO.FORMAT_PNG;
                if(isOpaque() || !io.isFormatSupported(ImageIO.FORMAT_PNG)) {
                    if(io.isFormatSupported(ImageIO.FORMAT_JPEG)) {
                        format = ImageIO.FORMAT_JPEG;
                    }
                }
                if(io.isFormatSupported(format)) {
                    // do an image IO scale which is more efficient
                    ByteArrayOutputStream bo = new ByteArrayOutputStream();
                    io.save(new ByteArrayInputStream(getImageData()), bo, format, width, height, 0.9f);
                    EncodedImage img = EncodedImage.create(bo.toByteArray());
                    Util.cleanup(bo);
                    img.opaque = opaque;
                    img.opaqueChecked = opaqueChecked;
                    if(width > -1 && height > -1) {
                        img.width = width;
                        img.height = height;
                    }
                    return img;
                }
            }
        } catch(IOException err) {
            // normally this shouldn't happen but this will keep falling back to the existing scaled code
            Log.e(err);
        }
        return null;
    }
    
    /**
     * {@inheritDoc}
     */
    public Image scaled(int width, int height) {
        // J2ME/RIM don't support image IO and Windows Phone doesn't support PNG which prevents
        // scaling translucent images properly
        if(Display.getInstance().getProperty("encodedImageScaling", "true").equals("true") && 
                ImageIO.getImageIO() != null && ImageIO.getImageIO().isFormatSupported(ImageIO.FORMAT_PNG)) {
            return scaledEncoded(width, height);
        }
        return getInternalImpl().scaled(width, height);
    }

    /**
     * {@inheritDoc}
     */
    public void scale(int width, int height) {
        getInternalImpl().scale(width, height);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAnimation() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isOpaque() {
        if(opaqueChecked) {
            return opaque;
        }
        opaque = getInternalImpl().isOpaque();
        return opaque;
    }
}
