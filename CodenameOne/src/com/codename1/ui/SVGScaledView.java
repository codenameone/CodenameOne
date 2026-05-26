/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 */
package com.codename1.ui;

/// Lightweight scaled view returned by [GeneratedSVGImage#scaled(int, int)].
/// Reports the caller-supplied width and height from [#getWidth] / [#getHeight]
/// so component layout sees the correct size, while delegating all rendering
/// (and animation state) to the source SVG.
final class SVGScaledView extends Image {

    private final GeneratedSVGImage source;
    private final int width;
    private final int height;

    SVGScaledView(GeneratedSVGImage source, int width, int height) {
        super(null);
        this.source = source;
        this.width = width < 1 ? 1 : width;
        this.height = height < 1 ? 1 : height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public boolean isAnimation() {
        return source.isAnimation();
    }

    @Override
    public boolean animate() {
        return source.animate();
    }

    @Override
    protected void drawImage(Graphics g, Object nativeGraphics, int x, int y) {
        source.drawImage(g, nativeGraphics, x, y, width, height);
    }

    @Override
    protected void drawImage(Graphics g, Object nativeGraphics, int x, int y, int w, int h) {
        // Honor the requested draw size rather than our reported size; the
        // surrounding component may stretch us inside a different rectangle.
        source.drawImage(g, nativeGraphics, x, y, w, h);
    }

    @Override
    public Image scaled(int width, int height) {
        return new SVGScaledView(source, width, height);
    }
}
