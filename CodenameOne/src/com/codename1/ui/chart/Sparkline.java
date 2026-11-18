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
package com.codename1.ui.chart;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.plaf.Style;

/// A word sized chart for list rows, table cells and dashboards: a line, area or bar trend
/// with no axes, legend or interaction, cheap enough to have hundreds on screen.
///
/// ```java
/// Sparkline s = new Sparkline(3, 5, 4, 8, 7, 9).setType(Sparkline.AREA);
/// row.add(BorderLayout.EAST, s);
/// ```
public class Sparkline extends Component {
    /// A line.
    public static final int LINE = 0;
    /// A line with the area under it filled.
    public static final int AREA = 1;
    /// Bars, negative values going down.
    public static final int BARS = 2;

    private double[] values;
    private int type = LINE;
    private Integer color;
    private boolean showLast = true;
    private boolean showExtremes;
    private ChartTheme theme;
    // the number of separate line runs the last line or area paint drew, for tests
    int lastSegments;

    /// Creates a sparkline.
    ///
    /// #### Parameters
    ///
    /// - `values`: the values in order
    public Sparkline(double... values) {
        setUIID("Sparkline");
        setValues(values);
    }

    /// Replaces the values.
    ///
    /// #### Parameters
    ///
    /// - `v`: the values in order
    ///
    /// #### Returns
    ///
    /// this sparkline
    public Sparkline setValues(double... v) {
        values = new double[v == null ? 0 : v.length];
        if (v != null) {
            System.arraycopy(v, 0, values, 0, v.length);
        }
        repaint();
        return this;
    }

    /// Chooses `LINE`, `AREA` or `BARS`.
    ///
    /// #### Parameters
    ///
    /// - `type`: the drawing type
    ///
    /// #### Returns
    ///
    /// this sparkline
    public Sparkline setType(int type) {
        this.type = type;
        repaint();
        return this;
    }

    /// Fixes the colour instead of the style's foreground.
    ///
    /// #### Parameters
    ///
    /// - `rgb`: the colour
    ///
    /// #### Returns
    ///
    /// this sparkline
    public Sparkline setColor(int rgb) {
        color = Integer.valueOf(rgb);
        repaint();
        return this;
    }

    /// Marks the last value with a dot.
    ///
    /// #### Parameters
    ///
    /// - `b`: true to mark it
    ///
    /// #### Returns
    ///
    /// this sparkline
    public Sparkline setShowLast(boolean b) {
        showLast = b;
        repaint();
        return this;
    }

    /// Marks the highest and lowest values with dots in the positive and negative colours.
    ///
    /// #### Parameters
    ///
    /// - `b`: true to mark them
    ///
    /// #### Returns
    ///
    /// this sparkline
    public Sparkline setShowExtremes(boolean b) {
        showExtremes = b;
        repaint();
        return this;
    }

    private ChartTheme theme() {
        if (theme == null) {
            theme = ChartTheme.fromStyle(getStyle());
        }
        return theme;
    }

    @Override
    protected Dimension calcPreferredSize() {
        Style s = getStyle();
        return new Dimension(CN.convertToPixels(18) + s.getHorizontalPadding(), CN.convertToPixels(5) + s.getVerticalPadding());
    }

    @Override
    public void paint(Graphics g) {
        int n = values.length;
        if (n == 0) {
            return;
        }
        Style s = getStyle();
        int dot = Math.max(3, CN.convertToPixels(1));
        int x = getX() + s.getPaddingLeftNoRTL() + dot / 2;
        int y = getY() + s.getPaddingTop() + dot / 2;
        int w = getWidth() - s.getHorizontalPadding() - dot;
        int h = getHeight() - s.getVerticalPadding() - dot;
        if (w <= 1 || h <= 1) {
            return;
        }
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        int iMin = -1;
        int iMax = -1;
        for (int i = 0; i < n; i++) {
            double v = values[i];
            if (v != v) {
                continue;
            }
            if (v < min) {
                min = v;
                iMin = i;
            }
            if (v > max) {
                max = v;
                iMax = i;
            }
        }
        if (iMin < 0) {
            return;
        }
        if (type == BARS) {
            min = Math.min(0, min);
            max = Math.max(0, max);
        }
        double span = max - min;
        if (span == 0) {
            // a flat series draws through the middle
            span = 1;
            max += 0.5;
        }
        int c = color != null ? color.intValue() : theme().getSeriesColor(0);
        boolean aa = g.isAntiAliased();
        g.setAntiAliased(true);
        g.setColor(c);
        int alpha = g.getAlpha();
        if (type == BARS) {
            float bw = w / (float) n;
            int zero = y + (int) Math.round((max - 0) / span * h);
            for (int i = 0; i < n; i++) {
                double v = values[i];
                if (v != v) {
                    continue;
                }
                int vy = y + (int) Math.round((max - v) / span * h);
                int bx = x + Math.round(i * bw + bw * 0.1f);
                int bwi = Math.max(1, Math.round(bw * 0.8f));
                g.setColor(v < 0 ? theme().getNegativeColor() : c);
                g.fillRect(bx, Math.min(vy, zero), bwi, Math.max(1, Math.abs(zero - vy)));
            }
        } else {
            GeneralPath line = new GeneralPath();
            GeneralPath area = new GeneralPath();
            boolean started = false;
            float firstX = 0;
            float lastX = 0;
            int segments = 0;
            for (int i = 0; i < n; i++) {
                double v = values[i];
                if (v != v) {
                    // a gap breaks the line and its area, as in LineSeries; never bridge across it
                    if (started) {
                        area.lineTo(lastX, y + h);
                        area.lineTo(firstX, y + h);
                        area.closePath();
                        started = false;
                    }
                    continue;
                }
                float px = x + (n == 1 ? w / 2f : i * w / (float) (n - 1));
                float py = (float) (y + (max - v) / span * h);
                if (!started) {
                    line.moveTo(px, py);
                    area.moveTo(px, y + h);
                    area.lineTo(px, py);
                    firstX = px;
                    started = true;
                    segments++;
                } else {
                    line.lineTo(px, py);
                    area.lineTo(px, py);
                }
                lastX = px;
            }
            lastSegments = segments;
            if (type == AREA) {
                if (started) {
                    area.lineTo(lastX, y + h);
                    area.lineTo(firstX, y + h);
                    area.closePath();
                }
                g.setAlpha(alpha * 50 / 255);
                g.fillShape(area);
                g.setAlpha(alpha);
            }
            g.drawShape(line, new Stroke(Math.max(1, CN.convertToPixels(0.35f)), Stroke.CAP_ROUND, Stroke.JOIN_ROUND, 1));
            if (showExtremes) {
                dot(g, x, y, w, h, n, iMax, values[iMax], max, span, theme().getPositiveColor(), dot);
                dot(g, x, y, w, h, n, iMin, values[iMin], max, span, theme().getNegativeColor(), dot);
            }
            if (showLast) {
                int last = n - 1;
                while (last >= 0 && values[last] != values[last]) {
                    last--;
                }
                if (last >= 0) {
                    dot(g, x, y, w, h, n, last, values[last], max, span, c, dot);
                }
            }
        }
        g.setAntiAliased(aa);
    }

    private static void dot(Graphics g, int x, int y, int w, int h, int n, int i, double v, double max, double span, int color, int d) {
        float px = x + (n == 1 ? w / 2f : i * w / (float) (n - 1));
        float py = (float) (y + (max - v) / span * h);
        g.setColor(color);
        g.fillArc(Math.round(px - d / 2f), Math.round(py - d / 2f), d, d, 0, 360);
    }
}
