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
import com.codename1.analytics.Analytics;
import com.codename1.analytics.AnalyticsCapability;
import com.codename1.analytics.AnalyticsConsent;
import com.codename1.analytics.AnalyticsContext;
import com.codename1.analytics.ConsentMode;
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
        // The argument cannot be trusted to distinguish "refused" from "not
        // asked yet". Analytics.addProvider synthesizes AnalyticsConsent.denied()
        // for the null state, and this provider is registered on every facade
        // entry -- so a second launch before the user has answered the prompt
        // would arrive here looking exactly like an explicit refusal, delete
        // the deferred profile captured on the first launch, and move to
        // DECLINED. A later grant could then never resume, and the invite that
        // caused the install would be lost for a user who never refused
        // anything.
        //
        // Analytics.getConsent() returns null until there is a real choice on
        // record, so ask it instead of believing the argument.
        AnalyticsConsent recorded = Analytics.getConsent();
        if (recorded != null) {
            Invites.onConsentChanged(recorded.isAnalytics());
            return;
        }
        // No choice on record. Under OPT_IN that means the prompt has not been
        // answered and there is nothing to act on -- returning is the whole
        // point of the paragraph above. Under OPT_OUT it means something else
        // entirely: the mode's implicit allow is back in force, which is a real
        // transition. Clearing an explicit denial there resumed ordinary
        // analytics while a declined invite lookup stayed stopped and a
        // resolved attribution's dimensions stayed cleared, so the two
        // disagreed about the same user.
        if (Analytics.getConsentMode() == ConsentMode.OPT_OUT) {
            Invites.onConsentChanged(true);
        }
    }

    @Override
    public boolean supports(AnalyticsCapability capability) {
        return false;
    }
}
