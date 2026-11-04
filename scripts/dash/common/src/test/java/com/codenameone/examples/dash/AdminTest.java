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

import com.codenameone.examples.dash.net.Session;

/// God mode: the overview, the flag queue with the demo's flagged player,
/// and that player's file.
public class AdminTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        signIn(ADMIN);
        waitForForm("home");
        assertTrue(Session.user().admin());
        click("adminButton");
        waitForForm("admin");
        until(() -> has("stat-Open flags"), 20000, "the numbers");
        shot("admin-overview");
        onEdt(() -> ((com.codename1.ui.Tabs) findByName("adminTabs")).setSelectedIndex(8));
        until(() -> has("promoteTitle"), 20000, "the Promote tab");
        shot("admin-promote");
        onEdt(() -> ((com.codename1.ui.Tabs) findByName("adminTabs")).setSelectedIndex(1));
        until(() -> findFlag() != null, 20000, "a flag in the queue");
        shot("admin-flags");
        touch(findFlag());
        waitForForm("adminPlayer");
        until(() -> has("act-SUSPEND"), 20000, "the actions");
        shot("admin-player");
        return true;
    }

    private String findFlag() {
        com.codename1.ui.Form f = com.codename1.ui.Display.getInstance().getCurrent();
        return find(f);
    }

    private static String find(com.codename1.ui.Container c) {
        for (int i = 0; i < c.getComponentCount(); i++) {
            com.codename1.ui.Component k = c.getComponentAt(i);
            if (k.getName() != null && k.getName().startsWith("flag-")) {
                return k.getName();
            }
            if (k instanceof com.codename1.ui.Container) {
                String inner = find((com.codename1.ui.Container) k);
                if (inner != null) {
                    return inner;
                }
            }
        }
        return null;
    }
}
