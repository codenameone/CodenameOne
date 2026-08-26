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
package com.codename1.call.voip;

import com.codename1.call.CallError;
import com.codename1.call.CallException;
import com.codename1.call.CallHandle;
import com.codename1.call.CallId;
import com.codename1.call.CallState;
import com.codename1.call.session.CallSession;
import com.codename1.call.session.Calls;
import com.codename1.call.spi.CallBridge;
import com.codename1.impl.async.EdtResult;
import com.codename1.impl.call.CallRequests;
import com.codename1.impl.call.CallWire;
import com.codename1.util.AsyncResource;
import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.List;

/// Ringing when the app is not running.
///
/// A VoIP push is a push the operating system delivers straight to the
/// calling machinery, launching the app if it has to. It is the only way an
/// app that is not running can make the phone ring.
///
/// ```java
/// VoipPush.setListener(new VoipPushListener() {
///     public void callReceived(PushedCall call) {
///         if (call.isStale()) { history.addMissed(call.getHandle()); return; }
///         signalling.attach(call.getSession().getCallId(), call.getData());
///     }
///     public void tokenChanged(String token) { server.registerVoip(token); }
/// });
/// VoipPush.register();
/// ```
///
/// #### The payload has a fixed shape, and the server must honour it
///
/// Because the call is reported to the system before any Java runs, the
/// **native code parses the push itself**. It looks for one key:
///
/// ```json
/// { "cn1call": { "uuid": "6B29FC40-CA47-1067-B31D-00DD010662DA",
///                "handle": "+14155551212", "handleType": "phoneNumber",
///                "displayName": "Jane Doe", "video": false,
///                "ttl": 30, "data": "opaque, handed back untouched" } }
/// ```
///
/// `uuid` and `handle` are required; the rest have defaults from the build
/// hints. `data` is never parsed by the framework. Sending a payload without
/// `cn1call` means the app is woken and nothing rings -- on iOS, that is the
/// case that gets the app killed.
///
/// To **retract** a call that was cancelled before it was answered, send the
/// same `uuid` with `"cancel": true`.
///
/// #### Set the listener before registering
///
/// Calls that arrived before the app was listening are held and delivered
/// when [#setListener] installs one. An app that registers first and listens
/// later still gets them -- the queue is drained on the first listener -- but
/// an app that never sets a listener will find its calls timed out and ended
/// by the platform.
public final class VoipPush {

    private static final List<VoipPushListener> LISTENERS =
            new ArrayList<VoipPushListener>();

    private static String token;

    private VoipPush() {
    }

    /// Whether this platform can be woken by a VoIP push.
    public static boolean isSupported() {
        CallBridge b = CallRequests.bridge();
        return b != null && b.isVoipPushSupported();
    }

    /// Installs the listener and immediately drains any calls that arrived
    /// before the app was listening.
    public static void setListener(VoipPushListener l) {
        synchronized (LISTENERS) {
            LISTENERS.clear();
            if (l != null) {
                LISTENERS.add(l);
            }
        }
        // BEFORE the bridge lookup. Returning early when there is no port
        // yet -- setListener from an app's init runs before Display.init --
        // left this false while a listener was stored, so the readiness the
        // bridge later published was "not listening" and the cold-start calls
        // it was registered for stayed queued until they expired.
        //
        // Through the shared flag: an action listener registered without
        // VoipPush counts too, and turning this off here must not silence a
        // Calls listener that is still installed.
        CallRequests.setPushesWanted(l != null);
        drainWanted = l != null;
        CallBridge b = CallRequests.bridge();
        if (b == null) {
            return;
        }
        drainIfWanted(b);
    }

    /// Whether a listener is waiting for the calls the port already has.
    private static boolean drainWanted;

    /// Drains once, when there is somewhere to drain from.
    ///
    /// A listener installed before the port existed had nothing to ask, and
    /// no later operation asked on its behalf -- so a call the native side
    /// had already reported was never handed over.
    ///
    /// @hidden not part of the public API.
    public static void drainIfWanted(CallBridge b) {
        synchronized (VoipPush.class) {
            if (!drainWanted || b == null) {
                return;
            }
            drainWanted = false;
        }
        b.drainPendingCalls(CallRequests.nextId());
    }

    /// Registers for VoIP pushes, resolving with the token to give the
    /// application's server.
    public static AsyncResource<String> register() {
        CallBridge b = CallRequests.bridge();
        if (b == null) {
            EdtResult<String> r = new EdtResult<String>();
            r.error(new CallException(CallError.NOT_SUPPORTED));
            return r;
        }
        int id = CallRequests.nextId();
        EdtResult<String> r = CallRequests.openString(id);
        b.registerVoipPush(id);
        return r;
    }

