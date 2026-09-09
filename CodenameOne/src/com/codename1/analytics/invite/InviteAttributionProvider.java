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

import com.codename1.analytics.AbstractAnalyticsProvider;
import com.codename1.analytics.AnalyticsCapability;
import com.codename1.analytics.AnalyticsConsent;
import com.codename1.analytics.AnalyticsContext;
import com.codename1.io.Preferences;

// The seam that lets invite attribution honour an erasure request and a
// consent change without any edit to the Analytics facade.
//
// Analytics already calls init(context) on every provider from
// resetClientId(), with a context carrying the NEW client id, and
// onConsentChanged(consent) on every provider from setConsent(). Registering
// a provider is therefore enough to observe both, and this class exists only
// to do that.
//
// It matters because Analytics.resetClientId() does not clear custom
// dimensions. That is the right default -- an application's own dimensions
// are its data and it never asked to lose them -- but the referral dimensions
// identify an inviter, so leaving them behind would re-link a freshly issued
// pseudonymous id to the same person and defeat the erasure. Widening
// resetClientId to clear everything would have taken the application's
// dimensions with it, so the scoped erase lives here instead.
//
// The provider reports no capabilities and does nothing with events; it is a
// listener wearing a provider's interface.
final class InviteAttributionProvider extends AbstractAnalyticsProvider {
    // The last client id this provider saw. A change means resetClientId()
    // ran, which is what an erasure request looks like from here.
    private static final String PREF_LAST_CLIENT_ID = "cn1$inviteLastClientId";

    @Override
    public String getName() {
        return "invite-attribution";
    }

    @Override
    public void init(AnalyticsContext context) {
        super.init(context);
        String seen = context == null ? null : context.getClientId();
        if (seen == null) {
            return;
        }
        String last = Preferences.get(PREF_LAST_CLIENT_ID, "");
        if (last == null || last.length() == 0) {
            // First registration on this device. Record the baseline; this is
            // a provider being added, not an identity being erased.
            Preferences.set(PREF_LAST_CLIENT_ID, seen);
            return;
        }
        if (!last.equals(seen)) {
            Invites.eraseInternal();
            Preferences.set(PREF_LAST_CLIENT_ID, seen);
        }
    }

    @Override
    public void onConsentChanged(AnalyticsConsent consent) {
        Invites.onConsentChanged(consent != null && consent.isAnalytics());
    }

    @Override
    public boolean supports(AnalyticsCapability capability) {
        return false;
    }
}
