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
package com.codenameone.examples.pulse.owner;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codenameone.examples.pulse.api.WeekPointDto;
import com.codenameone.examples.pulse.ui.Lang;
import java.util.List;

/// The owner's insights, drawn: the busy-hours heatmap, membership over
/// twelve weeks, the plan-mix donut and the coloured bars and swatches beside
/// them. Each takes its colours from `theme.css` through a style, so dark
/// mode is the theme's business; only the series that have a colour of their
/// own -- a class type, a plan -- are handed one.
final class Graphs {
    /// The plans' colours, the same family the avatars are drawn from.
    static final int[] PALETTE = {0xdc3418, 0x7c4dff, 0x1f9d6b, 0x2f6fd6, 0xe08a00, 0xc2185b};

    private Graphs() {
    }

    /// `#e4572e` or `e4572e` as a colour, or `fallback` when it is neither.
    static int color(String hex, int fallback) {
        if (hex == null) {
            return fallback;
        }
        String digits = hex.startsWith("#") ? hex.substring(1) : hex;
        if (digits.length() != 6) {
            return fallback;
        }
        int value = 0;
        for (int iter = 0; iter < 6; iter++) {
            int digit = Character.digit(digits.charAt(iter), 16);
            if (digit < 0) {
                return fallback;
            }
            value = value * 16 + digit;
        }
        return value;
    }

    private static int fg(String uiid) {
        return UIManager.getInstance().getComponentStyle(uiid).getFgColor();
    }

    /// Check-ins by weekday and hour: a row a day, Monday first, a cell an
    /// hour, each the brand's colour as strong as the hour is busy.
    static final class Heatmap extends Component {
        private static final String[] DAYS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        private static final float CELL_MM = 4.2f;
        private final int firstHour;
        private final int hours;
        private final int[] counts;
        private int most;

        Heatmap(int firstHour, int lastHour, List<Integer> busy) {
            this.firstHour = firstHour;
            this.hours = Math.max(1, lastHour - firstHour);
            counts = new int[7 * hours];
            int peak = -1;
            for (int iter = 0; busy != null && iter < busy.size() && iter < counts.length; iter++) {
                Integer value = busy.get(iter);
                counts[iter] = value == null ? 0 : value.intValue();
                if (counts[iter] > most) {
                    most = counts[iter];
                    peak = iter;
                }
            }
            setUIID("SvHeatmap");
            setName("busyHours");
            setFocusable(false);
            setAccessibilityText(peak < 0 ? Lang.tr("No visits yet")
                    : Lang.tr("Busiest: {0} at {1}:00", Lang.tr(DAYS[peak / hours]),
                    Integer.valueOf(firstHour + peak % hours)));
        }

        @Override
        protected Dimension calcPreferredSize() {
            Style style = getStyle();
            return new Dimension(CN.convertToPixels(40f), 7 * CN.convertToPixels(CELL_MM)
                    + style.getFont().getHeight() + style.getVerticalPadding());
        }

        @Override
        public void paint(Graphics g) {
            Style style = getStyle();
            Font font = style.getFont();
            int left = getX() + style.getPaddingLeftNoRTL();
            int top = getY() + style.getPaddingTop();
            int width = getWidth() - style.getHorizontalPadding();
            int label = 0;
            for (int day = 0; day < 7; day++) {
                label = Math.max(label, font.stringWidth(Lang.tr(DAYS[day])));
            }
            label += CN.convertToPixels(1.4f);
            int cellW = Math.max(1, (width - label) / hours);
            int cellH = CN.convertToPixels(CELL_MM);
            int gap = Math.max(1, Math.min(cellW, cellH) / 8);
            boolean rtl = isRTL();
            int grid = rtl ? left : left + label;
            int labels = rtl ? left + width - label + CN.convertToPixels(1.4f) : left;
            int empty = fg("SvChartGrid");
            int text = fg("SvChartAxis");
            int brand = style.getFgColor();
            g.setAntiAliased(true);
            g.setFont(font);
            int alpha = g.getAlpha();
            for (int day = 0; day < 7; day++) {
                int y = top + day * cellH;
                g.setColor(text);
                g.drawString(Lang.tr(DAYS[day]), labels, y + (cellH - font.getHeight()) / 2);
                for (int hour = 0; hour < hours; hour++) {
                    int at = rtl ? hours - 1 - hour : hour;
                    int x = grid + at * cellW;
                    int value = counts[day * hours + hour];
                    g.setColor(empty);
                    g.fillRoundRect(x, y, cellW - gap, cellH - gap, gap * 2, gap * 2);
                    if (value > 0 && most > 0) {
                        // Never fainter than a fifth, so a quiet hour that had
                        // someone still reads as not empty.
                        g.setColor(brand);
                        g.setAlpha(51 + 204 * value / most);
                        g.fillRoundRect(x, y, cellW - gap, cellH - gap, gap * 2, gap * 2);
                        g.setAlpha(alpha);
                    }
                }
            }
            g.setColor(text);
            int foot = top + 7 * cellH;
            for (int hour = 0; hour < hours; hour += 3) {
                int at = rtl ? hours - 1 - hour : hour;
                g.drawString(String.valueOf(firstHour + hour), grid + at * cellW, foot);
            }
        }
    }

