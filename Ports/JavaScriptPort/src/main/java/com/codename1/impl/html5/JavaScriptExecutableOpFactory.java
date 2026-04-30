/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

import com.codename1.impl.html5.HTML5Implementation.NativeFont;
import com.codename1.impl.html5.HTML5Implementation.NativeImage;
import com.codename1.impl.html5.graphics.ClearRect;
import com.codename1.impl.html5.graphics.ClipRect;
import com.codename1.impl.html5.graphics.ClipShape;
import com.codename1.impl.html5.graphics.ClipState;
import com.codename1.impl.html5.graphics.DrawLine;
import com.codename1.impl.html5.graphics.DrawRect;
import com.codename1.impl.html5.graphics.DrawString;
import com.codename1.impl.html5.graphics.DrawImage;
import com.codename1.impl.html5.graphics.ExecutableOp;
import com.codename1.impl.html5.graphics.FillRect;
import com.codename1.impl.html5.graphics.FillLinearGradient;
import com.codename1.impl.html5.graphics.FillRadialGradient;
import com.codename1.impl.html5.graphics.FillRectRadialGradient;
import com.codename1.impl.html5.graphics.FillShape;
import com.codename1.impl.html5.graphics.DrawShape;
import com.codename1.impl.html5.graphics.SetTransform;
import com.codename1.impl.html5.graphics.TileImage;
import com.codename1.teavm.geom.JSAffineTransform;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.Shape;

public final class JavaScriptExecutableOpFactory implements JavaScriptPrimitiveRenderAdapter.PrimitiveOpFactory<NativeFont, ExecutableOp>,
        JavaScriptImageTransformRenderAdapter.ImageTransformOpFactory<NativeImage, Shape, JSAffineTransform, ExecutableOp>,
        JavaScriptShapeGradientRenderAdapter.ShapeGradientOpFactory<Shape, Stroke, ExecutableOp> {
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

    @Override
    public ExecutableOp createDrawImage(NativeImage image, int x, int y, int alpha) {
        return new DrawImage(image, x, y, alpha);
    }

    @Override
    public ExecutableOp createDrawImage(NativeImage image, int x, int y, int width, int height, int alpha) {
        return new DrawImage(image, x, y, alpha, width, height);
    }

    @Override
    public ExecutableOp createTileImage(NativeImage image, int x, int y, int width, int height, int alpha) {
        return new TileImage(image, x, y, width, height, alpha);
    }

    @Override
    public ExecutableOp createTransform(JSAffineTransform transform, boolean replace) {
        return new SetTransform(transform, replace);
    }

    @Override
    public ExecutableOp createClipShape(Shape shape, JSAffineTransform transform, ClipState clipState) {
        return new ClipShape(shape, transform, clipState);
    }

    @Override
    public ExecutableOp createDrawShape(Shape shape, Stroke stroke, int color, int alpha) {
        return new DrawShape(shape, stroke, color, alpha);
    }

    @Override
    public ExecutableOp createFillShape(Shape shape, int color, int alpha) {
        return new FillShape(shape, color, alpha);
    }

    @Override
    public ExecutableOp createFillLinearGradient(int x, int y, int width, int height, int startColor, int endColor, boolean horizontal, int alpha) {
        return new FillLinearGradient(x, y, width, height, startColor, endColor, horizontal, alpha);
    }

    @Override
    public ExecutableOp createFillRadialGradient(int x, int y, int width, int height, int startColor, int endColor, int alpha, int startAngle, int arcAngle) {
        return new FillRadialGradient(x, y, width, height, startColor, endColor, alpha, startAngle, arcAngle);
    }

    @Override
    public ExecutableOp createFillRectRadialGradient(int x, int y, int width, int height, int startColor, int endColor, float relativeX, float relativeY, float relativeSize, int alpha) {
        return new FillRectRadialGradient(x, y, width, height, startColor, endColor, relativeX, relativeY, relativeSize, alpha);
    }
}
