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

import com.codename1.call.CallDirection;
import com.codename1.call.CallEndReason;
import com.codename1.call.CallHandle;
import com.codename1.call.CallState;
import com.codename1.call.spi.CallBridge;
import com.codename1.impl.async.EdtResult;
import com.codename1.impl.call.CallRequests;
import com.codename1.impl.call.CallWire;
import com.codename1.util.AsyncResource;

/// One call, from the system's point of view.
///
/// Obtained from [Calls#reportIncoming] or [Calls#reportOutgoing], or handed
/// over already ringing by
/// `com.codename1.call.voip.PushedCall#getSession()`. A session is a handle
/// on a call the operating system knows about; it does not carry audio.
///
/// Every method here is a request to the **system**, and every one of them is
/// asynchronous because the system can refuse. Acting on a call that has
/// already ended fails with [CallError#INVALID_ID] rather than doing nothing,
/// so a bug in the app's own bookkeeping shows up instead of hiding.
public final class CallSession {
    private final String callId;
    private final CallDirection direction;
    private CallHandle handle;
    private String displayName;
    private CallState state;
    private boolean muted;

    CallSession(String callId, CallDirection direction, CallHandle handle,
            String displayName, CallState state) {
        this.callId = callId;
        this.direction = direction;
        this.handle = handle;
        this.displayName = displayName;
        this.state = state;
    }

    /// The identifier naming this call everywhere -- here, in the system, and
    /// in whatever signalling the app uses.
    public String getCallId() {
        return callId;
    }

    /// Which way the call was placed.
    public CallDirection getDirection() {
        return direction;
    }

    /// Who is on the other end.
    public CallHandle getHandle() {
        return handle;
    }

    /// The name shown for the far end, or null.
    public String getDisplayName() {
        return displayName;
    }

    /// Where the call is in its life.
    public CallState getState() {
        synchronized (this) {
            return state;
        }
    }

    /// Whether the call is muted, as far as the system is concerned.
    public boolean isMuted() {
        return muted;
    }

    /// Tells the system the outgoing call has begun connecting -- the far end
    /// is being rung. Ignored for an incoming call.
    public void reportStartedConnecting() {
        CallBridge b = CallRequests.bridge();
        if (b == null || direction != CallDirection.OUTGOING) {
            return;
        }
        if (!moveUnlessOver(CallState.DIALING)) {
            return;
        }
        b.reportOutgoingStartedConnecting(callId, System.currentTimeMillis());
    }

    /// Moves to `next` unless the call is already over, as ONE step.
    ///
    /// ENDED is terminal in [CallState], and signalling is asynchronous: a
    /// media-connected callback that was already in flight when the call
    /// ended used to move the session back to ACTIVE. The ports drop the
    /// report -- the platform call is gone -- so the Java object was left
    /// contradicting both the system and its own contract, and anything
    /// watching it could render or restart media for a call that is over.
    ///
    /// The check and the assignment used to straddle a window an end event
    /// fits through: signalling calls reportConnected off the EDT while an
    /// end or a provider reset is being delivered ON it, so the end could
    /// land after the check and this would then move the retained session
    /// back to ACTIVE -- reporting a connection for a native call that was
    /// already gone, and contradicting the terminal-state guarantee ENDED is
    /// documented to give. A separate terminal-state test before the
    /// assignment only narrows that window; it cannot close it, which is why
    /// the predicate it used to call is gone rather than merely unused.
    private boolean moveUnlessOver(CallState next) {
        synchronized (this) {
            if (state == CallState.ENDED) {
                return false;
            }
            state = next;
            return true;
        }
    }

