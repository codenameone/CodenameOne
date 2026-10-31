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

import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codenameone.examples.dash.run.RaceDriver;
import com.codenameone.examples.dash.run.RunView;
import com.codenameone.examples.dash.ui.Nav;

/// A party of two: the app hosts, a second player signs in from the test,
/// joins by the party code and talks; then they race each other and trade
/// emotes. The screenshots are the party chat and the race with the guest's
/// emote and chat on screen.
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
                say.setText("Neon Causeway, best of three?");
                say.fireDoneEvent();
            });
            waitFor(600);
            guest.say("You're on. Loser buys the next crate");
            until(() -> guest.heard("best of three"), 10000, "the host's message at the guest");
            waitFor(1200);
            shot("party-chat");
            click("goButton");
            waitForForm("run");
            final RunView view = track();
            // The bot steers while the race warms up, so the shot is of the
            // race and not of a runner who hit the first barrier.
            while (view.driver().run().tick() < 60 * 6 && !driveFor(view, 200)) {
                // keep running
            }
            guest.emote("wow");
            guest.say("Catch me if you can!");
            click("emote-wave");
            driveToBusy(view, 8, 15000);
            shot("party-race", view);
            onEdt(() -> {
                ((RaceDriver) view.driver()).leave();
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
