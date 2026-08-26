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
package com.codename1.call.session;

import com.codename1.call.CallAvailability;
import com.codename1.call.CallDirection;
import com.codename1.call.CallEndReason;
import com.codename1.call.CallError;
import com.codename1.io.Log;
import com.codename1.call.CallException;
import com.codename1.call.CallHandle;
import com.codename1.call.CallId;
import com.codename1.call.CallState;
import com.codename1.call.spi.CallBridge;
import com.codename1.impl.async.EdtResult;
import com.codename1.impl.call.CallRequests;
import com.codename1.impl.call.CallWire;
import com.codename1.util.AsyncResource;
import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// System call integration: the entry point for reporting calls to the
/// operating system and hearing what the user does with them.
///
/// ```java
/// if (!Calls.isSupported()) {
///     return;                      // fall back to an in-app call screen
/// }
/// Calls.configure(new CallConfiguration().displayName("Acme Talk"));
/// Calls.addActionListener(new CallActionAdapter() {
///     public void answerRequested(String callId, CallAction action) {
///         signalling.accept(callId);
///     }
///     public void audioSessionActivated(CallAudioSession session) {
///         media.start();           // here, not in answerRequested
///     }
///     public void providerReset() {
///         media.stopEverything();
///     }
/// });
/// ```
///
/// Everything here is static because there is one operating system and one
/// call provider per app.
///
/// #### Order matters
///
/// [#configure] must run before any call is reported. On Android an
/// unconfigured provider makes `TelecomManager` ignore reported calls
/// silently.
///
/// #### Every callback arrives on the EDT
///
/// Unlike some other families in this framework, that is true here without
/// exception: an action from the system is a user-interface event and there
/// is nothing to gain by delivering it anywhere else.
public final class Calls {

    private static final List<CallActionListener> LISTENERS =
            new ArrayList<CallActionListener>();

    private static final Map<String, CallSession> SESSIONS =
            new HashMap<String, CallSession>();

    private Calls() {
    }

    // ------------------------------------------------------------------
    // capability
    // ------------------------------------------------------------------

    /// Whether this platform can show a call in its own call UI.
    ///
    /// False on the ports that have no such concept, and on Android below
    /// API 26. An app must have a fallback; there is no way to make a
    /// desktop show a lock-screen call.
    public static boolean isSupported() {
        CallBridge b = CallRequests.bridge();
        return b != null && b.isCallSupported();
    }

    /// The `CallBridge.CAPABILITY_*` mask this platform supports. Branch on
    /// this rather than on the platform.
    public static int getCapabilities() {
        CallBridge b = CallRequests.bridge();
        return b == null ? 0 : b.getCallCapabilities();
    }

    /// Whether a call could be rung **right now**.
    ///
    /// Different from [#isSupported()]: an emergency call, or another app's
    /// call, makes this refuse on a platform that supports calling
    /// perfectly. Check it before telling a caller their call is ringing.
    public static CallAvailability getAvailability() {
        CallBridge b = CallRequests.bridge();
        if (b == null) {
            return CallAvailability.UNSUPPORTED;
        }
        CallAvailability[] values = CallAvailability.values();
        int o = b.getCallAvailability();
        return o < 0 || o >= values.length ? CallAvailability.UNSUPPORTED : values[o];
    }

    /// The `CallBridge.PERMISSION_*` mask currently granted.
    public static int getGrantedPermissions() {
        CallBridge b = CallRequests.bridge();
        return b == null ? 0 : b.getGrantedPermissions();
    }

    /// Asks for the `CallBridge.PERMISSION_*` bits in `permissions`,
    /// resolving with the mask actually granted.
    public static AsyncResource<Integer> requestPermissions(int permissions) {
        CallBridge b = CallRequests.bridge();
        if (b == null) {
            EdtResult<Integer> r = new EdtResult<Integer>();
            r.error(new CallException(CallError.NOT_SUPPORTED));
            return r;
        }
        int id = CallRequests.nextId();
        EdtResult<Integer> r = CallRequests.openPermissionRequest(id);
        b.requestPermissions(id, permissions);
        return r;
    }

    // ------------------------------------------------------------------
    // configuration
    // ------------------------------------------------------------------

