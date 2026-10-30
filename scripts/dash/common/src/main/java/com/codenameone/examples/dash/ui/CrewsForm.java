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
package com.codenameone.examples.dash.ui;

import com.codename1.components.ShareButton;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.dash.api.CrewCreateDto;
import com.codenameone.examples.dash.api.CrewDto;
import com.codenameone.examples.dash.net.Api;
import com.codenameone.examples.dash.net.Net;
import com.codenameone.examples.dash.net.Session;
import com.codenameone.examples.dash.rules.Content;

/// Crews: a standing group of friends with its own leaderboard on every
/// level and a one-tap party. Joining takes the crew's code, which a member
/// shares however they like.
public final class CrewsForm {
    private CrewsForm() {
    }

    public static void show() {
        final Form f = Ui.form("Crews", "crews");
        Ui.back(f, Nav::home);
        final Container list = Ui.column();
        f.add(Ui.centeredColumn(list));
        final TextField name = new TextField("", "New crew name", 24, TextField.ANY);
        name.setName("crewName");
        final TextField code = new TextField("", "Crew code", 12, TextField.ANY);
        code.setName("crewCode");
        Container make = Ui.column(Ui.label("Start a crew", "SectionTitle"), name,
                Ui.wood("Create", "createCrew", e -> {
                    if (name.getText().trim().length() > 0) {
                        Api.game().createCrew(new CrewCreateDto(name.getText().trim()),
                                Net.to(c -> Ui.later(CrewsForm::show), Ui::failed));
                    }
                }),
                Ui.label("Join a crew", "SectionTitle"), code,
                Ui.stone("Join", "joinCrew", e -> {
                    if (code.getText().trim().length() > 0) {
                        Api.game().joinCrew(code.getText().trim().toUpperCase(),
                                Net.to(c -> Ui.later(CrewsForm::show), Ui::failed));
                    }
                }));
        f.add(Ui.centeredColumn(make));
        Api.game().crews(Net.to(crews -> Ui.later(() -> {
            if (crews.isEmpty()) {
                list.add(Ui.text("No crew yet. Start one, or ask a friend for theirs.", "Empty"));
            }
            for (CrewDto c : crews) {
                list.add(card(c));
            }
            f.revalidate();
        }), Ui::failed));
        f.show();
    }

    private static Container card(final CrewDto c) {
        Container card = new Container(new BorderLayout());
        card.setUIID("QuestCard");
        card.setName("crew-" + c.code());
        Container text = new Container(BoxLayout.y());
        text.add(new Label(c.name() + "   " + c.code(), "QuestTitle"));
        StringBuilder who = new StringBuilder();
        for (int i = 0; i < c.names().size(); i++) {
            if (i > 0) {
                who.append(", ");
            }
            who.append(c.names().get(i));
        }
        text.add(Ui.text(who.toString(), "QuestReward"));
        card.add(BorderLayout.CENTER, text);
        ShareButton share = new ShareButton();
        share.setText("Share");
        share.setUIID("DialogButton");
        share.setTextToShare("Join my crew in Dash: code " + c.code());
        card.add(BorderLayout.SOUTH, Ui.row(
                Ui.button("Party", "ClaimButton", "crewParty-" + c.code(), e -> RaceLobby.host(null, c.code())),
                Ui.button("Board", "DialogButton", "crewBoard-" + c.code(),
                        e -> ScoresForm.show(Content.CAUSEWAY, CrewsForm::show)),
                share,
                Ui.button("Leave", "DialogButton", "leaveCrew-" + c.code(),
                        e -> Api.game().leaveCrew(c.code(), Net.to(x -> Ui.later(CrewsForm::show), Ui::failed)))));
        if (Session.user() != null && Session.user().username().equals(c.owner())) {
            text.add(new Label("You started this crew", "XpText"));
        }
        return card;
    }
}
