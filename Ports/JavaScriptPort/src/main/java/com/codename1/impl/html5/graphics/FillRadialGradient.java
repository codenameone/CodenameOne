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
public class FillRadialGradient implements ExecutableOp {
    final int x, y, w, h, startColor, endColor, alpha, startAngle, arcAngle;
    
    public FillRadialGradient(int x, int y, int w, int h, int startColor, int endColor, int alpha, int startAngle, int arcAngle){
        this.x=x;
        this.y=y;
        this.w=w;
        this.h=h;
        this.startColor = startColor;
        this.endColor = endColor;
        this.alpha=alpha;
        this.startAngle=startAngle;
        this.arcAngle=arcAngle;
    }
    
    @Override
    public void execute(CanvasRenderingContext2D context) {
        execute(context, x, y, w, h, startColor, endColor, alpha, startAngle, arcAngle);
    }
    
    //@Sync
    public static void execute(CanvasRenderingContext2D context, int x, int y, int w, int h, int startColor, int endColor, int alpha, int startAngle, int arcAngle) {
        context.save();
        context.beginPath();
        
        double rx = w/2.0;
        double ry = h/2.0;
        double cx = x + rx;
        double cy = y + ry;
        
        double startRad = startAngle * Math.PI / 180.0;
        double endRad = (startAngle + arcAngle) * Math.PI / 180.0;
        
        context.translate(cx-rx, cy-ry);
        context.scale(rx, ry);
        context.moveTo(1, 1);
        context.lineTo(1 + Math.cos(-startRad), 1 + Math.sin(-startRad));
        context.arc(1, 1, 1, -startRad, -endRad, false);
        
        context.closePath();
        
        CanvasGradient grad = context.createRadialGradient(1, 1, 1, 1, 1, 0);
        
        grad.addColorStop(0, HTML5Graphics.color(endColor));
        grad.addColorStop(1, HTML5Graphics.color(startColor));
        context.setFillStyle(grad);
        //context.setGlobalAlpha(((double)alpha)/255.0);
        context.fill();
        context.restore();
        
    }

    @Override
    public String getDescription() {
        return "FillRadialGradient";
    }
    
}
