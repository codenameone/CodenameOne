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

import com.codename1.desktopcompat.java.awt.AlphaComposite;
import com.codename1.desktopcompat.java.awt.BasicStroke;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Composite;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.FontMetrics;
import com.codename1.desktopcompat.java.awt.GradientPaint;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Graphics2D;
import com.codename1.desktopcompat.java.awt.Image;
import com.codename1.desktopcompat.java.awt.Paint;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.RenderingHints;
import com.codename1.desktopcompat.java.awt.Shape;
import com.codename1.desktopcompat.java.awt.Stroke;
import com.codename1.desktopcompat.java.awt.geom.AffineTransform;
import com.codename1.desktopcompat.java.awt.geom.Arc2D;
import com.codename1.desktopcompat.java.awt.geom.Ellipse2D;
import com.codename1.desktopcompat.java.awt.geom.Line2D;
import com.codename1.desktopcompat.java.awt.geom.Path2D;
import com.codename1.desktopcompat.java.awt.geom.PathIterator;
import com.codename1.desktopcompat.java.awt.geom.Rectangle2D;
import com.codename1.desktopcompat.java.awt.geom.RoundRectangle2D;
import com.codename1.desktopcompat.java.awt.image.BufferedImage;
import com.codename1.desktopcompat.java.awt.image.ImageObserver;
import com.codename1.ui.LinearGradientPaint;
import com.codename1.ui.MultipleGradientPaint;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.GeneralPath;

import java.util.Map;

/// The layer's `Graphics2D`, drawing through a Codename One `Graphics`.
///
/// ## Coordinates
///
/// User space is logical pixels with the origin at the component's (or
/// image's) corner. The graphics keeps the whole mapping to the Codename One
/// context as one affine matrix -- the user transform, then the unit scale
/// ([Units]), then the component's position -- and applies it to geometry
/// *itself*: rectangles and straight lines are snapped to device pixels and
/// drawn with the port's primitives, every other shape is rebuilt as a path
/// in device coordinates. Nothing basic depends on the port's transform
/// support, so a translated or scaled drawing looks the same everywhere and
/// stays crisp at any density. Only what cannot be redrawn point by point
/// -- text and images under a rotation or shear -- uses the context's
/// transform, and falls back to drawing unrotated at the right place on a
/// port without one.
///
/// ## State
///
/// A Codename One context has one color, one clip and one font; AWT hands
/// out independent copies with `create()`. Each instance therefore keeps its
/// own state and writes it to the context before it draws, when another
/// instance drew last or something changed. [#finish()] puts the context
/// back the way [#forPeer] found it, which the paint cycle relies on.
///
/// ## What is refused
///
/// Composites other than source-over, paints other than a color or a
/// two-color gradient, XOR mode and `copyArea` throw
/// `UnsupportedOperationException`: drawing something else in their place
/// would be a bug nobody could see from the code.
public final class G2D extends Graphics2D {

    private static final BasicStroke DEFAULT_STROKE = new BasicStroke();

    /// What every graphics created from one root shares: the context, which
    /// of them wrote its state there last, and what to restore at the end.
    private static final class Target {
        final com.codename1.ui.Graphics g;
        final BufferedImage image;
        G2D active;
        int alpha;
        int clipX;
        int clipY;
        int clipW;
        int clipH;
        int color;
        com.codename1.ui.Font font;

        Target(com.codename1.ui.Graphics g, BufferedImage image) {
            this.g = g;
            this.image = image;
        }
    }

    private final Target target;
    private final com.codename1.ui.Graphics g;
    private final float originX;
    private final float originY;
    private final float unit;
    private final int baseX;
    private final int baseY;
    private final int baseW;
    private final int baseH;

    private AffineTransform tx = new AffineTransform();
    // Device coordinates: X = a*x + c*y + e, Y = b*x + d*y + f.
    private double a;
    private double b;
    private double c;
    private double d;
    private double e;
    private double f;
    /// Whether the matrix only scales and translates, without mirroring.
    private boolean upright;

    private Paint paint = Color.black;
    private Color color = Color.black;
    private Color background = Color.white;
    private Font font;
    private Stroke stroke = DEFAULT_STROKE;
    private AlphaComposite composite = AlphaComposite.SrcOver;
    private final RenderingHints hints = new RenderingHints(null);
    private int antialias = -1;
    private int textAntialias = -1;

    private int clipX;
    private int clipY;
    private int clipW;
    private int clipH;
    private GeneralPath clipShape;
    private boolean dirty = true;

    private G2D(Target target, float originX, float originY, float unit, int bx, int by, int bw, int bh) {
        this.target = target;
        this.g = target.g;
        this.originX = originX;
        this.originY = originY;
        this.unit = unit;
        this.baseX = bx;
        this.baseY = by;
        this.baseW = bw;
        this.baseH = bh;
        this.clipX = bx;
        this.clipY = by;
        this.clipW = bw;
        this.clipH = bh;
        matrixChanged();
    }

    private G2D(G2D o) {
        this.target = o.target;
        this.g = o.g;
        this.originX = o.originX;
        this.originY = o.originY;
        this.unit = o.unit;
        this.baseX = o.baseX;
        this.baseY = o.baseY;
        this.baseW = o.baseW;
        this.baseH = o.baseH;
        this.tx = new AffineTransform(o.tx);
        this.paint = o.paint;
        this.color = o.color;
        this.background = o.background;
        this.font = o.font;
        this.stroke = o.stroke;
        this.composite = o.composite;
        this.hints.add(o.hints);
        this.antialias = o.antialias;
        this.textAntialias = o.textAntialias;
        this.clipX = o.clipX;
        this.clipY = o.clipY;
        this.clipW = o.clipW;
        this.clipH = o.clipH;
        this.clipShape = o.clipShape;
        matrixChanged();
    }

