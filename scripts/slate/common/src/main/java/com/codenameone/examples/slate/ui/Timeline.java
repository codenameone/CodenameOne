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
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

import java.util.ArrayList;
import java.util.List;

// tag::slate-timeline[]
/// Time running down the page, one lane per day or per person: the hours a
/// provider works as a band, bookings as blocks as long as they last, busy
/// time from a calendar hatched, and a line at the current time.
///
/// It is the provider's day (one lane), their week (seven) and the owner's
/// board (a lane per provider): the same picture, so a gap reads the same
/// everywhere. Colours come from `theme.css` -- `SvTimeline` for the grid and
/// the closed hours, `SvTimelineShift`, `SvTimelineBusy`, `SvTimelineBooking`
/// (and `SvTimelineDone` for one that is over), `SvTimelineNow` -- so dark
/// mode is a stylesheet change, as it is for every other screen.
public final class Timeline extends Component {
    /// A working band: drawn under everything else.
    public static final int SHIFT = 0;
    /// Busy in a calendar, or time off.
    public static final int BUSY = 1;
    /// A booking.
    public static final int BOOKING = 2;
    /// A booking that is over and was kept.
    public static final int DONE = 3;
    /// A booking the client did not come to.
    public static final int NO_SHOW = 4;

    private static final float HOUR_MM = 13f;
    private static final float GUTTER_MM = 10f;
    private static final float HEADER_MM = 9f;
    private static final float MIN_LANE_MM = 22f;
    private static final long HOUR = 3600000L;

    /// Something to draw. `start` and `end` are epoch milliseconds.
    public static final class Block {
        final int kind;
        final long start;
        final long end;
        final String title;
        final String detail;
        final Runnable tap;

        public Block(int kind, long start, long end, String title, String detail, Runnable tap) {
            this.kind = kind;
            this.start = start;
            this.end = end;
            this.title = title == null ? "" : title;
            this.detail = detail == null ? "" : detail;
            this.tap = tap;
        }
    }

    /// One column: a day or a person, starting at `dayStart` (midnight).
    public static final class Lane {
        final String title;
        final long dayStart;
        final List<Block> blocks = new ArrayList<Block>();

        public Lane(String title, long dayStart) {
            this.title = title == null ? "" : title;
            this.dayStart = dayStart;
        }

        public Lane add(Block b) {
            blocks.add(b);
            return this;
        }

        /// How many bookings the lane shows, for a test.
        public int bookings() {
            int n = 0;
            for (int iter = 0; iter < blocks.size(); iter++) {
                int kind = blocks.get(iter).kind;
                if (booking(kind)) {
                    n++;
                }
            }
            return n;
        }
    }

    private final List<Lane> lanes;
    private final boolean headers;
    private int firstHour = 24;
    private int lastHour = 0;

    /// The kind of block an appointment in `state` draws as.
    public static int kindOf(String state) {
        return "COMPLETED".equals(state) ? DONE : "NO_SHOW".equals(state) ? NO_SHOW : BOOKING;
    }

    static boolean booking(int kind) {
        return kind == BOOKING || kind == DONE || kind == NO_SHOW;
    }

    /// The key to the colors, one row: what a booking, a finished one, a no-show
    /// and busy time look like. Put it under the timeline it explains.
    public static Container legend() {
        Container row = new Container(new FlowLayout());
        row.setUIID("SvLegend");
        String[][] keys = {{"SvTimelineBooking", "Booked"}, {"SvTimelineDone", "Done"},
            {"SvTimelineNoShow", "No-show"}, {"SvTimelineBusy", "Busy"}};
        for (int iter = 0; iter < keys.length; iter++) {
            Label swatch = new Label(" ", keys[iter][0]);
            swatch.getAllStyles().setPadding(0, 0, 0, 0);
            swatch.getAllStyles().setMargin(0, 0, iter == 0 ? 0 : CN.convertToPixels(3f),
                    CN.convertToPixels(1f));
            swatch.setPreferredW(CN.convertToPixels(3f));
            swatch.setPreferredH(CN.convertToPixels(3f));
            row.add(swatch);
            row.add(new Label(Lang.tr(keys[iter][1]), "SvCaption"));
        }
        return row;
    }

    /// @param headers whether each lane's title is drawn above it
    public Timeline(List<Lane> lanes, boolean headers) {
        this.lanes = lanes;
        this.headers = headers;
        setUIID("SvTimeline");
        setFocusable(false);
        fitHours();
    }

