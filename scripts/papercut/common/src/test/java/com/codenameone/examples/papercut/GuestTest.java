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
package com.codenameone.examples.papercut;

import com.codenameone.examples.papercut.play.ArenaView;

/// A guest plays every mode with no account, and is told how to get on the
/// boards when the run ends.
public class GuestTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        shot("home");
        click("playClassic");
        waitForForm("play");
        ArenaView view = arena();
        assertTrue(driveFor(view, 4000) || !view.getArena().over, "the run plays");
        shot("classic", view);
        finish(view);
        awaitResult("the end of the guest run");
        shot("guest-result");
        click("menuFromResult");
        waitForForm("home");

        click("playFold");
        waitForForm("play");
        view = arena();
        driveFor(view, 3000);
        shot("fold", view);
        finish(view);
        awaitResult("the end of the fold run");
        click("menuFromResult");
        waitForForm("home");
        return true;
    }
}
