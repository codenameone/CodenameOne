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
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/// What is scheduled as a list, day by day, with the days that have nothing
/// left out.
final class AgendaList extends ScrollSurface {
    /// A line of the list: a day's heading when `item` is null.
    static final class Row {
        long day;
        ScheduleItem item;
        int top;
        int height;
    }

    private static final Comparator<ScheduleItem> ORDER = new Comparator<ScheduleItem>() {
        @Override
        public int compare(ScheduleItem a, ScheduleItem b) {
            if (a.isAllDay() != b.isAllDay()) {
                return a.isAllDay() ? -1 : 1;
            }
            if (a.getStart() != b.getStart()) {
                return a.getStart() < b.getStart() ? -1 : 1;
            }
            return a.getId().compareTo(b.getId());
        }
    };

    private final Scheduler owner;
    private long firstDay;
    private int count = 30;
    private boolean dirty = true;
    private int laidW = -1;
    private int laidGen = -1;
    private int total;
    private int timeW;
    private final ArrayList<Row> rows = new ArrayList<Row>();
    private final ArrayList<ScheduleItem> loaded = new ArrayList<ScheduleItem>();

    AgendaList(Scheduler owner) {
        this.owner = owner;
        setUIID("Scheduler");
    }

    void show(long day, int days) {
        firstDay = day;
        count = days;
        dirty = true;
        setOffsets(0, 0);
        repaint();
    }

    void modelChanged() {
        dirty = true;
        repaint();
    }

    List<Row> rows() {
        return rows;
    }

    @Override
    void prepare(int width, int height) {
        int gen = UIManager.getThemeGeneration();
        if (!dirty && width == laidW && gen == laidGen) {
            return;
        }
        dirty = false;
        laidW = width;
        laidGen = gen;
        headerW = 0;
        headerH = 0;
        Font hf = Look.style("SchedulerHeader").getFont();
        Font f = Look.style("SchedulerAgendaItem").getFont();
        int headH = hf.getHeight() + Look.mm(3);
        int itemH = f.getHeight() * 2 + Look.mm(3);
        boolean h24 = owner.is24Hours();
        timeW = Math.max(f.stringWidth(Dates.time(22 * 60 + 30, h24, false)),
                f.stringWidth(owner.localize("scheduler.allDay", "All day"))) + Look.mm(3);
        rows.clear();
        loaded.clear();
        total = 0;
        if (owner.getModel() == null) {
            return;
        }
        ZoneId zone = owner.getZone();
        owner.getModel().getItems(TimeMath.startOfEpochDay(firstDay, zone),
                TimeMath.startOfEpochDay(firstDay + count, zone), loaded);
        Collections.sort(loaded, ORDER);
        for (int d = 0; d < count; d++) {
            long day = firstDay + d;
            long ds = TimeMath.startOfEpochDay(day, zone);
            long de = TimeMath.startOfEpochDay(day + 1, zone);
            boolean headed = false;
            for (ScheduleItem item : loaded) {
                if (item.getKind() != ScheduleItem.Kind.EVENT) {
                    continue;
                }
                boolean instant = item.getStart() == item.getEnd() && item.getStart() >= ds && item.getStart() < de;
                if (!instant && !item.intersects(ds, de)) {
                    continue;
                }
                if (!headed) {
                    headed = true;
                    add(day, null, headH);
                }
                add(day, item, itemH);
            }
        }
    }

    private void add(long day, ScheduleItem item, int height) {
        Row r = new Row();
        r.day = day;
        r.item = item;
        r.top = total;
        r.height = height;
        total += height;
        rows.add(r);
    }

    @Override
    int contentWidth() {
        return bodyWidth();
    }

    @Override
    int contentHeight() {
        return total;
    }