    /// Installs the calling identity. Must precede any reported call.
    public static AsyncResource<Boolean> configure(CallConfiguration config) {
        CallBridge b = CallRequests.bridge();
        if (b == null) {
            return unsupported();
        }
        int id = CallRequests.nextId();
        EdtResult<Boolean> r = CallRequests.openAck(id);
        b.configureProvider(id, (config == null
                ? new CallConfiguration() : config).toWire());
        return r;
    }

    // ------------------------------------------------------------------
    // reporting
    // ------------------------------------------------------------------

    /// Rings an incoming call.
    ///
    /// `callId` must be a canonical [CallId] and must be the same identifier
    /// the far end uses. Allocate one with [CallId#random()] for a call
    /// learned over the app's own connection; a call that arrived as a VoIP
    /// push already has one and must not be re-reported here -- see
    /// `com.codename1.call.voip.VoipPush`.
    public static AsyncResource<CallSession> reportIncoming(String callId,
            CallHandle handle, String displayName, boolean video) {
        return report(callId, handle, displayName, video, CallDirection.INCOMING);
    }

    /// Places an outgoing call in the system UI.
    public static AsyncResource<CallSession> reportOutgoing(String callId,
            CallHandle handle, String displayName, boolean video) {
        return report(callId, handle, displayName, video, CallDirection.OUTGOING);
    }

    private static AsyncResource<CallSession> report(String callId,
            CallHandle handle, String displayName, boolean video,
            CallDirection direction) {
        final EdtResult<CallSession> out = new EdtResult<CallSession>();
        CallBridge b = CallRequests.bridge();
        if (b == null) {
            out.error(new CallException(CallError.NOT_SUPPORTED));
            return out;
        }
        String id = CallId.normalize(callId);
        if (id == null) {
            out.error(new CallException(CallError.INVALID_ID,
                    "Not a canonical call id: " + callId));
            return out;
        }
        if (handle == null) {
            out.error(new CallException(CallError.INVALID_ID,
                    "A call handle is required"));
            return out;
        }
        // Refused here rather than by the platform. The registration below
        // replaces whatever is registered under this id, and the handover
        // forgets the id when the platform answers DUPLICATE_CALL -- so a
        // duplicate report used to take the ORIGINAL live call out of
        // getSessions() with it, leaving a call the system is still showing
        // that nothing in Java can address.
        // ONE critical section for the test and the reservation. Split, two
        // threads reporting the same id -- socket signalling racing a pushed
        // call is the ordinary way -- both passed the test and both inserted,
        // so two CallSession objects existed for one call with only the
        // second reachable, and the port's request parked under that id was
        // overwritten with one AsyncResource left unanswered.
        CallSession session = new CallSession(id, direction, handle, displayName,
                direction == CallDirection.INCOMING
                        ? CallState.RINGING : CallState.DIALING);
        synchronized (SESSIONS) {
            // Only live calls are in the map; forget() removes an ended one,
            // so presence is the whole test.
            if (SESSIONS.containsKey(id)) {
                out.error(new CallException(CallError.DUPLICATE_CALL,
                        "A call with this id is already live: " + id));
                return out;
            }
            SESSIONS.put(id, session);
        }
        int reqId = CallRequests.nextId();
        EdtResult<Boolean> ack = CallRequests.openAck(reqId);
        // The session is only handed over once the system has accepted it,
        // so an app can never act on a call that was refused.
        ack.onResult(new SessionHandover(out, session, id));
        String wire = CallWire.encodeHandle(handle);
        if (direction == CallDirection.INCOMING) {
            b.reportIncomingCall(reqId, id, wire, displayName, -1, video);
        } else {
            b.reportOutgoingCall(reqId, id, wire, displayName, -1, video);
        }
        return out;
    }

    /// Tells one listener a call ended, without letting it stop the others.
    ///
    /// The action arms deliberately let a listener's exception propagate:
    /// there is a request behind them, the caller is entitled to see the
    /// failure, and the token is settled in a `finally` either way. This is a
    /// notification -- nobody is waiting on an answer, every listener is
    /// entitled to hear it, and on a port the thread carrying it is a native
    /// callback thread that must not be taken down by application code. All
    /// FOUR notification arms are handled this way: ended, providerReset and
    /// both halves of the audio session.
    private static void tell(CallActionListener l, String callId,
            CallEndReason reason) {
        try {
            l.callEnded(callId, reason);
        } catch (Throwable t) {
            report(t);
        }
    }

