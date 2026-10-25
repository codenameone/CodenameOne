/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.fxcompat.runtime;

import com.codename1.ui.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;

import javafx.scene.Node;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.effect.InnerShadow;
import javafx.scene.paint.Color;

/// Draws the two effects the layer draws: the shadow a [DropShadow] casts
/// behind a node and the one an [InnerShadow] casts inside it.
///
/// A shadow is the outline of what the node paints, blurred and coloured.
/// The outline is whatever the node's peer paints -- a shape, a text, a
/// region with its children and their native components -- so the peer is
/// painted once into a picture of its own, the coverage of that picture is
/// blurred with box passes, and the result is kept. Painting the node then
/// costs one image.
///
/// The picture is kept on the node until something under the node changes:
/// every change of every node arrives at `Node.cn1Invalidated`, which
/// drops the pictures of the node and of what is above it. A changed
/// effect, size or scale is noticed when the picture is next asked for.
/// Nothing is walked while no node has a picture.
///
/// Every other effect is recorded and not drawn; see [Effect].
public final class Effects {

    /// The most pixels a shadow is made for. A node larger than this is
    /// painted without one rather than blurred on every change.
    private static final int MAX_PIXELS = 6000000;

    private static int live;
    private static int captures;
    private static Node building;

    private Effects() {
    }

    /// What is kept for a node.
    private static final class Kept {
        Effect effect;
        double[] values;
        int width;
        int height;
        int margin;
        Image behind;
        int behindX;
        int behindY;
        Image over;
    }

    /// How many pictures were made since the start; for tests, which
    /// check that a second paint makes none.
    public static int captureCount() {
        return captures;
    }

    /// Whether a node has an effect that is drawn.
    public static boolean has(Node node) {
        Effect e = node.getEffect();
        return e instanceof DropShadow || e instanceof InnerShadow;
    }

    /// Drops the pictures kept for a node and for everything above it,
    /// because what they show has changed.
    public static void invalidate(Node node) {
        if (live <= 0) {
            return;
        }
        for (Node n = node; n != null; n = n.getParent()) {
            if (n.cn1EffectCache() != null) {
                n.cn1SetEffectCache(null);
                live--;
            }
        }
    }

    private static int passes(BlurType type) {
        if (type == BlurType.ONE_PASS_BOX) {
            return 1;
        }
        return type == BlurType.TWO_PASS_BOX ? 2 : 3;
    }

    private static int argb(Color c) {
        if (c == null) {
            return 0xff000000;
        }
        return ((int) Math.round(c.getOpacity() * 255) << 24) | ((int) Math.round(c.getRed() * 255) << 16)
                | ((int) Math.round(c.getGreen() * 255) << 8) | (int) Math.round(c.getBlue() * 255);
    }

    /// The numbers a picture depends on: per shadow its kind, radius,
    /// spread, offsets, colour and passes, then the scale.
    private static double[] values(Effect top) {
        double[] out = new double[17];
        int at = 0;
        Effect e = top;
        for (int level = 0; level < 2 && e != null; level++) {
            if (e instanceof DropShadow) {
                DropShadow d = (DropShadow) e;
                out[at] = 1;
                out[at + 1] = d.getRadius();
                out[at + 2] = d.getSpread();
                out[at + 3] = d.getOffsetX();
                out[at + 4] = d.getOffsetY();
                out[at + 5] = argb(d.getColor());
                out[at + 6] = passes(d.getBlurType());
                e = d.getInput();
            } else if (e instanceof InnerShadow) {
                InnerShadow s = (InnerShadow) e;
                out[at] = 2;
                out[at + 1] = s.getRadius();
                out[at + 2] = s.getChoke();
                out[at + 3] = s.getOffsetX();
                out[at + 4] = s.getOffsetY();
                out[at + 5] = argb(s.getColor());
                out[at + 6] = passes(s.getBlurType());
                e = s.getInput();
            } else {
                e = null;
            }
            at += 8;
        }
        out[16] = Units.scale();
        return out;
    }

    private static boolean same(double[] a, double[] b) {
        for (int i = 0; i < a.length; i++) {
            if (Double.compare(a[i], b[i]) != 0) {
                return false;
            }
        }
        return true;
    }

