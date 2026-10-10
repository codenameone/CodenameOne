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
package com.codename1.designer.css.raster;

import com.codename1.lottie.transcoder.parser.LottieParser;
import com.codename1.svg.transcoder.model.SVGCircle;
import com.codename1.svg.transcoder.model.SVGClipPath;
import com.codename1.svg.transcoder.model.SVGDocument;
import com.codename1.svg.transcoder.model.SVGEllipse;
import com.codename1.svg.transcoder.model.SVGGradientStop;
import com.codename1.svg.transcoder.model.SVGGroup;
import com.codename1.svg.transcoder.model.SVGLine;
import com.codename1.svg.transcoder.model.SVGLinearGradient;
import com.codename1.svg.transcoder.model.SVGNode;
import com.codename1.svg.transcoder.model.SVGPath;
import com.codename1.svg.transcoder.model.SVGPolyline;
import com.codename1.svg.transcoder.model.SVGRadialGradient;
import com.codename1.svg.transcoder.model.SVGRect;
import com.codename1.svg.transcoder.model.SVGText;
import com.codename1.svg.transcoder.parser.PathCommand;
import com.codename1.svg.transcoder.parser.SVGPaint;
import com.codename1.svg.transcoder.parser.SVGParser;
import com.codename1.svg.transcoder.parser.SVGStyle;
import com.codename1.svg.transcoder.parser.SVGTransform;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/// An SVG or Lottie file, painted with Java2D.
///
/// The file is read by the parsers the build already transcodes these files
/// with (`codenameone-svg-transcoder` and `codenameone-lottie-transcoder`), so
/// what is understood here is what is understood there. An application draws
/// such a file at runtime from the class those transcoders generate; a rule
/// that needs a generated image has its background baked into that image, and
/// this is what bakes it.
///
/// An animated file is painted at rest: the values its elements are written
/// with, before any animation has moved them. A Lottie file is painted the
/// same way, from the first frame the parser resolves.
public final class VectorImage {
    private final SVGDocument doc;
    /// Whether the drawing is fitted to the viewport it is painted into.
    private final boolean scales;

    private VectorImage(SVGDocument doc, boolean scales) {
        this.doc = doc;
        this.scales = scales;
    }

    /// Reads an SVG document.
    public static VectorImage readSvg(InputStream in) throws IOException {
        SVGDocument doc = new SVGParser().parse(in);
        return new VectorImage(doc, doc.isViewBoxDeclared());
    }

    /// Reads a Lottie animation.
    public static VectorImage readLottie(InputStream in) throws IOException {
        return new VectorImage(LottieParser.parse(in), true);
    }

    /// The width the file asks for, in CSS pixels. At least 1.
    public double getWidth() {
        return doc.getWidth() > 0 ? doc.getWidth() : 1;
    }

    /// The height the file asks for, in CSS pixels. At least 1.
    public double getHeight() {
        return doc.getHeight() > 0 ? doc.getHeight() : 1;
    }

