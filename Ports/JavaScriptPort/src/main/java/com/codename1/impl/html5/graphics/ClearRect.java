/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5.graphics;

import com.codename1.impl.html5.HTML5Graphics;
import com.codename1.html5.js.canvas.CanvasRenderingContext2D;

/**
 *
 * @author shannah
 */
public class ClearRect implements ExecutableOp {
    final int x, y, w, h;
    
    public ClearRect(int x, int y, int w, int h){
        this.x=x;
        this.y=y;
        this.w=w;
        this.h=h;
    }
    
    @Override
    public void execute(CanvasRenderingContext2D context) {
        context.clearRect(x, y, w, h);
    }

    @Override
    public String getDescription() {
        return "ClearRect";
    }
    
}
