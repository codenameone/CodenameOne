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
package com.codenameone.examples.skirmish.ui;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.skirmish.api.PlayerDto;
import com.codenameone.examples.skirmish.battle.Art;
import com.codenameone.examples.skirmish.engage.Ads;
import com.codenameone.examples.skirmish.net.Session;
import java.util.List;

/// The menu between battles: the player's banner and wallet, the deck at a
/// glance, and the ways into a battle.
public final class HomeForm {
    private HomeForm() {
    }

    public static void show() {
        PlayerDto p = Session.user();
        Form f = Ui.form("", "home");
        f.setLayout(new BorderLayout());
        f.getToolbar().hideToolbar();
        Container body = new Container(BoxLayout.y());
        body.setScrollableY(true);
        body.add(Ui.wallet(p));
        body.add(Ui.centered(Ui.label(p.displayName(), "PlayerBanner")));
        if (p.suspended()) {
            body.add(Ui.text("Your account is suspended. Online battles are closed to you until it is lifted.",
                    "Warning"));
        }
        body.add(deck(p.deck()));
        body.add(Ui.wood("Battle", "battleButton", e -> MatchmakingForm.queue()));
        body.add(Ui.row(Ui.stone("Practice", "practiceButton", e -> BattleForm.practice()),
                Ui.stone("Challenge a friend", "challengeButton", e -> MatchmakingForm.host())));
        body.add(Ui.row(Ui.stone("Deck", "deckButton", e -> DeckForm.show()),
                Ui.stone("Shop", "shopButton", e -> ShopForm.show())));
        body.add(Ui.row(Ui.stone("Replays", "replaysButton", e -> HistoryForm.show()),
                Ui.stone("Leaders", "leadersButton", e -> LeaderboardForm.show())));
        body.add(Ui.stone("Settings", "settingsButton", e -> SettingsForm.show()));
        if (p.admin()) {
            body.add(Ui.button("Admin console", "AdminButton", "adminButton", e -> Nav.adminConsole()));
        }
        f.add(BorderLayout.CENTER, body);
        Component banner = Ads.banner(Ui.adFree());
        if (banner != null) {
            f.add(BorderLayout.SOUTH, banner);
        }
        f.show();
    }

    private static Container deck(List<String> cards) {
        Container c = new Container(new GridLayout(1, 8));
        c.setUIID("DeckStrip");
        c.setName("deckStrip");
        if (cards != null) {
            int size = com.codename1.ui.CN.convertToPixels(7);
            for (String id : cards) {
                c.add(new com.codename1.ui.Label(Art.cardIcon(id, size), "DeckStripCard"));
            }
        }
        return c;
    }
}
