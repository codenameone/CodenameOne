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
package com.codename1.desktopcompat.rt;

import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.PathIterator;

/// A two-color gradient drawn through a shape pixel by pixel, for [G2D].
///
/// The shape is filled in one color on an image of its own, which says how
/// much of every pixel it covers; each pixel then takes the color the
/// gradient has there, and the image is drawn where the shape is.
///
/// ## Why not the platform's paint
///
/// `Graphics.setColor(Paint)` followed by `fillShape` is the obvious way and
/// was the one used. It confines the paint by making the shape the clip and
/// putting the rectangle that was the clip back on top with `clipRect`, then
/// fills a band much larger than the shape under a matrix of its own. What a
/// port does with a rectangle on top of a shape clip decided the picture: one
/// dropped the shape, so the band covered the panel from its corner; two
/// rebuilt a shape with a curved corner as slivers along its edges, so only an
/// outline of the fill was left. Under a rotation the band is also made of
/// one-pixel lines, which shows as a hatch. Nothing here uses a shape clip, a
/// matrix or a paint object: filling a shape in one color, reading pixels and
/// drawing an image is all a port has to get right.
///
/// ## What it costs
///
/// An image the size of the part of the shape inside the clip, four bytes a
/// pixel, and a multiplication and a table look-up for each. The last few
/// are kept, so a shape repainted where it was costs one image draw; a shape
/// that changes every frame is worked out every frame. A gradient straight
/// across exactly the rectangle it fills never comes here -- [G2D] hands that
/// to the port's two-color fill, which is what a background usually is.
final class GradientFill {

    /// The most pixels worked out for one fill. Past it [G2D] falls back to
    /// the platform's paint.
    private static final int LIMIT = 4000000;

    /// The longest path whose picture is kept, in coordinates.
    private static final int KEY_LIMIT = 512;

    private static final int TABLE = 256;

    private static final class Entry {
        double[] key;
        Image image;
    }

    private static final Entry[] KEPT = new Entry[8];
    private static int next;

    private GradientFill() {
    }

    private static int mix(int from, int to, int shift, double t) {
        int a = (from >>> shift) & 0xff;
        int b = (to >>> shift) & 0xff;
        return ((int) (a + (b - a) * t + 0.5)) << shift;
    }

