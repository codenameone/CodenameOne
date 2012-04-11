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

package com.codename1.components;

import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Util;
import java.io.InputStream;
import java.util.Vector;

/**
 * This class is identical to FileEncodedImage with the difference of using
 * asynchronous loading for files (and the animation framework) which will
 * not work for all cases (e.g. renderers) but could improve some performance/RAM
 * aspects.
 *
 * @author Shai Almog
 */
public class FileEncodedImageAsync extends EncodedImage {
    private String fileName;
    private boolean changePending;
    private boolean imageCreated;
    private byte[] imageData;
    private byte[] placeholder;
    private Image placeholderImage;
    private boolean queued;
    private static final Object LOCK = new Object();
    private FileEncodedImageAsync(String fileName, byte[] placeholder, int w, int h) {
        super(w, h);
        this.fileName = fileName;
        this.placeholder = placeholder;
    }

    private FileEncodedImageAsync(String fileName, Image placeholderImage) {
        super(placeholderImage.getWidth(), placeholderImage.getHeight());
        this.fileName = fileName;
        this.placeholderImage = placeholderImage;
    }

    /**
     * @inheritDoc
     */
    protected void resetCache() {
        super.resetCache();
    }

    /**
     * @inheritDoc
     */
    protected Image getInternal() {
        if(imageData == null) {
            if(!queued) {
                getImageData();
            }
            return placeholderImage;
        }
        imageCreated = true;
        return super.getInternal();
    }

    /**
     * @inheritDoc
     */
    public byte[] getImageData() {
        if(imageData != null) {
            return imageData;
        }
        synchronized(LOCK) {
            if(queued) {
                return placeholder;
            }
            queued = true;
            Display.getInstance().scheduleBackgroundTask(new Runnable() {
                public void run() {
                    InputStream i = null;
                    try {
                        final byte[] imageDataLocal = new byte[(int) FileSystemStorage.getInstance().getLength(fileName)];
                        i = FileSystemStorage.getInstance().openInputStream(fileName);
                        Util.readFully(i, imageDataLocal);
                        i.close();

                        // we need to change the image on the EDT to avoid potential race conditions
                        Display.getInstance().callSerially(new Runnable() {
                            public void run() {
                                imageData = imageDataLocal;
                                resetCache();
                                changePending = true;
                                imageCreated = false;
                            }
                        });
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    } finally {
                        queued = false;
                        Util.cleanup(i);
                    }
                }
            });
        }
        if(placeholderImage != null) {
            return null;
        }
        return placeholder;
    }
    
    /**
     * @inheritDoc
     */
    public boolean animate() {
        if(changePending) {
            if(imageCreated) {
                changePending = false;
            }
            return true;
        }
        return false;
    }

    /**
     * @inheritDoc
     */
    public boolean isAnimation() {
        return true;
    }

    /**
     * Creates an encoded image that maps to a local file thus allowing to
     * seamlessly fetch files as needed. This only works reasonably well for very small
     * files.
     *
     * @param fileName the name of the file
     * @param placeholder a placeholder image until the actual image loads
     * @param width the width of the file or -1 if unknown (notice that this will improve performance)
     * @param height the height of the file or -1 if unknown (notice that this will improve performance)
     * @return image that will load the file seamlessly
     * @deprecated use the version that accepts a name and a placeholderImage
     */
    public static FileEncodedImageAsync create(String fileName, byte[] placeholder, int width, int height) {
        return new FileEncodedImageAsync(fileName, placeholder, width, height);
    }

    /**
     * Creates an encoded image that maps to a local file thus allowing to
     * seamlessly fetch files as needed. This only works reasonably well for very small
     * files.
     *
     * @param fileName the name of the file
     * @param placeholder an image that will occupy the space
     * @return image that will load the file seamlessly
     */
    public static FileEncodedImageAsync create(String fileName, Image placeholder) {
        return new FileEncodedImageAsync(fileName, placeholder);
    }
}
