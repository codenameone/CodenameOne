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

import com.codename1.appreview.AppReview;
import com.codename1.ui.Button;
import com.codename1.ui.Dialog;
import com.codename1.ui.CN;
import com.codename1.ui.Label;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.skirmish.api.AdRewardDto;
import com.codenameone.examples.skirmish.api.ReportDto;
import com.codenameone.examples.skirmish.engage.Ads;
import com.codenameone.examples.skirmish.engage.Friends;
import com.codenameone.examples.skirmish.engage.Telemetry;
import com.codenameone.examples.skirmish.net.Api;
import com.codenameone.examples.skirmish.net.Net;
import com.codenameone.examples.skirmish.net.Session;
import com.codenameone.examples.skirmish.rules.Replay;

/// The end of a battle: who won, what it was worth, and what next -- a
/// rewarded ad for bonus coins, reporting the opponent, the replay.
public final class ResultForm {
    /// Everything the screen shows, from either a practice or an online match.
    public static final class Outcome {
        final String mode;
        final int result;
        final int myCrowns;
        final int theirCrowns;
        final int trophies;
        final int coins;
        final String matchId;
        final String replay;
        final String name0;
        final String name1;
        final int me;

        public Outcome(String mode, int result, int myCrowns, int theirCrowns, int trophies, int coins,
                String matchId, String replay, String name0, String name1, int me) {
            this.mode = mode;
            this.result = result;
            this.myCrowns = myCrowns;
            this.theirCrowns = theirCrowns;
            this.trophies = trophies;
            this.coins = coins;
            this.matchId = matchId;
            this.replay = replay;
            this.name0 = name0;
            this.name1 = name1;
            this.me = me;
        }
    }

    private ResultForm() {
    }

    public static void show(final Outcome o) {
        Form f = Ui.form("", "result");
        Friends.battleFinished(o.result);
        f.setLayout(BoxLayout.yCenter());
        String title = o.result > 0 ? "VICTORY" : o.result < 0 ? "DEFEAT" : "DRAW";
        // The player's hero cheers a win; a loss gets the opponent's skeleton.
        String hero = o.result >= 0
                ? CardArt.heroOf(Session.user() == null ? null : Session.user().username()) : "skeleton_warrior";
        f.add(Ui.centered(new Label(CardArt.hero(hero, CN.convertToPixels(o.result > 0 ? 30 : 22)), "RewardHero")));
        f.add(Ui.centered(Ui.label(title, o.result > 0 ? "Victory" : o.result < 0 ? "Defeat" : "Draw")));
        f.add(Ui.centered(Ui.label(o.myCrowns + "  -  " + o.theirCrowns, "ResultCrowns")));
        if (o.trophies != 0 || o.coins != 0) {
            f.add(Ui.centered(Ui.label((o.trophies > 0 ? "+" : "") + o.trophies + " trophies   +"
                    + o.coins + " coins", "ResultReward")));
        }
        boolean signedIn = Session.user() != null;
        if (signedIn) {
            // tag::skirmish-rewarded[]
            final Button bonus = Ui.wood("Watch an ad: +20 coins", "rewardedAd", null);
            bonus.addActionListener(e -> {
                bonus.setEnabled(false);
                Ads.rewarded(() -> Api.shop().adReward(new AdRewardDto(String.valueOf(System.currentTimeMillis())),
                        Net.to(wallet -> {
                            Ui.toast("+20 coins (" + wallet.adRewardsLeft() + " more today)");
                            Session.refreshUser(() -> { }, (s, m) -> { });
                        }, Ui::failed)), () -> Ui.toast("No ad available right now"));
            });
            // end::skirmish-rewarded[]
            f.add(bonus);
        }
        if (o.replay != null || o.matchId != null) {
            f.add(Ui.stone("Watch the replay", "watchReplay", e -> {
                if (o.replay != null) {
                    ReplayForm.show(Replay.decode(o.replay), o.name0, o.name1, o.me, null);
                } else {
                    ReplayForm.load(o.matchId);
                }
            }));
        }
        if (signedIn && o.matchId != null && "online".equals(o.mode)) {
            f.add(Ui.stone("Report opponent", "reportPlayer", e -> report(o.matchId)));
        }
        f.add(Ui.wood(signedIn ? "Continue" : "Back", "continue", e -> {
            if (!signedIn) {
                SignInForm.show();
                return;
            }
            // Levels and achievements the battle earned are celebrated on
            // the way home.
            Session.refreshUser(() -> Ui.later(() -> Rewards.afterBattle(Nav::home)),
                    (s, m) -> Ui.later(Nav::home));
        }));
        f.show();
        // A win is when to ask for a rating, and an advert then would spoil
        // it: one or the other, never both.
        if (signedIn && o.result > 0 && AppReview.getInstance().shouldPrompt()) {
            AppReview.getInstance().requestReview();
        } else if (signedIn) {
            Ads.betweenBattles(Ui.adFree());
        }
    }

    // tag::skirmish-report[]
    private static void report(String matchId) {
        String[] reasons = {"Offensive name", "Abusive emotes", "Cheating", "Left on purpose"};
        Dialog d = new Dialog("Report", BoxLayout.y());
        d.setName("reportDialog");
        for (final String reason : reasons) {
            d.add(Ui.stone(reason, "reason-" + reason, e -> {
                d.dispose();
                Api.game().report(new ReportDto(matchId, reason), Net.to(flag -> {
                    Telemetry.screen("reported");
                    Ui.toast("Thanks. A moderator will look at it.");
                }, Ui::failed));
            }));
        }
        d.add(Ui.button("Cancel", "LinkButton", "cancelReport", e -> d.dispose()));
        d.show();
    }
    // end::skirmish-report[]
}
