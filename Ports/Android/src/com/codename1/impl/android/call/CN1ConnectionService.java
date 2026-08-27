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
package com.codename1.impl.android.call;

import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.ConnectionService;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;

import android.net.Uri;
import android.telecom.PhoneAccount;

import com.codename1.call.CallEndReason;
import com.codename1.call.CallError;
import com.codename1.call.CallHandle;
import com.codename1.call.CallHandleType;
import com.codename1.call.CallId;
import com.codename1.impl.call.CallWire;
import com.codename1.call.session.CallAudioRoute;
import com.codename1.call.session.Calls;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/// The `ConnectionService` Telecom binds to when this app owns a call.
///
/// #### Why this ships in the port instead of being generated
///
/// The Firebase messaging service is generated at build time because it has
/// to extend a class from a Gradle dependency the port cannot compile
/// against. `android.telecom` is part of the platform and the port compiles
/// against it, so that reason does not apply here -- and a several-hundred
/// line service written as a string template would get no compiler, no unit
/// test and no SpotBugs, so every mistake in it would surface only on the
/// build server.
///
/// It costs nothing to ship. Telecom instantiates a `ConnectionService` only
/// after a `PhoneAccount` has been registered, which happens inside
/// `Calls.configure`, so an app that never calls that never loads this class
/// and R8 strips it.
///
/// #### The refusal paths are the ones that matter
///
/// Telecom **refuses** a self-managed call during an emergency call, when
/// another app holds one, or when the user has switched this app's calling
/// off. Those arrive as `onCreate...ConnectionFailed`, and an implementation
/// that leaves them unwired produces a request that never answers -- which
/// the bridge contract calls worse than an outright failure, and which looks
/// exactly like a call that is still ringing.
public class CN1ConnectionService extends ConnectionService {

    private static final Map<String, CN1Connection> CONNECTIONS =
            new HashMap<String, CN1Connection>();

    private static final AtomicLong TOKENS = new AtomicLong(1);

    private static final Map<Long, PendingAction> PENDING =
            new HashMap<Long, PendingAction>();

    private static volatile int route = CallAudioRoute.EARPIECE.ordinal();

    /// Reports waiting for Telecom's answer, keyed by call id.
    ///
    /// A map rather than a single slot. Telecom does not carry the caller's
    /// request id through `addNewIncomingCall`, so it is parked here between
    /// the call and the callback -- but two reports can be in flight at once,
    /// and a single slot meant the second overwrote the first: one callback
    /// then acknowledged the wrong request and the other acknowledged
    /// nothing, leaving an AsyncResource that never settled. The call id is
    /// already in the request extras, so correlating by it costs nothing.
    /// Addresses this app asked Telecom to place or ring, each holding EVERY
    /// call id still waiting on it, oldest first. Guarded by PENDING_REPORTS.
    ///
    /// A list rather than one id: two calls to the same number can be in
    /// flight at once, and a single value let the second overwrite the first.
    /// On a device that omits the private extras -- the case this map exists
    /// to recover -- the first callback was then attributed to the second
    /// report, and the next one looked like a system-placed call while the
    /// first report waited for an answer that never came.
    private static final Map<String, List<Parked>> PENDING_ADDRESSES =
            new HashMap<String, List<Parked>>();

    /// A report parked against an address, with the direction it was made in.
    ///
    /// The direction is stored because the address alone does not identify a
    /// report: an app that reports an incoming call from a number while
    /// placing one TO it has two reports under one key, and the fallback
    /// would hand a callback whichever came first. Both ends know the
    /// direction, so there is no reason to guess.
    private static final class Parked {
        private final String callId;
        private final boolean incoming;

        Parked(String callId, boolean incoming) {
            this.callId = callId;
            this.incoming = incoming;
        }
    }

    private static final Map<String, Integer> PENDING_REPORTS =
            new HashMap<String, Integer>();

    /// Calls the SYSTEM asked this app to place and that Java has not
    /// reported back yet.
    ///
    /// The listener contract says the app answers startCallRequested by
    /// calling Calls.reportOutgoing with the id it was handed. On this
    /// platform that reaches placeCall, which would create a SECOND
    /// connection for a call Telecom already has -- the new one replacing
    /// this one in CONNECTIONS while the original stayed alive in Telecom.
    /// The report adopts the connection recorded here instead.
    private static final Map<String, CN1Connection> SYSTEM_STARTED =
            new HashMap<String, CN1Connection>();

