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

/// How packets reach the tunnel, which is where the two platforms differ.
///
/// This interface EXPOSES that difference rather than hiding it, because
/// hiding it would mean pretending one of the platforms works like the
/// other:
///
/// - On Android a `VpnService` runs in the app's own process and hands over a
///   file descriptor. Reading it blocks, and the read loop owns a thread.
/// - A host that delivers packets through a completion handler instead --
///   an `NEPacketTunnelProvider` is the example, though iOS does not run
///   these tunnels -- has nothing to block on, and a loop that tried to
///   would deadlock it. The simulation takes this shape so the case is
///   exercised.
///
/// [#isBlocking] is the discriminator, and the host reads it rather than
/// guessing from the platform. A tunnel written against [VpnTunnel] never
/// sees any of this; it is here for the ports and for the simulation.
public interface TunnelTransport {

    /// Whether [#read] blocks until packets arrive.
    ///
    /// True on Android, where the host gives the loop a thread. False for a
    /// host that arms a callback and returns, which is the simulation.
    boolean isBlocking();

    /// Takes the next packets, filling `into` and answering how many.
    ///
    /// Answers `0` when the tunnel is going down, which is how a blocking
    /// loop learns to stop.
    int read(PacketBuffer[] into);

    /// Sends one packet back out.
    void write(PacketBuffer packet);

    /// The buffers this transport reads into, sized for the link.
    ///
    /// Owned by the transport and reused; see [PacketBuffer].
    PacketBuffer[] buffers();

    /// Releases whatever the platform gave the tunnel.
    void close();
}
