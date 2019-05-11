/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.ios;

import com.codename1.capture.VideoCaptureConstraints;
import com.codename1.ui.geom.Dimension;
import java.util.Arrays;
import java.util.Objects;

/**
 *
 * @author shannah
 */
public class IOSVideoCaptureConstraintsCompiler implements VideoCaptureConstraints.Compiler {

    @Override
    public VideoCaptureConstraints compile(VideoCaptureConstraints cnst) {
        VideoCaptureConstraints out = new VideoCaptureConstraints(cnst);
        int[] prefSize = new int[]{cnst.getPreferredWidth(), cnst.getPreferredHeight()};
        
        Object[] supportedDimensions = new Object[]{
            new int[]{640, 480},
            new int[]{1280,720},
            new int[]{960, 540}
        };
        boolean dimensionsSupported = false;
        for (Object o : supportedDimensions) {
            int[] dim = (int[]) o;
            if (Arrays.equals(dim, prefSize)) {
                dimensionsSupported = true;
                break;
            }
        }
        if (!dimensionsSupported) {
            out.preferredWidth(0).preferredHeight(0);
        }
        out.preferredMaxFileSize(0);
        
        if (prefSize[0] != 0 && prefSize[1] != 0 && cnst.getPreferredQuality() == 0) {
            // Use the preferred size to infer a quality value.
            if (prefSize[0] <= 320 || prefSize[1] <= 240) {
                out.preferredQuality(VideoCaptureConstraints.QUALITY_LOW);
            } else if (prefSize[0] > 800 || prefSize[1] > 600) {
                out.preferredQuality(VideoCaptureConstraints.QUALITY_HIGH);
            }
        }
        return out;
    }
    
}
