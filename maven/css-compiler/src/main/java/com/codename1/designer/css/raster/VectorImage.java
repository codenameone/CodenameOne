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
import com.codename1.svg.transcoder.model.SVGFilter;
import com.codename1.svg.transcoder.model.SVGGradientStop;
import com.codename1.svg.transcoder.model.SVGGroup;
import com.codename1.svg.transcoder.model.SVGImage;
import com.codename1.svg.transcoder.model.SVGLine;
import com.codename1.svg.transcoder.model.SVGLinearGradient;
import com.codename1.svg.transcoder.model.SVGMarker;
import com.codename1.svg.transcoder.model.SVGNode;
import com.codename1.svg.transcoder.model.SVGPath;
import com.codename1.svg.transcoder.model.SVGPattern;
import com.codename1.svg.transcoder.model.SVGPolyline;
import com.codename1.svg.transcoder.model.SVGRadialGradient;
import com.codename1.svg.transcoder.model.SVGRect;
import com.codename1.svg.transcoder.model.SVGSymbol;
import com.codename1.svg.transcoder.model.SVGText;
import com.codename1.svg.transcoder.model.SVGUse;
import com.codename1.svg.transcoder.model.SVGViewport;
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
/// Nothing here is shared with the class the transcoders generate, which
/// draws through the framework at runtime, so the two can differ: this
/// follows `<use>` references and `fill-rule`, tiles a `<pattern>`, sees
/// through a `<mask>`, draws markers and applies filters, none of which
/// that class does.
///
/// An animated file is painted at rest: the values its elements are written
/// with, before any animation has moved them. A Lottie file is painted the
/// same way, from the first frame the parser resolves.
public final class VectorImage {
    private final SVGDocument doc;
    /// Whether the drawing is fitted to the viewport it is painted into.
    private final boolean scales;
    private static final int MAX_USE_DEPTH = 16;
    /// How many `<use>` elements deep the painting is. One image is painted
    /// by one thread at a time, which `paint` sees to.
    private int useDepth;

    /// Where the file is, for the pictures it names beside itself; null
    /// when it was not read from anywhere that has a beside.
    private java.net.URL base;
    /// How many files deep this one is: a picture inside an SVG can be an
    /// SVG with pictures of its own, and one that names itself would never
    /// end.
    private int fileDepth;
    /// Whether this was read from an SVG, which may leave its size open.
    private boolean svg;
    /// The `preserveAspectRatio` of the `<image>` this file is drawn by,
    /// which takes the place of its own; null when it is drawn by none.
    private String fitAs;
    private static final int MAX_FILE_DEPTH = 4;

    private VectorImage(SVGDocument doc, boolean scales) {
        this.doc = doc;
        this.scales = scales;
    }

    /// Reads an SVG document.
    public static VectorImage readSvg(InputStream in) throws IOException {
        return readSvg(in, null);
    }

    /// Reads an SVG document that is at `base`, which is what the pictures
    /// it names with a relative address are found against.
    public static VectorImage readSvg(InputStream in, java.net.URL base) throws IOException {
        SVGDocument doc = new SVGParser().parse(in);
        VectorImage image = new VectorImage(doc, doc.isViewBoxDeclared());
        image.base = base;
        image.svg = true;
        return image;
    }

    /// Reads a Lottie animation.
    ///
    /// The stream is either the animation's JSON or a dotLottie archive, a
    /// zip of animations of which the one its manifest picks is read.
    public static VectorImage readLottie(InputStream in) throws IOException {
        java.io.BufferedInputStream buffered = new java.io.BufferedInputStream(in);
        buffered.mark(4);
        int first = buffered.read();
        int second = buffered.read();
        buffered.reset();
        if (first != 'P' || second != 'K') {
            return new VectorImage(LottieParser.parse(buffered), true);
        }
        String json = com.codename1.lottie.transcoder.parser.DotLottie.animation(buffered);
        if (json == null) {
            throw new IOException("the dotLottie archive holds no animation");
        }
        return new VectorImage(LottieParser.parse(new java.io.ByteArrayInputStream(
                json.getBytes(java.nio.charset.StandardCharsets.UTF_8))), true);
    }

    // A file that gives only a view box is the size of that box: the
    // parser fills in a side the root element leaves open, in the shape of
    // the view box. A file with neither has no size of its own on that
    // side, and is as large as whatever it is put in: a background's
    // positioning area, which is for the caller to give. With nothing to
    // be put in it is the 300 by 150 a browser falls back to.
    private static final double DEFAULT_WIDTH = 300;
    private static final double DEFAULT_HEIGHT = 150;

    /// Whether the file says how wide it is: an SVG that gives a width, or
    /// a height and a view box to work the width out from. A view box
    /// alone is a shape and not a size; see [#hasShape]. A Lottie
    /// animation always says.
    public boolean hasWidth() {
        return doc.getWidth() > 0 && (!svg || doc.isGiven("width")
                || (doc.isGiven("height") && doc.isViewBoxDeclared()));
    }

    /// Whether the file says how high it is; see [#hasWidth].
    public boolean hasHeight() {
        return doc.getHeight() > 0 && (!svg || doc.isGiven("height")
                || (doc.isGiven("width") && doc.isViewBoxDeclared()));
    }

    /// Whether the file has a shape, a width to its height, even where it
    /// has no size: [#getWidth] and [#getHeight] are then in that shape.
    public boolean hasShape() {
        return doc.getWidth() > 0 && doc.getHeight() > 0;
    }

    /// The width the file asks for, in CSS pixels; 300 when [#hasWidth] is false.
    public double getWidth() {
        return doc.getWidth() > 0 ? doc.getWidth() : DEFAULT_WIDTH;
    }

