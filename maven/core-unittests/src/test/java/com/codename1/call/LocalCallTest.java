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
import com.codename1.call.session.CallActionListener;
import com.codename1.call.session.CallAudioRoute;
import com.codename1.call.session.CallAudioSession;
import com.codename1.call.session.CallConfiguration;
import com.codename1.call.session.CallSession;
import com.codename1.call.session.Calls;
import com.codename1.call.voip.PushedCall;
import com.codename1.impl.call.CallWire;
import com.codename1.call.voip.VoipPush;
import com.codename1.call.voip.VoipPushListener;
import com.codename1.impl.call.CallRequests;
import com.codename1.util.AsyncResource;
import com.codename1.call.spi.CallBridge;
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
        // AFTER configuring, because an unconfigured provider now reports
        // NOT_CONFIGURED -- which is the state this asserted through.
        CallAwait.value(Calls.configure(new CallConfiguration()));
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
        // Configured FIRST: the assertion below used to pass through an
        // unconfigured provider, so it never reached the emergency state it
        // is named for.
        CallAwait.value(Calls.configure(new CallConfiguration()));
        bridge.setAvailability(CallAvailability.EMERGENCY_CALL_IN_PROGRESS.ordinal());
        assertSame(CallAvailability.EMERGENCY_CALL_IN_PROGRESS, Calls.getAvailability());
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
    public void aRetryAfterAResetKeepsItsOwnSession() {
        // A call id is reusable the moment its call is gone, and a reset
        // followed by an immediate retry is the ordinary way that happens.
        // The old report's late answer used to remove whatever the id named
        // -- which by then was the RETRY's session: accepted, handed to its
        // caller, and absent from getSession() for the rest of its life.
        String id = CallId.random();
        CallSession first = ring(id);
        assertSame(first, Calls.getSession(id));

        bridge.simulateProviderReset();
        long limit = System.currentTimeMillis() + 5000;
        while (Calls.getSessions().length > 0
                && System.currentTimeMillis() < limit) {
            sleep();
        }

        CallSession retry = ring(id);
        assertSame(retry, Calls.getSession(id),
                "the retry owns the id now");
        // Whatever the first report still had to say cannot unregister it.
        first.reportEndedRemotely(CallEndReason.REMOTE_ENDED);
        assertSame(retry, Calls.getSession(id),
                "the stale report must not forget the retry's session");
    }

    @Test
    public void aProviderResetEndsTheSessionsTheAppStillHolds() {
        // Clearing the map is not enough: an app keeps the CallSession a
        // report handed it, and a reset says every call is gone. Left
        // RINGING or ACTIVE, that object contradicts both the reset and
        // CallState's terminal contract -- and would still take a
        // reportConnected for a call the provider destroyed.
        CallSession s = ring(CallId.random());
        assertSame(CallState.RINGING, s.getState());

        bridge.simulateProviderReset();
        long limit = System.currentTimeMillis() + 5000;
        while (Calls.getSessions().length > 0
                && System.currentTimeMillis() < limit) {
            sleep();
        }
        assertSame(CallState.ENDED, s.getState(),
                "a reset must end the sessions the app is still holding");
        s.reportConnected();
        assertSame(CallState.ENDED, s.getState());
    }

    @Test
    public void reportingTheCallAnswersTheSystemStart() {
        // The documented answer to startCallRequested is a reportOutgoing
        // with the id it was handed -- NOT a call to fulfill(). Failing an
        // action the listener had honoured destroyed the very call it had
        // just placed: on Android reportOutgoing adopts the connection and
        // acknowledges it, and the failed token then tore that connection
        // down, handing the app a CallSession for a call Telecom had ended.
        // Configured FIRST: the report has to be one the platform accepts,
        // or the action is answered false and the test would be asserting
        // the old behaviour of claiming success before asking.
        CallAwait.value(Calls.configure(new CallConfiguration().displayName("Acme")));
        final List<String> started = new ArrayList<String>();
        Calls.addActionListener(new CallActionAdapter() {
            public void startCallRequested(String callId, CallHandle handle,
                    boolean video, CallAction action) {
                started.add(callId);
                Calls.reportOutgoing(callId, handle, "Ada", video);
            }
        });
        String id = CallId.random();
        bridge.simulateStartCallRequest(id, CallHandle.phone("+14155551212"),
                false);
        waitFor(started, 1);

        long limit = System.currentTimeMillis() + 5000;
        while (bridge.getLastActionFulfilled() == null
                && System.currentTimeMillis() < limit) {
            sleep();
        }
        assertEquals(Boolean.TRUE, bridge.getLastActionFulfilled(),
                "reporting the call IS handling the request");
    }

    @Test
    public void aRefusedReportFailsTheSystemStart() {
        // The START used to be answered TRUE before the platform had accepted
        // the call, so a report the port refuses told the system the call had
        // been placed while the failed handover removed the Java session,
        // leaving the connection Telecom created dialing for ever.
        //
        // Calls.configure is deliberately NOT called: an unconfigured
        // provider is the cold start this guards -- the PhoneAccount is
        // registered from a previous launch, so the system can raise a call
        // before configure() has run.
        final List<String> asked = new ArrayList<String>();
        Calls.addActionListener(new CallActionAdapter() {
            public void startCallRequested(String callId, CallHandle handle,
                    boolean video, CallAction action) {
                asked.add(callId);
                Calls.reportOutgoing(callId, handle, "Ada", video);
            }
        });
        String id = CallId.random();
        bridge.simulateStartCallRequest(id, CallHandle.phone("+14155551212"),
                false);
        waitFor(asked, 1);

        long limit = System.currentTimeMillis() + 5000;
        while (bridge.getLastActionFulfilled() == null
                && System.currentTimeMillis() < limit) {
            sleep();
        }
        assertEquals(Boolean.FALSE, bridge.getLastActionFulfilled(),
                "a report the platform refused did not place the call");
        assertNull(Calls.getSession(id));
    }

    @Test
    public void aDeferredStartIsStillAnsweredByTheReport() {
        // defer() is the documented way to place the call asynchronously, so
        // the adoption entry has to outlive the listener returning. Dropping
        // it there left the later report unable to answer the action, and its
        // own safety timer then failed a request the app had honoured --
        // destroying the system-started connection after the report had
        // already succeeded.
        CallAwait.value(Calls.configure(new CallConfiguration().displayName("Acme")));
        final List<CallAction> deferred = new ArrayList<CallAction>();
        Calls.addActionListener(new CallActionAdapter() {
            public void startCallRequested(String callId, CallHandle handle,
                    boolean video, CallAction action) {
                action.defer();
                deferred.add(action);
            }
        });
        String id = CallId.random();
        bridge.simulateStartCallRequest(id, CallHandle.phone("+14155551212"),
                false);
        waitFor(deferred, 1);
        assertNull(bridge.getLastActionFulfilled(),
                "a deferred action is not answered when the listener returns");

        // The report arrives later, exactly as defer() promises.
        Calls.reportOutgoing(id, CallHandle.phone("+14155551212"), "Ada", false);
        long limit = System.currentTimeMillis() + 5000;
        while (bridge.getLastActionFulfilled() == null
                && System.currentTimeMillis() < limit) {
            sleep();
        }
        assertEquals(Boolean.TRUE, bridge.getLastActionFulfilled(),
                "the deferred report still answers the start request");
    }

    @Test
    public void anIgnoredSystemStartIsFailedRatherThanFulfilled() {
        // The one action where silence does NOT mean done. Everywhere else
        // the app was asked to do something to a call that exists, and the
        // platform kills an action nobody answers -- so fulfilling is right.
        // A START asks this app to PLACE a call, and only a
        // Calls.reportOutgoing can do that. An adapter subclassed for other
        // events does not override startCallRequested at all, so fulfilling
        // claimed the call had been placed when nothing had happened: iOS
        // closes the adoption window, Android leaves the connection dialing
        // for ever, and a call from Recents or an assistant is acknowledged
        // and never placed.
        final List<CallAction> seen = new ArrayList<CallAction>();
        Calls.addActionListener(new CallActionAdapter() {
            // startCallRequested deliberately NOT overridden.
            public void answerRequested(String callId, CallAction action) {
                seen.add(action);
            }
        });
        String id = CallId.random();
        bridge.simulateStartCallRequest(id, CallHandle.phone("+14155551212"),
                false);
        long limit = System.currentTimeMillis() + 5000;
        while (bridge.getLastActionFulfilled() == null
                && System.currentTimeMillis() < limit) {
            sleep();
        }
        assertEquals(Boolean.FALSE, bridge.getLastActionFulfilled(),
                "an unhandled system start must fail, not report success");
        assertTrue(seen.isEmpty());
    }

    @Test
    public void aRefusedEndLeavesTheCallUpOnBothSides() {
        // The simulation is only worth testing against while it models what
        // the devices do. A failed end used to leave the simulated platform
        // at ENDED while Calls correctly kept the session ringing -- so a
        // test inspecting both saw a disagreement no device produces. CallKit
        // and Telecom both RESTORE a call whose end the app refused.
        final List<CallAction> seen = new ArrayList<CallAction>();
        Calls.addActionListener(new CallActionAdapter() {
            public void endRequested(String callId, CallAction action) {
                seen.add(action);
                // The app says it could not hang up.
                action.fail();
            }
        });
        String id = CallId.random();
        ring(id);
        bridge.simulateEndRequest(id);
        waitFor(seen, 1);

        assertNotNull(Calls.getSession(id), "the facade keeps the call");
        assertSame(CallState.RINGING, bridge.callState(id),
                "and the platform it models still has it ringing");
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
    public void anEndedSessionStaysEnded() {
        // ENDED is terminal in CallState, and signalling is asynchronous: a
        // media-connected callback already in flight when the call ended used
        // to move the session back to ACTIVE. The ports drop the report --
        // the platform call is gone -- so the Java object was left
        // contradicting both the system and its own contract, and anything
        // watching it could render or restart media for a call that is over.
        String id = CallId.random();
        CallSession s = ring(id);
        CallAwait.value(s.end(CallEndReason.LOCAL_ENDED));
        assertSame(CallState.ENDED, s.getState());

        s.reportConnected();
        assertSame(CallState.ENDED, s.getState(),
                "a late connected report must not revive an ended call");
        s.reportStartedConnecting();
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
    public void availabilityBeforeConfigureAgreesWithTheRefusal() {
        // getAvailability() exists so an app can tell the far end to stop
        // retrying INSTEAD of finding out from a failed report -- its own
        // javadoc says so. It answered AVAILABLE before configure() while the
        // very next report was refused unconditionally, which is the one
        // outcome that makes the check worthless: the app tells the caller
        // everything is fine and then cannot ring.
        assertEquals(CallAvailability.NOT_CONFIGURED, Calls.getAvailability(),
                "an unconfigured provider cannot accept a call");
        CallAwait.assertFailedWith(CallError.CALL_REFUSED,
                Calls.reportIncoming(CallId.random(),
                        CallHandle.phone("+14155551212"), "Ada", false));

        CallAwait.value(Calls.configure(
                new CallConfiguration().displayName("Acme")));
        assertEquals(CallAvailability.AVAILABLE, Calls.getAvailability(),
                "configuring is what makes a report possible");
    }

    @Test
    public void aHeldCallThatEndedBeforeItsListenerArrivesIsStale() {
        // A pushed call with nobody to hand it to waits in HELD, and the call
        // does NOT wait with it: the far end hangs up, or the platform
        // retires it, and the session ends. PushedCall is immutable, so the
        // replay went on describing it as live -- and an app following the
        // documented shape reads isStale() to decide whether to attach
        // signalling and media, so it attached them to a call that was over.
        String id = CallId.random();
        CallAwait.value(Calls.configure(
                new CallConfiguration().displayName("Acme")));
        VoipPush.deliverPushedCall(id,
                CallWire.encodeHandle(CallHandle.phone("+14155551212")),
                "Ada", false, false, false, "room-9",
                System.currentTimeMillis());
        // It ends while nothing is listening. Through the port-facing
        // delivery, because the simulated platform only ends calls IT
        // created and this one was injected as a push.
        Calls.deliverCallEnded(id, CallEndReason.REMOTE_ENDED.ordinal());
        long limit = System.currentTimeMillis() + 5000;
        while (Calls.getSession(id) != null
                && System.currentTimeMillis() < limit) {
            sleep();
        }
        assertNull(Calls.getSession(id), "the call is over before the replay");

        final List<PushedCall> got = new ArrayList<PushedCall>();
        VoipPush.setListener(new Collector(got));
        waitFor(got, 1);
        assertTrue(got.get(0).isStale(),
                "a call that ended while held must be replayed as stale");
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
    public void answeringAnActionThePlatformGaveUpOnHasNoLocalEffect() {
        // CallKit abandons an action after about five seconds. If the EDT was
        // blocked past that deadline the queued event still runs afterwards,
        // and the app's fulfil then removed the Java session while the system
        // call UI went on showing the call -- the two disagreeing silently,
        // which is the failure the whole action contract exists to prevent.
        // completeAction now reports that the platform no longer held the
        // action and the local effect is skipped.
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
        bridge.expireOutstandingActions();
        seen.get(0).fulfill();
        sleepFor(200);
        assertNotNull(Calls.getSession(id),
                "an end the platform timed out must not forget the session");
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
    public void aListenerInstalledAfterTheTokenArrivedStillHearsIt() {
        // PushKit supplies credentials during a cold launch -- the registry
        // is built in willFinishLaunching, so this is the ordinary ordering
        // rather than a race -- and with no listener yet the delivery was
        // dropped. The documented setListener(); register(); sequence then
        // got the SAME token back, so "changed" was false and tokenChanged
        // never fired: an app following the example, which ignores the
        // returned resource, never registered its token with its server and
        // could not be called at all.
        VoipPush.deliverToken(-1, "cafebabe");

        final List<String> tokens = new ArrayList<String>();
        VoipPush.setListener(new VoipPushListener() {
            public void callReceived(PushedCall call) {
            }

            public void tokenChanged(String token) {
                tokens.add(token);
            }
        });
        waitFor(tokens, 1);
        assertEquals("cafebabe", tokens.get(0),
                "a listener has to be told the token it has never seen");
    }

    @Test
    public void aPushedCallDeliveredTooEarlyIsHeldRatherThanDropped() {
        // The port's readiness flag is the UNION of the two listener kinds,
        // so an app that registers a Calls ACTION listener and no VoipPush
        // one makes the native side think it can drain. The call was then
        // handed to a facade with no push listener and dropped for good --
        // and the drain had already claimed it, so the platform's own
        // unanswered-call watchdog would not retire it either. The system
        // went on ringing a call the app was never told about.
        VoipPush.deliverPushedCall(CallId.random(),
                CallWire.encodeHandle(CallHandle.phone("+14155551212")),
                "Ada", false, false, false, "room-9",
                System.currentTimeMillis());

        final List<PushedCall> got = new ArrayList<PushedCall>();
        VoipPush.setListener(new Collector(got));
        waitFor(got, 1);
        assertEquals("room-9", got.get(0).getData(),
                "a call delivered before the listener existed must survive");
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
    public void aRefusedHoldLeavesTheSimulatedPlatformWhereItWas() {
        // The session half of this is covered above. What was NOT is the
        // simulated platform: it moved the call to the requested state and
        // only then consumed the primed failure, so the bridge reported a
        // state the request had just been told it could not have. No device
        // does that -- CallKit and Telecom either carry an operation out or
        // leave the call exactly as it was -- so a test reading the bridge
        // could confirm behaviour that cannot happen, which is the opposite
        // of what this simulation is for.
        String id = CallId.random();
        CallSession s = ring(id);
        CallAwait.value(s.setHeld(true));
        assertSame(CallState.HELD, bridge.callState(id));

        bridge.primeOperationFailure();
        CallAwait.errorOf(s.setHeld(false));
        assertSame(CallState.HELD, bridge.callState(id),
                "a refused resume must leave the simulated platform held");
        assertSame(CallState.HELD, s.getState(),
                "and the session with it");
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
    public void aFailedSystemMuteActionDoesNotMoveTheSessionFlag() {
        // The other direction from aRefusedMuteDoesNotMoveTheSessionFlag: here
        // the SYSTEM asked, and a listener that fails the action leaves CallKit holding the
        // previous mute state, and Java used to report the one the system had
        // just rejected.
        final List<CallAction> seen = new ArrayList<CallAction>();
        Calls.addActionListener(new CallActionAdapter() {
            public void muteRequested(String callId, boolean muted,
                    CallAction action) {
                action.defer();
                seen.add(action);
            }
        });
        String id = CallId.random();
        CallSession s = ring(id);
        bridge.simulateMute(id, true);
        waitFor(seen, 1);
        seen.get(0).fail();
        assertFalse(s.isMuted(),
                "a failed mute action must not show as muted");
    }

    @Test
    public void aPushedCallWithNoHandleDoesNotAbortTheBatch() {
        // CallHandle rejects an empty value, so the old fallback threw and
        // took every good call queued behind it with it.
        bridge.enqueuePushedCall(CallId.random(), null, "No Handle", false,
                false, null);
        String good = CallId.random();
        bridge.enqueuePushedCall(good, CallHandle.phone("+14155551212"),
                "Ada", false, false, null);
        final List<PushedCall> got = new ArrayList<PushedCall>();
        VoipPush.setListener(new Collector(got));
        waitFor(got, 2);
        assertEquals(2, got.size(),
                "a malformed record must not lose the rest of the drain");
        assertNotNull(Calls.getSession(good));
    }

    @Test
    public void groupingIsRefusedRatherThanSimulated() {
        // No platform lets an app conference two of its own calls, so a
        // simulation that answered success would have apps ship a conference
        // button that works on the desktop and does nothing on a device.
        String a = CallId.random();
        CallSession first = ring(a);
        CallSession second = ring(CallId.random());
        CallAwait.assertFailedWith(CallError.NOT_SUPPORTED,
                first.groupWith(second));
        assertEquals(0, Calls.getCapabilities() & CallBridge.CAPABILITY_GROUPING,
                "and the capability must not be advertised either");
    }

    @Test
    public void aProviderResetFailsWhatWasInFlight() {
        // The provider is gone and the port has dropped the request ids with
        // it, so a report that raced the reset can never be answered -- and
        // this SPI calls an operation that never answers worse than one that
        // fails.
        // Opened directly rather than by racing a simulated report against
        // the reset: what is being asserted is that a request outstanding
        // when the reset arrives is failed, and a race would test the
        // simulation's timing instead.
        int requestId = CallRequests.nextId();
        AsyncResource<Boolean> racing = CallRequests.openAck(requestId);
        Calls.deliverProviderReset();
        Throwable err = CallAwait.errorOf(racing);
        assertNotNull(err, "an in-flight report must not hang after a reset");
        assertTrue(err instanceof CallException, "got " + err);
        assertSame(CallError.PROVIDER_RESET,
                ((CallException) err).getError());
    }

    @Test
    public void aSystemMuteThatCannotBeRefusedStillMoves() {
        // Telecom reports a mute it has already applied. A listener that
        // fails that cannot un-apply it, so the session has to agree with the
        // system rather than with the listener.
        final List<CallAction> seen = new ArrayList<CallAction>();
        Calls.addActionListener(new CallActionAdapter() {
            public void muteRequested(String callId, boolean muted,
                    CallAction action) {
                seen.add(action);
                action.fail();
            }
        });
        String id = CallId.random();
        CallSession s = ring(id);
        Calls.deliverMuteChanged(id, true);
        waitFor(seen, 1);
        assertTrue(s.isMuted(),
                "a mute the platform already applied must show as muted even"
                        + " when the listener failed the action");
    }

    @Test
    public void aDuplicateReportLeavesTheLiveCallAlone() {
        // The registration replaces whatever is under the id, and the
        // handover forgets the id when the platform answers DUPLICATE_CALL --
        // so a duplicate used to take the ORIGINAL live call out of
        // getSessions() with it, leaving a call the system is still showing
        // that nothing in Java can address.
        String id = CallId.random();
        CallSession first = ring(id);
        CallAwait.assertFailedWith(CallError.DUPLICATE_CALL,
                Calls.reportIncoming(id, CallHandle.phone("+14155550000"),
                        "Somebody Else", false));
        assertSame(first, Calls.getSession(id),
                "the live call must survive a duplicate report, unchanged");
        assertEquals(1, Calls.getSessions().length);
    }

    @Test
    public void aThrowingEndListenerStillLetsTheCallGo() {
        // The cleanup is mandatory: without it an ended session stayed in
        // getSessions() addressing a native call that no longer exists, and
        // one badly written listener was enough to do it.
        final List<CallEndReason> seen = new ArrayList<CallEndReason>();
        Calls.addActionListener(new CallActionAdapter() {
            public void callEnded(String callId, CallEndReason reason) {
                seen.add(reason);
                throw new IllegalStateException("a listener that throws");
            }
        });
        String id = CallId.random();
        ring(id);
        bridge.simulateRemoteEnd(id, CallEndReason.REMOTE_ENDED);
        waitFor(seen, 1);
        assertNull(Calls.getSession(id),
                "an ended call must be forgotten even when a listener threw");
        assertEquals(0, Calls.getSessions().length);
    }

    @Test
    public void digitsTheAppSendsComeBackToItsOwnListener() {
        // On iOS sendDigits() submits a CXPlayDTMFCallAction and CallKit
        // hands it straight back through dtmfRequested, which is where an app
        // puts the tone into its media. A port that acknowledged and
        // delivered nothing let that code be written once and do nothing.
        final List<String> played = new ArrayList<String>();
        Calls.addActionListener(new CallActionAdapter() {
            public void dtmfRequested(String callId, String digits,
                    CallAction action) {
                played.add(digits);
            }
        });
        String id = CallId.random();
        CallSession s = ring(id);
        CallAwait.value(s.sendDigits("42#"));
        waitFor(played, 1);
        assertEquals("42#", played.get(0),
                "the digits the app sent must reach its own listener");
    }

    @Test
    public void anActionListenerAloneMakesJavaReady() {
        // The iOS port holds every system-originated action until the facade
        // says Java is listening. Driven only by VoipPush.setListener, an app
        // that used Calls without ever touching pushes -- a foreground
        // signalling call, or an outgoing request from Recents -- had its
        // actions held until CallKit timed them out, with a listener
        // registered the whole time.
        assertFalse(bridge.isJavaReady(),
                "nothing is listening yet");
        CallActionListener l = new CallActionAdapter() { };
        Calls.addActionListener(l);
        assertTrue(bridge.isJavaReady(),
                "an action listener is what listening means for an app that"
                        + " never receives a push");
        Calls.removeActionListener(l);
        assertFalse(bridge.isJavaReady(),
                "and the last one leaving turns it back off");
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
