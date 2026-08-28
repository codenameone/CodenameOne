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
package com.codename1.impl.android.vpn;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;

import com.codename1.impl.vpn.TunnelWire;
import com.codename1.vpn.VpnError;
import com.codename1.vpn.tunnel.TunnelHost;
import com.codename1.vpn.tunnel.TunnelStopReason;
import com.codename1.vpn.tunnel.Tunnels;
import com.codename1.vpn.tunnel.VpnTunnel;

/// The Android host for a packet tunnel the application implements.
///
/// #### Why this ships in the port rather than being generated
///
/// `android.net.VpnService` is on the port's compile classpath, so this
/// class is compiled by CI on every pull request. A generated template
/// would be compiled by nothing in this repository until a customer built
/// an app with it -- which is how a generated Kotlin bridge on another
/// branch reached production with a compile error in it. The class is inert
/// unless the builder writes the manifest `<service>`, and R8 strips it from
/// an app that never references the tunnel package.
///
/// #### The tunnel runs in the app's process
///
/// Unlike iOS, where the tunnel lives in a Network Extension with its own
/// VM, this service is part of the app. So the instance the app registered
/// through [Tunnels#start] is the instance that runs, and everything it
/// closed over is still there. An app that relies on that will not port to
/// iOS, which is why [com.codename1.vpn.tunnel.TunnelSetup#data] exists and
/// why the guide says to use it.
public class CN1VpnService extends VpnService {

    /// The setup record, handed over on the start intent.
    static final String EXTRA_SETUP = "cn1.vpn.tunnel.setup";

    /// Asks the service to stop rather than start.
    static final String ACTION_STOP = "com.codename1.vpn.tunnel.STOP";

    /// The request to answer once the link is up, or -1.
    static final String EXTRA_REQUEST = "cn1.vpn.tunnel.request";

    /// The one live tunnel. Guarded by the class monitor, which also orders
    /// a stop arriving while a start is still establishing.
    private static TunnelHost host;

    private static Thread loop;