    /// One action the system asked for and the app has not answered.
    private static final class PendingAction {
        private final CN1Connection connection;
        private final int kind;

        PendingAction(CN1Connection connection, int kind) {
            this.connection = connection;
            this.kind = kind;
        }
    }

    @Override
    public Connection onCreateIncomingConnection(PhoneAccountHandle handle,
            ConnectionRequest request) {
        return adopt(request, true);
    }

    @Override
    public Connection onCreateOutgoingConnection(PhoneAccountHandle handle,
            ConnectionRequest request) {
        return adopt(request, false);
    }

    @Override
    public void onCreateIncomingConnectionFailed(PhoneAccountHandle handle,
            ConnectionRequest request) {
        refuse(request, true);
    }

    @Override
    public void onCreateOutgoingConnectionFailed(PhoneAccountHandle handle,
            ConnectionRequest request) {
        refuse(request, false);
    }

    private Connection adopt(ConnectionRequest request, boolean incoming) {
        String id = request == null || request.getExtras() == null ? null
                : request.getExtras().getString(EXTRA_CALL_ID);
        // A call the SYSTEM asked this app to place -- from Recents, from a
        // contact, or from a voice assistant -- carries none of this bridge's
        // extras, because this app never placed it. Treating it as a report
        // whose extras went missing meant adopt() either reused an unrelated
        // call id or refused outright, and startCallRequested never fired --
        // so the documented way to hear about these calls could not work at
        // all. The discriminator is a report actually being in flight:
        // without one there is nothing for a dropped extra to belong to.
        boolean external = false;
        if (id == null) {
            // Matched on the ADDRESS, not on "is any report pending". A call
            // the system placed while an unrelated report happened to be in
            // flight was classified as that report: it adopted the other
            // call's id, acknowledged a request nobody had answered, and
            // never delivered startCallRequested -- and the real report's
            // callback then built a second connection under the same id.
            //
            // Only a report to the same address can be this request.
            // BOTH directions. The recovery was gated on the callback being
            // outgoing, so an OEM that dropped the extra from an INCOMING
            // callback left the id null and the call was refused -- a real
            // ringing call rejected, while the address had been recorded by
            // reportIncomingCall for exactly this purpose and refuse() was
            // already consulting it on the failure path. Recorded for the
            // fallback, used only by half of it.
            String matched = pendingReportFor(
                    request == null ? null : request.getAddress(), incoming);
            if (matched != null) {
                id = matched;
            } else if (!incoming) {
                // Only an OUTGOING callback with nothing waiting can be a
                // call the SYSTEM asked this app to place. Telecom raises an
                // incoming connection for a self-managed account only because
                // this app called addNewIncomingCall, so an unmatched one is
                // not a new call to invent an id for -- it is a report whose
                // identity is genuinely lost, and refusing stays correct.
                external = true;
            }
        }
        if (external) {
            id = CallId.random();
        }
        if (id == null) {
            // Nothing can be routed to a call with no identifier, so refusing
            // is the honest answer rather than creating an orphan Telecom
            // knows about and this app cannot address.
            refuse(request, incoming);
            return null;
        }
        CN1Connection c = new CN1Connection(this, id);
        c.setRingingName(request == null || request.getExtras() == null ? null
                : request.getExtras().getString(
                        android.telecom.TelecomManager.EXTRA_CALL_SUBJECT));
        c.setVideo(request != null && request.getExtras() != null
                && request.getExtras().getBoolean(EXTRA_VIDEO, false));
        c.setInitializing();
        if (incoming) {
            c.setRinging();
        } else {
            c.setDialing();
        }
        synchronized (CONNECTIONS) {
            CONNECTIONS.put(id, c);
        }
        if (external) {
            // No report to answer -- nothing asked for this call -- so the
            // app is TOLD about it instead, with an id it has never seen and
            // the address the system supplied. This is the Android half of
            // what performStartCallAction delivers on iOS.
            synchronized (SYSTEM_STARTED) {
                SYSTEM_STARTED.put(id, c);
            }
            Calls.deliverStartCallRequest(id, externalHandleWire(request),
                    c.isVideo(), nextActionToken(c, CN1Connection.ACTION_START));
            return c;
        }
        answerReport(id, true, 0, null);
        return c;
    }

