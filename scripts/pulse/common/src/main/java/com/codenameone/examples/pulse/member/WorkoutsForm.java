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
package com.codenameone.examples.pulse.member;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.pulse.ui.ZoneBar;
import com.codenameone.examples.pulse.ui.Beat;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.CN;
import com.codename1.ui.Button;
import com.codenameone.examples.pulse.api.BookingDto;
import com.codenameone.examples.pulse.api.SessionDto;
import com.codenameone.examples.pulse.api.WorkoutDto;
import com.codenameone.examples.pulse.health.Workouts;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// The member's workouts: classes recorded here and workouts brought in from
/// the phone's health store. During a booked class this is where recording
/// it starts, which also shares the heart rate with the trainer.
public final class WorkoutsForm {
    private static boolean share = true;

    private WorkoutsForm() {
    }

    public static void show(final Form previous) {
        final Form form = Ui.form("Workouts", "Workouts");
        Ui.back(form, previous);
        final Container list = Ui.list("workouts");
        final Runnable[] load = new Runnable[1];
        load[0] = () -> Api.engagement().workouts(Net.to(workouts ->
                Api.bookings().upcoming(Net.to(upcoming ->
                        fill(form, list, workouts, upcoming, load[0]), Ui.failed(list, load[0]))),
                Ui.failed(list, load[0])));
        Ui.pull(list, load[0]);
        form.add(BorderLayout.CENTER, list);
        form.show();
        load[0].run();
    }

    private static void fill(final Form form, Container list, List<WorkoutDto> workouts,
            List<BookingDto> upcoming, final Runnable load) {
        list.removeAll();
        list.add(Ui.row((char) 0, Lang.tr("Share with friends"), null,
                Ui.toggle("share", share, on -> share = on)));
        List<SessionDto> booked = new ArrayList<SessionDto>();
        for (int iter = 0; upcoming != null && iter < upcoming.size(); iter++) {
            if (Classes.BOOKED.equals(upcoming.get(iter).state())
                    || Classes.CHECKED_IN.equals(upcoming.get(iter).state())) {
                booked.add(upcoming.get(iter).session());
            }
        }
        SessionDto recording = Workouts.recording();
        final SessionDto now = recording != null ? recording
                : Workouts.classNow(booked, System.currentTimeMillis());
        if (now != null) {
            // The class on now, on the same ink card as the home's next class,
            // with the heart rate big enough to read mid-set.
            Container live = new Container(BoxLayout.y(), "SvInk");
            live.setName("live");
            Container head = new Container(new BorderLayout(), "SvInkRow");
            head.add(BorderLayout.CENTER, Ui.label(recording != null ? "RECORDING" : "ON NOW",
                    "SvInkEyebrow"));
            Beat beat = new Beat("SvBeat", recording != null);
            beat.setPreferredW(CN.convertToPixels(14));
            beat.setPreferredH(CN.convertToPixels(5));
            head.add(BorderLayout.EAST, beat);
            live.add(head);
            live.add(Ui.text(now.name(), "SvInkTitle"));
            final Label rate = Ui.label(recording != null ? "--" : "", "SvInkBpm");
            rate.setName("bpm");
            live.add(rate);
            live.add(Ui.text(recording != null
                    ? "Wear a strap or your watch and this fills in. Worn sensors earn a bonus."
                    : "Record it to log the workout and earn points for the class.",
                    "SvInkText"));
            Button act;
            if (recording == null) {
                act = new Button(Lang.tr("Record this class"), "SvInkButton");
                act.setName("recordClass");
                act.addActionListener(e -> {
                    Workouts.startClass(now, share, new Workouts.Progress() {
                        @Override
                        public void beat(int bpm) {
                            rate.setText(bpm + " bpm");
                            rate.repaint();
                        }

                        @Override
                        public void ended(WorkoutDto posted, String error) {
                            Ui.say(error != null ? error : posted != null && posted.points() > 0
                                    ? Lang.tr("Workout saved: +{0} points",
                                            Integer.valueOf(posted.points()))
                                    : Lang.tr("Workout saved"));
                            load.run();
                        }
                    });
                    load.run();
                });
            } else {
                act = new Button(Lang.tr("End workout"), "SvInkButton");
                act.setName("endClass");
                act.addActionListener(e -> Workouts.stopClass());
            }
            live.add(FlowLayout.encloseIn(act));
            list.add(live);
        }
        if (Workouts.storeAvailable()) {
            list.add(Ui.secondary("Bring in recent workouts", "importHealth",
                    e -> Workouts.importRecent(share, count -> {
                        int posted = count.intValue();
                        Ui.say(posted < 0 ? Lang.tr("Health data could not be read")
                                : Lang.tr("{0} workouts added", count));
                        load.run();
                    })));
        }
        Ui.section(list, "Recent");
        for (int iter = 0; workouts != null && iter < workouts.size(); iter++) {
            list.add(row(workouts.get(iter), iter));
        }
        if (workouts == null || workouts.isEmpty()) {
            list.add(Ui.text("No workouts yet.", "SvMuted"));
        }
        Ui.refresh(form);
    }

