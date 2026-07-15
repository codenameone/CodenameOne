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