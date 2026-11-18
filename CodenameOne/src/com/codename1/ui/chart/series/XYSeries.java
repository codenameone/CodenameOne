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
package com.codename1.ui.chart.series;

import com.codename1.ui.chart.Chart;
import com.codename1.ui.chart.ChartContext;
import com.codename1.ui.chart.ChartPoint;
import com.codename1.ui.chart.Series;

import java.util.List;

/// A series of `(x, y)` points in primitive arrays. A `NaN` y is a gap: lines break there
/// and bars are left out.
///
/// Series sharing a stack group are drawn on top of each other, each point resting on the
/// sum of the earlier series at the same index.
public abstract class XYSeries extends Series {
    double[] xs = new double[16];
    double[] ys = new double[16];
    int count;
    private double[] fromY;
    // the x of each fromY slot, so a transition matches old values to new ones by x; null matches by index
    private double[] fromX;
    private double[] fromBase;
    // each extra channel's offset from its point's value where the last transition started, per
    // fromY slot; null on entry, so a first appearance grows out of the point's own value
    private double[][] fromAux;
    private String stackGroup;
    private int maxPoints;
    /// Extra values per point (open/high/low, a range start, quartiles), parallel to `ys`.
    double[][] aux;

    /// Creates an empty series.
    ///
    /// #### Parameters
    ///
    /// - `name`: the legend name
    protected XYSeries(String name) {
        super(name);
    }

    /// Creates a series that keeps `auxChannels` extra values beside each y.
    ///
    /// #### Parameters
    ///
    /// - `name`: the series name
    ///
    /// - `auxChannels`: number of extra values per point
    protected XYSeries(String name, int auxChannels) {
        super(name);
        aux = new double[auxChannels][16];
    }

    /// Replaces all points, each with its extra values.
    ///
    /// #### Parameters
    ///
    /// - `x`: x values, or null for 0, 1, 2...
    ///
    /// - `y`: the main values
    ///
    /// - `channels`: one array per extra channel, each as long as `y`
    protected void setRows(double[] x, double[] y, double[][] channels) {
        // validate before begin(), so a bad call leaves the series and its transition untouched
        if (x != null && x.length < y.length) {
            throw new IllegalArgumentException("x has " + x.length + " values for " + y.length + " y values");
        }
        for (int k = 0; k < aux.length; k++) {
            if (channels == null || k >= channels.length || channels[k] == null || channels[k].length < y.length) {
                throw new IllegalArgumentException("channel " + k + " is shorter than y");
            }
        }
        begin();
        count = y.length;
        int cap = Math.max(16, count);
        xs = new double[cap];
        ys = new double[cap];
        for (int k = 0; k < aux.length; k++) {
            aux[k] = new double[cap];
        }
        for (int i = 0; i < count; i++) {
            xs[i] = x == null ? i : x[i];
            ys[i] = y[i];
            for (int k = 0; k < aux.length; k++) {
                aux[k][i] = channels[k][i];
            }
        }
        dataChanged();
    }

    /// Appends one point with its extra values, sliding the window when a maximum is set.
    ///
    /// #### Parameters
    ///
    /// - `x`: the x value
    ///
    /// - `y`: the main value
    ///
    /// - `values`: the extra values, one per channel
    protected void addRow(double x, double y, double[] values) {
        begin();
        slide();
        ensure(count + 1);
        xs[count] = x;
        ys[count] = y;
        for (int k = 0; k < aux.length; k++) {
            aux[k][count] = values[k];
        }
        count++;
        dataChanged();
    }

    /// An extra value of point `i`.
    ///
    /// #### Parameters
    ///
    /// - `channel`: the channel index
    ///
    /// - `i`: the point index
    ///
    /// #### Returns
    ///
    /// the stored value
    /// The value an extra channel takes for a point added without one, by `add(x, y)` or
    /// `setData`. The default is the point's own value, which collapses an open/high/low, a box or an
    /// error bar onto the point itself; a subclass whose channel means something else overrides it.
    ///
    /// #### Parameters
    ///
    /// - `channel`: the extra channel
    ///
    /// - `y`: the point's value
    ///
    /// #### Returns
    ///
    /// the channel's value for that point
    /// Replaces the value and extra channels of the point at `i` in place.
    void replaceRow(int i, double y, double[] values) {
        begin();
        ys[i] = y;
        for (int k = 0; k < aux.length; k++) {
            aux[k][i] = values[k];
        }
        dataChanged();
    }

