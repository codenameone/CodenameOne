/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.android;

import com.codename1.capture.VideoCaptureConstraints;

/**
 *
 * @author shannah
 */
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
