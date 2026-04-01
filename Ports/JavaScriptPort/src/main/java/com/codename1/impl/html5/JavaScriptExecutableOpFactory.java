/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

import com.codename1.impl.html5.HTML5Implementation.NativeFont;
import com.codename1.impl.html5.graphics.ClearRect;
import com.codename1.impl.html5.graphics.ClipRect;
import com.codename1.impl.html5.graphics.ClipState;
import com.codename1.impl.html5.graphics.DrawLine;
import com.codename1.impl.html5.graphics.DrawRect;
import com.codename1.impl.html5.graphics.DrawString;
import com.codename1.impl.html5.graphics.ExecutableOp;
import com.codename1.impl.html5.graphics.FillRect;

public final class JavaScriptExecutableOpFactory implements JavaScriptPrimitiveRenderAdapter.PrimitiveOpFactory<NativeFont, ExecutableOp> {
    public static final JavaScriptExecutableOpFactory INSTANCE = new JavaScriptExecutableOpFactory();

    private JavaScriptExecutableOpFactory() {
    }

    @Override
    public ExecutableOp createFillRect(int x, int y, int width, int height, int color, int alpha) {
        return new FillRect(x, y, width, height, color, alpha);
    }

    @Override
    public ExecutableOp createClearRect(int x, int y, int width, int height) {
        return new ClearRect(x, y, width, height);
    }

    @Override
    public ExecutableOp createDrawRect(int x, int y, int width, int height, int color, int alpha) {
        return new DrawRect(x, y, width, height, color, alpha);
    }

    @Override
    public ExecutableOp createDrawLine(int x1, int y1, int x2, int y2, int color, int alpha) {
        return new DrawLine(x1, y1, x2, y2, color, alpha);
    }

    @Override
    public ExecutableOp createDrawString(String str, int x, int y, int color, int alpha, NativeFont font) {
        return new DrawString(str, x, y, color, alpha, font);
    }

    @Override
    public ExecutableOp createClipRect(int x, int y, int width, int height, ClipState clipState) {
        return new ClipRect(x, y, width, height, clipState);
    }
}
