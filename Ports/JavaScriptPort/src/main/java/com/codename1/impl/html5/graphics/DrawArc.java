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