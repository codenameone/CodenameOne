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
