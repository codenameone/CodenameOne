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
package com.codename1.impl.javase;

import com.codename1.call.CallEndReason;
import com.codename1.call.CallAvailability;
import com.codename1.call.CallHandle;
import com.codename1.call.CallId;
import com.codename1.call.session.CallAudioRoute;
import com.codename1.call.session.CallSession;
import com.codename1.call.session.Calls;
import com.codename1.impl.call.LocalCallBridge;

/// Simulator hooks that script the simulated call stack.
///
/// Registered in `META-INF/codenameone/simulator-hooks.properties`. The
/// labelled ones become a Simulate menu; every one is callable from a test
/// with `CN.execute("call:itemN")`.
///
/// #### These reproduce traps, not happy paths
///
/// A menu item for "everything works" would be worth nothing -- that is what
/// running the app already does. What is worth a click is the second call
/// arriving while the first is up, the system refusing to ring at all, and
/// the audio session that never comes. Each of those is a real device
/// behaviour that an app written against the cheerful path gets wrong, and
/// each is otherwise reachable only by arranging a phone call.
public final class CallSimulatorHooks {

    private static String lastRung;

    private CallSimulatorHooks() {
    }

    private static LocalCallBridge bridge() {
        return JavaSEPort.getSimulatedCalls();
    }

    /// Rings a call the app never reported, the way a VoIP push does.
    public static void ringIncomingCall() {
        String id = CallId.random();
        lastRung = id;
        bridge().enqueuePushedCall(id, CallHandle.phone("+14155551212"),
                "Ada Lovelace", false, false, "simulated");
    }

    /// Rings a second call while one is already up.
    ///
    /// The state machine of an app that assumed one call at a time breaks
    /// here, which is the entire point of the item.
    public static void ringSecondCall() {
        bridge().enqueuePushedCall(CallId.random(),
                CallHandle.phone("+442071838750"), "Grace Hopper", false,
                false, "simulated-second");
    }

    /// Answers the last rung call from the system UI.
    public static void answerCurrentCall() {
        String id = current();
        if (id != null) {
            bridge().simulateAnswer(id);
        }
    }

    /// Hangs up the last rung call from the system UI.
    public static void endCurrentCall() {
        String id = current();
        if (id != null) {
            bridge().simulateEndRequest(id);
        }
    }

    /// The far end hangs up.
    public static void remoteEndCurrentCall() {
        String id = current();
        if (id != null) {
            bridge().simulateRemoteEnd(id, CallEndReason.REMOTE_ENDED);
        }
    }

    /// Makes the next reported call be refused, as though an emergency call
    /// were in progress.
    public static void refuseNextCall() {
        bridge().setAvailability(
                CallAvailability.EMERGENCY_CALL_IN_PROGRESS.ordinal());
    }

    /// Lets calls be reported again.
    public static void allowNextCall() {
        bridge().setAvailability(CallAvailability.AVAILABLE.ordinal());
    }

    /// Stops the audio session ever arriving.
    ///
    /// An app that starts media when the user answers instead of when the
    /// audio session activates works everywhere except a device. This is the
    /// switch that makes the mistake visible here instead.
    public static void withholdAudioSession() {
        bridge().setAudioSessionWithheld(true);
    }

    /// Restores audio session delivery.
    public static void restoreAudioSession() {
        bridge().setAudioSessionWithheld(false);
    }

    /// The system takes every call away.
    public static void resetProvider() {
        lastRung = null;
        bridge().simulateProviderReset();
    }

    /// Queues a call as though it had arrived while the app was not
    /// listening, for the cold-start path.
    public static void queuePushedCall() {
        bridge().enqueuePushedCall(CallId.random(),
                CallHandle.phone("+13105550101"), "Katherine Johnson", false,
                false, "cold-start");
    }

    /// Queues a call that already ended before the app got to it.
    public static void queueStalePushedCall() {
        bridge().enqueuePushedCall(CallId.random(),
                CallHandle.phone("+13105550102"), "Missed Caller", false,
                true, null);
    }

    /// Routes audio to the speaker.
    public static void routeToSpeaker() {
        bridge().simulateRouteChange(CallAudioRoute.SPEAKER);
    }

    /// Sends a keypad digit from the system UI.
    public static void sendDtmfDigit() {
        String id = current();
        if (id != null) {
            bridge().simulateDtmf(id, "5");
        }
    }

    /// Makes the port report no call support at all.
    public static void makeCallsUnsupported() {
        bridge().setSupported(false);
    }

    /// Restores call support.
    public static void makeCallsSupported() {
        bridge().setSupported(true);
    }

    /// The call to act on: whatever the app has, or the last one rung.
    private static String current() {
        CallSession[] sessions = Calls.getSessions();
        if (sessions.length > 0) {
            return sessions[sessions.length - 1].getCallId();
        }
        return lastRung;
    }
}
