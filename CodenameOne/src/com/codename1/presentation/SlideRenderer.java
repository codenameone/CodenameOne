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
package com.codename1.presentation;

import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Stroke;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Shape;
import com.codename1.wordprocessing.Document;
import com.codename1.wordprocessing.DocumentLayout;
import com.codename1.wordprocessing.LayoutItem;
import com.codename1.wordprocessing.LayoutPainter;
import com.codename1.wordprocessing.Measurer;
import com.codename1.wordprocessing.PageSpec;
import com.codename1.wordprocessing.Picture;
import com.codename1.wordprocessing.ScreenMeasurer;
import com.codename1.wordprocessing.Block;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Paints slides on a `Graphics`: thumbnails, the editor's canvas and the slide show all draw
/// through this class, and PDF export draws the same elements with the same geometry.
///
/// Text is laid out by `com.codename1.wordprocessing.DocumentLayout` and drawn by
/// `LayoutPainter`, charts by `com.codename1.ui.chart.Chart`, and diagrams are arranged by
/// `com.codename1.graph.arrange` -- nothing here measures or draws text by itself.
///
/// ```java
/// SlideRenderer renderer = new SlideRenderer();
/// Image thumb = renderer.toImage(deck, deck.getSlides().get(0), 320);
/// ```
public class SlideRenderer {
    private static final int MAX_CACHE = 256;

    /// How a slide show is showing an element partway through a build: its opacity, offset,
    /// scale and how much of it a wipe has revealed.
    public static final class ElementState {
        /// The opacity, 0 to 1.
        public float alpha = 1;

        /// The horizontal offset in points.
        public float dx;

        /// The vertical offset in points.
        public float dy;

        /// The scale about the element's centre.
        public float scale = 1;

        /// The fraction revealed left to right by a wipe, 0 to 1.
        public float reveal = 1;

        /// True when the element is not painted at all.
        public boolean hidden;
    }

    private final ScreenMeasurer measurer = new ScreenMeasurer();
    private final LayoutPainter painter = new LayoutPainter(measurer);
    private final Map<Object, Object> cache = new HashMap<Object, Object>();

    /// The measurer text is laid out with on the screen.
    ///
    /// #### Returns
    ///
    /// the measurer
    public Measurer getMeasurer() {
        return measurer;
    }

    /// Paints a slide.
    ///
    /// #### Parameters
    ///
    /// - `g`: the graphics
    ///
    /// - `presentation`: the presentation the slide belongs to
    ///
    /// - `slide`: the slide
    ///
    /// - `x`: where the slide's left edge goes, in pixels
    ///
    /// - `y`: where its top edge goes
    ///
    /// - `scale`: pixels per point
    public void paint(Graphics g, Presentation presentation, Slide slide, int x, int y,
            float scale) {
        paint(g, presentation, slide, x, y, scale, null);
    }

    /// Paints a slide with some elements partway through their builds.
    ///
    /// #### Parameters
    ///
    /// - `g`: the graphics
    ///
    /// - `presentation`: the presentation
    ///
    /// - `slide`: the slide
    ///
    /// - `x`: left edge in pixels
    ///
    /// - `y`: top edge
    ///
    /// - `scale`: pixels per point
    ///
    /// - `states`: element states by id, or null to paint every element as it is
    public void paint(Graphics g, Presentation presentation, Slide slide, int x, int y,
            float scale, Map<String, ElementState> states) {
        int alpha = g.getAlpha();
        boolean aa = g.isAntiAliased();
        g.setAntiAliased(true);
        g.pushClip();
        g.clipRect(x, y, Math.round(presentation.width * scale),
                Math.round(presentation.height * scale));
        try {
            paintSlide(new ScreenPainter(g, x, y, scale), presentation, slide, states);
        } finally {
            g.popClip();
            g.setAlpha(alpha);
            g.setAntiAliased(aa);
        }
    }

    /// Paints one element, for a view that draws a dragged element by itself.
    ///
    /// #### Parameters
    ///
    /// - `g`: the graphics
    ///
    /// - `e`: the element
    ///
    /// - `x`: where the slide's left edge is, in pixels
    ///
    /// - `y`: where its top edge is
    ///
    /// - `scale`: pixels per point
    public void paintElement(Graphics g, SlideElement e, int x, int y, float scale) {
        int alpha = g.getAlpha();
        try {
            element(new ScreenPainter(g, x, y, scale), e, null, cache);
        } finally {
            g.setAlpha(alpha);
        }
    }

