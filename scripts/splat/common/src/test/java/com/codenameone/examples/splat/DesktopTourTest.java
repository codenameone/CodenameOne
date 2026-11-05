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
import com.codenameone.examples.splat.rules.Match;

/// The game itself on a desktop window: the front end at full width, the
/// loadout and the armory, and a match with the detailed desktop HUD. The
/// phone pass shoots the same screens in the phone skin.
public class DesktopTourTest extends E2e {
    @Override
    protected boolean desktop() {
        return true;
    }

    @Override
    protected boolean run() throws Exception {
        signIn(PLAYER);
        waitForForm("home");
        waitFor(1500);
        shot("desktop-home");
        click("shopButton");
        waitForForm("shop");
        waitFor(800);
        shot("desktop-shop");
        onEdt(() -> com.codenameone.examples.splat.ui.Nav.home());
        waitForForm("home");
        click("runButton");
        waitForForm("play");
        click("mission-cul-de-sac:CAPTURE_THE_FLAG");
        shot("desktop-operations");
        click("practice-cul-de-sac:CAPTURE_THE_FLAG");
        waitForForm("match");
        final ArenaView view = arena();
        shot("desktop-briefing");
        until(() -> view.driver().countdownTicks() == 0, 20000, "the briefing to end");
        long give = System.currentTimeMillis() + 20000;
        // the HUD as you play it: a few seconds in, and not while painted out
        while ((view.driver().match().tick() < 3 * Match.TICK_HZ || !alive(view))
                && System.currentTimeMillis() < give) {
            driveFor(view, 200);
        }
        shot("desktop-match", view);
        finish(view, 10 * 60 * Match.TICK_HZ);
        awaitResult("the scoreboard");
        shot("desktop-scoreboard");
        click("done");
        waitForForm("play");
        return true;
    }

    private static boolean alive(ArenaView view) {
        Match m = view.driver().match();
        int me = view.driver().me();
        return me >= 0 && me < m.playerCount() && m.player(me).alive();
    }
}