    /// Tells one listener the audio session is live. See [#tell]: this is the
    /// notification an app starts its media from, so a listener that throws
    /// must not leave the ones after it with a silent call.
    private static void tellAudioOn(CallActionListener l, CallAudioSession s) {
        try {
            l.audioSessionActivated(s);
        } catch (Throwable t) {
            report(t);
        }
    }

    /// Tells one listener the audio session is gone. See [#tellAudioOn]:
    /// stopping early here leaves a media engine running.
    private static void tellAudioOff(CallActionListener l, String callId) {
        try {
            l.audioSessionDeactivated(callId);
        } catch (Throwable t) {
            report(t);
        }
    }

    /// Tells one listener the provider reset. See [#tell] for why this one
    /// swallows.
    private static void tellReset(CallActionListener l) {
        try {
            l.providerReset();
        } catch (Throwable t) {
            report(t);
        }
    }

    /// Records a listener's exception without depending on a Display.
    ///
    /// Log.e goes through the platform implementation, which is null before
    /// Display.init -- so logging a throw from a listener registered that
    /// early replaced it with an NPE from the logger. There is no log to
    /// write to at that point; the stack trace is all there is.
    private static void report(Throwable t) {
        if (Display.isInitialized()) {
            Log.e(t);
        } else {
            t.printStackTrace(); //NOPMD AvoidPrintStackTrace
        }
    }

    /// Completes the caller's resource once the system has accepted or
    /// refused the call. A static class rather than an anonymous one so it
    /// holds no synthetic reference to its enclosing scope.
    private static final class SessionHandover
            implements com.codename1.util.AsyncResult<Boolean> {
        private final EdtResult<CallSession> out;
        private final CallSession session;
        private final String id;

        SessionHandover(EdtResult<CallSession> out, CallSession session, String id) {
            this.out = out;
            this.session = session;
            this.id = id;
        }

        @Override
        public void onReady(Boolean value, Throwable error) {
            if (error != null) {
                forget(id);
                out.error(error);
            } else {
                out.complete(session);
            }
        }
    }

    /// The session for a call id, or null when this app has none.
    public static CallSession getSession(String callId) {
        String id = CallId.normalize(callId);
        if (id == null) {
            return null;
        }
        synchronized (SESSIONS) {
            return SESSIONS.get(id);
        }
    }

    /// Every call this app currently has.
    public static CallSession[] getSessions() {
        synchronized (SESSIONS) {
            return SESSIONS.values().toArray(new CallSession[SESSIONS.size()]);
        }
    }

    // ------------------------------------------------------------------
    // audio
    // ------------------------------------------------------------------

    /// Where call audio is going right now.
    public static CallAudioRoute getAudioRoute() {
        CallBridge b = CallRequests.bridge();
        if (b == null) {
            return CallAudioRoute.UNKNOWN;
        }
        return route(b.getAudioRoute());
    }

    /// Asks for a particular audio route.
    public static AsyncResource<Boolean> setAudioRoute(CallAudioRoute r) {
        CallBridge b = CallRequests.bridge();
        if (b == null) {
            return unsupported();
        }
        int id = CallRequests.nextId();
        EdtResult<Boolean> res = CallRequests.openAck(id);
        b.setAudioRoute(id, r == null ? CallAudioRoute.UNKNOWN.ordinal() : r.ordinal());
        return res;
    }

    /// Shows the system's audio route picker for a call.
    public static AsyncResource<Boolean> showAudioRoutePicker(String callId) {
        CallBridge b = CallRequests.bridge();
        if (b == null) {
            return unsupported();
        }
        int id = CallRequests.nextId();
        EdtResult<Boolean> res = CallRequests.openAck(id);
        b.showAudioRoutePicker(id, callId);
        return res;
    }

    // ------------------------------------------------------------------
    // listeners
    // ------------------------------------------------------------------

    /// Adds a listener. Every method arrives on the EDT.
    public static void addActionListener(CallActionListener l) {
        if (l == null) {
            return;
        }
        synchronized (LISTENERS) {
            if (!LISTENERS.contains(l)) {
                LISTENERS.add(l);
            }
        }
        // The port holds system-originated actions until Java says it is
        // listening, and THIS is what listening means for an app that never
        // uses VoipPush.
        CallRequests.setActionsWanted(true);
    }

