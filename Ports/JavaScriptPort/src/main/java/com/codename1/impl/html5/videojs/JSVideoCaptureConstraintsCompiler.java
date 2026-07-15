/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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

package com.codename1.impl.html5.videojs;

import com.codename1.capture.VideoCaptureConstraints;
import com.codename1.impl.html5.videojs.MediaTool.MediaResult;

/**
 *
 * @author shannah
 */
public class JSVideoCaptureConstraintsCompiler implements VideoCaptureConstraints.Compiler {

    @Override
    public VideoCaptureConstraints compile(VideoCaptureConstraints vcc) {
        VideoCaptureConstraints out = new VideoCaptureConstraints();
        int prefW = vcc.getPreferredWidth();
        int prefH = vcc.getPreferredHeight();
        if (vcc.getPreferredQuality() != 0 && prefW == 0 && prefH == 0) {
            switch (vcc.getPreferredQuality()) {
                case VideoCaptureConstraints.QUALITY_LOW:
                    prefW = 640;
                    prefH = 480;
                    break;
                case VideoCaptureConstraints.QUALITY_HIGH:
                    prefW = 1280;
                    prefH = 720;
                    break;
            }
            out.preferredQuality(0);
        }
        if (prefW > 0 || prefH > 0) {
            MediaResult res = new MediaTool().query(prefW, prefH);
            out.preferredWidth(res.getWidth())
                    .preferredHeight(res.getHeight());
        } 
        out.preferredMaxLength(vcc.getPreferredMaxLength());
        return out;
    }

   
    
}
