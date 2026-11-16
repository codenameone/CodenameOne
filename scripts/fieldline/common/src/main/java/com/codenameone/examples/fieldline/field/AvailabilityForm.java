package com.codenameone.examples.fieldline.field;

import com.codename1.appkit.Net;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Slider;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.spinner.Picker;
import com.codenameone.examples.fieldline.api.AvailabilityDto;
import com.codenameone.examples.fieldline.api.WindowDto;
import com.codenameone.examples.fieldline.net.Api;
import com.codenameone.examples.fieldline.ui.Lang;
import com.codenameone.examples.fieldline.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// The constraints a technician sets on their own week: which days, which
/// hours, how far they will drive between jobs, how many hours at most.
///
/// Saving is not just storing. The planner treats these as hard constraints,
/// so it re-plans the coming days at once and moves whatever no longer fits
/// to somebody else -- and the screen says so.
public final class AvailabilityForm {
    private static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday",
        "Friday", "Saturday", "Sunday"};

    private AvailabilityForm() {
    }

    public static void show(final Form previous) {
        final Form form = Ui.form("Working hours", "Availability");
        Ui.back(form, previous);
        final Container page = Ui.page();
        page.add(Ui.label("Loading...", "WlMuted"));
        form.add(BorderLayout.CENTER, page);
        form.show();
        Api.field().availability(Net.to(a -> fill(form, previous, page, a), Ui::fail));
    }

    private static void fill(final Form form, final Form previous, Container page,
            final AvailabilityDto a) {
        page.removeAll();
        page.add(Ui.banner(FontImage.MATERIAL_TUNE, "You set the limits",
                "The planner never books you outside these hours, and re-plans the week "
                + "the moment you change them.", "FlBannerIcon"));
        final boolean[] on = new boolean[7];
        final Picker[] from = new Picker[7];
        final Picker[] to = new Picker[7];
        for (int d = 0; d < 7; d++) {
            from[d] = time(d, "from", 8 * 60);
            to[d] = time(d, "to", 17 * 60);
        }
        for (WindowDto w : a.week()) {
            // The server numbers the week 1 (Monday) to 7; the rows here
            // are 0 to 6. Read as row numbers, Monday's hours showed under
            // Tuesday, Sunday's were dropped, and saving Monday sent a 0 the
            // server refuses.
            int row = w.day() - 1;
            if (row >= 0 && row < 7) {
                on[row] = true;
                from[row].setTime(w.start());
                to[row].setTime(w.end());
            }
        }
        Ui.section(page, "Week");
        Container week = Ui.card();
        for (int d = 0; d < 7; d++) {
            final int day = d;
            final Container times = new Container(new GridLayout(1, 2));
            times.add(from[d]).add(to[d]);
            times.setHidden(!on[d]);
            Container head = Ui.row((char) 0, DAYS[d], null, Ui.toggle("day-" + d, on[d], value -> {
                on[day] = value;
                times.setHidden(!value);
                form.animateLayout(150);
            }));
            Container line = new Container(BoxLayout.y());
            line.setUIID("FlDay");
            line.add(head).add(times);
            week.add(line);
        }
        page.add(week);

        Ui.section(page, "Limits");
        Container limits = Ui.card();
        final Slider drive = slider("maxDrive", 15, 90, a.maxDriveMinutes() <= 0 ? 45
                : a.maxDriveMinutes());
        final Label driveValue = Ui.label("", "FlSliderValue");
        limits.add(labelled("Longest drive between jobs", driveValue, drive,
                v -> Ui.duration(v)));
        final Slider hours = slider("maxHours", 20, 60, a.maxHoursPerWeek() <= 0 ? 40
                : a.maxHoursPerWeek());
        final Label hoursValue = Ui.label("", "FlSliderValue");
        limits.add(labelled("Most hours in a week", hoursValue, hours,
                v -> Lang.tr("{0} h", String.valueOf(v))));
        page.add(limits);

        final Button save = Ui.primary("Save and re-plan", "saveAvailability", null);
        save.addActionListener(e -> {
            List<WindowDto> windows = new ArrayList<WindowDto>();
            for (int d = 0; d < 7; d++) {
                if (on[d]) {
                    if (to[d].getTime() <= from[d].getTime()) {
                        Ui.say(Lang.tr("{0} ends before it starts", Lang.tr(DAYS[d])));
                        return;
                    }
                    windows.add(new WindowDto(d + 1, from[d].getTime(), to[d].getTime()));
                }
            }
            save.setEnabled(false);
            AvailabilityDto change = new AvailabilityDto(a.techId(), windows,
                    drive.getProgress(), hours.getProgress(), a.regularHoursPerWeek());
            Api.field().saveAvailability(change, Net.to(saved -> {
                Ui.say("Saved. Your week was re-planned around it.");
                previous.showBack();
            }, (status, message) -> {
                save.setEnabled(true);
                Ui.fail(status, message);
            }));
        });
        page.add(save);
        form.revalidate();
    }

    private interface Format {
        String format(int value);
    }

    private static Container labelled(String caption, final Label value, final Slider slider,
            final Format format) {
        Container box = new Container(BoxLayout.y());
        Container head = new Container(new BorderLayout());
        head.add(BorderLayout.CENTER, Ui.label(caption, "FlCardText"));
        head.add(BorderLayout.EAST, value);
        value.setText(format.format(slider.getProgress()));
        slider.addDataChangedListener((type, index) -> {
            value.setText(format.format(slider.getProgress()));
            value.getParent().revalidate();
        });
        box.add(head).add(slider);
        return box;
    }

    private static Slider slider(String name, int min, int max, int value) {
        Slider s = new Slider();
        s.setName(name);
        s.setMinValue(min);
        s.setMaxValue(max);
        s.setProgress(Math.max(min, Math.min(max, value)));
        s.setEditable(true);
        s.setUIID("FlSlider");
        return s;
    }

    private static Picker time(int day, String which, int minutes) {
        Picker p = new Picker();
        p.setType(Display.PICKER_TYPE_TIME);
        p.setTime(minutes);
        p.setName(which + "-" + day);
        p.setUIID("FlTimePicker");
        return p;
    }
}
