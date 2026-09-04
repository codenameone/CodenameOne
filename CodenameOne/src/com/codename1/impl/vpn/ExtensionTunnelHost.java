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
import com.codename1.vpn.tunnel.TunnelStopReason;
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

    /// Which start [#host] and [#transport] belong to.
    ///
    /// Delivery used to resolve them from these statics at call time, so a
    /// read handler that had already passed its own generation check could
    /// hand a packet captured on the old link to a tunnel that started while
    /// it was being delivered. The extension passes the generation it is
    /// reading for, and a mismatch is dropped -- the inbound half of the
    /// same rule the writer follows outbound.
    private static int generation;

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
    /// @return whether this start committed. False means a newer start
    ///         owns the extension, and the caller has nothing to arm a read
    ///         for.
    public static boolean begin(Object tunnel, String setupWire,
            int startGeneration) {
        // instanceof rather than a cast: ParparVM does not check CHECKCAST,
        // so a wrong type here would not throw, it would read a VpnTunnel's
        // fields out of whatever object the generated code passed.
        if (!(tunnel instanceof VpnTunnel)) {
            return false;
        }
        String[] fields = TunnelWire.split(setupWire == null ? "" : setupWire);
        int mtu = TunnelWire.mtu(fields);
        ExtensionTransport t;
        TunnelHost h;
        TunnelHost displaced;
        synchronized (ExtensionTunnelHost.class) {
            if (startGeneration < generation) {
                // A NEWER start already owns the extension. This one lost
                // its race -- the provider checked the generation before
                // building the tunnel and was preempted -- and installing it
                // now would replace a live host with a cancelled one and set
                // the generation backwards, so the running tunnel's reads
                // would ask buffer() for a generation the statics no longer
                // name and get null. A tunnel that carries nothing, from a
                // start that was over before it finished.
                //
                // Committed under the SAME lock that publishes the fields,
                // which is what makes it a decision rather than another
                // check-then-act.
                return false;
            }
            // BUILT under the lock, not before it. Constructed outside, the
            // transport captured the writer through a second handshake of
            // its own: an older completion that installed its writer after
            // the newer one had installed and before the newer one got here
            // handed this start the previous start's writer, and every
            // packet the live tunnel forwarded then failed the check in
            // writeNative and vanished. setWriter refuses to go backwards
            // now, and the capture names the start it is for, so the two
            // cannot disagree.
            t = new ExtensionTransport(mtu, startGeneration);
            h = new TunnelHost((VpnTunnel) tunnel, t);
            // WHAT THIS REPLACES, kept rather than dropped. A stop preempted
            // by this start has not run yet, and when it does it finds a
            // generation newer than its own and leaves well alone -- so
            // overwriting the only reference to the old host retired nobody:
            // its tunnel never saw onStop, stayed attached to a transport
            // that would never fill again, and kept whatever it had started
            // running for the life of a process iOS caps at a few tens of
            // megabytes.
            displaced = host;
            host = h;
            transport = t;
            generation = startGeneration;
        }
        if (displaced != null) {
            // BEFORE the new one starts, so an application sees the end of
            // one tunnel before the beginning of the next. UNKNOWN because
            // that is the truth: the platform replaced this tunnel without
            // saying why, and TunnelHost.stop is idempotent, so the stop this
            // start overtook may still deliver its own reason.
            displaced.stop(TunnelStopReason.UNKNOWN.ordinal());
        }
        h.start(TunnelWire.server(fields), TunnelWire.routes(fields),
                TunnelWire.dnsServers(fields), mtu, TunnelWire.data(fields));
        return true;
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
    public static byte[] buffer(int capacity, int forGeneration) {
        ExtensionTransport t;
        synchronized (ExtensionTunnelHost.class) {
            // The transport AND the start it belongs to, read together under
            // the lock. Taken apart they could disagree.
            t = forGeneration == generation ? transport : null;
        }
        return t == null ? null : t.backing(capacity);
    }

    /// Delivers the packet just written into [#buffer].
    ///
    /// @hidden not part of the public API.
    public static void received(int length, int forGeneration) {
        TunnelHost h;
        ExtensionTransport t;
        synchronized (ExtensionTunnelHost.class) {
            // Both, or neither: a packet belongs to the start whose buffer it
            // was written into, and delivering it to a tunnel that replaced
            // that start is the crossing this exists to stop.
            h = forGeneration == generation ? host : null;
            t = forGeneration == generation ? transport : null;
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
    public static void end(int reasonOrdinal, int invalidatedGeneration) {
        TunnelHost h;
        synchronized (ExtensionTunnelHost.class) {
            if (invalidatedGeneration < generation) {
                // A NEWER start already owns the extension. This stop was
                // preempted between invalidating its own generation and
                // getting here, and tearing down now would stop the tunnel
                // that replaced it -- clearing the host and the transport of
                // a tunnel that is running and reporting a stop its
                // application never asked for. The start it belonged to is
                // over either way; there is nothing left for it to do.
                return;
            }
            h = host;
            host = null;
            transport = null;
            // The WATERMARK the stop moved to, not zero.
            //
            // Zeroing it looked safe -- no reader carries zero -- and left a
            // hole that mattered: a settings completion that had already
            // passed its own generation check could call begin() after the
            // stop, and with the watermark at zero the guard in begin() had
            // nothing to reject it with. It installed a host and ran the
            // application's onStart for a tunnel that was already over, and
            // no onStop would follow, because the stop it belonged to had
            // been and gone.
            //
            // The extension passes the counter as the stop left it, which is
            // one past every start that can still be in flight, so begin()
            // rejects them all on the same comparison it already made.
            generation = invalidatedGeneration;
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

    /// Which start [#writer] belongs to.
    private static int writerGeneration;

    /// Installs the platform's writer. The generated extension calls this
    /// before it starts the tunnel.
    ///
    /// @param generation the start this writer belongs to
    /// @param w the writer
    ///
    /// @hidden not part of the public API.
    public static void setWriter(int generation, Writer w) {
        synchronized (ExtensionTunnelHost.class) {
            if (generation < writerGeneration) {
                // NEVER BACKWARDS. Two settings completions can overlap
                // across a stop and a restart, and the older one resuming
                // last used to leave its writer installed for the tunnel
                // that is actually running -- which then tagged every packet
                // with a generation writeNative rejects, so the tunnel came
                // up and carried nothing.
                return;
            }
            writerGeneration = generation;
            writer = w;
        }
    }

    /// The writer installed for one start, or null if the one installed
    /// belongs to another.
    private static Writer writer(int startGeneration) {
        synchronized (ExtensionTunnelHost.class) {
            return writerGeneration == startGeneration ? writer : null;
        }
    }

    /// The iOS half of [TunnelTransport].
    ///
    /// One packet is staged at a time because the provider delivers a batch
    /// and this is called once per packet in it -- the batching happens on
    /// the other side of the boundary, where the array already exists.
    private static final class ExtensionTransport implements TunnelTransport {
        private final PacketBuffer[] pool;

        /// The writer THIS start installed, captured rather than looked up
        /// when a packet is sent.
        ///
        /// Resolving the global at send time undid the generation the
        /// writer carries: a tunnel that is over can be inside forward()
        /// when a stop and a restart install a new writer, and the old
        /// transport then reached for it, passed the CURRENT generation to
        /// writeNative and had its guard wave the packet through -- one
        /// session's traffic leaving on another's link, which is exactly
        /// what tagging the writer was meant to stop. Held here, an old
        /// transport keeps an old writer and the guard sees a generation
        /// that has moved.
        private final Writer sink;

        /// Whether the pooled buffer holds a packet the host has not taken.
        private boolean staged;

        ExtensionTransport(int mtu, int startGeneration) {
            this.pool = new PacketBuffer[]{TunnelBuffers.allocate(mtu)};
            // NAMED, not "whatever is current". The provider installs the
            // writer before it calls begin, but two completions can overlap,
            // so the one current here is only this start's if it says so.
            this.sink = writer(startGeneration);
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
            // THIS start's writer; see the field. Null when nothing installed
            // one, which is the same silence as before.
            if (sink != null) {
                sink.write(packet.getData(), packet.getOffset(),
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
