/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5.graphics;

import com.codename1.impl.html5.BufferedGraphics;
import com.codename1.impl.html5.JavaScriptShapePathAdapter;
import com.codename1.teavm.geom.JSAffineTransform;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Shape;
import com.codename1.html5.js.canvas.CanvasRenderingContext2D;

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
        // See ClipRect.execute for the save/restore-leaks-transform problem.
        // The shape coords are in ``transform``-space, so the work order is:
        //   1. capture the live canvas transform (assumed to match the
        //      Java-side current transform)
        //   2. restore() pops the prior clip op's saved state
        //   3. setTransform(transform) for the shape's coord space
        //   4. save() records [transform, no-clip] -- this is what the *next*
        //      clip op will pop to; that's fine because the next clip op will
        //      capture its own ``preserved`` and overwrite the transform
        //      anyway
        //   5. addShape + clip(): adds the clip in device space
        //   6. setTransform(preserved) so subsequent draw ops see the
        //      transform the Java side currently has set
        JSAffineTransform preserved = null;
        if (clipState.isSet()){
            preserved = JSAffineTransform.Factory.capture(context);
            context.restore();
        }
        if (transform != null) {
            JSAffineTransform.Factory.setTransform(context, transform);
        } else if (preserved != null) {
            JSAffineTransform.Factory.setTransform(context, preserved);
        }
        clipState.set(true);
        context.save();
        context.beginPath();
        addShapeToPath(context, shape);
        context.clip();
        if (preserved != null) {
            JSAffineTransform.Factory.setTransform(context, preserved);
        }
    }

    @Override
    public String getDescription() {
        return "ClipRect";
    }
    
    
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
}
