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
package com.codename1.impl.javase;

import com.codename1.gpu.Renderer;
import java.awt.image.BufferedImage;

/// Drives a renderer into the JavaSE port's software rasteriser with no
/// window: what `JavaSEGLSurface.paintComponent` does, without the Swing
/// component it does it in.
///
/// It is in the port's package because the rasteriser is package-private.
public final class OffscreenSurface {
    private final JavaSESoftwareDevice device = new JavaSESoftwareDevice();
    private boolean initialized;
    private int lastWidth = -1;
    private int lastHeight = -1;

    /// Renders one frame at a size and answers the rasteriser's image,
    /// which the next frame draws over.
    public BufferedImage frame(Renderer renderer, int width, int height) {
        device.resize(width, height);
        if (!initialized) {
            renderer.onInit(device);
            initialized = true;
            lastWidth = -1;
        }
        if (width != lastWidth || height != lastHeight) {
            lastWidth = width;
            lastHeight = height;
            renderer.onResize(device, width, height);
        }
        renderer.onFrame(device);
        return device.getImage();
    }
}
