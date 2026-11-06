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
package com.codenameone.examples.slate.owner;

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.slate.api.PolicyDto;
import com.codenameone.examples.slate.api.PreviewDto;
import com.codenameone.examples.slate.api.PreviewRequestDto;
import com.codenameone.examples.slate.api.PreviewRowDto;
import com.codenameone.examples.slate.api.ServiceDto;
import com.codenameone.examples.slate.client.Reasons;
import com.codenameone.examples.slate.client.Times;
import com.codenameone.examples.slate.net.Api;
import com.codename1.appkit.Net;
import com.codenameone.examples.slate.ui.Lang;
import com.codenameone.examples.slate.ui.Layouts;
import com.codenameone.examples.slate.ui.Ui;

import java.util.List;

// tag::slate-policy-preview[]
/// The booking rules, with what they do shown before they are saved.
///
/// Rules interact: a ten-minute step with a thirty-minute minimum gap and
/// tight packing leaves far fewer starts than any of them alone suggests.
/// "Preview" runs the slot engine with the edited policy against tomorrow's
/// real schedule (`/api/owner/preview`), without saving anything, and shows
/// every provider's starts with the reason each one is refused.
public final class PolicyForm {
    private final Form form = Ui.form("Booking rules", "Policy");
    private final Container page = Ui.page();
    private final Container preview = new Container(BoxLayout.y());
    private TextField step;
    private TextField notice;
    private TextField horizon;
    private TextField hold;
    private TextField cancel;
    private TextField reschedule;
    private TextField deposit;
    private TextField noShow;
    private TextField clientGap;
    private TextField clientPerDay;
    private TextField usableGap;
    private TextField reminder;
    private String packing = "ANY";
    private PolicyDto loaded;
    private String serviceId = "";
    private String locationId = "";

    private PolicyForm(Form previous) {
        Ui.back(form, previous);
        form.add(BorderLayout.CENTER, page);
        preview.setName("preview");
        Api.business().policy(Net.to(this::fill, Ui::fail));
        Api.owner().services(Net.to(this::services, Ui::fail));
        Api.business().locations(Net.to(found -> {
            if (!found.isEmpty()) {
                locationId = found.get(0).id();
            }
        }, Ui::fail));
    }

    public static void show(Form previous) {
        new PolicyForm(previous).form.show();
    }

    private void services(List<ServiceDto> services) {
        if (!services.isEmpty()) {
            serviceId = services.get(0).id();
        }
    }

    private void fill(PolicyDto p) {
        loaded = p;
        packing = p.packing() == null ? "ANY" : p.packing();
        page.removeAll();
        Ui.section(page, "Starts");
        step = CatalogForm.number(page, "Start times every, minutes", "step", p.stepMinutes());
        notice = CatalogForm.number(page, "Book at least, minutes ahead", "notice",
                p.minNoticeMinutes());
        horizon = CatalogForm.number(page, "Book at most, days ahead", "horizon", p.horizonDays());
        page.add(Ui.label("Packing", "SvLabel"));
        page.add(Ui.choice("packing", new String[] {"Any free start", "Next to other bookings"},
                new String[] {"ANY", "TIGHT"}, packing, chosen -> packing = chosen));
        usableGap = CatalogForm.number(page, "Never leave a gap shorter than, minutes",
                "usable-gap", p.minUsableGapMinutes());
        Ui.section(page, "Clients");
        clientGap = CatalogForm.number(page, "Between one client's appointments, minutes",
                "client-gap", p.clientGapMinutes());
        clientPerDay = CatalogForm.number(page, "Appointments a client may have a day",
                "client-per-day", p.clientPerDay());
        hold = CatalogForm.number(page, "A chosen time is held for, minutes", "hold",
                p.holdMinutes());
        Ui.section(page, "Money and changes");
        deposit = CatalogForm.number(page, "Deposit, percent", "deposit", p.depositPercent());
        cancel = CatalogForm.number(page, "Free to cancel until, hours before", "cancel",
                p.cancelHours());
        reschedule = CatalogForm.number(page, "Can be moved until, hours before", "reschedule",
                p.rescheduleHours());
        noShow = CatalogForm.number(page, "No-show fee, percent", "no-show", p.noShowFeePercent());
        reminder = CatalogForm.number(page, "Remind clients, hours before", "reminder",
                p.reminderHours());
        page.add(Ui.secondary("Preview tomorrow", "run-preview", e -> preview()));
        page.add(preview);
        page.add(Ui.primary("Save rules", "save", e -> Api.owner().savePolicy(edited(),
                Net.to(saved -> {
                    Ui.say("Rules saved");
                    form.getBackCommand().actionPerformed(null);
                }, Ui::fail))));
        Layouts.broaden(page);
        Ui.relayout(page);
    }

    private PolicyDto edited() {
        return new PolicyDto(CatalogForm.num(step), CatalogForm.num(notice),
                CatalogForm.num(horizon), CatalogForm.num(hold), CatalogForm.num(cancel),
                CatalogForm.num(reschedule), CatalogForm.num(deposit), CatalogForm.num(noShow),
                CatalogForm.num(clientGap), CatalogForm.num(clientPerDay),
                CatalogForm.num(usableGap), packing, CatalogForm.num(reminder));
    }

    private void preview() {
        if (loaded == null || serviceId.length() == 0 || locationId.length() == 0) {
            return;
        }
        String day = Times.iso(Times.addDays(System.currentTimeMillis(), 1));
        Api.owner().preview(new PreviewRequestDto(serviceId, locationId, day, edited()),
                Net.to(this::showPreview, Ui::fail));
    }

    private void showPreview(PreviewDto p) {
        preview.removeAll();
        preview.add(Ui.label(Lang.tr("{0} bookable starts", String.valueOf(p.bookable())),
                "SvHeading"));
        for (int iter = 0; iter < p.rows().size(); iter++) {
            PreviewRowDto row = p.rows().get(iter);
            preview.add(Ui.label(row.name(), "SvCaption"));
            int columns = Math.max(1, Math.min(6, Layouts.columns(page.getWidth(), 14f, 8)));
            Container grid = new Container(new GridLayout(
                    (row.reasons().size() + columns - 1) / columns, columns));
            grid.setName("preview-" + row.provider());
            for (int i = 0; i < row.reasons().size(); i++) {
                String reason = row.reasons().get(i);
                String clock = Times.minuteOfDay(p.starts().get(i).intValue());
                boolean ok = "OK".equals(reason);
                grid.add(Ui.text(ok ? clock : clock + "\n" + Reasons.say(reason),
                        ok ? "SvSlot" : "SvSlotRefused"));
            }
            preview.add(grid);
        }
        Ui.relayout(page);
    }
}
// end::slate-policy-preview[]
