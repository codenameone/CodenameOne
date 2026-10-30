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

import com.codename1.ui.Component;
import com.codenameone.examples.dash.forge.PackEditor;
import com.codenameone.examples.dash.forge.TrackTimeline;
import com.codenameone.examples.dash.rules.Content;
import com.codenameone.examples.dash.rules.forge.Samples;
import com.codenameone.examples.dash.run.RunView;

/// The track editor: the official pack, then a new pack with a level built
/// on the timeline -- a hurdle painted onto a lane by touch -- and a playtest
/// of the draft.
public class ForgeTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        signIn(PLAYER);
        waitForForm("home");
        click("forgeButton");
        waitForForm("forge");
        until(() -> has("pack-" + Samples.OFFICIAL_ID), 20000, "the official pack");
        shot("forge-home");
        click("pack-" + Samples.OFFICIAL_ID);
        waitForForm("pack");
        until(() -> has("level-" + Content.CAUSEWAY), 20000, "the official levels");
        shot("forge-pack");

        onEdt(() -> PackEditor.create("Frost Pass"));
        waitForForm("packEditor");
        shot("forge-editor");
        click("addLevel");
        waitForForm("levelEditor");
        click("tool-" + Content.HURDLE);
        final TrackTimeline timeline = (TrackTimeline) findByName("timeline");
        onEdt(() -> timeline.getComponentForm().scrollComponentToVisible(timeline));
        waitFor(300);
        // The second lane, a little way down the track.
        Component t = timeline;
        int rows = 2 + 3;
        touch(t.getAbsoluteX() + t.getWidth() / 8 - t.getScrollX(), t.getAbsoluteY() + t.getHeight() * 2 / rows
                + t.getHeight() / (2 * rows));
        waitFor(300);
        shot("forge-level-editor");
        click("playtest");
        waitForForm("run");
        final RunView view = track();
        until(() -> view.driver().run().tick() > 240, 20000, "the playtest");
        shot("forge-playtest");
        drive(view, 120000);
        until(() -> has("result"), 20000, "the playtest result");
        click("done");
        waitForForm("levelEditor");
        return true;
    }
}