    /// The address of a call the system asked this app to place, as a handle
    /// record.
    ///
    /// Telecom hands it over as a `tel:` or `sip:` URI; anything else is
    /// carried through as a generic handle rather than dropped, because the
    /// app can still recognise an address this bridge does not.
    private static String externalHandleWire(ConnectionRequest request) {
        Uri address = request == null ? null : request.getAddress();
        if (address == null) {
            return "";
        }
        String part = address.getSchemeSpecificPart();
        if (part == null || part.length() == 0) {
            return "";
        }
        CallHandleType type = CallHandleType.GENERIC;
        if (PhoneAccount.SCHEME_TEL.equals(address.getScheme())) {
            type = CallHandleType.PHONE_NUMBER;
        } else if ("mailto".equals(address.getScheme())) {
            type = CallHandleType.EMAIL_ADDRESS;
        }
        return CallWire.encodeHandle(new CallHandle(type, part));
    }

    private void refuse(ConnectionRequest request, boolean incoming) {
        String id = request == null || request.getExtras() == null ? null
                : request.getExtras().getString(EXTRA_CALL_ID);
        if (id == null) {
            // Matched on the ADDRESS, exactly as adopt() does. Falling back
            // to the last report failed an unrelated one whenever Telecom
            // refused a call the SYSTEM placed -- and that report's own
            // callback could then still arrive and build a connection for a
            // CallSession Java had already failed and forgotten.
            id = pendingReportFor(request == null ? null
                    : request.getAddress(), incoming);
        }
        if (id == null) {
            // A system-placed call Telecom refused. Nothing of this app's was
            // waiting on it, so there is nothing to answer; the user sees the
            // system's own failure.
            return;
        }
        answerReport(id, false,
                CallError.CALL_REFUSED.ordinal(),
                "Telecom refused the call: an emergency call is in progress,"
                + " another application holds a call, or calling is switched"
                + " off for this app");
    }

    private static void answerReport(String callId, boolean ok, int error,
            String message) {
        Integer requestId;
        synchronized (PENDING_REPORTS) {
            requestId = takeReportLocked(callId);
            if (requestId == null && PENDING_REPORTS.size() == 1) {
                // Telecom answered with nothing we could key on and exactly
                // one report is outstanding, so it is that one.
                String only = PENDING_REPORTS.keySet().iterator().next();
                requestId = takeReportLocked(only);
            }
        }
        if (requestId != null) {
            Calls.deliverAck(requestId.intValue(), ok, error, message);
        }
    }

    /// Adopts the connection for a call the system asked this app to place.
    ///
    /// Answers true when this id names such a call, in which case the report
    /// is complete: Telecom already has the call and placing it again would
    /// duplicate it.
    static boolean adoptSystemStarted(int requestId, String callId) {
        CN1Connection c;
        synchronized (SYSTEM_STARTED) {
            c = callId == null ? null : SYSTEM_STARTED.remove(callId);
        }
        if (c == null) {
            return false;
        }
        // Already dialing from adopt(); the report is the app saying it has
        // taken the call on, which is exactly what the acknowledgement means.
        Calls.deliverAck(requestId, true, 0, null);
        return true;
    }

    /// Forgets a system-started call that was never reported back.
    static void forgetSystemStarted(String callId) {
        synchronized (SYSTEM_STARTED) {
            if (callId != null) {
                SYSTEM_STARTED.remove(callId);
            }
        }
    }

    /// Parks the request id a forthcoming Telecom callback will answer.
    ///
    /// The address is parked with it so an incoming request that lost this
    /// bridge's extras can still be recognised as THIS report rather than as
    /// a call the system placed on its own; see adopt().
    static void expectReport(int requestId, String callId, String address,
            boolean incoming) {
        synchronized (PENDING_REPORTS) {
            PENDING_REPORTS.put(callId, Integer.valueOf(requestId));
            if (address != null) {
                List<Parked> waiting = PENDING_ADDRESSES.get(address);
                if (waiting == null) {
                    waiting = new ArrayList<Parked>();
                    PENDING_ADDRESSES.put(address, waiting);
                }
                waiting.add(new Parked(callId, incoming));
            }
        }
    }

