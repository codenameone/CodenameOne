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
package com.codename1.impl.ios;

import com.codename1.capture.VideoCaptureConstraints;
import com.codename1.ui.geom.Dimension;
import java.util.Arrays;
import java.util.Objects;

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
