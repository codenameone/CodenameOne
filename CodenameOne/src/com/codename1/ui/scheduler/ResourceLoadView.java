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

import com.codename1.scheduling.ProjectModel;
import com.codename1.scheduling.ResourceLeveler;
import com.codename1.scheduling.ScheduleResource;
import com.codename1.scheduling.TimeMath;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;

import java.util.List;

/// How much of each resource a project asks for over time, under the chart
/// of a `Gantt` and on the same time axis: a row per resource, a bar per
/// stretch of time as tall as the load in it, and the ones over the
/// resource's capacity in another colour.
final class ResourceLoadView extends ScrollSurface {
    private final Gantt owner;
    private final GanttChart chart;
    private int rowH;
    private boolean syncing;

    // The profile last worked out, and what it was worked out for.
    private float[][] profile;
    private long profileFrom;
    private long profileBucket;
    private int profileCount;
    private boolean stale = true;

    ResourceLoadView(Gantt owner, GanttChart chart) {
        this.owner = owner;
        this.chart = chart;
        setUIID("Gantt");
    }

    void modelChanged() {
        stale = true;
        repaint();
    }

    /// The chart was laid out again or changed its scale.
    void chartChanged() {
        stale = true;
        chartScrolled();
    }

    void chartScrolled() {
        if (!syncing) {
            setOffsets(chart.getOffX(), getOffY());
            repaint();
        }
    }

    int getRowHeight() {
        return rowH;
    }

    /// The load of a resource in the stretch of time at a point of the axis,
    /// as last painted, or -1 when nothing was.
    float loadAt(int resourceIndex, long time) {
        if (profile == null || resourceIndex < 0 || resourceIndex >= profile.length) {
            return -1;
        }
        long i = (time - profileFrom) / profileBucket;
        float[] row = profile[resourceIndex];
        return row == null || i < 0 || i >= profileCount ? -1 : row[(int) i];
    }

    @Override
    void prepare(int width, int height) {
        Style row = Look.style("GanttRow");
        rowH = Math.max(Look.mm(7), row.getFont().getHeight() * 2 + Look.mm(1.5f));
        headerW = chart.headerW;
        headerH = 0;
        // The chart is the one that knows where time is.
        setOffsets(chart.getOffX(), getOffY());
    }

    @Override
    int contentWidth() {
        return chart.axis().width();
    }

    @Override
    int contentHeight() {
        ProjectModel model = owner.getModel();
        return model == null ? 0 : model.getResources().size() * rowH;
    }

    @Override
    protected Dimension calcPreferredSize() {
        ProjectModel model = owner.getModel();
        int rows = model == null ? 0 : model.getResources().size();
        Style row = Look.style("GanttRow");
        int h = Math.max(Look.mm(7), row.getFont().getHeight() * 2 + Look.mm(1.5f));
        // Three rows and a part of the fourth, which says there are more.
        return new Dimension(Look.mm(60), Math.min(rows * h, h * 7 / 2));
    }

    @Override
    void scrolled() {
        syncing = true;
        chart.scrollTo(getOffX(), chart.getOffY());
        syncing = false;
    }

    @Override
    boolean zoomBy(float factor, int x, int y) {
        return chart.zoomBy(factor, x, y);
    }

    // A bucket is a day while a day is wide enough to see, then a week, then
    // four of them.
    private long bucketSize() {
        double mpp = chart.axis().getMillisPerPixel();
        int least = Math.max(3, Look.mm(0.9f));
        if (TimeMath.DAY / mpp >= least) {
            return TimeMath.DAY;
        }
        if (7 * TimeMath.DAY / mpp >= least) {
            return 7 * TimeMath.DAY;
        }
        return 28 * TimeMath.DAY;
    }

    private void profile(List<ScheduleResource> resources, int left, int right) {
        TimeAxis axis = chart.axis();
        long bucket = bucketSize();
        long from = TimeMath.startOfDay(axis.time(Math.max(0, left)), axis.getZone());
        if (bucket > TimeMath.DAY) {
            // Whole buckets from the start of the axis, so that scrolling
            // does not move their edges.
            long first = TimeMath.startOfDay(axis.getStart(), axis.getZone());
            from = first + (from - first) / bucket * bucket;
        }
        int count = (int) ((axis.time(right) - from) / bucket) + 1;
        if (!stale && profile != null && profile.length == resources.size() && from == profileFrom
                && bucket == profileBucket && count == profileCount) {
            return;
        }
        // A row's load is worked out when the row is first painted: a
        // project has more resources than a screen has rows.
        profile = new float[resources.size()][];
        profileFrom = from;
        profileBucket = bucket;
        profileCount = count;
        stale = false;
    }

