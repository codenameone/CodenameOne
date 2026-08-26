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
package com.codename1.impl.ios;

import com.codename1.vpn.spi.VpnBridge;

/// The iOS half of `com.codename1.vpn`, on `NEVPNManager`.
///
/// A thin forwarder onto `CN1Vpn.m`. Note this is a different thing from the
/// VPN *detection* in `IOSNative.m`, which is always compiled in, needs no
/// entitlement, and answers whether some VPN is carrying this device's
/// traffic rather than managing one.
class IOSVpnBridge implements VpnBridge {

    private final IOSNative nativeInstance;

    IOSVpnBridge(IOSNative nativeInstance) {
        this.nativeInstance = nativeInstance;
        IOSCallCallbacks.install(nativeInstance);
    }

    @Override
    public boolean isVpnSupported() {
        return nativeInstance.vpnSupported();
    }

    @Override
    public boolean isCustomTunnelSupported() {
        return nativeInstance.vpnTunnelSupported();
    }

    @Override
    public int getVpnCapabilities() {
        return nativeInstance.vpnCapabilities();
    }

    @Override
    public int getVpnStatus() {
        return nativeInstance.vpnStatus();
    }

    @Override
    public void installProfile(int requestId, String profileWire) {
        nativeInstance.vpnInstallProfile(requestId, profileWire);
    }

    @Override
    public void removeProfile(int requestId) {
        nativeInstance.vpnRemoveProfile(requestId);
    }

    @Override
    public void loadProfile(int requestId) {
        nativeInstance.vpnLoadProfile(requestId);
    }

    @Override
    public void startVpn(int requestId) {
        nativeInstance.vpnStart(requestId);
    }

    @Override
    public void stopVpn(int requestId) {
        nativeInstance.vpnStop(requestId);
    }

    @Override
    public void setStatusListening(boolean listening) {
        nativeInstance.vpnSetStatusListening(listening);
    }
}
