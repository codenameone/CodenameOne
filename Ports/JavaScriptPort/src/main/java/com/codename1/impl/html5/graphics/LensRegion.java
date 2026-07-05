/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5.graphics;

import com.codename1.html5.js.canvas.CanvasRenderingContext2D;

/**
 * In-place iOS 26 selection-drop lens for the Tabs glass indicator. Records
 * {@link SurfaceCommandRecorder#OP_LENS_SELF_REGION}: the host clips to the
 * region, magnifies the surface's own pixels there (the content "bulge"),
 * then optionally washes them toward the accent tint.
 *
 * <p>The native iOS port renders this with a Metal fragment shader that also
 * applies chromatic aberration; the web keeps the magnify + tint, which is
 * the readable part of the drop. Nothing crosses the worker&lt;-&gt;host
 * barrier - the magnify uses a self-referential {@code drawImage} during
 * host-side command replay.</p>
 */
public class LensRegion implements ExecutableOp {
    final int x, y, w, h;
    final float cornerRadius, magnify, tintStrength;
    final int tintColor;

    public LensRegion(int x, int y, int w, int h, float cornerRadius, float magnify,
                      int tintColor, float tintStrength) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.cornerRadius = cornerRadius;
        this.magnify = magnify;
        this.tintColor = tintColor;
        this.tintStrength = tintStrength;
    }

    @Override
    public void execute(CanvasRenderingContext2D context) {
        if (!(context instanceof SurfaceCommandRecorder)) {
            return;
        }
        String tintCss = null;
        if (tintStrength > 0) {
            int r = (tintColor >> 16) & 0xff;
            int g = (tintColor >> 8) & 0xff;
            int b = tintColor & 0xff;
            // The model's tintStrength assumes the native lens's luminance
            // KEYING (only dark glyph pixels drift to accent; the bright frost
            // is untouched). On canvas we can't key without a forbidden pixel
            // read, so a flat fill at the full strength paints a solid accent
            // blob over the whole drop and buries the glyph. Scale it right
            // down so the fill reads as a translucent blue CAST over the still-
            // visible magnified content instead of covering it.
            double a = Math.min(0.30, Math.max(0.0, tintStrength) * 0.32);
            tintCss = "rgba(" + r + "," + g + "," + b + "," + a + ")";
        }
        // magnify < ~1 would shrink; clamp to at least 1 so the drop only ever
        // bulges (the model can pass values slightly under 1 at rest).
        double mag = Math.max(1.0, magnify);
        ((SurfaceCommandRecorder) context).lensSelfRegion(x, y, w, h, cornerRadius, mag, tintCss);
    }

    @Override
    public String getDescription() {
        return "LensRegion";
    }
}
