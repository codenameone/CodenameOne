/*
 * Copyright (c) 2008, 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 */
package com.codename1.io.bonjour;

/// Callback for `BonjourBrowser.browse(...)`. All methods fire on the EDT.
public interface BonjourServiceListener {
    /// A new service appeared, or an existing one was re-resolved with a new
    /// host/port. The platform may also call this when only TXT metadata
    /// changes.
    void onServiceResolved(BonjourService service);

    /// A previously announced service went away.
    void onServiceLost(BonjourService service);

    /// The browse itself failed (e.g. WiFi disconnected, or the platform
    /// does not support Bonjour).
    void onBrowseError(Throwable error);
}
