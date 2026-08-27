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
        CallBridge b;
        // The listener, the readiness and the drain decision under ONE
        // ordering. Split, a call clearing the listener could pause after
        // removing it, a second could install a replacement and publish
        // "listening", and the first could then publish "not listening" and
        // clear the drain -- leaving a registered listener while iOS queued
        // its pushed calls and CallKit actions until they expired.
        synchronized (LISTENERS) {
            LISTENERS.clear();
            if (l != null) {
                LISTENERS.add(l);
            }
            // BEFORE the bridge lookup. Returning early when there is no port
            // yet -- setListener from an app's init runs before Display.init
            // -- left this false while a listener was stored, so the
            // readiness the bridge later published was "not listening".
            //
            // Through the shared flag: an action listener registered without
            // VoipPush counts too, and turning this off here must not silence
            // a Calls listener that is still installed.
            CallRequests.setPushesWanted(l != null);
            drainWanted = l != null;
            b = CallRequests.bridge();
        }
        if (b != null) {
            drainIfWanted(b);
        }
        // The TOKEN this listener has never been told about.
        //
        // PushKit supplies credentials during a cold launch -- the registry
        // is created in willFinishLaunching, so this is the ordinary
        // ordering, not a race -- and with no listener installed yet the
        // delivery was dropped. The documented setListener(); register();
        // sequence then got the same token back from native, so "changed" was
        // false and tokenChanged never fired at all: an app following the
        // example, which ignores the returned resource, never registered its
        // token with its server and could not be called.
        //
        // Round 46 declined to hold tokens on the grounds that replaying a
        // stale one would tell a listener its token had changed when it had
        // not. That is right for a listener that has already been told and
        // wrong for one that has not -- which is this case, and which is why
        // the question is asked per listener rather than per token.
        String known;
        synchronized (VoipPush.class) {
            known = token;
        }
        if (l != null && known != null) {
            // Checked against the current token at delivery, because
            // setListener can run off the EDT and a
            // rotation landing between this snapshot and the delivery would
            // queue the NEW token first and this stale one after it. A
            // listener that updates its server in the callback would finish
            // registered under a token that is no longer valid and stop
            // receiving calls -- the exact failure the replay exists to
            // prevent, arrived at from the other side.
            //
            // Addressed to THIS listener, not to whoever is registered when
            // it runs. Two setListener calls before the EDT drains queued two
            // replays of the same value, and both found the replacement
            // installed -- so it heard tokenChanged twice, which the
            // value check cannot filter because the value never changed.
            //
            // Not covered by a unit test on purpose: core-unittests never
            // initialises Display, so post() runs every delivery inline on
            // the calling thread and nothing can overtake anything. A test
            // there would assert the harness, not this.
            post(new Delivery(null, known, l));
        }
        // Anything that arrived before this listener existed. Taken out under
        // the monitor and delivered outside it, so a listener that installs
        // another one from callReceived does not deadlock.
        Delivery[] held;
        synchronized (LISTENERS) {
            if (l == null || HELD.isEmpty()) {
                return;
            }
            held = HELD.toArray(new Delivery[HELD.size()]);
            HELD.clear();
        }
        for (Delivery d : held) {
            post(d);
        }
    }

    /// Whether a listener is waiting for the calls the port already has.
    /// Guarded by LISTENERS, like the listener it belongs to.
    private static boolean drainWanted;

    /// Calls delivered while no push listener was installed.
    ///
    /// The port's readiness flag is the UNION of the two listener kinds --
    /// an app that registers a Calls action listener makes it true without
    /// touching VoipPush -- so a pushed call can be drained before any push
    /// listener exists. Dropping it there lost the call for good: the drain
    /// had already claimed it, so the platform's own unanswered-call
    /// watchdog would not retire it either, and the system went on ringing a
    /// call the app was never told about.
    ///
    /// Held here instead, and replayed by [#setListener]. Guarded by
    /// LISTENERS, because whether to hold is decided by whether one exists.
    private static final List<Delivery> HELD = new ArrayList<Delivery>();

    /// Drains once, when there is somewhere to drain from.
    ///
    /// A listener installed before the port existed had nothing to ask, and
    /// no later operation asked on its behalf -- so a call the native side
    /// had already reported was never handed over.
    ///
    /// @hidden not part of the public API.
    public static void drainIfWanted(CallBridge b) {
        // The same monitor the flag is written under; the drain itself
        // happens outside it.
        synchronized (LISTENERS) {
            if (!drainWanted || b == null) {
                return;
            }
            drainWanted = false;
        }
        b.drainPendingCalls(CallRequests.nextId());
    }

    /// Registers for VoIP pushes, resolving with the token to give the
    /// application's server.
    ///
    /// Errors with [com.codename1.call.CallError#NOT_SUPPORTED] where
    /// [#isSupported()] is false, rather than resolving with a token that
    /// could never receive anything.
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
        // The CACHE goes here, not only wherever a port happens to tell us.
        // An explicit opt-out means the token this app holds is no longer
        // one anybody should act on, and leaving it left getToken() handing
        // back a dead value and a later setListener replaying it as though
        // it were current. iOS does deliver a null now, but that is an
        // asynchronous courtesy from one port; the facade's own contract
        // should not depend on it.
        synchronized (VoipPush.class) {
            token = null;
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
                receivedAt)));
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
        // A CALL with nobody to hand it to is held, not dropped; see HELD.
        // A token is not: it is re-read from getToken() by whoever asks, and
        // replaying a stale one at a listener that registers later would say
        // "your token just changed" about a token that did not.
        if (d.call != null) {
            synchronized (LISTENERS) {
                if (LISTENERS.isEmpty()) {
                    HELD.add(d);
                    return;
                }
            }
        }
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
        /// The listener this delivery was made FOR, or null for anyone.
        ///
        /// A replay belongs to the listener that installed itself; a rotation
        /// belongs to whoever is listening when it lands.
        private final VoipPushListener addressee;

        Delivery(PushedCall call) {
            this(call, null, null);
        }

        Delivery(PushedCall call, String newToken) {
            this(call, newToken, null);
        }

        Delivery(PushedCall call, String newToken,
                VoipPushListener addressee) {
            this.call = call;
            this.newToken = newToken;
            this.addressee = addressee;
        }

        /// Whether a later delivery has already superseded this one.
        ///
        /// Asked as "is this still the current token" rather than counted.
        /// A version counter has to be advanced on exactly the deliveries
        /// that replace a queued one and on no others, and getting that
        /// wrong is silent in both directions: advancing it for a settlement
        /// that posts nothing of its own -- the iOS drain answers each
        /// waiting register() separately with the SAME value -- retires a
        /// queued tokenChanged() that nothing replaces, so the listener never
        /// hears the token at all. Comparing the value cannot express either
        /// mistake, because the invariant IS the comparison: a listener is
        /// never told a token that is not the one getToken() would return.
        private boolean superseded() {
            if (call != null) {
                return false;
            }
            synchronized (VoipPush.class) {
                return newToken == null ? token != null
                        : !newToken.equals(token);
            }
        }

        @Override
        public void run() {
            if (superseded()) {
                // A rotation overtook this one; the listener has been told
                // the newer value and must not now be told an older one.
                return;
            }
            VoipPushListener[] ls = listeners();
            if (ls.length == 0) {
                // The listener went away between post() deciding there was
                // one and this reaching the EDT. A CALL dropped here is lost
                // for good: the native drain already claimed it, so its
                // unclaimed-call timeout no longer owns it, and HELD is only
                // consulted by setListener. Put it back so the next listener
                // gets it.
                //
                // A token is not requeued, for the reason post() gives.
                if (call == null) {
                    return;
                }
                // RE-READ under the monitor, and only held if it is still
                // true. Between the empty snapshot above and this block a
                // setListener can install a listener and drain an
                // already-empty HELD -- so adding here would park the call
                // behind a listener that is live right now, and nothing would
                // look at HELD again until the NEXT setListener.
                synchronized (LISTENERS) {
                    if (LISTENERS.isEmpty()) {
                        HELD.add(this);
                        return;
                    }
                    ls = LISTENERS.toArray(new VoipPushListener[LISTENERS.size()]);
                }
            }
            for (VoipPushListener l : ls) {
                if (addressee != null && addressee != l) { //NOPMD CompareObjectsWithEquals
                    // Somebody else's replay. Identity, not equals: two
                    // listeners that compare equal are still two
                    // registrations, and only the one that asked should be
                    // told what it missed.
                    continue;
                }
                if (call != null) {
                    l.callReceived(current());
                } else {
                    l.tokenChanged(newToken);
                }
            }
        }

        /// The call as it stands NOW, rather than as it stood when it was
        /// queued.
        ///
        /// A held call waits in HELD for as long as it takes a listener to
        /// arrive, and the call itself does not wait with it: the far end
        /// hangs up, or the platform retires it, and Calls marks the session
        /// ENDED and forgets it. PushedCall is immutable, so the replay went
        /// on saying stale=false and handed the new listener a dead call
        /// described as live -- inviting it to attach signalling and media,
        /// which is exactly what isStale() exists to prevent it doing.
        ///
        /// Re-read here rather than mended when the call ends, because this
        /// is the point where the answer is used; a flag set earlier would
        /// have to be kept right through every later transition.
        private PushedCall current() {
            if (call.isStale()) {
                // Already stale, and never registered: a stale call gets a
                // DETACHED session, so the lookup below would say "gone"
                // about something that was never there and this would rebuild
                // it for no reason.
                return call;
            }
            CallSession s = call.getSession();
            if (s != null && s.getState() != CallState.ENDED
                    && Calls.getSession(s.getCallId()) != null) {
                return call;
            }
            return new PushedCall(s, call.getData(), true,
                    call.isIdentifierSynthesized(), call.getReceivedAt());
        }
    }

    /// Clears the listener and cached token.
    ///
    /// @hidden not part of the public API; test-only.
    public static void resetForTest() {
        synchronized (LISTENERS) {
            LISTENERS.clear();
            HELD.clear();
        }
        synchronized (VoipPush.class) {
            token = null;
        }
    }
}
