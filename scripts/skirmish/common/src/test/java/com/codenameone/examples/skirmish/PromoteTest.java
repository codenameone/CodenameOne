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

import com.codename1.ui.Tabs;

/// The console's Promote tab, in a desktop window: the numbers, a push
/// broadcast counted against its audience, and a home spotlight that every
/// player then sees.
public class PromoteTest extends E2e {
    @Override
    protected boolean desktop() {
        return true;
    }

    @Override
    protected boolean run() throws Exception {
        signIn(ADMIN);
        waitForForm("home");
        click("adminButton");
        waitForForm("admin");
        until(() -> has("stat-Open flags"), 20000, "the numbers");
        shot("desktop-admin-overview");
        onEdt(() -> ((Tabs) findByName("adminTabs")).setSelectedIndex(6));
        until(() -> has("promoteTitle") && has("openAnalytics"), 20000, "the Promote tab");
        type("promoteTitle", "Season two is here");
        type("promoteBody", "New arena, new cards. Come and battle!");
        type("promoteCampaign", "season-two");
        click("promoteCount");
        waitFor(1500);
        shot("desktop-admin-promote");
        type("spotlightText", "Season two: the Frost Arena is open");
        click("spotlightSave");
        waitFor(1500);
        onEdt(() -> com.codenameone.examples.skirmish.ui.Nav.home());
        waitForForm("home");
        until(() -> has("spotlight"), 20000, "the spotlight banner");
        shot("desktop-home-spotlight");
        return true;
    }
}
