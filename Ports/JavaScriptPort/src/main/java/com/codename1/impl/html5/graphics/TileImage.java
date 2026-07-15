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

import com.codename1.impl.html5.HTML5Implementation;
import com.codename1.impl.html5.HTML5Implementation.NativeImage;
import com.codename1.html5.js.canvas.CanvasRenderingContext2D;

/**
 *
 * @author shannah
 */
public class TileImage implements ExecutableOp {

    final NativeImage img;
    final int x, y, w, h, alpha;
    
    public TileImage(NativeImage img, int x, int y, int w, int h, int alpha){
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.alpha = alpha;
        this.img = img;
    }
    @Override
    public void execute(CanvasRenderingContext2D context) {
        context.save();
        context.setGlobalAlpha(((double)alpha)/255.0);
        img.tile(context, x, y, w, h);
        context.restore();
        
    }

    @Override
    public String getDescription() {
        return "TileImage";
    }
    
}
