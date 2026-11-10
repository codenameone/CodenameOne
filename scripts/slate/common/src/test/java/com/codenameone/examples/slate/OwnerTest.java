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
package com.codenameone.examples.slate;

import com.codenameone.examples.slate.ui.Timeline;

/// The owner opens the dashboard, the day board and the booking rules, and
/// previews what tomorrow looks like under them without saving anything.
public class OwnerTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        onEdt(() -> AppConfig.setMode(""));
        signIn(OWNER, "Owner");
        until(() -> findByName("chart-booked") != null, 20000, "the dashboard's chart");
        shot("owner-dashboard");
        click("open-board");
        waitForForm("Board");
        until(() -> findByName("board-grid") instanceof Timeline
                && ((Timeline) findByName("board-grid")).lanes().size() >= 2
                && booked((Timeline) findByName("board-grid")), 20000, "the day's bookings on the board");
        shot("owner-board");
        back();
        click("menu-open");
        click("menu-policy");
        waitForForm("Policy");
        until(() -> has("run-preview"), 20000, "the rules");
        type("step", "30");
        click("run-preview");
        until(() -> firstNamed("preview-").length() > 0, 20000, "the preview");
        shot("owner-preview");
        return true;
    }

    // The demo books every provider today, so a board with no booking on it is
    // still loading, or broken.
    static boolean booked(Timeline t) {
        for (int iter = 0; iter < t.lanes().size(); iter++) {
            if (t.lanes().get(iter).bookings() > 0) {
                return true;
            }
        }
        return false;
    }
}
