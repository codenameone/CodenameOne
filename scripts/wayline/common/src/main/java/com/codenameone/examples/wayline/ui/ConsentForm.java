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
package com.codenameone.examples.wayline.ui;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.wayline.Telemetry;

/// Asks, once on a device, whether the app may report how it is used.
///
/// It is shown after the first sign-in and before anything is counted, and it
/// has no way out but an answer: nothing is reported while there is none, so
/// leaving the question open would be a no that is asked again at every
/// start. Either answer can be changed in Settings, which is said here.
public final class ConsentForm {
    private ConsentForm() {
    }

    /// @param then where to go once there is an answer
    public static void show(final Runnable then) {
        Form form = Ui.form("Usage statistics", "Consent");
        Container page = Ui.page();
        Container banner = Ui.banner(FontImage.MATERIAL_INSIGHTS, "Help improve Wayline",
                "Wayline can count which screens are opened and which features are used, "
                + "so that we can see what works and fix what does not.", "WlBannerIcon");
        // The page has its own top padding, and the banner's on top of it
        // pushed the last line of the promise below under the answers.
        banner.getAllStyles().setPaddingTop(0);
        page.add(banner);
        page.add(Ui.row(FontImage.MATERIAL_VISIBILITY_OFF, "No personal details",
                "Never your name, e-mail, phone number, places or card details", null));
        // The two answers stay in view however much there is to read.
        Container answers = new Container(BoxLayout.y());
        answers.setUIID("WlPage");
        answers.setSafeArea(true);
        answers.add(Ui.primary("Share usage statistics", "consentYes", e -> answer(true, then)));
        answers.add(Ui.secondary("Not now", "consentNo", e -> answer(false, then)));
        answers.add(Ui.text("Turn it on or off at any time in Settings", "WlMuted"));
        form.add(BorderLayout.SOUTH, answers);
        form.add(BorderLayout.CENTER, page);
        form.show();
    }

    private static void answer(boolean yes, Runnable then) {
        Telemetry.allow(yes);
        then.run();
    }
}