    /// Tells the system the call is connected. Call this when media is
    /// actually flowing, because it starts the duration the user sees.
    public void reportConnected() {
        CallBridge b = CallRequests.bridge();
        if (b == null || !moveUnlessOver(CallState.ACTIVE)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (direction == CallDirection.OUTGOING) {
            b.reportOutgoingConnected(callId, now);
        } else {
            b.reportIncomingConnected(callId, now);
        }
    }

    /// Changes what the system shows for this call. Arguments left null are
    /// left alone.
    public void update(CallHandle newHandle, String newDisplayName) {
        CallBridge b = CallRequests.bridge();
        if (newHandle != null) {
            handle = newHandle;
        }
        if (newDisplayName != null) {
            displayName = newDisplayName;
        }
        if (b != null) {
            b.updateCall(callId, newHandle == null ? null
                    : CallWire.encodeHandle(newHandle), newDisplayName, -1, false);
        }
    }

    /// Hangs up, and writes `reason` into the system call log.
    ///
    /// Do **not** call this from
    /// [CallActionListener#providerReset()] -- by then the call no longer
    /// exists.
    public AsyncResource<Boolean> end(CallEndReason reason) {
        CallBridge b = CallRequests.bridge();
        if (b == null) {
            return Calls.unsupported();
        }
        int id = CallRequests.nextId();
        EdtResult<Boolean> r = CallRequests.openAck(id);
        // The state moves and the session is forgotten only if the system
        // agreed. Setting ENDED up front left an app whose end request was
        // refused holding a session that said ended over a call that was
        // still up.
        r.onResult(new EndOutcome(this));
        b.endCall(id, callId, reason == null
                ? CallEndReason.LOCAL_ENDED.ordinal() : reason.ordinal());
        return r;
    }

    /// Tells the system the **far end** ended the call. Use this rather than
    /// [#end] when the hang-up came down the app's own signalling, so the
    /// call log says what happened.
    public void reportEndedRemotely(CallEndReason reason) {
        CallBridge b = CallRequests.bridge();
        synchronized (this) {
            state = CallState.ENDED;
        }
        // Unconditional here, unlike end(): this is the app telling the
        // framework the call is already over, not asking for it to be. Still
        // identity-checked, because the id can already name a newer call.
        Calls.forget(callId, this);
        if (b != null) {
            b.reportCallEnded(callId, reason == null
                    ? CallEndReason.REMOTE_ENDED.ordinal() : reason.ordinal(),
                    System.currentTimeMillis());
        }
    }

    /// Holds or resumes the call.
    public AsyncResource<Boolean> setHeld(boolean held) {
        CallBridge b = CallRequests.bridge();
        if (b == null) {
            return Calls.unsupported();
        }
        int id = CallRequests.nextId();
        EdtResult<Boolean> r = CallRequests.openAck(id);
        // Applied from the acknowledgement, as end() does. Setting it up
        // front left the session claiming HELD after a transaction the system
        // had rejected -- for instance because the call ended while the
        // request was in flight -- with nothing to roll it back.
        r.onResult(new StateOutcome(this,
                held ? CallState.HELD : CallState.ACTIVE));
        b.setHeld(id, callId, held);
        return r;
    }

    /// Mutes or unmutes the call in the system UI.
    ///
    /// This tells the operating system what the mute button should look like.
    /// It does **not** stop the app sending audio -- nothing here touches
    /// media -- so an app that only calls this is still transmitting.
    public AsyncResource<Boolean> setMuted(boolean value) {
        CallBridge b = CallRequests.bridge();
        if (b == null) {
            return Calls.unsupported();
        }
        int id = CallRequests.nextId();
        EdtResult<Boolean> r = CallRequests.openAck(id);
        // Applied from the acknowledgement for the same reason as setHeld.
        r.onResult(new MuteOutcome(this, value));
        b.setMuted(id, callId, value);
        return r;
    }

    /// Sends DTMF digits through the system.
    public AsyncResource<Boolean> sendDigits(String digits) {
        CallBridge b = CallRequests.bridge();
        if (b == null) {
            return Calls.unsupported();
        }
        int id = CallRequests.nextId();
        EdtResult<Boolean> r = CallRequests.openAck(id);
        b.sendDtmf(id, callId, digits);
        return r;
    }

    /// Puts this call in a conference with `other`, or takes it out of one
    /// when `other` is null.
    ///
    /// **Answers NOT_SUPPORTED on every platform today**, which is why
    /// [com.codename1.call.spi.CallBridge#CAPABILITY_GROUPING] is set by no
    /// port: CallKit's group action travels system to app with no
    /// app-initiated counterpart, and Telecom conferences self-managed calls
    /// only through a `ConnectionService` conference this framework does not
    /// build. It stays here because the system may still ask an app to group
    /// calls, and because a conference an app mixes itself needs no
    /// permission from either platform.
    public AsyncResource<Boolean> groupWith(CallSession other) {
        CallBridge b = CallRequests.bridge();
        if (b == null) {
            return Calls.unsupported();
        }
        int id = CallRequests.nextId();
        EdtResult<Boolean> r = CallRequests.openAck(id);
        b.setCallGroup(id, callId, other == null ? null : other.getCallId());
        return r;
    }

    /// Moves the session to ENDED and forgets it, but only once the system
    /// has accepted the end request.
    ///
    /// A named static class rather than an anonymous one so it holds no
    /// synthetic reference to an enclosing scope.
    private static final class EndOutcome
            implements com.codename1.util.AsyncResult<Boolean> {
        private final CallSession session;

        EndOutcome(CallSession session) {
            this.session = session;
        }

        @Override
        public void onReady(Boolean value, Throwable error) {
            if (error != null) {
                // Still up as far as the system is concerned, so it stays
                // addressable through Calls.getSession.
                return;
            }
            synchronized (session) {
                session.state = CallState.ENDED;
            }
            Calls.forget(session.getCallId(), session);
        }
    }

    /// Moves the session's state, but only once the system has accepted the
    /// request that asked for it.
    ///
    /// A named static class rather than an anonymous one so it holds no
    /// synthetic reference to an enclosing scope.
    private static final class StateOutcome
            implements com.codename1.util.AsyncResult<Boolean> {
        private final CallSession session;
        private final CallState target;

        StateOutcome(CallSession session, CallState target) {
            this.session = session;
            this.target = target;
        }

        @Override
        public void onReady(Boolean value, Throwable error) {
            // Never out of ENDED. Every transition here is an acknowledgement
            // that was in flight, so a hold or resume the system accepted
            // just as the call was ending would otherwise move a terminal
            // session back to HELD or ACTIVE -- the same defect as a late
            // reportConnected, and reachable by the same route.
            //
            // Guarded by inspection rather than by a test: LocalCallBridge
            // settles an acknowledgement before end() can run, so the
            // simulation cannot produce the ordering this defends against.
            // The late-reportConnected half IS covered, in LocalCallTest.
            if (error == null) {
                session.moveUnlessOver(target);
            }
        }
    }

    /// Records the mute flag once the system has accepted it.
    private static final class MuteOutcome
            implements com.codename1.util.AsyncResult<Boolean> {
        private final CallSession session;
        private final boolean target;

        MuteOutcome(CallSession session, boolean target) {
            this.session = session;
            this.target = target;
        }

        @Override
        public void onReady(Boolean value, Throwable error) {
            if (error == null) {
                session.muted = target;
            }
        }
    }

    /// Sets the state without telling the system, for events coming the other
    /// way.
    void setStateInternal(CallState value) {
        // Under the monitor every other transition uses. This is how the
        // PORT reports a change, including the end that moveUnlessOver has
        // to see, so it cannot be the one write that races.
        synchronized (this) {
            state = value;
        }
    }

    /// Sets the mute flag without telling the system.
    void setMutedInternal(boolean value) {
        muted = value;
    }
}
