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

import com.codename1.ui.ScrollSurface;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.scheduling.DefaultScheduleModel;
import com.codename1.scheduling.ScheduleItem;
import com.codename1.scheduling.ScheduleResource;
import com.codename1.scheduling.TimeMath;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The scheduler through its views: where items are laid out, what a point
/// hits, and what a drag does to the model.
class SchedulerTest extends UITestBase {
    private static final ZoneId UTC = ZoneId.of("UTC");

    /// A day of January 2026. The 5th is a Monday.
    private static long day(int dayOfMonth) {
        return LocalDate.of(2026, 1, dayOfMonth).toEpochDay();
    }

    private static long at(int dayOfMonth, int hour, int minute) {
        return TimeMath.at(day(dayOfMonth), hour * 60 + minute, UTC);
    }

    private static ScheduleItem item(String id, long start, long end) {
        return new ScheduleItem(id, id, start, end);
    }

    private Scheduler show(DefaultScheduleModel model, Scheduler.View view) {
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());
        Scheduler s = new Scheduler(model);
        s.setZone(UTC);
        s.set24Hours(true);
        s.setCurrentTime(at(5, 10, 0));
        s.setDate(at(5, 0, 0));
        s.setView(view);
        s.setDragEdits(true);
        s.setOpenOnTap(false);
        form.add(BorderLayout.CENTER, s);
        form.revalidate();
        ScrollSurface surface = (ScrollSurface) s.surface();
        surface.refreshLayout();
        return s;
    }

    private static DayGrid.Box box(DayGrid grid, String id) {
        for (DayGrid.Box b : grid.boxes()) {
            if (b.item.getId().equals(id)) {
                return b;
            }
        }
        return null;
    }

    private static void drag(ScrollSurface s, int fromX, int fromY, int toX, int toY) {
        int ax = s.getAbsoluteX();
        int ay = s.getAbsoluteY();
        s.pointerPressed(ax + fromX, ay + fromY);
        s.pointerDragged(ax + (fromX + toX) / 2, ay + (fromY + toY) / 2);
        s.pointerDragged(ax + toX, ay + toY);
        s.pointerReleased(ax + toX, ay + toY);
    }

    private static void tap(ScrollSurface s, int x, int y) {
        s.pointerPressed(s.getAbsoluteX() + x, s.getAbsoluteY() + y);
        s.pointerReleased(s.getAbsoluteX() + x, s.getAbsoluteY() + y);
    }

    @FormTest
    void overlappingItemsShareTheColumn() {
        DefaultScheduleModel model = new DefaultScheduleModel();
        model.addItem(item("a", at(5, 9, 0), at(5, 11, 0)));
        model.addItem(item("b", at(5, 10, 0), at(5, 12, 0)));
        model.addItem(item("c", at(5, 14, 0), at(5, 15, 0)));
        DayGrid grid = (DayGrid) show(model, Scheduler.View.DAY).surface();

        DayGrid.Box a = box(grid, "a");
        DayGrid.Box b = box(grid, "b");
        DayGrid.Box c = box(grid, "c");
        assertEquals(2, a.lanes);
        assertEquals(2, b.lanes);
        assertTrue(a.lane != b.lane, "two items at the same time are side by side");
        assertEquals(1, c.lanes, "an item alone has the whole column");

        int[] ra = new int[4];
        int[] rb = new int[4];
        grid.boxRect(a, ra);
        grid.boxRect(b, rb);
        assertTrue(ra[0] + ra[2] <= rb[0] || rb[0] + rb[2] <= ra[0], "their rectangles do not overlap");
    }

    @FormTest
    void anItemAcrossMidnightIsInBothDays() {
        DefaultScheduleModel model = new DefaultScheduleModel();
        model.addItem(item("late", at(5, 22, 0), at(6, 2, 0)));
        DayGrid grid = (DayGrid) show(model, Scheduler.View.WEEK).surface();

        assertEquals(2, grid.boxes().size());
        DayGrid.Box first = grid.boxes().get(0).col == 0 ? grid.boxes().get(0) : grid.boxes().get(1);
        DayGrid.Box second = first == grid.boxes().get(0) ? grid.boxes().get(1) : grid.boxes().get(0);
        assertEquals(22 * 60, first.startMin);
        assertEquals(TimeMath.MINUTES_PER_DAY, first.endMin);
        assertTrue(first.endsAfter);
        assertEquals(1, second.col);
        assertEquals(0, second.startMin);
        assertEquals(2 * 60, second.endMin);
        assertTrue(second.startsBefore);
    }

    @FormTest
    void allDayAndLongItemsGoToTheLaneAbove() {
        DefaultScheduleModel model = new DefaultScheduleModel();
        model.addItem(item("holiday", at(6, 0, 0), at(7, 0, 0)).setAllDay(true));
        model.addItem(item("trip", at(7, 8, 0), at(9, 18, 0)));
        model.addItem(item("call", at(6, 9, 0), at(6, 10, 0)));
        DayGrid grid = (DayGrid) show(model, Scheduler.View.WEEK).surface();

        assertEquals(1, grid.boxes().size());
        assertEquals(2, grid.chips().size());
        for (DayGrid.Chip chip : grid.chips()) {
            if (chip.item.getId().equals("holiday")) {
                assertEquals(1, chip.fromCol);
                assertEquals(1, chip.toCol, "a day that ends at midnight is not on the next one");
            } else {
                assertEquals(2, chip.fromCol);
                assertEquals(4, chip.toCol);
            }
        }
    }

    @FormTest
    void aDragMovesAnItemBySnappedTime() {
        DefaultScheduleModel model = new DefaultScheduleModel();
        ScheduleItem a = item("a", at(5, 9, 0), at(5, 10, 0));
        model.addItem(a);
        Scheduler s = show(model, Scheduler.View.DAY);
        DayGrid grid = (DayGrid) s.surface();

        int[] r = new int[4];
        grid.boxRect(box(grid, "a"), r);
        int x = grid.getHeaderWidth() + r[0] + r[2] / 2 - grid.getOffsetX();
        int y = grid.getHeaderHeight() + r[1] + r[3] / 2 - grid.getOffsetY();
        // Two hours and ten minutes down snaps to two hours and a quarter.
        int down = Math.round(grid.getHourHeight() * 130 / 60f);
        drag(grid, x, y, x, y + down);

        assertEquals(at(5, 11, 15), a.getStart());
        assertEquals(at(5, 12, 15), a.getEnd());
        assertSame(a, s.getSelectedItem());
    }

    @FormTest
    void aListenerCanRefuseAChange() {
        DefaultScheduleModel model = new DefaultScheduleModel();
        ScheduleItem a = item("a", at(5, 9, 0), at(5, 10, 0));
        model.addItem(a);
        Scheduler s = show(model, Scheduler.View.DAY);
        final List<String> asked = new ArrayList<String>();
        s.addSchedulerListener(new SchedulerAdapter() {
            @Override
            public boolean itemChanging(Scheduler sc, ScheduleItem item, long start, long end, String resourceId) {
                asked.add(item.getId());
                return false;
            }
        });
        DayGrid grid = (DayGrid) s.surface();
        int[] r = new int[4];
        grid.boxRect(box(grid, "a"), r);
        int x = grid.getHeaderWidth() + r[0] + r[2] / 2 - grid.getOffsetX();
        int y = grid.getHeaderHeight() + r[1] + r[3] / 2 - grid.getOffsetY();
        drag(grid, x, y, x, y + Math.round(grid.getHourHeight() * 2));

        assertEquals(1, asked.size());
        assertEquals(at(5, 9, 0), a.getStart(), "a refused change leaves the item where it was");
    }

    @FormTest
    void theBottomEdgeResizes() {
        DefaultScheduleModel model = new DefaultScheduleModel();
        ScheduleItem a = item("a", at(5, 9, 0), at(5, 11, 0));
        model.addItem(a);
        DayGrid grid = (DayGrid) show(model, Scheduler.View.DAY).surface();
        int[] r = new int[4];
        grid.boxRect(box(grid, "a"), r);
        int x = grid.getHeaderWidth() + r[0] + r[2] / 2 - grid.getOffsetX();
        int y = grid.getHeaderHeight() + r[1] + r[3] - 1 - grid.getOffsetY();
        drag(grid, x, y, x, y + Math.round(grid.getHourHeight()));

        assertEquals(at(5, 9, 0), a.getStart());
        assertEquals(at(5, 12, 0), a.getEnd());
    }

    @FormTest
    void aDragOnEmptyTimeSelectsARangeAndCreates() {
        DefaultScheduleModel model = new DefaultScheduleModel();
        model.setCreationEnabled(true);
        Scheduler s = show(model, Scheduler.View.DAY);
        final long[] range = new long[2];
        s.addSchedulerListener(new SchedulerAdapter() {
            @Override
            public void rangeSelected(Scheduler sc, long start, long end, String resourceId) {
                range[0] = start;
                range[1] = end;
            }
        });
        DayGrid grid = (DayGrid) s.surface();
        float hour = grid.getHourHeight();
        int x = grid.getHeaderWidth() + grid.getBodyWidth() / 2;
        int from = grid.getHeaderHeight() + Math.round(hour * 13) + 1 - grid.getOffsetY();
        int to = grid.getHeaderHeight() + Math.round(hour * 15) + 1 - grid.getOffsetY();
        drag(grid, x, from, x, to);

        assertEquals(at(5, 13, 0), range[0]);
        assertEquals(at(5, 15, 0), range[1]);
        assertEquals(1, model.getAllItems().size());
        assertSame(model.getAllItems().get(0), s.getSelectedItem());
    }

    @FormTest
    void aTapSelectsAndADoubleTapOpens() {
        DefaultScheduleModel model = new DefaultScheduleModel();
        ScheduleItem a = item("a", at(5, 9, 0), at(5, 11, 0));
        model.addItem(a);
        Scheduler s = show(model, Scheduler.View.DAY);
        final List<String> events = new ArrayList<String>();
        s.addSchedulerListener(new SchedulerAdapter() {
            @Override
            public void itemSelected(Scheduler sc, ScheduleItem item) {
                events.add("selected " + (item == null ? null : item.getId()));
            }

            @Override
            public void itemOpened(Scheduler sc, ScheduleItem item) {
                events.add("opened " + item.getId());
            }
        });
        DayGrid grid = (DayGrid) s.surface();
        int[] r = new int[4];
        grid.boxRect(box(grid, "a"), r);
        int x = grid.getHeaderWidth() + r[0] + r[2] / 2 - grid.getOffsetX();
        int y = grid.getHeaderHeight() + r[1] + r[3] / 2 - grid.getOffsetY();
        tap(grid, x, y);
        assertEquals("[selected a]", events.toString());
        tap(grid, x, y);
        assertEquals("[selected a, opened a]", events.toString());

        // A tap on empty time lets go of the selection.
        tap(grid, x, grid.getHeaderHeight() + Math.round(grid.getHourHeight() * 20) - grid.getOffsetY());
        assertNull(s.getSelectedItem());
    }

    @FormTest
    void aReadOnlySchedulerDoesNotMoveItems() {
        DefaultScheduleModel model = new DefaultScheduleModel();
        ScheduleItem a = item("a", at(5, 9, 0), at(5, 10, 0));
        model.addItem(a);
        Scheduler s = show(model, Scheduler.View.DAY);
        s.setEditable(false);
        DayGrid grid = (DayGrid) s.surface();
        int[] r = new int[4];
        grid.boxRect(box(grid, "a"), r);
        int x = grid.getHeaderWidth() + r[0] + r[2] / 2 - grid.getOffsetX();
        int y = grid.getHeaderHeight() + r[1] + r[3] / 2 - grid.getOffsetY();
        drag(grid, x, y, x, y + Math.round(grid.getHourHeight() * 2));
        assertEquals(at(5, 9, 0), a.getStart());
    }

    @FormTest
    void stepsFollowTheView() {
        DefaultScheduleModel model = new DefaultScheduleModel();
        Scheduler s = show(model, Scheduler.View.WEEK);
        assertEquals(at(5, 0, 0), s.getVisibleStart());
        assertEquals(at(12, 0, 0), s.getVisibleEnd());
        final long[] seen = new long[2];
        s.addSchedulerListener(new SchedulerAdapter() {
            @Override
            public void visibleRangeChanged(Scheduler sc, long start, long end) {
                seen[0] = start;
                seen[1] = end;
            }
        });
        s.next();
        assertEquals(at(12, 0, 0), seen[0]);
        assertEquals(at(19, 0, 0), seen[1]);
        s.previous();
        s.setView(Scheduler.View.DAY);
        assertEquals(at(5, 0, 0), s.getVisibleStart());
        assertEquals(at(6, 0, 0), s.getVisibleEnd());
        s.setView(Scheduler.View.MONTH);
        // January 2026 starts on a Thursday: the grid opens on Monday the 29th.
        assertEquals(TimeMath.startOfEpochDay(LocalDate.of(2025, 12, 29).toEpochDay(), UTC), s.getVisibleStart());
        s.next();
        assertEquals(TimeMath.startOfEpochDay(LocalDate.of(2026, 1, 26).toEpochDay(), UTC), s.getVisibleStart());
    }

    @FormTest
    void theWorkWeekLeavesTheWeekendOut() {
        DefaultScheduleModel model = new DefaultScheduleModel();
        model.addItem(item("sat", at(10, 9, 0), at(10, 10, 0)));
        model.addItem(item("fri", at(9, 9, 0), at(9, 10, 0)));
        DayGrid grid = (DayGrid) show(model, Scheduler.View.WORK_WEEK).surface();
        assertEquals(1, grid.boxes().size());
        assertEquals("fri", grid.boxes().get(0).item.getId());
        assertEquals(4, grid.boxes().get(0).col);
    }

    @FormTest
    void theMonthListsEachDay() {
        DefaultScheduleModel model = new DefaultScheduleModel();
        model.addItem(item("a", at(5, 9, 0), at(5, 10, 0)));
        model.addItem(item("b", at(5, 8, 0), at(5, 8, 30)));
        model.addItem(item("trip", at(7, 8, 0), at(9, 18, 0)));
        MonthGrid grid = (MonthGrid) show(model, Scheduler.View.MONTH).surface();
        List<ScheduleItem> fifth = grid.itemsOn(day(5));
        assertEquals(2, fifth.size());
        assertEquals("b", fifth.get(0).getId(), "a day is in the order of its times");
        assertEquals(1, grid.itemsOn(day(7)).size());
        assertEquals(1, grid.itemsOn(day(8)).size());
        assertEquals(1, grid.itemsOn(day(9)).size());
        assertEquals(0, grid.itemsOn(day(10)).size());
    }

    @FormTest
    void theAgendaSkipsEmptyDays() {
        DefaultScheduleModel model = new DefaultScheduleModel();
        model.addItem(item("a", at(5, 9, 0), at(5, 10, 0)));
        model.addItem(item("b", at(8, 9, 0), at(8, 10, 0)));
        AgendaList list = (AgendaList) show(model, Scheduler.View.AGENDA).surface();
        assertEquals(4, list.rows().size());
        assertNull(list.rows().get(0).item);
        assertEquals(day(5), list.rows().get(0).day);
        assertEquals("a", list.rows().get(1).item.getId());
        assertEquals(day(8), list.rows().get(2).day);
        assertEquals("b", list.rows().get(3).item.getId());
    }

    @FormTest
    void theTimelineStacksWhatOverlapsAndMovesBetweenResources() {
        DefaultScheduleModel model = new DefaultScheduleModel();
        model.addResource(new ScheduleResource("r1", "Room one"));
        model.addResource(new ScheduleResource("r2", "Room two"));
        ScheduleItem a = item("a", at(5, 9, 0), at(5, 11, 0)).setResourceId("r1");
        ScheduleItem b = item("b", at(5, 10, 0), at(5, 12, 0)).setResourceId("r1");
        ScheduleItem c = item("c", at(5, 9, 0), at(5, 10, 0)).setResourceId("r2");
        model.addItem(a).addItem(b).addItem(c);
        Scheduler s = show(model, Scheduler.View.RESOURCE_TIMELINE);
        ResourceTimeline line = (ResourceTimeline) s.surface();

        int[] ra = new int[4];
        int[] rb = new int[4];
        int[] rc = new int[4];
        for (ResourceTimeline.Bar bar : line.bars()) {
            if (bar.item == a) {
                assertEquals(0, bar.lane);
                line.barRect(bar, ra);
            } else if (bar.item == b) {
                assertEquals(1, bar.lane);
                line.barRect(bar, rb);
            } else {
                assertEquals(1, bar.row);
                line.barRect(bar, rc);
            }
        }
        assertTrue(rb[1] >= ra[1] + ra[3], "the second of two at once is under the first");
        assertTrue(rc[1] > rb[1], "the second resource is below the whole of the first");

        // Drag the item of the second room up into the first.
        line.scrollTo(Math.max(0, rc[0] - 10), 0);
        int x = line.getHeaderWidth() + rc[0] + rc[2] / 2 - line.getOffsetX();
        int y = line.getHeaderHeight() + rc[1] + rc[3] / 2 - line.getOffsetY();
        drag(line, x, y, x, line.getHeaderHeight() + ra[1] + 1 - line.getOffsetY());
        assertEquals("r1", c.getResourceId());
        assertEquals(at(5, 9, 0), c.getStart(), "a move straight up changes the resource and not the time");
    }

    @FormTest
    void theResourceDayHasAColumnPerResource() {
        DefaultScheduleModel model = new DefaultScheduleModel();
        model.addResource(new ScheduleResource("r1", "Ann"));
        model.addResource(new ScheduleResource("r2", "Bob"));
        ScheduleItem a = item("a", at(5, 9, 0), at(5, 10, 0)).setResourceId("r2");
        model.addItem(a);
        model.addItem(item("other", at(5, 9, 0), at(5, 10, 0)).setResourceId("nobody"));
        Scheduler s = show(model, Scheduler.View.RESOURCE_DAY);
        DayGrid grid = (DayGrid) s.surface();
        assertEquals(1, grid.boxes().size());
        assertEquals(1, grid.boxes().get(0).col);

        int[] r = new int[4];
        grid.boxRect(grid.boxes().get(0), r);
        int x = grid.getHeaderWidth() + r[0] + r[2] / 2 - grid.getOffsetX();
        int y = grid.getHeaderHeight() + r[1] + r[3] / 2 - grid.getOffsetY();
        drag(grid, x, y, grid.getHeaderWidth() + 2, y);
        assertEquals("r1", a.getResourceId());
        assertEquals(at(5, 9, 0), a.getStart());
    }

    @FormTest
    void theAxisPicksItsTicksFromTheScale() {
        TimeAxis axis = new TimeAxis(UTC);
        axis.setRange(at(5, 0, 0), at(12, 0, 0));
        axis.setScaleLimits(1000, 10 * TimeMath.DAY);
        axis.setMillisPerPixel(TimeMath.HOUR / 50.0);
        axis.pickUnits(40);
        assertEquals(TimeAxis.HOUR, axis.getMinorUnit());
        assertFalse(axis.snapsToDays());
        assertEquals(50, axis.x(at(5, 1, 0)));
        assertEquals(at(5, 2, 0), axis.time(100));

        axis.setMillisPerPixel(TimeMath.DAY / 50.0);
        axis.pickUnits(40);
        assertEquals(TimeAxis.DAY, axis.getMinorUnit());
        assertTrue(axis.snapsToDays());

        assertEquals(at(5, 0, 0), axis.floor(at(7, 13, 0), TimeAxis.WEEK, 1));
        assertEquals(at(1, 0, 0), axis.floor(at(7, 13, 0), TimeAxis.MONTH, 1));
        assertEquals(at(1, 0, 0), axis.floor(at(7, 13, 0), TimeAxis.QUARTER, 1));
        assertEquals(TimeMath.startOfEpochDay(LocalDate.of(2026, 4, 1).toEpochDay(), UTC),
                axis.next(at(1, 0, 0), TimeAxis.QUARTER, 1));
        assertEquals(at(5, 12, 0), axis.floor(at(5, 13, 59), TimeAxis.HOUR, 6));
        assertEquals(at(6, 0, 0), axis.next(at(5, 18, 0), TimeAxis.HOUR, 6));
        assertNotNull(axis.label(at(5, 18, 0), TimeAxis.HOUR, 100, com.codename1.ui.Font.getDefaultFont()));
    }
}
