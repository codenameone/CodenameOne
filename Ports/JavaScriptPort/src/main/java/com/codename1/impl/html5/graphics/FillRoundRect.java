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
public class FillRoundRect implements ExecutableOp {

    final int x, y, width, height, color, alpha;
    final double arcWidth, arcHeight;
    public FillRoundRect(int x, int y, int width,
            int height, int arcWidth, int arcHeight, int color, int alpha){
        this.x=x;
        this.y=y;
        this.width=width;
        this.height=height;
        this.arcWidth=Math.max(arcWidth, arcHeight);
        this.arcHeight=this.arcWidth;
        this.color=color;
        this.alpha=alpha;
    }
    
    @Override
    public void execute(CanvasRenderingContext2D context) {
        double r = Math.max(arcWidth, arcHeight)/2;
        context.save();
        
        context.setFillStyle(HTML5Graphics.color(color));
        context.setGlobalAlpha(((double)alpha)/255.0);
        context.beginPath();
        context.moveTo(x+arcWidth/2, y);
        context.lineTo(x+width-arcWidth/2, y);
        context.arcTo(x+width, y, x+width, y+arcHeight/2, r);
        context.lineTo(x+width, y+height-arcHeight/2);
        context.arcTo(x+width, y+height, x+width-arcWidth/2, y+height, r);
        context.lineTo(x+arcWidth/2, y+height);
        context.arcTo(x, y+height, x, y+height-arcHeight/2, r);
        context.lineTo(x, y+arcHeight/2);
        context.arcTo(x, y, x+arcWidth/2, y, r);
        context.closePath();
        context.fill();
        context.restore();
    }

    @Override
    public String getDescription() {
        return "FillRoundRect";
    }
    
    
}