    public List<Lane> lanes() {
        return lanes;
    }

    /// The hours shown: exactly the hours something is in -- shifts included,
    /// so a day opens on working time rather than on the closed early morning
    /// -- and at least six of them; 09 to 18 when there is nothing at all.
    private void fitHours() {
        for (int l = 0; l < lanes.size(); l++) {
            Lane lane = lanes.get(l);
            for (int iter = 0; iter < lane.blocks.size(); iter++) {
                Block b = lane.blocks.get(iter);
                long from = Math.max(0, b.start - lane.dayStart);
                long to = Math.min(24 * HOUR, b.end - lane.dayStart);
                if (to <= 0 || from >= 24 * HOUR) {
                    continue;
                }
                firstHour = Math.min(firstHour, (int) (from / HOUR));
                lastHour = Math.max(lastHour, (int) ((to + HOUR - 1) / HOUR));
            }
        }
        if (lastHour <= firstHour) {
            firstHour = 9;
            lastHour = 18;
        }
        while (lastHour - firstHour < 6) {
            if (lastHour < 24) {
                lastHour++;
            } else {
                firstHour--;
            }
        }
    }

    private int hourPx() {
        return CN.convertToPixels(HOUR_MM);
    }

    private int gutter() {
        return CN.convertToPixels(GUTTER_MM);
    }

    private int header() {
        return headers ? CN.convertToPixels(HEADER_MM) : CN.convertToPixels(2f);
    }

    @Override
    protected Dimension calcPreferredSize() {
        int lanesWide = Math.max(1, lanes.size()) * CN.convertToPixels(MIN_LANE_MM);
        return new Dimension(gutter() + lanesWide,
                header() + (lastHour - firstHour) * hourPx() + CN.convertToPixels(2f));
    }

    private int laneWidth() {
        return Math.max(1, (getWidth() - gutter()) / Math.max(1, lanes.size()));
    }

    private int yOf(Lane lane, long time) {
        double hours = (time - lane.dayStart) / (double) HOUR - firstHour;
        hours = Math.max(0, Math.min(lastHour - firstHour, hours));
        return getY() + header() + (int) Math.round(hours * hourPx());
    }

    @Override
    public void paint(Graphics g) {
        UIManager styles = getUIManager();
        Style grid = getStyle();
        Font small = styles.getComponentStyle("SvChartAxis").getFont();
        int x0 = getX() + gutter();
        int top = getY() + header();
        int bottom = top + (lastHour - firstHour) * hourPx();
        int lane = laneWidth();
        int pad = CN.convertToPixels(0.6f);
        g.setAntiAliased(true);
        g.setAntiAliasedText(true);

        // Closed hours are the grid's own background; a shift paints over it.
        g.setColor(grid.getBgColor());
        g.fillRect(x0, top, lane * lanes.size(), bottom - top);
        for (int l = 0; l < lanes.size(); l++) {
            Lane ln = lanes.get(l);
            int lx = x0 + l * lane;
            fill(g, ln, SHIFT, lx, lane, 0, styles.getComponentStyle("SvTimelineShift").getBgColor());
        }

        // Hour lines and labels.
        g.setFont(small);
        for (int h = firstHour; h <= lastHour; h++) {
            int y = top + (h - firstHour) * hourPx();
            g.setColor(grid.getFgColor());
            g.drawLine(x0, y, x0 + lane * lanes.size(), y);
            if (h < lastHour) {
                g.setColor(styles.getComponentStyle("SvChartAxis").getFgColor());
                String text = (h < 10 ? "0" : "") + h + ":00";
                g.drawString(text, getX() + gutter() - small.stringWidth(text) - pad,
                        y - small.getHeight() / 2 < getY() ? y : y - small.getHeight() / 2);
            }
        }
        for (int l = 0; l <= lanes.size(); l++) {
            g.setColor(grid.getFgColor());
            g.drawLine(x0 + l * lane, top, x0 + l * lane, bottom);
        }

        Font titleFont = styles.getComponentStyle("SvTimelineBooking").getFont();
        for (int l = 0; l < lanes.size(); l++) {
            Lane ln = lanes.get(l);
            int lx = x0 + l * lane;
            if (headers) {
                header(g, ln, lx, lane, pad, small, styles);
            }
            Style busy = styles.getComponentStyle("SvTimelineBusy");
            fill(g, ln, BUSY, lx, lane, pad, busy.getBgColor());
            for (int iter = 0; iter < ln.blocks.size(); iter++) {
                Block b = ln.blocks.get(iter);
                if (!booking(b.kind)) {
                    if (b.kind == BUSY) {
                        hatch(g, ln, b, lx + pad, lane - 2 * pad, busy.getFgColor(), pad);
                        label(g, ln, b, lx, lane, pad, busy.getFgColor(), small, small);
                    }
                    continue;
                }
                Style s = styles.getComponentStyle(b.kind == DONE ? "SvTimelineDone"
                        : b.kind == NO_SHOW ? "SvTimelineNoShow" : "SvTimelineBooking");
                int y1 = yOf(ln, b.start);
                int y2 = Math.max(y1 + small.getHeight(), yOf(ln, b.end));
                g.setColor(s.getBgColor());
                g.fillRoundRect(lx + pad, y1 + 1, lane - 2 * pad, y2 - y1 - 2, 3 * pad, 3 * pad);
                g.setColor(s.getFgColor());
                g.fillRect(lx + pad, y1 + 1, Math.max(2, pad), y2 - y1 - 2);
                label(g, ln, b, lx + pad, lane - pad, pad, s.getFgColor(), titleFont, small);
            }
        }

        // Now, where it falls inside a lane's day.
        long now = System.currentTimeMillis();
        g.setColor(styles.getComponentStyle("SvTimelineNow").getFgColor());
        for (int l = 0; l < lanes.size(); l++) {
            Lane ln = lanes.get(l);
            long into = now - ln.dayStart;
            if (into >= firstHour * HOUR && into < lastHour * HOUR) {
                int y = yOf(ln, now);
                int lx = x0 + l * lane;
                g.fillRect(lx, y - 1, lane, Math.max(2, pad / 2));
                g.fillArc(lx - pad, y - pad, 2 * pad, 2 * pad, 0, 360);
            }
        }
    }

