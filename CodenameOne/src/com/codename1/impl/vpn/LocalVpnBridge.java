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
package com.codename1.impl.vpn;

import com.codename1.ui.Display;
import com.codename1.vpn.VpnError;
import com.codename1.vpn.VpnStatus;
import com.codename1.vpn.profile.Vpn;
import com.codename1.vpn.spi.VpnBridge;

import java.util.List;

/// A working VPN configuration store that tunnels nothing.
///
/// What the simulator, the desktop builds, the browser port and the unit
/// tests run against. Like the call simulation this is a **simulation and not
/// a stub**: installing asks a simulated user, connecting passes through
/// `CONNECTING` before `CONNECTED`, and a profile read back has no password,
/// because the real platforms keep the secret and an app written against a
/// simulation that handed it back would break on a device.
///
/// @hidden not part of the public API.
public class LocalVpnBridge implements VpnBridge {

    private static final int LATENCY_MILLIS = 40;

    private static final int CONNECT_MILLIS = 120;

    private List<Runnable> deferred;

    /// Set when the bridge is discarded; see [#retire()].
    private boolean retired;

    private boolean supported = true;
    private boolean userAccepts = true;
    private boolean authenticates = true;
    private String profileWire;
    private VpnStatus status = VpnStatus.NOT_CONFIGURED;
    private boolean listening;

    /// Holds every scheduled answer in `sink` instead of running it.
    public void setDeferred(List<Runnable> sink) {
        this.deferred = sink;
    }

    /// Whether this bridge claims VPN support.
    public void setSupported(boolean value) {
        this.supported = value;
    }

    /// Whether the simulated user accepts the install prompt. Setting this
    /// false reproduces the ordinary, non-exceptional decline both platforms
    /// make it easy to forget about.
    public void setUserAccepts(boolean value) {
        this.userAccepts = value;
    }

    /// Whether the simulated server accepts the credentials.
    public void setAuthenticates(boolean value) {
        this.authenticates = value;
    }

    /// The current simulated status.
    public VpnStatus getStatus() {
        return status;
    }

    @Override
    public boolean isVpnSupported() {
        return supported;
    }

    @Override
    public boolean isCustomTunnelSupported() {
        // False here as on every port: a packet tunnel the app implements is
        // not shipped, and a simulation that offered one would be the only
        // place an app's tunnel code appeared to work.
        return false;
    }

    @Override
    public int getVpnCapabilities() {
        if (!supported) {
            return 0;
        }
        // No CAPABILITY_ALWAYS_ON: no ordinary app can ask either platform
        // for it, so the simulation must not be the one place it works.
        return CAPABILITY_IKEV2 | CAPABILITY_IPSEC | CAPABILITY_ON_DEMAND;
    }

    @Override
    public int getVpnStatus() {
        return status.ordinal();
    }

    @Override
    public void installProfile(int requestId, String wire) {
        if (!supported) {
            fail(requestId, VpnError.NOT_SUPPORTED, null);
            return;
        }
        if (VpnWire.decodeProfile(wire) == null) {
            fail(requestId, VpnError.INVALID_CONFIGURATION,
                    "The profile has no server address");
            return;
        }
        if (!userAccepts) {
            fail(requestId, VpnError.USER_DECLINED,
                    "The user declined the VPN configuration prompt");
            return;
        }
        this.profileWire = wire;
        setStatus(VpnStatus.DISCONNECTED);
        ok(requestId);
    }

    @Override
    public void removeProfile(int requestId) {
        if (!supported) {
            fail(requestId, VpnError.NOT_SUPPORTED, null);
            return;
        }
        profileWire = null;
        setStatus(VpnStatus.NOT_CONFIGURED);
        ok(requestId);
    }

    @Override
    public void loadProfile(int requestId) {
        if (!supported) {
            later(LATENCY_MILLIS, new ProfileFailure(requestId,
                    VpnError.NOT_SUPPORTED.ordinal(), null));
            return;
        }
        // The password is stripped on the way out, because the platforms keep
        // it in their own keychain and never hand it back. A simulation that
        // returned it would let an app depend on something no device does.
        later(LATENCY_MILLIS, new ProfileDelivery(requestId,
                stripSecrets(profileWire)));
    }

