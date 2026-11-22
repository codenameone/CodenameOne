/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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

import javax.swing.JComponent;

/// Lifecycle handle the JavaSE port keeps for a `RenderView` peer, independent of
/// whether the surface is backed by the real JOGL OpenGL renderer or the
/// software fallback. Lets the implementation drive both the same way.
interface JavaSEGpuSurface {
    /// The AWT component to wrap as the Codename One native peer.
    JComponent getComponent();

    /// Switches the surface between continuous (animation loop) and on-demand
    /// rendering.
    void setContinuous(boolean continuous);

    /// Requests that a single frame be rendered.
    void requestRender();

    /// The last frame this surface drew, or null before the first. Reading it
    /// renders nothing, so a screenshot or recording thread may call it.
    java.awt.image.BufferedImage lastFrame();

    /// Releases the surface and its renderer resources.
    void disposeSurface();

    /// Hidden simulator (`cn1.simulator.hidden`) only: run after each frame the
    /// surface renders on its own timer, since no window paints it. The port
    /// copies the frame into the peer buffer that captures and recordings read.
    void setAfterOffscreenFrame(Runnable afterFrame);
}
