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