    // A lane's title above it. When the lanes are days, today's is in the
    // now-line's color, so the week says which day it is. Titles too wide for a
    // phone's week lane ("Thu 5") all go on two lines, centred, rather than
    // being cut to "Th..." -- all of them, or the row reads ragged.
    private void header(Graphics g, Lane ln, int lx, int lane, int pad, Font font,
                        UIManager styles) {
        long now = System.currentTimeMillis();
        boolean today = days() && now >= ln.dayStart && now < ln.dayStart + 24 * HOUR;
        g.setColor(styles.getComponentStyle(today ? "SvTimelineNow" : "SvCaption").getFgColor());
        g.setFont(font);
        int room = lane - 2 * pad;
        int space = ln.title.indexOf(' ');
        if (!wraps(font, room) || space < 0) {
            String text = clip(font, ln.title, room);
            g.drawString(text, lx + (lane - font.stringWidth(text)) / 2,
                    getY() + (header() - font.getHeight()) / 2);
            return;
        }
        String first = clip(font, ln.title.substring(0, space), room);
        String second = clip(font, ln.title.substring(space + 1), room);
        int y = getY() + (header() - 2 * font.getHeight()) / 2;
        g.drawString(first, lx + (lane - font.stringWidth(first)) / 2, y);
        g.drawString(second, lx + (lane - font.stringWidth(second)) / 2, y + font.getHeight());
    }

    // Whether the lanes are days rather than people on one day.
    private boolean days() {
        for (int iter = 1; iter < lanes.size(); iter++) {
            if (lanes.get(iter).dayStart != lanes.get(0).dayStart) {
                return true;
            }
        }
        return false;
    }

    private boolean wraps(Font font, int room) {
        for (int iter = 0; iter < lanes.size(); iter++) {
            if (font.stringWidth(lanes.get(iter).title) > room) {
                return true;
            }
        }
        return false;
    }

    private void fill(Graphics g, Lane ln, int kind, int lx, int lane, int pad, int color) {
        g.setColor(color);
        for (int iter = 0; iter < ln.blocks.size(); iter++) {
            Block b = ln.blocks.get(iter);
            if (b.kind == kind) {
                int y1 = yOf(ln, b.start);
                int y2 = yOf(ln, b.end);
                if (y2 > y1) {
                    g.fillRect(lx + pad, y1, lane - 2 * pad, y2 - y1);
                }
            }
        }
    }

