/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.slate.ui;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

/// A row of bars: visits a week, takings a day, check-ins by the hour.
///
/// Drawn here, in a few lines, because that is all a bar chart is and because
/// it then takes its colours from `theme.css` like everything else: the bars
/// are the `SvChart` style's colour, the highest of them `SvChartPeak`'s, and
/// the lines behind them `SvChartGrid`'s. Nothing here names a colour.
public final class Chart extends Component {
    private static final float HEIGHT_MM = 24f;

    private final double[] values;

    private Chart(double[] values) {
        this.values = values;
        setUIID("SvChart");
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
        axis.add(BorderLayout.WEST, Ui.plain(from, "SvChartAxis"));
        axis.add(BorderLayout.EAST, Ui.plain(to, "SvChartAxis"));
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

    // Whole numbers as they are, anything else to one decimal.
    private static String number(double value) {
        long whole = Math.round(value);
        return Math.abs(value - whole) < 0.05 ? String.valueOf(whole) : Ui.tenths(value);
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
        int count = values.length;
        double most = 0;
        int peak = -1;
        for (int iter = 0; iter < count; iter++) {
            if (values[iter] >= most && values[iter] > 0) {
                most = values[iter];
                peak = iter;
            }
        }
        // The scale: the top line's value and half of it, in a gutter of their
        // own, so a bar's height is a number and not just taller or shorter.
        if (most > 0) {
            Style axis = styles.getComponentStyle("SvChartAxis");
            Font font = axis.getFont();
            String high = number(most);
            String half = number(most / 2);
            int pad = CN.convertToPixels(1f);
            int gutter = font.stringWidth(high) + pad;
            g.setFont(font);
            g.setColor(axis.getFgColor());
            int x = isRTL() ? left + width - gutter + pad : left;
            g.drawString(high, x, top);
            g.drawString(half, x, top + (height - 1) / 2 - font.getHeight() / 2);
            width -= gutter;
            if (!isRTL()) {
                left += gutter;
            }
        }
        g.setColor(styles.getComponentStyle("SvChartGrid").getFgColor());
        // Three lines to read the heights against: the foot, half, the top.
        for (int line = 0; line <= 2; line++) {
            int y = top + (height - 1) * line / 2;
            g.drawLine(left, y, left + width, y);
        }
        if (count == 0 || most <= 0) {
            return;
        }
        // The peak is picked out only when it is one day; four days tied at the
        // top would make the one colored look chosen at random.
        for (int iter = 0; iter < count; iter++) {
            if (iter != peak && values[iter] == most) {
                peak = -1;
                break;
            }
        }
        // The space is shared out in whole pixels, so every bar is as wide as
        // every other and what is left over goes to the two ends.
        int slot = Math.max(1, width / count);
        int gap = Math.max(1, slot / 4);
        int bar = Math.max(1, slot - gap);
        int start = left + (width - slot * count + gap) / 2;
        int round = Math.min(bar, CN.convertToPixels(1.2f));
        int normal = style.getFgColor();
        int highest = styles.getComponentStyle("SvChartPeak").getFgColor();
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
    /// is the `SvMeter` style's colour and the track a faint wash of it.
    public static final class Meter extends Component {
        private static final int TRACK_ALPHA = 36;
        private final double share;

        /// @param share from 0 to 1
        public Meter(double share) {
            this.share = share < 0 ? 0 : share > 1 ? 1 : share;
            setUIID("SvMeter");
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
