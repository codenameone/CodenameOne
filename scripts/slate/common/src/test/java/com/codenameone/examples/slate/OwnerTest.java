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
        until(() -> firstNamed("column-").length() > 0, 20000, "a provider's column");
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
}
