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
public class DrawPolygon implements ExecutableOp {

    final int[] xPoints, yPoints;
    final int nPoints, color, alpha;
    public DrawPolygon(int[] xPoints, int[] yPoints, int nPoints, int color, int alpha){
        this.xPoints=xPoints;
        this.yPoints=yPoints;
        this.nPoints=nPoints;
        this.color=color;
        this.alpha=alpha;
    }
    
    @Override
    public void execute(CanvasRenderingContext2D context) {
        if (nPoints <= 1) {
            return;
        }
        context.save();
        context.setStrokeStyle(HTML5Graphics.color(color));
        context.setGlobalAlpha(((double)alpha)/255.0);
        
        context.beginPath();
        for ( int i=0; i<nPoints; i++){
            context.lineTo(xPoints[i], yPoints[i]);
        }
        context.stroke();
        context.restore();
    }

    @Override
    public String getDescription() {
        return "DrawPolygon";
    }
    
}