    /// Renders a slide into a new image.
    ///
    /// #### Parameters
    ///
    /// - `presentation`: the presentation
    ///
    /// - `slide`: the slide
    ///
    /// - `width`: the image width in pixels; the height follows the slide's proportions
    ///
    /// #### Returns
    ///
    /// the image
    public Image toImage(Presentation presentation, Slide slide, int width) {
        float scale = width / presentation.width;
        int h = Math.max(1, Math.round(presentation.height * scale));
        Image img = Image.createImage(Math.max(1, width), h, 0xffffffff);
        Graphics g = img.getGraphics();
        paint(g, presentation, slide, 0, 0, scale);
        return img;
    }

    /// Forgets laid out text and rendered charts, for when memory is short.
    public void clearCache() {
        cache.clear();
    }

    // --------------------------------------------------------------- shared painting

    /// Paints a slide with a painter: background, then every element back to front.
    static void paintSlide(SlidePainter p, Presentation presentation, Slide slide,
            Map<String, ElementState> states) {
        paintSlide(p, presentation, slide, states, new HashMap<Object, Object>());
    }

    private void paintSlide(ScreenPainter p, Presentation presentation, Slide slide,
            Map<String, ElementState> states) {
        if (cache.size() > MAX_CACHE) {
            cache.clear();
        }
        paintSlide(p, presentation, slide, states, cache);
    }

    private static void paintSlide(SlidePainter p, Presentation presentation, Slide slide,
            Map<String, ElementState> states, Map<Object, Object> cache) {
        GeneralPath all = rect(0, 0, presentation.width, presentation.height);
        background(p, slide.background, all, presentation.width, presentation.height);
        for (SlideElement e : slide.elements) {
            element(p, e, states, cache);
        }
    }

    private static void background(SlidePainter p, Fill f, GeneralPath area, float w,
            float h) {
        if (!f.isVisible()) {
            return;
        }
        if (f.kind == Fill.PICTURE) {
            p.picture(f.picture, 0, 0, w, h, 0, 0, 0, 0, false, false, f.alpha);
        } else if (f.kind == Fill.GRADIENT) {
            p.gradient(area, f.color, f.color2, f.angle, f.alpha, 0, 0, w, h);
        } else {
            p.fill(area, f.color, f.alpha);
        }
    }

    static GeneralPath rect(float x, float y, float w, float h) {
        GeneralPath r = new GeneralPath();
        r.moveTo(x, y);
        r.lineTo(x + w, y);
        r.lineTo(x + w, y + h);
        r.lineTo(x, y + h);
        r.closePath();
        return r;
    }

    static void element(SlidePainter p, SlideElement e, Map<String, ElementState> states,
            Map<Object, Object> cache) {
        ElementState st = states == null ? null : states.get(e.id);
        if (st != null && (st.hidden || st.alpha <= 0)) {
            return;
        }
        float cx = e.x + e.width / 2;
        float cy = e.y + e.height / 2;
        float[] clip = null;
        if (st != null && st.reveal < 1) {
            clip = new float[] {e.x - 2, e.y - 2, (e.width + 4) * Math.max(0, st.reveal),
                e.height + 4};
        }
        p.push(cx, cy, e.rotation, st == null ? 0 : st.dx, st == null ? 0 : st.dy,
                st == null ? 1 : st.scale, st == null ? 1 : st.alpha, clip);
        try {
            if (e instanceof ShapeElement) {
                shape(p, (ShapeElement) e, cache);
            } else if (e instanceof PictureElement) {
                PictureElement pe = (PictureElement) e;
                p.picture(pe.picture, e.x, e.y, e.width, e.height, pe.cropLeft, pe.cropTop,
                        pe.cropRight, pe.cropBottom, e.flipH, e.flipV, 1);
                if (pe.line.isVisible()) {
                    p.stroke(rect(e.x, e.y, e.width, e.height), pe.line);
                }
            } else if (e instanceof TableElement) {
                table(p, (TableElement) e, cache);
            } else if (e instanceof ChartElement) {
                p.chart((ChartElement) e, e.x, e.y, e.width, e.height);
            } else if (e instanceof DiagramElement) {
                GroupElement g = (GroupElement) cached(cache, e, null);
                if (g == null) {
                    g = ((DiagramElement) e).toGroup();
                    cache.put(e, g);
                }
                for (SlideElement c : g.children) {
                    element(p, c, null, cache);
                }
            } else if (e instanceof GroupElement) {
                for (SlideElement c : ((GroupElement) e).children) {
                    element(p, c, states, cache);
                }
            }
        } finally {
            p.pop();
        }
    }

