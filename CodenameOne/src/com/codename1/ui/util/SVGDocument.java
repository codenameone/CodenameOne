/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.ui.util;

/// Cross-module view of an SVG document embedded in an Image. Implemented by
/// platform-specific SVG representations (e.g. the javase-svg port's SVG class)
/// so that the resource serializer in the css-compiler module can read and
/// write SVG metadata without a compile-time dependency on the port.
public interface SVGDocument {
    byte[] getSvgData();
    String getBaseURL();
    float getRatioW();
    float getRatioH();
    void setRatioW(float ratioW);
    void setRatioH(float ratioH);
    int[] getDpis();
    void setDpis(int[] dpis);
    int[] getWidthForDPI();
    void setWidthForDPI(int[] widthForDPI);
    int[] getHeightForDPI();
    void setHeightForDPI(int[] heightForDPI);
}
