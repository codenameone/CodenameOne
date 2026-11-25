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
package com.codename1.art.effects;

import com.codename1.art.BlendMode;
import com.codename1.art.Compositor;
import com.codename1.art.Raster;
import com.codename1.util.MathUtil;

/// Factories for every effect kind, and the code that applies them.
///
/// Lengths (offsets, radii, widths) are in the units of the node the effect is on; they are
/// scaled to device pixels when the effect is applied. A blur `radius` is twice the Gaussian
/// standard deviation, as Sketch, Figma and CSS use it. Angles are in degrees,
/// counter-clockwise from 3 o'clock, as Photoshop measures light. Every effect also takes
/// `opacity` (0 to 1) and most take `blend`.
public final class Effects {
    private Effects() {
    }

    /// A drop shadow.
    ///
    /// #### Parameters
    ///
    /// - `color`: shadow color
    /// - `dx`: horizontal offset
    /// - `dy`: vertical offset
    /// - `radius`: blur radius
    /// - `spread`: how far the shadow grows before it is blurred
    public static Effect dropShadow(int color, float dx, float dy, float radius, float spread) {
        return new Effect(Effect.DROP_SHADOW).setColor("color", color).set("dx", dx).set("dy", dy)
                .set("radius", radius).set("spread", spread);
    }

    /// An inner shadow.
    ///
    /// #### Parameters
    ///
    /// - `color`: shadow color
    /// - `dx`: horizontal offset
    /// - `dy`: vertical offset
    /// - `radius`: blur radius
    /// - `spread`: choke, how far the shadow grows inward before it is blurred
    public static Effect innerShadow(int color, float dx, float dy, float radius, float spread) {
        return new Effect(Effect.INNER_SHADOW).setColor("color", color).set("dx", dx).set("dy", dy)
                .set("radius", radius).set("spread", spread);
    }

    /// An outer glow.
    ///
    /// #### Parameters
    ///
    /// - `color`: glow color
    /// - `radius`: blur radius
    /// - `spread`: how far the glow grows before it is blurred
    public static Effect outerGlow(int color, float radius, float spread) {
        return new Effect(Effect.OUTER_GLOW).setColor("color", color).set("radius", radius).set("spread", spread)
                .set("blend", BlendMode.SCREEN);
    }

    /// An inner glow.
    ///
    /// #### Parameters
    ///
    /// - `color`: glow color
    /// - `radius`: blur radius
    /// - `center`: true to glow from the center instead of the edges
    public static Effect innerGlow(int color, float radius, boolean center) {
        return new Effect(Effect.INNER_GLOW).setColor("color", color).set("radius", radius)
                .set("source", center ? "center" : "edge").set("blend", BlendMode.SCREEN);
    }

    /// A Gaussian blur of the layer.
    ///
    /// #### Parameters
    ///
    /// - `radius`: blur radius
    public static Effect gaussianBlur(float radius) {
        return new Effect(Effect.GAUSSIAN_BLUR).set("radius", radius);
    }

    /// A blur of what is behind the layer, seen through the layer's shape (frosted glass).
    ///
    /// #### Parameters
    ///
    /// - `radius`: blur radius
    public static Effect backgroundBlur(float radius) {
        return new Effect(Effect.BACKGROUND_BLUR).set("radius", radius);
    }

    /// A blur along one direction.
    ///
    /// #### Parameters
    ///
    /// - `angle`: direction in degrees
    /// - `distance`: length of the blur
    public static Effect motionBlur(float angle, float distance) {
        return new Effect(Effect.MOTION_BLUR).set("angle", angle).set("distance", distance);
    }

    /// A zoom blur: every pixel is smeared along the line toward a center point.
    ///
    /// #### Parameters
    ///
    /// - `amount`: how far toward the center the smear reaches, 0 (none) to 1 (all the way)
    /// - `cx`: the center's x, as a fraction of the layer's width
    /// - `cy`: the center's y, as a fraction of the layer's height
    public static Effect zoomBlur(float amount, float cx, float cy) {
        return new Effect(Effect.ZOOM_BLUR).set("amount", amount).set("cx", cx).set("cy", cy);
    }

    /// An outline around the layer's opaque pixels.
    ///
    /// #### Parameters
    ///
    /// - `color`: stroke color
    /// - `width`: stroke width
    /// - `position`: "outside", "inside" or "center"
    public static Effect stroke(int color, float width, String position) {
        return new Effect(Effect.STROKE).setColor("color", color).set("width", width).set("position", position);
    }

    /// A solid color over the layer.
    ///
    /// #### Parameters
    ///
    /// - `color`: the color
    public static Effect colorOverlay(int color) {
        return new Effect(Effect.COLOR_OVERLAY).setColor("color", color);
    }

