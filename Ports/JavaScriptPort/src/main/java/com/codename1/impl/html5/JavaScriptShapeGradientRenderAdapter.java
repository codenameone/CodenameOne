/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

public final class JavaScriptShapeGradientRenderAdapter<S, K, O> {
    private static int debugLogCount;

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
        O operation = factory.createDrawShape(shape, stroke, state.getColor(), state.getAlpha());
        debugSubmit("drawShape", operation);
        sink.submit(operation);
    }

    public void fillShape(S shape) {
        O operation = factory.createFillShape(shape, state.getColor(), state.getAlpha());
        debugSubmit("fillShape", operation);
        sink.submit(operation);
    }

    public void fillLinearGradient(int x, int y, int width, int height, int startColor, int endColor, boolean horizontal) {
        O operation = factory.createFillLinearGradient(x, y, width, height, startColor, endColor, horizontal, state.getAlpha());
        debugSubmit("fillLinearGradient", operation);
        sink.submit(operation);
    }

    public void fillRadialGradient(int x, int y, int width, int height, int startColor, int endColor, int startAngle, int arcAngle) {
        O operation = factory.createFillRadialGradient(x, y, width, height, startColor, endColor, state.getAlpha(), startAngle, arcAngle);
        debugSubmit("fillRadialGradient", operation);
        sink.submit(operation);
    }

    public void fillRectRadialGradient(int x, int y, int width, int height, int startColor, int endColor, float relativeX, float relativeY, float relativeSize) {
        O operation = factory.createFillRectRadialGradient(x, y, width, height, startColor, endColor, relativeX, relativeY, relativeSize, state.getAlpha());
        debugSubmit("fillRectRadialGradient", operation);
        sink.submit(operation);
    }

    private static void debugSubmit(String operationName, Object operation) {
        if (debugLogCount >= 80) {
            return;
        }
        debugLogCount++;
        System.out.println("CN1JS:ShapeGradientAdapter." + operationName + " op="
                + (operation == null ? "null" : operation.getClass().getName()));
    }
}
