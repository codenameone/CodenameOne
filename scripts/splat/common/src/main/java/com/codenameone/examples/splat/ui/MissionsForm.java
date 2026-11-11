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
package com.codenameone.examples.splat.ui;

import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.splat.net.MissionsClient;
import com.codenameone.examples.splat.net.Session;
import com.codenameone.examples.splat.rules.Mission;

/// The Mission Builder's front page: your draft, and the missions you and
/// your friends have published. A published mission plays like any yard --
/// practice against bots, or take a party into it.
public final class MissionsForm {
    private MissionsForm() {
    }

    public static void show() {
        final Form f = Ui.form("MISSION BUILDER", "missions");
        Ui.back(f, Nav::home);
        f.add(Ui.text("Design one half of a field. The other half is its mirror, so every mission is fair. "
                + "Play-test it against bots, then publish it for your friends and your party.", "OpsBriefing"));
        f.add(Ui.primary("CONTINUE DRAFT", "editDraft", e -> MissionEditor.show()));
        f.add(Ui.secondary("START A NEW FIELD", "newMission", e -> {
            Mission m = Mission.blank();
            MissionEditor.saveDraft(m);
            MissionEditor.open(m);
        }));
        f.add(new Label("// PUBLISHED BY YOU AND YOUR FRIENDS", "OpsKicker"));
        final Container list = new Container(BoxLayout.y());
        list.setName("missionList");
        f.add(list);
        if (Session.user() == null) {
            list.add(new Label("SIGN IN TO SEE PUBLISHED MISSIONS", "MissionRowSub"));
        } else {
            list.add(new Label("LOADING...", "MissionRowSub"));
            MissionsClient.list(rows -> {
                list.removeAll();
                if (rows.isEmpty()) {
                    list.add(new Label("NOTHING YET. PUBLISH THE FIRST ONE.", "MissionRowSub"));
                }
                for (MissionsClient.Row r : rows) {
                    list.add(row(r));
                }
                f.revalidate();
            });
        }
        if (CN.isDesktop()) {
            Ui.desktopColumn(f);
        }
        f.show();
    }

    private static Container row(final MissionsClient.Row r) {
        Container buttons = new Container(new GridLayout(1, Session.user() == null ? 1 : 2));
        buttons.add(Ui.button("PRACTICE", "OpsAction", "practiceMission-" + r.code, e -> play(r.code, false)));
        if (Session.user() != null) {
            buttons.add(Ui.button("PARTY", "OpsAction", "partyMission-" + r.code, e -> play(r.code, true)));
        }
        Container c = BoxLayout.encloseY(new Label(Ui.caps(r.name), "MissionRowTitle"),
                new Label(Ui.caps(r.owner + "  //  " + r.modeTitle + "  //  " + r.plays + " PLAYS"), "MissionRowSub"),
                buttons);
        c.setUIID("MissionRow");
        c.setName("missionRow-" + r.code);
        return c;
    }

    /// Fetches the mission, makes it a playable arena, and goes in.
    static void play(String code, final boolean party) {
        MissionsClient.get(code, m -> {
            if (m == null) {
                Ui.toast("That mission could not be loaded.");
                return;
            }
            m.register();
            String ref = m.arenaId() + ":" + m.mode.name();
            if (party) {
                MatchLobby.host(ref);
            } else {
                MatchForm.practice(ref, MissionsForm::show);
            }
        });
    }

    /// Publishes the editor's mission, after the same check the server makes.
    static void publish(Mission mission) {
        String p = mission.problem();
        if (p != null) {
            Ui.toast(p);
            return;
        }
        if (Session.user() == null) {
            Ui.toast("Sign in to publish.");
            return;
        }
        MissionsClient.publish(mission, code -> {
            if (code == null) {
                Ui.toast("Publishing failed. Try again.");
                return;
            }
            Ui.toast("Published as " + code + ". Your friends can play it now.");
            show();
        });
    }
}