    /// A gradient over the layer.
    ///
    /// #### Parameters
    ///
    /// - `positions`: stop offsets 0 to 1
    /// - `colors`: stop colors
    /// - `angle`: direction in degrees
    /// - `style`: "linear", "radial", "angle", "reflected" or "diamond"
    public static Effect gradientOverlay(float[] positions, int[] colors, float angle, String style) {
        return new Effect(Effect.GRADIENT_OVERLAY).set("positions", positions).set("colors", colors)
                .set("angle", angle).set("style", style);
    }

    /// A tiled pattern over the layer.
    ///
    /// #### Parameters
    ///
    /// - `pattern`: the tile
    /// - `scale`: tile scale
    public static Effect patternOverlay(Raster pattern, float scale) {
        return new Effect(Effect.PATTERN_OVERLAY).set("pattern", pattern).set("scale", scale);
    }

    /// Bevel and emboss.
    ///
    /// #### Parameters
    ///
    /// - `style`: "inner", "outer", "emboss" or "pillow"
    /// - `size`: bevel size
    /// - `depth`: strength, 1 is Photoshop's 100%
    /// - `angle`: light angle in degrees
    /// - `altitude`: light altitude in degrees
    public static Effect bevel(String style, float size, float depth, float angle, float altitude) {
        return new Effect(Effect.BEVEL).set("style", style).set("size", size).set("depth", depth)
                .set("angle", angle).set("altitude", altitude);
    }

    /// Satin shading.
    ///
    /// #### Parameters
    ///
    /// - `color`: shading color
    /// - `angle`: direction in degrees
    /// - `distance`: offset of the two copies
    /// - `size`: blur radius
    public static Effect satin(int color, float angle, float distance, float size) {
        return new Effect(Effect.SATIN).setColor("color", color).set("angle", angle).set("distance", distance)
                .set("size", size).set("blend", BlendMode.MULTIPLY);
    }

    /// A color matrix in SVG's layout: four rows of five, applied to non-premultiplied
    /// `r, g, b, a, 1` with values 0 to 1.
    ///
    /// #### Parameters
    ///
    /// - `m`: twenty coefficients
    public static Effect colorMatrix(float[] m) {
        return new Effect(Effect.COLOR_MATRIX).set("matrix", m);
    }

    /// Levels.
    ///
    /// #### Parameters
    ///
    /// - `inBlack`: input black point, 0 to 1
    /// - `inWhite`: input white point, 0 to 1
    /// - `gamma`: midtone gamma, 1 for none
    /// - `outBlack`: output black point
    /// - `outWhite`: output white point
    public static Effect levels(float inBlack, float inWhite, float gamma, float outBlack, float outWhite) {
        return new Effect(Effect.LEVELS).set("inBlack", inBlack).set("inWhite", inWhite).set("gamma", gamma)
                .set("outBlack", outBlack).set("outWhite", outWhite);
    }

    /// Tone curves through control points, each `x, y` pairs from 0 to 1.
    ///
    /// #### Parameters
    ///
    /// - `master`: the curve for all channels; may be null
    /// - `red`: red curve; may be null
    /// - `green`: green curve; may be null
    /// - `blue`: blue curve; may be null
    public static Effect curves(float[] master, float[] red, float[] green, float[] blue) {
        Effect e = new Effect(Effect.CURVES);
        if (master != null) {
            e.set("master", master);
        }
        if (red != null) {
            e.set("red", red);
        }
        if (green != null) {
            e.set("green", green);
        }
        if (blue != null) {
            e.set("blue", blue);
        }
        return e;
    }

    /// Hue, saturation and lightness.
    ///
    /// #### Parameters
    ///
    /// - `hue`: hue rotation in degrees
    /// - `saturation`: -1 (gray) to 1
    /// - `lightness`: -1 (black) to 1 (white)
    public static Effect hueSaturation(float hue, float saturation, float lightness) {
        return new Effect(Effect.HUE_SATURATION).set("hue", hue).set("saturation", saturation)
                .set("lightness", lightness);
    }

    /// Brightness and contrast.
    ///
    /// #### Parameters
    ///
    /// - `brightness`: -1 to 1
    /// - `contrast`: -1 to 1
    public static Effect brightnessContrast(float brightness, float contrast) {
        return new Effect(Effect.BRIGHTNESS_CONTRAST).set("brightness", brightness).set("contrast", contrast);
    }

    /// Inverted colors.
    public static Effect invert() {
        return new Effect(Effect.INVERT);
    }

    /// Black where luminance is below `level`, white elsewhere.
    ///
    /// #### Parameters
    ///
    /// - `level`: 0 to 1
    public static Effect threshold(float level) {
        return new Effect(Effect.THRESHOLD).set("level", level);
    }

    /// Luminance mapped through a gradient.
    ///
    /// #### Parameters
    ///
    /// - `positions`: stop offsets
    /// - `colors`: stop colors
    public static Effect gradientMap(float[] positions, int[] colors) {
        return new Effect(Effect.GRADIENT_MAP).set("positions", positions).set("colors", colors);
    }

