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

import java.util.Arrays;
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
    // each extra channel's offset from its point's value where the last transition started, per
    // fromY slot; null on entry, so a first appearance grows out of the point's own value
    private double[][] fromAux;
    // the stacked base as painted where the last transition started, per fromY slot; null when
    // there is no transition, so the base is the settled one
    private double[] fromBase;
    // for a series that rests on a stack base, its value as painted (visibility weighted) where
    // the last transition started, per fromY slot, so the layer and the base above it tween alike
    private double[] fromPainted;
    private String stackGroup;
    private int maxPoints;
    /// Extra values per point (open/high/low, a range start, quartiles), parallel to `ys`.
    double[][] aux;
    // per channel and point: true when the value was given explicitly, false when it came from
    // auxDefault and so follows the point's value through set()
    private boolean[][] explicit;

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
        explicit = new boolean[auxChannels][16];
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
            explicit[k] = new boolean[cap];
        }
        for (int i = 0; i < count; i++) {
            xs[i] = x == null ? i : x[i];
            ys[i] = y[i];
            for (int k = 0; k < aux.length; k++) {
                aux[k][i] = channels[k][i];
                explicit[k][i] = true;
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
            explicit[k][count] = true;
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
            explicit[k][i] = true;
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
            for (int k = 0; k < aux.length; k++) {
                System.arraycopy(aux[k], i + 1, aux[k], i, tail);
                System.arraycopy(explicit[k], i + 1, explicit[k], i, tail);
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

    // marks every extra channel of point i as given explicitly, so set() leaves them alone
    void markExplicit(int i) {
        if (explicit != null) {
            for (boolean[] e : explicit) {
                e[i] = true;
            }
        }
    }

    private void slide() {
        if (maxPoints > 0 && count >= maxPoints) {
            System.arraycopy(xs, 1, xs, 0, count - 1);
            System.arraycopy(ys, 1, ys, 0, count - 1);
            if (aux != null) {
                for (int k = 0; k < aux.length; k++) {
                    System.arraycopy(aux[k], 1, aux[k], 0, count - 1);
                    aux[k][count - 1] = 0;
                    System.arraycopy(explicit[k], 1, explicit[k], 0, count - 1);
                    explicit[k][count - 1] = false;
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
                explicit[k] = new boolean[xs.length];
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
                explicit[k][count] = false;
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
        if (aux != null) {
            // a channel derived from the value follows it; one given explicitly stays put
            for (int k = 0; k < aux.length; k++) {
                if (!explicit[k][index]) {
                    aux[k][index] = auxDefault(k, y);
                }
            }
        }
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
                for (int k = 0; k < aux.length; k++) {
                    System.arraycopy(aux[k], drop, aux[k], 0, n);
                    System.arraycopy(explicit[k], drop, explicit[k], 0, n);
                }
            }
            count = n;
            dataChanged();
        }
        return this;
    }

    /// Stacks this series on earlier ones sharing the same group name; null for none.
    public XYSeries setStackGroup(String group) {
        // snapshot first: the bases move, and they must move from what is on screen
        begin();
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
                    boolean[] ne = new boolean[cap];
                    System.arraycopy(explicit[k], 0, ne, 0, count);
                    explicit[k] = ne;
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
        return stackBase(false, i);
    }

    // the sum of the earlier layers under point i: settled values weighted by the fraction each
    // is shown now, or with `target` by the fraction it ends at (1 shown, 0 hidden)
    private double stackBase(boolean target, int i) {
        if (stackGroup == null || getChart() == null) {
            return 0;
        }
        double sum = 0;
        double x = xs[i];
        if (isGap(x)) {
            // a point with no x has no column to stack on; its value is a gap anyway
            return 0;
        }
        // positives stack up from zero and negatives down from it, so mixed signs never overlap.
        // The same index is checked first, so the usual aligned stack costs one look per series.
        boolean negative = ys[i] < 0;
        List<Series> all = getChart().getSeries();
        for (Series o : all) {
            if (o == this) { //NOPMD CompareObjectsWithEquals - identity of the same instance is intended
                break;
            }
            if (o instanceof XYSeries && ((XYSeries) o).consumesStackBase()) {
                // a mark that keeps its own values (scatter) is not a layer and lifts nothing
                XYSeries xo = (XYSeries) o;
                // a series fading in or out carries its layers by the fraction it is shown, so
                // the stack above it slides with the fade instead of snapping at its start
                float frac = target ? (xo.isVisible() ? 1 : 0) : xo.getVisibilityFraction();
                if (!(frac > 0)) { //NOPMD LogicInversion - also skips a NaN fraction
                    continue;
                }
                if (stackGroup.equals(xo.stackGroup) && xo.isSecondaryAxis() == isSecondaryAxis()) {
                    int j = i < xo.count && xo.xs[i] == x ? i : xo.indexOfX(x);
                    if (j >= 0) {
                        double v = xo.ys[j];
                        if (!isGap(v) && (v < 0) == negative) {
                            sum += v * frac;
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
        minXSpacing = 0;
        super.dataChanged();
    }

    /// True when point `i` has no value to show: its x or its y is NaN or infinite.
    ///
    /// #### Parameters
    ///
    /// - `i`: the point index
    ///
    /// #### Returns
    ///
    /// true for a gap
    public boolean isGapAt(int i) {
        return isGap(xs[i]) || isGap(ys[i]);
    }

    /// True when the x values never decrease, as a time series' do; a scatter filled in any
    /// order is not. Checked once per change to the data.
    public boolean isAscending() {
        if (orderState == 0) {
            orderState = count > 0 && isGap(xs[0]) ? 2 : 1;
            for (int i = 1; i < count; i++) {
                // a non-finite x cannot be ordered: binary searches over it would go astray, so
                // such data takes the linear-scan paths that skip it as a gap
                if (xs[i] < xs[i - 1] || isGap(xs[i]) || isGap(xs[i - 1])) {
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
        if (!isGapAt(best)) {
            return best;
        }
        // a gap: the nearest neighbour on either side that has a value
        for (int d = 1; d < count; d++) {
            int a = best - d;
            int b = best + d;
            boolean aok = a >= 0 && !isGapAt(a);
            boolean bok = b < count && !isGapAt(b);
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

    // the index whose x equals `x`, or -1; never a gap slot. Binary search only over ascending
    // data: an unordered series (or one with a non-finite x, which isAscending() rejects) would
    // send the search astray, and a NaN x compares neither less nor greater than anything, so
    // the search would answer that slot for every query
    int indexOfX(double x) {
        if (isGap(x)) {
            return -1;
        }
        if (!isAscending()) {
            for (int i = 0; i < count; i++) {
                if (xs[i] == x) {
                    return i;
                }
            }
            return -1;
        }
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
        double v = rawAnimatedY(ctx, i);
        if (ctx == null || stackGroup == null || !consumesStackBase()) {
            return v;
        }
        // a stack layer fading in or out is drawn at the fraction it is shown. Mid transition it
        // tweens from its painted value to its settled weighted one with the same progress the
        // base above it tweens with (animatedBase), so the two meet and the stack never gaps
        double t = ctx.getProgress();
        int j = fromSlot(i);
        if (fromPainted != null && t < 1 && j >= 0 && j < fromPainted.length) {
            return lerp(fromPainted[j], ys[i] * (isVisible() ? 1 : 0), t);
        }
        float f = getVisibilityFraction();
        if (f >= 0 && f < 1) {
            v *= f;
        }
        return v;
    }

    /// True for a series drawn on its stack base (an area or a bar), which a stack group lifts
    /// and whose layer shrinks with the fraction it is shown while it fades; false for marks
    /// that keep their own values (scatter, error bars, candles, boxes).
    ///
    /// #### Returns
    ///
    /// false by default
    protected boolean consumesStackBase() {
        return false;
    }

    // the interpolated value without the visibility weighting of a fading stack layer
    private double rawAnimatedY(ChartContext ctx, int i) {
        double t = ctx == null ? 1 : ctx.getProgress();
        double target = ys[i];
        if (fromY == null || t >= 1) {
            return target;
        }
        int j = fromSlot(i);
        return j < 0 ? target : lerp(fromY[j], target, t);
    }

    // the slot of fromY that point i animates from, or -1 for none. Matched by x when the
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
        if (fromY == null || t >= 1 || isGap(target)) {
            return target;
        }
        if (!isAuxRelative(channel)) {
            // an absolute channel (a bubble size) has no relation to y: a matched point tweens
            // its own old value, a new point (and every point of a first appearance) grows from zero
            int j = fromSlot(i);
            if (fromAux == null) {
                return lerp(0, target, t);
            }
            if (j < 0 || fromX != null && fromX[j] != xs[i]) { //NOPMD CompareDoubles - exact x match intended
                return lerp(0, target, t);
            }
            double f = j >= fromAux[channel].length ? Double.NaN : fromAux[channel][j];
            return isGap(f) ? target : lerp(f, target, t);
        }
        double y = ys[i];
        double off = target - y;
        if (isGap(off)) {
            return target;
        }
        double ay = rawAnimatedY(ctx, i);
        int j = fromSlot(i);
        double from = 0;
        if (fromAux != null && j >= 0 && j < fromAux[channel].length) {
            double f = fromAux[channel][j];
            if (!isGap(f)) {
                from = f;
            }
        }
        return ay + lerp(from, off, t);
    }

    /// True when extra channel `channel` is a value on the y scale (a high, a low, a quartile)
    /// and so animates as an offset from the point's value; false for a channel of its own
    /// scale, such as a bubble size, which animates by itself.
    ///
    /// #### Parameters
    ///
    /// - `channel`: the extra channel
    ///
    /// #### Returns
    ///
    /// true by default
    protected boolean isAuxRelative(int channel) {
        return true;
    }

    /// The stacked base shown at `i` in the current frame.
    public double animatedBase(ChartContext ctx, int i) {
        double t = ctx == null ? 1 : ctx.getProgress();
        if (t >= 1 || fromBase == null) {
            return getBase(i);
        }
        // tweened from the base as painted when the change began to the settled one, with the
        // same progress as the layers below (animatedY), so a fade, a removed layer or a new
        // group all slide instead of snapping
        double target = targetBase(i);
        int j = fromSlot(i);
        return j < 0 || j >= fromBase.length ? target : lerp(fromBase[j], target, t);
    }

    /// The base point `i` settles on once the running transition ends: the stack of the earlier
    /// layers weighted by the fraction each ends at, or [#getBase(int)] outside a stack group.
    /// A series whose base is its own data (a range bar's start, a waterfall's running total)
    /// overrides this to answer that, so the tween ends exactly where the settled value is.
    ///
    /// - `i`: the point index
    ///
    /// #### Returns
    ///
    /// the settled base
    protected double targetBase(int i) {
        return stackGroup == null ? getBase(i) : stackBase(true, i);
    }

    // the stack under point i as painted in the current frame: each earlier layer's drawn value
    private double paintedStackBase(ChartContext ctx, int i) {
        double x = xs[i];
        if (stackGroup == null || isGap(x)) {
            return getBase(i);
        }
        boolean negative = ys[i] < 0;
        double sum = 0;
        for (Series o : getChart().getSeries()) {
            if (o == this) { //NOPMD CompareObjectsWithEquals - identity of the same instance is intended
                break;
            }
            if (o instanceof XYSeries && ((XYSeries) o).consumesStackBase()) {
                XYSeries xo = (XYSeries) o;
                if (stackGroup.equals(xo.stackGroup) && xo.isSecondaryAxis() == isSecondaryAxis()) {
                    int j = i < xo.count && xo.xs[i] == x ? i : xo.indexOfX(x);
                    if (j >= 0 && !isGap(xo.ys[j]) && (xo.ys[j] < 0) == negative) {
                        double v = xo.animatedY(ctx, j);
                        if (!isGap(v)) {
                            sum += v;
                        }
                    }
                }
            }
        }
        return sum;
    }

    @Override
    protected void captureTransitionStart() {
        ChartContext ctx = getChart() == null ? null : getChart().getLastContext();
        double[] fy = new double[count];
        double[] fb = new double[count];
        double[] fp = consumesStackBase() ? new double[count] : null;
        for (int i = 0; i < count; i++) {
            fy[i] = rawAnimatedY(ctx, i);
            fb[i] = animatedBase(ctx, i);
            if (fp != null) {
                fp[i] = animatedY(ctx, i);
            }
        }
        double[][] fa = null;
        if (aux != null) {
            fa = new double[aux.length][count];
            for (int k = 0; k < aux.length; k++) {
                for (int i = 0; i < count; i++) {
                    double a = animatedAux(ctx, k, i);
                    fa[k][i] = isAuxRelative(k) ? a - fy[i] : a;
                }
            }
        }
        double[] fx = null;
        if (isAscending()) {
            fx = new double[count];
            System.arraycopy(xs, 0, fx, 0, count);
        }
        fromY = fy;
        fromX = fx;
        fromAux = fa;
        fromBase = fb;
        fromPainted = fp;
    }

    @Override
    protected void transitionEnded() {
        fromY = null;
        fromX = null;
        fromAux = null;
        fromBase = null;
        fromPainted = null;
    }

    @Override
    protected void captureEntryStart() {
        fromY = new double[count];
        fromX = null;
        fromAux = null;
        // the chart's first entry grows the whole stack out of zero together; a series added
        // to a chart already shown rests on the lower layers as painted and only its own value
        // grows from zero, so it never dips into the stack below it
        double[] fb = new double[count];
        Chart c = getChart();
        ChartContext ctx = c == null ? null : c.getLastContext();
        if (ctx != null && stackGroup != null && consumesStackBase()) {
            for (int i = 0; i < count; i++) {
                fb[i] = paintedStackBase(ctx, i);
            }
        }
        fromBase = fb;
        fromPainted = consumesStackBase() ? new double[count] : null;
    }

    @Override
    public double[] getXBounds() {
        if (count == 0) {
            return null;
        }
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            double x = xs[i];
            // one NaN x would turn the whole min/max into NaN and drop the series from the axis
            if (isGap(x)) {
                continue;
            }
            min = Math.min(min, x);
            max = Math.max(max, x);
        }
        return min > max ? null : new double[]{min, max};
    }

    @Override
    public double[] getYBounds() {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        boolean any = false;
        for (int i = 0; i < count; i++) {
            double v = ys[i];
            if (isGap(v)) {
                continue;
            }
            // a mark that keeps its own values is drawn at them, never lifted by the stack
            double b = consumesStackBase() ? getBase(i) : 0;
            if (isGap(b)) {
                continue;
            }
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
                if (!isGap(v)) {
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
        // computed once per change to the data: paint asks for it every frame
        if (minXSpacing > 0) {
            return minXSpacing;
        }
        double[] v = xs;
        int n = count;
        if (!isAscending()) {
            // descending or unsorted x (or one with gaps) still has slots as wide as the
            // closest two distinct values; sort a copy of the finite ones to find them
            if (spacingBuffer == null || spacingBuffer.length < count) {
                spacingBuffer = new double[count];
            }
            n = 0;
            for (int i = 0; i < count; i++) {
                if (!isGap(xs[i])) {
                    spacingBuffer[n++] = xs[i];
                }
            }
            for (int i = n; i < spacingBuffer.length; i++) {
                spacingBuffer[i] = Double.MAX_VALUE;
            }
            Arrays.sort(spacingBuffer);
            v = spacingBuffer;
        }
        double m = Double.MAX_VALUE;
        for (int i = 1; i < n; i++) {
            double d = v[i] - v[i - 1];
            if (d > 0 && d < m) {
                m = d;
            }
        }
        minXSpacing = m == Double.MAX_VALUE ? 1 : m;
        return minXSpacing;
    }

    // 0 until getMinXSpacing computes it; reset by every change to the data
    private double minXSpacing;
    private double[] spacingBuffer;

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
        double top = paintedTop(ctx, i);
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
            if (isGapAt(i)) {
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

    // where point i is drawn in this frame, so a hover finds what is on screen mid transition
    private double paintedTop(ChartContext ctx, int i) {
        if (!consumesStackBase()) {
            return animatedY(ctx, i);
        }
        return animatedBase(ctx, i) + animatedY(ctx, i);
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
            if (isGap(v) || isGap(xs[i])) {
                continue;
            }
            double top = paintedTop(ctx, i);
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
            if (isGapAt(i)) {
                // a gap has no value to read out; a row of NaN would be noise
                continue;
            }
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
        double top = paintedTop(ctx, index);
        int sx = (int) ctx.screenX(this, xs[index], top) - ctx.getOriginX();
        int sy = (int) ctx.screenY(this, xs[index], top) - ctx.getOriginY();
        return new ChartPoint(this, index, xs[index], ys[index], sx, sy, null);
    }
}
