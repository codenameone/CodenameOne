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

import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.chart.ChartContext;
import com.codename1.ui.chart.ChartPoint;
import com.codename1.ui.chart.ColorScale;
import com.codename1.ui.chart.Series;

/// A heatmap: a grid of cells coloured by value. Column `c` sits at x = c and row `r` at y = r,
/// so pair it with a `CategoryAxis` on each axis to name the columns and rows.
///
/// ```java
/// chart.setXAxis(new CategoryAxis(Axis.BOTTOM, "Mon", "Tue", "Wed"));
/// chart.setYAxis(new CategoryAxis(Axis.LEFT, "Morning", "Noon", "Evening"));
/// chart.addSeries(new HeatmapSeries("Visits").setMatrix(values));
/// ```
public class HeatmapSeries extends Series {
    private double[][] matrix = new double[0][0];
    private double[][] from;
    private ColorScale scale;
    private Double fixedMin;
    private Double fixedMax;

    /// Creates an empty heatmap.
    ///
    /// #### Parameters
    ///
    /// - `name`: the series name
    public HeatmapSeries(String name) {
        super(name);
    }

    @Override
    public boolean isCartesian() {
        return true;
    }

    /// Replaces the values, animated.
    ///
    /// #### Parameters
    ///
    /// - `values`: `values[row][column]`; `NaN` leaves a cell empty
    ///
    /// #### Returns
    ///
    /// this series
    public HeatmapSeries setMatrix(double[][] values) {
        if (getChart() != null) {
            getChart().beginChange();
        }
        matrix = new double[values.length][];
        for (int r = 0; r < values.length; r++) {
            matrix[r] = new double[values[r].length];
            System.arraycopy(values[r], 0, matrix[r], 0, values[r].length);
        }
        dataChanged();
        return this;
    }

    /// The value of a cell.
    public double getValue(int row, int column) {
        return matrix[row][column];
    }

    /// The number of rows.
    public int getRows() {
        return matrix.length;
    }

    /// The number of columns.
    public int getColumns() {
        int c = 0;
        for (double[] row : matrix) {
            c = Math.max(c, row.length);
        }
        return c;
    }

    /// The colour scale; by default a sequential scale of the series colour.
    ///
    /// #### Parameters
    ///
    /// - `scale`: the scale
    ///
    /// #### Returns
    ///
    /// this series
    public HeatmapSeries setColorScale(ColorScale scale) {
        this.scale = scale;
        repaintChart();
        return this;
    }

    /// Fixes the values mapped to the ends of the scale instead of the data's extent.
    ///
    /// #### Parameters
    ///
    /// - `min`: the value at the low end
    ///
    /// - `max`: the value at the high end
    ///
    /// #### Returns
    ///
    /// this series
    public HeatmapSeries setScaleRange(double min, double max) {
        fixedMin = Double.valueOf(min);
        fixedMax = Double.valueOf(max);
        repaintChart();
        return this;
    }

    private ColorScale scale() {
        return scale != null ? scale : ColorScale.sequential(getColor());
    }

    /// The range of values the colour scale spans: the fixed range when one is set, otherwise the
    /// smallest and largest value present. A matrix with no values at all answers `{0, 1}`.
    ///
    /// #### Returns
    ///
    /// a new `{min, max}` array
    public double[] getValueRange() {
        return extent();
    }

    /// The hit point id of a cell, `row * getColumns() + column`, which never aliases however wide
    /// the matrix is.
    ///
    /// #### Parameters
    ///
    /// - `row`: the row
    ///
    /// - `column`: the column
    ///
    /// #### Returns
    ///
    /// the id `hitTest` gives that cell's point
    public int cellId(int row, int column) {
        return row * Math.max(1, getColumns()) + column;
    }

