/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

    /// Releases the surface and its renderer resources.
    void disposeSurface();
}
