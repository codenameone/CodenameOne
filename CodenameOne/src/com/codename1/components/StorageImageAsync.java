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
package com.codename1.components;

import com.codename1.io.Storage;
import com.codename1.io.Util;
import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;
import java.io.InputStream;

/**
 * Asynchronous storage image that loads in the background and not during the paint
 * cycle effectively not blocking the EDT drawing speed.
 *
 * @author Shai Almog
 */
public class StorageImageAsync extends EncodedImage {
    private String fileName;
    private boolean changePending;
    private boolean imageCreated;
    private byte[] imageData;
    private Image placeholderImage;
    private boolean queued;
    private static final Object LOCK = new Object();

    private StorageImageAsync(String fileName, Image placeholderImage) {
        super(placeholderImage.getWidth(), placeholderImage.getHeight());
        this.fileName = fileName;
        this.placeholderImage = placeholderImage;
    }

    /**
     * {@inheritDoc}
     */
    protected void resetCache() {
        super.resetCache();
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    public byte[] getImageData() {
        if(imageData != null) {
            return imageData;
        }
        synchronized(LOCK) {
            if(queued) {
                return null;
            }
            queued = true;
            Display.getInstance().scheduleBackgroundTask(new Runnable() {
                public void run() {
                    InputStream i = null;
                    try {
                        final byte[] imageDataLocal = (byte[])Storage.getInstance().readObject(fileName);

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
        return null;
    }
    
    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    public boolean isAnimation() {
        return true;
    }
    
    /**
     * Creates an encoded image that maps to a storage file thus allowing to
     * seamlessly cache images as needed. This only works reasonably well for very small
     * files.
     *
     * @param fileName the name of the storage file
     * @param placeholder an image that must be of the same size as the EncodedImage
     * @return image that will load the file seamlessly
     */
    public static StorageImageAsync create(String fileName, Image placeholder) {
        return new StorageImageAsync(fileName, placeholder);
    }
}
