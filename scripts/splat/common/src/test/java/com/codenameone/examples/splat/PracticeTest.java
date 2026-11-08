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
import com.codenameone.examples.splat.rules.Player;
import com.codenameone.examples.splat.rules.Rng;
import com.codenameone.examples.splat.rules.Trig;

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
        give = System.currentTimeMillis() + 40000;
        // Nor while facing a wall from arm's length: a shot of the field.
        // Two checks apart, so a stamp from a moment ago (PAINTED, CAUGHT) has
        // cleared and the player is up and looking ahead.
        while (System.currentTimeMillis() < give && !(inPlay(view) && !driveFor(view, 1500) && inPlay(view))) {
            driveFor(view, 150);
        }
        shotNow("match-practice");
        // a ball in the air can still land in the instant the capture takes
        while (!inPlay(view) && System.currentTimeMillis() < give) {
            driveFor(view, 1500);
            if (inPlay(view)) {
                shotNow("match-practice");
            }
        }

        // The bird's-eye view, a button away in any match.
        // Shot only while still in play, or a hit's replay takes the screen.
        onEdt(view::toggleOverhead);
        give = System.currentTimeMillis() + 40000;
        do {
            driveFor(view, 1500);
        } while (!inPlay(view) && System.currentTimeMillis() < give);
        shotNow("match-overhead");
        onEdt(view::toggleOverhead);

        // Getting painted, and the instant replay from the shooter's side. The bot drives until the moment you are hit
        // and no longer, or it would wipe the paint off and the ref would see.
        onEdt(() -> view.setAutopilot(new Rng(7)));
        until(() -> view.replayQueued() || view.driver().match().over(), 90000, "a hit");
        onEdt(() -> view.setAutopilot(null));
        if (view.replayQueued()) {
            until(() -> view.replayingShooter() >= 0, 8000, "the instant replay");
            waitFor(900);
            shotNow("instant-replay");
        }
        finish(view, 10 * 60 * Match.TICK_HZ);
        assertTrue(view.driver().match().over(), "the match ended");
        awaitResult("the scoreboard");
        shot("scoreboard");
        click("done");
        waitForForm("play");
        return true;
    }

    static boolean inPlay(ArenaView view) {
        return view.driver().match().player(view.driver().me()).alive() && !view.replayQueued()
                && lookingDownfield(view);
    }

    /// True when the player can see five metres straight ahead at eye height.
    private static boolean lookingDownfield(ArenaView view) {
        Match m = view.driver().match();
        Player p = m.player(view.driver().me());
        int eye = p.y() + 1500;
        int x = p.x() + (int) (5000L * Trig.sin(p.yaw()) / Trig.ONE);
        int z = p.z() + (int) (5000L * Trig.cos(p.yaw()) / Trig.ONE);
        return m.arena().clear(p.x(), eye, p.z(), x, eye, z);
    }
}
