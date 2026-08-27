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
package com.codename1.impl.call;

import com.codename1.call.CallEndReason;
import com.codename1.call.CallError;
import com.codename1.call.CallHandle;
import com.codename1.call.CallId;
import com.codename1.call.CallState;
import com.codename1.call.session.CallAudioRoute;
import com.codename1.call.session.Calls;
import com.codename1.call.spi.CallBridge;
import com.codename1.call.voip.VoipPush;
import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A working call stack that talks to nobody.
///
/// This is what the simulator, the desktop builds, the browser port and the
/// unit tests run against. It is a **simulation and not a stub**: calls have
/// state, the state moves on its own, the audio session arrives after the
/// answer rather than with it, and a call reported before anyone was
/// listening is queued and drained later. Every one of those is a real
/// ordering an app has to cope with on a device, and a stub that answered
/// everything instantly would hide all of them until the first device build.
///
/// #### Nothing here completes inline
///
/// A synthetic call that goes `RINGING` to `ACTIVE` in the same stack frame
/// is the single most misleading thing a simulation can do, because app code
/// written against it deadlocks or drops events the moment a real platform
/// takes a millisecond longer. Answers are scheduled through
/// `Display.setTimeout`, or held for a test that wants to drive the clock
/// itself -- see [#setDeferred].
///
/// @hidden not part of the public API.
public class LocalCallBridge implements CallBridge {

    /// How long a simulated platform takes to answer. Long enough that code
    /// which assumes an inline answer breaks here rather than on a device.
    private static final int LATENCY_MILLIS = 40;

    /// How long after an answer the audio session arrives. Deliberately
    /// separate from the answer, because that gap is where media bugs live.
    private static final int AUDIO_MILLIS = 90;

    private final Map<String, SimCall> calls = new HashMap<String, SimCall>();

    private final List<PendingPush> pending = new ArrayList<PendingPush>();

    private List<Runnable> deferred;

    /// Set when the bridge is discarded; see [#retire()]. Guarded by the
    /// bridge itself rather than volatile, which this project's analysers
    /// reject in core.
    private boolean retired;

    private boolean supported = true;
    private boolean voipSupported = true;
    private boolean directorySupported = true;
    private boolean configured;
    private boolean javaReady;
    private boolean directoryEnabled = true;
    private boolean audioWithheld;
    private boolean endFails;
    private boolean failNext;
    private int availability;
    private int grantedPermissions = PERMISSION_MANAGE_CALLS | PERMISSION_MICROPHONE;
    private int route = CallAudioRoute.EARPIECE.ordinal();
    private String directoryPath;
    private long nextToken = 1;

    /// What each outstanding action was asking for, so completeAction can
    /// apply the OUTCOME rather than only record it. Guarded by this.
    ///
    /// A simulation that records the boolean and leaves its own state alone
    /// models the opposite of both devices: a failed end leaves CallKit and
    /// Telecom showing the call, and a failed answer does not leave it
    /// active. Tests written against that exercise behaviour no device has.
    private final Map<Long, PendingSim> simActions = new HashMap<Long, PendingSim>();

    /// One outstanding simulated action.
    private static final class PendingSim {
        private final String callId;
        private final int kind;

        PendingSim(String callId, int kind) {
            this.callId = callId;
            this.kind = kind;
        }
    }

    /// Action kinds completeAction has to undo or finish.
    private static final int SIM_ANSWER = 1;
    private static final int SIM_END = 2;
    private String voipToken = "SIMULATED-VOIP-TOKEN";

    /// One simulated call.
    private static final class SimCall {
        String id;
        CallHandle handle;
        String displayName;
        boolean video;
        boolean incoming;
        boolean muted;
        CallState state;
    }

    /// A call reported before Java was listening.
    private static final class PendingPush {
        String id;
        String handleWire;
        String displayName;
        boolean video;
        boolean stale;
        boolean synthesized;
        String data;
        long receivedAt;
    }

    // ------------------------------------------------------------------
    // test controls
    // ------------------------------------------------------------------

    /// Holds every scheduled answer in `sink` instead of running it, so a
    /// test can decide when the simulated platform replies.
    public void setDeferred(List<Runnable> sink) {
        this.deferred = sink;
    }