    /// Removes a listener.
    public static void removeActionListener(CallActionListener l) {
        boolean empty;
        synchronized (LISTENERS) {
            LISTENERS.remove(l);
            empty = LISTENERS.isEmpty();
        }
        if (empty) {
            CallRequests.setActionsWanted(false);
        }
    }

    private static CallActionListener[] listeners() {
        synchronized (LISTENERS) {
            return LISTENERS.toArray(new CallActionListener[LISTENERS.size()]);
        }
    }

    // ------------------------------------------------------------------
    // helpers shared with the rest of the family
    // ------------------------------------------------------------------

    /// A resource already failed with [CallError#NOT_SUPPORTED].
    ///
    /// @hidden not part of the public API.
    public static EdtResult<Boolean> unsupported() {
        EdtResult<Boolean> r = new EdtResult<Boolean>();
        r.error(new CallException(CallError.NOT_SUPPORTED));
        return r;
    }

    static CallAudioRoute route(int ordinal) {
        CallAudioRoute[] values = CallAudioRoute.values();
        return ordinal < 0 || ordinal >= values.length
                ? CallAudioRoute.UNKNOWN : values[ordinal];
    }

    static void forget(String callId) {
        synchronized (SESSIONS) {
            SESSIONS.remove(callId);
        }
    }

    /// Registers a session the port created, used by the VoIP push facade for
    /// a call the native side reported before this app was listening.
    ///
    /// @hidden not part of the public API.
    public static CallSession adoptSession(String callId, CallHandle handle,
            String displayName, CallState state) {
        CallSession s = new CallSession(callId, CallDirection.INCOMING, handle,
                displayName, state);
        synchronized (SESSIONS) {
            SESSIONS.put(callId, s);
        }
        return s;
    }

    // ------------------------------------------------------------------
    // Entry points the ports call up into. Every one marshals to the EDT.
    // ------------------------------------------------------------------

    /// Answers an acknowledged operation.
    ///
    /// @hidden not part of the public API.
    public static void deliverAck(int requestId, boolean ok, int errorOrdinal,
            String message) {
        EdtResult<Boolean> r = CallRequests.takeAck(requestId);
        if (r == null) {
            return;
        }
        if (ok) {
            r.complete(Boolean.TRUE);
        } else {
            r.error(CallWire.decodeError(errorOrdinal, message));
        }
    }

    /// Answers a permission request with the granted mask.
    ///
    /// @hidden not part of the public API.
    public static void deliverPermissionResult(int requestId, int grantedMask) {
        EdtResult<Integer> r = CallRequests.takePermissionRequest(requestId);
        if (r != null) {
            r.complete(Integer.valueOf(grantedMask));
        }
    }

    /// The user answered a call through the system UI.
    ///
    /// @hidden not part of the public API.
    public static void deliverAnswer(String callId, long actionToken) {
        dispatch(new ActionEvent(ActionEvent.ANSWER, callId, actionToken, false,
                null, null, false));
    }

    /// The user hung up through the system UI.
    ///
    /// @hidden not part of the public API.
    public static void deliverEndRequest(String callId, long actionToken) {
        dispatch(new ActionEvent(ActionEvent.END, callId, actionToken, false,
                null, null, false));
    }

    /// The user or the system held or resumed a call.
    ///
    /// @hidden not part of the public API.
    public static void deliverHold(String callId, boolean held, long actionToken) {
        dispatch(new ActionEvent(ActionEvent.HOLD, callId, actionToken, held,
                null, null, false));
    }

    /// The user muted or unmuted a call.
    ///
    /// @hidden not part of the public API.
    public static void deliverMute(String callId, boolean muted, long actionToken) {
        dispatch(new ActionEvent(ActionEvent.MUTE, callId, actionToken, muted,
                null, null, false));
    }

    /// The system CHANGED the mute state, rather than asking whether it may.
    ///
    /// Telecom's `onCallAudioStateChanged` reports a mute the platform has
    /// already applied, and a self-managed app cannot refuse it -- the port's
    /// own `setMuted` says as much. Delivered as a refusable action it let a
    /// listener "fail" a fact: the system went on showing muted, the app went
    /// on transmitting, and `isMuted()` answered for neither. This applies
    /// the state first and hands listeners an action that is already
    /// fulfilled, so the same `muteRequested` callback serves both platforms
    /// and only the one that can be refused ever is.
    ///
    /// @hidden not part of the public API.
    public static void deliverMuteChanged(String callId, boolean muted) {
        dispatch(new ActionEvent(ActionEvent.MUTE_CHANGED, callId, CallAction.NONE, muted,
                null, null, false));
    }

