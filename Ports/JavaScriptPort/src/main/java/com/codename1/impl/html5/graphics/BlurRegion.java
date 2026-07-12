/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5.graphics;

import com.codename1.html5.js.canvas.CanvasRenderingContext2D;

/**
 * In-place backdrop blur for CSS {@code backdrop-filter: blur()} styles.
 * Records {@link SurfaceCommandRecorder#OP_BLUR_SELF_REGION} - the host
 * clips its canvas to the region (rect / rounded / capsule) and redraws it
 * through a canvas {@code filter: blur(...)}, which is exactly the
 * browser's own backdrop-filter sampling model: pixels just outside the
 * clip bleed into the blurred edge the way a real frosted surface reads.
 *
 * <p>Everything happens host-side during command replay; nothing crosses
 * the worker&lt;-&gt;host barrier here (a first cut used
 * {@code context.getCanvas()}, which the recorder answers with null - the
 * barrier-read lint caught it and the drawImage silently no-oped).</p>
 *
 * <p>Without this op the modern iOS theme's glass surfaces (the floating
 * tab pill, toolbar chrome, glass buttons) rendered as their bare
 * translucent tint - the DARK tab pill was near-invisible on the JS
 * port.</p>
 *
 * <p>Cost: one full-canvas filtered drawImage per glass region per frame.
 * Fine for themed bars/buttons; a screenful of glass would want the same
 * patch-caching the iOS port applies. The blur samples in the host
 * context's CURRENT transform - glass inside a rotated/scaled layer would
 * sample incorrectly, matching the other ports' v1 limitation.</p>
 */
public class BlurRegion implements ExecutableOp {
    final int x, y, w, h;
    final float radius, cornerRadius;

    public BlurRegion(int x, int y, int w, int h, float radius, float cornerRadius) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.radius = radius;
        this.cornerRadius = cornerRadius;
    }

    @Override
    public void execute(CanvasRenderingContext2D context) {
        if (!(context instanceof SurfaceCommandRecorder)) {
            // Every JS-port graphics path records into a SurfaceCommandRecorder
            // (display and mutable images alike); a live host context proxy
            // would need a barrier-read of its canvas, which is forbidden.
            return;
        }
        // CSS blur(px) takes a gaussian sigma; the framework passes a blur
        // RADIUS (matching the iOS CIGaussianBlur calibration), so halve it.
        double sigma = Math.max(1.0, radius / 2.0);
        ((SurfaceCommandRecorder) context).blurSelfRegion(x, y, w, h, sigma, cornerRadius);
    }

    @Override
    public String getDescription() {
        return "BlurRegion";
    }
}
