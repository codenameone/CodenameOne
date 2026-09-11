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
import com.codename1.ui.Display;
import com.codename1.junit.EdtTest;
import com.codename1.junit.FormTest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import com.codename1.junit.UITestBase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What happens when the server, the storage or the process does not cooperate.
 * Every case here is a way the feature used to lose data quietly rather than
 * loudly, so each one asserts the durable record rather than a return value.
 */
class InviteResilienceTest extends UITestBase {

    @BeforeEach
    void setUp() {
        InviteTestSupport.freshInstall();
    }

    @AfterEach
    void tearDown() {
        InviteTestSupport.tearDown();
    }

    @Test
    @EdtTest
    void aServerErrorDoesNotRetireTheRegistration() {
        // ConnectionRequest reads error bodies by default and then runs the
        // ordinary success path over them, so a 503 reached postResponse()
        // exactly as a 200 did and the outbox entry -- the only durable copy of
        // the campaign, channel, payload and preview of a link already handed
        // out -- was discarded as though the server had accepted it.
        String entry = "{\"code\":\"abc123\",\"campaign\":\"launch\"}";
        InviteStore.writeOutbox(new ArrayList<String>(Arrays.asList(entry)));

        Invites.InviteConnection req = new Invites.InviteConnection(
                Invites.MATCH_DIRECT, false, true, entry, 0);
        req.handleErrorResponseCode(503, "Service Unavailable");
        req.postResponse();

        assertEquals(Arrays.asList(entry), InviteStore.readOutbox(),
                "a 503 retired the registration as though it had been accepted");
    }

