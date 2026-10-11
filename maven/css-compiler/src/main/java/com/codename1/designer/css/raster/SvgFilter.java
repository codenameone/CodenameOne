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

import com.codename1.svg.transcoder.model.SVGFilter;
import com.codename1.svg.transcoder.model.SVGFilter.Primitive;
import com.codename1.svg.transcoder.parser.ColorParser;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/// The effects of an SVG `<filter>`, worked out on the pixels of the element
/// it is on.
///
/// The element is painted first, on a layer of its own. The filter's region
/// of that layer is then taken through the effects in the order they are
/// written, each reading the result of the one before it or one it names,
/// and the last result replaces the element. Nothing of it is left outside
/// the region.
///
/// Colours are worked on as linear light unless
/// `color-interpolation-filters` says `sRGB`, and with their opacity
/// multiplied in. Lengths are those of the filtered element: a blur of 2 on
/// an element drawn at three times its size is a blur of 6 pixels.
///
/// `feImage` is not read; its result is empty, as are the page behind the
/// element and its fill and stroke as inputs, which no browser gives a
/// filter either.
final class SvgFilter {
    /// One image in the chain: four planes, colour multiplied by opacity.
    private static final class Plane {
        final float[] r;
        final float[] g;
        final float[] b;
        final float[] a;
        boolean linear;
        /// The part of the region this one covers.
        int x0;
        int y0;
        int x1;
        int y1;

        Plane(int w, int h, boolean linear) {
            r = new float[w * h];
            g = new float[w * h];
            b = new float[w * h];
            a = new float[w * h];
            this.linear = linear;
            x1 = w;
            y1 = h;
        }

        float[] channel(int c) {
            return c == 0 ? r : c == 1 ? g : c == 2 ? b : a;
        }
    }

    private static final float[] TO_LINEAR = new float[256];

    static {
        for (int i = 0; i < 256; i++) {
            TO_LINEAR[i] = (float) toLinear(i / 255.0);
        }
    }

    private final int w;
    private final int h;
    /// From the coordinates of the filtered element to the pixels of the region.
    private final AffineTransform at;
    /// Pixels to one of the lengths an effect is written in, each way.
    private final double ux;
    private final double uy;
    private final Rectangle2D box;
    private final boolean boxUnits;
    private final Map<String, Plane> named = new HashMap<String, Plane>();

    private SvgFilter(int w, int h, AffineTransform at, Rectangle2D box, boolean boxUnits) {
        this.w = w;
        this.h = h;
        this.at = at;
        this.box = box;
        this.boxUnits = boxUnits && box != null;
        double sx = Math.hypot(at.getScaleX(), at.getShearY());
        double sy = Math.hypot(at.getShearX(), at.getScaleY());
        this.ux = this.boxUnits ? sx * box.getWidth() : sx;
        this.uy = this.boxUnits ? sy * box.getHeight() : sy;
    }

    /// Takes `layer`, which holds one element and nothing else, through
    /// `filter`.
    ///
    /// #### Parameters
    ///
    /// - `box`: the bounding box of the element in its own coordinates, or
    ///   null when it has none to measure
    ///
    /// - `device`: from the element's coordinates to the pixels of `layer`
    static void apply(BufferedImage layer, SVGFilter filter, Rectangle2D box, AffineTransform device) {
        int lw = layer.getWidth();
        int lh = layer.getHeight();
        boolean boxed = box != null && box.getWidth() > 0 && box.getHeight() > 0;
        Rectangle2D region = null;
        if (filter.isRegionUserSpace()) {
            region = new Rectangle2D.Double(filter.getRegionX(), filter.getRegionY(),
                    filter.getRegionWidth(), filter.getRegionHeight());
        } else if (boxed) {
            region = new Rectangle2D.Double(box.getX() + filter.getRegionX() * box.getWidth(),
                    box.getY() + filter.getRegionY() * box.getHeight(),
                    filter.getRegionWidth() * box.getWidth(), filter.getRegionHeight() * box.getHeight());
        }
        // With no box to take a share of, the region is all there is.
        Rectangle whole = new Rectangle(0, 0, lw, lh);
        Rectangle px = region == null ? whole : device.createTransformedShape(region).getBounds().intersection(whole);
        int[] all = layer.getRGB(0, 0, lw, lh, null, 0, lw);
        int[] out = new int[all.length];
        if (!px.isEmpty()) {
            AffineTransform at = AffineTransform.getTranslateInstance(-px.x, -px.y);
            at.concatenate(device);
            SvgFilter f = new SvgFilter(px.width, px.height, at, boxed ? box : null,
                    filter.isPrimitiveObjectBoundingBox());
            Plane source = new Plane(px.width, px.height, false);
            for (int y = 0; y < px.height; y++) {
                for (int x = 0; x < px.width; x++) {
                    int argb = all[(y + px.y) * lw + x + px.x];
                    float a = (argb >>> 24) / 255f;
                    int i = y * px.width + x;
                    source.a[i] = a;
                    source.r[i] = ((argb >> 16) & 0xff) / 255f * a;
                    source.g[i] = ((argb >> 8) & 0xff) / 255f * a;
                    source.b[i] = (argb & 0xff) / 255f * a;
                }
            }
            Plane result = f.in(f.run(filter, source), false);
            for (int y = 0; y < px.height; y++) {
                for (int x = 0; x < px.width; x++) {
                    int i = y * px.width + x;
                    float a = clamp(result.a[i]);
                    if (a <= 0f) {
                        continue;
                    }
                    out[(y + px.y) * lw + x + px.x] = (Math.round(a * 255f) << 24)
                            | (Math.round(clamp(result.r[i] / a) * 255f) << 16)
                            | (Math.round(clamp(result.g[i] / a) * 255f) << 8)
                            | Math.round(clamp(result.b[i] / a) * 255f);
                }
            }
        }
        layer.setRGB(0, 0, lw, lh, out, 0, lw);
    }

