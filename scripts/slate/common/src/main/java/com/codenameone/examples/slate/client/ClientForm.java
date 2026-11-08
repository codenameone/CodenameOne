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
import com.codenameone.examples.slate.api.AppointmentDto;
import com.codenameone.examples.slate.api.BusinessDto;
import com.codenameone.examples.slate.api.ServiceDto;
import com.codenameone.examples.slate.api.UserDto;
import com.codenameone.examples.slate.live.LiveChannel;
import com.codenameone.examples.slate.net.Api;
import com.codename1.appkit.Net;
import com.codenameone.examples.slate.net.Session;
import com.codenameone.examples.slate.ui.Lang;
import com.codenameone.examples.slate.ui.Layouts;
import com.codenameone.examples.slate.ui.Menu;
import com.codenameone.examples.slate.ui.Nav;
import com.codenameone.examples.slate.ui.SettingsForm;
import com.codenameone.examples.slate.ui.Ui;

import java.util.List;

/// The client's home: the next appointment, and the menu of what can be
/// booked. Everything a client does starts from a service here.
///
/// The words are the business's: a salon books treatments with stylists, a
/// tutor's students book lessons. They come from `/api/business`, which the
/// server fills in from the vertical it is set up for.
public class ClientForm extends LiveChannel.Adapter implements Nav.Home {
    /// The business last read, for the words every client screen uses.
    private static BusinessDto business;

    private final Form form = Ui.home("Client");
    private final Menu menu = new Menu(form, Nav.CLIENT);
    private final Container next = Ui.card();
    private final Container services = new Container(com.codename1.ui.layouts.BoxLayout.y());

    /// The business, or a neutral stand-in until it has been read.
    public static BusinessDto business() {
        if (business == null) {
            return new BusinessDto("Slate", "", "", "UTC", "", "Provider", "Providers",
                    "Client", "Service", "Resource", "#db3416");
        }
        return business;
    }

    static void setBusiness(BusinessDto read) {
        business = read;
    }

    @Override
    public Form form() {
        return form;
    }

    @Override
    public void start() {
        menu.item(FontImage.MATERIAL_HOME, "Home", "home", () -> form.showBack());
        menu.item(FontImage.MATERIAL_EVENT_AVAILABLE, "Book", "book", () -> BookForm.show(form, null));
        menu.item(FontImage.MATERIAL_VIEW_AGENDA, "My appointments", "appointments",
                () -> AppointmentsForm.show(form));
        menu.item(FontImage.MATERIAL_HOURGLASS_EMPTY, "Waitlist", "waitlist",
                () -> WaitlistForm.show(form));

        UserDto user = Session.user();
        Container bar = new Container(new BorderLayout());
        bar.setUIID("SvHomeBar");
        bar.add(BorderLayout.WEST, menu.flatButton());
        bar.add(BorderLayout.CENTER, Ui.label(Lang.tr("Hello, {0}",
                user == null ? "" : first(user.displayName())), "SvTitle"));
        bar.add(BorderLayout.EAST, Ui.flat(FontImage.MATERIAL_VIEW_AGENDA, "My appointments",
                "home-appointments", e -> AppointmentsForm.show(form)));

        Container page = Ui.page();
        page.setName("home");
        next.setName("next");
        services.setName("services");
        // Side by side on a wide window: what is coming up on the left, what
        // can be booked on the right, both in view at once.
        Container[] halves = Layouts.halves(page);
        if (Layouts.wide()) {
            Layouts.broaden(page);
        }
        halves[0].add(next);
        halves[0].add(Ui.primary("Book an appointment", "home-book", e -> BookForm.show(form, null)));
        Ui.section(halves[1], "What you can book");
        halves[1].add(services);
        halves[0].add(Ui.link("Settings", "home-settings", e -> SettingsForm.show(form)));
        Ui.pull(page, this::load);

        Container shell = new Container(new BorderLayout());
        shell.add(BorderLayout.NORTH, bar);
        shell.add(BorderLayout.CENTER, page);
        form.add(shell);
        menu.install();
        form.addShowListener(e -> {
            LiveChannel.listen(this);
            load();
        });
    }

    @Override
    public void appointmentChanged(String id, String state) {
        load();
    }

    private void load() {
        Api.business().business(Net.to(b -> setBusiness(b), Ui::fail));
        Api.appointments().upcoming(Net.to(this::showNext, Ui::fail));
        Api.business().services(Net.to(this::showServices, Ui::fail));
        Reminders.resumed();
    }

    private void showNext(List<AppointmentDto> upcoming) {
        if (!Nav.isHome(form)) {
            return;
        }
        next.removeAll();
        AppointmentDto first = null;
        for (int iter = 0; iter < upcoming.size(); iter++) {
            AppointmentDto a = upcoming.get(iter);
            if (!"HELD".equals(a.state())) {
                first = a;
                break;
            }
        }
        if (first == null) {
            next.add(Ui.empty(FontImage.MATERIAL_EVENT, "Nothing booked",
                    "Pick a service below and see when it is free."));
        } else {
            final AppointmentDto shown = first;
            next.add(Ui.label("Next appointment", "SvCaption"));
            next.add(Ui.label(shown.serviceName(), "SvHeading"));
            next.add(Ui.text(Ui.weekday(shown.start()) + Ui.DOT + Times.clock(shown.start())
                    + Ui.DOT + Appointments.with(shown), "SvText"));
            next.add(Ui.link("Details", "next-details",
                    e -> AppointmentForm.show(form, shown)));
        }
        Ui.relayout(next);
    }

    private void showServices(List<ServiceDto> list) {
        if (!Nav.isHome(form)) {
            return;
        }
        services.removeAll();
        for (int iter = 0; iter < list.size(); iter++) {
            final ServiceDto s = list.get(iter);
            String detail = Times.length(s.minutes()) + Ui.DOT + Ui.money(s.priceCents(), s.currency())
                    + (s.capacity() > 1 ? Ui.DOT + Lang.tr("Group of {0}", String.valueOf(s.capacity())) : "")
                    + (s.online() ? Ui.DOT + Lang.tr("Online") : "");
            services.add(Ui.tap(Ui.row(FontImage.MATERIAL_SPA, s.name(), detail, null),
                    "service-" + s.id(), e -> BookForm.show(form, s)));
        }
        Ui.relayout(services);
    }

    private static String first(String name) {
        if (name == null) {
            return "";
        }
        int space = name.indexOf(' ');
        return space > 0 ? name.substring(0, space) : name;
    }
}