    /// The OLDEST pending report to this address, or null.
    ///
    /// Oldest first because Telecom answers in the order it was asked, so the
    /// first callback for an address belongs to the first report made to it.
    private static String pendingReportFor(Uri address, boolean incoming) {
        if (address == null) {
            return null;
        }
        synchronized (PENDING_REPORTS) {
            List<Parked> waiting = PENDING_ADDRESSES.get(address.toString());
            if (waiting == null) {
                return null;
            }
            for (Parked p : waiting) {
                if (p.incoming == incoming) {
                    return p.callId;
                }
            }
            return null;
        }
    }

    /// Removes a parked report AND the address parked with it, answering the
    /// request id it carried.
    ///
    /// The single place a report leaves the tables, so the two cannot
    /// diverge. They did: a reset cleared the reports and left the addresses,
    /// and a later system-placed call to the same address matched a stale id
    /// -- adopted as an app report, acknowledging nothing and never
    /// delivering startCallRequested, so the app never heard about the call
    /// at all. The same leak sat in the single-outstanding-report fallback
    /// below, which removed a report by a different key.
    ///
    /// Must be called holding PENDING_REPORTS.
    private static Integer takeReportLocked(String callId) {
        if (callId == null) {
            return null;
        }
        for (Map.Entry<String, List<Parked>> e : PENDING_ADDRESSES.entrySet()) {
            boolean removed = false;
            for (Parked p : e.getValue()) {
                if (p.callId.equals(callId)) {
                    e.getValue().remove(p);
                    removed = true;
                    break;
                }
            }
            if (removed) {
                if (e.getValue().isEmpty()) {
                    PENDING_ADDRESSES.remove(e.getKey());
                }
                break;
            }
        }
        return PENDING_REPORTS.remove(callId);
    }

    /// Answers a parked report that Telecom never called back about.
    static void failParkedReport(String callId, int error, String message) {
        answerReport(callId, false, error, message);
    }

    /// The connection for a call id, or null.
    static CN1Connection find(String callId) {
        synchronized (CONNECTIONS) {
            return CONNECTIONS.get(callId);
        }
    }

    /// Forgets a connection.
    static void forget(String callId) {
        CN1Connection gone;
        synchronized (CONNECTIONS) {
            gone = CONNECTIONS.remove(callId);
        }
        // And every action still outstanding ON that connection. A deferred
        // answer whose call the far end ended kept its token in PENDING, so
        // the safety timer reached completeAction, found the entry, treated
        // a destroyed connection as live and ran failAnswer -- delivering
        // callEnded(FAILED) for a call that had already ended for another
        // reason, after the app had been told about the real one.
        //
        // Dropped under the same monitor completeAction takes, so a timer
        // that gets there first finds nothing rather than half a teardown.
        //
        // Nothing is answered here: these are actions the SYSTEM asked for,
        // and the facade's own safety net fails the CallAction it handed the
        // app. Only the native bookkeeping is dropped, so completeAction
        // reports the action as no longer held -- which is exactly what it
        // is -- instead of acting on a destroyed connection.
        if (gone != null) {
            synchronized (PENDING) {
                Iterator<Map.Entry<Long, PendingAction>> it =
                        PENDING.entrySet().iterator();
                while (it.hasNext()) {
                    if (it.next().getValue().connection == gone) { //NOPMD CompareObjectsWithEquals
                        it.remove();
                    }
                }
            }
        }
        // A system-started call the app never reported back is gone too;
        // leaving its id here would have a later report with the same id
        // adopt a connection that no longer exists.
        forgetSystemStarted(callId);
    }

    /// Ends every call, for a provider that has gone away.
    static void endAll() {
        CN1Connection[] all;
        synchronized (CONNECTIONS) {
            all = CONNECTIONS.values().toArray(new CN1Connection[CONNECTIONS.size()]);
            CONNECTIONS.clear();
        }
        // The parked system starts go too. A call the system asked this app
        // to place is waiting on an ASYNCHRONOUS reportOutgoing, and a reset
        // destroys its connection -- so a report that arrives afterwards
        // would be adopted and acknowledged as successful, publishing a
        // dialing session in Java for a Telecom call that no longer exists.
        synchronized (SYSTEM_STARTED) {
            SYSTEM_STARTED.clear();
        }
        for (CN1Connection c : all) {
            c.setDisconnected(new DisconnectCause(DisconnectCause.CANCELED));
            c.destroy();
        }
    }