    @Override
    void paintBody(Graphics g, int x, int y) {
        Style grid = Look.style("SchedulerGrid");
        Style header = Look.style("SchedulerHeader");
        Style st = Look.style("SchedulerAgendaItem");
        Style sel = Look.style("SchedulerSelection");
        Font hf = header.getFont();
        Font f = st.getFont();
        int fh = f.getHeight();
        int cy = g.getClipY();
        int ch = g.getClipHeight();
        int w = bodyWidth();
        Look.fill(g, grid, g.getClipX(), cy, g.getClipWidth(), ch);
        ZoneId zone = owner.getZone();
        if (rows.isEmpty()) {
            String none = owner.localize("scheduler.empty", "Nothing scheduled");
            g.setFont(f);
            g.setColor(st.getFgColor());
            g.drawString(none, x + (w - f.stringWidth(none)) / 2, y + Look.mm(8));
            return;
        }
        long today = TimeMath.epochDay(owner.now(), zone);
        boolean h24 = owner.is24Hours();
        ScheduleItem selected = owner.getSelectedItem();
        int line = Look.mix(grid.getBgColor(), grid.getFgColor(), 110);
        int pad = Look.mm(1.5f);
        int alpha = g.getAlpha();
        for (Row r : rows) {
            int top = y + r.top;
            if (top > cy + ch || top + r.height < cy) {
                continue;
            }
            if (r.item == null) {
                Look.fill(g, header, x, top, w, r.height);
                g.setFont(hf);
                g.setColor(r.day == today ? Look.style("SchedulerNow").getFgColor() : header.getFgColor());
                String title = Dates.dayTitle(r.day, TimeMath.dayOfWeek(r.day));
                if (r.day == today) {
                    title = owner.localize("scheduler.today", "Today") + " - " + title;
                }
                g.drawString(Look.fit(title, hf, w - 2 * pad), x + pad, top + (r.height - hf.getHeight()) / 2);
                continue;
            }
            ScheduleItem item = r.item;
            if (item == selected) { //NOPMD CompareObjectsWithEquals - the selection is this very object
                Look.fill(g, sel, x, top, w, r.height);
            }
            long ds = TimeMath.startOfEpochDay(r.day, zone);
            long de = TimeMath.startOfEpochDay(r.day + 1, zone);
            int ty = top + (r.height - 2 * fh) / 2;
            g.setFont(f);
            g.setColor(st.getFgColor());
            if (item.isAllDay() || (item.getStart() <= ds && item.getEnd() >= de)) {
                g.drawString(owner.localize("scheduler.allDay", "All day"), x + pad, ty + fh / 2);
            } else {
                g.drawString(Dates.time(item.getStart() < ds ? 0 : TimeMath.minuteOfDay(item.getStart(), zone), h24,
                        false), x + pad, ty);
                g.setAlpha(alpha * 160 / 255);
                g.drawString(Dates.time(item.getEnd() > de ? 0 : TimeMath.minuteOfDay(item.getEnd(), zone), h24,
                        false), x + pad, ty + fh);
                g.setAlpha(alpha);
            }
            int own = item.getColor() >= 0 ? item.getColor() : owner.resourceColor(item.getResourceId());
            int bx = x + pad + timeW;
            int bar = Look.mm(1);
            boolean aa = g.isAntiAliased();
            g.setAntiAliased(true);
            g.setColor(own >= 0 ? own : Look.style(item.getUIID() != null ? item.getUIID() : "SchedulerItem")
                    .getBgColor());
            g.fillRoundRect(bx, top + pad, bar, r.height - 2 * pad, bar, bar);
            g.setAntiAliased(aa);
            int tx = bx + bar + pad;
            int tw = x + w - pad - tx;
            g.setColor(st.getFgColor());
            String second = item.getDetail();
            if (second == null || second.length() == 0) {
                ScheduleResource res = owner.getResource(item.getResourceId());
                second = res == null ? null : res.getName();
            }
            if (second == null || second.length() == 0) {
                g.drawString(Look.fit(item.getTitle(), f, tw), tx, ty + fh / 2);
            } else {
                g.drawString(Look.fit(item.getTitle(), f, tw), tx, ty);
                g.setAlpha(alpha * 160 / 255);
                g.drawString(Look.fit(second, f, tw), tx, ty + fh);
                g.setAlpha(alpha);
            }
            g.setColor(line);
            g.drawLine(tx, top + r.height - 1, x + w, top + r.height - 1);
        }
    }

    Row rowAt(int py) {
        int cy = py + getOffY();
        for (Row r : rows) {
            if (cy >= r.top && cy < r.top + r.height) {
                return r;
            }
        }
        return null;
    }

    @Override
    void tapped(int x, int y, boolean doubleTap) {
        Row r = rowAt(y);
        if (r == null) {
            return;
        }
        if (r.item == null) {
            owner.viewDrill(r.day);
            return;
        }
        owner.viewSelected(r.item);
        if (doubleTap || owner.isOpenOnTap()) {
            owner.viewOpened(r.item);
        }
    }
}
