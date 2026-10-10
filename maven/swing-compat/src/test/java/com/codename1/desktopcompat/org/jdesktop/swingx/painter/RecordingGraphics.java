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
package com.codename1.desktopcompat.org.jdesktop.swingx.painter;

import com.codename1.desktopcompat.java.awt.AlphaComposite;
import com.codename1.desktopcompat.java.awt.BasicStroke;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Composite;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.FontMetrics;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Graphics2D;
import com.codename1.desktopcompat.java.awt.Image;
import com.codename1.desktopcompat.java.awt.Paint;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.RenderingHints;
import com.codename1.desktopcompat.java.awt.Shape;
import com.codename1.desktopcompat.java.awt.Stroke;
import com.codename1.desktopcompat.java.awt.geom.AffineTransform;
import com.codename1.desktopcompat.java.awt.geom.Point2D;
import com.codename1.desktopcompat.java.awt.geom.Rectangle2D;
import com.codename1.desktopcompat.java.awt.image.ImageObserver;
import com.codename1.desktopcompat.rt.Fonts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A graphics that draws nothing and remembers what it was asked to draw:
/// every operation with the paint, alpha, stroke and transform it was
/// drawn under. Copies made with `create()` write to the same list.
public class RecordingGraphics extends Graphics2D {

    /// One drawing operation.
    public static final class Op {
        /// `fill`, `draw`, `text` or `image`.
        public final String kind;
        /// The shape filled or drawn, in the user space of the moment.
        public final Shape shape;
        public final String text;
        public final Paint paint;
        public final float alpha;
        public final Stroke stroke;
        public final AffineTransform transform;
        /// The bounds of the operation on the device: the shape's, through
        /// the transform.
        public final Rectangle2D bounds;

        Op(String kind, Shape shape, String text, RecordingGraphics g) {
            this.kind = kind;
            this.shape = shape;
            this.text = text;
            this.paint = g.paint;
            this.alpha = g.composite instanceof AlphaComposite ? ((AlphaComposite) g.composite).getAlpha() : 1f;
            this.stroke = g.stroke;
            this.transform = new AffineTransform(g.transform);
            this.bounds = shape == null ? null : g.transform.createTransformedShape(shape).getBounds2D();
        }

        /// The color of the operation, or `null` for another paint.
        public Color color() {
            return paint instanceof Color ? (Color) paint : null;
        }
    }

    public final List<Op> ops;
    private final Map<Object, Object> hints;
    private Paint paint = Color.BLACK;
    private Color background = Color.WHITE;
    private Composite composite = AlphaComposite.SrcOver;
    private Stroke stroke = new BasicStroke();
    private Font font = Fonts.defaultFont();
    private AffineTransform transform = new AffineTransform();
    private Shape clip;

    public RecordingGraphics() {
        ops = new ArrayList<Op>();
        hints = new HashMap<Object, Object>();
    }

    private RecordingGraphics(RecordingGraphics from) {
        ops = from.ops;
        hints = new HashMap<Object, Object>(from.hints);
        paint = from.paint;
        background = from.background;
        composite = from.composite;
        stroke = from.stroke;
        font = from.font;
        transform = new AffineTransform(from.transform);
        clip = from.clip;
    }

    /// The operations of one kind, in the order they were made.
    public List<Op> of(String kind) {
        List<Op> out = new ArrayList<Op>();
        for (int i = 0; i < ops.size(); i++) {
            if (ops.get(i).kind.equals(kind)) {
                out.add(ops.get(i));
            }
        }
        return out;
    }

    private void record(String kind, Shape s, String text) {
        ops.add(new Op(kind, s, text, this));
    }

    @Override
    public Graphics create() {
        return new RecordingGraphics(this);
    }

    @Override
    public void dispose() {
    }

    @Override
    public void draw(Shape s) {
        record("draw", s, null);
    }

    @Override
    public void fill(Shape s) {
        record("fill", s, null);
    }

