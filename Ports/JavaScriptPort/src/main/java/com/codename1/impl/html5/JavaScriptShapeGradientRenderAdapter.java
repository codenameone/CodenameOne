/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

public final class JavaScriptShapeGradientRenderAdapter<S, K, O> {
    public interface OperationSink<O> {
        void submit(O operation);
    }

    public interface ShapeGradientOpFactory<S, K, O> {
        O createDrawShape(S shape, K stroke, int color, int alpha);
        O createFillShape(S shape, int color, int alpha);
        O createFillLinearGradient(int x, int y, int width, int height, int startColor, int endColor, boolean horizontal, int alpha);
        O createFillRadialGradient(int x, int y, int width, int height, int startColor, int endColor, int alpha, int startAngle, int arcAngle);
        O createFillRectRadialGradient(int x, int y, int width, int height, int startColor, int endColor, float relativeX, float relativeY, float relativeSize, int alpha);
    }

    private final JavaScriptRenderState<?> state;
    private final OperationSink<O> sink;
    private final ShapeGradientOpFactory<S, K, O> factory;

    public JavaScriptShapeGradientRenderAdapter(JavaScriptRenderState<?> state, OperationSink<O> sink, ShapeGradientOpFactory<S, K, O> factory) {
        this.state = state;
        this.sink = sink;
        this.factory = factory;
    }

    public void drawShape(S shape, K stroke) {
        sink.submit(factory.createDrawShape(shape, stroke, state.getColor(), state.getAlpha()));
    }

    public void fillShape(S shape) {
        sink.submit(factory.createFillShape(shape, state.getColor(), state.getAlpha()));
    }

    public void fillLinearGradient(int x, int y, int width, int height, int startColor, int endColor, boolean horizontal) {
        sink.submit(factory.createFillLinearGradient(x, y, width, height, startColor, endColor, horizontal, state.getAlpha()));
    }

    public void fillRadialGradient(int x, int y, int width, int height, int startColor, int endColor, int startAngle, int arcAngle) {
        sink.submit(factory.createFillRadialGradient(x, y, width, height, startColor, endColor, state.getAlpha(), startAngle, arcAngle));
    }

    public void fillRectRadialGradient(int x, int y, int width, int height, int startColor, int endColor, float relativeX, float relativeY, float relativeSize) {
        sink.submit(factory.createFillRectRadialGradient(x, y, width, height, startColor, endColor, relativeX, relativeY, relativeSize, state.getAlpha()));
    }
}
