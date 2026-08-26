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

import com.codename1.call.CallError;
import com.codename1.call.session.CallAudioRoute;
import com.codename1.call.session.Calls;

import java.util.HashMap;
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

    /// The request id of the report that is waiting for Telecom's answer.
    ///
    /// Telecom does not carry the caller's request id through
    /// `addNewIncomingCall`, so it is parked here between the call and the
    /// callback. One at a time is enough: `TelecomManager` serializes them.
    private static volatile int pendingReportRequest = -1;

    private static volatile String pendingReportCallId;

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
        refuse();
    }

    @Override
    public void onCreateOutgoingConnectionFailed(PhoneAccountHandle handle,
            ConnectionRequest request) {
        refuse();
    }

    private Connection adopt(ConnectionRequest request, boolean incoming) {
        String id = request == null || request.getExtras() == null ? null
                : request.getExtras().getString(EXTRA_CALL_ID);
        if (id == null) {
            id = pendingReportCallId;
        }
        if (id == null) {
            // Nothing can be routed to a call with no identifier, so refusing
            // is the honest answer rather than creating an orphan Telecom
            // knows about and this app cannot address.
            refuse();
            return null;
        }
        CN1Connection c = new CN1Connection(this, id);
        c.setInitializing();
        if (incoming) {
            c.setRinging();
        } else {
            c.setDialing();
        }
        synchronized (CONNECTIONS) {
            CONNECTIONS.put(id, c);
        }
        answerReport(true, 0, null);
        return c;
    }

    private void refuse() {
        answerReport(false, CallError.CALL_REFUSED.ordinal(),
                "Telecom refused the call: an emergency call is in progress,"
                + " another application holds a call, or calling is switched"
                + " off for this app");
    }

    private static void answerReport(boolean ok, int error, String message) {
        int id = pendingReportRequest;
        pendingReportRequest = -1;
        pendingReportCallId = null;
        if (id >= 0) {
            Calls.deliverAck(id, ok, error, message);
        }
    }

    /// Parks the request id a forthcoming Telecom callback will answer.
    static void expectReport(int requestId, String callId) {
        pendingReportRequest = requestId;
        pendingReportCallId = callId;
    }

    /// Answers a parked report that Telecom never called back about.
    static void failParkedReport(int error, String message) {
        answerReport(false, error, message);
    }

    /// The connection for a call id, or null.
    static CN1Connection find(String callId) {
        synchronized (CONNECTIONS) {
            return CONNECTIONS.get(callId);
        }
    }

    /// Forgets a connection.
    static void forget(String callId) {
        synchronized (CONNECTIONS) {
            CONNECTIONS.remove(callId);
        }
    }

    /// Ends every call, for a provider that has gone away.
    static void endAll() {
        CN1Connection[] all;
        synchronized (CONNECTIONS) {
            all = CONNECTIONS.values().toArray(new CN1Connection[CONNECTIONS.size()]);
            CONNECTIONS.clear();
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
    static void completeAction(long token, boolean fulfilled) {
        PendingAction a;
        synchronized (PENDING) {
            a = PENDING.remove(Long.valueOf(token));
        }
        if (a == null || fulfilled) {
            return;
        }
        // Telecom has no "this action failed" channel, so the only honest way
        // to report that the app could not do what was asked is to end the
        // call rather than leave the system UI showing a state the app is not
        // in.
        if (a.kind == CN1Connection.ACTION_ANSWER) {
            a.connection.setDisconnected(new DisconnectCause(DisconnectCause.ERROR));
            a.connection.destroy();
            forget(a.connection.getCallId());
        }
    }

    /// The current audio route ordinal.
    static int getRoute() {
        return route;
    }

    /// Records the current audio route.
    static void setRoute(int ordinal) {
        route = ordinal;
    }

    /// Clears every static table, for a provider reset.
    static void reset() {
        endAll();
        synchronized (PENDING) {
            PENDING.clear();
        }
        route = CallAudioRoute.EARPIECE.ordinal();
    }

    /// The extras key carrying the portable call id through Telecom.
    static final String EXTRA_CALL_ID = "com.codename1.call.ID";
}
