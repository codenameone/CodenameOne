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

public final class JavaScriptCanvasImageBufferLifecycle {
    private JavaScriptCanvasImageBufferLifecycle() {
    }

    public interface ScratchCanvasFactory<C> {
        C createScratchCanvas();
    }

    public interface SizedCanvasFactory<C> {
        C createCanvas(int width, int height);
    }

    public interface CanvasSizeAccess<C> {
        int getWidth(C canvas);
        int getHeight(C canvas);
        void setWidth(C canvas, int width);
        void setHeight(C canvas, int height);
    }

    public interface GraphicsFactory<C, G> {
        // ``width``/``height`` are the dimensions the canvas was just created
        // with -- passed in so the graphics can initialise its clip bounds
        // WITHOUT reading canvas.getWidth()/getHeight() back across the
        // worker<->host barrier (those numeric round-trips can cross into a
        // concurrent object read and corrupt it; the Java side already knows
        // the size).
        G createGraphics(C canvas, int width, int height);
        void fillRect(G graphics, int fillColor, int width, int height);
    }

    public static final class CanvasImageBuffer<C, G> {
        private final C canvas;
        private final G graphics;
        private final int width;
        private final int height;

        public CanvasImageBuffer(C canvas, G graphics, int width, int height) {
            this.canvas = canvas;
            this.graphics = graphics;
            this.width = width;
            this.height = height;
        }

        public C getCanvas() {
            return canvas;
        }

        public G getGraphics() {
            return graphics;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

    public static <C> C ensureScratchBuffer(C scratchBuffer, int width, int height, ScratchCanvasFactory<C> factory, CanvasSizeAccess<C> sizeAccess) {
        C out = scratchBuffer;
        if (out == null) {
            out = factory.createScratchCanvas();
        }
        if (width > sizeAccess.getWidth(out) || height > sizeAccess.getHeight(out)) {
            sizeAccess.setWidth(out, width);
            sizeAccess.setHeight(out, height);
        }
        return out;
    }

    public static <C, G> CanvasImageBuffer<C, G> createBlankBuffer(int width, int height, SizedCanvasFactory<C> canvasFactory, GraphicsFactory<C, G> graphicsFactory) {
        C canvas = canvasFactory.createCanvas(width, height);
        G graphics = graphicsFactory.createGraphics(canvas, width, height);
        return new CanvasImageBuffer<C, G>(canvas, graphics, width, height);
    }

    public static <C, G> CanvasImageBuffer<C, G> createMutableBuffer(int width, int height, int fillColor, SizedCanvasFactory<C> canvasFactory, GraphicsFactory<C, G> graphicsFactory) {
        CanvasImageBuffer<C, G> buffer = createBlankBuffer(width, height, canvasFactory, graphicsFactory);
        if (((fillColor >> 24) & 0xff) != 0) {
            graphicsFactory.fillRect(buffer.getGraphics(), fillColor, width, height);
        }
        return buffer;
    }
}
