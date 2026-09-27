/*
 * Copyright (c) 2019, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.android;

import com.codename1.capture.VideoCaptureConstraints;

public class AndroidVideoCaptureConstraintsCompiler implements VideoCaptureConstraints.Compiler {

    @Override
    public VideoCaptureConstraints compile(VideoCaptureConstraints cnst) {
        VideoCaptureConstraints out = new VideoCaptureConstraints(cnst);
        
        // We can't actually support explicit width and height constraints
        // right now, so we set these to zero
        out.preferredHeight(0);
        out.preferredWidth(0);
        
        // But we do support low and high quality
        switch (cnst.getPreferredQuality()) {
            case VideoCaptureConstraints.QUALITY_LOW:
            case VideoCaptureConstraints.QUALITY_HIGH:
                break;
            default:
                // If the constraints don't set quality, but they do set width and height constraints
                // we can provide a low/high quality hint that might help to satisfy the
                // caller's intentions.
                // Smaller than 640x480 we'll call low quality.  That number is just pulled out of the air.
                if (cnst.getPreferredHeight() > 0 && cnst.getPreferredWidth() > 0) {
                    if (cnst.getPreferredHeight() <= 480 || cnst.getPreferredWidth() <= 640) {
                        out.preferredQuality(VideoCaptureConstraints.QUALITY_LOW);
                    } else {
                        out.preferredQuality(VideoCaptureConstraints.QUALITY_HIGH);
                    }
                }
        }
        
        return out;
        
        
    }
    
}
