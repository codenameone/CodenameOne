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
import com.codename1.call.CallError;
import com.codename1.call.CallException;
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
        synchronized (this) {
            return handle;
        }
    }

    /// The name shown for the far end, or null.
    public String getDisplayName() {
        synchronized (this) {
            return displayName;
        }
    }

    /// Where the call is in its life.
    public CallState getState() {
        synchronized (this) {
            return state;
        }
    }

    /// Whether the call is muted, as far as the system is concerned.
    public boolean isMuted() {
        // Under the monitor, like the state beside it. Telecom and CallKit
        // both deliver a mute change on the EDT while a media worker reads
        // this to decide whether to keep transmitting, and a plain field
        // gives that worker no reason ever to see the new value -- so it
        // could go on sending audio after the system UI showed the call
        // muted. Not volatile: PMD's forbidden list rules it out in core, and
        // the monitor this class already has answers the same question.
        synchronized (this) {
            return muted;
        }
    }

    /// Tells the system the outgoing call has begun connecting -- the far end
    /// is being rung. Ignored for an incoming call, and ignored once the call
    /// has connected.
    public void reportStartedConnecting() {
        CallBridge b = CallRequests.bridge();
        if (b == null || direction != CallDirection.OUTGOING
                || !Calls.owns(callId, this)) {
            return;
        }
        // STILL DIALING, rather than "not over". This was
        // moveUnlessOver(DIALING), which guards ENDED and nothing else, so
        // reordered signalling -- reportConnected() followed by a
        // reportStartedConnecting() that was already in flight -- moved an
        // ACTIVE or HELD session back to DIALING. The platform half is worse
        // than the Java half: AndroidCallBridge turns this into
        // Connection.setDialing(), so the SYSTEM call UI went back to dialing
        // for a call that was already up, and the user saw a connected call
        // start ringing out again.
        //
        // The transition it was making could never be anything else. An
        // outgoing session is constructed DIALING by Calls' report, and
        // nothing else in core moves a session to DIALING, so
        // moveUnlessOver(DIALING) was either a no-op or that regression --
        // there was no case it existed to serve.
        //
        // The test and the report are ONE step, under `reporting`. Guarding
        // the state alone was not enough, and the reason I first gave for
        // leaving that window open was wrong: I said a connect landing in it
        // was a report ordering the platform resolves. The platform does not
        // resolve anything -- it applies the last report it is given. So
        // signalling on two threads could run this check while the call was
        // still dialing, let reportConnected move the session to ACTIVE and
        // call Telecom's setActive, and only then arrive here with
        // setDialing. Telecom regresses to dialing and the Java session stays
        // ACTIVE: not a race about which correct state wins, but the two
        // halves of this API disagreeing, with the system showing the wrong
        // one to the user.
        //
        // A SEPARATE lock, not the session monitor, and that is the whole
        // trick: the monitor cannot be held across a bridge call because the
        // ports call back into Java, so holding it here is a deadlock. This
        // one is held only for the length of a report and only by reports, so
        // it orders them against each other without standing between a port
        // callback and the state it needs. TunnelHost's `lifecycle` lock
        // orders start, stop and delivery for the same reason.
        synchronized (reporting) {
            if (!stillDialing()) {
                return;
            }
            b.reportOutgoingStartedConnecting(callId,
                    System.currentTimeMillis());
        }
    }

    /// Orders a state test against the native report that follows it.
    ///
    /// Two reports that both move a live call and both tell the platform have
    /// to reach the platform in the order they took effect in Java, or the
    /// system shows a state the session denies. `end` needs no part of this:
    /// ENDED is terminal, so a report racing it fails moveUnlessOver and
    /// never reaches its bridge call at all.
    private final Object reporting = new Object();

    /// Whether this call has not connected yet.
    ///
    /// Read under the monitor the state is written under, for the reason
    /// isMuted() is: signalling reads this off the EDT while the ports
    /// deliver a connect on it.
    private boolean stillDialing() {
        synchronized (this) {
            return state == CallState.DIALING;
        }
    }

    /// Moves to `next` unless the call is already over, as ONE step.
    ///
    /// Package-private because Calls' deferred-action hooks need the same
    /// step: they were restating the check and then assigning, which is the
    /// same two-step this exists to replace.
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
    /// Moves to ACTIVE, but only from a call that has not connected yet.
    ///
    /// moveUnlessOver rejects ENDED and nothing else, which is right for the
    /// transitions that may happen at any point in a call and wrong for this
    /// one. A delayed or duplicate connected callback on a HELD call moved it
    /// back to ACTIVE and had the Android bridge call Connection.setActive --
    /// undoing a hold the USER asked for, from a signalling event that says
    /// nothing about hold at all.
    ///
    /// RINGING and DIALING are the whole of the legitimate source: hold is
    /// left by unholding, which is its own operation, and a call already
    /// ACTIVE has nothing to gain from being told so twice -- the platform
    /// starts the duration the user sees from that report.
    boolean moveToConnected() {
        synchronized (this) {
            if (state != CallState.RINGING && state != CallState.DIALING) {
                return false;
            }
            state = CallState.ACTIVE;
            return true;
        }
    }

    boolean moveUnlessOver(CallState next) {
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
        if (b == null || !Calls.owns(callId, this)) {
            return;
        }
        // Inside `reporting` with the transition, so a started-connecting
        // report cannot pass its own state test before this one and then
        // reach the platform after it. See the note there.
        synchronized (reporting) {
            if (!moveToConnected()) {
                return;
            }
            long now = System.currentTimeMillis();
            if (direction == CallDirection.OUTGOING) {
                b.reportOutgoingConnected(callId, now);
            } else {
                b.reportIncomingConnected(callId, now);
            }
        }
    }

    /// Changes what the system shows for this call. Arguments left null are
    /// left alone.
    ///
    /// The two fields are written under the session monitor, like the state
    /// and the mute flag: a signalling worker calls this while the EDT reads
    /// the same session to refresh the in-app call UI, and plain writes gave
    /// that reader no reason ever to see them -- so the system UI could show
    /// the new identity while Java went on displaying the old one, or a
    /// mixed pair of the two.
    public void update(CallHandle newHandle, String newDisplayName) {
        CallBridge b = CallRequests.bridge();
        if (!Calls.owns(callId, this)) {
            // The local fields are left alone too: this object no longer
            // describes anything the system knows about.
            return;
        }
        if (newHandle != null) {
            synchronized (this) {
                handle = newHandle;
            }
        }
        if (newDisplayName != null) {
            synchronized (this) {
                displayName = newDisplayName;
            }
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
        // The ownership test and the handoff as ONE step against the
        // registry. Checked and then handed off, an end authorised while THIS
        // session held the id could reach the port after a replacement had
        // claimed it -- and the SPI carries no session identity, so
        // AndroidCallBridge.endCall looks up the id alone and would finish
        // the replacement, acknowledge success, and leave that live call
        // registered in Java with nothing to say it had been hung up.
        //
        // reportEndedRemotely never had this hole: its check is an
        // identity-checked forget() under the registry monitor, and it
        // reports only when that said this session owned the id. This path
        // asked a weaker question and acted on it later.
        //
        // See Calls.HANDOFF for why it is not the SESSIONS monitor.
        synchronized (Calls.HANDOFF) {
            if (!Calls.owns(callId, this)) {
                return staleSession();
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
    }

    /// Tells the system the **far end** ended the call. Use this rather than
    /// [#end] when the hang-up came down the app's own signalling, so the
    /// call log says what happened.
    public void reportEndedRemotely(CallEndReason reason) {
        CallBridge b = CallRequests.bridge();
        synchronized (this) {
            state = CallState.ENDED;
        }
        // The registry entry is released and the port told immediately
        // after, not atomically. Review asked for a tombstone held across
        // the native call, or native identity by generation. Neither is
        // written here on purpose: the window is two adjacent statements on
        // one thread, and to lose it another thread would have to complete a
        // whole reportIncoming for the same id in between -- which the ports
        // themselves refuse while the call is still live natively, because
        // that is a duplicate. A tombstone buys that sliver and adds a way
        // for an id to become permanently unusable when a native report
        // throws; generation-tagged native identity is a much larger change
        // to both ports for the same sliver. Reporting only when this
        // session OWNED the entry is the part that was actually reachable.
        //
        // The native report is made only if THIS session still owned the id.
        // forget() is identity-checked, so a stale session leaves the
        // replacement registered -- but the report went out regardless, and
        // both ports resolve it by call id alone, so a late signalling
        // callback on a retained old session ended the new live call that
        // had reused the id after a provider reset.
        boolean owned = Calls.forget(callId, this);
        if (b != null && owned) {
            b.reportCallEnded(callId, reason == null
                    ? CallEndReason.REMOTE_ENDED.ordinal() : reason.ordinal(),
                    System.currentTimeMillis());
        }
    }

    /// A resource already failed because this session lost its id.
    ///
    /// Every operation here reaches the port with nothing but the call id,
    /// and the ports resolve by that alone -- so an object the app still
    /// holds after a provider reset, or a detached one describing a call
    /// that is already over, would otherwise act on whatever owns the id
    /// now. end() was gated first; this is the same question for the rest of
    /// them, which is where it should have been asked in the first place.
    private static EdtResult<Boolean> staleSession() {
        EdtResult<Boolean> stale = new EdtResult<Boolean>();
        stale.error(new CallException(CallError.INVALID_ID,
                "This session no longer names a live call"));
        return stale;
    }

    /// Holds or resumes the call.
    public AsyncResource<Boolean> setHeld(boolean held) {
        CallBridge b = CallRequests.bridge();
        if (b == null) {
            return Calls.unsupported();
        }
        if (!Calls.owns(callId, this)) {
            return staleSession();
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
    ///
    /// Gated by `CallBridge.CAPABILITY_MUTE`, which Android does not offer: a
    /// self-managed call there cannot tell Telecom its mute state, so this
    /// answers `NOT_SUPPORTED` and [#isMuted()] stays where the system left
    /// it. Hearing what the user does with the system's own mute control is a
    /// separate thing and works everywhere; see
    /// [CallActionListener#muteRequested].
    public AsyncResource<Boolean> setMuted(boolean value) {
        CallBridge b = CallRequests.bridge();
        if (b == null) {
            return Calls.unsupported();
        }
        if (!Calls.owns(callId, this)) {
            return staleSession();
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
        if (!Calls.owns(callId, this)) {
            return staleSession();
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
                synchronized (session) {
                    session.muted = target;
                }
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
        synchronized (this) {
            muted = value;
        }
    }
}
