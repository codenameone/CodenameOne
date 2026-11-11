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

/// Time running across the screen under a row per resource: who or what is
/// booked when, over days or weeks. Items of a row that overlap are stacked,
/// and the row grows to hold them.
final class ResourceTimeline extends ScrollSurface {
    private static final int MOVE = 1;
    private static final int RESIZE_START = 2;
    private static final int RESIZE_END = 3;
    private static final int RANGE = 4;

    /// An item in a row.
    static final class Bar {
        ScheduleItem item;
        int row;
        int lane;
    }

    private static final Comparator<Bar> BY_START = new Comparator<Bar>() {
        @Override
        public int compare(Bar a, Bar b) {
            if (a.row != b.row) {
                return a.row < b.row ? -1 : 1;
            }
            if (a.item.getStart() != b.item.getStart()) {
                return a.item.getStart() < b.item.getStart() ? -1 : 1;
            }
            return a.item.getId().compareTo(b.item.getId());
        }
    };

    private final Scheduler owner;
    private final TimeAxis axis;
    private ScheduleResource[] resources = new ScheduleResource[0];
    private int rowCount = 1;
    private int[] rowTop = new int[2];
    private int[] rowLanes = new int[1];
    private boolean dirty = true;
    private boolean scaled;
    private boolean pendingScroll;
    private int laidW = -1;
    private int laidH = -1;
    private int laidGen = -1;
    private int laneH;
    private int rowPad;
    private int days = 1;
    private final ArrayList<Bar> bars = new ArrayList<Bar>();
    private final ArrayList<Bar> backgrounds = new ArrayList<Bar>();
    private final ArrayList<ScheduleItem> loaded = new ArrayList<ScheduleItem>();
    private final int[] rect = new int[4];

    private int mode;
    private ScheduleItem editItem;
    private long editStart;
    private long editEnd;
    private int editRow;
    private long grabTime;
    private long anchorTime;

    ResourceTimeline(Scheduler owner) {
        this.owner = owner;
        axis = new TimeAxis(owner.getZone());
        setUIID("Scheduler");
    }

    void show(long firstDay, int count) {
        ZoneId zone = owner.getZone();
        axis.setZone(zone);
        axis.setFirstDayOfWeek(owner.getFirstDayOfWeek());
        axis.set24Hours(owner.is24Hours());
        axis.setRange(TimeMath.startOfEpochDay(firstDay, zone), TimeMath.startOfEpochDay(firstDay + count, zone));
        if (count != days) {
            scaled = false;
        }
        days = count;
        dirty = true;
        pendingScroll = true;
        repaint();
    }

    void modelChanged() {
        dirty = true;
        repaint();
    }

    TimeAxis axis() {
        return axis;
    }

    List<Bar> bars() {
        return bars;
    }

    // ------------------------------------------------------------- layout

