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
package com.codenameone.examples.bazaar.admin;

import com.codename1.ui.Container;
import com.codenameone.examples.bazaar.ui.Layouts;
import com.codenameone.examples.bazaar.ui.Nav;
import com.codenameone.examples.bazaar.ui.Sections;
import com.codenameone.examples.bazaar.ui.Shell;

/// The platform console's sections: the overview, the accounts and the
/// stores and the payments.
///
/// Everything here is behind `/api/admin`, which the server opens to the
/// `admin` role and to nobody else. That is where the protection is.
public final class AdminSections {
    private AdminSections() {
    }

    /// Puts the admin screens in the place of the placeholders.
    public static void install() {
        Sections.content(Nav.ADMIN, Sections.OVERVIEW, AdminSections::overview);
        Sections.content(Nav.ADMIN, Sections.USERS, () -> {
            EmbeddedPanes panes = new EmbeddedPanes();
            UsersForm.show(panes, UsersForm.ALL);
            return panes.host();
        });
        Sections.content(Nav.ADMIN, Sections.STORES, () -> {
            EmbeddedPanes panes = new EmbeddedPanes();
            StoresForm.show(panes);
            return panes.host();
        });
        Sections.content(Nav.ADMIN, Sections.PAYMENTS, () -> {
            EmbeddedPanes panes = new EmbeddedPanes();
            PaymentsForm.show(panes);
            return panes.host();
        });
    }

    private static Container overview() {
        final Dashboard dashboard = new Dashboard(Layouts.wide(), () -> {
            Shell shell = Shell.current();
            if (shell != null) {
                shell.show(Sections.USERS);
            }
        });
        dashboard.start();
        final Container page = dashboard.page();
        dashboard.shown();
        // It hears of new orders and payments over the live channel; pulling
        // the page down asks again.
        page.addPullToRefresh(dashboard::load);
        return page;
    }
}
