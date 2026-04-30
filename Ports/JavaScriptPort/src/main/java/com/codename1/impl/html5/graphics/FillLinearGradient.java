/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5.graphics;

import com.codename1.impl.html5.HTML5Graphics;
import com.codename1.html5.js.canvas.CanvasGradient;
import com.codename1.html5.js.canvas.CanvasRenderingContext2D;

/**
 *
 * @author shannah
 */
public class FillLinearGradient implements ExecutableOp {
    final int x, y, w, h, startColor, endColor, alpha;
    final boolean horizontal;
    
    public FillLinearGradient(int x, int y, int w, int h, int startColor, int endColor, boolean horizontal, int alpha){
        this.x=x;
        this.y=y;
        this.w=w;
        this.h=h;
        this.startColor = startColor;
        this.endColor = endColor;
        this.horizontal = horizontal;
        this.alpha=alpha;
    }
    
    @Override
    public void execute(CanvasRenderingContext2D context) {
        execute(context, x, y, w, h, startColor, endColor, horizontal, alpha);
    }
    
    //@Sync
    public static void execute(CanvasRenderingContext2D context, int x, int y, int w, int h, int startColor, int endColor, boolean horizontal, int alpha) {
        
        CanvasGradient grad = horizontal ? 
                context.createLinearGradient(x, y + h/2, x + w, y + h/2) :
                context.createLinearGradient(x + w/2, y, x + w/2, y + h) ;
        int startAlpha = (startColor >> 24) & 0xFF;
        int endAlpha = (endColor >> 24) & 0xFF;
        
        String startColorStr = startAlpha > 0 ? HTML5Graphics.colorWithAlpha(startColor) : HTML5Graphics.color(startColor);
        String endColorStr = endAlpha > 0 ? HTML5Graphics.colorWithAlpha(endColor) : HTML5Graphics.color(endColor);
        
        grad.addColorStop(0, startColorStr);
        grad.addColorStop(1, endColorStr);
        
        context.save();
        context.setFillStyle(grad);
        context.setGlobalAlpha(((double)alpha)/255.0);
        context.beginPath();
        context.rect(x, y, w, h);
        context.fill();
        context.restore();
    }

    @Override
    public String getDescription() {
        return "FillLinearGradient";
    }
    
}
