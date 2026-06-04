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
import com.codename1.html5.js.webgl.WebGLRenderingContext;

import static com.codename1.impl.html5.HTML5Implementation.scaleCoord;

/// Browser peer that hosts an `HTMLCanvasElement` with a WebGL context and drives
/// the application `Renderer`. The canvas is wrapped as a Codename One native
/// peer (an `HTML5Peer`), participating in normal layout and z-ordering. The
/// canvas backing-store size is kept in sync with the peer's pixel size; the
/// renderer lifecycle callbacks (`onInit`, `onResize`, `onFrame`, `onDispose`)
/// are driven from layout changes and a `requestAnimationFrame` loop.
class HTML5GLSurface extends HTML5Peer {
    /// Live WebGL peers, composited into screenshots by HTML5Implementation so
    /// that 3D scenes (which render to their own canvas, separate from the
    /// Codename One output canvas) appear in captured images.
    static final java.util.List<HTML5GLSurface> ACTIVE =
            java.util.Collections.synchronizedList(new java.util.ArrayList<HTML5GLSurface>());

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
        // Mark the canvas so the host-side screenshot capture composites it onto
        // the output canvas (3D peers are DOM overlays, otherwise missed).
        canvas.setAttribute("data-cn1gl3d", "1");
        // Obtain the WebGL context via the canvas INTERFACE method so the JSO
        // bridge dispatches it against the real DOM canvas on the main thread.
        // (A canvas passed into a @JSBody arrives as an opaque worker-side bridge
        // proxy with no getContext.) The returned context is likewise a bridge
        // proxy; we drive it through the WebGLRenderingContext interface so every
        // call is proxied to the real main-thread context. preserveDrawingBuffer
        // keeps the drawn frame readable for the screenshot composite path.
        JSObject opts = webglContextOptions();
        JSObject ctx = canvas.getContext("webgl", opts);
        if (ctx == null) {
            ctx = canvas.getContext("experimental-webgl", opts);
        }
        if (ctx == null) {
            return null;
        }
        HTML5GLSurface surface = new HTML5GLSurface(canvas, view);
        surface.device = new HTML5GraphicsDevice((WebGLRenderingContext) ctx);
        return surface;
    }

    @JSBody(params = {}, script = "return { preserveDrawingBuffer: true, antialias: true };")
    private static native JSObject webglContextOptions();

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
        // Render synchronously rather than via requestAnimationFrame: the app runs
        // in a Web Worker and a worker-side callback cannot be handed to the
        // main-thread rAF ("parameter 1 is not of type 'Function'"). WebGL calls
        // are synchronous bridge round-trips to the main thread, so an on-demand
        // frame can simply run inline here.
        renderFrame();
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
        if (!ACTIVE.contains(this)) {
            ACTIVE.add(this);
        }
        if (!initialized) {
            renderFrame();
        }
        if (continuous) {
            scheduleFrame();
        }
    }

    /// Composites the current frame of every live WebGL peer onto the supplied
    /// 2D canvas context (the Codename One output canvas), at each peer's
    /// absolute on-screen position. Called by the screenshot path so 3D content
    /// is captured. Each peer is re-rendered first; the contexts are created with
    /// `preserveDrawingBuffer` so the drawn frame survives the `drawImage` read.
    static void compositeInto(JSObject context2d) {
        java.util.List<HTML5GLSurface> peers;
        synchronized (ACTIVE) {
            peers = new java.util.ArrayList<HTML5GLSurface>(ACTIVE);
        }
        for (HTML5GLSurface s : peers) {
            try {
                if (s.contextLost || s.device == null) {
                    continue;
                }
                s.renderFrame();
                drawCanvasInto(context2d, s.canvas,
                        scaleCoord(s.getAbsoluteX()), scaleCoord(s.getAbsoluteY()));
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    @Override
    protected void onPositionSizeChange() {
        super.onPositionSizeChange();
        // Do not gate on `initialized`: the first successful frame can only run
        // once the peer has a non-zero layout size, which typically arrives via
        // this callback. Gating on initialized deadlocked (renderFrame bails at
        // 0 size so initialized stays false, so this never synced the real size).
        if (!contextLost) {
            syncSize();
            requestRender();
        }
    }

    @Override
    protected void deinitialize() {
        cancelFrame();
        ACTIVE.remove(this);
        super.deinitialize();
    }

    void disposeSurface() {
        cancelFrame();
        ACTIVE.remove(this);
        if (initialized && !contextLost) {
            try {
                renderer.onDispose(device);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        initialized = false;
    }

    @JSBody(params = {"ctx", "canvas", "x", "y"},
            script = "try { ctx.drawImage(canvas, x, y); } catch (e) {}")
    private static native void drawCanvasInto(JSObject ctx, HTMLCanvasElement canvas, int x, int y);
}
