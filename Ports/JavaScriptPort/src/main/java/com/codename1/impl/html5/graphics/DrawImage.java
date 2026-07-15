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


import com.codename1.impl.html5.HTML5Implementation.NativeImage;
import com.codename1.html5.js.canvas.CanvasRenderingContext2D;


/**
 *
 * @author shannah
 */
public class DrawImage implements ExecutableOp {
    final NativeImage img;
    final int x, y, w, h, alpha;

    public DrawImage(NativeImage img, int x, int y){
        this(img, x, y, 255);
    }

    public DrawImage(NativeImage img, int x, int y, int alpha){
        this(img, x, y, alpha, -1, -1);
    }

    public DrawImage(NativeImage img, int x, int y, int alpha, int w, int h){
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.alpha = alpha;
        this.img = img;
    }
    @Override
    public void execute(CanvasRenderingContext2D context) {
        if (img == null) return;
        context.save();
        context.setGlobalAlpha(((double)alpha)/255.0);
        if (w==-1 || h==-1){
            img.draw(context, x, y);
        } else {
            img.draw(context, x, y, w, h);
        }
        context.restore();
    }

    @Override
    public String getDescription() {
        return "DrawImage";
    }
    
}
