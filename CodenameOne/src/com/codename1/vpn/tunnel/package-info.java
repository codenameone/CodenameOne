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
/// #### The cost, before the API
///
/// - On **iOS** the tunnel is a Network Extension: a separate process with
///   its own bundle, its own copy of the VM, and a memory budget far below
///   an app's. Its entitlement,
///   `com.apple.developer.networking.networkextension`, is one Apple grants
///   case by case rather than one a paid account switches on -- so a build
///   that needs it can be refused for reasons no code here can fix.
/// - On **Android** it is a `VpnService` in the app's own process, needing
///   `BIND_VPN_SERVICE` and the user's consent.
///
/// The packet loop is [VpnTunnel] and is written once. What differs is how
/// packets arrive, and [TunnelTransport] states that difference plainly
/// rather than papering over it.
package com.codename1.vpn.tunnel;
