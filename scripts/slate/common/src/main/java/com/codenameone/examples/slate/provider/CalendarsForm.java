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
package com.codenameone.examples.slate.provider;

import com.codename1.calendar.CalendarAccess;
import com.codename1.calendar.CalendarInfo;
import com.codename1.io.Util;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.slate.Telemetry;
import com.codenameone.examples.slate.api.CalendarConfigDto;
import com.codenameone.examples.slate.api.CalendarLinkDto;
import com.codenameone.examples.slate.api.ConnectCalendarDto;
import com.codenameone.examples.slate.calendar.DeviceCalendar;
import com.codenameone.examples.slate.net.Api;
import com.codenameone.examples.slate.net.Net;
import com.codenameone.examples.slate.net.Session;
import com.codenameone.examples.slate.ui.Lang;
import com.codenameone.examples.slate.ui.Layouts;
import com.codenameone.examples.slate.ui.Ui;

import java.util.ArrayList;
import java.util.List;

/// The calendars that make a provider busy: the phone's own, Google,
/// Microsoft, or any calendar published as an ICS address.
///
/// The phone's calendars are read on the phone and only busy intervals go
/// up (`DeviceSync`). The others are read by the server, which keeps the
/// credential; a Google or Microsoft account is connected by signing in to
/// it in a browser here and handing the server the one-time code. A server
/// with no client id for one of them simulates it, which is what the demo
/// does.
public final class CalendarsForm {
    private static final String GOOGLE = "GOOGLE";
    private static final String MICROSOFT = "MICROSOFT";

    private final Form form = Ui.form("Calendars", "Calendars");
    private final Container page = Ui.page();
    private final Container links = new Container(BoxLayout.y());
    private CalendarConfigDto config;

    private CalendarsForm(Form previous) {
        Ui.back(form, previous);
        links.setName("links");
        page.add(Ui.text(Lang.tr("Time that is busy in a connected calendar cannot be booked. "
                + "Only when you are busy is shared, never what the time is for."), "SvText"));
        page.add(links);
        Ui.section(page, "Connect");
        if (DeviceCalendar.canReadBusy()) {
            page.add(Ui.secondary("This phone's calendars", "connect-device", e -> device()));
        }
        page.add(Ui.secondary("Google Calendar", "connect-google", e -> oauth(GOOGLE)));
        page.add(Ui.secondary("Microsoft Outlook", "connect-microsoft", e -> oauth(MICROSOFT)));
        page.add(Ui.secondary("A calendar address (ICS)", "connect-ics", e -> ics()));
        Layouts.broaden(page);
        form.add(BorderLayout.CENTER, page);
        Api.provider().calendarConfig(Net.to(c -> config = c, Ui::fail));
        load();
    }

    public static void show(Form previous) {
        new CalendarsForm(previous).form.show();
    }

    private void load() {
        Api.provider().calendars(Net.to(this::show, Ui::fail));
    }

    private void show(List<CalendarLinkDto> connected) {
        links.removeAll();
        if (connected.isEmpty()) {
            links.add(Ui.empty(FontImage.MATERIAL_SYNC_DISABLED, "Nothing connected",
                    "Connect a calendar below and its busy times keep clients away."));
        }
        for (int iter = 0; iter < connected.size(); iter++) {
            final CalendarLinkDto l = connected.get(iter);
            String detail = l.lastError() != null && l.lastError().length() > 0 ? l.lastError()
                    : l.lastSyncedAt() > 0 ? Lang.tr("Read {0}", Ui.when(l.lastSyncedAt()))
                    : Lang.tr("Not read yet");
            Container actions = new Container(BoxLayout.x());
            actions.add(Ui.link("Read now", "sync-" + l.id(), e -> {
                if (l.serverSync()) {
                    Api.provider().sync(l.id(), Net.to(done -> load(), Ui::fail));
                } else {
                    DeviceSync.upload();
                    Ui.say("Busy times sent");
                }
            }));
            actions.add(Ui.link("Remove", "disconnect-" + l.id(), e -> Api.provider()
                    .disconnect(l.id(), Net.to(gone -> {
                        if (!l.serverSync()) {
                            DeviceSync.forget();
                        }
                        load();
                    }, Ui::fail))));
            links.add(Ui.row(FontImage.MATERIAL_EVENT_NOTE, l.label(), detail, actions));
        }
        Ui.relayout(page);
    }

    // tag::slate-device-calendars[]
    /// Asks for read access, lists the phone's calendars and lets the
    /// provider pick which count. A work calendar and a family one may be
    /// on the phone; a birthday calendar should not block a morning.
    private void device() {
        DeviceCalendar.authorize(CalendarAccess.EVENTS_READ_ONLY, ok -> {
            if (!ok.booleanValue()) {
                Ui.say("Calendar access is off. It can be turned on in Settings.");
                return;
            }
            DeviceCalendar.calendars(this::pick);
        });
    }