    /// Active members a week as a line, and under it each week's joiners as a
    /// bar up and leavers as a bar down from a middle line.
    static final class Membership extends Component {
        private final int[] active;
        private final int[] joined;
        private final int[] left;

        Membership(List<WeekPointDto> weeks) {
            int count = weeks == null ? 0 : weeks.size();
            active = new int[count];
            joined = new int[count];
            left = new int[count];
            for (int iter = 0; iter < count; iter++) {
                WeekPointDto week = weeks.get(iter);
                active[iter] = week.active();
                joined[iter] = week.joined();
                left[iter] = week.left();
            }
            setUIID("SvChart");
            setName("membershipChart");
            setFocusable(false);
            setAccessibilityText(count == 0 ? Lang.tr("No members yet")
                    : Lang.tr("{0} active members, {1} twelve weeks ago",
                    Integer.valueOf(active[count - 1]), Integer.valueOf(active[0])));
        }

        @Override
        protected Dimension calcPreferredSize() {
            return new Dimension(CN.convertToPixels(40f), CN.convertToPixels(38f)
                    + getStyle().getVerticalPadding());
        }

        @Override
        public void paint(Graphics g) {
            Style style = getStyle();
            int x0 = getX() + style.getPaddingLeftNoRTL();
            int top = getY() + style.getPaddingTop();
            int width = getWidth() - style.getHorizontalPadding();
            int height = getHeight() - style.getVerticalPadding();
            int count = active.length;
            if (count == 0 || width <= 0 || height <= 0) {
                return;
            }
            int lineH = height * 3 / 5;
            int barTop = top + lineH + CN.convertToPixels(2f);
            int barH = top + height - barTop;
            int middle = barTop + barH / 2;
            int slot = Math.max(1, width / count);
            int start = x0 + (width - slot * count) / 2;
            boolean rtl = isRTL();
            g.setAntiAliased(true);
            g.setColor(fg("SvChartGrid"));
            g.drawLine(x0, top + lineH, x0 + width, top + lineH);
            g.drawLine(x0, middle, x0 + width, middle);
            int hi = 1;
            int lo = Integer.MAX_VALUE;
            int churn = 1;
            for (int iter = 0; iter < count; iter++) {
                hi = Math.max(hi, active[iter]);
                lo = Math.min(lo, active[iter]);
                churn = Math.max(churn, Math.max(joined[iter], left[iter]));
            }
            // The line starts from a little under the lowest week, so a studio
            // that grew by ten members does not read as flat.
            int floor = Math.max(0, lo - Math.max(1, (hi - lo) / 3));
            int span = Math.max(1, hi - floor);
            int bar = Math.max(1, slot / 2);
            int dot = CN.convertToPixels(1.2f);
            int px = 0;
            int py = 0;
            for (int iter = 0; iter < count; iter++) {
                int at = rtl ? count - 1 - iter : iter;
                int cx = start + at * slot + slot / 2;
                int up = (barH / 2 - 1) * joined[iter] / churn;
                int down = (barH / 2 - 1) * left[iter] / churn;
                g.setColor(fg("SvLegendJoined"));
                if (up > 0) {
                    g.fillRect(cx - bar / 2, middle - up, bar, up);
                }
                g.setColor(fg("SvLegendLeft"));
                if (down > 0) {
                    g.fillRect(cx - bar / 2, middle + 1, bar, down);
                }
                int cy = top + lineH - 1 - (lineH - dot) * (active[iter] - floor) / span;
                g.setColor(style.getFgColor());
                if (iter > 0) {
                    g.drawLine(px, py, cx, cy);
                    g.drawLine(px, py + 1, cx, cy + 1);
                }
                g.fillArc(cx - dot / 2, cy - dot / 2, dot, dot, 0, 360);
                px = cx;
                py = cy;
            }
        }
    }

