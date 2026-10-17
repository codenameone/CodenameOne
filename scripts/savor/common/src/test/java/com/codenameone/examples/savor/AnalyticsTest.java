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
package com.codenameone.examples.savor;

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
        signIn(CUSTOMER, "Consent");
        shot("analytics-consent");
        assertFalse(Telemetry.asked(), "the question is answered before anyone answered it");
        click("consentNo");
        waitForForm("Customer");
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
        assertTrue(recorder.has("property:role=customer"), "the role: " + recorder.seen);
        assertTrue(recorder.has("property:language=en"), "the language: " + recorder.seen);

        // Each screen, once, under the name it was built with, and where the
        // user came to it from.
        back();
        waitForForm("Customer");
        until(() -> recorder.has("screen:Customer<Settings"), 5000, "the Customer screen view");
        quote();
        assertTrue(recorder.has("screen:PlaceSearch<Customer"), "the search: " + recorder.seen);
        until(() -> recorder.event("delivery_quoted") != null, 5000, "the quote to be reported");
        assertTrue(recorder.event("delivery_quoted").startsWith("delivery_quoted[delivery] {products="),
                "the quote: " + recorder.event("delivery_quoted"));

        // Asking for the delivery, with what an operator would chart about it.
        click("request");
        until(() -> recorder.event("delivery_requested") != null, 20000,
                "the request to be reported");
        assertEqual("delivery_requested[delivery] {product=standard, payment=cash}",
                recorder.event("delivery_requested"));
        // Nobody is onboarding, so the server may give the delivery up before the
        // customer does; either way the screen ends where it began.
        until(() -> {
            clickIfThere("dismiss");
            clickIfThere("cancel");
            return findByName("whereTo") != null;
        }, 30000, "the delivery to be cancelled");

        // Signing out, and in again: both are reported now.
        signOut();
        until(() -> recorder.has("event:logout[account] {reason=user}"), 5000,
                "the sign-out: " + recorder.seen);
        assertTrue(recorder.has("screen:Welcome<Customer"), "the welcome: " + recorder.seen);
        // The answer was given on this device: it is not asked for again.
        signIn(CUSTOMER, "Customer");
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
        until(() -> recorder.has("screen:Settings<Customer"), 5000, "the Settings screen view");
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
