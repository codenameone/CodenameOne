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
package com.codename1.call;

import com.codename1.call.session.CallAction;
import com.codename1.call.session.CallActionAdapter;
import com.codename1.call.session.CallAudioRoute;
import com.codename1.call.session.CallAudioSession;
import com.codename1.call.session.CallConfiguration;
import com.codename1.call.session.CallSession;
import com.codename1.call.session.Calls;
import com.codename1.call.voip.PushedCall;
import com.codename1.call.voip.VoipPush;
import com.codename1.call.voip.VoipPushListener;
import com.codename1.impl.call.CallRequests;
import com.codename1.impl.call.LocalCallBridge;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The whole call stack, against the simulation. */
public class LocalCallTest {

    private LocalCallBridge bridge;

    @BeforeEach
    public void install() {
        bridge = new LocalCallBridge();
        CallRequests.resetForTest(bridge);
    }

    @AfterEach
    public void clear() {
        CallRequests.resetForTest(null);
    }

    private CallSession ring(String id) {
        CallAwait.value(Calls.configure(new CallConfiguration().displayName("Acme")));
        return CallAwait.value(Calls.reportIncoming(id,
                CallHandle.phone("+14155551212"), "Ada", false));
    }

    @Test
    public void everythingWorksAndSaysItIsSupported() {
        assertTrue(Calls.isSupported());
        assertTrue(VoipPush.isSupported());
        assertSame(CallAvailability.AVAILABLE, Calls.getAvailability());
    }

    @Test
    public void aReportedCallIsRingingAndFindable() {
        String id = CallId.random();
        CallSession s = ring(id);
        assertEquals(id, s.getCallId());
        assertSame(CallState.RINGING, s.getState());
        assertSame(CallDirection.INCOMING, s.getDirection());
        assertSame(s, Calls.getSession(id));
        assertEquals(1, Calls.getSessions().length);
    }

    @Test
    public void reportingBeforeConfiguringIsRefused() {
        // On Android an unregistered PhoneAccount makes the platform ignore
        // the call and say nothing. The simulation refuses instead, because
        // silence is not something a test can assert on.
        CallAwait.assertFailedWith(CallError.CALL_REFUSED,
                Calls.reportIncoming(CallId.random(),
                        CallHandle.phone("+14155551212"), "Ada", false));
    }

    @Test
    public void aRefusedCallLeavesNoSessionBehind() {
        // The session must not be handed over or remembered when the system
        // said no, or the app acts on a call that is not ringing.
        String id = CallId.random();
        CallAwait.errorOf(Calls.reportIncoming(id,
                CallHandle.phone("+14155551212"), "Ada", false));
        assertNull(Calls.getSession(id));
        assertEquals(0, Calls.getSessions().length);
    }

    @Test
    public void aNonCanonicalIdIsRefusedBeforeReachingThePlatform() {
        CallAwait.assertFailedWith(CallError.INVALID_ID,
                Calls.reportIncoming("nope", CallHandle.phone("+1"), "Ada", false));
    }

    @Test
    public void aCallWithNoHandleIsRefused() {
        CallAwait.assertFailedWith(CallError.INVALID_ID,
                Calls.reportIncoming(CallId.random(), null, "Ada", false));
    }

    @Test
    public void theSameIdCannotRingTwice() {
        // Reporting a duplicate uuid is a hard error on iOS rather than a
        // no-op, and the socket-plus-push race makes it likely.
        String id = CallId.random();
        ring(id);
        CallAwait.assertFailedWith(CallError.DUPLICATE_CALL,
                Calls.reportIncoming(id, CallHandle.phone("+1"), "Ada", false));
    }

    @Test
    public void anEmergencyCallRefusesTheReportRatherThanRingingSilently() {
        bridge.setAvailability(CallAvailability.EMERGENCY_CALL_IN_PROGRESS.ordinal());
        assertSame(CallAvailability.EMERGENCY_CALL_IN_PROGRESS, Calls.getAvailability());
        CallAwait.value(Calls.configure(new CallConfiguration()));
        CallAwait.assertFailedWith(CallError.CALL_REFUSED,
                Calls.reportIncoming(CallId.random(),
                        CallHandle.phone("+1"), "Ada", false));
    }

