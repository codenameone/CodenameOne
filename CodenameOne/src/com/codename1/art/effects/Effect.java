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
import com.codename1.art.Raster;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// An effect: a kind and its parameters.
///
/// Parameters are numbers (`Float`), colors (`Integer`, `0xAARRGGBB`), number arrays
/// (`float[]`), color arrays (`int[]`), strings, `BlendMode`s, `Raster`s and, for SVG
/// filters, lists of primitives (each a `Map`). Every kind is described in `Effects`.
public final class Effect {
    /// Shadow behind the layer.
    public static final String DROP_SHADOW = "dropShadow";
    /// Shadow inside the layer's edges.
    public static final String INNER_SHADOW = "innerShadow";
    /// Glow around the layer.
    public static final String OUTER_GLOW = "outerGlow";
    /// Glow inside the layer's edges.
    public static final String INNER_GLOW = "innerGlow";
    /// Gaussian blur of the layer.
    public static final String GAUSSIAN_BLUR = "gaussianBlur";
    /// Blur of what is behind the layer, seen through it.
    public static final String BACKGROUND_BLUR = "backgroundBlur";
    /// Blur along a direction.
    public static final String MOTION_BLUR = "motionBlur";
    /// A blur radiating from a point, as a camera zooming during the exposure.
    public static final String ZOOM_BLUR = "zoomBlur";
    /// Outline around the layer's edges.
    public static final String STROKE = "stroke";
    /// Solid color over the layer.
    public static final String COLOR_OVERLAY = "colorOverlay";
    /// Gradient over the layer.
    public static final String GRADIENT_OVERLAY = "gradientOverlay";
    /// Pattern over the layer.
    public static final String PATTERN_OVERLAY = "patternOverlay";
    /// Bevel and emboss.
    public static final String BEVEL = "bevel";
    /// Satin shading.
    public static final String SATIN = "satin";
    /// A 4x5 color matrix.
    public static final String COLOR_MATRIX = "colorMatrix";
    /// Input and output levels with gamma.
    public static final String LEVELS = "levels";
    /// Tone curves.
    public static final String CURVES = "curves";
    /// Hue, saturation and lightness.
    public static final String HUE_SATURATION = "hueSaturation";
    /// Brightness and contrast.
    public static final String BRIGHTNESS_CONTRAST = "brightnessContrast";
    /// Inverted colors.
    public static final String INVERT = "invert";
    /// Black and white at a threshold.
    public static final String THRESHOLD = "threshold";
    /// Luminance mapped through a gradient.
    public static final String GRADIENT_MAP = "gradientMap";
    /// Output channels mixed from the input channels.
    public static final String CHANNEL_MIXER = "channelMixer";
    /// A colored filter over the image.
    public static final String PHOTO_FILTER = "photoFilter";
    /// Fewer tones per channel.
    public static final String POSTERIZE = "posterize";
    /// Exposure, offset and gamma.
    public static final String EXPOSURE = "exposure";
    /// Saturation boost that spares saturated colors.
    public static final String VIBRANCE = "vibrance";
    /// Grayscale with per-hue weights.
    public static final String BLACK_AND_WHITE = "blackAndWhite";
    /// Shifts colors toward red, green or blue separately in shadows, midtones and highlights.
    public static final String COLOR_BALANCE = "colorBalance";
    /// Changes the ink of chosen color ranges, as Photoshop's selective color does.
    public static final String SELECTIVE_COLOR = "selectiveColor";
    /// An SVG filter graph.
    public static final String SVG_FILTER = "svgFilter";

    private final String kind;
    private final Map<String, Object> params = new LinkedHashMap<String, Object>();
    private boolean enabled = true;

    /// Creates an effect with no parameters set; every parameter then has its default.
    ///
    /// #### Parameters
    ///
    /// - `kind`: one of the kind constants
    public Effect(String kind) {
        this.kind = kind;
    }

    /// The kind.
    public String getKind() {
        return kind;
    }

    /// False keeps the effect but does not apply it.
    public boolean isEnabled() {
        return enabled;
    }

    /// Turns the effect on or off.
    ///
    /// #### Parameters
    ///
    /// - `enabled`: true to apply it
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /// The parameters that have been set. The map is live.
    public Map<String, Object> getParameters() {
        return params;
    }

    /// Sets a parameter and returns this effect.
    ///
    /// #### Parameters
    ///
    /// - `name`: the parameter
    /// - `value`: its value
    public Effect set(String name, Object value) {
        params.put(name, value);
        return this;
    }

