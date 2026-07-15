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
import com.codename1.html5.js.canvas.CanvasRenderingContext2D;

/**
 *
 * @author shannah
 */
public class DrawRoundRect implements ExecutableOp {
    
    final int x, y, width, height, color, alpha;
    final double arcWidth, arcHeight;
    
    public DrawRoundRect(int x, int y, int width,
            int height, int arcWidth, int arcHeight, int color, int alpha){
        this.x=x;
        this.y=y;
        this.width=width;
        this.height=height;
        this.arcWidth=Math.max(arcWidth, arcHeight);
        this.arcHeight=this.arcWidth;
        this.color=color;
        this.alpha=alpha;
    }
    
    @Override
    public void execute(CanvasRenderingContext2D context) {
        double r = Math.max(arcWidth, arcHeight)/2;
        context.save();
        
        context.setStrokeStyle(HTML5Graphics.color(color));
        context.setGlobalAlpha(((double)alpha)/255.0);
        context.beginPath();
        context.moveTo(x+arcWidth/2, y);
        context.lineTo(x+width-arcWidth/2, y);
        context.arcTo(x+width, y, x+width, y+arcHeight/2, r);
        context.lineTo(x+width, y+height-arcHeight/2);
        context.arcTo(x+width, y+height, x+width-arcWidth/2, y+height, r);
        context.lineTo(x+arcWidth/2, y+height);
        context.arcTo(x, y+height, x, y+height-arcHeight/2, r);
        context.lineTo(x, y+arcHeight/2);
        context.arcTo(x, y, x+arcWidth/2, y, r);
        context.closePath();
        context.stroke();
        context.restore();
    }

    @Override
    public String getDescription() {
        return "DrawRoundRect";
    }

    
    
    
    
}