    private static DescriptorTunnelTransport transport;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            int requestId = intent.getIntExtra(EXTRA_REQUEST, -1);
            stopTunnel(TunnelStopReason.REQUESTED);
            if (requestId >= 0) {
                Tunnels.deliverAck(requestId, true, 0, null);
            }
            stopSelf();
            // NOT_STICKY: a tunnel the app asked to stop must not be brought
            // back by the system, which would leave a link up that nothing
            // in the app believes exists.
            return START_NOT_STICKY;
        }
        int requestId = intent == null ? -1
                : intent.getIntExtra(EXTRA_REQUEST, -1);
        String wire = intent == null ? null
                : intent.getStringExtra(EXTRA_SETUP);
        if (wire == null) {
            // Android restarted the service on its own -- the only way here
            // with no intent -- and the setup it was started with is not
            // something this process kept. Refusing is honest: re-reading a
            // stale setup would bring a link up that the app has no tunnel
            // registered for.
            stopSelf();
            return START_NOT_STICKY;
        }
        VpnTunnel tunnel = Tunnels.getRegistered();
        if (tunnel == null) {
            fail(requestId, VpnError.INVALID_CONFIGURATION,
                    "No tunnel is registered; Tunnels.start() registers one"
                    + " before the service is asked to run");
            stopSelf();
            return START_NOT_STICKY;
        }
        String[] fields = TunnelWire.split(wire);
        ParcelFileDescriptor fd;
        try {
            fd = establish(fields);
        } catch (RuntimeException refused) {
            // establish() answers null when consent was revoked between the
            // prompt and here, and throws when the builder was given
            // something it cannot express -- a malformed CIDR, most often.
            fail(requestId, VpnError.INVALID_CONFIGURATION,
                    String.valueOf(refused.getMessage()));
            stopSelf();
            return START_NOT_STICKY;
        }
        if (fd == null) {
            fail(requestId, VpnError.UNAUTHORIZED,
                    "The VPN consent this app was granted is no longer in"
                    + " force; call Tunnels.start() again to ask for it");
            stopSelf();
            return START_NOT_STICKY;
        }
        start(tunnel, fd, fields, requestId);
        // NOT_STICKY here too. A tunnel is the app's to own: bringing it
        // back without the app's tunnel object, which a restarted process
        // does not have, would establish a link with nothing serving it.
        return START_NOT_STICKY;
    }

    private ParcelFileDescriptor establish(String[] fields) {
        Builder b = new Builder();
        String address = TunnelWire.address(fields);
        if (address.length() > 0) {
            int slash = address.indexOf('/');
            // The PREFIX is required by Builder.addAddress and a plain
            // address is the ordinary way to write one, so a missing prefix
            // is filled rather than refused: /32 for IPv4, /128 for IPv6.
            String host = slash < 0 ? address : address.substring(0, slash);
            int prefix = slash < 0
                    ? (host.indexOf(':') >= 0 ? 128 : 32)
                    : parsePrefix(address.substring(slash + 1),
                            host.indexOf(':') >= 0 ? 128 : 32);
            b.addAddress(host, prefix);
        }
        String[] routes = TunnelWire.routes(fields);
        for (int i = 0; i < routes.length; i++) {
            int slash = routes[i].indexOf('/');
            String net = slash < 0 ? routes[i] : routes[i].substring(0, slash);
            int prefix = slash < 0
                    ? (net.indexOf(':') >= 0 ? 128 : 32)
                    : parsePrefix(routes[i].substring(slash + 1),
                            net.indexOf(':') >= 0 ? 128 : 32);
            b.addRoute(net, prefix);
        }
        String[] dns = TunnelWire.dnsServers(fields);
        for (int i = 0; i < dns.length; i++) {
            b.addDnsServer(dns[i]);
        }
        String[] domains = TunnelWire.searchDomains(fields);
        for (int i = 0; i < domains.length; i++) {
            b.addSearchDomain(domains[i]);
        }
        b.setMtu(TunnelWire.mtu(fields));
        String session = TunnelWire.sessionName(fields);
        if (session.length() > 0) {
            b.setSession(session);
        }
        return b.establish();
    }

    /// A CIDR prefix, or `fallback` when the text is not one.
    ///
    /// Tested rather than caught: this runs in the port, where a
    /// NumberFormatException would take the tunnel down over a typo in a
    /// route, and a route this cannot read is better refused by Builder --
    /// which names it -- than turned into a silent exception here.
    private static int parsePrefix(String text, int fallback) {
        String t = text.trim();
        if (t.length() == 0 || t.length() > 3) {
            return fallback;
        }
        int value = 0;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c < '0' || c > '9') {
                return fallback;
            }
            value = value * 10 + (c - '0');
        }
        return value > 0 && value <= fallback ? value : fallback;
    }

    private void start(VpnTunnel tunnel, ParcelFileDescriptor fd,
            String[] fields, int requestId) {
        DescriptorTunnelTransport t =
                new DescriptorTunnelTransport(fd, TunnelWire.mtu(fields));
        TunnelHost h = new TunnelHost(tunnel, t);
        Thread runner = new Thread(new Loop(h,
                TunnelWire.server(fields), TunnelWire.routes(fields),
                TunnelWire.dnsServers(fields), TunnelWire.mtu(fields),
                TunnelWire.data(fields)), "CN1 VPN tunnel");
        // A daemon thread: the loop parks on a descriptor read, and a
        // non-daemon thread doing that keeps the process alive after
        // everything else has finished with it.
        runner.setDaemon(true);
        synchronized (CN1VpnService.class) {
            // A start arriving while one is already up replaces it, so the
            // previous link is torn down first rather than left with a
            // thread still reading it.
            stopLocked(TunnelStopReason.REQUESTED);
            host = h;
            transport = t;
            loop = runner;
        }
        // ANSWERED before the loop is started, and deliberately: the link is
        // established by now -- establish() returned a descriptor -- so the
        // app's start() has succeeded, and making it wait for the first
        // packet would leave it pending on a tunnel that may legitimately be
        // idle for minutes.
        if (requestId >= 0) {
            Tunnels.deliverAck(requestId, true, 0, null);
        }
        runner.start();
    }

    /// Runs the host's blocking loop off the main thread.
    ///
    /// A named class rather than an anonymous one so it holds no synthetic
    /// reference to the service, which the loop outlives by design.
    private static final class Loop implements Runnable {
        private final TunnelHost host;
        private final String server;
        private final String[] routes;
        private final String[] dns;
        private final int mtu;
        private final String data;

        Loop(TunnelHost host, String server, String[] routes, String[] dns,
                int mtu, String data) {
            this.host = host;
            this.server = server;
            this.routes = routes;
            this.dns = dns;
            this.mtu = mtu;
            this.data = data;
        }

        @Override
        public void run() {
            // Returns when the descriptor closes, which is how stop() ends
            // it; the host has already told the tunnel by then.
            host.start(server, routes, dns, mtu, data);
        }
    }

    @Override
    public void onRevoke() {
        // The user turned this VPN off from Settings, or another app claimed
        // the tunnel. Telling the tunnel WHY matters: an app that reconnects
        // on an unexpected stop must not fight the user who just switched it
        // off.
        stopTunnel(TunnelStopReason.USER_DISABLED);
        stopSelf();
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        stopTunnel(TunnelStopReason.SYSTEM_RECLAIMED);
        super.onDestroy();
    }

    /// Tears down whatever is running, once.
    static void stopTunnel(TunnelStopReason reason) {
        synchronized (CN1VpnService.class) {
            stopLocked(reason);
        }
        Tunnels.clearRegistered();
    }

    private static void stopLocked(TunnelStopReason reason) {
        if (host == null) {
            return;
        }
        TunnelHost h = host;
        DescriptorTunnelTransport t = transport;
        Thread runner = loop;
        host = null;
        transport = null;
        loop = null;
        // The HOST first: it tells the tunnel and closes the transport, and
        // closing the descriptor is what ends the parked read.
        h.stop(reason.ordinal());
        if (t != null) {
            t.close();
        }
        if (runner != null) {
            runner.interrupt();
        }
    }

    private static void fail(int requestId, VpnError e, String message) {
        if (requestId >= 0) {
            Tunnels.deliverAck(requestId, false, e.ordinal(), message);
        }
    }
}
