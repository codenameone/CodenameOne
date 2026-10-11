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
        return image;
    }

    /// Reads a Lottie animation.
    ///
    /// The stream is either the animation's JSON or a dotLottie archive, a
    /// zip that holds the JSON under `animations/`.
    public static VectorImage readLottie(InputStream in) throws IOException {
        java.io.BufferedInputStream buffered = new java.io.BufferedInputStream(in);
        buffered.mark(4);
        int first = buffered.read();
        int second = buffered.read();
        buffered.reset();
        if (first != 'P' || second != 'K') {
            return new VectorImage(LottieParser.parse(buffered), true);
        }
        java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(buffered);
        byte[] fallback = null;
        for (java.util.zip.ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
            String name = entry.getName();
            if (entry.isDirectory() || !name.endsWith(".json") || name.endsWith("manifest.json")) {
                continue;
            }
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            for (int n = zip.read(chunk); n >= 0; n = zip.read(chunk)) {
                bytes.write(chunk, 0, n);
            }
            if (name.startsWith("animations/") || name.startsWith("a/")) {
                return new VectorImage(LottieParser.parse(
                        new java.io.ByteArrayInputStream(bytes.toByteArray())), true);
            }
            if (fallback == null) {
                fallback = bytes.toByteArray();
            }
        }
        if (fallback == null) {
            throw new IOException("the dotLottie archive holds no animation");
        }
        return new VectorImage(LottieParser.parse(new java.io.ByteArrayInputStream(fallback)), true);
    }

    // A file that gives only a view box is the size of that box: the
    // parser fills in a side the root element leaves open, in the shape of
    // the view box. The 1 below is for a file with neither.
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
    public synchronized BufferedImage paint(double width, double height, int maxSide) {
        double k = Math.min(1.0, maxSide / Math.max(1.0, Math.max(width, height)));
        if (!(k > 0)) {
            k = 1;
        }
        int w = (int) Math.max(1, Math.min(maxSide, Math.ceil(width * k)));
        int h = (int) Math.max(1, Math.min(maxSide, Math.ceil(height * k)));
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        SVGStyle root = doc.getStyle();
        if (root != null && root.isDisplayNone()) {
            return out;
        }
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
        float opacity = style.getOpacity() == null ? 1f : clamp(style.getOpacity().floatValue());
        if (opacity <= 0f) {
            return;
        }
        SVGFilter filter = null;
        if (own != null && own.getFilterRef() != null) {
            SVGNode def = doc.getDefinitions().get(own.getFilterRef());
            if (def instanceof SVGFilter) {
                filter = (SVGFilter) def;
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
                SvgFilter.apply(layer, filter, boundsOf(node), device);
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
            if (!mask.isAlphaMask()) {
                share *= (0.2126f * ((m >> 16) & 0xff) + 0.7152f * ((m >> 8) & 0xff) + 0.0722f * (m & 0xff)) / 255f;
            }
            kept[i] = (Math.round(a * share) << 24) | (kept[i] & 0xffffff);
        }
        layer.setRGB(0, 0, w, h, kept, 0, w);
    }

    /// Draws the picture an `<image>` names into the box it gives, fitted
    /// the way its `preserveAspectRatio` says. One that cannot be found or
    /// read leaves its box empty, as it does in a browser.
    private void paintImage(Graphics2D g, SVGImage image) {
        BufferedImage picture = loadPicture(image);
        if (picture == null) {
            return;
        }
        double bw = image.getWidth() > 0 ? image.getWidth() : picture.getWidth();
        double bh = image.getHeight() > 0 ? image.getHeight() : picture.getHeight();
        String par = image.getPreserveAspectRatio() == null ? "" : image.getPreserveAspectRatio().trim();
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
            if (href.regionMatches(true, 0, "data:", 0, 5)) {
                int comma = href.indexOf(',');
                if (comma < 0) {
                    return null;
                }
                String kind = href.substring(5, comma);
                svg = kind.regionMatches(true, 0, "image/svg", 0, 9);
                String payload = href.substring(comma + 1);
                if (kind.length() >= 7 && kind.regionMatches(true, kind.length() - 7, ";base64", 0, 7)) {
                    data = java.util.Base64.getMimeDecoder().decode(payload);
                } else {
                    data = java.net.URLDecoder.decode(payload.replace("+", "%2B"), "UTF-8")
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
                }
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
            return inner.paint(pw, ph, 4096);
        } catch (IOException ex) {
            return null;
        } catch (RuntimeException ex) {
            return null;
        }
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
                    g.clip(new Rectangle2D.Double(port.getX(), port.getY(), port.getWidth(), port.getHeight()));
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
                    fillAndStroke(g, shape, style);
                    if (node instanceof SVGPath || node instanceof SVGLine || node instanceof SVGPolyline) {
                        paintMarkers(g, shape, style, w, h);
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
    /// A point is where a segment of the line ends. A marker that turns
    /// with the line faces the way the line runs there, and where the line
    /// bends, half way between the two directions. An arc is drawn as a
    /// few curves, and the points where those meet count as points of the
    /// line.
    private void paintMarkers(Graphics2D g, Shape shape, SVGStyle style, int w, int h) {
        SVGMarker start = markerOf(style.getMarkerStart());
        SVGMarker mid = markerOf(style.getMarkerMid());
        SVGMarker end = markerOf(style.getMarkerEnd());
        if (start == null && mid == null && end == null) {
            return;
        }
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
            try {
                turn = Math.toRadians(Double.parseDouble(orient.endsWith("deg")
                        ? orient.substring(0, orient.length() - 3) : orient));
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
        Shape outline = clipOutline((SVGClipPath) clip);
        if (((SVGClipPath) clip).isObjectBoundingBox()) {
            // In fractions of the box around what is clipped. With no box
            // to measure, nothing is inside it.
            Rectangle2D box = boundsOf(node);
            outline = box == null || box.isEmpty() ? new Rectangle2D.Double()
                    : new AffineTransform(box.getWidth(), 0, 0, box.getHeight(), box.getX(), box.getY())
                            .createTransformedShape(outline);
        }
        return outline;
    }

    /// Draws the element a `<use>` names, at the offset it gives. What the
    /// element does not set for itself it takes from the `<use>`, as if it
    /// were written there.
    private void paintUse(Graphics2D g, SVGUse use, SVGStyle style, int w, int h) {
        SVGNode target = use.getHref() == null ? null : doc.getDefinitions().get(use.getHref());
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
        try {
            g.transform(placing(use, target));
            paintNode(g, target, style, w, h);
        } finally {
            useDepth--;
        }
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
            SVGNode target = use.getHref() == null ? null : doc.getDefinitions().get(use.getHref());
            if (target == null || depth >= MAX_USE_DEPTH) {
                return null;
            }
            Rectangle2D box = boundsOf(target, depth + 1);
            if (box == null) {
                return null;
            }
            AffineTransform at = placing(use, target);
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
        fillAndStroke(g, shape, style, true);
    }

    /// Fills `shape` and then strokes it. `ruled` is whether `fill-rule`
    /// applies, which it does not to the outlines of text.
    private void fillAndStroke(Graphics2D g, Shape shape, SVGStyle style, boolean ruled) {
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
            SVGNode def = doc.getDefinitions().get(paint.getReference());
            if (def instanceof SVGPattern) {
                paintPattern(g, (SVGPattern) def, alpha, area, boundsOf.getBounds2D());
                return;
            }
            p = gradient(def, boundsOf.getBounds2D());
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
        def = resolved(def);
        List<SVGGradientStop> stops = def instanceof SVGLinearGradient ? ((SVGLinearGradient) def).getStops()
                : def instanceof SVGRadialGradient ? ((SVGRadialGradient) def).getStops() : null;
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
                    paintNode(tg, child, content.getStyle(), iw, ih);
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
        Shape outline = textOutline(text, g.getFontRenderContext());
        if (outline != null) {
            // Filled and then stroked like any other shape: text with no
            // fill and a stroke is an outline, not nothing.
            fillAndStroke(g, outline, style, false);
        }
    }

    /// The outlines of the glyphs of a `<text>`, where it puts them, or
    /// null when it has no text.
    private static Shape textOutline(SVGText text, java.awt.font.FontRenderContext context) {
        String content = text.getContent();
        if (content == null || content.length() == 0) {
            return null;
        }
        float size = text.getFontSize() > 0 ? text.getFontSize() : 12f;
        int fontStyle = (text.isBold() ? Font.BOLD : 0) | (text.isItalic() ? Font.ITALIC : 0);
        Font font = new Font(familyOf(text.getFontFamily()), fontStyle, 1).deriveFont(size);
        java.awt.font.GlyphVector glyphs = font.createGlyphVector(context, content);
        double advance = glyphs.getLogicalBounds().getWidth();
        double x = text.getX();
        if (text.getAnchor() == SVGText.Anchor.MIDDLE) {
            x -= advance / 2.0;
        } else if (text.getAnchor() == SVGText.Anchor.END) {
            x -= advance;
        }
        return glyphs.getOutline((float) x, text.getY());
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