    /// A channel mixer: three rows of `r, g, b, constant`.
    ///
    /// #### Parameters
    ///
    /// - `m`: twelve coefficients
    public static Effect channelMixer(float[] m) {
        return new Effect(Effect.CHANNEL_MIXER).set("matrix", m);
    }

    /// A photo filter.
    ///
    /// #### Parameters
    ///
    /// - `color`: the filter color
    /// - `density`: 0 to 1
    public static Effect photoFilter(int color, float density) {
        return new Effect(Effect.PHOTO_FILTER).setColor("color", color).set("density", density)
                .set("preserveLuminosity", 1);
    }

    /// Posterize.
    ///
    /// #### Parameters
    ///
    /// - `levels`: tones per channel, at least 2
    public static Effect posterize(int levels) {
        return new Effect(Effect.POSTERIZE).set("levels", levels);
    }

    /// Exposure.
    ///
    /// #### Parameters
    ///
    /// - `exposure`: stops
    /// - `offset`: added after exposure
    /// - `gamma`: gamma correction, 1 for none
    public static Effect exposure(float exposure, float offset, float gamma) {
        return new Effect(Effect.EXPOSURE).set("exposure", exposure).set("offset", offset).set("gamma", gamma);
    }

    /// Vibrance.
    ///
    /// #### Parameters
    ///
    /// - `amount`: -1 to 1
    public static Effect vibrance(float amount) {
        return new Effect(Effect.VIBRANCE).set("amount", amount);
    }

    /// Black and white with Photoshop's default hue weights.
    public static Effect blackAndWhite() {
        return new Effect(Effect.BLACK_AND_WHITE).set("weights", new float[]{0.4f, 0.6f, 0.4f, 0.6f, 0.2f, 0.8f});
    }

    /// Color balance. Each range is three numbers from -1 to 1: cyan to red, magenta to
    /// green and yellow to blue.
    ///
    /// #### Parameters
    ///
    /// - `shadows`: the shift in the shadows
    /// - `midtones`: the shift in the midtones
    /// - `highlights`: the shift in the highlights
    /// - `preserveLuminosity`: keep each pixel's luminosity
    public static Effect colorBalance(float[] shadows, float[] midtones, float[] highlights,
            boolean preserveLuminosity) {
        return new Effect(Effect.COLOR_BALANCE).set("shadows", shadows).set("midtones", midtones)
                .set("highlights", highlights).set("preserveLuminosity", preserveLuminosity ? 1 : 0);
    }

    /// Selective color. `ranges` holds four numbers (cyan, magenta, yellow and black, -1 to
    /// 1) for each of reds, yellows, greens, cyans, blues, magentas, whites, neutrals and
    /// blacks, in that order.
    ///
    /// #### Parameters
    ///
    /// - `ranges`: 36 adjustments
    /// - `absolute`: true to add ink, false to change it in proportion to the ink present
    public static Effect selectiveColor(float[] ranges, boolean absolute) {
        return new Effect(Effect.SELECTIVE_COLOR).set("ranges", ranges).set("absolute", absolute ? 1 : 0);
    }

    /// An empty SVG filter graph; add primitives to `getPrimitives`.
    public static Effect svgFilter() {
        return new Effect(Effect.SVG_FILTER).set("primitives", new java.util.ArrayList<Object>());
    }

    /// True for the kinds that only change colors pixel by pixel, which an
    /// `AdjustmentNode` can hold.
    ///
    /// #### Parameters
    ///
    /// - `kind`: the kind
    public static boolean isAdjustment(String kind) {
        return Effect.COLOR_MATRIX.equals(kind) || Effect.LEVELS.equals(kind) || Effect.CURVES.equals(kind)
                || Effect.HUE_SATURATION.equals(kind) || Effect.BRIGHTNESS_CONTRAST.equals(kind)
                || Effect.INVERT.equals(kind) || Effect.THRESHOLD.equals(kind) || Effect.GRADIENT_MAP.equals(kind)
                || Effect.CHANNEL_MIXER.equals(kind) || Effect.PHOTO_FILTER.equals(kind)
                || Effect.POSTERIZE.equals(kind) || Effect.EXPOSURE.equals(kind) || Effect.VIBRANCE.equals(kind)
                || Effect.BLACK_AND_WHITE.equals(kind) || Effect.COLOR_BALANCE.equals(kind)
                || Effect.SELECTIVE_COLOR.equals(kind);
    }

