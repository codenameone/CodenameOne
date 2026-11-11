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
package com.codenameone.examples.splat;

import com.codename1.ui.CN;
import com.codenameone.examples.splat.api.RewardDto;
import com.codenameone.examples.splat.play.ArenaView;
import com.codenameone.examples.splat.ui.Nav;
import com.codenameone.examples.splat.ui.Rewards;
import com.codenameone.examples.splat.play.Arenas;
import com.codenameone.examples.splat.rules.Match;
import com.codenameone.examples.splat.ui.MatchForm;
import java.util.Arrays;

/// The home screen with progress on it, then the first practice match the way
/// a new player meets it: the briefing, the match, the scoreboard and the
/// reward.
public class TutorialTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        signIn(PLAYER);
        waitForForm("home");
        until(() -> has("level"), 20000, "the progress on the home screen");
        shot("home");
        onEdt(() -> MatchForm.practice(Arenas.DEFAULT, Nav::home));
        waitForForm("match");
        final ArenaView view = arena();
        shot("tutorial-briefing");
        until(() -> view.driver().countdownTicks() == 0, 20000, "the briefing to end");
        finish(view, 10 * 60 * Match.TICK_HZ);
        awaitResult("the result");
        shot("tutorial-result");
        click("done");
        // Not through onEdt: the popup is modal and would hold it forever.
        CN.callSerially(() -> Rewards.show(new RewardDto("Training complete", 100, 15, 150, 2, 2, true,
                Arrays.asList("Weekend Warrior")), Nav::home));
        until(() -> has("collectReward"), 10000, "the reward");
        shot("reward-levelup");
        click("collectReward");
        waitForForm("home");
        return true;
    }
}
