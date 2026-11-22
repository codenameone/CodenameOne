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

import com.codename1.ui.Tabs;

/// The admin opens the console, reads the day, finds the flagged run the demo
/// data carries and strikes it, and deals a sheet of their own.
public class AdminTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        click("signIn");
        waitForForm("signin");
        signIn(ADMIN);
        waitForForm("home");
        click("console");
        waitForForm("console");
        shot("console-today");
        final Tabs tabs = (Tabs) findByType(Tabs.class);
        onEdt(() -> tabs.setSelectedIndex(2));
        until(() -> findStrike() != null, 15000, "the flagged run");
        shot("console-flags");
        click(findStrike());
        until(() -> findStrike() == null, 15000, "the run to be struck");
        onEdt(() -> tabs.setSelectedIndex(3));
        type("dailySeed", "424242");
        click("setDaily");
        waitFor(1500);
        onEdt(() -> tabs.setSelectedIndex(4));
        waitFor(1500);
        shot("console-audit");
        return true;
    }

    private String findStrike() {
        com.codename1.ui.Component c = findByNamePrefix("strike-");
        return c == null ? null : c.getName();
    }
}