    private void pick(final List<CalendarInfo> calendars) {
        final Form choose = Ui.form("This phone", "DeviceCalendars");
        Ui.back(choose, form);
        Container list = Ui.page();
        final List<CheckBox> boxes = new ArrayList<CheckBox>();
        for (int iter = 0; iter < calendars.size(); iter++) {
            CheckBox box = CheckBox.createToggle(calendars.get(iter).getName());
            box.setUIID("SvCheck");
            box.setName("calendar-" + iter);
            box.setSelected(true);
            boxes.add(box);
            list.add(box);
        }
        list.add(Ui.primary("Use these calendars", "use", e -> {
            StringBuilder ids = new StringBuilder();
            for (int iter = 0; iter < boxes.size(); iter++) {
                if (boxes.get(iter).isSelected()) {
                    if (ids.length() > 0) {
                        ids.append(',');
                    }
                    ids.append(calendars.get(iter).getId());
                }
            }
            final String chosen = ids.toString();
            Api.provider().connect(new ConnectCalendarDto("DEVICE", Lang.tr("This phone"), "", "",
                    chosen, ""), Net.to(link -> {
                        Telemetry.calendarConnected("DEVICE");
                        DeviceSync.start(link.id(), chosen);
                        form.showBack();
                        load();
                    }, Ui::fail));
        }));
        Layouts.broaden(list);
        choose.add(BorderLayout.CENTER, list);
        choose.show();
    }
    // end::slate-device-calendars[]

    /// Signs in to Google or Microsoft in a browser and hands the server the
    /// code it redirects with. The redirect address is never loaded: the
    /// browser is stopped the moment it is asked to go there.
    private void oauth(final String kind) {
        String clientId = config == null ? "" : GOOGLE.equals(kind) ? config.googleClientId()
                : config.microsoftClientId();
        if (clientId == null || clientId.length() == 0) {
            // No client set up: a development server simulates the account.
            connect(kind, "demo", "");
            return;
        }
        final String redirect = Session.serverUrl() + "/oauth/calendar";
        String url = GOOGLE.equals(kind)
                ? "https://accounts.google.com/o/oauth2/v2/auth?access_type=offline&prompt=consent"
                        + "&scope=" + Util.encodeUrl("https://www.googleapis.com/auth/calendar.freebusy")
                : "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?scope="
                        + Util.encodeUrl("offline_access Calendars.ReadBasic");
        url = url + "&response_type=code&client_id=" + Util.encodeUrl(clientId)
                + "&redirect_uri=" + Util.encodeUrl(redirect);
        final Form sign = Ui.form(GOOGLE.equals(kind) ? "Google" : "Microsoft", "OAuth");
        Ui.back(sign, form);
        BrowserComponent browser = new BrowserComponent();
        browser.setBrowserNavigationCallback(next -> {
            if (!next.startsWith(redirect)) {
                return true;
            }
            final String code = parameter(next, "code");
            com.codename1.ui.CN.callSerially(() -> {
                form.showBack();
                if (code.length() == 0) {
                    Ui.say("The sign-in was cancelled");
                } else {
                    connect(kind, code, redirect);
                }
            });
            return false;
        });
        browser.setURL(url);
        sign.add(BorderLayout.CENTER, browser);
        sign.show();
    }

    private void ics() {
        final Form ask = Ui.form("Calendar address", "Ics");
        Ui.back(ask, form);
        Container p = Ui.page();
        p.add(Ui.text(Lang.tr("Most calendars can publish a private address ending in .ics. "
                + "The server reads it every few minutes."), "SvText"));
        final TextField url = Ui.field(p, "Address", "ics-url", "https://...");
        p.add(Ui.primary("Connect", "ics-connect", e -> {
            form.showBack();
            connect("ICS", url.getText().trim(), "");
        }));
        Layouts.broaden(p);
        ask.add(BorderLayout.CENTER, p);
        ask.show();
    }

    private void connect(final String kind, String credential, String redirect) {
        Api.provider().connect(new ConnectCalendarDto(kind, "", credential, redirect, "", ""),
                Net.to(link -> {
                    Telemetry.calendarConnected(kind);
                    load();
                }, Ui::fail));
    }

    static String parameter(String url, String name) {
        int query = url.indexOf('?');
        String rest = query < 0 ? "" : url.substring(query + 1);
        String[] pairs = Util.split(rest, "&");
        for (int iter = 0; iter < pairs.length; iter++) {
            if (pairs[iter].startsWith(name + "=")) {
                return Util.decode(pairs[iter].substring(name.length() + 1), "UTF-8", true);
            }
        }
        return "";
    }
}
