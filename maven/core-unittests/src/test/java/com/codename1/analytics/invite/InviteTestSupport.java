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
import com.codename1.ui.Display;

/**
 * Puts the static invite state back to a fresh-install baseline. Invites keeps
 * process-wide state on purpose -- it models one device -- so every case has to
 * start from a known point or the order tests run in changes their result.
 */
final class InviteTestSupport {
    private InviteTestSupport() {
    }

    /** The source freshInstall() leaves registered; holds its callback. */
    static PendingHandoffSource pendingHandoff;

    /**
     * An App Clip source that is supported and never answers on its own, so a
     * test can decide when -- and whether -- the handoff arrives.
     */
    static final class PendingHandoffSource implements AppClipHandoffSource {
        private AppClipHandoffCallback callback;

        public boolean isSupported() {
            return true;
        }

        public void requestHandoff(AppClipHandoffCallback cb) {
            callback = cb;
        }

        /** Counts the discards, which is what the iOS source clears on. */
        private int discarded;

        public void discardHandoff() {
            discarded++;
        }

        /** How many times the framework said it was done with the handoff. */
        int discardedCount() {
            return discarded;
        }

        /** True once Invites has asked. */
        boolean wasAsked() {
            return callback != null;
        }

        /** Delivers a code, as a clip that saw the link would. */
        void answer(String code) {
            answer(code, 0L);
        }

        /** Delivers a code with the tap time the clip observed. */
        void answer(String code, long clickedSeconds) {
            AppClipHandoffCallback cb = callback;
            callback = null;
            if (cb != null) {
                cb.onHandoff(code, clickedSeconds);
            }
        }

        /** Answers that no clip left anything, which is the common case. */
        void answerNothing(String reason) {
            AppClipHandoffCallback cb = callback;
            callback = null;
            if (cb != null) {
                cb.onUnavailable(reason);
            }
        }
    }

    static RecordingProvider freshInstall() {
        clearAppArg();
        Analytics.clearProviders();
        Analytics.clearDimensions();
        Analytics.setConsentMode(ConsentMode.OPT_IN);
        Analytics.setConsent(AnalyticsConsent.granted());
        Invites.setInviteListener(null);
        Invites.setLinkBase(null);
        Invites.setReattribution(false);
        Invites.setAttributionWindow(Invites.DEFAULT_ATTRIBUTION_WINDOW);
        Invites.registerInstallReferrerSource(null);
        // A clip source that is present and never answers, which is the state
        // the old statistical match left behind: a lookup outstanding with its
        // response still to come. Without one, every deferred lookup settles
        // the instant it starts -- correct on a device with no App Clip, and
        // useless for testing anything that happens while one is in flight.
        // A case that wants an answer installs its own.
        Invites.lookupRetryDelay = 30000L;
        // Any unspent write failure a previous case armed is disarmed here.
        // It used to disarm itself, because one failure consumed it; a case
        // that asks for several can leave a count behind, and a store that
        // refuses to save in a test that never asked for it is a confusing
        // way to fail.
        InviteStore.failWritesForTest(null, 0);
        Invites.reset();
        // Registered AFTER the reset, which now tells the source to discard
        // whatever the clip left -- forgetting has to reach a handoff nothing
        // has read yet. Registering first counted that discard against the
        // fixture and made every case start from one.
        pendingHandoff = new PendingHandoffSource();
        Invites.registerAppClipHandoffSource(pendingHandoff);
        Preferences.delete(Invites.PREF_SLUG);
        Preferences.delete(Invites.PREF_CONSUMED_ARG);
        // reset() clears the records; clearProviders() above dropped the
        // provider Invites registers, and the next facade call re-adds it.
        RecordingProvider recorder = new RecordingProvider();
        Analytics.addProvider(recorder);
        return recorder;
    }

    static void tearDown() {
        clearAppArg();
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

    // The launch argument is process-wide, so a test that sets one and does
    // not clear it sends every later test down the direct-link path.
    private static void clearAppArg() {
        Display d = Display.getInstance();
        if (d != null) {
            d.setProperty("AppArg", null);
        }
    }

    /** A canned server answer, in the shape the link service returns. */
    static String resolvedJson(String code, String campaign, String channel) {
        return "{\"resolved\":true,\"code\":\"" + code + "\",\"campaign\":\""
                + campaign + "\",\"channel\":\"" + channel
                + "\",\"score\":100,\"clickTs\":1700000000000}";
    }
}
