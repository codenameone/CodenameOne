/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.impl.gpu;

import com.codename1.gpu.RenderView;
import com.codename1.ui.PeerComponent;

/// Per-platform backend for the portable 3D GPU API. A platform that supports
/// `com.codename1.gpu.RenderView` returns a single `GpuImplementation` from
/// `CodenameOneImplementation.getGpuImplementation()`; platforms without a 3D
/// backend return null. Grouping the peer factory and its lifecycle hooks into
/// one class lets each port segregate all of its GPU wiring in one place instead
/// of scattering individual overrides across the platform implementation.
///
/// This type is internal: applications interact with the GPU API through
/// `RenderView` and `com.codename1.gpu.GraphicsDevice`, never with this class.
public abstract class GpuImplementation {
    /// Creates the native GPU peer that backs a `RenderView`. The peer owns the
    /// platform GPU context and drives the view's `Renderer`.
    ///
    /// #### Parameters
    ///
    /// - `view`: the render view requesting a peer
    ///
    /// #### Returns
    ///
    /// the native GPU peer, or null if a peer could not be created
    public abstract PeerComponent createPeer(RenderView view);

    /// Sets whether a GPU peer renders continuously or only on demand.
    ///
    /// #### Parameters
    ///
    /// - `peer`: a peer previously returned from `createPeer`
    ///
    /// - `continuous`: true to render every frame
    public abstract void setContinuous(PeerComponent peer, boolean continuous);

    /// Requests that a GPU peer render a single frame. No effect in continuous
    /// mode.
    ///
    /// #### Parameters
    ///
    /// - `peer`: a peer previously returned from `createPeer`
    public abstract void requestRender(PeerComponent peer);
}
