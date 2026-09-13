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
import com.codename1.io.Log;
import com.codename1.io.Preferences;
import java.util.LinkedHashMap;
import java.util.Map;

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
    // Package private: a test models a genuinely first launch by removing it,
    // which is the state that decides whether a reset is seen as an identity
    // change at all.
    static final String PREF_LAST_CLIENT_ID = "cn1$inviteLastClientId";

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
        String last = baseline();
        if (last == null || last.length() == 0) {
            // No baseline, and with a VERIFIED store that now means one thing.
            //
            // It used to mean two, because the baseline lived in Preferences:
            // set() updates a static table and swallows the store's answer, so
            // a write that failed left exactly the same empty value as a write
            // that never happened. Records beside an empty baseline were read
            // as an erasure that never finished, and eraseInternal() ran --
            // which on a first launch whose baseline write had merely failed
            // DELETED a perfectly good invite. One storage hiccup was enough:
            // a direct link or an App Clip persists a pending invite, the
            // process exits, and the next launch destroys it.
            //
            // The baseline is a checked InviteStore write now, so an absent one
            // means the write never landed -- a storage failure, not a reset,
            // and storage that cannot hold this could not have held the records
            // either. So the baseline is simply (re)written, and nothing is
            // erased on the strength of its absence.
            //
            // A real reset is not detected by absence anyway. It is detected by
            // the baseline DIFFERING below, and by the durable erasure marker
            // that reset() and eraseInternal() leave when they cannot finish,
            // which resumeOwedErasure() retries on every entry point.
            rememberBaseline(seen);
            return;
        }
        if (!last.equals(seen)) {
            // The baseline moves only once the erasure is durable.
            //
            // Recording the new id regardless meant a failed marker write ended
            // the erasure for good: the held copy is retried by the next read of
            // the record, but a process that exits before one loses it, and the
            // next launch sees no change of identity, does not erase again, and
            // finds a state indistinguishable from a fresh install -- free to
            // start deferred attribution and be handed the same inviter back
            // under the new id. Leaving the baseline where it is costs one
            // repeated erasure and is the only thing here that survives the
            // process.
            if (Invites.eraseInternal()) {
                rememberBaseline(seen);
            }
        }
    }

    /// The client id this provider last saw, or the empty string.
    ///
    /// Reads the durable record, falling back to the Preferences key earlier
    /// builds used so an upgrade does not look like a first launch -- which
    /// would be harmless now, but would still cost a needless rewrite.
    private static String baseline() {
        Map<String, String> record = InviteStore.read(InviteStore.BASELINE);
        String stored = InviteStore.get(record, "clientId", null);
        if (stored != null && stored.length() > 0) {
            return stored;
        }
        return Preferences.get(PREF_LAST_CLIENT_ID, "");
    }

    /// Records the baseline durably.
    ///
    /// The Preferences key is kept in step so a downgrade, or any code still
    /// reading it, sees the same answer. It is the copy that cannot be
    /// trusted, not the one that is wrong.
    ///
    /// A write that does not land is reported rather than returned, because
    /// there is nothing a caller could do about it: the answer is to try again
    /// on the next entry point, which is what happens anyway.
    ///
    /// #### Parameters
    ///
    /// - `clientId`: the id now in force
    private static void rememberBaseline(String clientId) {
        Map<String, String> record = new LinkedHashMap<String, String>();
        record.put("clientId", clientId);
        boolean stored = InviteStore.write(InviteStore.BASELINE, record);
        Preferences.set(PREF_LAST_CLIENT_ID, clientId);
        if (!stored) {
            Log.p("invite: the identity baseline could not be stored, so an identity "
                    + "change may go unnoticed until it can be", Log.WARNING);
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
        // No recorded choice, so the MODE decides -- and the two answers are
        // not "allowed" and "refused". Under OPT_IN the prompt is simply
        // unanswered and NOTHING happens: reporting a refusal there would
        // delete the profile captured on the first launch and move to DECLINED
        // for a user who has refused nothing, which is the paragraph above.
        // Under OPT_OUT the implicit allow is in force and that is a real
        // transition.
        //
        // Analytics.setConsentMode now dispatches here when the mode changes,
        // which is what lets a switch to OPT_OUT reach this at all -- ordinary
        // analytics used to resume on that switch while a declined lookup
        // stayed stopped and an attribution's dimensions stayed cleared.
        if (Analytics.getConsentMode() == ConsentMode.OPT_OUT) {
            Invites.onConsentChanged(true);
            return;
        }
        // OPT_IN with nothing on record, which is a transition in the other
        // direction: the mode's implicit allow has just been withdrawn, so
        // allowed() answers no from here on. Requests queued a moment ago have
        // already passed that gate and would transmit the client id and the
        // invite metadata after transmission stopped being permitted.
        //
        // Killing them is all that happens. onConsentChanged(false) is the
        // refusal path -- it settles the lookup and clears the dimensions --
        // and nothing has been refused here: the prompt has not been answered.
        Invites.suspendTransmission();
    }

    @Override
    public boolean supports(AnalyticsCapability capability) {
        return false;
    }
}
