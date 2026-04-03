/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5.graphics;

import com.codename1.impl.html5.HTML5Graphics;
import com.codename1.teavm.geom.JSAffineTransform;
import com.codename1.html5.js.canvas.CanvasRenderingContext2D;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSObject;

/**
 *
 * @author shannah
 */
public class FillArc implements ExecutableOp {

    final int x, y, w, h, startAngle, arcAngle, color, alpha;
    
    public FillArc(int x, int y, int w, int h, int startAngle, int arcAngle, int color, int alpha){
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
        
        context.save();
        //JSAffineTransform.Factory.setTransform(context, JSAffineTransform.Factory.getTranslateInstance(0, 0));
        context.setFillStyle(HTML5Graphics.color(color));
        context.setGlobalAlpha(((double)alpha)/255.0);
        
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
        
        
        context.fill();
        context.restore();
        
    }
    
    //@Sync
    @Override
    public void execute(CanvasRenderingContext2D context) {
        execute(context, x, y, w, h, startAngle, arcAngle, color, alpha);
    }

    @Override
    public String getDescription() {
        return "FillArc";
    }
    
    
    
}
