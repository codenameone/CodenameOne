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
package com.codename1.vpn.profile;

import com.codename1.impl.async.EdtResult;
import com.codename1.impl.vpn.VpnRequests;
import com.codename1.impl.vpn.VpnWire;
import com.codename1.io.Log;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;
import com.codename1.vpn.VpnError;
import com.codename1.vpn.VpnException;
import com.codename1.vpn.VpnStatus;
import com.codename1.vpn.spi.VpnBridge;

import java.util.ArrayList;
import java.util.List;

/// Installing and controlling a VPN configuration the operating system runs
/// for you.
///
/// ```java
/// if (Vpn.isSupported()) {
///     Vpn.install(new VpnProfile("vpn.example.com")
///             .usernamePassword("alice", secret))
///        .ready(v -> Vpn.start());
/// }
/// ```
///
/// #### The tunnel is the platform's, not this app's
///
/// Nothing here carries packets. The configuration is handed to the operating
/// system, which runs its own IKEv2 or IPsec client; the app starts it, stops
/// it and watches it. An app that needs to implement the tunnel itself wants
/// [com.codename1.vpn.tunnel] instead, and should read that package's
/// documentation before committing to it.
///
/// #### The user is asked, every time a configuration is installed
///
/// [#install] shows a system prompt on both platforms and there is no way
/// around it. A declined prompt fails with [VpnError#USER_DECLINED], which is
/// an ordinary outcome and not an error to report to the user again.
///
/// #### One configuration per app
///
/// Both platforms give an app a single managed configuration, so [#install]
/// replaces whatever was there rather than adding to it.
public final class Vpn {

    private static final List<VpnStatusListener> LISTENERS =
            new ArrayList<VpnStatusListener>();

    private Vpn() {
    }

    /// Whether this platform can install and control a VPN configuration.
    ///
    /// False on Android below API 30, and on every port with no VPN
    /// machinery. Note this is a different question from whether a VPN is
    /// *running* -- `com.codename1.io.NetworkManager#isVPNActive()` answers
    /// that one, on many more platforms and with no entitlement.
    public static boolean isSupported() {
        VpnBridge b = VpnRequests.bridge();
        return b != null && b.isVpnSupported();
    }

    /// The `VpnBridge.CAPABILITY_*` mask this platform supports.
    public static int getCapabilities() {
        VpnBridge b = VpnRequests.bridge();
        return b == null ? 0 : b.getVpnCapabilities();
    }

    /// Where the tunnel is right now.
    public static VpnStatus getStatus() {
        VpnBridge b = VpnRequests.bridge();
        if (b == null) {
            return VpnStatus.NOT_CONFIGURED;
        }
        return VpnWire.status(b.getVpnStatus());
    }

    /// Installs or replaces this app's configuration, after asking the user.
    public static AsyncResource<Boolean> install(VpnProfile profile) {
        VpnBridge b = VpnRequests.bridge();
        if (b == null) {
            return failed(VpnError.NOT_SUPPORTED, null);
        }
        if (profile == null) {
            return failed(VpnError.INVALID_CONFIGURATION,
                    "A profile is required");
        }
        int id = VpnRequests.nextId();
        EdtResult<Boolean> r = VpnRequests.openAck(id);
        b.installProfile(id, VpnWire.encodeProfile(profile));
        return r;
    }

    /// Removes this app's configuration.
    public static AsyncResource<Boolean> remove() {
        VpnBridge b = VpnRequests.bridge();
        if (b == null) {
            return failed(VpnError.NOT_SUPPORTED, null);
        }
        int id = VpnRequests.nextId();
        EdtResult<Boolean> r = VpnRequests.openAck(id);
        b.removeProfile(id);
        return r;
    }

    /// Reads back the installed configuration, resolving null when none is
    /// installed.
    ///
    /// The result never carries the password -- the platform keeps it. See
    /// [VpnProfile#getPassword()].
    public static AsyncResource<VpnProfile> load() {
        final EdtResult<VpnProfile> out = new EdtResult<VpnProfile>();
        VpnBridge b = VpnRequests.bridge();
        if (b == null) {
            out.error(new VpnException(VpnError.NOT_SUPPORTED));
            return out;
        }
        int id = VpnRequests.nextId();
        EdtResult<String> raw = VpnRequests.openString(id);
        raw.onResult(new ProfileDecoder(out));
        b.loadProfile(id);
        return out;
    }

    /// Brings the tunnel up.
    public static AsyncResource<Boolean> start() {
        VpnBridge b = VpnRequests.bridge();
        if (b == null) {
            return failed(VpnError.NOT_SUPPORTED, null);
        }
        int id = VpnRequests.nextId();
        EdtResult<Boolean> r = VpnRequests.openAck(id);
        b.startVpn(id);
        return r;
    }

