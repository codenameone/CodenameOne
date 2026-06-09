/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.impl.ios;

import com.codename1.gpu.RenderView;
import com.codename1.gpu.Renderer;
import com.codename1.io.Log;

import java.util.HashMap;
import java.util.Map;

/// Java side driver for an iOS Metal `RenderView` peer. Owns the
/// `IOSGraphicsDevice` bound to one native Metal 3D context and forwards the
/// native render loop callbacks (init, resize, frame, dispose) to the
/// application supplied `Renderer`.
///
/// The native `CN1GL3D` MTKView invokes back into Java through the static
/// callbacks below, identifying its surface by the context peer handle. We keep
/// an identity map of live surfaces keyed by that handle so callbacks arriving
/// from the native render loop resolve to the right renderer.
class IOSGLSurface {
    // Live surfaces keyed by their native context peer handle. Native callbacks
    // carry the handle so we can dispatch to the owning surface.
    private static final Map<Long, IOSGLSurface> SURFACES = new HashMap<Long, IOSGLSurface>();

    private final Renderer renderer;
    private final IOSGraphicsDevice device;
    private final long contextPeer;
    private boolean initialized;
    private int lastWidth;
    private int lastHeight;

    IOSGLSurface(RenderView view, long contextPeer) {
        this.renderer = view.getRenderer();
        this.contextPeer = contextPeer;
        this.device = new IOSGraphicsDevice(contextPeer);
        synchronized (SURFACES) {
            SURFACES.put(Long.valueOf(contextPeer), this);
        }
    }

    long getContextPeer() {
        return contextPeer;
    }

    void setContinuous(boolean continuous) {
        IOSImplementation.nativeInstance.gl3dSetContinuous(contextPeer, continuous);
    }

    void requestRender() {
        IOSImplementation.nativeInstance.gl3dRequestRender(contextPeer);
    }

    void dispose() {
        synchronized (SURFACES) {
            SURFACES.remove(Long.valueOf(contextPeer));
        }
        try {
            renderer.onDispose(device);
        } catch (Throwable t) {
            Log.e(t);
        }
        device.destroy();
    }

    private void frame(int width, int height) {
        try {
            if (!initialized) {
                renderer.onInit(device);
                initialized = true;
                lastWidth = -1;
                lastHeight = -1;
            }
            if (width != lastWidth || height != lastHeight) {
                lastWidth = width;
                lastHeight = height;
                device.setViewport(0, 0, width, height);
                renderer.onResize(device, width, height);
            }
            renderer.onFrame(device);
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    // ---------------------------------------------------------------------
    // Native -> Java callbacks. Invoked from CN1GL3D.m on the render thread
    // that owns the Metal context. The native side has already begun the
    // frame (acquired the drawable and opened the command encoder) before
    // onFrameNative and presents/commits after it returns.
    // ---------------------------------------------------------------------

    /// Called from native code once per frame after the command encoder for the
    /// drawable has been opened. The Java renderer issues its draw calls here.
    static void onFrameNative(long contextPeer, int width, int height) {
        IOSGLSurface s;
        synchronized (SURFACES) {
            s = SURFACES.get(Long.valueOf(contextPeer));
        }
        if (s != null) {
            s.frame(width, height);
        }
    }

    /// Called from native code when the context is being torn down without an
    /// explicit Java side dispose (for example on context loss).
    static void onDisposeNative(long contextPeer) {
        IOSGLSurface s;
        synchronized (SURFACES) {
            s = SURFACES.remove(Long.valueOf(contextPeer));
        }
        if (s != null) {
            try {
                s.renderer.onDispose(s.device);
            } catch (Throwable t) {
                Log.e(t);
            }
        }
    }
}