    /// A graphics for a component whose peer Codename One is painting:
    /// `g` as the peer's `paint` received it and the peer's bounds in that
    /// context. Pair with [#finish()].
    public static G2D forPeer(com.codename1.ui.Graphics g, int x, int y, int width, int height) {
        Target t = new Target(g, null);
        t.alpha = g.getAlpha();
        t.clipX = g.getClipX();
        t.clipY = g.getClipY();
        t.clipW = g.getClipWidth();
        t.clipH = g.getClipHeight();
        t.color = g.getColor();
        t.font = g.getFont();
        int x1 = Math.max(x, t.clipX);
        int y1 = Math.max(y, t.clipY);
        int x2 = Math.min(x + width, t.clipX + t.clipW);
        int y2 = Math.min(y + height, t.clipY + t.clipH);
        return new G2D(t, x, y, Units.scale(), x1, y1, Math.max(0, x2 - x1), Math.max(0, y2 - y1));
    }

    /// A graphics that draws into a buffered image through `g`, the graphics
    /// of the image's Codename One surface. Image pixels are user units.
    public static G2D forImage(BufferedImage image, com.codename1.ui.Graphics g, int width, int height) {
        Target t = new Target(g, image);
        t.alpha = 255;
        t.clipW = width;
        t.clipH = height;
        t.color = g.getColor();
        t.font = g.getFont();
        return new G2D(t, 0, 0, 1f, 0, 0, width, height);
    }

    /// Restores the context to the clip, color, alpha and font it had when
    /// this graphics was made for a peer.
    public void finish() {
        g.setClip(target.clipX, target.clipY, target.clipW, target.clipH);
        g.setColor(target.color);
        g.setAlpha(target.alpha);
        if (target.font != null) {
            g.setFont(target.font);
        }
        target.active = null;
    }

    /// The Codename One context this graphics draws through.
    public com.codename1.ui.Graphics nativeGraphics() {
        return g;
    }

    // ------------------------------------------------------------ mapping

    private void matrixChanged() {
        a = unit * tx.getScaleX();
        c = unit * tx.getShearX();
        e = unit * tx.getTranslateX() + originX;
        b = unit * tx.getShearY();
        d = unit * tx.getScaleY();
        f = unit * tx.getTranslateY() + originY;
        upright = b == 0 && c == 0 && a > 0 && d > 0;
    }

    /// The matrix from user space to the context's coordinates as
    /// `{m00, m10, m01, m11, m02, m12}`, the order of `AffineTransform`'s
    /// constructor.
    public double[] deviceMatrix() {
        return new double[]{a, b, c, d, e, f};
    }

    private double mapX(double x, double y) {
        return a * x + c * y + e;
    }

    private double mapY(double x, double y) {
        return b * x + d * y + f;
    }

    /// How many device pixels a user unit of length covers.
    private double lineScale() {
        return Math.sqrt(Math.abs(a * d - b * c));
    }

    /// The device rectangle `{x, y, w, h}` a user rectangle snaps to: its
    /// edges rounded one by one when upright, else the bounds of its four
    /// corners.
    public int[] deviceRect(double x, double y, double w, double h) {
        if (upright) {
            int x1 = (int) Math.round(a * x + e);
            int y1 = (int) Math.round(d * y + f);
            int x2 = (int) Math.round(a * (x + w) + e);
            int y2 = (int) Math.round(d * (y + h) + f);
            return new int[]{x1, y1, x2 - x1, y2 - y1};
        }
        double[] xs = {mapX(x, y), mapX(x + w, y), mapX(x, y + h), mapX(x + w, y + h)};
        double[] ys = {mapY(x, y), mapY(x + w, y), mapY(x, y + h), mapY(x + w, y + h)};
        double minX = xs[0];
        double maxX = xs[0];
        double minY = ys[0];
        double maxY = ys[0];
        for (int i = 1; i < 4; i++) {
            minX = Math.min(minX, xs[i]);
            maxX = Math.max(maxX, xs[i]);
            minY = Math.min(minY, ys[i]);
            maxY = Math.max(maxY, ys[i]);
        }
        int x1 = (int) Math.floor(minX);
        int y1 = (int) Math.floor(minY);
        return new int[]{x1, y1, (int) Math.ceil(maxX) - x1, (int) Math.ceil(maxY) - y1};
    }

    /// The shape in device coordinates, every point moved by `offset` user
    /// units first (half a pixel for the integer outline calls, whose
    /// coordinates name pixels rather than the lines between them).
    private GeneralPath devicePath(Shape s, double offset) {
        GeneralPath p = new GeneralPath();
        PathIterator it = s.getPathIterator(null);
        p.setWindingRule(it.getWindingRule() == PathIterator.WIND_EVEN_ODD
                ? com.codename1.ui.geom.PathIterator.WIND_EVEN_ODD
                : com.codename1.ui.geom.PathIterator.WIND_NON_ZERO);
        double[] k = new double[6];
        while (!it.isDone()) {
            int type = it.currentSegment(k);
            for (int i = 0; i < 6; i += 2) {
                double x = k[i] + offset;
                double y = k[i + 1] + offset;
                k[i] = mapX(x, y);
                k[i + 1] = mapY(x, y);
            }
            switch (type) {
                case PathIterator.SEG_MOVETO:
                    p.moveTo(k[0], k[1]);
                    break;
                case PathIterator.SEG_LINETO:
                    p.lineTo(k[0], k[1]);
                    break;
                case PathIterator.SEG_QUADTO:
                    p.quadTo(k[0], k[1], k[2], k[3]);
                    break;
                case PathIterator.SEG_CUBICTO:
                    p.curveTo(k[0], k[1], k[2], k[3], k[4], k[5]);
                    break;
                default:
                    p.closePath();
                    break;
            }
            it.next();
        }
        return p;
    }

