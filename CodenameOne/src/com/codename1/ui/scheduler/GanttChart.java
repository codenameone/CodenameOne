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

import com.codename1.scheduling.GanttTask;
import com.codename1.scheduling.ProjectModel;
import com.codename1.scheduling.ResourceAssignment;
import com.codename1.scheduling.ScheduleResource;
import com.codename1.scheduling.TaskDependency;
import com.codename1.scheduling.TimeMath;
import com.codename1.scheduling.WorkCalendar;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/// The chart of a `Gantt`: the table of tasks pinned on the left, and beside
/// it a row per task under a time axis, with the links between them.
final class GanttChart extends ScrollSurface {
    private static final int MOVE = 1;
    private static final int RESIZE_END = 2;
    private static final int PROGRESS = 3;
    private static final int LINK = 4;
    private static final int REFLOW_MS = 260;

    private final Gantt owner;
    private final TimeAxis axis;
    private final ArrayList<GanttTask> rows = new ArrayList<GanttTask>();
    private final HashMap<GanttTask, Integer> rowOf = new HashMap<GanttTask, Integer>();
    private GanttTask linkProbe;
    private boolean dirty = true;
    private boolean scaled;
    private boolean fit;
    private int laidW = -1;
    private int laidH = -1;
    private int laidGen = -1;
    private int rowH;
    private int barH;
    private int nameW;
    private int[] colW = new int[0];
    private Gantt.Column[] cols = new Gantt.Column[0];
    private final int[] rect = new int[4];
    private final int[] rect2 = new int[4];
    private ResourceLoadView load;

    // Where the bars were before the project was last scheduled, while they
    // slide to where they are now.
    private HashMap<GanttTask, long[]> reflowFrom;
    private long reflowStart;

    private int mode;
    private GanttTask editTask;
    private long editStart;
    private long editEnd;
    private int editPercent;
    private long grabTime;
    private int linkX;
    private int linkY;
    private GanttTask linkTarget;

    GanttChart(Gantt owner) {
        this.owner = owner;
        axis = new TimeAxis(ZoneId.systemDefault());
        setUIID("Gantt");
    }

    void setLoadView(ResourceLoadView v) {
        load = v;
    }

    TimeAxis axis() {
        return axis;
    }

    List<GanttTask> rows() {
        return rows;
    }

    void modelChanged() {
        // What could be linked a moment ago is asked again.
        linkProbe = null;
        if (mode == LINK) {
            linkTarget = null;
        }
        dirty = true;
        repaint();
    }

    /// Remembers where every bar is, so that the next layout slides them to
    /// their new dates and does not jump.
    void beginReflow() {
        beginReflow(null, 0, 0);
    }

    /// As `beginReflow()`, with one bar starting from where a hand left it
    /// and not from its dates: the project may put it somewhere else again,
    /// and it slides there from under the hand, not from where it was.
    void beginReflow(GanttTask held, long heldStart, long heldEnd) {
        HashMap<GanttTask, long[]> from = new HashMap<GanttTask, long[]>();
        for (GanttTask t : rows) {
            if (t == held) { //NOPMD CompareObjectsWithEquals - the very task that was dragged
                from.put(t, new long[]{heldStart, heldEnd});
            } else {
                from.put(t, new long[]{t.getStart(), t.getEnd()});
            }
        }
        reflowFrom = from;
        reflowStart = tick();
        requestAnimation();
    }

    /// Ends the slide at once, leaving every bar at its date.
    void reflowDone() {
        reflowFrom = null;
    }

    @Override
    boolean animateSurface() {
        if (reflowFrom == null) {
            return false;
        }
        if (tick() - reflowStart >= REFLOW_MS) {
            reflowFrom = null;
        }
        repaint();
        return reflowFrom != null;
    }

    private long shown(GanttTask t, boolean end) {
        long now = end ? t.getEnd() : t.getStart();
        if (reflowFrom == null) {
            return now;
        }
        long[] from = reflowFrom.get(t);
        if (from == null) {
            return now;
        }
        float p = Math.min(1f, (tick() - reflowStart) / (float) REFLOW_MS);
        // Ease out: fast at first, settling at the end.
        p = 1 - (1 - p) * (1 - p);
        long was = from[end ? 1 : 0];
        return was + (long) ((now - was) * p);
    }

    void zoomToFit() {
        fit = true;
        scaled = false;
        laidW = -1;
        repaint();
    }

    void zoom(float factor) {
        zoomBy(factor, headerW + bodyWidth() / 2, headerH);
    }

    void scrollToTime(long time) {
        layoutNow();
        scrollTo(axis.x(time) - bodyWidth() / 4, getOffY());
    }

    void scrollToTask(GanttTask t) {
        layoutNow();
        Integer row = rowOf.get(t);
        int y = getOffY();
        if (row != null) {
            int top = row.intValue() * rowH;
            if (top < y || top + rowH > y + bodyHeight()) {
                y = top - bodyHeight() / 3;
            }
        }
        scrollTo(axis.x(t.getStart()) - bodyWidth() / 4, y);
    }

    // ------------------------------------------------------------- layout

    private void addRows(List<GanttTask> tasks) {
        for (GanttTask t : tasks) {
            rowOf.put(t, Integer.valueOf(rows.size()));
            rows.add(t);
            if (t.isSummary() && !owner.isCollapsed(t)) {
                addRows(t.getChildren());
            }
        }
    }

