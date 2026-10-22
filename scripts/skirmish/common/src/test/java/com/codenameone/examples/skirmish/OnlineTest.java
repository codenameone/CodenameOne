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

import com.codenameone.examples.skirmish.battle.BattleView;
import com.codenameone.examples.skirmish.battle.OnlineDriver;
import com.codenameone.examples.skirmish.rules.Match;
import com.codenameone.examples.skirmish.ui.Nav;

/// The ranked queue, in lockstep with the server. The test server matches a
/// lone player with the bot at once, so this needs no second player.
public class OnlineTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        signIn(RIVAL);
        waitForForm("home");
        click("battleButton");
        waitForForm("battle");
        final BattleView view = arena();
        assertTrue(view.driver() instanceof OnlineDriver, "an online battle");
        // The clock only moves when the server says it may: reaching tick 100
        // means the ticks are arriving.
        until(() -> view.driver().match().tick() > 100, 30000, "server ticks");
        final Match m = view.driver().match();
        until(() -> m.player(view.driver().me()).wholeElixir() >= 7, 30000, "elixir");
        touch("card0");
        touch(view.getAbsoluteX() + view.getWidth() * 2 / 3, view.getAbsoluteY() + view.getHeight() * 3 / 4);
        until(() -> m.player(view.driver().me()).wholeElixir() < 7, 10000, "the card to be accepted");
        click("emote-GG");
        waitFor(1500);
        shot("online-battle");
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
