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
package com.codename1.analytics.invite;

import com.codename1.analytics.Analytics;
import com.codename1.analytics.AnalyticsConsent;
import com.codename1.analytics.ConsentMode;
import com.codename1.io.Preferences;

/**
 * Puts the static invite state back to a fresh-install baseline. Invites keeps
 * process-wide state on purpose -- it models one device -- so every case has to
 * start from a known point or the order tests run in changes their result.
 */
final class InviteTestSupport {
    private InviteTestSupport() {
    }

    static RecordingProvider freshInstall() {
        Analytics.clearProviders();
        Analytics.clearDimensions();
        Analytics.setConsentMode(ConsentMode.OPT_IN);
        Analytics.setConsent(AnalyticsConsent.granted());
        Invites.setInviteListener(null);
        Invites.setLinkBase(null);
        Invites.setReattribution(false);
        Invites.setAttributionWindow(Invites.DEFAULT_ATTRIBUTION_WINDOW);
        Invites.registerInstallReferrerSource(null);
        Invites.lookupRetryDelay = 30000L;
        Invites.reset();
        Preferences.delete(Invites.PREF_SLUG);
        Preferences.delete(Invites.PREF_CONSUMED_ARG);
        // reset() clears the records; clearProviders() above dropped the
        // provider Invites registers, and the next facade call re-adds it.
        RecordingProvider recorder = new RecordingProvider();
        Analytics.addProvider(recorder);
        return recorder;
    }

    static void tearDown() {
        Invites.setInviteListener(null);
        Invites.registerInstallReferrerSource(null);
        Invites.reset();
        Analytics.clearProviders();
        Analytics.clearDimensions();
        Analytics.setConsent(null);
        Analytics.setConsentMode(ConsentMode.OPT_IN);
        Preferences.delete(Invites.PREF_SLUG);
        Preferences.delete(Invites.PREF_CONSUMED_ARG);
    }

    /** A canned server answer, in the shape the link service returns. */
    static String resolvedJson(String code, String campaign, String channel) {
        return "{\"resolved\":true,\"code\":\"" + code + "\",\"campaign\":\""
                + campaign + "\",\"channel\":\"" + channel
                + "\",\"score\":100,\"clickTs\":1700000000000}";
    }
}