    /// Removes the point at `i`, closing the gap it leaves.
    void removeRow(int i) {
        begin();
        int tail = count - i - 1;
        System.arraycopy(xs, i + 1, xs, i, tail);
        System.arraycopy(ys, i + 1, ys, i, tail);
        if (aux != null) {
            for (double[] channel : aux) {
                System.arraycopy(channel, i + 1, channel, i, tail);
            }
        }
        count--;
        dataChanged();
    }

    /// The index of the first point at exactly `x`, by a linear scan that does not assume the
    /// series is sorted; -1 when there is none.
    int scanIndexOfX(double x) {
        for (int i = 0; i < count; i++) {
            if (xs[i] == x) {
                return i;
            }
        }
        return -1;
    }

    protected double auxDefault(int channel, double y) {
        return y;
    }

    protected double getAux(int channel, int i) {
        return aux[channel][i];
    }

    private void slide() {
        if (maxPoints > 0 && count >= maxPoints) {
            System.arraycopy(xs, 1, xs, 0, count - 1);
            System.arraycopy(ys, 1, ys, 0, count - 1);
            if (aux != null) {
                for (double[] channel : aux) {
                    System.arraycopy(channel, 1, channel, 0, count - 1);
                    channel[count - 1] = 0;
                }
            }
            // clear the vacated slot so nothing of the dropped point can leak into the next one
            xs[count - 1] = 0;
            ys[count - 1] = 0;
            count--;
        }
    }

    @Override
    public boolean isCartesian() {
        return true;
    }

    /// Replaces all data. `x` may be null, in which case points are numbered 0, 1, 2 ...
    /// (the positions of a category axis).
    public XYSeries setData(double[] x, double[] y) {
        // validate before begin(), so a bad call leaves the series and its transition untouched
        if (x != null && x.length < y.length) {
            throw new IllegalArgumentException("x has " + x.length + " values for " + y.length + " y values");
        }
        begin();
        count = y.length;
        xs = new double[Math.max(16, count)];
        ys = new double[Math.max(16, count)];
        for (int i = 0; i < count; i++) {
            xs[i] = x == null ? i : x[i];
            ys[i] = y[i];
        }
        if (aux != null) {
            for (int k = 0; k < aux.length; k++) {
                double[] na = new double[xs.length];
                for (int i = 0; i < count; i++) {
                    na[i] = auxDefault(k, y[i]);
                }
                aux[k] = na;
            }
        }
        dataChanged();
        return this;
    }

    /// Replaces all data with values at x = 0, 1, 2 ...
    public XYSeries setValues(double... y) {
        return setData(null, y);
    }

    /// Appends a point at the next index.
    public XYSeries add(double y) {
        return add(count == 0 ? 0 : xs[count - 1] + 1, y);
    }

    /// Appends a point. With `setMaxPoints` the oldest point drops off, for real time data.
    public XYSeries add(double x, double y) {
        begin();
        slide();
        ensure(count + 1);
        xs[count] = x;
        ys[count] = y;
        if (aux != null) {
            for (int k = 0; k < aux.length; k++) {
                aux[k][count] = auxDefault(k, y);
            }
        }
        count++;
        dataChanged();
        return this;
    }

    /// Changes the value at `index`.
    public XYSeries set(int index, double y) {
        if (index < 0 || index >= count) {
            throw new IndexOutOfBoundsException("Index " + index + " of a series of " + count + " points");
        }
        begin();
        ys[index] = y;
        dataChanged();
        return this;
    }

    /// Removes every point.
    public XYSeries clear() {
        begin();
        count = 0;
        dataChanged();
        return this;
    }

    /// Keeps at most `n` points, dropping the oldest as new ones arrive; 0 keeps all.
    public XYSeries setMaxPoints(int n) {
        maxPoints = n;
        if (n > 0 && count > n) {
            // trim now rather than on the next add, so the limit holds from the moment it is set
            begin();
            int drop = count - n;
            System.arraycopy(xs, drop, xs, 0, n);
            System.arraycopy(ys, drop, ys, 0, n);
            if (aux != null) {
                for (double[] channel : aux) {
                    System.arraycopy(channel, drop, channel, 0, n);
                }
            }
            count = n;
            dataChanged();
        }
        return this;
    }

    /// Stacks this series on earlier ones sharing the same group name; null for none.
    public XYSeries setStackGroup(String group) {
        stackGroup = group;
        dataChanged();
        return this;
    }

    /// The stack group, or null.
    public String getStackGroup() {
        return stackGroup;
    }

    /// The number of points.
    public int size() {
        return count;
    }

    /// The x value at `i`.
    public double getX(int i) {
        return xs[i];
    }

