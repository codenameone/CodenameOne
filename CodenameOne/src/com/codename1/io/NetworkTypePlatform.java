/*
 * Copyright (c) 2008, 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 */
package com.codename1.io;

/// Platform-supplied implementation of the network-type tracking API.
/// `NetworkManager.getCurrentNetworkType()`,
/// `NetworkManager.addNetworkTypeListener(...)` and the matching remover
/// all dispatch through this.
///
/// Part of the framework's service-provider interface, not intended for
/// application use.
public class NetworkTypePlatform {
    /// One of the `NetworkManager.NETWORK_TYPE_*` constants. Default
    /// returns `NETWORK_TYPE_OTHER` when an access point is configured so
    /// stub platforms still indicate "some connectivity present".
    public int getCurrentNetworkType() {
        return NetworkManager.NETWORK_TYPE_NONE;
    }

    /// Subscribe to platform network transitions. The platform must call
    /// `target.fireNetworkTypeChange(newType, vpnActive)` whenever the
    /// active network changes. Default is a no-op for platforms that
    /// can't observe transitions.
    public void install(NetworkManager target) {
    }

    /// Tear down the watcher installed by `install`. Default no-op.
    public void uninstall(NetworkManager target) {
    }
}
