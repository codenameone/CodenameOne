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
package com.codename1.ui.scheduler;

import com.codename1.scheduling.ScheduleItem;
import com.codename1.scheduling.ScheduleResource;
import com.codename1.scheduling.TimeMath;
import com.codename1.scheduling.WorkCalendar;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/// Time running down the screen under a row of columns: the day, work week
/// and week views, where a column is a day, and the resource day view, where
/// a column is a resource and the day is the same in all of them.
final class DayGrid extends ScrollSurface {
    private static final int MOVE = 1;
    private static final int RESIZE_START = 2;
    private static final int RESIZE_END = 3;
    private static final int RANGE = 4;
    private static final int MAX_ALL_DAY_ROWS = 3;
    // An item shorter than this is laid out as if it lasted this long, so
    // that it has room for a line of text and its neighbours stay off it.
    private static final int MIN_LAYOUT_MINUTES = 25;

    /// An item, or the part of one that falls in a column.
    static final class Box {
        ScheduleItem item;
        int col;
        int startMin;
        int endMin;
        int lane;
        int lanes = 1;
        boolean startsBefore;
        boolean endsAfter;
    }

    /// An item in the lane above the grid: all day, or a day long or more.
    static final class Chip {
        ScheduleItem item;
        int fromCol;
        int toCol;
        int row;
        boolean startsBefore;
        boolean endsAfter;
    }

    private static final Comparator<Box> BY_START = new Comparator<Box>() {
        @Override
        public int compare(Box a, Box b) {
            if (a.startMin != b.startMin) {
                return a.startMin < b.startMin ? -1 : 1;
            }
            if (a.endMin != b.endMin) {
                return a.endMin > b.endMin ? -1 : 1;
            }
            return a.item.getId().compareTo(b.item.getId());
        }
    };

    private static final Comparator<Chip> CHIP_ORDER = new Comparator<Chip>() {
        @Override
        public int compare(Chip a, Chip b) {
            if (a.fromCol != b.fromCol) {
                return a.fromCol < b.fromCol ? -1 : 1;
            }
            if (a.toCol != b.toCol) {
                return a.toCol > b.toCol ? -1 : 1;
            }
            return a.item.getId().compareTo(b.item.getId());
        }
    };

    private final Scheduler owner;
    private long[] days = new long[0];
    private ScheduleResource[] resources;
    private boolean dirty = true;
    private int laidW = -1;
    private int laidH = -1;
    private int laidGen = -1;
    private float hourH;
    private int colHeaderH;
    private int allDayH;
    private int chipH;
    private int allDayRows;
    private int contentW;
    private int pendingScrollMinute = -1;
    private final ArrayList<Box> boxes = new ArrayList<Box>();
    private final ArrayList<Box> backgrounds = new ArrayList<Box>();
    private final ArrayList<Chip> chips = new ArrayList<Chip>();
    private final ArrayList<ScheduleItem> loaded = new ArrayList<ScheduleItem>();
    private final int[] rect = new int[4];

    private int mode;
    private ScheduleItem editItem;
    private long editStart;
    private long editEnd;
    private String editResource;
    private long grabTime;
    private int anchorCol;
    private int anchorMin;

    DayGrid(Scheduler owner) {
        this.owner = owner;
        setUIID("Scheduler");
    }

    /// Shows `count` days from `firstDay`, or one day across `columns` when
    /// that is not null.
    void show(long firstDay, int count, List<ScheduleResource> columns) {
        boolean first = days.length == 0;
        if (columns != null) {
            resources = new ScheduleResource[columns.size()];
            columns.toArray(resources);
            days = new long[Math.max(1, resources.length)];
            for (int i = 0; i < days.length; i++) {
                days[i] = firstDay;
            }
        } else {
            resources = null;
            days = new long[count];
            for (int i = 0; i < count; i++) {
                days[i] = firstDay + i;
            }
        }
        dirty = true;
        if (first) {
            pendingScrollMinute = owner.getInitialMinute();
        }
        repaint();
    }

    /// Lists the days to show one by one, for a work week that skips some.
    void show(long[] list) {
        boolean first = days.length == 0;
        resources = null;
        days = list;
        dirty = true;
        if (first) {
            pendingScrollMinute = owner.getInitialMinute();
        }
        repaint();
    }

    void modelChanged() {
        dirty = true;
        repaint();
    }