    @Override
    void prepare(int width, int height) {
        int gen = UIManager.getThemeGeneration();
        if (!dirty && width == laidW && height == laidH && gen == laidGen) {
            return;
        }
        boolean reload = dirty;
        laidW = width;
        laidH = height;
        laidGen = gen;
        dirty = false;
        Style header = Look.style("SchedulerHeader");
        Font hf = header.getFont();
        Font itemFont = Look.style("SchedulerItem").getFont();
        List<ScheduleResource> list = owner.getModel() == null ? null : owner.getModel().getResources();
        resources = new ScheduleResource[list == null ? 0 : list.size()];
        if (list != null) {
            list.toArray(resources);
        }
        rowCount = Math.max(1, resources.length);
        // A drag under way keeps a row that exists.
        editRow = Math.min(editRow, rowCount - 1);
        int widest = 0;
        boolean detail = false;
        for (ScheduleResource r : resources) {
            widest = Math.max(widest, hf.stringWidth(r.getName()));
            if (r.getDetail() != null && r.getDetail().length() > 0) {
                detail = true;
                widest = Math.max(widest, hf.stringWidth(r.getDetail()));
            }
        }
        headerW = resources.length == 0 ? 0 : Math.min(width * 2 / 5, Math.min(Look.mm(38), widest + Look.mm(4)));
        headerH = axis.headerHeight(header);
        laneH = itemFont.getHeight() + Look.mm(2.2f);
        rowPad = Look.mm(1);
        int minRow = hf.getHeight() * (detail ? 2 : 1) + Look.mm(3);

        int bodyW = Math.max(1, width - headerW);
        long span = axis.getEnd() - axis.getStart();
        axis.setScaleLimits(15.0 * TimeMath.MINUTE / Look.mm(9), Math.max(1.0, (double) span / bodyW));
        if (!scaled && width > 0) {
            scaled = true;
            axis.setMillisPerPixel(days <= 7 ? (double) TimeMath.DAY / Math.max(bodyW, Look.mm(110))
                    : (double) TimeMath.DAY / Look.mm(14));
        }
        axis.pickUnits(hf.stringWidth(Dates.time(22 * 60, owner.is24Hours(), true)) + Look.mm(3));

        if (reload) {
            load();
        }
        rowTop = new int[rowCount + 1];
        for (int r = 0; r < rowCount; r++) {
            rowTop[r + 1] = rowTop[r] + Math.max(minRow, Math.max(1, rowLanes[r]) * laneH + 2 * rowPad);
        }
        if (pendingScroll && width > 0) {
            pendingScroll = false;
            long now = owner.now();
            long target = now >= axis.getStart() && now < axis.getEnd() ? now - 2 * TimeMath.HOUR
                    : axis.getStart() + owner.getInitialMinute() * TimeMath.MINUTE;
            setOffsets(Math.max(0, axis.x(target)), getOffY());
        }
    }

    private int rowOf(String resourceId) {
        if (resources.length == 0) {
            return 0;
        }
        if (resourceId == null) {
            return -1;
        }
        for (int i = 0; i < resources.length; i++) {
            if (resources[i].getId().equals(resourceId)) {
                return i;
            }
        }
        return -1;
    }

    private void load() {
        bars.clear();
        backgrounds.clear();
        loaded.clear();
        rowLanes = new int[rowCount];
        if (owner.getModel() == null) {
            return;
        }
        owner.getModel().getItems(axis.getStart(), axis.getEnd(), loaded);
        for (ScheduleItem item : loaded) {
            int row = rowOf(item.getResourceId());
            boolean event = item.getKind() == ScheduleItem.Kind.EVENT;
            if (row < 0) {
                if (event || item.getResourceId() != null) {
                    continue;
                }
                // Background with no resource is everybody's.
                for (int r = 0; r < rowCount; r++) {
                    Bar b = new Bar();
                    b.item = item;
                    b.row = r;
                    backgrounds.add(b);
                }
                continue;
            }
            Bar b = new Bar();
            b.item = item;
            b.row = row;
            if (event) {
                bars.add(b);
            } else {
                backgrounds.add(b);
            }
        }
        Collections.sort(bars, BY_START);
        ArrayList<Long> laneEnd = new ArrayList<Long>();
        int row = -1;
        for (Bar b : bars) {
            if (b.row != row) {
                row = b.row;
                laneEnd.clear();
            }
            int lane = -1;
            for (int i = 0; i < laneEnd.size(); i++) {
                if (laneEnd.get(i).longValue() <= b.item.getStart()) {
                    lane = i;
                    break;
                }
            }
            long end = Math.max(b.item.getEnd(), b.item.getStart() + 1);
            if (lane < 0) {
                lane = laneEnd.size();
                laneEnd.add(Long.valueOf(end));
            } else {
                laneEnd.set(lane, Long.valueOf(end));
            }
            b.lane = lane;
            rowLanes[row] = Math.max(rowLanes[row], laneEnd.size());
        }
    }

    @Override
    double[] saveZoom() {
        return new double[]{axis.getMillisPerPixel()};
    }

    @Override
    void restoreZoom(double[] saved) {
        axis.setMillisPerPixel(saved[0]);
    }

    @Override
    int contentWidth() {
        return axis.width();
    }

    @Override
    int contentHeight() {
        return rowTop[rowCount];
    }

    private int rowAt(int cy) {
        for (int r = 0; r < rowCount; r++) {
            if (cy < rowTop[r + 1]) {
                return r;
            }
        }
        return rowCount - 1;
    }