    /// Allocates a token for a system-originated action.
    long nextActionToken(CN1Connection c, int kind) {
        long token = TOKENS.getAndIncrement();
        synchronized (PENDING) {
            PENDING.put(Long.valueOf(token), new PendingAction(c, kind));
        }
        return token;
    }

    /// Answers a system-originated action.
    ///
    /// A second answer for the same token is ignored rather than treated as
    /// an error: the facade's safety net and a slow application may both
    /// answer, and the race between them is not worth making anyone think
    /// about.
    static boolean completeAction(long token, boolean fulfilled) {
        PendingAction a;
        synchronized (PENDING) {
            a = PENDING.remove(Long.valueOf(token));
        }
        if (a == null) {
            // Telecom no longer has this action -- the connection was torn
            // down while the request sat in Java. Saying so keeps the caller
            // from applying a local effect the system will not match.
            return false;
        }
        if (fulfilled) {
            if (a.kind == CN1Connection.ACTION_ANSWER) {
                // The audio session is announced HERE, not in onAnswer: an
                // app that defers the answer while it negotiates signalling
                // must not be told its media can start before it has accepted
                // the call.
                a.connection.answerFulfilled();
            }
            // A reject or a hang-up the app agreed to still has to be carried
            // out. Telecom asked; delivering the request to Java and letting
            // the facade fulfil it is not the same as ending the call, and
            // without this the call stayed alive in Telecom and in
            // CONNECTIONS after the user had hung up.
            if (a.kind == CN1Connection.ACTION_REJECT) {
                a.connection.finish(CallEndReason.FILTERED);
                forget(a.connection.getCallId());
            } else if (a.kind == CN1Connection.ACTION_DISCONNECT) {
                a.connection.finish(CallEndReason.LOCAL_ENDED);
                forget(a.connection.getCallId());
            }
            return true;
        }
        // Telecom has no "this action failed" channel, so the only honest way
        // to report that the app could not do what was asked is to end the
        // call rather than leave the system UI showing a state the app is not
        // in.
        if (a.kind == CN1Connection.ACTION_ANSWER) {
            // Through the full teardown, not just destroy(): the answer had
            // already activated the connection and announced the audio
            // session, so media has to be stopped and the facade has to hear
            // that the call is over.
            a.connection.failAnswer();
            forget(a.connection.getCallId());
        } else if (a.kind == CN1Connection.ACTION_HOLD) {
            // onHold() moved Telecom before the app was asked, the way
            // Telecom's own API requires, so a refusal has to move it back --
            // otherwise the system holds a call the app still considers
            // active, and nothing ever resumes it.
            a.connection.setActive();
        } else if (a.kind == CN1Connection.ACTION_UNHOLD) {
            a.connection.setOnHold();
        } else if (a.kind == CN1Connection.ACTION_START) {
            // The app declined a call the SYSTEM asked it to place. Telecom
            // is already showing it as dialing, so leaving it would strand a
            // call nothing will ever connect.
            a.connection.finish(CallEndReason.FAILED);
            forget(a.connection.getCallId());
        }
        return true;
    }

    /// The current audio route ordinal.
    static int getRoute() {
        return route;
    }

    /// Records the current audio route.
    static void setRoute(int ordinal) {
        route = ordinal;
    }

    /// Whether this app currently owns a call.
    ///
    /// The difference between "somebody is in a call" and "somebody ELSE is",
    /// which TelecomManager.isInCall cannot express.
    static boolean hasOwnCalls() {
        synchronized (CONNECTIONS) {
            return !CONNECTIONS.isEmpty();
        }
    }

    /// Clears every static table, for a provider reset.
    static void reset() {
        CN1CallNotifications.dismissAll();
        endAll();
        synchronized (PENDING) {
            PENDING.clear();
        }
        synchronized (PENDING_REPORTS) {
            // BOTH, or the addresses outlive the reports they belong to and
            // a later system-placed call to one of them is mistaken for an
            // app report that no longer exists.
            PENDING_REPORTS.clear();
            PENDING_ADDRESSES.clear();
        }
        route = CallAudioRoute.EARPIECE.ordinal();
    }

    /// The extras key carrying the portable call id through Telecom.
    static final String EXTRA_CALL_ID = "com.codename1.call.ID";

    /// The extras key carrying the call's initial video state.
    static final String EXTRA_VIDEO = "com.codename1.call.VIDEO";
}
