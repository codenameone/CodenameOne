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
package com.codenameone.examples.bazaar.merchant;

import com.codename1.appkit.Net;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;
import com.codenameone.examples.bazaar.api.StoreTimeZoneDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;

/// The store's time zone, from the settings: the zone analytics, reports
/// and "today" count days and hours in. A curated list of IANA zones
/// ([StoreZones#ZONES]); saving goes through `StoreClockApi`.
final class TimeZonePage {
    private final MerchantPane host;
    private final Container page = MUi.page("time-zone-page");

    TimeZonePage(MerchantPane host) {
        this.host = host;
    }

    void show() {
        host.push("Time zone", page);
        Api.storeClock().timeZone(MerchantState.storeId(), Net.to(this::paint, Ui::fail));
    }

    private void paint(StoreTimeZoneDto now) {
        page.removeAll();
        final String current = now == null || now.timeZone() == null ? "UTC" : now.timeZone();
        page.add(Ui.text(Lang.tr("Analytics, reports and today's figures start and end the day "
                + "at the store's midnight in this zone."), "SvMuted"));
        Container card = new Container(BoxLayout.y());
        card.setUIID("MeCard");
        final String[] zones = StoreZones.choices(current);
        final Picker picker = new Picker();
        picker.setType(com.codename1.ui.Display.PICKER_TYPE_STRINGS);
        picker.setUIID("SvField");
        picker.setName("time-zone-picker");
        picker.setStrings(StoreZones.labels(zones));
        picker.setSelectedStringIndex(Math.max(0, StoreZones.indexOf(zones, current)));
        card.add(Ui.label("Time zone", "SvLabel")).add(picker);
        page.add(card);
        page.add(MUi.buttons(Ui.primary(Lang.tr("Save time zone"), "time-zone-save", e -> {
            String wanted = zones[Math.max(0, picker.getSelectedStringIndex())];
            Api.storeClock().saveTimeZone(MerchantState.storeId(), new StoreTimeZoneDto(wanted),
                    Net.to(saved -> {
                        Ui.say("Saved");
                        paint(saved);
                    }, Ui::fail));
        })));
        Ui.relayout(page);
    }
}
