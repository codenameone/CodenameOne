/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5.graphics;

import com.codename1.impl.html5.BufferedGraphics;
import com.codename1.teavm.geom.JSAffineTransform;
import com.codename1.html5.js.canvas.CanvasRenderingContext2D;

/**
 *
 * @author shannah
 */
public class ClipRect implements ExecutableOp {
    
    final int x, y, w, h;
    final ClipState clipState;
    
    public ClipRect(int x, int y, int w, int h, ClipState clipState){
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
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
        clipState.set(true);
        context.save();
        context.beginPath();
        
        context.rect(x, y, w, h);
        context.clip();
    }

    @Override
    public String getDescription() {
        return "ClipRect";
    }
    
}