    static int padding(Effect e, double s) {
        String k = e.getKind();
        float r = e.getFloat("radius", 0);
        if (Effect.DROP_SHADOW.equals(k)) {
            return (int) Math.ceil((Math.abs(e.getFloat("dx", 0)) + Math.abs(e.getFloat("dy", 0))
                    + r * 1.5 + Math.max(0, e.getFloat("spread", 0))) * s) + 2;
        }
        if (Effect.OUTER_GLOW.equals(k)) {
            return (int) Math.ceil((r * 1.5 + Math.max(0, e.getFloat("spread", 0))) * s) + 2;
        }
        if (Effect.GAUSSIAN_BLUR.equals(k)) {
            return (int) Math.ceil(r * 1.5 * s) + 2;
        }
        if (Effect.MOTION_BLUR.equals(k)) {
            return (int) Math.ceil(e.getFloat("distance", 0) * s) + 2;
        }
        if (Effect.STROKE.equals(k)) {
            return (int) Math.ceil(e.getFloat("width", 1) * s) + 2;
        }
        if (Effect.BEVEL.equals(k)) {
            String st = e.getString("style", "inner");
            return "inner".equals(st) ? 0 : (int) Math.ceil(e.getFloat("size", 5) * s) + 2;
        }
        if (Effect.SVG_FILTER.equals(k)) {
            return (int) Math.ceil(e.getFloat("margin", 20) * s);
        }
        return 0;
    }

