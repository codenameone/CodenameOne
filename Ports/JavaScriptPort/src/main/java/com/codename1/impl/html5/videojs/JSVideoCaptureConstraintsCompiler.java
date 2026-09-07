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
        // The size this platform uses to express the requested quality, or 0 when the
        // caller asked for no quality or pinned the size itself. Kept so the negotiated
        // result below can be compared against what the quality actually asked for.
        int qualityW = 0;
        int qualityH = 0;
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
            qualityW = prefW;
            qualityH = prefH;
        }
        if (prefW > 0 || prefH > 0) {
            MediaResult res = new MediaTool().query(prefW, prefH);
            out.preferredWidth(res.getWidth())
                    .preferredHeight(res.getHeight());
            // Report the quality back only when the device actually gave us the size that
            // quality maps to. Leaving the resolved quality at 0 unconditionally, as this
            // did, made isQualitySupported() answer false for every caller that asked for
            // QUALITY_LOW or QUALITY_HIGH -- the resolved value differed from the nonzero
            // preferred one -- even though the capture was constrained exactly as asked.
            // getUserMedia negotiates, so a device that cannot reach 1280x720 returns
            // something smaller, and then the quality genuinely was not honored.
            if (qualityW > 0 && res.getWidth() == qualityW && res.getHeight() == qualityH) {
                out.preferredQuality(vcc.getPreferredQuality());
            }
        } 
        out.preferredMaxLength(vcc.getPreferredMaxLength());
        return out;
    }

   
    
}
