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
package com.codename1.desktopcompat.java.awt;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/// Preferences for how a `Graphics2D` renders, keyed by the `KEY_`
/// constants. The antialiasing hints switch Codename One's antialiasing;
/// the others are remembered and answered back but change nothing.
public class RenderingHints implements Map<Object, Object>, Cloneable {

    /// The identity of one hint.
    public abstract static class Key {

        private final int privatekey;

        protected Key(int privatekey) {
            this.privatekey = privatekey;
        }

        public abstract boolean isCompatibleValue(Object val);

        protected final int intKey() {
            return privatekey;
        }

        @Override
        public final int hashCode() {
            return super.hashCode();
        }

        @Override
        public final boolean equals(Object o) {
            return this == o;
        }
    }

    private static final class HintKey extends Key {

        private final String description;

        HintKey(int number, String description) {
            super(number);
            this.description = description;
        }

        @Override
        public boolean isCompatibleValue(Object val) {
            return val instanceof HintValue && ((HintValue) val).key == this;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    private static final class HintValue {

        final Key key;
        private final String description;

        HintValue(Key key, String description) {
            this.key = key;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    public static final Key KEY_ANTIALIASING = new HintKey(1, "Global antialiasing enable key");
    public static final Object VALUE_ANTIALIAS_ON = new HintValue(KEY_ANTIALIASING, "Antialiased rendering mode");
    public static final Object VALUE_ANTIALIAS_OFF = new HintValue(KEY_ANTIALIASING, "Nonantialiased rendering mode");
    public static final Object VALUE_ANTIALIAS_DEFAULT = new HintValue(KEY_ANTIALIASING,
            "Default antialiasing rendering mode");

    public static final Key KEY_RENDERING = new HintKey(2, "Global rendering quality key");
    public static final Object VALUE_RENDER_SPEED = new HintValue(KEY_RENDERING, "Fastest rendering methods");
    public static final Object VALUE_RENDER_QUALITY = new HintValue(KEY_RENDERING,
            "Highest quality rendering methods");
    public static final Object VALUE_RENDER_DEFAULT = new HintValue(KEY_RENDERING, "Default rendering methods");

    public static final Key KEY_DITHERING = new HintKey(3, "Dithering quality key");
    public static final Object VALUE_DITHER_DISABLE = new HintValue(KEY_DITHERING, "Nondithered rendering mode");
    public static final Object VALUE_DITHER_ENABLE = new HintValue(KEY_DITHERING, "Dithered rendering mode");
    public static final Object VALUE_DITHER_DEFAULT = new HintValue(KEY_DITHERING, "Default dithering mode");

    public static final Key KEY_TEXT_ANTIALIASING = new HintKey(4, "Text-specific antialiasing enable key");
    public static final Object VALUE_TEXT_ANTIALIAS_ON = new HintValue(KEY_TEXT_ANTIALIASING,
            "Antialiased text mode");
    public static final Object VALUE_TEXT_ANTIALIAS_OFF = new HintValue(KEY_TEXT_ANTIALIASING,
            "Nonantialiased text mode");
    public static final Object VALUE_TEXT_ANTIALIAS_DEFAULT = new HintValue(KEY_TEXT_ANTIALIASING,
            "Default antialiasing text mode");
    public static final Object VALUE_TEXT_ANTIALIAS_GASP = new HintValue(KEY_TEXT_ANTIALIASING,
            "GASP antialiasing text mode");
    public static final Object VALUE_TEXT_ANTIALIAS_LCD_HRGB = new HintValue(KEY_TEXT_ANTIALIASING,
            "LCD HRGB antialiasing text mode");
    public static final Object VALUE_TEXT_ANTIALIAS_LCD_HBGR = new HintValue(KEY_TEXT_ANTIALIASING,
            "LCD HBGR antialiasing text mode");
    public static final Object VALUE_TEXT_ANTIALIAS_LCD_VRGB = new HintValue(KEY_TEXT_ANTIALIASING,
            "LCD VRGB antialiasing text mode");
    public static final Object VALUE_TEXT_ANTIALIAS_LCD_VBGR = new HintValue(KEY_TEXT_ANTIALIASING,
            "LCD VBGR antialiasing text mode");

    public static final Key KEY_FRACTIONALMETRICS = new HintKey(5, "Fractional metrics enable key");
    public static final Object VALUE_FRACTIONALMETRICS_OFF = new HintValue(KEY_FRACTIONALMETRICS,
            "Integer text metrics mode");
    public static final Object VALUE_FRACTIONALMETRICS_ON = new HintValue(KEY_FRACTIONALMETRICS,
            "Fractional text metrics mode");
    public static final Object VALUE_FRACTIONALMETRICS_DEFAULT = new HintValue(KEY_FRACTIONALMETRICS,
            "Default fractional text metrics mode");

    public static final Key KEY_INTERPOLATION = new HintKey(6, "Image interpolation method key");
    public static final Object VALUE_INTERPOLATION_NEAREST_NEIGHBOR = new HintValue(KEY_INTERPOLATION,
            "Nearest Neighbor image interpolation mode");
    public static final Object VALUE_INTERPOLATION_BILINEAR = new HintValue(KEY_INTERPOLATION,
            "Bilinear image interpolation mode");
    public static final Object VALUE_INTERPOLATION_BICUBIC = new HintValue(KEY_INTERPOLATION,
            "Bicubic image interpolation mode");

    public static final Key KEY_ALPHA_INTERPOLATION = new HintKey(7, "Alpha blending interpolation method key");
    public static final Object VALUE_ALPHA_INTERPOLATION_SPEED = new HintValue(KEY_ALPHA_INTERPOLATION,
            "Fastest alpha blending methods");
    public static final Object VALUE_ALPHA_INTERPOLATION_QUALITY = new HintValue(KEY_ALPHA_INTERPOLATION,
            "Highest quality alpha blending methods");
    public static final Object VALUE_ALPHA_INTERPOLATION_DEFAULT = new HintValue(KEY_ALPHA_INTERPOLATION,
            "Default alpha blending methods");

    public static final Key KEY_COLOR_RENDERING = new HintKey(8, "Color rendering quality key");
    public static final Object VALUE_COLOR_RENDER_SPEED = new HintValue(KEY_COLOR_RENDERING,
            "Fastest color rendering methods");
    public static final Object VALUE_COLOR_RENDER_QUALITY = new HintValue(KEY_COLOR_RENDERING,
            "Highest quality color rendering methods");
    public static final Object VALUE_COLOR_RENDER_DEFAULT = new HintValue(KEY_COLOR_RENDERING,
            "Default color rendering methods");

    public static final Key KEY_STROKE_CONTROL = new HintKey(9, "Stroke normalization control key");
    public static final Object VALUE_STROKE_DEFAULT = new HintValue(KEY_STROKE_CONTROL,
            "Default stroke normalization");
    public static final Object VALUE_STROKE_NORMALIZE = new HintValue(KEY_STROKE_CONTROL,
            "Stroke normalization for consistency");
    public static final Object VALUE_STROKE_PURE = new HintValue(KEY_STROKE_CONTROL,
            "Pure stroke conversion for accurate paths");

    private final HashMap<Object, Object> hintmap = new HashMap<Object, Object>();

    public RenderingHints(Map<Key, ?> init) {
        if (init != null) {
            for (Map.Entry<Key, ?> e : init.entrySet()) {
                hintmap.put(e.getKey(), e.getValue());
            }
        }
    }

    public RenderingHints(Key key, Object value) {
        hintmap.put(key, value);
    }

    @Override
    public int size() {
        return hintmap.size();
    }

    @Override
    public boolean isEmpty() {
        return hintmap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return hintmap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return hintmap.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return hintmap.get(key);
    }

    @Override
    public Object put(Object key, Object value) {
        if (!(key instanceof Key)) {
            throw new IllegalArgumentException("a rendering hint key must be a RenderingHints.Key");
        }
        if (!((Key) key).isCompatibleValue(value)) {
            throw new IllegalArgumentException(value + " incompatible with " + key);
        }
        return hintmap.put(key, value);
    }

    public void add(RenderingHints hints) {
        hintmap.putAll(hints.hintmap);
    }

    @Override
    public void clear() {
        hintmap.clear();
    }

    @Override
    public Object remove(Object key) {
        return hintmap.remove(key);
    }

    @Override
    public void putAll(Map<?, ?> m) {
        for (Map.Entry<?, ?> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public Set<Object> keySet() {
        return hintmap.keySet();
    }

    @Override
    public Collection<Object> values() {
        return hintmap.values();
    }

    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {
        return hintmap.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof RenderingHints) {
            return hintmap.equals(((RenderingHints) o).hintmap);
        }
        return o instanceof Map && hintmap.equals(o);
    }

    @Override
    public int hashCode() {
        return hintmap.hashCode();
    }

    /// A copy holding the same hints.
    @Override
    public Object clone() {
        RenderingHints copy = new RenderingHints(null);
        copy.hintmap.putAll(hintmap);
        return copy;
    }

    @Override
    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode()) + " (" + hintmap.size() + " hints)";
    }
}
