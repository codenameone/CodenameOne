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
public class DrawLine implements ExecutableOp{

    final int x1, y1, x2, y2, color, alpha;
    
    public DrawLine(int x1, int y1, int x2, int y2, int color, int alpha){
        this.x1=x1;
        this.x2=x2;
        this.y1=y1;
        this.y2=y2;
        this.color=color;
        this.alpha=alpha;
    }
    
    
    
    @Override
    public void execute(CanvasRenderingContext2D context) {
        context.save();
        context.setLineWidth(1);
        context.setStrokeStyle(HTML5Graphics.color(color));
        context.setGlobalAlpha(((double)alpha)/255.0);
        context.beginPath();
        context.moveTo(x1, y1);
        context.lineTo(x2, y2);
        
        context.stroke();
        context.restore();
    }

    @Override
    public String getDescription() {
        return "DrawLine";
    }
    
}