    // ------------------------------------------------------------ state

    private int alphaOf(Color col) {
        return Math.round(target.alpha * (col.getAlpha() / 255f) * composite.getAlpha());
    }

    private void applyColor(Color col) {
        g.setColor(col.getRGB() & 0xffffff);
        g.setAlpha(alphaOf(col));
    }

    /// Makes the context carry this graphics' state. Called before every
    /// draw; cheap when this graphics drew last and nothing changed.
    private void sync() {
        if (target.image != null) {
            target.image.cn1Drawing();
        }
        if (target.active == this && !dirty) {
            return;
        }
        target.active = this;
        dirty = false;
        if (clipShape != null) {
            g.setClip(clipShape);
            g.clipRect(clipX, clipY, clipW, clipH);
        } else {
            g.setClip(clipX, clipY, clipW, clipH);
        }
        applyColor(color);
        if (antialias >= 0) {
            g.setAntiAliased(antialias == 1);
        }
        if (textAntialias >= 0) {
            g.setAntiAliasedText(textAntialias == 1);
        }
    }

    @Override
    public Graphics create() {
        return new G2D(this);
    }

    @Override
    public void dispose() {
        if (target.active == this) {
            target.active = null;
        }
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public void setColor(Color col) {
        if (col == null) {
            return;
        }
        color = col;
        paint = col;
        dirty = true;
    }

    @Override
    public Paint getPaint() {
        return paint;
    }

    /// Accepts a [Color] or a [GradientPaint]; null is ignored as in the JDK.
    @Override
    public void setPaint(Paint p) {
        if (p == null) {
            return;
        }
        if (p instanceof Color) {
            setColor((Color) p);
        } else if (p instanceof GradientPaint) {
            paint = p;
        } else {
            throw new UnsupportedOperationException("Unsupported paint " + p.getClass().getName()
                    + ": only Color and GradientPaint can be drawn");
        }
    }

    @Override
    public void setPaintMode() {
    }

    @Override
    public void setXORMode(Color c1) {
        throw new UnsupportedOperationException("XOR painting is not supported");
    }

    @Override
    public Composite getComposite() {
        return composite;
    }

    /// Accepts `AlphaComposite` in the `SRC_OVER` rule with any alpha, and
    /// `SRC` at full alpha, which differs from it only for translucent
    /// colors.
    @Override
    public void setComposite(Composite comp) {
        if (comp == null) {
            throw new IllegalArgumentException("null Composite");
        }
        if (comp instanceof AlphaComposite) {
            AlphaComposite ac = (AlphaComposite) comp;
            if (ac.getRule() == AlphaComposite.SRC_OVER
                    || ac.getRule() == AlphaComposite.SRC && ac.getAlpha() >= 1f) {
                composite = ac;
                dirty = true;
                return;
            }
        }
        throw new UnsupportedOperationException(
                "Unsupported composite: only AlphaComposite.SRC_OVER can be drawn with");
    }

    @Override
    public Stroke getStroke() {
        return stroke;
    }

    @Override
    public void setStroke(Stroke s) {
        if (s == null) {
            throw new IllegalArgumentException("null Stroke");
        }
        stroke = s;
    }

    @Override
    public void setBackground(Color col) {
        background = col;
    }

    @Override
    public Color getBackground() {
        return background;
    }

    @Override
    public Font getFont() {
        if (font == null) {
            font = Fonts.defaultFont();
        }
        return font;
    }

    @Override
    public void setFont(Font fnt) {
        if (fnt != null) {
            font = fnt;
        }
    }

    @Override
    public FontMetrics getFontMetrics(Font fnt) {
        return Fonts.metrics(fnt);
    }

    @Override
    public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
        hints.put(hintKey, hintValue);
        if (hintKey == RenderingHints.KEY_ANTIALIASING) {
            antialias = hintValue == RenderingHints.VALUE_ANTIALIAS_ON ? 1
                    : hintValue == RenderingHints.VALUE_ANTIALIAS_OFF ? 0 : -1;
            dirty = true;
        } else if (hintKey == RenderingHints.KEY_TEXT_ANTIALIASING) {
            textAntialias = hintValue == RenderingHints.VALUE_TEXT_ANTIALIAS_OFF ? 0
                    : hintValue == RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT ? -1 : 1;
            dirty = true;
        }
    }

    @Override
    public Object getRenderingHint(RenderingHints.Key hintKey) {
        return hints.get(hintKey);
    }

    @Override
    public void setRenderingHints(Map<?, ?> newHints) {
        hints.clear();
        antialias = -1;
        textAntialias = -1;
        addRenderingHints(newHints);
    }

    @Override
    public void addRenderingHints(Map<?, ?> newHints) {
        for (Map.Entry<?, ?> en : newHints.entrySet()) {
            Object key = en.getKey();
            if (key instanceof RenderingHints.Key) {
                setRenderingHint((RenderingHints.Key) key, en.getValue());
            }
        }
    }