    /// The rectangle of a bar in the content's coordinates.
    void barRect(Bar b, int[] out) {
        int x0 = axis.x(b.item.getStart());
        int x1 = axis.x(b.item.getEnd());
        out[0] = x0;
        out[1] = rowTop[b.row] + rowPad + b.lane * laneH;
        out[2] = Math.max(Look.mm(1.5f), x1 - x0 - 1);
        out[3] = laneH - 1;
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
        for (int r = 0; r < rowCount; r++) {
            int top = y + rowTop[r];
            int h = rowTop[r + 1] - rowTop[r];
            if (top > cy + ch || top + h < cy) {
                continue;
            }
            WorkCalendar cal = resources.length > 0 && resources[r].getCalendar() != null
                    ? resources[r].getCalendar() : owner.getWorkCalendar();
            axis.paintOffHours(g, cal, off, x, top, h, cx, cx + cw);
        }
        // Background bands go under the grid lines, as the hours off do.
        ScheduleItemRenderer renderer = owner.getItemRenderer();
        for (Bar b : backgrounds) {
            int x0 = x + axis.x(b.item.getStart());
            int x1 = x + axis.x(b.item.getEnd());
            if (x1 < cx || x0 > cx + cw) {
                continue;
            }
            renderer.paintItem(g, owner, b.item, x0, y + rowTop[b.row] + 1, x1 - x0, rowTop[b.row + 1] - rowTop[b.row]
                    - 1, ScheduleItemRenderer.HORIZONTAL | ScheduleItemRenderer.COMPACT);
        }
        axis.paintGrid(g, Look.line(grid, 255), Look.under(grid), x, cy, ch, cx,
                cx + cw);
        g.setColor(Look.line(grid, 255));
        for (int r = 1; r <= rowCount; r++) {
            int ly = y + rowTop[r];
            g.drawLine(cx, ly, cx + cw, ly);
        }
        paintRowHover(g, y, cx, cw, hovered(), grid.getFgColor());
        paintRowHover(g, y, cx, cw, fading(), grid.getFgColor());
        ScheduleItem selected = owner.getSelectedItem();
        boolean lit = anyHover();
        float lift = mode == MOVE || mode == RESIZE_START || mode == RESIZE_END ? liftLevel() : 0;
        int alpha = g.getAlpha();
        for (Bar b : bars) {
            barRect(b, rect);
            settle(b.item.getId(), rect);
            if (x + rect[0] > cx + cw || x + rect[0] + rect[2] < cx) {
                continue;
            }
            int state = ScheduleItemRenderer.HORIZONTAL | ScheduleItemRenderer.COMPACT;
            float over = lit ? hoverLevel(b.item.getId()) : 0;
            if (over > 0) {
                state |= ScheduleItemRenderer.HOVERED;
            }
            boolean held = lift > 0 && b.item == editItem; //NOPMD CompareObjectsWithEquals - the very item that is held
            if (held) {
                // What is left behind while the item is carried.
                g.setAlpha(Math.round(alpha * (1 - 0.6f * lift)));
            }
            if (b.item == selected) { //NOPMD CompareObjectsWithEquals - the selection is this very object
                state |= ScheduleItemRenderer.SELECTED;
            }
            if (b.item.getStart() < axis.getStart()) {
                state |= ScheduleItemRenderer.STARTS_BEFORE;
            }
            if (b.item.getEnd() > axis.getEnd()) {
                state |= ScheduleItemRenderer.ENDS_AFTER;
            }
            // The title stays in view while its item does.
            int left = Math.max(x + rect[0], cx);
            int right = x + rect[0] + rect[2];
            if (left > x + rect[0] && right - left > Look.mm(6)) {
                renderer.paintItem(g, owner, b.item, left - Look.mm(2), y + rect[1], right - left + Look.mm(2), rect[3],
                        state | ScheduleItemRenderer.STARTS_BEFORE);
            } else {
                renderer.paintItem(g, owner, b.item, x + rect[0], y + rect[1], rect[2], rect[3], state);
            }
            if (held) {
                g.setAlpha(alpha);
            } else {
                Look.glow(g, grid.getFgColor(), Math.max(x + rect[0], cx - Look.mm(2)), y + rect[1],
                        x + rect[0] + rect[2] - Math.max(x + rect[0], cx - Look.mm(2)), rect[3], over);
            }
        }
        if (mode != 0) {
            int x0 = x + axis.x(editStart);
            int x1 = x + axis.x(editEnd);
            int w = Math.max(Look.mm(1.5f), x1 - x0);
            if (mode == RANGE) {
                Style sel = Look.style("SchedulerSelection");
                int top = y + rowTop[editRow] + 1;
                int h = rowTop[editRow + 1] - rowTop[editRow] - 1;
                Look.fill(g, sel, x0, top, w, h);
                g.setColor(sel.getFgColor());
                g.drawRect(x0, top, w - 1, h - 1);
            } else {
                Look.shadow(g, x0, y + rowTop[editRow] + rowPad, w, laneH - 1, liftLevel());
                renderer.paintItem(g, owner, editItem, x0, y + rowTop[editRow] + rowPad, w, laneH - 1,
                        ScheduleItemRenderer.DRAGGED | ScheduleItemRenderer.SELECTED | ScheduleItemRenderer.HORIZONTAL
                                | ScheduleItemRenderer.COMPACT);
            }
            Style sel = Look.style("SchedulerSelection");
            Font f = sel.getFont();
            g.setFont(f);
            g.setColor(grid.getFgColor());
            g.drawString(owner.formatRange(editStart, editEnd), Math.max(cx + 2, x0),
                    Math.max(cy, y + rowTop[editRow] - f.getHeight()));
        }
        long now = owner.now();
        if (now >= axis.getStart() && now < axis.getEnd()) {
            g.setColor(Look.style("SchedulerNow").getFgColor());
            g.fillRect(x + axis.x(now) - 1, cy, 2, ch);
        }
    }

