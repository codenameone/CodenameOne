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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
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

    /// Which start is current. Bumped by every start and every stop, and
    /// re-checked by an opener before it publishes anything.
    ///
    /// A stop that arrives while an opener is still resolving DNS or
    /// establishing the interface finds no published host, answers
    /// successfully and calls stopSelf -- and the opener then went on to
    /// establish, promote and publish a tunnel the caller had been told was
    /// stopped. Nothing it could check said otherwise, because the state it
    /// would have checked is the state it had not written yet.
    private static int startGeneration;

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
        // Claimed by REQUEST, not read from a global. Two starts racing
        // used to leave whichever registered last in the field, so this
        // service command could run the second tunnel object under the first
        // setup and acknowledge the first request.
        VpnTunnel tunnel = Tunnels.claim(requestId);
        if (tunnel == null) {
            fail(requestId, VpnError.INVALID_CONFIGURATION,
                    "No tunnel is registered for this request; Tunnels"
                    + ".start() registers one before the service is asked to"
                    + " run, and a restart of this service does not carry it");
            stopSelf();
            return START_NOT_STICKY;
        }
        // OFF the main thread from here. establish() resolves the gateway
        // name so it can be kept out of the routes, and a DNS lookup on the
        // main thread is an ANR waiting for a slow network -- which is
        // exactly the network a VPN is being started on.
        final String[] fields = TunnelWire.split(wire);
        final VpnTunnel starting = tunnel;
        final int rid = requestId;
        int generation;
        synchronized (CN1VpnService.class) {
            generation = ++startGeneration;
        }
        Thread opener = new Thread(
                new Opener(this, starting, fields, rid, generation),
                "CN1 VPN tunnel start");
        opener.setDaemon(true);
        opener.start();
        return START_NOT_STICKY;
    }

    /// Establishes the link and starts the loop, off the main thread.
    ///
    /// A named class rather than an anonymous one so it holds no synthetic
    /// reference to the intent or anything else the callback outlives.
    private static final class Opener implements Runnable {
        private final CN1VpnService service;
        private final VpnTunnel tunnel;
        private final String[] fields;
        private final int requestId;
        private final int generation;

        Opener(CN1VpnService service, VpnTunnel tunnel, String[] fields,
                int requestId, int generation) {
            this.service = service;
            this.tunnel = tunnel;
            this.fields = fields;
            this.requestId = requestId;
            this.generation = generation;
        }

        @Override
        public void run() {
            service.open(tunnel, fields, requestId, generation);
        }
    }

    /// Whether this opener is still the current start.
    private static boolean current(int generation) {
        synchronized (CN1VpnService.class) {
            return generation == startGeneration;
        }
    }

    /// The rest of the start, with DNS allowed.
    private void open(VpnTunnel tunnel, String[] fields, int requestId,
            int generation) {
        if (!current(generation)) {
            // Superseded before this thread got going.
            fail(requestId, VpnError.UNKNOWN,
                    "The tunnel start was superseded before it opened");
            return;
        }
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
            return;
        }
        if (fd == null) {
            fail(requestId, VpnError.UNAUTHORIZED,
                    "The VPN consent this app was granted is no longer in"
                    + " force; call Tunnels.start() again to ask for it");
            stopSelf();
            return;
        }
        // The generation is re-checked INSIDE the publication, not before
        // it; see start(). A check here and an install a few statements
        // later is a window, and it is the same window this whole mechanism
        // exists to close.
        start(tunnel, fd, fields, requestId, generation);
    }

    /// The notification channel the ongoing-tunnel notification lives in.
    private static final String CHANNEL = "cn1-vpn-tunnel";

    /// A fixed id: there is one tunnel, so one notification.
    private static final int NOTIFICATION_ID = 0x7602;

    /// Promotes this service so the platform keeps it running.
    private void promote(String sessionName) {
        if (Build.VERSION.SDK_INT < 26) {
            // Before Oreo a started service simply keeps running, and
            // startForeground would demand a channel the platform has no
            // concept of.
            return;
        }
        NotificationManager nm = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(CHANNEL,
                "VPN", NotificationManager.IMPORTANCE_LOW);
        // LOW, and no sound: this notification exists because the platform
        // requires one, not because the user needs telling repeatedly that
        // their VPN is still on.
        channel.setShowBadge(false);
        nm.createNotificationChannel(channel);
        Notification n = new Notification.Builder(this, CHANNEL)
                .setContentTitle(sessionName == null || sessionName.length() == 0
                        ? "VPN" : sessionName)
                .setContentText("Connected")
                // A PLATFORM icon, because this class ships in the port and
                // has no resources of its own; stat_sys_vpn_ic is not in the
                // public R, so the app's own icon is the honest fallback and
                // it is what the notification a VPN shows should carry
                // anyway.
                .setSmallIcon(applicationIcon())
                .setOngoing(true)
                .build();
        if (Build.VERSION.SDK_INT >= 34) {
            // Android 14 demands a TYPE, and refuses the promotion without
            // one. Reflective because the port compiles against an older
            // SDK: the three-argument startForeground is API 29 and
            // FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED -- the type whose
            // documented exemptions include VPN apps -- is API 34.
            try {
                java.lang.reflect.Method m = getClass().getMethod(
                        "startForeground", int.class, Notification.class,
                        int.class);
                m.invoke(this, Integer.valueOf(NOTIFICATION_ID), n,
                        Integer.valueOf(1024));
                return;
            } catch (Exception unavailable) {
                // Fall through: a platform that says 34 without the method
                // is not one this can reason about, and an un-promoted
                // service is better than no tunnel.
            }
        }
        startForeground(NOTIFICATION_ID, n);
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
        // The SERVER stays outside the tunnel. TunnelSetup.server()
        // promises exactly that, and without it a default route captures the
        // tunnel's own connection to its gateway: the app dials the server,
        // the packet goes into the TUN it is trying to serve, and nothing
        // moves. Every VPN has to solve this; Android's two answers are
        // VpnService.protect on the socket -- which needs the socket, and a
        // Codename One app never has it -- and keeping the address out of
        // the routes, which is what the setup already describes.
        String server = serverAddress(TunnelWire.server(fields));
        String[] routes = TunnelWire.routes(fields);
        boolean excluded = false;
        if (server != null && Build.VERSION.SDK_INT >= 33) {
            // The direct way, where the platform has it.
            excluded = excludeRoute(b, server);
        }
        for (int i = 0; i < routes.length; i++) {
            int slash = routes[i].indexOf('/');
            String net = slash < 0 ? routes[i] : routes[i].substring(0, slash);
            int prefix = slash < 0
                    ? (net.indexOf(':') >= 0 ? 128 : 32)
                    : parsePrefix(routes[i].substring(slash + 1),
                            net.indexOf(':') >= 0 ? 128 : 32);
            if (!excluded && server != null
                    && (net.indexOf(':') >= 0) == (server.indexOf(':') >= 0)) {
                // No excludeRoute on this platform, so the route is SPLIT
                // around the server instead: the complement of one address
                // inside a prefix is at most one block per remaining bit,
                // each exact. The same traffic is carried, minus the one
                // host the tunnel needs to reach to carry it.
                //
                // BOTH families. The first version of this did v4 only, so
                // an IPv6 gateway under ::/0 kept the route it was supposed
                // to be excluded from and captured its own connection --
                // the exact loop the v4 case was written to prevent.
                if (addSplitRoutes(b, net, prefix, server)) {
                    continue;
                }
            }
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

    /// The application's own launcher icon, for the ongoing notification.
    ///
    /// Resolved from the package rather than named: this class is in the
    /// port and has no resources, and a notification with no valid small
    /// icon is one Android refuses to post -- which would take the
    /// foreground promotion down with it.
    private int applicationIcon() {
        try {
            return getPackageManager().getApplicationInfo(getPackageName(), 0)
                    .icon;
        } catch (Exception missing) {
            return android.R.drawable.ic_dialog_info;
        }
    }

    /// The server as a bare address, RESOLVING a host name.
    ///
    /// Routes take addresses, so a setup naming its gateway
    /// `vpn.example.com` -- which is the ordinary way to write one -- has to
    /// be resolved before the route table can leave it out. Returning null
    /// for a name, as this first did, meant the common case got no exclusion
    /// at all: the default route went in, the tunnel's own connection to its
    /// gateway went into the TUN, and nothing moved.
    ///
    /// Resolution happens on the service's start thread, never the main one;
    /// see onStartCommand.
    private static String serverAddress(String server) {
        if (server == null || server.length() == 0) {
            return null;
        }
        if (!isLiteral(server)) {
            try {
                // The FIRST address only. A gateway behind several is one
                // this cannot fully exclude, and excluding the one the
                // transport will most likely use is better than excluding
                // none -- but an app in that position should give the
                // literal it dials.
                return java.net.InetAddress.getByName(server)
                        .getHostAddress();
            } catch (java.io.IOException unresolved) {
                // No DNS yet, or a name that does not resolve. The tunnel
                // comes up without the exclusion rather than not at all,
                // which is what it did before this existed.
                return null;
            }
        }
        return server;
    }

    /// Whether this is already an address rather than a name.
    private static boolean isLiteral(String server) {
        if (server.indexOf(':') >= 0) {
            // An IPv6 literal; a name never carries a colon.
            return true;
        }
        int dots = 0;
        for (int i = 0; i < server.length(); i++) {
            char c = server.charAt(i);
            if (c == '.') {
                dots++;
            } else if (c < '0' || c > '9') {
                return false;
            }
        }
        return dots == 3;
    }

    /// Excludes one address from the tunnel, on a platform that can.
    ///
    /// Reflective: excludeRoute and IpPrefix's InetAddress constructor are
    /// API 33 and the port compiles against an older SDK.
    private static boolean excludeRoute(Builder b, String server) {
        try {
            Class<?> prefixClass = Class.forName("android.net.IpPrefix");
            Object prefix = prefixClass.getConstructor(
                    java.net.InetAddress.class, int.class).newInstance(
                            java.net.InetAddress.getByName(server),
                            Integer.valueOf(server.indexOf(':') >= 0 ? 128 : 32));
            Builder.class.getMethod("excludeRoute", prefixClass)
                    .invoke(b, prefix);
            return true;
        } catch (Exception unavailable) {
            // Older platform, or a refusal. The caller splits instead.
            return false;
        }
    }

    /// Adds `net/prefix` as a set of routes that omits `server`.
    ///
    /// Walks the prefix bit by bit: at each step the sibling half that does
    /// NOT contain the server is a complete route, and the half that does is
    /// narrowed further. That yields at most 32 routes and covers exactly
    /// the original block minus the one address.
    ///
    /// @return false when the server is not inside this route, in which case
    /// the caller adds the route whole
    private static boolean addSplitRoutes(Builder b, String net, int prefix,
            String server) {
        byte[] netBits = addressBytes(net);
        byte[] serverBits = addressBytes(server);
        if (netBits == null || serverBits == null
                || netBits.length != serverBits.length) {
            return false;
        }
        int width = netBits.length * 8;
        if (prefix < 0 || prefix > width) {
            return false;
        }
        for (int i = 0; i < prefix; i++) {
            if (bitAt(netBits, i) != bitAt(serverBits, i)) {
                // The server is somewhere else entirely; nothing to split.
                return false;
            }
        }
        for (int bit = prefix; bit < width; bit++) {
            // The sibling of the half the server is in: the server's own
            // bits down to this depth, with this bit flipped and everything
            // below it cleared.
            byte[] sibling = new byte[netBits.length];
            for (int i = 0; i < bit; i++) {
                setBit(sibling, i, bitAt(serverBits, i));
            }
            setBit(sibling, bit, bitAt(serverBits, bit) == 0 ? 1 : 0);
            b.addRoute(addressText(sibling), bit + 1);
        }
        return true;
    }

    /// An address literal as its raw bytes, or null when it is not one.
    ///
    /// Through InetAddress rather than parsed here: IPv6 has eight notations
    /// and a hand-written parser would get one of them wrong. No lookup
    /// happens -- the caller has already resolved anything that was a name.
    private static byte[] addressBytes(String literal) {
        try {
            return java.net.InetAddress.getByName(literal).getAddress();
        } catch (Exception notAnAddress) {
            return null;
        }
    }

    private static int bitAt(byte[] address, int index) {
        return (address[index / 8] >> (7 - (index % 8))) & 1;
    }

    private static void setBit(byte[] address, int index, int value) {
        int mask = 1 << (7 - (index % 8));
        if (value == 0) {
            address[index / 8] &= (byte) ~mask;
        } else {
            address[index / 8] |= (byte) mask;
        }
    }

    /// The textual form of raw address bytes.
    private static String addressText(byte[] address) {
        try {
            return java.net.InetAddress.getByAddress(address)
                    .getHostAddress();
        } catch (Exception impossible) {
            // getByAddress only rejects a length that is not 4 or 16, and
            // these came from getAddress.
            return null;
        }
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
        // ZERO IS VALID, and it is the important one: /0 is the default
        // route, which is what a full-tunnel VPN asks for and what the
        // documentation shows. Treating it as unparseable handed the
        // fallback back instead -- a /32 host route -- so the tunnel came up
        // acknowledged and carried nothing, and the split around the server
        // could not fire either because a /32 contains no other address.
        return value >= 0 && value <= fallback ? value : fallback;
    }

    private void start(VpnTunnel tunnel, ParcelFileDescriptor fd,
            String[] fields, int requestId, int generation) {
        DescriptorTunnelTransport t =
                new DescriptorTunnelTransport(fd, TunnelWire.mtu(fields));
        TunnelHost h = new TunnelHost(tunnel, t);
        Thread runner = new Thread(new Loop(this, h,
                TunnelWire.server(fields), TunnelWire.routes(fields),
                TunnelWire.dnsServers(fields), TunnelWire.mtu(fields),
                TunnelWire.data(fields)), "CN1 VPN tunnel");
        // A daemon thread: the loop parks on a descriptor read, and a
        // non-daemon thread doing that keeps the process alive after
        // everything else has finished with it.
        runner.setDaemon(true);
        boolean published;
        synchronized (CN1VpnService.class) {
            // The CHECK and the install in ONE critical section. Separated,
            // a stop landing between them bumped the generation, found no
            // published host, answered successfully -- and then this
            // installed the tunnel and acknowledged the start anyway. The
            // check has to be part of the publication, not a prelude to it.
            published = generation == startGeneration;
            if (published) {
                // A start arriving while one is already up replaces it, so
                // the previous link is torn down first rather than left with
                // a thread still reading it.
                stopLocked(TunnelStopReason.REQUESTED);
                host = h;
                transport = t;
                loop = runner;
            }
        }
        if (!published) {
            t.close();
            fail(requestId, VpnError.UNKNOWN,
                    "The tunnel start was superseded while it was opening");
            stopSelf();
            return;
        }
        // FOREGROUND once the tunnel is really this service's. Android 8
        // shuts down an ordinary started service that keeps running, so a
        // tunnel brought up without this was acknowledged, established, and
        // then killed a little later with nothing in the app to say why.
        // After the publication rather than before it, so a superseded start
        // does not leave a notification for a tunnel it never installed.
        promote(TunnelWire.sessionName(fields));
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
        private final CN1VpnService service;
        private final TunnelHost host;
        private final String server;
        private final String[] routes;
        private final String[] dns;
        private final int mtu;
        private final String data;

        Loop(CN1VpnService service, TunnelHost host, String server,
                String[] routes, String[] dns, int mtu, String data) {
            this.service = service;
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
            // And the service lets go too. A link that failed on its own --
            // TunnelHost retires the tunnel for that now -- used to leave
            // this service holding a published host, a foreground
            // notification and a registered tunnel with nothing forwarding
            // for it. Nothing here runs when a stop caused the loop to end:
            // stopLocked has already cleared the fields, so this finds none
            // of its own and does nothing.
            // The identity check and the teardown in ONE transition. Split,
            // a stop-then-restart could clear the old host and publish a new
            // one between them, and this stale loop would then tear down the
            // tunnel that had just replaced it. Same shape as the
            // publication in start(): the check is part of the act.
            //
            // QUALIFIED, because this class has its own `host` field and the
            // unqualified name resolved to it -- a self-comparison SpotBugs
            // caught, always true, so this would have fired after an
            // ordinary stop too.
            boolean mine;
            synchronized (CN1VpnService.class) {
                mine = CN1VpnService.host == this.host;
                if (mine) {
                    stopLocked(TunnelStopReason.NETWORK_LOST);
                }
            }
            if (mine) {
                // Outside the monitor: this reaches application code and
                // the service, neither of which may run under it.
                Tunnels.clearRegistered();
                service.stopSelf();
            }
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
            // Invalidates any opener still in flight; see startGeneration.
            startGeneration++;
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