    /// Takes the tunnel down.
    public static AsyncResource<Boolean> stop() {
        VpnBridge b = VpnRequests.bridge();
        if (b == null) {
            return failed(VpnError.NOT_SUPPORTED, null);
        }
        int id = VpnRequests.nextId();
        EdtResult<Boolean> r = VpnRequests.openAck(id);
        b.stopVpn(id);
        return r;
    }

    /// Adds a status listener. Arrives on the EDT.
    public static void addStatusListener(VpnStatusListener l) {
        if (l == null) {
            return;
        }
        boolean first;
        synchronized (LISTENERS) {
            if (LISTENERS.contains(l)) {
                return;
            }
            first = LISTENERS.isEmpty();
            LISTENERS.add(l);
        }
        VpnBridge b = VpnRequests.bridge();
        if (first && b != null) {
            b.setStatusListening(true);
        }
    }

    /// Removes a status listener, stopping delivery when the last one goes.
    public static void removeStatusListener(VpnStatusListener l) {
        boolean last;
        synchronized (LISTENERS) {
            LISTENERS.remove(l);
            last = LISTENERS.isEmpty();
        }
        VpnBridge b = VpnRequests.bridge();
        if (last && b != null) {
            b.setStatusListening(false);
        }
    }

    // ------------------------------------------------------------------
    // Entry points the ports call up into.
    // ------------------------------------------------------------------

    /// Answers an acknowledged operation.
    ///
    /// @hidden not part of the public API.
    public static void deliverAck(int requestId, boolean ok, int errorOrdinal,
            String message) {
        EdtResult<Boolean> r = VpnRequests.takeAck(requestId);
        if (r == null) {
            return;
        }
        if (ok) {
            r.complete(Boolean.TRUE);
        } else {
            r.error(VpnWire.decodeError(errorOrdinal, message));
        }
    }

    /// Answers [#load] with a wire record, or the empty string for none.
    ///
    /// @hidden not part of the public API.
    public static void deliverProfile(int requestId, String profileWire) {
        EdtResult<String> r = VpnRequests.takeString(requestId);
        if (r != null) {
            r.complete(profileWire == null ? "" : profileWire);
        }
    }

    /// Fails a load.
    ///
    /// @hidden not part of the public API.
    public static void deliverProfileFailed(int requestId, int errorOrdinal,
            String message) {
        EdtResult<String> r = VpnRequests.takeString(requestId);
        if (r != null) {
            r.error(VpnWire.decodeError(errorOrdinal, message));
        }
    }

    /// The tunnel changed state.
    ///
    /// @hidden not part of the public API.
    public static void deliverStatusChanged(int statusOrdinal) {
        StatusEvent e = new StatusEvent(VpnWire.status(statusOrdinal));
        if (Display.isInitialized() && !Display.getInstance().isEdt()) {
            Display.getInstance().callSerially(e);
        } else {
            e.run();
        }
    }

    private static AsyncResource<Boolean> failed(VpnError e, String message) {
        EdtResult<Boolean> r = new EdtResult<Boolean>();
        r.error(message == null ? new VpnException(e)
                : new VpnException(e, message));
        return r;
    }

    private static VpnStatusListener[] listeners() {
        synchronized (LISTENERS) {
            return LISTENERS.toArray(new VpnStatusListener[LISTENERS.size()]);
        }
    }

    /// Carries a status change to the EDT. A static class so it holds no
    /// synthetic reference to an enclosing scope.
    private static final class StatusEvent implements Runnable {
        private final VpnStatus status;

        StatusEvent(VpnStatus status) {
            this.status = status;
        }

        @Override
        public void run() {
            VpnStatusListener[] ls = listeners();
            for (VpnStatusListener l : ls) {
                tell(l, status);
            }
        }

        /// Tells one listener, without letting it stop the others.
        ///
        /// A status change is a notification: nobody is waiting on an answer
        /// and every listener is entitled to hear it, so one that throws must
        /// not leave the rest showing the previous state. The call facade's
        /// notification arms are handled the same way.
        private void tell(VpnStatusListener l, VpnStatus s) {
            try {
                l.vpnStatusChanged(s);
            } catch (Throwable t) {
                if (Display.isInitialized()) {
                    Log.e(t);
                } else {
                    // Log.e goes through the platform implementation, which
                    // is null before Display.init.
                    t.printStackTrace(); //NOPMD AvoidPrintStackTrace
                }
            }
        }
    }

    /// Turns the port's record into a [VpnProfile], or null for none.
    private static final class ProfileDecoder
            implements com.codename1.util.AsyncResult<String> {
        private final EdtResult<VpnProfile> out;

        ProfileDecoder(EdtResult<VpnProfile> out) {
            this.out = out;
        }

        @Override
        public void onReady(String value, Throwable error) {
            if (error != null) {
                out.error(error);
            } else {
                out.complete(VpnWire.decodeProfile(value));
            }
        }
    }

    /// Clears every listener.
    ///
    /// @hidden not part of the public API; test-only.
    public static void resetForTest() {
        synchronized (LISTENERS) {
            LISTENERS.clear();
        }
    }
}