    @Override
    public RenderingHints getRenderingHints() {
        RenderingHints copy = new RenderingHints(null);
        copy.add(hints);
        return copy;
    }

    // ------------------------------------------------------------ transform

    @Override
    public void translate(int x, int y) {
        tx.translate(x, y);
        matrixChanged();
    }

    @Override
    public void translate(double x, double y) {
        tx.translate(x, y);
        matrixChanged();
    }

    @Override
    public void rotate(double theta) {
        tx.rotate(theta);
        matrixChanged();
    }

    @Override
    public void rotate(double theta, double x, double y) {
        tx.rotate(theta, x, y);
        matrixChanged();
    }

    @Override
    public void scale(double sx, double sy) {
        tx.scale(sx, sy);
        matrixChanged();
    }

    @Override
    public void shear(double shx, double shy) {
        tx.shear(shx, shy);
        matrixChanged();
    }

    @Override
    public void transform(AffineTransform t) {
        tx.concatenate(t);
        matrixChanged();
    }

    /// Replaces the user transform. Identity is the component's own corner,
    /// so the transform [#getTransform()] answered earlier restores it.
    @Override
    public void setTransform(AffineTransform t) {
        tx = t == null ? new AffineTransform() : new AffineTransform(t);
        matrixChanged();
    }

    @Override
    public AffineTransform getTransform() {
        return new AffineTransform(tx);
    }

    // ------------------------------------------------------------ clip

    private void intersectClip(int[] r, int x, int y, int w, int h) {
        int x1 = Math.max(r[0], x);
        int y1 = Math.max(r[1], y);
        int x2 = Math.min(r[0] + r[2], x + w);
        int y2 = Math.min(r[1] + r[3], y + h);
        clipX = x1;
        clipY = y1;
        clipW = Math.max(0, x2 - x1);
        clipH = Math.max(0, y2 - y1);
        dirty = true;
    }

    private GeneralPath shapeClip(Shape s) {
        return g.isShapeClipSupported() ? devicePath(s, 0) : null;
    }

    private void clipTo(double x, double y, double w, double h, boolean replace) {
        int[] r = deviceRect(x, y, Math.max(0, w), Math.max(0, h));
        GeneralPath shape = upright ? null : shapeClip(new Rectangle2D.Double(x, y, w, h));
        if (replace) {
            intersectClip(r, baseX, baseY, baseW, baseH);
            clipShape = shape;
        } else {
            intersectClip(r, clipX, clipY, clipW, clipH);
            if (shape != null) {
                clipShape = shape;
            }
        }
    }

    @Override
    public void clipRect(int x, int y, int width, int height) {
        clipTo(x, y, width, height, false);
    }

    @Override
    public void setClip(int x, int y, int width, int height) {
        clipTo(x, y, width, height, true);
    }

    /// Replaces the clip, never beyond the area the graphics was created
    /// for; null goes back to that area. A shape that is not a rectangle
    /// clips exactly where the port clips to shapes and to its bounds
    /// elsewhere.
    @Override
    public void setClip(Shape clip) {
        clipToShape(clip, true);
    }

    /// Narrows the clip to its intersection with the shape. When both the
    /// current clip and the new one are shapes rather than rectangles, the
    /// result is the new shape within the bounds of the old.
    @Override
    public void clip(Shape s) {
        if (s == null) {
            setClip(null);
            return;
        }
        clipToShape(s, false);
    }

    private void clipToShape(Shape s, boolean replace) {
        if (s == null) {
            clipX = baseX;
            clipY = baseY;
            clipW = baseW;
            clipH = baseH;
            clipShape = null;
            dirty = true;
            return;
        }
        if (s instanceof Rectangle2D) {
            Rectangle2D r = (Rectangle2D) s;
            clipTo(r.getX(), r.getY(), r.getWidth(), r.getHeight(), replace);
            return;
        }
        Rectangle2D bounds = s.getBounds2D();
        int[] r = deviceRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
        if (replace) {
            intersectClip(r, baseX, baseY, baseW, baseH);
        } else {
            intersectClip(r, clipX, clipY, clipW, clipH);
        }
        GeneralPath shape = shapeClip(s);
        if (shape != null || replace) {
            clipShape = shape;
        }
    }

    /// The clip as a rectangle in user space: exact for a rectangular clip
    /// under an upright transform, the bounding box otherwise.
    @Override
    public Shape getClip() {
        return getClipBounds();
    }

    @Override
    public Rectangle getClipBounds() {
        if (upright) {
            // A coordinate that was snapped to a device pixel comes back as
            // the logical pixel it was, not its neighbor.
            double slackX = 0.5 / a + 1e-6;
            double slackY = 0.5 / d + 1e-6;
            int x1 = (int) Math.floor((clipX - e) / a + slackX);
            int y1 = (int) Math.floor((clipY - f) / d + slackY);
            int x2 = (int) Math.ceil((clipX + clipW - e) / a - slackX);
            int y2 = (int) Math.ceil((clipY + clipH - f) / d - slackY);
            return new Rectangle(x1, y1, Math.max(0, x2 - x1), Math.max(0, y2 - y1));
        }
        double det = a * d - b * c;
        if (det == 0) {
            return new Rectangle();
        }
        double minX = 0;
        double minY = 0;
        double maxX = 0;
        double maxY = 0;
        for (int i = 0; i < 4; i++) {
            double px = clipX + ((i & 1) == 0 ? 0 : clipW) - e;
            double py = clipY + ((i & 2) == 0 ? 0 : clipH) - f;
            double ux = (d * px - c * py) / det;
            double uy = (a * py - b * px) / det;
            if (i == 0) {
                minX = ux;
                maxX = ux;
                minY = uy;
                maxY = uy;
            } else {
                minX = Math.min(minX, ux);
                maxX = Math.max(maxX, ux);
                minY = Math.min(minY, uy);
                maxY = Math.max(maxY, uy);
            }
        }
        int x1 = (int) Math.floor(minX);
        int y1 = (int) Math.floor(minY);
        return new Rectangle(x1, y1, (int) Math.ceil(maxX) - x1, (int) Math.ceil(maxY) - y1);
    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        throw new UnsupportedOperationException("copyArea is not supported");
    }