    @Test
    @EdtTest
    void aServerErrorIsNotReadAsAnOrganicInstall() {
        // The same fall-through, on the lookup side: an error body parses to
        // nothing that says "resolved", which the resolution path treats as a
        // terminal "you were not invited" -- turning one bad minute on the
        // server into a permanent wrong answer on the device.
        Invites.checkForInvite();
        Invites.InviteConnection req = new Invites.InviteConnection(
                Invites.MATCH_DIRECT, true, false, null, Invites.currentLookupEpochForTest());
        req.handleErrorResponseCode(500, "Internal Server Error");
        // The error body really is read -- that is the whole mechanism -- so
        // the test has to supply one. An error page carries no "resolved", and
        // the resolution path reads that as a settled negative.
        try {
            req.readResponse(new ByteArrayInputStream(
                    "{\"error\":\"upstream unavailable\"}".getBytes("UTF-8")));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        req.postResponse();

        assertFalse(Invites.getState() == Invites.STATE_NONE_FOUND,
                "a server fault was recorded as a settled negative answer");
    }

    @Test
    @EdtTest
    void aNoMatchAnswerIsNotAskedAgainOnTheNextLaunch() {
        // Deleting the pending record was not enough: an absent record reads
        // back as STATE_NONE, so the next launch built a fresh profile and
        // queried again, and an ordinary uninvited install kept contacting the
        // server for ever.
        Invites.checkForInvite();
        Invites.handleResolution("{\"resolved\":false}", Invites.MATCH_APP_CLIP, true);
        assertEquals(Invites.STATE_NONE_FOUND, Invites.getState());

        Invites.forgetLoadedState();
        assertEquals(Invites.STATE_NONE_FOUND, Invites.getState(),
                "the terminal answer did not survive a relaunch");
    }

    @Test
    @EdtTest
    void aCodeTheServerHasNotSeenYetIsNotSettledAsOrganic() {
        // The offline-mint window. An invite minted with no network is handed
        // over before its registration reaches the server, so a claim can
        // arrive first -- and the server has never heard of the code. Read as
        // a final "no invite", that settled the install as organic
        // permanently, seconds before the code became claimable, which loses
        // exactly the attribution the offline mint exists to preserve.
        Invites.checkForInvite();
        Invites.handleResolution("{\"resolved\":false,\"retry\":true}",
                Invites.MATCH_APP_CLIP, true);
        assertEquals(Invites.STATE_PENDING, Invites.getState(),
                "a not-yet answer was treated as a final no");

        // And the real answer still lands when the registration catches up.
        Invites.handleResolution(InviteTestSupport.resolvedJson("LATE1", "spring", "sms"),
                Invites.MATCH_APP_CLIP, true);
        assertEquals(Invites.STATE_RESOLVED, Invites.getState());
        assertNotNull(Invites.getAttribution(), "the late answer was refused");
    }

    @FormTest
    void aNotYetAnswerIsAskedAgainInTheSameProcess() {
        // beginDeferred() runs at most once per process, so after a "not yet"
        // the documented call-me-from-start() contract did nothing for the rest
        // of the run: the request had already completed, no delayed retry
        // exists, and deferredStarted stayed set. An invite that became
        // claimable seconds later -- the whole point of the offline-mint
        // window -- waited for the next cold start, withholding its payload,
        // its callback and its dimensions through the entire onboarding.
        //
        // Driven through the referrer source, because that is the path that
        // sets deferredStarted: handleUrl() issues its claim directly and
        // leaves the flag alone, so a fixture built on it re-enters
        // beginDeferred() either way and cannot tell the two behaviours apart.
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Invites.lookupRetryDelay = 0L;
        Invites.registerInstallReferrerSource(new InstallReferrerSource() {
            public boolean isSupported() {
                return true;
            }

            public void requestReferrer(InstallReferrerCallback callback) {
                callback.onReferrer("utm_source=cn1_invite&cn1_invite=RETRY1", 0L, 0L);
            }
        });
        try {
            Invites.checkForInvite();
            assertTrue(implementation.getQueuedRequests().size() > 0,
                    "the fixture never issued a first claim, so it proves nothing");
            Invites.handleResolution("{\"resolved\":false,\"retry\":true}",
                    Invites.MATCH_REFERRER, true);
            assertEquals(Invites.STATE_PENDING, Invites.getState(),
                    "a not-yet answer was treated as a final no");
            implementation.clearQueuedRequests();

            // The next start, or the next form: the same call the application
            // already makes, and the only one it is told to make.
            Invites.checkForInvite();

            assertTrue(implementation.getQueuedRequests().size() > 0,
                    "a not-yet answer was never asked again in this process");
        } finally {
            Invites.lookupRetryDelay = 30000L;
        }
    }

    @Test
    @EdtTest
    void aPlainNoIsStillFinalEvenBesideTheRetryAnswer() {
        // The retry flag must not soften the ordinary case. Most installs are
        // not invited, and an uninvited one that keeps asking contacts the
        // server on every launch for ever.
        Invites.checkForInvite();
        Invites.handleResolution("{\"resolved\":false,\"retry\":false}",
                Invites.MATCH_APP_CLIP, true);
        assertEquals(Invites.STATE_NONE_FOUND, Invites.getState());
    }

    @Test
    @EdtTest
    void theTerminalMarkerKeepsNoDeviceProfile() {
        // It is durable and it is empty: the profile existed to be matched,
        // and there is nothing left to match it against.
        Invites.checkForInvite();
        Invites.handleResolution("{\"resolved\":false}", Invites.MATCH_APP_CLIP, true);
        Map<String, String> marker = InviteStore.read(InviteStore.PENDING);
        assertNotNull(marker, "the answer has to be durable");
        assertTrue(marker.containsKey("state"));
        // The state, the reason, and the two clock readings that enforce the
        // original window across a reopen -- and nothing that describes the
        // device. Asserting a field count instead would fail the next time the
        // marker legitimately carries one more, which is how this assertion
        // came to be arguing against a privacy improvement.
        for (String key : new String[] {"platform", "osVersion", "deviceModel",
                "screenWidth", "screenHeight", "locale", "code"}) {
            assertFalse(marker.containsKey(key), "the marker held " + key + ": " + marker);
        }
    }

    @Test
    @EdtTest
    void aPendingReattributionSurvivesAProcessRestart() {
        // With re-attribution on, a later invite writes a new claim while the
        // earlier attribution still stands. Answering STATE_RESOLVED from the
        // old attribution made beginDeferred() return, so a claim interrupted
        // by process death was never retried and last touch lost to first.
        Invites.handleResolution(InviteTestSupport.resolvedJson("first", "c1", "sms"),
                Invites.MATCH_DIRECT, false);
        assertEquals(Invites.STATE_RESOLVED, Invites.getState());

        Invites.setReattribution(true);
        Invites.handleUrl("https://cloud.codenameone.com/i/acme/second");
        Invites.forgetLoadedState();

        assertEquals(Invites.STATE_PENDING, Invites.getState(),
                "the pending re-attribution claim was lost behind the old attribution");
    }

    @Test
    @EdtTest
    void withoutReattributionAStalePendingRecordDoesNotReopenAnAttribution() {
        // The other half of the same rule, and the reason the check is scoped:
        // first touch stands, so a leftover pending record must never unsettle
        // an attribution that has already resolved.
        Invites.checkForInvite();
        Invites.handleResolution(InviteTestSupport.resolvedJson("first", "c1", "sms"),
                Invites.MATCH_DIRECT, false);
        Invites.forgetLoadedState();
        assertEquals(Invites.STATE_RESOLVED, Invites.getState());
    }

    @Test
    @EdtTest
    void aFailedOutboxWriteIsReportedRatherThanSwallowed() {
        // The caller has to be able to tell, because an entry that never
        // reached the outbox is a registration nothing can reconstruct.
        List<String> ok = new ArrayList<String>(Arrays.asList("{\"code\":\"a\"}"));
        assertTrue(InviteStore.writeOutbox(ok), "a healthy store must report success");
        assertEquals(ok, InviteStore.readOutbox());
    }

    @Test
    @EdtTest
    void aDisabledAttributionWindowIsAnsweredOnceAndSurvivesARelaunch() {
        // setState() only rewrites a record that already exists, and on a fresh
        // install none does -- so this answer lived only in memory and the
        // listener heard it again on every launch.
        Invites.setAttributionWindow(0);
        final int[] told = new int[1];
        Invites.setInviteListener(new InviteListener() {
            public void inviteReceived(InviteAttribution a) {
            }

            public void attributionUnavailable(String reason) {
                told[0]++;
            }
        });
        Invites.checkForInvite();
        assertEquals(1, told[0]);
        assertEquals(Invites.STATE_NONE_FOUND, Invites.getState());

        Invites.forgetLoadedState();
        Invites.checkForInvite();
        assertEquals(1, told[0], "the listener was told again after a relaunch");
    }

    @Test
    @EdtTest
    void reenablingTheWindowReopensThatOneTerminalMarker() {
        // The disabled-window marker is the only terminal answer that can stop
        // being true, so it is the only one that is reopened. An application
        // that ships a non-zero window later is asking for attribution again.
        Invites.setAttributionWindow(0);
        Invites.checkForInvite();
        assertEquals(Invites.STATE_NONE_FOUND, Invites.getState());

        Invites.setAttributionWindow(Invites.DEFAULT_ATTRIBUTION_WINDOW);
        Invites.forgetLoadedState();
        Invites.checkForInvite();
        assertEquals(Invites.STATE_PENDING, Invites.getState(),
                "a re-enabled window did not reopen the lookup");
        // And the window it was reopened with is a real one. The marker was
        // written while the kill switch was on, so it recorded
        // expiresAt = firstLaunch + 0 -- a window already over at the instant
        // it was created. Reopening kept it, the expiry check settled the
        // lookup again on the same pass, and shipping a non-zero window later
        // could never work. Asserted on the record rather than only through the
        // state, because whether the state assertion above catches it depends
        // on which other test in this class ran first.
        Map<String, String> reopened = InviteStore.read(InviteStore.PENDING);
        assertNotNull(reopened);
        assertTrue(InviteStore.getLong(reopened, "expiresAt", 0) > System.currentTimeMillis(),
                "the reopened lookup carries the kill switch's zero-length window");
    }

    @Test
    @EdtTest
    void aNoMatchDoesNotSettleTheInstallWhileAReferrerRetryIsOutstanding() {
        // The Play referrer failed transiently, so the source deliberately left
        // its once-only flag unset and a later launch can still read the exact
        // referrer. Settling the install as organic on the statistical
        // fallback's answer would throw that deterministic result away.
        Invites.checkForInvite();
        Map<String, String> pending = InviteStore.read(InviteStore.PENDING);
        assertNotNull(pending);
        pending.put("referrerRetry", "true");
        InviteStore.write(InviteStore.PENDING, pending);

        Invites.handleResolution("{\"resolved\":false}", Invites.MATCH_APP_CLIP, true);

        Invites.forgetLoadedState();
        assertEquals(Invites.STATE_PENDING, Invites.getState(),
                "a transient store outage settled the install as organic");
    }

    @Test
    @EdtTest
    void aDirectlySentRegistrationIsNotRegisteredUntilItIsAcknowledged() {
        // Absence from the outbox is not acknowledgement. When the store cannot
        // be written the registration is sent directly and never queued, so the
        // outbox says nothing about it -- and reading that silence as success
        // reported an in-flight, possibly failed, registration as acknowledged.
        Invite invite = Invites.create(InviteRequest.create().campaign("launch").build());
        assertNotNull(invite);
        // Exactly what a failed enqueue leaves behind: nothing in the durable
        // queue, and a request on the wire. The outbox is emptied to stand in
        // for the write that did not happen.
        InviteStore.writeOutbox(new ArrayList<String>());
        Invites.markSentDirectlyForTest(invite.getCode());
        assertFalse(Invites.isRegistered(invite),
                "an unacknowledged direct send reported itself as registered");
    }

    @Test
    @EdtTest
    void aDirectLinkSupersedesADeferredLookupAlreadyOnTheWire() {
        // Both requests used to be issued under the same epoch, so both answers
        // passed the guard and a statistical match arriving second overwrote
        // the exact one -- dimensions and durable record included.
        Invites.checkForInvite();
        int deferredEpoch = Invites.currentLookupEpochForTest();

        Invites.handleUrl("https://cloud.codenameone.com/i/acme/DIRECT1");
        Invites.handleResolution(InviteTestSupport.resolvedJson("DIRECT1", "c1", "sms"),
                Invites.MATCH_DIRECT, false);

        // The deferred answer arrives late, under the epoch it was issued in.
        Invites.handleResolution(InviteTestSupport.resolvedJson("GUESS", "c2", "unknown"),
                Invites.MATCH_APP_CLIP, true, deferredEpoch);

        InviteAttribution a = Invites.getAttribution();
        assertNotNull(a);
        assertEquals("DIRECT1", a.getCode(),
                "a late statistical match overwrote the exact direct attribution");
    }

    @Test
    @EdtTest
    void aPendingReferrerRetryTellsTheListenerNothing() {
        // attributionUnavailable() is the terminal callback and this outcome is
        // the opposite of terminal. It also sets deliveredThisRun, so a
        // referrer that succeeded moments later could no longer deliver
        // inviteReceived() at all.
        Invites.checkForInvite();
        Map<String, String> pending = InviteStore.read(InviteStore.PENDING);
        assertNotNull(pending);
        pending.put("referrerRetry", "true");
        InviteStore.write(InviteStore.PENDING, pending);

        final int[] told = new int[1];
        final int[] received = new int[1];
        Invites.setInviteListener(new InviteListener() {
            public void inviteReceived(InviteAttribution a) {
                received[0]++;
            }

            public void attributionUnavailable(String reason) {
                told[0]++;
            }
        });
        Invites.handleResolution("{\"resolved\":false}", Invites.MATCH_APP_CLIP, true);
        assertEquals(0, told[0], "a pending outcome used the terminal callback");

        // And the exact answer that arrives afterwards is still deliverable.
        Invites.handleResolution(InviteTestSupport.resolvedJson("LATE1", "c1", "sms"),
                Invites.MATCH_REFERRER, true);
        assertEquals(1, received[0], "the later exact referrer result was suppressed");
    }

    @Test
    @EdtTest
    void aDefinitiveReferrerAnswerClearsTheRetryMarker() {
        // An outage set the marker; a later successful read that carries no
        // invite is definitive and must clear it, or the following no-match
        // looks retryable for ever.
        Invites.checkForInvite();
        Map<String, String> pending = InviteStore.read(InviteStore.PENDING);
        pending.put("referrerRetry", "true");
        InviteStore.write(InviteStore.PENDING, pending);

        Invites.registerInstallReferrerSource(new InstallReferrerSource() {
            public boolean isSupported() {
                return true;
            }

            public void requestReferrer(InstallReferrerCallback callback) {
                callback.onReferrer("utm_source=organic", 0L, 0L);
            }
        });
        Invites.reset();
        Invites.checkForInvite();
        Invites.handleResolution("{\"resolved\":false}", Invites.MATCH_APP_CLIP, true);

        Invites.forgetLoadedState();
        assertEquals(Invites.STATE_NONE_FOUND, Invites.getState(),
                "a stale retry marker kept a definitive organic answer pending");
    }

    @Test
    @EdtTest
    void aRefusalIsDurableAndIsReopenedByALaterGrant() {
        // The refusal was in memory only, so the listener heard it again on
        // every launch; making it durable must not make it permanent, because
        // granting consent afterwards is a real answer too.
        Invites.checkForInvite();
        Analytics.setConsent(AnalyticsConsent.builder().analytics(false).build());
        assertEquals(Invites.STATE_DECLINED, Invites.getState());

        Invites.forgetLoadedState();
        assertEquals(Invites.STATE_DECLINED, Invites.getState(),
                "the refusal did not survive a relaunch");

        Analytics.setConsent(AnalyticsConsent.granted());
        Invites.forgetLoadedState();
        Invites.checkForInvite();
        assertEquals(Invites.STATE_PENDING, Invites.getState(),
                "granting consent afterwards did not reopen the lookup");
    }

    @Test
    @EdtTest
    void flushSupersedesWhateverTheLastAttemptLeftOutstanding() {
        // Retrying under the same epoch let a fingerprint answer from the
        // earlier attempt land after the retried referrer resolved exactly, and
        // overwrite it. Genuinely concurrent on an app with more than one
        // NetworkManager thread.
        Invites.checkForInvite();
        int stale = Invites.currentLookupEpochForTest();

        // The first attempt has aged out; flush() deliberately does nothing
        // while one is still outstanding, which is the sibling case below.
        Invites.lookupRetryDelay = 0L;
        Invites.flush();
        Invites.handleResolution(InviteTestSupport.resolvedJson("EXACT1", "c1", "sms"),
                Invites.MATCH_REFERRER, true);

        Invites.handleResolution(InviteTestSupport.resolvedJson("GUESS", "c2", "unknown"),
                Invites.MATCH_APP_CLIP, true, stale);

        InviteAttribution a = Invites.getAttribution();
        assertNotNull(a);
        assertEquals("EXACT1", a.getCode(),
                "a stale statistical answer overwrote the retried exact one");
    }

    @Test
    @EdtTest
    void grantingConsentResumesADeclinedLookupWithoutWaitingForTheApp() {
        // The refusal leaves STATE_DECLINED with a reopenable marker, and
        // nothing restarted the lookup until the application happened to call
        // checkForInvite() again -- by which time the attribution window may
        // have closed.
        Invites.checkForInvite();
        Analytics.setConsent(AnalyticsConsent.builder().analytics(false).build());
        assertEquals(Invites.STATE_DECLINED, Invites.getState());

        Analytics.setConsent(AnalyticsConsent.granted());

        assertEquals(Invites.STATE_PENDING, Invites.getState(),
                "granting consent did not resume the declined lookup");
    }

    @Test
    @EdtTest
    void anUnavailableAnswerReachedBeforeRegistrationIsStillDelivered() {
        // The answer is terminal, so no later lookup produces it again, and
        // setInviteListener only replays a resolved attribution -- so an app
        // that answered the deferred question before registering its listener
        // got neither callback for the entire install.
        Invites.setAttributionWindow(0);
        Invites.checkForInvite();
        assertEquals(Invites.STATE_NONE_FOUND, Invites.getState());

        final String[] told = new String[1];
        Invites.setInviteListener(new InviteListener() {
            public void inviteReceived(InviteAttribution a) {
            }

            public void attributionUnavailable(String reason) {
                told[0] = reason;
            }
        });
        assertEquals(Invites.REASON_UNSUPPORTED, told[0],
                "the answer reached before registration was dropped");
    }

    @Test
    @EdtTest
    void aRefusedDirectLinkTellsTheListener() {
        // checkForInvite marks the url consumed and skips the deferred path
        // after this, so it is the only chance the listener gets -- and a
        // registered one heard nothing at all.
        Analytics.setConsent(AnalyticsConsent.builder().analytics(false).build());
        final String[] told = new String[1];
        Invites.setInviteListener(new InviteListener() {
            public void inviteReceived(InviteAttribution a) {
            }

            public void attributionUnavailable(String reason) {
                told[0] = reason;
            }
        });
        Invites.handleUrl("https://cloud.codenameone.com/i/acme/CODE1");
        assertEquals(Invites.REASON_CONSENT_DENIED, told[0],
                "a refused direct link told the listener nothing");
    }

    @Test
    @EdtTest
    void aTransientReferrerFailureIsNotSettledAsOrganic() {
        // The Play service was busy, the bind did not take, or it dropped
        // before answering. None of those is an answer about this install, and
        // the source keeps its once-only flag unset precisely so a later
        // launch can read the exact referrer.
        //
        // On Android the retry marker was written and then ignored: there is
        // no App Clip to fall through to, so control reached the settle path
        // immediately and wrote a PERMANENT no-match over a referrer that was
        // readable the whole time.
        // Android, so there is no clip to fall through to -- which is the
        // whole point: the settle path is reached immediately instead of
        // parking on a handoff that would keep the lookup alive by itself.
        Invites.registerAppClipHandoffSource(null);
        Invites.registerInstallReferrerSource(new InstallReferrerSource() {
            public boolean isSupported() {
                return true;
            }

            public void requestReferrer(InstallReferrerCallback callback) {
                callback.onUnavailable(Invites.REASON_NO_MATCH);
            }
        });
        Invites.checkForInvite();

        assertEquals(Invites.STATE_PENDING, Invites.getState(),
                "a transient store failure was settled as a final no");
        assertFalse(Invites.getState() == Invites.STATE_NONE_FOUND);

        // And the exact answer still lands when the store recovers.
        Invites.registerInstallReferrerSource(new InstallReferrerSource() {
            public boolean isSupported() {
                return true;
            }

            public void requestReferrer(InstallReferrerCallback callback) {
                callback.onReferrer("utm_source=cn1_invite&cn1_invite=LATER1", 0L, 0L);
            }
        });
        Invites.flush();
        Map<String, String> pending = InviteStore.read(InviteStore.PENDING);
        assertEquals("LATER1", InviteStore.get(pending, "code", null),
                "the retried referrer was never read");
    }

    @Test
    @EdtTest
    void anExactReferrerAnswerOfNoInviteIsStillFinal() {
        // The referrer was READ and carries no invite: a real answer, and a
        // permanent one. The retry path must not swallow this case, or an
        // ordinary uninvited install asks again on every launch for ever.
        Invites.registerAppClipHandoffSource(null);
        Invites.registerInstallReferrerSource(new InstallReferrerSource() {
            public boolean isSupported() {
                return true;
            }

            public void requestReferrer(InstallReferrerCallback callback) {
                callback.onReferrer("utm_source=google-play&utm_medium=organic", 0L, 0L);
            }
        });
        Invites.checkForInvite();

        assertEquals(Invites.STATE_NONE_FOUND, Invites.getState(),
                "an exact 'no invite' answer was left pending");
    }

    @FormTest
    void thedeleteTombstoneIsNotMistakenForAPendingLookup() {
        // InviteStore.delete() overwrites a record it could not remove with an
        // empty one, on purpose: an empty record carries no code, no inviter
        // and no campaign, so a delete that cannot happen leaves nothing
        // behind. But an absent "state" key defaulted to STATE_PENDING, and
        // under re-attribution a pending state outranks the durable
        // attribution -- so the settled claim was resubmitted and the install
        // funnel counted one install twice.
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Invites.setReattribution(true);
        Invites.handleResolution(
                InviteTestSupport.resolvedJson("SETTLED1", "spring", "sms"),
                Invites.MATCH_REFERRER, true);
        assertNotNull(Invites.getAttribution(), "the fixture did not resolve");

        // The tombstone a failed delete leaves.
        InviteStore.write(InviteStore.PENDING, new java.util.LinkedHashMap<String, String>());
        Invites.forgetLoadedState();

        assertEquals(Invites.STATE_RESOLVED, Invites.getState(),
                "an empty deletion tombstone reopened a settled attribution");
    }

    @FormTest
    void asettledClaimWhosePendingRecordSurvivesIsNotAskedAgain() {
        // The store refuses to delete the record AND refuses the empty
        // overwrite delete() falls back to, so the real pending state lives on
        // beside the new attribution. Under re-attribution loadState() prefers
        // that record -- deliberately, so a claim interrupted by process death
        // is retried -- and the already-successful claim was resubmitted,
        // emitting a second invite_install for one install.
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Invites.setReattribution(true);
        Invites.handleUrl("https://cloud.codenameone.com/i/PENDSURV");

        InviteStore.failNextDeleteForTest(InviteStore.PENDING);
        Invites.handleResolution(
                InviteTestSupport.resolvedJson("PENDSURV", "spring", "sms"),
                Invites.MATCH_DIRECT, false);
        assertNotNull(Invites.getAttribution(), "the fixture did not resolve");

        Invites.forgetLoadedState();
        assertEquals(Invites.STATE_RESOLVED, Invites.getState(),
                "a settled claim was left pending and would be submitted again");
    }

    @FormTest
    void anerasureOwedSurvivesTheProcessThatCouldNotFinishIt() {
        // erasurePending is a static. A reset whose deletes failed and whose
        // process then exited left nothing to retry from -- and a plain reset
        // keeps the client id, so the provider sees no identity change on the
        // next launch and does not erase either. The surviving attribution
        // came back and was transmitted, which is the one thing reset()
        // promises will not happen.
        InviteTestSupport.freshInstall();
        implementation.setAutoProcessConnections(false);
        Invites.handleResolution(
                InviteTestSupport.resolvedJson("OWED1", "spring", "sms"),
                Invites.MATCH_REFERRER, true);
        assertNotNull(Invites.getAttribution(), "the fixture did not resolve");

        InviteStore.failNextDeleteForTest(InviteStore.ATTRIBUTION);
        Invites.reset();
        assertNotNull(InviteStore.read(InviteStore.ERASURE),
                "a failed reset left no durable trace of the erasure it owed");

        // The next process: nothing in memory remembers, and the store has
        // recovered.
        Invites.forgetErasurePendingForTest();
        Invites.forgetCachedAttributionForTest();
        Invites.checkForInvite();

        assertNull(Invites.getAttribution(),
                "the erasure was never finished and the attribution came back");
        assertNull(InviteStore.read(InviteStore.ERASURE),
                "the marker outlived the erasure it asked for");
    }

    @Test
    @EdtTest
    void theReferrerCodeIsPersistedBeforeTheClaimGoesOut() {
        // The source has already burned its once-only flag by the time the
        // callback runs, so a claim that fails leaves the exact code nowhere
        // but that callback and the next flush() falls back to a statistical
        // match for an answer that had been read exactly.
        Invites.registerInstallReferrerSource(new InstallReferrerSource() {
            public boolean isSupported() {
                return true;
            }

            public void requestReferrer(InstallReferrerCallback callback) {
                callback.onReferrer("utm_source=cn1_invite&cn1_invite=EXACT9", 0L, 0L);
            }
        });
        Invites.checkForInvite();

        Map<String, String> pending = InviteStore.read(InviteStore.PENDING);
        assertNotNull(pending, "the pending record was not kept at all");
        assertEquals("EXACT9", InviteStore.get(pending, "code", null),
                "the exact referrer code was not persisted before the claim");
    }

    @FormTest
    void aFailedOutboxWriteStillTransmitsNothingWithoutConsent() {
        // drainOutbox carries the consent guard and this fallback had none, so
        // a storage failure was the one way an undecided user's client id and
        // invite metadata reached the server.
        Analytics.setConsent(null);
        implementation.clearQueuedRequests();
        implementation.setAutoProcessConnections(false);
        InviteStore.failNextOutboxWriteForTest();

        Invite invite = Invites.create(InviteRequest.create().campaign("launch").build());
        assertNotNull(invite, "minting is offline and must still work");
        assertEquals(0, implementation.getQueuedRequests().size(),
                "a registration was transmitted before consent was given");
    }

    @FormTest
    void anInviteTheServerNeverSawIsNotReportedAsRegistered() {
        // isRegistered() reads absence from BOTH the outbox and the in-memory
        // unacknowledged set as acknowledgement. On this path neither holds the
        // code -- the outbox write is what failed, and consent forbade sending
        // -- so the one invite the server is guaranteed never to have seen was
        // the one reported as registered, and an application that waits for
        // isRegistered() before sharing would hand out a link with no campaign,
        // channel or preview behind it.
        Analytics.setConsent(null);
        implementation.clearQueuedRequests();
        implementation.setAutoProcessConnections(false);
        InviteStore.failNextOutboxWriteForTest();

        Invite invite = Invites.create(InviteRequest.create().campaign("launch").build());
        assertNotNull(invite, "minting is offline and must still work");
        assertEquals(0, implementation.getQueuedRequests().size(),
                "a registration was transmitted before consent was given");
        assertFalse(Invites.isRegistered(invite),
                "an invite that was neither queued nor sent reported itself registered");
    }

    @FormTest
    void anEvictedRegistrationIsNotReportedAsRegistered() {
        // The outbox is capped, and the cap drops the OLDEST entry. isRegistered()
        // reads absence from both the outbox and the unacknowledged set as
        // acknowledgement, and an evicted entry is in neither -- so the one
        // registration the server is guaranteed never to have received was the
        // one reported as registered, and only a log line said otherwise.
        //
        // Driven through the store rather than by minting 513 invites: the cap
        // is InviteStore's and this is what it does when it is reached.
        Analytics.setConsent(null);
        implementation.setAutoProcessConnections(false);
        Invite first = Invites.create(InviteRequest.create().campaign("evicted").build());
        assertNotNull(first);
        assertFalse(Invites.isRegistered(first),
                "the fixture is already acknowledged, so the assertion below proves nothing");

        List<String> stuffed = new ArrayList<String>(InviteStore.readOutbox());
        while (stuffed.size() <= InviteStore.MAX_OUTBOX) {
            stuffed.add("{\"code\":\"FILLER" + stuffed.size() + "\"}");
        }
        assertTrue(InviteStore.writeOutbox(stuffed), "the stuffed outbox could not be written");

        assertFalse(Invites.isRegistered(first),
                "an evicted registration reported itself as acknowledged");
    }

    @FormTest
    void aqueuedRegistrationIsSentWithTodaysConsentNotYesterdays() {
        // The body is serialized at mint time, and under the default opt-in
        // mode an invite is very often minted BEFORE the prompt is answered --
        // so the stored JSON carries consentAnalytics:false. Draining is gated
        // on consent having been granted, but the flag travels WITH the body
        // and the analytics transport reads it as the proof that the gate was
        // satisfied. Sent unchanged, a registration queued before the grant
        // arrived looking unconsented and could be refused, and the link it
        // describes would keep its code and lose its campaign, payload and
        // preview for good.
        Analytics.setConsent(null);
        implementation.clearQueuedRequests();
        implementation.setAutoProcessConnections(false);

        Invite invite = Invites.create(InviteRequest.create().campaign("launch").build());
        assertNotNull(invite);
        assertEquals(0, implementation.getQueuedRequests().size(),
                "the registration was transmitted before consent was given");

        Analytics.setConsent(AnalyticsConsent.granted());
        Invites.flush();

        String body = null;
        for (int i = 0; i < implementation.getQueuedRequests().size(); i++) {
            String candidate = implementation.getQueuedRequests().get(i).getRequestBody();
            if (candidate != null && candidate.indexOf(invite.getCode()) >= 0) {
                body = candidate;
            }
        }
        assertNotNull(body, "the queued registration was never drained");
        assertTrue(body.indexOf("\"consentAnalytics\":true") >= 0
                        || body.indexOf("\"consentAnalytics\": true") >= 0,
                "the registration went out with the consent it was minted under: " + body);
        assertTrue(body.indexOf("launch") >= 0,
                "rewriting the consent flag lost the metadata the outbox exists to keep");
    }

    @FormTest
    void arewrittenRegistrationStillRetiresItsOriginalOutboxEntry() {
        // The body is rewritten on the way out so its consent flag is current;
        // the entry sitting in the outbox is still the original. Passing the
        // rewritten string as the acknowledgement key made outbox.remove()
        // match nothing, so the registration was resent on every flush for ever
        // and isRegistered() never became true -- a fix for one silent failure
        // that introduced a louder one.
        Analytics.setConsent(null);
        implementation.clearQueuedRequests();
        implementation.setAutoProcessConnections(false);

        Invite invite = Invites.create(InviteRequest.create().campaign("launch").build());
        assertNotNull(invite);
        List<String> queued = InviteStore.readOutbox();
        assertEquals(1, queued.size(), "the registration was not queued");
        String stored = queued.get(0);
        assertTrue(stored.indexOf("false") >= 0,
                "the fixture was queued with consent already granted");

        Analytics.setConsent(AnalyticsConsent.granted());
        Invites.flush();

        // The server accepts it. The connection has to hand back the ORIGINAL
        // entry, or nothing is retired.
        Invites.InviteConnection req = null;
        for (int i = 0; i < implementation.getQueuedRequests().size(); i++) {
            ConnectionRequest r = implementation.getQueuedRequests().get(i);
            if (r instanceof Invites.InviteConnection
                    && r.getRequestBody() != null
                    && r.getRequestBody().indexOf(invite.getCode()) >= 0) {
                req = (Invites.InviteConnection) r;
            }
        }
        assertNotNull(req, "the queued registration was never sent");
        try {
            req.readResponse(new ByteArrayInputStream(
                    "{\"registered\":true}".getBytes("UTF-8")));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        req.postResponse();

        assertEquals(0, InviteStore.readOutbox().size(),
                "the acknowledged registration stayed in the outbox and will be resent for ever");
        assertTrue(Invites.isRegistered(invite),
                "an acknowledged registration never reports itself registered");
    }

    @FormTest
    void aFailedPendingWriteDoesNotLoseTheDirectCode() {
        // handleUrl() commits STATE_PENDING and issues the claim before it
        // knows the record reached the disk. When the write failed and the
        // claim failed too, the exact code existed nowhere: the retry read a
        // record with no code in it and fell back to the install referrer or
        // the fingerprint -- answering with a guess, or not at all, a question
        // the device had an exact answer to. The copy is held in memory until a
        // write succeeds, which the next read of the record retries.
        implementation.clearQueuedRequests();
        implementation.setAutoProcessConnections(false);
        // The record has to EXIST first, so the failing write is the one that
        // adds the code rather than the one that creates the record. That is
        // also the harder case: a stale record with no code in it sits on the
        // disk underneath the copy that never landed, and reading the disk
        // first found it and answered with a guess.
        Invites.checkForInvite();
        assertNull(InviteStore.get(InviteStore.read(InviteStore.PENDING), "code", null),
                "the fixture already has a code, so the assertion below proves nothing");
        InviteStore.failNextWriteForTest(InviteStore.PENDING);

        assertTrue(Invites.handleUrl("https://cloud.codenameone.com/i/acme/DIRECT7"),
                "the link was not recognised at all");
        assertEquals(Invites.STATE_PENDING, Invites.getState());

        // And the next read of the record still has the code, and persists it.
        Map<String, String> record = Invites.pendingRecordForTest();
        assertNotNull(record, "the failed write was never retried");
        assertEquals("DIRECT7", InviteStore.get(record, "code", null),
                "the exact code was lost, so the retry will guess instead");
        assertEquals(Invites.MATCH_DIRECT, InviteStore.get(record, "codeMatch", null),
                "the direct claim lost its provenance");
    }

    @Test
    @EdtTest
    void flushDoesNotSpendAnAttemptOnALookupThatIsStillOutstanding() {
        // create() calls flush() unconditionally, so minting five invites in a
        // row exhausted MAX_ATTEMPTS without a single observed failure -- and
        // the last one settled the install as terminal while its own answer was
        // still on the wire.
        Invites.checkForInvite();
        Map<String, String> after = InviteStore.read(InviteStore.PENDING);
        int attempts = InviteStore.getInt(after, "attempts", 0);

        for (int i = 0; i < 8; i++) {
            Invites.flush();
        }

        Map<String, String> now = InviteStore.read(InviteStore.PENDING);
        assertEquals(attempts, InviteStore.getInt(now, "attempts", 0),
                "flush() spent the attempt budget on a lookup that had not failed");
        assertEquals(Invites.STATE_PENDING, Invites.getState(),
                "the install was settled while its answer was still on the wire");
    }

    @Test
    @EdtTest
    void aReferrerCallbackThatArrivesAfterADirectLinkIsIgnored() {
        // The platform callback used to read lookupEpoch at callback time, so
        // an outstanding referrer read inherited the epoch a direct link had
        // just advanced, passed the guard, and could overwrite the direct
        // attribution. Incrementing an epoch cannot invalidate a callback that
        // does not remember which epoch it belongs to.
        final InstallReferrerCallback[] held = new InstallReferrerCallback[1];
        Invites.registerInstallReferrerSource(new InstallReferrerSource() {
            public boolean isSupported() {
                return true;
            }

            public void requestReferrer(InstallReferrerCallback callback) {
                held[0] = callback;
            }
        });
        Invites.checkForInvite();
        assertNotNull(held[0], "the referrer read was never issued");

        Invites.handleUrl("https://cloud.codenameone.com/i/acme/DIRECT2");
        Invites.handleResolution(InviteTestSupport.resolvedJson("DIRECT2", "c1", "sms"),
                Invites.MATCH_DIRECT, false);

        // The referrer finally answers, carrying a different code. It must be
        // dropped where it arrives -- before it writes its code into the
        // pending record and issues a claim -- because once a claim goes out
        // under the current epoch nothing downstream can tell it apart from a
        // legitimate one.
        held[0].onReferrer("utm_source=cn1_invite&cn1_invite=LATE2", 0L, 0L);

        Map<String, String> pending = InviteStore.read(InviteStore.PENDING);
        String recorded = pending == null ? null : InviteStore.get(pending, "code", null);
        assertNotEquals("LATE2", recorded,
                "a stale referrer callback wrote its code and issued a claim");
        InviteAttribution a = Invites.getAttribution();
        assertNotNull(a);
        assertEquals("DIRECT2", a.getCode());
    }

    @Test
    @EdtTest
    void aRefusalHeldForALateListenerIsDiscardedWhenTheLookupResumes() {
        // The refusal was recorded for a listener that had not registered yet.
        // Once consent is granted it is not the answer any more, and leaving it
        // held reported a lookup that went on to resolve as unavailable.
        Invites.checkForInvite();
        Analytics.setConsent(AnalyticsConsent.builder().analytics(false).build());
        Analytics.setConsent(AnalyticsConsent.granted());
        Invites.handleResolution(InviteTestSupport.resolvedJson("RESOLVED1", "c1", "sms"),
                Invites.MATCH_APP_CLIP, true);

        final String[] unavailable = new String[1];
        final InviteAttribution[] received = new InviteAttribution[1];
        Invites.setInviteListener(new InviteListener() {
            public void inviteReceived(InviteAttribution a) {
                received[0] = a;
            }

            public void attributionUnavailable(String reason) {
                unavailable[0] = reason;
            }
        });
        assertNull(unavailable[0], "a stale refusal was reported over a resolved attribution");
        assertNotNull(received[0], "the resolved attribution was never delivered");
    }

    @Test
    @EdtTest
    void anUnavailableAnswerSurvivesTheProcessThatReachedIt() {
        // The contract is "exactly one of the two methods per install, and the
        // answer is remembered". A resolved attribution has carried a durable
        // delivered flag from the start; the unavailable answer had nothing, so
        // an application whose deferred question was settled before it
        // registered a listener, in a process that then exited, got neither
        // callback for the life of the install.
        Invites.setAttributionWindow(0);
        Invites.checkForInvite();
        assertEquals(Invites.STATE_NONE_FOUND, Invites.getState());

        Invites.forgetLoadedState();

        final String[] told = new String[1];
        Invites.setInviteListener(new InviteListener() {
            public void inviteReceived(InviteAttribution a) {
            }

            public void attributionUnavailable(String reason) {
                told[0] = reason;
            }
        });
        assertEquals(Invites.REASON_UNSUPPORTED, told[0],
                "the answer did not survive the process that reached it");
    }

    @Test
    @EdtTest
    void anAnswerAlreadyDeliveredIsNotDeliveredAgainOnALaterLaunch() {
        // The other half of the same contract: exactly one, not one per launch.
        Invites.setAttributionWindow(0);
        Invites.checkForInvite();
        final int[] told = new int[1];
        InviteListener l = new InviteListener() {
            public void inviteReceived(InviteAttribution a) {
            }

            public void attributionUnavailable(String reason) {
                told[0]++;
            }
        };
        Invites.setInviteListener(l);
        assertEquals(1, told[0]);

        Invites.forgetLoadedState();
        Invites.setInviteListener(l);
        assertEquals(1, told[0], "the answer was delivered twice across launches");
    }

    @Test
    @EdtTest
    void clearingAnExplicitDenialUnderOptOutResumesAttribution() {
        // Under OPT_OUT a null recorded choice is the mode's implicit allow, not
        // an unanswered prompt. Ignoring it resumed ordinary analytics while a
        // declined invite lookup stayed stopped, so the two disagreed about the
        // same user.
        Analytics.setConsentMode(ConsentMode.OPT_OUT);
        Invites.checkForInvite();
        Analytics.setConsent(AnalyticsConsent.builder().analytics(false).build());
        assertEquals(Invites.STATE_DECLINED, Invites.getState());

        Analytics.setConsent(null);

        assertEquals(Invites.STATE_PENDING, Invites.getState(),
                "clearing the denial under opt-out did not resume the lookup");
    }

    @Test
    @EdtTest
    void aReattributionThatFindsNothingLeavesTheEarlierAnswerStanding() {
        // The earlier attribution is still the answer for this install, so a
        // failed replacement is not terminal. Terminalizing it contradicted the
        // durable record -- which still says RESOLVED and puts the state back on
        // the next launch -- and told the listener "no invite" as a second,
        // opposite callback after it had already been given one.
        Invites.handleResolution(InviteTestSupport.resolvedJson("FIRST1", "c1", "sms"),
                Invites.MATCH_DIRECT, false);
        Invites.setReattribution(true);

        final int[] told = new int[1];
        Invites.setInviteListener(new InviteListener() {
            public void inviteReceived(InviteAttribution a) {
            }

            public void attributionUnavailable(String reason) {
                told[0]++;
            }
        });
        Invites.handleResolution("{\"resolved\":false}", Invites.MATCH_DIRECT, false);

        assertEquals(Invites.STATE_RESOLVED, Invites.getState(),
                "a failed re-attribution terminalized an attributed install");
        assertEquals(0, told[0], "the listener was told the opposite of what it had heard");
        assertNotNull(Invites.getAttribution());
    }

    @Test
    @EdtTest
    void areplacementAttributionIsNotDeliveredASecondTime() {
        // Re-attribution rewrites the attribution but not the fact that the
        // listener has already been told about this install, and the contract is
        // exactly one callback per install.
        final int[] received = new int[1];
        Invites.setInviteListener(new InviteListener() {
            public void inviteReceived(InviteAttribution a) {
                received[0]++;
            }

            public void attributionUnavailable(String reason) {
            }
        });
        Invites.handleResolution(InviteTestSupport.resolvedJson("FIRST2", "c1", "sms"),
                Invites.MATCH_DIRECT, false);
        assertEquals(1, received[0]);

        Invites.setReattribution(true);
        Invites.handleResolution(InviteTestSupport.resolvedJson("SECOND2", "c2", "email"),
                Invites.MATCH_DIRECT, false);
        Invites.forgetLoadedState();
        Invites.setInviteListener(null);
        Invites.setInviteListener(new InviteListener() {
            public void inviteReceived(InviteAttribution a) {
                received[0]++;
            }

            public void attributionUnavailable(String reason) {
            }
        });

        assertEquals(1, received[0], "the replacement was delivered as a second callback");
    }

    @Test
    @EdtTest
    void anExpiredWindowReportsExpiryToALateListener() {
        // The expiry marker carried no reason, so after the process that
        // reached it exited, a late listener was told REASON_NO_MATCH -- the
        // marker's default -- instead of what actually happened.
        Invites.checkForInvite();
        Map<String, String> pending = InviteStore.read(InviteStore.PENDING);
        assertNotNull(pending);
        pending.put("expiresAt", String.valueOf(System.currentTimeMillis() - 1000L));
        InviteStore.write(InviteStore.PENDING, pending);

        Invites.forgetLoadedState();
        Invites.checkForInvite();
        assertEquals(Invites.STATE_NONE_FOUND, Invites.getState());

        Invites.forgetLoadedState();
        final String[] told = new String[1];
        Invites.setInviteListener(new InviteListener() {
            public void inviteReceived(InviteAttribution a) {
            }

            public void attributionUnavailable(String reason) {
                told[0] = reason;
            }
        });
        assertEquals(Invites.REASON_EXPIRED, told[0],
                "the late listener was told the wrong reason");
    }

    @Test
    @EdtTest
    void aReferrerReadCountsAsALookupInFlight() {
        // Only claim() and requestMatch() said so, so a flush() during the read
        // -- create() issues one unconditionally -- treated it as stale,
        // advanced the epoch, and the epoch guard then discarded the exact
        // answer when it arrived. Worse than a lost retry: the source has
        // already burned its once-only flag, so the deterministic result is
        // gone and a statistical guess replaces it.
        final InstallReferrerCallback[] held = new InstallReferrerCallback[1];
        Invites.registerInstallReferrerSource(new InstallReferrerSource() {
            public boolean isSupported() {
                return true;
            }

            public void requestReferrer(InstallReferrerCallback callback) {
                held[0] = callback;
            }
        });
        Invites.checkForInvite();
        assertNotNull(held[0]);
        int issued = Invites.currentLookupEpochForTest();

        Invites.flush();
        assertEquals(issued, Invites.currentLookupEpochForTest(),
                "flush() superseded a referrer read that was still outstanding");

        held[0].onReferrer("utm_source=cn1_invite&cn1_invite=KEPT1", 0L, 0L);
        Map<String, String> pending = InviteStore.read(InviteStore.PENDING);
        assertEquals("KEPT1", InviteStore.get(pending, "code", null),
                "the exact referrer answer was discarded");
    }

    @Test
    @EdtTest
    void aFailedReplacementPutsTheInstallBackWhereItWas() {
        // handleUrl writes a PENDING record for the replacement before issuing
        // the claim, so simply returning left the install pending: every later
        // flush and launch retried the failed replacement until the attempt cap
        // reported unavailable, with the durable attribution sitting beside it
        // the whole time.
        Invites.handleResolution(InviteTestSupport.resolvedJson("FIRST3", "c1", "sms"),
                Invites.MATCH_DIRECT, false);
        Invites.setReattribution(true);
        Invites.handleUrl("https://cloud.codenameone.com/i/acme/SECOND3");
        assertEquals(Invites.STATE_PENDING, Invites.getState());

        Invites.handleResolution("{\"resolved\":false}", Invites.MATCH_DIRECT, false);

        assertEquals(Invites.STATE_RESOLVED, Invites.getState());
        assertNull(InviteStore.read(InviteStore.PENDING),
                "the failed replacement's pending record was left behind");
        Invites.forgetLoadedState();
        assertEquals(Invites.STATE_RESOLVED, Invites.getState(),
                "the install came back pending on the next launch");
    }

    @Test
    @EdtTest
    void aFailedReplacementWhosePendingRecordSurvivesIsNotAskedAgain() {
        // The same abandonment, with a store that refuses to delete the record
        // AND refuses the empty overwrite delete() falls back to. Memory moved
        // on to RESOLVED and the disk still said PENDING -- which loadState()
        // prefers under re-attribution -- so a claim that had already ended
        // definitively was resubmitted on every launch, for ever, with the
        // public state reading pending throughout.
        Invites.handleResolution(InviteTestSupport.resolvedJson("FIRST4", "c1", "sms"),
                Invites.MATCH_DIRECT, false);
        Invites.setReattribution(true);
        Invites.handleUrl("https://cloud.codenameone.com/i/acme/SECOND4");
        assertEquals(Invites.STATE_PENDING, Invites.getState());

        InviteStore.failNextDeleteForTest(InviteStore.PENDING);
        Invites.handleResolution("{\"resolved\":false}", Invites.MATCH_DIRECT, false);

        Invites.forgetLoadedState();
        assertEquals(Invites.STATE_RESOLVED, Invites.getState(),
                "an abandoned replacement survived on disk and reopened the lookup");
        assertNotNull(Invites.getAttribution(),
                "the install lost the attribution it already had");
    }

    @Test
    @EdtTest
    void aResumedLookupDoesNotAnnounceItselfToAListenerAlreadyTold() {
        // The refusal was delivered, so the listener has had its one callback
        // for this install. Reopening deleted the marker that recorded that,
        // and the resumed lookup's attribution was written as undelivered --
        // arriving as a second callback on the next launch.
        final int[] told = new int[1];
        final int[] received = new int[1];
        InviteListener l = new InviteListener() {
            public void inviteReceived(InviteAttribution a) {
                received[0]++;
            }

            public void attributionUnavailable(String reason) {
                told[0]++;
            }
        };
        Invites.setInviteListener(l);
        Invites.checkForInvite();
        Analytics.setConsent(AnalyticsConsent.builder().analytics(false).build());
        assertEquals(1, told[0], "the refusal was not delivered, so this proves nothing");

        Analytics.setConsent(AnalyticsConsent.granted());
        Invites.handleResolution(InviteTestSupport.resolvedJson("LATER3", "c1", "sms"),
                Invites.MATCH_APP_CLIP, true);

        Invites.forgetLoadedState();
        Invites.setInviteListener(null);
        Invites.setInviteListener(l);
        assertEquals(0, received[0],
                "the resumed lookup announced itself to a listener already told");
    }

    @FormTest
    void aRetriedReferrerClaimIsStillAReferrerClaim() {
        // The persisted code was resent as a direct link, so the answer came
        // back with isDeferred() false and was recorded as invite_opened rather
        // than invite_install -- corrupting the install funnel for exactly the
        // deterministic answers this retry exists to save.
        Invites.registerInstallReferrerSource(new InstallReferrerSource() {
            public boolean isSupported() {
                return true;
            }

            public void requestReferrer(InstallReferrerCallback callback) {
                callback.onReferrer("utm_source=cn1_invite&cn1_invite=PROV1", 0L, 0L);
            }
        });
        Invites.checkForInvite();

        // The retry itself, on the wire: what the record holds only matters if
        // the resend uses it.
        implementation.clearQueuedRequests();
        implementation.setAutoProcessConnections(false);
        Invites.lookupRetryDelay = 0L;
        Invites.flush();

        String body = null;
        for (ConnectionRequest r : implementation.getQueuedRequests()) {
            if (r.getUrl() != null && r.getUrl().indexOf("/claim") >= 0) {
                body = r.getRequestBody();
            }
        }
        assertNotNull(body, "the persisted referrer code was never resent");
        assertTrue(body.contains("PROV1"), body);
        assertTrue(body.replace(" ", "").contains("\"source\":\"install_referrer\""),
                "a referrer answer was resent as a direct link: " + body);
    }

    @Test
    @EdtTest
    void anExhaustedReplacementLeavesTheEarlierAnswerStanding() {
        // Every way of giving up on a replacement has to abandon it, not just
        // the server no-match: the attempt cap wrote a terminal marker the
        // durable attribution contradicts, and told the listener "no invite"
        // after it had already been given one.
        Invites.handleResolution(InviteTestSupport.resolvedJson("FIRST4", "c1", "sms"),
                Invites.MATCH_DIRECT, false);
        Invites.setReattribution(true);
        Invites.handleUrl("https://cloud.codenameone.com/i/acme/SECOND4");

        Map<String, String> pending = InviteStore.read(InviteStore.PENDING);
        pending.put("attempts", "99");
        InviteStore.write(InviteStore.PENDING, pending);

        final int[] told = new int[1];
        Invites.setInviteListener(new InviteListener() {
            public void inviteReceived(InviteAttribution a) {
            }

            public void attributionUnavailable(String reason) {
                told[0]++;
            }
        });
        Invites.forgetLoadedState();
        Invites.checkForInvite();

        assertEquals(0, told[0], "an exhausted replacement told the listener the opposite");
        assertEquals(Invites.STATE_RESOLVED, Invites.getState());
    }

    @Test
    @EdtTest
    void aDeniedLinkDoesNotOverwriteAnAttributionAlreadyGiven() {
        // Writing a fresh DECLINED marker contradicted the durable attribution,
        // which is still there and makes the state RESOLVED again on the next
        // launch, and delivered a second, opposite callback for one install.
        final int[] delivered = new int[1];
        Invites.setInviteListener(new InviteListener() {
            public void inviteReceived(InviteAttribution a) {
                delivered[0]++;
            }

            public void attributionUnavailable(String reason) {
            }
        });
        Invites.handleResolution(InviteTestSupport.resolvedJson("FIRST5", "c1", "sms"),
                Invites.MATCH_DIRECT, false);
        assertEquals(1, delivered[0], "the attribution was not delivered, so this proves nothing");

        // A later process: the callback has been given, and only the durable
        // records remain.
        Invites.forgetLoadedState();
        Analytics.setConsent(AnalyticsConsent.builder().analytics(false).build());

        final int[] told = new int[1];
        Invites.setInviteListener(new InviteListener() {
            public void inviteReceived(InviteAttribution a) {
            }

            public void attributionUnavailable(String reason) {
                told[0]++;
            }
        });
        Invites.handleUrl("https://cloud.codenameone.com/i/acme/SECOND5");

        assertEquals(0, told[0], "a denied link told an attributed install it had no invite");
        Invites.forgetLoadedState();
        assertEquals(Invites.STATE_RESOLVED, Invites.getState(),
                "the state contradicted the durable attribution");
    }

    @Test
    @EdtTest
    void anEmptyButSuccessfulReferrerReadIsDefinitive() {
        // The source burns its once-only flag for this case, so isSupported()
        // can never read a referrer again -- but the reason it reports is the
        // same one a transient failure uses, so the lookup stayed pending until
        // the attempt budget ran out for an answer that had already arrived.
        Invites.registerInstallReferrerSource(new InstallReferrerSource() {
            private boolean spent;

            public boolean isSupported() {
                return !spent;
            }

            public void requestReferrer(InstallReferrerCallback callback) {
                spent = true;
                callback.onUnavailable(Invites.REASON_NO_MATCH);
            }
        });
        Invites.checkForInvite();
        Invites.handleResolution("{\"resolved\":false}", Invites.MATCH_APP_CLIP, true);

        Invites.forgetLoadedState();
        assertEquals(Invites.STATE_NONE_FOUND, Invites.getState(),
                "a definitive empty referrer read was treated as retryable");
    }

    @Test
    @EdtTest
    void aDirectLinkGetsItsOwnWindowAndBudget() {
        // Inheriting them from an older deferred lookup meant a link opened
        // after that lookup had expired, or after its retries were spent, was
        // marked expired by beginDeferred() before the saved code was ever
        // looked at -- so an exact answer we were holding was never sent.
        Invites.checkForInvite();
        Map<String, String> stale = InviteStore.read(InviteStore.PENDING);
        assertNotNull(stale);
        stale.put("expiresAt", String.valueOf(System.currentTimeMillis() - 1000L));
        stale.put("attempts", String.valueOf(99));
        InviteStore.write(InviteStore.PENDING, stale);

        Invites.handleUrl("https://cloud.codenameone.com/i/acme/FRESH1");

        Map<String, String> pending = InviteStore.read(InviteStore.PENDING);
        assertEquals("FRESH1", InviteStore.get(pending, "code", null));
        assertTrue(InviteStore.getLong(pending, "expiresAt", 0) > System.currentTimeMillis(),
                "the direct claim inherited an expired window");
        // One, not zero: the reset puts it back to zero and the claim this
        // call issues counts as the first attempt against the new budget.
        assertEquals(1, InviteStore.getInt(pending, "attempts", -1),
                "the direct claim inherited a spent retry budget");
    }

    @Test
    @EdtTest
    void reopeningAfterConsentKeepsTheOriginalWindow() {
        // Without the original timings a reopened marker started the window
        // again from the moment consent was granted, so a user answering the
        // prompt a week later ran a fresh fingerprint lookup and could report
        // invite_install for somebody else's click.
        Invites.checkForInvite();
        Map<String, String> first = InviteStore.read(InviteStore.PENDING);
        assertNotNull(first);
        // A distinctive value rather than whatever the clock produced a
        // millisecond ago: a fresh window computed at grant time would land on
        // almost the same number, and the test would pass by coincidence.
        long originalExpiry = System.currentTimeMillis() + 123_456_789L;
        first.put("expiresAt", String.valueOf(originalExpiry));
        InviteStore.write(InviteStore.PENDING, first);

        Analytics.setConsent(AnalyticsConsent.builder().analytics(false).build());
        Analytics.setConsent(AnalyticsConsent.granted());

        Map<String, String> resumed = InviteStore.read(InviteStore.PENDING);
        assertNotNull(resumed);
        assertEquals(originalExpiry, InviteStore.getLong(resumed, "expiresAt", 0),
                "granting consent restarted the attribution window");
    }

    @Test
    @EdtTest
    void theZeroWindowDoesNotDiscardAnExactCodeWeAreHolding() {
        // setAttributionWindow(0) turns off the DEFERRED lookup, which is the
        // one that needs a window to mean anything. A code already in hand is
        // an exact answer that needs none, and refusing to send it reported
        // "unsupported" for an invite the user really did open.
        Invites.handleUrl("https://cloud.codenameone.com/i/acme/EXACT9");
        assertEquals("EXACT9", InviteStore.get(
                InviteStore.read(InviteStore.PENDING), "code", null));

        Invites.setAttributionWindow(0);
        Invites.forgetLoadedState();
        final String[] told = new String[1];
        Invites.setInviteListener(new InviteListener() {
            public void inviteReceived(InviteAttribution a) {
            }

            public void attributionUnavailable(String reason) {
                told[0] = reason;
            }
        });
        Invites.checkForInvite();

        assertNull(told[0], "the kill switch discarded an exact code we were holding");
        assertEquals(Invites.STATE_PENDING, Invites.getState());
    }

    @Test
    @EdtTest
    void thekillSwitchStillLetsAnExactAnswerLand() {
        // The switch turns off the STATISTICAL lookup, not an exact code the
        // device is holding -- hasSavedCode() exempts one where the lookup
        // begins, and refusing a direct claim on the way back in would break
        // the same exemption from the other end. This is why the guard reads
        // the deferred flag rather than bumping the epoch, which is global.
        Invites.handleUrl("https://cloud.codenameone.com/i/acme/EXACT7");
        int inFlight = Invites.currentLookupEpochForTest();

        Invites.setAttributionWindow(0);

        Invites.handleResolution(InviteTestSupport.resolvedJson("EXACT7", "c1", "sms"),
                Invites.MATCH_DIRECT, false, inFlight);

        InviteAttribution a = Invites.getAttribution();
        assertNotNull(a, "the kill switch discarded an exact answer we had asked for");
        assertEquals("EXACT7", a.getCode());
    }

    @Test
    @EdtTest
    void thekillSwitchStillLetsAnInstallReferrerClaimLand() {
        // An install-referrer claim is exact AND deferred: the code came back
        // through the store, which is the whole reason the Android path is the
        // deterministic one. Keying the guard on the deferred flag therefore
        // dropped the best answer the device will ever have -- the same
        // saved-code exemption the lookup start honours, broken from the
        // returning end.
        Invites.checkForInvite();
        int inFlight = Invites.currentLookupEpochForTest();

        Invites.setAttributionWindow(0);

        Invites.handleResolution(InviteTestSupport.resolvedJson("REF9", "c1", "sms"),
                Invites.MATCH_REFERRER, true, inFlight);

        InviteAttribution a = Invites.getAttribution();
        assertNotNull(a, "the kill switch discarded an exact install-referrer claim");
        assertEquals("REF9", a.getCode());
    }

    @Test
    @EdtTest
    void turningOnReattributionLetsTheStateBeReadAgain() {
        // loadState() reads the pending record only when re-attribution is on,
        // so a process that cached STATE_RESOLVED before the setter ran would
        // never look at a durable replacement again -- and setInviteListener,
        // which most applications call first, is enough to cache it.
        Invites.handleResolution(InviteTestSupport.resolvedJson("FIRST6", "c1", "sms"),
                Invites.MATCH_DIRECT, false);
        Invites.setReattribution(true);
        Invites.handleUrl("https://cloud.codenameone.com/i/acme/SECOND6");
        Invites.setReattribution(false);

        // A later process: the listener is registered first, caching the state
        // under the default, and only then is re-attribution turned on.
        Invites.forgetLoadedState();
        Invites.setInviteListener(null);
        assertEquals(Invites.STATE_RESOLVED, Invites.getState());
        Invites.setReattribution(true);

        assertEquals(Invites.STATE_PENDING, Invites.getState(),
                "the cached state hid the durable replacement");
    }

    @Test
    @EdtTest
    void turningReattributionOffDiscardsAreplacementAlreadyInFlight() {
        // Changing the setting only changed how the state is READ. An
        // outstanding replacement response still passed handleResolution()'s
        // epoch guard and overwrote the first-touch attribution the setting had
        // just said to keep -- so an application that turned last touch off
        // could still have a user's cohort change underneath its reports, once,
        // by a request that was already on the wire.
        Invites.handleResolution(InviteTestSupport.resolvedJson("FIRST8", "c1", "sms"),
                Invites.MATCH_DIRECT, false);
        Invites.setReattribution(true);
        Invites.handleUrl("https://cloud.codenameone.com/i/acme/SECOND8");
        int inFlight = Invites.currentLookupEpochForTest();

        Invites.setReattribution(false);

        // The response that was already on the wire lands now.
        Invites.handleResolution(InviteTestSupport.resolvedJson("SECOND8", "c1", "sms"),
                Invites.MATCH_DIRECT, false, inFlight);

        InviteAttribution a = Invites.getAttribution();
        assertNotNull(a);
        assertEquals("FIRST8", a.getCode(),
                "an in-flight replacement overwrote first touch after last touch was turned off");
    }

    @Test
    @EdtTest
    void aResumedLookupThatEndsTerminallyIsNotAnnouncedTwice() {
        // The reopen carries the delivery state onto the pending record, and
        // the terminal rewrite dropped it -- so a listener registered in the
        // next process was told a second time.
        final int[] told = new int[1];
        InviteListener l = new InviteListener() {
            public void inviteReceived(InviteAttribution a) {
            }

            public void attributionUnavailable(String reason) {
                told[0]++;
            }
        };
        Invites.setInviteListener(l);
        Invites.checkForInvite();
        Analytics.setConsent(AnalyticsConsent.builder().analytics(false).build());
        assertEquals(1, told[0], "the refusal was not delivered, so this proves nothing");

        Analytics.setConsent(AnalyticsConsent.granted());
        Invites.handleResolution("{\"resolved\":false}", Invites.MATCH_APP_CLIP, true);

        Invites.forgetLoadedState();
        Invites.setInviteListener(null);
        Invites.setInviteListener(l);
        assertEquals(1, told[0], "the resumed lookup announced its end a second time");
    }

    @Test
    @EdtTest
    void aDirectLinkDiscardsAHeldAnswerThatIsNoLongerTrue() {
        // A no-match that became terminal with no listener is remembered, and
        // leaving it there handed a listener registered after this link
        // resolved the stale unavailable result -- with deliveredThisRun then
        // suppressing the correct one.
        Invites.checkForInvite();
        Invites.handleResolution("{\"resolved\":false}", Invites.MATCH_APP_CLIP, true);
        assertEquals(Invites.STATE_NONE_FOUND, Invites.getState());

        Invites.handleUrl("https://cloud.codenameone.com/i/acme/LATER6");
        Invites.handleResolution(InviteTestSupport.resolvedJson("LATER6", "c1", "sms"),
                Invites.MATCH_DIRECT, false);

        final String[] unavailable = new String[1];
        final InviteAttribution[] received = new InviteAttribution[1];
        Invites.setInviteListener(new InviteListener() {
            public void inviteReceived(InviteAttribution a) {
                received[0] = a;
            }

            public void attributionUnavailable(String reason) {
                unavailable[0] = reason;
            }
        });
        assertNull(unavailable[0], "a stale held answer was reported over a resolved one");
        assertNotNull(received[0], "the resolved attribution was suppressed by it");
    }

    @Test
    @EdtTest
    void aSavedExactCodeIsNotSubjectToTheDeferredWindow() {
        // The window bounds the deferred lookup, and a code we are holding is
        // an exact answer rather than one. Applying the expiry to it lost that
        // answer whenever the two coexist -- a zero window, where handleUrl
        // records an expiry of "now", or a first claim that failed and is
        // retried after the window ran out.
        Invites.handleUrl("https://cloud.codenameone.com/i/acme/SAVED9");
        Map<String, String> pending = InviteStore.read(InviteStore.PENDING);
        assertNotNull(pending);
        pending.put("expiresAt", String.valueOf(System.currentTimeMillis() - 1000L));
        InviteStore.write(InviteStore.PENDING, pending);

        final String[] told = new String[1];
        Invites.setInviteListener(new InviteListener() {
            public void inviteReceived(InviteAttribution a) {
            }

            public void attributionUnavailable(String reason) {
                told[0] = reason;
            }
        });
        Invites.forgetLoadedState();
        Invites.checkForInvite();

        assertNull(told[0], "an exact code we were holding was marked expired");
        assertEquals(Invites.STATE_PENDING, Invites.getState());
    }

    @Test
    @EdtTest
    void switchingToOptOutResumesADeclinedLookup() {
        // setConsentMode changes what an absent choice means, so it changes
        // what is allowed -- and it dispatched to no provider, so ordinary
        // analytics resumed while a declined lookup stayed stopped and an
        // attribution's dimensions stayed cleared.
        Analytics.setConsentMode(ConsentMode.OPT_IN);
        Invites.checkForInvite();
        Analytics.setConsent(AnalyticsConsent.builder().analytics(false).build());
        assertEquals(Invites.STATE_DECLINED, Invites.getState());
        Analytics.setConsent(null);
        assertEquals(Invites.STATE_DECLINED, Invites.getState(),
                "clearing the choice under opt-in must change nothing");

        Analytics.setConsentMode(ConsentMode.OPT_OUT);

        assertEquals(Invites.STATE_PENDING, Invites.getState(),
                "switching to opt-out did not resume the declined lookup");
    }

    @Test
    @EdtTest
    void tappingTheSameLinkAgainInALaterRunIsProcessed() {
        // Through checkForInvite, which is where the deduplication lives -- a
        // test calling handleUrl directly never reaches it and proves nothing.
        //
        // The durable guard could not tell a repeated read of one delivery from
        // a second tap, which delivers the identical string, so the same link
        // was ignored for ever: the install lost its invite_opened
        // re-engagement event, and under re-attribution the later open could
        // never win.
        String url = "https://cloud.codenameone.com/i/acme/TAP1";
        Display.getInstance().setProperty("AppArg", url);
        assertTrue(Invites.checkForInvite(), "the first delivery was not handled");

        // Repeated reads within one run are still ignored, which is what the
        // deduplication is for.
        assertFalse(Invites.checkForInvite(), "one delivery was handled twice");

        // And a second delivery IN THE SAME RUN -- an Android onNewIntent
        // after the app is backgrounded, which is the ordinary case -- is a new
        // delivery, not a repeated read.
        Display.getInstance().setProperty("AppArg", url);
        assertTrue(Invites.checkForInvite(),
                "a second delivery in the same run was ignored");

        // A later run behaves the same way.
        Invites.forgetLoadedState();
        Display.getInstance().setProperty("AppArg", url);
        assertTrue(Invites.checkForInvite(), "a second tap on the same link was ignored");
    }

    @Test
    @EdtTest
    void anInviteArgumentIsConsumedAndAnythingElseIsLeftAlone() {
        // Consuming it is what distinguishes a delivery from a read. Only an
        // invite is consumed: an application routing its own deep links must
        // find its argument exactly as it arrived.
        Display.getInstance().setProperty("AppArg",
                "https://cloud.codenameone.com/i/acme/EATEN1");
        assertTrue(Invites.checkForInvite());
        assertNull(Display.getInstance().getProperty("AppArg", null),
                "the invite argument was left behind for the next read");

        Display.getInstance().setProperty("AppArg", "https://example.com/some/other/link");
        assertFalse(Invites.checkForInvite());
        assertEquals("https://example.com/some/other/link",
                Display.getInstance().getProperty("AppArg", null),
                "an argument that is not an invite was consumed");
    }

    @FormTest
    void aFailedAttributionWriteLeavesTheLookupPending() {
        // Everything after the write assumes the record is on disk:
        // deliverPending() re-reads it and finds nothing, and flush() will not
        // retry because the state says resolved -- so a valid answer was
        // neither delivered nor asked for again until a restart.
        Invites.checkForInvite();
        InviteStore.failNextWriteForTest(InviteStore.ATTRIBUTION);
        Invites.handleResolution(InviteTestSupport.resolvedJson("NOSPACE1", "c1", "sms"),
                Invites.MATCH_DIRECT, false);

        assertEquals(Invites.STATE_PENDING, Invites.getState(),
                "a failed write still reported the install as resolved");
        assertNotNull(InviteStore.read(InviteStore.PENDING),
                "the retry information was thrown away with it");
    }

    @Test
    @EdtTest
    void aConsentUpdateThatChangesNothingDoesNotQueueASecondLookup() {
        // An application may call setConsent again with analytics still allowed
        // -- to change only personalization or ad storage -- and restarting on
        // that queued a second lookup whose answer was as valid as the first,
        // so the funnel event fired twice and the retry budget was spent
        // without a failure.
        Invites.checkForInvite();
        Map<String, String> pending = InviteStore.read(InviteStore.PENDING);
        int attempts = InviteStore.getInt(pending, "attempts", 0);

        for (int i = 0; i < 5; i++) {
            Analytics.setConsent(AnalyticsConsent.builder().analytics(true)
                    .personalization(i % 2 == 0).build());
        }

        Map<String, String> now = InviteStore.read(InviteStore.PENDING);
        assertEquals(attempts, InviteStore.getInt(now, "attempts", 0),
                "consent updates queued lookups for a request that had not failed");
    }

    @Test
    @EdtTest
    void withdrawingConsentDuringAReplacementAbandonsIt() {
        // Withdrawing consent stops the replacement; it does not un-attribute
        // the install, whose record is still there and makes the state resolved
        // again on the next launch. Writing a DECLINED marker told a registered
        // listener "no invite" as a second, contradictory callback.
        Invites.handleResolution(InviteTestSupport.resolvedJson("FIRST7", "c1", "sms"),
                Invites.MATCH_DIRECT, false);
        Invites.setReattribution(true);
        Invites.handleUrl("https://cloud.codenameone.com/i/acme/SECOND7");

        final int[] told = new int[1];
        Invites.setInviteListener(new InviteListener() {
            public void inviteReceived(InviteAttribution a) {
            }

            public void attributionUnavailable(String reason) {
                told[0]++;
            }
        });
        Analytics.setConsent(AnalyticsConsent.builder().analytics(false).build());

        assertEquals(0, told[0], "a withdrawal told an attributed install it had no invite");
        assertEquals(Invites.STATE_RESOLVED, Invites.getState());
    }

    @FormTest
    void aFailedWriteOnTheLastAttemptCanStillBeRetried() {
        // Leaving the counter at the cap meant the next flush took the
        // attempt-cap branch and marked the install terminal instead of
        // performing the retry -- so the very last response, the one most
        // likely to be the only one left, could never be stored.
        Invites.checkForInvite();
        Map<String, String> pending = InviteStore.read(InviteStore.PENDING);
        pending.put("attempts", String.valueOf(Invites.MAX_ATTEMPTS));
        InviteStore.write(InviteStore.PENDING, pending);

        InviteStore.failNextWriteForTest(InviteStore.ATTRIBUTION);
        Invites.handleResolution(InviteTestSupport.resolvedJson("LAST1", "c1", "sms"),
                Invites.MATCH_DIRECT, false);

        Map<String, String> after = InviteStore.read(InviteStore.PENDING);
        assertNotNull(after);
        assertTrue(InviteStore.getInt(after, "attempts", 0) < Invites.MAX_ATTEMPTS,
                "the promised retry could never happen: the budget was still exhausted");
    }

    @Test
    @EdtTest
    void aDeniedDirectLinkKeepsItsCodeForTheReopening() {
        // A refusal is reopenable, so the code has to survive it. Discarding it
        // meant a user who denied consent when the link arrived and granted it
        // afterwards had the exact claim replaced by a referrer read or a
        // statistical match, which can miss or credit a different click.
        Analytics.setConsent(AnalyticsConsent.builder().analytics(false).build());
        Invites.handleUrl("https://cloud.codenameone.com/i/acme/DENIED1");
        assertEquals(Invites.STATE_DECLINED, Invites.getState());

        Analytics.setConsent(AnalyticsConsent.granted());

        Map<String, String> resumed = InviteStore.read(InviteStore.PENDING);
        assertNotNull(resumed);
        assertEquals("DENIED1", InviteStore.get(resumed, "code", null),
                "the reopened lookup lost the exact code and fell back to a guess");
    }

    @FormTest
    void aTerminalAnswerThatCannotBePersistedIsNotReported() {
        // Reporting an outcome the device cannot remember meant the same lookup
        // and the same callback repeated after every restart -- or, worse, the
        // delivery flag landed on the OLD pending record and left the state at
        // PENDING, so a settled lookup ran again and could never deliver.
        Invites.setAttributionWindow(0);
        final int[] told = new int[1];
        Invites.setInviteListener(new InviteListener() {
            public void inviteReceived(InviteAttribution a) {
            }

            public void attributionUnavailable(String reason) {
                told[0]++;
            }
        });
        InviteStore.failNextWriteForTest(InviteStore.PENDING);
        Invites.checkForInvite();

        assertEquals(0, told[0],
                "an answer the device cannot remember was reported to the listener");
        // The disk carries no terminal marker, which is the condition this
        // guards: the state must not be terminal while the only copy of that
        // answer is in memory. Read through InviteStore rather than through
        // Invites, because the accessor now reconciles the held copy first --
        // which is the point of the assertion below.
        assertNotEquals(Invites.STATE_NONE_FOUND,
                InviteStore.getInt(InviteStore.read(InviteStore.PENDING), "state",
                        Invites.STATE_PENDING),
                "the terminal marker reached the disk, so this proves nothing");

        // And the answer is deferred rather than dropped: the held record is
        // persisted by the next read and the state then agrees with it. Before
        // the record was held at all this stayed pending for ever, so the same
        // lookup ran again on every launch and the listener heard nothing.
        assertEquals(Invites.STATE_NONE_FOUND, Invites.getState(),
                "the terminal answer was neither recorded nor reachable afterwards");
    }

    @Test
    @EdtTest
    void afailedDeliveryWriteIsStillOwedToTheListener() {
        // The other half of the record-held-in-memory change. When the
        // delivered=true write fails, the callback is withheld -- but the map
        // carrying that flag is the one the failure holds for retry, so the
        // next read persisted the very flag the failure was supposed to
        // prevent. The answer then read as already delivered and the listener
        // never heard it, on this launch or any other.
        Invites.setAttributionWindow(0);
        Invites.checkForInvite();
        assertEquals(Invites.STATE_NONE_FOUND, Invites.getState(),
                "the fixture did not reach a terminal answer");

        final int[] told = new int[1];
        InviteStore.failNextWriteForTest(InviteStore.PENDING);
        Invites.setInviteListener(new InviteListener() {
            public void inviteReceived(InviteAttribution a) {
            }

            public void attributionUnavailable(String reason) {
                told[0]++;
            }
        });
        assertEquals(0, told[0],
                "a delivery the device could not record was reported anyway");

        // The record must not claim it was delivered, or nothing will ever
        // report it.
        Map<String, String> marker = Invites.pendingRecordForTest();
        assertNotNull(marker);
        assertFalse(InviteStore.getBoolean(marker, "delivered", false),
                "a delivery that never happened was recorded as done");

        // And a listener registered afterwards is told, which is the contract:
        // exactly one callback per install, and the answer is remembered.
        Invites.setInviteListener(new InviteListener() {
            public void inviteReceived(InviteAttribution a) {
            }

            public void attributionUnavailable(String reason) {
                told[0]++;
            }
        });
        assertEquals(1, told[0], "the answer was owed to the listener and never arrived");
    }

    @Test
    @EdtTest
    void aterminalMarkerPersistedLateIsNotReopenedAsPending() {
        // markTerminal() deliberately does not set the state when its write
        // fails, so the record held for retry can be terminal while memory
        // still says pending. Persisting it without reconciling left the two
        // disagreeing: a later flush read the cached pending state, treated the
        // lookup as live, rewrote the terminal marker back to STATE_PENDING and
        // issued another lookup -- with the device profile markTerminal had
        // stripped, so it could not have matched anyway.
        Invites.checkForInvite();
        assertEquals(Invites.STATE_PENDING, Invites.getState());

        // The terminal write fails, so the answer is held rather than recorded.
        Invites.setAttributionWindow(0);
        InviteStore.failNextWriteForTest(InviteStore.PENDING);
        Invites.forgetLoadedState();
        Invites.checkForInvite();

        // Storage recovers: the next read persists the held terminal record.
        Map<String, String> persisted = Invites.pendingRecordForTest();
        assertNotNull(persisted);
        assertEquals(Invites.STATE_NONE_FOUND,
                InviteStore.getInt(InviteStore.read(InviteStore.PENDING), "state", -1),
                "the held terminal record was never persisted");

        // And the state agrees with the record that is now on the disk.
        assertEquals(Invites.STATE_NONE_FOUND, Invites.getState(),
                "the cached state still says pending, so a flush will reopen a settled lookup");
    }

    @Test
    @EdtTest
    void getStateNeverAnswersFromAcacheTheRecordContradicts() {
        // markTerminal() deliberately does not set the state when its write
        // fails, so the record held for retry can be terminal while memory
        // still says pending. Every path that ACTS on the state reads the
        // record and reconciles on the way, so the disagreement never reached a
        // write -- but getState() is public API, and answering PENDING out of a
        // cache the device's own record already contradicts is wrong on its own
        // terms. Reconciled at the top of loadState(), which every state
        // decision comes through.
        Invites.checkForInvite();
        assertEquals(Invites.STATE_PENDING, Invites.getState());

        Invites.setAttributionWindow(0);
        InviteStore.failNextWriteForTest(InviteStore.PENDING);
        Invites.forgetLoadedState();
        Invites.checkForInvite();

        // Nothing has read the record yet, so the terminal answer is still only
        // in memory and the cached state still says pending -- the precondition
        // this is about.
        assertTrue(Invites.pendingFallbackPresentForTest(),
                "the record was already persisted, so this proves nothing");

        // getState() is public API and must not answer out of a cache the
        // device's own record contradicts. Asserted before anything else
        // touches the record, because every path that acts on the state reads
        // the record and reconciles on the way -- so this is the one caller
        // that can observe the disagreement.
        assertEquals(Invites.STATE_NONE_FOUND, Invites.getState(),
                "getState() answered from a cache the held record contradicts");
        assertEquals(Invites.STATE_NONE_FOUND,
                InviteStore.getInt(InviteStore.read(InviteStore.PENDING), "state", -1),
                "asking for the state did not persist the record it answered from");
    }

    @Test
    @EdtTest
    void aFirstTimeDenialStartsItsOwnClock() {
        // Someone who had already refused reaches this on a first launch, when
        // nothing has written a pending record yet. Copying the absent clock
        // left expiresAt at 0, which beginDeferred reads as "no window", so an
        // arbitrarily old install could still run a fingerprint match after a
        // later grant.
        Analytics.setConsent(AnalyticsConsent.builder().analytics(false).build());
        Invites.checkForInvite();
        assertEquals(Invites.STATE_DECLINED, Invites.getState());

        Map<String, String> marker = InviteStore.read(InviteStore.PENDING);
        assertNotNull(marker);
        assertTrue(InviteStore.getLong(marker, "expiresAt", 0) > System.currentTimeMillis(),
                "the denial marker carries no window, so a reopening would have none");
        assertTrue(InviteStore.getLong(marker, "firstLaunch", 0) > 0);
    }

    @FormTest
    void aDeliveryThatCannotBeRecordedIsNotMadeTwice() {
        // deliveredThisRun suppresses duplicates only until the process exits,
        // so calling the listener on a delivery the device cannot remember
        // means inviteReceived() fires again on the next launch.
        Invites.handleResolution(InviteTestSupport.resolvedJson("ONCE1", "c1", "sms"),
                Invites.MATCH_DIRECT, false);

        final int[] received = new int[1];
        InviteListener l = new InviteListener() {
            public void inviteReceived(InviteAttribution a) {
                received[0]++;
            }

            public void attributionUnavailable(String reason) {
            }
        };
        InviteStore.failNextWriteForTest(InviteStore.ATTRIBUTION);
        Invites.setInviteListener(l);
        assertEquals(0, received[0],
                "the listener was told about a delivery the device cannot remember");

        // A later launch, with storage working, delivers it exactly once.
        Invites.forgetLoadedState();
        Invites.setInviteListener(null);
        Invites.setInviteListener(l);
        assertEquals(1, received[0], "the attribution was never delivered at all");
    }
}