    static Raster apply(Effect e, Raster layer, EffectContext ctx) {
        String k = e.getKind();
        double s = ctx.getScale();
        float opacity = e.getFloat("opacity", 1);
        if (Effect.DROP_SHADOW.equals(k) || Effect.OUTER_GLOW.equals(k)) {
            boolean drop = Effect.DROP_SHADOW.equals(k);
            int pad = padding(e, s);
            Raster ex = Pixels.expand(layer, pad);
            int w = ex.getWidth();
            int h = ex.getHeight();
            float[] a = Pixels.alpha(ex);
            int spread = (int) Math.round(e.getFloat("spread", 0) * s);
            if (spread > 0) {
                a = disk(a, w, h, spread, true);
            } else if (spread < 0) {
                a = disk(a, w, h, -spread, false);
            }
            if (drop) {
                a = Pixels.shift(a, w, h, (int) Math.round(e.getFloat("dx", 0) * s),
                        (int) Math.round(e.getFloat("dy", 0) * s), 0);
            }
            double sigma = e.getFloat("radius", 0) * s / 2;
            Pixels.blur(a, w, h, sigma, sigma);
            Raster out = Pixels.fromAlpha(a, e.getColor("color", drop ? 0x80000000 : 0xbfffffbe),
                    ex.getX(), ex.getY(), w, h, opacity);
            if (e.getFloat("knockout", 0) != 0) {
                knockout(out, ex);
            }
            out.composite(ex, BlendMode.NORMAL, 1);
            return out;
        }
        if (Effect.INNER_SHADOW.equals(k) || Effect.INNER_GLOW.equals(k)) {
            boolean shadow = Effect.INNER_SHADOW.equals(k);
            Raster out = layer.copy();
            int w = out.getWidth();
            int h = out.getHeight();
            float[] a = Pixels.alpha(out);
            float[] src;
            boolean center = !shadow && "center".equals(e.getString("source", "edge"));
            if (center) {
                src = a.clone();
            } else {
                src = new float[a.length];
                for (int i = 0; i < a.length; i++) {
                    src[i] = 1 - a[i];
                }
            }
            if (shadow) {
                src = Pixels.shift(src, w, h, (int) Math.round(e.getFloat("dx", 0) * s),
                        (int) Math.round(e.getFloat("dy", 0) * s), 1);
            }
            int spread = (int) Math.round(e.getFloat("spread", 0) * s);
            if (spread > 0) {
                src = disk(src, w, h, spread, !center);
            }
            double sigma = e.getFloat("radius", 0) * s / 2;
            float[] b = padBlur(src, w, h, sigma, center ? 0 : 1);
            if (center) {
                for (int i = 0; i < b.length; i++) {
                    b[i] = 1 - b[i];
                }
            }
            for (int i = 0; i < a.length; i++) {
                b[i] *= a[i];
            }
            Raster fx = Pixels.fromAlpha(b, e.getColor("color", shadow ? 0xbf000000 : 0xbfffffbe),
                    out.getX(), out.getY(), w, h, opacity);
            atop(out, fx, e.getBlend(shadow ? BlendMode.NORMAL : BlendMode.SCREEN), 1);
            return out;
        }
        if (Effect.GAUSSIAN_BLUR.equals(k)) {
            double sigma = e.getFloat("radius", 0) * s / 2;
            return sigma <= 0 ? layer : Pixels.blurRaster(layer, sigma, sigma);
        }
        if (Effect.BACKGROUND_BLUR.equals(k)) {
            Raster bd = ctx.getBackdrop();
            if (bd == null) {
                return layer;
            }
            double sigma = e.getFloat("radius", 0) * s / 2;
            int pad = (int) Math.ceil(sigma * 3);
            Raster region = bd.resize(layer.getX() - pad, layer.getY() - pad,
                    layer.getWidth() + pad * 2, layer.getHeight() + pad * 2);
            Raster blurred = Pixels.blurRaster(region, sigma, sigma)
                    .resize(layer.getX(), layer.getY(), layer.getWidth(), layer.getHeight());
            int[] bp = blurred.getPixels();
            int[] lp = layer.getPixels();
            for (int i = 0; i < bp.length; i++) {
                int la = lp[i] >>> 24;
                int ba = ((bp[i] >>> 24) * la + 127) / 255;
                bp[i] = (ba << 24) | (bp[i] & 0xffffff);
            }
            blurred.composite(layer, BlendMode.NORMAL, 1);
            return blurred;
        }
        if (Effect.MOTION_BLUR.equals(k)) {
            return motion(layer, e.getFloat("angle", 0), e.getFloat("distance", 0) * s);
        }
        if (Effect.ZOOM_BLUR.equals(k)) {
            return zoom(layer, e.getFloat("amount", 0), e.getFloat("cx", 0.5f), e.getFloat("cy", 0.5f));
        }
        if (Effect.STROKE.equals(k)) {
            return strokeFx(e, layer, s, opacity);
        }
        if (Effect.COLOR_OVERLAY.equals(k)) {
            Raster out = layer.copy();
            int c = e.getColor("color", 0xffff0000);
            Raster ov = new Raster(out.getX(), out.getY(), out.getWidth(), out.getHeight());
            java.util.Arrays.fill(ov.getPixels(), c);
            atop(out, ov, e.getBlend(BlendMode.NORMAL), opacity);
            return out;
        }
        if (Effect.GRADIENT_OVERLAY.equals(k)) {
            Raster out = layer.copy();
            atop(out, gradientFill(e, out), e.getBlend(BlendMode.NORMAL), opacity);
            return out;
        }
        if (Effect.PATTERN_OVERLAY.equals(k)) {
            Raster pat = e.getRaster("pattern");
            if (pat == null || pat.getWidth() == 0 || pat.getHeight() == 0) {
                return layer;
            }
            Raster out = layer.copy();
            Raster ov = new Raster(out.getX(), out.getY(), out.getWidth(), out.getHeight());
            double sc = e.getFloat("scale", 1) * s;
            int[] op = ov.getPixels();
            int i = 0;
            for (int y = 0; y < ov.getHeight(); y++) {
                for (int x = 0; x < ov.getWidth(); x++) {
                    int px = (int) Math.floor((ov.getX() + x) / sc) % pat.getWidth();
                    int py = (int) Math.floor((ov.getY() + y) / sc) % pat.getHeight();
                    if (px < 0) {
                        px += pat.getWidth();
                    }
                    if (py < 0) {
                        py += pat.getHeight();
                    }
                    op[i++] = pat.getPixels()[py * pat.getWidth() + px];
                }
            }
            atop(out, ov, e.getBlend(BlendMode.NORMAL), opacity);
            return out;
        }
        if (Effect.BEVEL.equals(k)) {
            return bevelFx(e, layer, s);
        }
        if (Effect.SATIN.equals(k)) {
            Raster out = layer.copy();
            int w = out.getWidth();
            int h = out.getHeight();
            float[] a = Pixels.alpha(out);
            double ang = Math.toRadians(e.getFloat("angle", 19));
            double d = e.getFloat("distance", 11) * s;
            int dx = (int) Math.round(Math.cos(ang) * d);
            int dy = (int) Math.round(-Math.sin(ang) * d);
            float[] s1 = Pixels.shift(a, w, h, dx, dy, 0);
            float[] s2 = Pixels.shift(a, w, h, -dx, -dy, 0);
            float[] diff = new float[a.length];
            for (int i = 0; i < a.length; i++) {
                diff[i] = Math.abs(s1[i] - s2[i]);
            }
            double sigma = e.getFloat("size", 14) * s / 2;
            Pixels.blur(diff, w, h, sigma, sigma);
            boolean inv = e.getFloat("invert", 0) != 0;
            for (int i = 0; i < a.length; i++) {
                float v = inv ? 1 - diff[i] : diff[i];
                diff[i] = v * a[i];
            }
            atop(out, Pixels.fromAlpha(diff, e.getColor("color", 0xff000000), out.getX(), out.getY(), w, h, 1),
                    e.getBlend(BlendMode.MULTIPLY), opacity);
            return out;
        }
        if (Effect.SVG_FILTER.equals(k)) {
            return FilterGraph.run(e, layer, ctx);
        }
        if (isAdjustment(k)) {
            Raster out = layer.copy();
            Adjust.apply(e, out.getPixels());
            if (opacity < 1) {
                Raster base = layer.copy();
                base.composite(out, BlendMode.NORMAL, opacity);
                return base;
            }
            return out;
        }
        return layer;
    }