    /// Draws the gradient through `path`, which is in the coordinates of
    /// `g`. A point `(x, y)` there is at `ta * x + tb * y + tc` on the
    /// gradient: 0 where the first color is and 1 where the second is.
    /// Outside that the nearest color goes on, or with `cyclic` the colors
    /// run back and forth. `clip` is the rectangle `{x, y, w, h}` nothing is
    /// drawn outside of, and `alpha` what `g` draws the result with.
    ///
    /// Returns false when it drew nothing and the caller has to: the fill is
    /// larger than [#LIMIT], or an image here has no shapes.
    static boolean fill(Graphics g, GeneralPath path, int[] clip, boolean antialias, double ta, double tb,
                        double tc, int argb1, int argb2, boolean cyclic, int alpha) {
        float[] b = path.getBounds2D();
        int left = (int) Math.floor(Math.max(b[0] - 1, clip[0]));
        int top = (int) Math.floor(Math.max(b[1] - 1, clip[1]));
        int right = (int) Math.ceil(Math.min(b[0] + (double) b[2] + 1, clip[0] + (double) clip[2]));
        int bottom = (int) Math.ceil(Math.min(b[1] + (double) b[3] + 1, clip[1] + (double) clip[3]));
        int w = right - left;
        int h = bottom - top;
        if (w <= 0 || h <= 0 || alpha <= 0) {
            return true;
        }
        if ((long) w * h > LIMIT) {
            return false;
        }
        // The path as the image sees it, and the same numbers as the key the
        // picture is kept under: a shape drawn again somewhere else is the
        // same picture when it moved by whole pixels.
        GeneralPath local = new GeneralPath();
        local.setWindingRule(path.getWindingRule());
        double[] key = new double[KEY_LIMIT];
        int n = 0;
        key[n++] = w;
        key[n++] = h;
        key[n++] = ta;
        key[n++] = tb;
        // Where the middle of the image's first pixel is on the gradient.
        double origin = ta * (left + 0.5) + tb * (top + 0.5) + tc;
        key[n++] = origin;
        key[n++] = argb1;
        key[n++] = argb2;
        key[n++] = (cyclic ? 1 : 0) + (antialias ? 2 : 0) + path.getWindingRule() * 4;
        boolean keyed = true;
        PathIterator it = path.getPathIterator();
        float[] k = new float[6];
        while (!it.isDone()) {
            int type = it.currentSegment(k);
            int count = type == PathIterator.SEG_CLOSE ? 0
                    : (type == PathIterator.SEG_CUBICTO ? 6 : (type == PathIterator.SEG_QUADTO ? 4 : 2));
            for (int i = 0; i < count; i += 2) {
                k[i] -= left;
                k[i + 1] -= top;
            }
            if (keyed && n + count + 1 <= KEY_LIMIT) {
                key[n++] = type;
                for (int i = 0; i < count; i++) {
                    key[n++] = k[i];
                }
            } else {
                keyed = false;
            }
            switch (type) {
                case PathIterator.SEG_MOVETO:
                    local.moveTo(k[0], k[1]);
                    break;
                case PathIterator.SEG_LINETO:
                    local.lineTo(k[0], k[1]);
                    break;
                case PathIterator.SEG_QUADTO:
                    local.quadTo(k[0], k[1], k[2], k[3]);
                    break;
                case PathIterator.SEG_CUBICTO:
                    local.curveTo(k[0], k[1], k[2], k[3], k[4], k[5]);
                    break;
                default:
                    local.closePath();
                    break;
            }
            it.next();
        }
        Image image = null;
        if (keyed) {
            double[] exact = new double[n];
            System.arraycopy(key, 0, exact, 0, n);
            key = exact;
            for (int i = 0; i < KEPT.length && image == null; i++) {
                if (KEPT[i] != null && java.util.Arrays.equals(KEPT[i].key, key)) {
                    image = KEPT[i].image;
                }
            }
        }
        if (image == null) {
            Image mask = Image.createImage(w, h, 0);
            Graphics mg = mask.getGraphics();
            if (!mg.isShapeSupported()) {
                return false;
            }
            mg.setColor(0xffffff);
            mg.setAntiAliased(antialias);
            mg.fillShape(local);
            int[] pixels = mask.getRGB();
            int[] table = new int[TABLE];
            for (int i = 0; i < TABLE; i++) {
                double t = i / (double) (TABLE - 1);
                table[i] = mix(argb1, argb2, 24, t) | mix(argb1, argb2, 16, t) | mix(argb1, argb2, 8, t)
                        | mix(argb1, argb2, 0, t);
            }
            for (int row = 0; row < h; row++) {
                double t = origin + tb * row;
                int at = row * w;
                for (int column = 0; column < w; column++, at++, t += ta) {
                    int cover = pixels[at] >>> 24;
                    if (cover == 0) {
                        pixels[at] = 0;
                        continue;
                    }
                    double place = t;
                    if (cyclic) {
                        // Back and forth: 0 to 1 is the first color to the
                        // second, 1 to 2 the way back.
                        place = place / 2;
                        place = (place - Math.floor(place)) * 2;
                        if (place > 1) {
                            place = 2 - place;
                        }
                    } else if (!(place > 0)) {
                        place = 0;
                    } else if (place > 1) {
                        place = 1;
                    }
                    int c = table[(int) (place * (TABLE - 1) + 0.5)];
                    pixels[at] = (((c >>> 24) * cover + 127) / 255 << 24) | (c & 0xffffff);
                }
            }
            image = Image.createImage(pixels, w, h);
            if (keyed) {
                Entry e = new Entry();
                e.key = key;
                e.image = image;
                KEPT[next] = e;
                next = (next + 1) % KEPT.length;
            }
        }
        g.setAlpha(alpha);
        g.drawImage(image, left, top);
        return true;
    }
}