    private double[] extent() {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double[] row : matrix) {
            for (double v : row) {
                if (!isGap(v)) {
                    min = Math.min(min, v);
                    max = Math.max(max, v);
                }
            }
        }
        if (min > max) {
            // no value at all: an empty but sane range rather than {MAX_VALUE, -MAX_VALUE}
            min = 0;
            max = 1;
        }
        if (fixedMin != null) {
            min = fixedMin.doubleValue();
        }
        if (fixedMax != null) {
            max = fixedMax.doubleValue();
        }
        return new double[]{min, max};
    }

    @Override
    public double[] getXBounds() {
        int c = getColumns();
        return c == 0 ? null : new double[]{-0.5, c - 0.5};
    }

    // the columns are the points along x: a category axis with no labels counts those at or
    // below maxX, so a matrix wider than its reach still gives bands for the columns within it
    @Override
    public double[] getXBoundsWithin(double maxX) {
        int c = getColumns();
        if (c == 0 || !(maxX >= 0)) { //NOPMD LogicInversion - NaN counts no column
            return null;
        }
        double last = Math.min(c - 1, Math.floor(maxX));
        return new double[]{-0.5, last + 0.5};
    }

    @Override
    public double[] getYBounds() {
        return matrix.length == 0 ? null : new double[]{-0.5, matrix.length - 0.5};
    }

    private double shown(ChartContext ctx, int r, int c) {
        double target = matrix[r][c];
        double t = ctx.getProgress();
        if (t >= 1 || from == null || r >= from.length || c >= from[r].length) {
            return target;
        }
        return lerp(from[r][c], target, t);
    }

    @Override
    protected void captureTransitionStart() {
        ChartContext ctx = getChart() == null ? null : getChart().getLastContext();
        double[][] f = new double[matrix.length][];
        for (int r = 0; r < matrix.length; r++) {
            f[r] = new double[matrix[r].length];
            for (int c = 0; c < f[r].length; c++) {
                f[r][c] = ctx == null ? matrix[r][c] : shown(ctx, r, c);
            }
        }
        from = f;
    }

    @Override
    protected void captureEntryStart() {
        double[] e = extent();
        from = new double[matrix.length][];
        for (int r = 0; r < matrix.length; r++) {
            from[r] = new double[matrix[r].length];
            for (int c = 0; c < from[r].length; c++) {
                from[r][c] = e[0];
            }
        }
    }

    private float[] cell(ChartContext ctx, int r, int c) {
        float x0 = ctx.screenX(this, c - 0.5, r - 0.5);
        float y0 = ctx.screenY(this, c - 0.5, r - 0.5);
        float x1 = ctx.screenX(this, c + 0.5, r + 0.5);
        float y1 = ctx.screenY(this, c + 0.5, r + 0.5);
        return new float[]{Math.min(x0, x1), Math.min(y0, y1), Math.abs(x1 - x0), Math.abs(y1 - y0)};
    }

    @Override
    public void paint(ChartContext ctx) {
        Graphics g = ctx.getGraphics();
        Font f = ctx.getFont();
        int fh = f.getHeight();
        double[] e = extent();
        double span = e[1] - e[0];
        ColorScale cs = scale();
        int gap = Math.max(1, ctx.mm(0.2f));
        int alpha = g.getAlpha();
        g.setAlpha((int) (alpha * getVisibilityFraction()));
        g.setFont(f);
        for (int r = 0; r < matrix.length; r++) {
            for (int c = 0; c < matrix[r].length; c++) {
                double v = shown(ctx, r, c);
                if (isGap(v)) {
                    continue;
                }
                float[] b = cell(ctx, r, c);
                int color = cs.colorAt(span > 0 ? (v - e[0]) / span : 0.5);
                g.setColor(color);
                int w = Math.max(1, Math.round(b[2]) - gap);
                int h = Math.max(1, Math.round(b[3]) - gap);
                int x = Math.round(b[0]);
                int y = Math.round(b[1]);
                g.fillRect(x, y, w, h);
                if (isShowValues() && ctx.getProgress() >= 1) {
                    String s = formatValue(ctx, matrix[r][c]);
                    int sw = f.stringWidth(s);
                    if (sw + ctx.mm(1) <= w && fh <= h) {
                        g.setColor(BarSeries.contrast(color));
                        g.drawString(s, x + (w - sw) / 2, y + (h - fh) / 2);
                    }
                }
            }
        }
        g.setAlpha(alpha);
    }

    @Override
    public void paintLegendSymbol(Graphics g, int x, int y, int size) {
        ColorScale cs = scale();
        int w = size * 3;
        for (int i = 0; i < w; i++) {
            g.setColor(cs.colorAt(i / (double) Math.max(1, w - 1)));
            g.fillRect(x + i, y, 1, size);
        }
    }

    /// Every cell is a point, numbered row by row as `cellId(row, column)`, so a selection or a
    /// tooltip survives a repaint and `Chart.select` can name a cell.
    @Override
    public int getPointCount() {
        return matrix.length * Math.max(1, getColumns());
    }

    @Override
    public ChartPoint getPoint(ChartContext ctx, int index) {
        int stride = Math.max(1, getColumns());
        int r = index < 0 ? -1 : index / stride;
        int c = index < 0 ? -1 : index % stride;
        if (ctx == null || r < 0 || r >= matrix.length || c >= matrix[r].length || isGap(matrix[r][c])) {
            return null;
        }
        float[] b = cell(ctx, r, c);
        return new ChartPoint(this, index, c, matrix[r][c], Math.round(b[0] + b[2] / 2) - ctx.getOriginX(),
                Math.round(b[1] + b[3] / 2) - ctx.getOriginY(), null);
    }

    @Override
    public ChartPoint hitTest(ChartContext ctx, int x, int y, int radius) {
        float ax = x + ctx.getOriginX();
        float ay = y + ctx.getOriginY();
        for (int r = 0; r < matrix.length; r++) {
            for (int c = 0; c < matrix[r].length; c++) {
                float[] b = cell(ctx, r, c);
                if (ax >= b[0] && ax <= b[0] + b[2] && ay >= b[1] && ay <= b[1] + b[3] && !isGap(matrix[r][c])) {
                    return getPoint(ctx, cellId(r, c));
                }
            }
        }
        return null;
    }

    @Override
    public String formatValue(ChartContext ctx, double value) {
        // the axes name rows and columns; cell values are plain numbers
        if (getValueFormatter() != null) {
            return getValueFormatter().format(value, 0);
        }
        return com.codename1.ui.chart.axis.Axis.formatNumber(value, value == Math.rint(value) ? 0 : 2);
    }

    @Override
    public String formatTooltip(ChartContext ctx, ChartPoint p) {
        int stride = Math.max(1, getColumns());
        int r = p.getIndex() / stride;
        int c = p.getIndex() % stride;
        String col = ctx.getXAxis().format(c, 0);
        String row = ctx.getValueAxis(this).format(r, 0);
        return row + ", " + col + ": " + formatValue(ctx, p.getY());
    }

    @Override
    public void collectTableRows(java.util.List<String[]> rows) {
        com.codename1.ui.chart.Chart ch = getChart();
        // the row labels come from the axis this series is drawn against, as in formatTooltip
        com.codename1.ui.chart.axis.Axis rowAxis = null;
        if (ch != null) {
            rowAxis = isSecondaryAxis() && !ch.isRotated() && ch.getSecondaryYAxis() != null ? ch.getSecondaryYAxis() : ch.getYAxis();
        }
        for (int r = 0; r < matrix.length; r++) {
            for (int c = 0; c < matrix[r].length; c++) {
                String item = rowAxis == null ? r + ", " + c : rowAxis.format(r, 0) + ", " + ch.getXAxis().format(c, 0);
                rows.add(new String[]{getName(), item, formatValue(null, matrix[r][c])});
            }
        }
    }
}
