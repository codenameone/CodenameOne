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

import com.codenameone.examples.splat.play.ArenaView;
import com.codenameone.examples.splat.play.PracticeDriver;
import com.codenameone.examples.splat.rules.Match;

/// Practice against bots from the operations list: the briefing, the match in
/// progress, and the scoreboard at the end.
public class PracticeTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        signIn(PLAYER);
        waitForForm("home");
        click("runButton");
        waitForForm("play");
        click("mission-cul-de-sac:CAPTURE_THE_FLAG");
        shot("operations");
        click("practice-cul-de-sac:CAPTURE_THE_FLAG");
        waitForForm("match");
        final ArenaView view = arena();
        assertTrue(view.driver() instanceof PracticeDriver, "a practice match");
        shot("briefing");
        until(() -> view.driver().countdownTicks() == 0, 20000, "the briefing to end");
        long give = System.currentTimeMillis() + 20000;
        while (view.driver().match().tick() < 6 * Match.TICK_HZ && System.currentTimeMillis() < give) {
            driveFor(view, 200);
        }
        // The shot is of the HUD in play, so not while painted: then the view
        // looks at the floor and the HUD counts down to the respawn.
        give = System.currentTimeMillis() + 20000;
        while (!view.driver().match().player(view.driver().me()).alive() && System.currentTimeMillis() < give) {
            driveFor(view, 200);
        }
        shot("match-practice", view);
        finish(view, 10 * 60 * Match.TICK_HZ);
        assertTrue(view.driver().match().over(), "the match ended");
        awaitResult("the scoreboard");
        shot("scoreboard");
        click("done");
        waitForForm("play");
        return true;
    }
}