    // ------------------------------------------------------------ fills

    private boolean solid() {
        return paint instanceof Color;
    }

    private void fillSnapped(double x, double y, double w, double h) {
        int[] r = deviceRect(x, y, w, h);
        g.fillRect(r[0], r[1], Math.max(1, r[2]), Math.max(1, r[3]));
    }

    /// Fills a rectangle with the gradient through the port's two-color
    /// fill when the gradient runs straight across exactly that rectangle;
    /// false when it does not and the general path must draw it.
    private boolean fillAxisGradient(GradientPaint gp, double x, double y, double w, double h) {
        if (!upright || gp.isCyclic()) {
            return false;
        }
        double x1 = gp.getPoint1().getX();
        double y1 = gp.getPoint1().getY();
        double x2 = gp.getPoint2().getX();
        double y2 = gp.getPoint2().getY();
        boolean horizontal = y1 == y2 && x1 != x2;
        boolean vertical = x1 == x2 && y1 != y2;
        if (!horizontal && !vertical) {
            return false;
        }
        double from = horizontal ? Math.min(x1, x2) : Math.min(y1, y2);
        double to = horizontal ? Math.max(x1, x2) : Math.max(y1, y2);
        double start = horizontal ? x : y;
        double end = horizontal ? x + w : y + h;
        if (Math.abs(from - start) > 1 || Math.abs(to - end) > 1) {
            return false;
        }
        boolean forward = horizontal ? x1 < x2 : y1 < y2;
        Color first = forward ? gp.getColor1() : gp.getColor2();
        Color second = forward ? gp.getColor2() : gp.getColor1();
        int[] r = deviceRect(x, y, w, h);
        g.setAlpha(Math.round(target.alpha * composite.getAlpha()
                * Math.min(first.getAlpha(), second.getAlpha()) / 255f));
        g.fillLinearGradient(first.getRGB() & 0xffffff, second.getRGB() & 0xffffff, r[0], r[1], r[2], r[3],
                horizontal);
        dirty = true;
        return true;
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (upright && solid()) {
            sync();
            fillSnapped(x, y, width, height);
        } else {
            fill(new Rectangle2D.Double(x, y, width, height));
        }
    }

    /// Fills with the background color. On an image the area is erased
    /// first, so a translucent background replaces what was there.
    @Override
    public void clearRect(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        sync();
        if (upright && target.image != null && background.getAlpha() < 255) {
            int[] r = deviceRect(x, y, width, height);
            g.clearRect(r[0], r[1], r[2], r[3]);
        }
        applyColor(background);
        dirty = true;
        if (upright) {
            fillSnapped(x, y, width, height);
        } else if (g.isShapeSupported()) {
            g.fillShape(devicePath(new Rectangle2D.Double(x, y, width, height), 0));
        }
    }

    @Override
    public void fill(Shape s) {
        sync();
        if (s instanceof Rectangle2D) {
            Rectangle2D r = (Rectangle2D) s;
            if (upright && solid()) {
                if (r.getWidth() > 0 && r.getHeight() > 0) {
                    fillSnapped(r.getX(), r.getY(), r.getWidth(), r.getHeight());
                }
                return;
            }
            if (paint instanceof GradientPaint
                    && fillAxisGradient((GradientPaint) paint, r.getX(), r.getY(), r.getWidth(), r.getHeight())) {
                return;
            }
        }
        if (!g.isShapeSupported()) {
            fillFlattened(s);
            return;
        }
        GeneralPath p = devicePath(s, 0);
        if (paint instanceof GradientPaint) {
            GradientPaint gp = (GradientPaint) paint;
            double x1 = gp.getPoint1().getX();
            double y1 = gp.getPoint1().getY();
            double x2 = gp.getPoint2().getX();
            double y2 = gp.getPoint2().getY();
            g.setColor(new LinearGradientPaint(mapX(x1, y1), mapY(x1, y1), mapX(x2, y2), mapY(x2, y2),
                    new float[]{0f, 1f},
                    new int[]{gp.getColor1().getRGB() & 0xffffff, gp.getColor2().getRGB() & 0xffffff},
                    gp.isCyclic() ? MultipleGradientPaint.CycleMethod.REFLECT
                            : MultipleGradientPaint.CycleMethod.NO_CYCLE,
                    MultipleGradientPaint.ColorSpaceType.SRGB, null));
            g.setAlpha(Math.round(target.alpha * composite.getAlpha()
                    * Math.min(gp.getColor1().getAlpha(), gp.getColor2().getAlpha()) / 255f));
            g.fillShape(p);
            dirty = true;
        } else {
            g.fillShape(p);
        }
    }