    /// Digits the APP asked to send, handed back to its own listener.
    ///
    /// On iOS sendDigits() submits a CXPlayDTMFCallAction and CallKit hands
    /// it straight back through `dtmfRequested`, which is where an app puts
    /// the tone into its media. A port with no such round trip synthesizes
    /// it here, the way the Android port synthesizes audioSessionActivated,
    /// so one listener carries the tone on every platform.
    ///
    /// The action is unanswerable: the app asked for this itself, and there
    /// is no system state to refuse.
    ///
    /// @hidden not part of the public API.
    public static void deliverDtmfPlayed(String callId, String digits) {
        dispatch(new ActionEvent(ActionEvent.DTMF, callId, CallAction.NONE,
                false, digits, null, false));
    }

    /// The user typed on the system keypad.
    ///
    /// @hidden not part of the public API.
    public static void deliverDtmf(String callId, String digits, long actionToken) {
        dispatch(new ActionEvent(ActionEvent.DTMF, callId, actionToken, false,
                digits, null, false));
    }

    /// The system is asking this app to place a call.
    ///
    /// @hidden not part of the public API.
    public static void deliverStartCallRequest(String callId, String handleWire,
            boolean video, long actionToken) {
        dispatch(new ActionEvent(ActionEvent.START, callId, actionToken, false,
                null, CallWire.decodeHandle(handleWire), video));
    }

    /// The operating system activated the audio session.
    ///
    /// @hidden not part of the public API.
    public static void deliverAudioActivated(String callId, int routeOrdinal) {
        dispatch(new ActionEvent(ActionEvent.AUDIO_ON, callId, 0L, false, null,
                null, false, routeOrdinal));
    }

    /// The operating system took the audio session back.
    ///
    /// @hidden not part of the public API.
    public static void deliverAudioDeactivated(String callId) {
        dispatch(new ActionEvent(ActionEvent.AUDIO_OFF, callId, 0L, false, null,
                null, false));
    }

    /// A call ended for a reason that did not come from this app.
    ///
    /// @hidden not part of the public API.
    public static void deliverCallEnded(String callId, int reasonOrdinal) {
        dispatch(new ActionEvent(ActionEvent.ENDED, callId, 0L, false, null,
                null, false, reasonOrdinal));
    }

    /// The system's call provider was reset and every call is gone.
    ///
    /// @hidden not part of the public API.
    public static void deliverProviderReset() {
        dispatch(new ActionEvent(ActionEvent.RESET, null, 0L, false, null,
                null, false));
    }

    /// Records a mute flag once the action that asked for it is fulfilled.
    ///
    /// A named static class rather than an anonymous one so it holds no
    /// synthetic reference to an enclosing scope.
    private static final class MuteChange implements Runnable {
        private final CallSession session;
        private final boolean muted;

        MuteChange(CallSession session, boolean muted) {
            this.session = session;
            this.muted = muted;
        }

        @Override
        public void run() {
            if (session != null) {
                session.setMutedInternal(muted);
            }
        }
    }

    /// Moves a session's state once the action that asked for it is
    /// fulfilled.
    ///
    /// A named static class rather than an anonymous one so it holds no
    /// synthetic reference to an enclosing scope.
    private static final class StateChange implements Runnable {
        private final CallSession session;
        private final CallState target;

        StateChange(CallSession session, CallState target) {
            this.session = session;
            this.target = target;
        }

        @Override
        public void run() {
            if (session != null) {
                session.setStateInternal(target);
            }
        }
    }

    /// Marks a call ended and forgets it, once the end action is fulfilled.
    ///
    /// A named static class rather than an anonymous one so it holds no
    /// synthetic reference to an enclosing scope.
    private static final class EndCleanup implements Runnable {
        private final String callId;
        private final CallSession session;

        EndCleanup(String callId, CallSession session) {
            this.callId = callId;
            this.session = session;
        }

        @Override
        public void run() {
            if (session != null) {
                session.setStateInternal(CallState.ENDED);
            }
            forget(callId);
        }
    }

