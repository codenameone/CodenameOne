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
package com.codename1.vpn.tunnel;

/// The packet loop, written once for both platforms.
///
/// ```java
/// public class MyTunnel extends VpnTunnel {
///     protected void onStart(TunnelConfiguration cfg) { }
///     protected void onPacket(PacketBuffer p) { forward(p); }
///     protected void onStop(TunnelStopReason r) { }
/// }
/// ```
///
/// #### Where this runs
///
/// On Android, in the app's own process, inside the port's `VpnService`.
/// On iOS, in a Network Extension: a separate process, with a virtual
/// machine of its own, which the build generates for a project that sets
/// `ios.vpn.tunnel`.
///
/// Write the tunnel as though it ran somewhere else, because on one of the
/// two platforms it does. Everything it needs travels in
/// [TunnelConfiguration#getData]; a static the app set happens to be there
/// on Android and is not a thing to rely on -- on iOS it is not there at
/// all, and reaching for the app's own classes drags them into the
/// extension's translation, where the ones backed by UIKit fail its link.
///
/// #### What is not there, on iOS
///
/// The extension carries the translated program and the virtual machine and
/// no networking stack. [com.codename1.io.Socket], `ConnectionRequest` and
/// anything else that reaches the implementation find nothing there, and
/// ParparVM's `java.net` is URI and URL -- there are no sockets in it
/// either. So an iOS tunnel inspects, rewrites, drops and [#forward]s
/// packets; it cannot open a connection to a remote server. On Android it
/// can, because it runs in the app's own process. A tunnel that needs to
/// relay is not the same class on both platforms, and the difference is
/// worth an interface rather than a surprise.
///
/// #### Constructing it
///
/// On Android the app constructs the tunnel and hands it to
/// [Tunnels#start]. On iOS the EXTENSION constructs it, because
/// [Tunnels#start] ran in another process that has since gone away -- so an
/// iOS tunnel needs an accessible no-argument constructor, and the class
/// named by `ios.vpn.tunnel.class` is the one it calls. A tunnel with only a
/// parameterised constructor fails the build rather than the device.
///
/// #### What is expensive
///
/// A host process for a tunnel runs under a memory budget far below an
/// app's, and is killed rather than warned when it exceeds it. The buffers
/// handed to [#onPacket] are reused for exactly that reason, so copying
/// every packet -- or holding one past the call -- gives back the headroom
/// the pooling was for. See [PacketBuffer].
public abstract class VpnTunnel {

    private TunnelTransport transport;

    /// The tunnel is up and the platform has configured the link.
    protected abstract void onStart(TunnelConfiguration configuration);

    /// One packet arrived from the device, bound for the far end.
    ///
    /// The buffer is REUSED once this returns; see [PacketBuffer].
    protected abstract void onPacket(PacketBuffer packet);

    /// The tunnel is going down, for the given reason.
    protected abstract void onStop(TunnelStopReason reason);

    /// Sends a packet back to the device.
    ///
    /// Safe to call with the buffer `onPacket` was given, which is the
    /// ordinary pass-through case and copies nothing.
    protected final void forward(PacketBuffer packet) {
        TunnelTransport t = transport();
        if (t != null && packet != null && packet.getLength() > 0) {
            t.write(packet);
        }
    }

    /// Sends bytes the tunnel produced itself.
    protected final void forward(byte[] packet, int offset, int length) {
        TunnelTransport t = transport();
        if (t == null || packet == null || length <= 0) {
            return;
        }
        PacketBuffer b = new PacketBuffer(length);
        b.fill(packet, offset, length);
        t.write(b);
    }

    private synchronized TunnelTransport transport() {
        return transport;
    }

    /// Attaches the transport this tunnel runs over. Called by the port.
    synchronized void attach(TunnelTransport t) {
        this.transport = t;
    }

    /// Delivers one packet; the host calls this rather than onPacket, so a
    /// tunnel that throws cannot take the loop down with it.
    final void deliver(PacketBuffer packet) {
        try {
            onPacket(packet);
        } catch (Throwable appFailure) {
            // A packet loop that dies on one bad packet takes the whole VPN
            // with it, and the user sees a tunnel that stopped for no reason
            // they can act on.
            report(appFailure);
        }
    }

    /// Announces the start; see [#deliver] for why this is wrapped.
    final void begin(TunnelConfiguration configuration) {
        try {
            onStart(configuration);
        } catch (Throwable appFailure) {
            report(appFailure);
        }
    }

    /// Announces the stop; see [#deliver] for why this is wrapped.
    final void finish(TunnelStopReason reason) {
        try {
            onStop(reason == null ? TunnelStopReason.UNKNOWN : reason);
        } catch (Throwable appFailure) {
            report(appFailure);
        }
    }

    /// Logs an application failure without being able to become one.
    ///
    /// Log.e reaches the implementation, and in a process where there is
    /// none -- a Network Extension before anything installed one, or a unit
    /// test -- it throws on its way to the log. Containment that can be
    /// killed by its own error reporting is not containment, so the report
    /// is wrapped too and a failure to log is simply dropped: there is
    /// nowhere left to say it.
    private static void report(Throwable appFailure) {
        try {
            com.codename1.io.Log.e(appFailure);
        } catch (Throwable nowhereToLog) { //NOPMD EmptyCatchBlock
            // Deliberately nothing.
        }
    }
}
