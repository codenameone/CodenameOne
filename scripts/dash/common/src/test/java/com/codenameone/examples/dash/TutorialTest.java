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

import com.codename1.ui.CN;
import com.codenameone.examples.dash.api.RewardDto;
import com.codenameone.examples.dash.run.RunView;
import com.codenameone.examples.dash.ui.Nav;
import com.codenameone.examples.dash.ui.Rewards;
import com.codenameone.examples.dash.ui.RunForm;
import java.util.Arrays;

/// The home screen with progress on it, then the first level the way a new
/// player meets it: the countdown, the coach's lines as the hazards arrive,
/// and the result.
public class TutorialTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        signIn(PLAYER);
        waitForForm("home");
        until(() -> has("chests") || has("spotlight"), 20000, "the progress on the home screen");
        shot("home");
        onEdt(RunForm::tutorial);
        waitForForm("run");
        final RunView view = track();
        until(() -> has("countdown") && view.driver().run().tick() == 0, 10000, "the countdown");
        shot("tutorial-countdown");
        until(() -> view.driver().run().tick() > 120, 20000, "the run to start");
        until(() -> has("coach"), 20000, "the coach");
        shot("tutorial-coach");
        drive(view, 120000);
        until(() -> has("result"), 20000, "the result");
        shot("tutorial-result");
        click("done");
        // Not through onEdt: the popup is modal and would hold it forever.
        CN.callSerially(() -> Rewards.show(new RewardDto("Training complete", 100, 15, 150, 2, 2, true,
                Arrays.asList("Sprinter")), Nav::home));
        until(() -> has("collectReward"), 10000, "the reward");
        shot("reward-levelup");
        click("collectReward");
        waitForForm("home");
        return true;
    }
}
