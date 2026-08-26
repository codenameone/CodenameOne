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
        return state;
    }

    /// Whether the call is muted, as far as the system is concerned.
    public boolean isMuted() {
        return muted;
    }

    /// Tells the system the outgoing call has begun connecting -- the far end
    /// is being rung. Ignored for an incoming call.
    public void reportStartedConnecting() {
        CallBridge b = CallRequests.bridge();
        if (b != null && direction == CallDirection.OUTGOING) {
            state = CallState.DIALING;
            b.reportOutgoingStartedConnecting(callId, System.currentTimeMillis());
        }
    }

    /// Tells the system the call is connected. Call this when media is
    /// actually flowing, because it starts the duration the user sees.
    public void reportConnected() {
        CallBridge b = CallRequests.bridge();
        if (b == null) {
            return;
        }
        state = CallState.ACTIVE;
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
        state = CallState.ENDED;
        // Unconditional here, unlike end(): this is the app telling the
        // framework the call is already over, not asking for it to be.
        Calls.forget(callId);
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
        state = held ? CallState.HELD : CallState.ACTIVE;
        int id = CallRequests.nextId();
        EdtResult<Boolean> r = CallRequests.openAck(id);
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
        muted = value;
        int id = CallRequests.nextId();
        EdtResult<Boolean> r = CallRequests.openAck(id);
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
            session.state = CallState.ENDED;
            Calls.forget(session.getCallId());
        }
    }

    /// Sets the state without telling the system, for events coming the other
    /// way.
    void setStateInternal(CallState value) {
        state = value;
    }

    /// Sets the mute flag without telling the system.
    void setMutedInternal(boolean value) {
        muted = value;
    }
}
