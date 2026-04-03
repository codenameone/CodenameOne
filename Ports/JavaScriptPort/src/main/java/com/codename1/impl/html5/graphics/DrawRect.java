/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5.graphics;

import com.codename1.impl.html5.HTML5Graphics;
import com.codename1.html5.js.canvas.CanvasRenderingContext2D;

/**
 *
 * @author shannah
 */
public class DrawRect implements ExecutableOp {

    final int x, y, w, h, color, alpha;
    
    public DrawRect(int x, int y, int w, int h, int color, int alpha){
        this.x=x;
        this.y=y;
        this.w=w;
        this.h=h;
        this.color=color;
        this.alpha=alpha;
    }
    
    @Override
    public void execute(CanvasRenderingContext2D context) {
        context.save();
        context.setStrokeStyle(HTML5Graphics.color(color));
        context.setGlobalAlpha(((double)alpha)/255.0);
        context.beginPath();
        context.rect(x, y, w, h);
        context.setLineWidth(1);
        context.stroke();
        context.restore();
    }

    @Override
    public String getDescription() {
        return "DrawRect";
    }
    
}