    /// Whether this bridge claims call support.
    public void setSupported(boolean value) {
        this.supported = value;
    }

    /// Whether this bridge claims VoIP push support.
    public void setVoipSupported(boolean value) {
        this.voipSupported = value;
    }

    /// Whether this bridge claims directory support.
    public void setDirectorySupported(boolean value) {
        this.directorySupported = value;
    }

    /// Sets the ordinal reported by [#getCallAvailability()], so a test can
    /// reproduce "an emergency call is in progress".
    public void setAvailability(int ordinal) {
        this.availability = ordinal;
    }

    /// Sets the granted permission mask.
    public void setGrantedPermissions(int mask) {
        this.grantedPermissions = mask;
    }

    /// Whether the user has switched caller identification on.
    public void setDirectoryEnabled(boolean value) {
        this.directoryEnabled = value;
    }

    /// Makes the next end request be refused by the simulated platform.
    ///
    /// A real refusal is possible -- CallKit fails a transaction it cannot
    /// carry out -- and an app that assumed ending always works would leave a
    /// live call it had already forgotten about.
    public void primeEndFailure() {
        this.endFails = true;
    }

    /// Makes the next acknowledged operation be refused.
    ///
    /// Broader than [#primeEndFailure]: hold, resume, mute and the keypad can
    /// all be rejected by a real platform -- most ordinarily when the call
    /// ends while the request is in flight -- and code that assumed they
    /// always succeed shows the user a state the system rejected.
    public void primeOperationFailure() {
        this.failNext = true;
    }

    /// Stops the audio session ever being activated.
    ///
    /// The simulation's most useful trap. An app that starts media when the
    /// user answers rather than when the audio session activates behaves
    /// correctly everywhere except a device, where CallKit owns the session
    /// and the call is silent. Withholding it here makes that mistake
    /// reproduce off-device, which is the only place it is cheap to find.
    public void setAudioSessionWithheld(boolean value) {
        this.audioWithheld = value;
    }

    /// The path the last [#setDirectorySource] was given, or null.
    public String getDirectorySource() {
        return directoryPath;
    }

    /// Whether [#configureProvider] has run.
    ///
    /// Worth asserting in a test: on Android an unconfigured provider makes
    /// every reported call vanish silently, and this bridge reproduces that
    /// rather than papering over it.
    public boolean isConfigured() {
        return configured;
    }

    /// What the **simulated platform** believes a call's state is.
    ///
    /// Deliberately separate from the state the facade tracks: a test that
    /// asserts on both is checking that the two agree, which is the thing
    /// that silently stops being true on a real device when a report is
    /// refused and the app keeps its session anyway.
    ///
    /// Answers null when the simulated platform has no such call.
    public CallState getSimulatedState(String callId) {
        SimCall c = find(callId);
        return c == null ? null : c.state;
    }

    /// Who the simulated platform thinks is calling, or null.
    public CallHandle getSimulatedHandle(String callId) {
        SimCall c = find(callId);
        return c == null ? null : c.handle;
    }

    /// The name the simulated platform is showing, or null.
    public String getSimulatedDisplayName(String callId) {
        SimCall c = find(callId);
        return c == null ? null : c.displayName;
    }

    /// Whether the simulated platform has the call muted.
    public boolean isSimulatedMuted(String callId) {
        SimCall c = find(callId);
        return c != null && c.muted;
    }

    /// Whether the simulated platform considers the call a video call.
    public boolean isSimulatedVideo(String callId) {
        SimCall c = find(callId);
        return c != null && c.video;
    }

    /// Whether the simulated platform considers the call incoming.
    public boolean isSimulatedIncoming(String callId) {
        SimCall c = find(callId);
        return c != null && c.incoming;
    }

