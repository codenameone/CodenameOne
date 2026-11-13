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

import com.codename1.scheduling.DefaultScheduleModel;
import com.codename1.scheduling.ScheduleItem;
import com.codename1.scheduling.ScheduleResource;
import com.codename1.scheduling.TimeMath;
import com.codename1.scheduling.WorkCalendar;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.scheduler.Scheduler;
import com.codename1.ui.scheduler.SchedulerAdapter;

import java.util.ArrayList;
import java.util.List;

// tag::slate-timeline[]
/// Time running down the page, one lane per day or per person: the hours a
/// provider works as a band, bookings as blocks as long as they last, busy
/// time from a calendar hatched, and a line at the current time.
///
/// It is the provider's day (one lane), their week (seven) and the owner's
/// board (a lane per provider), and all three are one `Scheduler` in a
/// different view. What this class adds is the translation: a lane becomes a
/// day or a `ScheduleResource`, a block a `ScheduleItem` of the kind that
/// draws it -- a band under the bookings, a hatched block, a booking a tap
/// opens. The scheduler does the rest: the pinned hours and lane titles,
/// scrolling both ways, pinch to zoom, bookings that overlap side by side.
///
/// Colours come from `theme.css`. `Scheduler`, `SchedulerGrid`,
/// `SchedulerHeader` and the others style the grid, and each item names the
/// style it is painted with -- `SvTimelineShift`, `SvTimelineBusy`,
/// `SvTimelineBooking`, `SvTimelineDone`, `SvTimelineNoShow` -- so dark mode
/// is a stylesheet change, as it is for every other screen.
public final class Timeline extends Container {
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
    private final Scheduler scheduler;
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

    /// @param headers whether each lane's title is drawn above it; a single
    ///                day is always headed by its date
    public Timeline(List<Lane> lanes, boolean headers) {
        super(new BorderLayout());
        this.lanes = lanes;
        this.headers = headers;
        setUIID("SvTimeline");
        fitHours();
        scheduler = new Scheduler(model());
        // Slate has its own heading, with the range and the two arrows, and
        // the forms decide what is shown: the scheduler is only the grid.
        scheduler.setToolbarVisible(false);
        // Moving a booking is the client's to do, inside the business's
        // rescheduling window, so nothing here is dragged.
        scheduler.setEditable(false);
        scheduler.setOpenOnTap(true);
        // The hours somebody works are bands of their own, drawn from the
        // shifts the server sent; the scheduler's own working week would
        // shade nine to five over them.
        scheduler.setWorkCalendar(WorkCalendar.continuous(scheduler.getZone()));
        // A little before the first hour, so its label is not cut in half by
        // the header above it.
        scheduler.setInitialMinute(Math.max(0, firstHour * 60 - 20));
        if (!lanes.isEmpty()) {
            long first = lanes.get(0).dayStart;
            if (!days()) {
                scheduler.setView(lanes.size() > 1 || headers
                        ? Scheduler.View.RESOURCE_DAY : Scheduler.View.DAY);
            } else {
                // A week that starts on the day it was asked from, whichever
                // day that is.
                scheduler.setFirstDayOfWeek(TimeMath.dayOfWeek(
                        TimeMath.epochDay(first, scheduler.getZone())));
                scheduler.setView(Scheduler.View.WEEK);
            }
            scheduler.setDate(first);
        }
        scheduler.addSchedulerListener(new SchedulerAdapter() {
            @Override
            public void itemOpened(Scheduler s, ScheduleItem item) {
                Object block = item.getUserObject();
                if (block instanceof Block && ((Block) block).tap != null) {
                    ((Block) block).tap.run();
                }
            }
        });
        add(BorderLayout.CENTER, scheduler);
    }

    public List<Lane> lanes() {
        return lanes;
    }

    /// The scheduler the lanes are shown in, for a test.
    public Scheduler scheduler() {
        return scheduler;
    }

    // The lanes as a schedule: a resource per lane when they are people on
    // one day, and an item per block.
    private DefaultScheduleModel model() {
        DefaultScheduleModel model = new DefaultScheduleModel();
        boolean people = !days() && (lanes.size() > 1 || headers);
        int n = 0;
        for (int l = 0; l < lanes.size(); l++) {
            Lane lane = lanes.get(l);
            String resource = null;
            if (people) {
                resource = "lane-" + l;
                model.addResource(new ScheduleResource(resource, lane.title));
            }
            for (int iter = 0; iter < lane.blocks.size(); iter++) {
                Block b = lane.blocks.get(iter);
                if (b.end <= b.start) {
                    continue;
                }
                ScheduleItem item = new ScheduleItem("block-" + n++, b.title, b.start, b.end);
                item.setResourceId(resource);
                item.setUserObject(b);
                if (b.kind == SHIFT) {
                    item.setKind(ScheduleItem.Kind.BACKGROUND);
                    item.setUIID("SvTimelineShift");
                } else if (b.kind == BUSY) {
                    // Hatched, so it never reads as a booking that lost its
                    // color.
                    item.setKind(ScheduleItem.Kind.BLOCKED);
                    item.setUIID("SvTimelineBusy");
                } else {
                    item.setDetail(b.detail.length() == 0 ? null : b.detail);
                    item.setUIID(b.kind == DONE ? "SvTimelineDone"
                            : b.kind == NO_SHOW ? "SvTimelineNoShow" : "SvTimelineBooking");
                }
                model.addItem(item);
            }
        }
        return model;
    }

    /// The hours it opens on: exactly the hours something is in -- shifts
    /// included, so a day opens on working time rather than on the closed
    /// early morning -- and at least six of them; 09 to 18 when there is
    /// nothing at all. The rest of the day is a scroll away.
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

    // Whether the lanes are days rather than people on one day.
    private boolean days() {
        for (int iter = 1; iter < lanes.size(); iter++) {
            if (lanes.get(iter).dayStart != lanes.get(0).dayStart) {
                return true;
            }
        }
        return false;
    }

    /// What it asks for when nothing gives it a size: a readable lane each,
    /// and the hours it opens on. Put it where it is given the room there is
    /// -- the centre of a `BorderLayout` -- and it scrolls inside that.
    @Override
    protected Dimension calcPreferredSize() {
        int lanesWide = Math.max(1, lanes.size()) * CN.convertToPixels(MIN_LANE_MM);
        return new Dimension(CN.convertToPixels(GUTTER_MM) + lanesWide,
                CN.convertToPixels(HEADER_MM) + (lastHour - firstHour) * CN.convertToPixels(HOUR_MM));
    }
}
// end::slate-timeline[]
