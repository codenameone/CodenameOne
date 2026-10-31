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
package com.codenameone.examples.pulse.owner;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.pulse.api.InvoiceDto;
import com.codenameone.examples.pulse.api.MemberDetailDto;
import com.codenameone.examples.pulse.api.NameCountDto;
import com.codenameone.examples.pulse.api.OwnerMemberDto;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.ui.Chart;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Skeleton;
import com.codenameone.examples.pulse.ui.StatRow;
import com.codenameone.examples.pulse.ui.Ui;
import java.util.List;

/// One member as the owner sees them: where their membership stands, how
/// often they come, what they come for and every bill they have had.
public final class MemberDetailForm {
    private MemberDetailForm() {
    }

    public static void show(final Form previous, final String id) {
        final Form form = Ui.form("Member", "OwnerMember");
        Ui.back(form, previous);
        final Container page = Ui.page();
        page.setName("member");
        page.add(new Skeleton(6));
        final Runnable[] load = new Runnable[1];
        load[0] = () -> Api.owner().member(id, Net.to(found -> fill(form, page, found, load[0]),
                Ui.failed(page, load[0])));
        Ui.pull(page, load[0]);
        form.add(BorderLayout.CENTER, page);
        form.show();
        load[0].run();
    }

    private static void fill(Form form, Container page, MemberDetailDto detail, Runnable load) {
        page.removeAll();
        OwnerMemberDto member = detail.member();
        Container head = new Container(new BorderLayout(), "SvPanel");
        head.setName("memberHeader");
        head.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(Ui.avatar(member.name())));
        Container names = new Container(BoxLayout.y());
        names.add(Ui.plain(member.name(), "SvHeading"));
        if (member.planName() != null) {
            names.add(Ui.plain(member.planName(), "SvMuted"));
        }
        // On a line of its own: plan and date together did not fit beside the pill.
        names.add(Ui.plain(Lang.tr("Joined {0}", Ui.day(member.joinedAt())), "SvMuted"));
        head.add(BorderLayout.CENTER, names);
        head.add(BorderLayout.EAST, FlowLayout.encloseCenterMiddle(MembersForm.state(member)));
        page.add(head);

        StatRow tiles = Ui.stats(3);
        tiles.stat(String.valueOf(member.visits30()), "Visits in 30 days", "visits30")
                .stat(String.valueOf(detail.streakWeeks()), "Week streak", "streak")
                .stat(Ui.moneyShort(detail.lifetimeCents(), null), "Lifetime value", "lifetime");
        page.add(tiles);

        Ui.section(page, "Visits a week");
        List<Integer> weeks = detail.visitsByWeek();
        double[] values = new double[weeks == null ? 0 : weeks.size()];
        for (int iter = 0; iter < values.length; iter++) {
            Integer value = weeks.get(iter);
            values[iter] = value == null ? 0 : value.intValue();
        }
        Container chart = Chart.of("memberVisits", values, Lang.tr("11 weeks ago"),
                Lang.tr("This week"));
        page.add(chart);

        Ui.section(page, "Favourite classes");
        List<NameCountDto> favourites = detail.favourites();
        for (int iter = 0; favourites != null && iter < favourites.size(); iter++) {
            NameCountDto favourite = favourites.get(iter);
            Container row = new Container(new BorderLayout(), "SvRankRow");
            row.add(BorderLayout.CENTER, Ui.plain(favourite.name(), "SvRowTitle"));
            row.add(BorderLayout.EAST, Ui.plain(Lang.tr("{0} visits",
                    Integer.valueOf(favourite.count())), "SvRowDetail"));
            page.add(row);
        }
        if (favourites == null || favourites.isEmpty()) {
            page.add(Ui.text("No classes yet.", "SvMuted"));
        }

        Ui.section(page, "Invoices");
        List<InvoiceDto> invoices = detail.invoices();
        for (int iter = 0; invoices != null && iter < invoices.size(); iter++) {
            page.add(BillingForm.row(form, invoices.get(iter), "invoice-" + iter, load, false));
        }
        if (invoices == null || invoices.isEmpty()) {
            page.add(Ui.empty(FontImage.MATERIAL_RECEIPT_LONG, Lang.tr("No invoices"), null));
        }
        Ui.refresh(form);
    }
}
