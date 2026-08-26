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
/// Installing and controlling a VPN configuration.
///
/// #### What is realistic here, and what is not
///
/// There are two very different things called "VPN support", and conflating
/// them wastes a lot of time:
///
/// - **Asking the operating system to run a standard tunnel** -- an IKEv2 or
///   IPsec configuration the platform itself implements. Portable, needs a
///   capability any paid developer account can switch on, and is what almost
///   every app that says "connect to VPN" actually wants. That is
///   [com.codename1.vpn.profile].
/// - **Shipping a tunnel of your own**, which receives raw IP packets and
///   decides what to do with them. On iOS that code runs in a separate
///   process that no Java virtual machine runs inside, so it cannot be
///   written in this framework at all, and the entitlement for it is one
///   Apple grants case by case. That is [com.codename1.vpn.tunnel], and its
///   documentation is mostly about what it cannot do.
///
/// #### Detecting a VPN is a different question and already answered
///
/// `com.codename1.io.NetworkManager#isVPNActive()` reports whether *any* VPN
/// is carrying this device's traffic, works on far more platforms than can
/// install one, and needs no entitlement. An app that only wants to refuse to
/// run behind a VPN, or to warn about one, should use that and reference
/// nothing here.
///
/// This package itself holds only the shared value types -- [VpnStatus],
/// [VpnError], [VpnException] and [VpnProtocol]. Referencing it alone costs
/// nothing.
package com.codename1.vpn;
