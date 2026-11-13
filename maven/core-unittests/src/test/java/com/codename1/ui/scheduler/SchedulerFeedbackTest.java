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

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.scheduling.DefaultScheduleModel;
import com.codename1.scheduling.GanttTask;
import com.codename1.scheduling.ProjectModel;
import com.codename1.scheduling.ScheduleItem;
import com.codename1.scheduling.TimeMath;
import com.codename1.scheduling.WorkCalendar;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.layouts.BorderLayout;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// What a pointer that only moves does to a schedule and to a chart, and the
/// animations of picking an item up and putting it down, each read at a
/// clock that has been stopped.
class SchedulerFeedbackTest extends UITestBase {
    private static final ZoneId UTC = ZoneId.of("UTC");

    /// A day of January 2026. The 5th is a Monday.
    private static long at(int dayOfMonth, int hour, int minute) {
        return TimeMath.at(LocalDate.of(2026, 1, dayOfMonth).toEpochDay(), hour * 60 + minute, UTC);
    }

    private Scheduler show(DefaultScheduleModel model) {
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());
        Scheduler s = new Scheduler(model);
        s.setZone(UTC);
        s.set24Hours(true);
        s.setCurrentTime(at(5, 10, 0));
        s.setDate(at(5, 0, 0));
        s.setView(Scheduler.View.DAY);
        s.setDragEdits(true);
        s.setOpenOnTap(false);
        form.add(BorderLayout.CENTER, s);
        form.revalidate();
        ((ScrollSurface) s.surface()).layoutNow();
        return s;
    }

    private static DefaultScheduleModel oneItem() {
        DefaultScheduleModel model = new DefaultScheduleModel();
        model.addItem(new ScheduleItem("a", "Review", at(5, 9, 0), at(5, 11, 0)));
        return model;
    }

    /// The rectangle of the item in the grid's own coordinates.
    private static int[] rect(DayGrid grid, String id) {
        int[] r = new int[4];
        for (DayGrid.Box b : grid.boxes()) {
            if (b.item.getId().equals(id)) {
                grid.boxRect(b, r);
                r[0] += grid.headerW - grid.getOffX();
                r[1] += grid.headerH - grid.getOffY();
                return r;
            }
        }
        throw new AssertionError("no box for " + id);
    }

    private static void hover(ScrollSurface s, int x, int y) {
        s.pointerHover(new int[]{s.getAbsoluteX() + x}, new int[]{s.getAbsoluteY() + y});
    }

    @FormTest
    void thePointerLightsTheItemUnderItAndTheLightFades() {
        Scheduler s = show(oneItem());
        DayGrid grid = (DayGrid) s.surface();
        int[] r = rect(grid, "a");
        s.setAnimationTime(1000);
        hover(grid, r[0] + r[2] / 2, r[1] + r[3] / 2);

        assertEquals("a", grid.hovered());
        assertNotNull(grid.getTooltip());
        assertTrue(grid.getTooltip().startsWith("Review"), "the tooltip names the item: " + grid.getTooltip());
        assertEquals(0f, grid.hoverLevel("a"), 0.001f, "the light comes up from nothing");
        s.setAnimationTime(1070);
        assertEquals(0.5f, grid.hoverLevel("a"), 0.01f);
        s.setAnimationTime(1140);
        assertEquals(1f, grid.hoverLevel("a"), 0.001f);

        grid.setHovered(false);
        assertNull(grid.hovered());
        assertNull(grid.getTooltip(), "no tooltip for an item the pointer has left");
        assertEquals("a", grid.fading());
        assertEquals(1f, grid.hoverLevel("a"), 0.001f, "and goes down from where it was");
        s.setAnimationTime(1210);
        assertEquals(0.5f, grid.hoverLevel("a"), 0.01f);
        s.setAnimationTime(1280);
        assertEquals(0f, grid.hoverLevel("a"), 0.001f);
        s.setAnimationTime(-1);
    }

    @FormTest
    void theCursorSaysWhatADragWouldDo() {
        Scheduler s = show(oneItem());
        DayGrid grid = (DayGrid) s.surface();
        int[] r = rect(grid, "a");
        int x = r[0] + r[2] / 2;

        hover(grid, x, r[1] + r[3] / 2);
        assertEquals(Component.MOVE_CURSOR, grid.getCursor());
        hover(grid, x, r[1] + r[3] - 1);
        assertEquals(Component.S_RESIZE_CURSOR, grid.getCursor());
        hover(grid, x, r[1] + 1);
        assertEquals(Component.N_RESIZE_CURSOR, grid.getCursor());

        hover(grid, x, r[1] + r[3] + Math.round(grid.getHourHeight()));
        assertTrue(grid.hovered() instanceof Long, "free time is marked by its slot: " + grid.hovered());
        assertNull(grid.getTooltip());

        s.setEditable(false);
        hover(grid, x, r[1] + r[3] - 1);
        assertEquals(Component.HAND_CURSOR, grid.getCursor(), "an item that cannot be changed can still be opened");
        hover(grid, x, r[1] + r[3] + Math.round(grid.getHourHeight()));
        assertNull(grid.hovered(), "nothing can be made in a schedule that is read only");
        assertEquals(Component.DEFAULT_CURSOR, grid.getCursor());
    }

    @FormTest
    void aPressedPointerDoesNotHover() {
        Scheduler s = show(oneItem());
        DayGrid grid = (DayGrid) s.surface();
        int[] r = rect(grid, "a");
        int x = grid.getAbsoluteX() + r[0] + r[2] / 2;
        int y = grid.getAbsoluteY() + r[1] + r[3] + 2 * Math.round(grid.getHourHeight());
        grid.pointerPressed(x, y);
        hover(grid, r[0] + r[2] / 2, r[1] + r[3] / 2);
        assertNull(grid.hovered());
        grid.pointerReleased(x, y);
    }

    @FormTest
    void anItemPickedUpLiftsAndADroppedOneSettlesFromWhereItWasLetGo() {
        Scheduler s = show(oneItem());
        DayGrid grid = (DayGrid) s.surface();
        int[] r = rect(grid, "a");
        int x = grid.getAbsoluteX() + r[0] + r[2] / 2;
        int y = grid.getAbsoluteY() + r[1] + r[3] / 2;
        int down = 2 * Math.round(grid.getHourHeight());

        s.setAnimationTime(1000);
        assertEquals(0f, grid.liftLevel(), 0.001f, "nothing is held");
        grid.pointerPressed(x, y);
        grid.pointerDragged(x, y + down / 2);
        grid.pointerDragged(x, y + down);
        assertEquals(0f, grid.liftLevel(), 0.001f);
        s.setAnimationTime(1080);
        assertEquals(0.5f, grid.liftLevel(), 0.01f);
        s.setAnimationTime(1160);
        assertEquals(1f, grid.liftLevel(), 0.001f);

        int[] held = new int[4];
        assertTrue(grid.editRect(held));
        s.setAnimationTime(2000);
        grid.pointerReleased(x, y + down);
        assertEquals(0f, grid.liftLevel(), 0.001f);
        assertEquals(at(5, 11, 0), ((DefaultScheduleModel) s.getModel()).getItem("a").getStart());

        grid.layoutNow();
        int[] placed = new int[4];
        for (DayGrid.Box b : grid.boxes()) {
            grid.boxRect(b, placed);
        }
        int[] now = placed.clone();
        assertTrue(grid.settle("a", now));
        assertArrayEquals(held, now, "it starts from where it was let go");
        s.setAnimationTime(2110);
        now = placed.clone();
        assertTrue(grid.settle("a", now));
        for (int i = 0; i < 4; i++) {
            int lo = Math.min(held[i], placed[i]);
            int hi = Math.max(held[i], placed[i]);
            assertTrue(now[i] >= lo && now[i] <= hi, "on its way between the two");
        }
        s.setAnimationTime(2220);
        now = placed.clone();
        assertFalse(grid.settle("a", now), "and is where it belongs when the time is up");
        assertArrayEquals(placed, now);
        s.setAnimationTime(-1);
    }

    @FormTest
    void aRendererIsToldTheItemIsUnderThePointer() {
        Scheduler s = show(oneItem());
        DayGrid grid = (DayGrid) s.surface();
        final int[] seen = {-1};
        s.setItemRenderer(new DefaultScheduleItemRenderer() {
            @Override
            public void paintItem(Graphics g, Scheduler scheduler, ScheduleItem item, int x, int y, int width,
                    int height, int state) {
                seen[0] = state;
                super.paintItem(g, scheduler, item, x, y, width, height, state);
            }
        });
        Image out = Image.createImage(Math.max(1, s.getWidth()), Math.max(1, s.getHeight()));
        s.paintComponent(out.getGraphics(), true);
        assertTrue(seen[0] >= 0, "the item was painted");
        assertEquals(0, seen[0] & ScheduleItemRenderer.HOVERED);

        // By a clock that is held: the light comes up from nothing, so a
        // frame painted in the millisecond of the hover has none yet, and a
        // machine fast enough to paint it then is not what is tested.
        int[] r = rect(grid, "a");
        s.setAnimationTime(1000);
        hover(grid, r[0] + r[2] / 2, r[1] + r[3] / 2);
        s.setAnimationTime(1060);
        s.paintComponent(out.getGraphics(), true);
        assertTrue((seen[0] & ScheduleItemRenderer.HOVERED) != 0);
        s.setAnimationTime(-1);
    }

    @FormTest
    void aChartLightsBarsAndRowsAndOffersTheirHandles() {
        long day = TimeMath.DAY;
        ProjectModel p = new ProjectModel(new WorkCalendar(UTC), at(5, 0, 0));
        GanttTask group = new GanttTask("group", "Group");
        GanttTask a = new GanttTask("a", "Alpha", 3 * day);
        GanttTask b = new GanttTask("b", "Beta", 2 * day);
        p.addTask(group);
        p.addTask(group, a);
        p.addTask(b);
        p.update();
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());
        Gantt g = new Gantt(p);
        g.set24Hours(true);
        g.setCurrentTime(at(5, 10, 0));
        g.setDragEdits(true);
        form.add(BorderLayout.CENTER, g);
        form.revalidate();
        GanttChart chart = g.surface();
        chart.layoutNow();
        double dayPx = TimeMath.DAY / chart.axis().getMillisPerPixel();
        chart.zoom((float) (48 / dayPx));
        chart.layoutNow();
        chart.scrollTo(0, 0);

        int[] r = new int[4];
        chart.barRect(a, chart.rows().indexOf(a), r);
        int x = r[0] + chart.headerW - chart.getOffX();
        int y = r[1] + chart.headerH - chart.getOffY() + r[3] / 2;

        g.setAnimationTime(1000);
        hover(chart, x + r[2] / 2, y);
        assertSame(a, chart.hovered());
        assertEquals(Component.MOVE_CURSOR, chart.getCursor());
        assertTrue(chart.getTooltip().startsWith("Alpha"), chart.getTooltip());
        g.setAnimationTime(1140);
        assertEquals(1f, chart.hoverLevel(a), 0.001f);

        hover(chart, x + r[2] - 1, y);
        assertEquals(Component.E_RESIZE_CURSOR, chart.getCursor(), "the end of a bar is what makes it longer");

        chart.barRect(group, chart.rows().indexOf(group), r);
        hover(chart, r[0] + chart.headerW - chart.getOffX() + r[2] / 2,
                r[1] + chart.headerH - chart.getOffY() + r[3] / 2);
        assertSame(group, chart.hovered());
        assertEquals(Component.HAND_CURSOR, chart.getCursor(), "a summary is where its tasks are and cannot be moved");

        // Beside the bars, the row itself.
        hover(chart, chart.headerW + chart.getWidth() - chart.headerW - 2, y);
        assertTrue(chart.hovered() instanceof Integer, "the row: " + chart.hovered());
        assertNull(chart.getTooltip());

        // The handle a dependency is drawn from, past the end of the bar
        // of the task that is selected.
        g.setSelectedTask(a);
        chart.barRect(a, chart.rows().indexOf(a), r);
        boolean offered = false;
        for (int dx = 0; dx < 80 && !offered; dx++) {
            hover(chart, x + r[2] + dx, y);
            offered = chart.getCursor() == Component.CROSSHAIR_CURSOR;
        }
        assertTrue(offered, "a selected bar offers its link handle");

        chart.setHovered(false);
        assertNull(chart.hovered());
        assertEquals(Component.DEFAULT_CURSOR, chart.getCursor());
        g.setAnimationTime(-1);
    }
}