    /// Queues a call as though it had arrived as a VoIP push while the app
    /// was not listening.
    ///
    /// This is how the cold-start path is tested without a device: queue one
    /// or more, then let the facade drain them.
    public void enqueuePushedCall(String callId, CallHandle handle,
            String displayName, boolean video, boolean stale, String data) {
        PendingPush p = new PendingPush();
        p.id = CallId.normalize(callId);
        if (p.id == null) {
            // Exactly what the native side does: report something rather than
            // let the platform kill the app, and flag it so the server bug is
            // findable.
            p.id = CallId.random();
            p.synthesized = true;
        }
        p.handleWire = CallWire.encodeHandle(handle);
        p.displayName = displayName;
        p.video = video;
        p.stale = stale;
        p.data = data;
        p.receivedAt = System.currentTimeMillis();
        synchronized (calls) {
            pending.add(p);
            if (!p.stale) {
                SimCall c = new SimCall();
                c.id = p.id;
                c.handle = handle;
                c.displayName = displayName;
                c.video = video;
                c.incoming = true;
                c.state = CallState.RINGING;
                calls.put(c.id, c);
            }
        }
        if (javaReady) {
            drainPendingCalls(-1);
        }
    }

    /// Simulates the user answering through the system UI.
    public void simulateAnswer(String callId) {
        final SimCall c = find(callId);
        if (c == null) {
            return;
        }
        c.state = CallState.ACTIVE;
        final long token = nextToken();
        synchronized (this) {
            simActions.put(Long.valueOf(token), new PendingSim(callId, SIM_ANSWER));
        }
        later(LATENCY_MILLIS, new AnswerDelivery(callId, token));
        // NO audio here. Both platforms activate the session only once the
        // answer action has been fulfilled, and a listener that defers for
        // longer than the gap between these two timers used to get
        // audioSessionActivated before it had accepted the call -- and, if it
        // then FAILED the answer, no deactivation either. A simulator test
        // could start and keep media for a call the app never took.
    }

    /// Simulates the user hanging up through the system UI.
    public void simulateEndRequest(String callId) {
        SimCall c = find(callId);
        if (c == null) {
            return;
        }
        // NOT ended yet: the user has ASKED. Both platforms keep the call up
        // until the action is fulfilled, and restore it when it is failed.
        final long token = nextToken();
        synchronized (this) {
            simActions.put(Long.valueOf(token), new PendingSim(callId, SIM_END));
        }
        later(LATENCY_MILLIS, new EndDelivery(callId, token));
    }

    /// Simulates the far end hanging up.
    public void simulateRemoteEnd(String callId, CallEndReason reason) {
        SimCall c = find(callId);
        if (c == null) {
            return;
        }
        c.state = CallState.ENDED;
        remove(callId);
        later(LATENCY_MILLIS, new EndedDelivery(callId,
                reason == null ? CallEndReason.REMOTE_ENDED.ordinal()
                        : reason.ordinal()));
        later(LATENCY_MILLIS, new AudioDelivery(callId, route, false));
    }

    /// Simulates the system taking every call away.
    public void simulateProviderReset() {
        synchronized (calls) {
            calls.clear();
            pending.clear();
        }
        configured = false;
        later(LATENCY_MILLIS, new ResetDelivery());
    }

    /// Simulates the audio route changing.
    public void simulateRouteChange(CallAudioRoute newRoute) {
        this.route = newRoute == null
                ? CallAudioRoute.UNKNOWN.ordinal() : newRoute.ordinal();
    }

    /// Simulates the user muting or unmuting through the system UI.
    public void simulateMute(String callId, boolean muted) {
        later(LATENCY_MILLIS, new MuteDelivery(callId, muted, nextToken()));
    }

    /// Simulates the user typing on the system keypad.
    public void simulateDtmf(String callId, String digits) {
        later(LATENCY_MILLIS, new DtmfDelivery(callId, digits, nextToken()));
    }

    /// Simulates the system asking this app to place a call.
    public void simulateStartCallRequest(String callId, CallHandle handle,
            boolean video) {
        later(LATENCY_MILLIS, new StartDelivery(callId,
                CallWire.encodeHandle(handle), video, nextToken()));
    }

    // ------------------------------------------------------------------
    // CallBridge
    // ------------------------------------------------------------------

    @Override
    public boolean isCallSupported() {
        return supported;
    }

    @Override
    public boolean isVoipPushSupported() {
        return supported && voipSupported;
    }