    /// The y value at `i`.
    public double getY(int i) {
        return ys[i];
    }

    private void ensure(int n) {
        if (n > xs.length) {
            int cap = Math.max(n, xs.length * 2);
            double[] nx = new double[cap];
            double[] ny = new double[cap];
            System.arraycopy(xs, 0, nx, 0, count);
            System.arraycopy(ys, 0, ny, 0, count);
            xs = nx;
            ys = ny;
            if (aux != null) {
                for (int k = 0; k < aux.length; k++) {
                    double[] na = new double[cap];
                    System.arraycopy(aux[k], 0, na, 0, count);
                    aux[k] = na;
                }
            }
        }
    }

    private void begin() {
        Chart c = getChart();
        if (c != null) {
            c.beginChange();
        }
    }

    /// The base this series' point `i` rests on: 0, or the stacked sum of the earlier series
    /// in its group.
    public double getBase(int i) {
        if (stackGroup == null || getChart() == null) {
            return 0;
        }
        double sum = 0;
        double x = xs[i];
        // positives stack up from zero and negatives down from it, so mixed signs never overlap.
        // The same index is checked first, so the usual aligned stack costs one look per series.
        boolean negative = ys[i] < 0;
        List<Series> all = getChart().getSeries();
        for (Series o : all) {
            if (o == this) { //NOPMD CompareObjectsWithEquals - identity of the same instance is intended
                break;
            }
            if (o instanceof XYSeries && o.isVisible()) {
                XYSeries xo = (XYSeries) o;
                if (stackGroup.equals(xo.stackGroup) && xo.isSecondaryAxis() == isSecondaryAxis()) {
                    int j = i < xo.count && xo.xs[i] == x ? i : xo.indexOfX(x);
                    if (j >= 0) {
                        double v = xo.ys[j];
                        if (v == v && (v < 0) == negative) {
                            sum += v;
                        }
                    }
                }
            }
        }
        return sum;
    }

    // 0 unknown, 1 ascending, 2 not; reset by every change to the data
    private int orderState;

    @Override
    protected void dataChanged() {
        orderState = 0;
        super.dataChanged();
    }

    /// True when the x values never decrease, as a time series' do; a scatter filled in any
    /// order is not. Checked once per change to the data.
    public boolean isAscending() {
        if (orderState == 0) {
            orderState = 1;
            for (int i = 1; i < count; i++) {
                if (xs[i] < xs[i - 1]) {
                    orderState = 2;
                    break;
                }
            }
        }
        return orderState == 1;
    }

    /// The index whose x is closest to `x` among the points that have a y value, by binary
    /// search; only meaningful when `isAscending()`.
    ///
    /// #### Parameters
    ///
    /// - `x`: a data value on the x axis
    ///
    /// #### Returns
    ///
    /// the nearest index, or -1 for an empty series
    public int nearestIndexOfX(double x) {
        if (count == 0) {
            return -1;
        }
        int lo = 0;
        int hi = count - 1;
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (xs[mid] < x) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        int best = Math.abs(xs[lo] - x) <= Math.abs(xs[hi] - x) ? lo : hi;
        if (ys[best] == ys[best]) {
            return best;
        }
        // a gap: the nearest neighbour on either side that has a value
        for (int d = 1; d < count; d++) {
            int a = best - d;
            int b = best + d;
            boolean aok = a >= 0 && ys[a] == ys[a];
            boolean bok = b < count && ys[b] == ys[b];
            if (aok && bok) {
                return Math.abs(xs[a] - x) <= Math.abs(xs[b] - x) ? a : b;
            }
            if (aok) {
                return a;
            }
            if (bok) {
                return b;
            }
            if (a < 0 && b >= count) {
                break;
            }
        }
        return -1;
    }

