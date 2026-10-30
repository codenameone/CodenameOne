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
package com.codenameone.examples.pulse.trainer;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.pulse.api.RosterEntryDto;
import com.codenameone.examples.pulse.api.SessionDto;
import com.codenameone.examples.pulse.live.LiveChannel;
import com.codenameone.examples.pulse.member.Classes;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Ui;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The class as it happens: a tile per person in the room, coloured by the
/// heart rate zone their strap or watch last reported. Readings arrive over
/// [LiveChannel] as members stream them; nothing here polls.
public final class LiveClassForm extends LiveChannel.Adapter {
    private final SessionDto session;
    private final Form form;
    private final Container grid = new Container(new GridLayout(2));
    private final Map<String, Label> rates = new HashMap<String, Label>();
    private final Map<String, Container> tiles = new HashMap<String, Container>();

    private LiveClassForm(Form previous, SessionDto session) {
        this.session = session;
        form = Ui.form(session.name(), "LiveClass");
        Ui.back(form, previous);
        grid.setName("zones");
        Container page = Ui.page();
        page.add(Ui.text(Lang.tr("Heart rate zones, live. Zone 5 is flat out."), "SvMuted"));
        page.add(grid);
        form.add(BorderLayout.CENTER, page);
        form.addShowListener(e -> {
            LiveChannel.listen(this);
            load();
        });
    }

    public static void show(Form previous, SessionDto session) {
        new LiveClassForm(previous, session).form.show();
    }

    private void load() {
        Api.trainer().roster(session.id(), Net.to(this::fill, Ui::fail));
    }

    private void fill(List<RosterEntryDto> roster) {
        grid.removeAll();
        rates.clear();
        tiles.clear();
        for (int iter = 0; roster != null && iter < roster.size(); iter++) {
            RosterEntryDto entry = roster.get(iter);
            if (Classes.WAITLISTED.equals(entry.state()) || Classes.CANCELLED.equals(entry.state())) {
                continue;
            }
            Container tile = new Container(BoxLayout.y());
            tile.setName("zone-" + entry.memberId());
            tile.add(Ui.label(entry.name(), "SvZoneName"));
            Label rate = Ui.label("", "SvZoneRate");
            tile.add(rate);
            rates.put(entry.memberId(), rate);
            tiles.put(entry.memberId(), tile);
            show(entry.memberId(), entry.heartRate(), entry.zone());
            grid.add(tile);
        }
        if (rates.isEmpty()) {
            grid.add(Ui.empty(FontImage.MATERIAL_FAVORITE, Lang.tr("Nobody in yet"),
                    Lang.tr("Members appear here once they are booked.")));
        }
        Ui.refresh(form);
    }

    private void show(String memberId, int bpm, int zone) {
        Label rate = rates.get(memberId);
        Container tile = tiles.get(memberId);
        if (rate == null) {
            return;
        }
        rate.setText(bpm > 0 ? bpm + " bpm" : "--");
        tile.setUIID(bpm > 0 ? "SvZone" + Math.max(1, Math.min(5, zone)) : "SvZone0");
    }

    @Override
    public void heartRate(String sessionId, String memberId, int bpm, int zone) {
        if (!session.id().equals(sessionId)) {
            return;
        }
        if (!rates.containsKey(memberId)) {
            // Someone checked in after the view was opened.
            load();
            return;
        }
        show(memberId, bpm, zone);
        grid.repaint();
    }

    @Override
    public void bookingChanged(String id, String state) {
        load();
    }
}
