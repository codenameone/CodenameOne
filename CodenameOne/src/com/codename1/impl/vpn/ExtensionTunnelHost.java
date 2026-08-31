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

import com.codename1.vpn.tunnel.PacketBuffer;
import com.codename1.vpn.tunnel.TunnelBuffers;
import com.codename1.vpn.tunnel.TunnelHost;
import com.codename1.vpn.tunnel.TunnelTransport;
import com.codename1.vpn.tunnel.VpnTunnel;

/// The entry point a generated Network Extension calls into.
///
/// #### Nothing here may touch the framework
///
/// This runs in the extension process, which has a translated VM and
/// **nothing else**: no `Display`, no implementation, no resources, no EDT.
/// So this class uses none of them, and neither may anything it calls --
/// which is why the tunnel API was built out of plain objects and why
/// `TunnelHost` takes primitives rather than a configuration object it would
/// have to construct through the framework.
///
/// A call to `Display.getInstance()` anywhere under here is not a slow path;
/// it is a null dereference in a process that has no user to show it to.
///
/// #### The transport is callback-driven
///
/// iOS hands packets over through a completion handler, so there is nothing
/// to block on -- [TunnelTransport#isBlocking] answers false and the host
/// arms rather than loops. [#deliver] is what the provider calls for each
/// packet it read.
///
/// @hidden not part of the public API; called by generated extension code.
public final class ExtensionTunnelHost {

    private static TunnelHost host;

    private static ExtensionTransport transport;

    private ExtensionTunnelHost() {
    }

    /// Starts the application's tunnel with the setup the app supplied.
    ///
    /// @param tunnel   the application's `VpnTunnel`, freshly constructed by
    ///                 the extension -- it cannot be the app's instance,
    ///                 which lives in another process
    /// @param setupWire the record the app's `TunnelSetup` was encoded into
    ///
    /// @hidden not part of the public API.
    public static void begin(Object tunnel, String setupWire) {
        // instanceof rather than a cast: ParparVM does not check CHECKCAST,
        // so a wrong type here would not throw, it would read a VpnTunnel's
        // fields out of whatever object the generated code passed.
        if (!(tunnel instanceof VpnTunnel)) {
            return;
        }
        String[] fields = TunnelWire.split(setupWire == null ? "" : setupWire);
        int mtu = TunnelWire.mtu(fields);
        ExtensionTransport t = new ExtensionTransport(mtu);
        TunnelHost h = new TunnelHost((VpnTunnel) tunnel, t);
        synchronized (ExtensionTunnelHost.class) {
            host = h;
            transport = t;
        }
        h.start(TunnelWire.server(fields), TunnelWire.routes(fields),
                TunnelWire.dnsServers(fields), mtu, TunnelWire.data(fields));
    }

    /// The pooled array the extension writes the next packet into.
    ///
    /// The extension used to allocate a Java array per packet, copy the
    /// NSData into it, and hand that over -- and the transport then copied
    /// it AGAIN into the pooled buffer. Two copies and an allocation per
    /// packet at line rate, inside a process with a hard memory cap, in an
    /// API whose buffers are pooled precisely to avoid that. The extension
    /// writes into this and calls [#received] instead.
    ///
    /// @param capacity the packet's length
    /// @return the array to write into, or null when no tunnel is running
    ///
    /// @hidden not part of the public API.
    public static byte[] buffer(int capacity) {
        ExtensionTransport t;
        synchronized (ExtensionTunnelHost.class) {
            t = transport;
        }
        return t == null ? null : t.backing(capacity);
    }

    /// Delivers the packet just written into [#buffer].
    ///
    /// @hidden not part of the public API.
    public static void received(int length) {
        TunnelHost h;
        ExtensionTransport t;
        synchronized (ExtensionTunnelHost.class) {
            h = host;
            t = transport;
        }
        if (h == null || t == null || length <= 0) {
            return;
        }
        t.received(length);
        h.pump();
    }

    /// Stops the tunnel, with a `TunnelStopReason` ordinal.
    ///
    /// @hidden not part of the public API.
    public static void end(int reasonOrdinal) {
        TunnelHost h;
        synchronized (ExtensionTunnelHost.class) {
            h = host;
            host = null;
            transport = null;
        }
        if (h != null) {
            h.stop(reasonOrdinal);
        }
    }

    /// How a packet gets back onto the link.
    ///
    /// An interface rather than a `native` on this class, and the difference
    /// is not stylistic: a native declared in core has to be implemented by
    /// EVERY port, and this symbol exists only inside a generated Network
    /// Extension. Declaring it here failed the native-signature gate for the
    /// Windows and Linux ports, which is exactly the link error it would
    /// have been on a device.
    ///
    /// @hidden not part of the public API.
    public interface Writer {
        /// Writes one packet out, from `offset` for `length` bytes.
        void write(byte[] packet, int offset, int length);
    }

    private static Writer writer;

    /// Installs the platform's writer. The generated extension calls this
    /// before it starts the tunnel.
    ///
    /// @hidden not part of the public API.
    public static void setWriter(Writer w) {
        synchronized (ExtensionTunnelHost.class) {
            writer = w;
        }
    }

    private static Writer writer() {
        synchronized (ExtensionTunnelHost.class) {
            return writer;
        }
    }

    /// The iOS half of [TunnelTransport].
    ///
    /// One packet is staged at a time because the provider delivers a batch
    /// and this is called once per packet in it -- the batching happens on
    /// the other side of the boundary, where the array already exists.
    private static final class ExtensionTransport implements TunnelTransport {
        private final PacketBuffer[] pool;

        /// Whether the pooled buffer holds a packet the host has not taken.
        private boolean staged;

        ExtensionTransport(int mtu) {
            this.pool = new PacketBuffer[]{TunnelBuffers.allocate(mtu)};
        }

        /// The pooled buffer's array, grown for this packet.
        byte[] backing(int capacity) {
            return TunnelBuffers.backing(pool[0], capacity);
        }

        /// Marks the pooled buffer as holding a packet of this length.
        void received(int length) {
            TunnelBuffers.received(pool[0], length);
            this.staged = true;
        }

        @Override
        public boolean isBlocking() {
            // FALSE, and this is the whole reason the flag exists: a loop
            // here would park the provider's completion handler and the
            // extension would never read another packet.
            return false;
        }

        @Override
        public int read(PacketBuffer[] into) {
            boolean ready = staged;
            staged = false;
            // NOTHING is copied here: `into` IS this transport's pool, and
            // the extension has already written into the buffer's own array.
            return ready && into != null && into.length > 0 ? 1 : 0;
        }

        @Override
        public void write(PacketBuffer packet) {
            if (packet == null || packet.getLength() <= 0) {
                return;
            }
            Writer w = writer();
            if (w != null) {
                w.write(packet.getData(), packet.getOffset(),
                        packet.getLength());
            }
        }

        @Override
        public PacketBuffer[] buffers() {
            return pool;
        }

        @Override
        public void close() {
            staged = false;
        }
    }
}
