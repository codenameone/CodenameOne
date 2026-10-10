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
package javafx.scene.image;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import com.codename1.ui.Display;

import javafx.scene.paint.Color;

/// An image whose pixels the application writes.
///
/// The pixels are kept as an array, which is what the writer and the
/// reader work on. The Codename One image that is drawn is made from
/// the array when it is next needed after a change, so a burst of
/// writes costs one image. A change reaches the views of the image on
/// the application thread, once for all the writes made since the last
/// time, whichever thread wrote.
public class WritableImage extends Image {

    private final int w;
    private final int h;
    private final int[] argb;
    private final ArrayList<Runnable> listeners = new ArrayList<Runnable>();
    private final AtomicBoolean pending = new AtomicBoolean();
    private final AtomicBoolean stale = new AtomicBoolean(true);
    private com.codename1.ui.Image built;
    private PixelWriter writer;

    private final Runnable tell = new Runnable() {
        @Override
        public void run() {
            pending.set(false);
            Runnable[] all = listeners.toArray(new Runnable[listeners.size()]);
            for (int i = 0; i < all.length; i++) {
                all[i].run();
            }
        }
    };

    /// Creates a transparent image of a size.
    public WritableImage(int width, int height) {
        super(width, height);
        this.w = width;
        this.h = height;
        this.argb = new int[width * height];
    }

    /// Creates an image of a size from the pixels a reader answers.
    public WritableImage(PixelReader reader, int width, int height) {
        this(reader, 0, 0, width, height);
    }

    /// Creates an image from a rectangle of the pixels a reader answers.
    public WritableImage(PixelReader reader, int x, int y, int width, int height) {
        this(width, height);
        if (reader == null) {
            throw new NullPointerException("reader must not be null");
        }
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                argb[row * width + col] = reader.getArgb(x + col, y + row);
            }
        }
    }

    @Override
    int[] pixels() {
        return argb;
    }

    @Override
    com.codename1.ui.Image current() {
        if (stale.getAndSet(false) || built == null) {
            built = com.codename1.ui.Image.createImage(argb, w, h);
        }
        return built;
    }

    @Override
    void addPixelListener(Runnable listener) {
        listeners.add(listener);
    }

    @Override
    void removePixelListener(Runnable listener) {
        listeners.remove(listener);
    }

    /// The pixels changed: the image drawn is out of date, and the views
    /// hear of it on the application thread.
    private void changed() {
        stale.set(true);
        if (pending.compareAndSet(false, true)) {
            if (Display.isInitialized() && !Display.getInstance().isEdt()) {
                Display.getInstance().callSerially(tell);
            } else {
                tell.run();
            }
        }
    }

    /// Replaces every pixel; the array is `0xAARRGGBB` row after row and
    /// as long as the image is large.
    public final void cn1SetPixels(int[] pixels) {
        if (pixels == null || pixels.length != argb.length) {
            throw new IllegalArgumentException("as many pixels as the image has");
        }
        System.arraycopy(pixels, 0, argb, 0, argb.length);
        changed();
    }

    /// Returns what writes the pixels of the image.
    public final PixelWriter getPixelWriter() {
        if (writer == null) {
            writer = new PixelWriter() {
                @Override
                public void setArgb(int x, int y, int value) {
                    if (x < 0 || y < 0 || x >= w || y >= h) {
                        throw new IndexOutOfBoundsException(x + ", " + y);
                    }
                    argb[y * w + x] = value;
                    changed();
                }

                @Override
                public void setColor(int x, int y, Color c) {
                    if (c == null) {
                        throw new NullPointerException("Color cannot be null");
                    }
                    setArgb(x, y, c.cn1Argb());
                }

                @Override
                public void setPixels(int dstx, int dsty, int width, int height, PixelReader reader, int srcx,
                        int srcy) {
                    if (reader == null) {
                        throw new NullPointerException("Reader cannot be null");
                    }
                    for (int row = 0; row < height; row++) {
                        for (int col = 0; col < width; col++) {
                            int x = dstx + col;
                            int y = dsty + row;
                            if (x >= 0 && y >= 0 && x < w && y < h) {
                                argb[y * w + x] = reader.getArgb(srcx + col, srcy + row);
                            }
                        }
                    }
                    changed();
                }
            };
        }
        return writer;
    }
}
