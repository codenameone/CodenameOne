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
package com.codenameone.examples.dash;

import com.codenameone.examples.dash.run.RaceDriver;
import com.codenameone.examples.dash.run.RunView;
import com.codenameone.examples.dash.ui.Nav;

/// A quick race, in lockstep with the server. The test server fills a lone
/// player's race with bots at once, so this needs no second player.
public class OnlineTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        signIn(RIVAL);
        waitForForm("home");
        click("quickRaceButton");
        waitForForm("run");
        final RunView view = track();
        assertTrue(view.driver() instanceof RaceDriver, "a live race");
        assertTrue(view.driver().run().runnerCount() > 1, "other runners on the track");
        // The clock only moves when the server says it may: reaching tick 100
        // means the ticks are arriving.
        until(() -> view.driver().run().tick() > 100, 30000, "server ticks");
        click("emote-gg");
        waitFor(1500);
        shot("race");
        onEdt(() -> {
            ((RaceDriver) view.driver()).leave();
            view.stop();
            view.driver().dispose();
            Nav.home();
        });
        waitForForm("home");
        return true;
    }
}
