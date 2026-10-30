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
            Ui.section(list, now.name());
            final Label rate = Ui.label(recording != null ? Lang.tr("Recording") : "", "SvMuted");
            rate.setName("bpm");
            list.add(rate);
            if (recording == null) {
                list.add(Ui.primary("Record this class", "recordClass", e -> {
                    Workouts.startClass(now, share, new Workouts.Progress() {
                        @Override
                        public void beat(int bpm) {
                            rate.setText(bpm + " bpm");
                            rate.repaint();
                        }

                        @Override
                        public void ended(WorkoutDto posted, String error) {
                            Ui.say(error == null ? Lang.tr("Workout saved") : error);
                            load.run();
                        }
                    });
                    load.run();
                }));
            } else {
                list.add(Ui.danger("End workout", "endClass", e -> Workouts.stopClass()));
            }
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
            WorkoutDto workout = workouts.get(iter);
            StringBuilder detail = new StringBuilder(Ui.when(workout.startedAt()));
            detail.append(Ui.DOT).append(Lang.tr("{0} min", Integer.valueOf(workout.minutes())));
            if (workout.avgHr() > 0) {
                detail.append(Ui.DOT).append(Lang.tr("avg {0} bpm",
                        Integer.valueOf(workout.avgHr())));
            }
            if (workout.calories() > 0) {
                detail.append(Ui.DOT).append(workout.calories()).append(" kcal");
            }
            list.add(Ui.row(FontImage.MATERIAL_DIRECTIONS_RUN, title(workout.activity()),
                    detail.toString(), workout.shared() ? Ui.badge(Lang.tr("Shared"),
                    "SvBadgeGood") : null));
        }
        if (workouts == null || workouts.isEmpty()) {
            list.add(Ui.text("No workouts yet.", "SvMuted"));
        }
        Ui.refresh(form);
    }

    /// `HIGH_INTENSITY_INTERVAL_TRAINING` as "High intensity interval training".
    static String title(String activity) {
        if (activity == null || activity.length() == 0) {
            return Lang.tr("Workout");
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
