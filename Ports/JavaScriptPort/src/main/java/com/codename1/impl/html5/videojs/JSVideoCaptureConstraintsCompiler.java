/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
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
