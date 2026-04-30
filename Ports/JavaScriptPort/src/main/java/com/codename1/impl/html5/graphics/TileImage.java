/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
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
