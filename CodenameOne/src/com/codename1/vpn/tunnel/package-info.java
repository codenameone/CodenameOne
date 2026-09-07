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
/// Writing the tunnel itself, rather than asking the system to run one.
///
/// `com.codename1.vpn.profile` asks the operating system to run an IKEv2 or
/// IPsec client it already implements, which is what most apps want. This
/// package is the other thing: the app receives raw IP packets and decides
/// what happens to them.
///
/// #### Where it runs
///
/// [Tunnels#isSupported] answers true on **Android**, and on an **iOS**
/// build that generated the extension. Ask it, and keep a path for the
/// answer being no -- on iOS that is the default.
///
/// - **Android**: `CN1VpnService`, which ships in the port and which the
///   builder declares in the manifest for an app that referenced this
///   package. It is a `VpnService` in the app's own process, needing
///   `BIND_VPN_SERVICE` and the user's consent. The tunnel runs in that
///   process, so the instance passed to [Tunnels#start] is the instance
///   that runs and everything it closed over is still there.
/// - **iOS**: an `NEPacketTunnelProvider` in a Network Extension, which
///   the build generates for a project that sets `ios.vpn.tunnel=true` and
///   names its tunnel in `ios.vpn.tunnel.class`. That extension is a
///   separate process with a virtual machine of its own: it is translated
///   from the tunnel rather than from the application, so it carries what
///   the tunnel reaches and none of the app -- which is why the rule below
///   is a link error there rather than advice. It also needs
///   `com.apple.developer.networking.networkextension`, which Apple grants
///   case by case, so no iOS build produces one without being asked.
/// - **Simulator and desktop**: a loopback transport, so the packet loop can
///   be exercised without a device.
///
/// #### Why the API is shaped for another process anyway
///
/// [TunnelSetup#data], [TunnelConfiguration] and the packet pooling in
/// [PacketBuffer] all assume the tunnel may be constructed somewhere the
/// app's statics are not, under a memory budget far below an app's. That is
/// the shape a Network Extension needs, and it costs an Android tunnel
/// nothing to be written that way. A tunnel that takes its configuration
/// from [VpnTunnel#onStart] rather than reaching for a static the app set
/// is the one that stays portable.
package com.codename1.vpn.tunnel;
