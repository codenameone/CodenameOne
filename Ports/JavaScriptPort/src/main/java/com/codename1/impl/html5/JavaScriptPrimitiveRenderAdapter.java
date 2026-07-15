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
package com.codename1.impl.html5;

import com.codename1.impl.html5.graphics.ClipState;

public final class JavaScriptPrimitiveRenderAdapter<F, O> {
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
        sink.submit(factory.createFillRect(x, y, width, height, state.getColor(), state.getAlpha()));
    }

    public void clearRect(int x, int y, int width, int height) {
        sink.submit(factory.createClearRect(x, y, width, height));
    }

    public void drawRect(int x, int y, int width, int height) {
        sink.submit(factory.createDrawRect(x, y, width, height, state.getColor(), state.getAlpha()));
    }

    public void drawLine(int x1, int y1, int x2, int y2) {
        sink.submit(factory.createDrawLine(x1, y1, x2, y2, state.getColor(), state.getAlpha()));
    }

    public void drawString(String str, int x, int y) {
        sink.submit(factory.createDrawString(str, x, y, state.getColor(), state.getAlpha(), state.getFont()));
    }

    public void setClipRect(int x, int y, int width, int height) {
        sink.submit(factory.createClipRect(x, y, width, height, state.getClipState()));
    }
}
