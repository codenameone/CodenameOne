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

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.splat.net.Session;
import com.codenameone.examples.splat.play.Arenas;
import com.codenameone.examples.splat.rules.Catalog;
import com.codenameone.examples.splat.rules.Mode;

/// Operations: the playlist down the left, every yard in every mode, and the
/// selected operation's briefing on the right -- the overhead map, the orders
/// and three ways in: deploy into a quick match online, take your party, or
/// practise against bots with no network at all.
// tag::splat-operations[]
public final class PlayForm {
    private PlayForm() {
    }

    public static void show() {
        Form f = Ui.form("OPERATIONS", "play");
        f.setLayout(new BorderLayout());
        f.setUIID("OpsForm");
        Ui.back(f, Nav::home);
        final String[] refs = Arenas.all();
        final Container detail = new Container(new BorderLayout());
        detail.setUIID("OpsDetail");
        final Container list = new Container(BoxLayout.y());
        list.setUIID("OpsList");
        list.setScrollableY(true);
        final Container[] rows = new Container[refs.length];
        for (int i = 0; i < refs.length; i++) {
            final String ref = refs[i];
            final int index = i;
            Mode mode = Arenas.mode(ref);
            Button pick = Ui.button(Ui.caps(mode.operation), "OpsRowTitle", "mission-" + ref, null);
            rows[i] = BoxLayout.encloseY(pick, new Label(Ui.caps(Arenas.arenaName(ref) + "  //  " + mode.title),
                    "OpsRowSub"));
            rows[i].setUIID("OpsRow");
            rows[i].setLeadComponent(pick);
            pick.addActionListener(e -> {
                for (int k = 0; k < rows.length; k++) {
                    rows[k].setUIID(k == index ? "OpsRowOn" : "OpsRow");
                }
                brief(detail, ref);
                f.revalidate();
            });
            list.add(rows[i]);
        }
        rows[0].setUIID("OpsRowOn");
        brief(detail, refs[0]);
        boolean wide = CN.getDisplayWidth() > CN.getDisplayHeight();
        if (wide) {
            Container split = new Container(new GridLayout(1, 2));
            split.add(list).add(detail);
            f.add(BorderLayout.CENTER, split);
        } else {
            f.add(BorderLayout.NORTH, list);
            f.add(BorderLayout.CENTER, detail);
            list.setPreferredH(CN.getDisplayHeight() / 3);
        }
        f.show();
    }

    /// The briefing for `ref`: map, orders, and the three ways in.
    private static void brief(Container detail, final String ref) {
        detail.removeAll();
        Mode mode = Arenas.mode(ref);
        String arenaId = ref.substring(0, ref.indexOf(':'));
        Container head = BoxLayout.encloseY(new Label("// OPERATION BRIEFING", "OpsKicker"),
                new Label(Ui.caps(mode.operation.replace("Operation: ", "")), "OpsTitle"),
                new Label(Ui.caps(Arenas.arenaName(ref)) + "   " + Ui.caps(mode.title) + "   FIRST TO " + mode.target,
                        "OpsCaption"));
        detail.add(BorderLayout.NORTH, head);
        Container mid = new Container(BoxLayout.y());
        mid.setScrollableY(true);
        mid.add(new TacticalMap(Catalog.arena(arenaId), mode));
        mid.add(Ui.text(briefing(mode), "OpsBriefing"));
        detail.add(BorderLayout.CENTER, mid);
        Container buttons = new Container(BoxLayout.y());
        boolean online = Session.user() != null;
        if (online) {
            buttons.add(Ui.button("DEPLOY", "DeployButton", "quick-" + ref, e -> MatchLobby.quick(ref)));
        }
        Container two = new Container(new GridLayout(1, online ? 2 : 1));
        if (online) {
            two.add(Ui.button("PARTY", "OpsAction", "party-" + ref, e -> MatchLobby.host(ref)));
        }
        two.add(Ui.button("PRACTICE VS BOTS", "OpsAction", "practice-" + ref,
                e -> MatchForm.practice(ref, PlayForm::show)));
        buttons.add(two);
        detail.add(BorderLayout.SOUTH, buttons);
    }

    /// The mission brief, in the voice of a man who has watched too many films.
    static String briefing(Mode mode) {
        switch (mode) {
            case CAPTURE_THE_FLAG:
                return "Intelligence confirms the enemy has a bedsheet on a pole. Their mother wants it back "
                        + "by six. Bring it home. Leave no snack behind.";
            case SNACK_BAR:
                return "The snack bar is the only source of nachos for three counties. Hold it. "
                        + "Every second it is ours, history remembers. History is also the scoreboard.";
            default:
                return "Two teams enter. Both leave, because it is paintball and nobody actually dies. "
                        + "First team to paint the target wins. Goggles on at all times. This is not a drill.";
        }
    }
}
// end::splat-operations[]
