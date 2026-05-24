/*
 * Copyright (c) 2008, 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 */
package com.codename1.io.wifi;

/// Listener for WiFi Direct peer discovery. Methods fire on the EDT.
public interface WiFiDirectListener {
    /// Fired whenever the platform reports a new snapshot of the peer list.
    /// The array is sorted by signal strength when the platform supplies it,
    /// otherwise by discovery order.
    void onPeersAvailable(WiFiDirectPeer[] peers);

    /// Fired when discovery itself fails (e.g. WiFi is off).
    void onDiscoveryError(Throwable error);
}