    @Override
    public boolean isDirectorySupported() {
        return supported && directorySupported;
    }

    @Override
    public int getCallCapabilities() {
        if (!supported) {
            return 0;
        }
        int caps = CAPABILITY_SYSTEM_UI | CAPABILITY_OUTGOING | CAPABILITY_HOLD
                | CAPABILITY_MUTE | CAPABILITY_DTMF
                | CAPABILITY_VIDEO;
        if (voipSupported) {
            caps |= CAPABILITY_VOIP_PUSH;
        }
        if (directorySupported) {
            caps |= CAPABILITY_DIRECTORY | CAPABILITY_SCREENING;
        }
        return caps;
    }

    @Override
    public int getCallAvailability() {
        return supported ? availability : 4;
    }

    @Override
    public int getGrantedPermissions() {
        return grantedPermissions;
    }

    @Override
    public void requestPermissions(int requestId, int permissionBits) {
        grantedPermissions |= permissionBits;
        later(LATENCY_MILLIS, new PermissionDelivery(requestId, grantedPermissions));
    }

    @Override
    public void configureProvider(int requestId, String configWire) {
        configured = true;
        ok(requestId);
    }

    @Override
    public void reportIncomingCall(int requestId, String callId,
            String handleWire, String displayName, int capabilityBits,
            boolean hasVideo) {
        report(requestId, callId, handleWire, displayName, hasVideo, true);
    }

    @Override
    public void reportOutgoingCall(int requestId, String callId,
            String handleWire, String displayName, int capabilityBits,
            boolean hasVideo) {
        report(requestId, callId, handleWire, displayName, hasVideo, false);
    }

    private void report(int requestId, String callId, String handleWire,
            String displayName, boolean video, boolean incoming) {
        if (!supported) {
            fail(requestId, CallError.NOT_SUPPORTED, null);
            return;
        }
        if (!configured) {
            // The Android failure this simulation exists to surface: without a
            // registered PhoneAccount the platform ignores the call and says
            // nothing. Answering with an error is a deliberate improvement on
            // reproducing the silence, because the silence is untestable.
            fail(requestId, CallError.CALL_REFUSED,
                    "Calls.configure() must run before a call is reported");
            return;
        }
        if (availability != 0) {
            fail(requestId, CallError.CALL_REFUSED,
                    "The system will not ring a call right now");
            return;
        }
        synchronized (calls) {
            if (calls.containsKey(callId)) {
                fail(requestId, CallError.DUPLICATE_CALL,
                        "A call with that id already exists");
                return;
            }
            SimCall c = new SimCall();
            c.id = callId;
            c.handle = CallWire.decodeHandle(handleWire);
            c.displayName = displayName;
            c.video = video;
            c.incoming = incoming;
            c.state = incoming ? CallState.RINGING : CallState.DIALING;
            calls.put(callId, c);
        }
        ok(requestId);
    }

    @Override
    public void reportOutgoingStartedConnecting(String callId, long timestampMs) {
        SimCall c = find(callId);
        if (c != null) {
            c.state = CallState.DIALING;
        }
    }

    @Override
    public void reportOutgoingConnected(String callId, long timestampMs) {
        connected(callId);
    }

    @Override
    public void reportIncomingConnected(String callId, long timestampMs) {
        connected(callId);
    }

    private void connected(String callId) {
        SimCall c = find(callId);
        if (c == null) {
            return;
        }
        c.state = CallState.ACTIVE;
        audioOn(callId);
    }

    @Override
    public void updateCall(String callId, String handleWire, String displayName,
            int capabilityBits, boolean hasVideo) {
        SimCall c = find(callId);
        if (c == null) {
            return;
        }
        if (handleWire != null && handleWire.length() > 0) {
            CallHandle h = CallWire.decodeHandle(handleWire);
            if (h != null) {
                c.handle = h;
            }
        }
        if (displayName != null) {
            c.displayName = displayName;
        }
    }

    @Override
    public void reportCallEnded(String callId, int endReasonOrdinal,
            long timestampMs) {
        remove(callId);
    }

