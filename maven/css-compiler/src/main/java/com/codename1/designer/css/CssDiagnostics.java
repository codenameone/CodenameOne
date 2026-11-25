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
package com.codename1.designer.css;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/// Stable IDs and suggested fixes for the CSS compiler's most common errors, so that
/// a developer (or an agent) reading a failed compile learns what to write instead.
/// Each message keeps the compiler's original wording and appends `[ID]` and a
/// suggestion; tools key on the ID.
public final class CssDiagnostics {
    /// A property the compiler does not support.
    public static final String UNSUPPORTED_PROPERTY = "CN1-CSS-PROPERTY";
    /// A colour value that is not a colour.
    public static final String INVALID_COLOR = "CN1-CSS-COLOR";

    /// The properties `CSSTheme.apply` handles. `CssDiagnosticsTest` holds this list to
    /// the switch's case labels, so a property added there cannot be missed here.
    static final List<String> PROPERTIES = Collections.unmodifiableList(Arrays.asList(
        "backdrop-filter", "background", "background-color", "background-image",
        "background-repeat", "background-size", "border", "border-bottom", "border-bottom-color",
        "border-bottom-left-radius", "border-bottom-right-radius", "border-bottom-style",
        "border-bottom-width", "border-color", "border-color-bottom", "border-color-left",
        "border-color-right", "border-color-top", "border-image", "border-image-slice",
        "border-left", "border-left-color", "border-left-style", "border-left-width",
        "border-radius", "border-right", "border-right-color", "border-right-style",
        "border-right-width", "border-style", "border-style-bottom", "border-style-left",
        "border-style-right", "border-style-top", "border-top", "border-top-color",
        "border-top-left-radius", "border-top-right-radius", "border-top-style", "border-top-width",
        "border-width", "border-width-bottom", "border-width-left", "border-width-right",
        "border-width-top", "box-shadow", "cn1-9patch", "cn1-background-type",
        "cn1-border-bottom-left-radius-x", "cn1-border-bottom-left-radius-y",
        "cn1-border-bottom-right-radius-x", "cn1-border-bottom-right-radius-y",
        "cn1-border-top-left-radius-x", "cn1-border-top-left-radius-y",
        "cn1-border-top-right-radius-x", "cn1-border-top-right-radius-y", "cn1-box-shadow-blur",
        "cn1-box-shadow-color", "cn1-box-shadow-h", "cn1-box-shadow-inset", "cn1-box-shadow-spread",
        "cn1-box-shadow-v", "cn1-derive", "cn1-pill-border", "cn1-round-border", "cn1-source-dpi",
        "cn1-stroke-gradient", "cn1-stroke-gradient-angle", "cn1-svg-height", "cn1-svg-width",
        "color", "elevation", "filter", "font", "font-family", "font-size", "font-style",
        "font-weight", "height", "icon-gap", "letter-spacing", "margin", "margin-bottom",
        "margin-left", "margin-right", "margin-top", "min-height", "min-width", "opacity",
        "padding", "padding-bottom", "padding-left", "padding-right", "padding-top",
        "refresh-images", "text-align", "text-decoration", "width"
    ));

    private CssDiagnostics() {
    }

    /// The message for an unsupported property, naming the closest supported one.
    public static String unsupportedProperty(String property) {
        String best = nearest(property, PROPERTIES);
        return "Unsupported CSS property " + property + " [" + UNSUPPORTED_PROPERTY + "]"
                + (best != null ? ". Did you mean " + best + "?"
                : ". See the CSS chapter of the developer guide for the supported properties.");
    }

    /// The message for an invalid colour, naming the closest colour name when the value
    /// looks like a misspelt one.
    public static String invalidColor(String value, Collection<String> names) {
        String best = value == null ? null : nearest(value, names);
        return "Invalid color specification: Color string was " + value + " [" + INVALID_COLOR + "]. "
                + (best != null ? "Did you mean " + best + "? " : "")
                + "Use a color name, #rgb, #rrggbb, #rrggbbaa, rgb(r, g, b) or rgba(r, g, b, a).";
    }

    /// The candidate within a misspelling's distance of `word`, or null.
    static String nearest(String word, Collection<String> candidates) {
        String w = word.toLowerCase(Locale.ROOT);
        String best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (String c : candidates) {
            int d = distance(w, c.toLowerCase(Locale.ROOT));
            if (d < bestDistance) {
                bestDistance = d;
                best = c;
            }
        }
        return best != null && bestDistance > 0 && bestDistance <= Math.max(2, w.length() / 4) ? best : null;
    }

    static int distance(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return prev[b.length()];
    }
}