    /// The plans as slices of a ring, the members on each the slice's size,
    /// and the total in the middle.
    static final class Donut extends Component {
        private final int[] values;
        private final int total;

        Donut(int[] values, String spoken) {
            this.values = values;
            int sum = 0;
            for (int iter = 0; iter < values.length; iter++) {
                sum += Math.max(0, values[iter]);
            }
            total = sum;
            setUIID("SvDonut");
            setName("planDonut");
            setFocusable(false);
            setAccessibilityText(spoken);
        }

        @Override
        protected Dimension calcPreferredSize() {
            int size = CN.convertToPixels(30f);
            return new Dimension(size + getStyle().getHorizontalPadding(),
                    size + getStyle().getVerticalPadding());
        }

        @Override
        public void paint(Graphics g) {
            Style style = getStyle();
            int width = getWidth() - style.getHorizontalPadding();
            int height = getHeight() - style.getVerticalPadding();
            int size = Math.min(width, height);
            if (size <= 0) {
                return;
            }
            int x = getX() + style.getPaddingLeftNoRTL() + (width - size) / 2;
            int y = getY() + style.getPaddingTop() + (height - size) / 2;
            g.setAntiAliased(true);
            if (total <= 0) {
                g.setColor(fg("SvChartGrid"));
                g.fillArc(x, y, size, size, 0, 360);
            } else {
                int angle = 90;
                for (int iter = 0; iter < values.length; iter++) {
                    int sweep = iter == values.length - 1 ? 450 - angle
                            : Math.round(360f * Math.max(0, values[iter]) / total);
                    g.setColor(PALETTE[iter % PALETTE.length]);
                    g.fillArc(x, y, size, size, angle, sweep);
                    angle += sweep;
                }
            }
            int hole = size * 3 / 5;
            g.setColor(UIManager.getInstance().getComponentStyle("SvPage").getBgColor());
            g.fillArc(x + (size - hole) / 2, y + (size - hole) / 2, hole, hole, 0, 360);
            Font font = style.getFont();
            String text = String.valueOf(total);
            g.setFont(font);
            g.setColor(style.getFgColor());
            g.drawString(text, x + (size - font.stringWidth(text)) / 2,
                    y + (size - font.getHeight()) / 2);
        }
    }

    /// A dot of one colour: what a legend line is the key of.
    static final class Swatch extends Component {
        private final int color;

        Swatch(int color) {
            this.color = color;
            setUIID("Container");
            setFocusable(false);
        }

        @Override
        protected Dimension calcPreferredSize() {
            int size = CN.convertToPixels(3f);
            return new Dimension(size + CN.convertToPixels(2f), size);
        }

        @Override
        public void paint(Graphics g) {
            int size = getHeight();
            g.setAntiAliased(true);
            g.setColor(color);
            g.fillArc(isRTL() ? getX() + getWidth() - size : getX(), getY(), size, size, 0, 360);
        }
    }

    /// A rounded bar filled to `share` of its track, in a colour of its own.
    static final class Bar extends Component {
        private final double share;
        private final int color;

        Bar(double share, int color) {
            this.share = share < 0 ? 0 : share > 1 ? 1 : share;
            this.color = color;
            setUIID("SvRankBar");
            setFocusable(false);
        }

        @Override
        protected Dimension calcPreferredSize() {
            return new Dimension(CN.convertToPixels(30),
                    CN.convertToPixels(1.6f) + getStyle().getVerticalPadding());
        }

        @Override
        public void paint(Graphics g) {
            Style style = getStyle();
            int x = getX() + style.getPaddingLeftNoRTL();
            int y = getY() + style.getPaddingTop();
            int w = getWidth() - style.getHorizontalPadding();
            int h = getHeight() - style.getVerticalPadding();
            if (w <= 0 || h <= 0) {
                return;
            }
            g.setAntiAliased(true);
            g.setColor(style.getFgColor());
            g.fillRoundRect(x, y, w, h, h, h);
            int filled = Math.max(h, (int) (w * share));
            g.setColor(color);
            g.fillRoundRect(isRTL() ? x + w - filled : x, y, filled, h, h, h);
        }
    }
}
