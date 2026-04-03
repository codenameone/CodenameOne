/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5.graphics;

import com.codename1.impl.html5.HTML5Graphics;
import com.codename1.impl.html5.JavaScriptShapePathAdapter;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Shape;

import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.canvas.CanvasRenderingContext2D;

/**
 *
 * @author shannah
 */
public class DrawShape implements ExecutableOp {

    final Shape shape;
    final Stroke stroke;
    final int color, alpha;
    
    public DrawShape(Shape shape, Stroke stroke, int color, int alpha){
        this.shape = new GeneralPath();
        ((GeneralPath)this.shape).setShape(shape, null);
        this.stroke = new Stroke();
        this.stroke.setStroke(stroke);
        this.color = color;
        this.alpha = alpha;
    }
    
    @JSBody(params={"o"}, script="console.log(o)")
    private static native void log(JSObject o);
    
    static void addShapeToPath(CanvasRenderingContext2D context, Shape shape) {
        JavaScriptShapePathAdapter.addShapeToPath(new JavaScriptShapePathAdapter.PathSink() {
            @Override
            public void moveTo(float x, float y) {
                context.moveTo(x, y);
            }

            @Override
            public void closePath() {
                context.closePath();
            }

            @Override
            public void lineTo(float x, float y) {
                context.lineTo(x, y);
            }

            @Override
            public void quadraticCurveTo(float cpx, float cpy, float x, float y) {
                context.quadraticCurveTo(cpx, cpy, x, y);
            }

            @Override
            public void bezierCurveTo(float cp1x, float cp1y, float cp2x, float cp2y, float x, float y) {
                context.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, x, y);
            }
        }, shape);
    }
    
    //@Sync
    public static void execute(CanvasRenderingContext2D context, Shape shape, Stroke stroke, int color, int alpha){
        
        context.save();
        context.setStrokeStyle(HTML5Graphics.color(color));
        context.setGlobalAlpha(((double)alpha)/255.0);
        
        JavaScriptShapePathAdapter.applyStrokeStyle(new JavaScriptShapePathAdapter.StrokeStyleSink() {
            @Override
            public void setLineWidth(float width) {
                context.setLineWidth(width);
            }

            @Override
            public void setLineJoin(String join) {
                context.setLineJoin(join);
            }

            @Override
            public void setMiterLimit(float limit) {
                context.setMiterLimit(limit);
            }

            @Override
            public void setLineCap(String cap) {
                context.setLineCap(cap);
            }
        }, stroke);
        
        context.beginPath();
        addShapeToPath(context, shape);
        context.stroke();
        context.restore();
    }
    
    //@Sync
    @Override
    public void execute(CanvasRenderingContext2D context) {
        execute(context, shape, stroke, color, alpha);
    }

    @Override
    public String getDescription() {
        return "DrawShape";
    }  
}
