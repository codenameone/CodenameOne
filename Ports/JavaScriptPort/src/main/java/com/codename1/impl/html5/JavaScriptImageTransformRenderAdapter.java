/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

import com.codename1.impl.html5.graphics.ClipState;

public final class JavaScriptImageTransformRenderAdapter<I, S, T, O> {
    private static int debugLogCount;

    public interface OperationSink<O> {
        void submit(O operation);
    }

    public interface ImageTransformOpFactory<I, S, T, O> {
        O createDrawImage(I image, int x, int y, int alpha);
        O createDrawImage(I image, int x, int y, int width, int height, int alpha);
        O createTileImage(I image, int x, int y, int width, int height, int alpha);
        O createTransform(T transform, boolean replace);
        O createClipShape(S shape, T transform, ClipState clipState);
    }

    private final JavaScriptRenderState<?> state;
    private final OperationSink<O> sink;
    private final ImageTransformOpFactory<I, S, T, O> factory;

    public JavaScriptImageTransformRenderAdapter(JavaScriptRenderState<?> state, OperationSink<O> sink, ImageTransformOpFactory<I, S, T, O> factory) {
        this.state = state;
        this.sink = sink;
        this.factory = factory;
    }

    public void drawImage(I image, int x, int y) {
        O operation = factory.createDrawImage(image, x, y, state.getAlpha());
        debugSubmit("drawImage", operation);
        sink.submit(operation);
    }

    public void drawImage(I image, int x, int y, int width, int height) {
        O operation = factory.createDrawImage(image, x, y, width, height, state.getAlpha());
        debugSubmit("drawImageScaled", operation);
        sink.submit(operation);
    }

    public void tileImage(I image, int x, int y, int width, int height) {
        O operation = factory.createTileImage(image, x, y, width, height, state.getAlpha());
        debugSubmit("tileImage", operation);
        sink.submit(operation);
    }

    public void applyTransform(T transform, boolean replace) {
        O operation = factory.createTransform(transform, replace);
        debugSubmit("applyTransform", operation);
        sink.submit(operation);
    }

    public void setClipShape(S shape, T transform) {
        O operation = factory.createClipShape(shape, transform, state.getClipState());
        debugSubmit("setClipShape", operation);
        sink.submit(operation);
    }

    private static void debugSubmit(String operationName, Object operation) {
        if (debugLogCount >= 80) {
            return;
        }
        debugLogCount++;
        System.out.println("CN1JS:ImageTransformAdapter." + operationName + " op="
                + (operation == null ? "null" : operation.getClass().getName()));
    }
}