    @Test
    public void answeringArrivesBeforeTheAudioSession() {
        // The ordering the whole simulation exists to preserve: an app that
        // starts media on the answer gets a silent call on a device.
        final List<String> order = new ArrayList<String>();
        Calls.addActionListener(new CallActionAdapter() {
            public void answerRequested(String callId, CallAction action) {
                order.add("answer");
            }
            public void audioSessionActivated(CallAudioSession session) {
                order.add("audio");
            }
        });
        String id = CallId.random();
        ring(id);
        bridge.simulateAnswer(id);
        waitFor(order, 2);
        assertEquals("answer", order.get(0));
        assertEquals("audio", order.get(1));
    }

    @Test
    public void theAudioSessionNamesItsCallAndRoute() {
        final List<CallAudioSession> got = new ArrayList<CallAudioSession>();
        Calls.addActionListener(new CallActionAdapter() {
            public void audioSessionActivated(CallAudioSession session) {
                got.add(session);
            }
        });
        String id = CallId.random();
        ring(id);
        bridge.simulateRouteChange(CallAudioRoute.SPEAKER);
        bridge.simulateAnswer(id);
        waitFor(got, 1);
        assertEquals(id, got.get(0).getCallId());
        assertSame(CallAudioRoute.SPEAKER, got.get(0).getRoute());
    }

    @Test
    public void theFarEndHangingUpEndsTheCallAndForgetsIt() {
        final List<CallEndReason> reasons = new ArrayList<CallEndReason>();
        Calls.addActionListener(new CallActionAdapter() {
            public void callEnded(String callId, CallEndReason reason) {
                reasons.add(reason);
            }
        });
        String id = CallId.random();
        ring(id);
        bridge.simulateRemoteEnd(id, CallEndReason.REMOTE_ENDED);
        waitFor(reasons, 1);
        assertSame(CallEndReason.REMOTE_ENDED, reasons.get(0));
        assertNull(Calls.getSession(id), "an ended call must not stay findable");
    }

    @Test
    public void aProviderResetClearsEverySessionBeforeTellingAnyone() {
        // A listener that iterates getSessions() during providerReset must not
        // see calls the system has already destroyed.
        final int[] visible = new int[]{-1};
        Calls.addActionListener(new CallActionAdapter() {
            public void providerReset() {
                visible[0] = Calls.getSessions().length;
            }
        });
        ring(CallId.random());
        bridge.simulateProviderReset();
        long limit = System.currentTimeMillis() + 5000;
        while (visible[0] < 0 && System.currentTimeMillis() < limit) {
            sleep();
        }
        assertEquals(0, visible[0]);
    }

    @Test
    public void anIgnoredActionIsFulfilledRatherThanDropped() {
        // Silence has to mean "done": the platform kills a call whose action
        // goes unanswered, so a listener that does nothing must still answer.
        final List<CallAction> seen = new ArrayList<CallAction>();
        Calls.addActionListener(new CallActionAdapter() {
            public void answerRequested(String callId, CallAction action) {
                seen.add(action);
            }
        });
        String id = CallId.random();
        ring(id);
        bridge.simulateAnswer(id);
        waitFor(seen, 1);
        assertTrue(isAnswered(seen.get(0)),
                "an action nobody deferred must be answered automatically");
    }

    @Test
    public void aDeferredActionIsLeftToTheApplication() {
        final List<CallAction> seen = new ArrayList<CallAction>();
        Calls.addActionListener(new CallActionAdapter() {
            public void answerRequested(String callId, CallAction action) {
                action.defer();
                seen.add(action);
            }
        });
        String id = CallId.random();
        ring(id);
        bridge.simulateAnswer(id);
        waitFor(seen, 1);
        assertFalse(isAnswered(seen.get(0)),
                "a deferred action must wait for the application");
        seen.get(0).fulfill();
        assertTrue(isAnswered(seen.get(0)));
    }

    @Test
    public void theFacadeAndThePlatformAgreeAboutTheCall() {
        // The two views drift apart exactly when a report is refused and the
        // app keeps its session anyway, so asserting they agree is worth more
        // than asserting either alone.
        String id = CallId.random();
        CallSession s = ring(id);
        assertSame(CallState.RINGING, bridge.getSimulatedState(id));
        assertEquals("Ada", bridge.getSimulatedDisplayName(id));
        assertEquals("+14155551212", bridge.getSimulatedHandle(id).getValue());
        assertTrue(bridge.isSimulatedIncoming(id));
        assertFalse(bridge.isSimulatedVideo(id));

        CallAwait.value(s.setMuted(true));
        assertTrue(bridge.isSimulatedMuted(id));
        assertEquals(s.isMuted(), bridge.isSimulatedMuted(id));

        CallAwait.value(s.setHeld(true));
        assertSame(s.getState(), bridge.getSimulatedState(id));
    }