    @Override
    public void endCall(int requestId, String callId, int endReasonOrdinal) {
        if (find(callId) == null) {
            fail(requestId, CallError.INVALID_ID, "No such call: " + callId);
            return;
        }
        if (endFails) {
            endFails = false;
            fail(requestId, CallError.BUSY,
                    "The simulated platform refused to end the call");
            return;
        }
        remove(callId);
        later(LATENCY_MILLIS, new AudioDelivery(callId, route, false));
        ok(requestId);
    }

    @Override
    public void setHeld(int requestId, String callId, boolean held) {
        SimCall c = find(callId);
        if (c == null) {
            fail(requestId, CallError.INVALID_ID, "No such call: " + callId);
            return;
        }
        c.state = held ? CallState.HELD : CallState.ACTIVE;
        ok(requestId);
    }

    @Override
    public void setMuted(int requestId, String callId, boolean muted) {
        SimCall c = find(callId);
        if (c == null) {
            fail(requestId, CallError.INVALID_ID, "No such call: " + callId);
            return;
        }
        c.muted = muted;
        ok(requestId);
    }

    @Override
    public void sendDtmf(int requestId, String callId, String digits) {
        if (find(callId) == null) {
            fail(requestId, CallError.INVALID_ID, "No such call: " + callId);
            return;
        }
        ok(requestId);
        // The round trip CallKit performs: an app puts the tone into its own
        // media when dtmfRequested fires, so a simulation that acknowledged
        // and delivered nothing let that code be written and never run until
        // a device.
        later(LATENCY_MILLIS, new DtmfPlayback(callId, digits));
    }

    @Override
    public void setCallGroup(int requestId, String callId, String otherCallId) {
        if (find(callId) == null) {
            fail(requestId, CallError.INVALID_ID, "No such call: " + callId);
            return;
        }
        // Refused rather than simulated, because no device does it. A
        // simulation that answered success here would have apps build a
        // conference button that works on the desktop and silently does
        // nothing on both platforms -- which is worse than not offering it.
        fail(requestId, CallError.NOT_SUPPORTED,
                "No platform lets an app conference its own calls");
    }

    @Override
    public int getAudioRoute() {
        return route;
    }

    @Override
    public void setAudioRoute(int requestId, int routeOrdinal) {
        this.route = routeOrdinal;
        ok(requestId);
    }

    @Override
    public void showAudioRoutePicker(int requestId, String callId) {
        // Refused, not simulated. No real platform has a system call route
        // picker, and a simulation that opened one would let an app ship code
        // that silently does nothing on every device.
        fail(requestId, CallError.NOT_SUPPORTED,
                "No platform offers a system call audio route picker");
    }

    @Override
    public boolean completeAction(long actionToken, boolean fulfilled) {
        // Nothing to time out against here; the simulation's purpose is to
        // let the facade's own answer-exactly-once logic be exercised. The
        // outcome is recorded so a test can tell fulfilled from failed --
        // which is the whole difference between an action the app handled and
        // a system start it ignored.
        PendingSim done;
        synchronized (this) {
            lastActionFulfilled = Boolean.valueOf(fulfilled);
            done = simActions.remove(Long.valueOf(actionToken));
        }
        if (done == null) {
            // Not held any more: the simulated platform has already timed
            // this action out, exactly as CallKit does after ~5s.
            return false;
        }
        SimCall c = find(done.callId);
        if (c == null) {
            return true;
        }
        if (done.kind == SIM_ANSWER) {
            if (fulfilled) {
                c.state = CallState.ACTIVE;
                // THE moment media may start, which is what both platforms
                // say and what the facade documents.
                audioOn(done.callId);
            } else {
                // A refused answer ends the call on both platforms; there is
                // nothing else the system can do with a call the app will not
                // take.
                c.state = CallState.ENDED;
                forgetSimCall(done.callId);
            }
            return true;
        }
        if (done.kind == SIM_END) {
            if (fulfilled) {
                c.state = CallState.ENDED;
                forgetSimCall(done.callId);
            }
            // A failed end leaves the call up, which is what CallKit and
            // Telecom both do -- the app said it could not hang up.
        }
        return true;
    }

    /// Drops a call from the simulated platform's own book.
    private void forgetSimCall(String callId) {
        synchronized (calls) {
            calls.remove(callId);
        }
    }

