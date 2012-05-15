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

import com.codename1.ui.Display;
import com.codename1.ui.events.ActionListener;

/**
 * This is the main class for capturing media files from the device.
 * Use this class to invoke the native camera to capture images, audio or video
 * @author Chen
 */
public class Capture {
    
    /**
     * This method tries to invoke the device native camera to capture images.
     * The method returns immediately and the response will be sent asynchronously
     * to the given ActionListener Object
     * The image is saved as a jpeg to a file on the device.
     * 
     * use this in the actionPerformed to retrieve the file path
     * String path = (String) evt.getSource();
     * 
     * if evt returns null the image capture was cancelled by the user.
     * 
     * @param response a callback Object to retrieve the file path
     * @throws RuntimeException if this feature failed or unsupported on the platform
     */
    public static void capturePhoto(ActionListener response){    
        Display.getInstance().capturePhoto(response);
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
    
    
}