    @Test
    public void updatingACallReachesThePlatform() {
        String id = CallId.random();
        CallSession s = ring(id);
        s.update(CallHandle.generic("ada@example.com"), "Ada Lovelace");
        assertEquals("Ada Lovelace", bridge.getSimulatedDisplayName(id));
        assertEquals("ada@example.com", bridge.getSimulatedHandle(id).getValue());
    }

    @Test
    public void aRefusedCallIsUnknownToThePlatformToo() {
        String id = CallId.random();
        CallAwait.errorOf(Calls.reportIncoming(id,
                CallHandle.phone("+14155551212"), "Ada", false));
        assertNull(bridge.getSimulatedState(id));
    }

    @Test
    public void aSuccessfullyEndedCallIsForgotten() {
        // getSessions() promises current calls, so an ended one must not sit
        // there for the life of the process.
        String id = CallId.random();
        CallSession s = ring(id);
        CallAwait.value(s.end(CallEndReason.LOCAL_ENDED));
        assertNull(Calls.getSession(id), "an ended call must be forgotten");
        assertEquals(0, Calls.getSessions().length);
        assertSame(CallState.ENDED, s.getState());
    }

    @Test
    public void aRefusedEndKeepsTheSessionAddressable() {
        // The call is still up as far as the system is concerned, so the app
        // must still be able to find it and try again.
        String id = CallId.random();
        CallSession s = ring(id);
        bridge.primeEndFailure();
        CallAwait.errorOf(s.end(CallEndReason.LOCAL_ENDED));
        assertNotNull(Calls.getSession(id),
                "a call the system refused to end is still a call");
        assertFalse(s.getState() == CallState.ENDED,
                "a refused end must not leave the session claiming ENDED");
    }

    @Test
    public void aDeferredEndThatFailsKeepsTheSession() {
        // The documented behaviour of failing an end action is that the
        // system UI restores the call. Forgetting the session on dispatch
        // left the app with a live call it could no longer address.
        final List<CallAction> seen = new ArrayList<CallAction>();
        Calls.addActionListener(new CallActionAdapter() {
            public void endRequested(String callId, CallAction action) {
                action.defer();
                seen.add(action);
            }
        });
        String id = CallId.random();
        ring(id);
        bridge.simulateEndRequest(id);
        waitFor(seen, 1);
        seen.get(0).fail();
        assertNotNull(Calls.getSession(id),
                "a failed end must leave the call addressable");
    }

    @Test
    public void aDeferredEndThatSucceedsLaterStillForgetsTheCall() {
        // The gap the first fix left: checking isAnswered() straight after
        // dispatch saw false for a deferred action, and a later fulfil had
        // nothing watching -- so a call the app really did end stayed in
        // getSessions() for the life of the process.
        final List<CallAction> seen = new ArrayList<CallAction>();
        Calls.addActionListener(new CallActionAdapter() {
            public void endRequested(String callId, CallAction action) {
                action.defer();
                seen.add(action);
            }
        });
        String id = CallId.random();
        ring(id);
        bridge.simulateEndRequest(id);
        waitFor(seen, 1);
        assertNotNull(Calls.getSession(id), "still up until the app answers");
        seen.get(0).fulfill();
        long limit = System.currentTimeMillis() + 5000;
        while (Calls.getSession(id) != null && System.currentTimeMillis() < limit) {
            sleep();
        }
        assertNull(Calls.getSession(id),
                "a deferred end that is fulfilled must forget the call");
    }

    @Test
    public void anIgnoredEndStillForgetsTheCall() {
        // The common case must keep working: a listener that does nothing
        // fulfills the action, so the call really did end.
        Calls.addActionListener(new CallActionAdapter());
        String id = CallId.random();
        ring(id);
        bridge.simulateEndRequest(id);
        long limit = System.currentTimeMillis() + 5000;
        while (Calls.getSession(id) != null && System.currentTimeMillis() < limit) {
            sleep();
        }
        assertNull(Calls.getSession(id));
    }

