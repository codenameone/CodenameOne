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
import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.Log;
import com.codename1.io.NetworkManager;
import com.codename1.io.Preferences;
import com.codename1.io.Util;
import com.codename1.share.ShareResult;
import com.codename1.share.ShareResultListener;
import com.codename1.ui.Display;
import com.codename1.ui.geom.Rectangle;
import com.codename1.util.Base64;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/// Invite a friend, and follow the invitation through to what it caused.
///
/// Mint an invite, share it, and on the friend's device recover the invite
/// that produced the install. Once attribution resolves it is written as
/// persistent analytics dimensions, so every later event -- including the
/// `purchase` event the framework already emits -- carries the campaign and
/// the referrer, and revenue per campaign comes out of the reports you have.
///
/// ### Sending
///
/// ```java
/// Invite invite = Invites.create(InviteRequest.create()
///         .campaign("spring")
///         .channel("share_sheet")
///         .build());
/// Invites.share(invite, "Come and try this with me");
/// ```
///
/// [#create] returns immediately and works with no network, so the share
/// sheet never waits on a server. Registration with the link service is
/// retried in the background.
///
/// ### Receiving
///
/// ```java
/// Invites.setInviteListener(new InviteListener() {
///     public void inviteReceived(InviteAttribution attribution) {
///         // attribution.getCode(), getCampaign(), getPayload()
///     }
///
///     public void attributionUnavailable(String reason) {
///     }
/// });
/// Invites.checkForInvite();
/// ```
///
/// Call [#checkForInvite] from your `start()` method. It is a pull rather
/// than a callback on purpose: Android delivers a link by replacing the
/// activity intent and iOS by setting a property, and reading the launch
/// argument is the one path that behaves the same on both.
///
/// ### Consent, and what is on the device before it
///
/// Everything reported here is gated on the analytics consent category of
/// [Analytics], and nothing is transmitted until consent is granted.
///
/// One thing does happen before consent: on first launch a coarse device
/// profile -- operating system version, hardware model, language, screen size
/// -- is written to local storage so that a deferred match is still possible
/// once consent arrives. It is never transmitted while consent is withheld,
/// and it is deleted outright if consent is refused. There is no alternative
/// that also works, because the window in which a deferred match can be made
/// closes within the hour, long before a typical consent prompt is answered.
/// [#setAttributionWindow] with `0` switches deferred attribution off
/// entirely.
///
/// ### How exact the answer is
///
/// [InviteAttribution#getMatchType] says how the attribution was made.
/// [#MATCH_DIRECT] and [#MATCH_REFERRER] are exact. [#MATCH_FINGERPRINT] is a
/// statistical match made on the server, used where the platform's store
/// carries no referrer, and it is occasionally wrong -- check
/// [InviteAttribution#getConfidence] and do not pay a referral bounty on it
/// without saying so.
public final class Invites {
    /// Nothing has been attributed and nothing is outstanding.
    public static final int STATE_NONE = 0;

    /// An invite is being resolved; the answer has not arrived yet.
    public static final int STATE_PENDING = 1;

    /// This install has been attributed to an invite.
    public static final int STATE_RESOLVED = 2;

    /// No invite will be attributed to this install.
    public static final int STATE_NONE_FOUND = 3;

    /// Attribution was abandoned because analytics consent was refused.
    public static final int STATE_DECLINED = 4;

    /// The link opened an application that was already installed. Exact.
    public static final String MATCH_DIRECT = "direct";

    /// The invite code made the whole trip through the application store and
    /// came back verbatim. Exact.
    public static final String MATCH_REFERRER = "referrer";

    /// The server matched this install to a click statistically, because the
    /// platform's store carries no referrer. Not exact.
    public static final String MATCH_FINGERPRINT = "fingerprint";

    /// No invite matched. The ordinary outcome for an uninvited install.
    public static final String REASON_NO_MATCH = "no_match";

    /// The attribution window closed before an answer arrived.
    public static final String REASON_EXPIRED = "expired";

    /// Analytics consent was refused, so attribution was abandoned.
    public static final String REASON_CONSENT_DENIED = "consent_denied";

    /// This platform cannot recover a deferred invite.
    public static final String REASON_UNSUPPORTED = "unsupported";

    /// The analytics category every invite event is reported under.
    public static final String CATEGORY = "referral";

    /// Dimension carrying the matched invite code.
    public static final String DIMENSION_CODE = "cn1_invite_code";

    /// Dimension carrying the campaign the invite belonged to.
    public static final String DIMENSION_CAMPAIGN = "cn1_campaign";

    /// Dimension carrying the channel the invite was sent through.
    public static final String DIMENSION_CHANNEL = "cn1_channel";

    /// Dimension carrying how the attribution was made.
    public static final String DIMENSION_MATCH = "cn1_invite_match";

    /// The default attribution window: how long after a first launch a
    /// deferred invite may still be resolved.
    public static final long DEFAULT_ATTRIBUTION_WINDOW = 7L * 24L * 60L * 60L * 1000L;

    static final String[] DIMENSIONS = {
        DIMENSION_CODE, DIMENSION_CAMPAIGN, DIMENSION_CHANNEL, DIMENSION_MATCH
    };

    private static final String DEFAULT_BASE_URL = "https://cloud.codenameone.com";
    private static final String PATH_MINT = "/api/v2/analytics/invites";
    private static final String PATH_CLAIM = "/api/v2/analytics/invites/claim";
    private static final String PATH_MATCH = "/api/v2/analytics/invites/match";

    // Package private so the unit tests can clear them between cases.
    /// Display property carrying the invite host the build registered, stamped
    /// by the builders from the `invite.domain` build hint.
    static final String PROPERTY_DOMAIN = "invite.domain";

    // The build stamps this beside the domain. It has to reach the client:
    // the Android app-links filter and the iOS path claim are both scoped to
    // /i/<slug>/, so a link minted without the slug does not match the app's
    // own filter and opens the browser instead.
    static final String PROPERTY_SLUG = "invite.slug";

    static final String PREF_SLUG = "cn1$inviteSlug";
    static final String PREF_CONSUMED_ARG = "cn1$inviteConsumedArg";

    // The referrer key the link service puts on the store url. Compared with
    // equals and never case folded: String.toLowerCase is locale sensitive and
    // has no root-locale overload here, so under a Turkish default locale the
    // 'i' folds to a dotless i and the key silently stops matching on exactly
    // the devices nobody can reproduce on.
    private static final String REFERRER_KEY = "cn1_invite";

    private static final int MAX_ATTEMPTS = 5;

    private static String linkBase;
    private static long attributionWindow = DEFAULT_ATTRIBUTION_WINDOW;
    private static boolean reattribution;
    private static InviteListener listener;
    private static InstallReferrerSource referrerSource;
    private static InviteAttribution resolved;
    private static boolean attributionLoaded;
    private static int state = STATE_NONE;
    private static boolean stateLoaded;
    private static boolean deliveredThisRun;
    private static boolean deferredStarted;

    // Bumped whenever the identity or the permission behind an outstanding
    // lookup changes -- an erasure, or consent being withdrawn. A response
    // carries the epoch it was issued under and is dropped if it no longer
    // matches, so a request already on the wire cannot resurrect an attribution
    // the user has just erased or refused.
    private static int lookupEpoch;

    // Only ever touched on the fallback path in newCode(), and held as a field
    // so there is one generator for the process rather than one per call.
    private static final java.util.Random FALLBACK_RANDOM = new java.util.Random();

