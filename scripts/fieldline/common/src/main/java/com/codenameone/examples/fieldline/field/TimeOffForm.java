package com.codenameone.examples.fieldline.field;

import com.codename1.appkit.Net;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.spinner.Picker;
import com.codenameone.examples.fieldline.Telemetry;
import com.codenameone.examples.fieldline.api.TimeOffDto;
import com.codenameone.examples.fieldline.net.Api;
import com.codenameone.examples.fieldline.ui.Lang;
import com.codenameone.examples.fieldline.ui.Ui;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/// Asking for time away, and what came of earlier asks.
///
/// A request goes to dispatch. When it is approved the planner takes the
/// technician off those days and gives their jobs to whoever can cover them
/// with the least disruption -- nobody here has to find a substitute.
public final class TimeOffForm {
    private static final String[] REASONS = {"VACATION", "SICK", "TRAINING", "PERSONAL"};
    private static final String[] REASON_NAMES = {"Vacation", "Sick", "Training", "Personal"};

    private TimeOffForm() {
    }

    public static void show(final Form previous) {
        final Form form = Ui.form("Time off", "TimeOff");
        Ui.back(form, previous);
        final Container page = Ui.page();
        form.add(BorderLayout.CENTER, page);

        Ui.section(page, "New request");
        Container card = Ui.card();
        final Picker from = date("from", 1);
        final Picker to = date("to", 1);
        Container dates = new Container(new GridLayout(1, 2));
        dates.add(from).add(to);
        card.add(Ui.label("From - to", "WlLabel"));
        card.add(dates);
        final String[] reason = {REASONS[0]};
        card.add(Ui.label("Reason", "WlLabel"));
        card.add(Ui.choice("reason", REASON_NAMES, REASONS, reason[0], v -> reason[0] = v, 2));
        final Button ask = Ui.primary("Send to dispatch", "requestTimeOff", null);
        ask.addActionListener(e -> {
            long start = dayStart(from.getDate());
            long end = dayStart(to.getDate()) + 24L * 3600000L;
            if (end <= start) {
                Ui.say("The last day is before the first");
                return;
            }
            ask.setEnabled(false);
            Api.field().requestTimeOff(new TimeOffDto("", "", "", start, end, reason[0],
                    "PENDING"), Net.to(sent -> {
                        Telemetry.timeOffRequested();
                        Ui.say("Sent. Your jobs are covered once it is approved.");
                        show(previous);
                    }, (status, message) -> {
                        ask.setEnabled(true);
                        Ui.fail(status, message);
                    }));
        });
        card.add(ask);
        page.add(card);

        Ui.section(page, "Your requests");
        final Container list = Ui.list("timeOffList");
        page.add(list);
        form.show();
        Api.field().myTimeOff(Net.to(all -> fill(list, all), Ui::fail));
    }

    private static void fill(Container list, List<TimeOffDto> all) {
        list.removeAll();
        if (all.isEmpty()) {
            list.add(Ui.empty(FontImage.MATERIAL_BEACH_ACCESS, "No requests yet",
                    "Days you ask for show here with what dispatch decided."));
        }
        for (TimeOffDto t : all) {
            String status = t.status();
            String style = "APPROVED".equals(status) ? "FlBadgeDone"
                    : "DECLINED".equals(status) ? "FlBadgeHigh" : "FlBadgeWarn";
            list.add(Ui.row(FontImage.MATERIAL_EVENT, range(t.start(), t.end()),
                    reasonName(t.reason()), Ui.badge(statusName(status), style)));
        }
        list.getComponentForm().revalidate();
    }

    public static String range(long start, long end) {
        long last = end - 1;
        if (Ui.day(start).equals(Ui.day(last))) {
            return Ui.day(start);
        }
        return Ui.day(start) + " - " + Ui.day(last);
    }

    public static String reasonName(String reason) {
        for (int i = 0; i < REASONS.length; i++) {
            if (REASONS[i].equals(reason)) {
                return REASON_NAMES[i];
            }
        }
        return reason == null ? "" : reason;
    }

    private static String statusName(String status) {
        if ("APPROVED".equals(status)) {
            return Lang.tr("Approved");
        }
        return "DECLINED".equals(status) ? Lang.tr("Declined") : Lang.tr("Waiting");
    }

    private static Picker date(String name, int daysAhead) {
        Picker p = new Picker();
        p.setType(Display.PICKER_TYPE_DATE);
        p.setDate(new Date(System.currentTimeMillis() + daysAhead * 24L * 3600000L));
        p.setName(name);
        p.setUIID("FlTimePicker");
        return p;
    }

    private static long dayStart(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime().getTime();
    }
}