    /// The height the file asks for, in CSS pixels; 150 when [#hasHeight] is false.
    public double getHeight() {
        return doc.getHeight() > 0 ? doc.getHeight() : DEFAULT_HEIGHT;
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
    public synchronized BufferedImage paint(double width, double height, int maxSide) {
        double k = Math.min(1.0, maxSide / Math.max(1.0, Math.max(width, height)));
        if (!(k > 0)) {
            k = 1;
        }
        int w = (int) Math.max(1, Math.min(maxSide, Math.ceil(width * k)));
        int h = (int) Math.max(1, Math.min(maxSide, Math.ceil(height * k)));
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        SVGStyle root = doc.getStyle();
        if ((root != null && root.isDisplayNone()) || doc.isGiven("empty")) {
            // Not displayed, or given a width or a height of nothing.
            return out;
        }
        Graphics2D g = out.createGraphics();
        try {
            Pixels.hints(g);
            g.scale(k, k);
            AffineTransform view = new AffineTransform();
            if (scales) {
                double vw = doc.getViewBoxWidth() > 0 ? doc.getViewBoxWidth() : getWidth();
                double vh = doc.getViewBoxHeight() > 0 ? doc.getViewBoxHeight() : getHeight();
                String par = fitAs != null ? fitAs.trim()
                        : doc.getPreserveAspectRatio() == null ? "" : doc.getPreserveAspectRatio().trim();
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
                view.translate(tx, ty);
                view.scale(sx, sy);
                view.translate(-doc.getViewBoxX(), -doc.getViewBoxY());
            }
            SVGGroup framed = rootEffects(view, width, height);
            if (framed != null) {
                paintNode(g, framed, doc.getStyle(), w, h);
            } else {
                g.transform(view);
                for (SVGNode child : doc.getChildren()) {
                    paintNode(g, child, doc.getStyle(), w, h);
                }
            }
        } finally {
            g.dispose();
        }
        // The opacity of the root element is not something its children
        // inherit: it fades the finished drawing, once and as a whole.
        float opacity = root == null || root.getOpacity() == null ? 1f : clamp(root.getOpacity().floatValue());
        if (opacity < 1f) {
            int[] px = out.getRGB(0, 0, w, h, null, 0, w);
            for (int i = 0; i < px.length; i++) {
                px[i] = (Math.round((px[i] >>> 24) * opacity) << 24) | (px[i] & 0xffffff);
            }
            out.setRGB(0, 0, w, h, px, 0, w);
        }
        return out;
    }

    /// The drawing as one group carrying what the root element does to it
    /// as a whole: its `transform`, `clip-path`, `mask` and `filter`. None
    /// of them is inherited, so handing the root's style down to its
    /// children applies none of them. Null when the root asks for none.
    ///
    /// They belong to the root element and not to what is inside it, so
    /// they are worked out in the viewport's coordinates, before the view
    /// box is fitted. The root is the one element of the file that is a
    /// box on the page, which is why its transform turns about the middle
    /// of the viewport, where a transform on anything inside turns about
    /// the origin.
    private SVGGroup rootEffects(AffineTransform view, double width, double height) {
        SVGStyle root = doc.getStyle();
        SVGTransform turned = doc.getTransform();
        boolean styled = root != null
                && (root.getClipPathRef() != null || root.getMaskRef() != null || root.getFilterRef() != null);
        if (!styled && (turned == null || turned.isIdentity())) {
            return null;
        }
        SVGGroup inner = new SVGGroup();
        double[] m = new double[6];
        view.getMatrix(m);
        inner.setTransform(new SVGTransform((float) m[0], (float) m[1], (float) m[2], (float) m[3],
                (float) m[4], (float) m[5]));
        for (SVGNode child : doc.getChildren()) {
            inner.addChild(child);
        }
        SVGGroup outer = new SVGGroup();
        outer.addChild(inner);
        if (styled) {
            SVGStyle effects = new SVGStyle();
            effects.setClipPathRef(root.getClipPathRef());
            effects.setMaskRef(root.getMaskRef());
            effects.setFilterRef(root.getFilterRef());
            outer.setStyle(effects);
        }
        if (turned != null && !turned.isIdentity()) {
            AffineTransform about = AffineTransform.getTranslateInstance(width / 2, height / 2);
            about.concatenate(new AffineTransform(turned.a, turned.b, turned.c, turned.d, turned.e, turned.f));
            about.translate(-width / 2, -height / 2);
            about.getMatrix(m);
            outer.setTransform(new SVGTransform((float) m[0], (float) m[1], (float) m[2], (float) m[3],
                    (float) m[4], (float) m[5]));
        }
        return outer;
    }

    /// Paints the file into a viewport of `width` by `height` pixels.
    public BufferedImage paint(int width, int height) {
        return paint(width, height, Math.max(1, Math.max(width, height)));
    }

    private void paintNode(Graphics2D g, SVGNode node, SVGStyle parentStyle, int w, int h) {
        // Worked out on a style of its own. Filling in the element's would
        // fix what it inherits the first time it is drawn, and an element a
        // `<use>` draws twice inherits from each of them in turn.
        SVGStyle style = new SVGStyle();
        SVGStyle own = node.getStyle();
        if (own != null) {
            if (own.isDisplayNone()) {
                return;
            }
            style.inherit(own);
            style.setOpacity(own.getOpacity());
            style.setClipPathRef(own.getClipPathRef());
            style.setNonScalingStroke(own.isNonScalingStroke());
        }
        style.inherit(parentStyle);
        // `currentColor` is the `color` of the element it is used on, which
        // is its own or the nearest one above it, and black when nobody
        // gives one. It is put in here, each time, because an element that
        // inherits a `currentColor` fill takes the word and not the colour:
        // it stands for that element's own `color`.
        int current = style.getColor() == null ? 0xff000000 : style.getColor().intValue();
        if (style.isFillCurrent()) {
            style.setFill(SVGPaint.ofColor(current));
        }
        if (style.isStrokeCurrent()) {
            style.setStroke(SVGPaint.ofColor(current));
        }
        // `context-fill` and `context-stroke` are the fill and the stroke
        // of the element a marker is drawn on, so that one arrowhead takes
        // the colour of every line it ends. Anywhere else they are nothing.
        if (style.getFillContext() != null) {
            style.setFill("fill".equals(style.getFillContext()) ? contextFill : contextStroke);
        }
        if (style.getStrokeContext() != null) {
            style.setStroke("fill".equals(style.getStrokeContext()) ? contextFill : contextStroke);
        }
        float opacity = style.getOpacity() == null ? 1f : clamp(style.getOpacity().floatValue());
        if (opacity <= 0f) {
            return;
        }
        SVGFilter filter = null;
        if (own != null && own.getFilterRef() != null) {
            SVGNode def = doc.getDefinitions().get(own.getFilterRef());
            if (def instanceof SVGFilter) {
                filter = inherited((SVGFilter) def);
            }
        }
        SVGClipPath mask = null;
        if (own != null && own.getMaskRef() != null && useDepth < MAX_USE_DEPTH) {
            SVGNode def = doc.getDefinitions().get(own.getMaskRef());
            if (def instanceof SVGClipPath && ((SVGClipPath) def).isMask()) {
                mask = (SVGClipPath) def;
            }
        }
        if (opacity < 1f || mask != null || filter != null) {
            // An element that is not opaque is painted whole on a layer of
            // its own, and the layer is laid down once at that opacity.
            // Fading its parts one by one would let them show through each
            // other where they overlap.
            // The layer is the size of the image being painted, which is
            // handed down: what a Graphics2D reports as its device is not
            // the image behind it on every JDK.
            BufferedImage layer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            // A filter comes before the clip: what it spreads, a blur for
            // one, is cut off at the clip's edge and not blurred past it.
            String clipRef = style.getClipPathRef();
            if (filter != null) {
                style.setClipPathRef(null);
            }
            Graphics2D lg = layer.createGraphics();
            try {
                Pixels.hints(lg);
                lg.setTransform(g.getTransform());
                lg.setClip(g.getClip());
                paintOpaque(lg, node, style, w, h);
            } finally {
                lg.dispose();
            }
            if (filter != null) {
                AffineTransform device = new AffineTransform(g.getTransform());
                SVGTransform t = node.getTransform();
                if (t != null) {
                    device.concatenate(new AffineTransform(t.a, t.b, t.c, t.d, t.e, t.f));
                }
                BufferedImage fillPaint = SvgFilter.reads(filter, "FillPaint")
                        ? flooded(style.getFill() == null ? SVGPaint.BLACK : style.getFill(),
                                style.getFillOpacity(), node, device, w, h) : null;
                BufferedImage strokePaint = SvgFilter.reads(filter, "StrokePaint")
                        ? flooded(style.getStroke(), style.getStrokeOpacity(), node, device, w, h) : null;
                SvgFilter.apply(layer, filter, boundsOf(node), device, fillPaint, strokePaint,
                        new SvgFilter.Pictures() {
                            @Override
                            public BufferedImage picture(SVGFilter.Primitive p, Rectangle2D area,
                                    AffineTransform toPixels, int pw, int ph) {
                                return filterPicture(p, area, toPixels, pw, ph);
                            }
                        });
                Shape outline = clipRef == null ? null : clipShape(node, clipRef);
                if (outline != null) {
                    BufferedImage cut = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D cg = cut.createGraphics();
                    try {
                        Pixels.hints(cg);
                        cg.setTransform(device);
                        cg.clip(outline);
                        cg.setTransform(new AffineTransform());
                        cg.drawImage(layer, 0, 0, null);
                    } finally {
                        cg.dispose();
                    }
                    layer = cut;
                }
            }
            if (mask != null) {
                applyMask(layer, mask, node, g, w, h);
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

    /// The whole image covered with a paint of `node`, its fill or its
    /// stroke, which is what a filter reads as `FillPaint` or `StrokePaint`.
    /// Empty when the paint is `none`.
    private BufferedImage flooded(SVGPaint paint, Float opacity, SVGNode node, AffineTransform device, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        if (paint == null || paint.isNone()) {
            return out;
        }
        Graphics2D fg = out.createGraphics();
        try {
            Pixels.hints(fg);
            fg.setTransform(device);
            Shape all = device.createInverse().createTransformedShape(new Rectangle2D.Double(0, 0, w, h));
            Rectangle2D box = boundsOf(node);
            paintWith(fg, paint, opacity == null ? 1f : clamp(opacity.floatValue()), all,
                    box == null ? all.getBounds2D() : box);
        } catch (java.awt.geom.NoninvertibleTransformException ex) {
            // Squashed flat: there is nowhere to paint.
            return out;
        } finally {
            fg.dispose();
        }
        return out;
    }

    /// Lets through as much of `layer`, which holds `node`, as `mask` says.
    ///
    /// The mask's content is painted on a layer of its own, in the
    /// coordinates of the element it masks. Each pixel of the element then
    /// keeps the share of its opacity that the mask's pixel there is light
    /// and opaque, or for an alpha mask just opaque. Where the mask paints
    /// nothing, and everywhere outside its region, nothing of the element
    /// is left.
    private void applyMask(BufferedImage layer, SVGClipPath mask, SVGNode node, Graphics2D g, int w, int h) {
        BufferedImage seen = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D mg = seen.createGraphics();
        useDepth++;
        try {
            Pixels.hints(mg);
            mg.setTransform(g.getTransform());
            SVGTransform t = node.getTransform();
            if (t != null) {
                mg.transform(new AffineTransform(t.a, t.b, t.c, t.d, t.e, t.f));
            }
            // The mask covers a region and no more: outside it nothing of
            // the element shows, whatever the mask's content would paint
            // there. The region is in fractions of the element's bounding
            // box unless it says otherwise, and for an element with no box
            // to measure that leaves no region to keep to.
            Rectangle2D box = boundsOf(node);
            if (mask.isRegionUserSpace()) {
                mg.clip(new Rectangle2D.Double(mask.getRegionX(), mask.getRegionY(),
                        mask.getRegionWidth(), mask.getRegionHeight()));
            } else if (box != null && !box.isEmpty()) {
                mg.clip(new Rectangle2D.Double(box.getX() + mask.getRegionX() * box.getWidth(),
                        box.getY() + mask.getRegionY() * box.getHeight(),
                        mask.getRegionWidth() * box.getWidth(), mask.getRegionHeight() * box.getHeight()));
            }
            boolean measurable = true;
            if (mask.isObjectBoundingBox()) {
                measurable = box != null && !box.isEmpty();
                if (measurable) {
                    mg.transform(new AffineTransform(box.getWidth(), 0, 0, box.getHeight(), box.getX(), box.getY()));
                }
            }
            if (measurable) {
                for (SVGNode child : mask.getChildren()) {
                    paintNode(mg, child, mask.getStyle(), w, h);
                }
            }
        } finally {
            useDepth--;
            mg.dispose();
        }
        int[] kept = layer.getRGB(0, 0, w, h, null, 0, w);
        int[] through = seen.getRGB(0, 0, w, h, null, 0, w);
        for (int i = 0; i < kept.length; i++) {
            int a = kept[i] >>> 24;
            if (a == 0) {
                continue;
            }
            int m = through[i];
            float share = (m >>> 24) / 255f;
            if (!mask.isAlphaMask() && mask.isLinearLuminance()) {
                // The brightness of the light, not of the number written
                // for it: a mid grey lets a fifth through, not a half.
                share *= (float) (0.2126 * linearLight((m >> 16) & 0xff) + 0.7152 * linearLight((m >> 8) & 0xff)
                        + 0.0722 * linearLight(m & 0xff));
            } else if (!mask.isAlphaMask()) {
                share *= (0.2126f * ((m >> 16) & 0xff) + 0.7152f * ((m >> 8) & 0xff) + 0.0722f * (m & 0xff)) / 255f;
            }
            kept[i] = (Math.round(a * share) << 24) | (kept[i] & 0xffffff);
        }
        layer.setRGB(0, 0, w, h, kept, 0, w);
    }

    /// An sRGB channel, 0 to 255, as linear light, 0 to 1.
    private static double linearLight(int channel) {
        double c = channel / 255.0;
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    /// Draws the picture an `<image>` names into the box it gives, fitted
    /// the way its `preserveAspectRatio` says. One that cannot be found or
    /// read leaves its box empty, as it does in a browser.
    private void paintImage(Graphics2D g, SVGImage image) {
        // A width or a height of nothing, written out, is not one left to
        // the picture: it shows nothing.
        if ((image.isGiven("width") && !(image.getWidth() > 0))
                || (image.isGiven("height") && !(image.getHeight() > 0))) {
            return;
        }
        BufferedImage picture = loadPicture(image);
        if (picture == null) {
            return;
        }
        double bw = image.getWidth() > 0 ? image.getWidth() : picture.getWidth();
        double bh = image.getHeight() > 0 ? image.getHeight() : picture.getHeight();
        String par = undeferred(image.getPreserveAspectRatio());
        double sx = bw / picture.getWidth();
        double sy = bh / picture.getHeight();
        double tx = 0;
        double ty = 0;
        if (!par.startsWith("none")) {
            double s = par.endsWith("slice") ? Math.max(sx, sy) : Math.min(sx, sy);
            double freeX = bw - picture.getWidth() * s;
            double freeY = bh - picture.getHeight() * s;
            tx = par.startsWith("xMin") ? 0 : (par.startsWith("xMax") ? freeX : freeX / 2);
            ty = par.indexOf("YMin") >= 0 ? 0 : (par.indexOf("YMax") >= 0 ? freeY : freeY / 2);
            sx = s;
            sy = s;
        }
        Shape savedClip = g.getClip();
        Object savedHint = g.getRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION);
        try {
            g.clip(new Rectangle2D.Double(image.getX(), image.getY(), bw, bh));
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            AffineTransform at = new AffineTransform();
            at.translate(image.getX() + tx, image.getY() + ty);
            at.scale(sx, sy);
            g.drawImage(picture, at, null);
        } finally {
            g.setClip(savedClip);
            if (savedHint != null) {
                g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, savedHint);
            }
        }
    }

    /// What an `feImage` shows: a picture, fitted into the effect's area
    /// like an `<image>`, or an element of the file, drawn like a `<use>`
    /// of it. Null when it names neither.
    private BufferedImage filterPicture(SVGFilter.Primitive p, Rectangle2D area, AffineTransform toPixels,
            int w, int h) {
        String href = p.get("href");
        if (href == null || href.trim().length() == 0 || useDepth >= MAX_USE_DEPTH) {
            return null;
        }
        href = href.trim();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        useDepth++;
        try {
            Pixels.hints(g);
            g.setTransform(toPixels);
            if (href.charAt(0) == '#') {
                SVGNode target = doc.getDefinitions().get(href.substring(1));
                if (target == null || target instanceof SVGFilter) {
                    return null;
                }
                // Where the effect says, when it says: left out, the
                // element stays where it was drawn.
                if (p.get("x") != null || p.get("y") != null) {
                    g.translate(p.get("x") != null ? area.getX() : 0, p.get("y") != null ? area.getY() : 0);
                }
                paintNode(g, target, new SVGStyle(), w, h);
                return out;
            }
            SVGImage image = new SVGImage();
            image.setHref(href);
            image.setX((float) area.getX());
            image.setY((float) area.getY());
            image.setWidth((float) area.getWidth());
            image.setHeight((float) area.getHeight());
            image.setPreserveAspectRatio(p.get("preserveAspectRatio"));
            if (loadPicture(image) == null) {
                return null;
            }
            paintImage(g, image);
            return out;
        } finally {
            useDepth--;
            g.dispose();
        }
    }

    /// A `preserveAspectRatio` without the `defer` an `<image>` may put
    /// before it, which only matters to a picture that has one of its own.
    private static String undeferred(String par) {
        String p = par == null ? "" : par.trim();
        return p.startsWith("defer") ? p.substring(5).trim() : p;
    }

    /// The pixels of the picture an `<image>` names, or null. A `data:`
    /// address is decoded; any other is a file beside this one. An SVG is
    /// painted at the size of the box it goes in.
    private BufferedImage loadPicture(SVGImage image) {
        String href = image.getHref();
        if (href == null || href.length() == 0 || fileDepth >= MAX_FILE_DEPTH) {
            return null;
        }
        try {
            byte[] data;
            boolean svg;
            java.net.URL at = null;
            if (DataUrl.is(href)) {
                DataUrl held = DataUrl.parse(href);
                if (held == null) {
                    return null;
                }
                svg = held.isSvg();
                data = held.getBytes();
            } else {
                // Only a file: an image baked into a theme is not something
                // to fetch from a server named inside a picture.
                if (base == null || href.indexOf(':') >= 0) {
                    return null;
                }
                at = new java.net.URL(base, href);
                if (!"file".equals(at.getProtocol())) {
                    return null;
                }
                String path = at.getPath();
                svg = path.length() >= 4 && path.regionMatches(true, path.length() - 4, ".svg", 0, 4);
                java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                try (InputStream in = at.openStream()) {
                    byte[] chunk = new byte[8192];
                    for (int n = in.read(chunk); n >= 0; n = in.read(chunk)) {
                        bytes.write(chunk, 0, n);
                    }
                }
                data = bytes.toByteArray();
            }
            if (!svg) {
                return javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(data));
            }
            VectorImage inner = readSvg(new java.io.ByteArrayInputStream(data), at);
            inner.fileDepth = fileDepth + 1;
            double pw = image.getWidth() > 0 ? image.getWidth() : inner.getWidth();
            double ph = image.getHeight() > 0 ? image.getHeight() : inner.getHeight();
            if (!inner.scales) {
                // No view box to fit: it is the size it says, and is laid
                // in the box like any other picture.
                return inner.paint(inner.getWidth(), inner.getHeight(), 4096);
            }
            // How it is fitted to the box is for the `<image>` to say. What
            // the file's own root says is for when it is shown by itself.
            // Unless it defers: then the file's own, where it has one.
            String asked = image.getPreserveAspectRatio() == null ? "" : image.getPreserveAspectRatio().trim();
            inner.fitAs = asked.startsWith("defer") && inner.doc.getPreserveAspectRatio() != null ? null
                    : undeferred(asked);
            return inner.paint(pw, ph, 4096);
        } catch (IOException ex) {
            return null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /// A filter with what it takes from the one it names through `href`:
    /// the effects, when it has none of its own, and each of the region,
    /// the region's units and the effects' units that it does not give.
    /// The filter itself when it names none.
    private SVGFilter inherited(SVGFilter filter) {
        SVGFilter from = filter;
        SVGFilter out = filter;
        for (int depth = 0; depth < MAX_USE_DEPTH && from.getHref() != null; depth++) {
            SVGNode def = doc.getDefinitions().get(from.getHref());
            if (!(def instanceof SVGFilter) || def == filter) {
                break;
            }
            from = (SVGFilter) def;
            if (out == filter) {
                out = new SVGFilter();
                out.setId(filter.getId());
                out.getPrimitives().addAll(filter.getPrimitives());
                out.setRegion(filter.getRegionX(), filter.getRegionY(), filter.getRegionWidth(),
                        filter.getRegionHeight());
                out.setRegionUserSpace(filter.isRegionUserSpace());
                out.setPrimitiveObjectBoundingBox(filter.isPrimitiveObjectBoundingBox());
                out.setSrgb(filter.isSrgb());
                for (String given : new String[] {"x", "y", "width", "height", "filterUnits", "primitiveUnits"}) {
                    if (filter.isGiven(given)) {
                        out.markGiven(given);
                    }
                }
            }
            if (out.getPrimitives().isEmpty()) {
                out.getPrimitives().addAll(from.getPrimitives());
                out.setSrgb(from.isSrgb());
            }
            // Each side it does not give, and the units when it gives
            // none. A side is kept as the number written, which reads the
            // same in either units, so one taken from here goes with the
            // units taken from there.
            float[] region = {out.getRegionX(), out.getRegionY(), out.getRegionWidth(), out.getRegionHeight()};
            float[] theirs = {from.getRegionX(), from.getRegionY(), from.getRegionWidth(), from.getRegionHeight()};
            String[] sides = {"x", "y", "width", "height"};
            for (int k = 0; k < sides.length; k++) {
                if (!out.isGiven(sides[k]) && from.isGiven(sides[k])) {
                    region[k] = theirs[k];
                    out.markGiven(sides[k]);
                }
            }
            if (!out.isGiven("filterUnits") && from.isGiven("filterUnits")) {
                out.setRegionUserSpace(from.isRegionUserSpace());
                out.markGiven("filterUnits");
                // The sides nobody gives are a tenth out all round, in
                // those units: as the filter they come from has them.
                for (int k = 0; k < sides.length; k++) {
                    if (!out.isGiven(sides[k])) {
                        region[k] = theirs[k];
                    }
                }
            }
            out.setRegion(region[0], region[1], region[2], region[3]);
            if (!out.isGiven("primitiveUnits") && from.isGiven("primitiveUnits")) {
                out.setPrimitiveObjectBoundingBox(from.isPrimitiveObjectBoundingBox());
                out.markGiven("primitiveUnits");
            }
        }
        return out;
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
                Shape outline = clipShape(node, style.getClipPathRef());
                if (outline != null) {
                    g.clip(outline);
                }
            }
            if (node instanceof SVGClipPath) {
                // A definition. It is used through clip-path, never drawn.
                return;
            }
            if (node.isGiven("transform-box")) {
                throw new IllegalArgumentException("the SVG transforms an element about its own box"
                        + " (transform-box), which is not supported here. Give transform-origin as a point of the"
                        + " drawing, or write the transform out");
            }
            if (node instanceof com.codename1.svg.transcoder.model.SVGForeignObject) {
                // What is in one is a page, HTML as a rule, and drawing it
                // takes a browser. Leaving it out would give an image with
                // a hole where a label was and no word of it, so the image
                // is refused, with what to do about it. Only here, where
                // it would be drawn: one nothing shows is no fault.
                throw new IllegalArgumentException("the SVG holds a <foreignObject>, whose content cannot be drawn"
                        + " here. Put it in a <switch> with something to draw in its place, or take it out");
            }
            if (node instanceof SVGUse) {
                paintUse(g, (SVGUse) node, style, w, h);
                return;
            }
            if (node instanceof SVGViewport) {
                // An `<svg>` inside the drawing: its content is laid out in
                // its box and cut off at the box's edges. A box with no
                // size shows nothing.
                SVGViewport port = (SVGViewport) node;
                if (port.getWidth() > 0 && port.getHeight() > 0) {
                    if (!port.isOverflowVisible()) {
                        g.clip(new Rectangle2D.Double(port.getX(), port.getY(), port.getWidth(), port.getHeight()));
                    }
                    g.transform(layout(port));
                    for (SVGNode child : port.getContent().getChildren()) {
                        paintNode(g, child, style, w, h);
                    }
                }
            } else if (node instanceof SVGGroup) {
                for (SVGNode child : ((SVGGroup) node).getChildren()) {
                    paintNode(g, child, style, w, h);
                }
            } else if (Boolean.FALSE.equals(style.getVisible())) {
                // Hidden, which unlike `display: none` a child may undo.
                return;
            } else if (node instanceof SVGImage) {
                paintImage(g, (SVGImage) node);
            } else if (node instanceof SVGText) {
                paintText(g, (SVGText) node, style);
            } else {
                Shape shape = outline(node);
                if (shape != null) {
                    for (String layer : layers(style)) {
                        if ("fill".equals(layer)) {
                            fill(g, shape, style, true);
                        } else if ("stroke".equals(layer)) {
                            stroke(g, shape, style);
                        } else if (node instanceof SVGPath || node instanceof SVGLine
                                || node instanceof SVGPolyline) {
                            paintMarkers(g, shape, style, w, h);
                        }
                    }
                }
            }
        } finally {
            g.setTransform(savedTransform);
            g.setClip(savedClip);
        }
    }

    /// Draws the markers of a line on its points: `marker-start` on the
    /// first, `marker-end` on the last and `marker-mid` on each between.
    ///
    /// First and last are of the whole element. A path of several subpaths
    /// has one first point and one last, and where one subpath ends and the
    /// next begins are points between like any other: that is how the
    /// browser these images used to be captured in places them, and what
    /// the specification says ("the first vertex of the given shape").
    ///
    /// A point is where a segment of the line ends. A marker that turns
    /// with the line faces the way the line runs there, and where the line
    /// bends, half way between the two directions. An arc is drawn as a
    /// few curves, and the points where those meet count as points of the
    /// line.
    /// What `context-fill` and `context-stroke` stand for: the paints of
    /// the element whose markers are being drawn, and `none` outside one.
    private SVGPaint contextFill = SVGPaint.NONE;
    private SVGPaint contextStroke = SVGPaint.NONE;

    private void paintMarkers(Graphics2D g, Shape shape, SVGStyle style, int w, int h) {
        SVGMarker start = markerOf(style.getMarkerStart());
        SVGMarker mid = markerOf(style.getMarkerMid());
        SVGMarker end = markerOf(style.getMarkerEnd());
        if (start == null && mid == null && end == null) {
            return;
        }
        SVGPaint outerFill = contextFill;
        SVGPaint outerStroke = contextStroke;
        contextFill = style.getFill() == null ? SVGPaint.BLACK : style.getFill();
        contextStroke = style.getStroke() == null ? SVGPaint.NONE : style.getStroke();
        try {
            paintMarkers(g, shape, style, start, mid, end, w, h);
        } finally {
            contextFill = outerFill;
            contextStroke = outerStroke;
        }
    }

    private void paintMarkers(Graphics2D g, Shape shape, SVGStyle style, SVGMarker start, SVGMarker mid,
            SVGMarker end, int w, int h) {
        // Each point: where it is, the way the line comes in and the way
        // it leaves, either of which a point at an end does not have.
        List<double[]> points = new ArrayList<double[]>();
        double[] seg = new double[6];
        double[] first = null;
        double[] last = null;
        for (java.awt.geom.PathIterator it = shape.getPathIterator(null); !it.isDone(); it.next()) {
            int type = it.currentSegment(seg);
            if (type == java.awt.geom.PathIterator.SEG_MOVETO) {
                last = new double[] {seg[0], seg[1], Double.NaN, 0, Double.NaN, 0};
                first = last;
                points.add(last);
                continue;
            }
            if (last == null) {
                continue;
            }
            double ex;
            double ey;
            double outX;
            double outY;
            double inX;
            double inY;
            if (type == java.awt.geom.PathIterator.SEG_CLOSE) {
                ex = first[0];
                ey = first[1];
                outX = ex - last[0];
                outY = ey - last[1];
                inX = outX;
                inY = outY;
            } else if (type == java.awt.geom.PathIterator.SEG_LINETO) {
                ex = seg[0];
                ey = seg[1];
                outX = ex - last[0];
                outY = ey - last[1];
                inX = outX;
                inY = outY;
            } else if (type == java.awt.geom.PathIterator.SEG_QUADTO) {
                ex = seg[2];
                ey = seg[3];
                outX = seg[0] - last[0];
                outY = seg[1] - last[1];
                inX = ex - seg[0];
                inY = ey - seg[1];
            } else {
                ex = seg[4];
                ey = seg[5];
                outX = seg[0] - last[0];
                outY = seg[1] - last[1];
                if (outX == 0 && outY == 0) {
                    outX = seg[2] - last[0];
                    outY = seg[3] - last[1];
                }
                inX = ex - seg[2];
                inY = ey - seg[3];
                if (inX == 0 && inY == 0) {
                    inX = ex - seg[0];
                    inY = ey - seg[1];
                }
            }
            // A control point on top of an end gives no direction; the
            // straight line between the ends does.
            if (outX == 0 && outY == 0) {
                outX = ex - last[0];
                outY = ey - last[1];
            }
            if (inX == 0 && inY == 0) {
                inX = ex - last[0];
                inY = ey - last[1];
            }
            if (outX != 0 || outY != 0) {
                last[4] = outX;
                last[5] = outY;
            }
            double[] next = new double[] {ex, ey, Double.NaN, 0, Double.NaN, 0};
            if (inX != 0 || inY != 0) {
                next[2] = inX;
                next[3] = inY;
            }
            if (type == java.awt.geom.PathIterator.SEG_CLOSE) {
                // Back where the line began: it bends there as it does
                // anywhere else, between its last stretch and its first.
                next[4] = first[4];
                next[5] = first[5];
                first[2] = next[2];
                first[3] = next[3];
            }
            points.add(next);
            last = next;
        }
        float strokeWidth = style.getStrokeWidth() == null ? 1f : style.getStrokeWidth().floatValue();
        for (int i = 0; i < points.size(); i++) {
            SVGMarker marker = i == 0 ? start : i == points.size() - 1 ? end : mid;
            if (marker == null) {
                continue;
            }
            double[] p = points.get(i);
            double angle = 0;
            boolean in = !Double.isNaN(p[2]);
            boolean out = !Double.isNaN(p[4]);
            if (in && out) {
                double li = Math.hypot(p[2], p[3]);
                double lo = Math.hypot(p[4], p[5]);
                double bx = p[2] / li + p[4] / lo;
                double by = p[3] / li + p[5] / lo;
                // Doubling straight back, there is no half way: the way in.
                angle = bx == 0 && by == 0 ? Math.atan2(p[3], p[2]) : Math.atan2(by, bx);
            } else if (in) {
                angle = Math.atan2(p[3], p[2]);
            } else if (out) {
                angle = Math.atan2(p[5], p[4]);
            }
            paintMarker(g, marker, p[0], p[1], angle, i == 0, strokeWidth, w, h);
        }
    }

    private SVGMarker markerOf(String id) {
        SVGNode def = id == null || id.length() == 0 ? null : doc.getDefinitions().get(id);
        return def instanceof SVGMarker ? (SVGMarker) def : null;
    }

    /// Draws one marker with its reference point on a point of a line.
    private void paintMarker(Graphics2D g, SVGMarker marker, double x, double y, double angle, boolean first,
            float strokeWidth, int w, int h) {
        double mw = marker.getMarkerWidth();
        double mh = marker.getMarkerHeight();
        if (useDepth >= MAX_USE_DEPTH || !(mw > 0) || !(mh > 0)) {
            return;
        }
        String orient = marker.getOrient() == null ? "" : marker.getOrient();
        double turn = 0;
        if ("auto".equals(orient)) {
            turn = angle;
        } else if ("auto-start-reverse".equals(orient)) {
            turn = first ? angle + Math.PI : angle;
        } else if (orient.length() > 0) {
            // An angle in any of its units; a bare number is degrees.
            String o = orient.trim();
            try {
                if (o.endsWith("grad")) {
                    turn = Math.toRadians(Double.parseDouble(o.substring(0, o.length() - 4)) * 0.9);
                } else if (o.endsWith("rad")) {
                    turn = Double.parseDouble(o.substring(0, o.length() - 3));
                } else if (o.endsWith("turn")) {
                    turn = Double.parseDouble(o.substring(0, o.length() - 4)) * 2 * Math.PI;
                } else {
                    turn = Math.toRadians(Double.parseDouble(o.endsWith("deg") ? o.substring(0, o.length() - 3) : o));
                }
            } catch (NumberFormatException ex) {
                turn = 0;
            }
        }
        AffineTransform savedTransform = g.getTransform();
        Shape savedClip = g.getClip();
        useDepth++;
        try {
            g.translate(x, y);
            g.rotate(turn);
            if (!marker.isUserSpace()) {
                // Sized in widths of the stroke it is on.
                g.scale(strokeWidth, strokeWidth);
            }
            AffineTransform fit = marker.getViewBoxWidth() > 0 && marker.getViewBoxHeight() > 0
                    ? fit(marker.getPreserveAspectRatio(), mw, mh, marker.getViewBoxX(), marker.getViewBoxY(),
                            marker.getViewBoxWidth(), marker.getViewBoxHeight())
                    : new AffineTransform();
            Point2D ref = fit.transform(new Point2D.Double(marker.getRefX(), marker.getRefY()), null);
            g.translate(-ref.getX(), -ref.getY());
            if (!marker.isOverflowVisible()) {
                g.clip(new Rectangle2D.Double(0, 0, mw, mh));
            }
            g.transform(fit);
            for (SVGNode child : marker.getChildren()) {
                paintNode(g, child, marker.getStyle(), w, h);
            }
        } finally {
            useDepth--;
            g.setTransform(savedTransform);
            g.setClip(savedClip);
        }
    }

    /// Fits a view box into a box of `width` by `height` at the origin the
    /// way `par`, a `preserveAspectRatio`, says: stretched for `none`, and
    /// otherwise keeping its shape, as large as fits or for `slice` as
    /// small as covers, in the middle unless it names a side.
    private static AffineTransform fit(String par, double width, double height, double vx, double vy,
            double vw, double vh) {
        AffineTransform at = new AffineTransform();
        String p = par == null ? "" : par.trim();
        double sx = width / vw;
        double sy = height / vh;
        if (!p.startsWith("none")) {
            double s = p.endsWith("slice") ? Math.max(sx, sy) : Math.min(sx, sy);
            double freeX = width - vw * s;
            double freeY = height - vh * s;
            at.translate(p.startsWith("xMin") ? 0 : (p.startsWith("xMax") ? freeX : freeX / 2),
                    p.indexOf("YMin") >= 0 ? 0 : (p.indexOf("YMax") >= 0 ? freeY : freeY / 2));
            sx = s;
            sy = s;
        }
        at.scale(sx, sy);
        at.translate(-vx, -vy);
        return at;
    }

    /// The outline `node` is clipped to by the clip path of that id, in the
    /// coordinates of `node`, or null when there is no such clip path.
    private Shape clipShape(SVGNode node, String ref) {
        SVGNode clip = doc.getDefinitions().get(ref);
        if (!(clip instanceof SVGClipPath)) {
            return null;
        }
        // A clip path that is clipped itself, or that holds a shape that
        // is, asks for another one from in here. One that comes back to
        // itself that way would never end, and clips nothing.
        if (clipDepth >= MAX_USE_DEPTH) {
            return null;
        }
        clipDepth++;
        try {
            return clipShape(node, (SVGClipPath) clip);
        } finally {
            clipDepth--;
        }
    }

    private Shape clipShape(SVGNode node, SVGClipPath clip) {
        Shape outline = clipOutline(clip);
        if (clip.isObjectBoundingBox()) {
            // In fractions of the box around what is clipped. With no box
            // to measure, nothing is inside it.
            Rectangle2D box = boundsOf(node);
            outline = box == null || box.isEmpty() ? new Rectangle2D.Double()
                    : new AffineTransform(box.getWidth(), 0, 0, box.getHeight(), box.getX(), box.getY())
                            .createTransformedShape(outline);
        }
        // A `clip-path` on the clip path itself: only what is inside both
        // is kept.
        String again = clip.getStyle() == null ? null : clip.getStyle().getClipPathRef();
        if (again != null) {
            Shape outer = clipShape(node, again);
            if (outer != null) {
                java.awt.geom.Area both = new java.awt.geom.Area(outline);
                both.intersect(new java.awt.geom.Area(outer));
                outline = both;
            }
        }
        return outline;
    }

    /// How many clip paths deep the one being worked out is.
    private int clipDepth;

    /// Draws the element a `<use>` names, at the offset it gives. What the
    /// element does not set for itself it takes from the `<use>`, as if it
    /// were written there.
    private void paintUse(Graphics2D g, SVGUse use, SVGStyle style, int w, int h) {
        String href = use.getHref();
        SVGNode target = href == null ? null : doc.getDefinitions().get(href);
        // The file the element is in, which is the one that knows the
        // gradients and clip paths it refers to.
        VectorImage owner = this;
        if (target == null && href != null && href.indexOf('#') > 0) {
            // `sprite.svg#icon`: an element of another file beside this one.
            owner = beside(href.substring(0, href.indexOf('#')));
            target = owner == null ? null : owner.doc.getDefinitions().get(href.substring(href.indexOf('#') + 1));
        }
        if (target == null || target instanceof SVGClipPath || target instanceof SVGPattern
                || target instanceof SVGMarker || target instanceof SVGFilter
                || target instanceof SVGLinearGradient
                || target instanceof SVGRadialGradient) {
            return;
        }
        // An element that draws itself, directly or through others, would
        // never finish.
        if (useDepth >= MAX_USE_DEPTH) {
            return;
        }
        useDepth++;
        int ownerDepth = owner.useDepth;
        try {
            owner.useDepth = useDepth;
            g.transform(placing(use, target));
            target = sized(use, target);
            if (target == null) {
                return;
            }
            owner.paintNode(g, target, style, w, h);
        } finally {
            owner.useDepth = ownerDepth;
            useDepth--;
        }
    }

    /// The SVG files this one has drawn elements of, by the name it calls
    /// them; a name that could not be read is kept with null.
    private java.util.Map<String, VectorImage> besides;

    /// The SVG file of that name beside this one, or null: when this one
    /// has no beside, the name is not that of a file there, it cannot be
    /// read, or the files are nested too deep. Like a picture, it is only
    /// ever a file; nothing is fetched.
    private VectorImage beside(String name) {
        if (besides == null) {
            besides = new java.util.HashMap<String, VectorImage>();
        }
        if (besides.containsKey(name)) {
            return besides.get(name);
        }
        VectorImage found = null;
        if (base != null && name.indexOf(':') < 0 && fileDepth < MAX_FILE_DEPTH) {
            try {
                java.net.URL at = new java.net.URL(base, name);
                if ("file".equals(at.getProtocol())) {
                    try (InputStream in = at.openStream()) {
                        found = readSvg(in, at);
                        found.fileDepth = fileDepth + 1;
                    }
                }
            } catch (IOException ex) {
                found = null;
            } catch (RuntimeException ex) {
                found = null;
            }
        }
        besides.put(name, found);
        return found;
    }

    /// How the content of a nested `<svg>` gets into its box: moved to the
    /// box's corner, and with a view box fitted to the box the way
    /// `preserveAspectRatio` says, which by default keeps its shape and
    /// centres it.
    private static AffineTransform layout(SVGViewport port) {
        AffineTransform at = AffineTransform.getTranslateInstance(port.getX(), port.getY());
        double vw = port.getViewBoxWidth();
        double vh = port.getViewBoxHeight();
        if (!(vw > 0) || !(vh > 0)) {
            return at;
        }
        at.concatenate(fit(port.getPreserveAspectRatio(), port.getWidth(), port.getHeight(),
                port.getViewBoxX(), port.getViewBoxY(), vw, vh));
        return at;
    }

    /// Where a `<use>` puts the element it draws: at its offset, and a
    /// symbol with a view box fitted into the size the `<use>` gives, the
    /// way the symbol's `preserveAspectRatio` says.
    /// What a `<use>` draws of `target`: the element itself, or for an
    /// `<svg>` the same at the size the `<use>` gives, which takes the place
    /// of the one that element gives itself. Null when the `<use>` gives an
    /// `<svg>` or a symbol a width or a height of zero, which is a size, and
    /// one at which nothing is drawn.
    private static SVGNode sized(SVGUse use, SVGNode target) {
        boolean wide = use.isGiven("width");
        boolean high = use.isGiven("height");
        if ((target instanceof SVGViewport || target instanceof SVGSymbol)
                && ((wide && !(use.getWidth() > 0)) || (high && !(use.getHeight() > 0)))) {
            return null;
        }
        if (!(target instanceof SVGViewport) || !(use.getWidth() > 0 || use.getHeight() > 0)) {
            return target;
        }
        SVGViewport as = (SVGViewport) target;
        SVGViewport sized = new SVGViewport();
        sized.setStyle(as.getStyle());
        sized.setTransform(as.getTransform());
        sized.setX(as.getX());
        sized.setY(as.getY());
        sized.setWidth(use.getWidth() > 0 ? use.getWidth() : as.getWidth());
        sized.setHeight(use.getHeight() > 0 ? use.getHeight() : as.getHeight());
        sized.setViewBox(as.getViewBoxX(), as.getViewBoxY(), as.getViewBoxWidth(), as.getViewBoxHeight());
        sized.setPreserveAspectRatio(as.getPreserveAspectRatio());
        sized.setOverflowVisible(as.isOverflowVisible());
        for (SVGNode child : as.getContent().getChildren()) {
            sized.getContent().addChild(child);
        }
        return sized;
    }

    private static AffineTransform placing(SVGUse use, SVGNode target) {
        AffineTransform at = AffineTransform.getTranslateInstance(use.getX(), use.getY());
        if (target instanceof SVGSymbol) {
            SVGSymbol symbol = (SVGSymbol) target;
            if (symbol.getViewBoxWidth() > 0 && symbol.getViewBoxHeight() > 0
                    && use.getWidth() > 0 && use.getHeight() > 0) {
                at.concatenate(fit(symbol.getPreserveAspectRatio(), use.getWidth(), use.getHeight(),
                        symbol.getViewBoxX(), symbol.getViewBoxY(),
                        symbol.getViewBoxWidth(), symbol.getViewBoxHeight()));
            }
        }
        return at;
    }

    /// The box around the shapes of `node`, in its own coordinates, or null
    /// when it has none that can be measured.
    private Rectangle2D boundsOf(SVGNode node) {
        return boundsOf(node, 0);
    }

    private Rectangle2D boundsOf(SVGNode node, int depth) {
        if (node.getStyle() != null && node.getStyle().isDisplayNone()) {
            return null;
        }
        if (node instanceof SVGImage) {
            SVGImage image = (SVGImage) node;
            return image.getWidth() > 0 && image.getHeight() > 0
                    ? new Rectangle2D.Double(image.getX(), image.getY(), image.getWidth(), image.getHeight()) : null;
        }
        if (node instanceof SVGText) {
            Shape glyphs = textOutline((SVGText) node, new java.awt.font.FontRenderContext(null, true, true));
            return glyphs == null ? null : glyphs.getBounds2D();
        }
        if (node instanceof SVGUse) {
            SVGUse use = (SVGUse) node;
            String href = use.getHref();
            SVGNode target = href == null ? null : doc.getDefinitions().get(href);
            // An element of another file is measured by that file, as it
            // is drawn by it.
            VectorImage owner = this;
            if (target == null && href != null && href.indexOf('#') > 0) {
                owner = beside(href.substring(0, href.indexOf('#')));
                target = owner == null ? null : owner.doc.getDefinitions().get(href.substring(href.indexOf('#') + 1));
            }
            if (target == null || depth >= MAX_USE_DEPTH) {
                return null;
            }
            // Measured as it is drawn: at the size the `<use>` gives.
            AffineTransform put = placing(use, target);
            target = sized(use, target);
            if (target == null) {
                return null;
            }
            Rectangle2D box = owner.boundsOf(target, depth + 1);
            if (box == null) {
                return null;
            }
            AffineTransform at = put;
            SVGTransform t = target.getTransform();
            if (t != null) {
                at.concatenate(new AffineTransform(t.a, t.b, t.c, t.d, t.e, t.f));
            }
            return at.createTransformedShape(box).getBounds2D();
        }
        if (node instanceof SVGViewport) {
            // No more than its box, which is all of it that shows.
            SVGViewport port = (SVGViewport) node;
            Rectangle2D inside = boundsOf(port.getContent(), depth);
            if (inside == null || !(port.getWidth() > 0) || !(port.getHeight() > 0)) {
                return null;
            }
            Rectangle2D shown = layout(port).createTransformedShape(inside).getBounds2D().createIntersection(
                    new Rectangle2D.Double(port.getX(), port.getY(), port.getWidth(), port.getHeight()));
            return shown.isEmpty() ? null : shown;
        }
        if (node instanceof SVGClipPath || node instanceof SVGPattern || node instanceof SVGMarker) {
            return null;
        }
        if (!(node instanceof SVGGroup)) {
            Shape shape = outline(node);
            return shape == null ? null : shape.getBounds2D();
        }
        Rectangle2D all = null;
        for (SVGNode child : ((SVGGroup) node).getChildren()) {
            if (child instanceof SVGClipPath) {
                continue;
            }
            Rectangle2D box = boundsOf(child, depth);
            if (box == null) {
                continue;
            }
            SVGTransform t = child.getTransform();
            if (t != null) {
                box = new AffineTransform(t.a, t.b, t.c, t.d, t.e, t.f).createTransformedShape(box).getBounds2D();
            }
            if (all == null) {
                all = box;
            } else {
                all = all.createUnion(box);
            }
        }
        return all;
    }

    /// The union of the shapes of a clip path, each filled by the
    /// `clip-rule` it has or inherits: with `evenodd` a subpath inside
    /// another is a hole in the clip.
    private Shape clipOutline(SVGClipPath clip) {
        java.awt.geom.Area all = new java.awt.geom.Area();
        // A transform on the clip path itself moves all of its children.
        SVGTransform own = clip.getTransform();
        addOutlines(all, clip, own == null ? new AffineTransform()
                        : new AffineTransform(own.a, own.b, own.c, own.d, own.e, own.f),
                clip.getStyle() == null ? null : clip.getStyle().getClipEvenOdd(), 0);
        return all;
    }

    private void addOutlines(java.awt.geom.Area all, SVGGroup group, AffineTransform at, Boolean evenOdd,
            int depth) {
        for (SVGNode child : group.getChildren()) {
            // What is not displayed cuts nothing, nor does anything in it.
            if (child.getStyle() != null && child.getStyle().isDisplayNone()) {
                continue;
            }
            AffineTransform childAt = new AffineTransform(at);
            SVGTransform t = child.getTransform();
            if (t != null) {
                childAt.concatenate(new AffineTransform(t.a, t.b, t.c, t.d, t.e, t.f));
            }
            Boolean rule = child.getStyle() != null && child.getStyle().getClipEvenOdd() != null
                    ? child.getStyle().getClipEvenOdd() : evenOdd;
            if (child instanceof SVGUse) {
                // A clip path made of a shape defined elsewhere: the shape
                // is cut out where the `<use>` puts it.
                SVGUse use = (SVGUse) child;
                SVGNode target = use.getHref() == null ? null : doc.getDefinitions().get(use.getHref());
                if (target == null || target instanceof SVGClipPath || depth >= MAX_USE_DEPTH) {
                    continue;
                }
                childAt.concatenate(placing(use, target));
                SVGGroup one = new SVGGroup();
                one.addChild(target);
                addOutlines(all, one, childAt, rule, depth + 1);
            } else if (child instanceof SVGGroup) {
                addOutlines(all, (SVGGroup) child, childAt, rule, depth + 1);
            } else {
                Shape shape = outline(child);
                if (shape != null) {
                    Path2D.Double ruled = new Path2D.Double(shape);
                    ruled.setWindingRule(Boolean.TRUE.equals(rule) ? Path2D.WIND_EVEN_ODD : Path2D.WIND_NON_ZERO);
                    java.awt.geom.Area part = new java.awt.geom.Area(ruled);
                    // A shape of a clip path can be clipped in its turn, in
                    // its own coordinates: only that much of it is added.
                    String own = child.getStyle() == null ? null : child.getStyle().getClipPathRef();
                    Shape cut = own == null ? null : clipShape(child, own);
                    if (cut != null) {
                        part.intersect(new java.awt.geom.Area(cut));
                    }
                    part.transform(childAt);
                    all.add(part);
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

    /// The layers of a shape, `fill`, `stroke` and `markers`, in the order
    /// its `paint-order` has them painted: the ones it names first, in
    /// the order it names them, and then the rest in their usual order.
    private static String[] layers(SVGStyle style) {
        List<String> order = new ArrayList<String>();
        String given = style.getPaintOrder();
        if (given != null) {
            for (String word : given.trim().split("\\s+")) {
                if (("fill".equals(word) || "stroke".equals(word) || "markers".equals(word))
                        && !order.contains(word)) {
                    order.add(word);
                }
            }
        }
        for (String word : new String[] {"fill", "stroke", "markers"}) {
            if (!order.contains(word)) {
                order.add(word);
            }
        }
        return order.toArray(new String[0]);
    }

    /// Fills `shape` and strokes it, in the order `paint-order` gives the
    /// two. `ruled` is whether `fill-rule` applies, which it does not to
    /// the outlines of text.
    private void fillAndStroke(Graphics2D g, Shape shape, SVGStyle style, boolean ruled) {
        for (String layer : layers(style)) {
            if ("fill".equals(layer)) {
                fill(g, shape, style, ruled);
            } else if ("stroke".equals(layer)) {
                stroke(g, shape, style);
            }
        }
    }

    private void fill(Graphics2D g, Shape shape, SVGStyle style, boolean ruled) {
        SVGPaint fill = style.getFill() == null ? SVGPaint.BLACK : style.getFill();
        if (!fill.isNone()) {
            float a = style.getFillOpacity() == null ? 1f : clamp(style.getFillOpacity().floatValue());
            Shape filled = shape;
            if (ruled && shape instanceof Path2D && Boolean.TRUE.equals(style.getFillEvenOdd())) {
                // `fill-rule: evenodd`: a subpath inside another is a hole.
                Path2D holes = new Path2D.Double(shape);
                holes.setWindingRule(Path2D.WIND_EVEN_ODD);
                filled = holes;
            }
            paintWith(g, fill, a, filled, shape);
        }
    }

    private void stroke(Graphics2D g, Shape shape, SVGStyle style) {
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
            float[] dashes = style.getStrokeDashArray();
            if (dashes != null && dashes.length == 0) {
                dashes = null;
            }
            float phase = 0f;
            if (dashes != null && style.getStrokeDashOffset() != null) {
                // A negative offset moves the pattern the other way, which
                // is the same as moving it on by what is left of one round.
                float round = 0f;
                for (float d : dashes) {
                    round += d;
                }
                phase = style.getStrokeDashOffset().floatValue() % round;
                if (phase < 0f) {
                    phase += round;
                }
            }
            BasicStroke pen = new BasicStroke(width,
                    cap == SVGStyle.LINECAP_ROUND ? BasicStroke.CAP_ROUND
                            : cap == SVGStyle.LINECAP_SQUARE ? BasicStroke.CAP_SQUARE : BasicStroke.CAP_BUTT,
                    join == SVGStyle.LINEJOIN_ROUND ? BasicStroke.JOIN_ROUND
                            : join == SVGStyle.LINEJOIN_BEVEL ? BasicStroke.JOIN_BEVEL : BasicStroke.JOIN_MITER,
                    Math.max(1f, miter), dashes, phase);
            float a = style.getStrokeOpacity() == null ? 1f : clamp(style.getStrokeOpacity().floatValue());
            Shape stroked = null;
            if (style.isNonScalingStroke()) {
                // The pen is as wide as it says on the image, whatever the
                // drawing is scaled by: the line is drawn where it lands,
                // and brought back to be filled like any other.
                AffineTransform at = g.getTransform();
                try {
                    stroked = at.createInverse().createTransformedShape(
                            pen.createStrokedShape(at.createTransformedShape(shape)));
                } catch (java.awt.geom.NoninvertibleTransformException ex) {
                    stroked = null;
                }
            }
            paintWith(g, stroke, a, stroked == null ? pen.createStrokedShape(shape) : stroked, shape);
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
            String ref = paint.getReference();
            SVGNode def = doc.getDefinitions().get(ref);
            // A gradient or a pattern of another file beside this one,
            // `paints.svg#g`, is worked out by that file, which is the one
            // that knows what it refers to in its turn.
            VectorImage owner = this;
            if (def == null && ref.indexOf('#') > 0) {
                VectorImage other = beside(ref.substring(0, ref.indexOf('#')));
                if (other != null) {
                    owner = other;
                    def = other.doc.getDefinitions().get(ref.substring(ref.indexOf('#') + 1));
                }
            }
            if (def instanceof SVGPattern) {
                owner.paintPattern(g, (SVGPattern) def, alpha, area, boundsOf.getBounds2D());
                return;
            }
            if (def instanceof SVGLinearGradient || def instanceof SVGRadialGradient) {
                p = owner.gradient(def, boundsOf.getBounds2D());
                if (p == null) {
                    return;
                }
            } else {
                // Nothing to paint with has that id. The colour written
                // after the reference stands in; with none, nothing is
                // painted.
                SVGPaint fallback = paint.getFallback();
                if (fallback == null || fallback.isNone()) {
                    return;
                }
                p = new Color(fallback.getColor(), true);
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
        def = resolved(def);
        List<SVGGradientStop> stops = def instanceof SVGLinearGradient ? ((SVGLinearGradient) def).getStops()
                : def instanceof SVGRadialGradient ? ((SVGRadialGradient) def).getStops() : null;
        if (stops == null || stops.isEmpty()) {
            // Not a gradient, or one with nothing in it: SVG paints nothing.
            return null;
        }
        // Java2D wants strictly increasing fractions. Stops that share an
        // offset are a hard edge, kept as the smallest step a float holds:
        // each is put just after the one before it, and where that runs
        // past the end -- stops sharing the offset 1 -- the last is the end
        // and the ones before it are moved down instead. It is the last of
        // them the gradient goes on in past its end, so it cannot be the
        // one that is dropped.
        //
        // The colours between two stops are mixed channel by channel with
        // the alpha as one more channel, not multiplied by it first. That
        // is how an SVG gradient is defined and how a browser paints one --
        // only a CSS gradient is mixed premultiplied -- so red fading to
        // `transparent`, which is transparent black, darkens on the way
        // here as it does there.
        float[] placed = new float[stops.size()];
        float before = -1f;
        for (int i = 0; i < placed.length; i++) {
            float offset = clamp(stops.get(i).getOffset());
            placed[i] = offset <= before ? Math.nextUp(before) : offset;
            before = placed[i];
        }
        float after = Math.nextUp(1f);
        for (int i = placed.length - 1; i >= 0 && placed[i] >= after; i--) {
            placed[i] = Math.max(0f, Math.nextDown(after));
            after = placed[i];
        }
        List<Float> fractions = new ArrayList<Float>();
        List<Color> colors = new ArrayList<Color>();
        float last = -1f;
        for (int i = 0; i < placed.length; i++) {
            SVGGradientStop stop = stops.get(i);
            float offset = placed[i];
            if (offset <= last) {
                // More stops on one offset than there are floats to tell
                // them apart; never in a file anyone wrote.
                continue;
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
        // Past its ends a gradient goes on in its last colour, unless it
        // says to start over or to run back.
        String spread = def instanceof SVGLinearGradient ? ((SVGLinearGradient) def).getSpreadMethod()
                : ((SVGRadialGradient) def).getSpreadMethod();
        MultipleGradientPaint.CycleMethod cycle = "repeat".equals(spread) ? MultipleGradientPaint.CycleMethod.REPEAT
                : "reflect".equals(spread) ? MultipleGradientPaint.CycleMethod.REFLECT
                        : MultipleGradientPaint.CycleMethod.NO_CYCLE;
        SVGTransform gt = def instanceof SVGLinearGradient ? ((SVGLinearGradient) def).getGradientTransform()
                : ((SVGRadialGradient) def).getGradientTransform();
        AffineTransform turned = gt == null ? null : new AffineTransform(gt.a, gt.b, gt.c, gt.d, gt.e, gt.f);
        if (turned != null && Math.abs(turned.getDeterminant()) < 1e-12) {
            // Squashed flat: there is no gradient left to see.
            return c[c.length - 1];
        }
        if (def instanceof SVGLinearGradient && turned != null) {
            SVGLinearGradient lg = (SVGLinearGradient) def;
            Point2D start = new Point2D.Double(lg.getX1(), lg.getY1());
            Point2D end = new Point2D.Double(lg.getX2(), lg.getY2());
            if (start.equals(end)) {
                return c[c.length - 1];
            }
            // The gradient is laid out in its own coordinates and those are
            // then moved: by its transform, and for one given in fractions
            // of the bounding box by that box after it.
            AffineTransform at = new AffineTransform();
            if (!lg.isUserSpace()) {
                if (bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
                    return c[c.length - 1];
                }
                at.setTransform(bounds.getWidth(), 0, 0, bounds.getHeight(), bounds.getX(), bounds.getY());
            }
            at.concatenate(turned);
            return new LinearGradientPaint(start, end, f, c, cycle,
                    MultipleGradientPaint.ColorSpaceType.SRGB, at);
        }
        if (def instanceof SVGLinearGradient) {
            SVGLinearGradient lg = (SVGLinearGradient) def;
            Point2D start = point(lg.getX1(), lg.getY1(), lg.isUserSpace(), bounds);
            Point2D end = point(lg.getX2(), lg.getY2(), lg.isUserSpace(), bounds);
            if (start.equals(end)) {
                return c[c.length - 1];
            }
            return new LinearGradientPaint(start, end, f, c, cycle);
        }
        SVGRadialGradient rg = (SVGRadialGradient) def;
        if (rg.isUserSpace()) {
            if (rg.getR() <= 0) {
                return c[c.length - 1];
            }
            return new RadialGradientPaint(new Point2D.Double(rg.getCx(), rg.getCy()), rg.getR(), focusOf(rg), f, c,
                    cycle, MultipleGradientPaint.ColorSpaceType.SRGB,
                    turned == null ? new AffineTransform() : turned);
        }
        if (rg.getR() <= 0 || bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
            return c[c.length - 1];
        }
        // In fractions of the bounding box, which makes the circle an
        // ellipse when the box is not square.
        AffineTransform toBox = new AffineTransform(bounds.getWidth(), 0, 0, bounds.getHeight(),
                bounds.getX(), bounds.getY());
        if (turned != null) {
            toBox.concatenate(turned);
        }
        Point2D centre = new Point2D.Double(rg.getCx(), rg.getCy());
        return new RadialGradientPaint(centre, rg.getR(), focusOf(rg), f, c,
                cycle, MultipleGradientPaint.ColorSpaceType.SRGB, toBox);
    }

    /// Where a radial gradient's first colour starts: its focal point,
    /// which is its centre unless `fx` or `fy` says otherwise.
    private static Point2D focusOf(SVGRadialGradient rg) {
        return new Point2D.Double(rg.getFx() == null ? rg.getCx() : rg.getFx().floatValue(),
                rg.getFy() == null ? rg.getCy() : rg.getFy().floatValue());
    }

    /// The elements `def` takes what it leaves out from, itself first: the
    /// one its `href` names, the one that one names, and so on. They are
    /// patterns when `pattern` is set and gradients otherwise; the chain
    /// ends at anything else, and at an element it has already passed.
    private List<SVGNode> chainOf(SVGNode def, boolean pattern) {
        List<SVGNode> chain = new ArrayList<SVGNode>();
        SVGNode at = def;
        while (at != null && chain.size() < 8 && !chain.contains(at)) {
            String href;
            if (pattern && at instanceof SVGPattern) {
                href = ((SVGPattern) at).getHref();
            } else if (!pattern && at instanceof SVGLinearGradient) {
                href = ((SVGLinearGradient) at).getHref();
            } else if (!pattern && at instanceof SVGRadialGradient) {
                href = ((SVGRadialGradient) at).getHref();
            } else {
                break;
            }
            chain.add(at);
            at = href == null ? null : doc.getDefinitions().get(href.startsWith("#") ? href.substring(1) : href);
        }
        return chain;
    }

    /// The first element of `chain` that is a `type` and wrote `attribute`
    /// itself, or null.
    private static <T extends SVGNode> T giver(List<SVGNode> chain, String attribute, Class<T> type) {
        for (SVGNode node : chain) {
            if (type.isInstance(node) && node.isGiven(attribute)) {
                return type.cast(node);
            }
        }
        return null;
    }

    /// A gradient with everything it leaves to the one its `href` names
    /// filled in: not only the stops but where it runs, what that is
    /// measured in and its transform. Where it runs is taken only from a
    /// gradient of its own kind, since a line has no radius to give a
    /// circle. Anything that refers to nothing is answered as it is.
    private SVGNode resolved(SVGNode def) {
        List<SVGNode> chain = chainOf(def, false);
        if (chain.size() < 2) {
            return def;
        }
        boolean userSpace = false;
        SVGTransform transform = null;
        List<SVGGradientStop> stops = null;
        boolean unitsFound = false;
        String spread = null;
        for (SVGNode node : chain) {
            boolean linear = node instanceof SVGLinearGradient;
            if (spread == null && node.isGiven("spreadMethod")) {
                spread = linear ? ((SVGLinearGradient) node).getSpreadMethod()
                        : ((SVGRadialGradient) node).getSpreadMethod();
            }
            if (!unitsFound && node.isGiven("gradientUnits")) {
                unitsFound = true;
                userSpace = linear ? ((SVGLinearGradient) node).isUserSpace()
                        : ((SVGRadialGradient) node).isUserSpace();
            }
            if (transform == null) {
                transform = linear ? ((SVGLinearGradient) node).getGradientTransform()
                        : ((SVGRadialGradient) node).getGradientTransform();
            }
            List<SVGGradientStop> own = linear ? ((SVGLinearGradient) node).getStops()
                    : ((SVGRadialGradient) node).getStops();
            if (stops == null && !own.isEmpty()) {
                stops = own;
            }
        }
        if (def instanceof SVGLinearGradient) {
            SVGLinearGradient out = new SVGLinearGradient();
            SVGLinearGradient from = giver(chain, "x1", SVGLinearGradient.class);
            if (from != null) {
                out.setX1(from.getX1());
            }
            from = giver(chain, "y1", SVGLinearGradient.class);
            if (from != null) {
                out.setY1(from.getY1());
            }
            from = giver(chain, "x2", SVGLinearGradient.class);
            if (from != null) {
                out.setX2(from.getX2());
            }
            from = giver(chain, "y2", SVGLinearGradient.class);
            if (from != null) {
                out.setY2(from.getY2());
            }
            out.setUserSpace(userSpace);
            out.setSpreadMethod(spread);
            out.setGradientTransform(transform);
            if (stops != null) {
                out.getStops().addAll(stops);
            }
            return out;
        }
        SVGRadialGradient out = new SVGRadialGradient();
        SVGRadialGradient from = giver(chain, "cx", SVGRadialGradient.class);
        if (from != null) {
            out.setCx(from.getCx());
        }
        from = giver(chain, "cy", SVGRadialGradient.class);
        if (from != null) {
            out.setCy(from.getCy());
        }
        from = giver(chain, "r", SVGRadialGradient.class);
        if (from != null) {
            out.setR(from.getR());
        }
        from = giver(chain, "fx", SVGRadialGradient.class);
        if (from != null) {
            out.setFx(from.getFx());
        }
        from = giver(chain, "fy", SVGRadialGradient.class);
        if (from != null) {
            out.setFy(from.getFy());
        }
        out.setUserSpace(userSpace);
        out.setSpreadMethod(spread);
        out.setGradientTransform(transform);
        if (stops != null) {
            out.getStops().addAll(stops);
        }
        return out;
    }

    private static final int MAX_TILE_SIDE = 2048;

    /// Fills `area` with the tile of a pattern, laid side by side from the
    /// tile's own corner.
    ///
    /// The tile is painted once, at the size it has on the image, and then
    /// repeated. Its content is cut off at its edges. A pattern with no
    /// size, or measured against a box that has none, paints nothing.
    private void paintPattern(Graphics2D g, SVGPattern def, float alpha, Shape area, Rectangle2D bounds) {
        List<SVGNode> chain = chainOf(def, true);
        if (chain.isEmpty() || useDepth >= MAX_USE_DEPTH) {
            return;
        }
        SVGPattern units = giver(chain, "patternUnits", SVGPattern.class);
        boolean userSpace = units != null && units.isUserSpace();
        SVGPattern px = giver(chain, "x", SVGPattern.class);
        SVGPattern py = giver(chain, "y", SVGPattern.class);
        SVGPattern pw = giver(chain, "width", SVGPattern.class);
        SVGPattern ph = giver(chain, "height", SVGPattern.class);
        double x = px == null ? 0 : px.getX();
        double y = py == null ? 0 : py.getY();
        double tw = pw == null ? 0 : pw.getWidth();
        double th = ph == null ? 0 : ph.getHeight();
        boolean boxed = bounds.getWidth() > 0 && bounds.getHeight() > 0;
        if (!userSpace) {
            if (!boxed) {
                return;
            }
            x = bounds.getX() + x * bounds.getWidth();
            y = bounds.getY() + y * bounds.getHeight();
            tw *= bounds.getWidth();
            th *= bounds.getHeight();
        }
        if (!(tw > 0) || !(th > 0)) {
            return;
        }
        SVGPattern turned = giver(chain, "patternTransform", SVGPattern.class);
        SVGTransform t = turned == null ? null : turned.getPatternTransform();
        AffineTransform pt = t == null ? new AffineTransform() : new AffineTransform(t.a, t.b, t.c, t.d, t.e, t.f);
        AffineTransform back;
        try {
            back = pt.createInverse();
        } catch (java.awt.geom.NoninvertibleTransformException ex) {
            return;
        }
        SVGPattern content = def;
        for (SVGNode node : chain) {
            if (!((SVGPattern) node).getChildren().isEmpty()) {
                content = (SVGPattern) node;
                break;
            }
        }
        // As many pixels as the tile covers on the image, so that it is
        // not drawn small and stretched.
        AffineTransform device = new AffineTransform(g.getTransform());
        device.concatenate(pt);
        double sx = Math.hypot(device.getScaleX(), device.getShearY());
        double sy = Math.hypot(device.getShearX(), device.getScaleY());
        int iw = (int) Math.max(1, Math.min(MAX_TILE_SIDE, Math.ceil(tw * sx)));
        int ih = (int) Math.max(1, Math.min(MAX_TILE_SIDE, Math.ceil(th * sy)));
        BufferedImage tile = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
        Graphics2D tg = tile.createGraphics();
        useDepth++;
        try {
            Pixels.hints(tg);
            // What a pattern holds is drawn from the corner of its tile:
            // the tile's `x` and `y` say where the tiles are laid, and what
            // is in them moves along. A rectangle at 20 in a tile at 20 is
            // 20 inside the tile, in a browser as here, so the tile is not
            // shifted back by its own position. This is the specification:
            // "If there is no viewBox attribute, then the new coordinate
            // system has its origin at (x, y)" of the pattern (SVG 1.1,
            // 13.3). That the content is in the user space of the element
            // the pattern is used on says which units and directions it is
            // drawn in, not where its origin is.
            tg.scale(iw / tw, ih / th);
            SVGPattern boxGiver = giver(chain, "viewBox", SVGPattern.class);
            SVGPattern contentUnits = giver(chain, "patternContentUnits", SVGPattern.class);
            boolean drawn = true;
            if (boxGiver != null && boxGiver.getViewBoxWidth() > 0 && boxGiver.getViewBoxHeight() > 0) {
                SVGPattern fit = giver(chain, "preserveAspectRatio", SVGPattern.class);
                String par = fit == null || fit.getPreserveAspectRatio() == null ? ""
                        : fit.getPreserveAspectRatio().trim();
                double vw = boxGiver.getViewBoxWidth();
                double vh = boxGiver.getViewBoxHeight();
                double fx = tw / vw;
                double fy = th / vh;
                double ox = 0;
                double oy = 0;
                if (!par.startsWith("none")) {
                    double s = par.endsWith("slice") ? Math.max(fx, fy) : Math.min(fx, fy);
                    double freeX = tw - vw * s;
                    double freeY = th - vh * s;
                    ox = par.startsWith("xMin") ? 0 : (par.startsWith("xMax") ? freeX : freeX / 2);
                    oy = par.indexOf("YMin") >= 0 ? 0 : (par.indexOf("YMax") >= 0 ? freeY : freeY / 2);
                    fx = s;
                    fy = s;
                }
                tg.translate(ox, oy);
                tg.scale(fx, fy);
                tg.translate(-boxGiver.getViewBoxX(), -boxGiver.getViewBoxY());
            } else if (contentUnits != null && contentUnits.isContentObjectBoundingBox()) {
                drawn = boxed;
                if (drawn) {
                    tg.scale(bounds.getWidth(), bounds.getHeight());
                }
            }
            if (drawn) {
                for (SVGNode child : content.getChildren()) {
                    // What a pattern has from another through `href` is
                    // drawn as if it were written in this one, so it has
                    // from this one what it does not set itself: a
                    // `color`, say, for its `currentColor`.
                    paintNode(tg, child, def.getStyle(), iw, ih);
                }
            }
        } finally {
            useDepth--;
            tg.dispose();
        }
        AffineTransform savedTransform = g.getTransform();
        java.awt.Composite saved = g.getComposite();
        Paint savedPaint = g.getPaint();
        try {
            g.transform(pt);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, alpha)));
            g.setPaint(new java.awt.TexturePaint(tile, new Rectangle2D.Double(x, y, tw, th)));
            g.fill(back.createTransformedShape(area));
        } finally {
            g.setPaint(savedPaint);
            g.setComposite(saved);
            g.setTransform(savedTransform);
        }
    }

    private static Point2D point(double x, double y, boolean userSpace, Rectangle2D bounds) {
        return userSpace ? new Point2D.Double(x, y)
                : new Point2D.Double(bounds.getX() + x * bounds.getWidth(), bounds.getY() + y * bounds.getHeight());
    }

    private void paintText(Graphics2D g, SVGText text, SVGStyle style) {
        List<SVGText.Run> runs = runsOf(text);
        List<Shape> outlines = textOutlines(text, runs, g.getFontRenderContext());
        for (int i = 0; i < runs.size(); i++) {
            Shape outline = outlines.get(i);
            if (outline == null) {
                continue;
            }
            // A span is styled by what it sets and, for the rest, by the
            // text it is in.
            SVGStyle own = runs.get(i).getStyle();
            SVGStyle runStyle = style;
            if (own != null) {
                if (own.isDisplayNone()) {
                    continue;
                }
                runStyle = new SVGStyle();
                runStyle.inherit(own);
                runStyle.inherit(style);
                int current = runStyle.getColor() == null ? 0xff000000 : runStyle.getColor().intValue();
                if (runStyle.isFillCurrent()) {
                    runStyle.setFill(SVGPaint.ofColor(current));
                }
                if (runStyle.isStrokeCurrent()) {
                    runStyle.setStroke(SVGPaint.ofColor(current));
                }
            }
            if (Boolean.FALSE.equals(runStyle.getVisible())) {
                continue;
            }
            // Filled and then stroked like any other shape: text with no
            // fill and a stroke is an outline, not nothing.
            fillAndStroke(g, outline, runStyle, false);
        }
    }

    /// The stretches of a `<text>`. One that was not read from a file has
    /// its content as a single stretch.
    private static List<SVGText.Run> runsOf(SVGText text) {
        List<SVGText.Run> runs = text.getRuns();
        if (!runs.isEmpty() || text.getContent() == null || text.getContent().length() == 0) {
            return runs;
        }
        SVGText.Run whole = new SVGText.Run();
        whole.setText(text.getContent());
        List<SVGText.Run> one = new ArrayList<SVGText.Run>();
        one.add(whole);
        return one;
    }

    /// The outlines of the glyphs of each stretch of a `<text>`, where it
    /// puts them; null for a stretch with nothing in it.
    ///
    /// The text runs on from one stretch to the next unless a stretch says
    /// where it starts. `text-anchor` is about each piece that starts
    /// somewhere of its own: that piece, with the stretches that run on
    /// from it, begins, is centred or ends at the point given.
    private List<Shape> textOutlines(SVGText text, List<SVGText.Run> written,
            java.awt.font.FontRenderContext context) {
        // A stretch that is not displayed takes no part: it has no width,
        // moves nothing and starts nothing. It stays in the list, empty,
        // so that the outlines still answer to the stretches one for one.
        List<SVGText.Run> runs = new ArrayList<SVGText.Run>(written.size());
        for (SVGText.Run run : written) {
            if (run.getStyle() != null && run.getStyle().isDisplayNone()) {
                SVGText.Run gone = new SVGText.Run();
                gone.setPathRef(run.getPathRef());
                gone.setStartOffset(run.getStartOffset(), run.isStartOffsetShare());
                runs.add(gone);
            } else {
                runs.add(run);
            }
        }
        List<Shape> out = new ArrayList<Shape>();
        double cx = text.getX();
        double cy = text.getY();
        int i = 0;
        while (i < runs.size()) {
            int end = i + 1;
            String along = runs.get(i).getPathRef();
            while (end < runs.size() && runs.get(end).getX() == null && runs.get(end).getY() == null
                    && (along == null ? runs.get(end).getPathRef() == null : along.equals(runs.get(end).getPathRef()))) {
                end++;
            }
            SVGText.Run head = runs.get(i);
            if (along != null) {
                // Text on a path: each glyph is set down on the path, turned
                // the way the path runs there. What follows it goes on from
                // where the path ends.
                double[] ended = onPath(text, runs, i, end, context, out);
                if (ended != null) {
                    cx = ended[0];
                    cy = ended[1];
                }
                i = end;
                continue;
            }
            if (head.getX() != null) {
                cx = head.getX().floatValue();
            }
            if (head.getY() != null) {
                cy = head.getY().floatValue();
            }
            double startX = cx;
            List<java.awt.font.GlyphVector> glyphs = new ArrayList<java.awt.font.GlyphVector>();
            double[] xs = new double[end - i];
            double[] ys = new double[end - i];
            for (int k = i; k < end; k++) {
                SVGText.Run run = runs.get(k);
                cx += run.getDx();
                cy += run.getDy();
                float size = run.getFontSize() != null ? run.getFontSize().floatValue() : text.getFontSize();
                if (!(size > 0)) {
                    size = 12f;
                }
                boolean bold = run.getBold() != null ? run.getBold().booleanValue() : text.isBold();
                boolean italic = run.getItalic() != null ? run.getItalic().booleanValue() : text.isItalic();
                String family = run.getFontFamily() != null ? run.getFontFamily() : text.getFontFamily();
                Font font = new Font(familyOf(family), (bold ? Font.BOLD : 0) | (italic ? Font.ITALIC : 0), 1)
                        .deriveFont(size);
                java.awt.font.GlyphVector gv = font.createGlyphVector(context, run.getText());
                glyphs.add(gv);
                xs[k - i] = cx;
                ys[k - i] = cy;
                cx += gv.getLogicalBounds().getWidth();
            }
            double advance = cx - startX;
            // The anchor of the piece is that of the stretch it starts
            // with, which is the text's own unless a span sets another.
            SVGText.Anchor anchor = head.getAnchor() != null ? head.getAnchor() : text.getAnchor();
            double shift = anchor == SVGText.Anchor.MIDDLE ? -advance / 2.0
                    : anchor == SVGText.Anchor.END ? -advance : 0;
            for (int k = 0; k < glyphs.size(); k++) {
                out.add(runs.get(i + k).getText().length() == 0 ? null
                        : glyphs.get(k).getOutline((float) (xs[k] + shift), (float) ys[k]));
            }
            cx += shift;
            i = end;
        }
        return out;
    }

    /// Lays the stretches `from` to `to`, which are on one `<textPath>`,
    /// along its path, adding an outline for each to `out`: null for one
    /// with no text or none of it on the path. A glyph is where its middle
    /// falls on the path, and one whose middle is off either end is not
    /// drawn. Returns where the path ends, or null when there is no such
    /// path, in which case the text is not drawn at all.
    private double[] onPath(SVGText text, List<SVGText.Run> runs, int from, int to,
            java.awt.font.FontRenderContext context, List<Shape> out) {
        SVGNode target = doc.getDefinitions().get(runs.get(from).getPathRef());
        Shape line = target == null ? null : outline(target);
        if (line == null) {
            for (int k = from; k < to; k++) {
                out.add(null);
            }
            return null;
        }
        SVGTransform moved = target.getTransform();
        AffineTransform at = moved == null ? null
                : new AffineTransform(moved.a, moved.b, moved.c, moved.d, moved.e, moved.f);
        // The path as short straight pieces: where each starts, and how
        // far along the path that is.
        List<double[]> pieces = new ArrayList<double[]>();
        double[] seg = new double[6];
        double total = 0;
        double px = 0;
        double py = 0;
        double sx = 0;
        double sy = 0;
        for (java.awt.geom.PathIterator it = new java.awt.geom.FlatteningPathIterator(line.getPathIterator(at), 0.05);
                !it.isDone(); it.next()) {
            int kind = it.currentSegment(seg);
            double nx = kind == java.awt.geom.PathIterator.SEG_CLOSE ? sx : seg[0];
            double ny = kind == java.awt.geom.PathIterator.SEG_CLOSE ? sy : seg[1];
            if (kind == java.awt.geom.PathIterator.SEG_MOVETO) {
                sx = nx;
                sy = ny;
            } else {
                double len = Math.hypot(nx - px, ny - py);
                if (len > 0) {
                    pieces.add(new double[] {px, py, nx, ny, total, len});
                    total += len;
                }
            }
            px = nx;
            py = ny;
        }
        if (pieces.isEmpty()) {
            for (int k = from; k < to; k++) {
                out.add(null);
            }
            return null;
        }
        List<java.awt.font.GlyphVector> glyphs = new ArrayList<java.awt.font.GlyphVector>();
        double advance = 0;
        for (int k = from; k < to; k++) {
            SVGText.Run run = runs.get(k);
            float size = run.getFontSize() != null ? run.getFontSize().floatValue() : text.getFontSize();
            if (!(size > 0)) {
                size = 12f;
            }
            boolean bold = run.getBold() != null ? run.getBold().booleanValue() : text.isBold();
            boolean italic = run.getItalic() != null ? run.getItalic().booleanValue() : text.isItalic();
            String family = run.getFontFamily() != null ? run.getFontFamily() : text.getFontFamily();
            Font font = new Font(familyOf(family), (bold ? Font.BOLD : 0) | (italic ? Font.ITALIC : 0), 1)
                    .deriveFont(size);
            java.awt.font.GlyphVector gv = font.createGlyphVector(context, run.getText());
            glyphs.add(gv);
            advance += run.getDx() + gv.getLogicalBounds().getWidth();
        }
        SVGText.Run head = runs.get(from);
        double along = head.isStartOffsetShare() ? head.getStartOffset() * total : head.getStartOffset();
        SVGText.Anchor anchor = head.getAnchor() != null ? head.getAnchor() : text.getAnchor();
        along += anchor == SVGText.Anchor.MIDDLE ? -advance / 2.0
                : anchor == SVGText.Anchor.END ? -advance : 0;
        int piece = 0;
        for (int k = from; k < to; k++) {
            SVGText.Run run = runs.get(k);
            java.awt.font.GlyphVector gv = glyphs.get(k - from);
            along += run.getDx();
            Path2D.Double drawn = null;
            for (int j = 0; j < gv.getNumGlyphs(); j++) {
                double wide = gv.getGlyphMetrics(j).getAdvanceX();
                double middle = along + wide / 2;
                along += wide;
                if (middle < 0 || middle > total) {
                    continue;
                }
                while (piece > 0 && pieces.get(piece)[4] > middle) {
                    piece--;
                }
                while (piece < pieces.size() - 1 && pieces.get(piece)[4] + pieces.get(piece)[5] < middle) {
                    piece++;
                }
                double[] on = pieces.get(piece);
                double part = (middle - on[4]) / on[5];
                AffineTransform place = AffineTransform.getTranslateInstance(on[0] + (on[2] - on[0]) * part,
                        on[1] + (on[3] - on[1]) * part);
                place.rotate(Math.atan2(on[3] - on[1], on[2] - on[0]));
                // `dy` lifts the glyph off the path, across the way it runs.
                place.translate(-wide / 2, run.getDy());
                Point2D origin = gv.getGlyphPosition(j);
                Shape glyph = gv.getGlyphOutline(j, (float) -origin.getX(), (float) -origin.getY());
                if (drawn == null) {
                    drawn = new Path2D.Double();
                }
                drawn.append(place.createTransformedShape(glyph), false);
            }
            out.add(drawn);
        }
        double[] last = pieces.get(pieces.size() - 1);
        return new double[] {last[2], last[3]};
    }

    /// The outlines of all the glyphs of a `<text>`, or null when it has no
    /// text.
    private Shape textOutline(SVGText text, java.awt.font.FontRenderContext context) {
        Path2D.Double all = null;
        for (Shape outline : textOutlines(text, runsOf(text), context)) {
            if (outline != null) {
                if (all == null) {
                    all = new Path2D.Double();
                }
                all.append(outline, false);
            }
        }
        return all;
    }

    /// The font family a `font-family` list comes to: the first name in it
    /// that is a generic family or a font this machine has, and sans-serif
    /// when there is none.
    private static String familyOf(String families) {
        if (families == null) {
            return Font.SANS_SERIF;
        }
        for (String part : families.split(",")) {
            String name = part.trim();
            if (name.length() >= 2 && (name.charAt(0) == '"' || name.charAt(0) == '\'')) {
                name = name.substring(1, name.length() - 1).trim();
            }
            if (name.length() == 0) {
                continue;
            }
            if ("serif".equalsIgnoreCase(name)) {
                return Font.SERIF;
            }
            if ("sans-serif".equalsIgnoreCase(name) || "system-ui".equalsIgnoreCase(name)) {
                return Font.SANS_SERIF;
            }
            if ("monospace".equalsIgnoreCase(name)) {
                return Font.MONOSPACED;
            }
            for (String have : InstalledFonts.NAMES) {
                if (have.equalsIgnoreCase(name)) {
                    return have;
                }
            }
        }
        return Font.SANS_SERIF;
    }

    /// The font families of this machine, asked for once and only when an
    /// SVG names one.
    private static final class InstalledFonts {
        static final String[] NAMES = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
    }

    private static float clamp(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