    /// Paints the file into a viewport of `width` by `height` CSS pixels.
    ///
    /// The view box is fitted to the viewport the way the file's
    /// `preserveAspectRatio` says, which by default keeps its shape and
    /// centres it. A file with no view box is not scaled at all, and a Lottie
    /// animation is fitted like an SVG with one.
    ///
    /// #### Parameters
    ///
    /// - `width`: viewport width
    ///
    /// - `height`: viewport height
    ///
    /// - `maxSide`: the most pixels the image may have on a side. A larger
    ///   viewport is painted smaller, in proportion, for the caller to
    ///   stretch back.
    public BufferedImage paint(double width, double height, int maxSide) {
        double k = Math.min(1.0, maxSide / Math.max(1.0, Math.max(width, height)));
        if (!(k > 0)) {
            k = 1;
        }
        int w = (int) Math.max(1, Math.min(maxSide, Math.ceil(width * k)));
        int h = (int) Math.max(1, Math.min(maxSide, Math.ceil(height * k)));
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            Pixels.hints(g);
            g.scale(k, k);
            if (scales) {
                double vw = doc.getViewBoxWidth() > 0 ? doc.getViewBoxWidth() : getWidth();
                double vh = doc.getViewBoxHeight() > 0 ? doc.getViewBoxHeight() : getHeight();
                String par = doc.getPreserveAspectRatio() == null ? "" : doc.getPreserveAspectRatio().trim();
                double sx = width / vw;
                double sy = height / vh;
                double tx = 0;
                double ty = 0;
                if (!par.startsWith("none")) {
                    double s = par.endsWith("slice") ? Math.max(sx, sy) : Math.min(sx, sy);
                    double freeX = width - vw * s;
                    double freeY = height - vh * s;
                    tx = par.startsWith("xMin") ? 0 : (par.startsWith("xMax") ? freeX : freeX / 2);
                    ty = par.indexOf("YMin") >= 0 ? 0 : (par.indexOf("YMax") >= 0 ? freeY : freeY / 2);
                    sx = s;
                    sy = s;
                }
                g.translate(tx, ty);
                g.scale(sx, sy);
                g.translate(-doc.getViewBoxX(), -doc.getViewBoxY());
            }
            for (SVGNode child : doc.getChildren()) {
                paintNode(g, child, doc.getStyle(), w, h);
            }
        } finally {
            g.dispose();
        }
        return out;
    }

    /// Paints the file into a viewport of `width` by `height` pixels.
    public BufferedImage paint(int width, int height) {
        return paint(width, height, Math.max(1, Math.max(width, height)));
    }

    private void paintNode(Graphics2D g, SVGNode node, SVGStyle parentStyle, int w, int h) {
        SVGStyle style = node.getStyle() == null ? new SVGStyle() : node.getStyle();
        style.inherit(parentStyle);
        float opacity = style.getOpacity() == null ? 1f : clamp(style.getOpacity().floatValue());
        if (opacity <= 0f) {
            return;
        }
        if (opacity < 1f) {
            // An element that is not opaque is painted whole on a layer of
            // its own, and the layer is laid down once at that opacity.
            // Fading its parts one by one would let them show through each
            // other where they overlap.
            // The layer is the size of the image being painted, which is
            // handed down: what a Graphics2D reports as its device is not
            // the image behind it on every JDK.
            BufferedImage layer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D lg = layer.createGraphics();
            try {
                Pixels.hints(lg);
                lg.setTransform(g.getTransform());
                lg.setClip(g.getClip());
                paintOpaque(lg, node, style, w, h);
            } finally {
                lg.dispose();
            }
            AffineTransform at = g.getTransform();
            java.awt.Composite composite = g.getComposite();
            try {
                g.setTransform(new AffineTransform());
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
                g.drawImage(layer, 0, 0, null);
            } finally {
                g.setComposite(composite);
                g.setTransform(at);
            }
            return;
        }
        paintOpaque(g, node, style, w, h);
    }

    /// Paints `node` with everything but its own opacity.
    private void paintOpaque(Graphics2D g, SVGNode node, SVGStyle style, int w, int h) {
        AffineTransform savedTransform = g.getTransform();
        Shape savedClip = g.getClip();
        try {
            SVGTransform t = node.getTransform();
            if (t != null) {
                g.transform(new AffineTransform(t.a, t.b, t.c, t.d, t.e, t.f));
            }
            if (style.getClipPathRef() != null) {
                SVGNode clip = doc.getDefinitions().get(style.getClipPathRef());
                if (clip instanceof SVGClipPath) {
                    g.clip(clipOutline((SVGClipPath) clip));
                }
            }
            if (node instanceof SVGClipPath) {
                // A definition. It is used through clip-path, never drawn.
                return;
            }
            if (node instanceof SVGGroup) {
                for (SVGNode child : ((SVGGroup) node).getChildren()) {
                    paintNode(g, child, style, w, h);
                }
            } else if (node instanceof SVGText) {
                paintText(g, (SVGText) node, style);
            } else {
                Shape shape = outline(node);
                if (shape != null) {
                    fillAndStroke(g, shape, style);
                }
            }
        } finally {
            g.setTransform(savedTransform);
            g.setClip(savedClip);
        }
    }

    /// The union of the shapes of a clip path.
    private Shape clipOutline(SVGClipPath clip) {
        Path2D.Double all = new Path2D.Double(Path2D.WIND_NON_ZERO);
        addOutlines(all, clip, new AffineTransform());
        return all;
    }

    private void addOutlines(Path2D.Double all, SVGGroup group, AffineTransform at) {
        for (SVGNode child : group.getChildren()) {
            AffineTransform childAt = new AffineTransform(at);
            SVGTransform t = child.getTransform();
            if (t != null) {
                childAt.concatenate(new AffineTransform(t.a, t.b, t.c, t.d, t.e, t.f));
            }
            if (child instanceof SVGGroup) {
                addOutlines(all, (SVGGroup) child, childAt);
            } else {
                Shape shape = outline(child);
                if (shape != null) {
                    all.append(childAt.createTransformedShape(shape), false);
                }
            }
        }
    }

    private static Shape outline(SVGNode node) {
        if (node instanceof SVGRect) {
            SVGRect r = (SVGRect) node;
            double rx = r.getRx();
            double ry = r.getRy();
            if (rx == 0 && ry > 0) {
                rx = ry;
            }
            if (ry == 0 && rx > 0) {
                ry = rx;
            }
            if (rx <= 0 && ry <= 0) {
                return new Rectangle2D.Double(r.getX(), r.getY(), r.getWidth(), r.getHeight());
            }
            rx = Math.min(rx, r.getWidth() / 2.0);
            ry = Math.min(ry, r.getHeight() / 2.0);
            return new RoundRectangle2D.Double(r.getX(), r.getY(), r.getWidth(), r.getHeight(), rx * 2, ry * 2);
        }
        if (node instanceof SVGCircle) {
            SVGCircle c = (SVGCircle) node;
            return new Ellipse2D.Double(c.getCx() - c.getR(), c.getCy() - c.getR(), 2.0 * c.getR(), 2.0 * c.getR());
        }
        if (node instanceof SVGEllipse) {
            SVGEllipse e = (SVGEllipse) node;
            return new Ellipse2D.Double(e.getCx() - e.getRx(), e.getCy() - e.getRy(),
                    2.0 * e.getRx(), 2.0 * e.getRy());
        }
        if (node instanceof SVGLine) {
            SVGLine l = (SVGLine) node;
            Path2D.Double p = new Path2D.Double();
            p.moveTo(l.getX1(), l.getY1());
            p.lineTo(l.getX2(), l.getY2());
            return p;
        }
        if (node instanceof SVGPolyline) {
            SVGPolyline pl = (SVGPolyline) node;
            float[] pts = pl.getPoints();
            if (pts.length < 4) {
                return null;
            }
            Path2D.Double p = new Path2D.Double(Path2D.WIND_NON_ZERO);
            p.moveTo(pts[0], pts[1]);
            for (int i = 2; i + 1 < pts.length; i += 2) {
                p.lineTo(pts[i], pts[i + 1]);
            }
            if (pl.isClosed()) {
                p.closePath();
            }
            return p;
        }
        if (node instanceof SVGPath) {
            return pathOutline((SVGPath) node);
        }
        return null;
    }

    private static Shape pathOutline(SVGPath path) {
        List<PathCommand> commands = path.getCommands();
        if (commands == null || commands.isEmpty()) {
            return null;
        }
        Path2D.Double p = new Path2D.Double(Path2D.WIND_NON_ZERO);
        boolean started = false;
        for (PathCommand pc : commands) {
            float[] a = pc.getArgs();
            switch (pc.getType()) {
                case MOVE:
                    p.moveTo(a[0], a[1]);
                    started = true;
                    break;
                case LINE:
                    if (started) {
                        p.lineTo(a[0], a[1]);
                    }
                    break;
                case CUBIC:
                    if (started) {
                        p.curveTo(a[0], a[1], a[2], a[3], a[4], a[5]);
                    }
                    break;
                case QUAD:
                    if (started) {
                        p.quadTo(a[0], a[1], a[2], a[3]);
                    }
                    break;
                case ARC:
                    // The arguments are the point the arc starts at, the two
                    // radii, the rotation, the two flags and the end point.
                    if (started) {
                        arcTo(p, a[0], a[1], a[2], a[3], a[4], a[5] != 0f, a[6] != 0f, a[7], a[8]);
                    }
                    break;
                case CLOSE:
                    if (started) {
                        p.closePath();
                    }
                    break;
                default:
                    break;
            }
        }
        return started ? p : null;
    }

    /// Appends an SVG elliptical arc, by the centre parameterization of the
    /// SVG specification's implementation notes.
    private static void arcTo(Path2D.Double p, double x0, double y0, double rx, double ry, double rotationDeg,
            boolean largeArc, boolean sweep, double x, double y) {
        if (rx == 0 || ry == 0) {
            p.lineTo(x, y);
            return;
        }
        if (x0 == x && y0 == y) {
            return;
        }
        rx = Math.abs(rx);
        ry = Math.abs(ry);
        double phi = Math.toRadians(rotationDeg % 360.0);
        double cosPhi = Math.cos(phi);
        double sinPhi = Math.sin(phi);
        double dx2 = (x0 - x) / 2.0;
        double dy2 = (y0 - y) / 2.0;
        double x1 = cosPhi * dx2 + sinPhi * dy2;
        double y1 = -sinPhi * dx2 + cosPhi * dy2;
        double check = (x1 * x1) / (rx * rx) + (y1 * y1) / (ry * ry);
        if (check > 1) {
            double s = Math.sqrt(check);
            rx *= s;
            ry *= s;
        }
        double rx2 = rx * rx;
        double ry2 = ry * ry;
        double sq = (rx2 * ry2 - rx2 * y1 * y1 - ry2 * x1 * x1) / (rx2 * y1 * y1 + ry2 * x1 * x1);
        double coef = (largeArc == sweep ? -1 : 1) * Math.sqrt(Math.max(0, sq));
        double cx1 = coef * (rx * y1 / ry);
        double cy1 = coef * -(ry * x1 / rx);
        double cx = (x0 + x) / 2.0 + (cosPhi * cx1 - sinPhi * cy1);
        double cy = (y0 + y) / 2.0 + (sinPhi * cx1 + cosPhi * cy1);
        double ux = (x1 - cx1) / rx;
        double uy = (y1 - cy1) / ry;
        double vx = (-x1 - cx1) / rx;
        double vy = (-y1 - cy1) / ry;
        double start = Math.toDegrees(Math.atan2(uy, ux));
        double n = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
        double extent = n == 0 ? 0 : Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, (ux * vx + uy * vy) / n))));
        if (ux * vy - uy * vx < 0) {
            extent = -extent;
        }
        if (!sweep && extent > 0) {
            extent -= 360;
        } else if (sweep && extent < 0) {
            extent += 360;
        }
        // Arc2D measures its angles counter-clockwise with y pointing up.
        Arc2D.Double arc = new Arc2D.Double(cx - rx, cy - ry, rx * 2, ry * 2, -start, -extent, Arc2D.OPEN);
        p.append(AffineTransform.getRotateInstance(phi, cx, cy).createTransformedShape(arc), true);
    }

    private void fillAndStroke(Graphics2D g, Shape shape, SVGStyle style) {
        SVGPaint fill = style.getFill() == null ? SVGPaint.BLACK : style.getFill();
        if (!fill.isNone()) {
            float a = style.getFillOpacity() == null ? 1f : clamp(style.getFillOpacity().floatValue());
            paintWith(g, fill, a, shape, shape);
        }
        SVGPaint stroke = style.getStroke();
        if (stroke != null && !stroke.isNone()) {
            float width = style.getStrokeWidth() == null ? 1f : style.getStrokeWidth().floatValue();
            if (width <= 0) {
                return;
            }
            int cap = style.getStrokeLineCap() == null ? SVGStyle.LINECAP_BUTT : style.getStrokeLineCap().intValue();
            int join = style.getStrokeLineJoin() == null ? SVGStyle.LINEJOIN_MITER
                    : style.getStrokeLineJoin().intValue();
            float miter = style.getStrokeMiterLimit() == null ? 4f : style.getStrokeMiterLimit().floatValue();
            BasicStroke pen = new BasicStroke(width,
                    cap == SVGStyle.LINECAP_ROUND ? BasicStroke.CAP_ROUND
                            : cap == SVGStyle.LINECAP_SQUARE ? BasicStroke.CAP_SQUARE : BasicStroke.CAP_BUTT,
                    join == SVGStyle.LINEJOIN_ROUND ? BasicStroke.JOIN_ROUND
                            : join == SVGStyle.LINEJOIN_BEVEL ? BasicStroke.JOIN_BEVEL : BasicStroke.JOIN_MITER,
                    Math.max(1f, miter));
            float a = style.getStrokeOpacity() == null ? 1f : clamp(style.getStrokeOpacity().floatValue());
            paintWith(g, stroke, a, pen.createStrokedShape(shape), shape);
        }
    }

    /// Fills `area` with `paint`. A gradient given in fractions of a
    /// bounding box is measured on that of `boundsOf`, the shape itself, also
    /// when what is filled is its stroke.
    private void paintWith(Graphics2D g, SVGPaint paint, float alpha, Shape area, Shape boundsOf) {
        if (alpha <= 0) {
            return;
        }
        Paint p;
        if (paint.isReference()) {
            p = gradient(doc.getDefinitions().get(paint.getReference()), boundsOf.getBounds2D());
            if (p == null) {
                return;
            }
        } else {
            p = new Color(paint.getColor(), true);
        }
        java.awt.Composite saved = g.getComposite();
        try {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, alpha)));
            g.setPaint(p);
            g.fill(area);
        } finally {
            g.setComposite(saved);
        }
    }

    private Paint gradient(SVGNode def, Rectangle2D bounds) {
        List<SVGGradientStop> stops = stopsOf(def, 0);
        if (stops == null || stops.isEmpty()) {
            // Not a gradient, or one with nothing in it: SVG paints nothing.
            return null;
        }
        // Java2D wants strictly increasing fractions. Stops that share an
        // offset are a hard edge, kept as the smallest step a float holds.
        List<Float> fractions = new ArrayList<Float>();
        List<Color> colors = new ArrayList<Color>();
        float last = -1f;
        for (SVGGradientStop stop : stops) {
            float offset = clamp(stop.getOffset());
            if (offset <= last) {
                offset = Math.nextUp(last);
            }
            if (offset > 1f) {
                break;
            }
            int alpha = Math.round(255f * clamp(stop.getOpacity()));
            int rgb = stop.getColor();
            alpha = alpha * (rgb >>> 24) / 255;
            fractions.add(Float.valueOf(offset));
            colors.add(new Color((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, alpha));
            last = offset;
        }
        if (colors.size() == 1) {
            return colors.get(0);
        }
        float[] f = new float[fractions.size()];
        for (int i = 0; i < f.length; i++) {
            f[i] = fractions.get(i).floatValue();
        }
        Color[] c = colors.toArray(new Color[0]);
        if (def instanceof SVGLinearGradient) {
            SVGLinearGradient lg = (SVGLinearGradient) def;
            Point2D start = point(lg.getX1(), lg.getY1(), lg.isUserSpace(), bounds);
            Point2D end = point(lg.getX2(), lg.getY2(), lg.isUserSpace(), bounds);
            if (start.equals(end)) {
                return c[c.length - 1];
            }
            return new LinearGradientPaint(start, end, f, c);
        }
        SVGRadialGradient rg = (SVGRadialGradient) def;
        if (rg.isUserSpace()) {
            if (rg.getR() <= 0) {
                return c[c.length - 1];
            }
            return new RadialGradientPaint(new Point2D.Double(rg.getCx(), rg.getCy()), rg.getR(), f, c);
        }
        if (rg.getR() <= 0 || bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
            return c[c.length - 1];
        }
        // In fractions of the bounding box, which makes the circle an
        // ellipse when the box is not square.
        AffineTransform toBox = new AffineTransform(bounds.getWidth(), 0, 0, bounds.getHeight(),
                bounds.getX(), bounds.getY());
        Point2D centre = new Point2D.Double(rg.getCx(), rg.getCy());
        return new RadialGradientPaint(centre, rg.getR(), centre, f, c,
                MultipleGradientPaint.CycleMethod.NO_CYCLE, MultipleGradientPaint.ColorSpaceType.SRGB, toBox);
    }

    /// The stops of a gradient, taken from the one it refers to when it has
    /// none of its own.
    private List<SVGGradientStop> stopsOf(SVGNode def, int depth) {
        List<SVGGradientStop> stops;
        String href;
        if (def instanceof SVGLinearGradient) {
            stops = ((SVGLinearGradient) def).getStops();
            href = ((SVGLinearGradient) def).getHref();
        } else if (def instanceof SVGRadialGradient) {
            stops = ((SVGRadialGradient) def).getStops();
            href = ((SVGRadialGradient) def).getHref();
        } else {
            return null;
        }
        if (stops.isEmpty() && href != null && depth < 8) {
            String id = href.startsWith("#") ? href.substring(1) : href;
            List<SVGGradientStop> inherited = stopsOf(doc.getDefinitions().get(id), depth + 1);
            return inherited == null ? stops : inherited;
        }
        return stops;
    }

    private static Point2D point(double x, double y, boolean userSpace, Rectangle2D bounds) {
        return userSpace ? new Point2D.Double(x, y)
                : new Point2D.Double(bounds.getX() + x * bounds.getWidth(), bounds.getY() + y * bounds.getHeight());
    }

    private void paintText(Graphics2D g, SVGText text, SVGStyle style) {
        String content = text.getContent();
        if (content == null || content.length() == 0) {
            return;
        }
        SVGPaint fill = style.getFill() == null ? SVGPaint.BLACK : style.getFill();
        if (fill.isNone()) {
            return;
        }
        float size = text.getFontSize() > 0 ? text.getFontSize() : 12f;
        int fontStyle = (text.isBold() ? Font.BOLD : 0) | (text.isItalic() ? Font.ITALIC : 0);
        Font font = new Font(Font.SANS_SERIF, fontStyle, 1).deriveFont(size);
        java.awt.font.GlyphVector glyphs = font.createGlyphVector(g.getFontRenderContext(), content);
        double advance = glyphs.getLogicalBounds().getWidth();
        double x = text.getX();
        if (text.getAnchor() == SVGText.Anchor.MIDDLE) {
            x -= advance / 2.0;
        } else if (text.getAnchor() == SVGText.Anchor.END) {
            x -= advance;
        }
        Shape outline = glyphs.getOutline((float) x, text.getY());
        float a = style.getFillOpacity() == null ? 1f : clamp(style.getFillOpacity().floatValue());
        paintWith(g, fill, a, outline, outline);
    }

    private static float clamp(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