    /// Sets a number parameter and returns this effect.
    ///
    /// #### Parameters
    ///
    /// - `name`: the parameter
    /// - `value`: its value
    public Effect set(String name, float value) {
        params.put(name, Float.valueOf(value));
        return this;
    }

    /// Sets a color parameter and returns this effect.
    ///
    /// #### Parameters
    ///
    /// - `name`: the parameter
    /// - `argb`: the color
    public Effect setColor(String name, int argb) {
        params.put(name, Integer.valueOf(argb));
        return this;
    }

    /// A number parameter.
    ///
    /// #### Parameters
    ///
    /// - `name`: the parameter
    /// - `def`: the value when it is not set
    public float getFloat(String name, float def) {
        Object o = params.get(name);
        if (o instanceof Number) {
            return ((Number) o).floatValue();
        }
        return def;
    }

    /// A color parameter.
    ///
    /// #### Parameters
    ///
    /// - `name`: the parameter
    /// - `def`: the value when it is not set
    public int getColor(String name, int def) {
        Object o = params.get(name);
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        return def;
    }

    /// A string parameter.
    ///
    /// #### Parameters
    ///
    /// - `name`: the parameter
    /// - `def`: the value when it is not set
    public String getString(String name, String def) {
        Object o = params.get(name);
        return o instanceof String ? (String) o : def;
    }

    /// A number array parameter.
    ///
    /// #### Parameters
    ///
    /// - `name`: the parameter
    /// - `def`: the value when it is not set
    public float[] getFloats(String name, float[] def) {
        Object o = params.get(name);
        return o instanceof float[] ? (float[]) o : def;
    }

    /// A color array parameter.
    ///
    /// #### Parameters
    ///
    /// - `name`: the parameter
    /// - `def`: the value when it is not set
    public int[] getColors(String name, int[] def) {
        Object o = params.get(name);
        return o instanceof int[] ? (int[]) o : def;
    }

    /// The blend mode parameter `blend`.
    ///
    /// #### Parameters
    ///
    /// - `def`: the value when it is not set
    public BlendMode getBlend(BlendMode def) {
        Object o = params.get("blend");
        return o instanceof BlendMode ? (BlendMode) o : def;
    }

    /// A raster parameter, or null.
    ///
    /// #### Parameters
    ///
    /// - `name`: the parameter
    public Raster getRaster(String name) {
        Object o = params.get(name);
        return o instanceof Raster ? (Raster) o : null;
    }

    /// The SVG filter primitives, or an empty list.
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getPrimitives() {
        Object o = params.get("primitives");
        if (o instanceof List) {
            return (List<Map<String, Object>>) o;
        }
        return new ArrayList<Map<String, Object>>();
    }

    /// A deep copy.
    public Effect copy() {
        Effect e = new Effect(kind);
        e.enabled = enabled;
        Iterator<Map.Entry<String, Object>> it = params.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> en = it.next();
            e.params.put(en.getKey(), copyValue(en.getValue()));
        }
        return e;
    }

    @SuppressWarnings("unchecked")
    static Object copyValue(Object v) {
        if (v instanceof float[]) {
            float[] a = (float[]) v;
            float[] c = new float[a.length];
            System.arraycopy(a, 0, c, 0, a.length);
            return c;
        }
        if (v instanceof int[]) {
            int[] a = (int[]) v;
            int[] c = new int[a.length];
            System.arraycopy(a, 0, c, 0, a.length);
            return c;
        }
        if (v instanceof List) {
            List<Object> src = (List<Object>) v;
            List<Object> out = new ArrayList<Object>();
            for (int i = 0; i < src.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                out.add(copyValue(src.get(i)));
            }
            return out;
        }
        if (v instanceof Map) {
            Map<String, Object> src = (Map<String, Object>) v;
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            Iterator<Map.Entry<String, Object>> it = src.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Object> en = it.next();
                out.put(en.getKey(), copyValue(en.getValue()));
            }
            return out;
        }
        return v;
    }

    /// Applies the effect.
    ///
    /// #### Parameters
    ///
    /// - `layer`: the layer's pixels in device space
    /// - `ctx`: the scale and backdrop
    ///
    /// #### Returns
    ///
    /// the new pixels, possibly a larger raster; `layer` is not changed
    public Raster apply(Raster layer, EffectContext ctx) {
        if (!enabled) {
            return layer;
        }
        return Effects.apply(this, layer, ctx);
    }

    /// How far, in device pixels, the effect can paint outside the layer's content.
    ///
    /// #### Parameters
    ///
    /// - `scale`: device pixels per unit
    public int padding(double scale) {
        if (!enabled) {
            return 0;
        }
        return Effects.padding(this, scale);
    }
}
