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
package com.codenameone.examples.reel;

import com.codenameone.examples.reel.ui.Nav;

/// An admin reads the dashboard, finds a viewer and opens their account.
public class AdminFlowTest extends E2e {
    @Override
    protected String mode() {
        return Nav.ADMIN;
    }

    @Override
    protected boolean run() throws Exception {
        signIn(ADMIN, "Admin");
        until(() -> findByName("dashboardPage") != null, 20000, "the dashboard");
        shot("admin-dashboard");
        click("menu-open");
        click("menu-users");
        type("userSearch", "viewer");
        click("user-0");
        waitForForm("AdminUser");
        until(() -> findByName("suspend") != null || findByName("reinstate") != null, 20000,
                "the account's actions");
        shot("admin-user");
        back();
        waitForForm("Admin");
        signOut();
        return true;
    }
}