    @Override
    public void startVpn(int requestId) {
        if (!supported) {
            fail(requestId, VpnError.NOT_SUPPORTED, null);
            return;
        }
        if (profileWire == null) {
            fail(requestId, VpnError.NOT_CONFIGURED, null);
            return;
        }
        setStatus(VpnStatus.CONNECTING);
        if (!authenticates) {
            later(CONNECT_MILLIS, new Transition(this, VpnStatus.DISCONNECTED));
            later(CONNECT_MILLIS, new AckDelivery(requestId, false,
                    VpnError.AUTHENTICATION_FAILED.ordinal(),
                    "The server refused the credentials"));
            return;
        }
        later(CONNECT_MILLIS, new Transition(this, VpnStatus.CONNECTED));
        later(CONNECT_MILLIS, new AckDelivery(requestId, true, 0, null));
    }

    @Override
    public void stopVpn(int requestId) {
        if (!supported) {
            fail(requestId, VpnError.NOT_SUPPORTED, null);
            return;
        }
        if (profileWire == null) {
            fail(requestId, VpnError.NOT_CONFIGURED, null);
            return;
        }
        setStatus(VpnStatus.DISCONNECTING);
        later(CONNECT_MILLIS, new Transition(this, VpnStatus.DISCONNECTED));
        later(CONNECT_MILLIS, new AckDelivery(requestId, true, 0, null));
    }

    @Override
    public void setStatusListening(boolean value) {
        this.listening = value;
    }

    /// Moves to a new status, telling the facade when anyone is listening.
    ///
    /// Public because the simulator drives it directly: a tunnel dropping
    /// underneath a running app is a state the app has to handle and cannot
    /// otherwise be produced on a desktop.
    public void setStatus(VpnStatus s) {
        this.status = s;
        if (listening) {
            later(LATENCY_MILLIS, new StatusDelivery(s.ordinal()));
        }
    }

    /// Removes the password and shared secret from a stored record.
    private static String stripSecrets(String wire) {
        if (wire == null || wire.length() == 0) {
            return "";
        }
        String[] f = com.codename1.impl.call.CallWire.split(wire);
        String[] out = new String[f.length];
        System.arraycopy(f, 0, out, 0, f.length);
        if (out.length > 5) {
            out[5] = "";
        }
        if (out.length > 6) {
            out[6] = "";
        }
        return com.codename1.impl.call.CallWire.join(out);
    }

    private void ok(int requestId) {
        later(LATENCY_MILLIS, new AckDelivery(requestId, true, 0, null));
    }

    private void fail(int requestId, VpnError e, String message) {
        later(LATENCY_MILLIS,
                new AckDelivery(requestId, false, e.ordinal(), message));
    }

    private void later(int millis, Runnable delivery) {
        List<Runnable> sink = deferred;
        if (sink != null) {
            sink.add(delivery);
            return;
        }
        if (Display.isInitialized()) {
            // Wrapped for the reason LocalCallBridge gives: setTimeout hands
            // back nothing to cancel, so an answer scheduled by a finished
            // test would otherwise fire into the next one.
            Display.getInstance().setTimeout(millis, new Retired(this, delivery));
            return;
        }
        delivery.run();
    }

    /// Drops every delivery this bridge has scheduled and refuses more.
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
        private final LocalVpnBridge bridge;
        private final Runnable delivery;

        Retired(LocalVpnBridge bridge, Runnable delivery) {
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
            Vpn.deliverAck(requestId, ok, error, message);
        }
    }

    private static final class ProfileDelivery implements Runnable {
        private final int requestId;
        private final String wire;

        ProfileDelivery(int requestId, String wire) {
            this.requestId = requestId;
            this.wire = wire;
        }

        @Override
        public void run() {
            Vpn.deliverProfile(requestId, wire);
        }
    }

    private static final class ProfileFailure implements Runnable {
        private final int requestId;
        private final int error;
        private final String message;

        ProfileFailure(int requestId, int error, String message) {
            this.requestId = requestId;
            this.error = error;
            this.message = message;
        }

        @Override
        public void run() {
            Vpn.deliverProfileFailed(requestId, error, message);
        }
    }

    private static final class StatusDelivery implements Runnable {
        private final int ordinal;

        StatusDelivery(int ordinal) {
            this.ordinal = ordinal;
        }

        @Override
        public void run() {
            Vpn.deliverStatusChanged(ordinal);
        }
    }

    /// A deferred move to a new status. Holds the bridge deliberately -- the
    /// transition is the bridge's own state change, not a callback.
    private static final class Transition implements Runnable {
        private final LocalVpnBridge bridge;
        private final VpnStatus target;

        Transition(LocalVpnBridge bridge, VpnStatus target) {
            this.bridge = bridge;
            this.target = target;
        }

        @Override
        public void run() {
            bridge.setStatus(target);
        }
    }
}
