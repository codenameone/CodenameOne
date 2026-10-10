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

import com.codename1.analytics.AbstractAnalyticsProvider;
import com.codename1.analytics.AnalyticsCrashReport;
import com.codename1.analytics.AnalyticsEvent;
import com.codename1.components.Switch;

import java.util.ArrayList;
import java.util.List;

/// What the app reports about how it is used: nothing until the user agrees,
/// then each screen and what was done on it, and never who did it.
///
/// The reports go to a provider that only writes them down, so none of this
/// leaves the test.
public class AnalyticsTest extends E2e {
    private final Recorder recorder = new Recorder();

    @Override
    protected boolean run() throws Exception {
        // As on a device the app was just installed on: nobody has answered.
        onEdt(() -> {
            Telemetry.use(recorder);
            Telemetry.unask();
        });

        // The first sign-in is asked, before the first screen.
        signIn(RIDER, "Consent");
        shot("analytics-consent");
        assertFalse(Telemetry.asked(), "the question is answered before anyone answered it");
        click("consentNo");
        waitForForm("Rider");
        assertTrue(Telemetry.asked() && !Telemetry.allowed(), "the answer was no");

        // A no is a no: screens are shown, a fare is worked out, and nothing
        // is reported.
        quote();
        click("back");
        click("menu-open");
        click("menu-settings");
        waitForForm("Settings");
        assertEqual(0, recorder.seen.size(), "reported without consent: " + recorder.seen);

        // The switch in Settings is the same answer, and can be changed.
        reveal("usageStatistics");
        assertFalse(((Switch) findByName("usageStatistics")).isValue(), "the switch shows yes");
        // Further than it needs to be seen, so that it is whole in the capture.
        reveal("privacy");
        shot("settings-analytics");
        onEdt(() -> ((Switch) findByName("usageStatistics")).setValue(true));
        assertTrue(Telemetry.allowed(), "the switch did not grant consent");
        // From the yes on: who, in the two words kept about them, and the
        // screen they are on.
        until(() -> recorder.has("screen:Settings"), 5000, "the Settings screen view");
        assertTrue(recorder.has("property:role=rider"), "the role: " + recorder.seen);
        assertTrue(recorder.has("property:language=en"), "the language: " + recorder.seen);

        // Each screen, once, under the name it was built with, and where the
        // user came to it from.
        back();
        waitForForm("Rider");
        until(() -> recorder.has("screen:Rider<Settings"), 5000, "the Rider screen view");
        quote();
        assertTrue(recorder.has("screen:PlaceSearch<Rider"), "the search: " + recorder.seen);
        until(() -> recorder.event("ride_quoted") != null, 5000, "the quote to be reported");
        assertTrue(recorder.event("ride_quoted").startsWith("ride_quoted[ride] {products="),
                "the quote: " + recorder.event("ride_quoted"));

        // Asking for the ride, with what an operator would chart about it.
        click("request");
        until(() -> recorder.event("ride_requested") != null, 20000,
                "the request to be reported");
        assertEqual("ride_requested[ride] {product=standard, payment=cash}",
                recorder.event("ride_requested"));
        // Nobody is driving, so the server may give the ride up before the
        // rider does; either way the screen ends where it began.
        until(() -> {
            clickIfThere("dismiss");
            clickIfThere("cancel");
            return findByName("whereTo") != null;
        }, 30000, "the ride to be cancelled");

        // Signing out, and in again: both are reported now.
        signOut();
        until(() -> recorder.has("event:logout[account] {reason=user}"), 5000,
                "the sign-out: " + recorder.seen);
        assertTrue(recorder.has("screen:Welcome<Rider"), "the welcome: " + recorder.seen);
        // The answer was given on this device: it is not asked for again.
        signIn(RIDER, "Rider");
        assertTrue(recorder.has("event:login[account] {method=password}"),
                "the sign-in: " + recorder.seen);

        // Nothing in any of it says who this is or where they went.
        final List<String> reported = new ArrayList<String>();
        onEdt(() -> reported.addAll(recorder.seen));
        for (String line : reported) {
            assertFalse(line.indexOf('@') >= 0 || line.indexOf("Riley") >= 0
                    || line.indexOf("Ferry") >= 0 || line.indexOf("37.") >= 0
                    || line.indexOf("122.") >= 0 || line.indexOf("userId") >= 0,
                    "personal data in a report: " + line);
        }

        // Taking the yes back stops it at once.
        click("menu-open");
        click("menu-settings");
        waitForForm("Settings");
        until(() -> recorder.has("screen:Settings<Rider"), 5000, "the Settings screen view");
        reveal("usageStatistics");
        assertTrue(((Switch) findByName("usageStatistics")).isValue(), "the switch shows no");
        onEdt(() -> ((Switch) findByName("usageStatistics")).setValue(false));
        assertFalse(Telemetry.allowed(), "the switch did not withdraw consent");
        final int before = recorder.seen.size();
        click("signOut");
        waitForForm("Welcome");
        waitFor(500);
        assertEqual(before, recorder.seen.size(), "reported after consent was withdrawn: "
                + recorder.seen.subList(before, recorder.seen.size()));
        return true;
    }

    @Override
    public void cleanup() {
        if (!desktopRun()) {
            // The tests after this one are not asked, and report nothing.
            onEdt(() -> Telemetry.allow(false));
        }
        super.cleanup();
    }

    /// Writes down what it is told, a line each.
    static final class Recorder extends AbstractAnalyticsProvider {
        final List<String> seen = new ArrayList<String>();

        @Override
        public String getName() {
            return "recorder";
        }

        @Override
        public void trackScreen(String name, String referrer) {
            seen.add("screen:" + name + (referrer == null ? "" : "<" + referrer));
        }

        @Override
        public void trackEvent(AnalyticsEvent event) {
            seen.add("event:" + event.getName() + "[" + event.getCategory() + "] "
                    + event.getParameters());
        }

        @Override
        public void setUserId(String id) {
            seen.add("userId:" + id);
        }

        @Override
        public void setUserProperty(String key, String value) {
            seen.add("property:" + key + "=" + value);
        }

        @Override
        public void reportCrash(AnalyticsCrashReport report) {
            seen.add("crash:" + report.getMessage());
        }

        boolean has(String line) {
            return seen.contains(line);
        }

        /// The last report of the event called `name`, without the `event:`
        /// it is written down with; null when there is none.
        String event(String name) {
            for (int iter = seen.size() - 1; iter >= 0; iter--) {
                if (seen.get(iter).startsWith("event:" + name + "[")) {
                    return seen.get(iter).substring("event:".length());
                }
            }
            return null;
        }
    }
}