    int indexOfX(double x) {
        int lo = 0;
        int hi = count - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            double v = xs[mid];
            if (v < x) {
                lo = mid + 1;
            } else if (v > x) {
                hi = mid - 1;
            } else {
                return mid;
            }
        }
        return -1;
    }

    /// The y shown at `i` in the current frame: the value interpolated from where the last
    /// transition started.
    public double animatedY(ChartContext ctx, int i) {
        double t = ctx == null ? 1 : ctx.getProgress();
        double target = ys[i];
        if (fromY == null || t >= 1) {
            return target;
        }
        int j = fromSlot(i);
        return j < 0 ? target : lerp(fromY[j], target, t);
    }

    // the slot of fromY/fromBase that point i animates from, or -1 for none. Matched by x when the
    // old and new data are both ascending, so a point that slid one place left (setMaxPoints) keeps
    // its own old value instead of taking its neighbour's; a new x starts from the old point just
    // before it, so an appended point grows out of the previous last point instead of snapping in.
    private int fromSlot(int i) {
        if (fromY == null) {
            return -1;
        }
        if (fromX == null || !isAscending()) {
            return i < fromY.length ? i : -1;
        }
        int n = fromX.length;
        if (n == 0) {
            return -1;
        }
        double x = xs[i];
        int lo = 0;
        int hi = n;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (fromX[mid] < x) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        if (lo < n && fromX[lo] == x) {
            return lo;
        }
        return lo > 0 ? lo - 1 : 0;
    }

    /// Extra channel `channel` of point `i` as shown in the current frame. It moves with the
    /// point's animated value and, like it, from where the last transition started, matched by x;
    /// so a change to one point leaves every other point still. A point with no earlier state
    /// (the series' entry) grows out of its own value.
    ///
    /// #### Parameters
    ///
    /// - `ctx`: the drawing context, or null for the settled value
    ///
    /// - `channel`: the extra channel
    ///
    /// - `i`: the point index
    ///
    /// #### Returns
    ///
    /// the channel value for this frame
    public double animatedAux(ChartContext ctx, int channel, int i) {
        double t = ctx == null ? 1 : ctx.getProgress();
        double target = aux[channel][i];
        if (fromY == null || t >= 1 || target != target) {
            return target;
        }
        double y = ys[i];
        double off = target - y;
        if (off != off) {
            return target;
        }
        double ay = animatedY(ctx, i);
        int j = fromSlot(i);
        double from = 0;
        if (fromAux != null && j >= 0 && j < fromAux[channel].length) {
            double f = fromAux[channel][j];
            if (f == f) {
                from = f;
            }
        }
        return ay + lerp(from, off, t);
    }

    /// The stacked base shown at `i` in the current frame.
    public double animatedBase(ChartContext ctx, int i) {
        double target = getBase(i);
        double t = ctx == null ? 1 : ctx.getProgress();
        if (fromBase == null || fromY == null || t >= 1) {
            return target;
        }
        int j = fromSlot(i);
        return j < 0 || j >= fromBase.length ? target : lerp(fromBase[j], target, t);
    }

    @Override
    protected void captureTransitionStart() {
        ChartContext ctx = getChart() == null ? null : getChart().getLastContext();
        double[] fy = new double[count];
        double[] fb = new double[count];
        for (int i = 0; i < count; i++) {
            fy[i] = animatedY(ctx, i);
            fb[i] = animatedBase(ctx, i);
        }
        double[][] fa = null;
        if (aux != null) {
            fa = new double[aux.length][count];
            for (int k = 0; k < aux.length; k++) {
                for (int i = 0; i < count; i++) {
                    fa[k][i] = animatedAux(ctx, k, i) - fy[i];
                }
            }
        }
        double[] fx = null;
        if (isAscending()) {
            fx = new double[count];
            System.arraycopy(xs, 0, fx, 0, count);
        }
        fromY = fy;
        fromBase = fb;
        fromX = fx;
        fromAux = fa;
    }

    @Override
    protected void captureEntryStart() {
        fromY = new double[count];
        fromX = null;
        fromAux = null;
        fromBase = new double[count];
        for (int i = 0; i < count; i++) {
            fromBase[i] = getBase(i);
        }
    }

    @Override
    public double[] getXBounds() {
        if (count == 0) {
            return null;
        }
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            min = Math.min(min, xs[i]);
            max = Math.max(max, xs[i]);
        }
        return new double[]{min, max};
    }

    @Override
    public double[] getYBounds() {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        boolean any = false;
        for (int i = 0; i < count; i++) {
            double v = ys[i];
            if (v != v) {
                continue;
            }
            double b = getBase(i);
            min = Math.min(min, Math.min(b, b + v));
            max = Math.max(max, Math.max(b, b + v));
            any = true;
        }
        if (!any) {
            return null;
        }
        if (stackGroup == null) {
            // a lone series should not drag the axis to 0 unless the chart asks for it
            min = Double.MAX_VALUE;
            max = -Double.MAX_VALUE;
            for (int i = 0; i < count; i++) {
                double v = ys[i];
                if (v == v) {
                    min = Math.min(min, v);
                    max = Math.max(max, v);
                }
            }
        }
        return new double[]{min, max};
    }

    /// The smallest gap between neighbouring x values, the width of one bar or candle slot;
    /// 1 when there are fewer than two points, which is a category band.
    public double getMinXSpacing() {
        double m = Double.MAX_VALUE;
        for (int i = 1; i < count; i++) {
            double d = xs[i] - xs[i - 1];
            if (d > 0 && d < m) {
                m = d;
            }
        }
        return m == Double.MAX_VALUE ? 1 : m;
    }

    /// First index whose x is at or after `x`, for culling points outside the viewport.
    protected int lowerBound(double x) {
        int lo = 0;
        int hi = count;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (xs[mid] < x) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    /// The index range `{from, to}` (inclusive) worth painting: the visible points plus one
    /// neighbour each side so lines run to the plot edge. Data whose x values are not
    /// ascending (a scatter filled in any order) cannot be binary searched, so every point is
    /// in range then.
    protected int[] visibleRange(ChartContext ctx) {
        if (count == 0) {
            return new int[]{0, -1};
        }
        if (!isAscending()) {
            return new int[]{0, count - 1};
        }
        double lo = ctx.getXAxis().getVisibleMin();
        double hi = ctx.getXAxis().getVisibleMax();
        int a = Math.max(0, lowerBound(lo) - 1);
        int b = Math.min(count - 1, lowerBound(hi));
        return new int[]{a, b};
    }

    /// The screen position of point `i` along one direction, relative to the chart like
    /// `ChartPoint.getScreenX()`, without allocating a point.
    ///
    /// #### Parameters
    ///
    /// - `ctx`: the frame
    ///
    /// - `i`: the point index
    ///
    /// - `vertical`: true for the y coordinate, false for x
    ///
    /// #### Returns
    ///
    /// the coordinate in pixels
    public int screenPosition(ChartContext ctx, int i, boolean vertical) {
        double top = getBase(i) + ys[i];
        if (vertical) {
            return (int) ctx.screenY(this, xs[i], top) - ctx.getOriginY();
        }
        return (int) ctx.screenX(this, xs[i], top) - ctx.getOriginX();
    }

    /// The index of the point with a y value whose screen position along one direction is
    /// nearest `pos`, scanning every point; for data that is not ascending. Allocates nothing,
    /// as it runs on every pointer move.
    ///
    /// #### Parameters
    ///
    /// - `ctx`: the frame
    ///
    /// - `pos`: the pointer coordinate, relative to the chart
    ///
    /// - `vertical`: true to compare y coordinates, false for x
    ///
    /// #### Returns
    ///
    /// the index, or -1 when no point has a value
    public int nearestScreenIndex(ChartContext ctx, int pos, boolean vertical) {
        int best = -1;
        int bestD = Integer.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            if (ys[i] != ys[i]) {
                continue;
            }
            int d = Math.abs(screenPosition(ctx, i, vertical) - pos);
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    @Override
    public ChartPoint hitTest(ChartContext ctx, int x, int y, int radius) {
        int[] r = visibleRange(ctx);
        int best = -1;
        double bestD = (double) radius * radius;
        int ox = ctx.getOriginX();
        int oy = ctx.getOriginY();
        for (int i = r[0]; i <= r[1]; i++) {
            double v = ys[i];
            if (v != v) {
                continue;
            }
            double top = getBase(i) + v;
            double dx = ctx.screenX(this, xs[i], top) - ox - x;
            double dy = ctx.screenY(this, xs[i], top) - oy - y;
            double d = dx * dx + dy * dy;
            if (d <= bestD) {
                bestD = d;
                best = i;
            }
        }
        return best < 0 ? null : getPoint(ctx, best);
    }

    @Override
    public void collectTableRows(List<String[]> rows) {
        Chart c = getChart();
        for (int i = 0; i < count; i++) {
            String item = c == null ? com.codename1.ui.chart.axis.Axis.formatNumber(xs[i], 2) : c.getXAxis().format(xs[i], 0);
            rows.add(new String[]{getName(), item, tableValue(i)});
        }
    }

    /// The text of point `i` in the accessible data table.
    ///
    /// #### Parameters
    ///
    /// - `i`: the point index
    ///
    /// #### Returns
    ///
    /// the formatted value
    protected String tableValue(int i) {
        return formatValue(null, ys[i]);
    }

    @Override
    public int getPointCount() {
        return count;
    }

    @Override
    public ChartPoint getPoint(ChartContext ctx, int index) {
        if (index < 0 || index >= count) {
            return null;
        }
        double top = getBase(index) + ys[index];
        int sx = (int) ctx.screenX(this, xs[index], top) - ctx.getOriginX();
        int sy = (int) ctx.screenY(this, xs[index], top) - ctx.getOriginY();
        return new ChartPoint(this, index, xs[index], ys[index], sx, sy, null);
    }
}