    @Override
    void paintTopHeader(Graphics g, int x, int y) {
        axis.paintHeader(g, Look.style("SchedulerHeader"), x, y, g.getClipX(), g.getClipX() + g.getClipWidth());
    }

    @Override
    void paintLeftHeader(Graphics g, int x, int y) {
        Style header = Look.style("SchedulerHeader");
        Font f = header.getFont();
        int fh = f.getHeight();
        Look.fill(g, header, x, g.getClipY(), headerW, g.getClipHeight());
        g.setFont(f);
        int line = Look.mix(header.getBgColor(), header.getFgColor(), 50);
        int pad = Look.mm(1.5f);
        int alpha = g.getAlpha();
        for (int r = 0; r < resources.length; r++) {
            int top = y + rowTop[r];
            int h = rowTop[r + 1] - rowTop[r];
            ScheduleResource res = resources[r];
            boolean two = res.getDetail() != null && res.getDetail().length() > 0;
            int ty = top + (h - (two ? 2 * fh : fh)) / 2;
            g.setColor(header.getFgColor());
            g.drawString(Look.fit(res.getName(), f, headerW - 2 * pad), x + pad, ty);
            if (two) {
                g.setAlpha(alpha * 160 / 255);
                g.drawString(Look.fit(res.getDetail(), f, headerW - 2 * pad), x + pad, ty + fh);
                g.setAlpha(alpha);
            }
            g.setColor(line);
            g.drawLine(x, top + h, x + headerW, top + h);
        }
        g.setColor(line);
        g.drawLine(x + headerW - 1, g.getClipY(), x + headerW - 1, g.getClipY() + g.getClipHeight());
    }

    @Override
    void paintCorner(Graphics g, int x, int y) {
        Style header = Look.style("SchedulerHeader");
        Look.fill(g, header, x, y, headerW, headerH);
        g.setColor(Look.mix(header.getBgColor(), header.getFgColor(), 50));
        g.drawLine(x, y + headerH - 1, x + headerW, y + headerH - 1);
        g.drawLine(x + headerW - 1, y, x + headerW - 1, y + headerH);
    }

    // ------------------------------------------------------------ pointer

    Bar barAt(int px, int py) {
        if (px < headerW || py < headerH) {
            return null;
        }
        int cx = px - headerW + getOffX();
        int cy = py - headerH + getOffY();
        for (int i = bars.size() - 1; i >= 0; i--) {
            Bar b = bars.get(i);
            barRect(b, rect);
            if (cx >= rect[0] && cx < rect[0] + rect[2] && cy >= rect[1] && cy < rect[1] + rect[3]) {
                return b;
            }
        }
        return null;
    }

