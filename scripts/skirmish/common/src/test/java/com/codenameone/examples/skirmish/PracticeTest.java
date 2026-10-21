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

import com.codename1.ui.Component;
import com.codenameone.examples.skirmish.battle.BattleView;
import com.codenameone.examples.skirmish.battle.PracticeDriver;
import com.codenameone.examples.skirmish.rules.Match;
import com.codenameone.examples.skirmish.rules.Replay;
import com.codenameone.examples.skirmish.rules.Unit;
import com.codenameone.examples.skirmish.ui.ReplayForm;
import com.codenameone.examples.skirmish.ui.ResultForm;
import java.util.List;

/// Practice against the bot, placed by touch: a press on a card in the hand,
/// then a press on the field. Then the replay of what was played, and the
/// result screen.
public class PracticeTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        signIn(PLAYER);
        waitForForm("home");
        click("practiceButton");
        waitForForm("battle");
        final BattleView view = arena();
        // Wait for elixir to be enough for any card in the hand.
        until(() -> view.driver().match().player(0).wholeElixir() >= 7, 30000, "elixir");
        touch("card0");
        until(() -> view.selected() != null, 5000, "the card to be chosen");
        Component a = view;
        touch(a.getAbsoluteX() + a.getWidth() / 3, a.getAbsoluteY() + a.getHeight() * 3 / 4);
        until(() -> mine(view.driver().match()) > 0, 10000, "a unit of ours on the field");
        waitFor(2500);
        shot("battle");

        // The replay is what was played so far, run again by the same rules.
        final String replay = ((PracticeDriver) view.driver()).replay();
        onEdt(() -> {
            view.stop();
            ReplayForm.show(Replay.decode(replay), "You", "Training Bot", 0, null);
        });
        waitForForm("replay");
        waitFor(3000);
        shot("replay");

        onEdt(() -> ResultForm.show(new ResultForm.Outcome("online", 1, 3, 1, 31, 25, null, replay,
                "You", "Training Bot", 0)));
        waitForForm("result");
        shot("result");
        return true;
    }

    private static int mine(Match m) {
        int n = 0;
        List<Unit> units = m.units();
        for (int i = 0; i < units.size(); i++) {
            Unit u = units.get(i);
            if (u.owner() == 0 && !u.card().isStructure()) {
                n++;
            }
        }
        return n;
    }
}
