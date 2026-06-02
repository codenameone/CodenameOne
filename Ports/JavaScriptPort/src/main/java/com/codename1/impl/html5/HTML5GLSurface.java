/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

import com.codename1.gpu.RenderView;
import com.codename1.gpu.Renderer;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.browser.AnimationFrameCallback;
import com.codename1.html5.js.browser.Window;
import com.codename1.html5.js.dom.HTMLCanvasElement;

import static com.codename1.impl.html5.HTML5Implementation.scaleCoord;

/// Browser peer that hosts an `HTMLCanvasElement` with a WebGL context and drives
/// the application `Renderer`. The canvas is wrapped as a Codename One native
/// peer (an `HTML5Peer`), participating in normal layout and z-ordering. The
/// canvas backing-store size is kept in sync with the peer's pixel size; the
/// renderer lifecycle callbacks (`onInit`, `onResize`, `onFrame`, `onDispose`)
/// are driven from layout changes and a `requestAnimationFrame` loop.
class HTML5GLSurface extends HTML5Peer {
    private final RenderView view;
    private final Renderer renderer;
    private final HTMLCanvasElement canvas;
    private HTML5GraphicsDevice device;
    private boolean initialized;
    private boolean contextLost;
    private int lastW = -1;
    private int lastH = -1;
    private boolean continuous;
    private int animationFrameId = -1;
    private boolean framePending;

    private final AnimationFrameCallback frameCallback = new AnimationFrameCallback() {
        public void onAnimationFrame(double timestamp) {
            animationFrameId = -1;
            framePending = false;
            renderFrame();
            if (continuous) {
                scheduleFrame();
            }
        }
    };

    private HTML5GLSurface(HTMLCanvasElement canvas, RenderView view) {
        super(canvas);
        this.canvas = canvas;
        this.view = view;
        this.renderer = view.getRenderer();
    }

    /// Creates a WebGL backed surface for the supplied render view, or returns
    /// null if a WebGL context could not be obtained.
    static HTML5GLSurface create(RenderView view) {
        HTMLCanvasElement canvas = (HTMLCanvasElement)
                Window.current().getDocument().createElement("canvas");
        JSObject gl = getWebGLContext(canvas);
        if (gl == null) {
            return null;
        }
        HTML5GLSurface surface = new HTML5GLSurface(canvas, view);
        surface.device = new HTML5GraphicsDevice(gl);
        return surface;
    }

    void setContinuous(boolean continuous) {
        this.continuous = continuous;
        if (continuous) {
            scheduleFrame();
        } else {
            cancelFrame();
        }
    }

    void requestRender() {
        if (continuous) {
            return;
        }
        scheduleFrame();
    }

    private void scheduleFrame() {
        if (framePending) {
            return;
        }
        framePending = true;
        animationFrameId = Window.current().requestAnimationFrame(frameCallback);
    }

    private void cancelFrame() {
        if (animationFrameId >= 0) {
            Window.current().cancelAnimationFrame(animationFrameId);
            animationFrameId = -1;
        }
        framePending = false;
    }

    private void syncSize() {
        int w = scaleCoord(getWidth());
        int h = scaleCoord(getHeight());
        if (w <= 0 || h <= 0) {
            return;
        }
        if (canvas.getWidth() != w) {
            canvas.setWidth(w);
        }
        if (canvas.getHeight() != h) {
            canvas.setHeight(h);
        }
        if (w != lastW || h != lastH) {
            lastW = w;
            lastH = h;
            try {
                renderer.onResize(device, w, h);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private void renderFrame() {
        if (contextLost || device == null) {
            return;
        }
        int w = scaleCoord(getWidth());
        int h = scaleCoord(getHeight());
        if (w <= 0 || h <= 0) {
            return;
        }
        if (!initialized) {
            try {
                renderer.onInit(device);
            } catch (Throwable t) {
                t.printStackTrace();
            }
            initialized = true;
            lastW = -1;
        }
        syncSize();
        try {
            renderer.onFrame(device);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        if (!initialized) {
            renderFrame();
        }
        if (continuous) {
            scheduleFrame();
        }
    }

    @Override
    protected void onPositionSizeChange() {
        super.onPositionSizeChange();
        if (initialized && !contextLost) {
            syncSize();
            requestRender();
        }
    }

    @Override
    protected void deinitialize() {
        cancelFrame();
        super.deinitialize();
    }

    void disposeSurface() {
        cancelFrame();
        if (initialized && !contextLost) {
            try {
                renderer.onDispose(device);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        initialized = false;
    }

    @JSBody(params = {"canvas"},
            script = "try { return canvas.getContext('webgl') || canvas.getContext('experimental-webgl') || null; }"
                    + " catch (e) { return null; }")
    private static native JSObject getWebGLContext(HTMLCanvasElement canvas);
}