    /// Stops VoIP push delivery.
    public static void unregister() {
        CallBridge b = CallRequests.bridge();
        if (b != null) {
            b.unregisterVoipPush(CallRequests.nextId());
        }
    }

    /// The last known token, or null.
    public static String getToken() {
        synchronized (VoipPush.class) {
            return token;
        }
    }

    // ------------------------------------------------------------------
    // Entry points the ports call up into.
    // ------------------------------------------------------------------

    /// Delivers one call the native side already reported to the system.
    ///
    /// @hidden not part of the public API.
    public static void deliverPushedCall(String callId, String handleWire,
            String displayName, boolean video, boolean stale,
            boolean synthesizedId, String data, long receivedAt) {
        String id = CallId.normalize(callId);
        if (id == null) {
            // Nothing useful can be done with a record whose identifier did
            // not survive; dropping one row is better than failing the drain
            // and losing the rest of the batch.
            return;
        }
        CallHandle handle = CallWire.decodeHandle(handleWire);
        if (handle == null) {
            // A blank rather than an empty string, matching what the native
            // side already showed the user: CN1Call.m substitutes " " for a
            // handle with no value before reporting to CallKit, so the call
            // rang with an empty caller and this describes the same thing.
            //
            // CallHandle rejects "" outright, so the previous fallback threw
            // and took the rest of the drained batch with it -- one malformed
            // push losing every good call queued behind it.
            handle = CallHandle.generic(" ");
        }
        CallSession existing = Calls.getSession(id);
        CallSession session;
        if (existing != null) {
            session = existing;
        } else if (stale) {
            // Detached on purpose. A stale call is already over, so
            // registering it would leave every missed or cancelled cold-start
            // push sitting in getSessions() for the life of the process --
            // and those APIs promise CURRENT calls. The app still gets a
            // session to read the handle and the id off, which is all a
            // missed-call log needs.
            session = Calls.detachedSession(id, handle, displayName);
        } else {
            session = Calls.adoptSession(id, handle, displayName,
                    CallState.RINGING);
        }
        post(new Delivery(new PushedCall(session, data, stale, synthesizedId,
                receivedAt), null));
    }

    /// Answers the drain with how many calls it produced.
    ///
    /// @hidden not part of the public API.
    public static void deliverPendingCallsDrained(int requestId, int count) {
        EdtResult<Integer> r = CallRequests.takeCount(requestId);
        if (r != null) {
            r.complete(Integer.valueOf(count));
        }
    }

    /// Delivers the VoIP push token.
    ///
    /// @hidden not part of the public API.
    public static void deliverToken(int requestId, String value) {
        boolean changed;
        synchronized (VoipPush.class) {
            changed = value == null ? token != null : !value.equals(token);
            token = value;
        }
        EdtResult<String> r = CallRequests.takeString(requestId);
        if (r != null) {
            r.complete(value);
        }
        if (changed) {
            // Only on a real change. A port with several registrations
            // waiting settles them one at a time through here, and a
            // rotation that produces the same token is not a rotation --
            // neither is a reason to tell the app its token changed.
            post(new Delivery(null, value));
        }
    }

    /// Fails a registration.
    ///
    /// @hidden not part of the public API.
    public static void deliverRegistrationFailed(int requestId,
            int errorOrdinal, String message) {
        EdtResult<String> r = CallRequests.takeString(requestId);
        if (r != null) {
            r.error(CallWire.decodeError(errorOrdinal, message));
        }
    }

    private static void post(Delivery d) {
        if (Display.isInitialized() && !Display.getInstance().isEdt()) {
            Display.getInstance().callSerially(d);
        } else {
            d.run();
        }
    }

    private static VoipPushListener[] listeners() {
        synchronized (LISTENERS) {
            return LISTENERS.toArray(new VoipPushListener[LISTENERS.size()]);
        }
    }

    /// One inbound event carried to the EDT. A static class so it holds no
    /// synthetic reference to an enclosing scope.
    private static final class Delivery implements Runnable {
        private final PushedCall call;
        private final String newToken;

        Delivery(PushedCall call, String newToken) {
            this.call = call;
            this.newToken = newToken;
        }

        @Override
        public void run() {
            VoipPushListener[] ls = listeners();
            for (VoipPushListener l : ls) {
                if (call != null) {
                    l.callReceived(call);
                } else {
                    l.tokenChanged(newToken);
                }
            }
        }
    }

    /// Clears the listener and cached token.
    ///
    /// @hidden not part of the public API; test-only.
    public static void resetForTest() {
        synchronized (LISTENERS) {
            LISTENERS.clear();
        }
        synchronized (VoipPush.class) {
            token = null;
        }
    }
}
