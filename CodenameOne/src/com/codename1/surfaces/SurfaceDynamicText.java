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
package com.codename1.surfaces;

import java.util.Date;
import java.util.Map;

/// A date-driven text node the operating system animates natively -- the headline feature for
/// timers, delivery ETAs and elapsed-time displays: a countdown keeps ticking every second on the
/// widget or Dynamic Island even though the app process is not running.
///
/// ```java
/// // counts down to the ETA the OS-native way, no app wakeups needed
/// new SurfaceDynamicText(SurfaceDynamicText.STYLE_TIMER_DOWN, "eta")
///         .setFontSize(22).setFontWeight(SurfaceFontWeight.BOLD)
/// ```
///
/// The target date is either fixed at build time or referenced from the state map by key (the
/// state value is the epoch time in milliseconds as a `Long`), so a live-activity update can move
/// the ETA without republishing the layout.
///
/// Rendering: iOS uses `Text(date, style:)`, Android uses `Chronometer` / `TextClock`. On Android
/// the `STYLE_DATE` and `STYLE_RELATIVE` styles are approximated -- they render as static text
/// computed when the widget last refreshed.
public class SurfaceDynamicText extends SurfaceNode {
    /// Counts down to the target date, e.g. `04:59`. Rendered by `Text(date, style: .timer)` on
    /// iOS and a countdown `Chronometer` on Android.
    public static final int STYLE_TIMER_DOWN = 0;

    /// Counts up since the target date, e.g. `12:07`.
    public static final int STYLE_TIMER_UP = 1;

    /// The target date's clock time, e.g. `9:41 AM`.
    public static final int STYLE_TIME = 2;

    /// The target date's calendar date, e.g. `June 3`. Approximated as static text on Android.
    public static final int STYLE_DATE = 3;

    /// The distance to the target date in words, e.g. `in 5 min`. Approximated as static text on
    /// Android.
    public static final int STYLE_RELATIVE = 4;

    private final int style;
    private final Date date;
    private final String dateKey;
    private int fontSize;
    private SurfaceFontWeight fontWeight;
    private SurfaceColor color;

    /// Creates a dynamic text node with a fixed target date.
    ///
    /// #### Parameters
    ///
    /// - `style`: one of the `STYLE_...` constants
    /// - `date`: the target date
    public SurfaceDynamicText(int style, Date date) {
        this.style = style;
        this.date = date;
        this.dateKey = null;
    }

    /// Creates a dynamic text node whose target date comes from the state map. The state value is
    /// the epoch time in milliseconds as a `Long`.
    ///
    /// #### Parameters
    ///
    /// - `style`: one of the `STYLE_...` constants
    /// - `dateStateKey`: the state-map key holding the epoch millis
    public SurfaceDynamicText(int style, String dateStateKey) {
        this.style = style;
        this.date = null;
        this.dateKey = dateStateKey;
    }

    /// Sets the font size.
    ///
    /// #### Parameters
    ///
    /// - `fontSize`: the size in dips
    ///
    /// #### Returns
    ///
    /// this node, for chaining
    public SurfaceDynamicText setFontSize(int fontSize) {
        this.fontSize = fontSize;
        return this;
    }

    /// Sets the font weight.
    ///
    /// #### Parameters
    ///
    /// - `fontWeight`: the weight
    ///
    /// #### Returns
    ///
    /// this node, for chaining
    public SurfaceDynamicText setFontWeight(SurfaceFontWeight fontWeight) {
        this.fontWeight = fontWeight;
        return this;
    }

    /// Sets the text color.
    ///
    /// #### Parameters
    ///
    /// - `color`: the color
    ///
    /// #### Returns
    ///
    /// this node, for chaining
    public SurfaceDynamicText setColor(SurfaceColor color) {
        this.color = color;
        return this;
    }

    /// Returns the `STYLE_...` constant of this node.
    public int getStyle() {
        return style;
    }

    /// Returns the fixed target date, or null when the date comes from the state map.
    public Date getDate() {
        return date;
    }

    /// Returns the state-map key of the target date, or null when the date is fixed.
    public String getDateKey() {
        return dateKey;
    }

    /// Returns the font size in dips, 0 for the platform default.
    public int getFontSize() {
        return fontSize;
    }

    /// Returns the font weight, or null for the platform default.
    public SurfaceFontWeight getFontWeight() {
        return fontWeight;
    }

    /// Returns the text color, or null for the platform default.
    public SurfaceColor getColor() {
        return color;
    }

    @Override
    public SurfaceDynamicText setPadding(int all) {
        super.setPadding(all);
        return this;
    }

    @Override
    public SurfaceDynamicText setPadding(int top, int right, int bottom, int left) {
        super.setPadding(top, right, bottom, left);
        return this;
    }

    @Override
    public SurfaceDynamicText setBackground(SurfaceColor background) {
        super.setBackground(background);
        return this;
    }

    @Override
    public SurfaceDynamicText setCornerRadius(int radius) {
        super.setCornerRadius(radius);
        return this;
    }

    @Override
    public SurfaceDynamicText setAlignment(SurfaceAlignment alignment) {
        super.setAlignment(alignment);
        return this;
    }

    @Override
    public SurfaceDynamicText setWeight(int weight) {
        super.setWeight(weight);
        return this;
    }

    @Override
    public SurfaceDynamicText setSize(int widthDips, int heightDips) {
        super.setSize(widthDips, heightDips);
        return this;
    }

    @Override
    public SurfaceDynamicText setAction(String actionId) {
        super.setAction(actionId);
        return this;
    }

    @Override
    public SurfaceDynamicText setAction(String actionId, Map<String, Object> params) {
        super.setAction(actionId, params);
        return this;
    }

    @Override
    String getType() {
        return "dyn";
    }

    @Override
    void serializeContent(Map<String, Object> out, Map<String, byte[]> images, int depth) {
        String styleName;
        switch (style) {
            case STYLE_TIMER_UP:
                styleName = "timerUp";
                break;
            case STYLE_TIME:
                styleName = "time";
                break;
            case STYLE_DATE:
                styleName = "date";
                break;
            case STYLE_RELATIVE:
                styleName = "relative";
                break;
            default:
                styleName = "timerDown";
        }
        out.put("style", styleName);
        if (dateKey != null) {
            out.put("dateKey", dateKey);
        } else if (date != null) {
            out.put("date", Long.valueOf(date.getTime()));
        }
        if (fontSize != 0) {
            out.put("size", Integer.valueOf(fontSize));
        }
        if (fontWeight != null) {
            out.put("fw", fontWeight.getJsonName());
        }
        if (color != null) {
            out.put("color", SurfaceSerializer.colorMap(color));
        }
    }
}
