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
            // Stop the animation loop once this surface is no longer part of the
            // shown UI -- e.g. the user switched samples, replacing the preview
            // Form. deinitialize() does not reliably fire for a RenderView nested
            // in the embedded (re-parented) preview Form, so without this the rAF
            // keeps firing and flushing the display forever, degrading the whole
            // playground (and stacking up if several samples were run).
            if (!isLive()) {
                continuous = false;
                return;
            }
            // Repaint; the actual GL render + blit happens in paint() so the 3D
            // surface composites in z-order with the rest of the UI.
            repaint();
            if (continuous) {
                scheduleFrame();
            }
        }
    };

    /// True while this surface is still part of the currently shown UI (its
    /// parent chain reaches the on-screen Form). Once the preview is replaced the
    /// chain no longer reaches Display.getCurrent(), so the animation loop stops.
    private boolean isLive() {
        if (contextLost) {
            return false;
        }
        com.codename1.ui.Form current = com.codename1.ui.Display.getInstance().getCurrent();
        if (current == null) {
            return false;
        }
        com.codename1.ui.Component c = this;
        while (c != null) {
            if (c == current) {
                return true;
            }
            c = c.getParent();
        }
        return false;
    }

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
        // Obtain the WebGL context via the canvas INTERFACE method so the JSO
        // bridge dispatches it against the real DOM canvas on the main thread.
        // (A canvas passed into a @JSBody arrives as an opaque worker-side bridge
        // proxy with no getContext.) The returned context is likewise a bridge
        // proxy; we drive it through the WebGLRenderingContext interface so every
        // call is proxied to the real main-thread context. preserveDrawingBuffer
        // keeps the drawn frame readable for the screenshot composite path.
        JSObject opts = webglContextOptions();
        // Obtaining the WebGL context is the one unavoidable getContext round-trip;
        // it runs once at creation, not per frame.
        JSObject ctx = canvas.getContext("webgl", opts); // LINT-ALLOW-CANVAS-BARRIER-READ: one-time WebGL context creation
        if (ctx == null) {
            ctx = canvas.getContext("experimental-webgl", opts); // LINT-ALLOW-CANVAS-BARRIER-READ: one-time legacy context creation
        }
        if (ctx == null) {
            // Some browsers (notably Firefox configurations where the WebGL 1
            // path is blocklisted) expose only WebGL 2. A WebGL2RenderingContext
            // is a superset of WebGLRenderingContext, so every call this device
            // makes still resolves; try it before giving up.
            ctx = canvas.getContext("webgl2", opts); // LINT-ALLOW-CANVAS-BARRIER-READ: one-time WebGL2 context creation
        }
        if (ctx == null) {
            return null;
        }
        HTML5GLSurface surface = new HTML5GLSurface(canvas, view);
        surface.device = new HTML5GraphicsDevice((WebGLRenderingContext) ctx);
        // The WebGL canvas is NOT shown as a DOM peer overlay. Native peers sit
        // BEHIND the output canvas (z-index -1000) and show through a transparent
        // "hole" punched in it -- but a negative-z WebGL canvas is composited as
        // its own GPU layer and many real GPUs push that layer behind the page,
        // so the hole shows the page background: a blank preview (only software
        // GL / some GPUs composited it, which is why screenshots looked fine).
        // The hole also covered the device skin, and the peer DOM position is
        // wrong for a RenderView inside a hosted form. Instead we keep this canvas
        // OFFSCREEN as a pure render target and blit its pixels into the display
        // op stream during paint() (see paint()), so the 3D scene composites in
        // z-order like any other component -- correct position, correct layering,
        // and no fragile WebGL DOM layer.
        // `visibility:hidden` (not `display:none`): the element is not painted,
        // so it never acts as the broken DOM overlay -- but it stays in layout
        // with a live backing store, so WebGL keeps rendering into it and the
        // host can read it back via drawImage during the flushGraphics composite.
        // (`display:none` zeroes the canvas's rendered content, leaving a blank
        // blit.)
        canvas.getStyle().setProperty("visibility", "hidden");
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
        // On-demand: repaint; paint() renders the frame and blits it in z-order.
        repaint();
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
        if (w != lastW || h != lastH) {
            lastW = w;
            lastH = h;
            // The canvas pixel size is the scaled component size, tracked Java-side
            // via lastW/lastH; it is written (fire-and-forget) but never read back
            // off the canvas host-ref (that would be a worker<->host barrier read).
            canvas.setWidth(w);
            canvas.setHeight(h);
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
    protected com.codename1.ui.geom.Dimension calcPreferredSize() {
        // Don't let the native canvas's intrinsic size inflate the hosted form's
        // layout. The RenderView fills its BorderLayout.CENTER slot regardless,
        // and a large preferred size pushed the playground's device bezel out to
        // the full preview width -- so the 3D scene covered the skin. A zero
        // preferred size lets the surrounding bezel keep its real dimensions.
        return new com.codename1.ui.geom.Dimension(0, 0);
    }

    @Override
    public void paint(com.codename1.ui.Graphics g) {
        if (contextLost || device == null) {
            return;
        }
        int w = scaleCoord(getWidth());
        int h = scaleCoord(getHeight());
        if (w <= 0 || h <= 0) {
            return;
        }
        // Render the scene into the offscreen WebGL canvas, then blit it into the
        // display op stream HERE -- in paint order -- so the 3D surface behaves
        // like any other component: Codename One elements painted after it layer
        // on top, the device bezel and neighbouring UI render correctly, and it
        // sits at its real on-screen position (g is translated so our content is
        // at getX(),getY()). The canvas itself is never shown as a DOM overlay.
        renderFrame();
        BufferedGraphics bg = HTML5Implementation.getInstance().displayGraphics();
        if (bg != null) {
            bg.drawCanvas(canvas,
                    scaleCoord(g.getTranslateX() + getX()),
                    scaleCoord(g.getTranslateY() + getY()), w, h);
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
}
