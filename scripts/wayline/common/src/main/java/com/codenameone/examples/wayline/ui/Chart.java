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
package com.codenameone.examples.wayline.ui;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

/// A row of bars: rides a day, takings a day, requests by the hour.
///
/// Drawn here, in a few lines, because that is all a bar chart is and because
/// it then takes its colours from `theme.css` like everything else: the bars
/// are the `WlChart` style's colour, the highest of them `WlChartPeak`'s, and
/// the lines behind them `WlChartGrid`'s. Nothing here names a colour.
public final class Chart extends Component {
    private static final float HEIGHT_MM = 24f;

    private final double[] values;

    private Chart(double[] values) {
        this.values = values;
        setUIID("WlChart");
        setFocusable(false);
    }

    /// The chart, with what its two ends are written under it.
    ///
    /// @param name what a test finds the chart by
    /// @param from the caption under the first bar, already translated
    /// @param to the caption under the last
    public static Container of(String name, double[] values, String from, String to) {
        Chart chart = new Chart(values);
        chart.setName(name);
        Container box = new Container(new BorderLayout());
        box.add(BorderLayout.CENTER, chart);
        Container axis = new Container(new BorderLayout());
        axis.add(BorderLayout.WEST, Ui.plain(from, "WlChartAxis"));
        axis.add(BorderLayout.EAST, Ui.plain(to, "WlChartAxis"));
        box.add(BorderLayout.SOUTH, axis);
        return box;
    }

    /// How many bars there are, for a test.
    public int bars() {
        return values.length;
    }

    @Override
    protected Dimension calcPreferredSize() {
        Style style = getStyle();
        return new Dimension(CN.convertToPixels(40f),
                CN.convertToPixels(HEIGHT_MM) + style.getVerticalPadding());
    }

    @Override
    public void paint(Graphics g) {
        Style style = getStyle();
        int left = getX() + style.getPaddingLeftNoRTL();
        int top = getY() + style.getPaddingTop();
        int width = getWidth() - style.getHorizontalPadding();
        int height = getHeight() - style.getVerticalPadding();
        if (width <= 0 || height <= 0) {
            return;
        }
        UIManager styles = getUIManager();
        g.setColor(styles.getComponentStyle("WlChartGrid").getFgColor());
        // Three lines to read the heights against: the foot, half, the top.
        for (int line = 0; line <= 2; line++) {
            int y = top + (height - 1) * line / 2;
            g.drawLine(left, y, left + width, y);
        }
        int count = values.length;
        if (count == 0) {
            return;
        }
        double most = 0;
        int peak = -1;
        for (int iter = 0; iter < count; iter++) {
            if (values[iter] > most) {
                most = values[iter];
                peak = iter;
            }
        }
        if (most <= 0) {
            return;
        }
        // The space is shared out in whole pixels, so every bar is as wide as
        // every other and what is left over goes to the two ends.
        int slot = Math.max(1, width / count);
        int gap = Math.max(1, slot / 4);
        int bar = Math.max(1, slot - gap);
        int start = left + (width - slot * count + gap) / 2;
        int round = Math.min(bar, CN.convertToPixels(1.2f));
        int normal = style.getFgColor();
        int highest = styles.getComponentStyle("WlChartPeak").getFgColor();
        boolean rtl = isRTL();
        g.setAntiAliased(true);
        for (int iter = 0; iter < count; iter++) {
            int tall = (int) Math.round(values[iter] / most * (height - 2));
            if (values[iter] > 0 && tall < round) {
                tall = round;
            }
            if (tall <= 0) {
                continue;
            }
            // Time runs the way the language is read.
            int at = rtl ? count - 1 - iter : iter;
            g.setColor(iter == peak ? highest : normal);
            g.fillRoundRect(start + at * slot, top + height - tall, bar, tall, round, round);
        }
    }

    /// A share of something, as a bar part of the way along a track: the bar
    /// is the `WlMeter` style's colour and the track a faint wash of it.
    public static final class Meter extends Component {
        private static final int TRACK_ALPHA = 36;
        private final double share;

        /// @param share from 0 to 1
        public Meter(double share) {
            this.share = share < 0 ? 0 : share > 1 ? 1 : share;
            setUIID("WlMeter");
            setFocusable(false);
        }

        @Override
        protected Dimension calcPreferredSize() {
            Style style = getStyle();
            return new Dimension(CN.convertToPixels(20f),
                    CN.convertToPixels(1.6f) + style.getVerticalPadding());
        }

        @Override
        public void paint(Graphics g) {
            Style style = getStyle();
            int left = getX() + style.getPaddingLeftNoRTL();
            int top = getY() + style.getPaddingTop();
            int width = getWidth() - style.getHorizontalPadding();
            int height = getHeight() - style.getVerticalPadding();
            if (width <= 0 || height <= 0) {
                return;
            }
            g.setAntiAliased(true);
            // The track is the bar's own colour, faint: a background colour
            // would be painted square behind the rounded ends.
            int alpha = g.getAlpha();
            g.setColor(style.getFgColor());
            g.setAlpha(TRACK_ALPHA);
            g.fillRoundRect(left, top, width, height, height, height);
            g.setAlpha(alpha);
            int filled = (int) Math.round(width * share);
            if (filled > 0) {
                filled = Math.max(filled, height);
                g.setColor(style.getFgColor());
                g.fillRoundRect(isRTL() ? left + width - filled : left, top, filled, height,
                        height, height);
            }
        }
    }
}
