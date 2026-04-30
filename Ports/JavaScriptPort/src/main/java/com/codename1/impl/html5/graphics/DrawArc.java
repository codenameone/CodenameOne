/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5.graphics;

import com.codename1.impl.html5.HTML5Graphics;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.canvas.CanvasRenderingContext2D;

/**
 *
 * @author shannah
 */
public class DrawArc implements ExecutableOp {

    final int x, y, w, h, startAngle, arcAngle, color, alpha;
    
    public DrawArc(int x, int y, int w, int h, int startAngle, int arcAngle, int color, int alpha){
        this.x=x;
        this.y=y;
        this.w=w;
        this.h=h;
        this.startAngle=startAngle;
        this.arcAngle=arcAngle;
        this.color = color;
        this.alpha = alpha;
    }
    
    @JSBody(params={"o"}, script="console.log(o)")
    private static native void log(JSObject o);
    
    //@Sync
    public static void execute(CanvasRenderingContext2D context, int x, int y, int w, int h, int startAngle, int arcAngle, int color, int alpha){
        double startRad = startAngle * Math.PI/180.0;
        double arcRad = arcAngle * Math.PI/180.0;
        double rx = w/2;
        double ry = h/2;
        
        double cx = x+rx;
        double cy = y+ry;
        
        
        double endRad = startRad + arcRad;

        if (w == h) {
            context.save();
            context.setStrokeStyle(HTML5Graphics.color(color));
            context.setGlobalAlpha(((double)alpha)/255.0);
            context.beginPath();
            context.arc(cx, cy, rx, -startRad, -endRad, true);
            context.setLineWidth(1);
            context.stroke();
            context.restore();
        } else {
            GeneralPath p = new GeneralPath();
            p.arc(x, y, w, h, startRad, arcRad);
            
            DrawShape.execute(context, p, new Stroke(), color, alpha);
        }
        
        
    }
    
    //@Sync
    @Override
    public void execute(CanvasRenderingContext2D context) {
        execute(context, x, y, w, h, startAngle, arcAngle, color, alpha);
    }

    @Override
    public String getDescription() {
        return "DrawArc";
    }  
}