    @Override
    public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
        record("image", null, null);
        return true;
    }

    @Override
    public void drawString(String str, int x, int y) {
        FontMetrics fm = getFontMetrics(font);
        record("text", new Rectangle(x, y - fm.getAscent(), fm.stringWidth(str), fm.getHeight()), str);
    }

    @Override
    public void drawString(String str, float x, float y) {
        drawString(str, (int) x, (int) y);
    }

    @Override
    public void setComposite(Composite comp) {
        composite = comp;
    }

    @Override
    public void setPaint(Paint p) {
        if (p != null) {
            paint = p;
        }
    }

    @Override
    public void setStroke(Stroke s) {
        stroke = s;
    }

    @Override
    public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
        hints.put(hintKey, hintValue);
    }

    @Override
    public Object getRenderingHint(RenderingHints.Key hintKey) {
        return hints.get(hintKey);
    }

    @Override
    public void setRenderingHints(Map<?, ?> h) {
        hints.clear();
        hints.putAll(h);
    }

    @Override
    public void addRenderingHints(Map<?, ?> h) {
        hints.putAll(h);
    }

    @Override
    public RenderingHints getRenderingHints() {
        return null;
    }

    @Override
    public void translate(int x, int y) {
        transform.translate(x, y);
    }

    @Override
    public void translate(double tx, double ty) {
        transform.translate(tx, ty);
    }

    @Override
    public void rotate(double theta) {
        transform.rotate(theta);
    }

    @Override
    public void rotate(double theta, double x, double y) {
        transform.rotate(theta, x, y);
    }

    @Override
    public void scale(double sx, double sy) {
        transform.scale(sx, sy);
    }

    @Override
    public void shear(double shx, double shy) {
        transform.shear(shx, shy);
    }

    @Override
    public void transform(AffineTransform tx) {
        transform.concatenate(tx);
    }

    @Override
    public void setTransform(AffineTransform tx) {
        transform = tx == null ? new AffineTransform() : new AffineTransform(tx);
    }

    @Override
    public AffineTransform getTransform() {
        return new AffineTransform(transform);
    }

    @Override
    public Paint getPaint() {
        return paint;
    }

    @Override
    public Composite getComposite() {
        return composite;
    }

    @Override
    public void setBackground(Color color) {
        background = color;
    }

    @Override
    public Color getBackground() {
        return background;
    }

    @Override
    public Stroke getStroke() {
        return stroke;
    }

    @Override
    public void clip(Shape s) {
        clip = s;
    }

    @Override
    public Color getColor() {
        return paint instanceof Color ? (Color) paint : Color.BLACK;
    }

    @Override
    public void setColor(Color c) {
        if (c != null) {
            paint = c;
        }
    }

    @Override
    public void setPaintMode() {
    }

    @Override
    public void setXORMode(Color c1) {
    }

    @Override
    public Font getFont() {
        return font;
    }

    @Override
    public void setFont(Font f) {
        if (f != null) {
            font = f;
        }
    }

    @Override
    public FontMetrics getFontMetrics(Font f) {
        return Fonts.metrics(f);
    }

    @Override
    public Rectangle getClipBounds() {
        return clip == null ? null : clip.getBounds();
    }

    @Override
    public void clipRect(int x, int y, int width, int height) {
        clip = new Rectangle(x, y, width, height);
    }

    @Override
    public void setClip(int x, int y, int width, int height) {
        clip = new Rectangle(x, y, width, height);
    }

    @Override
    public Shape getClip() {
        return clip;
    }

    @Override
    public void setClip(Shape c) {
        clip = c;
    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        record("draw", new Rectangle(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1)),
                null);
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        record("fill", new Rectangle(x, y, width, height), null);
    }

    @Override
    public void clearRect(int x, int y, int width, int height) {
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        record("draw", new Rectangle(x, y, width, height), null);
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        record("fill", new Rectangle(x, y, width, height), null);
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        record("draw", new Rectangle(x, y, width, height), null);
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
        record("fill", new Rectangle(x, y, width, height), null);
    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        record("draw", new Rectangle(x, y, width, height), null);
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        record("fill", new Rectangle(x, y, width, height), null);
    }

    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        record("draw", null, null);
    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        record("draw", null, null);
    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        record("fill", null, null);
    }

    private boolean image(int x, int y, int w, int h) {
        record("image", new Rectangle(x, y, w, h), null);
        return true;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        return image(x, y, img.getWidth(null), img.getHeight(null));
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
        return image(x, y, width, height);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
        return image(x, y, img.getWidth(null), img.getHeight(null));
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor,
                             ImageObserver observer) {
        return image(x, y, width, height);
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2,
                             int sy2, ImageObserver observer) {
        return image(dx1, dy1, dx2 - dx1, dy2 - dy1);
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2,
                             int sy2, Color bgcolor, ImageObserver observer) {
        return image(dx1, dy1, dx2 - dx1, dy2 - dy1);
    }

    /// The device position of a user-space point under the current
    /// transform.
    public Point2D onDevice(double x, double y) {
        return transform.transform(new Point2D.Double(x, y), null);
    }
}