    /// One box pass over rows of `w` values, then over columns. `edge` is
    /// what lies beyond the picture: nothing for a shadow cast outwards,
    /// everything for one cast inwards.
    static void box(int[] a, int w, int h, int r, int edge) {
        if (r <= 0 || w <= 0 || h <= 0) {
            return;
        }
        int span = 2 * r + 1;
        int[] line = new int[Math.max(w, h)];
        for (int y = 0; y < h; y++) {
            int row = y * w;
            int sum = edge * r;
            for (int x = 0; x <= r; x++) {
                sum += x < w ? a[row + x] : edge;
            }
            for (int x = 0; x < w; x++) {
                line[x] = sum / span;
                int out = x - r;
                int in = x + r + 1;
                sum += (in < w ? a[row + in] : edge) - (out >= 0 ? a[row + out] : edge);
            }
            System.arraycopy(line, 0, a, row, w);
        }
        for (int x = 0; x < w; x++) {
            int sum = edge * r;
            for (int y = 0; y <= r; y++) {
                sum += y < h ? a[y * w + x] : edge;
            }
            for (int y = 0; y < h; y++) {
                line[y] = sum / span;
                int out = y - r;
                int in = y + r + 1;
                sum += (in < h ? a[in * w + x] : edge) - (out >= 0 ? a[out * w + x] : edge);
            }
            for (int y = 0; y < h; y++) {
                a[y * w + x] = line[y];
            }
        }
    }

    private static int boxRadius(double radius, int passes) {
        if (!(radius > 0)) {
            return 0;
        }
        // Passes of a box of radius r reach r * passes; JavaFX's radius is
        // how far the whole blur reaches.
        return Math.max(1, (int) Math.round(radius / passes));
    }

    private static int strengthen(int a, double spread) {
        if (!(spread > 0)) {
            return a;
        }
        if (spread >= 1) {
            return a > 0 ? 255 : 0;
        }
        return (int) Math.min(255, a / (1 - spread));
    }

    /// The shadow a picture casts: its coverage blurred over `radius`
    /// pixels in `passes` box passes, the inner `spread` of the blur at
    /// full strength, in a colour. The answer has the size of the picture.
    public static int[] dropShadow(int[] picture, int w, int h, double radius, int passes, double spread, int color) {
        int[] a = new int[picture.length];
        for (int i = 0; i < a.length; i++) {
            a[i] = picture[i] >>> 24;
        }
        int r = boxRadius(radius, passes);
        for (int p = 0; p < passes; p++) {
            box(a, w, h, r, 0);
        }
        int alpha = color >>> 24;
        int rgb = color & 0xffffff;
        for (int i = 0; i < a.length; i++) {
            a[i] = ((strengthen(a[i], spread) * alpha / 255) << 24) | rgb;
        }
        return a;
    }

