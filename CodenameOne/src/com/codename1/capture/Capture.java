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
package com.codename1.capture;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.util.ImageIO;
import com.codename1.util.StringUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * <p>{@code Capture} can "capture" media files from the device e.g. record audio, video and snap photos.
 * Notice that files returned by this class are potentially temporary files and might be
 * deleted by the OS in the future.<br>
 * The code below demonstrates the capturing of a photo thru this API:</p>
 * <script src="https://gist.github.com/codenameone/b18c37dfcc7de752e0e6.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/graphics-image-masking.png" alt="Picture after the capture was complete and the resulted image was rounded. The background was set to red so the rounding effect will be more noticeable" />
 *<p>
 * The code below demonstrates capturing and playing back audio files using this API:
 * </p>
 * <script src="https://gist.github.com/codenameone/a347dc9dcadaa759d0cb.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/capture-audio.png" alt="Captured recordings in the demo" />
 * 
 * @author Chen
 */
public class Capture {
    
    /**
     * Returns true if the device has camera false otherwise.
     * @return true if the device has a camera
     */ 
    public static boolean hasCamera(){
        return Display.getInstance().hasCamera();
    }
    
    /**
     * This method tries to invoke the device native camera to capture images.
     * The method returns immediately and the response will be sent asynchronously
     * to the given ActionListener Object
     * The image is saved as a jpeg to a file on the device.
     * 
     * use this in the actionPerformed to retrieve the file path
     * String path = (String) evt.getSource();
     * 
     * if evt returns null the image capture was canceled by the user.
     * 
     * @param response a callback Object to retrieve the file path
     * @throws RuntimeException if this feature failed or unsupported on the platform
     */
    public static void capturePhoto(ActionListener response){    
        Display.getInstance().capturePhoto(response);
    }

    /**
     * Invokes the camera and takes a photo synchronously while blocking the EDT
     * @return the photo file location or null if the user canceled
     */
    public static String capturePhoto() {
        return capturePhoto(-1, -1);
    }
    
    /**
     * Same as captureAudio only a blocking version that holds the EDT
     * @return the photo file location or null if the user canceled
     */
    public static String captureAudio() {
        CallBack c = new CallBack();
        captureAudio(c);
        Display.getInstance().invokeAndBlock(c);
        return c.url;
    }

    
    /**
     * Same as captureVideo only a blocking version that holds the EDT
     * @return the photo file location or null if the user canceled
     */
    public static String captureVideo() {
        CallBack c = new CallBack();
        captureVideo(c);
        Display.getInstance().invokeAndBlock(c);
        return c.url;
    }

    /**
     * <p>Invokes the camera and takes a photo synchronously while blocking the EDT, the sample below
     * demonstrates a simple usage and applying a mask to the result</p>
     * <script src="https://gist.github.com/codenameone/b18c37dfcc7de752e0e6.js"></script>
     * <img src="https://www.codenameone.com/img/developer-guide/graphics-image-masking.png" alt="Picture after the capture was complete and the resulted image was rounded. The background was set to red so the rounding effect will be more noticeable" />
     * 
     * @param width the target width for the image if possible, some platforms don't support scaling. To maintain aspect ratio set to -1
     * @param height the target height for the image if possible, some platforms don't support scaling. To maintain aspect ratio set to -1
     * @return the photo file location or null if the user canceled
     */
    public static String capturePhoto(int width, int height) {
        CallBack c = new CallBack();
        if("ios".equals(Display.getInstance().getPlatformName())) {
            // workaround for threading issues in iOS https://github.com/codenameone/CodenameOne/issues/2246
            capturePhoto(c);
            Display.getInstance().invokeAndBlock(c);
            ImageIO scale = Display.getInstance().getImageIO();
            if(scale != null) {
                try {
                    String path = c.url.substring(0, c.url.indexOf(".")) + "s" + c.url.substring(c.url.indexOf("."));
                    OutputStream os = FileSystemStorage.getInstance().openOutputStream(path);
                    scale.save(c.url, os, ImageIO.FORMAT_JPEG, width, height, 1);
                    Util.cleanup(os);
                    FileSystemStorage.getInstance().delete(c.url);
                    return path;
                } catch (IOException ex) {
                    Log.e(ex);
                }
            }
        } else {
            c.targetWidth = width;
            c.targetHeight = height;
            capturePhoto(c);
            Display.getInstance().invokeAndBlock(c);
        }
        return c.url;
    }
        
    /**
     * This method tries to invoke the device native hardware to capture audio.
     * The method returns immediately and the response will be sent asynchronously
     * to the given ActionListener Object
     * The audio is saved to a file on the device.
     * 
     * use this in the actionPerformed to retrieve the file path
     * String path = (String) evt.getSource();
     * 
     * @param response a callback Object to retrieve the file path
     * @throws RuntimeException if this feature failed or unsupported on the platform
     */
    public static void captureAudio(ActionListener response){    
        Display.getInstance().captureAudio(response);
    }

    /**
     * This method tries to invoke the device native camera to capture video.
     * The method returns immediately and the response will be sent asynchronously
     * to the given ActionListener Object
     * The video is saved to a file on the device.
     * 
     * use this in the actionPerformed to retrieve the file path
     * String path = (String) evt.getSource();
     * 
     * @param response a callback Object to retrieve the file path
     * @throws RuntimeException if this feature failed or unsupported on the platform
     */
    public static void captureVideo(ActionListener response){    
        Display.getInstance().captureVideo(response);
    }
    
    static class CallBack implements ActionListener, Runnable {
        String url;
        private boolean completed;
        private int targetWidth = -1;
        private int targetHeight = -1;
        public void actionPerformed(ActionEvent evt) {
            if(evt == null) {
                url = null;
            } else {
                url = (String)evt.getSource();
            }
            completed = true;
            synchronized(this) {
                this.notify();
            }
        }

        public void run() {
            while(!completed) {
                synchronized(this) {
                    try {
                        this.wait();
                    } catch (InterruptedException ex) {
                    }
                }
            }
            if(url == null) {
                return;
            }
            if(targetWidth > 0 || targetHeight > 0) {
                ImageIO scale = Display.getInstance().getImageIO();
                if(scale != null) {
                    try {
                        
                        String path = url.substring(0, url.indexOf(".")) + "s" + url.substring(url.indexOf("."));
                        OutputStream os = FileSystemStorage.getInstance().openOutputStream(path);
                        scale.save(url, os, ImageIO.FORMAT_JPEG, targetWidth, targetHeight, 1);
                        Util.cleanup(os);
                        FileSystemStorage.getInstance().delete(url);
                        url = path;
                    } catch (IOException ex) {
                        Log.e(ex);
                    }
                }
            }
        }
        
    }
}
