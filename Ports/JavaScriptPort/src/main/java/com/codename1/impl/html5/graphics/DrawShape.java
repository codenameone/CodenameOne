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
import org.teavm.interop.Sync;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.canvas.CanvasRenderingContext2D;

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
        PathIterator it = shape.getPathIterator();
        float[] points = new float[6];
        //it.next();
        while (!it.isDone()) {
            int type = it.currentSegment(points);
            switch (type) {
                case PathIterator.SEG_MOVETO:
                    context.moveTo(points[0], points[1]);
                    break;
                case PathIterator.SEG_CLOSE:
                    context.closePath();
                    break;
                case PathIterator.SEG_LINETO:
                    context.lineTo(points[0], points[1]);
                    break;
                case PathIterator.SEG_QUADTO:
                    context.quadraticCurveTo(points[0], points[1], points[2], points[3]);
                    break;
                case PathIterator.SEG_CUBICTO:
                    context.bezierCurveTo(points[0], points[1], points[2], points[3], points[4], points[5]);
                    break;
                
            }
            it.next();
            
        }
    }
    
    //@Sync
    public static void execute(CanvasRenderingContext2D context, Shape shape, Stroke stroke, int color, int alpha){
        
        context.save();
        context.setStrokeStyle(HTML5Graphics.color(color));
        context.setGlobalAlpha(((double)alpha)/255.0);
        
        context.setLineWidth(stroke.getLineWidth());
        String joinStyle="miter";
        switch (stroke.getJoinStyle()) {
            case Stroke.JOIN_BEVEL:
                joinStyle = "bevel";
                break;
            case Stroke.JOIN_MITER:
                joinStyle = "miter";
                break;
            case Stroke.JOIN_ROUND:
                joinStyle = "round";
                break;
        }
        context.setLineJoin(joinStyle);
        
        context.setMiterLimit(stroke.getMiterLimit());
        
        String capStyle = "butt";
        switch (stroke.getCapStyle()) {
            case Stroke.CAP_BUTT:
                capStyle = "butt";
                break;
            case Stroke.CAP_ROUND:
                capStyle = "round";
                break;
            case Stroke.CAP_SQUARE:
                capStyle = "square";
                break;
        }
        context.setLineCap(capStyle);
        
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