    private static void dispatch(ActionEvent e) {
        if (Display.isInitialized() && !Display.getInstance().isEdt()) {
            Display.getInstance().callSerially(e);
        } else {
            e.run();
        }
    }

    /// One inbound event, carried to the EDT.
    ///
    /// A single class with a kind tag rather than ten anonymous `Runnable`s:
    /// each of those would hold a synthetic reference to its enclosing scope,
    /// which SpotBugs reports and which keeps a call alive after it ended.
    private static final class ActionEvent implements Runnable {
        static final int ANSWER = 0;
        static final int END = 1;
        static final int HOLD = 2;
        static final int MUTE = 3;
        static final int DTMF = 4;
        static final int START = 5;
        static final int AUDIO_ON = 6;
        static final int AUDIO_OFF = 7;
        static final int ENDED = 8;
        static final int RESET = 9;
        static final int MUTE_CHANGED = 10;

        private final int kind;
        private final String callId;
        private final long token;
        private final boolean flag;
        private final String text;
        private final CallHandle handle;
        private final boolean video;
        private final int ordinal;

        ActionEvent(int kind, String callId, long token, boolean flag,
                String text, CallHandle handle, boolean video) {
            this(kind, callId, token, flag, text, handle, video, 0);
        }

        ActionEvent(int kind, String callId, long token, boolean flag,
                String text, CallHandle handle, boolean video,
                int ordinal) {
            this.kind = kind;
            this.callId = callId;
            this.token = token;
            this.flag = flag;
            this.text = text;
            this.handle = handle;
            this.video = video;
            this.ordinal = ordinal;
        }