    private Plane run(SVGFilter filter, Plane source) {
        // A filter with no effects in it leaves nothing of the element.
        Plane last = new Plane(w, h, false);
        boolean first = true;
        for (Primitive p : filter.getPrimitives()) {
            if (!p.getName().startsWith("fe")) {
                continue;
            }
            String space = p.get("color-interpolation-filters");
            boolean linear = space == null ? !filter.isSrgb() : !"sRGB".equalsIgnoreCase(space.trim());
            Plane out = effect(p, first ? source : last, source, linear);
            first = false;
            bound(p, out);
            String name = p.get("result");
            if (name != null && name.trim().length() > 0) {
                named.put(name.trim(), out);
            }
            last = out;
        }
        return last;
    }

    private Plane effect(Primitive p, Plane last, Plane source, boolean linear) {
        String name = p.getName();
        if ("feGaussianBlur".equals(name)) {
            double[] sd = numbers(p.get("stdDeviation"));
            double sdx = sd.length > 0 ? sd[0] : 0;
            double sdy = sd.length > 1 ? sd[1] : sdx;
            return blur(input(p, "in", last, source, linear), sdx * ux, sdy * uy);
        }
        if ("feOffset".equals(name)) {
            return offset(input(p, "in", last, source, linear), number(p, "dx", 0), number(p, "dy", 0));
        }
        if ("feFlood".equals(name)) {
            return flood(p.get("flood-color"), (float) number(p, "flood-opacity", 1), linear);
        }
        if ("feColorMatrix".equals(name)) {
            return colorMatrix(p, input(p, "in", last, source, linear));
        }
        if ("feComponentTransfer".equals(name)) {
            return componentTransfer(p, input(p, "in", last, source, linear));
        }
        if ("feComposite".equals(name)) {
            return composite(p, input(p, "in", last, source, linear), input(p, "in2", last, source, linear));
        }
        if ("feBlend".equals(name)) {
            return blend(p.get("mode"), input(p, "in", last, source, linear), input(p, "in2", last, source, linear));
        }
        if ("feMerge".equals(name)) {
            Plane out = new Plane(w, h, linear);
            for (Primitive node : p.getChildren()) {
                if ("feMergeNode".equals(node.getName())) {
                    out = over(input(node, "in", last, source, linear), out);
                }
            }
            return out;
        }
        if ("feDropShadow".equals(name)) {
            Plane in = input(p, "in", last, source, linear);
            double[] sd = numbers(p.get("stdDeviation"));
            double sdx = sd.length > 0 ? sd[0] : 2;
            double sdy = sd.length > 1 ? sd[1] : sdx;
            Plane shadow = offset(blur(alphaOf(in), sdx * ux, sdy * uy), number(p, "dx", 2), number(p, "dy", 2));
            Plane colour = flood(p.get("flood-color"), (float) number(p, "flood-opacity", 1), linear);
            for (int i = 0; i < w * h; i++) {
                float a = shadow.a[i];
                shadow.r[i] = colour.r[i] * a;
                shadow.g[i] = colour.g[i] * a;
                shadow.b[i] = colour.b[i] * a;
                shadow.a[i] = colour.a[i] * a;
            }
            return over(in, shadow);
        }
        if ("feMorphology".equals(name)) {
            return morphology(p, input(p, "in", last, source, linear));
        }
        if ("feTile".equals(name)) {
            return tile(input(p, "in", last, source, linear));
        }
        if ("feConvolveMatrix".equals(name)) {
            return convolve(p, input(p, "in", last, source, linear));
        }
        if ("feDisplacementMap".equals(name)) {
            return displace(p, input(p, "in", last, source, linear), input(p, "in2", last, source, linear));
        }
        if ("feTurbulence".equals(name)) {
            return turbulence(p, linear);
        }
        if ("feDiffuseLighting".equals(name) || "feSpecularLighting".equals(name)) {
            return lighting(p, input(p, "in", last, source, linear), linear, "feSpecularLighting".equals(name));
        }
        return new Plane(w, h, linear);
    }

    /// The image an effect reads: the result before it, one that was given
    /// a name, or the element itself.
    private Plane input(Primitive p, String attribute, Plane last, Plane source, boolean linear) {
        String name = p.get(attribute);
        name = name == null ? "" : name.trim();
        Plane found;
        if (name.length() == 0) {
            found = last;
        } else if ("SourceGraphic".equals(name)) {
            found = source;
        } else if ("SourceAlpha".equals(name)) {
            found = alphaOf(source);
        } else if ("BackgroundImage".equals(name) || "BackgroundAlpha".equals(name)
                || "FillPaint".equals(name) || "StrokePaint".equals(name)) {
            found = new Plane(w, h, linear);
        } else {
            found = named.containsKey(name) ? named.get(name) : last;
        }
        return in(found, linear);
    }

