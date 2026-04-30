/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5.graphics;

import com.codename1.impl.html5.HTML5Graphics;
import com.codename1.impl.html5.HTML5Implementation;
import com.codename1.impl.html5.HTML5Implementation.NativeFont;
import com.codename1.html5.js.canvas.CanvasRenderingContext2D;

/**
 *
 * @author shannah
 */
public class DrawString implements ExecutableOp {
    final String str;
    final int color, x, y, alpha;
    final NativeFont font;
    
    public DrawString(String str, int x, int y, int color, int alpha, NativeFont font){
        this.str=str;
        this.x=x;
        this.y=y;
        this.color=color;
        this.alpha=alpha;
        if (font == null) {
            font = (NativeFont)HTML5Implementation.getInstance().getDefaultFont();
        }
        this.font=font;
    }
    @Override
    public void execute(CanvasRenderingContext2D context) {
        context.save();
        boolean useBaselineRendering = HTML5Implementation.useBaselineTextRendering();
        if (useBaselineRendering && font.getCSS().contains("Material")) {
            // This is a special case for material design fonts since we often need to
            // rotate them and the baseline font approximation doesn't 
            // work well for that.
            // https://github.com/codenameone/CodenameOne/issues/2631
            useBaselineRendering = false;
        }
        if (!useBaselineRendering) {
            context.setTextBaseline("top");
        } else {
            context.setTextBaseline("alphabetic");
        }
        context.setFillStyle(HTML5Graphics.color(color));
        context.setFont(font.getCSS());
        context.setGlobalAlpha(((double)alpha)/255.0);
        if (!useBaselineRendering) {
            context.fillText(str, x, y);
        } else {
            int ascent = font.fontAscent();
            context.fillText(str, x, y+ascent + font.fontLeading());
        }
        context.restore();
    }

    @Override
    public String getDescription() {
        return "DrawString";
    }
    
    
}