    /// Drops every outstanding action, as the platform does on a timeout.
    ///
    /// CallKit gives an action about five seconds and then abandons it; a
    /// Telecom connection can be torn down under one just as abruptly. This
    /// makes that reachable without waiting, so the facade's behaviour when
    /// it answers an action the platform no longer holds can be tested.
    ///
    /// @hidden not part of the public API; test-only.
    public void expireOutstandingActions() {
        synchronized (this) {
            simActions.clear();
        }
    }

    /// How the facade last answered an action, or null if it has not.
    ///
    /// @hidden not part of the public API; test-only.
    public Boolean getLastActionFulfilled() {
        synchronized (this) {
            return lastActionFulfilled;
        }
    }

    /// See [#getLastActionFulfilled]. Guarded by this.
    private Boolean lastActionFulfilled;

    @Override
    public void registerVoipPush(int requestId) {
        if (!isVoipPushSupported()) {
            failString(requestId, CallError.NOT_SUPPORTED, null);
            return;
        }
        later(LATENCY_MILLIS, new TokenDelivery(requestId, voipToken));
    }

    @Override
    public void unregisterVoipPush(int requestId) {
        voipToken = null;
    }

    @Override
    public void setJavaReady(boolean ready) {
        this.javaReady = ready;
    }

    /// Whether the facade has told this bridge that Java is listening.
    ///
    /// The iOS port holds system-originated actions until this is true, so a
    /// test can assert that an ordinary Calls listener is enough to turn it
    /// on -- without VoipPush, which an app that never receives pushes has no
    /// reason to touch.
    public boolean isJavaReady() {
        return javaReady;
    }

    @Override
    public void drainPendingCalls(int requestId) {
        List<PendingPush> batch;
        synchronized (calls) {
            batch = new ArrayList<PendingPush>(pending);
            pending.clear();
        }
        for (PendingPush p : batch) {
            later(LATENCY_MILLIS, new PushDelivery(p));
        }
        if (requestId >= 0) {
            later(LATENCY_MILLIS, new DrainedDelivery(requestId, batch.size()));
        }
    }

    @Override
    public void setDirectorySource(int requestId, String filePath) {
        if (!isDirectorySupported()) {
            fail(requestId, CallError.NOT_SUPPORTED, null);
            return;
        }
        this.directoryPath = filePath;
        ok(requestId);
    }

    @Override
    public void reloadDirectory(int requestId) {
        if (!isDirectorySupported()) {
            fail(requestId, CallError.NOT_SUPPORTED, null);
            return;
        }
        if (directoryPath == null) {
            fail(requestId, CallError.DIRECTORY_FAILED,
                    "No directory source has been installed");
            return;
        }
        ok(requestId);
    }

    @Override
    public void getDirectoryStatus(int requestId) {
        later(LATENCY_MILLIS, new StringDelivery(requestId, CallWire.join(
                new String[]{CallWire.flagOf(directoryEnabled),
                    String.valueOf(directoryPath == null ? -1 : 0),
                    "simulated"})));
    }

    @Override
    public void requestScreeningRole(int requestId) {
        ok(requestId);
    }

    // ------------------------------------------------------------------
    // internals
    // ------------------------------------------------------------------

    /// Activates the audio session unless the trap switch is on.
    private void audioOn(String callId) {
        if (audioWithheld) {
            return;
        }
        later(AUDIO_MILLIS, new AudioDelivery(callId, route, true));
    }

    /// The state the simulated PLATFORM has this call in, or null when it
    /// has no such call.
    ///
    /// Presence alone is not enough to tell the interesting cases apart: a
    /// refused end leaves the call present on both sides, and what a test
    /// needs to know is whether the platform still considers it live.
    ///
    /// @hidden not part of the public API; test-only.
    public CallState callState(String callId) {
        SimCall c = find(callId);
        return c == null ? null : c.state;
    }

    /// Whether the simulated PLATFORM still holds this call.
    ///
    /// Distinct from Calls.getSession: a test that inspects only the facade
    /// cannot tell whether the two agree, and the cases worth simulating are
    /// exactly the ones where they might not.
    ///
    /// @hidden not part of the public API; test-only.
    public boolean hasCall(String callId) {
        return find(callId) != null;
    }

