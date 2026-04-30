/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
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
