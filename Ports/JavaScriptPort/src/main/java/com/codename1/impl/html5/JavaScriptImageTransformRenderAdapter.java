/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

import com.codename1.impl.html5.graphics.ClipState;

public final class JavaScriptImageTransformRenderAdapter<I, S, T, O> {
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
        sink.submit(factory.createDrawImage(image, x, y, state.getAlpha()));
    }

    public void drawImage(I image, int x, int y, int width, int height) {
        sink.submit(factory.createDrawImage(image, x, y, width, height, state.getAlpha()));
    }

    public void tileImage(I image, int x, int y, int width, int height) {
        sink.submit(factory.createTileImage(image, x, y, width, height, state.getAlpha()));
    }

    public void applyTransform(T transform, boolean replace) {
        sink.submit(factory.createTransform(transform, replace));
    }

    public void setClipShape(S shape, T transform) {
        sink.submit(factory.createClipShape(shape, transform, state.getClipState()));
    }
}