    void scrollToMinute(int minute) {
        pendingScrollMinute = minute;
        dirty = true;
        repaint();
    }

    List<Box> boxes() {
        return boxes;
    }

    List<Chip> chips() {
        return chips;
    }

    // ------------------------------------------------------------- layout

    @Override
    void prepare(int width, int height) {
        int gen = UIManager.getThemeGeneration();
        if (!dirty && width == laidW && height == laidH && gen == laidGen) {
            return;
        }
        laidW = width;
        laidH = height;
        laidGen = gen;
        if (dirty) {
            dirty = false;
            load();
        }
        Style time = Look.style("SchedulerTimeLabel");
        Style header = Look.style("SchedulerHeader");
        Font hf = header.getFont();
        headerW = time.getFont().stringWidth(Dates.time(22 * 60, owner.is24Hours(), true)) + Look.mm(2.5f);
        if (resources != null) {
            boolean detail = false;
            for (ScheduleResource r : resources) {
                if (r.getDetail() != null && r.getDetail().length() > 0) {
                    detail = true;
                }
            }
            colHeaderH = hf.getHeight() * (detail ? 2 : 1) + Look.mm(2.5f);
        } else {
            colHeaderH = hf.getHeight() * 2 + Look.mm(3.5f);
        }
        chipH = Look.style("SchedulerItem").getFont().getHeight() + Look.mm(1.2f);
        allDayH = allDayRows == 0 ? 0 : Math.min(allDayRows, MAX_ALL_DAY_ROWS) * chipH + Look.mm(1);
        headerH = colHeaderH + allDayH;
        int bodyW = Math.max(0, width - headerW);
        int bodyH = Math.max(0, height - headerH);
        int minCol = Look.mm(resources != null ? 24 : 7);
        contentW = Math.max(bodyW, days.length * minCol);
        float least = Math.max(Look.mm(4), bodyH / 24f);
        if (hourH <= 0) {
            hourH = Look.mm(9);
        }
        hourH = Math.max(least, Math.min(Look.mm(40), hourH));
        if (pendingScrollMinute >= 0 && bodyH > 0) {
            setOffsets(getOffX(), Math.round(pendingScrollMinute * hourH / 60f));
            pendingScrollMinute = -1;
        }
    }

    @Override
    double[] saveZoom() {
        return new double[]{hourH};
    }

    @Override
    void restoreZoom(double[] saved) {
        hourH = (float) saved[0];
    }

    @Override
    int contentWidth() {
        return contentW;
    }

    @Override
    int contentHeight() {
        return Math.round(24 * hourH);
    }

    private int colX(int col) {
        return (int) ((long) col * contentW / days.length);
    }

    private int colAt(int cx) {
        int c = (int) ((long) cx * days.length / Math.max(1, contentW));
        return Math.max(0, Math.min(days.length - 1, c));
    }

    private int minuteY(int minute) {
        return Math.round(minute * hourH / 60f);
    }

    private int minuteAt(int cy) {
        return Math.max(0, Math.min(TimeMath.MINUTES_PER_DAY, (int) (cy * 60f / hourH)));
    }

