/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

import com.codename1.impl.html5.graphics.ClipState;

public final class JavaScriptPrimitiveRenderAdapter<F, O> {
    private static int debugLogCount;

    public interface OperationSink<O> {
        void submit(O operation);
    }

    public interface PrimitiveOpFactory<F, O> {
        O createFillRect(int x, int y, int width, int height, int color, int alpha);
        O createClearRect(int x, int y, int width, int height);
        O createDrawRect(int x, int y, int width, int height, int color, int alpha);
        O createDrawLine(int x1, int y1, int x2, int y2, int color, int alpha);
        O createDrawString(String str, int x, int y, int color, int alpha, F font);
        O createClipRect(int x, int y, int width, int height, ClipState clipState);
    }

    private final JavaScriptRenderState<F> state;
    private final OperationSink<O> sink;
    private final PrimitiveOpFactory<F, O> factory;

    public JavaScriptPrimitiveRenderAdapter(JavaScriptRenderState<F> state, OperationSink<O> sink, PrimitiveOpFactory<F, O> factory) {
        this.state = state;
        this.sink = sink;
        this.factory = factory;
    }

    public void fillRect(int x, int y, int width, int height) {
        O operation = factory.createFillRect(x, y, width, height, state.getColor(), state.getAlpha());
        debugSubmit("fillRect", operation);
        sink.submit(operation);
    }

    public void clearRect(int x, int y, int width, int height) {
        O operation = factory.createClearRect(x, y, width, height);
        debugSubmit("clearRect", operation);
        sink.submit(operation);
    }

    public void drawRect(int x, int y, int width, int height) {
        O operation = factory.createDrawRect(x, y, width, height, state.getColor(), state.getAlpha());
        debugSubmit("drawRect", operation);
        sink.submit(operation);
    }

    public void drawLine(int x1, int y1, int x2, int y2) {
        O operation = factory.createDrawLine(x1, y1, x2, y2, state.getColor(), state.getAlpha());
        debugSubmit("drawLine", operation);
        sink.submit(operation);
    }

    public void drawString(String str, int x, int y) {
        O operation = factory.createDrawString(str, x, y, state.getColor(), state.getAlpha(), state.getFont());
        debugSubmit("drawString", operation);
        sink.submit(operation);
    }

    public void setClipRect(int x, int y, int width, int height) {
        O operation = factory.createClipRect(x, y, width, height, state.getClipState());
        debugSubmit("setClipRect", operation);
        sink.submit(operation);
    }

    private static void debugSubmit(String operationName, Object operation) {
        if (!PortDiag.isPickerDiag()) {
            return;
        }
        if (debugLogCount >= 5000) {
            return;
        }
        debugLogCount++;
        System.out.println("CN1JS:PrimitiveAdapter." + operationName + " op="
                + (operation == null ? "null" : operation.getClass().getName()));
    }
}