    private long snap(long time) {
        ZoneId zone = owner.getZone();
        if (axis.snapsToDays()) {
            return TimeMath.startOfDay(time + TimeMath.DAY / 2, zone);
        }
        int step = Math.max(1, owner.getSnapMinutes());
        long day = TimeMath.epochDay(time, zone);
        int m = TimeMath.minuteOfDay(time, zone);
        return TimeMath.at(day, Math.min(TimeMath.MINUTES_PER_DAY, (m + step / 2) / step * step), zone);
    }

    private long step() {
        return axis.snapsToDays() ? TimeMath.DAY : Math.max(1, owner.getSnapMinutes()) * TimeMath.MINUTE;
    }

    @Override
    boolean beginEdit(int x, int y, boolean handlesOnly) {
        if (!owner.isEditable() || x < headerW || y < headerH) {
            return false;
        }
        int cx = x - headerW + getOffX();
        int cy = y - headerH + getOffY();
        Bar b = barAt(x, y);
        if (b != null) {
            barRect(b, rect);
            int edge = Math.min(rect[2] / 3, Look.mm(handlesOnly ? 3f : 1.6f));
            boolean right = rect[0] + rect[2] - cx <= edge;
            boolean left = !right && cx - rect[0] <= edge;
            if (handlesOnly && (b.item != owner.getSelectedItem() || !(left || right))) { //NOPMD CompareObjectsWithEquals
                return false;
            }
            if ((left || right) && b.item.isResizable()) {
                mode = right ? RESIZE_END : RESIZE_START;
            } else if (!handlesOnly && b.item.isMovable()) {
                mode = MOVE;
            } else {
                return false;
            }
            editItem = b.item;
            editStart = b.item.getStart();
            editEnd = b.item.getEnd();
            editRow = b.row;
            grabTime = axis.time(cx);
            owner.viewSelected(b.item);
            return true;
        }
        if (handlesOnly) {
            return false;
        }
        if (resources.length > 0 && cy >= rowTop[rowCount]) {
            // Under the last row there is nobody to book.
            return false;
        }
        mode = RANGE;
        editItem = null;
        editRow = rowAt(cy);
        anchorTime = snap(axis.time(cx) - step() / 2);
        editStart = anchorTime;
        editEnd = anchorTime + defaultLength();
        return true;
    }

    // The row a key of `hoverAt` names, lit from edge to edge.
    private void paintRowHover(Graphics g, int y, int cx, int cw, Object key, int rgb) {
        if (!(key instanceof Integer)) {
            return;
        }
        int r = ((Integer) key).intValue();
        if (r >= 0 && r < rowCount) {
            Look.wash(g, rgb, cx, y + rowTop[r] + 1, cw, rowTop[r + 1] - rowTop[r] - 1, hoverLevel(key));
        }
    }

    @Override
    void hoverAt(int x, int y) {
        if (x < headerW || y < headerH) {
            hover(null, DEFAULT_CURSOR);
            return;
        }
        int cx = x - headerW + getOffX();
        int cy = y - headerH + getOffY();
        Bar b = barAt(x, y);
        if (b != null) {
            int cursor = HAND_CURSOR;
            if (owner.isEditable() && isDragEdits()) {
                barRect(b, rect);
                int edge = Math.min(rect[2] / 3, Look.mm(1.6f));
                boolean right = rect[0] + rect[2] - cx <= edge;
                boolean left = !right && cx - rect[0] <= edge;
                if ((left || right) && b.item.isResizable()) {
                    cursor = left ? W_RESIZE_CURSOR : E_RESIZE_CURSOR;
                } else if (b.item.isMovable()) {
                    cursor = MOVE_CURSOR;
                }
            }
            hover(b.item.getId(), cursor);
            tip(b.item.getTitle() + ", " + owner.formatRange(b.item.getStart(), b.item.getEnd()));
            return;
        }
        tip(null);
        // The row a drag or a double click would book.
        if (rowCount > 0 && cy < rowTop[rowCount]) {
            hover(Integer.valueOf(rowAt(cy)), DEFAULT_CURSOR);
        } else {
            hover(null, DEFAULT_CURSOR);
        }
    }

    private long defaultLength() {
        return axis.snapsToDays() ? TimeMath.DAY : owner.getDefaultMinutes() * TimeMath.MINUTE;
    }

