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

/// A progress indicator node. The value is a fraction between 0 and 1, supplied either literally,
/// by state-map key, or as a date interval the OS animates natively (iOS only; other platforms
/// freeze the interval's value as of the last refresh).
///
/// Determinate `STYLE_CIRCULAR` progress falls back to a linear bar on Android app widgets, which
/// only support indeterminate circular spinners.
public class SurfaceProgress extends SurfaceNode {
    /// A horizontal progress bar.
    public static final int STYLE_LINEAR = 0;

    /// A circular gauge. Falls back to linear on Android app widgets.
    public static final int STYLE_CIRCULAR = 1;

    private final int style;
    private float value = -1;
    private String valueKey;
    private Date intervalStart;
    private Date intervalEnd;
    private SurfaceColor color;

    /// Creates a progress node.
    ///
    /// #### Parameters
    ///
    /// - `style`: `STYLE_LINEAR` or `STYLE_CIRCULAR`
    public SurfaceProgress(int style) {
        this.style = style;
    }

    /// Sets a literal progress value.
    ///
    /// #### Parameters
    ///
    /// - `value`: the progress fraction between 0 and 1
    ///
    /// #### Returns
    ///
    /// this progress node, for chaining
    public SurfaceProgress setValue(float value) {
        this.value = value;
        return this;
    }

    /// Reads the progress value from the state map. The state value is a `Number` between 0 and 1.
    ///
    /// #### Parameters
    ///
    /// - `valueStateKey`: the state-map key holding the fraction
    ///
    /// #### Returns
    ///
    /// this progress node, for chaining
    public SurfaceProgress setValueState(String valueStateKey) {
        this.valueKey = valueStateKey;
        return this;
    }

    /// Animates progress across a date interval on the OS clock without app wakeups. Only iOS
    /// renders this natively; other platforms display the interval's fraction as of their last
    /// refresh.
    ///
    /// #### Parameters
    ///
    /// - `start`: interval start
    /// - `end`: interval end
    ///
    /// #### Returns
    ///
    /// this progress node, for chaining
    public SurfaceProgress setDateInterval(Date start, Date end) {
        this.intervalStart = start;
        this.intervalEnd = end;
        return this;
    }

    /// Sets the indicator color.
    ///
    /// #### Parameters
    ///
    /// - `color`: the color
    ///
    /// #### Returns
    ///
    /// this progress node, for chaining
    public SurfaceProgress setColor(SurfaceColor color) {
        this.color = color;
        return this;
    }

    /// Returns `STYLE_LINEAR` or `STYLE_CIRCULAR`.
    public int getStyle() {
        return style;
    }

    /// Returns the literal progress fraction, or -1 when unset.
    public float getValue() {
        return value;
    }

    /// Returns the state-map key of the progress value, or null.
    public String getValueKey() {
        return valueKey;
    }

    /// Returns the animated interval start, or null.
    public Date getIntervalStart() {
        return intervalStart;
    }

    /// Returns the animated interval end, or null.
    public Date getIntervalEnd() {
        return intervalEnd;
    }

    /// Returns the indicator color, or null for the platform default.
    public SurfaceColor getColor() {
        return color;
    }

    @Override
    public SurfaceProgress setPadding(int all) {
        super.setPadding(all);
        return this;
    }

    @Override
    public SurfaceProgress setPadding(int top, int right, int bottom, int left) {
        super.setPadding(top, right, bottom, left);
        return this;
    }

    @Override
    public SurfaceProgress setBackground(SurfaceColor background) {
        super.setBackground(background);
        return this;
    }

    @Override
    public SurfaceProgress setCornerRadius(int radius) {
        super.setCornerRadius(radius);
        return this;
    }

    @Override
    public SurfaceProgress setAlignment(SurfaceAlignment alignment) {
        super.setAlignment(alignment);
        return this;
    }

    @Override
    public SurfaceProgress setWeight(int weight) {
        super.setWeight(weight);
        return this;
    }

    @Override
    public SurfaceProgress setSize(int widthDips, int heightDips) {
        super.setSize(widthDips, heightDips);
        return this;
    }

    @Override
    public SurfaceProgress setAction(String actionId) {
        super.setAction(actionId);
        return this;
    }

    @Override
    public SurfaceProgress setAction(String actionId, Map<String, Object> params) {
        super.setAction(actionId, params);
        return this;
    }

    @Override
    String getType() {
        return "prog";
    }

    @Override
    void serializeContent(Map<String, Object> out, Map<String, byte[]> images, int depth) {
        if (style == STYLE_CIRCULAR) {
            out.put("style", "circular");
        } else {
            out.put("style", "linear");
        }
        if (valueKey != null) {
            out.put("valueKey", valueKey);
        } else if (intervalStart != null && intervalEnd != null) {
            out.put("start", Long.valueOf(intervalStart.getTime()));
            out.put("end", Long.valueOf(intervalEnd.getTime()));
        } else if (value >= 0) {
            out.put("value", Double.valueOf(value));
        }
        if (color != null) {
            out.put("color", SurfaceSerializer.colorMap(color));
        }
    }
}