    /// The shadow cast inside a picture by its own edge: what is NOT
    /// covered, moved by an offset and blurred, kept only where the
    /// picture is. Beyond the picture nothing is covered.
    public static int[] innerShadow(int[] picture, int w, int h, double radius, int passes, double choke, int color,
            int dx, int dy) {
        int[] a = new int[picture.length];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int sx = x - dx;
                int sy = y - dy;
                boolean inside = sx >= 0 && sy >= 0 && sx < w && sy < h;
                a[y * w + x] = inside ? 255 - (picture[sy * w + sx] >>> 24) : 255;
            }
        }
        int r = boxRadius(radius, passes);
        for (int p = 0; p < passes; p++) {
            box(a, w, h, r, 255);
        }
        int alpha = color >>> 24;
        int rgb = color & 0xffffff;
        for (int i = 0; i < a.length; i++) {
            int own = picture[i] >>> 24;
            a[i] = ((strengthen(a[i], choke) * own / 255 * alpha / 255) << 24) | rgb;
        }
        return a;
    }

    private static Kept kept(Node node, Component peer) {
        Effect effect = node.getEffect();
        double[] values = values(effect);
        Object cached = node.cn1EffectCache();
        if (cached instanceof Kept) {
            Kept k = (Kept) cached;
            if (k.effect == effect && k.width == peer.getWidth() && k.height == peer.getHeight()
                    && same(k.values, values)) {
                return k;
            }
        }
        Kept k = new Kept();
        k.effect = effect;
        k.values = values;
        k.width = peer.getWidth();
        k.height = peer.getHeight();
        if (cached == null) {
            live++;
        }
        // Kept before the picture is made, so that painting the peer into
        // the picture finds it and does not start over.
        node.cn1SetEffectCache(k);
        build(k, node, peer);
        return k;
    }

    private static void build(Kept k, Node node, Component peer) {
        double scale = Units.scale();
        double reach = 0;
        for (int at = 0; at < 16; at += 8) {
            if (k.values[at] != 0) {
                reach = Math.max(reach, k.values[at + 1] * scale + 2);
            }
        }
        int margin = (int) Math.ceil(reach) + 1;
        if (node.cn1PaintMatrix() != null) {
            // A turned or grown node paints beyond its box.
            margin += Math.max(k.width, k.height) / 2;
        }
        int w = k.width + 2 * margin;
        int h = k.height + 2 * margin;
        if (k.width <= 0 || k.height <= 0 || (long) w * h > MAX_PIXELS) {
            return;
        }
        int[] picture;
        Node outer = building;
        building = node;
        try {
            Image image = Image.createImage(w, h, 0);
            Graphics g = image.getGraphics();
            g.translate(margin - peer.getX(), margin - peer.getY());
            g.setClip(peer.getX() - margin, peer.getY() - margin, w, h);
            if (peer instanceof ParentPeer) {
                ((ParentPeer) peer).capture(g);
            } else if (peer instanceof NodePeer) {
                ((NodePeer) peer).capture(g);
            } else {
                return;
            }
            // Read into an array of the size asked for: a port's picture
            // may report another.
            picture = new int[w * h];
            image.getRGB(picture);
        } catch (RuntimeException unavailable) {
            // A port without pictures to paint into draws no shadow.
            return;
        } finally {
            building = outer;
        }
        captures++;
        k.margin = margin;
        for (int at = 0; at < 16; at += 8) {
            double[] v = k.values;
            int color = (int) v[at + 5];
            int dx = (int) Math.round(v[at + 3] * scale);
            int dy = (int) Math.round(v[at + 4] * scale);
            if (v[at] == 1 && k.behind == null) {
                k.behind = Image.createImage(
                        dropShadow(picture, w, h, v[at + 1] * scale, (int) v[at + 6], v[at + 2], color), w, h);
                k.behindX = dx;
                k.behindY = dy;
            } else if (v[at] == 2 && k.over == null) {
                k.over = Image.createImage(
                        innerShadow(picture, w, h, v[at + 1] * scale, (int) v[at + 6], v[at + 2], color, dx, dy), w,
                        h);
            }
        }
    }

    private static void draw(Graphics g, Component peer, Image image, int x, int y) {
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        // A shadow lies outside its node, which the node's own clip ends at.
        PeerPaint.widenClip(g, peer);
        g.drawImage(image, x, y);
        g.setClip(cx, cy, cw, ch);
    }

    /// Draws what a node's effect puts behind it; called by the peer
    /// before it paints the node.
    static void paintBehind(Graphics g, Node node, Component peer) {
        if (!has(node) || capturing(node)) {
            return;
        }
        Kept k = kept(node, peer);
        if (k.behind != null) {
            draw(g, peer, k.behind, peer.getX() - k.margin + k.behindX, peer.getY() - k.margin + k.behindY);
        }
    }

    /// Draws what a node's effect puts over it; called by the peer after
    /// it has painted the node and everything in it.
    static void paintOver(Graphics g, Node node, Component peer) {
        if (!has(node) || capturing(node)) {
            return;
        }
        Kept k = kept(node, peer);
        if (k.over != null) {
            draw(g, peer, k.over, peer.getX() - k.margin, peer.getY() - k.margin);
        }
    }

    /// Whether the picture of this node is being made right now, when the
    /// node is painted as it is without its effect.
    private static boolean capturing(Node node) {
        return building == node;
    }
}
