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
import com.codename1.ui.Display;
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
    ///
    /// The frame runs on the event dispatch thread, whichever thread asks:
    /// what a frame shows is then complete when this returns, which is what
    /// the checks compare. A port that draws on a thread of its own is
    /// [#frameOffEdt].
    public BufferedImage frame(final Renderer renderer, final int width, final int height) {
        if (Display.getInstance().isEdt()) {
            return frameOffEdt(renderer, width, height);
        }
        final BufferedImage[] drawn = new BufferedImage[1];
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            public void run() {
                drawn[0] = frameOffEdt(renderer, width, height);
            }
        });
        return drawn[0];
    }

    /// Renders one frame on the thread that calls, as iOS does on its main
    /// thread and Android on its GL thread. A game view asks the event
    /// dispatch thread for the texts of such a frame and does not wait, so
    /// one that is new is on the screen a frame after that thread ran.
    public BufferedImage frameOffEdt(Renderer renderer, int width, int height) {
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
