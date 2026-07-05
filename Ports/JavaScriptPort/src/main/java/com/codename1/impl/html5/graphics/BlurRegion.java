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
 * Clips to the region (optionally as a rounded rect / capsule) and redraws
 * the canvas onto itself through a canvas {@code filter: blur(...)}, which
 * is exactly the browser's own backdrop-filter sampling model: pixels just
 * outside the clip bleed into the blurred edge the way a real frosted
 * surface reads.
 *
 * <p>Without this op the modern iOS theme's glass surfaces (the floating
 * tab pill, toolbar chrome, glass buttons) rendered as their bare
 * translucent tint - the DARK tab pill is {@code transparent +
 * backdrop-filter}, so it disappeared entirely on the JS port.</p>
 *
 * <p>Cost: one full-canvas filtered drawImage per glass region per frame.
 * Fine for themed bars/buttons; a screenful of glass would want the same
 * patch-caching the iOS port applies. The blur is drawn in the context's
 * CURRENT transform - glass components inside a rotated/scaled layer would
 * sample incorrectly, which matches the other ports' v1 limitation.</p>
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
        context.save();
        context.beginPath();
        if (cornerRadius != 0) {
            // -1 is the capsule sentinel (fully rounded sides); positive values
            // are an explicit corner radius in px. Either way the radius cannot
            // exceed half the shorter side.
            double r = cornerRadius < 0
                    ? Math.min(w, h) / 2.0
                    : Math.min(cornerRadius, Math.min(w, h) / 2.0);
            context.moveTo(x + r, y);
            context.arcTo(x + w, y, x + w, y + h, r);
            context.arcTo(x + w, y + h, x, y + h, r);
            context.arcTo(x, y + h, x, y, r);
            context.arcTo(x, y, x + w, y, r);
            context.closePath();
        } else {
            context.rect(x, y, w, h);
        }
        context.clip();
        // CSS blur(px) takes a gaussian sigma; the framework passes a blur
        // RADIUS (matching the iOS CIGaussianBlur calibration), so halve it.
        double sigma = Math.max(1.0, radius / 2.0);
        context.setFilter("blur(" + sigma + "px)");
        // drawImage(self) snapshots the canvas bitmap before compositing per
        // the HTML spec, so this is a well-defined in-place backdrop blur.
        context.drawImage(context.getCanvas(), 0, 0);
        context.setFilter("none");
        context.restore();
    }

    @Override
    public String getDescription() {
        return "BlurRegion";
    }
}