    /// `plane` in the colours an effect works in. A copy when they differ.
    private Plane in(Plane plane, boolean linear) {
        if (plane.linear == linear) {
            return plane;
        }
        Plane out = new Plane(w, h, linear);
        out.x0 = plane.x0;
        out.y0 = plane.y0;
        out.x1 = plane.x1;
        out.y1 = plane.y1;
        for (int i = 0; i < w * h; i++) {
            float a = plane.a[i];
            out.a[i] = a;
            if (a <= 0f) {
                continue;
            }
            out.r[i] = convert(plane.r[i] / a, linear) * a;
            out.g[i] = convert(plane.g[i] / a, linear) * a;
            out.b[i] = convert(plane.b[i] / a, linear) * a;
        }
        return out;
    }

    private static float convert(float v, boolean toLinear) {
        v = clamp(v);
        return (float) (toLinear ? toLinear(v) : toSrgb(v));
    }

    private static double toLinear(double v) {
        return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }

    private static double toSrgb(double v) {
        return v <= 0.0031308 ? v * 12.92 : 1.055 * Math.pow(v, 1 / 2.4) - 0.055;
    }

    /// Cuts a result down to the part of the region its effect is given
    /// with `x`, `y`, `width` and `height`.
    private void bound(Primitive p, Plane out) {
        if (p.get("x") == null && p.get("y") == null && p.get("width") == null && p.get("height") == null) {
            return;
        }
        AffineTransform back;
        try {
            back = at.createInverse();
        } catch (NoninvertibleTransformException ex) {
            return;
        }
        // Sides left out are those of the region.
        Rectangle2D all = back.createTransformedShape(new Rectangle2D.Double(0, 0, w, h)).getBounds2D();
        double x = side(p, "x", all.getX(), true, false);
        double y = side(p, "y", all.getY(), false, false);
        double width = side(p, "width", all.getWidth(), true, true);
        double height = side(p, "height", all.getHeight(), false, true);
        Rectangle part = at.createTransformedShape(new Rectangle2D.Double(x, y, width, height)).getBounds()
                .intersection(new Rectangle(0, 0, w, h));
        out.x0 = part.x;
        out.y0 = part.y;
        out.x1 = part.x + Math.max(0, part.width);
        out.y1 = part.y + Math.max(0, part.height);
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                if (px < out.x0 || px >= out.x1 || py < out.y0 || py >= out.y1) {
                    int i = py * w + px;
                    out.r[i] = 0;
                    out.g[i] = 0;
                    out.b[i] = 0;
                    out.a[i] = 0;
                }
            }
        }
    }

    private double side(Primitive p, String attribute, double fallback, boolean horizontal, boolean size) {
        String value = p.get(attribute);
        if (value == null) {
            return fallback;
        }
        double[] v = numbers(value);
        if (v.length == 0) {
            return fallback;
        }
        if (!boxUnits) {
            return v[0];
        }
        double scaled = v[0] * (horizontal ? box.getWidth() : box.getHeight());
        return size ? scaled : scaled + (horizontal ? box.getX() : box.getY());
    }

    private Plane alphaOf(Plane in) {
        Plane out = new Plane(w, h, in.linear);
        System.arraycopy(in.a, 0, out.a, 0, w * h);
        return out;
    }

    private Plane copy(Plane in) {
        Plane out = new Plane(w, h, in.linear);
        for (int c = 0; c < 4; c++) {
            System.arraycopy(in.channel(c), 0, out.channel(c), 0, w * h);
        }
        return out;
    }

    private Plane blur(Plane in, double sx, double sy) {
        Plane out = copy(in);
        if (sx < 0 || sy < 0 || (sx == 0 && sy == 0)) {
            return out;
        }
        for (int c = 0; c < 4; c++) {
            float[] data = out.channel(c);
            if (Math.abs(sx - sy) < 1e-6) {
                GaussianBlur.blur(data, w, h, sx);
            } else {
                // A different amount each way: one row of the blur at a time.
                if (sx > 0) {
                    float[] k = GaussianBlur.kernel(sx);
                    for (int y = 0; y < h; y++) {
                        line(data, y * w, 1, w, k);
                    }
                }
                if (sy > 0) {
                    float[] k = GaussianBlur.kernel(sy);
                    for (int x = 0; x < w; x++) {
                        line(data, x, w, h, k);
                    }
                }
            }
        }
        return out;
    }

    private static void line(float[] data, int off, int stride, int len, float[] k) {
        float[] src = new float[len];
        for (int i = 0; i < len; i++) {
            src[i] = data[off + i * stride];
        }
        int r = k.length / 2;
        for (int i = 0; i < len; i++) {
            float sum = 0;
            for (int j = Math.max(0, i - r); j <= Math.min(len - 1, i + r); j++) {
                sum += src[j] * k[j - i + r];
            }
            data[off + i * stride] = sum;
        }
    }

    private Plane offset(Plane in, double dx, double dy) {
        // The offset is a direction in the element's coordinates.
        Point2D moved = at.deltaTransform(new Point2D.Double(boxUnits ? dx * box.getWidth() : dx,
                boxUnits ? dy * box.getHeight() : dy), null);
        int ox = (int) Math.round(moved.getX());
        int oy = (int) Math.round(moved.getY());
        Plane out = new Plane(w, h, in.linear);
        for (int y = 0; y < h; y++) {
            int fromY = y - oy;
            if (fromY < 0 || fromY >= h) {
                continue;
            }
            for (int x = 0; x < w; x++) {
                int fromX = x - ox;
                if (fromX < 0 || fromX >= w) {
                    continue;
                }
                int i = y * w + x;
                int j = fromY * w + fromX;
                out.r[i] = in.r[j];
                out.g[i] = in.g[j];
                out.b[i] = in.b[j];
                out.a[i] = in.a[j];
            }
        }
        return out;
    }

    /// A colour as three values in the colours being worked in.
    private static float[] colour(String value, boolean linear, int fallback) {
        int argb = fallback;
        if (value != null && !ColorParser.isCurrentColor(value)) {
            argb = ColorParser.parseOrDefault(value, fallback);
        }
        float[] out = {((argb >> 16) & 0xff) / 255f, ((argb >> 8) & 0xff) / 255f, (argb & 0xff) / 255f,
            (argb >>> 24) / 255f};
        if (linear) {
            out[0] = TO_LINEAR[(argb >> 16) & 0xff];
            out[1] = TO_LINEAR[(argb >> 8) & 0xff];
            out[2] = TO_LINEAR[argb & 0xff];
        }
        return out;
    }

    private Plane flood(String value, float opacity, boolean linear) {
        float[] c = colour(value, linear, 0xff000000);
        float a = clamp(opacity) * c[3];
        Plane out = new Plane(w, h, linear);
        java.util.Arrays.fill(out.r, c[0] * a);
        java.util.Arrays.fill(out.g, c[1] * a);
        java.util.Arrays.fill(out.b, c[2] * a);
        java.util.Arrays.fill(out.a, a);
        return out;
    }

    private Plane colorMatrix(Primitive p, Plane in) {
        String type = p.get("type") == null ? "matrix" : p.get("type").trim();
        double[] v = numbers(p.get("values"));
        double[] m = {1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0};
        if ("matrix".equals(type)) {
            if (v.length == 20) {
                m = v;
            }
        } else if ("saturate".equals(type)) {
            double s = v.length > 0 ? v[0] : 1;
            m = new double[] {0.213 + 0.787 * s, 0.715 - 0.715 * s, 0.072 - 0.072 * s, 0, 0,
                0.213 - 0.213 * s, 0.715 + 0.285 * s, 0.072 - 0.072 * s, 0, 0,
                0.213 - 0.213 * s, 0.715 - 0.715 * s, 0.072 + 0.928 * s, 0, 0,
                0, 0, 0, 1, 0};
        } else if ("hueRotate".equals(type)) {
            double angle = Math.toRadians(v.length > 0 ? v[0] : 0);
            double c = Math.cos(angle);
            double s = Math.sin(angle);
            m = new double[] {0.213 + c * 0.787 - s * 0.213, 0.715 - c * 0.715 - s * 0.715,
                0.072 - c * 0.072 + s * 0.928, 0, 0,
                0.213 - c * 0.213 + s * 0.143, 0.715 + c * 0.285 + s * 0.140,
                0.072 - c * 0.072 - s * 0.283, 0, 0,
                0.213 - c * 0.213 - s * 0.787, 0.715 - c * 0.715 + s * 0.715,
                0.072 + c * 0.928 + s * 0.072, 0, 0,
                0, 0, 0, 1, 0};
        } else if ("luminanceToAlpha".equals(type)) {
            m = new double[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.2125, 0.7154, 0.0721, 0, 0};
        }
        Plane out = new Plane(w, h, in.linear);
        for (int i = 0; i < w * h; i++) {
            float a = in.a[i];
            float r = a > 0 ? in.r[i] / a : 0;
            float g = a > 0 ? in.g[i] / a : 0;
            float b = a > 0 ? in.b[i] / a : 0;
            float na = clamp((float) (m[15] * r + m[16] * g + m[17] * b + m[18] * a + m[19]));
            out.a[i] = na;
            out.r[i] = clamp((float) (m[0] * r + m[1] * g + m[2] * b + m[3] * a + m[4])) * na;
            out.g[i] = clamp((float) (m[5] * r + m[6] * g + m[7] * b + m[8] * a + m[9])) * na;
            out.b[i] = clamp((float) (m[10] * r + m[11] * g + m[12] * b + m[13] * a + m[14])) * na;
        }
        return out;
    }

    private Plane componentTransfer(Primitive p, Plane in) {
        Primitive[] functions = new Primitive[4];
        for (Primitive f : p.getChildren()) {
            String n = f.getName();
            int c = "feFuncR".equals(n) ? 0 : "feFuncG".equals(n) ? 1 : "feFuncB".equals(n) ? 2
                    : "feFuncA".equals(n) ? 3 : -1;
            if (c >= 0) {
                functions[c] = f;
            }
        }
        Plane out = new Plane(w, h, in.linear);
        for (int i = 0; i < w * h; i++) {
            float a = in.a[i];
            float na = transfer(functions[3], a);
            out.a[i] = na;
            out.r[i] = transfer(functions[0], a > 0 ? in.r[i] / a : 0) * na;
            out.g[i] = transfer(functions[1], a > 0 ? in.g[i] / a : 0) * na;
            out.b[i] = transfer(functions[2], a > 0 ? in.b[i] / a : 0) * na;
        }
        return out;
    }

    private static float transfer(Primitive f, float c) {
        if (f == null) {
            return c;
        }
        String type = f.get("type") == null ? "identity" : f.get("type").trim();
        if ("linear".equals(type)) {
            return clamp((float) (number(f, "slope", 1) * c + number(f, "intercept", 0)));
        }
        if ("gamma".equals(type)) {
            return clamp((float) (number(f, "amplitude", 1) * Math.pow(c, number(f, "exponent", 1))
                    + number(f, "offset", 0)));
        }
        boolean table = "table".equals(type);
        if (table || "discrete".equals(type)) {
            double[] v = numbers(f.get("tableValues"));
            if (v.length == 0) {
                return c;
            }
            if (table) {
                int n = v.length - 1;
                if (n == 0) {
                    return clamp((float) v[0]);
                }
                int k = Math.min(n - 1, (int) (c * n));
                return clamp((float) (v[k] + (c * n - k) * (v[k + 1] - v[k])));
            }
            return clamp((float) v[Math.min(v.length - 1, (int) (c * v.length))]);
        }
        return c;
    }

    private Plane over(Plane top, Plane bottom) {
        Plane out = new Plane(w, h, top.linear);
        for (int i = 0; i < w * h; i++) {
            float keep = 1 - top.a[i];
            out.r[i] = top.r[i] + bottom.r[i] * keep;
            out.g[i] = top.g[i] + bottom.g[i] * keep;
            out.b[i] = top.b[i] + bottom.b[i] * keep;
            out.a[i] = top.a[i] + bottom.a[i] * keep;
        }
        return out;
    }

    private Plane composite(Primitive p, Plane a, Plane b) {
        String op = p.get("operator") == null ? "over" : p.get("operator").trim();
        if ("over".equals(op)) {
            return over(a, b);
        }
        float k1 = (float) number(p, "k1", 0);
        float k2 = (float) number(p, "k2", 0);
        float k3 = (float) number(p, "k3", 0);
        float k4 = (float) number(p, "k4", 0);
        Plane out = new Plane(w, h, a.linear);
        for (int i = 0; i < w * h; i++) {
            float fa;
            float fb;
            if ("in".equals(op)) {
                fa = b.a[i];
                fb = 0;
            } else if ("out".equals(op)) {
                fa = 1 - b.a[i];
                fb = 0;
            } else if ("atop".equals(op)) {
                fa = b.a[i];
                fb = 1 - a.a[i];
            } else if ("xor".equals(op)) {
                fa = 1 - b.a[i];
                fb = 1 - a.a[i];
            } else if ("lighter".equals(op)) {
                fa = 1;
                fb = 1;
            } else if ("arithmetic".equals(op)) {
                float na = clamp(k1 * a.a[i] * b.a[i] + k2 * a.a[i] + k3 * b.a[i] + k4);
                out.a[i] = na;
                out.r[i] = Math.min(na, clamp(k1 * a.r[i] * b.r[i] + k2 * a.r[i] + k3 * b.r[i] + k4));
                out.g[i] = Math.min(na, clamp(k1 * a.g[i] * b.g[i] + k2 * a.g[i] + k3 * b.g[i] + k4));
                out.b[i] = Math.min(na, clamp(k1 * a.b[i] * b.b[i] + k2 * a.b[i] + k3 * b.b[i] + k4));
                continue;
            } else {
                fa = 1;
                fb = 1 - a.a[i];
            }
            out.a[i] = clamp(a.a[i] * fa + b.a[i] * fb);
            out.r[i] = Math.min(out.a[i], a.r[i] * fa + b.r[i] * fb);
            out.g[i] = Math.min(out.a[i], a.g[i] * fa + b.g[i] * fb);
            out.b[i] = Math.min(out.a[i], a.b[i] * fa + b.b[i] * fb);
        }
        return out;
    }

    /// `top` blended onto `bottom` the way `mode` says.
    private Plane blend(String mode, Plane top, Plane bottom) {
        String m = mode == null ? "normal" : mode.trim();
        if ("normal".equals(m)) {
            return over(top, bottom);
        }
        Plane out = new Plane(w, h, top.linear);
        for (int i = 0; i < w * h; i++) {
            float as = top.a[i];
            float ab = bottom.a[i];
            out.a[i] = as + ab - as * ab;
            for (int c = 0; c < 3; c++) {
                float ps = top.channel(c)[i];
                float pb = bottom.channel(c)[i];
                float cs = as > 0 ? ps / as : 0;
                float cb = ab > 0 ? pb / ab : 0;
                out.channel(c)[i] = ps * (1 - ab) + pb * (1 - as) + as * ab * clamp(mix(m, cb, cs));
            }
        }
        return out;
    }

    private static float mix(String mode, float cb, float cs) {
        if ("multiply".equals(mode)) {
            return cb * cs;
        }
        if ("screen".equals(mode)) {
            return cb + cs - cb * cs;
        }
        if ("darken".equals(mode)) {
            return Math.min(cb, cs);
        }
        if ("lighten".equals(mode)) {
            return Math.max(cb, cs);
        }
        if ("overlay".equals(mode)) {
            return mix("hard-light", cs, cb);
        }
        if ("hard-light".equals(mode)) {
            return cs <= 0.5f ? cb * 2 * cs : mix("screen", cb, 2 * cs - 1);
        }
        if ("color-dodge".equals(mode)) {
            return cb == 0 ? 0 : cs >= 1 ? 1 : Math.min(1, cb / (1 - cs));
        }
        if ("color-burn".equals(mode)) {
            return cb >= 1 ? 1 : cs <= 0 ? 0 : 1 - Math.min(1, (1 - cb) / cs);
        }
        if ("soft-light".equals(mode)) {
            if (cs <= 0.5f) {
                return cb - (1 - 2 * cs) * cb * (1 - cb);
            }
            float d = cb <= 0.25f ? ((16 * cb - 12) * cb + 4) * cb : (float) Math.sqrt(cb);
            return cb + (2 * cs - 1) * (d - cb);
        }
        if ("difference".equals(mode)) {
            return Math.abs(cb - cs);
        }
        if ("exclusion".equals(mode)) {
            return cb + cs - 2 * cb * cs;
        }
        return cs;
    }

    private Plane morphology(Primitive p, Plane in) {
        double[] radius = numbers(p.get("radius"));
        double rx = radius.length > 0 ? radius[0] : 0;
        double ry = radius.length > 1 ? radius[1] : rx;
        int px = (int) Math.round(rx * ux);
        int py = (int) Math.round(ry * uy);
        if (rx <= 0 || ry <= 0) {
            return copy(in);
        }
        boolean dilate = p.get("operator") != null && "dilate".equals(p.get("operator").trim());
        Plane out = new Plane(w, h, in.linear);
        float[] row = new float[w * h];
        for (int c = 0; c < 4; c++) {
            float[] src = in.channel(c);
            float[] dst = out.channel(c);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    float v = dilate ? 0f : 1f;
                    for (int k = x - px; k <= x + px; k++) {
                        float s = k < 0 || k >= w ? 0f : src[y * w + k];
                        v = dilate ? Math.max(v, s) : Math.min(v, s);
                    }
                    row[y * w + x] = v;
                }
            }
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    float v = dilate ? 0f : 1f;
                    for (int k = y - py; k <= y + py; k++) {
                        float s = k < 0 || k >= h ? 0f : row[k * w + x];
                        v = dilate ? Math.max(v, s) : Math.min(v, s);
                    }
                    dst[y * w + x] = v;
                }
            }
        }
        return out;
    }

    /// Repeats the part of the region `in` covers over all of it.
    private Plane tile(Plane in) {
        int tw = in.x1 - in.x0;
        int th = in.y1 - in.y0;
        if (tw <= 0 || th <= 0) {
            return new Plane(w, h, in.linear);
        }
        Plane out = new Plane(w, h, in.linear);
        for (int y = 0; y < h; y++) {
            int fromY = in.y0 + Math.floorMod(y - in.y0, th);
            for (int x = 0; x < w; x++) {
                int j = fromY * w + in.x0 + Math.floorMod(x - in.x0, tw);
                int i = y * w + x;
                out.r[i] = in.r[j];
                out.g[i] = in.g[j];
                out.b[i] = in.b[j];
                out.a[i] = in.a[j];
            }
        }
        return out;
    }

    private Plane convolve(Primitive p, Plane in) {
        double[] order = numbers(p.get("order"));
        int ox = order.length > 0 ? (int) order[0] : 3;
        int oy = order.length > 1 ? (int) order[1] : ox;
        double[] k = numbers(p.get("kernelMatrix"));
        if (ox <= 0 || oy <= 0 || k.length != ox * oy) {
            return new Plane(w, h, in.linear);
        }
        double sum = 0;
        for (double v : k) {
            sum += v;
        }
        double divisor = number(p, "divisor", 0);
        if (divisor == 0) {
            divisor = sum == 0 ? 1 : sum;
        }
        double bias = number(p, "bias", 0);
        int tx = (int) number(p, "targetX", ox / 2);
        int ty = (int) number(p, "targetY", oy / 2);
        String edge = p.get("edgeMode") == null ? "duplicate" : p.get("edgeMode").trim();
        boolean keepAlpha = p.get("preserveAlpha") != null && "true".equals(p.get("preserveAlpha").trim());
        Plane out = new Plane(w, h, in.linear);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double[] acc = new double[4];
                for (int j = 0; j < oy; j++) {
                    for (int i = 0; i < ox; i++) {
                        int sx = x - tx + i;
                        int sy = y - ty + j;
                        if ("wrap".equals(edge)) {
                            sx = Math.floorMod(sx, w);
                            sy = Math.floorMod(sy, h);
                        } else if ("none".equals(edge)) {
                            if (sx < 0 || sx >= w || sy < 0 || sy >= h) {
                                continue;
                            }
                        } else {
                            sx = Math.max(0, Math.min(w - 1, sx));
                            sy = Math.max(0, Math.min(h - 1, sy));
                        }
                        double weight = k[(oy - 1 - j) * ox + (ox - 1 - i)];
                        int s = sy * w + sx;
                        float a = in.a[s];
                        // With the opacity kept, the colours are worked on
                        // without it multiplied in.
                        float un = keepAlpha && a > 0 ? 1 / a : 1;
                        acc[0] += weight * in.r[s] * un;
                        acc[1] += weight * in.g[s] * un;
                        acc[2] += weight * in.b[s] * un;
                        acc[3] += weight * a;
                    }
                }
                int d = y * w + x;
                float a = keepAlpha ? in.a[d] : clamp((float) (acc[3] / divisor + bias));
                float cap = keepAlpha ? 1f : a;
                float scale = keepAlpha ? a : 1f;
                out.a[d] = a;
                out.r[d] = Math.min(cap, clamp((float) (acc[0] / divisor + bias * cap))) * scale;
                out.g[d] = Math.min(cap, clamp((float) (acc[1] / divisor + bias * cap))) * scale;
                out.b[d] = Math.min(cap, clamp((float) (acc[2] / divisor + bias * cap))) * scale;
            }
        }
        return out;
    }

    private Plane displace(Primitive p, Plane in, Plane map) {
        double scale = number(p, "scale", 0);
        int xc = selector(p.get("xChannelSelector"));
        int yc = selector(p.get("yChannelSelector"));
        Plane out = new Plane(w, h, in.linear);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                float a = map.a[i];
                float vx = xc == 3 ? a : a > 0 ? map.channel(xc)[i] / a : 0;
                float vy = yc == 3 ? a : a > 0 ? map.channel(yc)[i] / a : 0;
                int fromX = (int) Math.round(x + scale * ux * (vx - 0.5));
                int fromY = (int) Math.round(y + scale * uy * (vy - 0.5));
                if (fromX < 0 || fromX >= w || fromY < 0 || fromY >= h) {
                    continue;
                }
                int j = fromY * w + fromX;
                out.r[i] = in.r[j];
                out.g[i] = in.g[j];
                out.b[i] = in.b[j];
                out.a[i] = in.a[j];
            }
        }
        return out;
    }

    private static int selector(String value) {
        String v = value == null ? "A" : value.trim();
        return "R".equals(v) ? 0 : "G".equals(v) ? 1 : "B".equals(v) ? 2 : 3;
    }

    // ---- feTurbulence, by the reference code of the specification.

    private static final int NOISE_SIZE = 0x100;
    private static final int NOISE_MASK = 0xff;
    private static final int PERLIN_N = 0x1000;

    private static final class Noise {
        final int[] lattice = new int[NOISE_SIZE + NOISE_SIZE + 2];
        final double[][][] gradient = new double[4][NOISE_SIZE + NOISE_SIZE + 2][2];

        Noise(long seed) {
            if (seed <= 0) {
                seed = -(seed % 2147483646L) + 1;
            }
            if (seed > 2147483646L) {
                seed = 2147483646L;
            }
            for (int k = 0; k < 4; k++) {
                for (int i = 0; i < NOISE_SIZE; i++) {
                    lattice[i] = i;
                    for (int j = 0; j < 2; j++) {
                        seed = random(seed);
                        gradient[k][i][j] = (double) ((seed % (NOISE_SIZE + NOISE_SIZE)) - NOISE_SIZE) / NOISE_SIZE;
                    }
                    double s = Math.sqrt(gradient[k][i][0] * gradient[k][i][0]
                            + gradient[k][i][1] * gradient[k][i][1]);
                    if (s > 0) {
                        gradient[k][i][0] /= s;
                        gradient[k][i][1] /= s;
                    }
                }
            }
            for (int i = NOISE_SIZE - 1; i > 0; i--) {
                int k = lattice[i];
                seed = random(seed);
                int j = (int) (seed % NOISE_SIZE);
                lattice[i] = lattice[j];
                lattice[j] = k;
            }
            for (int i = 0; i < NOISE_SIZE + 2; i++) {
                lattice[NOISE_SIZE + i] = lattice[i];
                for (int k = 0; k < 4; k++) {
                    gradient[k][NOISE_SIZE + i][0] = gradient[k][i][0];
                    gradient[k][NOISE_SIZE + i][1] = gradient[k][i][1];
                }
            }
        }

        private static long random(long seed) {
            long result = 16807L * (seed % 127773L) - 2836L * (seed / 127773L);
            return result <= 0 ? result + 2147483647L : result;
        }

        double noise(int channel, double x, double y) {
            double t = x + PERLIN_N;
            int bx0 = ((int) t) & NOISE_MASK;
            int bx1 = (bx0 + 1) & NOISE_MASK;
            double rx0 = t - (long) t;
            double rx1 = rx0 - 1.0;
            t = y + PERLIN_N;
            int by0 = ((int) t) & NOISE_MASK;
            int by1 = (by0 + 1) & NOISE_MASK;
            double ry0 = t - (long) t;
            double ry1 = ry0 - 1.0;
            int i = lattice[bx0];
            int j = lattice[bx1];
            int b00 = lattice[i + by0];
            int b10 = lattice[j + by0];
            int b01 = lattice[i + by1];
            int b11 = lattice[j + by1];
            double sx = rx0 * rx0 * (3.0 - 2.0 * rx0);
            double sy = ry0 * ry0 * (3.0 - 2.0 * ry0);
            double[][] g = gradient[channel];
            double u = rx0 * g[b00][0] + ry0 * g[b00][1];
            double v = rx1 * g[b10][0] + ry0 * g[b10][1];
            double a = u + sx * (v - u);
            u = rx0 * g[b01][0] + ry1 * g[b01][1];
            v = rx1 * g[b11][0] + ry1 * g[b11][1];
            double b = u + sx * (v - u);
            return a + sy * (b - a);
        }
    }

    private Plane turbulence(Primitive p, boolean linear) {
        double[] f = numbers(p.get("baseFrequency"));
        double fx = f.length > 0 ? f[0] : 0;
        double fy = f.length > 1 ? f[1] : fx;
        int octaves = (int) number(p, "numOctaves", 1);
        boolean fractal = p.get("type") != null && "fractalNoise".equals(p.get("type").trim());
        Noise noise = new Noise(Math.round(number(p, "seed", 0)));
        Plane out = new Plane(w, h, linear);
        AffineTransform back;
        try {
            back = at.createInverse();
        } catch (NoninvertibleTransformException ex) {
            return out;
        }
        Point2D.Double point = new Point2D.Double();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                point.setLocation(x, y);
                back.transform(point, point);
                float[] v = new float[4];
                for (int c = 0; c < 4; c++) {
                    double sum = 0;
                    double vx = point.x * fx;
                    double vy = point.y * fy;
                    double ratio = 1;
                    for (int o = 0; o < octaves; o++) {
                        double n = noise.noise(c, vx, vy);
                        sum += (fractal ? n : Math.abs(n)) / ratio;
                        vx *= 2;
                        vy *= 2;
                        ratio *= 2;
                    }
                    v[c] = clamp((float) (fractal ? (sum + 1) / 2 : sum));
                }
                int i = y * w + x;
                out.a[i] = v[3];
                out.r[i] = v[0] * v[3];
                out.g[i] = v[1] * v[3];
                out.b[i] = v[2] * v[3];
            }
        }
        return out;
    }

    // ---- feDiffuseLighting and feSpecularLighting.

    private Plane lighting(Primitive p, Plane in, boolean linear, boolean specular) {
        Plane out = new Plane(w, h, linear);
        Primitive light = null;
        for (Primitive child : p.getChildren()) {
            String n = child.getName();
            if ("feDistantLight".equals(n) || "fePointLight".equals(n) || "feSpotLight".equals(n)) {
                light = child;
                break;
            }
        }
        if (light == null) {
            return out;
        }
        float[] colour = colour(p.get("lighting-color"), linear, 0xffffffff);
        double surfaceScale = number(p, "surfaceScale", 1);
        double constant = number(p, specular ? "specularConstant" : "diffuseConstant", 1);
        double exponent = number(p, "specularExponent", 1);
        boolean distant = "feDistantLight".equals(light.getName());
        boolean spot = "feSpotLight".equals(light.getName());
        double azimuth = Math.toRadians(number(light, "azimuth", 0));
        double elevation = Math.toRadians(number(light, "elevation", 0));
        // The light is placed in the element's coordinates; its height is
        // taken to scale as its position across does.
        Point2D at0 = at.transform(new Point2D.Double(number(light, "x", 0), number(light, "y", 0)), null);
        double lz = number(light, "z", 0) * ux;
        Point2D to = at.transform(new Point2D.Double(number(light, "pointsAtX", 0),
                number(light, "pointsAtY", 0)), null);
        double toZ = number(light, "pointsAtZ", 0) * ux;
        double spotExponent = number(light, "specularExponent", 1);
        String cone = light.get("limitingConeAngle");
        double coneCos = cone == null ? -2 : Math.cos(Math.toRadians(Math.abs(number(light, "limitingConeAngle", 0))));
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                // The slope of the surface the opacity describes.
                double nx = -surfaceScale / 4.0 * ((height(in, x + 1, y - 1) + 2 * height(in, x + 1, y)
                        + height(in, x + 1, y + 1)) - (height(in, x - 1, y - 1) + 2 * height(in, x - 1, y)
                        + height(in, x - 1, y + 1)));
                double ny = -surfaceScale / 4.0 * ((height(in, x - 1, y + 1) + 2 * height(in, x, y + 1)
                        + height(in, x + 1, y + 1)) - (height(in, x - 1, y - 1) + 2 * height(in, x, y - 1)
                        + height(in, x + 1, y - 1)));
                double nz = 1;
                double nl = Math.sqrt(nx * nx + ny * ny + nz * nz);
                nx /= nl;
                ny /= nl;
                nz /= nl;
                double lx;
                double ly;
                double lzz;
                if (distant) {
                    lx = Math.cos(azimuth) * Math.cos(elevation);
                    ly = Math.sin(azimuth) * Math.cos(elevation);
                    lzz = Math.sin(elevation);
                } else {
                    lx = at0.getX() - x;
                    ly = at0.getY() - y;
                    lzz = lz - surfaceScale * height(in, x, y);
                    double ll = Math.sqrt(lx * lx + ly * ly + lzz * lzz);
                    if (ll > 0) {
                        lx /= ll;
                        ly /= ll;
                        lzz /= ll;
                    }
                }
                double strength = 1;
                if (spot) {
                    double sx = to.getX() - at0.getX();
                    double sy = to.getY() - at0.getY();
                    double sz = toZ - lz;
                    double sl = Math.sqrt(sx * sx + sy * sy + sz * sz);
                    double towards = sl > 0 ? -(lx * sx + ly * sy + lzz * sz) / sl : 0;
                    strength = towards <= 0 || towards < coneCos ? 0 : Math.pow(towards, spotExponent);
                }
                double amount;
                if (specular) {
                    double hx = lx;
                    double hy = ly;
                    double hz = lzz + 1;
                    double hl = Math.sqrt(hx * hx + hy * hy + hz * hz);
                    double nh = hl > 0 ? (nx * hx + ny * hy + nz * hz) / hl : 0;
                    amount = constant * Math.pow(Math.max(0, nh), exponent);
                } else {
                    amount = constant * Math.max(0, nx * lx + ny * ly + nz * lzz);
                }
                amount *= strength;
                int i = y * w + x;
                out.r[i] = clamp((float) (amount * colour[0]));
                out.g[i] = clamp((float) (amount * colour[1]));
                out.b[i] = clamp((float) (amount * colour[2]));
                out.a[i] = specular ? Math.max(out.r[i], Math.max(out.g[i], out.b[i])) : 1f;
            }
        }
        return out;
    }

    private float height(Plane in, int x, int y) {
        return in.a[Math.max(0, Math.min(h - 1, y)) * w + Math.max(0, Math.min(w - 1, x))];
    }

    private static double number(Primitive p, String attribute, double fallback) {
        double[] v = numbers(p.get(attribute));
        return v.length == 0 ? fallback : v[0];
    }

    /// The numbers of a list, as many as can be read from its start.
    private static double[] numbers(String value) {
        if (value == null || value.trim().length() == 0) {
            return new double[0];
        }
        String[] parts = value.trim().split("[\\s,]+");
        double[] out = new double[parts.length];
        int n = 0;
        for (String part : parts) {
            try {
                out[n] = Double.parseDouble(part);
            } catch (NumberFormatException ex) {
                break;
            }
            n++;
        }
        return n == out.length ? out : java.util.Arrays.copyOf(out, n);
    }

    private static float clamp(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
