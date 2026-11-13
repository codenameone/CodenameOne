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

import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codenameone.examples.splat.play.OnlineDriver;
import com.codenameone.examples.splat.play.ArenaView;
import com.codenameone.examples.splat.rules.Match;
import com.codenameone.examples.splat.ui.Nav;

/// A party of two: the app hosts, a second player signs in from the test,
/// joins by the party code and talks; then they play on opposite teams and
/// trade emotes. The screenshots are the party chat and the match with the
/// guest's emote and chat on screen.
public class PartyTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        signIn(PLAYER);
        waitForForm("home");
        click("partyButton");
        waitForForm("party");
        until(() -> ((Label) findByName("partyCode")).getText().length() > 2, 20000, "the party code");
        String code = ((Label) findByName("partyCode")).getText().trim();
        Guest guest = Guest.signIn(RIVAL, PASSWORD);
        try {
            guest.join(code);
            until(() -> guest.heard("\"party\""), 20000, "the guest in the party");
            waitFor(800);
            guest.say("Hey! Ready when you are");
            waitFor(600);
            onEdt(() -> {
                TextField say = (TextField) findByName("chatField");
                say.setText("Cul-de-sac, loser mows the lawn?");
                say.fireDoneEvent();
            });
            waitFor(600);
            guest.say("You're on. Bring goggles");
            until(() -> guest.heard("mows the lawn"), 10000, "the host's message at the guest");
            waitFor(1200);
            shot("party-chat");
            click("goButton");
            waitForForm("match");
            final ArenaView view = arena();
            // A match only moves as the server's snapshots arrive, so a stalled
            // one would spin here until the suite's timeout; give it half a
            // minute and say where it stood.
            long give = System.currentTimeMillis() + 30000;
            while (view.driver().match().tick() < 6 * Match.TICK_HZ && System.currentTimeMillis() < give
                    && !driveFor(view, 200)) {
                // keep playing
            }
            int tick = view.driver().match().tick();
            assertTrue(tick >= 6 * Match.TICK_HZ || view.driver().match().over(),
                    "the match reached tick " + tick + " in 30 s; "
                    + ((OnlineDriver) view.driver()).describe());
            assertTrue(((OnlineDriver) view.driver()).reconnects() == 0, "the socket dropped mid-match: "
                    + ((OnlineDriver) view.driver()).describe());
            guest.emote("bounce");
            guest.say("That one bounced, I swear");
            click("comms");
            click("emote-ref");
            driveFor(view, 1500);
            shot("party-match", view);
            onEdt(() -> {
                ((OnlineDriver) view.driver()).leave();
                view.stop();
                view.driver().dispose();
                Nav.home();
            });
            waitForForm("home");
        } finally {
            guest.leave();
        }
        return true;
    }
}
