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
import com.codename1.scheduling.TimeMath;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/// A month as weeks of day cells, each listing what is on that day and
/// counting what does not fit.
final class MonthGrid extends ScrollSurface {
    private static final int MOVE = 1;
    private static final int RANGE = 4;

    private static final Comparator<ScheduleItem> ORDER = new Comparator<ScheduleItem>() {
        @Override
        public int compare(ScheduleItem a, ScheduleItem b) {
            boolean la = a.isAllDay() || a.getEnd() - a.getStart() >= TimeMath.DAY;
            boolean lb = b.isAllDay() || b.getEnd() - b.getStart() >= TimeMath.DAY;
            if (la != lb) {
                return la ? -1 : 1;
            }
            if (a.getStart() != b.getStart()) {
                return a.getStart() < b.getStart() ? -1 : 1;
            }
            return a.getId().compareTo(b.getId());
        }
    };

    private final Scheduler owner;
    private long firstDay;
    private long monthStart;
    private long monthEnd;
    private int weeks = 5;
    private boolean dirty = true;
    private int laidW = -1;
    private int laidH = -1;
    private int laidGen = -1;
    private int numberH;
    private int chipH;
    private int capacity;
    private ArrayList<ScheduleItem>[] cells;
    private final ArrayList<ScheduleItem> loaded = new ArrayList<ScheduleItem>();

    private int mode;
    private ScheduleItem editItem;
    private int grabCell;
    private int anchorCell;
    private int editCell;

    MonthGrid(Scheduler owner) {
        this.owner = owner;
        setUIID("Scheduler");
    }

    /// Shows the month `day` is in.
    void show(long day) {
        monthStart = TimeMath.startOfMonth(day);
        monthEnd = TimeMath.plusMonths(monthStart, 1);
        firstDay = TimeMath.startOfWeek(monthStart, owner.getFirstDayOfWeek());
        weeks = (int) ((monthEnd - firstDay + 6) / 7);
        dirty = true;
        repaint();
    }

    long firstDay() {
        return firstDay;
    }

    int dayCount() {
        return weeks * 7;
    }

    void modelChanged() {
        dirty = true;
        repaint();
    }

    List<ScheduleItem> itemsOn(long day) {
        int i = (int) (day - firstDay);
        return cells == null || i < 0 || i >= cells.length ? null : cells[i];
    }

    @Override
    @SuppressWarnings("unchecked")
    void prepare(int width, int height) {
        int gen = UIManager.getThemeGeneration();
        if (!dirty && width == laidW && height == laidH && gen == laidGen) {
            return;
        }
        laidW = width;
        laidH = height;
        laidGen = gen;
        Style header = Look.style("SchedulerHeader");
        Font nf = Look.style("SchedulerMonthDay").getFont();
        headerW = 0;
        headerH = header.getFont().getHeight() + Look.mm(2);
        numberH = nf.getHeight() + Look.mm(1.6f);
        chipH = Look.style("SchedulerItem").getFont().getHeight() + Look.mm(0.8f);
        int cellH = Math.max(1, (height - headerH) / weeks);
        capacity = Math.max(0, (cellH - numberH) / chipH);
        if (dirty) {
            dirty = false;
            int n = weeks * 7;
            cells = new ArrayList[n];
            for (int i = 0; i < n; i++) {
                cells[i] = new ArrayList<ScheduleItem>();
            }
            loaded.clear();
            if (owner.getModel() != null) {
                ZoneId zone = owner.getZone();
                owner.getModel().getItems(TimeMath.startOfEpochDay(firstDay, zone),
                        TimeMath.startOfEpochDay(firstDay + n, zone), loaded);
                for (ScheduleItem item : loaded) {
                    if (item.getKind() != ScheduleItem.Kind.EVENT) {
                        continue;
                    }
                    long s = TimeMath.epochDay(item.getStart(), zone);
                    long e = TimeMath.epochDay(Math.max(item.getStart(), item.getEnd() - 1), zone);
                    for (long d = Math.max(s, firstDay); d <= e && d < firstDay + n; d++) {
                        cells[(int) (d - firstDay)].add(item);
                    }
                }
                for (int i = 0; i < n; i++) {
                    Collections.sort(cells[i], ORDER);
                }
            }
        }
    }

