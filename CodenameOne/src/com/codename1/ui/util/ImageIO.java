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
package com.codename1.ui.util;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Util;
import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Dimension;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Enable simple operations on image file &amp; image objects such as dynamic scaling
 * and storage to binary formats such as JPEG. Use Display.getImageIO() to get an instance
 * of this class.
 *
 * @author Shai Almog
 */
public abstract class ImageIO {
    /**
     * Indicates the JPEG output format
     */
    public static final String FORMAT_JPEG = "jpeg";

    /**
     * Indicates the PNG output format
     */
    public static final String FORMAT_PNG = "png";
    
    /**
     * Saves an image file at the given resolution, scaling if necessary
     * 
     * @param image source image stream
     * @param response resulting image output will be written to this stream
     * @param format the format for the image either FORMAT_PNG or FORMAT_JPEG
     * @param width the width for the resulting image, use -1 to not scale
     * @param height the height of the resulting image, use -1 to not scale
     * @param quality the quality for the resulting image output (applicable mostly for JPEG), a value between 0 and 1 notice that
     * this isn't implemented in all platforms.
     */
    public abstract void save(InputStream image, OutputStream response, String format, int width, int height, float quality) throws IOException;
    
    /**
     * Saves an image object to the given format
     * 
     * @param img the image object
     * @param response resulting image output will be written to this stream
     * @param format the format for the image either FORMAT_PNG or FORMAT_JPEG
     * @param quality the quality for the resulting image output (applicable mostly for JPEG), a value between 0 and 1 notice that
     * this isn't implemented in all platforms.
     */
    public void save(Image img, OutputStream response, String format, float quality) throws IOException {
        if(img instanceof EncodedImage) {
            EncodedImage i = (EncodedImage)img;
            save(new ByteArrayInputStream(i.getImageData()), response, format, i.getWidth(), i.getHeight(), quality);
        } else {
            if(img.getImage() == null) {
                Image img2 = Image.createImage(img.getWidth(), img.getHeight(), 0);
                Graphics g = img2.getGraphics();
                g.drawImage(img, 0, 0);
                saveImage(img2, response, format, quality);
            } else {
                saveImage(img, response, format, quality);
            }
        }
    }

    /**
     * Saves an image file at the given resolution, scaling if necessary
     * 
     * @param imageFilePath the image file path
     * @param response resulting image output will be written to this stream
     * @param format the format for the image either FORMAT_PNG or FORMAT_JPEG
     * @param width the width for the resulting image, use -1 to not scale
     * @param height the height of the resulting image, use -1 to not scale
     * @param quality the quality for the resulting image output (applicable mostly for JPEG), a value between 0 and 1 notice that
     * this isn't implemented in all platforms.
     */
    public void save(String imageFilePath, OutputStream response, String format, int width, int height, float quality) throws IOException{
        InputStream in = FileSystemStorage.getInstance().openInputStream(imageFilePath);
        save(in, response, format, width, height, quality); 
        Util.cleanup(in);
    }

    /**
     * Returns the image size in pixels
     * @param imageFilePath the path to the image
     * @return the size in pixels
     */
    public Dimension getImageSize(String imageFilePath) throws IOException {
        Image img = Image.createImage(imageFilePath);
        Dimension d = new Dimension(img.getWidth(), img.getHeight());
        img.dispose();
        return d;
    }
    
    /**
     * Scales an image on disk while maintaining an aspect ratio, the appropriate aspect size will be 
     * picked based on the status of scaleToFill
     * @param imageFilePath the path to the image
     * @param preferredOutputPath the url where the image will be saved
     * @param format the format for the image either FORMAT_JPEG or FORMAT_PNG
     * @param width the desired width, either width or height will be respected based on aspect dimensions 
     * @param height the desired height, either width or height will be respected based on aspect dimensions
     * @param quality the quality for the resulting image output (applicable mostly for JPEG), a value between 0 and 1 notice that
     * this isn't implemented in all platforms.
     * @param onlyDownscale will not scale if the resolution to scale will be higher in this case will return the imageFilePath
     * @param scaleToFill when set to true will pick the larger value so the resulting image will be at least as big as width x height, when set to false
     * will create an image that is no bigger than width x height
     * @return the url for the scaled image or the url of the unscaled image
     * @throws IOException if the operation fails
     */
    public String saveAndKeepAspect(String imageFilePath, String preferredOutputPath, String format, int width, int height, float quality, boolean onlyDownscale, boolean scaleToFill) throws IOException{
        Dimension d = getImageSize(imageFilePath);
        if(onlyDownscale) {
            if(scaleToFill) {
                if(d.getHeight() <= height || d.getWidth() <= width) {
                    return imageFilePath;
                }
            } else {
                if(d.getHeight() <= height && d.getWidth() <= width) {
                    return imageFilePath;
                }
            }
        }
        
        float ratio = ((float)d.getWidth()) / ((float)d.getHeight());
        int heightBasedOnWidth = (int)(((float)width) / ratio);
        int widthBasedOnHeight = (int)(((float)height) * ratio);
        if(scaleToFill) {
            if(heightBasedOnWidth >= width) {
                height = heightBasedOnWidth;
            } else {
                width = widthBasedOnHeight;
            }
        } else {
            if(heightBasedOnWidth > width) {
                width = widthBasedOnHeight;
            } else {
                height = heightBasedOnWidth;
            }
        }
        OutputStream im = FileSystemStorage.getInstance().openOutputStream(preferredOutputPath);
        save(imageFilePath, im, format, width, height, quality);
        return preferredOutputPath;
    }
    
    /**
     * Saves an image object to the given format
     * 
     * @param img the image object
     * @param response resulting image output will be written to this stream
     * @param format the format for the image either FORMAT_PNG or FORMAT_JPEG
     * @param quality the quality for the resulting image output (applicable mostly for JPEG), a value between 0 and 1 notice that
     * this isn't implemented in all platforms.
     */
    protected abstract void saveImage(Image img, OutputStream response, String format, float quality) throws IOException;
    
    /**
     * Indicates if the given format for output is supported by this implementation
     * 
     * @param format the format for the image either FORMAT_PNG or FORMAT_JPEG
     * @return true if supported
     */
    public abstract boolean isFormatSupported(String format);
    
    
    /**
     * Gets the ImageIO instance 
     * 
     * @return the image IO instance or null if image IO isn't supported for the 
     * given platform
     */
    public static ImageIO getImageIO(){
        return Display.getInstance().getImageIO();
    }
}
