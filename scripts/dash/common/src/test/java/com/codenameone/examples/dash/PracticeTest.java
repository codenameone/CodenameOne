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

import com.codenameone.examples.dash.rules.Content;
import com.codenameone.examples.dash.rules.ViewMode;
import com.codenameone.examples.dash.run.RunView;
import com.codenameone.examples.dash.run.SoloDriver;
import com.codenameone.examples.dash.ui.ScoresForm;

/// A solo run of the causeway from the level list: it starts side on, the
/// camera swings behind the runner at 300 m, and the server's re-simulation
/// of the input log is what puts the score on the board.
public class PracticeTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        signIn(PLAYER);
        waitForForm("home");
        click("runButton");
        waitForForm("levels");
        shot("levels");
        click("play-" + Content.CAUSEWAY);
        waitForForm("run");
        final RunView view = track();
        assertTrue(view.driver() instanceof SoloDriver, "a solo run");
        assertEqual(ViewMode.SIDE_2D, view.driver().run().level().startView(), "the causeway starts side on");
        until(() -> view.driver().run().tick() > 240, 20000, "the run");
        shot("run-side");
        new Thread(() -> drive(view, 60000)).start();
        until(() -> view.driver().run().runner(view.driver().me()).z / 1000 > 340
                && busyAhead(view, 8) || view.driver().run().over(), 60000, "the swing to the chase view");
        shot("run-chase");
        finish(view, 60 * 600);
        awaitResult("the result");
        shot("run-result");
        click("scores");
        waitForForm("scores");
        // The board for the guide is the day's endless run: a track with no
        // finish line, where every run ends somewhere different. A finite
        // level's all-time board is a row of finishers at its length.
        onEdt(() -> ScoresForm.show(Content.ENDLESS, null));
        until(() -> has("board-day"), 20000, "the endless board");
        touch("board-day");
        until(() -> has("score-3"), 20000, "the day's board");
        shot("scores");
        return true;
    }
}