    @Test
    public void aDeferredActionNobodyAnswersIsFailedByTheSafetyTimer() {
        // defer() promises this in its documentation, and without it the
        // platform times the action out instead -- which leaves the system UI
        // and the app disagreeing with nothing in the log.
        final List<CallAction> seen = new ArrayList<CallAction>();
        Calls.addActionListener(new CallActionAdapter() {
            public void answerRequested(String callId, CallAction action) {
                action.defer();
                seen.add(action);
            }
        });
        String id = CallId.random();
        ring(id);
        bridge.simulateAnswer(id);
        waitFor(seen, 1);
        long limit = System.currentTimeMillis() + 9000;
        while (!isAnswered(seen.get(0)) && System.currentTimeMillis() < limit) {
            sleep();
        }
        assertTrue(isAnswered(seen.get(0)),
                "a deferred action nobody answered must be failed for them");
    }

    @Test
    public void noPlatformClaimsASystemAudioRoutePicker() {
        // Neither iOS nor Android has one, so the simulation must not be the
        // single place an app's picker code appears to work.
        assertEquals(0, Calls.getCapabilities()
                & com.codename1.call.spi.CallBridge.CAPABILITY_ROUTE_PICKER);
        CallAwait.assertFailedWith(CallError.NOT_SUPPORTED,
                Calls.showAudioRoutePicker(CallId.random()));
    }

    @Test
    public void endingACallThatIsAlreadyGoneIsRefused() {
        String id = CallId.random();
        CallSession s = ring(id);
        CallAwait.value(s.end(CallEndReason.LOCAL_ENDED));
        CallAwait.assertFailedWith(CallError.INVALID_ID,
                s.end(CallEndReason.LOCAL_ENDED));
    }

    @Test
    public void holdAndMuteMoveTheSessionState() {
        String id = CallId.random();
        CallSession s = ring(id);
        CallAwait.value(s.setHeld(true));
        assertSame(CallState.HELD, s.getState());
        CallAwait.value(s.setHeld(false));
        assertSame(CallState.ACTIVE, s.getState());
        CallAwait.value(s.setMuted(true));
        assertTrue(s.isMuted());
    }

    // ------------------------------------------------------------------
    // the cold-start path
    // ------------------------------------------------------------------

    @Test
    public void aCallThatRangBeforeAnyoneWasListeningIsDrainedLater() {
        // This is the iOS cold start, reproduced without iOS: native code
        // reported the call to the system during launch, and the app only
        // finds out once it installs a listener.
        String id = CallId.random();
        bridge.enqueuePushedCall(id, CallHandle.phone("+14155551212"),
                "Ada", false, false, "room-7");
        final List<PushedCall> got = new ArrayList<PushedCall>();
        VoipPush.setListener(new Collector(got));
        waitFor(got, 1);
        assertEquals(id, got.get(0).getSession().getCallId());
        assertEquals("room-7", got.get(0).getData());
        assertFalse(got.get(0).isStale());
    }

    @Test
    public void aDrainedCallIsAlreadyRingingRatherThanNeedingToBeReported() {
        String id = CallId.random();
        bridge.enqueuePushedCall(id, CallHandle.phone("+1"), "Ada", false,
                false, null);
        final List<PushedCall> got = new ArrayList<PushedCall>();
        VoipPush.setListener(new Collector(got));
        waitFor(got, 1);
        assertSame(CallState.RINGING, got.get(0).getSession().getState());
        assertNotNull(Calls.getSession(id),
                "a pushed call is a real session the app can act on");
    }

    @Test
    public void aCallThatEndedBeforeTheDrainArrivesStale() {
        // The app was killed before it saw the call. It must be logged as
        // missed, not answered -- there is nothing left to answer.
        String id = CallId.random();
        bridge.enqueuePushedCall(id, CallHandle.phone("+1"), "Ada", false,
                true, null);
        final List<PushedCall> got = new ArrayList<PushedCall>();
        VoipPush.setListener(new Collector(got));
        waitFor(got, 1);
        assertTrue(got.get(0).isStale());
        assertSame(CallState.ENDED, got.get(0).getSession().getState());
    }