    private static Object cached(Map<Object, Object> cache, Object key, Object extra) {
        return cache.get(extra == null ? key : new Key(key, extra));
    }

    /// The outline of a shape in slide points, flipped as the shape says.
    ///
    /// #### Parameters
    ///
    /// - `s`: the shape
    ///
    /// #### Returns
    ///
    /// the path
    public static GeneralPath outline(ShapeElement s) {
        GeneralPath local = ShapeGeometry.path(s, s.width, s.height);
        Transform t = Transform.makeTranslation(s.x, s.y);
        if (s.flipH || s.flipV) {
            t.translate(s.width / 2, s.height / 2);
            t.scale(s.flipH ? -1 : 1, s.flipV ? -1 : 1);
            t.translate(-s.width / 2, -s.height / 2);
        }
        Shape moved = local.createTransformedShape(t);
        return moved instanceof GeneralPath ? (GeneralPath) moved : new GeneralPath(moved);
    }

    private static void shape(SlidePainter p, ShapeElement s, Map<Object, Object> cache) {
        GeneralPath path = outline(s);
        if (!s.isLine() && s.fill.isVisible()) {
            if (s.fill.kind == Fill.GRADIENT) {
                p.gradient(path, s.fill.color, s.fill.color2, s.fill.angle, s.fill.alpha, s.x,
                        s.y, s.width, s.height);
            } else if (s.fill.kind == Fill.PICTURE) {
                p.picture(s.fill.picture, s.x, s.y, s.width, s.height, 0, 0, 0, 0, false, false,
                        s.fill.alpha);
            } else {
                p.fill(path, s.fill.color, s.fill.alpha);
            }
        }
        if (s.line.isVisible()) {
            p.stroke(path, s.line);
            if (s.isLine()) {
                arrows(p, s);
            }
        }
        if (s.text != null && !s.text.blocks.isEmpty() && s.width > 1 && s.height > 1) {
            TextBoxLayout tl = textLayout(s, p.measurer(), cache);
            if (tl.layout.getPageCount() > 0) {
                p.text(tl.layout.getPage(0).getItems(), tl.layout.getMeasurer(), s.x,
                        s.y + tl.offsetY, tl.shrink);
            }
        }
    }

    /// The text of a shape laid out, cached by frame and size.
    static TextBoxLayout textLayout(ShapeElement s, Measurer m, Map<Object, Object> cache) {
        Key k = new Key(s.text, new Key(Float.valueOf(s.width), Float.valueOf(s.height)));
        Object o = cache.get(k);
        if (o instanceof TextBoxLayout && ((TextBoxLayout) o).layout.getMeasurer() == m) {
            return (TextBoxLayout) o;
        }
        TextBoxLayout t = TextBoxLayout.of(s.text, s.width, s.height, m);
        cache.put(k, t);
        return t;
    }

    private static void arrows(SlidePainter p, ShapeElement s) {
        if (LineStyle.ARROW_NONE.equals(s.line.head) && LineStyle.ARROW_NONE.equals(s.line.tail)) {
            return;
        }
        float x1 = s.flipH ? s.x + s.width : s.x;
        float y1 = s.flipV ? s.y + s.height : s.y;
        float x2 = s.flipH ? s.x : s.x + s.width;
        float y2 = s.flipV ? s.y : s.y + s.height;
        float size = Math.max(6, s.line.width * 3.5f);
        LineStyle solid = s.line.withDash(LineStyle.SOLID);
        GeneralPath head = ShapeGeometry.arrowHead(s.line.head, x1, y1, x2, y2, size);
        GeneralPath tail = ShapeGeometry.arrowHead(s.line.tail, x2, y2, x1, y1, size);
        if (head != null) {
            arrow(p, head, s.line.head, solid);
        }
        if (tail != null) {
            arrow(p, tail, s.line.tail, solid);
        }
    }

