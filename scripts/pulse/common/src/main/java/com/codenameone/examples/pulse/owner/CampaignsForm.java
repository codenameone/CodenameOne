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
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.pulse.api.CampaignDto;
import com.codenameone.examples.pulse.api.SessionDto;
import com.codenameone.examples.pulse.member.Classes;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// Messages to members, and how each one did: how many it reached, how many
/// opened it and how many booked because of it. A campaign goes to one of
/// three audiences -- those who take a class's kind, every member, or those
/// who have not been in for a while.
public final class CampaignsForm {
    public static final String CLASS = "CLASS";
    public static final String ALL = "ALL";
    public static final String LAPSED = "LAPSED";

    private CampaignsForm() {
    }

    public static void show(final Form previous) {
        final Form form = Ui.form("Campaigns", "Campaigns");
        Ui.back(form, previous);
        final Container list = Ui.list("campaigns");
        final Runnable[] load = new Runnable[1];
        load[0] = () -> Api.owner().campaigns(Net.to(campaigns -> {
            list.removeAll();
            for (int iter = 0; campaigns != null && iter < campaigns.size(); iter++) {
                CampaignDto campaign = campaigns.get(iter);
                Container row = Ui.row(FontImage.MATERIAL_CAMPAIGN, campaign.name(),
                        audience(campaign.kind()) + Ui.DOT + Ui.day(campaign.createdAt()) + "\n"
                        + Lang.tr("{0} sent, {1} opened, {2} booked",
                                Integer.valueOf(campaign.sent()), Integer.valueOf(campaign.opened()),
                                Integer.valueOf(campaign.booked())), null);
                row.setName("campaign-" + iter);
                list.add(row);
            }
            if (campaigns == null || campaigns.isEmpty()) {
                list.add(Ui.text("No campaigns sent yet.", "SvMuted"));
            }
            Ui.refresh(form);
        }, Ui.failed(list, load[0])));
        Ui.pull(list, load[0]);
        form.add(BorderLayout.CENTER, list);
        form.add(BorderLayout.SOUTH, Ui.dock(Ui.primary("New campaign", "newCampaign",
                e -> compose(form, null))));
        form.show();
        load[0].run();
    }

    static String audience(String kind) {
        return CLASS.equals(kind) ? Lang.tr("Class regulars") : LAPSED.equals(kind)
                ? Lang.tr("Lapsed members") : Lang.tr("Every member");
    }

    /// The composer. With `session`, a campaign to fill that class; without
    /// one, a win-back to lapsed members to start from.
    public static void compose(final Form previous, final SessionDto session) {
        final Form form = Ui.form("New campaign", "Compose");
        Ui.back(form, previous);
        final Container page = Ui.page();
        final String[] kind = {session == null ? LAPSED : CLASS};
        final String[] sessionId = {session == null ? null : session.id()};
        final Container classes = new Container(com.codename1.ui.layouts.BoxLayout.y());
        page.add(Ui.label("Audience", "SvLabel"));
        page.add(Ui.choice("audience", new String[] {Lang.tr("Class"), Lang.tr("Everyone"),
            Lang.tr("Lapsed")}, new String[] {CLASS, ALL, LAPSED}, kind[0], value -> {
                kind[0] = value;
                classes.setHidden(!CLASS.equals(value));
                Ui.refresh(form);
            }));
        page.add(classes);
        classes.setHidden(!CLASS.equals(kind[0]));
        final TextField name = Ui.field(page, "Name, for you", "name", "Spin Tuesday push");
        final TextField title = Ui.field(page, "Title", "title", "A place is waiting");
        final TextField body = Ui.field(page, "Message", "body",
                "Tuesday's spin has room. Come ride with us.");
        if (session != null) {
            name.setText(session.name() + " " + Ui.day(session.startsAt()));
            title.setText(Lang.tr("Places left in {0}", session.name()));
            body.setText(Lang.tr("{0} on {1} has room for you. Tap to book.", session.name(),
                    Ui.when(session.startsAt())));
        } else {
            name.setText(Lang.tr("Win back"));
            title.setText(Lang.tr("We miss you"));
            body.setText(Lang.tr("Your next class is on us. Tap to see the timetable."));
        }
        Api.studio().classes("", System.currentTimeMillis(), 7, Net.to(found -> {
            List<String> labels = new ArrayList<String>();
            List<String> ids = new ArrayList<String>();
            for (int iter = 0; found != null && iter < found.size(); iter++) {
                SessionDto upcoming = found.get(iter);
                if (Classes.CANCELLED.equals(upcoming.state())) {
                    continue;
                }
                labels.add(upcoming.name() + " " + Ui.day(upcoming.startsAt()));
                ids.add(upcoming.id());
            }
            if (sessionId[0] == null && !ids.isEmpty()) {
                sessionId[0] = ids.get(0);
            }
            if (!ids.isEmpty()) {
                classes.add(Ui.choice("class", labels.toArray(new String[labels.size()]),
                        ids.toArray(new String[ids.size()]), sessionId[0],
                        value -> sessionId[0] = value, 1));
            }
            Ui.refresh(form);
        }, Ui::fail));
        page.add(Ui.primary("Send", "send", e -> {
            CampaignDto wanted = new CampaignDto(null, name.getText().trim(), kind[0],
                    CLASS.equals(kind[0]) ? sessionId[0] : null, title.getText().trim(),
                    body.getText().trim(), 0L, 0, 0, 0);
            Api.owner().send(wanted, Net.to(sent -> {
                Ui.say(Lang.tr("Sent to {0} members", Integer.valueOf(sent.sent())));
                previous.showBack();
            }, Ui::fail));
        }));
        form.add(BorderLayout.CENTER, page);
        form.show();
    }
}
