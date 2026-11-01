/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter;

import java.util.List;

import com.codename1.flutter.rendering.GraphicsCanvas;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;

import dart.async.Future;

/**
 * dart:ui's {@code Picture}: the calls a {@link PictureRecorder} captured. {@link #toImage}
 * replays them onto a mutable Codename One image ({@code Image.createImage(w, h, 0)}) of
 * that size, transparent where nothing was drawn. Headless (no Display) there is nothing
 * to draw on, so the image is fully transparent, still {@code width x height}, and still
 * encodes to a valid PNG.
 */
public class Picture {
    private final List<Object[]> ops;
    private boolean disposed;

    Picture(List<Object[]> ops) {
        this.ops = ops;
    }

    /** How many drawing calls were recorded. */
    public long approximateBytesUsed() {
        return ops.size();
    }

    public Future<UiImage> toImage(long width, long height) {
        try {
            return Future.value(toImageSync(width, height));
        } catch (RuntimeException e) {
            return Future.error(e);
        }
    }

    public UiImage toImageSync(long width, long height) {
        if (disposed) {
            throw new dart.core.StateError("Picture has been disposed");
        }
        int w = (int) Math.max(1, width);
        int h = (int) Math.max(1, height);
        if (!Display.isInitialized()) {
            return new UiImage(w, h, null);
        }
        Image img = Image.createImage(w, h, 0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        Canvas target = new GraphicsCanvas(g, 0, 0, 1);
        for (Object[] op : ops) {
            Canvas.replay(target, (String) op[0], (Object[]) op[1]);
        }
        return new UiImage(w, h, img);
    }

    public void dispose() {
        disposed = true;
    }

    public boolean debugDisposed() {
        return disposed;
    }
}
