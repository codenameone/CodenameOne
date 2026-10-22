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
package com.codenameone.examples.skirmish.forge;

import com.codename1.ui.Dialog;
import com.codenameone.examples.skirmish.api.MissionResultDto;
import com.codenameone.examples.skirmish.battle.ForgeFiles;
import com.codenameone.examples.skirmish.battle.MissionDriver;
import com.codenameone.examples.skirmish.net.Api;
import com.codenameone.examples.skirmish.net.Net;
import com.codenameone.examples.skirmish.net.Session;
import com.codenameone.examples.skirmish.rules.CardDef;
import com.codenameone.examples.skirmish.rules.CardSet;
import com.codenameone.examples.skirmish.rules.Deck;
import com.codenameone.examples.skirmish.rules.forge.ContentPack;
import com.codenameone.examples.skirmish.rules.forge.MissionDef;
import com.codenameone.examples.skirmish.ui.BattleForm;
import com.codenameone.examples.skirmish.ui.CardArt;
import com.codenameone.examples.skirmish.ui.Rewards;
import com.codenameone.examples.skirmish.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// Plays a mission: fetches the files its cards use, runs the battle, and
/// then either sends the replay for the server to score or -- when its author
/// is trying it from the editor -- just says how it went.
// tag::skirmish-forge-play[]
public final class MissionPlay {
    private MissionPlay() {
    }

    public static void start(final ContentPack pack, final String missionId, final boolean test,
            final Runnable back) {
        List<String> files = new ArrayList<String>();
        for (CardDef c : pack.cards) {
            if (c.model() != null && c.model().startsWith("asset:")) {
                files.add(c.model().substring(6));
            }
        }
        ForgeFiles.fetch(files, () -> Ui.later(() -> battle(pack, missionId, test, back)), Ui::failed);
    }

    private static void battle(final ContentPack pack, String missionId, final boolean test, final Runnable back) {
        MissionDef m = pack.mission(missionId);
        CardSet cards = pack.cardSet();
        CardArt.use(cards);
        MissionDriver driver = new MissionDriver(pack, missionId, System.currentTimeMillis(),
                m.playerDeck == null ? ownDeck(cards) : null);
        BattleForm.mission(driver, new BattleForm.MissionEnd() {
            @Override
            public void finished(MissionDriver d) {
                CardArt.use(null);
                int stars = d.run().stars();
                if (test) {
                    Dialog.show(stars > 0 ? "Won" : "Lost", stars > 0 ? PackScreen.starText(stars)
                            + "  -- a test battle counts for nothing." : "The enemy won this one.", "OK", null);
                    back.run();
                    return;
                }
                Api.forge().result(new MissionResultDto(d.run().replay().encode()), Net.to(
                        outcome -> Ui.later(() -> Rewards.show(outcome.reward(), back)), (status, message) -> {
                            Ui.failed(status, message);
                            back.run();
                        }));
            }

            @Override
            public void left(MissionDriver d) {
                CardArt.use(null);
                back.run();
            }
        });
    }

    /// The player's saved deck when the pack's cards allow it, else the
    /// starter deck: a mission that leaves the deck to the player may still
    /// run on a pack that changed which cards there are.
    private static String[] ownDeck(CardSet cards) {
        if (Session.user() != null && Session.user().deck() != null) {
            List<String> d = Session.user().deck();
            String[] deck = d.toArray(new String[d.size()]);
            if (cards.isValidDeck(deck)) {
                return deck;
            }
        }
        return Deck.starter();
    }
}
// end::skirmish-forge-play[]
