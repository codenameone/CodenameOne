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
package com.codenameone.examples.skirmish;

import com.codename1.ui.CN;
import com.codenameone.examples.skirmish.api.RewardDto;
import com.codenameone.examples.skirmish.battle.BattleView;
import com.codenameone.examples.skirmish.battle.TutorialDriver;
import com.codenameone.examples.skirmish.rules.Arena;
import com.codenameone.examples.skirmish.rules.Cards;
import com.codenameone.examples.skirmish.rules.Deck;
import com.codenameone.examples.skirmish.ui.Nav;
import com.codenameone.examples.skirmish.ui.Rewards;
import java.util.Arrays;

/// The home screen with progress on it, then the training battle followed the
/// way a new player follows it: Next, tap the card the finger points at, drag
/// it to the circle.
public class TutorialTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        signIn(PLAYER);
        waitForForm("home");
        until(() -> has("quest-" + "win-1") || has("chests"), 20000, "the progress on the home screen");
        shot("home");
        click("tutorialButton");
        waitForForm("battle");
        final BattleView view = arena();
        until(() -> has("coachNext"), 20000, "the mentor");
        shot("tutorial-welcome");
        click("coachNext");
        waitFor(300);
        shot("tutorial-towers");
        click("coachNext");
        waitFor(300);
        click("coachNext");
        // Pick the squire: the finger is on it.
        waitFor(600);
        shot("tutorial-pick");
        touch("card" + slot(view, TutorialDriver.TROOP));
        until(() -> TutorialDriver.TROOP.equals(view.selected()), 5000, "the squire to be picked");
        waitFor(900);
        shot("tutorial-drag");
        place(view, Arena.LEFT_LANE_X, 21 * Arena.TILE);
        until(() -> !view.driver().match().units().isEmpty() && has("coachText"), 5000, "the squire placed");
        // The defence step waits for elixir and for the enemy.
        until(() -> slotHas(view, TutorialDriver.RANGED) && isPicking(view, TutorialDriver.RANGED), 30000,
                "the defence step");
        shot("tutorial-defend");
        touch("card" + slot(view, TutorialDriver.RANGED));
        until(() -> TutorialDriver.RANGED.equals(view.selected()), 5000, "the archers to be picked");
        place(view, Arena.LEFT_LANE_X, 24 * Arena.TILE);
        until(() -> isPicking(view, TutorialDriver.SPELL), 40000, "the spell step");
        touch("card" + slot(view, TutorialDriver.SPELL));
        until(() -> TutorialDriver.SPELL.equals(view.selected()), 5000, "the orb to be picked");
        waitFor(900);
        shot("tutorial-spell");
        place(view, Arena.LEFT_LANE_X, Arena.mirrorY(Arena.PRINCESS_Y_BOTTOM));
        until(() -> has("coachNext"), 10000, "the last word");
        click("coachNext");
        waitFor(2500);
        shot("tutorial-free");
        onEdt(view::stop);
        // Not through onEdt: the popup is modal and would hold it forever.
        CN.callSerially(() -> {
            Rewards.show(new RewardDto("Training complete", 100, 15, 150, 2, 2, true,
                    Arrays.asList("Recruit")), Nav::home);
        });
        until(() -> has("collectReward"), 10000, "the reward");
        shot("reward-levelup");
        click("collectReward");
        waitForForm("home");
        return true;
    }

    /// Picking is allowed only for the card the lesson is on, so a card that
    /// can be picked is the step being shown.
    private boolean isPicking(BattleView view, String card) {
        return has("coachText") && slotHas(view, card)
                && view.driver().match().player(0).wholeElixir() >= Cards.get(card).cost
                && view.driver().match().tick() > 0 && ((TutorialDriver) view.driver()).isPaused();
    }

    private static boolean slotHas(BattleView view, String card) {
        return slot(view, card) >= 0;
    }

    private static int slot(BattleView view, String card) {
        for (int i = 0; i < Deck.HAND; i++) {
            if (card.equals(view.driver().match().player(0).hand(i))) {
                return i;
            }
        }
        return -1;
    }

    private void place(final BattleView view, int ax, int ay) {
        until(() -> view.screenPoint(ax, ay) != null, 5000, "the field on screen");
        int[] p = view.screenPoint(ax, ay);
        touch(p[0], p[1]);
    }
}