    @Test
    public void theQueueDrainsExactlyOnce() {
        // A second listener must not replay calls the first one already saw,
        // or the user gets the same call twice.
        bridge.enqueuePushedCall(CallId.random(), CallHandle.phone("+1"),
                "Ada", false, false, null);
        final List<PushedCall> first = new ArrayList<PushedCall>();
        VoipPush.setListener(new Collector(first));
        waitFor(first, 1);

        final List<PushedCall> second = new ArrayList<PushedCall>();
        VoipPush.setListener(new Collector(second));
        sleepFor(300);
        assertEquals(0, second.size(), "the queue must not replay");
    }

    @Test
    public void aPushWithAMalformedIdStillRingsAndSaysSo() {
        // Refusing would kill the app on iOS, so the call is rung with an
        // invented id -- and flagged, so the server bug is findable instead
        // of presenting as calls that never connect.
        bridge.enqueuePushedCall("not-a-uuid", CallHandle.phone("+1"),
                "Ada", false, false, null);
        final List<PushedCall> got = new ArrayList<PushedCall>();
        VoipPush.setListener(new Collector(got));
        waitFor(got, 1);
        assertTrue(got.get(0).isIdentifierSynthesized());
        assertTrue(CallId.isValid(got.get(0).getSession().getCallId()));
    }

    @Test
    public void aStaleCallIsNotRegisteredAsACurrentSession() {
        // getSessions() promises current calls. Registering a stale one left
        // every missed cold-start push sitting there for the life of the
        // process.
        String id = CallId.random();
        bridge.enqueuePushedCall(id, CallHandle.phone("+1"), "Missed", false,
                true, null);
        final List<PushedCall> got = new ArrayList<PushedCall>();
        VoipPush.setListener(new Collector(got));
        waitFor(got, 1);
        assertTrue(got.get(0).isStale());
        assertNotNull(got.get(0).getSession(),
                "the app still needs somewhere to read the handle from");
        assertNull(Calls.getSession(id),
                "a call that is already over is not a current call");
        assertEquals(0, Calls.getSessions().length);
    }

    @Test
    public void aRefusedHoldDoesNotMoveTheSessionState() {
        // The platform can reject a hold -- the call ending while the request
        // is in flight is the ordinary way -- and the session used to claim
        // HELD anyway, with nothing to roll it back.
        String id = CallId.random();
        CallSession s = ring(id);
        CallAwait.value(s.setHeld(true));
        assertSame(CallState.HELD, s.getState());
        bridge.primeOperationFailure();
        CallAwait.errorOf(s.setHeld(false));
        assertSame(CallState.HELD, s.getState(),
                "a rejected resume must leave the state where the system has it");
    }

    @Test
    public void aRefusedMuteDoesNotMoveTheSessionFlag() {
        String id = CallId.random();
        CallSession s = ring(id);
        bridge.primeOperationFailure();
        CallAwait.errorOf(s.setMuted(true));
        assertFalse(s.isMuted(),
                "a rejected mute must not show as muted");
    }

    @Test
    public void registeringYieldsAToken() {
        String token = CallAwait.value(VoipPush.register());
        assertNotNull(token);
        assertEquals(token, VoipPush.getToken());
    }

    // ------------------------------------------------------------------

    /** Collects pushed calls. A named class so it holds no outer reference. */
    private static final class Collector implements VoipPushListener {
        private final List<PushedCall> sink;

        Collector(List<PushedCall> sink) {
            this.sink = sink;
        }

        public void callReceived(PushedCall call) {
            sink.add(call);
        }

        public void tokenChanged(String token) {
        }
    }

    /** Whether an action has been answered, which is package-private state. */
    private static boolean isAnswered(CallAction a) {
        try {
            java.lang.reflect.Method m =
                    CallAction.class.getDeclaredMethod("isAnswered");
            m.setAccessible(true);
            Object r = m.invoke(a);
            return r instanceof Boolean && ((Boolean) r).booleanValue();
        } catch (Exception e) {
            throw new AssertionError("could not read the action state: " + e);
        }
    }

    private static void waitFor(List<?> sink, int count) {
        long limit = System.currentTimeMillis() + 5000;
        while (sink.size() < count && System.currentTimeMillis() < limit) {
            sleep();
        }
        assertTrue(sink.size() >= count,
                "expected " + count + " event(s) and saw " + sink.size());
    }

    private static void sleepFor(long millis) {
        long limit = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < limit) {
            sleep();
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
