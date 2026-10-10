/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.wayline;

import com.codename1.ui.CN;
import com.codenameone.examples.wayline.ui.Layouts;

/// The admin console in a desktop window: the places down the side instead of
/// in a drawer, and a list beside what was picked from it.
///
/// It runs in the second pass of `run-e2e.sh`, where the simulator is a
/// desktop and not a phone. There the app is under the platform's own theme --
/// GNOME's, on the Linux machine the tests run on -- and the console wears it.
public class AdminDesktopTest extends E2e {
    @Override
    protected boolean desktop() {
        return true;
    }

    /// What the app is given to draw in, in pixels: the size of every capture
    /// this test takes.
    private static final int WIDE = 1280;
    private static final int HEIGHT = 754;

    /// What the window adds around the app, as it was last measured.
    private int frameWidth;
    private int frameHeight = 46;

    /// Makes the app's part of the window exactly `width` by `height`.
    ///
    /// The simulator is asked for the size of the whole window, and what is
    /// left for the app is that less the title bar, the border and the
    /// simulator's menu -- which differ with the window manager and the font
    /// the menu is drawn in, and are not known until the window is up. A
    /// capture of "a 1280 by 800 window" was 754 pixels high on one run and
    /// 750 on another, and a capture of another size cannot be compared with
    /// its golden at all. So the frame is measured and the request corrected
    /// until the app has the size asked for.
    private void window(final int width, final int height) {
        for (int attempt = 0; attempt < 8; attempt++) {
            final int askWidth = width + frameWidth;
            final int askHeight = height + frameHeight;
            onEdt(() -> CN.setWindowSize(askWidth, askHeight));
            int[] size = resized();
            if (size[0] == width && size[1] == height) {
                return;
            }
            frameWidth = askWidth - size[0];
            frameHeight = askHeight - size[1];
        }
        fail("The window could not be made " + width + " by " + height + "; the app has "
                + CN.getDisplayWidth() + " by " + CN.getDisplayHeight());
    }

    /// The size the app has once the window has stopped changing. A resize is
    /// carried out by the window system some time after it is asked for.
    private int[] resized() {
        int[] size = {CN.getDisplayWidth(), CN.getDisplayHeight()};
        int still = 0;
        for (int waited = 0; waited < 60 && still < 4; waited++) {
            waitFor(100);
            int[] now = {CN.getDisplayWidth(), CN.getDisplayHeight()};
            still = now[0] == size[0] && now[1] == size[1] ? still + 1 : 0;
            size = now;
        }
        return size;
    }

    @Override
    public void prepare() {
        if (desktopRun()) {
            // The window is whatever size it was left; the captures need one.
            window(WIDE, HEIGHT);
            until(() -> Layouts.wide(), 20000,
                    "the window to be laid out wide (it is " + CN.getDisplayWidth() + ")");
        }
        super.prepare();
    }

