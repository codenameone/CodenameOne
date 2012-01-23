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
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

/**
 * This class is identical to FileEncodedImage with the difference of using
 * asynchronious loading for files (and the animation framework) which will
 * not work for all cases (e.g. renderers) but could improve some performance/RAM
 * aspects.
 *
 * @author Shai Almog
 */
public class FileEncodedImageAsync extends EncodedImage {
    private static int yieldDuration = 20;

    /**
     * Indicates the time to yield between image loads to throttle download speed
     *
     * @param d delay of the yield
     */
    public static void setYieldDuration(int d) {
        yieldDuration = d;
    }

    private String fileName;
    private boolean changePending;
    private boolean imageCreated;
    private byte[] imageData;
    private byte[] placeholder;
    private static Thread imageDataThread;
    private static Vector loadingQueue;
    private static final Object LOCK = new Object();
    private FileEncodedImageAsync(String fileName, byte[] placeholder, int w, int h) {
        super(w, h);
        this.fileName = fileName;
        this.placeholder = placeholder;
    }

    private static Thread createLoadingThread() {
        return new Thread() {
            public void run() {
                setPriority(MIN_PRIORITY + 1);
                while(Display.isInitialized() && loadingQueue.size() > 0) {
                    FileEncodedImageAsync f;
                    synchronized(LOCK) {
                        f = (FileEncodedImageAsync)loadingQueue.elementAt(0);
                        loadingQueue.removeElementAt(0);
                    }
                    InputStream i = null;
                    try {
                        final byte[] imageData = new byte[(int) FileSystemStorage.getInstance().getLength(f.fileName)];
                        i = FileSystemStorage.getInstance().openInputStream(f.fileName);
                        Util.readFully(i, imageData);
                        i.close();

                        // we need to change the image on the EDT to avoid potential race conditions
                        final FileEncodedImageAsync currentImage = f;
                        Display.getInstance().callSerially(new Runnable() {
                            public void run() {
                                currentImage.imageData = imageData;
                                currentImage.resetCache();
                                currentImage.changePending = true;
                                currentImage.imageCreated = false;
                            }
                        });

                        // yield for other things running on the device
                        try {
                            sleep(yieldDuration);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    } finally {
                        Util.cleanup(i);
                    }
                }
                synchronized(LOCK) {
                    if(loadingQueue.size() == 0) {
                        imageDataThread = null;
                        loadingQueue = null;
                        return;
                    }
                }
                // still not over, this run is here to prevent us from holding the lock indefinetly.
                run();
            }
        };
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
            if(imageDataThread == null) {
                imageDataThread = createLoadingThread();
                loadingQueue = new Vector();
                imageDataThread.start();
            }
            if(!loadingQueue.contains(this)) {
                loadingQueue.addElement(this);
            }
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
     */
    public static FileEncodedImageAsync create(String fileName, byte[] placeholder, int width, int height) {
        return new FileEncodedImageAsync(fileName, placeholder, width, height);
    }
}
