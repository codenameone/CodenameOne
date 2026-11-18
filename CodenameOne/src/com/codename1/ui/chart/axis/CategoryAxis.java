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
package com.codename1.ui.chart.axis;

import java.util.ArrayList;
import java.util.List;

/// An axis of named, evenly spaced categories. Category `i` sits at value `i`, in the
/// middle of its band, so bars and points line up with their label.
///
/// When labels do not fit side by side the chart first rotates them, then shows every
/// n-th one, and finally truncates with an ellipsis; it never draws them over each other.
public class CategoryAxis extends Axis {
    private final List<String> categories = new ArrayList<String>();

    /// Creates a category axis.
    ///
    /// #### Parameters
    ///
    /// - `position`: one of `Axis.BOTTOM`, `Axis.LEFT`, `Axis.RIGHT` or `Axis.TOP`
    ///
    /// - `labels`: the category names, in order
    public CategoryAxis(int position, String... labels) {
        super(position);
        setGridVisible(false);
        setCategories(labels);
    }

    /// Replaces the category names.
    public CategoryAxis setCategories(String... labels) {
        categories.clear();
        if (labels != null) {
            for (String l : labels) {
                categories.add(l);
            }
        }
        return this;
    }

    /// The number of categories.
    public int getCategoryCount() {
        return categories.size();
    }

    /// The name of category `i`, or an empty string out of range.
    public String getCategory(int i) {
        return i >= 0 && i < categories.size() ? categories.get(i) : "";
    }

    @Override
    public void setDataBounds(double min, double max) {
        // series may pad their extent by half a band (bars do), so count by floor, not round
        int n = !categories.isEmpty() ? categories.size() : (int) Math.floor(max + 1e-9) + 1;
        super.setDataBounds(-0.5, n - 0.5);
    }

    @Override
    protected double[] adjustBounds(double min, double max) {
        return new double[]{min, max};
    }

    @Override
    public double[] ticks(int maxCount) {
        int lo = (int) Math.ceil(getVisibleMin());
        int hi = (int) Math.floor(getVisibleMax());
        int n = hi - lo + 1;
        if (n <= 0) {
            return new double[0];
        }
        int every = Math.max(1, (int) Math.ceil(n / (double) Math.max(1, maxCount)));
        double[] out = new double[(n + every - 1) / every];
        for (int i = 0; i < out.length; i++) {
            out[i] = lo + i * every;
        }
        return out;
    }

    @Override
    protected String defaultFormat(double value, double step) {
        return getCategory((int) Math.rint(value));
    }

    /// The width of one category band in normalized units, used to size bars.
    public double getBandWidth() {
        double span = getVisibleMax() - getVisibleMin();
        return span == 0 ? 1 : 1 / span;
    }
}