    private static Container row(WorkoutDto workout, int index) {
        boolean strap = Workouts.SOURCE_STRAP.equals(workout.source());
        boolean watch = Workouts.SOURCE_WATCH.equals(workout.source());
        Container row = new Container(new BorderLayout(), "SvRow");
        row.setName("workout-" + index);
        Label icon = new Label("", strap || watch ? "SvWorkoutWorn" : "SvRowIcon");
        Ui.icon(icon, strap ? FontImage.MATERIAL_MONITOR_HEART : watch ? FontImage.MATERIAL_WATCH
                : FontImage.MATERIAL_DIRECTIONS_RUN, 4f);
        icon.setAccessibilityText(strap ? Lang.tr("Measured by a strap")
                : watch ? Lang.tr("Measured by a watch") : "");
        row.add(BorderLayout.WEST, icon);
        Container lines = new Container(BoxLayout.y());
        lines.add(Ui.label(title(workout.activity()), "SvRowTitle"));
        StringBuilder detail = new StringBuilder(Ui.when(workout.startedAt()));
        detail.append(Ui.DOT).append(Lang.tr("{0} min", Integer.valueOf(workout.minutes())));
        if (workout.avgHr() > 0) {
            detail.append(Ui.DOT).append(Lang.tr("{0} bpm", Integer.valueOf(workout.avgHr())));
        }
        if (workout.calories() > 0) {
            detail.append(Ui.DOT).append(workout.calories()).append(" kcal");
        }
        lines.add(Ui.text(detail.toString(), "SvRowDetail"));
        ZoneBar zones = new ZoneBar(workout.zones());
        if (zones.hasData()) {
            lines.add(zones);
        }
        row.add(BorderLayout.CENTER, lines);
        if (workout.points() > 0) {
            Label points = Ui.label("+" + workout.points(), "SvPoints");
            points.setAccessibilityText(Lang.tr("{0} points", Integer.valueOf(workout.points())));
            row.add(BorderLayout.EAST, FlowLayout.encloseCenterMiddle(points));
        }
        return row;
    }

    /// `HIGH_INTENSITY_INTERVAL_TRAINING` as "High intensity interval training";
    /// a name that is not shouted, "HIIT Burn", as it is.
    static String title(String activity) {
        if (activity == null || activity.length() == 0) {
            return Lang.tr("Workout");
        }
        for (int iter = 0; iter < activity.length(); iter++) {
            char c = activity.charAt(iter);
            if (c >= 'a' && c <= 'z') {
                return activity;
            }
        }
        String words = activity.replace('_', ' ');
        StringBuilder out = new StringBuilder(words.length());
        out.append(words.charAt(0));
        for (int iter = 1; iter < words.length(); iter++) {
            char c = words.charAt(iter);
            out.append(c >= 'A' && c <= 'Z' ? (char) (c + ('a' - 'A')) : c);
        }
        return out.toString();
    }
}