    private static void knockout(Raster shadow, Raster layer) {
        int[] sp = shadow.getPixels();
        int[] lp = layer.getPixels();
        for (int i = 0; i < sp.length; i++) {
            int la = lp[i] >>> 24;
            if (la != 0) {
                int sa = ((sp[i] >>> 24) * (255 - la) + 127) / 255;
                sp[i] = (sa << 24) | (sp[i] & 0xffffff);
            }
        }
    }

    /// Blurs a channel, treating the outside as `outside`.
    private static float[] padBlur(float[] a, int w, int h, double sigma, float outside) {
        if (sigma <= 0) {
            return a.clone();
        }
        int pad = (int) Math.ceil(sigma * 3) + 1;
        int pw = w + pad * 2;
        int ph = h + pad * 2;
        float[] p = new float[pw * ph];
        java.util.Arrays.fill(p, outside);
        for (int y = 0; y < h; y++) {
            System.arraycopy(a, y * w, p, (y + pad) * pw + pad, w);
        }
        Pixels.blur(p, pw, ph, sigma, sigma);
        float[] o = new float[a.length];
        for (int y = 0; y < h; y++) {
            System.arraycopy(p, (y + pad) * pw + pad, o, y * w, w);
        }
        return o;
    }

    /// Dilation or erosion with a round kernel; square past 24 pixels for speed.
    static float[] disk(float[] a, int w, int h, int r, boolean grow) {
        if (r <= 0) {
            return a;
        }
        if (r > 24) {
            return Pixels.morph(a, w, h, r, r, grow);
        }
        int[] span = new int[r * 2 + 1];
        for (int dy = -r; dy <= r; dy++) {
            span[dy + r] = (int) Math.floor(Math.sqrt((double) r * r - dy * dy));
        }
        float[] o = new float[a.length];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float v = grow ? 0 : 1;
                for (int dy = -r; dy <= r; dy++) {
                    int yy = y + dy;
                    int sx = span[dy + r];
                    for (int dx = -sx; dx <= sx; dx++) {
                        int xx = x + dx;
                        float s = xx < 0 || yy < 0 || xx >= w || yy >= h ? 0 : a[yy * w + xx];
                        if (grow) {
                            if (s > v) {
                                v = s;
                            }
                        } else if (s < v) {
                            v = s;
                        }
                    }
                }
                o[y * w + x] = v;
            }
        }
        return o;
    }

    /// Paints `over` on `base` keeping `base`'s alpha (source-atop with a blend mode).
    static void atop(Raster base, Raster over, BlendMode mode, float opacity) {
        int[] bp = base.getPixels();
        int bw = base.getWidth();
        for (int y = 0; y < base.getHeight(); y++) {
            for (int x = 0; x < bw; x++) {
                int i = y * bw + x;
                int b = bp[i];
                int ba = b >>> 24;
                if (ba == 0) {
                    continue;
                }
                int o = over.get(base.getX() + x, base.getY() + y);
                if ((o >>> 24) == 0) {
                    continue;
                }
                int res = Compositor.blend(o, 0xff000000 | b, mode, opacity);
                bp[i] = (ba << 24) | (res & 0xffffff);
            }
        }
    }

    private static Raster motion(Raster layer, float angle, double dist) {
        if (dist <= 0) {
            return layer;
        }
        int pad = (int) Math.ceil(dist) + 1;
        Raster ex = Pixels.expand(layer, pad);
        float[][] c = Pixels.premul(ex);
        int w = ex.getWidth();
        int h = ex.getHeight();
        float[][] o = new float[4][c[0].length];
        double ang = Math.toRadians(angle);
        double ux = Math.cos(ang);
        double uy = -Math.sin(ang);
        int steps = Math.max(2, (int) Math.ceil(dist));
        for (int t = 0; t <= steps; t++) {
            double f = (t / (double) steps - 0.5) * dist;
            int dx = (int) Math.round(ux * f);
            int dy = (int) Math.round(uy * f);
            for (int y = 0; y < h; y++) {
                int sy = y - dy;
                if (sy < 0 || sy >= h) {
                    continue;
                }
                for (int x = 0; x < w; x++) {
                    int sx = x - dx;
                    if (sx < 0 || sx >= w) {
                        continue;
                    }
                    int si = sy * w + sx;
                    int di = y * w + x;
                    for (int k = 0; k < 4; k++) {
                        o[k][di] += c[k][si];
                    }
                }
            }
        }
        float inv = 1f / (steps + 1);
        for (int k = 0; k < 4; k++) {
            for (int i = 0; i < o[k].length; i++) {
                o[k][i] *= inv;
            }
        }
        Pixels.unpremul(o, ex);
        return ex;
    }

    private static Raster zoom(Raster layer, float amount, float fx, float fy) {
        if (amount <= 0 || layer.isEmpty()) {
            return layer;
        }
        float a = Math.min(1, amount);
        float[][] c = Pixels.premul(layer);
        int w = layer.getWidth();
        int h = layer.getHeight();
        double cx = fx * w;
        double cy = fy * h;
        double far = Math.sqrt(Math.max(cx * cx, (w - cx) * (w - cx)) + Math.max(cy * cy, (h - cy) * (h - cy)));
        int steps = Math.max(2, Math.min(64, (int) Math.ceil(far * a)));
        float[][] o = new float[4][c[0].length];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int di = y * w + x;
                for (int t = 0; t <= steps; t++) {
                    double f = 1 - a * t / (double) steps;
                    int sx = (int) Math.round(cx + (x - cx) * f);
                    int sy = (int) Math.round(cy + (y - cy) * f);
                    if (sx < 0 || sy < 0 || sx >= w || sy >= h) {
                        continue;
                    }
                    int si = sy * w + sx;
                    for (int k = 0; k < 4; k++) {
                        o[k][di] += c[k][si];
                    }
                }
            }
        }
        float inv = 1f / (steps + 1);
        for (int k = 0; k < 4; k++) {
            for (int i = 0; i < o[k].length; i++) {
                o[k][i] *= inv;
            }
        }
        Raster out = new Raster(layer.getX(), layer.getY(), w, h);
        Pixels.unpremul(o, out);
        return out;
    }

    private static Raster strokeFx(Effect e, Raster layer, double s, float opacity) {
        int width = Math.max(1, (int) Math.round(e.getFloat("width", 3) * s));
        String pos = e.getString("position", "outside");
        Raster ex = Pixels.expand(layer, width + 1);
        int w = ex.getWidth();
        int h = ex.getHeight();
        float[] a = Pixels.alpha(ex);
        float[] ring = new float[a.length];
        if ("inside".equals(pos)) {
            float[] er = disk(a, w, h, width, false);
            for (int i = 0; i < a.length; i++) {
                ring[i] = Math.max(0, a[i] - er[i]);
            }
        } else if ("center".equals(pos)) {
            int half = Math.max(1, width / 2);
            float[] di = disk(a, w, h, half, true);
            float[] er = disk(a, w, h, half, false);
            for (int i = 0; i < a.length; i++) {
                ring[i] = Math.max(0, di[i] - er[i]);
            }
        } else {
            float[] di = disk(a, w, h, width, true);
            for (int i = 0; i < a.length; i++) {
                ring[i] = Math.max(0, di[i] - a[i]);
            }
        }
        Raster rr = Pixels.fromAlpha(ring, e.getColor("color", 0xffff0000), ex.getX(), ex.getY(), w, h, opacity);
        if ("outside".equals(pos)) {
            rr.composite(ex, BlendMode.NORMAL, 1);
            return rr;
        }
        ex.composite(rr, e.getBlend(BlendMode.NORMAL), 1);
        return ex;
    }

    private static Raster gradientFill(Effect e, Raster r) {
        float[] pos = e.getFloats("positions", new float[]{0, 1});
        int[] cols = e.getColors("colors", new int[]{0xff000000, 0xffffffff});
        com.codename1.art.GradientPaint g = new com.codename1.art.GradientPaint(
                com.codename1.art.GradientPaint.LINEAR, pos, cols);
        String style = e.getString("style", "linear");
        double ang = Math.toRadians(e.getFloat("angle", 90));
        double ux = Math.cos(ang);
        double uy = -Math.sin(ang);
        double cx = r.getX() + r.getWidth() / 2.0;
        double cy = r.getY() + r.getHeight() / 2.0;
        double half = (Math.abs(ux) * r.getWidth() + Math.abs(uy) * r.getHeight()) / 2 / Math.max(0.01, e.getFloat("scale", 1));
        double rad = Math.sqrt(r.getWidth() * r.getWidth() + r.getHeight() * r.getHeight()) / 2;
        boolean reverse = e.getFloat("reverse", 0) != 0;
        Raster out = new Raster(r.getX(), r.getY(), r.getWidth(), r.getHeight());
        int[] p = out.getPixels();
        int i = 0;
        for (int y = 0; y < r.getHeight(); y++) {
            for (int x = 0; x < r.getWidth(); x++) {
                double dx = r.getX() + x + 0.5 - cx;
                double dy = r.getY() + y + 0.5 - cy;
                double t;
                if ("radial".equals(style)) {
                    t = Math.sqrt(dx * dx + dy * dy) / Math.max(1, rad);
                } else if ("angle".equals(style)) {
                    double a = MathUtil.atan2(-dy, dx) - ang;
                    while (a < 0) {
                        a += Math.PI * 2;
                    }
                    t = (a % (Math.PI * 2)) / (Math.PI * 2);
                } else if ("reflected".equals(style)) {
                    t = Math.abs(dx * ux + dy * uy) / Math.max(1, half);
                } else if ("diamond".equals(style)) {
                    t = (Math.abs(dx) + Math.abs(dy)) / Math.max(1, rad);
                } else {
                    t = (dx * ux + dy * uy) / Math.max(1, half * 2) + 0.5;
                }
                if (reverse) {
                    t = 1 - t;
                }
                p[i++] = g.colorFor(t);
            }
        }
        return out;
    }

    private static Raster bevelFx(Effect e, Raster layer, double s) {
        String style = e.getString("style", "inner");
        double size = Math.max(1, e.getFloat("size", 5) * s);
        boolean strokeStyle = "stroke".equals(style);
        int pad = "inner".equals(style) || strokeStyle ? 0 : (int) Math.ceil(size) + 2;
        Raster ex = Pixels.expand(layer, pad);
        int w = ex.getWidth();
        int h = ex.getHeight();
        float[] a = Pixels.alpha(ex);
        float[] hgt = a.clone();
        // A stroke emboss lights only the stroke: the band one stroke width inside the edge of
        // the layer as stroked, which the stroke effect painted before this one.
        float[] band = null;
        if (strokeStyle) {
            float[] eroded = disk(a, w, h, Math.max(1, (int) Math.ceil(e.getFloat("strokeWidth", 3) * s)), false);
            band = new float[a.length];
            for (int i = 0; i < a.length; i++) {
                band[i] = Math.max(0, a[i] - eroded[i]);
            }
        }
        if ("outer".equals(style) || "emboss".equals(style)) {
            float[] grown = disk(a, w, h, (int) Math.ceil(size), true);
            for (int i = 0; i < hgt.length; i++) {
                hgt[i] = (a[i] + grown[i]) / 2;
            }
        }
        Pixels.blur(hgt, w, h, size / 2, size / 2);
        double depth = e.getFloat("depth", 1) * size;
        double ang = Math.toRadians(e.getFloat("angle", 120));
        double alt = Math.toRadians(e.getFloat("altitude", 30));
        double lx = Math.cos(alt) * Math.cos(ang);
        double ly = -Math.cos(alt) * Math.sin(ang);
        double lz = Math.sin(alt);
        float[] hi = new float[a.length];
        float[] sh = new float[a.length];
        boolean pillow = "pillow".equals(style);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                double gx = (at(hgt, w, h, x + 1, y) - at(hgt, w, h, x - 1, y)) / 2 * depth;
                double gy = (at(hgt, w, h, x, y + 1) - at(hgt, w, h, x, y - 1)) / 2 * depth;
                if (pillow && a[i] < 0.5f) {
                    gx = -gx;
                    gy = -gy;
                }
                double nx = -gx;
                double ny = -gy;
                double nl = Math.sqrt(nx * nx + ny * ny + 1);
                double shade = (nx * lx + ny * ly + lz) / nl - lz;
                float region;
                if (band != null) {
                    region = band[i];
                } else if ("inner".equals(style)) {
                    region = a[i];
                } else if ("outer".equals(style)) {
                    region = 1 - a[i];
                } else {
                    region = 1;
                }
                if (shade > 0) {
                    hi[i] = (float) Math.min(1, shade * 2) * region;
                } else {
                    sh[i] = (float) Math.min(1, -shade * 2) * region;
                }
            }
        }
        Raster hr = Pixels.fromAlpha(hi, e.getColor("highlight", 0xffffffff), ex.getX(), ex.getY(), w, h,
                e.getFloat("highlightOpacity", 0.75f));
        Raster sr = Pixels.fromAlpha(sh, e.getColor("shadow", 0xff000000), ex.getX(), ex.getY(), w, h,
                e.getFloat("shadowOpacity", 0.75f));
        if ("inner".equals(style) || strokeStyle) {
            atop(ex, hr, BlendMode.SCREEN, 1);
            atop(ex, sr, BlendMode.MULTIPLY, 1);
            return ex;
        }
        Raster under = new Raster(ex.getX(), ex.getY(), w, h);
        under.composite(sr, BlendMode.NORMAL, 1);
        under.composite(hr, BlendMode.NORMAL, 1);
        under.composite(ex, BlendMode.NORMAL, 1);
        if (!"outer".equals(style)) {
            atop(under, hr, BlendMode.SCREEN, 1);
            atop(under, sr, BlendMode.MULTIPLY, 1);
        }
        return under;
    }

    private static float at(float[] a, int w, int h, int x, int y) {
        if (x < 0) {
            x = 0;
        } else if (x >= w) {
            x = w - 1;
        }
        if (y < 0) {
            y = 0;
        } else if (y >= h) {
            y = h - 1;
        }
        return a[y * w + x];
    }
}