    private SimCall find(String callId) {
        synchronized (calls) {
            return calls.get(callId);
        }
    }

    private void remove(String callId) {
        synchronized (calls) {
            calls.remove(callId);
        }
    }

    private synchronized long nextToken() {
        return nextToken++;
    }

    private void ok(int requestId) {
        if (failNext) {
            failNext = false;
            fail(requestId, CallError.BUSY,
                    "The simulated platform refused the request");
            return;
        }
        later(LATENCY_MILLIS, new AckDelivery(requestId, true, 0, null));
    }

    private void fail(int requestId, CallError e, String message) {
        later(LATENCY_MILLIS,
                new AckDelivery(requestId, false, e.ordinal(), message));
    }

    private void failString(int requestId, CallError e, String message) {
        later(LATENCY_MILLIS,
                new RegistrationFailure(requestId, e.ordinal(), message));
    }

    private void later(int millis, Runnable delivery) {
        List<Runnable> sink = deferred;
        if (sink != null) {
            // A test is driving the clock. Held until it says otherwise, so
            // the delayed ordering the simulation exists to reproduce can be
            // reproduced in a unit test too.
            sink.add(delivery);
            return;
        }
        if (Display.isInitialized()) {
            // Wrapped so a delivery scheduled by a finished test cannot fire
            // into the next one. Display.setTimeout hands back nothing to
            // cancel, and a stale answer arriving while another test is
            // starting its Display is not a failure anyone can read: the
            // symptom is that test timing out with the EDT not yet up.
            Display.getInstance().setTimeout(millis,
                    new Retired(this, delivery));
            return;
        }
        // No Display, so this is a unit test driving the bridge directly.
        delivery.run();
    }

    /// Drops every delivery this bridge has scheduled and refuses more.
    ///
    /// Called when the facade is reset -- which is what a test's teardown
    /// does -- because Display.setTimeout cannot be cancelled and the
    /// simulation is deliberately slow enough that answers outlive a short
    /// test.
    ///
    /// @hidden not part of the public API; test-only.
    public void retire() {
        synchronized (this) {
            retired = true;
        }
    }

    /// Whether this bridge has been discarded.
    boolean isRetired() {
        synchronized (this) {
            return retired;
        }
    }

    /// A scheduled delivery that checks its bridge is still in use.
    private static final class Retired implements Runnable {
        private final LocalCallBridge bridge;
        private final Runnable delivery;

        Retired(LocalCallBridge bridge, Runnable delivery) {
            this.bridge = bridge;
            this.delivery = delivery;
        }

        @Override
        public void run() {
            if (bridge.isRetired()) {
                return;
            }
            delivery.run();
        }
    }

    // ------------------------------------------------------------------
    // deliveries
    // ------------------------------------------------------------------
    // Named static classes rather than anonymous ones: an anonymous class
    // here holds a synthetic reference to the bridge, which SpotBugs reports
    // and which keeps a finished call alive.

    private static final class AckDelivery implements Runnable {
        private final int requestId;
        private final boolean ok;
        private final int error;
        private final String message;

        AckDelivery(int requestId, boolean ok, int error, String message) {
            this.requestId = requestId;
            this.ok = ok;
            this.error = error;
            this.message = message;
        }

        @Override
        public void run() {
            Calls.deliverAck(requestId, ok, error, message);
        }
    }

    private static final class PermissionDelivery implements Runnable {
        private final int requestId;
        private final int mask;

        PermissionDelivery(int requestId, int mask) {
            this.requestId = requestId;
            this.mask = mask;
        }

        @Override
        public void run() {
            Calls.deliverPermissionResult(requestId, mask);
        }
    }

    private static final class AnswerDelivery implements Runnable {
        private final String callId;
        private final long token;

        AnswerDelivery(String callId, long token) {
            this.callId = callId;
            this.token = token;
        }

        @Override
        public void run() {
            Calls.deliverAnswer(callId, token);
        }
    }

    private static final class EndDelivery implements Runnable {
        private final String callId;
        private final long token;

