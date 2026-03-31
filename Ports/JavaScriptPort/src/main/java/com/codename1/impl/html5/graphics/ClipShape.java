/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5.graphics;

import com.codename1.impl.html5.BufferedGraphics;
import com.codename1.teavm.geom.JSAffineTransform;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.PathIterator;
import com.codename1.ui.geom.Shape;
import org.teavm.jso.canvas.CanvasRenderingContext2D;

/**
 *
 * @author shannah
 */
public class ClipShape implements ExecutableOp {
    
    final Shape shape;
    final JSAffineTransform transform;
    final ClipState clipState;
    
    public ClipShape(Shape shape, JSAffineTransform transform, ClipState clipState){
        this.shape = new GeneralPath(shape);
        this.transform = transform == null ? null : transform.cloneTransform();
        this.clipState = clipState;
       
    }
    
    public static void resetClip(CanvasRenderingContext2D context, ClipState clipState){
        if (clipState.isSet()){
            context.restore();
            clipState.set(false);
        }
    }

    @Override
    public void execute(CanvasRenderingContext2D context) {
        if (clipState.isSet()){
            context.restore();
        }
        if (transform != null) {
            JSAffineTransform.Factory.setTransform(context, transform);
        }
        clipState.set(true);
        context.save();
        context.beginPath();
        addShapeToPath(context, shape);
        context.clip();
    }

    @Override
    public String getDescription() {
        return "ClipRect";
    }
    
    
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
}