    private Invites() {
    }

    /// Registers the platform hook that reads the application store's install
    /// referrer. The Codename One build calls this before the application
    /// starts on platforms that have one; an application does not.
    ///
    /// #### Parameters
    ///
    /// - `source`: the platform source, or null to remove it
    public static void registerInstallReferrerSource(InstallReferrerSource source) {
        referrerSource = source;
    }

    // ---- sending ---------------------------------------------------------

    /// Mints an invite and returns it immediately.
    ///
    /// This never blocks and never fails for want of a network. The code is
    /// generated on the device, so [Invite#getUrl] is usable at once;
    /// registration with the link service is queued and retried until it
    /// lands. A link clicked before that registration arrives is still
    /// attributed, because the server records the click against the code and
    /// joins it when the registration turns up.
    ///
    /// #### Parameters
    ///
    /// - `request`: what to mint, must not be null
    ///
    /// #### Returns
    ///
    /// the invite, never null
    public static Invite create(InviteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is null");
        }
        ensureProvider();
        String code = newCode();
        long now = System.currentTimeMillis();
        Invite invite = new Invite(code, buildUrl(code), request.getCampaign(),
                request.getChannel(), request.getPayload(), now);
        if (!queueRegistration(invite, request)) {
            // The outbox could not be persisted, and the invite has already
            // been minted -- so the choice is between sending now and losing
            // the registration for good. Send now: if it lands, the link is
            // registered with everything it carries; if it does not, nothing
            // is worse than the alternative. There is deliberately no retry,
            // because the queue that would drive one is the thing that failed.
            unacknowledged.add(invite.getCode());
            postRegistration(pendingRegistration);
        }
        Map<String, Object> p = new HashMap<String, Object>();
        p.put("invite_code", code);
        putIfSet(p, "campaign", request.getCampaign());
        putIfSet(p, "channel", request.getChannel());
        Analytics.autoEvent("invite_created", CATEGORY, p);
        flush();
        return invite;
    }

    /// Shares an invite through the native share sheet.
    ///
    /// #### Parameters
    ///
    /// - `invite`: the invite to share, must not be null
    ///
    /// - `message`: text placed before the link, or null for the link alone
    public static void share(Invite invite, String message) {
        share(invite, message, null, null);
    }

    /// Shares an invite through the native share sheet and reports the
    /// outcome.
    ///
    /// The invite funnel's `invite_shared` event is emitted from here, and
    /// only when the platform confirms the user actually shared -- a
    /// dismissed sheet reports `invite_share_dismissed` instead. That is what
    /// makes the "shared" number a measurement rather than an assumption.
    ///
    /// #### Parameters
    ///
    /// - `invite`: the invite to share, must not be null
    ///
    /// - `message`: text placed before the link, or null for the link alone
    ///
    /// - `sourceRect`: popover anchor hint, may be null
    ///
    /// - `resultListener`: receives the share outcome, may be null
    public static void share(Invite invite, String message, Rectangle sourceRect,
            ShareResultListener resultListener) {
        if (invite == null) {
            throw new IllegalArgumentException("invite is null");
        }
        Display d = Display.getInstance();
        if (d == null) {
            return;
        }
        String text = message == null || message.length() == 0
                ? invite.getUrl() : message + " " + invite.getUrl();
        d.share(text, null, null, sourceRect, chain(invite, resultListener));
    }

    /// Reports the outcome of a share your application performed itself,
    /// rather than through [#share]. Use this when the invite goes out
    /// through your own user interface -- a contact picker, a message
    /// composer, a copy-link button -- so the funnel still records whether it
    /// was really sent.
    ///
    /// `invite_shared` is emitted only when `result` says the user actually
    /// shared; a dismissed sheet reports `invite_share_dismissed` instead.
    /// Calling this is optional and calling it twice for one share double
    /// counts, so call it once, from the share callback.
    ///
    /// #### Parameters
    ///
    /// - `invite`: the invite that was shared, must not be null
    ///
    /// - `result`: the outcome the platform reported, may be null
    public static void reportShareResult(Invite invite, ShareResult result) {
        if (invite == null || result == null) {
            return;
        }
        Map<String, Object> p = new HashMap<String, Object>();
        p.put("invite_code", invite.getCode());
        putIfSet(p, "campaign", invite.getCampaign());
        putIfSet(p, "channel", invite.getChannel());
        if (result.isSharedTo()) {
            // May legitimately be null on older Android and the web share
            // api. Omitted rather than filled with a placeholder, so the
            // console's unknown rate stays honest.
            putIfSet(p, "target", result.getPackageName());
            Analytics.autoEvent("invite_shared", CATEGORY, p);
        } else if (result.isDismissed()) {
            Analytics.autoEvent("invite_share_dismissed", CATEGORY, p);
        }
    }

    // Wraps the caller's listener so the funnel sees the real outcome and the
    // caller still gets theirs.
    private static ShareResultListener chain(final Invite invite,
            final ShareResultListener delegate) {
        return new ShareResultListener() {
            @Override
            public void onResult(ShareResult result) {
                try {
                    reportShareResult(invite, result);
                } catch (Throwable t) {
                    Log.e(t);
                }
                if (delegate != null) {
                    delegate.onResult(result);
                }
            }
        };
    }

    // ---- receiving -------------------------------------------------------

    /// Registers the listener that receives the invite behind this install.
    ///
    /// An answer that arrived before the listener was registered -- which
    /// happens routinely on a cold launch from a link, because the platform
    /// delivers the link before the application starts -- is delivered as
    /// soon as this is called.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener, or null to remove it
    public static void setInviteListener(InviteListener l) {
        listener = l;
        ensureProvider();
        if (l != null) {
            deliverPending();
        }
    }

    /// The registered listener, or null.
    ///
    /// #### Returns
    ///
    /// the listener
    public static InviteListener getInviteListener() {
        return listener;
    }

    /// Looks for an invite: first in the launch argument, then, when this
    /// looks like a fresh install, by asking the link service.
    ///
    /// Safe and cheap to call on every start; it will not attribute twice and
    /// will not report twice.
    ///
    /// #### Returns
    ///
    /// true when the launch argument carried an invite link
    public static boolean checkForInvite() {
        ensureProvider();
        deliverPending();
        String appArg = null;
        Display d = Display.getInstance();
        if (d != null) {
            appArg = d.getProperty("AppArg", null);
        }
        boolean consumed = false;
        if (appArg != null && appArg.length() > 0
                && !appArg.equals(Preferences.get(PREF_CONSUMED_ARG, ""))) {
            consumed = handleUrl(appArg);
            if (consumed) {
                Preferences.set(PREF_CONSUMED_ARG, appArg);
            }
        }
        if (!consumed) {
            beginDeferred();
        }
        return consumed;
    }

    /// Offers a url to the invite machinery directly, for applications that
    /// consume the launch argument themselves or route it through
    /// `com.codename1.router`.
    ///
    /// #### Parameters
    ///
    /// - `url`: the url to inspect, may be null
    ///
    /// #### Returns
    ///
    /// true when the url carried an invite code
    public static boolean handleUrl(String url) {
        String code = extractCode(url);
        if (code == null) {
            return false;
        }
        ensureProvider();
        // The same guard beginDeferred() has. checkForInvite() treats a
        // consumed URL as handled and skips beginDeferred entirely, so without
        // this a refused user who opened an invite link still had a profile
        // persisted -- by a different route to the one that was fixed.
        if (explicitlyDenied()) {
            markTerminal(STATE_DECLINED, REASON_CONSENT_DENIED);
            return true;
        }
        if (getState() == STATE_RESOLVED && !reattribution) {
            // Already attributed. Re-engagement is worth counting, but
            // rewriting the cohort mid-stream would make lifetime value per
            // referrer unjoinable, so first touch stands.
            Map<String, Object> p = new HashMap<String, Object>();
            p.put("invite_code", code);
            p.put("match", MATCH_DIRECT);
            Analytics.autoEvent("invite_opened", CATEGORY, p);
            return true;
        }
        Map<String, String> pending = pendingRecord();
        pending.put("code", code);
        // The referrer question is settled: this install came from a link we
        // are holding the code for, so a referrer read is no longer a better
        // answer waiting to happen.
        pending.remove("referrerRetry");
        InviteStore.write(InviteStore.PENDING, pending);
        setState(STATE_PENDING);
        // A deferred fingerprint or referrer lookup may already be on the wire,
        // and this direct claim supersedes it. Without the bump both answers
        // pass the epoch guard, and a statistical match arriving second
        // overwrites the exact one -- its dimensions and its durable record
        // included. Advancing the epoch is how every other supersede in this
        // class is expressed, and claim() reads the new value.
        lookupEpoch++;
        claim(code, "universal_link", "", MATCH_DIRECT, false);
        return true;
    }

    /// The attribution for this install, or null when there is none yet.
    ///
    /// #### Returns
    ///
    /// the attribution
    public static InviteAttribution getAttribution() {
        loadAttribution();
        return resolved;
    }

    // Loads the durable record once. Guarded by a flag rather than by a null
    // check on the field itself: "no attribution" is a real answer, so a null
    // check would re-read storage on every call for the uninvited majority.
    // There is no locking here and there should not be -- the facade runs on
    // the EDT.
    private static void loadAttribution() {
        if (attributionLoaded) {
            return;
        }
        attributionLoaded = true;
        resolved = readAttribution();
    }

    // Drops everything cached in memory while leaving every durable record
    // in place -- which is exactly what a process restart does. Package
    // private and test-only: the whole point of the durable records is that
    // the answer survives a relaunch, and nothing else can check that.
    static void forgetLoadedState() {
        stateLoaded = false;
        attributionLoaded = false;
        resolved = null;
        state = STATE_NONE;
        deferredStarted = false;
        deliveredThisRun = false;
    }

    /// Where attribution has got to: one of the `STATE_` constants.
    ///
    /// #### Returns
    ///
    /// the current state
    public static int getState() {
        loadState();
        return state;
    }

    // Guarded by a flag rather than by a sentinel value on the field itself,
    // for the same reason loadAttribution() is: STATE_NONE is a real answer,
    // and re-deriving it from storage on every call would read the disk for
    // every uninvited install. No locking -- the facade runs on the EDT.
    private static void loadState() {
        if (stateLoaded) {
            return;
        }
        stateLoaded = true;
        Map<String, String> pending = InviteStore.read(InviteStore.PENDING);
        // Reduced to a value first. The obvious spelling -- null-check the
        // record inside the condition, then assign the static below it -- is
        // the shape PMD reads as an unsynchronized lazy singleton, and the
        // answer to that is not a lock: this facade runs on the EDT and adding
        // one would be the real mistake.
        int recorded = pending == null ? STATE_NONE
                : InviteStore.getInt(pending, "state", STATE_PENDING);
        // The pending record is consulted first only under re-attribution.
        // There a later invite writes a new claim while the earlier attribution
        // still stands, and answering STATE_RESOLVED from that old attribution
        // made beginDeferred() return -- so a claim interrupted by process
        // death was never retried and last touch silently kept losing to first.
        // Without re-attribution the resolved record is the answer, because a
        // stale pending record must never reopen a settled attribution.
        if (reattribution && recorded == STATE_PENDING) {
            state = STATE_PENDING;
            return;
        }
        if (getAttribution() != null) {
            state = STATE_RESOLVED;
            return;
        }
        state = recorded;
    }

    // ---- closing the funnel ---------------------------------------------

    /// Reports that the invited user reached the outcome the invite existed
    /// for -- signed up, joined the room, completed onboarding. No-op unless
    /// this install was attributed.
    ///
    /// #### Parameters
    ///
    /// - `action`: what the user did
    public static void conversion(String action) {
        conversion(action, 0d, null);
    }

    /// Reports a conversion carrying a value, so revenue can be attributed to
    /// the campaign and the referrer. No-op unless this install was
    /// attributed.
    ///
    /// #### Parameters
    ///
    /// - `action`: what the user did
    ///
    /// - `value`: the value of the conversion
    ///
    /// - `currency`: the currency code, or null
    public static void conversion(String action, double value, String currency) {
        InviteAttribution a = getAttribution();
        if (a == null) {
            return;
        }
        Map<String, Object> p = new HashMap<String, Object>();
        p.put("invite_code", a.getCode());
        putIfSet(p, "campaign", a.getCampaign());
        putIfSet(p, "channel", a.getChannel());
        putIfSet(p, "action", action);
        if (value != 0d) {
            p.put("value", Double.valueOf(value));
        }
        putIfSet(p, "currency", currency);
        Analytics.autoEvent("invite_converted", CATEGORY, p);
    }

    // ---- configuration ---------------------------------------------------

    /// Points the invite machinery at a different link service. Defaults to
    /// the Codename One cloud, honouring the `cloudServerURL` display
    /// property.
    ///
    /// #### Parameters
    ///
    /// - `url`: the base address, with no trailing path
    public static void setLinkBase(String url) {
        linkBase = url;
    }

    /// The link service base address in use.
    ///
    /// #### Returns
    ///
    /// the base address, never null
    public static String getLinkBase() {
        if (linkBase != null && linkBase.length() > 0) {
            return trimSlash(linkBase);
        }
        Display d = Display.getInstance();
        // The host the BUILD registered, stamped into the app by the builders
        // from the invite.domain hint. Without this the client happily minted
        // links for the default host while the generated Android intent filter
        // and iOS associated domain named a custom one, so an installed app
        // never opened its own links and nothing anywhere reported an error.
        String host = d == null ? null : d.getProperty(PROPERTY_DOMAIN, null);
        if (host != null && host.length() > 0) {
            return trimSlash(host.indexOf("://") >= 0 ? host : "https://" + host);
        }
        String base = d == null ? DEFAULT_BASE_URL
                : d.getProperty("cloudServerURL", DEFAULT_BASE_URL);
        if (base == null || base.length() == 0) {
            base = DEFAULT_BASE_URL;
        }
        return trimSlash(base);
    }

    /// How long after a first launch a deferred invite may still be
    /// resolved. Clamped to at most 30 days. Zero switches deferred
    /// attribution off, which is the supported way to ship without the
    /// statistical match.
    ///
    /// #### Parameters
    ///
    /// - `millis`: the window in milliseconds
    public static void setAttributionWindow(long millis) {
        long max = 30L * 24L * 60L * 60L * 1000L;
        if (millis < 0) {
            millis = 0;
        }
        if (millis > max) {
            millis = max;
        }
        attributionWindow = millis;
    }

    /// The attribution window in milliseconds.
    ///
    /// #### Returns
    ///
    /// the window
    public static long getAttributionWindow() {
        return attributionWindow;
    }

    /// Whether a later invite replaces an earlier attribution. Off by
    /// default: first touch stands, so a user's cohort does not change
    /// underneath the reports.
    ///
    /// #### Parameters
    ///
    /// - `value`: true for last touch
    public static void setReattribution(boolean value) {
        reattribution = value;
    }

    /// Whether last touch attribution is enabled.
    ///
    /// #### Returns
    ///
    /// true when a later invite replaces an earlier one
    public static boolean isReattribution() {
        return reattribution;
    }

    // ---- housekeeping ----------------------------------------------------

    /// Retries anything queued: unregistered invites, and an outstanding
    /// deferred match. Called for you on the paths that matter; exposed for
    /// an application that knows it has just regained connectivity.
    public static void flush() {
        drainOutbox();
        // A deferred lookup that failed because the first launch was offline
        // leaves deferredStarted set, and nothing else clears it inside the
        // process: the request is fail-silent, so no callback runs. Without
        // this, the documented "I have just regained connectivity" call would
        // drain registrations and silently leave the attribution unresolved
        // until the next cold start. The persisted attempt counter still
        // bounds the retries.
        if (getState() == STATE_PENDING) {
            deferredStarted = false;
            beginDeferred();
        }
    }

    /// Forgets every trace of invite attribution on this device: the pending
    /// fingerprint, the resolved attribution and the referral dimensions.
    ///
    /// [Analytics#resetClientId] triggers this for you, because an erasure
    /// that left the referral dimensions behind would re-link the fresh
    /// identity to the same inviter.
    public static void reset() {
        lookupEpoch++;
        InviteStore.delete(InviteStore.PENDING);
        InviteStore.delete(InviteStore.ATTRIBUTION);
        InviteStore.delete(InviteStore.OUTBOX);
        Preferences.delete(PREF_CONSUMED_ARG);
        clearDimensions();
        resolved = null;
        // Loaded, and the answer is "none" -- not "unknown", or the next call
        // would read the record we have just deleted back off the disk.
        attributionLoaded = true;
        state = STATE_NONE;
        stateLoaded = true;
        deliveredThisRun = false;
        deferredStarted = false;
        unacknowledged.clear();
    }

    // Package private test seam: the epoch an outstanding lookup was issued
    // under, so a test can simulate a response that raced an erasure.
    static int currentLookupEpochForTest() {
        return lookupEpoch;
    }

    // Package private test seam: drops the in-memory copy so the next read
    // comes off the disk, which is what the next process would do.
    static void forgetCachedAttributionForTest() {
        resolved = null;
        attributionLoaded = false;
        stateLoaded = false;
    }

    // Package private: the analytics provider hook calls this when the client
    // id changes underneath us, which is what an erasure request looks like.
    static void eraseInternal() {
        reset();
    }

    // Package private: called from the provider when consent changes.
    static void onConsentChanged(boolean allowed) {
        if (allowed) {
            if (getState() == STATE_PENDING) {
                deferredStarted = false;
                beginDeferred();
            } else if (getState() == STATE_RESOLVED) {
                // Re-granting restores the dimensions from the record we kept,
                // without re-reporting the install or telling the app again.
                InviteAttribution a = getAttribution();
                if (a != null) {
                    writeDimensions(a);
                }
            }
            drainOutbox();
            return;
        }
        // Refused. A device profile held for a match that is no longer
        // permitted has no reason to exist, so it goes now rather than at the
        // end of the window. The epoch bump additionally discards any response
        // already in flight.
        lookupEpoch++;
        if (getState() == STATE_PENDING) {
            // The profile goes and the answer stays. Deleting the record left
            // STATE_DECLINED in memory only -- setState() has nothing to
            // rewrite once the record is gone -- so the next launch read
            // STATE_NONE and told the listener again. The marker carries the
            // reason, which is what lets a later grant reopen it.
            markTerminal(STATE_DECLINED, REASON_CONSENT_DENIED);
            notifyUnavailable(REASON_CONSENT_DENIED);
        }
        clearDimensions();
    }

    // ---- internals -------------------------------------------------------

    // Registers the provider that gives us the erasure and consent hooks.
    // Analytics.clearProviders() can drop it, so this re-registers on facade
    // entry rather than only once; the provider list is a handful of entries.
    private static void ensureProvider() {
        try {
            List providers = Analytics.getProviders();
            for (Object provider : providers) {
                if (provider instanceof InviteAttributionProvider) {
                    return;
                }
            }
            Analytics.addProvider(new InviteAttributionProvider());
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    // The ordinary gate: what Analytics itself would allow.
    private static boolean allowed() {
        AnalyticsConsent c = Analytics.getConsent();
        if (Analytics.getConsentMode() == ConsentMode.OPT_OUT) {
            return c == null || c.isAnalytics();
        }
        return c != null && c.isAnalytics();
    }

    // The strict gate, for the statistical match only. Opt-out mode reports
    // permission with no user choice on record -- the deprecated
    // AnalyticsService forces exactly that for legacy callers -- and sending
    // a device profile under an implicit allow is not defensible. Everything
    // else uses allowed().
    // A recorded choice that says no. Distinct from "no choice yet", which
    // must never be treated as a refusal.
    private static boolean explicitlyDenied() {
        AnalyticsConsent c = Analytics.getConsent();
        return c != null && !c.isAnalytics();
    }

    private static boolean explicitlyAllowed() {
        AnalyticsConsent c = Analytics.getConsent();
        return c != null && c.isAnalytics();
    }

    private static String newCode() {
        byte[] raw = new byte[16];
        try {
            Util.secureRandomBytes(raw);
        } catch (Throwable t) {
            // A code identifies an invite and authorizes nothing, so a weaker
            // source degrades uniqueness, not security. Reported once rather
            // than failing the invite.
            Log.e(t);
            FALLBACK_RANDOM.nextBytes(raw);
        }
        String s = Base64.encodeUrlSafe(raw);
        int pad = s.indexOf('=');
        if (pad > 0) {
            s = s.substring(0, pad);
        }
        return s;
    }

    private static String buildUrl(String code) {
        String slug = configuredSlug();
        if (slug != null && slug.length() > 0) {
            return getLinkBase() + "/i/" + slug + "/" + code;
        }
        // No slug known yet -- the very first invite on a fresh install with
        // no network. The bare form still redirects correctly; the server
        // hands back the slugged url on registration and later invites use it.
        return getLinkBase() + "/i/" + code;
    }

    // Recognises our own link, or any url carrying the referrer key. The host
    // is compared with regionMatches rather than folded, because case folding
    // a protocol token is locale sensitive here.
    static String extractCode(String url) {
        if (url == null || url.length() == 0) {
            return null;
        }
        int q = url.indexOf('?');
        if (q >= 0) {
            String code = codeFromQuery(url.substring(q + 1));
            if (code != null) {
                return code;
            }
        }
        String host = hostOf(url);
        if (host == null) {
            return null;
        }
        String base = getLinkBase();
        String expected = hostOf(base);
        if (expected == null || !host.regionMatches(true, 0, expected, 0, expected.length())
                || host.length() != expected.length()) {
            return null;
        }
        String path = url;
        int schemeEnd = path.indexOf("://");
        if (schemeEnd >= 0) {
            int slash = path.indexOf('/', schemeEnd + 3);
            if (slash < 0) {
                return null;
            }
            path = path.substring(slash);
        }
        if (q >= 0) {
            int rel = path.indexOf('?');
            if (rel >= 0) {
                path = path.substring(0, rel);
            }
        }
        if (!path.startsWith("/i/")) {
            return null;
        }
        String rest = path.substring(3);
        while (rest.endsWith("/")) {
            rest = rest.substring(0, rest.length() - 1);
        }
        if (rest.length() == 0) {
            return null;
        }
        int slash = rest.lastIndexOf('/');
        String code = slash < 0 ? rest : rest.substring(slash + 1);
        if (slash > 0) {
            // Remember the slug so later invites mint the precise form.
            Preferences.set(PREF_SLUG, rest.substring(0, slash));
        }
        return code.length() == 0 ? null : code;
    }

    // Parses a referrer or query string for the invite key. Split on the
    // FIRST '=' only, and compare the key with equals -- never a case fold.
    static String codeFromQuery(String query) {
        if (query == null || query.length() == 0) {
            return null;
        }
        int start = 0;
        while (start <= query.length()) {
            int amp = query.indexOf('&', start);
            String pair = amp < 0 ? query.substring(start) : query.substring(start, amp);
            int eq = pair.indexOf('=');
            if (eq > 0) {
                String key = pair.substring(0, eq);
                if (REFERRER_KEY.equals(key)) {
                    String value = pair.substring(eq + 1);
                    try {
                        value = Util.decode(value, "UTF-8", true);
                    } catch (Throwable t) {
                        Log.e(t);
                    }
                    return value.length() == 0 ? null : value;
                }
            }
            if (amp < 0) {
                break;
            }
            start = amp + 1;
        }
        return null;
    }

    private static String hostOf(String url) {
        int schemeEnd = url.indexOf("://");
        if (schemeEnd < 0) {
            return null;
        }
        int start = schemeEnd + 3;
        int end = url.length();
        for (int i = start; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c == '/' || c == '?' || c == '#' || c == ':') {
                end = i;
                break;
            }
        }
        return end > start ? url.substring(start, end) : null;
    }

    // The build hint wins over the value the link service handed back: it is
    // what the generated intent filter and the associated domain were scoped
    // to, so minting anything else produces a link this build cannot open.
    // The stored value is the fallback for builds that set no hint, where the
    // server picks the slug and tells us on the first registration.
    private static String configuredSlug() {
        Display d = Display.getInstance();
        String slug = d == null ? null : d.getProperty(PROPERTY_SLUG, null);
        if (slug != null && slug.trim().length() > 0) {
            return slug.trim();
        }
        return Preferences.get(PREF_SLUG, "");
    }

    private static String trimSlash(String base) {
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    private static void putIfSet(Map<String, Object> p, String key, String value) {
        if (value != null && value.length() > 0) {
            p.put(key, value);
        }
    }

    private static void setState(int s) {
        state = s;
        stateLoaded = true;
        Map<String, String> pending = InviteStore.read(InviteStore.PENDING);
        if (pending != null) {
            pending.put("state", String.valueOf(s));
            InviteStore.write(InviteStore.PENDING, pending);
        }
    }

    // Replaces the pending record with a marker that says only "asked, and the
    // answer was no". Durable, so no later launch repeats the lookup, and it
    // carries none of the device profile the pending record held -- the profile
    // exists to be matched, and there is nothing left to match it against.
    private static void markTerminal() {
        markTerminal(null);
    }

    // reason is recorded only when the answer could stop being true. A window
    // of zero is the documented kill switch, and an application that later
    // ships a non-zero window is asking for attribution again -- so that one
    // marker is reopened rather than being permanent, which is why it is the
    // only one that carries a reason.
    private static void markTerminal(String reason) {
        markTerminal(STATE_NONE_FOUND, reason);
    }

    private static void markTerminal(int terminalState, String reason) {
        Map<String, String> done = new LinkedHashMap<String, String>();
        done.put("state", String.valueOf(terminalState));
        if (reason != null) {
            done.put("reason", reason);
        }
        InviteStore.write(InviteStore.PENDING, done);
        state = terminalState;
        stateLoaded = true;
    }

    private static Map<String, String> pendingRecord() {
        Map<String, String> pending = InviteStore.read(InviteStore.PENDING);
        if (pending != null) {
            return pending;
        }
        pending = new LinkedHashMap<String, String>();
        long now = System.currentTimeMillis();
        pending.put("firstLaunch", String.valueOf(now));
        pending.put("expiresAt", String.valueOf(now + attributionWindow));
        pending.put("attempts", "0");
        pending.put("state", String.valueOf(STATE_PENDING));
        Display d = Display.getInstance();
        if (d != null) {
            InviteStore.put(pending, "platform", d.getPlatformName());
            InviteStore.put(pending, "osVersion", d.getProperty("OSVer", ""));
            InviteStore.put(pending, "deviceModel",
                    d.getProperty("DeviceHardwareModel", d.getProperty("DeviceName", "")));
            pending.put("screenWidth", String.valueOf(d.getDisplayWidth()));
            pending.put("screenHeight", String.valueOf(d.getDisplayHeight()));
        }
        Locale loc = Locale.getDefault();
        InviteStore.put(pending, "locale", loc == null ? "" : loc.toString());
        InviteStore.write(InviteStore.PENDING, pending);
        return pending;
    }

    private static void beginDeferred() {
        if (deferredStarted) {
            return;
        }
        int s = getState();
        // Two terminal markers can stop being true, and both carry the reason
        // that made them. A window of zero is the documented kill switch and an
        // application that later ships a non-zero one is asking again; a
        // refusal is reversed by granting consent. Every other terminal answer
        // was a real answer about this install and stays. Reopening reads the
        // condition itself, never a second stored copy of it.
        if (s == STATE_NONE_FOUND || s == STATE_DECLINED) {
            Map<String, String> marker = InviteStore.read(InviteStore.PENDING);
            String why = InviteStore.get(marker, "reason", null);
            boolean reopen = (REASON_UNSUPPORTED.equals(why) && attributionWindow != 0)
                    || (REASON_CONSENT_DENIED.equals(why) && !explicitlyDenied());
            if (reopen) {
                InviteStore.delete(InviteStore.PENDING);
                state = STATE_NONE;
                s = STATE_NONE;
            }
        }
        if (s == STATE_RESOLVED || s == STATE_NONE_FOUND || s == STATE_DECLINED) {
            return;
        }
        if (attributionWindow == 0) {
            // setState() only rewrites a record that already exists, and on a
            // fresh install none does -- so this answer was purely in memory
            // and the listener heard it again on every launch, breaking the
            // documented once-per-install contract.
            markTerminal(REASON_UNSUPPORTED);
            notifyUnavailable(REASON_UNSUPPORTED);
            return;
        }
        // Checked BEFORE the profile is created, not after. pendingRecord()
        // persists on the spot, and onConsentChanged only deletes a record that
        // already exists when it runs -- so creating one here for a user who
        // had already refused left it on the device indefinitely, contradicting
        // the documented promise that a refused profile is deleted. An UNSET
        // choice still captures, which is the whole point: the match window
        // closes long before a consent prompt is answered.
        if (explicitlyDenied()) {
            // Durable, and profile free: markTerminal replaces the record with
            // the state and the reason and nothing else. The reason is what
            // lets beginDeferred reopen this if consent is later granted.
            markTerminal(STATE_DECLINED, REASON_CONSENT_DENIED);
            notifyUnavailable(REASON_CONSENT_DENIED);
            return;
        }
        Map<String, String> pending = pendingRecord();
        long expires = InviteStore.getLong(pending, "expiresAt", 0);
        if (expires > 0 && System.currentTimeMillis() > expires) {
            markTerminal();
            notifyUnavailable(REASON_EXPIRED);
            return;
        }
        if (InviteStore.getInt(pending, "attempts", 0) >= MAX_ATTEMPTS) {
            markTerminal();
            notifyUnavailable(REASON_NO_MATCH);
            return;
        }
        setState(STATE_PENDING);
        if (!allowed()) {
            // Nothing leaves the device. The record stays; onConsentChanged
            // restarts this the moment consent arrives.
            return;
        }
        deferredStarted = true;
        String code = InviteStore.get(pending, "code", null);
        if (code != null && code.length() > 0) {
            claim(code, "universal_link", "", MATCH_DIRECT, false);
            return;
        }
        InstallReferrerSource source = referrerSource;
        if (source != null && safeSupported(source)) {
            requestReferrer(source);
            return;
        }
        requestMatch(pending);
    }

    private static boolean safeSupported(InstallReferrerSource source) {
        try {
            return source.isSupported();
        } catch (Throwable t) {
            Log.e(t);
            return false;
        }
    }

    private static void requestReferrer(InstallReferrerSource source) {
        try {
            source.requestReferrer(new InstallReferrerCallback() {
                @Override
                public void onReferrer(final String rawReferrer, final long clickSeconds,
                        final long beginSeconds) {
                    onEdt(new Runnable() {
                        @Override
                        public void run() {
                            String code = codeFromQuery(rawReferrer);
                            if (code == null) {
                                // The referrer was read and carries no invite.
                                // That is an answer, not an outage.
                                fallBackToMatch(false);
                                return;
                            }
                            claim(code, "install_referrer",
                                    rawReferrer == null ? "" : rawReferrer,
                                    MATCH_REFERRER, true);
                        }
                    });
                }

                @Override
                public void onUnavailable(final String reason) {
                    onEdt(new Runnable() {
                        @Override
                        public void run() {
                            // REASON_UNSUPPORTED is the store saying this
                            // device will never have a referrer. Anything else
                            // is transient -- the store was busy, the bind
                            // failed -- and the source deliberately does not
                            // burn its once-only flag for those, so a later
                            // launch can still read the exact referrer. The
                            // statistical fallback runs either way, but a
                            // no-match answer to it must not be allowed to
                            // settle the install as organic while a
                            // deterministic answer is still reachable.
                            fallBackToMatch(!REASON_UNSUPPORTED.equals(reason));
                        }
                    });
                }
            });
        } catch (Throwable t) {
            Log.e(t);
            fallBackToMatch();
        }
    }

    // No store referrer: either the device has no store client, or this was
    // an organic install. Either way the statistical match is the only path
    // left, and it is the same one iOS always takes.
    private static void fallBackToMatch() {
        fallBackToMatch(false);
    }

    // retryable: the referrer could not be read this time but may be readable
    // later, so a no-match from the statistical fallback stays pending instead
    // of becoming the final word.
    private static void fallBackToMatch(boolean retryable) {
        Map<String, String> pending = InviteStore.read(InviteStore.PENDING);
        if (pending != null) {
            if (retryable) {
                pending.put("referrerRetry", "true");
            } else {
                // Definitive: either the referrer was read and carries no
                // invite, or the store says this device will never have one.
                // Leaving an earlier outage's marker in place made the
                // following no-match look retryable, so the lookup stayed
                // pending and every launch asked again until the attempt cap.
                pending.remove("referrerRetry");
            }
            InviteStore.write(InviteStore.PENDING, pending);
        }
        fallBackToMatchImpl();
    }

    private static void fallBackToMatchImpl() {
        Map<String, String> pending = InviteStore.read(InviteStore.PENDING);
        if (pending == null) {
            return;
        }
        requestMatch(pending);
    }

    private static void onEdt(Runnable r) {
        Display d = Display.getInstance();
        if (d == null) {
            r.run();
            return;
        }
        if (d.isEdt()) {
            r.run();
        } else {
            d.callSerially(r);
        }
    }

    private static void requestMatch(Map<String, String> pending) {
        if (!explicitlyAllowed()) {
            return;
        }
        bumpAttempts(pending);
        Map<String, Object> body = identity();
        body.put("platform", InviteStore.get(pending, "platform", ""));
        body.put("osVersion", InviteStore.get(pending, "osVersion", ""));
        body.put("deviceModel", InviteStore.get(pending, "deviceModel", ""));
        body.put("locale", InviteStore.get(pending, "locale", ""));
        body.put("screenWidth", Integer.valueOf(InviteStore.getInt(pending, "screenWidth", 0)));
        body.put("screenHeight", Integer.valueOf(InviteStore.getInt(pending, "screenHeight", 0)));
        post(getLinkBase() + PATH_MATCH, body, MATCH_FINGERPRINT, true);
    }

    private static void claim(String code, String source, String rawReferrer,
            final String matchType, final boolean deferred) {
        if (!allowed()) {
            return;
        }
        Map<String, String> pending = InviteStore.read(InviteStore.PENDING);
        if (pending != null) {
            bumpAttempts(pending);
        }
        Map<String, Object> body = identity();
        body.put("code", code);
        body.put("source", source);
        body.put("rawReferrer", rawReferrer == null ? "" : rawReferrer);
        post(getLinkBase() + PATH_CLAIM, body, matchType, deferred);
    }

    private static void bumpAttempts(Map<String, String> pending) {
        pending.put("attempts",
                String.valueOf(InviteStore.getInt(pending, "attempts", 0) + 1));
        InviteStore.write(InviteStore.PENDING, pending);
    }

    private static Map<String, Object> identity() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        Display d = Display.getInstance();
        body.put("clientId", Analytics.clientId());
        body.put("buildKey", d == null ? "" : d.getProperty("build_key", ""));
        body.put("packageName", d == null ? "" : d.getProperty("package_name", ""));
        body.put("consentAnalytics", Boolean.valueOf(allowed()));
        return body;
    }

    private static void post(String url, Map<String, Object> body, String matchType,
            boolean deferred) {
        send(url, JSONParser.mapToJson(body), matchType, deferred, false);
    }

    private static void send(String url, String json, String matchType, boolean deferred,
            boolean registration) {
        try {
            InviteConnection req = new InviteConnection(matchType, deferred, registration,
                    registration ? json : null, lookupEpoch);
            req.setUrl(url);
            req.setPost(true);
            req.setContentType("application/json");
            req.setRequestBody(json);
            req.setFailSilently(true);
            NetworkManager.getInstance().addToQueue(req);
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    // One request type for every invite call. Named rather than anonymous so
    // the two call sites share a single implementation, and so the equals()
    // exemption a one-shot request needs is scoped to one class.
    // Package private so a test can exercise the response handling directly.
    static final class InviteConnection extends ConnectionRequest {
        private final String matchType;
        private final boolean deferred;
        private final boolean registration;
        private final int epoch;
        // The outbox entry this request carries, so a success can retire
        // exactly that one rather than the whole queue.
        private final String outboxEntry;
        private String payload;
        // Set by the error hook below. ConnectionRequest reads the body of an
        // error response by default and then runs the ordinary success path
        // over it, so the status is the only thing that separates a real answer
        // from a 503 -- and getResponseCode() alone is not enough to test
        // against, because nothing can set it from outside the class.
        private boolean failed;

        InviteConnection(String matchType, boolean deferred, boolean registration,
                String outboxEntry, int epoch) {
            this.matchType = matchType;
            this.deferred = deferred;
            this.registration = registration;
            this.outboxEntry = outboxEntry;
            this.epoch = epoch;
        }

        @Override
        protected void handleErrorResponseCode(int code, String message) {
            failed = true;
        }

        // Package private so a test can drive the outcome this class exists to
        // get right without standing up a server.
        boolean isFailed() {
            int code = getResponseCode();
            return failed || (code != 0 && (code < 200 || code > 299));
        }

        @Override
        protected void readResponse(InputStream input) throws IOException {
            payload = new String(Util.readInputStream(input), "UTF-8");
        }

        @Override
        protected void postResponse() {
            // Reading the body of an error response is on by default
            // (ConnectionRequest.readResponseForErrorsDefault), and the error
            // path falls through to postResponse() exactly as a 200 does. So
            // this runs for a 503 too, and without the check a transient
            // outage retired the durable registration as though the server had
            // accepted it, while an error body parsed as "not resolved" turned
            // a server fault into a permanent "you were not invited".
            if (isFailed()) {
                return;
            }
            if (registration) {
                applySlug(payload);
                if (outboxEntry != null) {
                    registrationAcknowledged(outboxEntry);
                }
            } else {
                handleResolution(payload, matchType, deferred, epoch);
            }
        }
    }

    // The link service hands back the per-application path segment on any
    // answer. Remembering it is what lets later invites mint the precise form
    // that keeps two enrolled applications on one device from claiming each
    // other's links.
    private static void applySlug(String payload) {
        try {
            if (payload == null || payload.length() == 0) {
                return;
            }
            Map<String, Object> r = JSONParser.parseJSON(payload);
            if (r == null) {
                return;
            }
            Object slug = r.get("slug");
            if (slug instanceof String && ((String) slug).length() > 0) {
                Preferences.set(PREF_SLUG, (String) slug);
            }
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    // Package private rather than private so the unit tests can drive the real
    // resolution path with a canned server answer instead of racing the
    // network thread.
    static void handleResolution(String payload, String matchType, boolean deferred) {
        handleResolution(payload, matchType, deferred, lookupEpoch);
    }

    static void handleResolution(String payload, String matchType, boolean deferred, int epoch) {
        // A response that was already on the wire when consent was withdrawn or
        // the identity was erased must not be acted on. Both of those delete the
        // pending record and clear the dimensions; resolving anyway would write
        // them straight back, under the new identity, and undo the very
        // operation the user asked for.
        if (epoch != lookupEpoch || !allowed()) {
            return;
        }
        try {
            if (payload == null || payload.length() == 0) {
                return;
            }
            Map<String, Object> json = JSONParser.parseJSON(payload);
            if (json == null) {
                return;
            }
            applySlug(payload);
            if (!truthy(json.get("resolved"))) {
                // Not terminal while a deterministic answer is still
                // reachable. The Play referrer failed transiently -- the store
                // was busy, the bind did not take -- and the source keeps its
                // once-only flag unset precisely so a later launch can read the
                // exact referrer. Settling the install as organic here would
                // throw that away for a statistical guess. Bounded by the
                // attempt cap and the attribution window, both checked in
                // beginDeferred().
                Map<String, String> outstanding = InviteStore.read(InviteStore.PENDING);
                if (outstanding != null
                        && "true".equals(InviteStore.get(outstanding, "referrerRetry", null))) {
                    // Deliberately silent. attributionUnavailable() is the
                    // terminal callback -- it means no invite will be
                    // attributed -- and this outcome is the opposite of
                    // terminal. Worse, it sets deliveredThisRun, so a referrer
                    // that succeeded moments later in the same process could no
                    // longer deliver inviteReceived(), and a relaunch could
                    // deliver it as a second outcome after the first said
                    // never. The listener hears nothing until there is an
                    // answer.
                    setState(STATE_PENDING);
                    return;
                }
                // Terminal, and it has to be durable. Deleting the record is
                // not enough: loadState() reads an absent record as STATE_NONE,
                // so the next launch built a fresh profile and asked again, and
                // an ordinary uninvited install re-queried the server for ever.
                markTerminal();
                notifyUnavailable(REASON_NO_MATCH);
                return;
            }
            String code = str(json.get("code"));
            if (code == null) {
                return;
            }
            String confidence = str(json.get("confidence"));
            double score = 1d;
            Object rawScore = json.get("score");
            if (rawScore instanceof Number) {
                double s = ((Number) rawScore).doubleValue();
                score = s > 1d ? s / 100d : s;
            } else if (MATCH_FINGERPRINT.equals(matchType)) {
                score = 0d;
            }
            if (MATCH_DIRECT.equals(matchType) || MATCH_REFERRER.equals(matchType)) {
                score = 1d;
            }
            Map<String, String> params = new LinkedHashMap<String, String>();
            Object rawParams = json.get("parameters");
            if (rawParams instanceof Map) {
                Map raw = (Map) rawParams;
                for (Object next : raw.entrySet()) {
                    if (next instanceof Map.Entry) {
                        Map.Entry en = (Map.Entry) next;
                        Object k = en.getKey();
                        Object v = en.getValue();
                        if (k instanceof String && v instanceof String) {
                            params.put((String) k, (String) v);
                        }
                    }
                }
            }
            String serverMatch = str(json.get("match"));
            InviteAttribution a = new InviteAttribution(code, str(json.get("campaign")),
                    str(json.get("channel")), str(json.get("payload")),
                    serverMatch == null ? matchType : serverMatch, score, deferred,
                    longOf(json.get("clickTs")), System.currentTimeMillis(), params);
            resolve(a, confidence);
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    private static void resolve(InviteAttribution a, String confidence) {
        Map<String, String> record = new LinkedHashMap<String, String>();
        record.put("code", a.getCode());
        InviteStore.put(record, "campaign", a.getCampaign());
        InviteStore.put(record, "channel", a.getChannel());
        InviteStore.put(record, "payload", a.getPayload());
        record.put("match", a.getMatchType());
        record.put("confidence", String.valueOf(a.getConfidence()));
        record.put("deferred", String.valueOf(a.isDeferred()));
        record.put("clickTs", String.valueOf(a.getClickTimestamp()));
        record.put("resolvedTs", String.valueOf(a.getResolvedTimestamp()));
        // The inviter's custom parameters are part of the attribution the app
        // acts on, so they have to survive a restart -- an answer that arrives
        // before the listener is registered is delivered on the NEXT launch,
        // and would otherwise arrive stripped of them.
        if (!a.getParameters().isEmpty()) {
            InviteStore.put(record, "params",
                    JSONParser.mapToJson(new LinkedHashMap<String, Object>(a.getParameters())));
        }
        record.put("delivered", "false");
        // Storage was chosen over Preferences precisely because it reports a
        // failed write, so the result is checked. Deleting the pending record
        // after a failed write would leave neither an attribution nor any retry
        // information, losing the resolution permanently at the next restart.
        if (InviteStore.write(InviteStore.ATTRIBUTION, record)) {
            InviteStore.delete(InviteStore.PENDING);
        }
        resolved = a;
        attributionLoaded = true;
        state = STATE_RESOLVED;
        stateLoaded = true;
        writeDimensions(a);
        Map<String, Object> p = new HashMap<String, Object>();
        p.put("invite_code", a.getCode());
        putIfSet(p, "campaign", a.getCampaign());
        putIfSet(p, "channel", a.getChannel());
        p.put("match", a.getMatchType());
        putIfSet(p, "confidence", confidence);
        if (a.isDeferred()) {
            p.put("deferred", Boolean.TRUE);
            Analytics.autoEvent("invite_install", CATEGORY, p);
        } else {
            Analytics.autoEvent("invite_opened", CATEGORY, p);
        }
        deliverPending();
    }

    private static void writeDimensions(InviteAttribution a) {
        // Written unconditionally, nulls included -- setDimension(key, null)
        // removes the key. Required under re-attribution: a later invite with
        // no campaign used to leave the PREVIOUS campaign in place, so events
        // carried the new code beside the old campaign and the last-touch
        // cohort and its revenue were silently wrong.
        Analytics.setDimension(DIMENSION_CODE, a.getCode());
        Analytics.setDimension(DIMENSION_CAMPAIGN, a.getCampaign());
        Analytics.setDimension(DIMENSION_CHANNEL, a.getChannel());
        Analytics.setDimension(DIMENSION_MATCH, a.getMatchType());
    }

    private static void clearDimensions() {
        for (String dimension : DIMENSIONS) {
            Analytics.clearDimension(dimension);
        }
    }

    private static InviteAttribution readAttribution() {
        Map<String, String> r = InviteStore.read(InviteStore.ATTRIBUTION);
        if (r == null) {
            return null;
        }
        String code = InviteStore.get(r, "code", null);
        if (code == null) {
            return null;
        }
        return new InviteAttribution(code, InviteStore.get(r, "campaign", null),
                InviteStore.get(r, "channel", null), InviteStore.get(r, "payload", null),
                InviteStore.get(r, "match", MATCH_DIRECT),
                InviteStore.getDouble(r, "confidence", 1d),
                InviteStore.getBoolean(r, "deferred", false),
                InviteStore.getLong(r, "clickTs", 0),
                InviteStore.getLong(r, "resolvedTs", 0),
                parseParams(InviteStore.get(r, "params", null)));
    }

    private static Map<String, String> parseParams(String json) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        if (json == null || json.length() == 0) {
            return out;
        }
        try {
            Map<String, Object> parsed = JSONParser.parseJSON(json);
            if (parsed != null) {
                for (Object next : parsed.entrySet()) {
                    if (next instanceof Map.Entry) {
                        Map.Entry en = (Map.Entry) next;
                        Object k = en.getKey();
                        Object v = en.getValue();
                        if (k instanceof String && v instanceof String) {
                            out.put((String) k, (String) v);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.e(t);
        }
        return out;
    }

    // Delivers at most once per install. The durable flag is what survives a
    // restart; deliveredThisRun covers the window between resolving and the
    // flag reaching the disk, so a failed write costs at most a duplicate
    // after a crash rather than one on every launch.
    private static void deliverPending() {
        if (listener == null || deliveredThisRun) {
            return;
        }
        Map<String, String> r = InviteStore.read(InviteStore.ATTRIBUTION);
        if (r == null || InviteStore.getBoolean(r, "delivered", false)) {
            return;
        }
        InviteAttribution a = getAttribution();
        if (a == null) {
            return;
        }
        deliveredThisRun = true;
        r.put("delivered", "true");
        InviteStore.write(InviteStore.ATTRIBUTION, r);
        try {
            listener.inviteReceived(a);
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    private static void notifyUnavailable(String reason) {
        if (listener == null || deliveredThisRun) {
            return;
        }
        deliveredThisRun = true;
        try {
            listener.attributionUnavailable(reason);
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    // ---- registration outbox --------------------------------------------

    // The JSON of the registration the last queueRegistration() built, so a
    // failed enqueue can still be sent once rather than lost silently.
    private static String pendingRegistration;

    // Codes whose registration was sent directly because the outbox could not
    // be written. They are in flight and unacknowledged, and they are in no
    // durable queue -- so isRegistered() cannot infer anything from the outbox
    // for them and has to be told. In memory only, which is the honest limit:
    // the durable store is the thing that just failed.
    private static final List<String> unacknowledged = new ArrayList<String>();

    private static boolean queueRegistration(Invite invite, InviteRequest request) {
        Map<String, Object> body = identity();
        body.put("code", invite.getCode());
        putIfSet(body, "campaign", invite.getCampaign());
        putIfSet(body, "channel", invite.getChannel());
        putIfSet(body, "payload", invite.getPayload());
        putIfSet(body, "title", request.getTitle());
        putIfSet(body, "description", request.getDescription());
        putIfSet(body, "imageUrl", request.getImageUrl());
        if (!request.getParameters().isEmpty()) {
            body.put("parameters", new LinkedHashMap<String, String>(request.getParameters()));
        }
        pendingRegistration = JSONParser.mapToJson(body);
        List<String> outbox = InviteStore.readOutbox();
        outbox.add(pendingRegistration);
        return InviteStore.writeOutbox(outbox);
    }

    private static void drainOutbox() {
        if (!allowed()) {
            return;
        }
        List<String> outbox = InviteStore.readOutbox();
        if (outbox.isEmpty()) {
            return;
        }
        // Each entry is removed by its OWN successful response, never here.
        // Clearing the queue at send time looked harmless and was not: the
        // registration carries the campaign, channel, payload and preview
        // metadata, and none of it can be reconstructed from a click. The case
        // that loses it is exactly the case the outbox exists for -- an invite
        // minted with no network, which is the reason minting is offline in
        // the first place.
        //
        // Re-posting an entry that did land is harmless: the server keys on
        // the code and treats a repeat from the same inviter as idempotent.
        for (String json : outbox) {
            postRegistration(json);
        }
    }

    private static void postRegistration(String json) {
        send(getLinkBase() + PATH_MINT, json, MATCH_DIRECT, false, true);
    }

    // Called from the registration response, once its status has been checked.
    // Package private test seam: puts a code in the in-flight set without
    // having to make the durable store fail on demand.
    static void markSentDirectlyForTest(String code) {
        unacknowledged.add(code);
    }

    private static void registrationAcknowledged(String json) {
        for (int i = unacknowledged.size() - 1; i >= 0; i--) {
            String code = unacknowledged.get(i);
            if (json != null && json.indexOf(code) >= 0) {
                unacknowledged.remove(i);
            }
        }
        List<String> outbox = InviteStore.readOutbox();
        if (outbox.remove(json)) {
            InviteStore.writeOutbox(outbox);
        }
    }

    /// Whether the link service has acknowledged this invite.
    ///
    /// An unacknowledged invite is still shareable and still attributes --
    /// registration is retried until it lands -- so this is a diagnostic
    /// rather than a gate.
    ///
    /// #### Parameters
    ///
    /// - `invite`: the invite to ask about, may be null
    ///
    /// #### Returns
    ///
    /// true once the server has acknowledged it
    public static boolean isRegistered(Invite invite) {
        if (invite == null) {
            return false;
        }
        String code = invite.getCode();
        // Absence from the outbox is not acknowledgement on its own. When the
        // store could not be written the registration was sent directly and
        // never queued, so the outbox says nothing about it -- reading that
        // silence as success reported an in-flight, and possibly failed,
        // registration as acknowledged.
        if (unacknowledged.contains(code)) {
            return false;
        }
        for (String pending : InviteStore.readOutbox()) {
            if (pending != null && pending.indexOf(code) >= 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean truthy(Object o) {
        if (o instanceof Boolean) {
            return ((Boolean) o).booleanValue();
        }
        if (o instanceof String) {
            return "true".equals(o);
        }
        return false;
    }

    private static String str(Object o) {
        if (o instanceof String && ((String) o).length() > 0) {
            return (String) o;
        }
        return null;
    }

    private static long longOf(Object o) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        return 0;
    }
}