    // A block's words: the title, then the detail's parts (time, client,
    // state) a line each while there is height for them, the rest joined on
    // the last line. A block too narrow for its title shows its start time,
    // so a phone's week still reads at a glance.
    private void label(Graphics g, Lane ln, Block b, int lx, int width, int pad, int color,
                       Font titleFont, Font detailFont) {
        int y1 = yOf(ln, b.start);
        int y2 = yOf(ln, b.end);
        int room = width - 3 * pad;
        if (room <= 0 || y2 - y1 < detailFont.getHeight()) {
            return;
        }
        g.setColor(color);
        String title = clip(titleFont, b.title, room);
        if (title.length() == 0 && b.title.length() > 0) {
            long into = (b.start - ln.dayStart) / 60000L;
            String clock = (into / 60) + ":" + (into % 60 < 10 ? "0" : "") + (into % 60);
            if (detailFont.stringWidth(clock) > room) {
                clock = String.valueOf(into / 60);
            }
            if (detailFont.stringWidth(clock) <= room + 2 * pad) {
                g.setFont(detailFont);
                g.drawString(clock, lx + pad + Math.max(0, (width - pad - detailFont.stringWidth(clock)) / 2),
                        y1 + pad);
            }
            return;
        }
        if (y2 - y1 < titleFont.getHeight()) {
            return;
        }
        g.setFont(titleFont);
        g.drawString(title, lx + 2 * pad, y1 + pad);
        if (b.detail.length() == 0) {
            return;
        }
        List<String> parts = new ArrayList<String>();
        String rest = b.detail;
        int at = rest.indexOf(Ui.DOT);
        while (at >= 0) {
            parts.add(rest.substring(0, at));
            rest = rest.substring(at + Ui.DOT.length());
            at = rest.indexOf(Ui.DOT);
        }
        parts.add(rest);
        int y = y1 + pad + titleFont.getHeight();
        g.setFont(detailFont);
        for (int iter = 0; iter < parts.size(); iter++) {
            if (y + detailFont.getHeight() > y2 - pad / 2) {
                return;
            }
            String line = parts.get(iter);
            boolean last = y + 2 * detailFont.getHeight() > y2 - pad / 2;
            if (last) {
                for (int more = iter + 1; more < parts.size(); more++) {
                    line += Ui.DOT + parts.get(more);
                }
            }
            g.drawString(clip(detailFont, line, room), lx + 2 * pad, y);
            if (last) {
                return;
            }
            y += detailFont.getHeight();
        }
    }

    // Busy time is hatched, so it never reads as a booking that lost its color.
    private void hatch(Graphics g, Lane ln, Block b, int x, int w, int color, int pad) {
        int y1 = yOf(ln, b.start);
        int y2 = yOf(ln, b.end);
        if (y2 <= y1 || w <= 0) {
            return;
        }
        int[] clip = g.getClip();
        g.clipRect(x, y1, w, y2 - y1);
        g.setColor(color);
        g.setAlpha(70);
        int step = Math.max(4, 2 * pad);
        for (int off = -(y2 - y1); off < w; off += step) {
            g.drawLine(x + off, y2, x + off + (y2 - y1), y1);
        }
        g.setAlpha(255);
        g.setClip(clip);
    }

    private static String clip(Font f, String text, int width) {
        if (f.stringWidth(text) <= width) {
            return text;
        }
        String dots = "...";
        int n = text.length();
        while (n > 0 && f.stringWidth(text.substring(0, n) + dots) > width) {
            n--;
        }
        // "C..." says nothing; a narrow block is better left to its color.
        return n < 3 ? "" : text.substring(0, n) + dots;
    }

    @Override
    public void pointerReleased(int x, int y) {
        super.pointerReleased(x, y);
        Block b = blockAt(x - getAbsoluteX() + getX(), y - getAbsoluteY() + getY());
        if (b != null && b.tap != null) {
            b.tap.run();
        }
    }

    /// The booking under a point in this component's parent's coordinates.
    Block blockAt(int x, int y) {
        int lane = laneWidth();
        int l = (x - getX() - gutter()) / lane;
        if (x < getX() + gutter() || l < 0 || l >= lanes.size()) {
            return null;
        }
        Lane ln = lanes.get(l);
        for (int iter = 0; iter < ln.blocks.size(); iter++) {
            Block b = ln.blocks.get(iter);
            if ((b.kind == BOOKING || b.kind == DONE) && y >= yOf(ln, b.start) && y <= yOf(ln, b.end)) {
                return b;
            }
        }
        return null;
    }
}
// end::slate-timeline[]
