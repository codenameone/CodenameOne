/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
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
        G createGraphics(C canvas);
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
        G graphics = graphicsFactory.createGraphics(canvas);
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
