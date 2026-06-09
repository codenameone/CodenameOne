/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.impl.windows;

import com.codename1.gpu.RenderView;
import com.codename1.gpu.Renderer;
import com.codename1.io.Log;
import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.util.UITimer;

/// Native Windows `RenderView` peer backed by Direct3D 11. The Windows port
/// renders peers via the "peer image" model (a snapshot the lightweight
/// `PeerComponent.paint()` draws into the offscreen UI buffer), so this surface
/// renders one GPU frame to an offscreen D3D render target and reads it back as
/// an image each time the component paints. That avoids any native->Java render
/// loop: the application `Renderer` is driven synchronously inside
/// `generatePeerImage`.
class WindowsGLSurface extends PeerComponent {
    private final Renderer renderer;
    private final WindowsGraphicsDevice device;
    private final long contextPeer;
    private boolean initialized;
    private boolean continuous;
    private int lastWidth = -1;
    private int lastHeight = -1;
    private UITimer animationTimer;

    WindowsGLSurface(RenderView view) {
        super(null);
        this.renderer = view.getRenderer();
        this.contextPeer = WindowsNative.gl3dCreateContext();
        this.device = new WindowsGraphicsDevice(contextPeer);
    }

    long getContextPeer() {
        return contextPeer;
    }

    void setContinuous(boolean continuous) {
        this.continuous = continuous;
        if (continuous) {
            if (animationTimer == null && getComponentForm() != null) {
                animationTimer = UITimer.timer(16, true, getComponentForm(), new Runnable() {
                    public void run() {
                        repaint();
                    }
                });
            }
        } else if (animationTimer != null) {
            animationTimer.cancel();
            animationTimer = null;
        }
    }

    void requestRender() {
        repaint();
    }

    @Override
    protected boolean shouldRenderPeerImage() {
        return contextPeer != 0;
    }

    @Override
    protected Image generatePeerImage() {
        if (contextPeer == 0) {
            return null;
        }
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }
        try {
            WindowsNative.gl3dBeginFrame(contextPeer, w, h);
            if (!initialized) {
                renderer.onInit(device);
                initialized = true;
                lastWidth = -1;
                lastHeight = -1;
            }
            if (w != lastWidth || h != lastHeight) {
                lastWidth = w;
                lastHeight = h;
                device.setViewport(0, 0, w, h);
                renderer.onResize(device, w, h);
            }
            renderer.onFrame(device);
            byte[] png = WindowsNative.gl3dCaptureFrame(contextPeer);
            if (png == null) {
                return null;
            }
            return EncodedImage.create(png);
        } catch (Throwable t) {
            Log.e(t);
            return null;
        }
    }

    @Override
    protected Dimension calcPreferredSize() {
        return new Dimension(Display.getInstance().getDisplayWidth(),
                Display.getInstance().getDisplayHeight());
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        if (continuous) {
            setContinuous(true);
        }
    }

    @Override
    protected void deinitialize() {
        if (animationTimer != null) {
            animationTimer.cancel();
            animationTimer = null;
        }
        super.deinitialize();
    }

    void dispose() {
        if (animationTimer != null) {
            animationTimer.cancel();
            animationTimer = null;
        }
        try {
            renderer.onDispose(device);
        } catch (Throwable t) {
            Log.e(t);
        }
        device.destroy();
    }
}
