/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.gpu;

/// Application supplied callback that drives the contents of a `RenderView`.
/// The methods are always invoked on the platform render thread that owns the
/// GPU context; never touch Codename One UI components directly from these
/// callbacks. Use `RenderView.requestRender()` or
/// `RenderView.setContinuous(boolean)` to schedule frames.
public interface Renderer {
    /// Invoked once after the GPU context and its `GraphicsDevice` have been
    /// created and are current. Allocate buffers, textures and materials here.
    ///
    /// #### Parameters
    ///
    /// - `device`: the graphics device bound to this view
    void onInit(GraphicsDevice device);

    /// Invoked when the drawable surface size changes, including once after
    /// initialization. Reconfigure projection matrices and viewports here.
    ///
    /// #### Parameters
    ///
    /// - `device`: the graphics device bound to this view
    ///
    /// - `width`: the new drawable width in pixels
    ///
    /// - `height`: the new drawable height in pixels
    void onResize(GraphicsDevice device, int width, int height);

    /// Invoked once per frame to render the scene. Issue draw calls against the
    /// supplied device.
    ///
    /// #### Parameters
    ///
    /// - `device`: the graphics device bound to this view
    void onFrame(GraphicsDevice device);

    /// Invoked when the GPU context is being torn down (for example when the
    /// view is removed from the UI). Release any resources that are not owned by
    /// the device. May be invoked with a null device when the context was lost.
    ///
    /// #### Parameters
    ///
    /// - `device`: the graphics device bound to this view, or null if the
    ///   context was already lost
    void onDispose(GraphicsDevice device);
}
