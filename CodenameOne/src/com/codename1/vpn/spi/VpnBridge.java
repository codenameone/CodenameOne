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
package com.codename1.vpn.spi;

/// Internal service-provider interface implemented by each platform port to
/// carry the `com.codename1.vpn` API onto the native VPN machinery: Apple's
/// `NEVPNManager` and `NETunnelProviderManager`, and Android's `VpnManager`
/// and `VpnService`.
///
/// Application code never touches this interface. It is obtained by the
/// `com.codename1.vpn` packages from `com.codename1.ui.Display#getVpnBridge()`,
/// and the base implementation returns `null` -- which is why the public API
/// degrades to a well-behaved `NOT_SUPPORTED` on ports that implement
/// nothing.
///
/// The same rules as `com.codename1.call.spi.CallBridge` apply: primitives,
/// strings and byte arrays only; asynchrony by caller-allocated `requestId`;
/// **every operation answers exactly once**; unsolicited events marshal to the
/// EDT themselves.
public interface VpnBridge {

    /// [#getVpnCapabilities()] bit: an IKEv2 configuration can be installed.
    int CAPABILITY_IKEV2 = 1;

    /// [#getVpnCapabilities()] bit: an IPsec configuration can be installed.
    int CAPABILITY_IPSEC = 2;

    /// Reserved. **No port sets this.** A packet tunnel the app implements
    /// is not shipped: on iOS it runs in an app extension with no Java
    /// virtual machine in it, so its body could not be written in this
    /// framework, and on Android it needs a bound `VpnService` and a packet
    /// API that does not exist here. The constant and
    /// [#isCustomTunnelSupported()] are kept as the seam it would attach to.
    int CAPABILITY_CUSTOM_TUNNEL = 4;

    /// [#getVpnCapabilities()] bit: on-demand rules are honoured.
    ///
    /// iOS sets this; Android does not, because its managed profile API has
    /// no equivalent and a bit that promised one would send apps down a path
    /// that quietly becomes an ordinary manually started tunnel.
    int CAPABILITY_ON_DEMAND = 8;

    /// Reserved. **No port sets this**, and there is no `alwaysOn` on
    /// `VpnProfile` to pair it with: always-on VPN needs a supervised device
    /// and an MDM payload on iOS, and a Settings toggle or a device-owner API
    /// on Android. The constant is kept so the bit values below it do not
    /// shift if either platform ever opens it up.
    int CAPABILITY_ALWAYS_ON = 16;

    /// [#getVpnCapabilities()] bit: per-application routing is offered.
    int CAPABILITY_PER_APP = 32;

    /// Whether this port can install and control a VPN configuration.
    ///
    /// Distinct from merely detecting one: `com.codename1.io.NetworkManager`
    /// answers "is a VPN up" on far more platforms than can install one.
    boolean isVpnSupported();

    /// Whether this port can run a packet tunnel this app implements.
    ///
    /// Every port answers false today; see [#CAPABILITY_CUSTOM_TUNNEL].
    boolean isCustomTunnelSupported();

    /// The `CAPABILITY_*` bit mask this port supports.
    int getVpnCapabilities();

    /// The ordinal of the current `com.codename1.vpn.VpnStatus`.
    int getVpnStatus();

    /// Installs or replaces the configuration described by `profileWire`, a
    /// `com.codename1.impl.vpn.VpnWire` record.
    ///
    /// Both platforms show the user a prompt here, and both refuse silently
    /// if the app lacks the entitlement, so a port must answer
    /// `USER_DECLINED` or `UNAUTHORIZED` rather than letting the request
    /// hang.
    void installProfile(int requestId, String profileWire);

    /// Removes the installed configuration.
    void removeProfile(int requestId);

    /// Answers with the installed configuration as a wire record, or an empty
    /// string when none is installed.
    void loadProfile(int requestId);

    /// Brings the tunnel up.
    void startVpn(int requestId);

    /// Takes the tunnel down.
    void stopVpn(int requestId);

    /// Starts or stops delivery of status changes to
    /// `com.codename1.vpn.profile.Vpn#deliverStatusChanged`.
    void setStatusListening(boolean listening);
}
