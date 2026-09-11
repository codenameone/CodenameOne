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
/// [InviteAttribution#getMatchType] says how the attribution was made, and
/// every one of them is exact. [#MATCH_DIRECT] is a link opening an
/// application that was already installed; [#MATCH_REFERRER] is a code that
/// made the whole trip through the Play store; [#MATCH_APP_CLIP] is a code an
/// iOS App Clip received from the link itself and handed to the application it
/// installed.
///
/// There used to be a statistical match here as well, because the App Store
/// carries no referrer of its own and an iOS install could only be guessed at.
/// It was occasionally wrong, it could not say which times, and it required
/// collecting a hashed profile of people who installed nothing. App Clips made
/// it unnecessary and it is gone.
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

    /// An iOS App Clip received the invite link, kept the code, and handed it
    /// to the application the person then installed. Exact.
    public static final String MATCH_APP_CLIP = "app_clip";

    /// No invite matched. The ordinary outcome for an uninvited install.
    public static final String REASON_NO_MATCH = "no_match";

    /// The attribution window closed before an answer arrived.
    public static final String REASON_EXPIRED = "expired";

    /// Analytics consent was refused, so attribution was abandoned.
    public static final String REASON_CONSENT_DENIED = "consent_denied";

    /// This platform cannot recover a deferred invite.
    public static final String REASON_UNSUPPORTED = "unsupported";

    // Not public, and never delivered to a listener. It is the marker an
    // erasure leaves behind so the automatic lookup does not start again, and
    // an application has no decision to make about it -- the reasons above are
    // answers about an invite, this is a record that there is no longer anyone
    // to answer about.
    static final String REASON_ERASED = "erased";

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
    // Kept only so reset() can clear what earlier versions of this class
    // persisted. Nothing writes it any more -- see checkForInvite.
    static final String PREF_CONSUMED_ARG = "cn1$inviteConsumedArg";

    // The referrer key the link service puts on the store url. Compared with
    // equals and never case folded: String.toLowerCase is locale sensitive and
    // has no root-locale overload here, so under a Turkish default locale the
    // 'i' folds to a dotless i and the key silently stops matching on exactly
    // the devices nobody can reproduce on.
    private static final String REFERRER_KEY = "cn1_invite";

    // Package private so a test can drive the attempt cap without five round
    // trips.
    static final int MAX_ATTEMPTS = 5;

    private static String linkBase;
    private static long attributionWindow = DEFAULT_ATTRIBUTION_WINDOW;
    private static boolean reattribution;
    private static InviteListener listener;
    private static InstallReferrerSource referrerSource;

    // The iOS counterpart: the code an App Clip left in the container it shares
    // with this application. Registered by the build the same way, and absent
    // on every platform that has no clip.
    private static AppClipHandoffSource appClipSource;

    // Set when an erasure could not remove the durable records, and cleared
    // when a later attempt does. Nothing that transmits may run while it is
    // set: the records still on the disk describe the identity being erased.
    private static boolean erasurePending;
    private static InviteAttribution resolved;
    private static boolean attributionLoaded;
    private static int state = STATE_NONE;
    private static boolean stateLoaded;
    private static boolean deliveredThisRun;

    // A terminal "no invite" answer reached before a listener was registered.
    // Held for the run rather than persisted: the state itself is durable, and
    // a later launch reaches this answer again through the ordinary path.
    private static String undelivered;

    private static boolean deferredStarted;

    /// The pending record that could not be written, held until it can be.
    ///
    /// `Storage` can fail -- a full disk, a revoked sandbox -- and every
    /// caller here had already changed the in-memory state by the time it did.
    /// A direct link was the worst case: `handleUrl` committed STATE_PENDING
    /// and issued the claim, and if that request also failed, the exact code
    /// existed nowhere. The retry then read a record with no code in it and
    /// fell back to the install referrer or the App Clip handoff -- asking a
    /// question the device already had an exact answer to.
    ///
    /// Held only while the durable copy is missing: a successful write clears
    /// it, so this can never disagree with what is on the disk. It does not
    /// survive the process, and cannot -- that is what the durable record is
    /// for -- but a transient failure is over within one launch far more often
    /// than not.
    private static Map<String, String> pendingFallback;

    // When the last claim or match was issued. flush() restarts only once this
    // has aged out: retrying a request that is still outstanding spends an
    // attempt without a failure having been observed, and the attempt budget is
    // what decides when the install is settled.
    //
    // A timestamp rather than a boolean, because the requests are fail-silent
    // -- a failure produces no callback at all -- so a flag cleared by a
    // response would never be cleared for exactly the request a retry exists
    // for, and flush() could wedge for the rest of the process.
    private static long lookupIssuedAt;

    // Package private so a test can retry without waiting.
    static long lookupRetryDelay = 30000L;

    private static boolean lookupInFlight() {
        return lookupIssuedAt != 0
                && System.currentTimeMillis() - lookupIssuedAt < lookupRetryDelay;
    }

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

    /// Registers the platform hook that reads the invite code an iOS App Clip
    /// left for this application. The Codename One build calls this before the
    /// application starts on platforms that have one; an application does not.
    ///
    /// #### Parameters
    ///
    /// - `source`: the platform source, or null to remove it
    public static void registerAppClipHandoffSource(AppClipHandoffSource source) {
        appClipSource = source;
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
            if (allowed()) {
                unacknowledged.add(invite.getCode());
                postRegistration(pendingRegistration);
            } else {
                // Marked unacknowledged, exactly as the branch above does.
                //
                // isRegistered() reads absence from BOTH the outbox and this
                // set as acknowledgement, and neither holds this code: the
                // outbox write is what failed, and nothing was sent. So the
                // one invite the server is guaranteed never to have seen was
                // the one reported as registered.
                unacknowledged.add(invite.getCode());
                // Nothing leaves the device without consent, and that outranks
                // saving the registration. drainOutbox() carries the same guard;
                // this path had none, so a storage failure was the one way an
                // undecided or refused user's client id and invite metadata
                // reached the server.
                //
                // The registration is lost, because the queue that would have
                // held it is the thing that failed. That is the correct trade:
                // the link still attributes through the click, and only the
                // campaign, channel and preview metadata go with it.
                Log.p("invite: the registration outbox could not be written and consent "
                        + "does not permit sending, so this invite's campaign and preview "
                        + "metadata are lost", Log.WARNING);
            }
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
        // Deduplicated for this RUN, not for the life of the install.
        //
        // The point is to ignore repeated reads of one delivery -- an app that
        // calls this from start() and again from a form -- and a durable record
        // could not tell those apart from a second tap on the same link, which
        // delivers the identical string. So the same link tapped again was
        // ignored for ever: the install lost its invite_opened re-engagement
        // event, and under re-attribution the later open could never win.
        //
        // What made the durable guard necessary was Android handing the same
        // launch intent back on a later start. Both paths that read it now
        // consume the intent's data -- the lazy getAppArg() always did, and
        // dispatchNewIntentUrl does as well -- so a stale intent no longer
        // reproduces the argument.
        // The argument is CONSUMED, not remembered.
        //
        // Remembering the last value cannot tell two deliveries of one url
        // apart from two reads of one delivery -- and a live process really can
        // span both, an Android onNewIntent after the app is backgrounded being
        // the ordinary case. So the property is cleared instead: a later read
        // sees nothing, and a genuine second delivery sets it again and is
        // handled.
        //
        // Only when this really is an invite. Anything else is left exactly as
        // it arrived, so an application routing its own deep links is
        // unaffected -- and the property is read here after
        // Display.setProperty has already fired the external-url dispatch, so
        // a router that consumes it has done so before this runs.
        boolean consumed = false;
        if (appArg != null && appArg.length() > 0) {
            consumed = handleUrl(appArg);
            if (consumed && d != null) {
                d.setProperty("AppArg", null);
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
        // A tapped link is a fresh answer and would ordinarily reopen
        // attribution, but not while an erasure is still owed: claiming writes
        // a record the failing store cannot erase either, and the claim itself
        // carries the surviving old state. The retry usually succeeds, because
        // what stopped it was transient.
        if (!settleErasure()) {
            return false;
        }
        // The same guard beginDeferred() has. checkForInvite() treats a
        // consumed URL as handled and skips beginDeferred entirely, so without
        // this a refused user who opened an invite link still had a profile
        // persisted -- by a different route to the one that was fixed.
        if (explicitlyDenied()) {
            if (getAttribution() != null) {
                // Already attributed, and the listener has had its callback.
                // Writing a fresh DECLINED marker here contradicted the durable
                // attribution -- which is still there and makes the state
                // RESOLVED again on the next launch -- and delivered
                // attributionUnavailable() as a second, opposite answer for an
                // install that had already been given one.
                return true;
            }
            // The code is recorded on the way to the marker, which carries it
            // across the refusal. This branch runs BEFORE the pending record is
            // written, so without this there is nothing for markTerminal to
            // carry, and a user who grants consent afterwards has the exact
            // claim replaced by a referrer read or a statistical match.
            Map<String, String> denied = readPending();
            if (denied == null) {
                denied = new LinkedHashMap<String, String>();
            }
            denied.put("code", code);
            denied.put("codeSource", "universal_link");
            denied.put("codeMatch", MATCH_DIRECT);
            denied.put("codeDeferred", "false");
            writePending(denied);
            // Told, not silently dropped. checkForInvite() records the url as
            // consumed and skips the deferred path after this, so this is the
            // only chance the listener gets for this install -- and a
            // registered one heard nothing at all.
            if (markTerminal(STATE_DECLINED, REASON_CONSENT_DENIED)) {
                notifyUnavailable(REASON_CONSENT_DENIED);
            }
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
        // The held answer is not the answer any more. A no-match or an expiry
        // that became terminal with no listener is remembered in `undelivered`,
        // and leaving it there handed a listener registered after this link
        // resolved the stale unavailable result -- with deliveredThisRun then
        // suppressing the correct one.
        undelivered = null;
        Map<String, String> pending = pendingRecord();
        pending.put("code", code);
        // The window and the budget are reset, because this is a new question.
        // Inheriting them from an older deferred lookup meant a link opened
        // after that lookup had expired, or after its retries were spent, was
        // marked expired by beginDeferred() before the saved code was ever
        // looked at -- so an exact answer we were holding was never sent.
        long now = System.currentTimeMillis();
        pending.put("firstLaunch", String.valueOf(now));
        pending.put("expiresAt", String.valueOf(now + attributionWindow));
        pending.put("attempts", "0");
        pending.put("codeSource", "universal_link");
        pending.put("codeMatch", MATCH_DIRECT);
        pending.put("codeDeferred", "false");
        pending.put("codeReferrer", "");
        // The referrer question is settled: this install came from a link we
        // are holding the code for, so a referrer read is no longer a better
        // answer waiting to happen.
        pending.remove("referrerRetry");
        // And any terminal reason the record was carrying, which is what makes
        // a direct link the one thing that reopens an erased install: the
        // tombstone eraseInternal() leaves is a state and a reason, and this
        // overwrites both rather than reopening around them.
        pending.remove("reason");
        writePending(pending);
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
        ensureProvider();
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
        undelivered = null;
        lookupIssuedAt = 0;
        stateLoaded = false;
        attributionLoaded = false;
        resolved = null;
        state = STATE_NONE;
        deferredStarted = false;
        deliveredThisRun = false;
        // The in-memory pending copy goes with the rest of the loaded state.
        // Keeping it made "forget what you loaded" leave behind the one record
        // that had never reached the disk, so a test -- or an application
        // deliberately re-reading -- saw a record no launch could ever see.
        pendingFallback = null;
    }

    /// Where attribution has got to: one of the `STATE_` constants.
    ///
    /// #### Returns
    ///
    /// the current state
    public static int getState() {
        ensureProvider();
        loadState();
        return state;
    }

    // Guarded by a flag rather than by a sentinel value on the field itself,
    // for the same reason loadAttribution() is: STATE_NONE is a real answer,
    // and re-deriving it from storage on every call would read the disk for
    // every uninvited install. No locking -- the facade runs on the EDT.
    private static void loadState() {
        // The held record is reconciled BEFORE the cached answer is trusted.
        //
        // markTerminal() deliberately does not set the state when its write
        // fails, so the record held for retry can be terminal while memory
        // still says pending -- and getState() is public API. Answering PENDING
        // out of a cache the device's own record already contradicts is wrong
        // on its own terms, whatever the caller then does with it.
        //
        // It is NOT, measured, what a review round claimed: that flush() would
        // act on the stale answer, reopen the marker and issue a fresh lookup
        // without the profile markTerminal strips. It does not, because every
        // path that reopens or rewrites the record reads it first, and that
        // read drains the held copy and invalidates the cache before anything
        // is written. Traced end to end with the drain here removed: flush()
        // enters its restart branch on the stale PENDING and still finishes
        // with the state and the marker both terminal.
        //
        // Kept anyway, because "the answer is only ever wrong to callers that
        // go on to correct it" is an invariant nobody can see from here.
        Map<String, String> held = pendingFallback;
        if (held != null && writePending(held)) {
            stateLoaded = false;
        }
        if (stateLoaded) {
            return;
        }
        stateLoaded = true;
        Map<String, String> pending = readPending();
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
        boolean wasOn = reattribution;
        reattribution = value;
        // The cached state was derived under the old value. loadState() reads
        // the pending record only when re-attribution is on, so a process that
        // cached STATE_RESOLVED before this call would never look at a durable
        // replacement again -- and setInviteListener(), which most applications
        // call first, is enough to cache it.
        stateLoaded = false;
        // Turning it OFF discards a replacement RESPONSE already in flight.
        //
        // Changing the setting alone only changed how the state is read: an
        // outstanding replacement still passed handleResolution()'s epoch guard
        // and overwrote the first-touch attribution the setting had just said
        // to keep. The epoch bump fails it on arrival.
        //
        // The durable replacement record deliberately stays. Turning the
        // setting off and on again is a supported round trip -- there is a test
        // named for it -- and deleting the record would lose a link the user
        // really did open. What is cancelled is the request, not the invite.
        //
        // Guarded on there BEING an attribution, because that is what makes an
        // outstanding lookup a replacement. On a device with no attribution yet
        // the lookup in flight is the first one, and turning last touch off
        // says nothing about it -- discarding it there would lose an ordinary
        // install's attribution outright.
        if (wasOn && !value && getAttribution() != null) {
            lookupEpoch++;
            // Nothing is outstanding once the epoch has moved, so a later
            // resume can issue its own request rather than waiting out a retry
            // delay for one that can no longer be acted on.
            lookupIssuedAt = 0;
            deferredStarted = false;
        }
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
        ensureProvider();
        drainOutbox();
        // A deferred lookup that failed because the first launch was offline
        // leaves deferredStarted set, and nothing else clears it inside the
        // process: the request is fail-silent, so no callback runs. Without
        // this, the documented "I have just regained connectivity" call would
        // drain registrations and silently leave the attribution unresolved
        // until the next cold start. The persisted attempt counter still
        // bounds the retries.
        if (getState() == STATE_PENDING && !lookupInFlight()) {
            // Only when nothing is outstanding. Restarting on every call burned
            // the attempt budget without a single observed failure -- and
            // create() calls flush() unconditionally, so five invites minted in
            // a row exhausted MAX_ATTEMPTS and the last one settled the install
            // as terminal while its own answer was still on the wire.
            //
            // The retry supersedes whatever the last attempt left outstanding.
            // Without the bump, a fingerprint answer still on the wire from the
            // earlier attempt passes the guard and can land AFTER the retried
            // referrer resolved exactly -- overwriting the exact attribution
            // with a statistical one. Not hypothetical on an application with
            // more than one NetworkManager thread, where the two are genuinely
            // concurrent.
            lookupEpoch++;
            deferredStarted = false;
            beginDeferred();
        }
    }

    /// Forgets every trace of invite attribution on this device: the pending
    /// lookup, the resolved attribution and the referral dimensions.
    ///
    /// [Analytics#resetClientId] triggers this for you, because an erasure
    /// that left the referral dimensions behind would re-link the fresh
    /// identity to the same inviter.
    public static void reset() {
        if (!resetVerified()) {
            // The records did not go, and this method promised they would.
            //
            // Dropping the answer here left nothing blocked and nothing
            // retrying: a surviving PENDING record still carried its code, so
            // the next checkForInvite() claimed it, and a surviving outbox
            // entry still carried the old client id for the next flush. The
            // detection added inside resetVerified() was real and then thrown
            // away at the one call site an application reaches.
            //
            // Setting the flag is what settleErasure() gates every lookup,
            // claim and enqueue on, so nothing proceeds until a retry
            // succeeds. That retry runs eraseInternal(), which also writes the
            // tombstone -- so a reset that had to be retried ends terminal
            // rather than looking like a fresh install. That divergence is
            // deliberate: it only happens when the store refused, and there
            // the safe answer is to attribute nothing rather than to start a
            // fresh lookup over records that are still on the disk.
            //
            // Latched only when something really did survive, because the
            // consequence is severe and permanent-looking: nothing else
            // proceeds until an erasure succeeds, and only eraseInternal()
            // clears the flag. resetVerified() also answers false for a
            // reason that leaves nothing behind -- no Storage at all, which
            // is a device state rather than a refusal -- and latching on that
            // would block a device that has no invite data to block over.
            if (anythingSurvives()) {
                erasurePending = true;
            }
        }
    }

    /// Whether any durable invite record is still readable.
    ///
    /// The question a failed reset actually has to answer. A delete that could
    /// not run because there was no storage at all leaves nothing behind and
    /// is not the failure the erasure gate exists for; a record still on the
    /// disk is.
    ///
    /// #### Returns
    ///
    /// true when a record, an attribution or a queued registration remains
    private static boolean anythingSurvives() {
        Map<String, String> record = InviteStore.read(InviteStore.PENDING);
        if (record != null && !record.isEmpty()) {
            return true;
        }
        Map<String, String> attribution = InviteStore.read(InviteStore.ATTRIBUTION);
        if (attribution != null && !attribution.isEmpty()) {
            return true;
        }
        return !InviteStore.readOutbox().isEmpty();
    }

    /// The same work, reporting whether the durable records really went.
    ///
    /// Package private and separate so `reset()` keeps the signature an
    /// application already calls. The answer matters to exactly one caller:
    /// an erasure must not be reported complete while the attribution record
    /// is still readable, or it comes back on the next launch under the new
    /// identity.
    ///
    /// #### Returns
    ///
    /// true when nothing readable is left behind
    static boolean resetVerified() {
        lookupEpoch++;
        boolean cleared = InviteStore.delete(InviteStore.PENDING);
        forgetPendingFallback();
        // ATTRIBUTION names the inviter, and the OUTBOX is the queued
        // registration JSON -- which carries the OLD client id along with the
        // campaign, payload and preview. Both have to actually go.
        //
        // Ignoring the outbox result was a hole the size of the whole erasure:
        // if the store rejected deleting and overwriting it, the erasure still
        // reported success and the provider advanced its baseline, and the next
        // drainOutbox() transmitted a pre-erasure registration under the new
        // identity once storage recovered.
        //
        // The PENDING record is gated too. It looks like bookkeeping -- a
        // state, a deadline, an attempt count -- but it also carries the code
        // a direct link left on the device, and a code names an inviter. A
        // surviving one re-links the new identity to the old invite on the
        // next launch, which is the thing being erased.
        cleared &= InviteStore.delete(InviteStore.ATTRIBUTION);
        cleared &= InviteStore.delete(InviteStore.OUTBOX);
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
        lookupIssuedAt = 0;
        undelivered = null;
        unacknowledged.clear();
        if (cleared) {
            // The flag means "records survived an erasure", and they
            // demonstrably did not survive this one. Only eraseInternal()
            // cleared it before, so a plain reset() that succeeded left the
            // stale latch standing -- and the next gated call then ran a full
            // erasure, tombstone included, turning an application's ordinary
            // reset() into a terminal state it never asked for.
            erasurePending = false;
        }
        return cleared;
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
    static boolean eraseInternal() {
        // The DELETES have to have happened, not just been attempted.
        //
        // Storage.deleteStorageFile reports nothing useful: Android's
        // Context.deleteFile() and JavaSE's File.delete() both return a boolean
        // and neither throws, so a delete that failed looked exactly like one
        // that worked. reset() cleared the caches regardless, the tombstone was
        // written, and the provider recorded the new client id as fully
        // erased -- while the old attribution record was still on the disk. It
        // came back on the next launch, so getAttribution() and conversion()
        // reported the old referral identity under the new id, and a later
        // consent change restored its dimensions.
        boolean cleared = resetVerified();
        if (!cleared) {
            Log.p("invite: the attribution record could not be deleted, so the erasure is "
                    + "not complete and will be attempted again", Log.WARNING);
            erasurePending = true;
            return false;
        }
        // A tombstone, so the erasure is not undone by the next ordinary
        // launch.
        //
        // reset() deletes the records and leaves the state at STATE_NONE, which
        // is indistinguishable from a fresh install -- so the next routine
        // checkForInvite() built a new profile and started deferred matching
        // again. Inside the original click window, which on iOS is the normal
        // path, the server can match the same device to the same click and
        // restore the very inviter dimensions the user asked to be rid of,
        // under their new client id. The erasure would have lasted until the
        // next launch.
        //
        // The marker carries a state and a reason and NOTHING else: no code, no
        // fingerprint, no identifier, nothing the erasure was meant to remove.
        // It is marked delivered because there is no answer owed to anyone --
        // the install had one and it has just been erased -- and its reason is
        // not one beginDeferred() reopens, so the automatic lookup stays off.
        //
        // A direct link still reopens attribution: handleUrl() overwrites the
        // state and clears the reason, which is the right asymmetry. Somebody
        // who erases their identity and then taps a new invite is asking for
        // that invite; somebody who erases it and reopens the app is not.
        Map<String, String> erased = new LinkedHashMap<String, String>();
        erased.put("state", String.valueOf(STATE_NONE_FOUND));
        erased.put("reason", REASON_ERASED);
        erased.put("delivered", "true");
        if (!writePending(erased)) {
            // The caller is told, because the caller is what remembers that the
            // erasure happened.
            //
            // The held copy is retried by the next read of the record -- but if
            // the process exits before one, it is gone, and the provider had
            // already recorded the new client id as its baseline. The next
            // launch then sees no change, does not erase again, and finds
            // STATE_NONE: a fresh install as far as everything here is
            // concerned, free to start deferred attribution and be handed the
            // same inviter back. Leaving the baseline alone is what makes the
            // erasure happen again instead.
            Log.p("invite: the erasure marker could not be persisted; it will be applied "
                    + "again rather than reported as done", Log.WARNING);
            erasurePending = true;
            return false;
        }
        state = STATE_NONE_FOUND;
        stateLoaded = true;
        erasurePending = false;
        return true;
    }

    /// Retries an erasure that could not finish, and says whether anything
    /// else may proceed.
    ///
    /// `eraseInternal()` sets `erasurePending` when a delete or the tombstone
    /// write failed, and what survives on the disk is exactly what the erasure
    /// was asked to remove: a code, which names an inviter, and a queued
    /// registration carrying the OLD client id.
    ///
    /// This gate lived only in `drainOutbox()`, which left two ways past it.
    /// A lookup read the surviving code and claimed it under the NEW identity,
    /// which is the transmission the erasure existed to prevent. And `create()`
    /// appended to the surviving queue, after which the retry inside the very
    /// next drain deleted the whole queue -- the freshly minted invite with
    /// it, reported as enqueued and therefore not held in `unacknowledged`.
    ///
    /// A retry that fails again means storage is unusable, and the honest
    /// answer there is to do nothing rather than write more records that
    /// cannot be erased either.
    ///
    /// #### Returns
    ///
    /// true when no erasure is outstanding
    private static boolean settleErasure() {
        return !erasurePending || eraseInternal();
    }

    // Package private: called from the provider when consent changes.
    static void onConsentChanged(boolean allowed) {
        if (allowed) {
            // STATE_DECLINED belongs here too. It is the state a refusal during
            // a pending lookup leaves behind, and its marker is reopenable
            // precisely because granting consent afterwards is a real answer --
            // but nothing restarted the lookup until the application happened
            // to call checkForInvite() again, by which time the attribution
            // window may well have closed. beginDeferred() reopens the marker
            // itself, so calling it is the whole fix.
            int s = getState();
            // STATE_DECLINED has nothing outstanding by definition -- the
            // withdrawal that produced it discarded whatever was -- so only
            // STATE_PENDING is gated on the in-flight check.
            if (s == STATE_DECLINED || (s == STATE_PENDING && !lookupInFlight())) {
                // Only when nothing is outstanding, for the reason flush()
                // checks the same thing. An application may call setConsent()
                // again with analytics still allowed -- to change only
                // personalization or ad storage -- and restarting on that
                // queued a second lookup whose answer was every bit as valid as
                // the first, so the funnel event fired twice; repeated updates
                // also spent the retry budget without a failure.
                //
                // The refusal may have been recorded for a listener that had
                // not registered yet. It is not the answer any more, and
                // leaving it held meant a lookup that went on to resolve was
                // reported to that listener as unavailable instead.
                undelivered = null;
                deferredStarted = false;
                beginDeferred();
            } else if (s == STATE_RESOLVED) {
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
        // Nothing is outstanding once the epoch has moved: any response still
        // on the wire fails the guard. Saying so here is what lets a later
        // grant resume immediately rather than waiting out a retry delay for a
        // request that can no longer be acted on.
        lookupIssuedAt = 0;
        if (abandonReplacement()) {
            // A replacement running beside an existing attribution. Withdrawing
            // consent stops the replacement; it does not un-attribute the
            // install, whose record is still there and makes the state resolved
            // again on the next launch. Writing a DECLINED marker here told a
            // registered listener "no invite" as a second, contradictory
            // callback for an install it had already been told about.
            clearDimensions();
            return;
        }
        if (getState() == STATE_PENDING) {
            // The profile goes and the answer stays. Deleting the record left
            // STATE_DECLINED in memory only -- setState() has nothing to
            // rewrite once the record is gone -- so the next launch read
            // STATE_NONE and told the listener again. The marker carries the
            // reason, which is what lets a later grant reopen it.
            if (markTerminal(STATE_DECLINED, REASON_CONSENT_DENIED)) {
                notifyUnavailable(REASON_CONSENT_DENIED);
            }
        }
        clearDimensions();
    }

    // ---- internals -------------------------------------------------------

    // Registers the provider that gives us the erasure and consent hooks.
    // Analytics.clearProviders() can drop it, so this re-registers on facade
    // entry rather than only once; the provider list is a handful of entries.
    // Called by EVERY entry point that reads or transmits stored invite data,
    // not only the ones that start something.
    //
    // Analytics.clearProviders() is public and the deprecated
    // AnalyticsService.init() calls it, so this provider can be absent when an
    // erasure runs. Analytics.resetClientId() clears the reserved dimensions
    // itself, which needs no provider -- but the durable records are ours, and
    // only this provider's init() hook drops them. Registering here re-runs
    // that hook (addProvider calls init immediately), so the identity change is
    // noticed before anything reads the old attribution or sends the old
    // registration outbox under the new id.
    //
    // Analytics deliberately does not do this for us: a reference from
    // com.codename1.analytics to this package would match the platform feature
    // catalog's prefix and put a Play dependency and an API floor on every
    // application that logs a single event.
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
        // Stripped once, here, before either branch reads the url. Doing it on
        // the path branch alone left the query branch -- which runs first --
        // parsing "?cn1_invite=ABC123#section" and claiming a code with the
        // fragment glued to it. A fragment is client-side and part of neither
        // the path nor the query.
        int frag = url.indexOf('#');
        if (frag >= 0) {
            url = url.substring(0, frag);
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
        Map<String, String> pending = readPending();
        if (pending != null) {
            pending.put("state", String.valueOf(s));
            writePending(pending);
        }
    }

    // Replaces the pending record with a marker that says only "asked, and the
    // answer was no". Durable, so no later launch repeats the lookup, and it
    // carries none of the device profile the pending record held -- the profile
    // exists to be matched, and there is nothing left to match it against.
    // A pending record that sits BESIDE a resolved attribution is a
    // re-attribution replacement, not this install's only answer. Every way of
    // giving up on it -- a server no-match, the attempt cap, the window
    // expiring -- has to drop the replacement and leave the install resolved,
    // rather than writing a terminal marker the durable attribution contradicts
    // and telling the listener "no invite" after it has already been told
    // otherwise.
    //
    // Returns true when it handled the outcome.
    private static boolean abandonReplacement() {
        if (getAttribution() == null) {
            return false;
        }
        InviteStore.delete(InviteStore.PENDING);
        forgetPendingFallback();
        state = STATE_RESOLVED;
        stateLoaded = true;
        deferredStarted = false;
        lookupIssuedAt = 0;
        return true;
    }

    private static boolean hasSavedCode() {
        Map<String, String> pending = readPending();
        String code = InviteStore.get(pending, "code", null);
        return code != null && code.length() > 0;
    }

    private static boolean markTerminal() {
        return markTerminal(null);
    }

    // reason is recorded only when the answer could stop being true. A window
    // of zero is the documented kill switch, and an application that later
    // ships a non-zero window is asking for attribution again -- so that one
    // marker is reopened rather than being permanent, which is why it is the
    // only one that carries a reason.
    private static boolean markTerminal(String reason) {
        return markTerminal(STATE_NONE_FOUND, reason);
    }

    // Returns false when the marker could not be persisted, in which case
    // NOTHING is committed and the caller must not report the outcome.
    private static boolean markTerminal(int terminalState, String reason) {
        Map<String, String> done = new LinkedHashMap<String, String>();
        done.put("state", String.valueOf(terminalState));
        // The timing is carried, and only the timing. firstLaunch and expiresAt
        // say nothing about the device -- they are two clock readings -- and
        // without them a reopened marker started the window again from the
        // moment consent was granted. A user who answers the prompt a week
        // later would then have run a fresh fingerprint lookup and reported
        // invite_install for somebody else's click.
        //
        // Started here when there is no prior record, which is the ordinary
        // shape of a first launch by someone who had already refused: nothing
        // has run yet, so nothing wrote one. Copying nulls left the reopened
        // marker with expiresAt 0, and beginDeferred reads that as "no window",
        // so an arbitrarily old install could still run a fingerprint match.
        Map<String, String> before = readPending();
        long markedAt = System.currentTimeMillis();
        done.put("firstLaunch", InviteStore.get(before, "firstLaunch",
                String.valueOf(markedAt)));
        done.put("expiresAt", InviteStore.get(before, "expiresAt",
                String.valueOf(markedAt + attributionWindow)));
        // And the delivery state, for the same reason the resolved record
        // inherits it: a reopened lookup that ends terminally has still been
        // answered once, and dropping the flag here delivered a second
        // attributionUnavailable() to a listener registered afterwards. The
        // reopen protection covered a successful resolve and not this.
        if (InviteStore.getBoolean(before, "delivered", false)) {
            done.put("delivered", "true");
        }
        if (reason != null) {
            // Recorded on the marker, not only in memory. The listener contract
            // is "exactly one of the two methods per install, and the answer is
            // remembered": a resolved attribution has carried a durable
            // delivered flag from the start and the unavailable answer had
            // nothing, so an application whose deferred question was settled
            // before it registered its listener, in a process that then exited,
            // got neither callback for the life of the install.
            done.put("reason", reason);
        }
        // And the direct-link details, when there are any.
        //
        // A refusal is reopenable, so the code has to survive it: discarding it
        // meant a user who denied consent when the link arrived and granted it
        // afterwards had the exact claim replaced by a referrer read or a
        // statistical match, which can miss or credit a different click. Four
        // short fields, and none of them describes the device.
        for (String key : new String[] {"code", "codeSource", "codeMatch", "codeDeferred",
                "codeReferrer"}) {
            InviteStore.put(done, key, InviteStore.get(before, key, null));
        }
        if (!writePending(done)) {
            // Not reported now. Reporting a terminal outcome the device cannot
            // remember meant the same lookup and the same callback repeated
            // after every restart -- or, worse, the delivery flag landed on the
            // OLD pending record and left the state at PENDING, so a supposedly
            // settled lookup ran again and could never deliver its answer.
            //
            // The record is held by writePending() and persisted by the next
            // read, so the answer is not lost, only deferred: this run says
            // nothing and the marker is read back as an undelivered terminal
            // answer afterwards, which is what the contract promises. The state
            // is deliberately not set in memory either, so nothing here acts on
            // a record that may still be only in memory.
            Log.p("invite: a terminal answer could not be persisted; it will be reached "
                    + "again rather than reported now", Log.WARNING);
            return false;
        }
        state = terminalState;
        stateLoaded = true;
        return true;
    }

    // The terminal answer this install reached, if it was never delivered.
    // Null once a listener has heard it, so the contract's "exactly one per
    // install" holds across launches exactly as it does for a resolved
    // attribution.
    private static String undeliveredFromMarker() {
        int s = getState();
        if (s != STATE_NONE_FOUND && s != STATE_DECLINED) {
            return null;
        }
        Map<String, String> marker = readPending();
        if (marker == null || InviteStore.getBoolean(marker, "delivered", false)) {
            return null;
        }
        return InviteStore.get(marker, "reason", REASON_NO_MATCH);
    }

    // Returns false when the delivery could not be recorded. Same reasoning as
    // the resolved side: deliveredThisRun only suppresses duplicates until the
    // process exits, so telling the listener about a delivery the device cannot
    // remember means telling it again on the next launch.
    private static boolean markUnavailableDelivered() {
        Map<String, String> marker = readPending();
        if (marker == null) {
            // Nothing durable to mark. The answer is still terminal in memory
            // and the run's own guard prevents a repeat within it.
            return true;
        }
        if (InviteStore.getBoolean(marker, "delivered", false)) {
            return true;
        }
        marker.put("delivered", "true");
        if (writePending(marker)) {
            return true;
        }
        // Backed out of the map, not only reported.
        //
        // The caller withholds the callback when this returns false, so the
        // record must not go on claiming the answer was delivered -- and the
        // fallback holds THIS map. Left as it is, the next readPending() would
        // persist the very flag the failed write was supposed to prevent,
        // undeliveredFromMarker() would then read the answer as already given,
        // and the listener would never hear it on any launch. Removing is the
        // whole restore because the early return above means it was absent.
        marker.remove("delivered");
        return false;
    }

    /// Writes the pending record, keeping an in-memory copy while that fails.
    ///
    /// - `record`: the record to persist
    ///
    /// #### Returns
    ///
    /// true when it reached storage
    private static boolean writePending(Map<String, String> record) {
        boolean written = InviteStore.write(InviteStore.PENDING, record);
        // Cleared on success rather than left behind, so the fallback can never
        // shadow a newer durable record.
        pendingFallback = written ? null : record;
        return written;
    }

    /// Forgets the in-memory copy, for the paths that delete the record.
    private static void forgetPendingFallback() {
        pendingFallback = null;
    }

    /// Reads the pending record, preferring the copy a failed write left behind.
    ///
    /// The held copy is always the newer of the two, because it exists only
    /// between a write that failed and the next one that succeeds -- so the
    /// record still on the disk is whatever was there BEFORE the change that
    /// could not be saved. Reading the disk first was the shape of the bug this
    /// exists to close: a direct link's exact code was written into a record
    /// that never landed, the stale one underneath it had no code, and the
    /// retry answered with the install referrer or a fingerprint instead.
    ///
    /// Persisting is retried here rather than on a timer, which is the next
    /// time anything wanted the record anyway.
    ///
    /// #### Returns
    ///
    /// the record, or null when there is none
    /// The pending record as the feature itself sees it, for tests.
    ///
    /// Package private: the tests have to be able to tell the durable record
    /// apart from the copy held after a failed write, and going through
    /// `InviteStore` directly cannot.
    ///
    /// #### Returns
    ///
    /// the record, or null
    static boolean pendingFallbackPresentForTest() {
        return pendingFallback != null;
    }

    static Map<String, String> pendingRecordForTest() {
        return readPending();
    }

    private static Map<String, String> readPending() {
        Map<String, String> held = pendingFallback;
        if (held != null) {
            if (writePending(held)) {
                // Invalidated here TOO, not only in loadState().
                //
                // Whichever of the two drains the held record first is the one
                // that has to say so. loadState() reconciles before any state
                // decision is made, which is what beginDeferred() needs; but a
                // caller that reads the record directly can get here first, and
                // then loadState() finds nothing left to drain and trusts a
                // cached answer the record has already contradicted.
                stateLoaded = false;
            }
            return held;
        }
        return InviteStore.read(InviteStore.PENDING);
    }

    private static Map<String, String> pendingRecord() {
        Map<String, String> pending = readPending();
        if (pending != null) {
            return pending;
        }
        pending = new LinkedHashMap<String, String>();
        long now = System.currentTimeMillis();
        pending.put("firstLaunch", String.valueOf(now));
        pending.put("expiresAt", String.valueOf(now + attributionWindow));
        pending.put("attempts", "0");
        pending.put("state", String.valueOf(STATE_PENDING));
        writePending(pending);
        return pending;
    }

    private static void beginDeferred() {
        if (deferredStarted) {
            return;
        }
        // Before anything is read off the disk. A failed erasure leaves the
        // PENDING record there with the code it carried, and the lookup below
        // would reload that code and claim it under the new client id -- the
        // one thing the erasure was asked to make impossible.
        if (!settleErasure()) {
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
            Map<String, String> marker = readPending();
            String why = InviteStore.get(marker, "reason", null);
            boolean reopen = (REASON_UNSUPPORTED.equals(why) && attributionWindow != 0)
                    || (REASON_CONSENT_DENIED.equals(why) && !explicitlyDenied());
            if (reopen) {
                // The marker is CONVERTED, not deleted and rebuilt.
                //
                // Everything it carries has to survive the reopening: the
                // original window, so a late grant does not start a fresh one;
                // the delivered flag, so the listener is not told twice; and
                // the direct-link code, so an exact answer is not replaced by a
                // guess. Rebuilding from scratch lost each of those in turn,
                // one review round at a time, which is what this shape exists
                // to stop happening again.
                marker.put("state", String.valueOf(STATE_PENDING));
                if (REASON_UNSUPPORTED.equals(why)) {
                    // The window is recomputed for THIS reopening, and only
                    // this one.
                    //
                    // A marker written while the kill switch was on recorded
                    // expiresAt = firstLaunch + 0, so its window was already
                    // over at the instant it was created. Reopening it kept
                    // that zero-length window, the expiry check below settled
                    // the lookup again as "expired" on the same pass, and
                    // shipping a non-zero window later -- the documented way to
                    // ask again -- could therefore never work.
                    //
                    // firstLaunch is a fact about this install and stays; the
                    // window is a policy the application sets and the current
                    // one applies. The consent reopening is left alone: its
                    // marker was written under a real window, and recomputing
                    // there would change a value that is already right.
                    long began = InviteStore.getLong(marker, "firstLaunch",
                            System.currentTimeMillis());
                    marker.put("expiresAt", String.valueOf(began + attributionWindow));
                }
                marker.remove("reason");
                writePending(marker);
                state = STATE_PENDING;
                stateLoaded = true;
                s = STATE_PENDING;
            }
        }
        if (s == STATE_RESOLVED || s == STATE_NONE_FOUND || s == STATE_DECLINED) {
            return;
        }
        if (attributionWindow == 0 && !hasSavedCode()) {
            // The kill switch turns off DEFERRED attribution -- the statistical
            // lookup that needs a window to mean anything. A code we are
            // already holding is an exact answer that needs none, and refusing
            // to send it reported "unsupported" for an invite the user really
            // did open.
            //
            // setState() only rewrites a record that already exists, and on a
            // fresh install none does -- so this answer was purely in memory
            // and the listener heard it again on every launch, breaking the
            // documented once-per-install contract.
            if (markTerminal(REASON_UNSUPPORTED)) {
                notifyUnavailable(REASON_UNSUPPORTED);
            }
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
            if (markTerminal(STATE_DECLINED, REASON_CONSENT_DENIED)) {
                notifyUnavailable(REASON_CONSENT_DENIED);
            }
            return;
        }
        Map<String, String> pending = pendingRecord();
        // The window bounds the DEFERRED lookup, and a code we are holding is
        // not one -- it is an exact answer. Applying the expiry to it lost that
        // answer for the two cases where a saved code coexists with an expired
        // window: a zero window, where handleUrl records an expiry of "now",
        // and a first claim that failed and is being retried after the window
        // ran out. Same reasoning as the kill switch above.
        long expires = hasSavedCode() ? 0 : InviteStore.getLong(pending, "expiresAt", 0);
        if (expires > 0 && System.currentTimeMillis() > expires) {
            if (abandonReplacement()) {
                return;
            }
            if (markTerminal(REASON_EXPIRED)) {
                notifyUnavailable(REASON_EXPIRED);
            }
            return;
        }
        if (InviteStore.getInt(pending, "attempts", 0) >= MAX_ATTEMPTS) {
            if (abandonReplacement()) {
                return;
            }
            if (markTerminal()) {
                notifyUnavailable(REASON_NO_MATCH);
            }
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
            // Resent as what it was, not as a direct link. A referrer claim
            // that timed out is persisted here and retried, and hard-coding
            // the direct-link metadata reported it as invite_opened rather than
            // invite_install and handed the app an attribution whose
            // isDeferred() said false -- corrupting the install funnel for
            // exactly the deterministic answers this retry exists to save.
            String source = InviteStore.get(pending, "codeSource", "universal_link");
            String matchType = InviteStore.get(pending, "codeMatch", MATCH_DIRECT);
            boolean deferred = InviteStore.getBoolean(pending, "codeDeferred", false);
            claim(code, source, InviteStore.get(pending, "codeReferrer", ""),
                    matchType, deferred,
                    InviteStore.getLong(pending, "codeClicked", 0));
            return;
        }
        InstallReferrerSource source = referrerSource;
        if (source != null && safeSupported(source)) {
            requestReferrer(source);
            return;
        }
        requestAppClipHandoff(pending);
    }

    private static boolean safeSupported(InstallReferrerSource source) {
        try {
            return source.isSupported();
        } catch (Throwable t) {
            Log.e(t);
            return false;
        }
    }

    private static void requestReferrer(final InstallReferrerSource source) {
        // The epoch this read was ISSUED under, captured here. The platform
        // callback below can run long after a direct link arrived and advanced
        // the epoch, and reading the field at callback time made the old read
        // inherit the new epoch -- so it passed the guard and could overwrite
        // the direct attribution. Incrementing the epoch cannot invalidate a
        // callback that does not remember which epoch it belongs to.
        final int issued = lookupEpoch;
        // A referrer read IS a lookup in flight, and only claim() and
        // requestMatch() were saying so. A flush() during the read -- create()
        // issues one unconditionally -- therefore treated it as stale, advanced
        // the epoch and started again, and the guard above then discarded the
        // exact answer when it arrived. Worse than an ordinary lost retry,
        // because the source has already burned its once-only flag by then, so
        // the deterministic result is gone for good and the replacement falls
        // back to a statistical guess.
        lookupIssuedAt = System.currentTimeMillis();
        try {
            source.requestReferrer(new InstallReferrerCallback() {
                @Override
                public void onReferrer(final String rawReferrer, final long clickSeconds,
                        final long beginSeconds) {
                    onEdt(new Runnable() {
                        @Override
                        public void run() {
                            if (issued != lookupEpoch) {
                                return;
                            }
                            String code = codeFromQuery(rawReferrer);
                            if (code == null) {
                                // The referrer was read and carries no invite.
                                // That is an answer, not an outage.
                                fallBackToMatch(false);
                                return;
                            }
                            // Persisted BEFORE the claim goes out. The source
                            // has already burned its once-only flag by the time
                            // this runs, so if the claim fails -- a timeout, a
                            // dead network -- the exact code exists nowhere but
                            // this callback, and the next flush() falls back to
                            // a statistical match for an answer we had read
                            // exactly. Written into the pending record, the
                            // ordinary retry path resends it.
                            Map<String, String> pending = pendingRecord();
                            pending.put("code", code);
                            pending.put("codeSource", "install_referrer");
                            pending.put("codeMatch", MATCH_REFERRER);
                            pending.put("codeDeferred", "true");
                            InviteStore.put(pending, "codeReferrer",
                                    rawReferrer == null ? "" : rawReferrer);
                            // Play reports the tap time too, and it was being
                            // dropped for the same reason the clip's was: read
                            // from the callback and never written down. The
                            // redirect DID see this tap, so the server usually
                            // has its own record -- but not for a link opened
                            // from a place the redirect never ran, and not
                            // after retention has swept the click. Carrying it
                            // costs nothing and makes the two platforms report
                            // the same field the same way.
                            if (clickSeconds > 0) {
                                pending.put("codeClicked",
                                        String.valueOf(clickSeconds * 1000L));
                            }
                            pending.remove("referrerRetry");
                            writePending(pending);
                            claim(code, "install_referrer",
                                    rawReferrer == null ? "" : rawReferrer,
                                    MATCH_REFERRER, true,
                                    clickSeconds > 0 ? clickSeconds * 1000L : 0);
                        }
                    });
                }

                @Override
                public void onUnavailable(final String reason) {
                    onEdt(new Runnable() {
                        @Override
                        public void run() {
                            if (issued != lookupEpoch) {
                                return;
                            }
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
                            // Retryable only while the SOURCE would try
                            // again. Reading the reason alone was not enough:
                            // a successful read that returns an empty referrer
                            // reports REASON_NO_MATCH and burns the once-only
                            // flag, so it is definitive -- and treating it as
                            // transient left the lookup pending until the
                            // attempt budget ran out, for an answer that had
                            // already arrived.
                            fallBackToMatch(!REASON_UNSUPPORTED.equals(reason)
                                    && safeSupported(source));
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
        Map<String, String> pending = readPending();
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
            writePending(pending);
        }
        fallBackToMatchImpl();
    }

    private static void fallBackToMatchImpl() {
        Map<String, String> pending = readPending();
        if (pending == null) {
            return;
        }
        requestAppClipHandoff(pending);
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

    /// Asks the platform whether an App Clip left a code behind.
    ///
    /// This replaced a statistical match against a hashed device profile. That
    /// existed only because the App Store carries no referrer of its own, so an
    /// install deferred through it could only be guessed at -- from a coarse
    /// profile, a network prefix and an hour-long window, sometimes wrong and
    /// never able to say so. A clip is launched BY the invite link and receives
    /// it exactly, so the answer is a fact and the guess is gone, along with
    /// everything that was collected to make it.
    ///
    /// No consent gate beyond the ordinary one. The strict grant the match
    /// needed was for transmitting a device fingerprint; there is no
    /// fingerprint now, and the code this reads is one the person produced
    /// themselves by tapping an invite.
    ///
    /// - `pending`: the pending record, for the attempt budget
    private static void requestAppClipHandoff(final Map<String, String> pending) {
        final AppClipHandoffSource source = appClipSource;
        if (source == null || !source.isSupported()) {
            // No clip on this platform or this build, which is the ordinary
            // case: Android answered through the install referrer before
            // reaching here, and the desktop and the simulator have neither.
            //
            // NO_MATCH rather than UNSUPPORTED. This install was not invited --
            // that is a real answer about it, and a permanent one. UNSUPPORTED
            // is the reopenable marker the kill switch writes, so reporting it
            // here would have every launch reopen a lookup that can never have
            // anything to find.
            lookupIssuedAt = 0;
            settleNoHandoff(REASON_NO_MATCH);
            return;
        }
        bumpAttempts(pending);
        lookupIssuedAt = System.currentTimeMillis();
        // The epoch this read was issued under, checked when it answers.
        //
        // The read is asynchronous, and everything that supersedes a lookup
        // bumps the epoch: an erasure, a consent withdrawal, a direct link
        // arriving while this was outstanding. Without the check a clip code
        // read before an erasure could restore the attribution it removed, or
        // overwrite the newer exact claim that superseded it -- and the
        // unavailable branch could settle a lookup that is no longer the one
        // this answer belongs to.
        final int issued = lookupEpoch;
        source.requestHandoff(new AppClipHandoffCallback() {
            @Override
            public void onHandoff(final String code, final long clickedSeconds) {
                onEdt(new Runnable() {
                    @Override
                    public void run() {
                        if (issued != lookupEpoch) {
                            return;
                        }
                        lookupIssuedAt = 0;
                        if (code == null || code.length() == 0) {
                            settleNoHandoff(REASON_NO_MATCH);
                            return;
                        }
                        // WRITTEN DOWN before it is sent.
                        //
                        // The claim is one fail-silent request. If it does not
                        // land -- offline first launch, which is exactly when a
                        // fresh install happens -- the code existed only in
                        // this callback, the clip had already cleared its own
                        // copy, and the invite was gone for good. Persisting it
                        // first is what makes the retry possible, and it is
                        // what handleUrl() does with a direct code for the same
                        // reason.
                        Map<String, String> record = pendingRecord();
                        InviteStore.put(record, "code", code);
                        record.put("codeSource", "app_clip");
                        record.put("codeMatch", MATCH_APP_CLIP);
                        record.put("codeDeferred", "true");
                        record.put("codeReferrer", "");
                        // The tap time, and this is the only place it exists.
                        //
                        // An App Clip invocation is resolved by iOS from the
                        // association file, so it never reaches our redirect
                        // and the server has no click of its own to date the
                        // funnel from. The clip observed the tap and the
                        // native side cleared the handoff as it read it, so a
                        // value dropped here is gone -- and every App Clip
                        // attribution reported a click time of zero.
                        //
                        // Persisted in the record rather than only passed on,
                        // because the claim can fail and be resent from here.
                        if (clickedSeconds > 0) {
                            record.put("codeClicked",
                                    String.valueOf(clickedSeconds * 1000L));
                        }
                        writePending(record);
                        // Claimed exactly as a referrer code is: the trip
                        // through the store is what makes both of them exact,
                        // and the server treats them the same way.
                        claim(code, "app_clip", "", MATCH_APP_CLIP, true,
                                clickedSeconds > 0 ? clickedSeconds * 1000L : 0);
                    }
                });
            }

            @Override
            public void onUnavailable(final String reason) {
                onEdt(new Runnable() {
                    @Override
                    public void run() {
                        if (issued != lookupEpoch) {
                            return;
                        }
                        lookupIssuedAt = 0;
                        settleNoHandoff(reason == null ? REASON_NO_MATCH : reason);
                    }
                });
            }
        });
    }

    /// Settles an install no clip left anything for, which is most of them.
    private static void settleNoHandoff(String reason) {
        if (abandonReplacement()) {
            return;
        }
        // A transient referrer failure is not an answer about this install.
        //
        // fallBackToMatch(true) records referrerRetry for exactly that case --
        // the Play service was busy, the bind did not take, the service
        // dropped before it answered -- and then hands over to the clip
        // handoff. On Android there is no clip, so control arrives here
        // immediately and wrote a PERMANENT no-match over a referrer that was
        // there the whole time and readable on the next launch. The retry
        // marker was consulted on the server's answer and nowhere else, so
        // this path discarded it.
        //
        // Silent, like the matching branch in handleResolution:
        // attributionUnavailable() means no invite will ever be attributed,
        // and this is the opposite of terminal. The attempt cap and the
        // attribution window still bound how long it can go on.
        Map<String, String> outstanding = readPending();
        if (outstanding != null
                && "true".equals(InviteStore.get(outstanding, "referrerRetry", null))) {
            setState(STATE_PENDING);
            return;
        }
        if (markTerminal(reason)) {
            notifyUnavailable(reason);
        }
    }

    private static void claim(String code, String source, String rawReferrer,
            final String matchType, final boolean deferred) {
        claim(code, source, rawReferrer, matchType, deferred, 0);
    }

    /// clickedMillis: when the link was tapped, as the device observed it, or
    /// 0 when nothing on the device saw it. Only an App Clip has this: iOS
    /// resolves a clip invocation from the association file, so that tap never
    /// reaches the redirect and the server has no click to date the funnel
    /// from. It is a hint, never an override -- the server prefers its own
    /// observation, because this one is a number an app could put anything in.
    private static void claim(String code, String source, String rawReferrer,
            final String matchType, final boolean deferred, long clickedMillis) {
        if (!allowed()) {
            return;
        }
        Map<String, String> pending = readPending();
        if (pending != null) {
            bumpAttempts(pending);
        }
        Map<String, Object> body = identity();
        body.put("code", code);
        body.put("source", source);
        body.put("rawReferrer", rawReferrer == null ? "" : rawReferrer);
        if (clickedMillis > 0) {
            body.put("clickedMillis", Long.valueOf(clickedMillis));
        }
        lookupIssuedAt = System.currentTimeMillis();
        post(getLinkBase() + PATH_CLAIM, body, matchType, deferred);
    }

    private static void bumpAttempts(Map<String, String> pending) {
        pending.put("attempts",
                String.valueOf(InviteStore.getInt(pending, "attempts", 0) + 1));
        writePending(pending);
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
        send(url, json, json, matchType, deferred, registration);
    }

    /// Sends `json`, and remembers `outboxKey` as the entry to retire when the
    /// server accepts it.
    ///
    /// The two are the same string everywhere except one place: a queued
    /// registration is rewritten on the way out so its consent flag is current,
    /// and the entry sitting in the outbox is still the original. Passing the
    /// rewritten body as the key made `outbox.remove(...)` match nothing, so
    /// the registration was resent on every flush for ever and isRegistered()
    /// never became true.
    ///
    /// - `url`: where to send it
    /// - `json`: the body to transmit
    /// - `outboxKey`: the stored entry this acknowledges, or null
    /// - `matchType`: how the attribution was reached
    /// - `deferred`: whether this is the statistical path
    /// - `registration`: whether this is a mint registration
    private static void send(String url, String json, String outboxKey, String matchType,
            boolean deferred, boolean registration) {
        try {
            InviteConnection req = new InviteConnection(matchType, deferred, registration,
                    registration ? outboxKey : null, lookupEpoch);
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
        if (epoch == lookupEpoch) {
            lookupIssuedAt = 0;
        }
        // A response that was already on the wire when consent was withdrawn or
        // the identity was erased must not be acted on. Both of those delete the
        // pending record and clear the dimensions; resolving anyway would write
        // them straight back, under the new identity, and undo the very
        // operation the user asked for.
        if (epoch != lookupEpoch || !allowed()) {
            return;
        }
        // There is no kill-switch guard on the answer any more, because every
        // answer is exact.
        //
        // It refused a statistical match that setAttributionWindow(0) had
        // switched off, and a statistical match that arrived after the window
        // closed. Both were about a guess: a coarse profile matched on the
        // server, which could be wrong and could be stale. A referrer code and
        // an App Clip code are facts that made the trip through the store, and
        // a fact arriving late is still the right answer -- which is why the
        // guard had to be keyed on the match type rather than on `deferred` in
        // the first place, and why it has nothing left to key on now.
        //
        // The window still governs where the lookup STARTS: beginDeferred()
        // refuses to begin one past the deadline, and hasSavedCode() exempts a
        // code already in hand.
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
                if (truthy(json.get("retry"))) {
                    // "Not yet", not "no". The server has never seen this code
                    // at all, which during the offline-mint window is the
                    // normal state of a perfectly good invite: the inviter
                    // minted it with no network and their registration has not
                    // landed yet. Settling here reported an invited install as
                    // organic, permanently, seconds before the code became
                    // claimable.
                    //
                    // Silent for the same reason the referrer retry below is:
                    // attributionUnavailable() means no invite will ever be
                    // attributed, and this is the opposite of terminal. The
                    // existing attempt cap and attribution window bound how
                    // long this can go on.
                    setState(STATE_PENDING);
                    return;
                }
                // Not terminal while a deterministic answer is still
                // reachable. The Play referrer failed transiently -- the store
                // was busy, the bind did not take -- and the source keeps its
                // once-only flag unset precisely so a later launch can read the
                // exact referrer. Settling the install as organic here would
                // throw that away for a statistical guess. Bounded by the
                // attempt cap and the attribution window, both checked in
                // beginDeferred().
                Map<String, String> outstanding = readPending();
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
                if (getAttribution() != null) {
                    // A re-attribution claim that found nothing. The earlier
                    // attribution is still the answer for this install, so
                    // nothing is terminal here -- terminalizing it contradicted
                    // the durable record, which still says RESOLVED, and told
                    // the listener "no invite" as a second, opposite callback
                    // after it had already been given one.
                    //
                    // Returning is not enough either: handleUrl wrote a PENDING
                    // record for the replacement before issuing this claim, so
                    // leaving it there kept the install pending, and every
                    // later flush and launch retried the failed replacement
                    // until the attempt cap finally reported unavailable --
                    // still with the durable attribution sitting beside it. The
                    // replacement attempt is dropped and the install goes back
                    // to what it was.
                    InviteStore.delete(InviteStore.PENDING);
                    forgetPendingFallback();
                    state = STATE_RESOLVED;
                    stateLoaded = true;
                    deferredStarted = false;
                    lookupIssuedAt = 0;
                    return;
                }
                // Terminal, and it has to be durable. Deleting the record is
                // not enough: loadState() reads an absent record as STATE_NONE,
                // so the next launch built a fresh profile and asked again, and
                // an ordinary uninvited install re-queried the server for ever.
                if (markTerminal()) {
                    notifyUnavailable(REASON_NO_MATCH);
                }
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
            }
            // Every match type is exact now, so the score is one whatever the
            // server said. It survives because InviteAttribution advertises it
            // and an application may read it; it no longer varies.
            if (MATCH_DIRECT.equals(matchType) || MATCH_REFERRER.equals(matchType)
                    || MATCH_APP_CLIP.equals(matchType)) {
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
        // Carried across from the record this one replaces. Re-attribution
        // rewrites the attribution but not the fact that the listener has
        // already been told about this install, and the contract is exactly one
        // callback per install -- resetting the flag delivered inviteReceived()
        // a second time, immediately if the first had happened in an earlier
        // process and on the next launch if it had happened in this one.
        // The attribution being replaced, or -- when there is none, because
        // this lookup was resumed after a delivered refusal -- the pending
        // record that carried the fact across the reopen.
        Map<String, String> previous = InviteStore.read(InviteStore.ATTRIBUTION);
        if (previous == null) {
            previous = readPending();
        }
        record.put("delivered",
                String.valueOf(InviteStore.getBoolean(previous, "delivered", false)));
        // Storage was chosen over Preferences precisely because it reports a
        // failed write, so the result is checked. Deleting the pending record
        // after a failed write would leave neither an attribution nor any retry
        // information, losing the resolution permanently at the next restart.
        if (!InviteStore.write(InviteStore.ATTRIBUTION, record)) {
            // The store is full or read-only. Everything below assumes the
            // record is on disk: deliverPending() re-reads it before calling
            // the listener and finds nothing, and flush() will not retry
            // because the state says resolved -- so a valid answer was neither
            // delivered nor asked for again until the process restarted. The
            // pending record is deliberately left in place, so the next flush
            // or launch resends the lookup.
            // And the attempt is given back. Leaving the counter at the cap
            // meant the next flush took the attempt-cap branch and marked the
            // install terminal instead of performing the retry this promises --
            // so the very last response, the one most likely to be the only one
            // left, could never be stored.
            Map<String, String> retry = readPending();
            if (retry != null) {
                int spent = InviteStore.getInt(retry, "attempts", 0);
                retry.put("attempts", String.valueOf(spent > 0 ? spent - 1 : 0));
                if (!writePending(retry)) {
                    // The refund failed for the same reason the attribution
                    // did -- the store is unwritable -- so the count ON DISK is
                    // still at the cap. writePending() holds the refunded copy
                    // and the next read persists it, so a retry within this
                    // launch sees the right number; a restart before that does
                    // not, and settles the install rather than asking again.
                    // Said out loud rather than assumed away.
                    Log.p("invite: the attempt could not be refunded, so a later retry may "
                            + "settle this install instead of asking again", Log.WARNING);
                }
            }
            Log.p("invite: the attribution could not be persisted, so the lookup stays "
                    + "pending and will be retried", Log.WARNING);
            return;
        }
        InviteStore.delete(InviteStore.PENDING);
        forgetPendingFallback();
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
        // Taken into a local and cleared unconditionally, rather than
        // null-checked in place and cleared inside the branch. Same reason as
        // notifyUnavailable above. The durable half comes second: an answer
        // reached in an earlier process left nothing in memory, and the
        // contract says the answer is remembered.
        String held = undelivered;
        undelivered = null;
        if (held == null) {
            held = undeliveredFromMarker();
        }
        if (held != null) {
            notifyUnavailable(held);
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
        r.put("delivered", "true");
        if (!InviteStore.write(InviteStore.ATTRIBUTION, r)) {
            // deliveredThisRun only suppresses duplicates until the process
            // exits, so calling the listener on a delivery the device cannot
            // remember means inviteReceived() fires again on the next launch --
            // against the exactly-once contract. Better to deliver late, on a
            // launch where the flag can be written, than twice.
            Log.p("invite: the delivery could not be recorded, so the attribution will be "
                    + "delivered on a later launch instead of twice", Log.WARNING);
            return;
        }
        deliveredThisRun = true;
        try {
            listener.inviteReceived(a);
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    private static void notifyUnavailable(String reason) {
        if (deliveredThisRun) {
            return;
        }
        // Read into a local before the branch, for the same reason loadState()
        // does: null-checking a static field and then assigning one inside the
        // branch is the shape PMD reads as an unsynchronized lazy singleton,
        // and the answer is not a lock -- this facade runs on the EDT.
        InviteListener target = listener;
        if (target == null) {
            // Held for this run, and durably by the marker markTerminal wrote.
            // Either way it is not dropped: the answer is terminal, so no later
            // lookup produces it again, and setInviteListener() would otherwise
            // replay only a resolved attribution.
            undelivered = reason;
            return;
        }
        if (!markUnavailableDelivered()) {
            Log.p("invite: the delivery could not be recorded, so this answer will be "
                    + "reported on a later launch instead of twice", Log.WARNING);
            return;
        }
        deliveredThisRun = true;
        try {
            target.attributionUnavailable(reason);
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

    /// Records that a queued registration was evicted to keep the outbox
    /// under its cap.
    ///
    /// The entry is gone for good -- its campaign, channel, payload and
    /// preview cannot be reconstructed from a click -- so the least this can
    /// do is stop [#isRegistered] answering yes about it. In memory only, like
    /// every other entry in that set: after a restart the outbox is the only
    /// record, and the evicted entry is not in it. The ERROR logged by the
    /// caller is the durable half.
    ///
    /// - `entry`: the registration JSON that was dropped
    static void registrationEvicted(String entry) {
        if (entry == null) {
            return;
        }
        try {
            Map<String, Object> parsed =
                    new JSONParser().parseJSON(new java.io.StringReader(entry));
            Object code = parsed == null ? null : parsed.get("code");
            if (code != null) {
                unacknowledged.add(code.toString());
            }
        } catch (Throwable t) {
            // A malformed entry is already lost; failing here would take the
            // whole write with it, and the write is what keeps the REST of the
            // queue.
            Log.e(t);
        }
    }

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
        // Settled before the queue is touched, and reported as a failed
        // enqueue when it cannot be.
        //
        // An outbox that survived an erasure is deleted WHOLE by the retry
        // inside the next drain -- which create() itself triggers through
        // flush() -- so an entry appended to it goes with it. It had reported
        // success, so nothing held its code in `unacknowledged` and
        // isRegistered() answered true about a registration the server was
        // guaranteed never to have seen; its campaign, channel and preview
        // were gone for good.
        //
        // The caller's existing failure path is the right answer here: it
        // sends this one registration now if consent permits, and otherwise
        // remembers the code as unacknowledged. Neither touches the queue.
        if (!settleErasure()) {
            return false;
        }
        List<String> outbox = InviteStore.readOutbox();
        outbox.add(pendingRegistration);
        return InviteStore.writeOutbox(outbox);
    }

    private static void drainOutbox() {
        if (!allowed()) {
            return;
        }
        // An erasure could not delete the queue, and these entries carry the
        // OLD client id along with the campaign, payload and preview. Sending
        // them once storage recovers is exactly the transmission the erasure
        // was asked to prevent, so the erasure is retried and nothing is
        // drained until it succeeds.
        if (!settleErasure()) {
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
            // The body is rewritten, the KEY is not. The outbox still holds the
            // original string, and that is what has to be removed when the
            // server accepts it.
            postRegistration(withCurrentConsent(json), json);
        }
    }

    /// Rewrites a queued registration's consent flag to what consent says now.
    ///
    /// The body is serialized at mint time, and under the default opt-in mode
    /// an invite is very often minted BEFORE the prompt is answered -- so the
    /// stored JSON carries `consentAnalytics:false`. Draining is already gated
    /// on consent having been granted, but the field travels with the body and
    /// the analytics transport reads it as the proof that the gate was
    /// satisfied. Sent unchanged, a registration queued before the grant
    /// arrived looking unconsented and could be refused, and the link it
    /// describes would keep its code and lose its campaign, payload and
    /// preview for good.
    ///
    /// Rewritten rather than rebuilt: everything else in the entry -- the code
    /// and the metadata -- is what the invite was minted with and must not be
    /// re-derived from today's state.
    ///
    /// - `json`: the queued registration
    ///
    /// #### Returns
    ///
    /// the registration with a current consent flag, or the original when it
    /// cannot be parsed
    private static String withCurrentConsent(String json) {
        if (json == null) {
            return null;
        }
        try {
            Map<String, Object> body =
                    new JSONParser().parseJSON(new java.io.StringReader(json));
            if (body == null) {
                return json;
            }
            body.put("consentAnalytics", Boolean.valueOf(allowed()));
            return JSONParser.mapToJson(body);
        } catch (Throwable t) {
            // An entry that cannot be parsed is still worth sending as it is:
            // the alternative is dropping a registration whose metadata exists
            // nowhere else.
            Log.e(t);
            return json;
        }
    }

    private static void postRegistration(String json) {
        postRegistration(json, json);
    }

    /// Posts `body`, retiring `outboxKey` from the outbox when it lands.
    ///
    /// - `body`: the registration to transmit
    /// - `outboxKey`: the stored entry it stands for
    private static void postRegistration(String body, String outboxKey) {
        send(getLinkBase() + PATH_MINT, body, outboxKey, MATCH_DIRECT, false, true);
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
