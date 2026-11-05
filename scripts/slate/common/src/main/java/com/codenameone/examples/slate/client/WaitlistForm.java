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
package com.codenameone.examples.slate.client;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.slate.Telemetry;
import com.codenameone.examples.slate.api.ServiceDto;
import com.codenameone.examples.slate.api.WaitlistDto;
import com.codenameone.examples.slate.api.WaitlistRequestDto;
import com.codenameone.examples.slate.net.Api;
import com.codenameone.examples.slate.net.Net;
import com.codenameone.examples.slate.ui.Lang;
import com.codenameone.examples.slate.ui.Layouts;
import com.codenameone.examples.slate.ui.Ui;

import java.util.List;

/// Times the client is waiting for. When a booking is cancelled the server
/// tells the first waiter whose window the freed start fits, by push; the
/// time is not held, so whoever books it first has it.
public final class WaitlistForm {
    private static final String[] WINDOWS = {"Any time", "Mornings", "Afternoons", "Evenings"};
    private static final String[] WINDOW_KEYS = {"any", "am", "pm", "eve"};
    private static final int[][] MINUTES = {{0, 1440}, {360, 720}, {720, 1020}, {1020, 1440}};

    private final Form form = Ui.form("Waiting list", "Waitlist");
    private final Container list = Ui.list("waitlist");

    private WaitlistForm(Form previous) {
        Ui.back(form, previous);
        form.add(BorderLayout.CENTER, list);
        Ui.pull(list, this::load);
        load();
    }

    public static void show(Form previous) {
        new WaitlistForm(previous).form.show();
    }

    /// Asks which part of the day suits, then joins the list for `day` and
    /// the six days after it.
    public static void join(final Form previous, final ServiceDto service, final String provider,
            final long day) {
        final Form ask = Ui.form("Waiting list", "WaitlistJoin");
        Ui.back(ask, previous);
        Container page = Ui.page();
        page.add(Ui.text(Lang.tr("If a {0} frees up in the week from {1}, you get a message "
                + "straight away.", service.name(), Ui.day(day)), "SvText"));
        final int[] window = {0};
        page.add(Ui.choice("window", WINDOWS, WINDOW_KEYS, WINDOW_KEYS[0], chosen -> {
            for (int iter = 0; iter < WINDOW_KEYS.length; iter++) {
                if (WINDOW_KEYS[iter].equals(chosen)) {
                    window[0] = iter;
                }
            }
        }, 2));
        page.add(Ui.primary("Join the waiting list", "join", e -> Api.appointments().join(
                new WaitlistRequestDto(service.id(), provider, Times.iso(day),
                        Times.iso(Times.addDays(day, 6)),
                        MINUTES[window[0]][0], MINUTES[window[0]][1]),
                Net.to(joined -> {
                    Telemetry.waitlisted();
                    Ui.say("You are on the waiting list");
                    previous.showBack();
                }, Ui::fail))));
        Layouts.broaden(page);
        ask.add(BorderLayout.CENTER, page);
        ask.show();
    }

    private void load() {
        Api.appointments().waitlist(Net.to(this::show, Ui.failed(list, this::load)));
    }

    private void show(List<WaitlistDto> waits) {
        list.removeAll();
        if (waits.isEmpty()) {
            list.add(Ui.empty(FontImage.MATERIAL_HOURGLASS_EMPTY, "Not waiting for anything",
                    "When a day is full, ask to be told if a time frees up."));
        }
        for (int iter = 0; iter < waits.size(); iter++) {
            final WaitlistDto w = waits.get(iter);
            String detail = w.fromDay().equals(w.toDay()) ? w.fromDay()
                    : w.fromDay() + " - " + w.toDay();
            if ("OFFERED".equals(w.state())) {
                detail = Lang.tr("A time you wanted is free");
            }
            list.add(Ui.row(FontImage.MATERIAL_HOURGLASS_TOP, w.serviceName(), detail,
                    Ui.link("Leave", "leave-" + w.id(), e -> Api.appointments().leave(w.id(),
                            Net.to(left -> load(), Ui::fail)))));
        }
        Ui.relayout(list);
    }
}