    @Override
    void editDragged(int x, int y) {
        if (mode == 0) {
            return;
        }
        int cx = Math.max(0, Math.min(contentWidth(), x - headerW + getOffX()));
        int cy = Math.max(0, y - headerH + getOffY());
        long pointer = axis.time(cx);
        long step = step();
        if (mode == MOVE) {
            if (axis.snapsToDays()) {
                // Whole days, at the hour it had: a booking at ten dragged to
                // another row or another day is still at ten.
                ZoneId zone = owner.getZone();
                long delta = pointer - grabTime;
                long days = (delta >= 0 ? delta + TimeMath.DAY / 2 : delta - TimeMath.DAY / 2) / TimeMath.DAY;
                editStart = TimeMath.at(TimeMath.epochDay(editItem.getStart(), zone) + days,
                        TimeMath.minuteOfDay(editItem.getStart(), zone), zone);
                editEnd = TimeMath.at(TimeMath.epochDay(editItem.getEnd(), zone) + days,
                        TimeMath.minuteOfDay(editItem.getEnd(), zone), zone);
            } else {
                long length = editItem.getEnd() - editItem.getStart();
                editStart = snap(editItem.getStart() + pointer - grabTime);
                editEnd = editStart + length;
            }
            if (resources.length > 0) {
                editRow = rowAt(cy);
            }
        } else if (mode == RESIZE_END) {
            editEnd = Math.max(editStart + step, snap(pointer));
        } else if (mode == RESIZE_START) {
            editStart = Math.min(editEnd - step, snap(pointer));
        } else {
            long p = snap(pointer);
            if (p > anchorTime) {
                editStart = anchorTime;
                editEnd = p;
            } else if (p < anchorTime) {
                editStart = p;
                editEnd = anchorTime + step;
            } else {
                editStart = anchorTime;
                editEnd = anchorTime + defaultLength();
            }
        }
    }

    @Override
    void endEdit(boolean commit) {
        int m = mode;
        ScheduleItem item = editItem;
        if (item != null && editRow < rowCount) {
            // From where it was let go to where it belongs, which is the
            // place it came from when the move is refused or given up.
            int x0 = axis.x(editStart);
            settleFrom(item.getId(), x0, rowTop[editRow] + rowPad, Math.max(Look.mm(1.5f), axis.x(editEnd) - x0),
                    laneH - 1);
        }
        mode = 0;
        editItem = null;
        if (!commit) {
            return;
        }
        // The model may have lost a resource while this was dragged.
        String res = resources.length > 0 ? resources[Math.min(editRow, resources.length - 1)].getId()
                : item == null ? null : item.getResourceId();
        if (m == RANGE) {
            owner.viewRange(editStart, editEnd, res);
        } else if (item != null && (editStart != item.getStart() || editEnd != item.getEnd()
                || !(res == null ? item.getResourceId() == null : res.equals(item.getResourceId())))) {
            owner.viewChanged(item, editStart, editEnd, res);
        }
    }

    @Override
    void tapped(int x, int y, boolean doubleTap) {
        if (x < headerW || y < headerH) {
            return;
        }
        Bar b = barAt(x, y);
        if (b != null) {
            owner.viewSelected(b.item);
            if (doubleTap || owner.isOpenOnTap()) {
                owner.viewOpened(b.item);
            }
            return;
        }
        if (owner.getSelectedItem() != null) {
            owner.viewSelected(null);
        } else if (doubleTap && owner.isEditable()) {
            long start = snap(axis.time(x - headerW + getOffX()) - step() / 2);
            int row = rowAt(y - headerH + getOffY());
            owner.viewRange(start, start + defaultLength(), resources.length > 0 ? resources[row].getId() : null);
        }
    }

    @Override
    boolean zoomBy(float factor, int x, int y) {
        double before = axis.getMillisPerPixel();
        long t = axis.time(x - headerW + getOffX());
        axis.setMillisPerPixel(before / factor);
        if (Math.abs(axis.getMillisPerPixel() - before) < before * 0.0001) {
            return false;
        }
        laidW = -1;
        setOffsets(axis.x(t) - (x - headerW), getOffY());
        repaint();
        return true;
    }
}