    @Override
    int contentWidth() {
        return bodyWidth();
    }

    @Override
    int contentHeight() {
        return bodyHeight();
    }

    private int colX(int col) {
        return col * bodyWidth() / 7;
    }

    private int rowY(int row) {
        return row * bodyHeight() / weeks;
    }

    private int cellAt(int px, int py) {
        int col = Math.max(0, Math.min(6, px * 7 / Math.max(1, bodyWidth())));
        int row = Math.max(0, Math.min(weeks - 1, (py - headerH) * weeks / Math.max(1, bodyHeight())));
        return row * 7 + col;
    }

    // How many items of a cell are drawn as chips; the rest are counted.
    private int shown(int cell) {
        int n = cells[cell].size();
        return n <= capacity ? n : Math.max(0, capacity - 1);
    }

    private ScheduleItem itemAt(int px, int py) {
        if (py < headerH || cells == null) {
            return null;
        }
        int cell = cellAt(px, py);
        int top = headerH + rowY(cell / 7) + numberH;
        if (py < top) {
            return null;
        }
        int i = (py - top) / chipH;
        return i < shown(cell) ? cells[cell].get(i) : null;
    }

    @Override
    void paintBody(Graphics g, int x, int y) {
        Style grid = Look.style("SchedulerGrid");
        Style dayStyle = Look.style("SchedulerMonthDay");
        Style other = Look.style("SchedulerMonthOther");
        Style todaySt = Look.style("SchedulerHeaderToday");
        Style sel = Look.style("SchedulerSelection");
        Font nf = dayStyle.getFont();
        int bw = bodyWidth();
        int bh = bodyHeight();
        Look.fill(g, grid, x, y, bw, bh);
        ZoneId zone = owner.getZone();
        long today = TimeMath.epochDay(owner.now(), zone);
        ScheduleItemRenderer renderer = owner.getItemRenderer();
        ScheduleItem selected = owner.getSelectedItem();
        int line = Look.mix(grid.getBgColor(), grid.getFgColor(), 150);
        int lo = Math.min(anchorCell, editCell);
        int hi = Math.max(anchorCell, editCell);
        for (int cell = 0; cell < weeks * 7; cell++) {
            long day = firstDay + cell;
            int x0 = x + colX(cell % 7);
            int x1 = x + colX(cell % 7 + 1);
            int y0 = y + rowY(cell / 7);
            int y1 = y + rowY(cell / 7 + 1);
            boolean inMonth = day >= monthStart && day < monthEnd;
            if (!inMonth) {
                Look.fill(g, other, x0, y0, x1 - x0, y1 - y0);
            }
            if ((mode == MOVE && cell == editCell) || (mode == RANGE && cell >= lo && cell <= hi)) {
                Look.fill(g, sel, x0, y0, x1 - x0, y1 - y0);
            }
            String num = String.valueOf(Dates.dayOfMonth(day));
            int nw = nf.stringWidth(num);
            int nx = x0 + Look.mm(1.2f);
            int ny = y0 + Look.mm(0.8f);
            g.setFont(nf);
            if (day == today) {
                int d = nf.getHeight() + Look.mm(1);
                boolean aa = g.isAntiAliased();
                g.setAntiAliased(true);
                g.setColor(todaySt.getBgColor());
                g.fillArc(nx + nw / 2 - d / 2, ny + nf.getHeight() / 2 - d / 2, d, d, 0, 360);
                g.setAntiAliased(aa);
                g.setColor(todaySt.getFgColor());
            } else {
                g.setColor(inMonth ? dayStyle.getFgColor() : other.getFgColor());
            }
            g.drawString(num, nx, ny);
            List<ScheduleItem> list = cells[cell];
            int n = shown(cell);
            int cy = y0 + numberH;
            for (int i = 0; i < n; i++) {
                ScheduleItem item = list.get(i);
                int state = ScheduleItemRenderer.COMPACT;
                if (item == selected) { //NOPMD CompareObjectsWithEquals - the selection is this very object
                    state |= ScheduleItemRenderer.SELECTED;
                }
                if (TimeMath.epochDay(item.getStart(), zone) < day) {
                    state |= ScheduleItemRenderer.STARTS_BEFORE;
                }
                if (TimeMath.epochDay(Math.max(item.getStart(), item.getEnd() - 1), zone) > day) {
                    state |= ScheduleItemRenderer.ENDS_AFTER;
                }
                renderer.paintItem(g, owner, item, x0 + 1, cy, x1 - x0 - 2, chipH - 1, state);
                cy += chipH;
            }
            if (list.size() > n) {
                g.setFont(nf);
                g.setColor(dayStyle.getFgColor());
                String more = "+" + (list.size() - n);
                if (capacity == 0) {
                    g.drawString(more, x1 - nf.stringWidth(more) - Look.mm(1), ny);
                } else {
                    g.drawString(more, x0 + Look.mm(1.2f), cy + (chipH - nf.getHeight()) / 2);
                }
            }
        }
        g.setColor(line);
        for (int c = 1; c < 7; c++) {
            g.drawLine(x + colX(c), y, x + colX(c), y + bh);
        }
        for (int r = 1; r < weeks; r++) {
            g.drawLine(x, y + rowY(r), x + bw, y + rowY(r));
        }
    }