    @Override
    void paintBody(Graphics g, int x, int y) {
        Style row = Look.style("GanttRow");
        Style loadStyle = Look.style("GanttLoad");
        Style over = Look.style("GanttOverload");
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        Look.fill(g, row, cx, cy, cw, ch);
        ProjectModel model = owner.getModel();
        if (model == null || rowH <= 0) {
            return;
        }
        List<ScheduleResource> resources = model.getResources();
        if (resources.isEmpty()) {
            return;
        }
        TimeAxis axis = chart.axis();
        axis.paintOffHours(g, model.getCalendar(), Look.style("SchedulerOffHours"), x, cy, ch, cx, cx + cw);
        axis.paintGrid(g, Look.mix(row.getBgColor(), row.getFgColor(), 45), row.getBgColor(), x, cy, ch, cx, cx + cw);
        profile(resources, cx - x, cx + cw - x);
        int firstRow = Math.max(0, (cy - y) / rowH);
        int lastRow = Math.min(resources.size() - 1, (cy + ch - y) / rowH);
        int pad = Math.max(1, Look.mm(0.6f));
        int line = Look.mix(row.getBgColor(), row.getFgColor(), 25);
        int capColor = Look.mix(row.getBgColor(), row.getFgColor(), 110);
        for (int r = firstRow; r <= lastRow; r++) {
            ScheduleResource res = resources.get(r);
            float capacity = res.getCapacity();
            float[] units = profile[r];
            if (units == null) {
                units = ResourceLeveler.loadProfile(model, res.getId(), profileFrom,
                        profileFrom + profileCount * profileBucket, profileCount);
                profile[r] = units;
            }
            float peak = capacity;
            for (float unit : units) {
                peak = Math.max(peak, unit);
            }
            // The capacity is always at the same height unless the load
            // goes over it by more than half, and then the row is the peak.
            float top = Math.max(capacity * 1.5f, peak);
            int base = y + (r + 1) * rowH - 1;
            int room = rowH - 2 * pad - 1;
            for (int i = 0; i < units.length; i++) {
                if (units[i] <= 0) {
                    continue;
                }
                int x0 = x + axis.x(profileFrom + i * profileBucket);
                int x1 = x + axis.x(profileFrom + (i + 1) * profileBucket);
                int h = Math.max(1, Math.round(room * units[i] / top));
                boolean tooMuch = units[i] > capacity + 0.001f;
                Look.fill(g, tooMuch ? over : loadStyle, x0, base - h, Math.max(1, x1 - x0 - 1), h);
            }
            int capY = base - Math.round(room * capacity / top);
            g.setColor(capColor);
            for (int dx = cx - (cx - x) % 6; dx < cx + cw; dx += 6) {
                g.drawLine(dx, capY, dx + 2, capY);
            }
            g.setColor(line);
            g.drawLine(cx, base, cx + cw, base);
            if (headerW == 0) {
                // No table to name the row in.
                Font f = row.getFont();
                g.setFont(f);
                g.setColor(row.getFgColor());
                g.drawString(res.getName(), cx + Look.mm(1), y + r * rowH + pad);
            }
        }
        long now = owner.now();
        if (now >= axis.getStart() && now < axis.getEnd()) {
            g.setColor(Look.style("SchedulerNow").getFgColor());
            g.fillRect(x + axis.x(now) - 1, cy, 2, ch);
        }
    }

    @Override
    void paintLeftHeader(Graphics g, int x, int y) {
        Style row = Look.style("GanttRow");
        Style over = Look.style("GanttOverload");
        int cy = g.getClipY();
        int ch = g.getClipHeight();
        Look.fill(g, row, x, cy, headerW, ch);
        ProjectModel model = owner.getModel();
        if (model == null || rowH <= 0) {
            return;
        }
        List<ScheduleResource> resources = model.getResources();
        Font f = row.getFont();
        int pad = Look.mm(1.2f);
        int firstRow = Math.max(0, (cy - y) / rowH);
        int lastRow = Math.min(resources.size() - 1, (cy + ch - y) / rowH);
        int line = Look.mix(row.getBgColor(), row.getFgColor(), 25);
        int alpha = g.getAlpha();
        g.setFont(f);
        for (int r = firstRow; r <= lastRow; r++) {
            ScheduleResource res = resources.get(r);
            int top = y + r * rowH;
            boolean tooMuch = owner.isOverallocated(res.getId());
            String detail = res.getDetail();
            if (tooMuch) {
                detail = owner.localize("gantt.overallocated", "Overallocated");
            }
            int textH = detail == null ? f.getHeight() : 2 * f.getHeight();
            int ty = top + (rowH - textH) / 2;
            g.setColor(row.getFgColor());
            g.drawString(Look.fit(res.getName(), f, headerW - 2 * pad), x + pad, ty);
            if (detail != null) {
                if (tooMuch) {
                    g.setColor(over.getBgColor());
                } else {
                    g.setAlpha(alpha * 150 / 255);
                }
                g.drawString(Look.fit(detail, f, headerW - 2 * pad), x + pad, ty + f.getHeight());
                g.setAlpha(alpha);
            }
            g.setColor(line);
            g.drawLine(x, top + rowH - 1, x + headerW, top + rowH - 1);
        }
        g.setColor(Look.mix(row.getBgColor(), row.getFgColor(), 60));
        g.drawLine(x + headerW - 1, cy, x + headerW - 1, cy + ch);
    }
}