    @Override
    void prepare(int width, int height) {
        int gen = UIManager.getThemeGeneration();
        if (!dirty && width == laidW && height == laidH && gen == laidGen) {
            return;
        }
        laidW = width;
        laidH = height;
        laidGen = gen;
        dirty = false;
        ProjectModel model = owner.getModel();
        Style header = Look.style("GanttHeader");
        Style rowStyle = Look.style("GanttRow");
        Font f = rowStyle.getFont();
        rows.clear();
        rowOf.clear();
        long first = Long.MAX_VALUE;
        long last = Long.MIN_VALUE;
        ZoneId zone = ZoneId.systemDefault();
        if (model != null) {
            zone = model.getCalendar().getZone();
            addRows(model.getRootTasks());
            first = model.getStart();
            last = model.getStart();
            for (GanttTask t : model.getTasks()) {
                first = Math.min(first, t.getStart());
                last = Math.max(last, t.getEnd());
                if (t.hasBaseline()) {
                    first = Math.min(first, t.getBaselineStart());
                    last = Math.max(last, t.getBaselineEnd());
                }
            }
        }
        if (first == Long.MAX_VALUE) {
            first = owner.now();
            last = first;
        }
        // Room on both sides: to drag a task earlier, and for the labels
        // after the last bar.
        long from = TimeMath.startOfDay(first, zone) - 3 * TimeMath.DAY;
        long to = TimeMath.startOfDay(last, zone) + 12 * TimeMath.DAY;
        long keep = scaled ? axis.time(getOffX()) : Long.MIN_VALUE;
        axis.setZone(zone);
        axis.setFirstDayOfWeek(owner.getFirstDayOfWeek());
        axis.set24Hours(owner.is24Hours());
        axis.setRange(from, to);

        rowH = Math.max(Look.mm(6.5f), f.getHeight() + Look.mm(2.6f));
        barH = Math.max(4, rowH * 11 / 20);
        headerH = axis.headerHeight(header);

        // The table: the names, then as many of the columns as fit.
        int indent = Look.mm(3);
        int widest = header.getFont().stringWidth(owner.localize("gantt.task", "Task"));
        for (GanttTask t : rows) {
            widest = Math.max(widest, f.stringWidth(t.getName()) + t.getLevel() * indent);
        }
        nameW = Math.max(Look.mm(20), Math.min(Look.mm(52), widest + Look.mm(7)));
        int cap = Math.max(Look.mm(18), width * 45 / 100);
        nameW = Math.min(nameW, cap);
        Gantt.Column[] wanted = owner.columns();
        ArrayList<Gantt.Column> shownCols = new ArrayList<Gantt.Column>();
        int[] widths = new int[wanted.length];
        int total = nameW;
        for (Gantt.Column c : wanted) {
            int w = Math.max(f.stringWidth(sample(c)), header.getFont().stringWidth(columnTitle(c))) + Look.mm(3);
            if (total + w > cap) {
                break;
            }
            widths[shownCols.size()] = w;
            shownCols.add(c);
            total += w;
        }
        cols = new Gantt.Column[shownCols.size()];
        shownCols.toArray(cols);
        colW = widths;
        headerW = owner.isTableVisible() ? total : 0;

        int bodyW = Math.max(1, width - headerW);
        long span = to - from;
        double dayScale = (double) TimeMath.DAY / Look.mm(7);
        axis.setScaleLimits((double) TimeMath.HOUR / Look.mm(9), Math.max((double) span / bodyW, dayScale));
        if (!scaled && width > 0) {
            scaled = true;
            if (fit) {
                axis.setMillisPerPixel((double) span / bodyW);
            } else {
                axis.setMillisPerPixel(Math.min(dayScale, Math.max((double) span / bodyW,
                        (double) TimeMath.DAY / Look.mm(14))));
            }
            fit = false;
            setOffsets(0, getOffY());
        } else if (keep != Long.MIN_VALUE) {
            // The range may have grown at its start: what was at the left
            // edge stays there.
            setOffsets(axis.x(keep), getOffY());
        }
        axis.pickUnits(header.getFont().stringWidth("Www 00") + Look.mm(2));
        if (load != null) {
            load.chartChanged();
        }
    }

    private String sample(Gantt.Column c) {
        switch (c) {
            case START:
            case END:
                return "Www 00";
            case DURATION:
                return "000d";
            default:
                return "100%";
        }
    }

    private String columnTitle(Gantt.Column c) {
        switch (c) {
            case START:
                return owner.localize("gantt.start", "Start");
            case END:
                return owner.localize("gantt.end", "End");
            case DURATION:
                return owner.localize("gantt.duration", "Days");
            default:
                return "%";
        }
    }