    private static void arrow(SlidePainter p, GeneralPath path, String kind, LineStyle line) {
        if (LineStyle.ARROW_OPEN.equals(kind)) {
            p.stroke(path, line);
        } else {
            p.fill(path, line.color, line.alpha);
        }
    }

    private static void table(SlidePainter p, TableElement t, Map<Object, Object> cache) {
        Key k = new Key(t.table, new Key(Float.valueOf(t.width), p.measurer()));
        Object o = cache.get(k);
        DocumentLayout l;
        if (o instanceof DocumentLayout) {
            l = (DocumentLayout) o;
        } else {
            List<Block> blocks = new ArrayList<Block>();
            blocks.add(t.table);
            PageSpec page = new PageSpec(Math.max(2, t.width), 100000, 0, 0, 0, 0);
            l = new DocumentLayout(new Document(blocks, page, null, 1), p.measurer());
            cache.put(k, l);
        }
        if (l.getPageCount() > 0) {
            p.text(l.getPage(0).getItems(), l.getMeasurer(), t.x, t.y, 1);
        }
    }

    /// The line a gradient runs along across a box at an angle in degrees (0 runs left to
    /// right, 90 top to bottom): x1, y1, x2, y2 in the box's units, through its centre and
    /// long enough that both corners on the line's normals get the end colors.
    static float[] gradientLine(float x, float y, float w, float h, float angle) {
        double a = Math.toRadians(angle);
        double ux = Math.cos(a);
        double uy = Math.sin(a);
        double half = Math.abs(w / 2 * ux) + Math.abs(h / 2 * uy);
        double cx = x + w / 2.0;
        double cy = y + h / 2.0;
        return new float[] {(float) (cx - ux * half), (float) (cy - uy * half),
            (float) (cx + ux * half), (float) (cy + uy * half)};
    }

    /// The bands a gradient is painted in where no gradient paint exists (PDF, whose writer
    /// has no shading objects): `count` quadrilaterals across the box at the
    /// gradient's angle, and the color of each.
    static float[][] gradientBands(float x, float y, float w, float h, float angle, int count) {
        double a = Math.toRadians(angle);
        double ux = Math.cos(a);
        double uy = Math.sin(a);
        // the extent of the box along the gradient direction
        double half = Math.abs(w / 2 * ux) + Math.abs(h / 2 * uy);
        double cx = x + w / 2.0;
        double cy = y + h / 2.0;
        double px = -uy;
        double py = ux;
        double side = Math.sqrt(w * w + h * h);
        float[][] bands = new float[count][];
        for (int i = 0; i < count; i++) {
            double t0 = -half + 2 * half * i / count;
            double t1 = -half + 2 * half * (i + 1) / count + 0.5;
            bands[i] = new float[] {
                (float) (cx + ux * t0 - px * side), (float) (cy + uy * t0 - py * side),
                (float) (cx + ux * t1 - px * side), (float) (cy + uy * t1 - py * side),
                (float) (cx + ux * t1 + px * side), (float) (cy + uy * t1 + py * side),
                (float) (cx + ux * t0 + px * side), (float) (cy + uy * t0 + py * side)};
        }
        return bands;
    }

    static int mix(int from, int to, float t) {
        int r = Math.round(((from >> 16) & 0xff) * (1 - t) + ((to >> 16) & 0xff) * t);
        int g = Math.round(((from >> 8) & 0xff) * (1 - t) + ((to >> 8) & 0xff) * t);
        int b = Math.round((from & 0xff) * (1 - t) + (to & 0xff) * t);
        return (r << 16) | (g << 8) | b;
    }

    /// A key of two parts.
    static final class Key {
        private final Object a;
        private final Object b;

