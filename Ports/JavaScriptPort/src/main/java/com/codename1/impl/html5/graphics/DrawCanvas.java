/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5.graphics;

import com.codename1.html5.js.canvas.CanvasRenderingContext2D;
import com.codename1.html5.js.dom.HTMLCanvasElement;

/**
 * Buffered draw op that blits a raw {@code HTMLCanvasElement} (e.g. an offscreen
 * WebGL render target) onto the surface. Unlike {@link DrawImage}, the source is
 * a live canvas rather than a {@code NativeImage}; the canvas host-ref is
 * marshalled through the surface flush like any other drawImage object.
 *
 * Used by the GPU (WebGL) compositing path: a {@code RenderView}'s offscreen
 * canvas is blitted onto the display surface during flushGraphics so the 3D
 * scene flows through the normal paint pipeline instead of a DOM overlay.
 */
public class DrawCanvas implements ExecutableOp {
    final HTMLCanvasElement canvas;
    final int x, y, w, h, alpha;

    public DrawCanvas(HTMLCanvasElement canvas, int x, int y, int w, int h, int alpha) {
        this.canvas = canvas;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.alpha = alpha;
    }

    @Override
    public void execute(CanvasRenderingContext2D context) {
        if (canvas == null || w <= 0 || h <= 0) {
            return;
        }
        context.save();
        context.setGlobalAlpha(((double) alpha) / 255.0);
        context.drawImage(canvas, x, y, w, h);
        context.restore();
    }

    @Override
    public String getDescription() {
        return "DrawCanvas";
    }
}
