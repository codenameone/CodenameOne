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
package com.codename1.svg.transcoder.parser;

/**
 * A paint value: either a solid ARGB color, a reference to a gradient
 * definition by id (set via {@link #setReference}), or "none".
 */
public final class SVGPaint {

    public static final SVGPaint NONE = new SVGPaint(true, 0, null);
    public static final SVGPaint BLACK = new SVGPaint(false, 0xFF000000, null);

    private final boolean none;
    private final int color;
    private final String reference;

    private final SVGPaint fallback;

    private SVGPaint(boolean none, int color, String reference) {
        this(none, color, reference, null);
    }

    private SVGPaint(boolean none, int color, String reference, SVGPaint fallback) {
        this.none = none;
        this.color = color;
        this.reference = reference;
        this.fallback = fallback;
    }

    /**
     * What a reference is painted with when nothing has its id: the colour
     * or {@code none} written after it, as in {@code url(#g) red}. Null when
     * nothing is written there.
     */
    public SVGPaint getFallback() { return fallback; }

    public static SVGPaint ofColor(int argb) {
        return new SVGPaint(false, argb, null);
    }

    public static SVGPaint ofReference(String id) {
        return new SVGPaint(false, 0, id);
    }

    public boolean isNone() { return none; }
    public int getColor() { return color; }
    public String getReference() { return reference; }
    public boolean isReference() { return reference != null; }

    public static SVGPaint setReference(String value) {
        String r = stripUrl(value);
        if (r == null) return null;
        String rest = value.substring(value.indexOf(')') + 1).trim();
        SVGPaint fallback = null;
        if (ColorParser.isNone(rest)) {
            fallback = NONE;
        } else if (!rest.isEmpty() && !ColorParser.isCurrentColor(rest)) {
            try {
                fallback = ofColor(ColorParser.parse(rest));
            } catch (RuntimeException e) {
                // not a colour: no fallback
            }
        }
        return new SVGPaint(false, 0, r, fallback);
    }

    /**
     * "url(#foo)" -> "foo"; returns null otherwise. The fragment may be
     * quoted, as in {@code url('#foo')} or {@code url("#foo")}.
     */
    public static String stripUrl(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (!t.startsWith("url(")) return null;
        int close = t.indexOf(')', 4);
        if (close < 0) return null;
        String inside = t.substring(4, close).trim();
        if (inside.length() >= 2) {
            char q = inside.charAt(0);
            if ((q == '"' || q == '\'') && inside.charAt(inside.length() - 1) == q) {
                inside = inside.substring(1, inside.length() - 1).trim();
            }
        }
        if (inside.startsWith("#")) inside = inside.substring(1);
        return inside;
    }
}
