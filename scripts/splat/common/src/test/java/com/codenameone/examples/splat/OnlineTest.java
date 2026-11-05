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
import com.codenameone.examples.splat.play.OnlineDriver;
import com.codenameone.examples.splat.ui.Nav;

/// A quick match against the server. The test server fills a lone player's
/// match with bots at once, so this needs no second player.
public class OnlineTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        signIn(RIVAL);
        waitForForm("home");
        click("quickRaceButton");
        waitForForm("match");
        final ArenaView view = arena();
        assertTrue(view.driver() instanceof OnlineDriver, "a live match");
        assertTrue(view.driver().match().playerCount() > 1, "other players on the field");
        // The match only moves past the briefing when the server's snapshots
        // arrive: reaching tick 100 means they are.
        long give = System.currentTimeMillis() + 30000;
        while (view.driver().match().tick() < 100 && System.currentTimeMillis() < give) {
            driveFor(view, 200);
        }
        assertTrue(view.driver().match().tick() >= 100, "server snapshots");
        click("emote-gg");
        shot("match-online", view);
        onEdt(() -> {
            ((OnlineDriver) view.driver()).leave();
            view.stop();
            view.driver().dispose();
            Nav.home();
        });
        waitForForm("home");
        return true;
    }
}
