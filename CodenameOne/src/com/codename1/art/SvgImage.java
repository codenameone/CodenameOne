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
package com.codename1.art;

import com.codename1.ui.Graphics;
import com.codename1.ui.Image;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/// An `Image` drawn from an SVG (or any `ArtDocument`) at whatever size it is painted.
///
/// The document is rasterized at the drawn size the first time it is drawn at that size,
/// and the result is cached until the next size.
public class SvgImage extends Image {
    private final ArtDocument document;
    private int width;
    private int height;
    private Image cache;

    /// Wraps a document; its natural size is the document's size, rounded up.
    ///
    /// #### Parameters
    ///
    /// - `document`: the artwork
    public SvgImage(ArtDocument document) {
        this(document, (int) Math.ceil(document.getWidth()), (int) Math.ceil(document.getHeight()));
    }

    /// Wraps a document at a given size.
    ///
    /// #### Parameters
    ///
    /// - `document`: the artwork
    /// - `width`: the image width in pixels
    /// - `height`: the image height in pixels
    public SvgImage(ArtDocument document, int width, int height) {
        super(null);
        this.document = document;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
    }

    /// Reads SVG bytes into an image.
    ///
    /// #### Parameters
    ///
    /// - `svg`: the SVG document
    public static SvgImage create(byte[] svg) throws IOException {
        return new SvgImage(SvgReader.read(new ByteArrayInputStream(svg)));
    }

    /// The document drawn.
    public ArtDocument getDocument() {
        return document;
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
        return false;
    }

    @Override
    public boolean isOpaque() {
        return false;
    }

    @Override
    public Image scaled(int w, int h) {
        return new SvgImage(document, w, h);
    }

    @Override
    public void scale(int w, int h) {
        width = Math.max(1, w);
        height = Math.max(1, h);
        cache = null;
    }

    @Override
    public int[] getRGB() {
        return raster(width, height).getRGB();
    }

    @Override
    public Graphics getGraphics() {
        return null;
    }

    @Override
    public boolean isSVG() {
        return true;
    }

    @Override
    public Object getImage() {
        return raster(width, height).getImage();
    }

    private Image raster(int w, int h) {
        if (cache == null || cache.getWidth() != w || cache.getHeight() != h) {
            double sx = w / Math.max(1e-9, document.getWidth());
            double sy = h / Math.max(1e-9, document.getHeight());
            Raster r = new ArtRenderer().render(document, Matrix.scale(sx, sy), new int[]{0, 0, w, h});
            cache = Image.createImage(r.getPixels(), w, h);
        }
        return cache;
    }

    @Override
    protected void drawImage(Graphics g, Object nativeGraphics, int x, int y) {
        g.drawImage(raster(width, height), x, y);
    }

    @Override
    protected void drawImage(Graphics g, Object nativeGraphics, int x, int y, int w, int h) {
        g.drawImage(raster(w, h), x, y);
    }
}