    @Override
    void paintTopHeader(Graphics g, int x, int y) {
        Style header = Look.style("SchedulerHeader");
        Font f = header.getFont();
        Look.fill(g, header, x, y, bodyWidth(), headerH);
        g.setFont(f);
        g.setColor(header.getFgColor());
        for (int c = 0; c < 7; c++) {
            int w = colX(c + 1) - colX(c);
            String s = Look.fit(Dates.weekdayShort(TimeMath.dayOfWeek(firstDay + c)), f, w);
            g.drawString(s, x + colX(c) + (w - f.stringWidth(s)) / 2, y + (headerH - f.getHeight()) / 2);
        }
        g.setColor(Look.mix(header.getBgColor(), header.getFgColor(), 50));
        g.drawLine(x, y + headerH - 1, x + bodyWidth(), y + headerH - 1);
    }

    @Override
    boolean beginEdit(int x, int y, boolean handlesOnly) {
        if (handlesOnly || !owner.isEditable() || y < headerH || cells == null) {
            return false;
        }
        ScheduleItem item = itemAt(x, y);
        int cell = cellAt(x, y);
        if (item != null) {
            if (!item.isMovable()) {
                return false;
            }
            mode = MOVE;
            editItem = item;
            grabCell = cell;
            editCell = cell;
            owner.viewSelected(item);
            return true;
        }
        mode = RANGE;
        anchorCell = cell;
        editCell = cell;
        return true;
    }

    @Override
    void editDragged(int x, int y) {
        if (mode != 0) {
            editCell = cellAt(Math.max(0, Math.min(getWidth() - 1, x)), Math.max(headerH, Math.min(getHeight() - 1, y)));
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
        ZoneId zone = owner.getZone();
        if (m == RANGE) {
            long a = firstDay + Math.min(anchorCell, editCell);
            long b = firstDay + Math.max(anchorCell, editCell) + 1;
            owner.viewRange(TimeMath.startOfEpochDay(a, zone), TimeMath.startOfEpochDay(b, zone), null);
        } else if (item != null && editCell != grabCell) {
            // The item keeps its time of day and its length on the new date.
            long day = TimeMath.epochDay(item.getStart(), zone) + editCell - grabCell;
            long start = TimeMath.at(day, TimeMath.minuteOfDay(item.getStart(), zone), zone);
            owner.viewChanged(item, start, start + item.getEnd() - item.getStart(), item.getResourceId());
        }
    }

    @Override
    void tapped(int x, int y, boolean doubleTap) {
        if (y < headerH || cells == null) {
            return;
        }
        ScheduleItem item = itemAt(x, y);
        if (item != null) {
            owner.viewSelected(item);
            if (doubleTap || owner.isOpenOnTap()) {
                owner.viewOpened(item);
            }
        } else if (owner.getSelectedItem() != null) {
            owner.viewSelected(null);
        } else {
            owner.viewDrill(firstDay + cellAt(x, y));
        }
    }
}