    /// Fills each subpath as a polygon, for a port without shapes. Holes
    /// and curves finer than a device pixel are lost.
    private void fillFlattened(Shape s) {
        PathIterator it = s.getPathIterator(null, 0.5 / Math.max(0.01, lineScale()));
        int[] xs = new int[16];
        int[] ys = new int[16];
        int n = 0;
        double[] k = new double[6];
        while (true) {
            boolean done = it.isDone();
            int type = done ? PathIterator.SEG_MOVETO : it.currentSegment(k);
            if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_CLOSE) {
                if (n > 2) {
                    g.fillPolygon(xs, ys, n);
                }
                if (type == PathIterator.SEG_CLOSE || done) {
                    n = 0;
                }
            }
            if (done) {
                break;
            }
            if (type == PathIterator.SEG_MOVETO) {
                n = 0;
            }
            if (type != PathIterator.SEG_CLOSE) {
                if (n == xs.length) {
                    int[] nx = new int[n * 2];
                    int[] ny = new int[n * 2];
                    System.arraycopy(xs, 0, nx, 0, n);
                    System.arraycopy(ys, 0, ny, 0, n);
                    xs = nx;
                    ys = ny;
                }
                xs[n] = (int) Math.round(mapX(k[0], k[1]));
                ys[n] = (int) Math.round(mapY(k[0], k[1]));
                n++;
            }
            it.next();
        }
    }

    // ------------------------------------------------------------ strokes

    private BasicStroke plainStroke() {
        if (stroke instanceof BasicStroke) {
            BasicStroke bs = (BasicStroke) stroke;
            if (bs.getDashArray() == null) {
                return bs;
            }
        }
        return null;
    }

    /// Whether outlines are a single device pixel wide, which the port's
    /// own outline primitives draw exactly.
    private boolean hairline() {
        BasicStroke bs = plainStroke();
        return bs != null && solid() && upright && bs.getLineWidth() * lineScale() < 1.5;
    }

    @Override
    public void draw(Shape s) {
        stroke(s, 0);
    }

    private void stroke(Shape s, double offset) {
        if (!(stroke instanceof BasicStroke)) {
            fill(stroke.createStrokedShape(s));
            return;
        }
        sync();
        BasicStroke bs = (BasicStroke) stroke;
        if (paint instanceof GradientPaint) {
            applyColor(((GradientPaint) paint).getColor1());
            dirty = true;
        }
        Shape src = bs.getDashArray() == null ? s : dashed(s, bs);
        if (!g.isShapeSupported()) {
            drawFlattened(src, offset);
            return;
        }
        int cap = bs.getEndCap() == BasicStroke.CAP_ROUND ? com.codename1.ui.Stroke.CAP_ROUND
                : bs.getEndCap() == BasicStroke.CAP_SQUARE ? com.codename1.ui.Stroke.CAP_SQUARE
                : com.codename1.ui.Stroke.CAP_BUTT;
        int join = bs.getLineJoin() == BasicStroke.JOIN_ROUND ? com.codename1.ui.Stroke.JOIN_ROUND
                : bs.getLineJoin() == BasicStroke.JOIN_BEVEL ? com.codename1.ui.Stroke.JOIN_BEVEL
                : com.codename1.ui.Stroke.JOIN_MITER;
        float width = (float) Math.max(1, bs.getLineWidth() * lineScale());
        g.drawShape(devicePath(src, offset), new com.codename1.ui.Stroke(width, cap, join, bs.getMiterLimit()));
    }

    /// Draws the flattened outline segment by segment, one device pixel
    /// wide, for a port without shapes.
    private void drawFlattened(Shape s, double offset) {
        PathIterator it = s.getPathIterator(null, 0.5 / Math.max(0.01, lineScale()));
        double[] k = new double[6];
        int startX = 0;
        int startY = 0;
        int lastX = 0;
        int lastY = 0;
        while (!it.isDone()) {
            int type = it.currentSegment(k);
            if (type == PathIterator.SEG_CLOSE) {
                g.drawLine(lastX, lastY, startX, startY);
                lastX = startX;
                lastY = startY;
            } else {
                int x = (int) Math.round(mapX(k[0] + offset, k[1] + offset) - 0.5);
                int y = (int) Math.round(mapY(k[0] + offset, k[1] + offset) - 0.5);
                if (type == PathIterator.SEG_MOVETO) {
                    startX = x;
                    startY = y;
                } else {
                    g.drawLine(lastX, lastY, x, y);
                }
                lastX = x;
                lastY = y;
            }
            it.next();
        }
    }

    /// The dashes of `s` under `bs` as a path of separate segments: the
    /// outline is flattened and the pattern walked along it, starting
    /// `dashPhase` into the pattern at every subpath.
    static Path2D.Double dashed(Shape s, BasicStroke bs) {
        float[] dash = bs.getDashArray();
        Path2D.Double out = new Path2D.Double();
        PathIterator it = s.getPathIterator(null, 0.25);
        double[] k = new double[6];
        double startX = 0;
        double startY = 0;
        double lastX = 0;
        double lastY = 0;
        int index = 0;
        double left = 0;
        boolean on = true;
        boolean pen = false;
        while (!it.isDone()) {
            int type = it.currentSegment(k);
            double x = type == PathIterator.SEG_CLOSE ? startX : k[0];
            double y = type == PathIterator.SEG_CLOSE ? startY : k[1];
            if (type == PathIterator.SEG_MOVETO) {
                startX = x;
                startY = y;
                index = 0;
                on = true;
                left = dash[0];
                double phase = bs.getDashPhase();
                while (phase > 0) {
                    if (phase >= left) {
                        phase -= left;
                        index = (index + 1) % dash.length;
                        left = dash[index];
                        on = !on;
                    } else {
                        left -= phase;
                        phase = 0;
                    }
                }
                pen = false;
            } else {
                double dx = x - lastX;
                double dy = y - lastY;
                double length = Math.sqrt(dx * dx + dy * dy);
                double done = 0;
                while (length - done > 1e-9) {
                    double step = Math.min(left, length - done);
                    double t0 = done / length;
                    double t1 = (done + step) / length;
                    if (on && step > 0) {
                        if (!pen) {
                            out.moveTo(lastX + dx * t0, lastY + dy * t0);
                            pen = true;
                        }
                        out.lineTo(lastX + dx * t1, lastY + dy * t1);
                    }
                    done += step;
                    left -= step;
                    if (left <= 1e-9) {
                        index = (index + 1) % dash.length;
                        left = dash[index];
                        on = !on;
                        pen = false;
                    }
                }
            }
            lastX = x;
            lastY = y;
            it.next();
        }
        return out;
    }

    /// A line names the pixels it passes through, each one logical pixel
    /// square. Horizontal and vertical lines are filled as the snapped
    /// rectangle those pixels cover, so they are crisp and exactly as wide as
    /// the stroke at any density; the rest are stroked through pixel centers.
    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        BasicStroke bs = plainStroke();
        if (bs != null && upright && solid() && (x1 == x2 || y1 == y2) && bs.getEndCap() != BasicStroke.CAP_ROUND) {
            sync();
            double w = Math.max(bs.getLineWidth(), 1 / lineScale());
            double ext = bs.getEndCap() == BasicStroke.CAP_SQUARE ? w / 2 : 0;
            double half = w / 2;
            if (y1 == y2) {
                fillSnapped(Math.min(x1, x2) + 0.5 - ext, y1 + 0.5 - half, Math.abs(x2 - x1) + 2 * ext, w);
            } else {
                fillSnapped(x1 + 0.5 - half, Math.min(y1, y2) + 0.5 - ext, w, Math.abs(y2 - y1) + 2 * ext);
            }
            return;
        }
        stroke(new Line2D.Double(x1, y1, x2, y2), 0.5);
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        if (width < 0 || height < 0) {
            return;
        }
        if (hairline() || !g.isShapeSupported()) {
            sync();
            int[] r = deviceRect(x, y, width, height);
            g.drawRoundRect(r[0], r[1], r[2], r[3], (int) Math.round(arcWidth * a), (int) Math.round(arcHeight * d));
            return;
        }
        stroke(new RoundRectangle2D.Double(x, y, width, height, arcWidth, arcHeight), 0.5);
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (upright && solid()) {
            sync();
            int[] r = deviceRect(x, y, width, height);
            g.fillRoundRect(r[0], r[1], r[2], r[3], (int) Math.round(arcWidth * a), (int) Math.round(arcHeight * d));
            return;
        }
        fill(new RoundRectangle2D.Double(x, y, width, height, arcWidth, arcHeight));
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        if (width < 0 || height < 0) {
            return;
        }
        if (hairline() || !g.isShapeSupported()) {
            sync();
            int[] r = deviceRect(x, y, width, height);
            g.drawArc(r[0], r[1], r[2], r[3], 0, 360);
            return;
        }
        stroke(new Ellipse2D.Double(x, y, width, height), 0.5);
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (upright && solid()) {
            sync();
            int[] r = deviceRect(x, y, width, height);
            g.fillArc(r[0], r[1], r[2], r[3], 0, 360);
            return;
        }
        fill(new Ellipse2D.Double(x, y, width, height));
    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        if (width < 0 || height < 0) {
            return;
        }
        if (hairline() || !g.isShapeSupported()) {
            sync();
            int[] r = deviceRect(x, y, width, height);
            g.drawArc(r[0], r[1], r[2], r[3], startAngle, arcAngle);
            return;
        }
        stroke(new Arc2D.Double(x, y, width, height, startAngle, arcAngle, Arc2D.OPEN), 0.5);
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (upright && solid()) {
            sync();
            int[] r = deviceRect(x, y, width, height);
            g.fillArc(r[0], r[1], r[2], r[3], startAngle, arcAngle);
            return;
        }
        fill(new Arc2D.Double(x, y, width, height, startAngle, arcAngle, Arc2D.PIE));
    }

    private static Path2D.Double polygon(int[] xPoints, int[] yPoints, int nPoints, boolean close) {
        Path2D.Double p = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        for (int i = 0; i < nPoints; i++) {
            if (i == 0) {
                p.moveTo(xPoints[i], yPoints[i]);
            } else {
                p.lineTo(xPoints[i], yPoints[i]);
            }
        }
        if (close && nPoints > 0) {
            p.closePath();
        }
        return p;
    }

    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        if (nPoints > 1) {
            stroke(polygon(xPoints, yPoints, nPoints, false), 0.5);
        }
    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        if (nPoints > 1) {
            stroke(polygon(xPoints, yPoints, nPoints, true), 0.5);
        }
    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        if (nPoints > 2) {
            fill(polygon(xPoints, yPoints, nPoints, true));
        }
    }

    // ------------------------------------------------------------ text

    @Override
    public void drawString(String str, int x, int y) {
        drawString(str, (float) x, (float) y);
    }

    /// Draws with the left end of the baseline at the point. The font is
    /// sized for the current scale; under a rotation the context's transform
    /// turns it, and a port without transforms draws it unrotated there.
    @Override
    public void drawString(String str, float x, float y) {
        if (str == null) {
            throw new NullPointerException("String is null");
        }
        if (str.length() == 0) {
            return;
        }
        sync();
        if (paint instanceof GradientPaint) {
            applyColor(((GradientPaint) paint).getColor1());
            dirty = true;
        }
        Font fnt = getFont();
        double ls = lineScale();
        g.setFont(Fonts.nativeFont(fnt, (float) (fnt.getSize2D() * ls)));
        double px = mapX(x, y);
        double py = mapY(x, y);
        if (upright || ls == 0 || !g.isTransformSupported()) {
            g.drawStringBaseline(str, (int) Math.round(px), (int) Math.round(py));
            return;
        }
        Transform saved = g.getTransform();
        Transform t = saved.copy();
        t.concatenate(Transform.makeAffine(a / ls, b / ls, c / ls, d / ls, px, py));
        g.setTransform(t);
        g.drawStringBaseline(str, 0, 0);
        g.setTransform(saved);
    }

    // ------------------------------------------------------------ images

    private boolean image(Image img, double x, double y, double w, double h, Color bg) {
        if (img == null) {
            return true;
        }
        com.codename1.ui.Image cn1 = img.cn1Image();
        if (cn1 == null || w <= 0 || h <= 0) {
            return false;
        }
        sync();
        if (bg != null) {
            applyColor(bg);
            if (upright) {
                fillSnapped(x, y, w, h);
            }
        }
        g.setAlpha(Math.round(target.alpha * composite.getAlpha()));
        dirty = true;
        if (upright || !g.isTransformSupported()) {
            int[] r = deviceRect(x, y, w, h);
            if (r[2] == cn1.getWidth() && r[3] == cn1.getHeight()) {
                g.drawImage(cn1, r[0], r[1]);
            } else {
                g.drawImage(cn1, r[0], r[1], Math.max(1, r[2]), Math.max(1, r[3]));
            }
            return true;
        }
        Transform saved = g.getTransform();
        Transform t = saved.copy();
        t.concatenate(Transform.makeAffine(a, b, c, d, mapX(x, y), mapY(x, y)));
        g.setTransform(t);
        g.drawImage(cn1, 0, 0, (int) Math.max(1, Math.round(w)), (int) Math.max(1, Math.round(h)));
        g.setTransform(saved);
        return true;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        return img == null || image(img, x, y, img.getWidth(observer), img.getHeight(observer), null);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
        return image(img, x, y, width, height, null);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
        return img == null || image(img, x, y, img.getWidth(observer), img.getHeight(observer), bgcolor);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor,
                             ImageObserver observer) {
        return image(img, x, y, width, height, bgcolor);
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
                             ImageObserver observer) {
        return drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null, observer);
    }

    /// Draws the source rectangle of the image scaled onto the destination
    /// rectangle. A destination or source given right-to-left or
    /// bottom-to-top is drawn unflipped.
    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
                             Color bgcolor, ImageObserver observer) {
        if (img == null) {
            return true;
        }
        int dw = Math.abs(dx2 - dx1);
        int dh = Math.abs(dy2 - dy1);
        int sw = Math.abs(sx2 - sx1);
        int sh = Math.abs(sy2 - sy1);
        if (dw == 0 || dh == 0 || sw == 0 || sh == 0) {
            return true;
        }
        int dx = Math.min(dx1, dx2);
        int dy = Math.min(dy1, dy2);
        double fx = dw / (double) sw;
        double fy = dh / (double) sh;
        G2D part = new G2D(this);
        part.clipTo(dx, dy, dw, dh, false);
        return part.image(img, dx - Math.min(sx1, sx2) * fx, dy - Math.min(sy1, sy2) * fy,
                img.getWidth(observer) * fx, img.getHeight(observer) * fy, bgcolor);
    }

    @Override
    public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
        if (img == null) {
            return true;
        }
        G2D moved = new G2D(this);
        if (xform != null) {
            moved.transform(xform);
        }
        return moved.image(img, 0, 0, img.getWidth(obs), img.getHeight(obs), null);
    }

    // ------------------------------------------------------------ peers

    /// Lets a Codename One component paint itself where this graphics has
    /// its origin: `painter` runs with the context carrying this graphics'
    /// clip and alpha and shifted so that the component, which paints at its
    /// own position (`x`, `y`), lands on the origin. A scaled or rotated
    /// user transform is applied through the context where it has one.
    public void paintNative(int x, int y, NativePainter painter) {
        sync();
        g.setAlpha(Math.round(target.alpha * composite.getAlpha()));
        double px = mapX(0, 0);
        double py = mapY(0, 0);
        boolean plain = upright && Math.abs(a - unit) < 1e-6 && Math.abs(d - unit) < 1e-6;
        if (plain || !g.isTransformSupported()) {
            int shiftX = (int) Math.round(px) - x;
            int shiftY = (int) Math.round(py) - y;
            g.translate(shiftX, shiftY);
            painter.paint(g);
            g.translate(-shiftX, -shiftY);
        } else {
            Transform saved = g.getTransform();
            Transform t = saved.copy();
            t.concatenate(Transform.makeAffine(a / unit, b / unit, c / unit, d / unit, px, py));
            t.translate(-x, -y);
            g.setTransform(t);
            painter.paint(g);
            g.setTransform(saved);
        }
        target.active = null;
    }

    /// Something that paints with a Codename One context; see
    /// [#paintNative].
    public interface NativePainter {
        void paint(com.codename1.ui.Graphics g);
    }
}