        Key(Object a, Object b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Key)) {
                return false;
            }
            Key k = (Key) o;
            return a.equals(k.a) && b.equals(k.b);
        }

        @Override
        public int hashCode() {
            return a.hashCode() * 31 + b.hashCode();
        }
    }

    // --------------------------------------------------------------- the screen

    /// Paints in pixels: points times `scale` from (`ox`, `oy`).
    private final class ScreenPainter implements SlidePainter {
        private final Graphics g;
        private final float ox;
        private final float oy;
        private final float scale;
        private final List<Object[]> stack = new ArrayList<Object[]>();
        private float alpha = 1;

        ScreenPainter(Graphics g, int ox, int oy, float scale) {
            this.g = g;
            this.ox = ox;
            this.oy = oy;
            this.scale = scale;
        }

        private int px(float x) {
            return Math.round(ox + x * scale);
        }

        private int py(float y) {
            return Math.round(oy + y * scale);
        }

        private Shape pixels(GeneralPath path) {
            Transform t = Transform.makeTranslation(ox, oy);
            t.scale(scale, scale);
            return path.createTransformedShape(t);
        }

        public Measurer measurer() {
            return measurer;
        }

        public void push(float cx, float cy, float rotation, float dx, float dy, float s,
                float a, float[] clip) {
            Transform before = null;
            boolean transformed = (rotation != 0 || dx != 0 || dy != 0 || s != 1)
                    && g.isTransformSupported();
            if (transformed) {
                before = g.getTransform();
                Transform t = before.copy();
                float pcx = ox + cx * scale;
                float pcy = oy + cy * scale;
                t.translate(pcx + dx * scale, pcy + dy * scale);
                if (rotation != 0) {
                    t.rotate((float) Math.toRadians(rotation), 0, 0);
                }
                if (s != 1) {
                    t.scale(s, s);
                }
                t.translate(-pcx, -pcy);
                g.setTransform(t);
            }
            if (clip != null) {
                g.pushClip();
                g.clipRect(px(clip[0]), py(clip[1]), Math.max(0, Math.round(clip[2] * scale)),
                        Math.round(clip[3] * scale));
            }
            stack.add(new Object[] {before, Float.valueOf(alpha), Boolean.valueOf(clip != null),
                Boolean.valueOf(transformed)});
            alpha *= a;
        }

        public void pop() {
            Object[] top = stack.remove(stack.size() - 1);
            if (Boolean.TRUE.equals(top[2])) {
                g.popClip();
            }
            if (Boolean.TRUE.equals(top[3])) {
                g.setTransform((Transform) top[0]);
            }
            alpha = ((Float) top[1]).floatValue();
        }

        private void alpha(float a) {
            g.setAlpha(Math.max(0, Math.min(255, Math.round(255 * a * alpha))));
        }

        public void fill(GeneralPath path, int color, float a) {
            g.setColor(color);
            alpha(a);
            if (g.isShapeSupported()) {
                g.fillShape(pixels(path));
            } else {
                float[] b = path.getBounds2D();
                g.fillRect(px(b[0]), py(b[1]), Math.round(b[2] * scale),
                        Math.round(b[3] * scale));
            }
        }

        public void gradient(GeneralPath path, int from, int to, float angle, float a, float x,
                float y, float w, float h) {
            if (!g.isShapeSupported()) {
                fill(path, mix(from, to, 0.5f), a);
                return;
            }
            float[] ends = gradientLine(x, y, w, h, angle);
            alpha(a);
            // core's own gradient paint, honoured by fillShape on every port that fills shapes
            g.setColor(new com.codename1.ui.LinearGradientPaint(ox + ends[0] * scale,
                    oy + ends[1] * scale, ox + ends[2] * scale, oy + ends[3] * scale,
                    new float[] {0, 1}, new int[] {0xff000000 | from, 0xff000000 | to},
                    com.codename1.ui.MultipleGradientPaint.CycleMethod.NO_CYCLE,
                    com.codename1.ui.MultipleGradientPaint.ColorSpaceType.SRGB, null));
            g.fillShape(pixels(path));
            g.setColor(from);
        }

        public void stroke(GeneralPath path, LineStyle line) {
            g.setColor(line.color);
            alpha(line.alpha);
            float wpx = Math.max(1, line.width * scale);
            if (!g.isShapeSupported()) {
                float[] b = path.getBounds2D();
                g.drawRect(px(b[0]), py(b[1]), Math.round(b[2] * scale),
                        Math.round(b[3] * scale));
                return;
            }
            Stroke st = new Stroke(wpx, Stroke.CAP_ROUND, Stroke.JOIN_ROUND, 4);
            if (LineStyle.SOLID.equals(line.dash)) {
                g.drawShape(pixels(path), st);
                return;
            }
            // the screen has no dash pattern: walk the straight segments of the path
            float on = LineStyle.DOT.equals(line.dash) ? line.width : line.width * 4;
            float off = line.width * (LineStyle.DOT.equals(line.dash) ? 2 : 3);
            g.drawShape(pixels(dashed(path, Math.max(1, on), Math.max(1, off))), st);
        }

        public void picture(Picture p, float x, float y, float w, float h, float cl, float ct,
                float cr, float cb, boolean flipH, boolean flipV, float a) {
            Image img = painter.image(p);
            if (img == null) {
                g.setColor(0x9aa3ad);
                alpha(1);
                g.drawRect(px(x), py(y), Math.round(w * scale) - 1, Math.round(h * scale) - 1);
                return;
            }
            if (flipH || flipV) {
                img = flipH ? img.flipHorizontally(false) : img;
                img = flipV ? img.flipVertically(false) : img;
            }
            alpha(a);
            float fw = w / Math.max(0.01f, 1 - cl - cr);
            float fh = h / Math.max(0.01f, 1 - ct - cb);
            g.pushClip();
            g.clipRect(px(x), py(y), Math.round(w * scale), Math.round(h * scale));
            g.drawImage(img, px(x - cl * fw), py(y - ct * fh), Math.round(fw * scale),
                    Math.round(fh * scale));
            g.popClip();
        }

        public void text(List<LayoutItem> items, Measurer m, float x, float y, float s) {
            alpha(1);
            LayoutPainter lp = m == measurer ? painter : new LayoutPainter(m);
            lp.paint(g, items, px(x), py(y), scale * s);
        }

        public void chart(ChartElement c, float x, float y, float w, float h) {
            int pw = Math.max(1, Math.round(w * scale));
            int ph = Math.max(1, Math.round(h * scale));
            Key k = new Key(c, new Key(Integer.valueOf(pw), Integer.valueOf(ph)));
            Object o = cache.get(k);
            Image img;
            if (o instanceof Image) {
                img = (Image) o;
            } else {
                img = chartImage(c, pw, ph);
                cache.put(k, img);
            }
            alpha(1);
            g.drawImage(img, px(x), py(y), pw, ph);
        }
    }

    /// A chart drawn by `com.codename1.ui.chart.Chart` into an image, settled at the end of
    /// its entrance animation.
    static Image chartImage(ChartElement c, int w, int h) {
        com.codename1.ui.chart.Chart chart = c.toChart();
        chart.setAnimationDuration(0);
        return chart.toImage(w, h);
    }

    /// The straight segments of a path cut into dashes. Curves are flattened to their end
    /// points, which is close enough for the dashed outlines slides use.
    static GeneralPath dashed(GeneralPath path, float on, float off) {
        GeneralPath out = new GeneralPath();
        com.codename1.ui.geom.PathIterator it = path.getPathIterator();
        float[] c = new float[6];
        float sx = 0;
        float sy = 0;
        float lx = 0;
        float ly = 0;
        float phase = 0;
        while (!it.isDone()) {
            int type = it.currentSegment(c);
            float nx;
            float ny;
            if (type == com.codename1.ui.geom.PathIterator.SEG_MOVETO) {
                sx = c[0];
                sy = c[1];
                lx = sx;
                ly = sy;
                it.next();
                continue;
            } else if (type == com.codename1.ui.geom.PathIterator.SEG_CLOSE) {
                nx = sx;
                ny = sy;
            } else if (type == com.codename1.ui.geom.PathIterator.SEG_QUADTO) {
                nx = c[2];
                ny = c[3];
            } else if (type == com.codename1.ui.geom.PathIterator.SEG_CUBICTO) {
                nx = c[4];
                ny = c[5];
            } else {
                nx = c[0];
                ny = c[1];
            }
            float len = (float) Math.sqrt((nx - lx) * (nx - lx) + (ny - ly) * (ny - ly));
            float pos = 0;
            while (pos < len && len > 0) {
                float period = on + off;
                float inPeriod = (phase + pos) % period;
                float step;
                if (inPeriod < on) {
                    step = Math.min(on - inPeriod, len - pos);
                    float t0 = pos / len;
                    float t1 = (pos + step) / len;
                    out.moveTo(lx + (nx - lx) * t0, ly + (ny - ly) * t0);
                    out.lineTo(lx + (nx - lx) * t1, ly + (ny - ly) * t1);
                } else {
                    step = Math.min(period - inPeriod, len - pos);
                }
                pos += step;
            }
            phase += len;
            lx = nx;
            ly = ny;
            it.next();
        }
        return out;
    }
}