        EndDelivery(String callId, long token) {
            this.callId = callId;
            this.token = token;
        }

        @Override
        public void run() {
            Calls.deliverEndRequest(callId, token);
        }
    }

    private static final class EndedDelivery implements Runnable {
        private final String callId;
        private final int reason;

        EndedDelivery(String callId, int reason) {
            this.callId = callId;
            this.reason = reason;
        }

        @Override
        public void run() {
            Calls.deliverCallEnded(callId, reason);
        }
    }

    private static final class AudioDelivery implements Runnable {
        private final String callId;
        private final int route;
        private final boolean on;

        AudioDelivery(String callId, int route, boolean on) {
            this.callId = callId;
            this.route = route;
            this.on = on;
        }

        @Override
        public void run() {
            if (on) {
                Calls.deliverAudioActivated(callId, route);
            } else {
                Calls.deliverAudioDeactivated(callId);
            }
        }
    }

    private static final class MuteDelivery implements Runnable {
        private final String callId;
        private final boolean muted;
        private final long token;

        MuteDelivery(String callId, boolean muted, long token) {
            this.callId = callId;
            this.muted = muted;
            this.token = token;
        }

        @Override
        public void run() {
            Calls.deliverMute(callId, muted, token);
        }
    }

    private static final class DtmfPlayback implements Runnable {
        private final String callId;
        private final String digits;

        DtmfPlayback(String callId, String digits) {
            this.callId = callId;
            this.digits = digits;
        }

        @Override
        public void run() {
            Calls.deliverDtmfPlayed(callId, digits);
        }
    }

    private static final class DtmfDelivery implements Runnable {
        private final String callId;
        private final String digits;
        private final long token;

        DtmfDelivery(String callId, String digits, long token) {
            this.callId = callId;
            this.digits = digits;
            this.token = token;
        }

        @Override
        public void run() {
            Calls.deliverDtmf(callId, digits, token);
        }
    }

    private static final class StartDelivery implements Runnable {
        private final String callId;
        private final String handleWire;
        private final boolean video;
        private final long token;

        StartDelivery(String callId, String handleWire, boolean video, long token) {
            this.callId = callId;
            this.handleWire = handleWire;
            this.video = video;
            this.token = token;
        }

        @Override
        public void run() {
            Calls.deliverStartCallRequest(callId, handleWire, video, token);
        }
    }

    private static final class ResetDelivery implements Runnable {
        @Override
        public void run() {
            Calls.deliverProviderReset();
        }
    }

    private static final class TokenDelivery implements Runnable {
        private final int requestId;
        private final String token;

        TokenDelivery(int requestId, String token) {
            this.requestId = requestId;
            this.token = token;
        }

        @Override
        public void run() {
            VoipPush.deliverToken(requestId, token);
        }
    }

    private static final class RegistrationFailure implements Runnable {
        private final int requestId;
        private final int error;
        private final String message;

        RegistrationFailure(int requestId, int error, String message) {
            this.requestId = requestId;
            this.error = error;
            this.message = message;
        }

        @Override
        public void run() {
            VoipPush.deliverRegistrationFailed(requestId, error, message);
        }
    }

    private static final class PushDelivery implements Runnable {
        private final PendingPush p;

        PushDelivery(PendingPush p) {
            this.p = p;
        }

        @Override
        public void run() {
            VoipPush.deliverPushedCall(p.id, p.handleWire, p.displayName,
                    p.video, p.stale, p.synthesized, p.data, p.receivedAt);
        }
    }

    private static final class DrainedDelivery implements Runnable {
        private final int requestId;
        private final int count;

        DrainedDelivery(int requestId, int count) {
            this.requestId = requestId;
            this.count = count;
        }

        @Override
        public void run() {
            VoipPush.deliverPendingCallsDrained(requestId, count);
        }
    }

    private static final class StringDelivery implements Runnable {
        private final int requestId;
        private final String value;

        StringDelivery(int requestId, String value) {
            this.requestId = requestId;
            this.value = value;
        }

        @Override
        public void run() {
            com.codename1.call.directory.CallDirectory.deliverStatus(requestId, value);
        }
    }
}
