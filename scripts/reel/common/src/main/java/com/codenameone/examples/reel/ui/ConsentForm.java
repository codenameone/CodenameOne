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
package com.codenameone.examples.reel.ui;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.reel.Telemetry;

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
        Container banner = Ui.banner(FontImage.MATERIAL_INSIGHTS, "Help improve Reel",
                "Reel can count which screens are opened and which features are used, "
                + "so that we can see what works and fix what does not.", "SvBannerIcon");
        // The page has its own top padding, and the banner's on top of it
        // pushed the last line of the promise below under the answers.
        banner.getAllStyles().setPaddingTop(0);
        page.add(banner);
        page.add(Ui.row(FontImage.MATERIAL_VISIBILITY_OFF, "No personal details",
                "Never your name, e-mail, phone number, places or card details", null));
        // The two answers stay in view however much there is to read.
        Container answers = new Container(BoxLayout.y());
        answers.setUIID("SvPage");
        answers.setSafeArea(true);
        answers.add(Ui.primary("Share usage statistics", "consentYes", e -> answer(true, then)));
        answers.add(Ui.secondary("Not now", "consentNo", e -> answer(false, then)));
        answers.add(Ui.text("Turn it on or off at any time in Settings", "SvMuted"));
        form.add(BorderLayout.SOUTH, answers);
        form.add(BorderLayout.CENTER, page);
        form.show();
    }

    private static void answer(boolean yes, Runnable then) {
        Telemetry.allow(yes);
        then.run();
    }
}