    @Override
    protected boolean run() throws Exception {
        assertTrue(CN.isDesktop(), "this pass runs the simulator as a desktop");
        assertTrue(Layouts.desktopLook(), "the platform's desktop theme is installed");
        onEdt(() -> AppConfig.setMode("admin"));
        signIn(ADMIN, "Admin");

        // The console, not the phone's home: the places are on the screen,
        // and there is no drawer to open.
        until(() -> has("nav-applications"), 20000, "the places down the side");
        assertFalse(has("menu-open"), "a wide console has a drawer");
        until(() -> findByName("chartRides") != null && has("alert-applications"), 20000,
                "the dashboard to be filled in");
        // The numbers are one row, and the charts side by side.
        // Waited for: the charts are added when their numbers arrive, and are
        // laid out on the turn of the event thread after that.
        until(() -> sameRow("ridesToday", "revenue"), 10000, "the four numbers to be in one row");
        until(() -> sameRow("chartRides", "chartRevenue"), 10000, "the charts to be side by side");
        shot("admin-desktop-dashboard");

        // An application opens beside the list it was picked from.
        click("nav-applications");
        until(() -> findByName("Applications") != null, 20000, "the applications");
        click("application-applicant@wayline.example");
        until(() -> findByName("ApplicationReview") != null, 20000, "the application");
        untilText("applicationStatus", "Waiting");
        assertTrue(has("application-applicant@wayline.example"),
                "the list is still there beside the application");
        assertTrue(leftOf("Applications", "ApplicationReview"),
                "the application is beside the list");
        shot("admin-desktop-applications");
        // A document is shown over the application, and left with back.
        click("document-licence");
        until(() -> findByName("Document") != null && findByName("picture") != null, 20000,
                "the licence to be shown");
        assertTrue(findByName("ApplicationReview") == null, "the document covers the application");
        click("pane-back");
        until(() -> findByName("ApplicationReview") != null, 20000,
                "back to lead to the application");

        // An account, the same way.
        click("nav-users");
        until(() -> findByName("Users") != null && has("user-" + RIDER), 20000, "the accounts");
        assertTrue(findByName("nothing") != null, "nothing is picked yet");
        click("user-" + RIDER);
        until(() -> findByName("User") != null, 20000, "the account");
        untilText("standing", "In good standing");
        assertTrue(has("user-" + DRIVER), "the list is still there beside the account");
        shot("admin-desktop-user");

        // Asking why is shown in the detail's place too, and refuses nothing.
        click("block");
        until(() -> findByName("Reason") != null, 20000, "the question why");
        click("pane-back");
        until(() -> findByName("User") != null, 20000, "back to lead to the account");
        untilText("standing", "In good standing");

        // The other places open, the dashboard again among them.
        click("nav-rides");
        until(() -> findByName("Rides") != null, 20000, "the rides");
        shot("admin-desktop-rides");
        click("nav-payments");
        until(() -> findByName("Payments") != null, 20000, "the payments");
        shot("admin-desktop-payments");
        click("nav-pricing");
        until(() -> findByName("Pricing") != null && text("baseFare").length() > 0, 20000,
                "the prices");
        shot("admin-desktop-pricing");
        click("nav-home");
        until(() -> findByName("chartRides") != null, 20000, "the dashboard again");

        // A window dragged narrow is laid out as the phone's console, with
        // its drawer, and dragged wide again is this one.
        window(520, HEIGHT);
        until(() -> has("menu-open") && !has("nav-applications"), 20000,
                "a narrow window to be laid out as a phone");
        window(WIDE, HEIGHT);
        until(() -> has("nav-applications") && !has("menu-open"), 20000,
                "a wide window to be laid out side by side again");
        until(() -> findByName("chartRides") != null, 20000, "the dashboard, wide again");

        // The map takes the whole window, and comes back to the console.
        click("nav-fleet");
        waitForForm("Fleet");
        shot("admin-desktop-live-map");
        back();
        waitForForm("Admin");

        // The screens that are not the console's are the phone's, kept to a
        // column in the middle of the window rather than stretched across it.
        click("menu-settings");
        waitForForm("Settings");
        shot("settings-desktop");
        back();
        waitForForm("Admin");
        click("switch-rider");
        waitForForm("Rider");
        shot("rider-desktop");
        signOut();
        return true;
    }

    /// Whether the two things called `first` and `second` are level.
    private boolean sameRow(String first, String second) {
        return findByName(first) != null && findByName(second) != null
                && Math.abs(findByName(first).getAbsoluteY() - findByName(second).getAbsoluteY())
                < CN.convertToPixels(3f)
                && findByName(first).getAbsoluteX() < findByName(second).getAbsoluteX();
    }

    private boolean leftOf(String first, String second) {
        return findByName(first).getAbsoluteX() + findByName(first).getWidth()
                <= findByName(second).getAbsoluteX();
    }
}