        @Override
        public void run() {
            CallActionListener[] ls = listeners();
            CallSession session = callId == null ? null : getSession(callId);
            switch (kind) {
                case ANSWER: {
                    CallAction a = new CallAction(token, callId);
                    // ACTIVE only once the answer is FULFILLED. Setting it
                    // before dispatch left a listener that failed the action
                    // -- directly, or through the deferred safety timer --
                    // holding an active session over a call iOS had put back
                    // to ringing and Android had destroyed.
                    a.whenFulfilled(new StateChange(session, CallState.ACTIVE));
                    try {
                        for (CallActionListener l : ls) {
                            l.answerRequested(callId, a);
                        }
                    } finally {
                        settle(a);
                    }
                    break;
                }
                case END: {
                    CallAction a = new CallAction(token, callId);
                    // Registered BEFORE dispatch, and deliberately a hook
                    // rather than a check. Forgetting unconditionally left an
                    // app that failed the action holding a live system call
                    // getSession() could not address; checking isAnswered()
                    // straight after dispatch instead missed the opposite
                    // case, where a listener defers and fulfils later and the
                    // ended call was never forgotten at all. Registering it
                    // first also means a listener that THROWS still gets the
                    // cleanup, because the finally below fulfils the action.
                    a.whenFulfilled(new EndCleanup(callId, session));
                    try {
                        for (CallActionListener l : ls) {
                            l.endRequested(callId, a);
                        }
                    } finally {
                        settle(a);
                    }
                    break;
                }
                case HOLD: {
                    CallAction a = new CallAction(token, callId);
                    // On fulfilment only, for the reason ANSWER gives.
                    a.whenFulfilled(new StateChange(session,
                            flag ? CallState.HELD : CallState.ACTIVE));
                    try {
                        for (CallActionListener l : ls) {
                            l.holdRequested(callId, flag, a);
                        }
                    } finally {
                        // A listener that throws must not leave the action
                        // unanswered: the platform then times it out, and the
                        // system UI and the app disagree with nothing in the log.
                        settle(a);
                    }
                    break;
                }
                case MUTE_CHANGED: {
                    // Applied before anyone is told, because it already
                    // happened. The action is handed over fulfilled so a
                    // listener written against the iOS shape still compiles
                    // and still runs -- fail() on it changes nothing, which
                    // is exactly the truth on this platform.
                    if (session != null) {
                        session.setMutedInternal(flag);
                    }
                    CallAction done = new CallAction(CallAction.NONE, callId);
                    done.fulfill();
                    try {
                        for (CallActionListener l : ls) {
                            l.muteRequested(callId, flag, done);
                        }
                    } finally {
                        settle(done);
                    }
                    break;
                }
                case MUTE: {
                    CallAction a = new CallAction(token, callId);
                    // On fulfilment only, for the reason ANSWER gives: a
                    // listener that fails this action leaves CallKit holding
                    // the PREVIOUS mute state, and Java used to report the
                    // one the system had just rejected.
                    a.whenFulfilled(new MuteChange(session, flag));
                    try {
                        for (CallActionListener l : ls) {
                            l.muteRequested(callId, flag, a);
                        }
                    } finally {
                        // A listener that throws must not leave the action
                        // unanswered: the platform then times it out, and the
                        // system UI and the app disagree with nothing in the log.
                        settle(a);
                    }
                    break;
                }
                case DTMF: {
                    CallAction a = new CallAction(token, callId);
                    try {
                        for (CallActionListener l : ls) {
                            l.dtmfRequested(callId, text, a);
                        }
                    } finally {
                        // A listener that throws must not leave the action
                        // unanswered: the platform then times it out, and the
                        // system UI and the app disagree with nothing in the log.
                        settle(a);
                    }
                    break;
                }
                case START: {
                    CallAction a = new CallAction(token, callId);
                    try {
                        for (CallActionListener l : ls) {
                            l.startCallRequested(callId, handle, video, a);
                        }
                    } finally {
                        // A listener that throws must not leave the action
                        // unanswered: the platform then times it out, and the
                        // system UI and the app disagree with nothing in the log.
                        settle(a);
                    }
                    break;
                }
                case AUDIO_ON: {
                    CallAudioSession s = new CallAudioSession(callId, route(ordinal));
                    for (CallActionListener l : ls) {
                        tellAudioOn(l, s);
                    }
                    break;
                }
                case AUDIO_OFF:
                    for (CallActionListener l : ls) {
                        tellAudioOff(l, callId);
                    }
                    break;
                case ENDED: {
                    if (session != null) {
                        session.setStateInternal(CallState.ENDED);
                    }
                    CallEndReason reason = CallWire.endReason(ordinal);
                    try {
                        for (CallActionListener l : ls) {
                            tell(l, callId, reason);
                        }
                    } finally {
                        // Mandatory, so a listener that throws cannot leave an
                        // ended session in getSessions() addressing a native
                        // call that no longer exists. The action arms settle
                        // their tokens in a finally for the same reason.
                        forget(callId);
                    }
                    break;
                }
                case RESET:
                    // Sessions go first: a listener that iterates getSessions()
                    // during providerReset must not see calls the system has
                    // already destroyed.
                    synchronized (SESSIONS) {
                        SESSIONS.clear();
                    }
                    // Then everything in flight. The provider is gone and the
                    // native side has dropped the request ids with it, so a
                    // report, end, configure or permission request that raced
                    // the reset would never be answered -- and this SPI calls
                    // an operation that never answers worse than one that
                    // fails. A late acknowledgement is ignored after this,
                    // which is the point: it would otherwise hand back a
                    // session RESET has already removed.
                    CallRequests.failAll(new CallException(
                            CallError.PROVIDER_RESET,
                            "The system's call provider was reset"));
                    for (CallActionListener l : ls) {
                        tellReset(l);
                    }
                    break;
                default:
                    break;
            }
        }

        /// Fulfills an action the application ignored.
        ///
        /// An action nobody deferred is one nobody intends to answer, and the
        /// platform kills a call whose action goes unanswered -- so silence
        /// has to mean "done", not "dropped".
        private static void settle(CallAction a) {
            if (!a.isDeferred() && !a.isAnswered()) {
                a.answer(true);
            }
        }
    }

    /// Builds a session for a call that is already over, WITHOUT registering
    /// it.
    ///
    /// Used for a pushed call that ended before the app heard about it: the
    /// app needs somewhere to read the handle and the id from, and
    /// getSessions() must not start listing calls that are finished.
    ///
    /// @hidden not part of the public API.
    public static CallSession detachedSession(String callId, CallHandle handle,
            String displayName) {
        return new CallSession(callId, CallDirection.INCOMING, handle,
                displayName, CallState.ENDED);
    }

    /// Clears every listener and session.
    ///
    /// @hidden not part of the public API; test-only.
    public static void resetForTest() {
        synchronized (LISTENERS) {
            LISTENERS.clear();
        }
        synchronized (SESSIONS) {
            SESSIONS.clear();
        }
    }
}
