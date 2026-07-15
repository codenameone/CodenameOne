/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
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
        if (clipState.isSet()){
            context.restore();
        }
        if (transform != null) {
            // Apply via the CanvasRenderingContext2D interface, not the canvas-only
            // @JSBody Factory.setTransform -- in the surface-id model ``context`` is a
            // SurfaceCommandRecorder, and the raw-JS @JSBody call throws
            // (undefined.apply), aborting the drain and freezing clip-with-transform
            // tests. See SetTransform.execute for the full root-cause note.
            context.setTransform(transform.getScaleX(), transform.getShearY(),
                    transform.getShearX(), transform.getScaleY(),
                    transform.getTranslateX(), transform.getTranslateY());
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
