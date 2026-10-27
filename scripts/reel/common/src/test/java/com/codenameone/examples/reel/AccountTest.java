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

/// A viewer's own corner: what they watched, their settings (playback,
/// subtitles, appearance), their account and devices, the plans, the
/// profiles of the household and a search.
public class AccountTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        signIn(VIEWER, "Home");

        click("menu-open");
        click("menu-library");
        waitForForm("Library");
        shot("viewer-library");
        back();
        waitForForm("Home");

        click("menu-open");
        click("menu-settings");
        waitForForm("Settings");
        shot("viewer-settings");
        reveal("captionSize-large");
        shot("viewer-settings-subtitles");
        click("profile");
        waitForForm("Profile");
        reveal("devices");
        shot("viewer-account");
        back();
        waitForForm("Settings");
        back();
        waitForForm("Home");

        click("menu-open");
        click("menu-plans");
        waitForForm("Plans");
        shot("viewer-plans");
        back();
        waitForForm("Home");

        click("search");
        waitForForm("Search");
        type("query", "the");
        waitFor(2500);
        shot("viewer-search");
        back();
        waitForForm("Home");

        if (has("menu-open")) {
            click("menu-open");
            if (findByName("menu-profiles") != null) {
                click("menu-profiles");
                waitForForm("Profiles");
                shot("viewer-profiles");
                click("profile-0");
                waitForForm("Home");
            } else {
                click("menu-dim");
            }
        }
        signOut();
        return true;
    }
}