    private int resourceColumn(String id) {
        if (id == null) {
            return -1;
        }
        for (int i = 0; i < resources.length; i++) {
            if (resources[i].getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private void load() {
        boxes.clear();
        backgrounds.clear();
        chips.clear();
        loaded.clear();
        allDayRows = 0;
        if (days.length == 0 || owner.getModel() == null) {
            return;
        }
        ZoneId zone = owner.getZone();
        long firstDay = days[0];
        long lastDay = days[days.length - 1];
        long from = TimeMath.startOfEpochDay(firstDay, zone);
        long to = TimeMath.startOfEpochDay(lastDay + 1, zone);
        owner.getModel().getItems(from, to, loaded);
        for (ScheduleItem item : loaded) {
            boolean event = item.getKind() == ScheduleItem.Kind.EVENT;
            int only = -1;
            if (resources != null) {
                only = resourceColumn(item.getResourceId());
                if (only < 0 && (event || item.getResourceId() != null)) {
                    continue;
                }
            }
            if (event && (item.isAllDay() || item.getEnd() - item.getStart() >= TimeMath.DAY)) {
                addChip(item, only, zone);
                continue;
            }
            for (int c = 0; c < days.length; c++) {
                if (resources != null && only >= 0 && c != only) {
                    continue;
                }
                long ds = TimeMath.startOfEpochDay(days[c], zone);
                long de = TimeMath.startOfEpochDay(days[c] + 1, zone);
                boolean instant = item.getEnd() == item.getStart() && item.getStart() >= ds && item.getStart() < de;
                if (!instant && !item.intersects(ds, de)) {
                    continue;
                }
                Box b = new Box();
                b.item = item;
                b.col = c;
                b.startsBefore = item.getStart() < ds;
                b.endsAfter = item.getEnd() > de;
                b.startMin = b.startsBefore ? 0 : TimeMath.minuteOfDay(item.getStart(), zone);
                b.endMin = item.getEnd() >= de ? TimeMath.MINUTES_PER_DAY : TimeMath.minuteOfDay(item.getEnd(), zone);
                if (b.endMin < b.startMin) {
                    b.endMin = b.startMin;
                }
                if (event) {
                    boxes.add(b);
                } else {
                    backgrounds.add(b);
                }
            }
        }
        Collections.sort(boxes, BY_START);
        for (int c = 0; c < days.length; c++) {
            layoutColumn(c);
        }
        layoutChips();
    }

    private void addChip(ScheduleItem item, int only, ZoneId zone) {
        Chip chip = new Chip();
        chip.item = item;
        if (resources != null) {
            chip.fromCol = only;
            chip.toCol = only;
        } else {
            long s = TimeMath.epochDay(item.getStart(), zone);
            // The end is exclusive: an item that ends at midnight is not on
            // the day that starts then.
            long e = TimeMath.epochDay(Math.max(item.getStart(), item.getEnd() - 1), zone);
            chip.fromCol = -1;
            chip.toCol = -1;
            for (int c = 0; c < days.length; c++) {
                if (days[c] >= s && days[c] <= e) {
                    if (chip.fromCol < 0) {
                        chip.fromCol = c;
                    }
                    chip.toCol = c;
                }
            }
            if (chip.fromCol < 0) {
                return;
            }
            chip.startsBefore = s < days[chip.fromCol];
            chip.endsAfter = e > days[chip.toCol];
        }
        chips.add(chip);
    }

    // The boxes of a column that overlap, directly or through one another,
    // share its width in equal lanes.
    private void layoutColumn(int col) {
        ArrayList<Box> group = new ArrayList<Box>();
        ArrayList<Integer> laneEnd = new ArrayList<Integer>();
        int groupEnd = -1;
        for (Box b : boxes) {
            if (b.col != col) {
                continue;
            }
            int end = Math.max(b.endMin, b.startMin + MIN_LAYOUT_MINUTES);
            if (!group.isEmpty() && b.startMin >= groupEnd) {
                close(group, laneEnd.size());
                group.clear();
                laneEnd.clear();
            }
            int lane = -1;
            for (int i = 0; i < laneEnd.size(); i++) {
                if (laneEnd.get(i).intValue() <= b.startMin) {
                    lane = i;
                    break;
                }
            }
            if (lane < 0) {
                lane = laneEnd.size();
                laneEnd.add(Integer.valueOf(end));
            } else {
                laneEnd.set(lane, Integer.valueOf(end));
            }
            b.lane = lane;
            group.add(b);
            groupEnd = Math.max(groupEnd, end);
        }
        close(group, laneEnd.size());
    }

    private static void close(List<Box> group, int lanes) {
        for (Box b : group) {
            b.lanes = Math.max(1, lanes);
        }
    }

    private void layoutChips() {
        Collections.sort(chips, CHIP_ORDER);
        ArrayList<Integer> rowEnd = new ArrayList<Integer>();
        for (Chip c : chips) {
            int row = -1;
            for (int i = 0; i < rowEnd.size(); i++) {
                if (rowEnd.get(i).intValue() < c.fromCol) {
                    row = i;
                    break;
                }
            }
            if (row < 0) {
                row = rowEnd.size();
                rowEnd.add(Integer.valueOf(c.toCol));
            } else {
                rowEnd.set(row, Integer.valueOf(c.toCol));
            }
            c.row = row;
        }
        allDayRows = rowEnd.size();
    }

    /// The rectangle of a box in the content's coordinates.
    void boxRect(Box b, int[] out) {
        int x0 = colX(b.col);
        int cw = colX(b.col + 1) - x0;
        // A strip is left clear at the edge of a column that has room, so
        // there is always empty space to press on.
        int usable = cw - (cw > Look.mm(14) ? Look.mm(1.6f) : 1);
        int lx = x0 + (int) ((long) b.lane * usable / b.lanes);
        int rx = x0 + (int) ((long) (b.lane + 1) * usable / b.lanes);
        int top = minuteY(b.startMin);
        int bottom = minuteY(b.endMin);
        out[0] = lx + 1;
        out[1] = top + 1;
        out[2] = Math.max(2, rx - lx - 1);
        out[3] = Math.max(Look.mm(2.6f), bottom - top - 1);
    }

    private boolean chipHidden(Chip c) {
        return allDayRows > MAX_ALL_DAY_ROWS && c.row >= MAX_ALL_DAY_ROWS - 1;
    }

    private void chipRect(Chip c, int[] out) {
        int x0 = colX(c.fromCol);
        int x1 = colX(c.toCol + 1);
        out[0] = x0 + 1;
        out[1] = colHeaderH + c.row * chipH;
        out[2] = x1 - x0 - 2;
        out[3] = chipH - 1;
    }

    private int hiddenChips(int col) {
        int n = 0;
        for (Chip c : chips) {
            if (chipHidden(c) && c.fromCol <= col && c.toCol >= col) {
                n++;
            }
        }
        return n;
    }

    // -------------------------------------------------------------- paint

    @Override
    void paintBody(Graphics g, int x, int y) {
        Style grid = Look.style("SchedulerGrid");
        Style off = Look.style("SchedulerOffHours");
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        Look.fill(g, grid, cx, cy, cw, ch);
        int height = contentHeight();
        ZoneId zone = owner.getZone();

        for (int c = 0; c < days.length; c++) {
            int x0 = x + colX(c);
            int x1 = x + colX(c + 1);
            if (x1 < cx || x0 > cx + cw) {
                continue;
            }
            WorkCalendar cal = resources != null && resources.length > 0 && resources[c].getCalendar() != null
                    ? resources[c].getCalendar() : owner.getWorkCalendar();
            if (cal != null) {
                int[] iv = cal.getWorkingHours(days[c]);
                int cursor = 0;
                for (int i = 0; i < iv.length; i += 2) {
                    if (iv[i] > cursor) {
                        Look.fill(g, off, x0, y + minuteY(cursor), x1 - x0, minuteY(iv[i]) - minuteY(cursor));
                    }
                    cursor = iv[i + 1];
                }
                if (cursor < TimeMath.MINUTES_PER_DAY) {
                    Look.fill(g, off, x0, y + minuteY(cursor), x1 - x0, height - minuteY(cursor));
                }
            }
        }

        int line = Look.mix(grid.getBgColor(), grid.getFgColor(), 150);
        int faint = Look.mix(grid.getBgColor(), grid.getFgColor(), 60);
        boolean halves = hourH >= Look.mm(11);
        for (int h = 0; h <= 24; h++) {
            int yy = y + minuteY(h * 60);
            if (yy >= cy - 1 && yy <= cy + ch) {
                g.setColor(line);
                g.drawLine(cx, yy, cx + cw, yy);
            }
            if (halves && h < 24) {
                int hy = y + minuteY(h * 60 + 30);
                if (hy >= cy && hy <= cy + ch) {
                    g.setColor(faint);
                    g.drawLine(cx, hy, cx + cw, hy);
                }
            }
        }
        g.setColor(line);
        for (int c = 1; c < days.length; c++) {
            int lx = x + colX(c);
            g.drawLine(lx, cy, lx, cy + ch);
        }

        ScheduleItemRenderer renderer = owner.getItemRenderer();
        for (Box b : backgrounds) {
            int x0 = x + colX(b.col);
            int x1 = x + colX(b.col + 1);
            int top = y + minuteY(b.startMin);
            renderer.paintItem(g, owner, b.item, x0 + 1, top, x1 - x0 - 1, y + minuteY(b.endMin) - top, 0);
        }
        ScheduleItem selected = owner.getSelectedItem();
        Box selectedBox = null;
        for (Box b : boxes) {
            boxRect(b, rect);
            if (y + rect[1] > cy + ch || y + rect[1] + rect[3] < cy) {
                continue;
            }
            int state = 0;
            if (b.item == selected) { //NOPMD CompareObjectsWithEquals - the selection is this very object
                state |= ScheduleItemRenderer.SELECTED;
                selectedBox = b;
            }
            if (b.startsBefore) {
                state |= ScheduleItemRenderer.STARTS_BEFORE;
            }
            if (b.endsAfter) {
                state |= ScheduleItemRenderer.ENDS_AFTER;
            }
            renderer.paintItem(g, owner, b.item, x + rect[0], y + rect[1], rect[2], rect[3], state);
        }
        if (selectedBox != null && owner.isEditable() && selectedBox.item.isResizable() && !isDragEdits()
                && mode == 0) {
            boxRect(selectedBox, rect);
            paintHandle(g, grid, x + rect[0] + rect[2] / 2, y + rect[1]);
            paintHandle(g, grid, x + rect[0] + rect[2] / 2, y + rect[1] + rect[3]);
        }
        if (mode != 0) {
            paintEdit(g, x, y, zone);
        }
        paintNow(g, x, y, zone);
    }

    private static void paintHandle(Graphics g, Style grid, int cx, int cy) {
        int r = Look.mm(1.1f);
        boolean aa = g.isAntiAliased();
        g.setAntiAliased(true);
        g.setColor(grid.getBgColor());
        g.fillArc(cx - r, cy - r, 2 * r, 2 * r, 0, 360);
        g.setColor(Look.style("SchedulerNow").getFgColor());
        g.drawArc(cx - r, cy - r, 2 * r, 2 * r, 0, 360);
        g.setAntiAliased(aa);
    }

    // What is being dragged out: a new range, or an item at the time it
    // would move to.
    private void paintEdit(Graphics g, int x, int y, ZoneId zone) {
        for (int c = 0; c < days.length; c++) {
            if (resources != null) {
                int target = mode == RANGE ? anchorCol : resourceColumn(editResource);
                if (c != target) {
                    continue;
                }
            }
            long ds = TimeMath.startOfEpochDay(days[c], zone);
            long de = TimeMath.startOfEpochDay(days[c] + 1, zone);
            if (editEnd <= ds || editStart >= de) {
                continue;
            }
            int top = editStart <= ds ? 0 : minuteY(TimeMath.minuteOfDay(editStart, zone));
            int bottom = editEnd >= de ? contentHeight() : minuteY(TimeMath.minuteOfDay(editEnd, zone));
            int x0 = x + colX(c) + 1;
            int w = colX(c + 1) - colX(c) - 2;
            int h = Math.max(Look.mm(2.6f), bottom - top);
            if (mode == RANGE) {
                Style sel = Look.style("SchedulerSelection");
                Look.fill(g, sel, x0, y + top, w, h);
                g.setColor(sel.getFgColor());
                g.drawRect(x0, y + top, w - 1, h - 1);
                Font f = sel.getFont();
                if (h >= f.getHeight()) {
                    g.setFont(f);
                    g.drawString(Look.fit(owner.formatRange(editStart, editEnd), f, w - Look.mm(2)), x0 + Look.mm(1),
                            y + top + 1);
                }
            } else {
                owner.getItemRenderer().paintItem(g, owner, editItem, x0, y + top, w, h,
                        ScheduleItemRenderer.DRAGGED | ScheduleItemRenderer.SELECTED);
                Style sel = Look.style("SchedulerSelection");
                Font f = sel.getFont();
                g.setFont(f);
                g.setColor(Look.style("SchedulerGrid").getFgColor());
                String s = owner.formatRange(editStart, editEnd);
                int ty = y + top - f.getHeight();
                if (ty > y + getOffY()) {
                    g.drawString(Look.fit(s, f, w), x0, ty);
                }
            }
        }
    }

    private void paintNow(Graphics g, int x, int y, ZoneId zone) {
        long now = owner.now();
        long today = TimeMath.epochDay(now, zone);
        int first = -1;
        int last = -1;
        for (int c = 0; c < days.length; c++) {
            if (days[c] == today) {
                if (first < 0) {
                    first = c;
                }
                last = c;
            }
        }
        if (first < 0) {
            return;
        }
        Style st = Look.style("SchedulerNow");
        int yy = y + minuteY(TimeMath.minuteOfDay(now, zone));
        int x0 = x + colX(first);
        int x1 = x + colX(last + 1);
        int r = Look.mm(0.9f);
        boolean aa = g.isAntiAliased();
        g.setAntiAliased(true);
        g.setColor(st.getFgColor());
        g.fillRect(x0, yy - 1, x1 - x0, 2);
        g.fillArc(x0 - r, yy - r, 2 * r, 2 * r, 0, 360);
        g.setAntiAliased(aa);
    }

    @Override
    void paintTopHeader(Graphics g, int x, int y) {
        Style header = Look.style("SchedulerHeader");
        Style todaySt = Look.style("SchedulerHeaderToday");
        Font f = header.getFont();
        int fh = f.getHeight();
        int cx = g.getClipX();
        int cw = g.getClipWidth();
        Look.fill(g, header, cx, y, cw, headerH);
        long today = TimeMath.epochDay(owner.now(), owner.getZone());
        g.setFont(f);
        int alpha = g.getAlpha();
        for (int c = 0; c < days.length; c++) {
            int x0 = x + colX(c);
            int w = colX(c + 1) - colX(c);
            if (x0 + w < cx || x0 > cx + cw) {
                continue;
            }
            if (resources != null) {
                if (resources.length == 0) {
                    continue;
                }
                ScheduleResource r = resources[c];
                String name = Look.fit(r.getName(), f, w - Look.mm(2));
                boolean two = colHeaderH > fh * 2;
                int ty = y + (colHeaderH - (two ? 2 * fh : fh)) / 2;
                g.setColor(header.getFgColor());
                g.drawString(name, x0 + (w - f.stringWidth(name)) / 2, ty);
                if (two && r.getDetail() != null) {
                    String d = Look.fit(r.getDetail(), f, w - Look.mm(2));
                    g.setAlpha(alpha * 160 / 255);
                    g.drawString(d, x0 + (w - f.stringWidth(d)) / 2, ty + fh);
                    g.setAlpha(alpha);
                }
                continue;
            }
            String wd = Look.fit(Dates.weekdayShort(TimeMath.dayOfWeek(days[c])), f, w);
            String num = String.valueOf(Dates.dayOfMonth(days[c]));
            int top = y + Look.mm(0.8f);
            g.setColor(header.getFgColor());
            g.setAlpha(alpha * 170 / 255);
            g.drawString(wd, x0 + (w - f.stringWidth(wd)) / 2, top);
            g.setAlpha(alpha);
            int nw = f.stringWidth(num);
            int ny = top + fh + Look.mm(0.6f);
            if (days[c] == today) {
                int d = Math.min(w - 2, fh + Look.mm(1.4f));
                boolean aa = g.isAntiAliased();
                g.setAntiAliased(true);
                g.setColor(todaySt.getBgColor());
                g.fillArc(x0 + (w - d) / 2, ny + (fh - d) / 2, d, d, 0, 360);
                g.setAntiAliased(aa);
                g.setColor(todaySt.getFgColor());
            }
            g.drawString(num, x0 + (w - nw) / 2, ny);
        }
        if (allDayH > 0) {
            ScheduleItemRenderer renderer = owner.getItemRenderer();
            ScheduleItem selected = owner.getSelectedItem();
            for (Chip c : chips) {
                if (chipHidden(c)) {
                    continue;
                }
                chipRect(c, rect);
                int state = ScheduleItemRenderer.COMPACT;
                if (c.item == selected) { //NOPMD CompareObjectsWithEquals - the selection is this very object
                    state |= ScheduleItemRenderer.SELECTED;
                }
                if (c.startsBefore) {
                    state |= ScheduleItemRenderer.STARTS_BEFORE;
                }
                if (c.endsAfter) {
                    state |= ScheduleItemRenderer.ENDS_AFTER;
                }
                renderer.paintItem(g, owner, c.item, x + rect[0], y + rect[1], rect[2], rect[3], state);
            }
            if (allDayRows > MAX_ALL_DAY_ROWS) {
                g.setFont(f);
                g.setColor(header.getFgColor());
                for (int c = 0; c < days.length; c++) {
                    int n = hiddenChips(c);
                    if (n > 0) {
                        g.drawString("+" + n, x + colX(c) + Look.mm(1),
                                y + colHeaderH + (MAX_ALL_DAY_ROWS - 1) * chipH + (chipH - fh) / 2);
                    }
                }
            }
        }
        g.setColor(Look.mix(header.getBgColor(), header.getFgColor(), 50));
        g.drawLine(cx, y + headerH - 1, cx + cw, y + headerH - 1);
    }

    @Override
    void paintLeftHeader(Graphics g, int x, int y) {
        Style st = Look.style("SchedulerTimeLabel");
        Font f = st.getFont();
        int fh = f.getHeight();
        Look.fill(g, st, x, g.getClipY(), headerW, g.getClipHeight());
        g.setFont(f);
        g.setColor(st.getFgColor());
        int every = hourH < fh * 1.3f ? 2 : 1;
        boolean h24 = owner.is24Hours();
        for (int h = every; h < 24; h += every) {
            String s = Dates.time(h * 60, h24, true);
            g.drawString(s, x + headerW - f.stringWidth(s) - Look.mm(1), y + minuteY(h * 60) - fh / 2);
        }
    }

    @Override
    void paintCorner(Graphics g, int x, int y) {
        Style header = Look.style("SchedulerHeader");
        Look.fill(g, header, x, y, headerW, headerH);
        g.setColor(Look.mix(header.getBgColor(), header.getFgColor(), 50));
        g.drawLine(x, y + headerH - 1, x + headerW, y + headerH - 1);
    }

    // ------------------------------------------------------------ pointer

    /// The box at a point of the component, or null.
    Box boxAt(int px, int py) {
        if (px < headerW || py < headerH) {
            return null;
        }
        int cx = px - headerW + getOffX();
        int cy = py - headerH + getOffY();
        for (int i = boxes.size() - 1; i >= 0; i--) {
            Box b = boxes.get(i);
            boxRect(b, rect);
            if (cx >= rect[0] && cx < rect[0] + rect[2] && cy >= rect[1] && cy < rect[1] + rect[3]) {
                return b;
            }
        }
        return null;
    }

    private Chip chipAt(int px, int py) {
        int cx = px - headerW + getOffX();
        for (Chip c : chips) {
            if (chipHidden(c)) {
                continue;
            }
            chipRect(c, rect);
            if (cx >= rect[0] && cx < rect[0] + rect[2] && py >= rect[1] && py < rect[1] + rect[3]) {
                return c;
            }
        }
        return null;
    }

    private long snap(long time) {
        ZoneId zone = owner.getZone();
        int step = Math.max(1, owner.getSnapMinutes());
        long day = TimeMath.epochDay(time, zone);
        int m = TimeMath.minuteOfDay(time, zone);
        int snapped = (m + step / 2) / step * step;
        return TimeMath.at(day, Math.min(TimeMath.MINUTES_PER_DAY, snapped), zone);
    }

    private int snapMinute(int minute, boolean down) {
        int step = Math.max(1, owner.getSnapMinutes());
        return (down ? minute : minute + step / 2) / step * step;
    }

    @Override
    boolean beginEdit(int x, int y, boolean handlesOnly) {
        if (!owner.isEditable() || x < headerW || y < headerH || days.length == 0) {
            return false;
        }
        int cx = x - headerW + getOffX();
        int cy = y - headerH + getOffY();
        ZoneId zone = owner.getZone();
        Box b = boxAt(x, y);
        if (b == null && handlesOnly) {
            // The handles reach a little outside the item they belong to.
            int reach = Look.mm(2.5f);
            b = boxAt(x, y - reach);
            if (b == null) {
                b = boxAt(x, y + reach);
            }
        }
        if (b != null) {
            boxRect(b, rect);
            int edge = Math.min(rect[3] / 3, Look.mm(handlesOnly ? 3f : 1.6f));
            boolean bottom = !b.endsAfter && rect[1] + rect[3] - cy <= edge;
            boolean top = !bottom && !b.startsBefore && cy - rect[1] <= edge;
            if (handlesOnly && (b.item != owner.getSelectedItem() || !(top || bottom))) { //NOPMD CompareObjectsWithEquals
                return false;
            }
            if ((top || bottom) && b.item.isResizable()) {
                mode = bottom ? RESIZE_END : RESIZE_START;
            } else if (!handlesOnly && b.item.isMovable()) {
                mode = MOVE;
            } else {
                return false;
            }
            editItem = b.item;
            editStart = b.item.getStart();
            editEnd = b.item.getEnd();
            editResource = b.item.getResourceId();
            grabTime = TimeMath.at(days[b.col], minuteAt(cy), zone);
            owner.viewSelected(b.item);
            return true;
        }
        if (handlesOnly) {
            return false;
        }
        mode = RANGE;
        editItem = null;
        anchorCol = colAt(cx);
        anchorMin = snapMinute(minuteAt(cy), true);
        editStart = TimeMath.at(days[anchorCol], anchorMin, zone);
        editEnd = editStart + owner.getDefaultMinutes() * TimeMath.MINUTE;
        editResource = resources != null && resources.length > 0 ? resources[anchorCol].getId() : null;
        return true;
    }

    @Override
    void editDragged(int x, int y) {
        if (mode == 0) {
            return;
        }
        ZoneId zone = owner.getZone();
        int cx = Math.max(0, Math.min(contentW - 1, x - headerW + getOffX()));
        int cy = Math.max(0, Math.min(contentHeight(), y - headerH + getOffY()));
        int col = colAt(cx);
        int minute = minuteAt(cy);
        long step = Math.max(1, owner.getSnapMinutes()) * TimeMath.MINUTE;
        long pointer = TimeMath.at(days[col], minute, zone);
        if (mode == MOVE) {
            long length = editItem.getEnd() - editItem.getStart();
            editStart = snap(editItem.getStart() + pointer - grabTime);
            editEnd = editStart + length;
            if (resources != null && resources.length > 0) {
                editResource = resources[col].getId();
            }
        } else if (mode == RESIZE_END) {
            editEnd = Math.max(editStart + step, snap(pointer));
        } else if (mode == RESIZE_START) {
            editStart = Math.min(editEnd - step, snap(pointer));
        } else {
            int m = snapMinute(minute, false);
            if (m > anchorMin) {
                editStart = TimeMath.at(days[anchorCol], anchorMin, zone);
                editEnd = TimeMath.at(days[anchorCol], m, zone);
            } else if (m < anchorMin) {
                editStart = TimeMath.at(days[anchorCol], m, zone);
                editEnd = TimeMath.at(days[anchorCol], anchorMin, zone) + step;
            } else {
                editStart = TimeMath.at(days[anchorCol], anchorMin, zone);
                editEnd = editStart + owner.getDefaultMinutes() * TimeMath.MINUTE;
            }
        }
    }

    @Override
    void endEdit(boolean commit) {
        int m = mode;
        ScheduleItem item = editItem;
        mode = 0;
        editItem = null;
        if (!commit) {
            return;
        }
        if (m == RANGE) {
            owner.viewRange(editStart, editEnd, editResource);
        } else if (item != null && (editStart != item.getStart() || editEnd != item.getEnd()
                || !same(editResource, item.getResourceId()))) {
            owner.viewChanged(item, editStart, editEnd, editResource);
        }
    }

    private static boolean same(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    @Override
    void tapped(int x, int y, boolean doubleTap) {
        if (days.length == 0 || x < headerW) {
            return;
        }
        int cx = x - headerW + getOffX();
        if (y < headerH) {
            Chip chip = chipAt(x, y);
            if (chip != null) {
                pick(chip.item, doubleTap);
            } else if (resources == null && (y < colHeaderH || hiddenChips(colAt(cx)) > 0)) {
                owner.viewDrill(days[colAt(cx)]);
            }
            return;
        }
        Box b = boxAt(x, y);
        if (b != null) {
            pick(b.item, doubleTap);
            return;
        }
        if (owner.getSelectedItem() != null) {
            owner.viewSelected(null);
        } else if (doubleTap && owner.isEditable()) {
            int col = colAt(cx);
            int minute = snapMinute(minuteAt(y - headerH + getOffY()), true);
            long start = TimeMath.at(days[col], minute, owner.getZone());
            owner.viewRange(start, start + owner.getDefaultMinutes() * TimeMath.MINUTE,
                    resources != null && resources.length > 0 ? resources[col].getId() : null);
        }
    }

    private void pick(ScheduleItem item, boolean doubleTap) {
        owner.viewSelected(item);
        if (doubleTap || owner.isOpenOnTap()) {
            owner.viewOpened(item);
        }
    }

    @Override
    boolean zoomBy(float factor, int x, int y) {
        float before = hourH;
        float least = Math.max(Look.mm(4), bodyHeight() / 24f);
        float next = Math.max(least, Math.min(Look.mm(40), hourH * factor));
        if (Math.abs(next - before) < 0.01f) {
            return false;
        }
        // The minute under the pointer stays under it.
        float minute = (y - headerH + getOffY()) * 60f / before;
        hourH = next;
        setOffsets(getOffX(), Math.round(minute * next / 60f) - (y - headerH));
        repaint();
        return true;
    }

    float getHourHeight() {
        return hourH;
    }
}