    private String columnText(Gantt.Column c, GanttTask t) {
        ZoneId zone = axis.getZone();
        switch (c) {
            case START:
                return Dates.monthDay(TimeMath.epochDay(t.getStart(), zone));
            case END:
                return Dates.monthDay(TimeMath.epochDay(Math.max(t.getStart(), t.getEnd() - 1), zone));
            case DURATION: {
                WorkCalendar cal = owner.getModel().getCalendar();
                long work = t.isSummary() ? cal.workingTimeBetween(t.getStart(), t.getEnd()) : t.getDuration();
                long perDay = Math.max(1, cal.getMillisPerDay());
                long tenths = work * 10 / perDay;
                return tenths % 10 == 0 ? String.valueOf(tenths / 10) : tenths / 10 + "." + tenths % 10;
            }
            default:
                return t.getPercentComplete() + "%";
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
        return rows.size() * rowH;
    }

    @Override
    void scrolled() {
        if (load != null) {
            load.chartScrolled();
        }
    }

    /// The rectangle of a task's bar in the content's coordinates.
    void barRect(GanttTask t, int row, int[] out) {
        int x0 = axis.x(shown(t, false));
        int x1 = axis.x(shown(t, true));
        int top = row * rowH + (rowH - barH) / 2;
        if (t.isMilestone()) {
            out[0] = x0 - barH / 2;
            out[2] = barH;
        } else {
            out[0] = x0;
            out[2] = Math.max(2, x1 - x0);
        }
        out[1] = top;
        out[3] = barH;
    }

    // -------------------------------------------------------------- paint

    @Override
    void paintBody(Graphics g, int x, int y) {
        Style rowStyle = Look.style("GanttRow");
        Style rowSel = Look.selected("GanttRow");
        Style summaryRow = Look.style("GanttSummaryRow");
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        Look.fill(g, rowStyle, cx, cy, cw, ch);
        ProjectModel model = owner.getModel();
        if (model == null) {
            return;
        }
        int firstRow = Math.max(0, (cy - y) / rowH);
        int lastRow = Math.min(rows.size() - 1, (cy + ch - y) / rowH);
        // A page shows the project, not what a hand was doing to it.
        GanttTask selected = isPrinting() ? null : owner.getSelectedTask();
        for (int r = firstRow; r <= lastRow; r++) {
            GanttTask t = rows.get(r);
            if (t == selected) { //NOPMD CompareObjectsWithEquals - the selection is this very object
                Look.fill(g, rowSel, cx, y + r * rowH, cw, rowH);
            } else if (t.isSummary()) {
                Look.fill(g, summaryRow, cx, y + r * rowH, cw, rowH);
            }
        }
        paintRowHover(g, cx, y, cw, rowStyle.getFgColor());
        axis.paintOffHours(g, model.getCalendar(), Look.style("SchedulerOffHours"), x, cy, ch, cx, cx + cw);
        int lineColor = Look.mix(rowStyle.getBgColor(), rowStyle.getFgColor(), 45);
        axis.paintGrid(g, lineColor, rowStyle.getBgColor(), x, cy, ch, cx, cx + cw);
        g.setColor(Look.mix(rowStyle.getBgColor(), rowStyle.getFgColor(), 25));
        for (int r = firstRow; r <= lastRow + 1; r++) {
            g.drawLine(cx, y + r * rowH, cx + cw, y + r * rowH);
        }
        long now = owner.now();
        if (now >= axis.getStart() && now < axis.getEnd()) {
            g.setColor(Look.style("SchedulerNow").getFgColor());
            g.fillRect(x + axis.x(now) - 1, cy, 2, ch);
        }

        if (owner.isShowBaseline()) {
            Style base = Look.style("GanttBaseline");
            int h = Math.max(2, barH / 3);
            for (int r = firstRow; r <= lastRow; r++) {
                GanttTask t = rows.get(r);
                if (t.hasBaseline() && !t.isSummary()) {
                    int x0 = x + axis.x(t.getBaselineStart());
                    int x1 = x + axis.x(t.getBaselineEnd());
                    Look.fill(g, base, x0, y + r * rowH + rowH - h - 1, Math.max(2, x1 - x0), h);
                }
            }
        }
        if (owner.isShowLinks()) {
            paintLinks(g, model, x, y, firstRow, lastRow);
        }

        GanttBarRenderer renderer = owner.getBarRenderer();
        Font f = rowStyle.getFont();
        boolean showCritical = owner.isShowCriticalPath();
        int alpha = g.getAlpha();
        boolean lit = anyHover();
        float lift = mode == MOVE || mode == RESIZE_END ? liftLevel() : 0;
        for (int r = firstRow; r <= lastRow; r++) {
            GanttTask t = rows.get(r);
            barRect(t, r, rect);
            if (x + rect[0] > cx + cw) {
                continue;
            }
            int state = 0;
            float over = lit ? hoverLevel(t) : 0;
            if (over > 0) {
                state |= GanttBarRenderer.HOVERED;
            }
            boolean held = lift > 0 && t == editTask; //NOPMD CompareObjectsWithEquals - the very task that is held
            if (held) {
                // What is left behind while the bar is carried.
                g.setAlpha(Math.round(alpha * (1 - 0.6f * lift)));
            }
            if (t == selected) { //NOPMD CompareObjectsWithEquals - the selection is this very object
                state |= GanttBarRenderer.SELECTED;
            }
            if (showCritical && t.isCritical() && !t.isSummary()) {
                state |= GanttBarRenderer.CRITICAL;
            }
            if (x + rect[0] + rect[2] >= cx) {
                renderer.paintBar(g, owner, t, x + rect[0], y + rect[1], rect[2], rect[3], state);
                if (!held && !t.isMilestone()) {
                    Look.glow(g, rowStyle.getFgColor(), x + rect[0], y + rect[1], rect[2], rect[3], over);
                }
            }
            g.setAlpha(alpha);
            String label = labelOf(model, t);
            if (label != null) {
                g.setFont(f);
                g.setColor(rowStyle.getFgColor());
                g.setAlpha(alpha * 170 / 255);
                g.drawString(label, x + rect[0] + rect[2] + Look.mm(2.2f) + (t == selected ? Look.mm(3) : 0), //NOPMD CompareObjectsWithEquals
                        y + r * rowH + (rowH - f.getHeight()) / 2);
                g.setAlpha(alpha);
            }
        }
        if (selected != null && owner.isEditable() && !selected.isSummary() && mode == 0) {
            Integer row = rowOf.get(selected);
            if (row != null) {
                paintHandles(g, selected, row.intValue(), x, y);
            }
        }
        if (mode != 0 && !isPrinting()) {
            paintEdit(g, x, y);
        }
    }

    // A page ends between two rows, so that no row is cut in half by it.
    @Override
    int pageStep(int bodyH) {
        return rowH > 0 && bodyH >= rowH ? bodyH - bodyH % rowH : bodyH;
    }

    // A drag is of a task that is in the project and on a row. One that was
    // removed, or whose parent was folded, while it was held has no bar left
    // to follow the pointer.
    private boolean editGone() {
        ProjectModel model = owner.getModel();
        return editTask == null || model == null || !model.contains(editTask) || rowOf.get(editTask) == null;
    }

    // The start a move by `delta` gives. The distance is what snaps, not the
    // start: a task that begins at one in the afternoon stays at one, and a
    // press that goes nowhere moves nothing.
    private long movedStart(GanttTask t, long delta) {
        ProjectModel model = owner.getModel();
        ZoneId zone = axis.getZone();
        long start = t.getStart();
        long to;
        if (axis.snapsToDays()) {
            long days = Math.round((double) delta / TimeMath.DAY);
            if (days == 0) {
                return start;
            }
            to = TimeMath.at(TimeMath.epochDay(start, zone) + days, TimeMath.minuteOfDay(start, zone), zone);
        } else {
            long step = 15 * TimeMath.MINUTE;
            long steps = Math.round((double) delta / step);
            if (steps == 0) {
                return start;
            }
            to = start + steps * step;
        }
        return model.getCalendar().nextWorkingTime(to);
    }

    // Who a task is assigned to, written after its bar.
    private static String labelOf(ProjectModel model, GanttTask t) {
        List<ResourceAssignment> as = t.getAssignments();
        if (as.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < as.size(); i++) { //NOPMD ForLoopCanBeForeach - painted for every row of every frame; a for-each allocates an iterator
            ScheduleResource r = model.getResource(as.get(i).getResourceId());
            if (r == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(r.getName());
            float units = as.get(i).getUnits();
            if (Math.abs(units - 1f) > 0.001f) {
                sb.append(" (").append(Math.round(units * 100)).append("%)");
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private void paintHandles(Graphics g, GanttTask t, int row, int x, int y) {
        barRect(t, row, rect);
        Style st = Look.style("GanttBar");
        boolean aa = g.isAntiAliased();
        g.setAntiAliased(true);
        // The dot a link is dragged from.
        int r = Look.mm(1.1f);
        int lx = x + rect[0] + rect[2] + Look.mm(2.2f);
        int ly = y + rect[1] + rect[3] / 2;
        g.setColor(Look.style("GanttRow").getBgColor());
        g.fillArc(lx - r, ly - r, 2 * r, 2 * r, 0, 360);
        g.setColor(Look.style("GanttLink").getFgColor());
        g.drawArc(lx - r, ly - r, 2 * r, 2 * r, 0, 360);
        if (!t.isMilestone()) {
            // The marker progress is dragged by.
            int px = x + rect[0] + rect[2] * t.getPercentComplete() / 100;
            int py = y + rect[1] + rect[3];
            int s = Look.mm(1.2f);
            g.setColor(st.getFgColor());
            g.fillPolygon(new int[]{px, px + s, px - s}, new int[]{py - s, py + s, py + s}, 3);
        }
        g.setAntiAliased(aa);
    }

    private void paintEdit(Graphics g, int x, int y) {
        Integer rowI = rowOf.get(editTask);
        if (rowI == null) {
            return;
        }
        int row = rowI.intValue();
        Style rowStyle = Look.style("GanttRow");
        if (mode == LINK) {
            barRect(editTask, row, rect);
            g.setColor(Look.style("GanttLink").getFgColor());
            g.drawLine(x + rect[0] + rect[2], y + rect[1] + rect[3] / 2, linkX, linkY);
            if (linkTarget != null) {
                Integer tr = rowOf.get(linkTarget);
                if (tr != null) {
                    barRect(linkTarget, tr.intValue(), rect);
                    g.drawRect(x + rect[0] - 2, y + rect[1] - 2, rect[2] + 3, rect[3] + 3);
                }
            }
            return;
        }
        if (mode == PROGRESS) {
            barRect(editTask, row, rect);
            Font f = rowStyle.getFont();
            g.setFont(f);
            g.setColor(rowStyle.getFgColor());
            int px = x + rect[0] + rect[2] * editPercent / 100;
            g.fillRect(px - 1, y + rect[1] - 2, 2, rect[3] + 4);
            g.drawString(editPercent + "%", px + Look.mm(1), y + rect[1] - f.getHeight());
            return;
        }
        int x0 = x + axis.x(editStart);
        int x1 = x + axis.x(editEnd);
        int top = y + row * rowH + (rowH - barH) / 2;
        if (editTask.isMilestone()) {
            Look.shadow(g, x0 - barH / 2, top, barH, barH, liftLevel());
            owner.getBarRenderer().paintBar(g, owner, editTask, x0 - barH / 2, top, barH, barH,
                    GanttBarRenderer.DRAGGED);
        } else {
            Look.shadow(g, x0, top, Math.max(2, x1 - x0), barH, liftLevel());
            owner.getBarRenderer().paintBar(g, owner, editTask, x0, top, Math.max(2, x1 - x0), barH,
                    GanttBarRenderer.DRAGGED);
        }
        Font f = rowStyle.getFont();
        g.setFont(f);
        g.setColor(rowStyle.getFgColor());
        ZoneId zone = axis.getZone();
        String s = Dates.monthDay(TimeMath.epochDay(editStart, zone)) + " - "
                + Dates.monthDay(TimeMath.epochDay(Math.max(editStart, editEnd - 1), zone));
        g.drawString(s, x0, top - f.getHeight() > y + getOffY() ? top - f.getHeight() : top + barH);
    }

    // A link leaves its predecessor, turns once or twice, and arrives at its
    // successor from outside it, never across a bar it belongs to.
    private void paintLinks(Graphics g, ProjectModel model, int x, int y, int firstRow, int lastRow) {
        List<TaskDependency> all = model.getDependencies();
        int plain = Look.style("GanttLink").getFgColor();
        int hot = Look.style("GanttLinkCritical").getFgColor();
        boolean showCritical = owner.isShowCriticalPath();
        int stub = Look.mm(1.8f);
        int head = Math.max(3, Look.mm(1));
        Object over = isPrinting() ? null : hovered();
        int strong = Look.style("GanttRow").getFgColor();
        for (TaskDependency d : all) {
            Integer pr = rowOf.get(d.getPredecessor());
            Integer sr = rowOf.get(d.getSuccessor());
            if (pr == null || sr == null) {
                continue;
            }
            int p = pr.intValue();
            int s = sr.intValue();
            if ((p < firstRow && s < firstRow) || (p > lastRow && s > lastRow)) {
                continue;
            }
            barRect(d.getPredecessor(), p, rect);
            barRect(d.getSuccessor(), s, rect2);
            int dirF = d.fromFinish() ? 1 : -1;
            int dirT = d.constrainsStart() ? 1 : -1;
            int fx = x + (d.fromFinish() ? rect[0] + rect[2] : rect[0]);
            int fy = y + rect[1] + rect[3] / 2;
            int tx = x + (d.constrainsStart() ? rect2[0] : rect2[0] + rect2[2]);
            int ty = y + rect2[1] + rect2[3] / 2;
            int out = fx + dirF * stub;
            int in = tx - dirT * stub;
            g.setColor(showCritical && d.getPredecessor().isCritical() && d.getSuccessor().isCritical() ? hot : plain);
            if (over != null && (over == d.getPredecessor() || over == d.getSuccessor())) { //NOPMD CompareObjectsWithEquals - the very task the pointer is on
                // What the task under the pointer waits for, and what waits for it.
                g.setColor(strong);
            }
            if (dirF == dirT && (in - out) * dirF >= 0) {
                g.drawLine(fx, fy, in, fy);
                g.drawLine(in, fy, in, ty);
            } else {
                int mid = y + (s > p ? (p + 1) * rowH : p * rowH);
                g.drawLine(fx, fy, out, fy);
                g.drawLine(out, fy, out, mid);
                g.drawLine(out, mid, in, mid);
                g.drawLine(in, mid, in, ty);
            }
            g.drawLine(in, ty, tx, ty);
            g.fillPolygon(new int[]{tx, tx - dirT * head, tx - dirT * head}, new int[]{ty, ty - head, ty + head}, 3);
        }
    }

    @Override
    void paintTopHeader(Graphics g, int x, int y) {
        axis.paintHeader(g, Look.style("GanttHeader"), x, y, g.getClipX(), g.getClipX() + g.getClipWidth());
    }

    @Override
    void paintLeftHeader(Graphics g, int x, int y) {
        Style rowStyle = Look.style("GanttRow");
        Style rowSel = Look.selected("GanttRow");
        Style summaryRow = Look.style("GanttSummaryRow");
        int cy = g.getClipY();
        int ch = g.getClipHeight();
        Look.fill(g, rowStyle, x, cy, headerW, ch);
        int firstRow = Math.max(0, (cy - y) / rowH);
        int lastRow = Math.min(rows.size() - 1, (cy + ch - y) / rowH);
        int indent = Look.mm(3);
        int pad = Look.mm(1.2f);
        int arrow = Look.mm(1.1f);
        // A page shows the project, not what a hand was doing to it.
        GanttTask selected = isPrinting() ? null : owner.getSelectedTask();
        int line = Look.mix(rowStyle.getBgColor(), rowStyle.getFgColor(), 25);
        boolean aa = g.isAntiAliased();
        boolean lit = anyHover();
        int hot = lit ? hoverRow(hovered()) : -1;
        int cold = lit ? hoverRow(fading()) : -1;
        for (int r = firstRow; r <= lastRow; r++) {
            GanttTask t = rows.get(r);
            int top = y + r * rowH;
            Style st = t.isSummary() ? summaryRow : rowStyle;
            if (t == selected) { //NOPMD CompareObjectsWithEquals - the selection is this very object
                Look.fill(g, rowSel, x, top, headerW, rowH);
            } else if (t.isSummary()) {
                Look.fill(g, summaryRow, x, top, headerW, rowH);
            }
            if (r == hot) {
                Look.wash(g, rowStyle.getFgColor(), x, top, headerW, rowH, hoverLevel(hovered()));
            } else if (r == cold) {
                Look.wash(g, rowStyle.getFgColor(), x, top, headerW, rowH, hoverLevel(fading()));
            }
            Font f = st.getFont();
            int tx = x + pad + t.getLevel() * indent;
            int mid = top + rowH / 2;
            g.setColor(st.getFgColor());
            if (t.isSummary()) {
                g.setAntiAliased(true);
                if (owner.isCollapsed(t)) {
                    g.fillPolygon(new int[]{tx, tx + arrow, tx}, new int[]{mid - arrow, mid, mid + arrow}, 3);
                } else {
                    g.fillPolygon(new int[]{tx - arrow / 2, tx + arrow + arrow / 2, tx + arrow / 2},
                            new int[]{mid - arrow / 2, mid - arrow / 2, mid + arrow}, 3);
                }
                g.setAntiAliased(aa);
            }
            tx += arrow + Look.mm(1.4f);
            g.setFont(f);
            g.drawString(Look.fit(t.getName(), f, x + nameW - tx - pad), tx, top + (rowH - f.getHeight()) / 2);
            int cxp = x + nameW;
            for (int c = 0; c < cols.length; c++) {
                String s = columnText(cols[c], t);
                g.drawString(s, cxp + colW[c] - f.stringWidth(s) - pad, top + (rowH - f.getHeight()) / 2);
                cxp += colW[c];
            }
            g.setColor(line);
            g.drawLine(x, top + rowH, x + headerW, top + rowH);
        }
        g.setColor(Look.mix(rowStyle.getBgColor(), rowStyle.getFgColor(), 60));
        g.drawLine(x + headerW - 1, cy, x + headerW - 1, cy + ch);
    }

    @Override
    void paintCorner(Graphics g, int x, int y) {
        Style header = Look.style("GanttHeader");
        Font f = header.getFont();
        Look.fill(g, header, x, y, headerW, headerH);
        g.setFont(f);
        g.setColor(header.getFgColor());
        int pad = Look.mm(1.2f);
        int ty = y + headerH - f.getHeight() - Look.mm(0.7f);
        g.drawString(Look.fit(owner.localize("gantt.task", "Task"), f, nameW - 2 * pad), x + pad, ty);
        int cxp = x + nameW;
        for (int c = 0; c < cols.length; c++) {
            String s = columnTitle(cols[c]);
            g.drawString(s, cxp + colW[c] - f.stringWidth(s) - pad, ty);
            cxp += colW[c];
        }
        g.setColor(Look.mix(header.getBgColor(), header.getFgColor(), 50));
        g.drawLine(x, y + headerH - 1, x + headerW, y + headerH - 1);
        g.drawLine(x + headerW - 1, y, x + headerW - 1, y + headerH);
    }

    // ------------------------------------------------------------ pointer

    // The row the pointer is in, whether it is on the bar or beside it.
    private int hoverRow(Object key) {
        if (key instanceof Integer) {
            return ((Integer) key).intValue();
        }
        if (key instanceof GanttTask) {
            Integer r = rowOf.get(key);
            return r == null ? -1 : r.intValue();
        }
        return -1;
    }

    // That row lit from edge to edge: the table and the chart are one row.
    private void paintRowHover(Graphics g, int x, int y, int w, int rgb) {
        if (!anyHover()) {
            return;
        }
        Object now = hovered();
        Object was = fading();
        int r = hoverRow(now);
        if (r >= 0 && r < rows.size()) {
            Look.wash(g, rgb, x, y + r * rowH, w, rowH, hoverLevel(now));
        }
        int old = hoverRow(was);
        if (old >= 0 && old < rows.size() && old != r) {
            Look.wash(g, rgb, x, y + old * rowH, w, rowH, hoverLevel(was));
        }
    }

    @Override
    void hoverAt(int x, int y) {
        int r = rowAt(y);
        if (r < 0 || x < 0 || x >= getWidth()) {
            hover(null, DEFAULT_CURSOR);
            return;
        }
        if (x < headerW) {
            // A click on the table picks the task or folds its children.
            hover(Integer.valueOf(r), HAND_CURSOR);
            return;
        }
        int cx = x - headerW + getOffX();
        int cy = y - headerH + getOffY();
        boolean edits = owner.isEditable();
        GanttTask sel = owner.getSelectedTask();
        Integer selRow = sel == null ? null : rowOf.get(sel);
        if (edits && sel != null && selRow != null && !sel.isSummary()) {
            // The two handles of the selected task, as beginEdit reads them.
            barRect(sel, selRow.intValue(), rect);
            int reach = Look.mm(2.6f);
            if (Math.abs(cx - (rect[0] + rect[2] + Look.mm(2.2f))) <= reach
                    && Math.abs(cy - (rect[1] + rect[3] / 2)) <= reach) {
                hover(sel, CROSSHAIR_CURSOR);
                return;
            }
            int px = rect[0] + rect[2] * sel.getPercentComplete() / 100;
            if (!sel.isMilestone() && Math.abs(cx - px) <= reach && cy >= rect[1] + rect[3] / 2
                    && cy <= rect[1] + rect[3] + reach) {
                hover(sel, E_RESIZE_CURSOR);
                return;
            }
        }
        GanttTask t = barAt(x, y);
        if (t == null) {
            hover(Integer.valueOf(r), DEFAULT_CURSOR);
            tip(null);
            return;
        }
        ZoneId zone = axis.getZone();
        tip(t.getName() + ", " + Dates.monthDay(TimeMath.epochDay(t.getStart(), zone)) + " - "
                + Dates.monthDay(TimeMath.epochDay(Math.max(t.getStart(), t.getEnd() - 1), zone)) + ", "
                + t.getPercentComplete() + "%");
        int cursor = HAND_CURSOR;
        if (edits && isDragEdits() && !t.isSummary()) {
            barRect(t, r, rect);
            int edge = Math.min(rect[2] / 3, Look.mm(1.6f));
            cursor = !t.isMilestone() && rect[0] + rect[2] - cx <= edge ? E_RESIZE_CURSOR : MOVE_CURSOR;
        }
        hover(t, cursor);
    }

    private int rowAt(int py) {
        int r = (py - headerH + getOffY()) / Math.max(1, rowH);
        return py < headerH || r < 0 || r >= rows.size() ? -1 : r;
    }

    /// The task whose bar is at a point of the component, or null.
    GanttTask barAt(int px, int py) {
        int r = rowAt(py);
        if (r < 0 || px < headerW) {
            return null;
        }
        GanttTask t = rows.get(r);
        barRect(t, r, rect);
        int cx = px - headerW + getOffX();
        int slack = Math.max(0, (Look.mm(2.5f) - rect[2]) / 2);
        return cx >= rect[0] - slack && cx < rect[0] + rect[2] + slack ? t : null;
    }

    private long snapEnd(long time) {
        ProjectModel model = owner.getModel();
        ZoneId zone = axis.getZone();
        long t;
        if (axis.snapsToDays()) {
            t = TimeMath.startOfDay(time + TimeMath.DAY / 2, zone);
        } else {
            long day = TimeMath.epochDay(time, zone);
            int m = (TimeMath.minuteOfDay(time, zone) + 7) / 15 * 15;
            t = TimeMath.at(day, Math.min(TimeMath.MINUTES_PER_DAY, m), zone);
        }
        return model.getCalendar().previousWorkingTime(t);
    }

    @Override
    boolean beginEdit(int x, int y, boolean handlesOnly) {
        ProjectModel model = owner.getModel();
        if (!owner.isEditable() || model == null || x < headerW || y < headerH) {
            return false;
        }
        int cx = x - headerW + getOffX();
        int cy = y - headerH + getOffY();
        GanttTask sel = owner.getSelectedTask();
        Integer selRow = sel == null ? null : rowOf.get(sel);
        if (sel != null && selRow != null && !sel.isSummary()) {
            barRect(sel, selRow.intValue(), rect);
            int reach = Look.mm(2.6f);
            int lx = rect[0] + rect[2] + Look.mm(2.2f);
            int ly = rect[1] + rect[3] / 2;
            if (Math.abs(cx - lx) <= reach && Math.abs(cy - ly) <= reach) {
                mode = LINK;
                editTask = sel;
                linkTarget = null;
                linkProbe = null;
                return true;
            }
            int px = rect[0] + rect[2] * sel.getPercentComplete() / 100;
            if (!sel.isMilestone() && Math.abs(cx - px) <= reach && cy >= rect[1] + rect[3] / 2
                    && cy <= rect[1] + rect[3] + reach) {
                mode = PROGRESS;
                editTask = sel;
                editPercent = sel.getPercentComplete();
                return true;
            }
        }
        GanttTask t = barAt(x, y);
        if (t == null || t.isSummary()) {
            return false;
        }
        barRect(t, rowOf.get(t).intValue(), rect);
        int edge = Math.min(rect[2] / 3, Look.mm(handlesOnly ? 3f : 1.6f));
        boolean right = !t.isMilestone() && rect[0] + rect[2] - cx <= edge;
        if (handlesOnly && (t != sel || !right)) { //NOPMD CompareObjectsWithEquals - the selection is this very object
            return false;
        }
        mode = right ? RESIZE_END : MOVE;
        editTask = t;
        editStart = t.getStart();
        editEnd = t.getEnd();
        grabTime = axis.time(cx);
        owner.viewSelected(t);
        return true;
    }

    @Override
    void editDragged(int x, int y) {
        if (mode == 0) {
            return;
        }
        if (editGone()) {
            cancelEdit();
            return;
        }
        ProjectModel model = owner.getModel();
        int cx = Math.max(0, Math.min(contentWidth(), x - headerW + getOffX()));
        long pointer = axis.time(cx);
        if (mode == MOVE) {
            editStart = movedStart(editTask, pointer - grabTime);
            // A task with dates of its own keeps the span it was given; it
            // has no duration the calendar could lay out again.
            editEnd = editStart == editTask.getStart() ? editTask.getEnd()
                    : editTask.isManuallyScheduled() ? editStart + (editTask.getEnd() - editTask.getStart())
                    : model.getCalendar().addWorkingTime(editStart, editTask.getDuration());
        } else if (mode == RESIZE_END) {
            long least = model.getCalendar().addWorkingTime(editStart, axis.snapsToDays()
                    ? Math.min(model.getCalendar().getMillisPerDay(), TimeMath.DAY) : 15 * TimeMath.MINUTE);
            editEnd = Math.max(least, snapEnd(pointer));
        } else if (mode == PROGRESS) {
            barRect(editTask, rowOf.get(editTask).intValue(), rect);
            editPercent = Math.max(0, Math.min(100, (cx - rect[0]) * 100 / Math.max(1, rect[2])));
            // Whole fives are what a hand means.
            editPercent = (editPercent + 2) / 5 * 5;
        } else {
            linkX = getX() + x;
            linkY = getY() + y;
            GanttTask t = barAt(x, y);
            // Asking whether a link would close a loop orders the whole
            // project, so it is asked once for each bar the pointer comes to.
            if (t != linkProbe) { //NOPMD CompareObjectsWithEquals - the bar under the pointer last time
                linkProbe = t;
                linkTarget = t != null && t != editTask && model.canLink(editTask, t) ? t : null; //NOPMD CompareObjectsWithEquals
            }
        }
    }

    @Override
    void endEdit(boolean commit) {
        int m = mode;
        GanttTask t = editTask;
        mode = 0;
        editTask = null;
        ProjectModel model = owner.getModel();
        if (!commit || t == null || model == null || !model.contains(t)) {
            linkTarget = null;
            return;
        }
        if (m == LINK) {
            // Asked once more, and before the listeners are: the project
            // changing under the pointer may have made the link one that
            // cannot be, and a listener is not asked about one of those.
            if (linkTarget != null && model.canLink(t, linkTarget) && owner.viewLinking(t, linkTarget)) {
                beginReflow();
                model.link(t, linkTarget);
                model.update();
            }
            linkTarget = null;
        } else if (m == PROGRESS) {
            // Progress is a change like a new date, so it is offered the
            // same way, with the dates the task keeps.
            if (editPercent != t.getPercentComplete() && owner.viewChanging(t, t.getStart(), t.getEnd())) {
                t.setPercentComplete(editPercent);
                model.update();
                owner.viewChanged(t);
            }
        } else if (m == MOVE) {
            if (editStart != t.getStart() && owner.viewChanging(t, editStart, editEnd)) {
                beginReflow(t, editStart, editEnd);
                if (t.isManuallyScheduled()) {
                    t.setDates(editStart, editEnd);
                } else {
                    // A task that is scheduled goes where it is put only as
                    // far as its links allow, so the drag is a constraint.
                    // A constraint that places the task keeps its kind and
                    // takes the new date. One that only says how late the
                    // task may be places nothing, so a task that has it, or
                    // none, is held from starting earlier: the bar has to
                    // end up where it was put.
                    GanttTask.Constraint c = t.getConstraint();
                    if (c == GanttTask.Constraint.MUST_START_ON) {
                        t.setConstraint(c, editStart);
                    } else if (c == GanttTask.Constraint.MUST_FINISH_ON
                            || c == GanttTask.Constraint.FINISH_NO_EARLIER_THAN) {
                        t.setConstraint(c, editEnd);
                    } else {
                        t.setConstraint(GanttTask.Constraint.START_NO_EARLIER_THAN, editStart);
                    }
                }
                model.update();
                owner.viewChanged(t);
            }
        } else if (editEnd != t.getEnd() && owner.viewChanging(t, editStart, editEnd)) {
            beginReflow(t, t.getStart(), editEnd);
            long work = Math.max(TimeMath.MINUTE, model.getCalendar().workingTimeBetween(t.getStart(), editEnd));
            if (t.isManuallyScheduled()) {
                t.setDates(t.getStart(), editEnd);
            }
            t.setDuration(work);
            model.update();
            owner.viewChanged(t);
        }
    }

    @Override
    void tapped(int x, int y, boolean doubleTap) {
        int r = rowAt(y);
        if (r < 0) {
            return;
        }
        GanttTask t = rows.get(r);
        if (x < headerW) {
            int toggle = Look.mm(1.2f) + t.getLevel() * Look.mm(3) + Look.mm(4.5f);
            if (t.isSummary() && x <= toggle) {
                owner.setCollapsed(t, !owner.isCollapsed(t));
                return;
            }
            owner.viewSelected(t);
            if (doubleTap) {
                scrollToTask(t);
                owner.viewOpened(t);
            }
            return;
        }
        GanttTask bar = barAt(x, y);
        owner.viewSelected(bar);
        if (bar != null && doubleTap) {
            owner.viewOpened(bar);
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
        axis.pickUnits(Look.style("GanttHeader").getFont().stringWidth("Www 00") + Look.mm(2));
        setOffsets(axis.x(t) - (x - headerW), getOffY());
        if (load != null) {
            load.chartChanged();
        }
        repaint();
        return true;
    }
}
