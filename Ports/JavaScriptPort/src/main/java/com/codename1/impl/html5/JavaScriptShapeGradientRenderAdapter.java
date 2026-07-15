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
