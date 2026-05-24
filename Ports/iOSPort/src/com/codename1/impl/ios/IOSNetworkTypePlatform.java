/*
 * Copyright (c) 2008, 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 */
package com.codename1.impl.ios;

import com.codename1.io.NetworkManager;
import com.codename1.io.NetworkTypePlatform;

/// iOS network-type tracker backed by SCNetworkReachability. The native
/// install/uninstall hooks live in IOSNative.m and call back into
/// `IOSConnectivity.networkTypeChangedDispatch`.
public final class IOSNetworkTypePlatform extends NetworkTypePlatform {
    @Override
    public int getCurrentNetworkType() {
        return IOSImplementation.nativeInstance.wifiNetworkType();
    }

    @Override
    public void install(NetworkManager target) {
        IOSConnectivity.registerNetworkTypeTarget(target);
        IOSImplementation.nativeInstance.wifiInstallTypeListener(IOSConnectivity.class);
    }

    @Override
    public void uninstall(NetworkManager target) {
        IOSImplementation.nativeInstance.wifiUninstallTypeListener();
        IOSConnectivity.unregisterNetworkTypeTarget();
    }
}
