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

    // the most bands a data point past the last label can open
    // how many bands past the labels a data point may add before it is taken as an outlier
    private static final int MAX_EXTRA_BANDS = 10000;

    // the most bands an axis with no category labels takes from its data
    private static final int MAX_DATA_BANDS = 1 << 20;

    @Override
    public void setDataBounds(double min, double max) {
        // series may pad their extent by half a band (bars do), so count by floor, not round.
        // A point a little past the labels gets its band rather than falling off the axis; one
        // further than MAX_EXTRA_BANDS past them (1e9, infinity, NaN) is an outlier and is ignored,
        // so a single bad value cannot squeeze the real categories into slivers.
        // With no labels at all the data alone says how many bands there are, so 20000 points
        // get 20000 bands; a max needing more than MAX_DATA_BANDS (or not finite) is an outlier
        // and is ignored the same way, never clamped to the cap. The chart passes the largest x
        // within getDataLimit() instead when one point is that far out.
        int fromData = 1;
        double last = Math.floor(max + 1e-9) + 1;
        if (last > 0 && last <= getDataLimit() + 1) {
            fromData = (int) last;
        }
        int n = Math.max(categories.size(), fromData);
        super.setDataBounds(-0.5, n - 0.5);
    }

    /// The largest x a data point may have and still open its own band; a point further out is
    /// an outlier the axis ignores.
    ///
    /// #### Returns
    ///
    /// the largest x that counts
    public double getDataLimit() {
        return (categories.isEmpty() ? MAX_DATA_BANDS : categories.size() + MAX_EXTRA_BANDS) - 1;
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
        // labels sit on multiples of the step, so panning never changes WHICH categories are
        // labelled; starting at the visible edge made them jump at every integer crossed.
        // Category indices are never negative.
        int first = (Math.max(0, lo) + every - 1) / every * every;
        int count = first > hi ? 0 : (hi - first) / every + 1;
        double[] out = new double[count];
        for (int i = 0; i < count; i++) {
            out[i] = first + i * every;
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
