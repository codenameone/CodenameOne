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
import com.codename1.ui.geom.PathIterator;
import com.codename1.ui.geom.Shape;
import com.codename1.html5.js.canvas.CanvasRenderingContext2D;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSObject;

/**
 *
 * @author shannah
 */
public class FillShape implements ExecutableOp {

    final Shape shape;
    final int color, alpha;
    
    public FillShape(Shape shape, int color, int alpha){
        this.shape = new GeneralPath();
        ((GeneralPath)this.shape).setShape(shape, null);
        this.color = color;
        this.alpha = alpha;
    }
    
    @JSBody(params={"o"}, script="console.log(o)")
    private static native void log(JSObject o);
    
    
    
    //@Sync
    public static void execute(CanvasRenderingContext2D context, Shape shape, int color, int alpha){
        
        context.save();
        context.setFillStyle(HTML5Graphics.color(color));
        context.setGlobalAlpha(((double)alpha)/255.0);
        context.beginPath();
        DrawShape.addShapeToPath(context, shape);
        context.fill();
        context.restore();
    }
    
    //@Sync
    @Override
    public void execute(CanvasRenderingContext2D context) {
        execute(context, shape, color, alpha);
    }

    @Override
    public String getDescription() {
        return "FillShape";
    }  
}