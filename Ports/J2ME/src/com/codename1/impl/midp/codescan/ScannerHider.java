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

package com.codename1.impl.midp.codescan;

import com.codename1.codescan.CodeScanner;
import com.codename1.impl.midp.GameCanvasImplementation;
import com.codename1.impl.midp.MMAPIPlayer;

/**
 * Class that effectively hides the code scanner API
 *
 * @author Shai Almog
 */
public class ScannerHider {
    /**
     * Returns the native implementation of the code scanner or null
     *
     * @return code scanner instance
     */
    public CodeScanner getCodeScanner(Object canvas) {
        try {
            MMAPIPlayer player = null;

            String platform = System.getProperty("microedition.platform");
            if (platform != null && platform.indexOf("Nokia") >= 0) {
                try {
                    player = MMAPIPlayer.createPlayer("capture://image", null);                
                } catch (Throwable e) {
                    // Ignore all exceptions for image capture, continue with video capture...
                }
            }
            if (player == null) {
                try {
                    player = MMAPIPlayer.createPlayer("capture://video", null);                
                } catch (Exception e) {
                    // The Nokia 2630 throws this if image/video capture is not supported
                    throw new RuntimeException("Image/video capture not supported on this phone");
                }
            }

            GameCanvasImplementation.MIDPVideoComponent video = new GameCanvasImplementation.MIDPVideoComponent(player, canvas);
            video.setFocusable(false);
            return new CodeScannerImpl(video);
        } catch(Throwable t) {
            return null;
        }
    }


}
