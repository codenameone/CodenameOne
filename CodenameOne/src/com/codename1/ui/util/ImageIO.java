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

import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;
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
     * @param quality the quality for the resulting image output (applicable mostly for JPEG), a value between 0 and 1.
     */
    public abstract void save(InputStream image, OutputStream response, String format, int width, int height, float quality) throws IOException;
    
    /**
     * Saves an image object to the given format
     * 
     * @param img the image object
     * @param response resulting image output will be written to this stream
     * @param format the format for the image either FORMAT_PNG or FORMAT_JPEG
     * @param quality the quality of the image, a value between 0 and 1.
     */
    public void save(Image img, OutputStream response, String format, float quality) throws IOException {
        if(img instanceof EncodedImage) {
            EncodedImage i = (EncodedImage)img;
            save(new ByteArrayInputStream(i.getImageData()), response, format, i.getWidth(), i.getHeight(), quality);
        } else {
            saveImage(img, response, format, quality);
        }
    }

    /**
     * Saves an image object to the given format
     * 
     * @param img the image object
     * @param response resulting image output will be written to this stream
     * @param format the format for the image either FORMAT_PNG or FORMAT_JPEG
     * @param quality the quality of the image, a value between 0 and 1.
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
     * @return ImageIO instance
     */
    public static ImageIO getImageIO(){
        return Display.getInstance().getImageIO();
    }
}
